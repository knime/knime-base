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
package org.knime.time.node.extract.durationperiod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.InputTableNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.LongValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings({"restriction", "squid:S5960", "squid:S1192"})
class ExtractDurationPeriodFieldsNodeModelTest {

    private record TestCase<I>(I input, ExtractableField targetUnit, long expectedTruncatedOutput) {
    }

    private static final String INPUT_COLUMN = "test_input";

    private static final String NODE_NAME = "ExtractDurationPeriodFieldsNodeSettings";

    private static final Class<? extends DefaultNodeSettings> SETTINGS_CLASS =
        ExtractDurationPeriodFieldsNodeSettings.class;

    /**
     * Test cases for extracting fields from a {@link Duration} value. Every one of them will also be negated and tested
     * again.
     */
    private static final List<TestCase<Duration>> DURATION_TEST_CASES = List.of( //
        new TestCase<>(Duration.parse("PT1H2M3.123456789S"), ExtractableField.HOURS, 1), //
        new TestCase<>(Duration.parse("PT1H2M3.123456789S"), ExtractableField.MINUTES, 2), //
        new TestCase<>(Duration.parse("PT1H2M3.123456789S"), ExtractableField.SECONDS, 3), //
        new TestCase<>(Duration.parse("PT1H2M3.123456789S"), ExtractableField.MILLIS_PART, 123), //
        new TestCase<>(Duration.parse("PT1H2M3.123456789S"), ExtractableField.MICROS_PART, 456), //
        new TestCase<>(Duration.parse("PT1H2M3.123456789S"), ExtractableField.NANOS_PART, 789), //
        new TestCase<>(Duration.parse("PT1H2M3.123456789S"), ExtractableField.MILLIS_ALL, 123), //
        new TestCase<>(Duration.parse("PT1H2M3.123456789S"), ExtractableField.MICROS_ALL, 123456), //
        new TestCase<>(Duration.parse("PT1H2M3.123456789S"), ExtractableField.NANOS_ALL, 123456789), //
        new TestCase<>(Duration.parse("PT25H"), ExtractableField.HOURS, 25) // with >24h
    );

    /**
     * Test cases for extracting fields from a {@link Period} value. Every one of them will also be negated and tested
     * again.
     */
    private static final List<TestCase<Period>> PERIOD_TEST_CASES = List.of( //
        new TestCase<>(Period.parse("P1Y2M3D"), ExtractableField.YEARS, 1), //
        new TestCase<>(Period.parse("P1Y2M3D"), ExtractableField.MONTHS, 2), //
        new TestCase<>(Period.parse("P1Y2M3D"), ExtractableField.DAYS, 3) //
    );

    static Stream<Arguments> provideArgumentsForDurationTestCase() {
        var testCasesPositive = DURATION_TEST_CASES.stream().map(testCase -> Arguments.of( //
            "Extract %s from '%s'".formatted(testCase.targetUnit.name(), testCase.input.toString()), //
            testCase.input, //
            testCase.targetUnit, //
            testCase.expectedTruncatedOutput //
        ));

        var testCasesNegative = DURATION_TEST_CASES.stream().map(testCase -> Arguments.of( //
            "Extract %s from '-%s'".formatted(testCase.targetUnit.name(), testCase.input.toString()), //
            testCase.input.negated(), //
            testCase.targetUnit, //
            -testCase.expectedTruncatedOutput //
        ));

        return Stream.concat(testCasesPositive, testCasesNegative);
    }

    static Stream<Arguments> provideArgumentsForPeriodTestCase() {
        var testCasesPositive = PERIOD_TEST_CASES.stream().map(testCase -> Arguments.of( //
            "Extract %s from '%s'".formatted(testCase.targetUnit.name(), testCase.input.toString()), //
            testCase.input, //
            testCase.targetUnit, //
            testCase.expectedTruncatedOutput //
        ));

        var testCasesNegative = PERIOD_TEST_CASES.stream().map(testCase -> Arguments.of( //
            "Extract %s from '-%s'".formatted(testCase.targetUnit.name(), testCase.input.toString()), //
            testCase.input.negated(), //
            testCase.targetUnit, //
            -testCase.expectedTruncatedOutput //
        ));

        return Stream.concat(testCasesPositive, testCasesNegative);
    }

    @Test
    void testThatOutputColumnNameIsRespected() throws InvalidSettingsException, IOException {
        var settings = new ExtractDurationPeriodFieldsNodeSettings();
        settings.m_extractFields = new ExtractFieldSettings[]{ //
            new ExtractFieldSettings(ExtractableField.HOURS, "some_output_col_name") //
        };
        settings.m_selectedColumn = INPUT_COLUMN;

        var outputTable =
            setupAndExecuteWorkflow(settings, DurationCellFactory.create(Duration.ofHours(1))).outputTable;

        var outputColumnName = outputTable.getDataTableSpec().getColumnNames()[1];
        assertEquals("some_output_col_name", outputColumnName, "Output column name is not as expected");
    }

