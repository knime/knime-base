/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Nov 13, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader;

import static org.knime.filehandling.core.node.table.reader.util.MultiTableUtils.transformToString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InternalTableAPI;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.ThreadUtils;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.rowkey.GenericRowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.mapping.DefaultTypeMapper;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapper;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapperException;
import org.knime.filehandling.core.node.table.reader.util.IndividualTableReader;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.TableTransformationFactory;

/**
 * Default implementation of a {@link StagedMultiTableRead}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type representing external types
 * @param <V> the type representing values
 * @noreference non-public API
 * @noextend non-public API
 */
final class DefaultStagedMultiTableRead<I, C extends ReaderSpecificConfig<C>, T, V>
    implements StagedMultiTableRead<I, T> {

    private final Map<I, TypedReaderTableSpec<T>> m_individualSpecs;

    private final RawSpec<T> m_rawSpec;

    private final GenericRowKeyGeneratorContextFactory<I, V> m_rowKeyGenFactory;

    private final Supplier<ReadAdapter<T, V>> m_readAdapterSupplier;

    private final MultiTableReadConfig<C, T> m_config;

    private final TableTransformationFactory<T> m_tableTransformationFactory;

    private final GenericTableReader<I, C, T, V> m_reader;

    private final DataColumnSpec m_itemIdColumn;

    /**
     * Constructor.
     *
     * @param reader {@link GenericTableReader}
     * @param individualSpecs individuals specs
     * @param rowKeyGenFactory {@link GenericRowKeyGeneratorContextFactory}
     * @param rawSpec the {@link RawSpec}
     * @param readAdapterSupplier {@link ReadAdapter} supplier
     * @param tableTransformationFactory for creating a default {@link TableTransformation} if necessary
     * @param config the {@link MultiTableReadConfig} for this read process
     */
    DefaultStagedMultiTableRead(final GenericTableReader<I, C, T, V> reader,
        final Map<I, TypedReaderTableSpec<T>> individualSpecs,
        final GenericRowKeyGeneratorContextFactory<I, V> rowKeyGenFactory, final RawSpec<T> rawSpec,
        final Supplier<ReadAdapter<T, V>> readAdapterSupplier,
        final TableTransformationFactory<T> tableTransformationFactory, final MultiTableReadConfig<C, T> config,
        final DataColumnSpec itemIdentifierColumn) {
        m_rawSpec = rawSpec;
        m_individualSpecs = individualSpecs;
        m_rowKeyGenFactory = rowKeyGenFactory;
        m_config = config;
        m_tableTransformationFactory = tableTransformationFactory;
        m_reader = reader;
        m_readAdapterSupplier = readAdapterSupplier;
        m_itemIdColumn = itemIdentifierColumn;
    }

    @Override
    public MultiTableRead<T> withoutTransformation(final SourceGroup<I> sourceGroup) {
        if (m_config.hasTableSpecConfig()) {
            final TableSpecConfig<T> tableSpecConfig = m_config.getTableSpecConfig();
            final TableTransformation<T> configuredTransformation = tableSpecConfig.getTableTransformation();
            if (tableSpecConfig.isConfiguredWith(m_config.getConfigID(), transformToString(sourceGroup))) {
                return createMultiTableRead(sourceGroup, configuredTransformation, m_config.getTableReadConfig(),
                    tableSpecConfig);
            } else {
                final TableTransformation<T> adaptedTransformation =
                    m_tableTransformationFactory.createFromExisting(m_rawSpec, m_config, configuredTransformation);
                return withTransformation(sourceGroup, adaptedTransformation);
            }
        } else {
            final TableTransformation<T> newTransformation =
                m_tableTransformationFactory.createNew(m_rawSpec, m_config);
            return withTransformation(sourceGroup, newTransformation);
        }
    }

    @Override
    public MultiTableRead<T> withTransformation(final SourceGroup<I> sourceGroup,
        final TableTransformation<T> transformationModel) {
        final TableReadConfig<C> tableReadConfig = m_config.getTableReadConfig();
        final ConfigID id = m_config.getConfigID();
        final TableSpecConfig<T> tableSpecConfig = DefaultTableSpecConfig.createFromTransformationModel(
            sourceGroup.getID(), id, m_individualSpecs, transformationModel, m_itemIdColumn);
        return createMultiTableRead(sourceGroup, transformationModel, tableReadConfig, tableSpecConfig);
    }

    // TODO remove tableReadConfig parameter as it is part of m_config
    private MultiTableRead<T> createMultiTableRead(final SourceGroup<I> sourceGroup,
        final TableTransformation<T> transformationModel, final TableReadConfig<C> tableReadConfig,
        final TableSpecConfig<T> tableSpecConfig) {
        final var keepReadsOpen = m_reader instanceof KeepReadOpenReader;
        var delegate = new DefaultMultiTableRead<>(sourceGroup, p -> createRead(p, tableReadConfig), () -> {
            var factory = createIndividualTableReaderFactory(transformationModel);
            return factory::create;
        }, tableReadConfig, tableSpecConfig, keepReadsOpen);
        if (!canBeParallelized(sourceGroup)) {
            return delegate;
        }
        var typeMapperExceptionParser =
            new TypeMapperExceptionParser(sourceGroup.size(), tableReadConfig.limitRowsForSpec());
        return new ParallelMultiTableRead(delegate, sourceGroup,
            () -> createIndividualTableReaderFactory(transformationModel), typeMapperExceptionParser);
    }

    private boolean canBeParallelized(final SourceGroup<I> sourceGroup) {
        var tableReadConfig = m_config.getTableReadConfig();
        var specialRowIDs = tableReadConfig.useRowIDIdx() ? tableReadConfig.prependSourceIdxToRowID()
            : !"Row".equals(tableReadConfig.getPrefixForGeneratedRowIDs());
        return !specialRowIDs && m_reader.canBeReadInParallel(sourceGroup);
    }

    private IndividualTableReaderFactory<I, T, V>
        createIndividualTableReaderFactory(final TableTransformation<T> transformationModel) {
        final TableReadConfig<C> tableReadConfig = m_config.getTableReadConfig();
        return new IndividualTableReaderFactory<>(m_individualSpecs, tableReadConfig, transformationModel,
            this::createTypeMapper, m_rowKeyGenFactory.createContext(tableReadConfig),
            createItemIdentifierCellFactory());
    }

    private Function<I, DataCell> createItemIdentifierCellFactory() {
        if (m_config.appendItemIdentifierColumn()) {
            return m_reader::createIdentifierCell;
        } else {
            return i -> null;
        }
    }

    @SuppressWarnings("resource")
    private Read<V> createRead(final I path, final TableReadConfig<C> config) throws IOException {
        final Read<V> rawRead = m_reader.read(path, config);
        return decorateRead(config, rawRead);
    }

    @SuppressWarnings("deprecation")
    private Read<V> decorateRead(final TableReadConfig<C> config, final Read<V> rawRead) {
        if (config.decorateRead() && rawRead.needsDecoration()) {
            return ReadUtils.decorateForReading(rawRead, config);
        }
        return rawRead;
    }

    private TypeMapper<V> createTypeMapper(final ProductionPath[] prodPaths, final FileStoreFactory fsFactory) {
        return new DefaultTypeMapper<>(m_readAdapterSupplier.get(), prodPaths, fsFactory,
            m_config.getTableReadConfig().getReaderSpecificConfig());
    }

    @Override
    public RawSpec<T> getRawSpec() {
        return m_rawSpec;
    }

    @Override
    public boolean isValidFor(final SourceGroup<I> sourceGroup) {
        return sourceGroup.size() == m_individualSpecs.size() && containsAll(m_individualSpecs.keySet(), sourceGroup);
    }

    private static <I> boolean containsAll(final Set<I> keys, final SourceGroup<I> sourceGroup) {
        return sourceGroup.stream().allMatch(keys::contains);
    }

    private final class ParallelMultiTableRead implements MultiTableRead<T> {

        private final DefaultMultiTableRead<I, T, V> m_delegate;

        private final SourceGroup<I> m_sourceGroup;

        private final Supplier<IndividualTableReaderFactory<I, T, V>> m_readerFactorySupplier;

        private final TypeMapperExceptionParser m_typeMapperExceptionParser;

        ParallelMultiTableRead(final DefaultMultiTableRead<I, T, V> delegate, final SourceGroup<I> sourceGroup,
            final Supplier<IndividualTableReaderFactory<I, T, V>> readerFactory,
            final TypeMapperExceptionParser typeMapperExceptionParser) {
            m_delegate = delegate;
            m_sourceGroup = sourceGroup;
            m_readerFactorySupplier = readerFactory;
            m_typeMapperExceptionParser = typeMapperExceptionParser;
        }

        @Override
        public BufferedDataTable readTable(final ExecutionContext exec) throws Exception {
            var fsFactory = FileStoreFactory.createFileStoreFactory(exec);
            var tables = new ArrayList<BufferedDataTable>();
            for (I item : m_sourceGroup) {
                exec.checkCanceled();
                final var itemExec = exec.createSubExecutionContext(1.0 / m_sourceGroup.size());
                var reads = createReads(item);
                var chunkReaders = reads.stream()//
                        .map(r -> new TableChunkReader(r, m_readerFactorySupplier.get().create(item, fsFactory),//
                            itemExec.createSilentSubExecutionContext(1.0 / reads.size()))//
                        ).toList();
                if (chunkReaders.size() == 1) {
                    // if there is only one chunk execute in current thread
                    try {
                        tables.add(chunkReaders.get(0).readTableChunk());
                    } catch (Exception ex) {
                        throw tryToParseException(ex, item);
                    }
                } else {
                    var tableChunks = readChunksInParallel(chunkReaders, item);
                    validateChunks(tableChunks);
                    tables.addAll(tableChunks);
                }
            }
            var concatenatedTable = concatenateTables(exec, tables);
            return sanitizeDomain(exec, concatenatedTable);
        }

        private BufferedDataTable sanitizeDomain(final ExecutionContext exec, final BufferedDataTable table) {
            var tableSpec = table.getDataTableSpec();
            var columnsWithTooManyPossibleValues = pruneColumnsWithTooManyPossibleValues(tableSpec);
            if (columnsWithTooManyPossibleValues.isEmpty()) {
                return table;
            }
            var specCreator = new DataTableSpecCreator(tableSpec);
            columnsWithTooManyPossibleValues
                .forEach(c -> specCreator.replaceColumn(tableSpec.findColumnIndex(c.getName()), c));
            return exec.createSpecReplacerTable(table, specCreator.createSpec());
        }

        /**
         * The concatenation logic does not respect the maximal number of possible values, so we have to enforce it
         */
        private static List<DataColumnSpec> pruneColumnsWithTooManyPossibleValues(final DataTableSpec spec) {
            var maxNumPossibleValues = DataContainerSettings.getDefault().getMaxDomainValues();
            return spec.stream()//
                    .filter(c -> {
                        var domain = c.getDomain();
                        return domain.hasValues() && domain.getValues().size() > maxNumPossibleValues;
                    }).map(DefaultStagedMultiTableRead.ParallelMultiTableRead::dropPossibleValues)//
                    .toList();
        }

        private static DataColumnSpec dropPossibleValues(final DataColumnSpec spec) {
            var specCreator = new DataColumnSpecCreator(spec);
            var domainCreator = new DataColumnDomainCreator(spec.getDomain());
            domainCreator.setValues(null);
            specCreator.setDomain(domainCreator.createDomain());
            return specCreator.createSpec();
        }

        private BufferedDataTable concatenateTables(final ExecutionContext exec,
            final ArrayList<BufferedDataTable> tables) throws CanceledExecutionException {
            if (m_config.getTableReadConfig().useRowIDIdx()) {
                return exec.createConcatenateTable(exec.createSubProgress(0), tables.toArray(BufferedDataTable[]::new));
            } else {
                return InternalTableAPI.concatenateWithNewRowID(exec, tables.toArray(BufferedDataTable[]::new));
            }
        }

        private void validateChunks(final ArrayList<BufferedDataTable> tableChunks) {
            if (!m_config.getTableReadConfig().allowShortRows()) {
                CheckUtils.checkArgument(//
                    tableChunks.stream()//
                    .mapToInt(t -> getNumColumns(t))//
                    .distinct()//
                    .count() == 1, //
                        "Not all rows have the same number of cells.");
            }
        }

        private ArrayList<BufferedDataTable> readChunksInParallel(final List<TableChunkReader> chunkReaders,
            final I item) throws Exception {
            try {
                // runInvisible ensures that the waiting thread does not block a core token
                return KNIMEConstants.GLOBAL_THREAD_POOL.runInvisible(() -> {
                    var tableFutures = submitChunks(chunkReaders);
                    return collectChunks(item, tableFutures);
                });
            } catch (ExecutionException ex) {//NOSONAR
                throw toExceptionOrThrowError(ex.getCause());
            }
        }

        private ArrayList<Future<BufferedDataTable>> submitChunks(final List<TableChunkReader> chunkReaders)
            throws InterruptedException {
            var tableFutures = new ArrayList<Future<BufferedDataTable>>(chunkReaders.size());
            for (var reader : chunkReaders) {
                tableFutures.add(KNIMEConstants.GLOBAL_THREAD_POOL
                    .submit(ThreadUtils.callableWithContext(reader::readTableChunk)));
            }
            return tableFutures;
        }

        private ArrayList<BufferedDataTable> collectChunks(final I item,
            final ArrayList<Future<BufferedDataTable>> tableFutures) throws Exception {
            var chunks = new ArrayList<BufferedDataTable>();
            for (var future : tableFutures) {
                try {
                    chunks.add(future.get());
                } catch (ExecutionException ex) {//NOSONAR
                    throw tryToParseException(ex.getCause(), item);
                }
            }
            return chunks;
        }

        private static int getNumColumns(final BufferedDataTable table) {
            return table.getDataTableSpec().getNumColumns();
        }

        private List<Read<V>> createReads(final I item) throws IOException {
            var rawReads = m_reader.multiRead(item, m_config.getTableReadConfig());
            return rawReads.stream()//
                    .map(r -> decorateRead(m_config.getTableReadConfig(), r))//
                    .toList();
        }

        private final class TableChunkReader {
            private final Read<V> m_read;

            private final IndividualTableReader<V> m_tableReader;

            private final ExecutionContext m_exec;

            TableChunkReader(final Read<V> read, final IndividualTableReader<V> reader, final ExecutionContext exec) {
                m_read = read;
                m_tableReader = reader;
                m_exec = exec;
            }

            BufferedDataTable readTableChunk() throws Exception {
                try {
                    var container = m_exec.createDataContainer(getOutputSpec());
                    var rowOutput = new BufferedDataTableRowOutput(container);
                    m_tableReader.fillOutput(m_read, rowOutput, m_exec);
                    container.close();
                    return container.getTable();
                } finally {
                    m_read.close();
                }
            }
        }

        private Exception tryToParseException(final Throwable throwable, final I item) throws Exception {
            if (throwable instanceof TypeMapperException typeMapperException) {
                return m_typeMapperExceptionParser.parse(typeMapperException, item.toString());
            }
            return toExceptionOrThrowError(throwable);
        }

        private static Exception toExceptionOrThrowError(final Throwable throwable) throws Exception {
            if (throwable instanceof Exception exception) {
                throw exception;
            } else if (throwable instanceof Error error) {
                throw error;
            } else {
                throw new IllegalStateException(throwable);
            }
        }


        @Override
        public DataTableSpec getOutputSpec() {
            return m_delegate.getOutputSpec();
        }

        @Override
        public TableSpecConfig<T> getTableSpecConfig() {
            return m_delegate.getTableSpecConfig();
        }

        @Override
        public PreviewRowIterator createPreviewIterator() {
            return m_delegate.createPreviewIterator();
        }

        @Override
        public void fillRowOutput(final RowOutput output, final ExecutionMonitor exec, final FileStoreFactory fsFactory)
            throws Exception {
            m_delegate.fillRowOutput(output, exec, fsFactory);
        }

    }
}