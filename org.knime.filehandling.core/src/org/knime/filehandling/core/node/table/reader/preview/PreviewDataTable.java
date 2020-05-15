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
 *   May 14, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.util.IndividualTableReader;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;

/**
 * A data table holding a preview of potentially multiple files to read in. If an error occurs, a
 * {@link PreviewExecutionMonitor} is informed instead of throwing an exception. </br>
 * </br>
 * Note that {@link #dispose()} should be called in order to properly clean up this table.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of the {@link ReaderSpecificConfig}
 * @param <V> the type of tokens the reader produces
 */
public final class PreviewDataTable<C extends ReaderSpecificConfig<C>, V> implements DataTable {

    private final List<Path> m_paths;

    private final MultiTableReadConfig<C> m_config;

    private final TableReader<C, ?, V> m_tableReader;

    private final MultiTableRead<V> m_multiTableRead;

    private final PreviewExecutionMonitor m_execMonitor;

    private final CopyOnWriteArrayList<MultiTablePreviewRowIterator> m_iterators = new CopyOnWriteArrayList<>();

    /**
     * Constructor.
     *
     * @param paths the paths to read from
     * @param config the config for reading
     * @param tableReader the table reader used for reading
     * @param multiTableRead the multi table read
     * @param execMonitor
     */
    public PreviewDataTable(final List<Path> paths, final MultiTableReadConfig<C> config,
        final TableReader<C, ?, V> tableReader, final MultiTableRead<V> multiTableRead,
        final PreviewExecutionMonitor execMonitor) {
        CheckUtils.checkArgumentNotNull(paths, "The paths must not be null.");
        CheckUtils.checkArgumentNotNull(config, "The config must not be null.");
        CheckUtils.checkArgumentNotNull(tableReader, "The table reader must not be null.");
        CheckUtils.checkArgumentNotNull(multiTableRead, "The multi table read must not be null.");
        CheckUtils.checkArgumentNotNull(execMonitor, "The preview execution monitor must not be null.");
        m_paths = paths;
        m_config = config;
        m_multiTableRead = multiTableRead;
        m_execMonitor = execMonitor;
        m_tableReader = tableReader;
    }

    @Override
    public DataTableSpec getDataTableSpec() {
        return m_multiTableRead.getOutputSpec();
    }

    @Override
    public RowIterator iterator() {
        final MultiTablePreviewRowIterator multiTablePreviewRowIterator = new MultiTablePreviewRowIterator();
        m_iterators.add(multiTablePreviewRowIterator);
        return multiTablePreviewRowIterator;
    }

    /**
     * @return the execution monitor
     */
    public PreviewExecutionMonitor getExecutionMonitor() {
        return m_execMonitor;
    }

    /**
     * Dispose all iterators and close their underlying sources.
     */
    public void dispose() {
        for (final MultiTablePreviewRowIterator iterator : m_iterators) {
            iterator.dispose();
        }
        m_iterators.clear();
    }

    private final class MultiTablePreviewRowIterator extends RowIterator {

        private int m_pathIdx = 0;

        private IndividualTablePreviewRowIterator m_currentIterator;

        @Override
        public boolean hasNext() {
            if (m_execMonitor.isIteratorErrorOccurred()) {
                return false;
            }
            assignCurrentIterator();
            return m_currentIterator != null && m_currentIterator.hasNext();
        }

        private void assignCurrentIterator() {
            // if no iterator is yet assigned or the current iterator does not have further elements,
            // take the next iterator if possible and check if it #hasNext
            if ((m_currentIterator == null || !m_currentIterator.hasNext()) && m_pathIdx < m_paths.size()) {
                if (m_currentIterator != null) {
                    m_currentIterator.dispose();
                }
                final Path path = m_paths.get(m_pathIdx++);
                final IndividualTableReader<V> individualTableReader = m_multiTableRead.createIndividualTableReader(
                    path, m_config.getTableReadConfig(), FileStoreFactory.createNotInWorkflowFileStoreFactory());
                try {
                    m_currentIterator = new IndividualTablePreviewRowIterator(path, individualTableReader);
                } catch (IOException e) {
                    m_execMonitor
                        .setSpecGuessingError("Error during reading of '" + path.toString() + "': " + e.getMessage());
                }
                // the now selected iterator may have an empty source, so check again
                assignCurrentIterator();
            }
        }

        @Override
        public DataRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return m_currentIterator.next();
        }

        private void dispose() {
            if (m_currentIterator!=null) {
                m_currentIterator.dispose();
            }
        }

    }

    private final class IndividualTablePreviewRowIterator extends RowIterator {

        private Read<V> m_read;

        DataRow m_next;

        private IndividualTableReader<V> m_individualTableReader;

        private IndividualTablePreviewRowIterator(final Path path, final IndividualTableReader<V> individualTableReader)
            throws IOException {
            m_individualTableReader = individualTableReader;
            m_read = ReadUtils.decorateForReading(m_tableReader.read(path, m_config.getTableReadConfig()),
                m_config.getTableReadConfig());
        }

        @Override
        public DataRow next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            final DataRow next = m_next;
            m_next = null;
            return next;
        }

        @Override

        public boolean hasNext() {
            if (m_execMonitor.isIteratorErrorOccurred()) {
                return false;
            }
            if (m_next != null) {
                return true;
            }
            try {
                final RandomAccessible<V> next = m_read.next();
                m_next = next == null ? null : m_individualTableReader.toRow(next);
            } catch (Exception e) {
                m_execMonitor.setIteratorErrorRow(e.getMessage());
                return false;
            }
            if (m_next != null) {
                m_execMonitor.incrementRowsIteratorTotalRead();
                return true;
            }
            return false;
        }

        private void dispose() {
            try {
                m_read.close();
            } catch (IOException ex) {
                // then don't close it
            }
        }
    }

}
