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
 *   Feb 21, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.flowvariable.tablecoltovariable4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.knime.base.node.flowvariable.tablecoltovariable4.TableColumnToVariable4NodeSettings.MissingOperation;
import org.knime.base.node.util.InputTableNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class TableColumnToVariable4NodeModelTest {

    private static final String NODE_NAME = "TableColumnToVariable4";

    private static final String INPUT_COLUMN = "column1";

    private static final Class<? extends DefaultNodeSettings> SETTINGS_CLASS = TableColumnToVariable4NodeSettings.class;

    @Test
    void testExecute() throws InvalidSettingsException, IOException {
        final var settings = new TableColumnToVariable4NodeSettings();
        settings.m_column = INPUT_COLUMN;
        settings.m_missingOperation = MissingOperation.IGNORE;
        final StringCell[] cells = new StringCell[] {new StringCell("Test1"), new StringCell("Test2"),
            new StringCell("Test1"), new StringCell("Test10")} ;

        var output = setupAndExecuteWorkflow(settings, cells);

        assertTrue(output.status.isExecuted(), "Should execute successfully.");
        final var variables = new HashMap<String, FlowVariable> (output.outFlowStack.getAvailableFlowVariables(new VariableType[] {VariableType.StringType.INSTANCE}));
        variables.remove("knime.workspace"); // Remove default variable to make testing easier

        assertEquals(4, variables.size(), "Should contain correct number of variables");
        for (int i = 0; i < 4; i++) {
            assertTrue(variables.containsKey("rowkey "+i), "Should contain variable");
            assertEquals(VariableType.StringType.INSTANCE, variables.get("rowkey "+i).getVariableType(), "Should have the correct type");
            assertEquals(cells[i].getStringValue(), variables.get("rowkey "+i).getStringValue(), "Should have the correct value");
        }
    }

    @Test
    void testIntColumns() throws InvalidSettingsException, IOException {
        final var settings = new TableColumnToVariable4NodeSettings();
        settings.m_column = INPUT_COLUMN;
        settings.m_missingOperation = MissingOperation.IGNORE;
        final IntCell[] cells = new IntCell[] {new IntCell(5), new IntCell(21),
            new IntCell(43), new IntCell(100)} ;

        var output = setupAndExecuteWorkflow(settings, cells);

        assertTrue(output.status.isExecuted(), "Should execute successfully.");
        final var variables = new HashMap<String, FlowVariable> (output.outFlowStack.getAllAvailableFlowVariables());
        variables.remove("knime.workspace"); // Remove default variable to make testing easier

        assertEquals(4, variables.size(), "Should contain correct number of variables");
        for (int i = 0; i < 4; i++) {
            assertTrue(variables.containsKey("rowkey "+i), "Should contain variable");
            assertEquals(VariableType.IntType.INSTANCE, variables.get("rowkey "+i).getVariableType(), "Should have the correct type");
            assertEquals(cells[i].getIntValue(), variables.get("rowkey "+i).getIntValue(), "Should have the correct value");
        }
    }

    @Test
    void testIgnoreMissing() throws InvalidSettingsException, IOException {
        final var settings = new TableColumnToVariable4NodeSettings();
        settings.m_column = INPUT_COLUMN;
        settings.m_missingOperation = MissingOperation.IGNORE;

        var output = setupAndExecuteWorkflow(settings, new StringCell("First"), new MissingCell(""));

        assertTrue(output.status.isExecuted(), "Should execute successfully.");
        final var variables = new HashMap<String, FlowVariable> (output.outFlowStack.getAllAvailableFlowVariables());
        variables.remove("knime.workspace"); // Remove default variable to make testing easier

        assertEquals(1, variables.size(), "Should contain correct number of variables");
        assertTrue(variables.containsKey("rowkey 0"), "Should contain variable");
        assertEquals(VariableType.StringType.INSTANCE, variables.get("rowkey 0").getVariableType(), "Should have the correct type");
        assertEquals("First", variables.get("rowkey 0").getStringValue(), "Should have the correct value");
    }

    @Test
    void testFailOnMissing() throws InvalidSettingsException, IOException {
        final var settings = new TableColumnToVariable4NodeSettings();
        settings.m_column = INPUT_COLUMN;
        settings.m_missingOperation = MissingOperation.FAIL;

        var output = setupAndExecuteWorkflow(settings, new StringCell("First"), new MissingCell(""));

        assertFalse(output.status.isExecuted(), "Should not execute successfully.");
        final var variables = new HashMap<String, FlowVariable> (output.outFlowStack.getAllAvailableFlowVariables());
        variables.remove("knime.workspace"); // Remove default variable to make testing easier

        assertEquals(0, variables.size(), "Should not contain any variables");
    }

    record TestSetup(FlowObjectStack outFlowStack, NodeContainerState status) {
    }

    private static TestSetup setupAndExecuteWorkflow(final TableColumnToVariable4NodeSettings settings,
        final DataCell... cellToAdd) throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();
        ;
        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new TableColumnToVariable4NodeFactory());
        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(SETTINGS_CLASS, settings, modelSettings);
        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // populate the input table
        var inputTableSpecBuilder = new TableTestUtil.SpecBuilder();
        if (cellToAdd != null && cellToAdd.length != 0) {
            inputTableSpecBuilder = inputTableSpecBuilder.addColumn(INPUT_COLUMN, cellToAdd[0].getType());
        } else {
            inputTableSpecBuilder = inputTableSpecBuilder.addColumn(INPUT_COLUMN, StringCellFactory.TYPE);
        }
        var inputTableSpec = inputTableSpecBuilder.build();
        var inputTableBuilder = new TableTestUtil.TableBuilder(inputTableSpec);
        if (cellToAdd != null) {
            for (var cell : cellToAdd) {
                inputTableBuilder = inputTableBuilder.addRow(cell);
            }
        }
        var inputTable = inputTableBuilder.build();
        var tableSupplierNode =
            WorkflowManagerUtil.createAndAddNode(workflowManager, new InputTableNode.InputDataNodeFactory(inputTable));
        // link the nodes
        workflowManager.addConnection(tableSupplierNode.getID(), 1, node.getID(), 1);
        // execute and wait...
        workflowManager.executeAllAndWaitUntilDone();

        return new TestSetup(node.getOutgoingFlowObjectStack(), workflowManager.getNodeContainerState());
    }
}
