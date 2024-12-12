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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalQueries;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.InputTableNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;
import org.knime.time.node.calculate.datetimedifference.DateTimeDifferenceNodeSettings.Mode;
import org.knime.time.node.calculate.datetimedifference.DateTimeDifferenceNodeSettings.OutputNumberType;
import org.knime.time.node.calculate.datetimedifference.DateTimeDifferenceNodeSettings.OutputType;
import org.knime.time.node.calculate.datetimedifference.DateTimeDifferenceNodeSettings.SecondDateTimeValueType;
import org.knime.time.util.Granularity;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings({"static-method", "restriction"})
final class DateTimeDifferenceNodeModel2Test {

    private static final String NODE_NAME = "DateTimeDifferenceNode";

    private static Object[] createTestRowData(final Temporal... objs) {
        return Arrays.stream(objs).map(s -> {
            if (s instanceof ZonedDateTime zdt) {
                return ZonedDateTimeCellFactory.create(zdt);
            } else if (s instanceof LocalDateTime ldt) {
                return LocalDateTimeCellFactory.create(ldt);
            } else if (s instanceof LocalDate ld) {
                return LocalDateCellFactory.create(ld);
            } else if (s instanceof LocalTime lt) {
                return LocalTimeCellFactory.create(lt);
            }

            throw new IllegalArgumentException("Could not parse " + s);
        }).toArray();
    }

    private static final DataTableSpec TEST_DATA_TABLE_SPEC = new TableTestUtil.SpecBuilder() //
        .addColumn("localTime1", LocalTimeCellFactory.TYPE) //
        .addColumn("localTime2", LocalTimeCellFactory.TYPE) //
        .addColumn("localDate1", LocalDateCellFactory.TYPE) //
        .addColumn("localDate2", LocalDateCellFactory.TYPE) //
        .addColumn("localDateTime1", LocalDateTimeCellFactory.TYPE) //
        .addColumn("localDateTime2", LocalDateTimeCellFactory.TYPE) //
        .addColumn("zonedDateTime1", ZonedDateTimeCellFactory.TYPE) //
        .addColumn("zonedDateTime2", ZonedDateTimeCellFactory.TYPE) //
        .build();

    private static final LocalDate BASE_DATE = LocalDate.EPOCH;

    private static final LocalTime BASE_TIME = LocalTime.MIDNIGHT;

    private static final LocalDateTime BASE_DATE_TIME = LocalDateTime.of(BASE_DATE, BASE_TIME);

    private static final ZonedDateTime BASE_ZONED_DATE_TIME = ZonedDateTime.of(BASE_DATE_TIME, ZoneId.of("UTC"));

    private static final Duration BASE_DURATION_INCREMENT = Duration.ofSeconds(1);

    private static final Period BASE_PERIOD_INCREMENT = Period.ofDays(1);

    private static final ZonedDateTime SECOND_ZONED_DATE_TIME =
        ZonedDateTime.of(LocalDate.EPOCH.plusDays(7), LocalTime.MIDNIGHT.plusHours(5), ZoneId.of("Europe/Berlin"));

    private static final Duration ZONED_DIFFERENCE = Duration.between(BASE_ZONED_DATE_TIME, SECOND_ZONED_DATE_TIME);

    private static final Supplier<BufferedDataTable> TEST_DATA_TABLE =
        new TableTestUtil.TableBuilder(TEST_DATA_TABLE_SPEC) //
            .addRow(createTestRowData(BASE_TIME, BASE_TIME, BASE_DATE, BASE_DATE, BASE_DATE_TIME, BASE_DATE_TIME,
                BASE_ZONED_DATE_TIME, BASE_ZONED_DATE_TIME)) //
            .addRow(createTestRowData(BASE_TIME, BASE_TIME.plus(BASE_DURATION_INCREMENT), BASE_DATE,
                BASE_DATE.plus(BASE_PERIOD_INCREMENT), BASE_DATE_TIME, BASE_DATE_TIME.plus(BASE_DURATION_INCREMENT),
                BASE_ZONED_DATE_TIME, SECOND_ZONED_DATE_TIME)) //
            .build();

