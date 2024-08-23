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
 *   Sept 30, 2010 (mb): created
 */
package org.knime.base.node.util.timerinfo;

import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;


/**
 * @author Michael Berthold, University of Konstanz
 * @author Magnus Gohm, KNIME AG, Konstanz, Germany
 * @since 5.4
 */
@SuppressWarnings("restriction")
public final class TimerinfoNodeFactory extends WebUINodeFactory<TimerinfoNodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
            .name("Timer Info")//
            .icon("timerinfo.png")//
            .shortDescription("""
                    Collects all timer information from nodes in this workflow.
                    """)//
            .fullDescription("""
                    <p>This node reports individual and aggregate timing/execution
                    information for all nodes of the workflow at this level and for (nested)
                    metanodes and components using the specified recursion option up until the specified depth.</p>
                    <p>The output table lists all nodes in the workflow that were executed
                    since the last reset. This also includes nodes in metanodes and components
                    up to the specified nesting depth. The detailed statistics are:
                    <ul>
                        <li>Name: Name of the node</li>
                        <li>Execution Time: The execution time of the most recent execution.</li>
                        <li>Execution Time since last Reset: The aggregated execution time since
                            the node was last reset. For all nodes but loop start and end nodes this
                            value is the same as the "execution time". Start &amp; end nodes do not
                            get reset as part of a loop restart.</li>
                        <li>Execution Time since Start: The aggregated execution time since the
                            workflow has been opened (or the node was instantiated).</li>
                        <li>Nr of Executions since last Reset: Number of executions since it was
                            last reset. This is usually 1 (or 0 if node is not executed) but can be a
                            larger number for loop start and end nodes.</li>
                        <li>Nr of Executions since Start: Number of times a node was executed since
                            the workflow has been opened.</li>
                        <li>NodeID: The unique ID within the workflow associated with the node.</li>
                        <li>Classname: The java class name of the node -- used to uniquely identify
                            the node implementation.</li>
                    </ul>
                    </p>
                    """)//
            .modelSettingsClass(TimerinfoNodeSettings.class)//
            .nodeType(NodeType.Sink)//
            .addInputPort("Variable Input", FlowVariablePortObject.TYPE_OPTIONAL,
                "Allows to make sure node is executed after others.")//
            .addOutputTable("Output table", "The collected timer information.")//
            .build();

    /**
     * Instantiates the 'Timer Info' node.
     */
    public TimerinfoNodeFactory() {
        super(CONFIG);
    }

    @Override
    public TimerinfoNodeModel createNodeModel() {
        return new TimerinfoNodeModel(CONFIG);
    }

}
