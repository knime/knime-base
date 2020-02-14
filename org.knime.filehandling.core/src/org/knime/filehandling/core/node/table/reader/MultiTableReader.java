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
 *   Jan 24, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static org.knime.filehandling.core.node.table.reader.util.MultiTableUtils.getNameAfterInit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorUtils;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.typehierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.util.IndexMapper;
import org.knime.filehandling.core.node.table.reader.util.DefaultIndexMapper.DefaultIndexMapperBuilder;

/**
 * Uses a {@link TableReader} to read tables from multiple paths, combines them according to the user settings and
 * performs type mapping.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used by the reader to identify individual data types
 * @param <V> the type of tokens the reader produces
 */
final class MultiTableReader<C extends ReaderSpecificConfig<C>, T, V> {

    private final TypeHierarchy<T, T> m_typeHierarchy;

    private final ProducerRegistry<T, ? extends ReadAdapter<T, V>> m_producerRegistry;

    private final ReadAdapterFactory<T, V> m_readAdapterFactory;

    private final TableReader<C, T, V> m_reader;

    private final Map<T, DataType> m_defaultKnimeTypes;

    private final Function<V, String> m_rowKeyExtractor;

    /**
     * Stores for each path the spec of the table stored at this path.
     */
    private Map<Path, ReaderTableSpec<T>> m_individualSpecs;

    /**
     * The combined {@link ReaderTableSpec} of all individual specs.
     */
    private ReaderTableSpec<T> m_mergedReaderTableSpec;

    /**
     * Constructor.
     *
     * @param typeHierarchy provides {@link TypeHierarchy} objects that are used to find the common most specific type
     *            of the individual read specs
     * @param readAdapterFactory a factory that creates concrete implementations of {@link ReadAdapter}
     * @param reader the {@link TableReader} implementation to use for reading
     * @param defaultKnimeTypes provides the default {@link DataType} for each external type
     * @param rowKeyExtractor to extract row keys from values
     */
    public MultiTableReader(final TypeHierarchy<T, T> typeHierarchy, final ReadAdapterFactory<T, V> readAdapterFactory,
        final TableReader<C, T, V> reader, final Map<T, DataType> defaultKnimeTypes,
        final Function<V, String> rowKeyExtractor) {
        m_typeHierarchy = typeHierarchy;
        m_producerRegistry = readAdapterFactory.getProducerRegistry();
        m_readAdapterFactory = readAdapterFactory;
        m_reader = reader;
        m_defaultKnimeTypes = new HashMap<>(defaultKnimeTypes);
        m_rowKeyExtractor = rowKeyExtractor;
    }

    void reset() {
        m_individualSpecs = null;
        m_mergedReaderTableSpec = null;
    }

    /**
     * Creates the {@link DataTableSpec} corresponding to the tables stored in <b>paths</b> combined according to the
     * provided {@link MultiTableReadConfig config}.
     *
     * @param paths to read from
     * @param config for reading
     * @return the {@link DataTableSpec} of the merged table consisting of the tables stored in <b>paths</b>
     * @throws IOException if reading the specs from {@link Path paths} fails
     */
    public DataTableSpec createTableSpec(final List<Path> paths, final MultiTableReadConfig<C> config)
        throws IOException {
        return readSpecs(paths, config).getOutputSpec();
    }

    private RunConfig readSpecs(final List<Path> paths, final MultiTableReadConfig<C> config) throws IOException {
        final Map<Path, ReaderTableSpec<T>> specs = new LinkedHashMap<>(paths.size());
        // TODO parallelize
        for (Path path : paths) {
            final ReaderTableSpec<T> spec = m_reader.readSpec(path, config.getTableReadConfig());
            specs.put(path, assignNamesIfMissing(spec));
        }
        // remember the specs to avoid calculating them again during the actual reading
        m_individualSpecs = specs;
        m_mergedReaderTableSpec = config.getSpecMergeMode().mergeSpecs(specs.values(), m_typeHierarchy);
        return new RunConfig(m_mergedReaderTableSpec, config);
    }

    private ReaderTableSpec<T> assignNamesIfMissing(final ReaderTableSpec<T> spec) {
        return new ReaderTableSpec<>(IntStream.range(0, spec.size())
            .mapToObj(i -> assignNameIfMissing(i, spec.getColumnSpec(i))).collect(Collectors.toList()));
    }

    private ReaderColumnSpec<T> assignNameIfMissing(final int idx, final ReaderColumnSpec<T> spec) {
        final Optional<String> name = spec.getName();
        if (name.isPresent()) {
            return spec;
        } else {
            return ReaderColumnSpec.createWithName(createDefaultColumnName(idx), spec.getType());
        }
    }

    // TODO add once we have a column filter config
    //    private ColumnFilterConfig getColumnFilter(final TableReadConfig<?> config) {
    //        final ColumnFilterConfig columnFilter = config.getColumnFilter();
    //        if (columnFilter.canAccess()) {
    //            return columnFilter;
    //        } else {
    //            return ConfigUtil.createAllAcceptingColumnFilterConfig(m_jointReaderTableSpec.size());
    //        }
    //    }

