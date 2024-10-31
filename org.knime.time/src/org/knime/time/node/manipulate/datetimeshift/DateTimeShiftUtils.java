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
 *   Nov 11, 2024 (Tobias Kampmann): created
 */
package org.knime.time.node.manipulate.datetimeshift;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Consumer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.message.Message;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.TimeInterval;
import org.knime.time.util.Granularity;
import org.knime.time.util.ReplaceOrAppend;

/**
 * Date/Time agnostic utility functions for the Date&Time shift nodes.
 */
@SuppressWarnings("restriction")
final class DateTimeShiftUtils {

    private DateTimeShiftUtils() {
        // utility class
    }

    /*
     * ------------------------------------------------------------------------
     * Column rearranger - execute the operation on the cells
     * ------------------------------------------------------------------------
     */

    static String[] getCompatibleColumns(final DataTableSpec spec,
        final Collection<Class<? extends DataValue>> valueClasses) {
        return spec.stream()
            .filter(s -> valueClasses.stream().anyMatch(valueClass -> s.getType().isCompatible(valueClass)))
            .map(DataColumnSpec::getName).toArray(String[]::new);
    }

    static ColumnRearranger createColumnRearranger(final DataTableSpec spec, final ShiftContext context,
        final GenericShiftType type) {

        ColumnRearranger rearranger = new ColumnRearranger(spec);

        int referenceColumnIndex = getReferenceColumnIndex(spec, context, type);

        for (String selectedColumn : context.selectedColumnNames()) {
            var typeOfTargetColumn = spec.getColumnSpec(selectedColumn).getType();
            var indexOfTargetColumn = spec.findColumnIndex(selectedColumn);

            if (context.replaceOrAppend() == ReplaceOrAppend.REPLACE) {
                final DataColumnSpec dataColSpec =
                    new DataColumnSpecCreator(selectedColumn, typeOfTargetColumn).createSpec();
                rearranger.replace(
                    createCellFactory(dataColSpec, indexOfTargetColumn, referenceColumnIndex, context, type),
                    selectedColumn);
            } else {
                final DataColumnSpec dataColSpec = new UniqueNameGenerator(spec)
                    .newColumn(selectedColumn + context.outputColumnSuffix(), typeOfTargetColumn);
                rearranger
                    .append(createCellFactory(dataColSpec, indexOfTargetColumn, referenceColumnIndex, context, type));
            }
        }
        return rearranger;
    }

    private static int getReferenceColumnIndex(final DataTableSpec spec, final ShiftContext context,
        final GenericShiftType type) {
        String columnName = switch (type) {
            case TEMPORAL_COLUMN -> context.temporalColumn();
            case NUMERICAL_COLUMN -> context.numericalColumn();
            default -> null;
        };

        int index = columnName != null ? spec.findColumnIndex(columnName) : -1;
        if (index < 0 && type != GenericShiftType.TEMPORAL_VALUE) {
            throw new IllegalStateException("The " + type.toString().toLowerCase(Locale.getDefault())
                + " column is missing. This should not happen and is an implementation error.");
        }
        return index;
    }

    private static SingleCellFactory createCellFactory(final DataColumnSpec dataColSpec, final int targetColIndex,
        final int refColIndex, final ShiftContext context, final GenericShiftType type) {

        return switch (type) {
            case TEMPORAL_COLUMN -> new ShiftTemporalAmountColumnCellFactory(dataColSpec, targetColIndex, refColIndex,
                context.messageBuilder(), context.messageConsumer());
            case NUMERICAL_COLUMN -> new ShiftNumericColumnCellFactory(dataColSpec, targetColIndex, refColIndex,
                context.granularity(), context.messageBuilder(), context.messageConsumer());
            case TEMPORAL_VALUE -> new ShiftDurationCellFactory(dataColSpec, targetColIndex, context.temporalValue(),
                context.messageBuilder(), context.messageConsumer());
        };
    }

    enum GenericShiftType {
            TEMPORAL_COLUMN, NUMERICAL_COLUMN, TEMPORAL_VALUE
    }

    /*
     * ------------------------------------------------------------------------
     * Cell factories - create new cells with the shifted temporals
     * ------------------------------------------------------------------------
     */

    abstract static class BaseShiftCellFactory extends SingleCellFactory {

        protected final int m_targetColumnIndex;

        protected final MessageBuilder m_messageBuilder;

        protected final Consumer<Message> m_warningConsumer;

        /**
         * @param newColSpec new column spec
         * @param targetColumnIndex index of the column to shift
         * @param messageBuilder the message builder to collect issues called from NodeModel context:
         *            "createMessageBuilder()"
         * @param warningConsumer the consumer to set warnings from the NodeModel context: "setWarning"
         */
        protected BaseShiftCellFactory(final DataColumnSpec newColSpec, final int targetColumnIndex,
            final MessageBuilder messageBuilder, final Consumer<Message> warningConsumer) {
            super(newColSpec);
            this.m_targetColumnIndex = targetColumnIndex;
            this.m_messageBuilder = messageBuilder;
            this.m_warningConsumer = warningConsumer;
        }

        @Override
        public DataCell getCell(final DataRow row, final long rowIndex) {
            final DataCell cell = row.getCell(m_targetColumnIndex);
            if (cell.isMissing()) {
                return cell;
            }

            try {
                TemporalAmount temporalAmount = getTemporalAmount(row, rowIndex);
                return DateTimeShiftUtils.addTemporalAmountToCell(cell, temporalAmount);
            } catch (ArithmeticException | DateTimeException | MissingValueException e) { // NOSONAR
                final var missingReason = e.getMessage();
                m_messageBuilder.addRowIssue(0, m_targetColumnIndex, rowIndex, missingReason);
                return new MissingCell(missingReason);
            }
        }

