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
package org.knime.time.node.manipulate.datetimeround;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.InputTableNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;
import org.knime.time.util.ReplaceOrAppend;

@SuppressWarnings({"restriction", "squid:S5960"})
final class DateRoundNodeModelTest {

    private NativeNodeContainer m_dateTimeRoundNode;

    private WorkflowManager m_wfm;

    private static final String INPUT_COLUMN = "test_input";

    private static final String NODE_NAME = "DateRoundNode";

    private static final Class<? extends DefaultNodeSettings> SETTINGS_CLASS = DateRoundNodeSettings.class;

    @BeforeEach
    void resetWorkflow() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
        m_dateTimeRoundNode = WorkflowManagerUtil.createAndAddNode(m_wfm, new DateRoundNodeFactory());
    }

    @Test
    void canBeExecuted() throws InvalidSettingsException {

        final var settings = new DateRoundNodeSettings();
        setSettings(settings);

        InputTableNode.addDefaultTableToNodeInputPort(m_wfm, m_dateTimeRoundNode, 1);

        assertTrue(m_wfm.executeAllAndWaitUntilDone());
        assertTrue(m_dateTimeRoundNode.getNodeContainerState().isExecuted());
    }

    @Test
    void appendReplaceTimeBasedRounding() throws InvalidSettingsException {

        String[] columnNamesToRound = {InputTableNode.COLUMN_LOCAL_DATE, InputTableNode.COLUMN_LOCAL_DATE_TIME,
            InputTableNode.COLUMN_ZONED_DATE_TIME};

        final var settings = new DateRoundNodeSettings();
        settings.m_columnFilter = new ColumnFilter(columnNamesToRound);

        testAppendingColumns(columnNamesToRound, settings);
        testReplacingColumns(settings);
    }

    @Test
    public void testThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new DateRoundNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});

        var testSetup = setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.success, "Execution should have been successful");
        assertTrue(testSetup.firstCell.isMissing(), "Output cell should be missing");
    }

    @Test
    void testThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        var settings = new DateRoundNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});

        var testSetup = setupAndExecuteWorkflow(settings, null);

        assertTrue(testSetup.success, "Execution should have been successful");
        assertTrue(testSetup.firstCell == null, "Output cell should not exists");
        assertTrue(testSetup.outputTable.size() == 0, "Ouptput table should be empty");
    }

    private void testAppendingColumns(final String[] columnNamesToRound, final DateRoundNodeSettings settings)
        throws InvalidSettingsException {

        settings.m_replaceOrAppend = ReplaceOrAppend.APPEND;
        settings.m_outputColumnSuffix = "TheSuffixToAppend";
        setSettings(settings);

        InputTableNode.addDefaultTableToNodeInputPort(m_wfm, m_dateTimeRoundNode, 1);
        assertTrue(m_wfm.executeAllAndWaitUntilDone(), "expected workflow to execute successfully");

        var outputTable = (BufferedDataTable)m_dateTimeRoundNode.getOutPort(1).getPortObject();

        Arrays.stream(columnNamesToRound).forEach(columnName -> assertTrue(
            outputTable.getDataTableSpec().containsName(columnName + settings.m_outputColumnSuffix),
            "expected column " + columnName + settings.m_outputColumnSuffix + " to be present in the output table"));
    }

    private void testReplacingColumns(final DateRoundNodeSettings settings) throws InvalidSettingsException {

        settings.m_replaceOrAppend = ReplaceOrAppend.REPLACE;
        setSettings(settings);

        var inputTable = InputTableNode.createDefaultTestTable();

        InputTableNode.addTableToNodeInputPort(m_wfm, inputTable, m_dateTimeRoundNode, 1);
        assertTrue(m_wfm.executeAllAndWaitUntilDone());

        var outputTable = (BufferedDataTable)m_dateTimeRoundNode.getOutPort(1).getPortObject();

        assertTrue(outputTable.getDataTableSpec().equalStructure(inputTable.get().getDataTableSpec()),
            "The output table should have the same structure as the input table");

    }

    private void setSettings(final DateRoundNodeSettings settings) throws InvalidSettingsException {
        final var nodeSettings = new NodeSettings("DateTimeRoundNode");
        m_wfm.saveNodeSettings(m_dateTimeRoundNode.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(DateRoundNodeSettings.class, settings, modelSettings);
        m_wfm.loadNodeSettings(m_dateTimeRoundNode.getID(), nodeSettings);
    }

    // The rounding logic is implemented in the TimeRoundingUtil and DateRoundingUtil classes.
    // There are dedicated tests for these classes, so we do not need to test the rounding logic here.

    record TestSetup(BufferedDataTable outputTable, DataCell firstCell, boolean success) {
    }

    static TestSetup setupAndExecuteWorkflow(final DateRoundNodeSettings settings, final DataCell cellToAdd)
        throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new DateRoundNodeFactory());

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
            return new TestSetup( //
                outputTable, //
                it.next().getCell(0), //
                success //
            );
        }

    }
}
