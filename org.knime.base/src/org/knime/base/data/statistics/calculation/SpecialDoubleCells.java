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
 *   29.04.2014 (Marcel): created
 */
package org.knime.base.data.statistics.calculation;

import org.knime.base.data.statistics.Statistic;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;

/**
 * Counts the number of special Double values: the NaN values, positive infinity and negative infinity.
 *
 * @author Marcel Hanser
 * @since 2.12
 * @deprecated see {@link org.knime.core.data.statistics}
 */
@Deprecated(since = "5.1")
public class SpecialDoubleCells extends Statistic {
    private int[] m_numberNaNValues;

    private int[] m_numberPositiveInfiniteValues;

    private int[] m_numberNegativeInfiniteValues;

    /**
     * @param columns the columns
     */
    public SpecialDoubleCells(final String... columns) {
        super(DoubleValue.class, columns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init(final DataTableSpec spec, final int size) {
        m_numberNaNValues = new int[size];
        m_numberPositiveInfiniteValues = new int[size];
        m_numberNegativeInfiniteValues = new int[size];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void consumeRow(final DataRow dataRow) {
        int index = 0;
        for (int i : getIndices()) {
            DataCell cell = dataRow.getCell(i);
            if (!cell.isMissing()) {
                double val = ((DoubleValue)cell).getDoubleValue();
                if (Double.isInfinite(val)) {
                    if (val > 0) {
                        m_numberPositiveInfiniteValues[index]++;
                    } else {
                        m_numberNegativeInfiniteValues[index]++;
                    }
                } else if (Double.isNaN(val)) {
                    m_numberNaNValues[index]++;
                }
            }
            index++;
        }
    }

    /**
     * @param column the columns
     * @return number of NaN values
     */
    public int getNumberNaNValues(final String column) {
        return m_numberNaNValues[assertIndexForColumn(column)];
    }

    /**
     * @param column the columns
     * @return number of positive infinite values
     */
    public int getNumberPositiveInfiniteValues(final String column) {
        return m_numberPositiveInfiniteValues[assertIndexForColumn(column)];
    }

    /**
     * @param column the columns
     * @return number of negative infinite values
     */
    public int getNumberNegativeInfiniteValues(final String column) {
        return m_numberNegativeInfiniteValues[assertIndexForColumn(column)];
    }
}
