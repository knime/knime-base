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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.type.mapping.MappingRuntimeException;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapperException;
import org.knime.filehandling.core.node.table.reader.util.IndividualTableReader;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.util.CheckedExceptionFunction;

/**
 * Default implementation of MultiTableRead.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <T> the type used to identify external types
 * @param <V> the type representing values
 * @noreference non-public API
 * @noextend non-public API
 */
public final class DefaultMultiTableRead<I, T, V> implements MultiTableRead<T> {

    private final DataTableSpec m_outputSpec;

    private final TableSpecConfig<T> m_tableSpecConfig;

    private final TableReadConfig<?> m_tableReadConfig;

    private final SourceGroup<I> m_sourceGroup;

    private final CheckedExceptionFunction<I, ? extends Read<V>, IOException> m_readFn;

    private final Supplier<BiFunction<I, FileStoreFactory, ? extends IndividualTableReader<V>>>
        m_individualTableReaderFactorySupplier;

    private final boolean m_keepReadsOpen;

    private final TypeMapperExceptionParser m_typeMapperExceptionParser;

    /**
     * Constructor.
     *
     * @param sourceGroup the {@link SourceGroup}
     * @param readFn produces a {@link Read} from a item
     * @param individualTableReaderFactorySupplier creates {@link IndividualTableReader IndividualTableReaders} from
     *            item
     * @param tableReadConfig the {@link TableReadConfig}
     * @param tableSpecConfig corresponding to this instance
     */
    public DefaultMultiTableRead(final SourceGroup<I> sourceGroup,
        final CheckedExceptionFunction<I, ? extends Read<V>, IOException> readFn,
        final Supplier<BiFunction<I, FileStoreFactory, ? extends IndividualTableReader<V>>>
            individualTableReaderFactorySupplier,
        final TableReadConfig<?> tableReadConfig, final TableSpecConfig<T> tableSpecConfig) {
        this(sourceGroup, readFn, individualTableReaderFactorySupplier, tableReadConfig, tableSpecConfig, false);
    }

    /**
     * Constructor.
     *
     * @param sourceGroup the {@link SourceGroup}
     * @param readFn produces a {@link Read} from a item
     * @param individualTableReaderFactorySupplier creates {@link IndividualTableReader IndividualTableReaders} from
     *            item
     * @param tableReadConfig the {@link TableReadConfig}
     * @param tableSpecConfig corresponding to this instance
     * @param keepReadsOpen indicate that reads should be kept open as part of the fix for AP-18002
     */
    DefaultMultiTableRead(final SourceGroup<I> sourceGroup,
        final CheckedExceptionFunction<I, ? extends Read<V>, IOException> readFn,
        final Supplier<BiFunction<I, FileStoreFactory, ? extends IndividualTableReader<V>>>
            individualTableReaderFactorySupplier,
        final TableReadConfig<?> tableReadConfig, final TableSpecConfig<T> tableSpecConfig,
        final boolean keepReadsOpen) {
        m_outputSpec = tableSpecConfig.getDataTableSpec();
        m_tableSpecConfig = tableSpecConfig;
        m_tableReadConfig = tableReadConfig;
        m_readFn = readFn;
        m_sourceGroup = sourceGroup;
        m_individualTableReaderFactorySupplier = individualTableReaderFactorySupplier;
        m_keepReadsOpen = keepReadsOpen;
        m_typeMapperExceptionParser =
            new TypeMapperExceptionParser(sourceGroup.size(), tableReadConfig.limitRowsForSpec());
    }

    @Override
    public DataTableSpec getOutputSpec() {
        return m_outputSpec;
    }

    @Override
    public TableSpecConfig<T> getTableSpecConfig() {
        return m_tableSpecConfig;
    }

