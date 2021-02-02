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
 *   Jan 28, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import org.knime.filehandling.core.node.table.reader.randomaccess.AbstractRandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessibleDecorator;

/**
 * A {@link RandomAccessible} that verifies that columns determined to be empty are actually empty and filters them out.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class EmptyCheckingRandomAccessibleDecorator<V> extends AbstractRandomAccessible<V>
    implements RandomAccessibleDecorator<V> {

    private final int[] m_offsets;

    private final int[] m_emptyPositions;

    private final String[] m_emptyColumnNames;

    private final int m_numFiltered;

    private RandomAccessible<V> m_decoratee;

    EmptyCheckingRandomAccessibleDecorator(final int[] emptyColumnPositions, final String[] emptyColumnNames) {
        m_emptyPositions = emptyColumnPositions;
        m_emptyColumnNames = emptyColumnNames;
        final OptionalInt largestEmptyPosition = Arrays.stream(emptyColumnPositions)//
            .max();
        if (largestEmptyPosition.isPresent()) {
            m_offsets = calculateOffsets(m_emptyPositions, largestEmptyPosition.getAsInt());
        } else {
            // the offset for all positions is 0
            m_offsets = new int[]{0};
        }
        m_numFiltered = m_emptyPositions.length;
    }

    private static int[] calculateOffsets(final int[] emptyPositions, final int largestEmptyPosition) {
        final int maxPos = largestEmptyPosition + 1;
        final List<Integer> offsetList = new ArrayList<>();
        int cummulativeOffset = 0;
        for (int i = 0; i < maxPos; i++) {
            if (i == emptyPositions[cummulativeOffset]) {
                cummulativeOffset++;
            } else {
                offsetList.add(cummulativeOffset);
            }
        }
        offsetList.add(cummulativeOffset);
        return offsetList.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public void set(final RandomAccessible<V> decoratee) {
        checkEmpty(decoratee);
        m_decoratee = decoratee;
    }

    void checkEmpty(final RandomAccessible<?> randomAccessible) {
        for (int i = 0; i < m_emptyPositions.length; i++) {
            if (randomAccessible.get(m_emptyPositions[i]) != null) {
                throw new NotEmptyException(
                    String.format("The column '%s' was falsely considered to be empty and filtered. "
                        + "Disable the 'Skip empty columns' option "
                        + "or increase the number of rows used to calculate the spec.", m_emptyColumnNames[i]));
            }
        }
    }

    @Override
    public int size() {
        return m_decoratee.size() - m_numFiltered;
    }

    @Override
    public V get(final int idx) {
        return m_decoratee.get(map(idx));
    }

    private int map(final int idx) {
        if (idx < m_offsets.length) {
            return idx + m_offsets[idx];
        } else {
            // m_offsets contains at least one offset see calculateOffsets
            return idx + m_offsets[m_offsets.length - 1];
        }
    }

    static class NotEmptyException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public NotEmptyException(final String msg) {
            super(msg);
        }
    }

}