    static record PreviousRowTestCaseWithTemporalOutput(String name, int column, TemporalAmount expected) {
    }

    static record PreviousRowTestCaseWithDecimalOutput(String name, int column, Granularity unit, double expected) {
    }

    static record PreviousRowTestCaseWithIntegerOutput(String name, int column, Granularity unit, long expected) {
    }

    static record TwoColumnsInSameRowTestCaseWithTemporalOutput(String name, int firstColumn, TemporalAmount expected) {
    }

    static record TwoColumnsInSameRowTestCaseWithDecimalOutput(String name, int firstColumn, Granularity unit,
        double expected) {
    }

    static record TwoColumnsInSameRowTestCaseWithIntegerOutput(String name, int firstColumn, Granularity unit,
        long expected) {
    }

    static record OneColumnAndFixedValueTestCaseWithTemporalOutput(String name, int column, Temporal fixedValue,
        TemporalAmount expected) {
    }

    static record OneColumnAndFixedValueTestCaseWithDecimalOutput(String name, int column, Temporal fixedValue,
        Granularity unit, double expected) {
    }

    static record OneColumnAndFixedValueTestCaseWithIntegerOutput(String name, int column, Temporal fixedValue,
        Granularity unit, long expected) {
    }

    static Stream<Arguments> providePreviousRowTestCasesWithTemporalOutput() {
        return Stream.of( //
            new PreviousRowTestCaseWithTemporalOutput("localTime", 1, BASE_DURATION_INCREMENT), //
            new PreviousRowTestCaseWithTemporalOutput("localDate", 3, BASE_PERIOD_INCREMENT), //
            new PreviousRowTestCaseWithTemporalOutput("localDateTime", 5, BASE_DURATION_INCREMENT), //
            new PreviousRowTestCaseWithTemporalOutput("zonedDateTime", 7, ZONED_DIFFERENCE) //
        ).map(tc -> Stream.of( //
            Arguments.of(tc.name + " (not negated)", tc.column, tc.expected, false), //
            Arguments.of(tc.name + " (negated)", tc.column, negate(tc.expected), true) //
        )).flatMap(Function.identity());
    }

    static Stream<Arguments> providePreviousRowTestCasesWithDecimalOutput() {
        return Stream.of( //
            new PreviousRowTestCaseWithDecimalOutput("localTime - seconds", 1, Granularity.SECOND,
                BASE_DURATION_INCREMENT.getSeconds()), //
            new PreviousRowTestCaseWithDecimalOutput("localTime - minutes", 1, Granularity.MINUTE,
                BASE_DURATION_INCREMENT.getSeconds() / 60.0), //
            new PreviousRowTestCaseWithDecimalOutput("localDateTime - seconds", 5, Granularity.SECOND,
                BASE_DURATION_INCREMENT.getSeconds()), //
            new PreviousRowTestCaseWithDecimalOutput("localDateTime - minutes", 5, Granularity.MINUTE,
                BASE_DURATION_INCREMENT.getSeconds() / 60.0), //
            new PreviousRowTestCaseWithDecimalOutput("zonedDateTime - seconds", 7, Granularity.SECOND,
                ZONED_DIFFERENCE.getSeconds()), //
            new PreviousRowTestCaseWithDecimalOutput("zonedDateTime - minutes", 7, Granularity.MINUTE,
                ZONED_DIFFERENCE.getSeconds() / 60.0) //
        ).map(tc -> Stream.of( //
            Arguments.of(tc.name + " (not negated)", tc.column, tc.unit, tc.expected, false), //
            Arguments.of(tc.name + " (negated)", tc.column, tc.unit, -tc.expected, true) //
        )).flatMap(Function.identity());
    }

