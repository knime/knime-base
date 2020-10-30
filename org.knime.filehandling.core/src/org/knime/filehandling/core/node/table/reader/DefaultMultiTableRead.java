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
 *   Mar 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.util.IndividualTableReader;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.util.CheckedExceptionFunction;

/**
 * Default implementation of MultiTableRead.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <V> the type representing values
 */
final class DefaultMultiTableRead<V> implements MultiTableRead {

    private final DataTableSpec m_outputSpec;

    private final TableSpecConfig m_tableSpecConfig;

    private final List<Path> m_paths;

    private final CheckedExceptionFunction<Path, Read<V>, IOException> m_readFn;

    private final BiFunction<Path, FileStoreFactory, IndividualTableReader<V>> m_individualTableReaderFactory;

    /**
     * Constructor.
     *
     * @param paths the list of {@link Path Paths} to read from
     * @param readFn produces a {@link Read} from a {@link Path}
     * @param individualTableReaderFactory creates {@link IndividualTableReader IndividualTableReaders} from {@link Path
     *            Paths}
     * @param tableSpecConfig corresponding to this instance
     * @param outputSpec {@link DataTableSpec} of the output table
     */
    DefaultMultiTableRead(final List<Path> paths, final CheckedExceptionFunction<Path, Read<V>, IOException> readFn,
        final BiFunction<Path, FileStoreFactory, IndividualTableReader<V>> individualTableReaderFactory,
        final TableSpecConfig tableSpecConfig, final DataTableSpec outputSpec) {
        m_outputSpec = outputSpec;
        m_tableSpecConfig = tableSpecConfig;
        m_readFn = readFn;
        m_paths = paths;
        m_individualTableReaderFactory = individualTableReaderFactory;
    }

    @Override
    public DataTableSpec getOutputSpec() {
        return m_outputSpec;
    }

    @Override
    public TableSpecConfig getTableSpecConfig() {
        return m_tableSpecConfig;
    }

    @Override
    public void fillRowOutput(final RowOutput output, final ExecutionMonitor exec, final FileStoreFactory fsFactory)
        throws Exception {
        for (Path path : m_paths) {
            exec.checkCanceled();
            final ExecutionMonitor progress = exec.createSubProgress(1.0 / m_paths.size());
            final IndividualTableReader<V> reader = m_individualTableReaderFactory.apply(path, fsFactory);
            try (final Read<V> read = m_readFn.apply(path)) {
                reader.fillOutput(read, output, progress);
            }
            progress.setProgress(1.0);
        }
        output.close();
    }

    @Override
    public PreviewRowIterator createPreviewIterator() {
        return new MultiTablePreviewRowIterator(m_paths.iterator(), this::createIndividualTableIterator);
    }

    @SuppressWarnings("resource") // the read is closed by the IndividualTablePreviewRowIterator
    private PreviewRowIterator createIndividualTableIterator(final Path path, final FileStoreFactory fsFactory)
        throws IOException {
        final IndividualTableReader<V> reader = m_individualTableReaderFactory.apply(path, fsFactory);
        return new IndividualTablePreviewRowIterator<V>(m_readFn.apply(path), reader::toRow);
    }

}
