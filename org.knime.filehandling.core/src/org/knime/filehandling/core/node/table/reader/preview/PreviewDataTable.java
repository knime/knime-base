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

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import javax.swing.event.ChangeEvent;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.CloseableTable;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.node.table.reader.PreviewRowIterator;

/**
 * A data table holding a preview of potentially multiple files to read in. If an error occurs, a
 * {@link PreviewExecutionMonitor} is informed instead of throwing an exception. </br>
 * </br>
 * Note that {@link #close()} should be called in order to properly clean up this table.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class PreviewDataTable implements CloseableTable {

    static final NodeLogger LOGGER = NodeLogger.getLogger(PreviewDataTable.class);

    private final DataTableSpec m_spec;

    private final Supplier<PreviewRowIterator> m_iteratorFn;

    private final CopyOnWriteArrayList<CloseableRowIterator> m_iterators = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<PreviewIterationErrorListener> m_errorListeners = new CopyOnWriteArrayList<>();

    /**
     * Listener interface for errors during preview iteration.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public interface PreviewIterationErrorListener {

        /**
         * Called whenever an error is encountered during preview iteration.
         *
         * @param row the index of the row that caused the error
         * @param errorMsg the error message
         */
        void errorEncountered(final long row, final String errorMsg);
    }

    /**
     * Constructor.
     *
     * @param iteratorSupplier supplies the {@link PreviewRowIterator}
     * @param spec the {@link DataTableSpec} of the table
     */
    public PreviewDataTable(final Supplier<PreviewRowIterator> iteratorSupplier, final DataTableSpec spec) {
        m_iteratorFn = iteratorSupplier;
        m_spec = spec;
    }

    /**
     * Adds a {@link PreviewIterationErrorListener} to the list of error listeners.
     *
     * @param listener to add
     */
    public void addErrorListener(final PreviewIterationErrorListener listener) {
        if (!m_errorListeners.contains(listener)) {//NOSONAR
            m_errorListeners.add(listener);//NOSONAR a small price to pay for thread-safety
        }
    }

    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    @Override
    public ObservablePreviewIterator iterator() {
        final ObservablePreviewIterator iterator = new ObservablePreviewIterator(m_iteratorFn.get());
        iterator.addErrorListener(this::handleIteratorError);
        m_iterators.add(iterator); //NOSONAR that's the prize we pay to avoid concurrency issues
        return iterator;
    }

    private void handleIteratorError(final ChangeEvent errorEvent) {
        @SuppressWarnings("resource") // the iterator will be closed by the #close()
        final ObservablePreviewIterator iterator = (ObservablePreviewIterator)errorEvent.getSource();
        final long errorRow = iterator.getCurrentRowIdx();
        final String errorMsg = iterator.getErrorMsg();
        for (PreviewIterationErrorListener listener : m_errorListeners) {
            listener.errorEncountered(errorRow, errorMsg);
        }
    }


    /**
     * Dispose all iterators and close their underlying sources.
     */
    @Override
    public void close() {
        for (final CloseableRowIterator iterator : m_iterators) {
            iterator.close();
        }
        m_iterators.clear();
        m_errorListeners.clear();
    }

}