    @Test
    void testThatEmptyColumnNameUsesPlaceholderValueDuration() throws InvalidSettingsException, IOException {
        var durationFields = Arrays.stream(ExtractableField.values()) //
            .filter(f -> f.isCompatibleWith(DurationCellFactory.TYPE)) //
            .toList();

        var settings = new ExtractDurationPeriodFieldsNodeSettings();
        settings.m_extractFields = durationFields.stream() //
            .map(f -> new ExtractFieldSettings(f, "")) //
            .toArray(ExtractFieldSettings[]::new);
        settings.m_selectedColumn = INPUT_COLUMN;

        var outputTable =
            setupAndExecuteWorkflow(settings, DurationCellFactory.create(Duration.ofHours(1))).outputTable;

        var outputColumnNames = outputTable.getDataTableSpec().getColumnNames();

        for (int i = 0; i < durationFields.size(); i++) {
            assertEquals(durationFields.get(i).niceName(), outputColumnNames[i + 1],
                "Output column name is not as expected");
        }
    }

    @Test
    void testThatEmptyColumnNameUsesPlaceholderValuePeriod() throws InvalidSettingsException, IOException {
        var periodFields = Arrays.stream(ExtractableField.values()) //
            .filter(f -> f.isCompatibleWith(PeriodCellFactory.TYPE)) //
            .toList();

        var settings = new ExtractDurationPeriodFieldsNodeSettings();
        settings.m_extractFields = periodFields.stream() //
            .map(f -> new ExtractFieldSettings(f, "")) //
            .toArray(ExtractFieldSettings[]::new);
        settings.m_selectedColumn = INPUT_COLUMN;

        var outputTable = setupAndExecuteWorkflow(settings, PeriodCellFactory.create(Period.ofDays(1))).outputTable;

        var outputColumnNames = outputTable.getDataTableSpec().getColumnNames();

        for (int i = 0; i < periodFields.size(); i++) {
            assertEquals(periodFields.get(i).niceName(), outputColumnNames[i + 1],
                "Output column name is not as expected");
        }
    }

    @Test
    void testThatDuplicateColumnNamesGivesConfigurationError() throws InvalidSettingsException, IOException {
        var settings = new ExtractDurationPeriodFieldsNodeSettings();
        settings.m_extractFields = new ExtractFieldSettings[]{ //
            new ExtractFieldSettings(ExtractableField.HOURS, "some_output_col_name"), //
            new ExtractFieldSettings(ExtractableField.MONTHS, "some_output_col_name") //
        };
        settings.m_selectedColumn = INPUT_COLUMN;

        var testSetup = setupAndExecuteWorkflow(settings, DurationCellFactory.create(Duration.ofHours(1)));

        assertFalse(testSetup.configured, "Configuration should have failed");
    }

    @Test
    void testThatExistingColumnNamesAreReplacedWithUniqueName() throws InvalidSettingsException, IOException {
        var settings = new ExtractDurationPeriodFieldsNodeSettings();
        settings.m_extractFields = new ExtractFieldSettings[]{ //
            new ExtractFieldSettings(ExtractableField.HOURS, INPUT_COLUMN), //
        };
        settings.m_selectedColumn = INPUT_COLUMN;

        var outputTable =
            setupAndExecuteWorkflow(settings, DurationCellFactory.create(Duration.ofHours(1))).outputTable;

        assertTrue(outputTable.getDataTableSpec().getColumnNames()[1].contains("#1"),
            "Expected a unique column name like '" + INPUT_COLUMN + "#1'");
    }

    @Test
    void testThatMissingColumnGivesExecutionError() throws InvalidSettingsException, IOException {
        var settings = new ExtractDurationPeriodFieldsNodeSettings();
        settings.m_extractFields = new ExtractFieldSettings[]{ //
            new ExtractFieldSettings(ExtractableField.HOURS, "some_output_col_name") //
        };
        settings.m_selectedColumn = "non_existing_column";

        var testSetup = setupAndExecuteWorkflow(settings, DurationCellFactory.create(Duration.ZERO));

        assertFalse(testSetup.executed, "Node should not have executed");
    }

