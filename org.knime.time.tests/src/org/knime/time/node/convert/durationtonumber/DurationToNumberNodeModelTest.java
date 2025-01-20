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
package org.knime.time.node.convert.durationtonumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.InputTableNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;
import org.knime.time.node.convert.durationtonumber.DurationToNumberNodeSettings.RoundingBehaviour;
import org.knime.time.util.ReplaceOrAppend;
import org.knime.time.util.TimeBasedGranularityUnit;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings({"restriction", "squid:S5960", "squid:S1192"})
final class DurationToNumberNodeModelTest {

    private record TestCase(TimeBasedGranularityUnit targetUnit, Duration input, long expectedTruncatedOutput,
        double expectedExactOutput) {
    }

    private static final String INPUT_COLUMN = "test_input";

    private static final String NODE_NAME = "DurationToNumberNode";

    private static final Class<? extends DefaultNodeSettings> SETTINGS_CLASS = DurationToNumberNodeSettings.class;

    /**
     * Test cases for the conversion of durations to numbers. The expected output is provided in two forms: one that is
     * truncated to an integer and one that is an exact double value.
     *
     * Every test case will be negated and tested again, so we know that negative durations work too.
     */
    private static final List<TestCase> TEST_CASES = List.of( //
        new TestCase(TimeBasedGranularityUnit.HOURS, Duration.parse("PT0.000000001S"), 0L, 1.0e-9 / 3600), //
        new TestCase(TimeBasedGranularityUnit.HOURS, Duration.parse("PT0.000002S"), 0L, 2.0e-6 / 3600), //
        new TestCase(TimeBasedGranularityUnit.HOURS, Duration.parse("PT0.003S"), 0L, 3.0e-3 / 3600), //
        new TestCase(TimeBasedGranularityUnit.HOURS, Duration.parse("PT4S"), 0L, 4.0e+0 / 3600), //
        new TestCase(TimeBasedGranularityUnit.HOURS, Duration.parse("PT5M"), 0L, 5.0e+0 / 60), //
        new TestCase(TimeBasedGranularityUnit.HOURS, Duration.parse("PT6H"), 6L, 6.0e+0), //
        new TestCase(TimeBasedGranularityUnit.HOURS, Duration.parse("PT1H2M3.0456789S"), 1L, 3.7230456789e+3 / 3600), //
        new TestCase(TimeBasedGranularityUnit.MINUTES, Duration.parse("PT0.000000001S"), 0L, 1.0e-9 / 60), //
        new TestCase(TimeBasedGranularityUnit.MINUTES, Duration.parse("PT0.000002S"), 0L, 2.0e-6 / 60), //
        new TestCase(TimeBasedGranularityUnit.MINUTES, Duration.parse("PT0.003S"), 0L, 5.0e-5), //
        new TestCase(TimeBasedGranularityUnit.MINUTES, Duration.parse("PT4S"), 0L, 4.0e+0 / 60), //
        new TestCase(TimeBasedGranularityUnit.MINUTES, Duration.parse("PT5M"), 5L, 5.0e+0), //
        new TestCase(TimeBasedGranularityUnit.MINUTES, Duration.parse("PT6H"), 360L, 3.6e+2), //
        new TestCase(TimeBasedGranularityUnit.MINUTES, Duration.parse("PT1H2M3.0456789S"), 62L, 3.7230456789e+3 / 60), //
        new TestCase(TimeBasedGranularityUnit.SECONDS, Duration.parse("PT0.000000001S"), 0L, 1.0e-9), //
        new TestCase(TimeBasedGranularityUnit.SECONDS, Duration.parse("PT0.000002S"), 0L, 2.0e-6), //
        new TestCase(TimeBasedGranularityUnit.SECONDS, Duration.parse("PT0.003S"), 0L, 3.0e-3), //
        new TestCase(TimeBasedGranularityUnit.SECONDS, Duration.parse("PT4S"), 4L, 4.0e+0), //
        new TestCase(TimeBasedGranularityUnit.SECONDS, Duration.parse("PT5M"), 300L, 3.0e+2), //
        new TestCase(TimeBasedGranularityUnit.SECONDS, Duration.parse("PT6H"), 21600L, 2.16e+4), //
        new TestCase(TimeBasedGranularityUnit.SECONDS, Duration.parse("PT1H2M3.0456789S"), 3723L, 3.7230456789e+3), //
        new TestCase(TimeBasedGranularityUnit.MILLISECONDS, Duration.parse("PT0.000000001S"), 0L, 1.0e-6), //
        new TestCase(TimeBasedGranularityUnit.MILLISECONDS, Duration.parse("PT0.000002S"), 0L, 2.0e-3), //
        new TestCase(TimeBasedGranularityUnit.MILLISECONDS, Duration.parse("PT0.003S"), 3L, 3.0e+0), //
        new TestCase(TimeBasedGranularityUnit.MILLISECONDS, Duration.parse("PT4S"), 4000L, 4.0e+3), //
        new TestCase(TimeBasedGranularityUnit.MILLISECONDS, Duration.parse("PT5M"), 300000L, 3.0e+5), //
        new TestCase(TimeBasedGranularityUnit.MILLISECONDS, Duration.parse("PT6H"), 21600000L, 2.16e+7), //
        new TestCase(TimeBasedGranularityUnit.MILLISECONDS, Duration.parse("PT1H2M3.0456789S"), 3723045L,
            3.7230456789e+6), //
        new TestCase(TimeBasedGranularityUnit.MICROSECONDS, Duration.parse("PT0.000000001S"), 0L, 1.0e-3), //
        new TestCase(TimeBasedGranularityUnit.MICROSECONDS, Duration.parse("PT0.000002S"), 2L, 2.0e+0), //
        new TestCase(TimeBasedGranularityUnit.MICROSECONDS, Duration.parse("PT0.003S"), 3000L, 3.0e+3), //
        new TestCase(TimeBasedGranularityUnit.MICROSECONDS, Duration.parse("PT4S"), 4000000L, 4.0e+6), //
        new TestCase(TimeBasedGranularityUnit.MICROSECONDS, Duration.parse("PT5M"), 300000000L, 3.0e+8), //
        new TestCase(TimeBasedGranularityUnit.MICROSECONDS, Duration.parse("PT6H"), 21600000000L, 2.16e+10), //
        new TestCase(TimeBasedGranularityUnit.MICROSECONDS, Duration.parse("PT1H2M3.0456789S"), 3723045678L,
            3.7230456789e+9), //
        new TestCase(TimeBasedGranularityUnit.NANOSECONDS, Duration.parse("PT0.000000001S"), 1L, 1.0e+0), //
        new TestCase(TimeBasedGranularityUnit.NANOSECONDS, Duration.parse("PT0.000002S"), 2000L, 2.0e+3), //
        new TestCase(TimeBasedGranularityUnit.NANOSECONDS, Duration.parse("PT0.003S"), 3000000L, 3.0e+6), //
        new TestCase(TimeBasedGranularityUnit.NANOSECONDS, Duration.parse("PT4S"), 4000000000L, 4.0e+9), //
        new TestCase(TimeBasedGranularityUnit.NANOSECONDS, Duration.parse("PT5M"), 300000000000L, 3.0e+11), //
        new TestCase(TimeBasedGranularityUnit.NANOSECONDS, Duration.parse("PT6H"), 21600000000000L, 2.16e+13), //
        new TestCase(TimeBasedGranularityUnit.NANOSECONDS, Duration.parse("PT1H2M3.0456789S"), 3723045678900L,
            3.7230456789e+12) //
    );