    static Stream<Arguments> providePreviousRowTestCasesWithIntegerOutput() {
        return Stream.of( //
            new PreviousRowTestCaseWithIntegerOutput("localTime - seconds", 1, Granularity.SECOND,
                BASE_DURATION_INCREMENT.getSeconds()), //
            new PreviousRowTestCaseWithIntegerOutput("localTime - minutes", 1, Granularity.MINUTE, 0), //
            new PreviousRowTestCaseWithIntegerOutput("localTime - milliseconds", 1, Granularity.MILLISECOND,
                BASE_DURATION_INCREMENT.toMillis()), //
            new PreviousRowTestCaseWithIntegerOutput("localDate - days", 3, Granularity.DAY,
                BASE_PERIOD_INCREMENT.getDays()), //
            new PreviousRowTestCaseWithIntegerOutput("localDate - months", 3, Granularity.MONTH, 0), //
            new PreviousRowTestCaseWithIntegerOutput("localDateTime - seconds", 5, Granularity.SECOND,
                BASE_DURATION_INCREMENT.getSeconds()), //
            new PreviousRowTestCaseWithIntegerOutput("localDateTime - minutes", 5, Granularity.MINUTE, 0), //
            new PreviousRowTestCaseWithIntegerOutput("localDateTime - milliseconds", 5, Granularity.MILLISECOND,
                BASE_DURATION_INCREMENT.toMillis()), //
            new PreviousRowTestCaseWithIntegerOutput("zonedDateTime - seconds", 7, Granularity.SECOND,
                ZONED_DIFFERENCE.getSeconds()), //
            new PreviousRowTestCaseWithIntegerOutput("zonedDateTime - minutes", 7, Granularity.MINUTE,
                ZONED_DIFFERENCE.toMinutes()), //
            new PreviousRowTestCaseWithIntegerOutput("zonedDateTime - milliseconds", 7, Granularity.MILLISECOND,
                ZONED_DIFFERENCE.toMillis()) //
        ).map(tc -> Stream.of( //
            Arguments.of(tc.name + " (not negated)", tc.column, tc.unit, tc.expected, false), //
            Arguments.of(tc.name + " (negated)", tc.column, tc.unit, -tc.expected, true) //
        )).flatMap(Function.identity());
    }

    static Stream<Arguments> provideTwoColumnsInSameRowTestCasesWithTemporalOutput() {
        return Stream.of( //
            new TwoColumnsInSameRowTestCaseWithTemporalOutput("localTime", 1, BASE_DURATION_INCREMENT), //
            new TwoColumnsInSameRowTestCaseWithTemporalOutput("localDate", 3, BASE_PERIOD_INCREMENT), //
            new TwoColumnsInSameRowTestCaseWithTemporalOutput("localDateTime", 5, BASE_DURATION_INCREMENT), //
            new TwoColumnsInSameRowTestCaseWithTemporalOutput("zonedDateTime", 7, ZONED_DIFFERENCE) //
        ).map(tc -> Stream.of( //
            Arguments.of(tc.name + " (not negated)", tc.firstColumn, tc.expected, false), //
            Arguments.of(tc.name + " (negated)", tc.firstColumn, negate(tc.expected), true) //
        )).flatMap(Function.identity());
    }

    static Stream<Arguments> provideTwoColumnsInSameRowTestCasesWithDecimalOutput() {
        return Stream.of( //
            new TwoColumnsInSameRowTestCaseWithDecimalOutput("localTime - seconds", 1, Granularity.SECOND,
                BASE_DURATION_INCREMENT.getSeconds()), //
            new TwoColumnsInSameRowTestCaseWithDecimalOutput("localTime - minutes", 1, Granularity.MINUTE,
                BASE_DURATION_INCREMENT.getSeconds() / 60.0), //
            new TwoColumnsInSameRowTestCaseWithDecimalOutput("localDateTime - seconds", 5, Granularity.SECOND,
                BASE_DURATION_INCREMENT.getSeconds()), //
            new TwoColumnsInSameRowTestCaseWithDecimalOutput("localDateTime - minutes", 5, Granularity.MINUTE,
                BASE_DURATION_INCREMENT.getSeconds() / 60.0), //
            new TwoColumnsInSameRowTestCaseWithDecimalOutput("zonedDateTime - seconds", 7, Granularity.SECOND,
                ZONED_DIFFERENCE.getSeconds()), //
            new TwoColumnsInSameRowTestCaseWithDecimalOutput("zonedDateTime - minutes", 7, Granularity.MINUTE,
                ZONED_DIFFERENCE.getSeconds() / 60.0) //
        ).map(tc -> Stream.of( //
            Arguments.of(tc.name + " (not negated)", tc.firstColumn, tc.unit, tc.expected, false), //
            Arguments.of(tc.name + " (negated)", tc.firstColumn, tc.unit, -tc.expected, true) //
        )).flatMap(Function.identity());
    }

