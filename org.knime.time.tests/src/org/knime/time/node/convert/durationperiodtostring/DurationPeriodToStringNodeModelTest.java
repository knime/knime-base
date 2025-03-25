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
 *   Nov 21, 2024 (david): created
 */
package org.knime.time.node.convert.durationperiodtostring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.NodeModelTestRunnerUtil;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.time.util.DurationPeriodStringFormat;
import org.knime.time.util.ReplaceOrAppend;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class DurationPeriodToStringNodeModelTest {

    private record TestCase<I>(I input, String expectedIso, String expectedShortForm, String expectedLongForm) {
    }

    private static final String INPUT_COLUMN = "test_input";

    private static final NodeModelTestRunnerUtil RUNNER =
        new NodeModelTestRunnerUtil(INPUT_COLUMN, "DurationPeriodToStringNode",
            DurationPeriodToStringNodeSettings.class, DurationPeriodToStringNodeFactory2.class);

    private static final List<TestCase<Duration>> DURATION_TEST_CASES = List.of( //
        new TestCase<Duration>(Duration.ofHours(1), "PT1H", "1H", "1 hour"), //
        new TestCase<Duration>(Duration.ofMinutes(1), "PT1M", "1m", "1 minute"), //
        new TestCase<Duration>(Duration.ofSeconds(1), "PT1S", "1s", "1 second"), //
        new TestCase<Duration>(Duration.ofMillis(1), "PT0.001S", "0.001s", "0.001 seconds"), //
        new TestCase<Duration>(Duration.ofNanos(1000), "PT0.000001S", "0.000001s", "0.000001 seconds"), //
        new TestCase<Duration>(Duration.ofNanos(1), "PT0.000000001S", "0.000000001s", "0.000000001 seconds"), //
        new TestCase<Duration>(Duration.ofHours(2), "PT2H", "2H", "2 hours"), //
        new TestCase<Duration>(Duration.ofMinutes(2), "PT2M", "2m", "2 minutes"), //
        new TestCase<Duration>(Duration.ofSeconds(2), "PT2S", "2s", "2 seconds"), //
        new TestCase<Duration>(Duration.parse("PT1H2M3.4S"), "PT1H2M3.4S", "1H 2m 3.4s",
            "1 hour 2 minutes 3.4 seconds"), //
        new TestCase<Duration>(Duration.ZERO, "PT0S", "0s", "0 seconds") //
    );

    private static final List<TestCase<Period>> PERIOD_TEST_CASES = List.of( //
        new TestCase<Period>(Period.ofDays(1), "P1D", "1d", "1 day"), //
        new TestCase<Period>(Period.ofMonths(1), "P1M", "1M", "1 month"), //
        new TestCase<Period>(Period.ofYears(1), "P1Y", "1y", "1 year"), //
        new TestCase<Period>(Period.ofDays(2), "P2D", "2d", "2 days"), //
        new TestCase<Period>(Period.ofMonths(2), "P2M", "2M", "2 months"), //
        new TestCase<Period>(Period.ofYears(2), "P2Y", "2y", "2 years"), //
        new TestCase<Period>(Period.ofWeeks(3), "P21D", "21d", "21 days"), //
        new TestCase<Period>(Period.of(1, 2, 3), "P1Y2M3D", "1y 2M 3d", "1 year 2 months 3 days"), //
        new TestCase<Period>(Period.ZERO, "P0D", "0d", "0 days") //
    );

    static Stream<Arguments> provideArgumentsForPeriodTestCases() {
        return Arrays.stream(DurationPeriodStringFormat.values())
            .map(format -> PERIOD_TEST_CASES.stream().map(testCase -> Arguments.of( //
                "Convert '%s' to %s".formatted(testCase.input.toString(), format.name()), //
                testCase.input, //
                format, //
                switch (format) {
                    case ISO -> testCase.expectedIso;
                    case LETTERS -> testCase.expectedShortForm;
                    case WORDS -> testCase.expectedLongForm;
                })))
            .flatMap(Function.identity());
    }

    static Stream<Arguments> provideArgumentsForDurationTestCases() {
        return Arrays.stream(DurationPeriodStringFormat.values())
            .map(format -> DURATION_TEST_CASES.stream().map(testCase -> Arguments.of( //
                "Convert '%s' to %s".formatted(testCase.input.toString(), format.name()), //
                testCase.input, //
                format, //
                switch (format) {
                    case ISO -> testCase.expectedIso;
                    case LETTERS -> testCase.expectedShortForm;
                    case WORDS -> testCase.expectedLongForm;
                })))
            .flatMap(Function.identity());
    }

    @Test
    void testOutputSettingReplace() throws InvalidSettingsException, IOException {
        var settings = new DurationPeriodToStringNodeSettings();
        settings.m_appendOrReplaceColumn = ReplaceOrAppend.REPLACE;
        settings.m_filter = new ColumnFilter(new String[]{INPUT_COLUMN});

        var testSetup =
            RUNNER.setupAndExecuteWorkflow(settings, DurationCellFactory.create(Duration.parse("PT1H2M3.0456789S")));

        var outputTableSpec = testSetup.outputTable().getDataTableSpec();
        assertEquals(1, outputTableSpec.getNumColumns(), "Expected exactly one output column");

        var outputColumnName = outputTableSpec.getColumnNames()[0];
        assertEquals(INPUT_COLUMN, outputColumnName, "Output column name is not as expected");
    }

    @Test
    void testOutputSettingAppendWithSuffix() throws InvalidSettingsException, IOException {
        var settings = new DurationPeriodToStringNodeSettings();
        settings.m_appendOrReplaceColumn = ReplaceOrAppend.APPEND;
        settings.m_filter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_suffix = "_converted";

        var testSetup =
            RUNNER.setupAndExecuteWorkflow(settings, DurationCellFactory.create(Duration.parse("PT1H2M3.0456789S")));

        var outputTableSpec = testSetup.outputTable().getDataTableSpec();
        assertEquals(2, outputTableSpec.getNumColumns(), "Expected exactly two output columns");
        assertEquals(INPUT_COLUMN, outputTableSpec.getColumnNames()[0], "First column name should not have changed");
        assertEquals(INPUT_COLUMN + settings.m_suffix, outputTableSpec.getColumnNames()[1],
            "Second column name is not as expected");
    }

    @Test
    void testThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new DurationPeriodToStringNodeSettings();
        settings.m_filter = new ColumnFilter(new String[]{INPUT_COLUMN});

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertTrue(testSetup.firstCell().isMissing(), "Output cell should be missing");
    }

    @Test
    void testThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        var settings = new DurationPeriodToStringNodeSettings();
        settings.m_filter = new ColumnFilter(new String[]{INPUT_COLUMN});

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, null, DurationCellFactory.TYPE);

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertNull(testSetup.firstCell(), "Output cell should not exists");
        assertEquals(0, testSetup.outputTable().size(), "Ouptput table should be empty");
    }

    @ParameterizedTest(name = "{0} (date/period based)")
    @MethodSource("provideArgumentsForPeriodTestCases")
    void testConvertPeriodToString(@SuppressWarnings("unused") final String testName, final Period input,
        final DurationPeriodStringFormat format, final String expected) throws InvalidSettingsException, IOException {

        var settings = new DurationPeriodToStringNodeSettings();
        settings.m_outputFormat = format;
        settings.m_appendOrReplaceColumn = ReplaceOrAppend.REPLACE;
        settings.m_filter = new ColumnFilter(new String[]{INPUT_COLUMN});

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, PeriodCellFactory.create(input));
        var outputValue = testSetup.firstCell();

        assertFalse(outputValue.isMissing(), "Output cell is missing");
        assertTrue(outputValue.getType().isCompatible(StringValue.class),
            "Expected a string output cell, but got a cell of type %s".formatted(outputValue.getType()));

        String actual = ((StringValue)outputValue).getStringValue();
        assertEquals(expected, actual, "Output value is not as expected");
    }

    @ParameterizedTest(name = "{0} (date/period based)")
    @MethodSource("provideArgumentsForDurationTestCases")
    void testConvertDurationToString(@SuppressWarnings("unused") final String testName, final Duration input,
        final DurationPeriodStringFormat format, final String expected) throws InvalidSettingsException, IOException {

        var settings = new DurationPeriodToStringNodeSettings();
        settings.m_outputFormat = format;
        settings.m_appendOrReplaceColumn = ReplaceOrAppend.REPLACE;
        settings.m_filter = new ColumnFilter(new String[]{INPUT_COLUMN});

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, DurationCellFactory.create(input));
        var outputValue = testSetup.firstCell();

        assertFalse(outputValue.isMissing(), "Output cell is missing");
        assertTrue(outputValue.getType().isCompatible(StringValue.class),
            "Expected a string output cell, but got a cell of type %s".formatted(outputValue.getType()));

        String actual = ((StringValue)outputValue).getStringValue();
        assertEquals(expected, actual, "Output value is not as expected");
    }
}
