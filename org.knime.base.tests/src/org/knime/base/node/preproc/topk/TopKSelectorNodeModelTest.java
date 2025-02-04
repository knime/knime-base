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
 *   Feb 4, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.topk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.topk.TopKSelectorNodeSettings.FilterMode;
import org.knime.base.node.preproc.topk.TopKSelectorNodeSettings.RowOrder;
import org.knime.base.node.util.InputTableNode;
import org.knime.base.node.util.preproc.SortingUtils.SortingCriterionSettings;
import org.knime.base.node.util.preproc.SortingUtils.SortingOrder;
import org.knime.base.node.util.preproc.SortingUtils.StringComparison;
import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class TopKSelectorNodeModelTest {

    private static final String NODE_NAME = "TopKSelectorNode";

    private static final String[] INPUT_COLUMNS = new String[]{"column1", "column2"};

    private static final Class<? extends DefaultNodeSettings> SETTINGS_CLASS = TopKSelectorNodeSettings.class;

    @Test
    void testExecute() throws InvalidSettingsException, IOException {
        final var settings = new TopKSelectorNodeSettings();
        settings.m_sortingCriteria =
            new SortingCriterionSettings[]{new SortingCriterionSettings(new ColumnSelection(INPUT_COLUMNS[0], null),
                SortingOrder.ASCENDING, StringComparison.NATURAL)};
        settings.m_amount = 2;
        settings.m_filterMode = FilterMode.UNIQUE_VALUES;
        settings.m_rowOrder = RowOrder.INPUT_ORDER;

        var output = setupAndExecuteWorkflow(settings, new StringCell("Test1"), new StringCell("Test2"),
            new StringCell("Test1"), new StringCell("Test10"));

        assertTrue(output.status().isExecuted(), "Should run successfully");
        assertNotNull(output.outputTable(), "Output should not be null");
        assertEquals(3, output.outputTable().size(), "Should contain 3 rows");
        assertEquals("Test1", output.firstCell().toString(), "The first cell should be Test1");
    }

    @Test
    void testMigrationAndExecute() throws InvalidSettingsException, IOException {
        final var settings = new NodeSettings("model");
        final boolean[] ascending = {false};
        settings.addBooleanArray("order", ascending);
        settings.addString("outputOrder", OutputOrder.SORT.name());
        settings.addInt("k", 2);
        final String[] inclCols = {"-ROWKEY -"};
        settings.addStringArray("columns", inclCols);
        settings.addBooleanArray("alphaNumStringComp", true);
        settings.addBoolean("missingsToEnd", false);
        settings.addString("selectionMode", TopKMode.TOP_K_ROWS.getText());

        final var nodeSettings = DefaultNodeSettings.loadSettings(settings, TopKSelectorNodeSettings.class);

        var output = setupAndExecuteWorkflow(nodeSettings,
            new String[]{RowKey.createRowKey(1L).getString(), RowKey.createRowKey(2L).getString(),
                RowKey.createRowKey(10L).getString()},
            new DataCell[]{new StringCell("MyRow1"), new DoubleCell(1.0d)},
            new DataCell[]{new StringCell("MyRow2"), new DoubleCell(2.0d)},
            new DataCell[]{new StringCell("MyRow10"), new DoubleCell(10.0d)});

        assertTrue(output.status.isExecuted(), "Should execute");
        try (final var it = output.outputTable.iterator()) {
            assertEquals(RowKey.createRowKey(10L), it.next().getKey(), "Should have the correct first item");
            assertEquals(RowKey.createRowKey(2L), it.next().getKey(), "Should have the correct second item");
            assertFalse(it.hasNext(), "Shouldn't have more than 2 items");
        }
    }

    @Test
    void testEmptyCriterionFails() throws InvalidSettingsException, IOException {
        final var settings = new TopKSelectorNodeSettings();
        settings.m_sortingCriteria = new SortingCriterionSettings[0];
        settings.m_amount = 2;
        settings.m_filterMode = FilterMode.UNIQUE_VALUES;
        settings.m_rowOrder = RowOrder.INPUT_ORDER;

        var output = setupAndExecuteWorkflow(settings, new StringCell("Test1"), new StringCell("Test2"),
            new StringCell("Test1"), new StringCell("Test10"));

        assertFalse(output.status().isExecuted(), "Should fail");
        assertNull(output.outputTable(), "Output should be null");
    }

    record TestSetup(BufferedDataTable outputTable, DataCell firstCell, NodeContainerState status) {
    }

    static TestSetup setupAndExecuteWorkflow(final TopKSelectorNodeSettings settings, final DataCell... cellToAdd)
        throws InvalidSettingsException, IOException {
        return setupAndExecuteWorkflow(settings, null,
            Arrays.stream(cellToAdd).map(cell -> new DataCell[]{cell}).toArray(DataCell[][]::new));
    }

    static TestSetup setupAndExecuteWorkflow(final TopKSelectorNodeSettings settings, final String[] rowKeys,
        final DataCell[]... cellToAdd) throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new TopKSelectorNodeFactory());

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(SETTINGS_CLASS, settings, modelSettings);
        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // populate the input table
        var inputTableSpecBuilder = new TableTestUtil.SpecBuilder();
        if (cellToAdd != null && cellToAdd.length > 0) {
            for (int i = 0; i < cellToAdd[0].length; i++) {
                inputTableSpecBuilder = inputTableSpecBuilder.addColumn(INPUT_COLUMNS[i], cellToAdd[0][i].getType());
            }
        } else {
            inputTableSpecBuilder = inputTableSpecBuilder.addColumn(INPUT_COLUMNS[0], StringCellFactory.TYPE);
        }
        var inputTableSpec = inputTableSpecBuilder.build();
        var inputTableBuilder = new TableTestUtil.TableBuilder(inputTableSpec);
        if (cellToAdd != null) {
            for (int i = 0; i < cellToAdd.length; i++) {
                if (rowKeys != null && rowKeys.length > i) {
                    inputTableBuilder = inputTableBuilder.addRowWithId(rowKeys[i], cellToAdd[i]);
                } else {
                    inputTableBuilder = inputTableBuilder.addRow(cellToAdd[i]);
                }
            }
        }

        var inputTable = inputTableBuilder.build();
        var tableSupplierNode =
            WorkflowManagerUtil.createAndAddNode(workflowManager, new InputTableNode.InputDataNodeFactory(inputTable));

        // link the nodes
        workflowManager.addConnection(tableSupplierNode.getID(), 1, node.getID(), 1);

        // execute and wait...
        workflowManager.executeAllAndWaitUntilDone();

        var status = workflowManager.getNodeContainerState();

        var outputTable = (BufferedDataTable)node.getOutPort(1).getPortObject();

        if (outputTable == null || outputTable.size() == 0) {
            return new TestSetup(outputTable, null, status);
        }
        try (var it = outputTable.iterator()) {
            return new TestSetup( //
                outputTable, //
                it.next().getCell(0), //
                status //
            );
        }

    }
}