    static Stream<Arguments> provideTwoColumnsInSameRowTestCasesWithIntegerOutput() {
        return Stream.of( //
            new TwoColumnsInSameRowTestCaseWithIntegerOutput("localTime - seconds", 1, Granularity.SECOND,
                BASE_DURATION_INCREMENT.getSeconds()), //
            new TwoColumnsInSameRowTestCaseWithIntegerOutput("localTime - minutes", 1, Granularity.MINUTE, 0), //
            new TwoColumnsInSameRowTestCaseWithIntegerOutput("localTime - milliseconds", 1, Granularity.MILLISECOND,
                BASE_DURATION_INCREMENT.toMillis()), //
            new TwoColumnsInSameRowTestCaseWithIntegerOutput("localDate - days", 3, Granularity.DAY,
                BASE_PERIOD_INCREMENT.getDays()), //
            new TwoColumnsInSameRowTestCaseWithIntegerOutput("localDate - months", 3, Granularity.MONTH, 0), //
            new TwoColumnsInSameRowTestCaseWithIntegerOutput("localDateTime - seconds", 5, Granularity.SECOND,
                BASE_DURATION_INCREMENT.getSeconds()), //
            new TwoColumnsInSameRowTestCaseWithIntegerOutput("localDateTime - minutes", 5, Granularity.MINUTE, 0), //
            new TwoColumnsInSameRowTestCaseWithIntegerOutput("localDateTime - milliseconds", 5, Granularity.MILLISECOND,
                BASE_DURATION_INCREMENT.toMillis()), //
            new TwoColumnsInSameRowTestCaseWithIntegerOutput("zonedDateTime - seconds", 7, Granularity.SECOND,
                ZONED_DIFFERENCE.getSeconds()), //
            new TwoColumnsInSameRowTestCaseWithIntegerOutput("zonedDateTime - minutes", 7, Granularity.MINUTE,
                ZONED_DIFFERENCE.toMinutes()), //
            new TwoColumnsInSameRowTestCaseWithIntegerOutput("zonedDateTime - milliseconds", 7, Granularity.MILLISECOND,
                ZONED_DIFFERENCE.toMillis()) //
        ).map(tc -> Stream.of( //
            Arguments.of(tc.name + " (not negated)", tc.firstColumn, tc.unit, tc.expected, false), //
            Arguments.of(tc.name + " (negated)", tc.firstColumn, tc.unit, -tc.expected, true) //
        )).flatMap(Function.identity());
    }

    static Stream<Arguments> provideOneColumnAndFixedValueTestCasesWithTemporalOutput() {
        return Stream.of( //
            new OneColumnAndFixedValueTestCaseWithTemporalOutput("localTime", 1, BASE_TIME,
                BASE_DURATION_INCREMENT.negated()), //
            new OneColumnAndFixedValueTestCaseWithTemporalOutput("localDate", 3, BASE_DATE,
                BASE_PERIOD_INCREMENT.negated()), //
            new OneColumnAndFixedValueTestCaseWithTemporalOutput("localDateTime", 5, BASE_DATE_TIME,
                BASE_DURATION_INCREMENT.negated()), //
            new OneColumnAndFixedValueTestCaseWithTemporalOutput("zonedDateTime", 7, BASE_ZONED_DATE_TIME,
                ZONED_DIFFERENCE.negated()) //
        ).map(tc -> Stream.of( //
            Arguments.of(tc.name + " (not negated)", tc.column, tc.fixedValue, tc.expected, false), //
            Arguments.of(tc.name + " (negated)", tc.column, tc.fixedValue, negate(tc.expected), true) //
        )).flatMap(Function.identity());
    }