    private static String createDefaultColumnName(final int iFinal) {
        return "Column" + iFinal;
    }

    /**
     * Reads a table from the provided {@link Path paths} according to the provided {@link MultiTableReadConfig config}.
     *
     * @param paths to read from
     * @param config for reading
     * @param exec for table creation and reporting progress
     * @return the read table
     * @throws Exception
     */
    public BufferedDataTable readTable(final List<Path> paths, final MultiTableReadConfig<C> config,
        final ExecutionContext exec) throws Exception {
        exec.setMessage("Creating table spec");
        final RunConfig runConfig = createRunConfig(paths, config);
        final BufferedDataTableRowOutput output =
            new BufferedDataTableRowOutput(exec.createDataContainer(runConfig.getOutputSpec()));
        fillRowOutput(runConfig, paths, config, output, exec);
        return output.getDataTable();
    }

    /**
     * Fills the {@link RowOutput output} with the tables stored in <b>paths</b> using the provided
     * {@link MultiTableReadConfig config}. The {@link ExecutionContext} is required for type mapping and reporting
     * progress. Note: The {@link RowOutput output} must have a {@link DataTableSpec} compatible with the
     * {@link DataTableSpec} returned by createTableSpec(List, MultiTableReadConfig).
     *
     * @param paths to read from
     * @param config for reading
     * @param output the {@link RowOutput} to fill
     * @param exec needed by the mapping framework
     * @throws Exception
     */
    public void fillRowOutput(final List<Path> paths, final MultiTableReadConfig<C> config, final RowOutput output,
        final ExecutionContext exec) throws Exception {
        exec.setMessage("Creating table spec");
        final RunConfig runConfig = createRunConfig(paths, config);
        fillRowOutput(runConfig, paths, config, output, exec);
    }

    private RunConfig createRunConfig(final List<Path> paths, final MultiTableReadConfig<C> config) throws IOException {
        if (m_mergedReaderTableSpec == null || m_individualSpecs == null || notAllPathsContained(paths)) {
            return readSpecs(paths, config);
        } else {
            return new RunConfig(m_mergedReaderTableSpec, config);
        }
    }

    private boolean notAllPathsContained(final List<Path> paths) {
        assert m_individualSpecs != null : "Only call this method if the individual specs have been initialized.";
        return !paths.stream().allMatch(m_individualSpecs::containsKey);
    }

    private void fillRowOutput(final RunConfig runConfig, final List<Path> paths, final MultiTableReadConfig<C> config,
        final RowOutput output, final ExecutionContext exec) throws Exception {
        exec.setMessage("Reading table");
        // TODO parallelize
        for (Path path : paths) {
            final ExecutionMonitor progress = exec.createSubProgress(1.0 / paths.size());
            final ReaderTableSpec<T> pathSpec = m_individualSpecs.get(path);
            final TableReadConfig<C> pathSpecificConfig =
                createIndividualConfig(runConfig.getOutputSpec(), pathSpec, config.getTableReadConfig());
            try (Read<V> read = m_reader.read(path, pathSpecificConfig)) {
                if (pathSpecificConfig.useColumnHeaderIdx()) {
                    read.next();
                }
                final IndividualTableReader<ReadAdapter<T, V>> reader = new IndividualTableReader<>(
                    createReadAdapter(read, runConfig.getOutputSpec(), pathSpec,
                        createKeyGenerator(path, pathSpecificConfig), pathSpecificConfig),
                    FileStoreFactory.createFileStoreFactory(exec), runConfig.getProductionPaths());
                reader.fillOutput(output, progress);
            }
            progress.setProgress(1.0);
        }
        output.close();
    }

    private TableReadConfig<C> createIndividualConfig(final DataTableSpec outputSpec,
        final ReaderTableSpec<T> individualSpec, final TableReadConfig<C> generalConfig) {
        /*
         * Adjust the filtering and possibly other settings that vary with differing table specs
         */
        // TODO add once we have a column filter config
        //        final ColumnFilterConfig generalColumnFilter = getColumnFilter(generalConfig);
        //        final boolean[] individualColumnFilter = new boolean[individualSpec.size()];
        //        for (int i = 0; i < individualSpec.size(); i++) {
        //            final ReaderColumnSpec<?> colSpec = individualSpec.getColumnSpec(i);
        //            final int jointIdx = outputSpec.findColumnIndex(TableReaderUtil.getNameAfterInit(colSpec));
        //            individualColumnFilter[i] = jointIdx != -1 && generalColumnFilter.test(jointIdx);
        //        }
        final TableReadConfig<C> individualConfig = generalConfig.copy();
        //        individualConfig.setColumnFilter(ConfigUtil.createColumnFilterConfig(individualColumnFilter));
        return individualConfig;
    }

    private ReadAdapter<T, V> createReadAdapter(final Read<V> read, final DataTableSpec outputSpec,
        final ReaderTableSpec<T> individualSpec, final RowKeyGenerator<V> keyGenerator,
        final TableReadConfig<?> config) {
        final IndexMapper indexMapper = createIndexMapper(outputSpec, individualSpec, config);
        final ReadAdapter<T, V> readAdapter = m_readAdapterFactory.createReadAdapter();
        readAdapter.setIndexMapper(indexMapper);
        readAdapter.setRowKeyGenerator(keyGenerator);
        readAdapter.setRead(read);
        return readAdapter;
    }

