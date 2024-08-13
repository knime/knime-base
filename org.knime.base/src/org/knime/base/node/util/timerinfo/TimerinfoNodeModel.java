/*
 * ------------------------------------------------------------------------
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
 *   Sept 17 2008 (mb): created (from wiswedel's TableToVariableNode)
 */
package org.knime.base.node.util.timerinfo;

import java.util.Map;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchConsumer;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * A simple node collecting timer information from the current workflow and
 * providing it as output table.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Magnus Gohm, KNIME AG, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
final class TimerinfoNodeModel extends WebUINodeModel<TimerinfoNodeSettings> implements InactiveBranchConsumer {

    /**
     * Creates a new timer info model
     *
     * @param config
     */
    public TimerinfoNodeModel(final WebUINodeConfiguration config) {
        super(config, TimerinfoNodeSettings.class);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final TimerinfoNodeSettings settings)
            throws InvalidSettingsException {
        return new PortObjectSpec[] { createSpec() };
    }

    private static DataTableSpec createSpec() {
        var dtsc = new DataTableSpecCreator();
        var colSpecs = new DataColumnSpec[] {
            new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Execution Time", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Execution Time since last Reset", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Execution Time since Start", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Nr of Executions since last Reset", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Nr of Executions since Start", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("NodeID", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Classname", StringCell.TYPE).createSpec()
        };
        dtsc.addColumns(colSpecs);
        return dtsc.createSpec();
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec,
        final TimerinfoNodeSettings settings) throws Exception {
        // we need to find the parent workflow manager of this node - which ain't easy...:
        var wfm = NodeContext.getContext().getWorkflowManager();
        TimerinfoNodeModel myThis = this;
        Map<NodeID, TimerinfoNodeModel> m =
            wfm.findNodes(TimerinfoNodeModel.class, new NodeModelFilter<TimerinfoNodeModel>() {
                @Override
                public boolean include(final TimerinfoNodeModel nodeModel) {
                    return nodeModel == myThis;
                }
            }, /*recurse metanodes*/true, /*recurse components*/true);
        // we should always find exactly one such node
        CheckUtils.checkState(m.size() == 1,
                "Expected to find 'this' node exactly once (result set has size %d)", m.size());
        NodeID myID = m.entrySet().iterator().next().getKey();
        var myNC = wfm.findNodeContainer(myID);
        WorkflowManager myWfm = myNC.getParent();
        BufferedDataContainer result = exec.createDataContainer(createSpec());
        reportThisLayer(myWfm, result, settings.m_maxDepth, myWfm.getID(),
            settings.m_componentResolution, settings.m_includeComponentIO);

        result.close();
        return new PortObject[] { result.getTable() };
    }

    /**
     * Internal method writing timer info into table for all nodes of a given WFM until
     * a certain depth in the provided BDT. Metanodes are treated normally (to keep this node backwards compatible).
     * Components are added depended on the settings. Either only components (depth = 0), only their nested leaf nodes
     * (depth > 0) or component and their nested leaf nodes (depth > 0).
     *
     * @param wfm the {@link WorkflowManager} of this layer
     * @param result the output table
     * @param depth the timer info depth
     * @param toplevelprefix the prefix of the parent {@link WorkflowManager}
     * @param componentResolution the {@link ComponentResolutionPolicy} chosen in the configuration
     * @param includeComponentIO whether to include the component input and output nodes
     */
    private static void reportThisLayer(final WorkflowManager wfm, final BufferedDataContainer result,
        final int depth, final NodeID toplevelprefix,
        final ComponentResolutionPolicy componentResolution, final boolean includeComponentIO) {
        for (NodeContainer nc : wfm.getNodeContainers()) {
            if (depth > 0 && nc instanceof WorkflowManager workflowManager) {
                // Metanode
                reportThisLayer(workflowManager, result, depth-1, toplevelprefix,
                    componentResolution, includeComponentIO);
            } else if (depth > 0 && nc instanceof SubNodeContainer) {
                // Component
                applyComponentResolutionPolicyOnThisLayer(((SubNodeContainer)nc).getWorkflowManager(), nc,
                    componentResolution, includeComponentIO, result, depth, toplevelprefix);
            } else {
                // Node
                if (includeComponentIO || !NativeNodeContainer.IS_VIRTUAL_IN_OUT_NODE.test(nc)) {
                    result.addRowToTable(createTimerInfoTableRow(nc, toplevelprefix));
                }
            }
        }
    }

    private static void applyComponentResolutionPolicyOnThisLayer(final WorkflowManager nestedWorkflowManager,
        final NodeContainer nc, final ComponentResolutionPolicy componentResolution, final boolean includeComponentIO,
        final BufferedDataContainer result, final int depth, final NodeID toplevelprefix) {
        // Skip this layer if we want only leaves
        if (componentResolution != ComponentResolutionPolicy.NODES) {
            result.addRowToTable(createTimerInfoTableRow(nc, toplevelprefix));
        }
        // Skip going into components if not configured and respect component lock
        if (componentResolution != ComponentResolutionPolicy.COMPONENTS && nestedWorkflowManager.isUnlocked()) {
            reportThisLayer(nestedWorkflowManager, result, depth-1, toplevelprefix,
                componentResolution, includeComponentIO);
        }
    }

    private static DataRow createTimerInfoTableRow(final NodeContainer nc, final NodeID toplevelprefix) {
        // For the RowID we only use the last part of the prefix - also to stay backwards compatible
        String rowid = "Node " + nc.getID().toString().substring(toplevelprefix.toString().length() + 1);
        var nt = nc.getNodeTimer();
        return new DefaultRow(
            new RowKey(rowid),
            new StringCell(nc.getName()),
            nt.getLastExecutionDuration() >= 0
                ? new LongCell(nt.getLastExecutionDuration()) : DataType.getMissingCell(),
            new LongCell(nt.getExecutionDurationSinceReset()),
            new LongCell(nt.getExecutionDurationSinceStart()),
            new IntCell(nt.getNrExecsSinceReset()),
            new IntCell(nt.getNrExecsSinceStart()),
            new StringCell(nc.getID().toString()),
            new StringCell(nc instanceof NativeNodeContainer nativeNodeContainer
                ? nativeNodeContainer.getNodeModel().getClass().getName() : "n/a")
        );
    }

}
