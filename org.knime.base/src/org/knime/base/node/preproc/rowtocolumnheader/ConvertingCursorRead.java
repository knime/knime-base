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
 *   Sep 13, 2022 (leonard.woerteler): created
 */
package org.knime.base.node.preproc.rowtocolumnheader;

import java.io.IOException;
import java.util.OptionalLong;
import java.util.function.Function;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.streamable.RowInput;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;

/**
 * Implementation of {@link Read} for {@link RowCursor table row cursors} which converts the data values read from the
 * cursor to an output type.
 *
 * @param <T> type to convert the data values to
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
public class ConvertingCursorRead<T> implements Read<T> {
    /** Converter function for the data values. */
    private final Function<DataValue, T> m_mapper;
    /** Number of rows the cursor will return, if known. */
    private final OptionalLong m_size;

    /** Row cursor, {@code null} if this read has been closed. */
    private RowCursor m_cursor;
    /** Number of rows processed until now. */
    private long m_processed;

    /**
     * Creates a new instance for the given cursor.
     *
     * @param cursor row cursor
     * @param size number of rows the given cursor will return if known, {@code -1L} otherwise
     * @param mapper function for converting {@link DataValue}s to the output type
     */
    public ConvertingCursorRead(final RowCursor cursor, final long size, final Function<DataValue, T> mapper) {
        m_cursor = cursor;
        m_size = size < 0 ? OptionalLong.empty() : OptionalLong.of(size);
        m_mapper = mapper;
    }

    /**
     * Creates a new instance for the given row input.
     *
     * @param input row input
     * @param mapper function for converting {@link DataValue}s to the output type
     */
    public ConvertingCursorRead(final RowInput input, final Function<DataValue, T> mapper) {
        m_cursor = new RowInputAdapter(input);
        m_size = OptionalLong.empty();
        m_mapper = mapper;
    }

    @Override
    public WrappedRow next() throws IOException {
        if (m_cursor == null) {
            return null;
        }

        final var row = m_cursor.forward();
        if (row == null) {
            close();
            return null;
        }

        m_processed++;
        return new WrappedRow(row);
    }

    @Override
    public OptionalLong getMaxProgress() {
        return m_size;
    }

    @Override
    public long getProgress() {
        return m_processed;
    }

    @Override
    public void close() throws IOException {
        if (m_cursor != null) {
            try {
                m_cursor.close();
            } finally {
                m_cursor = null;
            }
        }
    }

    /**
     * Adapter for {@link RowRead} that exposes the row's key.
     */
    public final class WrappedRow implements RandomAccessible<T> {
        /**  */
        private final RowRead m_rowRead;

        private WrappedRow(final RowRead rowRead) {
            m_rowRead = rowRead;
        }

        @Override
        public int size() {
            return m_rowRead.getNumColumns();
        }

        @Override
        public T get(final int idx) {
            return m_rowRead.isMissing(idx) ? null : m_mapper.apply(m_rowRead.getValue(idx));
        }

        /**
         * @return the current row's key
         */
        public RowKeyValue getRowKey() {
            return m_rowRead.getRowKey();
        }
    }

    private static final class RowInputAdapter implements RowCursor {

        /** Wrapped row cursor, or {@code null} if it was closed. */
        private RowInput m_input;

        /** Current row. */
        private DataRow m_current;

        /** Next row to be returned, or {@code null} if it wasn't retrieved yet. */
        private DataRow m_next;

        /**
         * Single instance of {@link RowRead} that is reused for all rows.
         * This is safe here because we never store a reference to it between iterations.
         */
        private final RowRead m_rowRead;

        /**
         * Creates an adapter from the given row input to the {@link RowCursor} interface.
         *
         * @param input row input to adapt
         */
        private RowInputAdapter(final RowInput input) {
            m_input = input;
            m_rowRead = RowRead.suppliedBy(() -> m_current, input.getDataTableSpec().getNumColumns());
        }

        @Override
        public int getNumColumns() {
            return m_input.getDataTableSpec().getNumColumns();
        }

        @Override
        public boolean canForward() {
            if (m_next != null) {
                return true;
            }
            if (m_input == null) {
                return false;
            }
            try {
                m_next = m_input.poll();
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            if (m_next == null) {
                close();
                return false;
            }
            return true;
        }

        @Override
        public RowRead forward() {
            if (!canForward()) {
                m_current = null;
                return null;
            }

            m_current = m_next;
            m_next = null;
            return m_rowRead;
        }

        @Override
        public void close() {
            if (m_input != null) {
                try {
                    m_input.close();
                } finally {
                    m_input = null;
                }
            }
        }
    }
}