    static Stream<Arguments> provideOneColumnAndFixedValueTestCasesWithDecimalOutput() {
        return Stream.of( //
            new OneColumnAndFixedValueTestCaseWithDecimalOutput("localTime - seconds", 1, BASE_TIME, Granularity.SECOND,
                -BASE_DURATION_INCREMENT.getSeconds()), //
            new OneColumnAndFixedValueTestCaseWithDecimalOutput("localTime - minutes", 1, BASE_TIME, Granularity.MINUTE,
                -BASE_DURATION_INCREMENT.getSeconds() / 60.0), //
            new OneColumnAndFixedValueTestCaseWithDecimalOutput("localDateTime - seconds", 5, BASE_DATE_TIME,
                Granularity.SECOND, -BASE_DURATION_INCREMENT.getSeconds()), //
            new OneColumnAndFixedValueTestCaseWithDecimalOutput("localDateTime - minutes", 5, BASE_DATE_TIME,
                Granularity.MINUTE, -BASE_DURATION_INCREMENT.getSeconds() / 60.0), //
            new OneColumnAndFixedValueTestCaseWithDecimalOutput("zonedDateTime - seconds", 7, BASE_ZONED_DATE_TIME,
                Granularity.SECOND, -ZONED_DIFFERENCE.getSeconds()), //
            new OneColumnAndFixedValueTestCaseWithDecimalOutput("zonedDateTime - minutes", 7, BASE_ZONED_DATE_TIME,
                Granularity.MINUTE, -ZONED_DIFFERENCE.getSeconds() / 60.0) //
        ).map(tc -> Stream.of( //
            Arguments.of(tc.name + " (not negated)", tc.column, tc.fixedValue, tc.unit, tc.expected, false), //
            Arguments.of(tc.name + " (negated)", tc.column, tc.fixedValue, tc.unit, -tc.expected, true) //
        )).flatMap(Function.identity());
    }

    static Stream<Arguments> provideOneColumnAndFixedValueTestCasesWithIntegerOutput() {
        return Stream.of( //
            new OneColumnAndFixedValueTestCaseWithIntegerOutput("localTime - seconds", 1, BASE_TIME, Granularity.SECOND,
                -BASE_DURATION_INCREMENT.getSeconds()), //
            new OneColumnAndFixedValueTestCaseWithIntegerOutput("localTime - minutes", 1, BASE_TIME, Granularity.MINUTE,
                0), //
            new OneColumnAndFixedValueTestCaseWithIntegerOutput("localTime - milliseconds", 1, BASE_TIME,
                Granularity.MILLISECOND, -BASE_DURATION_INCREMENT.toMillis()), //
            new OneColumnAndFixedValueTestCaseWithIntegerOutput("localDate - days", 3, BASE_DATE, Granularity.DAY,
                -BASE_PERIOD_INCREMENT.getDays()), //
            new OneColumnAndFixedValueTestCaseWithIntegerOutput("localDate - months", 3, BASE_DATE, Granularity.MONTH,
                0), //
            new OneColumnAndFixedValueTestCaseWithIntegerOutput("localDateTime - seconds", 5, BASE_DATE_TIME,
                Granularity.SECOND, -BASE_DURATION_INCREMENT.getSeconds()), //
            new OneColumnAndFixedValueTestCaseWithIntegerOutput("localDateTime - minutes", 5, BASE_DATE_TIME,
                Granularity.MINUTE, 0), //
            new OneColumnAndFixedValueTestCaseWithIntegerOutput("localDateTime - milliseconds", 5, BASE_DATE_TIME,
                Granularity.MILLISECOND, -BASE_DURATION_INCREMENT.toMillis()),
            new OneColumnAndFixedValueTestCaseWithIntegerOutput("zonedDateTime - seconds", 7, BASE_ZONED_DATE_TIME,
                Granularity.SECOND, -ZONED_DIFFERENCE.getSeconds()), //
            new OneColumnAndFixedValueTestCaseWithIntegerOutput("zonedDateTime - minutes", 7, BASE_ZONED_DATE_TIME,
                Granularity.MINUTE, -ZONED_DIFFERENCE.toMinutes()), //
            new OneColumnAndFixedValueTestCaseWithIntegerOutput("zonedDateTime - milliseconds", 7, BASE_ZONED_DATE_TIME,
                Granularity.MILLISECOND, -ZONED_DIFFERENCE.toMillis()) //
        ).map(tc -> Stream.of( //
            Arguments.of(tc.name + " (not negated)", tc.column, tc.fixedValue, tc.unit, tc.expected, false), //
            Arguments.of(tc.name + " (negated)", tc.column, tc.fixedValue, tc.unit, -tc.expected, true) //
        )).flatMap(Function.identity());
    }

