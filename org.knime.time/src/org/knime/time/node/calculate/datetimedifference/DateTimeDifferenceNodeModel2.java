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
 *   Dec 13, 2024 (david): created
 */
package org.knime.time.node.calculate.datetimedifference;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Objects;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.MissingCell;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.time.node.calculate.datetimedifference.DateTimeDifferenceNodeSettings.Mode;
import org.knime.time.node.calculate.datetimedifference.DateTimeDifferenceNodeSettings.OutputNumberType;
import org.knime.time.node.calculate.datetimedifference.DateTimeDifferenceNodeSettings.SecondDateTimeValueType;
import org.knime.time.util.Granularity;

/**
 * The new model for the DateTimeDifference node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class DateTimeDifferenceNodeModel2 extends WebUINodeModel<DateTimeDifferenceNodeSettings> {

    private static final String ERROR_MISSING_CELL = "Can't calculate difference to missing cell.";

    /**
     * @param configuration
     */
    protected DateTimeDifferenceNodeModel2(final WebUINodeConfiguration configuration) {
        super(configuration, DateTimeDifferenceNodeSettings.class);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final DateTimeDifferenceNodeSettings modelSettings) throws Exception {

        var inSpec = inData[0].getDataTableSpec();

        if (modelSettings.m_secondDateTimeValueType == SecondDateTimeValueType.PREVIOUS_ROW) {
            var outSpec = new DataTableSpecCreator(inSpec) //
                .addColumns(createNewColumnSpec(inSpec, modelSettings)) //
                .createSpec();

            var containerWritable = new BufferedDataTableRowOutput(exec.createDataContainer(outSpec));

            return new BufferedDataTable[]{ //
                createRowDifferenceTable(inData[0], containerWritable, modelSettings) //
            };
        } else {
            return new BufferedDataTable[]{ //
                exec.createColumnRearrangeTable( //
                    inData[0], //
                    createColumnRearranger(inSpec, modelSettings), //
                    exec //
                ) //
            };
        }
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final DateTimeDifferenceNodeSettings modelSettings) throws InvalidSettingsException {

        var inSpec = inSpecs[0];

        return new DataTableSpec[]{ //
            createNewSpec(inSpec, modelSettings) //
        };
    }

    private static ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
        final DateTimeDifferenceNodeSettings settings) {

        var rearranger = new ColumnRearranger(inSpec);
        rearranger.append(new DifferenceCellFactory(inSpec, settings));
        return rearranger;
    }

    private static DataTableSpec createNewSpec(final DataTableSpec inSpec,
        final DateTimeDifferenceNodeSettings settings) {
        return new DataTableSpecCreator(inSpec).addColumns(createNewColumnSpec(inSpec, settings)).createSpec();
    }

    private static DataColumnSpec createNewColumnSpec(final DataTableSpec inSpec,
        final DateTimeDifferenceNodeSettings settings) {

        var selectedFirstColumnSpec = inSpec.getColumnSpec(settings.m_firstColumnSelection.m_selected);

        var type = switch (settings.m_outputType) {
            case DURATION_OR_PERIOD -> selectedFirstColumnSpec.getType().equals(LocalDateCellFactory.TYPE) //
                ? PeriodCellFactory.TYPE //
                : DurationCellFactory.TYPE;
            case NUMBER -> settings.m_outputNumberType == OutputNumberType.DECIMALS //
                ? DoubleCellFactory.TYPE //
                : LongCellFactory.TYPE;
        };

        return new UniqueNameGenerator(inSpec).newColumn(settings.m_outputColumnName, type);
    }

    private static class DifferenceCellFactory extends SingleCellFactory {

        private final DateTimeDifferenceNodeSettings m_settings;

        private final DataTableSpec m_inSpec;

        private final int m_firstColumnIndex;

        public DifferenceCellFactory(final DataTableSpec inSpec, final DateTimeDifferenceNodeSettings settings) {
            super(createNewColumnSpec(inSpec, settings));

            m_settings = settings;
            m_inSpec = inSpec;

            m_firstColumnIndex = m_inSpec.findColumnIndex(m_settings.m_firstColumnSelection.m_selected);
        }

        @Override
        public DataCell getCell(final DataRow row) {
            var firstCell = row.getCell(m_firstColumnIndex);

            if (firstCell.isMissing()) {
                return new MissingCell(ERROR_MISSING_CELL);
            }

            var firstTemporal = extractTemporalFromDataCell(firstCell);

            if (m_settings.m_secondDateTimeValueType == SecondDateTimeValueType.COLUMN) {
                var secondColumnIndex = m_inSpec.findColumnIndex(m_settings.m_secondColumnSelection.m_selected);
                var secondCell = row.getCell(secondColumnIndex);

                if (secondCell.isMissing()) {
                    return new MissingCell(ERROR_MISSING_CELL);
                }

                var secondTemporal = extractTemporalFromDataCell(secondCell);

                return createDifferenceCell(firstTemporal, secondTemporal, m_settings);
            } else {
                return createDifferenceCell(firstTemporal, switch (m_settings.m_secondDateTimeValueType) {
                    case FIXED_DATE_TIME -> createTemporalFromPotentiallyNullFields( //
                        m_settings.m_localDateFixed, //
                        m_settings.m_localTimeFixed, //
                        m_settings.m_timezoneFixed //
                        );
                    case EXECUTION_DATE_TIME -> ZonedDateTime.now();
                    default -> throw new IllegalArgumentException(
                        "Unsupported SecondDateTimeValueType: " + m_settings.m_secondDateTimeValueType);
                }, m_settings);
            }
        }

        private static Temporal createTemporalFromPotentiallyNullFields(final LocalDate date, final LocalTime time,
            final ZoneId id) {

            if (date == null && time == null && id == null) {
                throw new IllegalArgumentException("All fields are null.");
            }

            if (id != null) {
                return ZonedDateTime.of(date, time, id);
            }

            if (date == null) {
                return Objects.requireNonNull(time);
            }

            if (time == null) {
                return Objects.requireNonNull(date);
            }

            return LocalDateTime.of(date, time);
        }
    }

    /**
     * Create a new table with a column containing the difference between each row and the previous row. The first row
     * will contain a missing cell for the difference.
     */
    private static BufferedDataTable createRowDifferenceTable(final BufferedDataTable inData,
        final BufferedDataTableRowOutput containerWritable, final DateTimeDifferenceNodeSettings settings)
        throws InterruptedException {

        // If there are no rows, we can't calculate a difference so return an empty table.
        if (inData.size() == 0) {
            containerWritable.close();
            return containerWritable.getDataTable();
        }

        var firstColumnIndex = inData.getDataTableSpec().findColumnIndex(settings.m_firstColumnSelection.m_selected);

        try (var inDataIterator = inData.iterator()) {
            var previousRow = inDataIterator.next();

            containerWritable.push(new AppendedColumnRow( //
                previousRow, //
                new MissingCell("No previous row for calculating difference available.") //
            ));

            while (inDataIterator.hasNext()) {
                var currentRow = inDataIterator.next();

                var previousCell = previousRow.getCell(firstColumnIndex);
                var currentCell = currentRow.getCell(firstColumnIndex);

                var newCell = createDifferenceCell( //
                    previousCell, //
                    currentCell, //
                    settings //
                );

                containerWritable.push(new AppendedColumnRow( //
                    currentRow, //
                    newCell //
                ));

                previousRow = currentRow;
            }
        }

        containerWritable.close();
        return containerWritable.getDataTable();

    }

    /**
     * Create a data cell representing the difference between two temporal values.
     */
    private static DataCell createDifferenceCell(final Temporal start, final Temporal end,
        final DateTimeDifferenceNodeSettings settings) {

        var negate = settings.m_mode == Mode.FIRST_MINUS_SECOND;

        return switch (settings.m_outputType) {
            case DURATION_OR_PERIOD -> start instanceof LocalDate //
                ? periodCellBetween(start, end, negate) //
                : durationCellBetween(start, end, negate);
            case NUMBER -> settings.m_outputNumberType == OutputNumberType.DECIMALS //
                ? doubleBetween(start, end, settings.m_granularity, negate) //
                : longCellBetween(start, end, settings.m_granularity, negate);
        };
    }

    /**
     * Create a data cell representing the difference between two data cells. If Either of the cells is missing, a
     * missing cell is returned.
     */
    private static DataCell createDifferenceCell(final DataCell firstCell, final DataCell secondCell,
        final DateTimeDifferenceNodeSettings settings) {
        if (firstCell.isMissing() || secondCell.isMissing()) {
            return new MissingCell(ERROR_MISSING_CELL);
        }

        return createDifferenceCell( //
            extractTemporalFromDataCell(firstCell), //
            extractTemporalFromDataCell(secondCell), //
            settings //
        );
    }

    /**
     * Will error for a LocalDate! But the settings dialogue should prevent that, unless the user manually overrides the
     * settings with a flow variable.
     */
    private static DataCell durationCellBetween(final Temporal start, final Temporal end, final boolean negate) {
        var duration = Duration.between(start, end);
        return DurationCellFactory.create(negate ? duration.negated() : duration);
    }

    /**
     * Will error for anything except a LocalDate! But the settings dialogue should prevent that, unless the user
     * manually overrides the settings with a flow variable.
     */
    private static DataCell periodCellBetween(final Temporal start, final Temporal end, final boolean negate) {
        var period = Period.between(LocalDate.from(start), LocalDate.from(end));

        return PeriodCellFactory.create(negate ? period.negated() : period);
    }

    private static DataCell longCellBetween(final Temporal start, final Temporal end, final Granularity unit,
        final boolean negate) {
        return LongCellFactory.create(unit.between(start, end) * (negate ? -1 : 1));
    }

    /**
     * Will error for date-based granularity units! But the settings dialogue should prevent that, unless the user
     * manually overrides the settings with a flow variable.
     */
    private static DataCell doubleBetween(final Temporal start, final Temporal end, final Granularity unit,
        final boolean negate) {
        return DoubleCellFactory.create(unit.betweenExact(start, end) * (negate ? -1 : 1));
    }

    static Temporal extractTemporalFromDataCell(final DataCell cell) {
        if (cell instanceof ZonedDateTimeValue zonedDateTimeValue) {
            return zonedDateTimeValue.getZonedDateTime();
        } else if (cell instanceof LocalDateTimeValue localDateTimeValue) {
            return localDateTimeValue.getLocalDateTime();
        } else if (cell instanceof LocalTimeValue localTimeValue) {
            return localTimeValue.getLocalTime();
        } else if (cell instanceof LocalDateValue localDateValue) {
            return localDateValue.getLocalDate();
        } else {
            throw new IllegalArgumentException("Unsupported Temporal type: " + cell.getClass());
        }
    }
}