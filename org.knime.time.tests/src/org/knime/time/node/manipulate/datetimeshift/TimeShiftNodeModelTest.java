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
 *   Nov 20, 2024 (Tobias Kampmann, TNG): created
 */
package org.knime.time.node.manipulate.datetimeshift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.InputTableNode;
import org.knime.InputTableNode.NamedCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.TimeInterval;
import org.knime.testing.util.WorkflowManagerUtil;
import org.knime.time.node.manipulate.datetimeshift.TimeShiftNodeSettings.TimeGranularity;
import org.knime.time.util.ReplaceOrAppend;

@SuppressWarnings("restriction")
class TimeShiftNodeModelTest {

    private NativeNodeContainer m_timeShiftNode;

    private WorkflowManager m_wfm;

    @BeforeEach
    void resetWorkflow() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
        m_timeShiftNode = WorkflowManagerUtil.createAndAddNode(m_wfm, new TimeShiftNodeFactory());
    }

    @Test
    void canBeExecuted() throws IOException, InvalidSettingsException {

        final var settings = new TimeShiftNodeSettings();
        setSettings(settings);

        InputTableNode.addDefaultTableToNodeInputPort(m_wfm, m_timeShiftNode, 1);

        m_wfm.executeAllAndWaitUntilDone();
        assertTrue(m_timeShiftNode.getNodeContainerState().isExecuted());
    }

    @Test
    void appendColumn() throws IOException, InvalidSettingsException {

        var suffixToAdd = "new column suffix";

        final var settings = new TimeShiftNodeSettings();
        settings.m_replaceOrAppend = ReplaceOrAppend.APPEND;
        settings.m_outputColumnSuffix = suffixToAdd;
        settings.m_columnFilter = new ColumnFilter(new String[]{InputTableNode.COLUMN_LOCAL_TIME,
            InputTableNode.COLUMN_LOCAL_DATE_TIME, InputTableNode.COLUMN_ZONED_DATE_TIME});
        setSettings(settings);

        InputTableNode.addDefaultTableToNodeInputPort(m_wfm, m_timeShiftNode, 1);

        m_wfm.executeAllAndWaitUntilDone();

        var outputTable = (BufferedDataTable)m_timeShiftNode.getOutPort(1).getPortObject();

        assertTrue(outputTable.getDataTableSpec().containsName(InputTableNode.COLUMN_LOCAL_TIME + suffixToAdd));
        assertTrue(outputTable.getDataTableSpec().containsName(InputTableNode.COLUMN_LOCAL_DATE_TIME + suffixToAdd));
        assertTrue(outputTable.getDataTableSpec().containsName(InputTableNode.COLUMN_ZONED_DATE_TIME + suffixToAdd));
    }

    // Define the method to supply parameters
    static Stream<Arguments> provideTestDataForShiftByDurationValue() {
        return Stream.of( //
            Arguments.of("PT1S", LocalTime.of(0, 0, 0), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift at Midnight (00:00:00)"),

            Arguments.of("PT1S", LocalTime.of(23, 59, 59), LocalDateTime.of(2024, 1, 1, 23, 59, 59),
                ZonedDateTime.of(2024, 1, 1, 23, 59, 59, 0, ZoneId.of("UTC")), "Shift at End of Day (23:59:59)"),

            Arguments.of("PT13H", LocalTime.of(12, 0, 0), LocalDateTime.of(2024, 2, 28, 12, 0, 0),
                ZonedDateTime.of(2024, 2, 28, 12, 0, 0, 0, ZoneId.of("UTC")), "Leap Year Edge Case"),

            Arguments.of("PT1H", LocalTime.of(23, 0, 0), LocalDateTime.of(2024, 3, 10, 23, 0, 0),
                ZonedDateTime.of(2024, 3, 10, 23, 0, 0, 0, ZoneId.of("America/New_York")), "DST Transition"),

            Arguments.of("PT1M", LocalTime.of(23, 59, 0), LocalDateTime.of(2024, 1, 1, 23, 59, 0),
                ZonedDateTime.of(2024, 1, 1, 23, 59, 0, 0, ZoneId.of("UTC")), "Shift at End of Hour (23:59:00)"),

            Arguments.of("-PT1H", LocalTime.of(12, 0, 0), LocalDateTime.of(2024, 1, 1, 12, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")), "Negative Shift"),

            Arguments.of("-PT1H", LocalTime.of(0, 0, 0), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Negative Shift on start of a day"),

            Arguments.of("PT1S", LocalTime.of(12, 0, 0), LocalDateTime.of(2024, 1, 1, 12, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")), "Large Shift by 1000 seconds"),

            Arguments.of("PT1H", LocalTime.of(23, 59, 59), LocalDateTime.of(2024, 12, 31, 23, 59, 59),
                ZonedDateTime.of(2024, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC")), "Shift Across Year Boundary"),

            Arguments.of("PT0.999S", LocalTime.of(12, 0, 0, 999_000_000),
                LocalDateTime.of(2024, 1, 1, 12, 0, 0, 1_999_999),
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 1_999_999, ZoneId.of("UTC")), "Milliseconds Overflow"));
    }

    /**
     * @param testName name of the test. not used besides giving each test a speaking name
     */
    @ParameterizedTest(name = "{4}")
    @MethodSource("provideTestDataForShiftByDurationValue")
    void canShiftTimeColumnsByDurationValue(final String durationValue, final LocalTime localTimeBefore,
        final LocalDateTime localDateTimeBefore, final ZonedDateTime zonedDateTimeBefore, final String testName)
        throws IOException, InvalidSettingsException {

        final var settings = new TimeShiftNodeSettings();

        settings.m_shiftMode = TimeShiftNodeSettings.ShiftMode.SHIFT_VALUE;

        var interval = Interval.parseHumanReadableOrIso(durationValue);
        if (!(interval instanceof TimeInterval)) {
            throw new InvalidSettingsException("Interval must be a time interval");
        }
        settings.m_shiftDurationValue = (TimeInterval)interval;

        settings.m_columnFilter = new ColumnFilter(new String[]{InputTableNode.COLUMN_LOCAL_TIME,
            InputTableNode.COLUMN_LOCAL_DATE_TIME, InputTableNode.COLUMN_ZONED_DATE_TIME});

        setSettings(settings);

        // input table generation
        NamedCell[] cells = { //
            new NamedCell(InputTableNode.COLUMN_DURATION, DurationCellFactory.create(durationValue)), //
            new NamedCell(InputTableNode.COLUMN_LOCAL_TIME, LocalTimeCellFactory.create(localTimeBefore)),
            new NamedCell(InputTableNode.COLUMN_LOCAL_DATE_TIME, LocalDateTimeCellFactory.create(localDateTimeBefore)),
            new NamedCell(InputTableNode.COLUMN_ZONED_DATE_TIME, ZonedDateTimeCellFactory.create(zonedDateTimeBefore))};

        var inputTableSupplier = InputTableNode.createTestTableSupplier(cells);
        InputTableNode.addTableToNodeInputPort(m_wfm, inputTableSupplier, m_timeShiftNode, 1);

        var inputTable = inputTableSupplier.get();
        var localTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_LOCAL_TIME);
        var localDateTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_LOCAL_DATE_TIME);
        var zonedDateTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_ZONED_DATE_TIME);

        // execute node
        assertTrue(m_wfm.executeAllAndWaitUntilDone());

        // output table and assertion
        var outputTable = (BufferedDataTable)m_timeShiftNode.getOutPort(1).getPortObject();

        var row = InputTableNode.getFirstRow(outputTable);

        var localTimeAfter = ((LocalTimeCell)row.getCell(localTimeIndex).materializeDataCell()).getLocalTime();
        var localDateTimeAfter =
            ((LocalDateTimeCell)row.getCell(localDateTimeIndex).materializeDataCell()).getLocalDateTime();
        var zonedDateTimeAfter =
            ((ZonedDateTimeCell)row.getCell(zonedDateTimeIndex).materializeDataCell()).getZonedDateTime();

        assertEquals(localTimeBefore.plus(settings.m_shiftDurationValue), localTimeAfter);
        assertEquals(localDateTimeBefore.plus(settings.m_shiftDurationValue), localDateTimeAfter);
        assertEquals(zonedDateTimeBefore.plus(settings.m_shiftDurationValue), zonedDateTimeAfter);
    }

    // Define the method to supply parameters
    static Stream<Arguments> provideTestDataForShiftByDurationColumn() {
        return Stream.of( //
            Arguments.of(Duration.ofSeconds(1), LocalTime.of(0, 0, 0), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift at Midnight (00:00:00)"),

            Arguments.of(Duration.ofSeconds(1), LocalTime.of(23, 59, 59), LocalDateTime.of(2024, 1, 1, 23, 59, 59),
                ZonedDateTime.of(2024, 1, 1, 23, 59, 59, 0, ZoneId.of("UTC")), "Shift at End of Day (23:59:59)"),

            Arguments.of(Duration.ofHours(13), LocalTime.of(12, 0, 0), LocalDateTime.of(2024, 2, 28, 12, 0, 0),
                ZonedDateTime.of(2024, 2, 28, 12, 0, 0, 0, ZoneId.of("UTC")), "Leap Year Edge Case"),

            Arguments.of(Duration.ofHours(1), LocalTime.of(23, 0, 0), LocalDateTime.of(2024, 3, 10, 23, 0, 0),
                ZonedDateTime.of(2024, 3, 10, 23, 0, 0, 0, ZoneId.of("America/New_York")), "DST Transition"),

            Arguments.of(Duration.ofMinutes(1), LocalTime.of(23, 59, 0), LocalDateTime.of(2024, 1, 1, 23, 59, 0),
                ZonedDateTime.of(2024, 1, 1, 23, 59, 0, 0, ZoneId.of("UTC")), "Shift at End of Hour (23:59:00)"),

            Arguments.of(Duration.ofHours(-1), LocalTime.of(12, 0, 0), LocalDateTime.of(2024, 1, 1, 12, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")), "Negative Shift"),

            Arguments.of(Duration.ofHours(-1), LocalTime.of(0, 0, 0), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Negative Shift on start of a day"),

            Arguments.of(Duration.ofSeconds(1000), LocalTime.of(12, 0, 0), LocalDateTime.of(2024, 1, 1, 12, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")), "Large Shift by 1000 seconds"),

            Arguments.of(Duration.ofHours(1), LocalTime.of(23, 59, 59), LocalDateTime.of(2024, 12, 31, 23, 59, 59),
                ZonedDateTime.of(2024, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC")), "Shift Across Year Boundary"),

            Arguments.of(Duration.ofNanos(999_999), LocalTime.of(12, 0, 0, 999_000_000),
                LocalDateTime.of(2024, 1, 1, 12, 0, 0, 999_000_000),
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 999_000_000, ZoneId.of("UTC")), "Milliseconds Edge Case"));
    }

    /**
     * @param testName name of the test. not used besides giving each test a speaking name
     */
    @ParameterizedTest(name = "{4}")
    @MethodSource("provideTestDataForShiftByDurationColumn")
    void canShiftTimeColumnsByDurationColumn(final Duration durationToShift, final LocalTime localTimeBefore,
        final LocalDateTime localDateTimeBefore, final ZonedDateTime zonedDateTimeBefore, final String testName)
        throws IOException, InvalidSettingsException {

        // settings
        final var settings = new TimeShiftNodeSettings();

        settings.m_shiftMode = TimeShiftNodeSettings.ShiftMode.DURATION_COLUMN;
        settings.m_durationColumn = InputTableNode.COLUMN_DURATION;
        settings.m_columnFilter = new ColumnFilter(new String[]{InputTableNode.COLUMN_LOCAL_TIME,
            InputTableNode.COLUMN_LOCAL_DATE_TIME, InputTableNode.COLUMN_ZONED_DATE_TIME});

        setSettings(settings);

        // input table generation
        NamedCell[] cells = { //
            new NamedCell(InputTableNode.COLUMN_DURATION, DurationCellFactory.create(durationToShift)), //
            new NamedCell(InputTableNode.COLUMN_LOCAL_TIME, LocalTimeCellFactory.create(localTimeBefore)),
            new NamedCell(InputTableNode.COLUMN_LOCAL_DATE_TIME, LocalDateTimeCellFactory.create(localDateTimeBefore)),
            new NamedCell(InputTableNode.COLUMN_ZONED_DATE_TIME, ZonedDateTimeCellFactory.create(zonedDateTimeBefore))};

        var inputTableSupplier = InputTableNode.createTestTableSupplier(cells);
        InputTableNode.addTableToNodeInputPort(m_wfm, inputTableSupplier, m_timeShiftNode, 1);

        var inputTable = inputTableSupplier.get();
        var localTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_LOCAL_TIME);
        var localDateTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_LOCAL_DATE_TIME);
        var zonedDateTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_ZONED_DATE_TIME);

        // execute node
        m_wfm.executeAllAndWaitUntilDone();

        // output table and assertion
        var outputTable = (BufferedDataTable)m_timeShiftNode.getOutPort(1).getPortObject();

        var row = InputTableNode.getFirstRow(outputTable);

        var localTimeAfter = ((LocalTimeCell)row.getCell(localTimeIndex).materializeDataCell()).getLocalTime();
        var localDateTimeAfter =
            ((LocalDateTimeCell)row.getCell(localDateTimeIndex).materializeDataCell()).getLocalDateTime();
        var zonedDateTimeAfter =
            ((ZonedDateTimeCell)row.getCell(zonedDateTimeIndex).materializeDataCell()).getZonedDateTime();

        assertEquals(localTimeBefore.plus(durationToShift), localTimeAfter);
        assertEquals(localDateTimeBefore.plus(durationToShift), localDateTimeAfter);
        assertEquals(zonedDateTimeBefore.plus(durationToShift), zonedDateTimeAfter);
    }

    // Supplies parameters for tests that checks shift by numerical column
    static Stream<Arguments> provideTestDataForShiftByNumericalColumn() {
        return Stream.of(
            Arguments.of(TimeGranularity.SECONDS, 1, LocalTime.of(0, 0, 0), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift at Midnight (00:00:00)"),

            Arguments.of(TimeGranularity.SECONDS, 1, LocalTime.of(23, 59, 59), LocalDateTime.of(2024, 1, 1, 23, 59, 59),
                ZonedDateTime.of(2024, 1, 1, 23, 59, 59, 0, ZoneId.of("UTC")), "Shift at End of Day (23:59:59)"),

            Arguments.of(TimeGranularity.HOURS, 13, LocalTime.of(12, 0, 0), LocalDateTime.of(2024, 2, 28, 12, 0, 0),
                ZonedDateTime.of(2024, 2, 28, 12, 0, 0, 0, ZoneId.of("UTC")), "Leap Year Edge Case"),

            Arguments.of(TimeGranularity.HOURS, 1, LocalTime.of(23, 0, 0), LocalDateTime.of(2024, 3, 10, 23, 0, 0),
                ZonedDateTime.of(2024, 3, 10, 23, 0, 0, 0, ZoneId.of("America/New_York")), "DST Transition"),

            Arguments.of(TimeGranularity.MINUTES, 1, LocalTime.of(23, 59, 0), LocalDateTime.of(2024, 1, 1, 23, 59, 0),
                ZonedDateTime.of(2024, 1, 1, 23, 59, 0, 0, ZoneId.of("UTC")), "Shift at End of Hour (23:59:00)"),

            Arguments.of(TimeGranularity.SECONDS, -1, LocalTime.of(12, 0, 0), LocalDateTime.of(2024, 1, 1, 12, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")), "Negative Shift"),

            Arguments.of(TimeGranularity.HOURS, -1, LocalTime.of(0, 0, 0), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Negative Shift on start of a day"),

            Arguments.of(TimeGranularity.SECONDS, 1000, LocalTime.of(12, 0, 0), LocalDateTime.of(2024, 1, 1, 12, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")), "Large Shift by 1000 seconds"),

            Arguments.of(TimeGranularity.HOURS, 1, LocalTime.of(23, 59, 59), LocalDateTime.of(2024, 12, 31, 23, 59, 59),
                ZonedDateTime.of(2024, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC")), "Shift Across Year Boundary"),

            Arguments.of(TimeGranularity.MILLISECONDS, 1, LocalTime.of(12, 0, 0, 999_000_000),
                LocalDateTime.of(2024, 1, 1, 12, 0, 0, 999_000_000),
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 999_000_000, ZoneId.of("UTC")), "Milliseconds Edge Case"));
    }

    /**
     * @param testName name of the test. not used besides giving each test a speaking name
     */
    @ParameterizedTest(name = "{5}")
    @MethodSource("provideTestDataForShiftByNumericalColumn")
    void canShiftTimeColumnsByNumericColumn(final TimeGranularity timeGranularity, final int numericalValueToShift,
        final LocalTime localTimeBefore, final LocalDateTime localDateTimeBefore,
        final ZonedDateTime zonedDateTimeBefore, final String testName) throws IOException, InvalidSettingsException {

        TemporalAmount temporalAmountToShift = timeGranularity.getGranularity().getPeriodOrDuration(numericalValueToShift);

        // settings
        final var settings = new TimeShiftNodeSettings();

        settings.m_shiftMode = TimeShiftNodeSettings.ShiftMode.NUMERICAL_COLUMN;
        settings.m_numericalColumn = InputTableNode.COLUMN_LONG;
        settings.m_granularity = timeGranularity;
        settings.m_columnFilter = new ColumnFilter(new String[]{InputTableNode.COLUMN_LOCAL_TIME,
            InputTableNode.COLUMN_LOCAL_DATE_TIME, InputTableNode.COLUMN_ZONED_DATE_TIME});

        setSettings(settings);

        // input table generation
        NamedCell[] cells = { //
            new NamedCell(InputTableNode.COLUMN_LONG, new LongCell(numericalValueToShift)), //
            new NamedCell(InputTableNode.COLUMN_LOCAL_TIME, LocalTimeCellFactory.create(localTimeBefore)),
            new NamedCell(InputTableNode.COLUMN_LOCAL_DATE_TIME, LocalDateTimeCellFactory.create(localDateTimeBefore)),
            new NamedCell(InputTableNode.COLUMN_ZONED_DATE_TIME, ZonedDateTimeCellFactory.create(zonedDateTimeBefore))};

        var inputTableSupplier = InputTableNode.createTestTableSupplier(cells);
        InputTableNode.addTableToNodeInputPort(m_wfm, inputTableSupplier, m_timeShiftNode, 1);

        var inputTable = inputTableSupplier.get();
        var localTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_LOCAL_TIME);
        var localDateTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_LOCAL_DATE_TIME);
        var zonedDateTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_ZONED_DATE_TIME);

        // execute node
        m_wfm.executeAllAndWaitUntilDone();

        // output table and assertion
        var outputTable = (BufferedDataTable)m_timeShiftNode.getOutPort(1).getPortObject();

        var row = InputTableNode.getFirstRow(outputTable);

        var localTimeAfter = ((LocalTimeCell)row.getCell(localTimeIndex).materializeDataCell()).getLocalTime();
        var localDateTimeAfter =
            ((LocalDateTimeCell)row.getCell(localDateTimeIndex).materializeDataCell()).getLocalDateTime();
        var zonedDateTimeAfter =
            ((ZonedDateTimeCell)row.getCell(zonedDateTimeIndex).materializeDataCell()).getZonedDateTime();

        assertEquals(localTimeBefore.plus(temporalAmountToShift), localTimeAfter);
        assertEquals(localDateTimeBefore.plus(temporalAmountToShift), localDateTimeAfter);
        assertEquals(zonedDateTimeBefore.plus(temporalAmountToShift), zonedDateTimeAfter);
    }

    private void setSettings(final TimeShiftNodeSettings settings) throws InvalidSettingsException {
        final var nodeSettings = new NodeSettings("TimeShiftNode");
        m_wfm.saveNodeSettings(m_timeShiftNode.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(TimeShiftNodeSettings.class, settings, modelSettings);
        m_wfm.loadNodeSettings(m_timeShiftNode.getID(), nodeSettings);
    }

}