    static Stream<Arguments> provideArgumentsForIntegerTestCase() {
        var positiveTestCases = TEST_CASES.stream().map(testCase -> Arguments.of( //
            "Convert '%s' to %s".formatted(testCase.input.toString(), testCase.targetUnit.name()), //
            testCase.input, //
            testCase.targetUnit, //
            testCase.expectedTruncatedOutput //
        ));

        var negativeTestCases = TEST_CASES.stream().map(testCase -> Arguments.of( //
            "Convert '-%s' to %s".formatted(testCase.input.toString(), testCase.targetUnit.name()), //
            testCase.input.negated(), //
            testCase.targetUnit, //
            -testCase.expectedTruncatedOutput //
        ));

        return Stream.concat(positiveTestCases, negativeTestCases);
    }

    static Stream<Arguments> provideArgumentsForDoubleTestCase() {
        var positiveTestCases = TEST_CASES.stream().map(testCase -> Arguments.of( //
            "Convert '%s' to %s".formatted(testCase.input.toString(), testCase.targetUnit.name()), //
            testCase.input, //
            testCase.targetUnit, //
            testCase.expectedExactOutput //
        ));

        var negativeTestCases = TEST_CASES.stream().map(testCase -> Arguments.of( //
            "Convert '-%s' to %s".formatted(testCase.input.toString(), testCase.targetUnit.name()), //
            testCase.input.negated(), //
            testCase.targetUnit, //
            -testCase.expectedExactOutput //
        ));

        return Stream.concat(positiveTestCases, negativeTestCases);
    }

