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
 *   Apr 15, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.partition.PartitionNodeFactory;
import org.knime.base.node.preproc.sample.AbstractSamplingNodeSettings.ActionOnEmptyInput;
import org.knime.base.node.preproc.sample.AbstractSamplingNodeSettings.CountMode;
import org.knime.base.node.preproc.sample.AbstractSamplingNodeSettings.SamplingMode;
import org.knime.base.node.util.InputTableNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
final class RowSamplingNodeModelTest {

    private static final String NODE_NAME = "PartitionNode";

    private static final String[] INPUT_COLUMNS = new String[]{"column1"};

    private static final Class<? extends DefaultNodeSettings> SETTINGS_CLASS = RowSamplingNodeSettings.class;

    @Test
    void testDefaultExecute() throws InvalidSettingsException, IOException {
        final var settings = new RowSamplingNodeSettings();

        var output = setupAndExecuteWorkflow(settings, new StringCell("Test1"), new StringCell("Test2"),
            new StringCell("Test1"), new StringCell("Test10"));

        assertTrue(output.status().isExecuted(), "Should run successfully");
        assertNotNull(output.outputTable(), "Output table should not be null");
        assertEquals(2, output.outputTable().size(), "Table 1 should contain 2 rows");
    }

    @Test
    void testMigrationAndExecute() throws InvalidSettingsException, IOException {
        final var settings = new NodeSettings("model");
        settings.addString("method", "Absolute");
        settings.addString("samplingMethod", "First");
        settings.addDouble("fraction", 0.0);
        settings.addString("random_seed", null);
        settings.addString("class_column", null);
        settings.addInt("count", 1);

        final var nodeSettings = DefaultNodeSettings.loadSettings(settings, RowSamplingNodeSettings.class);

        var output = setupAndExecuteWorkflow(nodeSettings, new StringCell("Test1"), new StringCell("Test2"),
            new StringCell("Test1"), new StringCell("Test10"));

        assertTrue(output.status.isExecuted(), "Should execute");
        assertTrue(output.status().isExecuted(), "Should run successfully");
        assertNotNull(output.outputTable(), "Output table should not be null");
        assertEquals(1, output.outputTable().size(), "Table should contain 1 rows");
        try (final var it = output.outputTable().iterator()) {
            assertEquals("Test1", it.next().getCell(0).toString(), "Table should have the correct cell");
        }

    }

    @Test
    void testAbsoluteCount() throws InvalidSettingsException, IOException {
        final var settings = new RowSamplingNodeSettings();
        settings.m_partitioningMode = CountMode.ABSOLUTE;
        settings.m_rowCount = 2;

        var output = setupAndExecuteWorkflow(settings, new StringCell("Test1"), new StringCell("Test2"),
            new StringCell("Test1"), new StringCell("Test10"));

        assertTrue(output.status().isExecuted(), "Should run successfully");
        assertEquals(2, output.outputTable().size(), "Table should contain 2 rows");
    }

    @Test
    void testFixedSeedRandom() throws InvalidSettingsException, IOException {
        final var settings = new RowSamplingNodeSettings();
        settings.m_mode = SamplingMode.RANDOM;
        settings.m_seed = Optional.of(10L);

        var output1 = setupAndExecuteWorkflow(settings, new StringCell("Test1"), new StringCell("Test2"),
            new StringCell("Test1"), new StringCell("Test10"));
        var output2 = setupAndExecuteWorkflow(settings, new StringCell("Test1"), new StringCell("Test2"),
            new StringCell("Test1"), new StringCell("Test10"));

        assertTrue(output1.status().isExecuted(), "Should run successfully");
        assertTrue(output2.status().isExecuted(), "Should run successfully");
        assertEquals(output1.outputTable().size(), output2.outputTable().size(),
            "Should have the same number of items");
        try (final var it1 = output1.outputTable().iterator(); final var it2 = output2.outputTable().iterator()) {
            while (it1.hasNext() && it2.hasNext()) {
                assertEquals(it1.next().getCell(0).toString(), it2.next().getCell(0).toString(),
                    "Tables should have the same cell");
            }
        }
    }

    @Test
    void testFailOnEmptyInput() throws InvalidSettingsException, IOException {
        final var settings = new RowSamplingNodeSettings();
        var output = setupAndExecuteWorkflow(settings);

        assertFalse(output.status().isExecuted(), "Should not run successfully");
    }

    @Test
    void testRunOnEmptyInput() throws InvalidSettingsException, IOException {
        final var settings = new RowSamplingNodeSettings();
        settings.m_actionEmpty = ActionOnEmptyInput.OUTPUT_EMPTY;
        var output = setupAndExecuteWorkflow(settings);

        assertTrue(output.status().isExecuted(), "Should run successfully");
        assertEquals(0, output.outputTable.size(), "Output should be empty");
    }

    record TestSetup(BufferedDataTable outputTable, NodeContainerState status) {
    }

    static TestSetup setupAndExecuteWorkflow(final RowSamplingNodeSettings settings, final DataCell... cellToAdd)
        throws InvalidSettingsException, IOException {

        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new PartitionNodeFactory());

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(SETTINGS_CLASS, settings, modelSettings);
        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // populate the input table
        var inputTableSpecBuilder = new TableTestUtil.SpecBuilder();
        inputTableSpecBuilder = inputTableSpecBuilder.addColumn(INPUT_COLUMNS[0], StringCellFactory.TYPE);
        var inputTableSpec = inputTableSpecBuilder.build();
        var inputTableBuilder = new TableTestUtil.TableBuilder(inputTableSpec);
        if (cellToAdd != null) {
            for (int i = 0; i < cellToAdd.length; i++) {
                inputTableBuilder = inputTableBuilder.addRow(cellToAdd[i]);
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

        return new TestSetup(outputTable, status);

    }
}