    @MethodSource("providePreviousRowTestCasesWithTemporalOutput")
    @ParameterizedTest(name = "{0}")
    void testPreviousRowWithTemporalOutput(@SuppressWarnings("unused") final String name, final int column,
        final TemporalAmount expected, final boolean negated) throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();

        settings.m_firstColumnSelection = new ColumnSelection(TEST_DATA_TABLE_SPEC.getColumnSpec(column));
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.PREVIOUS_ROW;
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;

        var setup = setupAndExecuteWorkflow(settings);
        var temporalAmount = extractTemporalAmountFromDataCell(setup.lastAppendedCell);

        assertEquals(expected, temporalAmount, "expected correct temporal amount");
    }

    @MethodSource("providePreviousRowTestCasesWithDecimalOutput")
    @ParameterizedTest(name = "{0}")
    void testPreviousRowWithDecimalOutput(@SuppressWarnings("unused") final String name, final int column,
        final Granularity unit, final double expected, final boolean negated)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();

        settings.m_firstColumnSelection = new ColumnSelection(TEST_DATA_TABLE_SPEC.getColumnSpec(column));
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.PREVIOUS_ROW;
        settings.m_outputType = OutputType.NUMBER;
        settings.m_outputNumberType = OutputNumberType.DECIMALS;
        settings.m_granularity = unit;

        var setup = setupAndExecuteWorkflow(settings);
        var amount = ((DoubleValue)setup.lastAppendedCell).getDoubleValue();

        assertEquals(expected, amount, 1e-10, "expected correct decimal amount");
    }

    @MethodSource("providePreviousRowTestCasesWithIntegerOutput")
    @ParameterizedTest(name = "{0}")
    void testPreviousRowWithIntegerOutput(@SuppressWarnings("unused") final String name, final int column,
        final Granularity unit, final long expected, final boolean negated)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();

        settings.m_firstColumnSelection = new ColumnSelection(TEST_DATA_TABLE_SPEC.getColumnSpec(column));
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.PREVIOUS_ROW;
        settings.m_outputType = OutputType.NUMBER;
        settings.m_outputNumberType = OutputNumberType.NO_DECIMALS;
        settings.m_granularity = unit;

        var setup = setupAndExecuteWorkflow(settings);
        var amount = ((LongValue)setup.lastAppendedCell).getLongValue();

        assertEquals(expected, amount, "expected correct integer amount");
    }

    @MethodSource("provideTwoColumnsInSameRowTestCasesWithTemporalOutput")
    @ParameterizedTest(name = "{0}")
    void testTwoColumnsInSameRowWithTemporalOutput(@SuppressWarnings("unused") final String name, final int firstColumn,
        final TemporalAmount expected, final boolean negated) throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();

        settings.m_firstColumnSelection = new ColumnSelection(TEST_DATA_TABLE_SPEC.getColumnSpec(firstColumn - 1));
        settings.m_secondColumnSelection = new ColumnSelection(TEST_DATA_TABLE_SPEC.getColumnSpec(firstColumn));
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;

        var setup = setupAndExecuteWorkflow(settings);
        var temporalAmount = extractTemporalAmountFromDataCell(setup.lastAppendedCell);

        assertEquals(expected, temporalAmount, "expected correct temporal amount");
    }

    @MethodSource("provideTwoColumnsInSameRowTestCasesWithDecimalOutput")
    @ParameterizedTest(name = "{0}")
    void testTwoColumnsInSameRowWithDecimalOutput(@SuppressWarnings("unused") final String name, final int firstColumn,
        final Granularity unit, final double expected, final boolean negated)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();

        settings.m_firstColumnSelection = new ColumnSelection(TEST_DATA_TABLE_SPEC.getColumnSpec(firstColumn - 1));
        settings.m_secondColumnSelection = new ColumnSelection(TEST_DATA_TABLE_SPEC.getColumnSpec(firstColumn));
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;
        settings.m_outputType = OutputType.NUMBER;
        settings.m_outputNumberType = OutputNumberType.DECIMALS;
        settings.m_granularity = unit;

        var setup = setupAndExecuteWorkflow(settings);
        var amount = ((DoubleValue)setup.lastAppendedCell).getDoubleValue();

        assertEquals(expected, amount, 1e-10, "expected correct decimal amount");
    }

    @MethodSource("provideTwoColumnsInSameRowTestCasesWithIntegerOutput")
    @ParameterizedTest(name = "{0}")
    void testTwoColumnsInSameRowWithIntegerOutput(@SuppressWarnings("unused") final String name, final int firstColumn,
        final Granularity unit, final long expected, final boolean negated)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();

        settings.m_firstColumnSelection = new ColumnSelection(TEST_DATA_TABLE_SPEC.getColumnSpec(firstColumn - 1));
        settings.m_secondColumnSelection = new ColumnSelection(TEST_DATA_TABLE_SPEC.getColumnSpec(firstColumn));
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;
        settings.m_outputType = OutputType.NUMBER;
        settings.m_outputNumberType = OutputNumberType.NO_DECIMALS;
        settings.m_granularity = unit;

        var setup = setupAndExecuteWorkflow(settings);
        var amount = ((LongValue)setup.lastAppendedCell).getLongValue();

        assertEquals(expected, amount, "expected correct integer amount");
    }

    @MethodSource("provideOneColumnAndFixedValueTestCasesWithTemporalOutput")
    @ParameterizedTest(name = "{0}")
    void testOneColumnAndFixedValueWithTemporalOutput(@SuppressWarnings("unused") final String name, final int column,
        final Temporal fixedValue, final TemporalAmount expected, final boolean negated)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();

        settings.m_firstColumnSelection = new ColumnSelection(TEST_DATA_TABLE_SPEC.getColumnSpec(column));
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.FIXED_DATE_TIME;
        settings.m_localDateFixed = TemporalQueries.localDate().queryFrom(fixedValue);
        settings.m_localTimeFixed = TemporalQueries.localTime().queryFrom(fixedValue);
        settings.m_timezoneFixed = TemporalQueries.zone().queryFrom(fixedValue);
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;

        var setup = setupAndExecuteWorkflow(settings);
        var temporalAmount = extractTemporalAmountFromDataCell(setup.lastAppendedCell);

        assertEquals(expected, temporalAmount, "expected correct temporal amount");
    }

    @MethodSource("provideOneColumnAndFixedValueTestCasesWithDecimalOutput")
    @ParameterizedTest(name = "{0}")
    void testOneColumnAndFixedValueWithDecimalOutput(@SuppressWarnings("unused") final String name, final int column,
        final Temporal fixedValue, final Granularity unit, final double expected, final boolean negated)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();

        settings.m_firstColumnSelection = new ColumnSelection(TEST_DATA_TABLE_SPEC.getColumnSpec(column));
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.FIXED_DATE_TIME;
        settings.m_localDateFixed = TemporalQueries.localDate().queryFrom(fixedValue);
        settings.m_localTimeFixed = TemporalQueries.localTime().queryFrom(fixedValue);
        settings.m_timezoneFixed = TemporalQueries.zone().queryFrom(fixedValue);
        settings.m_outputType = OutputType.NUMBER;
        settings.m_outputNumberType = OutputNumberType.DECIMALS;
        settings.m_granularity = unit;
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;

        var setup = setupAndExecuteWorkflow(settings);
        var amount = ((DoubleValue)setup.lastAppendedCell).getDoubleValue();

        assertEquals(expected, amount, 1e-10, "expected correct decimal amount");
    }

    @MethodSource("provideOneColumnAndFixedValueTestCasesWithIntegerOutput")
    @ParameterizedTest(name = "{0}")
    void testOneColumnAndFixedValueWithIntegerOutput(@SuppressWarnings("unused") final String name, final int column,
        final Temporal fixedValue, final Granularity unit, final long expected, final boolean negated)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();

        settings.m_firstColumnSelection = new ColumnSelection(TEST_DATA_TABLE_SPEC.getColumnSpec(column));
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.FIXED_DATE_TIME;
        settings.m_localDateFixed = TemporalQueries.localDate().queryFrom(fixedValue);
        settings.m_localTimeFixed = TemporalQueries.localTime().queryFrom(fixedValue);
        settings.m_timezoneFixed = TemporalQueries.zone().queryFrom(fixedValue);
        settings.m_outputType = OutputType.NUMBER;
        settings.m_outputNumberType = OutputNumberType.NO_DECIMALS;
        settings.m_granularity = unit;
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;

        var setup = setupAndExecuteWorkflow(settings);
        var amount = ((LongValue)setup.lastAppendedCell).getLongValue();

        assertEquals(expected, amount, "expected correct integer amount");
    }

    private record TestSetup(BufferedDataTable outputTable, DataCell lastAppendedCell) {
    }

    private static TestSetup setupAndExecuteWorkflow(final DateTimeDifferenceNodeSettings settings)
        throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new DateTimeDifferenceNodeFactory2());

        settings.m_outputColumnName = "output_column";

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(DateTimeDifferenceNodeSettings.class, settings, modelSettings);
        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // populate the input table
        var tableSupplierNode = WorkflowManagerUtil.createAndAddNode(workflowManager,
            new InputTableNode.InputDataNodeFactory(TEST_DATA_TABLE));

        // link the nodes
        workflowManager.addConnection(tableSupplierNode.getID(), 1, node.getID(), 1);

        // execute and wait...
        workflowManager.executeAllAndWaitUntilDone();

        var outputTable = (BufferedDataTable)node.getOutPort(1).getPortObject();

        var outputColumnIndex = outputTable.getDataTableSpec().findColumnIndex(settings.m_outputColumnName);

        var lastRow = InputTableNode.getLastRow(outputTable);

        return new TestSetup(outputTable, lastRow.getCell(outputColumnIndex));
    }

    private static TemporalAmount extractTemporalAmountFromDataCell(final DataCell cell) {
        if (cell instanceof PeriodValue pv) {
            return pv.getPeriod();
        } else if (cell instanceof DurationValue dv) {
            return dv.getDuration();
        } else {
            throw new IllegalArgumentException("Cell is not a PeriodValue or DurationValue");
        }
    }

    private static TemporalAmount negate(final TemporalAmount toNegate) {
        if (toNegate instanceof Period p) {
            return p.negated();
        } else if (toNegate instanceof Duration d) {
            return d.negated();
        } else {
            throw new IllegalArgumentException("Unsupported TemporalAmount type: " + toNegate.getClass());
        }
    }
}