    @SuppressWarnings("resource")
    @Override
    public void fillRowOutput(final RowOutput output, final ExecutionMonitor exec, final FileStoreFactory fsFactory)
        throws Exception {
        final BiFunction<I, FileStoreFactory, ? extends IndividualTableReader<V>> individualTableReaderFactory =
            m_individualTableReaderFactorySupplier.get();
        /* Workaround for unclear resource lifetime of resources referenced in data rows (AP-18002).
         * The problem: TableReads generally like to get closed as soon as possible in order to free resources.
         *              However, they may hand out data rows that use resources managed by the read,
         *              leading to a use-after-free bug.
         * The problem manifests e.g. as a filestore, managed by `NotInWorkflowFileStoreHandler`, being unavailable on
         * table output, since it was removed by the finalizer of a Buffer used in the table read.
         *
         * 1. A KnimeTableRead reads a very small table (e.g. one row) into its underlying Buffer and asynchronously
         *      queues the "batch" (list of data rows) to be written using the BufferedDataContainerDelegate.
         *      1a. Since the batch is tiny, it gets written out only when the buffer gets finalized.
         *      1b. Since the filestore handler created by the table read is a NotInWorkflowWriteFileStoreHandler, it
         *          will be cleaned up when the buffer gets finalized, since no one else seems to care about this
         *          handler (or the buffer).
         * 2. The read is closed.
         * 3. The system GC runs, finalizing the delegate’s Buffer (which has a higher probability to happen if the
         *      workflow does a lot of stuff that needs to be collected,… or a breakpoint is added before the code of
         *      the next item leading to a GC run due to elapsed time).
         *
         * 4. The rows are output and since these rows are associated with a filestore and this filestore is not in the
         *      workflow, it is tried to copy the filestore into the workflow directory.
         *
         * 5. The copy fails, since the filestore was already removed by the finalizer of the Buffer used in the
         *      `KnimeTableRead`.
         *
         * Immediate workaround:
         * Defer closing of reads from _marked_ table readers until the output is closed.
         */
        try (final MultiReadsCloser<AutoCloseable> openReads = new MultiReadsCloser<>(output)) {
            for (I item : m_sourceGroup) {
                exec.checkCanceled();
                final ExecutionMonitor progress = exec.createSubProgress(1.0 / m_sourceGroup.size());
                final IndividualTableReader<V> reader = individualTableReaderFactory.apply(item, fsFactory);
                // the opened resource will be closed by the MultiReadsCloser
                final Read<V> read = m_readFn.apply(item);
                // keep only our special read(s) open
                if (m_keepReadsOpen) {
                    openReads.add(read);
                }
                try {
                    reader.fillOutput(read, output, progress);
                } catch (TypeMapperException e) {
                    throw m_typeMapperExceptionParser.parse(e, item.toString());
                } finally {
                    if (!m_keepReadsOpen) {
                        // close all other kinds of reads
                        read.close();
                    }
                }
                progress.setProgress(1.0);
            }
        }
    }

    /**
     * Part of the workaround for AP-18002 (TableRead resources do not live long enough for output).
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    private static final class MultiReadsCloser<V extends AutoCloseable> implements AutoCloseable {

        private final ArrayDeque<V> m_openReads = new ArrayDeque<>();
        private RowOutput m_output;

        private MultiReadsCloser(final RowOutput output) {
            m_output = output;
        }

        @Override
        public void close() throws Exception {
            final var ex = new Exception();
            try {
                m_output.close();
            } catch (InterruptedException e) { // NOSONAR `e` is added to `ex` which is later thrown
                ex.addSuppressed(e);
            } finally {
                while (!m_openReads.isEmpty()) {
                    try {
                        m_openReads.removeLast().close();
                    } catch (IOException e) {
                        ex.addSuppressed(e);
                    }
                }
                if (ex.getSuppressed().length > 0) {
                    throw ex; // NOSONAR exception in question is caught and added to `ex` already
                }
            }
        }

        private void add(final V read) {
            m_openReads.addLast(read);
        }

    }

    @SuppressWarnings("resource")
    @Override
    public final PreviewRowIterator createPreviewIterator() {
        final BiFunction<I, FileStoreFactory, ? extends IndividualTableReader<V>> individualTableReaderFactory =
            m_individualTableReaderFactorySupplier.get();
        return new MultiTablePreviewRowIterator<>(m_sourceGroup.iterator(), (p, f) -> {
            final IndividualTableReader<V> reader = individualTableReaderFactory.apply(p, f);
            return new IndividualTablePreviewRowIterator<>(m_readFn.apply(p), reader::toRow);
        });
    }
}