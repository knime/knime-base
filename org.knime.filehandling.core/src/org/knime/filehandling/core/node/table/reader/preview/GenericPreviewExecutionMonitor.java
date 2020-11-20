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
 *   Nov 15, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader.preview;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.filehandling.core.node.table.reader.preview.dialog.PreviewDataTable;

/**
 * // TODO AP-14295: we need sth like our own TableReadExecutionMonitor (from that PreviewExecutionMonitor inherits)
 *
 * The execution monitor used when creating a {@link PreviewDataTable}.
 *
 * Note that no real sub progress can be created at the moment (see TODO above).
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 */
public class GenericPreviewExecutionMonitor<I> extends ExecutionMonitor {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GenericPreviewExecutionMonitor.class);
    private final CopyOnWriteArraySet<ChangeListener> m_listeners = new CopyOnWriteArraySet<>();
    private long m_specGuessingErrorRow;
    private String m_specGuessingErrorMsg;
    private long m_iteratorErrorRow;
    private String m_iteratorErrorMsg;
    private boolean m_isSizeAssessable;
    private Optional<I> m_currentItem = Optional.empty();
    private int m_numItemsToRead;
    private AtomicInteger m_currentlyReadingItemIdx = new AtomicInteger(0);
    private AtomicLong m_numRowsIteratorTotalRead = new AtomicLong(0);
    private AtomicLong m_numRowsSpecGuessingTotalRead = new AtomicLong(0);
    private AtomicLong m_numRowsSpecGuessingCurrentRead = new AtomicLong(0);
    private boolean m_startNextReadProgress = true;

    /**
     *
     */
    public GenericPreviewExecutionMonitor() {
        super();
    }

    /**
     * @param progress
     */
    public GenericPreviewExecutionMonitor(final NodeProgressMonitor progress) {
        super(progress);
    }

    /**
     * @return the row in which the error occurred
     */
    public long getSpecGuessingErrorRow() {
        return m_specGuessingErrorRow;
    }

    /**
     * @return the error message
     */
    public String getSpecGuessingErrorMsg() {
        return m_specGuessingErrorMsg;
    }

    /**
     * @param row the row in which the error occurred
     * @param msg the error message
     */
    public void setSpecGuessingError(final long row, final String msg) {
        m_specGuessingErrorRow = m_numRowsSpecGuessingTotalRead.get() + row;
        m_specGuessingErrorMsg = msg;
        fireErrorOccuredEvent();
    }

    /**
     * @param msg the error message
     */
    public void setSpecGuessingError(final String msg) {
        setSpecGuessingError(-1, msg);
    }

    /**
     * Returns {@code true} if an error occurred during spec guessing.
     *
     * @return {@code true} if an error occurred during spec guessing
     */
    public boolean isSpecGuessingErrorOccurred() {
        return m_specGuessingErrorMsg != null;
    }

    /**
     * @return the row in which the error occurred
     */
    public long getIteratorErrorRow() {
        return m_iteratorErrorRow;
    }

    /**
     * @return the error message
     */
    public String getIteratorErrorMsg() {
        return m_iteratorErrorMsg;
    }

    /**
     * @param msg the error message
     */
    public void setIteratorErrorRow(final String msg) {
        m_iteratorErrorRow = m_numRowsIteratorTotalRead.get();
        m_iteratorErrorMsg = msg;
        fireErrorOccuredEvent();
    }

    /**
     * Returns {@code true} if an error occurred in a {@link PreviewDataTable} row iterator. Note that if {@code false}
     * is returned it is not guaranteed that all data in the table is valid. It could be that no row iterator reached
     * the invalid data yet.
     *
     * @return {@code true} if an error occurred in a {@link PreviewDataTable} row iterator
     */
    public boolean isIteratorErrorOccurred() {
        return m_iteratorErrorMsg != null;
    }

    /**
     * Register a listener to be notified when an error occurs.
     *
     * @param listener the {@link ChangeListener} being notified when an error occurs.
     */
    public void addChangeListener(final ChangeListener listener) {
        m_listeners.add(listener);//NOSONAR a small price to pay for thread safety
    }

    /**
     * Clears the list of change listeners.
     *
     * @see #addChangeListener(ChangeListener)
     */
    public void removeAllChangeListeners() {
        m_listeners.clear();
    }

    private void fireErrorOccuredEvent() {
        final ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener l : m_listeners) {
            try {
                l.stateChanged(event);
            } catch (Exception t) {
                LOGGER.error("Exception while notifying listeners", t);
            }
        }
    }

    /**
     * @return {@code true} if the file size is assessable
     */
    public boolean isSizeAssessable() {
        return m_isSizeAssessable;
    }

    /**
     * @param isSizeAssessable if the file size is assessable
     */
    public void setSizeAssessable(final boolean isSizeAssessable) {
        m_isSizeAssessable = isSizeAssessable;
    }

    /**
     * @return the current item
     */
    public Optional<I> getCurrenttem() {
        return m_currentItem;
    }

    /**
     * @param currentItem the current item to set
     */
    public void setCurrentItem(final Optional<I> currentItem) {
        m_currentItem = currentItem;
    }

    /**
     * @return the numPathsToRead
     */
    public int getNumItemsToRead() {
        return m_numItemsToRead;
    }

    /**
     * @param numItemsToRead the number of items to read to set
     */
    public void setNumItemsToRead(final int numItemsToRead) {
        m_numItemsToRead = numItemsToRead;
    }

    /**
     * @return the currently reading item index
     */
    public int getCurrentlyReadingItemIdx() {
        return m_currentlyReadingItemIdx.get();
    }

    /**
     */
    public void incrementCurrentlyReadingItemIdx() {
        m_currentlyReadingItemIdx.incrementAndGet();
        m_startNextReadProgress = true;
    }

    /**
     * Sets the progress and automatically calculates the number of rows read across multiple files.
     *
     * @see NodeProgressMonitor#setProgress(double)
     * @param progress the progress values to set in the monitor
     * @param row the current row of the current file that is read
     */
    public void setProgress(final double progress, final long row) {
        if (m_startNextReadProgress) {
            m_numRowsSpecGuessingTotalRead.addAndGet(m_numRowsSpecGuessingCurrentRead.get());
            m_numRowsSpecGuessingCurrentRead.set(0);
            m_startNextReadProgress = false;
        }
        m_numRowsSpecGuessingCurrentRead.set(row);
        setProgress(progress, () -> "Analyzed "
            + (m_numRowsSpecGuessingTotalRead.get() + m_numRowsSpecGuessingCurrentRead.get()) + " rows");
    }

    /**
     * Increment the total rows that have been read by row iterators.
     *
     * @return the total rows read after incrementing
     */
    public long incrementRowsIteratorTotalRead() {
        return m_numRowsIteratorTotalRead.incrementAndGet();
    }

    /**
     * This returns the same {@link ExecutionMonitor}, so no real sub progress will be created. </br>
     * </br>
     * {@inheritDoc}
     */
    @Override
    public ExecutionMonitor createSubProgress(final double maxProg) {
        return this;
    }

}