    @Test
    void testOutputSettingReplace() throws InvalidSettingsException, IOException {
        var settings = new DurationToNumberNodeSettings();
        settings.m_unit = TimeBasedGranularityUnit.SECONDS;
        settings.m_appendOrReplaceColumn = ReplaceOrAppend.REPLACE;
        settings.m_filter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_roundingBehaviour = RoundingBehaviour.DOUBLE;

        var testSetup =
            setupAndExecuteWorkflow(settings, DurationCellFactory.create(Duration.parse("PT1H2M3.0456789S")));

        var outputTableSpec = testSetup.outputTable.getDataTableSpec();
        assertEquals(1, outputTableSpec.getNumColumns(), "Expected exactly one output column");

        var outputColumnName = outputTableSpec.getColumnNames()[0];
        assertEquals(INPUT_COLUMN, outputColumnName, "Output column name is not as expected");
    }

    @Test
    void testOutputSettingAppendWithSuffix() throws InvalidSettingsException, IOException {
        var settings = new DurationToNumberNodeSettings();
        settings.m_unit = TimeBasedGranularityUnit.SECONDS;
        settings.m_appendOrReplaceColumn = ReplaceOrAppend.APPEND;
        settings.m_filter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_roundingBehaviour = RoundingBehaviour.DOUBLE;
        settings.m_suffix = "_converted";

        var testSetup =
            setupAndExecuteWorkflow(settings, DurationCellFactory.create(Duration.parse("PT1H2M3.0456789S")));

        var outputTableSpec = testSetup.outputTable.getDataTableSpec();
        assertEquals(2, outputTableSpec.getNumColumns(), "Expected exactly two output columns");
        assertEquals(INPUT_COLUMN, outputTableSpec.getColumnNames()[0], "First column name should not have changed");
        assertEquals(INPUT_COLUMN + settings.m_suffix, outputTableSpec.getColumnNames()[1],
            "Second column name is not as expected");
    }

    @Test
    void testThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new DurationToNumberNodeSettings();
        settings.m_filter = new ColumnFilter(new String[]{INPUT_COLUMN});

