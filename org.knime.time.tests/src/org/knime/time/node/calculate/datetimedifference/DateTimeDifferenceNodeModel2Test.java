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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.DateTimeTestingUtil;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.testing.util.InputTableNode.InputDataNodeFactory;
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

    private static final String COLUMN_1_NAME = "column1";

    private static final String COLUMN_2_NAME = "column2";

    private static final String OUTPUT_COLUMN_NAME = "output_column";

    /**
     * Infer the type of the Temporal and create the appropriate DataCell. If t is null, then a MissingCell is created.
     *
     * @param t
     * @return
     */
    private static DataCell convertTemporalToDataCell(final Temporal t) {
        if (t == null) {
            return new MissingCell("");
        }

        if (t instanceof LocalTime lt) {
            return LocalTimeCellFactory.create(lt);
        } else if (t instanceof LocalDate ld) {
            return LocalDateCellFactory.create(ld);
        } else if (t instanceof LocalDateTime ldt) {
            return LocalDateTimeCellFactory.create(ldt);
        } else if (t instanceof ZonedDateTime zdt) {
            return ZonedDateTimeCellFactory.create(zdt);
        } else {
            throw new IllegalArgumentException("Unsupported Temporal type: " + t.getClass());
        }
    }

    /**
     * Creates a table with 1 or 2 data values, depending on the number of args.
     *
     * @param columns if true and args.length == 2, then the table will have two columns. If false and args.length == 2,
     *            then the table will have one column but two rows. If args.length == 1, then the table will have one
     *            column and one row regardless of this setting.
     * @param args the temporal values to populate the table with. Must have length 1 or 2.
     * @return a supplier of the table
     */
    private static Supplier<BufferedDataTable> createInputTable(final boolean columns, final DataType type,
        final Temporal... args) {
        if (args.length > 2 || args.length < 1) {
            throw new IllegalArgumentException("Only 1-2 columns are supported");
        }

        // Step 1: Create the DataTableSpec
        var specBuilder = new TableTestUtil.SpecBuilder().addColumn( //
            COLUMN_1_NAME, type //
        );
        if (args.length == 2 && columns) {
            specBuilder.addColumn( //
                COLUMN_2_NAME, type //
            );
        }

        var spec = specBuilder.build();

        // Step 2: Create the DataTable
        var tableBuilder = new TableTestUtil.TableBuilder(spec);

        if (columns) {
            tableBuilder.addRow((Object[])Arrays.stream(args)
                .map(DateTimeDifferenceNodeModel2Test::convertTemporalToDataCell).toArray(DataCell[]::new));
        } else {
            Arrays.stream(args).forEach(t -> tableBuilder.addRow(convertTemporalToDataCell(t)));
        }

        return tableBuilder.build();
    }

    private static record TestCaseTemporalAmount<T extends Temporal>(String name, T arg1, T arg2,
        TemporalAmount expected, DataType dataType) {
    }

    private static record TestCaseDecimalValue<T extends Temporal>(String name, T arg1, T arg2, Granularity unit,
        Double expected, DataType dataType) {
    }

    private static record TestCaseIntegerValue<T extends Temporal>(String name, T arg1, T arg2, Granularity unit,
        Long expected, DataType dataType) {
    }

    private static record TestCaseMissingOutput(String name, Temporal arg1, Temporal arg2, DataType dataType) {
    }

    private static final LocalDate BASE_DATE = LocalDate.EPOCH;

    private static final LocalTime BASE_TIME = LocalTime.MIDNIGHT;

    private static final LocalDateTime BASE_DATE_TIME = LocalDateTime.of(BASE_DATE, BASE_TIME);

    private static final ZonedDateTime BASE_ZONED_DATE_TIME = ZonedDateTime.of(BASE_DATE_TIME, ZoneId.of("UTC"));

    private static final Duration BASE_DURATION_INCREMENT = Duration.ofSeconds(1);

    private static final Period BASE_PERIOD_INCREMENT = Period.ofDays(1);

    private static final ZonedDateTime SECOND_ZONED_DATE_TIME =
        ZonedDateTime.of(LocalDate.EPOCH.plusDays(7), LocalTime.MIDNIGHT.plusHours(5), ZoneId.of("Europe/Berlin"));

    private static final Duration ZONED_DIFFERENCE = Duration.between(BASE_ZONED_DATE_TIME, SECOND_ZONED_DATE_TIME);

    private static final List<TestCaseTemporalAmount<?>> TEST_CASES_TEMPORAL_OUTPUT = List.of( //
        new TestCaseTemporalAmount<>("localTime", BASE_TIME, BASE_TIME.plus(BASE_DURATION_INCREMENT),
            BASE_DURATION_INCREMENT, LocalTimeCellFactory.TYPE), //
        new TestCaseTemporalAmount<>("localDate", BASE_DATE, BASE_DATE.plus(BASE_PERIOD_INCREMENT),
            BASE_PERIOD_INCREMENT, LocalDateCellFactory.TYPE), //
        new TestCaseTemporalAmount<>("localDateTime", BASE_DATE_TIME, BASE_DATE_TIME.plus(BASE_DURATION_INCREMENT),
            BASE_DURATION_INCREMENT, LocalDateTimeCellFactory.TYPE), //
        new TestCaseTemporalAmount<>("zonedDateTime", BASE_ZONED_DATE_TIME, SECOND_ZONED_DATE_TIME, ZONED_DIFFERENCE,
            ZonedDateTimeCellFactory.TYPE) //
    );

    private static final List<TestCaseDecimalValue<?>> TEST_CASES_DECIMAL_OUTPUT = List.of( //
        new TestCaseDecimalValue<>("localTime - seconds", BASE_TIME, BASE_TIME.plus(BASE_DURATION_INCREMENT),
            Granularity.SECOND, (double)BASE_DURATION_INCREMENT.getSeconds(), LocalTimeCellFactory.TYPE), //
        new TestCaseDecimalValue<>("localTime - minutes", BASE_TIME, BASE_TIME.plus(BASE_DURATION_INCREMENT),
            Granularity.MINUTE, BASE_DURATION_INCREMENT.getSeconds() / 60.0, LocalTimeCellFactory.TYPE), //
        new TestCaseDecimalValue<>("localDateTime - seconds", BASE_DATE_TIME,
            BASE_DATE_TIME.plus(BASE_DURATION_INCREMENT), Granularity.SECOND,
            (double)BASE_DURATION_INCREMENT.getSeconds(), LocalDateTimeCellFactory.TYPE), //
        new TestCaseDecimalValue<>("localDateTime - minutes", BASE_DATE_TIME,
            BASE_DATE_TIME.plus(BASE_DURATION_INCREMENT), Granularity.MINUTE,
            BASE_DURATION_INCREMENT.getSeconds() / 60.0, LocalDateTimeCellFactory.TYPE), //
        new TestCaseDecimalValue<>("zonedDateTime - seconds", BASE_ZONED_DATE_TIME, SECOND_ZONED_DATE_TIME,
            Granularity.SECOND, (double)ZONED_DIFFERENCE.getSeconds(), ZonedDateTimeCellFactory.TYPE), //
        new TestCaseDecimalValue<>("zonedDateTime - minutes", BASE_ZONED_DATE_TIME, SECOND_ZONED_DATE_TIME,
            Granularity.MINUTE, ZONED_DIFFERENCE.getSeconds() / 60.0, ZonedDateTimeCellFactory.TYPE) //
    );

    private static final List<TestCaseIntegerValue<Temporal>> TEST_CASES_INTEGER_OUTPUT =
        TEST_CASES_DECIMAL_OUTPUT.stream().map(tc -> new TestCaseIntegerValue<Temporal>(tc.name, tc.arg1, tc.arg2,
                tc.unit, tc.expected == null ? null : tc.expected.longValue(), tc.dataType)).toList();

    static Stream<Arguments> provideTestCasesWithTemporalOutput() {
        return TEST_CASES_TEMPORAL_OUTPUT.stream().map(tc -> Stream.of( //
            Arguments.of(tc.name + " (not negated)", tc.arg1, tc.arg2, tc.expected, false, tc.dataType), //
            Arguments.of(tc.name + " (negated)", tc.arg1, tc.arg2, negate(tc.expected), true, tc.dataType) //
        )).flatMap(Function.identity());
    }

    static Stream<Arguments> provideTestCasesWithDecimalOutput() {
        return TEST_CASES_DECIMAL_OUTPUT.stream().map(tc -> Stream.of( //
            Arguments.of(tc.name + " (not negated)", tc.arg1, tc.arg2, tc.unit, tc.expected, false, tc.dataType), //
            Arguments.of(tc.name + " (negated)", tc.arg1, tc.arg2, tc.unit, -tc.expected, true, tc.dataType) //
        )).flatMap(Function.identity());
    }

    static Stream<Arguments> provideTestCasesWithIntegerOutput() {
        return TEST_CASES_INTEGER_OUTPUT.stream().map(tc -> Stream.of( //
            Arguments.of(tc.name + " (not negated)", tc.arg1, tc.arg2, tc.unit, tc.expected, false, tc.dataType), //
            Arguments.of(tc.name + " (negated)", tc.arg1, tc.arg2, tc.unit, -tc.expected, true, tc.dataType) //
        )).flatMap(Function.identity());
    }

    @MethodSource("provideTestCasesWithTemporalOutput")
    @ParameterizedTest(name = "{0}")
    void testPreviousRowWithTemporalOutput(@SuppressWarnings("unused") final String name, final Temporal arg1,
        final Temporal arg2, final TemporalAmount expected, final boolean negated, final DataType type)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();

        var table = createInputTable(false, type, arg1, arg2);

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.PREVIOUS_ROW;
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;

        var setup = setupAndExecuteWorkflow(settings, table);
        var temporalAmount = extractTemporalAmountFromDataCell(setup.lastAppendedCell);

        assertEquals(expected, temporalAmount, "expected correct temporal amount");
        assertEquals(table.get().size(), setup.outputTable.size(),
            "expected same number of rows in input as output table");

        try (var it = setup.outputTable.iterator()) {
            var firstRow = it.next();

            assertTrue(firstRow.getCell(setup.outputTable.getSpec().getNumColumns() - 1).isMissing(),
                "first cell should be missing when using previous row");
        }
    }

    @MethodSource("provideTestCasesWithDecimalOutput")
    @ParameterizedTest(name = "{0}")
    void testPreviousRowWithDecimalOutput(@SuppressWarnings("unused") final String name, final Temporal arg1,
        final Temporal arg2, final Granularity unit, final Double expected, final boolean negated, final DataType type)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();

        var table = createInputTable(false, type, arg1, arg2);

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.PREVIOUS_ROW;
        settings.m_outputType = OutputType.NUMBER;
        settings.m_outputNumberType = OutputNumberType.DECIMALS;
        settings.m_granularity = unit;

        var setup = setupAndExecuteWorkflow(settings, table);
        var amount = ((DoubleValue)setup.lastAppendedCell).getDoubleValue();

        assertEquals(expected, amount, 1e-10, "expected correct decimal amount");
        assertEquals(table.get().size(), setup.outputTable.size(),
            "expected same number of rows in input as output table");

        try (var it = setup.outputTable.iterator()) {
            var firstRow = it.next();

            assertTrue(firstRow.getCell(setup.outputTable.getSpec().getNumColumns() - 1).isMissing(),
                "first cell should be missing when using previous row");
        }
    }

    @MethodSource("provideTestCasesWithIntegerOutput")
    @ParameterizedTest(name = "{0}")
    void testPreviousRowWithIntegerOutput(@SuppressWarnings("unused") final String name, final Temporal arg1,
        final Temporal arg2, final Granularity unit, final Long expected, final boolean negated, final DataType type)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();

        var table = createInputTable(false, type, arg1, arg2);

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.PREVIOUS_ROW;
        settings.m_outputType = OutputType.NUMBER;
        settings.m_outputNumberType = OutputNumberType.NO_DECIMALS;
        settings.m_granularity = unit;

        var setup = setupAndExecuteWorkflow(settings, table);
        var amount = ((LongValue)setup.lastAppendedCell).getLongValue();

        assertEquals(expected, amount, "expected correct integer amount");
        assertEquals(table.get().size(), setup.outputTable.size(),
            "expected same number of rows in input as output table");

        try (var it = setup.outputTable.iterator()) {
            var firstRow = it.next();

            assertTrue(firstRow.getCell(setup.outputTable.getSpec().getNumColumns() - 1).isMissing(),
                "first cell should be missing when using previous row");
        }
    }

    @Test
    void testPreviousRowWithMissingOutputBecauseFirstRowIsMissing() throws InvalidSettingsException, IOException {
        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(false, LocalTimeCellFactory.TYPE, (Temporal)null, BASE_TIME);

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_mode = Mode.FIRST_MINUS_SECOND;
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.PREVIOUS_ROW;
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;

        var setup = setupAndExecuteWorkflow(settings, table);

        assertTrue(setup.lastAppendedCell.isMissing(), "expected missing cell when the first row is missing");
        assertEquals(table.get().size(), setup.outputTable.size(),
            "expected same number of rows in input as output table");

        try (var it = setup.outputTable.iterator()) {
            var firstRow = it.next();

            assertTrue(firstRow.getCell(setup.outputTable.getSpec().getNumColumns() - 1).isMissing(),
                "first cell should be missing when using previous row");
        }
    }

    @Test
    void testPreviousRowWithMissingOutputBecauseSecondRowIsMissing() throws InvalidSettingsException, IOException {
        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(false, LocalTimeCellFactory.TYPE, BASE_TIME, (Temporal)null);

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_mode = Mode.FIRST_MINUS_SECOND;
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.PREVIOUS_ROW;
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;

        var setup = setupAndExecuteWorkflow(settings, table);

        assertTrue(setup.lastAppendedCell.isMissing(), "expected missing cell when the second row is missing");
        assertEquals(table.get().size(), setup.outputTable.size(),
            "expected same number of rows in input as output table");

        try (var it = setup.outputTable.iterator()) {
            var firstRow = it.next();

            assertTrue(firstRow.getCell(setup.outputTable.getSpec().getNumColumns() - 1).isMissing(),
                "first cell should be missing when using previous row");
        }
    }

    @Test
    void testPreviousRowWithMissingOutputBecauseBothRowsAreMissing() throws InvalidSettingsException, IOException {
        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(false, LocalTimeCellFactory.TYPE, (Temporal)null, (Temporal)null);

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_mode = Mode.FIRST_MINUS_SECOND;
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.PREVIOUS_ROW;
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;

        var setup = setupAndExecuteWorkflow(settings, table);

        assertTrue(setup.lastAppendedCell.isMissing(), "expected missing cell when both rows are missing");

        try (var it = setup.outputTable.iterator()) {
            var firstRow = it.next();

            assertTrue(firstRow.getCell(setup.outputTable.getSpec().getNumColumns() - 1).isMissing(),
                "first cell should be missing when using previous row");
        }
    }

    @Test
    void testPreviousRowWithZeroLengthTable() throws InvalidSettingsException, IOException {
        var settings = new DateTimeDifferenceNodeSettings();

        var tableSpec = new TableTestUtil.SpecBuilder() //
            .addColumn(COLUMN_1_NAME, LocalTimeCellFactory.TYPE) //
            .build();
        var emptyTable = new TableTestUtil.TableBuilder(tableSpec).build();

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_mode = Mode.FIRST_MINUS_SECOND;
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.PREVIOUS_ROW;
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;

        var outputTable = setupAndExecuteWorkflow(settings, emptyTable).outputTable;

        assertEquals(0, outputTable.size(), "expected empty output table");
    }

    @MethodSource("provideTestCasesWithTemporalOutput")
    @ParameterizedTest(name = "{0}")
    void testTwoColumnsInSameRowWithTemporalOutput(@SuppressWarnings("unused") final String name, final Temporal arg1,
        final Temporal arg2, final TemporalAmount expected, final boolean negated, final DataType type)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(true, type, arg1, arg2);

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_secondColumnSelection = COLUMN_2_NAME;
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;

        var setup = setupAndExecuteWorkflow(settings, table);
        var temporalAmount = extractTemporalAmountFromDataCell(setup.lastAppendedCell);

        assertEquals(expected, temporalAmount, "expected correct temporal amount");
    }

    @MethodSource("provideTestCasesWithDecimalOutput")
    @ParameterizedTest(name = "{0}")
    void testTwoColumnsInSameRowWithDecimalOutput(@SuppressWarnings("unused") final String name, final Temporal arg1,
        final Temporal arg2, final Granularity unit, final Double expected, final boolean negated, final DataType type)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(true, type, arg1, arg2);

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_secondColumnSelection = COLUMN_2_NAME;
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;
        settings.m_outputType = OutputType.NUMBER;
        settings.m_outputNumberType = OutputNumberType.DECIMALS;
        settings.m_granularity = unit;

        var setup = setupAndExecuteWorkflow(settings, table);
        var amount = ((DoubleValue)setup.lastAppendedCell).getDoubleValue();

        assertEquals(expected, amount, 1e-10, "expected correct decimal amount");
    }

    @MethodSource("provideTestCasesWithIntegerOutput")
    @ParameterizedTest(name = "{0}")
    void testTwoColumnsInSameRowWithIntegerOutput(@SuppressWarnings("unused") final String name, final Temporal arg1,
        final Temporal arg2, final Granularity unit, final Long expected, final boolean negated, final DataType type)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(true, type, arg1, arg2);

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_secondColumnSelection = COLUMN_2_NAME;
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;
        settings.m_outputType = OutputType.NUMBER;
        settings.m_outputNumberType = OutputNumberType.NO_DECIMALS;
        settings.m_granularity = unit;

        var setup = setupAndExecuteWorkflow(settings, table);
        var amount = ((LongValue)setup.lastAppendedCell).getLongValue();

        assertEquals(expected, amount, "expected correct integer amount");
    }

    @Test
    void testTwoColumnsInSameRowWithMissingOutputBecauseFirstColumnIsMissing()
        throws InvalidSettingsException, IOException {
        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(true, LocalTimeCellFactory.TYPE, (Temporal)null, BASE_TIME);

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_secondColumnSelection = COLUMN_2_NAME;
        settings.m_mode = Mode.FIRST_MINUS_SECOND;
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;

        var setup = setupAndExecuteWorkflow(settings, table);

        assertTrue(setup.lastAppendedCell.isMissing(), "expected missing cell when the first column is missing");
    }

    @Test
    void testTwoColumnsInSameRowWithMissingOutputBecauseSecondColumnIsMissing()
        throws InvalidSettingsException, IOException {
        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(true, LocalTimeCellFactory.TYPE, BASE_TIME, (Temporal)null);

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_secondColumnSelection = COLUMN_2_NAME;
        settings.m_mode = Mode.FIRST_MINUS_SECOND;
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;

        var setup = setupAndExecuteWorkflow(settings, table);

        assertTrue(setup.lastAppendedCell.isMissing(), "expected missing cell when the second column is missing");
    }

    @Test
    void testTwoColumnsInSameRowWithMissingOutputBecauseBothColumnsAreMissing()
        throws InvalidSettingsException, IOException {
        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(true, LocalTimeCellFactory.TYPE, (Temporal)null, (Temporal)null);

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_secondColumnSelection = COLUMN_2_NAME;
        settings.m_mode = Mode.FIRST_MINUS_SECOND;
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;

        var setup = setupAndExecuteWorkflow(settings, table);

        assertTrue(setup.lastAppendedCell.isMissing(), "expected missing cell when both columns are missing");
    }

    @Test
    void testTwoColumnsInSameRowWithZeroLengthTable() throws InvalidSettingsException, IOException {
        var settings = new DateTimeDifferenceNodeSettings();
        var tableSpec = new TableTestUtil.SpecBuilder().addColumn(COLUMN_1_NAME, LocalTimeCellFactory.TYPE)
            .addColumn(COLUMN_2_NAME, LocalTimeCellFactory.TYPE).build();
        var emptyTable = new TableTestUtil.TableBuilder(tableSpec).build();

        settings.m_firstColumnSelection = COLUMN_1_NAME;
        settings.m_secondColumnSelection = COLUMN_2_NAME;
        settings.m_mode = Mode.FIRST_MINUS_SECOND;
        settings.m_outputType = OutputType.DURATION_OR_PERIOD;

        var setup = setupAndExecuteWorkflow(settings, emptyTable);

        assertEquals(0, setup.outputTable.size(), "expected empty output table");
    }

    @MethodSource("provideTestCasesWithTemporalOutput")
    @ParameterizedTest(name = "{0}")
    void testOneColumnAndFixedValueWithTemporalOutput(@SuppressWarnings("unused") final String name,
        final Temporal arg1, final Temporal arg2, final TemporalAmount expected, final boolean negated,
        final DataType type) throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(false, type, arg1);

        settings.m_firstColumnSelection = COLUMN_1_NAME;

        setFixedFieldsInSettings(settings, arg2);

        settings.m_outputType = OutputType.DURATION_OR_PERIOD;
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;

        var setup = setupAndExecuteWorkflow(settings, table);
        var temporalAmount = extractTemporalAmountFromDataCell(setup.lastAppendedCell);

        assertEquals(expected, temporalAmount, "expected correct temporal amount");
    }

    @MethodSource("provideTestCasesWithDecimalOutput")
    @ParameterizedTest(name = "{0}")
    void testOneColumnAndFixedValueWithDecimalOutput(@SuppressWarnings("unused") final String name, final Temporal arg1,
        final Temporal arg2, final Granularity unit, final Double expected, final boolean negated, final DataType type)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(false, type, arg1);

        settings.m_firstColumnSelection = COLUMN_1_NAME;

        setFixedFieldsInSettings(settings, arg2);

        settings.m_outputType = OutputType.NUMBER;
        settings.m_outputNumberType = OutputNumberType.DECIMALS;
        settings.m_granularity = unit;
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;

        var setup = setupAndExecuteWorkflow(settings, table);
        var amount = ((DoubleValue)setup.lastAppendedCell).getDoubleValue();

        assertEquals(expected, amount, 1e-10, "expected correct decimal amount");
    }

    @MethodSource("provideTestCasesWithIntegerOutput")
    @ParameterizedTest(name = "{0}")
    void testOneColumnAndFixedValueWithIntegerOutput(@SuppressWarnings("unused") final String name, final Temporal arg1,
        final Temporal arg2, final Granularity unit, final Long expected, final boolean negated, final DataType type)
        throws InvalidSettingsException, IOException {

        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(false, type, arg1);

        settings.m_firstColumnSelection = COLUMN_1_NAME;

        setFixedFieldsInSettings(settings, arg2);

        settings.m_outputType = OutputType.NUMBER;
        settings.m_outputNumberType = OutputNumberType.NO_DECIMALS;
        settings.m_granularity = unit;
        settings.m_mode = negated ? Mode.FIRST_MINUS_SECOND : Mode.SECOND_MINUS_FIRST;

        var setup = setupAndExecuteWorkflow(settings, table);
        var amount = ((LongValue)setup.lastAppendedCell).getLongValue();

        assertEquals(expected, amount, "expected correct integer amount");
    }

    @Test
    void testOneColumnAndFixedValueWithMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new DateTimeDifferenceNodeSettings();
        var table = createInputTable(false, LocalTimeCellFactory.TYPE, (Temporal)null);

        settings.m_firstColumnSelection = COLUMN_1_NAME;

        setFixedFieldsInSettings(settings, BASE_ZONED_DATE_TIME);

        settings.m_outputType = OutputType.DURATION_OR_PERIOD;
        settings.m_mode = Mode.FIRST_MINUS_SECOND;

        var setup = setupAndExecuteWorkflow(settings, table);

        assertTrue(setup.lastAppendedCell.isMissing(), "expected missing cell");
    }

    @Test
    void testOneColumnAndFixedValueWithZeroLengthTable() throws InvalidSettingsException, IOException {
        var settings = new DateTimeDifferenceNodeSettings();

        var tableSpec = new TableTestUtil.SpecBuilder() //
            .addColumn(COLUMN_1_NAME, LocalTimeCellFactory.TYPE) //
            .build();
        var emptyTable = new TableTestUtil.TableBuilder(tableSpec).build();

        settings.m_firstColumnSelection = COLUMN_1_NAME;

        setFixedFieldsInSettings(settings, BASE_ZONED_DATE_TIME);

        settings.m_outputType = OutputType.DURATION_OR_PERIOD;
        settings.m_mode = Mode.FIRST_MINUS_SECOND;

        var outputTable = setupAndExecuteWorkflow(settings, emptyTable).outputTable;

        assertEquals(0, outputTable.size(), "expected empty output table");
    }

    private static void setFixedFieldsInSettings(final DateTimeDifferenceNodeSettings settings, final Temporal t) {
        settings.m_secondDateTimeValueType = SecondDateTimeValueType.FIXED_DATE_TIME;
        settings.m_localDateFixed = TemporalQueries.localDate().queryFrom(t);
        settings.m_localTimeFixed = TemporalQueries.localTime().queryFrom(t);

        if (settings.m_localTimeFixed != null && settings.m_localDateFixed != null) {
            settings.m_localDateTimeFixed = LocalDateTime.of(settings.m_localDateFixed, settings.m_localTimeFixed);

            var extractedZone = TemporalQueries.zone().queryFrom(t);
            if (extractedZone != null) {
                settings.m_zonedDateTimeFixed = ZonedDateTime.of(settings.m_localDateTimeFixed, extractedZone);
            }
        }
    }

    private record TestSetup(BufferedDataTable outputTable, DataCell lastAppendedCell) {
    }

    private static TestSetup setupAndExecuteWorkflow(final DateTimeDifferenceNodeSettings settings,
        final Supplier<BufferedDataTable> table) throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new DateTimeDifferenceNodeFactory2());

        settings.m_outputColumnName = OUTPUT_COLUMN_NAME;

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        NodeParametersUtil.saveSettings(DateTimeDifferenceNodeSettings.class, settings, modelSettings);
        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // populate the input table
        var tableSupplierNode = WorkflowManagerUtil.createAndAddNode(workflowManager, new InputDataNodeFactory(table));

        // link the nodes
        workflowManager.addConnection(tableSupplierNode.getID(), 1, node.getID(), 1);

        // execute and wait...
        workflowManager.executeAllAndWaitUntilDone();

        var outputTable = (BufferedDataTable)node.getOutPort(1).getPortObject();

        var outputColumnIndex = outputTable.getDataTableSpec().findColumnIndex(OUTPUT_COLUMN_NAME);

        if (outputTable.size() == 0) {
            return new TestSetup(outputTable, null);
        }
        var lastRow = DateTimeTestingUtil.getLastRow(outputTable);

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
