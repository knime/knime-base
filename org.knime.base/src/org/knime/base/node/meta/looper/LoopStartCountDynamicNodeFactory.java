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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.meta.looper;

import java.io.IOException;
import java.util.Optional;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.xml.sax.SAXException;

/**
 * This factory create all necessary classes for the for-loop head node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 4.5
 */
@SuppressWarnings("restriction")
public final class LoopStartCountDynamicNodeFactory extends
        ConfigurableNodeFactory<LoopStartCountDynamicNodeModel> implements NodeDialogFactory {

    static final String PORT_GROUP = "Pass through";

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
            .name("Counting Loop Start") //
            .icon("loop_start_count.png") //
            .shortDescription("Node at the start of a loop") //
            .fullDescription(
                """
                The Counting Loop Start is the node that starts a loop which is executed a predefined number
                of times. At the end of the loop you need LoopEnd, which collects the results from
                all loop iterations. All nodes in between are executed as many times as you specify in the dialog
                of Counting Loop Start.
                <p>
                    The input ports are just passed through to the output ports. You can add an arbitrary number of
                    port pairs by using the &#8220;&#8230;&#8221; menu.
               </p>
                """)
            .modelSettingsClass(LoopStartCountDynamicSettings.class) //
            .addExternalResource("https://docs.knime.com/latest/analytics_platform_flow_control_guide/index.html#loops",
                "KNIME Flow Control Guide: Section Loops") //
            .addInputPort(PORT_GROUP, PortObject.TYPE,
                "The input data, which can be a data table or any other arbitrary port object.", true) //
            .addOutputPort(PORT_GROUP, PortObject.TYPE, "The unaltered input object", true) //
            .nodeType(NodeType.LoopStart) //
            .sinceVersion(4, 2, 0) //
            .build();

    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription(CONFIG);
    }

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        b.addExtendablePortGroup(PORT_GROUP, t -> true);
        return Optional.of(b);
    }

    @Override
    protected LoopStartCountDynamicNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portsConfig = creationConfig.getPortConfig().orElseThrow();
        return new LoopStartCountDynamicNodeModel(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<LoopStartCountDynamicNodeModel> createNodeView(final int viewIndex,
        final LoopStartCountDynamicNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.3
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, LoopStartCountDynamicSettings.class);
    }
}
