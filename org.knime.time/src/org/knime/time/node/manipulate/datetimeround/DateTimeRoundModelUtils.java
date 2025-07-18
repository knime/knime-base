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
 *   Dec 3, 2024 (Tobias Kampmann): created
 */
package org.knime.time.node.manipulate.datetimeround;

import java.time.DateTimeException;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.function.UnaryOperator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.message.MessageBuilder;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.time.util.TemporalCellUtils;

/**
 *
 * @author Tobias Kampmann
 */
@SuppressWarnings("restriction")
final class DateTimeRoundModelUtils {

    private DateTimeRoundModelUtils() {
        // utility class
    }

    static class RoundCellFactory extends SingleCellFactory {

        private final int m_targetColumnIndex;

        private final MessageBuilder m_messageBuilder;

        private final UnaryOperator<Temporal> m_roundingOperator;

        /**
         * @param newColSpec new column spec
         * @param targetColumnIndex index of the column to round
         * @param roundingOperator the rounding operator to apply
         * @param messageBuilder the message builder to collect issues called from NodeModel context:
         *            "createMessageBuilder()"
         */
        public RoundCellFactory(final DataColumnSpec newColSpec, final int targetColumnIndex,
            final UnaryOperator<Temporal> roundingOperator, final MessageBuilder messageBuilder) {

            super(newColSpec);
            this.m_targetColumnIndex = targetColumnIndex;

            this.m_roundingOperator = roundingOperator;

            this.m_messageBuilder = messageBuilder;
        }

        @Override
        public DataCell getCell(final DataRow row, final long rowIndex) {
            final DataCell cell = row.getCell(m_targetColumnIndex);
            if (cell.isMissing()) {
                return cell;
            }

            try {
                return TemporalCellUtils
                    .createTemporalDataCell(m_roundingOperator.apply(TemporalCellUtils.getTemporalFromCell(cell)));
            } catch (IllegalArgumentException | DateTimeException | ArithmeticException e) { // NOSONAR - this is logging the error message
                m_messageBuilder.addRowIssue(0, m_targetColumnIndex, rowIndex, e.getMessage());
                return new MissingCell(e.getMessage());
            }
        }

    }

    /**
     * Get the compatible columns from the DataTableSpec by checking the value classes.
     *
     * @param spec The DataTable spec
     * @param valueClasses The value classes to check for compatibility
     * @return A list of compatible columns names
     */
    static String[] getSelectedColumns(final DataTableSpec spec,
        final Collection<Class<? extends DataValue>> valueClasses, final ColumnFilter columnFilter) {

        return columnFilter.filter(ColumnSelectionUtil.getCompatibleColumns(spec, valueClasses));
    }
}
