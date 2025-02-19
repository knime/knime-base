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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.preproc.constantvalue.ConstantValueColumnNodeSettings.NewColumnSettings.AppendOrReplace;
import org.knime.base.node.preproc.constantvalue.ConstantValueColumnNodeSettings.NewColumnSettings.CustomOrMissingValue;
import org.knime.base.node.preproc.constantvalue.ConstantValueColumnNodeSettings.NewColumnSettings.SupportedColumnType;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.testing.util.InputTableNode.InputDataNodeFactory;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings({"restriction", "static-method"})
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

    static Stream<Arguments> provideTypeTestCases() {
        return Arrays.stream(SupportedColumnType.values()) //
            .map(type -> Arguments.of(type, type.m_defaultValue));
    }

    @Test
    void testAppendColumn() throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToAppend = "new column";
        individualColumnSettings.m_type = SupportedColumnType.DOUBLE;
        individualColumnSettings.m_value = "42.0";
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.APPEND;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.CUSTOM;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};
        applySettings();

        var outputTable = setupAndExecuteWorkflow();

        // check the output table
        assertEquals(2, outputTable.getSpec().getNumColumns(), "Expected 2 column in output table");
        assertEquals(SupportedColumnType.DOUBLE.m_correspondingKnimeType,
            outputTable.getSpec().getColumnSpec(1).getType(), "Unexpected type in output table");
        assertEquals(SupportedColumnType.STRING.m_correspondingKnimeType,
            outputTable.getSpec().getColumnSpec(0).getType(), "Unexpected type in output table");

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
        individualColumnSettings.m_type = SupportedColumnType.DOUBLE;
        individualColumnSettings.m_value = "42.0";
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.REPLACE;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.CUSTOM;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};
        applySettings();

        var outputTable = setupAndExecuteWorkflow();

        // check the output table
        assertEquals(1, outputTable.getSpec().getNumColumns(), "Expected 1 column in output table");
        assertEquals(SupportedColumnType.DOUBLE.m_correspondingKnimeType,
            outputTable.getSpec().getColumnSpec(0).getType(), "Unexpected type in output table");

        try (var it = outputTable.iterator()) {
            while (it.hasNext()) {
                var row = it.next();
                assertEquals(42.0, ((DoubleValue)row.getCell(0)).getDoubleValue(), "Unexpected value in output table");
            }
        }
    }

    @Test
    void testInsertMissing() throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToReplace = "old column";
        individualColumnSettings.m_type = SupportedColumnType.DOUBLE;
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.REPLACE;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.MISSING;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};
        applySettings();

        var outputTable = setupAndExecuteWorkflow();

        // check the output table
        assertEquals(1, outputTable.getSpec().getNumColumns(), "Expected 1 column in output table");
        assertEquals(SupportedColumnType.DOUBLE.m_correspondingKnimeType,
            outputTable.getSpec().getColumnSpec(0).getType(), "Unexpected type in output table");

        try (var it = outputTable.iterator()) {
            while (it.hasNext()) {
                var row = it.next();
                assertTrue(row.getCell(0).isMissing(), "Expected new column to be missing");
            }
        }
    }

    @ParameterizedTest(name = "testType{0}")
    @MethodSource("provideTypeTestCases")
    void testType(final SupportedColumnType type, final String inputValue)
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

        // check the output table
        assertEquals(1, outputTable.getSpec().getNumColumns(), "Expected 1 column in output table");
        assertEquals(type.m_correspondingKnimeType, outputTable.getSpec().getColumnSpec(0).getType(),
            "Unexpected type in output table");

        try (var it = outputTable.iterator()) {
            while (it.hasNext()) {
                var row = it.next();
                assertEquals(inputValue, row.getCell(0).toString(), "Unexpected value in output table");
            }
        }
    }

    @Test
    void testValidateSettingsFailsWhenValueIsInvalid() throws InvalidSettingsException, IOException {
        var individualColumnSettings = new ConstantValueColumnNodeSettings.NewColumnSettings();
        individualColumnSettings.m_columnNameToReplace = "old column";
        individualColumnSettings.m_type = SupportedColumnType.DOUBLE;
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
        individualColumnSettings.m_type = SupportedColumnType.DOUBLE;
        individualColumnSettings.m_value = "42.0";
        individualColumnSettings.m_replaceOrAppend = AppendOrReplace.APPEND;
        individualColumnSettings.m_customOrMissingValue = CustomOrMissingValue.CUSTOM;
        m_settings.m_newColumnSettings =
            new ConstantValueColumnNodeSettings.NewColumnSettings[]{individualColumnSettings};

        var model = new ConstantValueColumnNodeModel2(ConstantValueColumnNodeFactory2.CONFIGURATION);
        assertThrows(InvalidSettingsException.class, () -> {
            model.validateSettings(m_settings);
        }, "Expected an exception for invalid column name");
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
