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
package org.knime.filehandling.core.node.table.reader.spec;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.GenericRead;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;

/**
 * A read that extracts the row containing the column headers from the provided {@link Read source}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <V> the value type
 */
public class GenericDefaultExtractColumnHeaderRead<I, V> implements GenericExtractColumnHeaderRead<I, V> {

    /** The underlying read. */
    protected final GenericRead<I, V> m_read;
    /** The column index containing the header. */
    protected final long m_columnHeaderIdx;
    /** The number of rows to read. */
    protected final long m_numRowsToRead;
    /** The {@link RandomAccessible} holding the column header. */
    protected Optional<RandomAccessible<V>> m_columnHeader;
    /** The number of rows returned, i.e., the number of next calls on {@link #m_read}. */
    private long m_numRowsReturned = 0;
    /** The number of rows to skip. */
    protected long m_numRowsToSkip;

    /**
     * Constructor.
     * @param source {@link GenericRead} to read from
     * @param config TableReadConfig to use
     */
    public GenericDefaultExtractColumnHeaderRead(final GenericRead<I, V> source, final TableReadConfig<?> config) {
        // get the column index
        long colHeaderIdx;
        if (config.useColumnHeaderIdx()) {
            colHeaderIdx = config.getColumnHeaderIdx();
            CheckUtils.checkArgument(colHeaderIdx >= 0, "The column header index cannot be negative.");
        } else {
            colHeaderIdx = -1;
            m_columnHeader = Optional.empty();
        }

        // get the number of rows to skip
        if (config.skipRows()) {
            m_numRowsToSkip = config.getNumRowsToSkip();
            CheckUtils.checkArgument(m_numRowsToSkip >= 0, "The number of rows to skip cannot be negative.");
        } else {
            m_numRowsToSkip = 0;
        }

        GenericRead<I, V> read = source;
        /* do initial skip if required, we basically distinguish two cases
         * 1. The number of rows to skip is larger than the column header idx where we find the column head.
         *    In this case we can only skip all rows before the column header and have to take care of the remaining
         *    rows that need to be skipped after we read the column header
         * 2. The number of rows to skip is less than or equal to the column header index. This means we can safely skip
         *    all rows
         *
         * In both cases we have to account for the number of rows that we skipped and update the position where we can
         * find the column header, i.e., the columnHeaderIdx
         */
        if (m_numRowsToSkip > colHeaderIdx) {
            if (colHeaderIdx == -1) {
                read = ReadUtils.skip(read, m_numRowsToSkip);
                m_numRowsToSkip = 0;
            } else {
                read = ReadUtils.skip(read, colHeaderIdx);
                m_numRowsToSkip -= colHeaderIdx;
                colHeaderIdx = 0;
            }
        } else if (m_numRowsToSkip > 0) {
            read = ReadUtils.skip(read, m_numRowsToSkip);
            colHeaderIdx -= m_numRowsToSkip;
            m_numRowsToSkip = 0;
        }

        // restrict the number of rows to be read
        if (config.limitRowsForSpec()) {
            m_numRowsToRead = config.getMaxRowsForSpec();
            CheckUtils.checkArgument(m_numRowsToRead >= 0, "The number of rows to scan cannot be negative.");
        } else {
            m_numRowsToRead = -1;
        }
        m_read = read;
        m_columnHeaderIdx = colHeaderIdx;
    }

    @Override
    public RandomAccessible<V> next() throws IOException {
        /* If we returned already the number of rows to be read we don't move m_read to next, but return null instead.
        *  This ensure that we are still able to find the column header at the defined index in case it's position is
        *  greater than or equal to m_numRowsToRead
        */
        if (m_numRowsToRead != -1 && m_numRowsReturned == m_numRowsToRead) {
            return null;
        }
        if (m_numRowsReturned == m_columnHeaderIdx) {
            m_columnHeader = Optional.ofNullable(m_read.next()).map(RandomAccessible::copy);
            /* Can only happen if the number of rows to skip was initially greater than the column header index.
             * Note that the Constructor ensure that m_columnHeaderIdx == 0 in this case.
             */
            while (m_numRowsToSkip-- > 0) {
                m_read.next();
            }
        }
        ++m_numRowsReturned;
        return m_read.next();
    }

    @Override
    public OptionalLong getMaxProgress() {
        return m_read.getMaxProgress();
    }

    @Override
    public long getProgress() {
        return m_read.getProgress();
    }

    @Override
    public void close() throws IOException {
        m_read.close();
    }

    @Override
    public Optional<RandomAccessible<V>> getColumnHeaders() throws IOException {
        // Check if the column header row comes after the number of rows to read and if so read it
        if (m_columnHeaderIdx >= m_numRowsReturned) {
            extractHeader();
        }
        return m_columnHeader;
    }

    private void extractHeader() throws IOException {
        // consume all rows (if not null) preceding the column header row
        while (m_numRowsReturned < m_columnHeaderIdx && m_read.next() != null) {
            ++m_numRowsReturned;
        }
        m_columnHeader = Optional.ofNullable(m_read.next()).map(RandomAccessible::copy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<I> getItem() {
        return m_read.getItem();
    }
}