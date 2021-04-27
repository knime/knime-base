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
 *   Apr 9, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.ftrf.adapter.cursor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.IntStream;

import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.ReadAccessRow;
import org.knime.core.table.cursor.Cursor;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <V> the type of values used
 */
public final class RowReadAccessCursorAdapter<V> implements Cursor<ReadAccessRow> {

    private final Read<?, V> m_read;

    private final RowReadAccessAdapter m_rowAccess;

    private final ReadAccessAdapterFactory<V> m_accessAdapterFactory;

    public RowReadAccessCursorAdapter(final Read<?, V> read, final ReadAccessAdapterFactory<V> accessAdapterFactory) {
        m_read = read;
        m_rowAccess = new RowReadAccessAdapter();
        m_accessAdapterFactory = accessAdapterFactory;
    }

    @Override
    public ReadAccessRow forward() {
        final RandomAccessible<V> next = readNext();
        if (next == null) {
            return null;
        } else {
            m_rowAccess.setDelegate(next);
        }
        return m_rowAccess;
    }

    private RandomAccessible<V> readNext() {
        RandomAccessible<V> next;
        try {
            next = m_read.next();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        return next;
    }

    @Override
    public void close() {
        try {
            m_read.close();
        } catch (IOException ex) {
            // TODO discuss what the correct behavior should be
            throw new IllegalStateException(ex);
        }
    }

    private final class RowReadAccessAdapter implements ReadAccessRow {

        private final ArrayList<ReadAccessAdapter<V>> m_accessAdapters = new ArrayList<>();

        private RandomAccessible<V> m_delegate;

        void setDelegate(final RandomAccessible<V> delegate) {
            m_delegate = delegate;
        }

        @Override
        public int getNumColumns() {
            return m_delegate.size();
        }

        @Override
        public <A extends ReadAccess> A getAccess(final int index) {
            final V value = m_delegate.get(index);
            ensureSize(index);
            ReadAccessAdapter<V> adapter = m_accessAdapters.get(index);
            if (adapter == null) {
                adapter = m_accessAdapterFactory.createAdapter(value);
                m_accessAdapters.set(index, adapter);
            }
            adapter.setValue(value);
            @SuppressWarnings("unchecked")
            final A cast = (A)adapter;
            return cast;
        }

        private void ensureSize(final int index) {
            if (index > m_accessAdapters.size()) {
                IntStream.range(0, index - m_accessAdapters.size()).forEach(i -> m_accessAdapters.add(null));
            }
        }

        @Override
        public boolean isMissing(final int index) {
            return m_delegate.get(index) == null;
        }

    }

}
