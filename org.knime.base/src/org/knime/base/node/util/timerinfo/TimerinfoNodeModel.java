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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.base.node.util.timerinfo.TimerinfoNodeSettings.RecursionPolicy;
import org.knime.core.data.DataCell;
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

    private DataTableSpec createSpec() {
        final var dtsc =
            new DataTableSpecCreator().addColumns(//
                new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Execution Time", LongCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Execution Time since last Reset", LongCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Execution Time since Start", LongCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Nr of Executions since last Reset", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Nr of Executions since Start", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("NodeID", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Classname", StringCell.TYPE).createSpec());
        if (getSettings().map(s -> s.m_includeNodeComments).orElse(false)) {
            dtsc.addColumns(new DataColumnSpecCreator("Node Comment", StringCell.TYPE).createSpec());
        }
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
        WorkflowManager myWorkflowManager = myNC.getParent();
        BufferedDataContainer result = exec.createDataContainer(createSpec());
        // traverse workflow
        var maxDepth = 0;
        if (settings.m_recursionPolicy != TimerinfoNodeSettings.RecursionPolicy.NO_RECURSION) {
            maxDepth = settings.m_maxDepth;
        }

        walkWorkflow(myWorkflowManager, new TimerNodeVisitor(myWorkflowManager.getID(), settings.m_recursionPolicy,
            maxDepth, settings.m_includeComponentIO, settings.m_includeNodeComments, result));

        result.close();
        return new PortObject[] { result.getTable() };
    }

    /**
     * Visitor interface for nodes in a workflow.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    private interface NodeVisitor {

        enum NodeVisitResult {
            /**
             * Indicates that visiting should continue (and recurse into metanodes or components).
             */
            CONTINUE,
            /**
             * Indicates that children should be skipped.
             */
            SKIP_CHILDREN;
        }

        NodeVisitResult visitMetanode(WorkflowManager wfm, NodeContainer nc, int level);

        NodeVisitResult visitComponent(WorkflowManager wfm, NodeContainer nc, int level);

        void visitNode(NodeContainer nc, int level);

    }

    /**
     * Visitor specific to the "Timer Info" node behavior that adds results into the given container. Metanodes are only
     * output if they are at the maximum level, otherwise, their children are visited. Components are always included in
     * the output, but their input/output nodes may be omitted based on the settings.
     */
    private static final class TimerNodeVisitor implements NodeVisitor {

        private final NodeID m_toplevelprefix;
        private final RecursionPolicy m_recursionPolicy;
        private final int m_maxLevel;
        private final boolean m_includeComponentIO;
        private final boolean m_includeNodeComments;

        private final BufferedDataContainer m_result;

        TimerNodeVisitor(final NodeID toplevelprefix, final TimerinfoNodeSettings.RecursionPolicy recursionPolicy,
            final int maxDepth, final boolean includeComponentIO, final boolean includeNodeComments,
            final BufferedDataContainer result) {
            m_toplevelprefix = toplevelprefix;
            m_recursionPolicy = recursionPolicy;
            m_maxLevel = maxDepth;
            m_includeComponentIO = includeComponentIO;
            m_includeNodeComments = includeNodeComments;
            m_result = result;
        }

        @Override
        public NodeVisitResult visitMetanode(final WorkflowManager wfm, final NodeContainer nc, final int level) {

            final var outputChildren = level < m_maxLevel && switch (m_recursionPolicy) {
                case ONLY_METANODES, COMPONENTS_AND_METANODES -> true;
                case NO_RECURSION -> false;
            };

            if (outputChildren) {
                return NodeVisitResult.CONTINUE;
            }
            // metanodes are only added if they are at the maximum depth (i.e. if their children are not listed)
            // or we don't recurse
            m_result.addRowToTable(createTimerInfoTableRow(nc));
            return NodeVisitResult.SKIP_CHILDREN;
        }

        @Override
        public NodeVisitResult visitComponent(final WorkflowManager wfm, final NodeContainer nc, final int level) {
            final var outputChildren =
                level < m_maxLevel && m_recursionPolicy == RecursionPolicy.COMPONENTS_AND_METANODES;
            m_result.addRowToTable(createTimerInfoTableRow(nc));
            return outputChildren ? NodeVisitResult.CONTINUE : NodeVisitResult.SKIP_CHILDREN;
        }

        @Override
        public void visitNode(final NodeContainer nc, final int level) {
            if (m_includeComponentIO || !NativeNodeContainer.IS_VIRTUAL_IN_OUT_NODE.test(nc)) {
                m_result.addRowToTable(createTimerInfoTableRow(nc));
            }
        }

        private DataRow createTimerInfoTableRow(final NodeContainer nc) {
            // For the RowID we only use the last part of the prefix - also to stay backwards compatible
            String rowid = "Node " + nc.getID().toString().substring(m_toplevelprefix.toString().length() + 1);
            var nt = nc.getNodeTimer();

            final var cells = new ArrayList<DataCell>();
            cells.add(new StringCell(nc.getName()));
            cells.add(nt.getLastExecutionDuration() >= 0 ? new LongCell(nt.getLastExecutionDuration())
                : DataType.getMissingCell());
            cells.add(new LongCell(nt.getExecutionDurationSinceReset()));
            cells.add(new LongCell(nt.getExecutionDurationSinceStart()));
            cells.add(new IntCell(nt.getNrExecsSinceReset()));
            cells.add(new IntCell(nt.getNrExecsSinceStart()));
            cells.add(new StringCell(nc.getID().toString()));
            cells.add(new StringCell(nc instanceof NativeNodeContainer nativeNodeContainer
                ? nativeNodeContainer.getNodeModel().getClass().getName() : "n/a"));
            if (m_includeNodeComments) {
                cells.add(new StringCell(nc.getNodeAnnotation().getText()));
            }

            return new DefaultRow(new RowKey(rowid), cells);
        }

    }

    private record Node(int level, NodeContainer nc) {
    }

    /**
     * Walks the workflow hierarchy starting at the given workflow manager.
     * @param startWorkflowManager workflow manager to start at
     * @param visitor visitor to call for each encountered node container
     */
    private static void walkWorkflow(final WorkflowManager startWorkflowManager, final NodeVisitor visitor) {
        // we use a queue, to output nodes in the order they are encountered
        final Deque<Node> stack = new ArrayDeque<>();
        for (final var ncStart : startWorkflowManager.getNodeContainers()) {
            stack.addFirst(new Node(0, ncStart));

            while (!stack.isEmpty()) {
                final var nc = stack.removeFirst();
                final NodeVisitor.NodeVisitResult visitResult;
                if (nc.nc instanceof WorkflowManager wfm) {
                    // Metanode
                    visitResult = visitor.visitMetanode(wfm, nc.nc, nc.level);
                } else if (nc.nc instanceof SubNodeContainer snc) {
                    // Component
                    final var wfm = snc.getWorkflowManager();
                    // Never recurse into locked components
                    visitResult = wfm.isUnlocked() ? visitor.visitComponent(wfm, nc.nc, nc.level)
                        : NodeVisitor.NodeVisitResult.SKIP_CHILDREN;
                } else {
                    // Node
                    visitor.visitNode(nc.nc, nc.level);
                    visitResult = null;
                }
                if (visitResult == null || visitResult == NodeVisitor.NodeVisitResult.SKIP_CHILDREN) {
                    continue;
                }
                // continue with children
                final int nextLevel = nc.level + 1;
                final var wfm =
                    nc.nc instanceof WorkflowManager wm ? wm : ((SubNodeContainer)nc.nc).getWorkflowManager();
                // adding children in reverse order to maintain legacy node behavior.
                final var iter = wfm.getNodeContainers().stream()
                        .collect(Collectors.toCollection(ArrayDeque::new)).descendingIterator();
                while (iter.hasNext()) {
                    stack.addFirst(new Node(nextLevel, iter.next()));
                }
            }

        }

    }
}