    private IndexMapper createIndexMapper(final DataTableSpec outputSpec,
        final ReaderTableSpec<T> individualSpec, final TableReadConfig<?> config) {
        final int rowIDIdx = config.getRowIDIdx();
        final boolean useRowIDIdx = config.useRowIDIdx();
        final DefaultIndexMapperBuilder mapperBuilder =
            useRowIDIdx ? new DefaultIndexMapperBuilder(outputSpec.getNumColumns(), rowIDIdx)
                : new DefaultIndexMapperBuilder(outputSpec.getNumColumns());
        for (int i = 0; i < individualSpec.size(); i++) {
            final ReaderColumnSpec<T> colSpec = individualSpec.getColumnSpec(i);
            final int jointIdx = outputSpec.findColumnIndex(getNameAfterInit(colSpec));
            if (jointIdx >= 0) {
                mapperBuilder.addMapping(jointIdx, i);
            }
        }
        return mapperBuilder.build();
    }

    private RowKeyGenerator<V> createKeyGenerator(final Path path, final TableReadConfig<?> config) {
        if (config.useRowIDIdx()) {
            return RowKeyGeneratorUtils.createExtractingRowKeyGenerator(path.toString(), m_rowKeyExtractor,
                config.getRowIDIdx());
        } else {
            return RowKeyGeneratorUtils.createCountingRowKeyGenerator(path.toString());
        }
    }

    private static IntStream createFilteredRange(/* TODO final ColumnFilterConfig filter*/ final int numColumns) {
        //        return IntStream.range(0, filter.getNumColumns()).filter(filter);
        return IntStream.range(0, numColumns);
    }

    /**
     * Stores the output {@link DataTableSpec} alongside the {@link ProductionPath ProductionPaths} used to create it.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private final class RunConfig {
        private final DataTableSpec m_outputSpec;

        private final ProductionPath[] m_productionPaths;

        RunConfig(final ReaderTableSpec<T> jointSpec, final MultiTableReadConfig<?> config) {
            m_productionPaths = getFinalProductionPaths(config);
            //            final ColumnFilterConfig columnFilter = getColumnFilter(config.getTableReadConfig());
            //            final Iterator<ReaderColumnSpec<T>> specIter =
            //                createFilteredRange(columnFilter).mapToObj(jointSpec::getColumnSpec).iterator();
            final Iterator<ReaderColumnSpec<T>> specIter = jointSpec.iterator();
            final DataColumnSpec[] colSpecs = new DataColumnSpec[m_productionPaths.length];
            for (int i = 0; i < m_productionPaths.length; i++) {
                assert specIter.hasNext();
                final DataType type = m_productionPaths[i].getConverterFactory().getDestinationType();
                final String name = getNameAfterInit(specIter.next());
                colSpecs[i] = new DataColumnSpecCreator(name, type).createSpec();
            }
            m_outputSpec = new DataTableSpec(colSpecs);
        }

        DataTableSpec getOutputSpec() {
            return m_outputSpec;
        }

        ProductionPath[] getProductionPaths() {
            return m_productionPaths;
        }

        private ProductionPath[] getFinalProductionPaths(final MultiTableReadConfig<?> config) {
            //            final ColumnFilterConfig columnFilter = getColumnFilter(config.getTableReadConfig());
            //            return getDefaultProductionPaths(columnFilter);
            return getDefaultProductionPaths();
        }

        private ProductionPath[] getDefaultProductionPaths(/* final ColumnFilterConfig columnFilter*/) {
            assert m_mergedReaderTableSpec != null : //
            "Joint reader table spec must be initialized before calling this method.";
            //            assert m_jointReaderTableSpec.size() == columnFilter
            //                .getNumColumns() : "Column filter doesn't match number of columns";
            //            return createFilteredRange(columnFilter).mapToObj(m_jointReaderTableSpec::getColumnSpec)
            //                .map(this::getDefaultProductionPath).toArray(ProductionPath[]::new);
            return m_mergedReaderTableSpec.stream() //
                .map(this::getDefaultProductionPath) //
                .toArray(ProductionPath[]::new);
        }

        private ProductionPath getDefaultProductionPath(final ReaderColumnSpec<T> spec) {
            final T type = spec.getType();
            final List<ProductionPath> availablePaths = m_producerRegistry.getAvailableProductionPaths(type);
            CheckUtils.checkState(!availablePaths.isEmpty(), "No production path available for type %s.", type);
            final DataType defaultKnimeType = m_defaultKnimeTypes.get(type);
            CheckUtils.checkState(defaultKnimeType != null, "No default type for type %s provided.", type);
            return availablePaths.stream()
                .filter(p -> p.getConverterFactory().getDestinationType().equals(defaultKnimeType)).findAny()
                .orElseThrow(() -> new IllegalStateException(String
                    .format("There exists no production path from type %s to KNIME type %s.", type, defaultKnimeType)));
        }
    }
}
