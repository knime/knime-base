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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
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
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.testing.util.WorkflowManagerUtil;
import org.knime.time.node.manipulate.datetimeshift.DateShiftNodeSettings.DateGranularity;
import org.knime.time.util.Granularity;
import org.knime.time.util.ReplaceOrAppend;

@SuppressWarnings("restriction")
class DateShiftNodeModelTest {

    private NativeNodeContainer m_dateShiftNode;

    private WorkflowManager m_wfm;

    @BeforeEach
    void resetWorkflow() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
        m_dateShiftNode = WorkflowManagerUtil.createAndAddNode(m_wfm, new DateShiftNodeFactory());
    }

    @Test
    void canBeExecuted() throws IOException, InvalidSettingsException {

        final var settings = new DateShiftNodeSettings();
        setSettings(settings);

        InputTableNode.addDefaultTableToNodeInputPort(m_wfm, m_dateShiftNode, 1);

        m_wfm.executeAllAndWaitUntilDone();
        assertTrue(m_dateShiftNode.getNodeContainerState().isExecuted());
    }

    @Test
    void appendColumn() throws IOException, InvalidSettingsException {

        var suffixToAdd = "new column suffix";

        final var settings = new DateShiftNodeSettings();
        settings.m_replaceOrAppend = ReplaceOrAppend.APPEND;
        settings.m_outputColumnSuffix = suffixToAdd;
        settings.m_columnFilter = new ColumnFilter(new String[]{InputTableNode.COLUMN_LOCAL_DATE,
            InputTableNode.COLUMN_LOCAL_DATE_TIME, InputTableNode.COLUMN_ZONED_DATE_TIME});
        setSettings(settings);

        InputTableNode.addDefaultTableToNodeInputPort(m_wfm, m_dateShiftNode, 1);

        m_wfm.executeAllAndWaitUntilDone();

        var outputTable = (BufferedDataTable)m_dateShiftNode.getOutPort(1).getPortObject();

        assertTrue(outputTable.getDataTableSpec().containsName(InputTableNode.COLUMN_LOCAL_DATE + suffixToAdd));
        assertTrue(outputTable.getDataTableSpec().containsName(InputTableNode.COLUMN_LOCAL_DATE_TIME + suffixToAdd));
        assertTrue(outputTable.getDataTableSpec().containsName(InputTableNode.COLUMN_ZONED_DATE_TIME + suffixToAdd));
    }

    // Define the method to supply parameters
    static Stream<Arguments> provideTestDataForShiftByPeriodValue() {
        return Stream.of( //
            Arguments.of("P1Y1M1D", LocalDate.of(2024, 1, 1), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift by P1Y1M1D (ISO format)"),

            Arguments.of("1 year 1 month 1 day", LocalDate.of(2024, 1, 1), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
                "Shift by 1 year, 1 month, 1 day (human-readable)"),

            Arguments.of("P1Y6M", LocalDate.of(2024, 1, 1), LocalDateTime.of(2024, 1, 1, 12, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")), "Shift by 1 year 6 months"),

            Arguments.of("3 years 2 months 10 days", LocalDate.of(2021, 1, 1), LocalDateTime.of(2021, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift by 3 years 2 months 10 days"),

            Arguments.of("P0D", LocalDate.of(2024, 1, 1), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift by P0D (no change)"),

            Arguments.of("P1D", LocalDate.of(2023, 12, 31), LocalDateTime.of(2023, 12, 31, 23, 59, 59),
                ZonedDateTime.of(2023, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC")), "Shift crossing new year"),

            Arguments.of("P1M", LocalDate.of(2024, 1, 31), LocalDateTime.of(2024, 1, 31, 12, 0, 0),
                ZonedDateTime.of(2024, 1, 31, 12, 0, 0, 0, ZoneId.of("UTC")), "Shift into shorter month (February)"),

            Arguments.of("-P1M", LocalDate.of(2024, 3, 31), LocalDateTime.of(2024, 3, 31, 12, 0, 0),
                ZonedDateTime.of(2024, 3, 31, 12, 0, 0, 0, ZoneId.of("UTC")),
                "Shift backward into shorter month (February)"),

            Arguments.of("P1Y", LocalDate.of(2020, 2, 29), LocalDateTime.of(2020, 2, 29, 0, 0, 0),
                ZonedDateTime.of(2020, 2, 29, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift leap day forward by 1 year"),

            Arguments.of("-P1Y", LocalDate.of(2024, 2, 29), LocalDateTime.of(2024, 2, 29, 12, 0, 0),
                ZonedDateTime.of(2024, 2, 29, 12, 0, 0, 0, ZoneId.of("UTC")), "Shift leap day backward by 1 year"),

            Arguments.of("P1D", LocalDate.of(2024, 3, 9), LocalDateTime.of(2024, 3, 9, 23, 0, 0),
                ZonedDateTime.of(2024, 3, 9, 23, 0, 0, 0, ZoneId.of("America/New_York")), "Shift across DST start"),

            Arguments.of("-P1D", LocalDate.of(2024, 11, 3), LocalDateTime.of(2024, 11, 3, 1, 0, 0),
                ZonedDateTime.of(2024, 11, 3, 1, 0, 0, 0, ZoneId.of("America/New_York")), "Shift across DST end") //
        );
    }

    /**
     * @param testName name of the test. not used besides giving each test a speaking name
     */
    @ParameterizedTest(name = "{4}")
    @MethodSource("provideTestDataForShiftByPeriodValue")
    void canShiftTimeColumnsByPeriodValue(final String periodValue, final LocalDate localDateBefore,
        final LocalDateTime localDateTimeBefore, final ZonedDateTime zonedDateTimeBefore, final String testName)
        throws IOException, InvalidSettingsException {

        final var settings = new DateShiftNodeSettings();

        settings.m_shiftMode = DateShiftNodeSettings.ShiftMode.SHIFT_VALUE;

        var interval = Interval.parseHumanReadableOrIso(periodValue);
        if (!(interval instanceof DateInterval)) {
            throw new IllegalArgumentException("Interval must be a DateInterval");
        }
        settings.m_shiftPeriodValue = (DateInterval)interval;

        settings.m_columnFilter = new ColumnFilter(new String[]{InputTableNode.COLUMN_LOCAL_DATE,
            InputTableNode.COLUMN_LOCAL_DATE_TIME, InputTableNode.COLUMN_ZONED_DATE_TIME});

        setSettings(settings);

        // input table generation
        NamedCell[] cells = { //
            new NamedCell(InputTableNode.COLUMN_PERIOD, PeriodCellFactory.create(periodValue)), //
            new NamedCell(InputTableNode.COLUMN_LOCAL_DATE, LocalDateCellFactory.create(localDateBefore)),
            new NamedCell(InputTableNode.COLUMN_LOCAL_DATE_TIME, LocalDateTimeCellFactory.create(localDateTimeBefore)),
            new NamedCell(InputTableNode.COLUMN_ZONED_DATE_TIME, ZonedDateTimeCellFactory.create(zonedDateTimeBefore))};

        var inputTableSupplier = InputTableNode.createTestTableSupplier(cells);
        InputTableNode.addTableToNodeInputPort(m_wfm, inputTableSupplier, m_dateShiftNode, 1);

        var inputTable = inputTableSupplier.get();
        var localDateIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_LOCAL_DATE);
        var localDateTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_LOCAL_DATE_TIME);
        var zonedDateTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_ZONED_DATE_TIME);

        // execute node
        assertTrue(m_wfm.executeAllAndWaitUntilDone());

        // output table and assertion
        var outputTable = (BufferedDataTable)m_dateShiftNode.getOutPort(1).getPortObject();

        var row = InputTableNode.getFirstRow(outputTable);

        var localDateAfter = ((LocalDateCell)row.getCell(localDateIndex).materializeDataCell()).getLocalDate();
        var localDateTimeAfter =
            ((LocalDateTimeCell)row.getCell(localDateTimeIndex).materializeDataCell()).getLocalDateTime();
        var zonedDateTimeAfter =
            ((ZonedDateTimeCell)row.getCell(zonedDateTimeIndex).materializeDataCell()).getZonedDateTime();

        assertEquals(localDateBefore.plus(settings.m_shiftPeriodValue), localDateAfter);
        assertEquals(localDateTimeBefore.plus(settings.m_shiftPeriodValue), localDateTimeAfter);
        assertEquals(zonedDateTimeBefore.plus(settings.m_shiftPeriodValue), zonedDateTimeAfter);
    }

    // Define the method to supply parameters
    static Stream<Arguments> provideTestDataForShiftByPeriodColumn() {
        return Stream.of( //
            Arguments.of(Period.ofYears(1), LocalDate.of(2024, 1, 1), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift by 1 year"),

            Arguments.of(Period.ofMonths(1), LocalDate.of(2024, 1, 1), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift by 1 month"),

            Arguments.of(Period.ofWeeks(6), LocalDate.of(2024, 1, 1), LocalDateTime.of(2024, 1, 1, 12, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")), "Shift by 6 weeks"),

            Arguments.of(Period.ofDays(0), LocalDate.of(2024, 1, 1), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift by 0 days (no change)"),

            Arguments.of(Period.ofDays(0), LocalDate.of(2023, 12, 31), LocalDateTime.of(2023, 12, 31, 23, 59, 59),
                ZonedDateTime.of(2023, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC")), "Shift crossing new year"),

            Arguments.of(Period.ofMonths(1), LocalDate.of(2024, 1, 31), LocalDateTime.of(2024, 1, 31, 12, 0, 0),
                ZonedDateTime.of(2024, 1, 31, 12, 0, 0, 0, ZoneId.of("UTC")), "Shift into shorter month (February)"),

            Arguments.of(Period.ofMonths(-1), LocalDate.of(2024, 3, 31), LocalDateTime.of(2024, 3, 31, 12, 0, 0),
                ZonedDateTime.of(2024, 3, 31, 12, 0, 0, 0, ZoneId.of("UTC")),
                "Shift backward into shorter month (February)"),

            Arguments.of(Period.ofYears(1), LocalDate.of(2020, 2, 29), LocalDateTime.of(2020, 2, 29, 0, 0, 0),
                ZonedDateTime.of(2020, 2, 29, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift leap day forward by 1 year"),

            Arguments.of(Period.ofYears(-1), LocalDate.of(2024, 2, 29), LocalDateTime.of(2024, 2, 29, 12, 0, 0),
                ZonedDateTime.of(2024, 2, 29, 12, 0, 0, 0, ZoneId.of("UTC")), "Shift leap day backward by 1 year"),

            Arguments.of(Period.ofDays(1), LocalDate.of(2024, 3, 9), LocalDateTime.of(2024, 3, 9, 23, 0, 0),
                ZonedDateTime.of(2024, 3, 9, 23, 0, 0, 0, ZoneId.of("America/New_York")), "Shift across DST start"),

            Arguments.of(Period.ofDays(-1), LocalDate.of(2024, 11, 3), LocalDateTime.of(2024, 11, 3, 1, 0, 0),
                ZonedDateTime.of(2024, 11, 3, 1, 0, 0, 0, ZoneId.of("America/New_York")), "Shift across DST end"),

            Arguments.of(Period.ofDays(1010), LocalDate.of(2021, 1, 1), LocalDateTime.of(2021, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift by 1010 days"));
    }

    /**
     * @param testName name of the test. not used besides giving each test a speaking name
     */
    @ParameterizedTest(name = "{4}")
    @MethodSource("provideTestDataForShiftByPeriodColumn")
    void canShiftTimeColumnsByPeriodColumn(final Period periodToShift, final LocalDate localDateBefore,
        final LocalDateTime localDateTimeBefore, final ZonedDateTime zonedDateTimeBefore, final String testName)
        throws IOException, InvalidSettingsException {

        // settings
        final var settings = new DateShiftNodeSettings();

        settings.m_shiftMode = DateShiftNodeSettings.ShiftMode.PERIOD_COLUMN;
        settings.m_periodColumn = InputTableNode.COLUMN_PERIOD;
        settings.m_columnFilter = new ColumnFilter(new String[]{InputTableNode.COLUMN_LOCAL_DATE,
            InputTableNode.COLUMN_LOCAL_DATE_TIME, InputTableNode.COLUMN_ZONED_DATE_TIME});

        setSettings(settings);

        // input table generation
        NamedCell[] cells = { //
            new NamedCell(InputTableNode.COLUMN_PERIOD, PeriodCellFactory.create(periodToShift)), //
            new NamedCell(InputTableNode.COLUMN_LOCAL_DATE, LocalDateCellFactory.create(localDateBefore)),
            new NamedCell(InputTableNode.COLUMN_LOCAL_DATE_TIME, LocalDateTimeCellFactory.create(localDateTimeBefore)),
            new NamedCell(InputTableNode.COLUMN_ZONED_DATE_TIME, ZonedDateTimeCellFactory.create(zonedDateTimeBefore))};

        var inputTableSupplier = InputTableNode.createTestTableSupplier(cells);
        InputTableNode.addTableToNodeInputPort(m_wfm, inputTableSupplier, m_dateShiftNode, 1);

        var inputTable = inputTableSupplier.get();
        var localDateIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_LOCAL_DATE);
        var localDateTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_LOCAL_DATE_TIME);
        var zonedDateTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_ZONED_DATE_TIME);

        // execute node
        m_wfm.executeAllAndWaitUntilDone();

        // output table and assertion
        var outputTable = (BufferedDataTable)m_dateShiftNode.getOutPort(1).getPortObject();

        var row = InputTableNode.getFirstRow(outputTable);

        var localDateAfter = ((LocalDateCell)row.getCell(localDateIndex).materializeDataCell()).getLocalDate();
        var localDateTimeAfter =
            ((LocalDateTimeCell)row.getCell(localDateTimeIndex).materializeDataCell()).getLocalDateTime();
        var zonedDateTimeAfter =
            ((ZonedDateTimeCell)row.getCell(zonedDateTimeIndex).materializeDataCell()).getZonedDateTime();

        assertEquals(localDateBefore.plus(periodToShift), localDateAfter);
        assertEquals(localDateTimeBefore.plus(periodToShift), localDateTimeAfter);
        assertEquals(zonedDateTimeBefore.plus(periodToShift), zonedDateTimeAfter);
    }

    // Supplies parameters for tests that checks shift by numerical column
    static Stream<Arguments> provideTestDataForShiftByNumericalColumn() {

        return Stream.of(
            Arguments.of(DateGranularity.YEARS, 1, LocalDate.of(2024, 1, 1), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift by 1 year"),

            Arguments.of(DateGranularity.MONTHS, 1, LocalDate.of(2024, 1, 1), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift by 1 month"),

            Arguments.of(DateGranularity.WEEKS, 6, LocalDate.of(2024, 1, 1), LocalDateTime.of(2024, 1, 1, 12, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC")), "Shift by 6 weeks"),

            Arguments.of(DateGranularity.DAYS, 0, LocalDate.of(2024, 1, 1), LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift by 0 days (no change)"),

            Arguments.of(DateGranularity.DAYS, 1, LocalDate.of(2023, 12, 31), LocalDateTime.of(2023, 12, 31, 23, 59, 59),
                ZonedDateTime.of(2023, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC")), "Shift crossing new year"),

            Arguments.of(DateGranularity.MONTHS, 1, LocalDate.of(2024, 1, 31), LocalDateTime.of(2024, 1, 31, 12, 0, 0),
                ZonedDateTime.of(2024, 1, 31, 12, 0, 0, 0, ZoneId.of("UTC")), "Shift into shorter month (February)"),

            Arguments.of(DateGranularity.MONTHS, -1, LocalDate.of(2024, 3, 31), LocalDateTime.of(2024, 3, 31, 12, 0, 0),
                ZonedDateTime.of(2024, 3, 31, 12, 0, 0, 0, ZoneId.of("UTC")),
                "Shift backward into shorter month (February)"),

            Arguments.of(DateGranularity.YEARS, 1, LocalDate.of(2020, 2, 29), LocalDateTime.of(2020, 2, 29, 0, 0, 0),
                ZonedDateTime.of(2020, 2, 29, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift leap day forward by 1 year"),

            Arguments.of(DateGranularity.YEARS, -1, LocalDate.of(2024, 2, 29), LocalDateTime.of(2024, 2, 29, 12, 0, 0),
                ZonedDateTime.of(2024, 2, 29, 12, 0, 0, 0, ZoneId.of("UTC")), "Shift leap day backward by 1 year"),

            Arguments.of(DateGranularity.DAYS, 1, LocalDate.of(2024, 3, 9), LocalDateTime.of(2024, 3, 9, 23, 0, 0),
                ZonedDateTime.of(2024, 3, 9, 23, 0, 0, 0, ZoneId.of("America/New_York")), "Shift across DST start"),

            Arguments.of(DateGranularity.DAYS, -1, LocalDate.of(2024, 11, 3), LocalDateTime.of(2024, 11, 3, 1, 0, 0),
                ZonedDateTime.of(2024, 11, 3, 1, 0, 0, 0, ZoneId.of("America/New_York")), "Shift across DST end"),

            Arguments.of(DateGranularity.DAYS, 1010, LocalDate.of(2021, 1, 1), LocalDateTime.of(2021, 1, 1, 0, 0, 0),
                ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "Shift by 1010 days"));
    }

    /**
     * @param testName name of the test. not used besides giving each test a speaking name
     */
    @ParameterizedTest(name = "{5}")
    @MethodSource("provideTestDataForShiftByNumericalColumn")
    void canShiftTimeColumnsByNumericColumn(final DateGranularity dateGranularity, final int numericalValueToShift,
        final LocalDate localDateBefore, final LocalDateTime localDateTimeBefore,
        final ZonedDateTime zonedDateTimeBefore, final String testName) throws IOException, InvalidSettingsException {

        Granularity granularity = dateGranularity.getGranularity();
        TemporalAmount temporalAmountToShift = granularity.getPeriodOrDuration(numericalValueToShift);

        // settings
        final var settings = new DateShiftNodeSettings();

        settings.m_shiftMode = DateShiftNodeSettings.ShiftMode.NUMERICAL_COLUMN;
        settings.m_numericalColumn = InputTableNode.COLUMN_LONG;
        settings.m_granularity = dateGranularity;
        settings.m_replaceOrAppend = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{InputTableNode.COLUMN_LOCAL_DATE,
            InputTableNode.COLUMN_LOCAL_DATE_TIME, InputTableNode.COLUMN_ZONED_DATE_TIME});

        setSettings(settings);

        // input table generation
        NamedCell[] cells = { //
            new NamedCell(InputTableNode.COLUMN_LONG, new LongCell(numericalValueToShift)), //
            new NamedCell(InputTableNode.COLUMN_LOCAL_DATE, LocalDateCellFactory.create(localDateBefore)),
            new NamedCell(InputTableNode.COLUMN_LOCAL_DATE_TIME, LocalDateTimeCellFactory.create(localDateTimeBefore)),
            new NamedCell(InputTableNode.COLUMN_ZONED_DATE_TIME, ZonedDateTimeCellFactory.create(zonedDateTimeBefore))};

        var inputTableSupplier = InputTableNode.createTestTableSupplier(cells);
        InputTableNode.addTableToNodeInputPort(m_wfm, inputTableSupplier, m_dateShiftNode, 1);

        var inputTable = inputTableSupplier.get();
        var localDateIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_LOCAL_DATE);
        var localDateTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_LOCAL_DATE_TIME);
        var zonedDateTimeIndex = inputTable.getDataTableSpec().findColumnIndex(InputTableNode.COLUMN_ZONED_DATE_TIME);

        // execute node
        m_wfm.executeAllAndWaitUntilDone();

        // output table and assertion
        var outputTable = (BufferedDataTable)m_dateShiftNode.getOutPort(1).getPortObject();

        var row = InputTableNode.getFirstRow(outputTable);

        var localDateAfter = ((LocalDateCell)row.getCell(localDateIndex).materializeDataCell()).getLocalDate();
        var localDateTimeAfter =
            ((LocalDateTimeCell)row.getCell(localDateTimeIndex).materializeDataCell()).getLocalDateTime();
        var zonedDateTimeAfter =
            ((ZonedDateTimeCell)row.getCell(zonedDateTimeIndex).materializeDataCell()).getZonedDateTime();

        assertEquals(localDateBefore.plus(temporalAmountToShift), localDateAfter);
        assertEquals(localDateTimeBefore.plus(temporalAmountToShift), localDateTimeAfter);
        assertEquals(zonedDateTimeBefore.plus(temporalAmountToShift), zonedDateTimeAfter);
    }

    private void setSettings(final DateShiftNodeSettings settings) throws InvalidSettingsException {
        final var nodeSettings = new NodeSettings("DateShiftNode");
        m_wfm.saveNodeSettings(m_dateShiftNode.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(DateShiftNodeSettings.class, settings, modelSettings);
        m_wfm.loadNodeSettings(m_dateShiftNode.getID(), nodeSettings);
    }

}
