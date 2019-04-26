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
 *   Apr 25, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain;

import org.knime.core.node.util.CheckUtils;

/**
 * Helper class that stores values in a matrix format and allows getting and setting of these values.
 * It also allows to assign roles to the row and column dimensions.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class Matrix {

    private final double[] m_values;

    private final int m_numRows;

    private final int m_numColumns;

    private final String m_rowRole;

    private final String m_columnRole;

    /**
     * Constructs a matrix where the roles of the row and column dimension are just row and column.
     * The product of <b>numRows</b> and <b>numColumns</b> must not exceed Integer.MAX_VALUE.
     * @param numRows must be greater than zero
     * @param numColumns must be greater than zero
     */
    Matrix(final int numRows, final int numColumns) {
        this(numRows, numColumns, "row", "column");
    }

    /**
     * Constructs a matrix with the provided roles for rows and columns.
     * The product of <b>numRows</b> and <b>numColumns</b> must not exceed Integer.MAX_VALUE.
     *
     * @param numRows must be greater than zero
     * @param numColumns must be greater than zero
     * @param rowRole role of the row dimension (used for error reporting)
     * @param columnRole role of the column dimension (used for error reporting)
     */
    Matrix(final int numRows, final int numColumns, final String rowRole, final String columnRole) {
        CheckUtils.checkNotNull(rowRole, "The rowRole must not be null.");
        CheckUtils.checkNotNull(columnRole, "The columnRole must not be null.");
        CheckUtils.checkArgument(numRows > 0, "The number of rows must be greater than 0 but was %s.", numRows);
        CheckUtils.checkArgument(numColumns > 0, "The number of columns must be greater than 0 but was $s.",
            numColumns);
        CheckUtils.checkArgument(((long)numRows) * numColumns <= Integer.MAX_VALUE,
            "The product of the number of rows and columns must not exceed Integer.MAX_VALUE.");
        m_numRows = numRows;
        m_numColumns = numColumns;
        m_rowRole = rowRole;
        m_columnRole = columnRole;
        m_values = new double[numRows * numColumns];
    }

    public int getNumRows() {
        return m_numRows;
    }

    public int getNumCols() {
        return m_numColumns;
    }

    public double get(final int row, final int column) {
        final int flatIdx = flatIdx(row, column);
        return m_values[flatIdx];
    }

    public void set(final int row, final int column, final double value) {
        final int flatIdx = flatIdx(row, column);
        m_values[flatIdx] = value;
    }

    private int flatIdx(final int row, final int column) {
        checkIndices(row, column);
        return uncheckedFlatIdx(row, column);
    }

    private int uncheckedFlatIdx(final int row, final int column) {
        return row * m_numColumns + column;
    }

    private void checkIndices(final int row, final int column) {
        checkIndex(row, m_numRows, m_rowRole);
        checkIndex(column, m_numColumns, m_columnRole);
    }

    private static void checkIndex(final int idx, final int maxIdx, final String idxName) {
        if (idx < 0) {
            throw new IndexOutOfBoundsException("The " + idxName + " index is negative. This is not supported.");
        }
        if (idx >= maxIdx) {
            throw new IndexOutOfBoundsException(
                "The " + idxName + " index (" + idx + ") exceeds the maximal allowed idx (" + maxIdx + ").");
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("NumRows: ").append(m_numRows)
        .append("NumColumns: ").append(m_numColumns)
        .append("Row role: ").append(m_rowRole)
        .append(" Column role: ").append(m_columnRole)
        .append(" Values:\n[");
        for (int i = 0; i < m_numRows; i++) {
            for (int j = 0; j < m_numColumns; j++) {
                sb.append(get(i, j));
                if (j < m_numColumns - 1) {
                    sb.append(", ");
                }
            }
            if (i < m_numRows - 1) {
                sb.append("\n");
            } else {
                sb.append("]");
            }
        }
        return sb.toString();

    }
}
