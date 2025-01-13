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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.message.Message;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;

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

        private final Consumer<Message> m_setWarning;

        private final UnaryOperator<Temporal> m_roundingOperator;

        /**
         * @param newColSpec new column spec
         * @param targetColumnIndex index of the column to round
         * @param roundingOperator the rounding operator to apply
         * @param messageBuilder the message builder to collect issues called from NodeModel context:
         *            "createMessageBuilder()"
         * @param setWarningConsumer the consumer to set warnings from the NodeModel context: "setWarning"
         */
        public RoundCellFactory(final DataColumnSpec newColSpec, final int targetColumnIndex,
            final UnaryOperator<Temporal> roundingOperator, final MessageBuilder messageBuilder,
            final Consumer<Message> setWarningConsumer) {

            super(newColSpec);
            this.m_targetColumnIndex = targetColumnIndex;

            this.m_roundingOperator = roundingOperator;

            this.m_messageBuilder = messageBuilder;
            this.m_setWarning = setWarningConsumer;
        }

        @Override
        public DataCell getCell(final DataRow row, final long rowIndex) {
            final DataCell cell = row.getCell(m_targetColumnIndex);
            if (cell.isMissing()) {
                return cell;
            }

            try {
                return createRoundedTemporalDataCell(m_roundingOperator.apply(getTemporalFromCell(cell)));
            } catch (IllegalArgumentException | DateTimeException | ArithmeticException e) { // NOSONAR - this is logging the error message
                m_messageBuilder.addRowIssue(0, m_targetColumnIndex, rowIndex, e.getMessage());
                return new MissingCell(e.getMessage());
            }
        }

        @Override
        public void afterProcessing() {
            final var issueCount = m_messageBuilder.getIssueCount();
            if (issueCount > 0) {
                m_messageBuilder.withSummary("Problems occurred in " + issueCount + " rows.").build()
                    .ifPresent(this.m_setWarning);
            }
        }

        /**
         * Convenience function to create a new data cell from a Temporal. Supported Temporal types: ZonedDateTime,
         * LocalDateTime, LocalTime, LocalDate
         *
         * @return The new data cell
         */
        private static DataCell createRoundedTemporalDataCell(final Temporal temporal) {

            if (temporal instanceof ZonedDateTime zonedDateTime) {
                return ZonedDateTimeCellFactory.create(zonedDateTime);
            } else if (temporal instanceof LocalDateTime localDateTime) {
                return LocalDateTimeCellFactory.create(localDateTime);
            } else if (temporal instanceof LocalTime localTime) {
                return LocalTimeCellFactory.create(localTime);
            } else if (temporal instanceof LocalDate localDate) {
                return LocalDateCellFactory.create(localDate);
            } else {
                throw new IllegalArgumentException("Unsupported Temporal type: " + temporal.getClass());
            }
        }

        /**
         * Convenience function to get a Temporal from a DataCell.
         *
         * @param cell The cell to get the Temporal from. Allowed: LocalTimeValue, LocalDateValue, LocalDateTimeValue,
         *            ZonedDateTime
         * @return The Temporal from the cell
         */
        private static Temporal getTemporalFromCell(final DataCell cell) {

            if (cell instanceof ZonedDateTimeCell zonedDateTimeCell) {
                return zonedDateTimeCell.getZonedDateTime();
            } else if (cell instanceof LocalDateTimeCell localDateTimeCell) {
                return localDateTimeCell.getLocalDateTime();
            } else if (cell instanceof LocalTimeCell localTimeCell) {
                return localTimeCell.getLocalTime();
            } else if (cell instanceof LocalDateCell localDateCell) {
                return localDateCell.getLocalDate();
            } else {
                throw new IllegalArgumentException("Unsupported Temporal type: " + cell.getClass());
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

        return columnFilter.getSelected(getCompatibleColumns(spec, valueClasses), spec);
    }

    static String[] getCompatibleColumns(final DataTableSpec spec,
        final Collection<Class<? extends DataValue>> valueClasses) {

        return spec.stream()
            .filter(dateColumnSpec -> valueClasses.stream().anyMatch(dateColumnSpec.getType()::isCompatible))
            .map(DataColumnSpec::getName).toArray(String[]::new);
    }

}
