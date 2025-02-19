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
 *   Jan 16, 2025 (Martin Sillye): created
 */
package org.knime;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.testing.util.InputTableNode;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final public class NodeModelTestRunnerUtil {

    private final String m_inputColumn;

    private final String m_nodeName;

    private final Class<? extends NodeFactory<? extends NodeModel>> m_factory;

    private final Class<? extends DefaultNodeSettings> m_settingsClass;

    /**
     * Create a Util class to run tests on Node Model
     *
     * @param inputColumn name of the input column
     * @param nodeName name of the node
     * @param settingsClass type of the settings class
     */
    public NodeModelTestRunnerUtil(final String inputColumn, final String nodeName,
        final Class<? extends DefaultNodeSettings> settingsClass,
        final Class<? extends NodeFactory<? extends NodeModel>> factory) {
        this.m_inputColumn = inputColumn;
        this.m_nodeName = nodeName;
        this.m_settingsClass = settingsClass;
        this.m_factory = factory;
    }

    /**
     * Output data of the test run
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     * @param outputTable Output data
     * @param firstCell First cell of output
     * @param lastCell Last cell of output
     * @param nodeState The status of the test node
     */
    public record TestOutput(BufferedDataTable outputTable, DataCell firstCell, DataCell lastCell,
        NodeContainerState nodeState) {
    }

    /**
     * Run test workflow
     *
     * @param settings Node settings to use
     * @param cellToAdd Cell to add as input
     * @param <T> Type of the node settings
     * @return Output data of the test
     * @throws InvalidSettingsException
     * @throws IOException
     */
    public <T extends DefaultNodeSettings> TestOutput setupAndExecuteWorkflow(final T settings,
        final DataCell cellToAdd) throws InvalidSettingsException, IOException {
        return setupAndExecuteWorkflow(settings, cellToAdd, StringCellFactory.TYPE);
    }

    /**
     * Run test workflow
     *
     * @param settings Node settings to use
     * @param cellToAdd Cell to add as input
     * @param <T> Type of the node settings
     * @param inputColumnType Column type to use for empty tables
     * @return Output data of the test
     * @throws InvalidSettingsException
     * @throws IOException
     */
    public <T extends DefaultNodeSettings> TestOutput setupAndExecuteWorkflow(final T settings,
        final DataCell cellToAdd, final DataType inputColumnType) throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        NodeFactory<? extends NodeModel> factory;
        try {
            factory = m_factory.getConstructor().newInstance();
        } catch (IllegalArgumentException | SecurityException | ReflectiveOperationException e) {
            fail(e.getMessage());
            return new TestOutput(null, null, null, workflowManager.getNodeContainerState());
        }
        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, factory);

        // set the settings
        final var nodeSettings = new NodeSettings(m_nodeName);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(m_settingsClass, settings, modelSettings);
        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // populate the input table
        var inputTableSpec = new TableTestUtil.SpecBuilder().addColumn(m_inputColumn, cellToAdd != null //
            ? cellToAdd.getType() //
            : inputColumnType //
        ).build();
        var tableBuilder = new TableTestUtil.TableBuilder(inputTableSpec);
        if (cellToAdd != null) {
            tableBuilder.addRow(cellToAdd);
        }
        var inputTable = tableBuilder.build();
        var tableSupplierNode =
            WorkflowManagerUtil.createAndAddNode(workflowManager, new InputTableNode.InputDataNodeFactory(inputTable));

        // link the nodes
        workflowManager.addConnection(tableSupplierNode.getID(), 1, node.getID(), 1);

        // execute and wait...
        var executed = workflowManager.executeAllAndWaitUntilDone();

        var outputTable = (BufferedDataTable)node.getOutPort(1).getPortObject();

        if (outputTable == null || outputTable.size() == 0) {
            return new TestOutput(outputTable, null, null, workflowManager.getNodeContainerState());
        }
        var firstCell = DateTimeTestingUtil.getFirstRow(outputTable).getCell(0);
        var lastCell = DateTimeTestingUtil.getLastRow(outputTable).getCell(0);
        return new TestOutput(outputTable, firstCell, lastCell, workflowManager.getNodeContainerState());
    }

}