        var testSetup = setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.success, "Execution should have been successful");
        assertTrue(testSetup.firstCell.isMissing(), "Output cell should be missing");
    }

    @Test
    void testThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        var settings = new DurationToNumberNodeSettings();
        settings.m_filter = new ColumnFilter(new String[]{INPUT_COLUMN});

        var testSetup = setupAndExecuteWorkflow(settings, null);

        assertTrue(testSetup.success, "Execution should have been successful");
        assertTrue(testSetup.firstCell == null, "Output cell should not exists");
        assertTrue(testSetup.outputTable.size() == 0, "Ouptput table should be empty");
    }

    @ParameterizedTest(name = "{0} (with truncation)")
    @MethodSource("provideArgumentsForIntegerTestCase")
    void testConvertDurationToNumberInteger(@SuppressWarnings("unused") final String testName, final Duration input,
        final TimeBasedGranularityUnit targetUnit, final Long expected) throws InvalidSettingsException, IOException {

        var settings = new DurationToNumberNodeSettings();
        settings.m_unit = targetUnit;
        settings.m_appendOrReplaceColumn = ReplaceOrAppend.REPLACE;
        settings.m_filter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_roundingBehaviour = RoundingBehaviour.INTEGER;

        var testSetup = setupAndExecuteWorkflow(settings, DurationCellFactory.create(input));
        var outputValue = testSetup.firstCell;

        assertFalse(outputValue.isMissing(), "Output cell is missing");
        assertTrue(outputValue.getType().isCompatible(LongValue.class),
            "Expected a long output cell, but got a cell of type %s".formatted(outputValue.getType()));

        long actual = ((LongValue)outputValue).getLongValue();
        assertEquals(expected, actual, "Output value is not as expected");
    }

    @ParameterizedTest(name = "{0} (no truncation)")
    @MethodSource("provideArgumentsForDoubleTestCase")
    void testConvertDurationToNumberDouble(@SuppressWarnings("unused") final String testName, final Duration input,
        final TimeBasedGranularityUnit targetUnit, final Double expected) throws InvalidSettingsException, IOException {

        var settings = new DurationToNumberNodeSettings();
        settings.m_unit = targetUnit;
        settings.m_appendOrReplaceColumn = ReplaceOrAppend.REPLACE;
        settings.m_filter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_roundingBehaviour = RoundingBehaviour.DOUBLE;

        var testSetup = setupAndExecuteWorkflow(settings, DurationCellFactory.create(input));
        var outputValue = testSetup.firstCell;

        assertFalse(outputValue.isMissing(), "Output cell is missing");
        assertTrue(outputValue.getType().isCompatible(DoubleValue.class),
            "Expected a double output cell, but got a cell of type %s".formatted(outputValue.getType()));

        double actual = ((DoubleValue)outputValue).getDoubleValue();
        assertEquals(expected, actual, 1e-9, "Output value is not as expected");
    }

    record TestSetup(BufferedDataTable outputTable, DataCell firstCell, boolean success) {
    }

    static TestSetup setupAndExecuteWorkflow(final DurationToNumberNodeSettings settings, final DataCell cellToAdd)
        throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new DurationToNumberNodeFactory2());

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(SETTINGS_CLASS, settings, modelSettings);
        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // populate the input table
        var inputTableSpecBuilder = new TableTestUtil.SpecBuilder();
        if (cellToAdd != null) {
            inputTableSpecBuilder = inputTableSpecBuilder.addColumn(INPUT_COLUMN, cellToAdd.getType());
        } else {
            inputTableSpecBuilder = inputTableSpecBuilder.addColumn(INPUT_COLUMN, DurationCellFactory.TYPE);
        }
        var inputTableSpec = inputTableSpecBuilder.build();
        var inputTableBuilder = new TableTestUtil.TableBuilder(inputTableSpec);
        if (cellToAdd != null) {
            inputTableBuilder = inputTableBuilder.addRow(cellToAdd);
        }
        var inputTable = inputTableBuilder.build();
        var tableSupplierNode =
            WorkflowManagerUtil.createAndAddNode(workflowManager, new InputTableNode.InputDataNodeFactory(inputTable));

        // link the nodes
        workflowManager.addConnection(tableSupplierNode.getID(), 1, node.getID(), 1);

        // execute and wait...
        var success = workflowManager.executeAllAndWaitUntilDone();

        var outputTable = (BufferedDataTable)node.getOutPort(1).getPortObject();

        if (outputTable.size() == 0) {
            return new TestSetup(outputTable, null, success);
        }
        try (var it = outputTable.iterator()) {
            return new TestSetup( //
                outputTable, //
                it.next().getCell(0), //
                success //
            );
        }

    }
}
