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
 *   Nov 10, 2024 (david): created
 */
package org.knime.time.node.create.createdatetime;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.time.node.create.createdatetime.CreateDateTimeNodeSettings.FixedSteps;
import org.knime.time.node.create.createdatetime.CreateDateTimeNodeSettings.OutputType;
import org.knime.time.util.DateTimeType;

/**
 * The node model of the node which creates date and time cells.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class CreateDateTimeNodeModel2 extends WebUINodeModel<CreateDateTimeNodeSettings> {

    /**
     * @param configuration
     */
    protected CreateDateTimeNodeModel2(final WebUINodeConfiguration configuration) {
        super(configuration, CreateDateTimeNodeSettings.class);
    }

    @Override
    protected void validateSettings(final CreateDateTimeNodeSettings loadedSettings) throws InvalidSettingsException {
        // If the duration is zero and being used, things will go badly, so let's check that first.
        assertNonZeroInterval(loadedSettings);

        // If the number of rows is zero and being used, things will go badly, so let's check that too.
        assertNumberOfRowsGreaterThanZero(loadedSettings);

        // We need to check that if the fixed step behaviour is INTERVAL_AND_END, that the start time is before the end
        // time if the interval is positive, or the other way around if the interval is negative. This doesn't apply if
        // the outmode mode is TIME, since they loop around.
        assertOrderOfStartAndEnd(loadedSettings);

        // Also check if the output column name is empty (blank is allowed)
        assertNonEmptyOutputColumnName(loadedSettings);
    }

    private static void assertNonEmptyOutputColumnName(final CreateDateTimeNodeSettings loadedSettings)
        throws InvalidSettingsException {
        if (loadedSettings.m_outputColumnName.isEmpty()) {
            throw new InvalidSettingsException("The output column name must not be empty.");
        }
    }

    private static void assertOrderOfStartAndEnd(final CreateDateTimeNodeSettings loadedSettings)
        throws InvalidSettingsException {
        if (loadedSettings.m_fixedSteps == CreateDateTimeNodeSettings.FixedSteps.INTERVAL_AND_END
            && loadedSettings.m_outputType != OutputType.TIME) {
            var start = extractStartTimeFromSettings(loadedSettings);
            var end = extractEndTimeFromSettings(loadedSettings);

            if (loadedSettings.m_interval.isStrictlyPositive() && start.isAfter(end)) {
                throw new InvalidSettingsException(
                    "The start time must be before the end time when the interval is ascending.");
            } else if (loadedSettings.m_interval.isStrictlyNegative() && start.isBefore(end)) {
                throw new InvalidSettingsException(
                    "The start time must be after the end time when the interval is descending.");
            }
        }
    }

    private static void assertNumberOfRowsGreaterThanZero(final CreateDateTimeNodeSettings loadedSettings)
        throws InvalidSettingsException {
        var numberOfRowsIsUsed = loadedSettings.m_fixedSteps == FixedSteps.NUMBER_AND_END
            || loadedSettings.m_fixedSteps == FixedSteps.NUMBER_AND_INTERVAL;
        if (numberOfRowsIsUsed && loadedSettings.m_numberOfRows <= 0) {
            throw new InvalidSettingsException("The number of steps must be greater than zero.");
        }
    }

    private static void assertNonZeroInterval(final CreateDateTimeNodeSettings loadedSettings)
        throws InvalidSettingsException {
        var intervalIsUsed = loadedSettings.m_fixedSteps == FixedSteps.INTERVAL_AND_END
            || loadedSettings.m_fixedSteps == FixedSteps.NUMBER_AND_INTERVAL;
        if (intervalIsUsed && loadedSettings.m_interval.isZero()) {
            throw new InvalidSettingsException("The provided interval must be non-zero.");
        }
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final CreateDateTimeNodeSettings settings)
        throws InvalidSettingsException {
        return new DataTableSpec[]{createOutSpec(settings)};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final CreateDateTimeNodeSettings settings) throws Exception {

        BufferedDataContainer container = exec.createDataContainer(createOutSpec(settings));

        switch (settings.m_fixedSteps) {
            case NUMBER_AND_INTERVAL:
                executeForFixedNumberOfStepsAndInterval(container, settings);
                break;
            case NUMBER_AND_END:
                executeForFixedNumberOfStepsAndEndPoint(container, settings);
                break;
            case INTERVAL_AND_END:
                executeForFixedEndPointAndInterval(container, settings);
                break;
        }

        container.close();

        return new BufferedDataTable[]{exec.createBufferedDataTable(container.getTable(), exec)};
    }

    private static void executeForFixedNumberOfStepsAndEndPoint(final BufferedDataContainer container,
        final CreateDateTimeNodeSettings settings) {

        final var startPoint = extractStartTimeFromSettings(settings);

        // If the number of rows is 0, return an empty table. If 1, return a table with just the start time.
        if (settings.m_numberOfRows == 0) {
            return;
        } else if (settings.m_numberOfRows == 1) {
            container.addRowToTable(createRow(0, settings.m_outputType, extractStartTimeFromSettings(settings)));
            return;
        }

        final var endPoint = extractEndTimeFromSettings(settings);

        var stepSize = Duration.between(startPoint, endPoint).dividedBy(Math.max(1, settings.m_numberOfRows - 1));

        // Create the cells. Skip the last row for now
        for (long i = 0; i < settings.m_numberOfRows - 1; ++i) {
            var next = startPoint.plus(stepSize.multipliedBy(i));

            container.addRowToTable(createRow(i, settings.m_outputType, next));
        }

        // Now add the last row. We do this so that there are no rounding errors
        // that would mean the exact end point specified by the user is not included.
        container.addRowToTable(createRow(settings.m_numberOfRows - 1, settings.m_outputType, endPoint));
    }

    private static void executeForFixedNumberOfStepsAndInterval(final BufferedDataContainer container,
        final CreateDateTimeNodeSettings settings) {

        final var startPoint = extractStartTimeFromSettings(settings);
        final var step = settings.m_interval;

        ZonedDateTime current = startPoint;

        // Create the cells
        for (long i = 0; i < settings.m_numberOfRows; ++i) {
            container.addRowToTable(createRow(i, settings.m_outputType, current));

            current = current.plus(step);
        }
    }

    /**
     * Has the current date time not passed the end point? (i.e. is it on the right side of (depending on the sign of
     * the step), or equal to, the end point)?
     */
    private static boolean hasNotPassedEndPoint(final ZonedDateTime current, final ZonedDateTime endPoint,
        final Interval step) {

        var hasNotReachedEndPointAscending = current.isBefore(endPoint) && step.isStrictlyPositive();
        var hasNotReachedEndPointDescending = current.isAfter(endPoint) && step.isStrictlyNegative();

        return current.isEqual(endPoint) || hasNotReachedEndPointAscending || hasNotReachedEndPointDescending;
    }

    private static void executeForFixedEndPointAndInterval(final BufferedDataContainer container,
        final CreateDateTimeNodeSettings settings) {
        /*
         *  Okay, this one is a little tricky. We need to know if the current time is before the end time,
         *  so to do that we convert them both to (zoned) date time and compare them. If the target output
         *  type is LocalTime, then after the conversion, if the start time is after the end time, we add one day
         *  to the end time. Then at the end, we cut off whatever part we need to cut off to make it fit the correct
         *  datatype.
         */
        final var startPoint = extractStartTimeFromSettings(settings);
        var endPoint = extractEndTimeFromSettings(settings);
        final var interval = settings.m_interval;

        // Check that the start, interval and endpoint are all mutually consistent
        if (startPoint.isAfter(endPoint) && interval.isStrictlyPositive()) {
            if (settings.m_outputType == OutputType.TIME) {
                endPoint = endPoint.plusDays(1);
            } else {
                throw new IllegalArgumentException("Start time must be before end time when interval is positive.");
            }
        } else if (startPoint.isBefore(endPoint) && interval.isStrictlyNegative()) {
            if (settings.m_outputType == OutputType.TIME) {
                endPoint = endPoint.minusDays(1);
            } else {
                throw new IllegalArgumentException("Start time must be after end time when interval is negative.");
            }
        }

        ZonedDateTime current = startPoint;

        // Create the cells. We include the end point if it is a multiple of the step.
        while (hasNotPassedEndPoint(current, endPoint, interval)) {
            container.addRowToTable(createRow(container.size(), settings.m_outputType, current));
            current = current.plus(interval);
        }
    }

    private static DataRow createRow(final long index, final OutputType type, final ZonedDateTime temporal) {
        var cellToAdd = switch (type) {
            case DATE -> LocalDateCellFactory.create(LocalDate.from(temporal));
            case TIME -> LocalTimeCellFactory.create(LocalTime.from(temporal));
            case DATE_TIME -> LocalDateTimeCellFactory.create(LocalDateTime.from(temporal));
            case DATE_TIME_WITH_TIMEZONE -> ZonedDateTimeCellFactory.create(temporal);
        };

        return new DefaultRow( //
            "Row" + index, //
            cellToAdd //
        );
    }

    private static DataTableSpec createOutSpec(final CreateDateTimeNodeSettings settings) {
        var newDataType = (switch (settings.m_outputType) {
            case DATE -> DateTimeType.LOCAL_DATE;
            case TIME -> DateTimeType.LOCAL_TIME;
            case DATE_TIME -> DateTimeType.LOCAL_DATE_TIME;
            case DATE_TIME_WITH_TIMEZONE -> DateTimeType.ZONED_DATE_TIME;
        }).getDataType();

        return new DataTableSpec( //
            new DataColumnSpecCreator(settings.m_outputColumnName, newDataType).createSpec() //
        );
    }

    /**
     * Extract the end time from the settings as a {@link ZonedDateTime}. Parts not present will be defaulted to epoch,
     * midnight, and UTC.
     */
    private static ZonedDateTime extractEndTimeFromSettings(final CreateDateTimeNodeSettings settings) {
        final var utc = ZoneId.of("UTC");

        return switch (settings.m_outputType) {
            case DATE -> settings.m_localDateEnd.atTime(LocalTime.NOON).atZone(utc);
            case TIME -> settings.m_localTimeEnd.atDate(LocalDate.EPOCH).atZone(utc);
            case DATE_TIME -> ZonedDateTime.of(settings.m_localDateEnd, settings.m_localTimeEnd, utc);
            case DATE_TIME_WITH_TIMEZONE -> ZonedDateTime.of(settings.m_localDateEnd, settings.m_localTimeEnd,
                settings.m_timezone);
        };
    }

    /**
     * Extract the start time from the settings as a {@link ZonedDateTime}. Parts not present will be defaulted to
     * epoch, midnight, and UTC.
     */
    private static ZonedDateTime extractStartTimeFromSettings(final CreateDateTimeNodeSettings settings) {
        final var utc = ZoneId.of("UTC");

        return switch (settings.m_outputType) {
            case DATE -> settings.m_localDateStart.atTime(LocalTime.NOON).atZone(utc);
            case TIME -> settings.m_localTimeStart.atDate(LocalDate.EPOCH).atZone(utc);
            case DATE_TIME -> ZonedDateTime.of(settings.m_localDateStart, settings.m_localTimeStart, utc);
            case DATE_TIME_WITH_TIMEZONE -> ZonedDateTime.of(settings.m_localDateStart, settings.m_localTimeStart,
                settings.m_timezone);
        };
    }
}
