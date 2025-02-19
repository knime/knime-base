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
 *   Feb 5, 2025 (david): created
 */
package org.knime.base.node.preproc.constantvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.preproc.constantvalue.ConstantValueColumnNodeSettings.NewColumnSettings.AppendOrReplace;
import org.knime.base.node.preproc.constantvalue.ConstantValueColumnNodeSettings.NewColumnSettings.CustomOrMissingValue;
import org.knime.base.node.util.InputTableNode.InputDataNodeFactory;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ConstantValueColumnNodeModel2Test {

    ConstantValueColumnNodeSettings m_settings;

    WorkflowManager m_workflowManager;

    NativeNodeContainer m_node;

    final static DataTableSpec INPUT_TABLE_SPEC = new TableTestUtil.SpecBuilder() //
        .addColumn("old column", StringCellFactory.TYPE) //
        .build();

    final static Supplier<BufferedDataTable> INPUT_TABLE = new TableTestUtil.TableBuilder(INPUT_TABLE_SPEC) //
        .addRow(StringCellFactory.create("foo")) //
        .addRow(StringCellFactory.create("bar")) //
        .addRow(StringCellFactory.create("baz")) //
        .build();

    @BeforeEach
    void setup() throws IOException {
        m_settings = new ConstantValueColumnNodeSettings();
        m_workflowManager = WorkflowManagerUtil.createEmptyWorkflow();
        m_node = WorkflowManagerUtil.createAndAddNode(m_workflowManager, new ConstantValueColumnNodeFactory2());
    }

    void applySettings() throws InvalidSettingsException {
        final var nodeSettings = new NodeSettings("ConstantValueColumnNode");
        m_workflowManager.saveNodeSettings(m_node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(ConstantValueColumnNodeSettings.class, m_settings, modelSettings);
        m_workflowManager.loadNodeSettings(m_node.getID(), nodeSettings);
    }

    /**
     * Test a variety of data types. Since the types all defer to their own factory, we don't need to overdo it here, or
     * we'd basically just be testing whether the factory is correctly implemented.
     */
    static Stream<Arguments> provideTypeTestCases() {
        record TestCase(DataType type, String inputValue, Predicate<DataCell> verifyValue) {
        }
        var outTestCases = List.of( //
            new TestCase(StringCellFactory.TYPE, "foo", c -> "foo".equals(((StringValue)c).getStringValue())), //
            new TestCase(DoubleCellFactory.TYPE, "42.0", c -> 42.0 == ((DoubleValue)c).getDoubleValue()), // NOSONAR
            new TestCase(LongCellFactory.TYPE, "42", c -> 42L == ((LongValue)c).getLongValue()), //
            new TestCase(BooleanCellFactory.TYPE, "true", c -> ((BooleanValue)c).getBooleanValue()), //
            new TestCase(IntCellFactory.TYPE, "42", c -> 42 == ((IntValue)c).getIntValue()) //
        );

        return outTestCases.stream().map( //
            tc -> Arguments.of(tc.type, tc.inputValue, tc.verifyValue) //
        );
    }

    /**
     * Test cases that should fail when the node is executed.
     */
    static Stream<Arguments> provideFailingTestCases() {
        record FailingTestCase(DataType type, String inputValue) {
        }
        var outTestCases = List.of( //
            new FailingTestCase(DoubleCellFactory.TYPE, "foo"), //
            new FailingTestCase(LongCellFactory.TYPE, "foo"), //
            new FailingTestCase(IntCellFactory.TYPE, "foo") //
        );

        return outTestCases.stream().map( //
            tc -> Arguments.of(tc.type, tc.inputValue) //
        );
    }

    @Test
    void testAppendColumn() throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToAppend = "new column";
        individualColumnSettings.m_type = DoubleCellFactory.TYPE;
        individualColumnSettings.m_value = "42.0";
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.APPEND;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.CUSTOM;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};
        applySettings();

        var outputTable = setupAndExecuteWorkflow();

        assertTrue(m_node.getNodeContainerState().isExecuted(), "Expected node to be executed");

        // check the output table
        assertEquals(2, outputTable.getSpec().getNumColumns(), "Expected 2 column in output table");
        assertEquals(DoubleCellFactory.TYPE, outputTable.getSpec().getColumnSpec(1).getType(),
            "Unexpected type in output table");
        assertEquals(StringCellFactory.TYPE, outputTable.getSpec().getColumnSpec(0).getType(),
            "Unexpected type in output table");

        try (var it = outputTable.iterator()) {
            while (it.hasNext()) {
                var row = it.next();
                assertEquals(42.0, ((DoubleValue)row.getCell(1)).getDoubleValue(), "Unexpected value in output table");
            }
        }
    }

    @Test
    void testReplaceColumn() throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToReplace = "old column";
        individualColumnSettings.m_type = DoubleCellFactory.TYPE;
        individualColumnSettings.m_value = "42.0";
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.REPLACE;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.CUSTOM;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};
        applySettings();

        var outputTable = setupAndExecuteWorkflow();

        assertTrue(m_node.getNodeContainerState().isExecuted(), "Expected node to be executed");

        // check the output table
        assertEquals(1, outputTable.getSpec().getNumColumns(), "Expected 1 column in output table");
        assertEquals(DoubleCellFactory.TYPE, outputTable.getSpec().getColumnSpec(0).getType(),
            "Unexpected type in output table");

        try (var it = outputTable.iterator()) {
            while (it.hasNext()) {
                var row = it.next();
                assertEquals(42.0, ((DoubleValue)row.getCell(0)).getDoubleValue(), "Unexpected value in output table"); // NOSONAR (double comparison is fine here)
            }
        }
    }

    @Test
    void testInsertMissing() throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToReplace = "old column";
        individualColumnSettings.m_type = DoubleCellFactory.TYPE;
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.REPLACE;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.MISSING;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};
        applySettings();

        var outputTable = setupAndExecuteWorkflow();

        assertTrue(m_node.getNodeContainerState().isExecuted(), "Expected node to be executed");

        // check the output table
        assertEquals(1, outputTable.getSpec().getNumColumns(), "Expected 1 column in output table");
        assertEquals(DoubleCellFactory.TYPE, outputTable.getSpec().getColumnSpec(0).getType(),
            "Unexpected type in output table");

        try (var it = outputTable.iterator()) {
            while (it.hasNext()) {
                var row = it.next();
                assertTrue(row.getCell(0).isMissing(), "Expected new column to be missing");
            }
        }
    }

    @ParameterizedTest(name = "testType ''{0}''")
    @MethodSource("provideTypeTestCases")
    void testType(final DataType type, final String inputValue, final Predicate<DataCell> verifyValue)
        throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToReplace = "old column";
        individualColumnSettings.m_type = type;
        individualColumnSettings.m_value = inputValue;
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.REPLACE;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.CUSTOM;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};
        applySettings();

        var outputTable = setupAndExecuteWorkflow();

        assertTrue(m_node.getNodeContainerState().isExecuted(), "Expected node to be executed");

        // check the output table
        assertEquals(1, outputTable.getSpec().getNumColumns(), "Expected 1 column in output table");
        assertEquals(type, outputTable.getSpec().getColumnSpec(0).getType(), "Unexpected type in output table");

        try (var it = outputTable.iterator()) {
            while (it.hasNext()) {
                var row = it.next();
                assertTrue(verifyValue.test(row.getCell(0)), "Unexpected value in output table for input type " + type);
            }
        }
    }

    @ParameterizedTest(name = "testFailingType ''{0}''")
    @MethodSource("provideFailingTestCases")
    void testFailingType(final DataType type, final String inputValue) throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToReplace = "old column";
        individualColumnSettings.m_type = type;
        individualColumnSettings.m_value = inputValue;
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.REPLACE;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.CUSTOM;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};

        var model = new ConstantValueColumnNodeModel2(ConstantValueColumnNodeFactory2.CONFIGURATION);

        assertThrows(InvalidSettingsException.class, () -> {
            model.validateSettings(m_settings);
        }, "Expected an exception for invalid value '%s' for type %s".formatted(inputValue, type.toPrettyString()));
    }

    @Test
    void testValidateSettingsFailsWhenValueIsInvalid() throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToReplace = "old column";
        individualColumnSettings.m_type = DoubleCellFactory.TYPE;
        individualColumnSettings.m_value = "42.0x"; // invalid value
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.REPLACE;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.CUSTOM;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};

        var model = new ConstantValueColumnNodeModel2(ConstantValueColumnNodeFactory2.CONFIGURATION);
        assertThrows(InvalidSettingsException.class, () -> {
            model.validateSettings(m_settings);
        }, "Expected an exception for invalid value");
    }

    @Test
    void testValidateSettingsFailsWhenAppendColumnNameIsBlank() throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToAppend = ""; // invalid value
        individualColumnSettings.m_type = DoubleCellFactory.TYPE;
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.APPEND;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.MISSING;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};

        var model = new ConstantValueColumnNodeModel2(ConstantValueColumnNodeFactory2.CONFIGURATION);
        assertThrows(InvalidSettingsException.class, () -> {
            model.validateSettings(m_settings);
        }, "Expected an exception for invalid column name");
    }

    @Test
    void testConfigureFailsWhenNonExistentColumnIsReplaced() throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToReplace = "non-existent column";
        individualColumnSettings.m_type = DoubleCellFactory.TYPE;
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.REPLACE;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.MISSING;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};
        applySettings();

        setupAndExecuteWorkflow();

        assertFalse(m_node.getNodeContainerState().isConfigured(),
            "Expected configure to fail when replacing a non-existent column");
    }

    /**
     * It should execute, but there should be a new column added using a UniqueNameGenerator so it will have a different
     * name.
     */
    @Test
    void testExecutesWhenExistingColumnIsAppended() throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToAppend = "old column";
        individualColumnSettings.m_type = DoubleCellFactory.TYPE;
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.APPEND;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.MISSING;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};
        applySettings();

        var outputTable = setupAndExecuteWorkflow();

        assertTrue(m_node.getNodeContainerState().isExecuted(),
            "Expected node to execute even when appending an existing column name");

        // check the output table columns
        assertEquals(2, outputTable.getSpec().getNumColumns(), "Expected 2 columns in output table");
        assertTrue(outputTable.getSpec().containsName("old column"), "Expected old column to be in output table");
        assertTrue(outputTable.getSpec().containsName("old column (#1)"),
            "Expected new column to be in output table with unique name");
    }

    @Test
    void testValidateSettingsFailsWhenAppendColumnNameAppearsTwice() throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToAppend = "foo";
        individualColumnSettings.m_type = DoubleCellFactory.TYPE;
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.APPEND;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.MISSING;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings, individualColumnSettings};

        var model = new ConstantValueColumnNodeModel2(ConstantValueColumnNodeFactory2.CONFIGURATION);
        assertThrows(InvalidSettingsException.class, () -> {
            model.validateSettings(m_settings);
        }, "Expected an exception for duplicate column name");
    }

    @Test
    void testValidateSettingsFailsWhenReplacedColumnNameAppearsTwice() throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToReplace = "foo";
        individualColumnSettings.m_type = DoubleCellFactory.TYPE;
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.REPLACE;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.MISSING;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings, individualColumnSettings};

        var model = new ConstantValueColumnNodeModel2(ConstantValueColumnNodeFactory2.CONFIGURATION);
        assertThrows(InvalidSettingsException.class, () -> {
            model.validateSettings(m_settings);
        }, "Expected an exception for duplicate column name");
    }

    @Test
    void testWarnsWhenNoColumnsAdded() throws InvalidSettingsException, IOException {
        m_settings.m_newColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings[0];

        var warningReceived = new AtomicBoolean(false);

        m_node.getNodeModel().addWarningListener(warning -> warningReceived.set(true));

        applySettings();
        setupAndExecuteWorkflow();

        assertTrue(warningReceived.get(), "Expected a warning when no columns are added");
    }

    private BufferedDataTable setupAndExecuteWorkflow() throws InvalidSettingsException, IOException {
        // populate the input table
        var tableSupplierNode =
            WorkflowManagerUtil.createAndAddNode(m_workflowManager, new InputDataNodeFactory(INPUT_TABLE));

        // link the nodes
        m_workflowManager.addConnection(tableSupplierNode.getID(), 1, m_node.getID(), 1);

        // execute and wait...
        m_workflowManager.executeAllAndWaitUntilDone();

        return (BufferedDataTable)m_node.getOutPort(1).getPortObject();
    }
}
