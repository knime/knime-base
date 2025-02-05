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
 *   Feb 5, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.rank.RankNodeSettings.RankDataType;
import org.knime.base.node.preproc.rank.RankNodeSettings.RankMode;
import org.knime.base.node.preproc.rank.RankNodeSettings.RankingCriterionSettings;
import org.knime.base.node.preproc.rank.RankNodeSettings.RowOrder;
import org.knime.base.node.util.InputTableNode;
import org.knime.base.node.util.preproc.SortingUtils.SortingOrder;
import org.knime.base.node.util.preproc.SortingUtils.StringComparison;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class RankNodeModelTest {

    private static final String NODE_NAME = "RankNode";

    private static final String[] INPUT_COLUMNS = new String[]{"column1", "column2"};

    private static final Class<? extends DefaultNodeSettings> SETTINGS_CLASS = RankNodeSettings.class;

    @Test
    void testStandardExecute() throws InvalidSettingsException, IOException {
        final var settings = new RankNodeSettings();
        settings.m_categoryColumns = new ColumnFilter(new String[]{INPUT_COLUMNS[0]});
        settings.m_sortingCriteria =
            new RankingCriterionSettings[]{new RankingCriterionSettings(new StringOrEnum<>(INPUT_COLUMNS[1]),
                SortingOrder.ASCENDING, StringComparison.LEXICOGRAPHIC)};
        settings.m_rankDataType = RankDataType.INTEGER;
        final var cells = new DataCell[][]{{StringCellFactory.create("A"), IntCellFactory.create(5)}, //
            {StringCellFactory.create("A"), IntCellFactory.create(6)}, //
            {StringCellFactory.create("B"), IntCellFactory.create(7)}, //
            {StringCellFactory.create("B"), IntCellFactory.create(8)}, //
            {StringCellFactory.create("C"), IntCellFactory.create(9)}, //
        };
        final var expectedResult = new int[]{1, 2, 1, 2, 1};

        var output = setupAndExecuteWorkflow(settings, cells);

        assertTrue(output.status().isExecuted(), "Should be successful");
        assertEquals(cells.length, output.outputTable().size(), "Should have the same number of rows");
        assertEquals(INPUT_COLUMNS.length + 1, output.outputTable().getDataTableSpec().getNumColumns(),
            "Should have an extra column");
        try (var iter = output.outputTable().iterator()) {
            for (int i = 0; iter.hasNext(); i++) {
                var row = iter.next();
                assertEquals(expectedResult[i], ((IntCell)row.getCell(2)).getIntValue(),
                    "Should have the correct ranking");
            }
        }
    }

    @Test
    void testDenseExecute() throws InvalidSettingsException, IOException {
        final var settings = new RankNodeSettings();
        settings.m_categoryColumns = new ColumnFilter(new String[]{INPUT_COLUMNS[0]});
        settings.m_sortingCriteria =
            new RankingCriterionSettings[]{new RankingCriterionSettings(new StringOrEnum<>(INPUT_COLUMNS[1]),
                SortingOrder.ASCENDING, StringComparison.LEXICOGRAPHIC)};
        settings.m_rankDataType = RankDataType.INTEGER;
        settings.m_rankMode = RankMode.DENSE;
        final var cells = new DataCell[][]{{StringCellFactory.create("A"), IntCellFactory.create(5)}, //
            {StringCellFactory.create("A"), IntCellFactory.create(6)}, //
            {StringCellFactory.create("B"), IntCellFactory.create(7)}, //
            {StringCellFactory.create("B"), IntCellFactory.create(8)}, //
            {StringCellFactory.create("C"), IntCellFactory.create(9)}, //
        };
        final var expectedResult = new int[]{1, 2, 1, 2, 1};

        var output = setupAndExecuteWorkflow(settings, cells);

        assertTrue(output.status().isExecuted(), "Should be successful");
        assertEquals(cells.length, output.outputTable().size(), "Should have the same number of rows");
        assertEquals(INPUT_COLUMNS.length + 1, output.outputTable().getDataTableSpec().getNumColumns(),
            "Should have an extra column");
        try (var iter = output.outputTable().iterator()) {
            for (int i = 0; iter.hasNext(); i++) {
                var row = iter.next();
                assertEquals(expectedResult[i], ((IntCell)row.getCell(2)).getIntValue(),
                    "Should have the correct ranking");
            }
        }
    }

    @Test
    void testOrdinalExecute() throws InvalidSettingsException, IOException {
        final var settings = new RankNodeSettings();
        settings.m_categoryColumns = new ColumnFilter(new String[]{INPUT_COLUMNS[0]});
        settings.m_sortingCriteria =
            new RankingCriterionSettings[]{new RankingCriterionSettings(new StringOrEnum<>(INPUT_COLUMNS[1]),
                SortingOrder.ASCENDING, StringComparison.LEXICOGRAPHIC)};
        settings.m_rankDataType = RankDataType.INTEGER;
        settings.m_rankMode = RankMode.ORDINAL;
        final var cells = new DataCell[][]{{StringCellFactory.create("A"), IntCellFactory.create(5)}, //
            {StringCellFactory.create("A"), IntCellFactory.create(6)}, //
            {StringCellFactory.create("B"), IntCellFactory.create(7)}, //
            {StringCellFactory.create("B"), IntCellFactory.create(8)}, //
            {StringCellFactory.create("C"), IntCellFactory.create(9)}, //
        };
        final var expectedResult = new int[]{1, 2, 1, 2, 1};

        var output = setupAndExecuteWorkflow(settings, cells);

        assertTrue(output.status().isExecuted(), "Should be successful");
        assertEquals(cells.length, output.outputTable().size(), "Should have the same number of rows");
        assertEquals(INPUT_COLUMNS.length + 1, output.outputTable().getDataTableSpec().getNumColumns(),
            "Should have an extra column");
        try (var iter = output.outputTable().iterator()) {
            for (int i = 0; iter.hasNext(); i++) {
                var row = iter.next();
                assertEquals(expectedResult[i], ((IntCell)row.getCell(2)).getIntValue(),
                    "Should have the correct ranking");
            }
        }
    }

    @Test
    void testStandardExecuteWithRetaining() throws InvalidSettingsException, IOException {
        final var settings = new RankNodeSettings();
        settings.m_categoryColumns = new ColumnFilter(new String[]{INPUT_COLUMNS[0]});
        settings.m_sortingCriteria =
            new RankingCriterionSettings[]{new RankingCriterionSettings(new StringOrEnum<>(INPUT_COLUMNS[1]),
                SortingOrder.DESCENDING, StringComparison.LEXICOGRAPHIC)};
        settings.m_rankDataType = RankDataType.INTEGER;
        settings.m_rowOrder = RowOrder.INPUT_ORDER;
        final var cells = new DataCell[][]{{StringCellFactory.create("A"), IntCellFactory.create(5)}, //
            {StringCellFactory.create("A"), IntCellFactory.create(6)}, //
            {StringCellFactory.create("B"), IntCellFactory.create(7)}, //
            {StringCellFactory.create("B"), IntCellFactory.create(8)}, //
            {StringCellFactory.create("C"), IntCellFactory.create(9)}, //
        };
        final var expectedResult = new int[]{2, 1, 2, 1, 1};

        var output = setupAndExecuteWorkflow(settings, cells);

        assertTrue(output.status().isExecuted(), "Should be successful");
        assertEquals(cells.length, output.outputTable().size(), "Should have the same number of rows");
        assertEquals(INPUT_COLUMNS.length + 1, output.outputTable().getDataTableSpec().getNumColumns(),
            "Should have an extra column");
        try (var iter = output.outputTable().iterator()) {
            for (int i = 0; iter.hasNext(); i++) {
                var row = iter.next();
                assertEquals(expectedResult[i], ((IntCell)row.getCell(2)).getIntValue(),
                    "Should have the correct ranking");
            }
        }
    }

    @Test
    void testNodeWorksWithRowKey() throws InvalidSettingsException, IOException {
        final var settings = new RankNodeSettings();
        settings.m_categoryColumns = new ColumnFilter(new String[]{INPUT_COLUMNS[0]});
        settings.m_sortingCriteria =
            new RankingCriterionSettings[]{new RankingCriterionSettings(new StringOrEnum<>(RowIDChoice.ROW_ID),
                SortingOrder.ASCENDING, StringComparison.LEXICOGRAPHIC)};
        settings.m_rankDataType = RankDataType.INTEGER;
        final var cells = new DataCell[][]{{StringCellFactory.create("A"), IntCellFactory.create(5)}, //
            {StringCellFactory.create("A"), IntCellFactory.create(6)}, //
            {StringCellFactory.create("B"), IntCellFactory.create(7)}, //
            {StringCellFactory.create("B"), IntCellFactory.create(8)}, //
            {StringCellFactory.create("C"), IntCellFactory.create(9)}, //
        };
        final var expectedResult = new int[]{1, 2, 1, 2, 1};

        var output = setupAndExecuteWorkflow(settings, cells);

        assertTrue(output.status().isExecuted(), "Should be successful");
        assertEquals(cells.length, output.outputTable().size(), "Should have the same number of rows");
        assertEquals(INPUT_COLUMNS.length + 1, output.outputTable().getDataTableSpec().getNumColumns(),
            "Should have an extra column");
        try (var iter = output.outputTable().iterator()) {
            for (int i = 0; iter.hasNext(); i++) {
                var row = iter.next();
                assertEquals(expectedResult[i], ((IntCell)row.getCell(2)).getIntValue(),
                    "Should have the correct ranking");
            }
        }
    }

    record TestSetup(BufferedDataTable outputTable, DataCell firstCell, NodeContainerState status) {
    }

    static TestSetup setupAndExecuteWorkflow(final RankNodeSettings settings, final DataCell[]... cellsToAdd)
        throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new RankNodeFactory());

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(SETTINGS_CLASS, settings, modelSettings);
        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // populate the input table
        var inputTableSpecBuilder = new TableTestUtil.SpecBuilder();
        if (cellsToAdd != null && cellsToAdd.length != 0) {
            for (int i = 0; i < cellsToAdd[0].length; i++) {
                inputTableSpecBuilder = inputTableSpecBuilder.addColumn(INPUT_COLUMNS[i], cellsToAdd[0][i].getType());
            }
        } else {
            for (int i = 0; i < INPUT_COLUMNS.length; i++) {
                inputTableSpecBuilder = inputTableSpecBuilder.addColumn(INPUT_COLUMNS[i], StringCellFactory.TYPE);
            }
        }
        var inputTableSpec = inputTableSpecBuilder.build();
        var inputTableBuilder = new TableTestUtil.TableBuilder(inputTableSpec);
        if (cellsToAdd != null && cellsToAdd.length != 0) {
            for (var cells : cellsToAdd) {
                inputTableBuilder = inputTableBuilder.addRow(cells);
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