        @Override
        public void afterProcessing() {
            final var issueCount = m_messageBuilder.getIssueCount();
            if (issueCount > 0) {
                m_messageBuilder.withSummary("Problems occurred in " + issueCount + " rows.").build()
                    .ifPresent(this.m_warningConsumer);
            }
        }

        protected abstract TemporalAmount getTemporalAmount(final DataRow row, final long rowIndex)
            throws MissingValueException;
    }

    static final class ShiftTemporalAmountColumnCellFactory extends BaseShiftCellFactory {

        private final int m_temporalAmountColumnIdx;

        public ShiftTemporalAmountColumnCellFactory(final DataColumnSpec newColSpec, final int targetColumnIndex,
            final int temporalAmountColumnIndex, final MessageBuilder messageBuilder,
            final Consumer<Message> warningConsumer) {
            super(newColSpec, targetColumnIndex, messageBuilder, warningConsumer);
            this.m_temporalAmountColumnIdx = temporalAmountColumnIndex;
        }

        @Override
        protected TemporalAmount getTemporalAmount(final DataRow row, final long rowIndex) {
            final DataCell temporalAmountCell = row.getCell(m_temporalAmountColumnIdx);

            if (temporalAmountCell.isMissing()) {
                final var missingReason = "The duration cell containing the value to shift is missing.";
                throw new MissingValueException(missingReason);
            }

            if (temporalAmountCell instanceof PeriodValue periodValue) {
                return periodValue.getPeriod();
            } else if (temporalAmountCell instanceof DurationValue durationValue) {
                return durationValue.getDuration();
            } else {
                throw new IllegalStateException("The cell is neither a period nor a duration cell.");
            }
        }
    }

    static final class ShiftNumericColumnCellFactory extends BaseShiftCellFactory {

        private final int m_numericalColumnIndex;

        private final Granularity m_granularity;

        public ShiftNumericColumnCellFactory(final DataColumnSpec newColSpec, final int targetColumnIndex,
            final int numericalColumnIndex, final Granularity granularity, final MessageBuilder messageBuilder,
            final Consumer<Message> warningConsumer) {
            super(newColSpec, targetColumnIndex, messageBuilder, warningConsumer);
            this.m_numericalColumnIndex = numericalColumnIndex;
            this.m_granularity = granularity;

        }

        @Override
        protected TemporalAmount getTemporalAmount(final DataRow row, final long rowIndex) {
            final DataCell numericalAmountCell = row.getCell(m_numericalColumnIndex);

            if (numericalAmountCell.isMissing()) {
                final var missingReason = "The numeric cell containing the value to shift is missing.";
                throw new MissingValueException(missingReason);
            }

            final long numericalValue = ((LongValue)numericalAmountCell).getLongValue();
            return m_granularity.getPeriodOrDuration(numericalValue);
        }
    }

    static final class ShiftDurationCellFactory extends BaseShiftCellFactory {

        private final TemporalAmount m_temporalAmount;

        public ShiftDurationCellFactory(final DataColumnSpec newColSpec, final int targetColumnIndex,
            final TemporalAmount temporalAmount, final MessageBuilder messageBuilder,
            final Consumer<Message> warningConsumer) {
            super(newColSpec, targetColumnIndex, messageBuilder, warningConsumer);
            this.m_temporalAmount = temporalAmount;
        }

        @Override
        protected TemporalAmount getTemporalAmount(final DataRow row, final long rowIndex) {
            return m_temporalAmount;
        }
    }

    /**
     * Convenience function to add a temporal amount to a data cell. Works with periods and durations.
     *
     * @param cell The cell to add the temporal amount to. Allowed: LocalTimeValue, LocalDateValue, LocalDateTimeValue,
     *            ZonedDateTime
     * @param temporalAmount The amount to add to the cell. Allowed: Duration, Period
     * @return The new cell with the temporal amount added
     */
    static DataCell addTemporalAmountToCell(final DataCell cell, final TemporalAmount temporalAmount)
        throws DateTimeException, ArithmeticException {

        if (cell instanceof LocalTimeValue timeCell) {
            if (temporalAmount instanceof TimeInterval || temporalAmount instanceof Duration) {
                final LocalTime time = timeCell.getLocalTime();
                return LocalTimeCellFactory.create(time.plus(temporalAmount));
            }

            throw new IllegalStateException("The temporal amount to shift " + temporalAmount
                + " is not a duration. This should not happen and is an implementation error.");
        }
        if (cell instanceof LocalDateValue dateCell) {
            if (temporalAmount instanceof DateInterval || temporalAmount instanceof Period) {
                final LocalDate localDate = dateCell.getLocalDate();
                return LocalDateCellFactory.create(localDate.plus(temporalAmount));
            }
            throw new IllegalStateException("The temporal amount to shift " + temporalAmount
                + " is not a period. This should not happen and is an implementation error.");
        }
        if (cell instanceof LocalDateTimeValue localDateTimeCell) {
            final LocalDateTime localDateTime = localDateTimeCell.getLocalDateTime();
            return LocalDateTimeCellFactory.create(localDateTime.plus(temporalAmount));
        }
        if (cell instanceof ZonedDateTimeValue zonedDateTimeCell) {
            final ZonedDateTime zonedDateTime = zonedDateTimeCell.getZonedDateTime();
            return ZonedDateTimeCellFactory.create(zonedDateTime.plus(temporalAmount));
        }
        throw new IllegalStateException("The data cell type " + cell.getClass()
            + " is not supported. This should not happen and is an implementation error.");
    }

}