    @ParameterizedTest(name = "{0} (time/duration based)")
    @MethodSource("provideArgumentsForDurationTestCase")
    void testExtractDurationPart(@SuppressWarnings("unused") final String testName, final Duration input,
        final ExtractableField targetUnit, final Long expected) throws InvalidSettingsException, IOException {

        var settings = new ExtractDurationPeriodFieldsNodeSettings();
        settings.m_extractFields = new ExtractFieldSettings[]{ //
            new ExtractFieldSettings(targetUnit, targetUnit.name()) //
        };
        settings.m_selectedColumn = INPUT_COLUMN;

        var outputTable = setupAndExecuteWorkflow(settings, DurationCellFactory.create(input)).outputTable;

        try (var it = outputTable.iterator()) {
            var outputValue = it.next().getCell(1);

            assertFalse(outputValue.isMissing(), "Output cell is missing");
            assertTrue(outputValue.getType().isCompatible(LongValue.class),
                "Expected a long output cell, but got a cell of type %s".formatted(outputValue.getType()));

            long actual = ((LongValue)outputValue).getLongValue();
            assertEquals(expected, actual, "Output value is not as expected");
        }
    }

    @ParameterizedTest(name = "{0} (date/period based)")
    @MethodSource("provideArgumentsForPeriodTestCase")
    void testExtractPeriodPart(@SuppressWarnings("unused") final String testName, final Period input,
        final ExtractableField targetUnit, final Long expected) throws InvalidSettingsException, IOException {

        var settings = new ExtractDurationPeriodFieldsNodeSettings();
        settings.m_extractFields = new ExtractFieldSettings[]{ //
            new ExtractFieldSettings(targetUnit, targetUnit.name()) //
        };
        settings.m_selectedColumn = INPUT_COLUMN;

        var outputTable = setupAndExecuteWorkflow(settings, PeriodCellFactory.create(input)).outputTable;

        try (var it = outputTable.iterator()) {
            var outputValue = it.next().getCell(1);

            assertFalse(outputValue.isMissing(), "Output cell is missing");
            assertTrue(outputValue.getType().isCompatible(LongValue.class),
                "Expected a long output cell, but got a cell of type %s".formatted(outputValue.getType()));

            long actual = ((LongValue)outputValue).getLongValue();
            assertEquals(expected, actual, "Output value is not as expected");
        }
    }

    @Test
    void testThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new ExtractDurationPeriodFieldsNodeSettings();
        settings.m_extractFields = new ExtractFieldSettings[]{ //
            new ExtractFieldSettings(ExtractableField.HOURS, "some_output_col_name") //
        };
        settings.m_selectedColumn = INPUT_COLUMN;

        var outputTable = setupAndExecuteWorkflow(settings, new MissingCell("missing")).outputTable;

        try (var it = outputTable.iterator()) {
            var outputValue = it.next().getCell(1);

            assertTrue(outputValue.isMissing(), "Output cell is not missing");
        }
    }

    @Test
    void testExtractingIncompatibleField() throws InvalidSettingsException, IOException {
        var settings = new ExtractDurationPeriodFieldsNodeSettings();
        settings.m_extractFields = new ExtractFieldSettings[]{ //
            new ExtractFieldSettings(ExtractableField.YEARS, "some_output_col_name") //
        };
        settings.m_selectedColumn = INPUT_COLUMN;

        var testSetup = setupAndExecuteWorkflow(settings, DurationCellFactory.create(Duration.ofHours(1)));

        assertFalse(testSetup.executed, "Node should not have executed");
    }

    static record TestSetup(BufferedDataTable outputTable, boolean configured, boolean executed) {
    }

    static TestSetup setupAndExecuteWorkflow(final ExtractDurationPeriodFieldsNodeSettings settings,
        final DataCell cellToAdd) throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new ExtractDurationPeriodFieldsNodeFactory2());

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(SETTINGS_CLASS, settings, modelSettings);

        try {
            workflowManager.loadNodeSettings(node.getID(), nodeSettings);
        } catch (InvalidSettingsException ex) {
            return new TestSetup(null, false, false);
        }

        // populate the input table
        var inputTableSpec = new TableTestUtil.SpecBuilder() //
            .addColumn(INPUT_COLUMN, cellToAdd.getType()) //
            .build();
        var inputTable = new TableTestUtil.TableBuilder(inputTableSpec) //
            .addRow(cellToAdd) //
            .build();
        var tableSupplierNode =
            WorkflowManagerUtil.createAndAddNode(workflowManager, new InputTableNode.InputDataNodeFactory(inputTable));

        // link the nodes
        workflowManager.addConnection(tableSupplierNode.getID(), 1, node.getID(), 1);

        workflowManager.executeAllAndWaitUntilDone();

        return new TestSetup((BufferedDataTable)node.getOutPort(1).getPortObject(), true,
            node.getNodeContainerState().isExecuted());
    }
}
