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
 *   Nov 11, 2024 (tobias): created
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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.message.Message;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;
import org.knime.time.util.ReplaceOrAppend;

/**
 * The node model of the node which rounds time and date columns.
 *
 * @author Tobias Kampmann, TNG
 */
@SuppressWarnings("restriction")
final class DateTimeRoundNodeModel extends WebUISimpleStreamableFunctionNodeModel<DateTimeRoundNodeSettings> {

    /**
     * @param configuration
     */
    protected DateTimeRoundNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, DateTimeRoundNodeSettings.class);
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec, final DateTimeRoundNodeSettings modelSettings)
        throws InvalidSettingsException {

        ColumnRearranger rearranger = new ColumnRearranger(spec);
        String[] selectedColumns = getSelectedColumns(spec, modelSettings);

        for (String selectedColumn : selectedColumns) {

            SingleCellFactory factory = createCellFactory(spec, selectedColumn, modelSettings);

            if (modelSettings.m_appendOrReplace == ReplaceOrAppend.REPLACE) {
                rearranger.replace(factory, selectedColumn);
            } else {
                rearranger.append(factory);
            }
        }
        return rearranger;
    }

    static class RoundCellFactory extends SingleCellFactory {

        private final int m_targetColumnIndex;

        private final MessageBuilder m_messageBuilder;

        private final Consumer<Message> m_setWarning;

        private final DateTimeRoundNodeSettings m_settings;

        /**
         * @param newColSpec new column spec
         * @param targetColumnIndex index of the column to round
         * @param settings the rounding settings of the node
         * @param messageBuilder the message builder to collect issues called from NodeModel context:
         *            "createMessageBuilder()"
         * @param setWarningConsumer the consumer to set warnings from the NodeModel context: "setWarning"
         */
        public RoundCellFactory(final DataColumnSpec newColSpec, final int targetColumnIndex,
            final DateTimeRoundNodeSettings settings, final MessageBuilder messageBuilder,
            final Consumer<Message> setWarningConsumer) {

            super(newColSpec);
            this.m_targetColumnIndex = targetColumnIndex;

            this.m_settings = settings;

            this.m_messageBuilder = messageBuilder;
            this.m_setWarning = setWarningConsumer;
        }

        @Override
        public DataCell getCell(final DataRow row, final long rowIndex) {
            final DataCell cell = row.getCell(m_targetColumnIndex);
            if (cell.isMissing()) {
                return cell;
            }

            var timeSettings = m_settings.m_timeRoundSettings;
            var dateSettings = m_settings.m_dateRoundSettings;

            try {
                return createRoundedTemporalDataCell( //
                    m_settings.m_roundingMode == DateTimeRoundNodeSettings.RoundingMode.TIME //
                        ? DateRoundingUtil.roundDateBasedTemporal(getTemporalFromCell(cell),
                            dateSettings.m_dateRoundingStrategy, dateSettings.m_dateRoundingPrecision,
                            dateSettings.m_shiftMode, dateSettings.m_dayOrWeekDay) //
                        : //
                        TimeRoundingUtil.roundTimeBasedTemporal(getTemporalFromCell(cell),
                            timeSettings.m_timeRoundingStrategy, timeSettings.m_timeRoundingPrecision.getDuration(),
                            timeSettings.m_shiftMode));

            } catch (IllegalArgumentException | DateTimeException e) { // NOSONAR - this is logging the error message
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
    }

    static String[] getCompatibleColumns(final DataTableSpec spec,
        final Collection<Class<? extends DataValue>> valueClasses) {
        return spec.stream()
            .filter(s -> valueClasses.stream().anyMatch(s.getType()::isCompatible))
            .map(DataColumnSpec::getName).toArray(String[]::new);
    }

    SingleCellFactory createCellFactory(final DataTableSpec spec, final String selectedColumn,
        final DateTimeRoundNodeSettings settings) {
        var indexOfTargetColumn = spec.findColumnIndex(selectedColumn);

        DataColumnSpec newColSpec = createColumnSpec(spec, selectedColumn, settings);

        return new RoundCellFactory( //
            newColSpec, //
            indexOfTargetColumn, //
            settings, //
            createMessageBuilder(), //
            this::setWarning); //

    }

    static String[] getSelectedColumns(final DataTableSpec spec, final DateTimeRoundNodeSettings fullSettings) {

        return fullSettings.m_roundingMode == DateTimeRoundNodeSettings.RoundingMode.TIME //
            ? fullSettings.m_timeRoundSettings.m_columnFilter
                .getSelected(getCompatibleColumns(spec, TimeRoundNodeSettings.TIME_COLUMN_TYPES), spec) //
            : fullSettings.m_dateRoundSettings.m_columnFilter
                .getSelected(getCompatibleColumns(spec, DateRoundNodeSettings.DATE_COLUMN_TYPES), spec);
    }

    /**
     * Creates a new DataColumnSpec with the given column name and the type of the selected column.
     *
     * @param spec The DataTableSpec of the input table
     * @param selectedColumn The name of the selected column
     * @param settings The settings of the node
     * @return The new DataColumnSpec
     */
    private static DataColumnSpec createColumnSpec(final DataTableSpec spec, final String selectedColumn,
        final DateTimeRoundNodeSettings settings) {

        var typeOfTargetColumn = spec.getColumnSpec(selectedColumn).getType();
        return settings.m_appendOrReplace == ReplaceOrAppend.REPLACE
            ? new DataColumnSpecCreator(selectedColumn, typeOfTargetColumn).createSpec() //
            : new UniqueNameGenerator(spec).newColumn(selectedColumn + settings.m_outputColumnSuffix,
                typeOfTargetColumn);
    }

    /**
     * Convenience function to create a new data cell from a Temporal. Supported Temporal types: ZonedDateTime,
     * LocalDateTime, LocalTime, LocalDate
     *
     * @return The new data cell
     */
    static DataCell createRoundedTemporalDataCell(final Temporal temporal) {

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
    static Temporal getTemporalFromCell(final DataCell cell) {

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
