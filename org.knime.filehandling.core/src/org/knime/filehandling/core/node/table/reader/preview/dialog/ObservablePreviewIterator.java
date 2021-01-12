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
 *   Aug 13, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog;

import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.filehandling.core.node.table.reader.PreviewIteratorException;
import org.knime.filehandling.core.node.table.reader.PreviewRowIterator;

/**
 * A {@link CloseableRowIterator} that allows to register {@link ChangeListener listeners} that are called if the
 * underlying {@link PreviewRowIterator} throws a {@link PreviewIteratorException}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ObservablePreviewIterator extends CloseableRowIterator {

    private final CopyOnWriteArraySet<ChangeListener> m_errorListeners = new CopyOnWriteArraySet<>();

    private final ChangeEvent m_changeEvent = new ChangeEvent(this);

    private final PreviewRowIterator m_iterator;

    private long m_currentRowIdx = -1;

    private String m_errorMsg = null;

    ObservablePreviewIterator(final PreviewRowIterator iterator) {
        m_iterator = iterator;
    }

    void addErrorListener(final ChangeListener errorListener) {
        m_errorListeners.add(errorListener);//NOSONAR a small price to pay for thread-safety
    }

    private void notifyErrorListeners() {
        for (ChangeListener listener : m_errorListeners) {
            listener.stateChanged(m_changeEvent);
        }
    }

    public long getCurrentRowIdx() {
        return m_currentRowIdx;
    }

    public String getErrorMsg() {
        return m_errorMsg;
    }

    @Override
    public void close() {
        m_errorListeners.clear();
        m_iterator.close();
    }

    @Override
    public boolean hasNext() {
        try {
            return m_errorMsg == null && m_iterator.hasNext();
        } catch (PreviewIteratorException pie) { // NOSONAR, the exception is handled appropriately
            m_errorMsg = pie.getMessage();
            notifyErrorListeners();
            return false;
        }
    }

    @Override
    public DataRow next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        m_currentRowIdx++;
        try {
            return m_iterator.next();
        } catch (PreviewIteratorException pie) { // NOSONAR, the exception is handled appropriately
            m_errorMsg = pie.getMessage();
            notifyErrorListeners();
            final NoSuchElementException ex = new NoSuchElementException(pie.getMessage());
            ex.initCause(pie);
            throw ex;
        }
    }

}
