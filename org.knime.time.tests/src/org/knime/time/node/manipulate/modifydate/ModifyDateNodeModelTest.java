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
 *   Jan 15, 2025 (sillyem): created
 */
package org.knime.time.node.manipulate.modifydate;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.knime.InputTableNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;
import org.knime.time.node.manipulate.modifydate.ModifyDateNodeSettings.BehaviourType;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ModifyDateNodeModelTest {
    private static final String INPUT_COLUMN = "test_input";

    private static final String NODE_NAME = "ModifyDateNode";

    private static final Class<? extends DefaultNodeSettings> SETTINGS_CLASS = ModifyDateNodeSettings.class;

    @Test
    void testAppendThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new ModifyDateNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.APPEND;

        var testSetup = setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.success, "Execution should have been successful");
        assertTrue(testSetup.firstCell.isMissing(), "Output cell should be missing");
    }

    @Test
    void testChangeThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new ModifyDateNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.CHANGE;

        var testSetup = setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.success, "Execution should have been successful");
        assertTrue(testSetup.firstCell.isMissing(), "Output cell should be missing");
    }

    @Test
    void testRemoveThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new ModifyDateNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.REMOVE;

        var testSetup = setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.success, "Execution should have been successful");
        assertTrue(testSetup.firstCell.isMissing(), "Output cell should be missing");
    }

    @Test
    void testAppendThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        var settings = new ModifyDateNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.APPEND;

        var testSetup = setupAndExecuteWorkflow(settings, null);

        assertTrue(testSetup.success, "Execution should have been successful");
        assertTrue(testSetup.firstCell == null, "Output cell should not exists");
        assertTrue(testSetup.outputTable.size() == 0, "Ouptput table should be empty");
    }

    @Test
    void testChangeThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        var settings = new ModifyDateNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.CHANGE;

        var testSetup = setupAndExecuteWorkflow(settings, null);

        assertTrue(testSetup.success, "Execution should have been successful");
        assertTrue(testSetup.firstCell == null, "Output cell should not exists");
        assertTrue(testSetup.outputTable.size() == 0, "Ouptput table should be empty");
    }

    @Test
    void testRemoveThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        var settings = new ModifyDateNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.REMOVE;

        var testSetup = setupAndExecuteWorkflow(settings, null);

        assertTrue(testSetup.success, "Execution should have been successful");
        assertTrue(testSetup.firstCell == null, "Output cell should not exists");
        assertTrue(testSetup.outputTable.size() == 0, "Ouptput table should be empty");
    }

    record TestSetup(BufferedDataTable outputTable, DataCell firstCell, boolean success) {
    }

    static TestSetup setupAndExecuteWorkflow(final ModifyDateNodeSettings settings, final DataCell cellToAdd)
        throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new ModifyDateNodeFactory2());

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(SETTINGS_CLASS, settings, modelSettings);
        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        var inputTableSpecBuilder = new TableTestUtil.SpecBuilder();
        if (cellToAdd != null) {
            inputTableSpecBuilder = inputTableSpecBuilder.addColumn(INPUT_COLUMN, cellToAdd.getType());
        } else {
            inputTableSpecBuilder = inputTableSpecBuilder.addColumn(INPUT_COLUMN, LocalDateTimeCellFactory.TYPE);
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
            var firstCell = it.next().getCell(0);
            return new TestSetup(outputTable, firstCell, success);
        }
    }
}
