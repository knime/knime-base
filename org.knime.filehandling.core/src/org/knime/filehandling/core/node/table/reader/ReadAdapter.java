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
 *   Jan 28, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.io.IOException;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.knime.core.data.RowKey;
import org.knime.core.data.convert.map.Source;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.util.IndexMapper;

/**
 * Serves as adapter between a {@link Read} and the mapping framework by representing a {@link Source}.</br>
 * It also extracts the row keys and fills in any columns that are missing in the source read.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> type used to identify data types
 * @param <V> type of tokens read by the reader
 * @noreference not meant to be referenced by clients
 */
public abstract class ReadAdapter<T, V> implements Source<T>, RandomAccessible<V>, Read<V> {

    private IndexMapper m_indexMapper;

    private Read<V> m_read;

    private RowKeyGenerator<V> m_keyGenerator;

    private RandomAccessible<V> m_current;

    private int m_size;

    /**
     * Constructor to be called by extending classes.
     */
    protected ReadAdapter() {
    }

    void setIndexMapper(final IndexMapper indexMapper) {
        final OptionalInt indexRangeEnd = CheckUtils.checkArgumentNotNull(indexMapper).getIndexRangeEnd();
        CheckUtils.checkArgument(indexRangeEnd.isPresent(),
            "Only index mappers with a limited range are supported by ReadAdapter.");
        m_indexMapper = indexMapper;
        // +1 since indices are zero based
        m_size = indexRangeEnd.getAsInt() + 1;
    }

    void setRead(final Read<V> read) {
        m_read = CheckUtils.checkArgumentNotNull(read);
    }

    void setRowKeyGenerator(final RowKeyGenerator<V> rowKeyGenerator) {
        m_keyGenerator = CheckUtils.checkArgumentNotNull(rowKeyGenerator);
    }

    @Override
    public OptionalLong getEstimatedSizeInBytes() {
        return m_read.getEstimatedSizeInBytes();
    }

    @Override
    public final V get(final int idx) {
        if (m_indexMapper.hasMapping(idx)) {
            return m_current.get(m_indexMapper.map(idx));
        } else {
            return null;
        }
    }

    /**
     * Returns the value identified by the provided {@link ReadAdapterParams}. When implementing your
     * CellValueProducers, call this method to access the values.
     *
     * @param params read parameters
     * @return the value identified by params
     */
    public final V get(final ReadAdapterParams<?> params) {
        return get(params.getIdx());
    }

    @Override
    public long readBytes() {
        if (m_read != null) {
            return m_read.readBytes();
        } else {
            return 0L;
        }
    }

    final RowKey getKey() {
        CheckUtils.checkState(m_current != null, "Either next hasn't been called, or we reached the end of the read.");
        return m_keyGenerator.createKey(m_current);
    }

    @Override
    public final int size() {
        return m_size;
    }

    /**
     * <b>NOTE:</b> The return value is actually this object which serves as proxy in order to avoid unnecessary object
     * creations.
     */
    @Override
    public final RandomAccessible<V> next() throws IOException {
        m_current = m_read.next();
        return m_current != null ? this : null;
    }

    @Override
    public final void close() throws IOException {
        m_read.close();
    }

    /**
     * Used to identify values in {@link ReadAdapter#get(ReadAdapterParams)}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param <A> the concrete ReadAdapter implementation (only necessary to satisfy the compiler)
     * @noreference not meant to be referenced by clients
     */
    public static final class ReadAdapterParams<A extends ReadAdapter<?, ?>> implements ProducerParameters<A> {

        private final int m_idx;

        ReadAdapterParams(final int idx) {
            m_idx = idx;
        }

        private int getIdx() {
            return m_idx;
        }

        @Override
        public String toString() {
            return Integer.toString(m_idx);
        }
    }

}
