package org.knime.base.node.meta.looper.recursive;
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
 * ------------------------------------------------------------------------
 *
 */

import static org.knime.node.impl.description.PortDescription.dynamicPort;
import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.ExternalResource;
import org.knime.node.impl.description.PortDescription;

/**
 * <code>NodeFactory</code> for the Recursive Loop End Node (arbitrary ports).
 *
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings("restriction")
public class RecursiveLoopEndDynamicNodeFactory extends ConfigurableNodeFactory<RecursiveLoopEndDynamicNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        b.addExtendablePortGroup("Collector", new PortType[]{BufferedDataTable.TYPE}, BufferedDataTable.TYPE);
        b.addExtendableInputPortGroup("Recursion", new PortType[]{BufferedDataTable.TYPE}, BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected RecursiveLoopEndDynamicNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var config = creationConfig.getPortConfig().orElseThrow();
        return new RecursiveLoopEndDynamicNodeModel(config.getInputPorts(), config.getOutputPorts());
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<RecursiveLoopEndDynamicNodeModel> createNodeView(final int viewIndex,
        final RecursiveLoopEndDynamicNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Recursive Loop End";

    private static final String NODE_ICON = "./delegateloopend.png";

    // TODO check
    private static final String SHORT_DESCRIPTION = """
            The loop end node for a recursive loop. This type of loop passes data from the Recursive Loop End node
                to the Recursive Loop Start node.
            """;

    // TODO check
    private static final String FULL_DESCRIPTION = """
            The Recursive Loop node pair enables the passing of data tables from the Recursive Loop End back to the
                Recursive Loop Start. The Recursive Loop Start requires initialized tables. These tables are output by
                the Recursive Loop Start in the first iteration of the loop. The tables received by the corresponding
                Recursive Loop End are passed back to the Recursive Loop Start node. Starting with the second iteration,
                the Recursive Loop Start node outputs the data as received by the Recursive Loop End. You can add more
                recursion input tables and collector input and output tables using the “…” menu. The number of recursion
                ports must be the same as the number of recursion ports of the corresponding Recursive Loop Start node.
                Recursion and collector ports are independent of each other. The collection ports are always listed
                first. The loop runs until one of the three stopping criteria is met: Maximum number of iterations: to
                ensure no endless loop is created, the loop will end after the set number of iterations. Minimal number
                of rows: to ensure enough rows are present for processing, the loop stops if one of it's recursion input
                tables contains less rows than the set minimum. This minimum can be set for each recursion input table
                individually. End loop with variable: the loop ends if the option is enabled and the value of the
                selected variable equals “true”. The data passed to the collector ports is collected and passed to the n
                respective output ports. The data at the recursion ports (all ports after input port n) is returned to
                the Recursive Loop Start node.
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(fixedPort("Collecting data", """
            Data to be collected for the output.
            """), dynamicPort("Collector", "Collecting data", """
            Data to be collected for the output.
            """), fixedPort("Recursion data", """
            Data to be passed back to loop start.
            """), dynamicPort("Recursion", "Recursion data", """
            Data to be passed back to loop start.
            """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("Collected data", """
            The rows collected from the corresponding collector port.
            """), dynamicPort("Collector", "Collected data", """
            The rows collected from the corresponding collector port.
            """));

    private static final List<ExternalResource> LINKS = List.of(
        new ExternalResource("https://docs.knime.com/latest/analytics_platform_flow_control_guide/index.html#loops", """
                KNIME Flow Control Guide: Section Loops
                """));

    @Override
    public NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, RecursiveLoopEndDynamicNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
            SHORT_DESCRIPTION, FULL_DESCRIPTION, LINKS, RecursiveLoopEndDynamicNodeParameters.class, null,
            NodeType.LoopEnd, List.of(), null);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, RecursiveLoopEndDynamicNodeParameters.class));
    }
}
