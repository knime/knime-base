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
 *   13.02.2008 (thor): created
 */
package org.knime.base.node.meta.looper;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.xmlbeans.XmlException;
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
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;
import org.xml.sax.SAXException;

/**
 * This factory create all necessary classes for the for-loop head node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 4.5
 */
public final class LoopEndDynamicNodeFactory extends ConfigurableNodeFactory<LoopEndDynamicNodeModel>
    implements NodeDialogFactory {

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        b.addExtendablePortGroup("Collector", new PortType[]{BufferedDataTable.TYPE}, BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected LoopEndDynamicNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var config = creationConfig.getPortConfig().orElseThrow();
        return new LoopEndDynamicNodeModel(config.getInputPorts(), config.getOutputPorts());
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.8
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, LoopEndDynamicNodeWebUISettings.class);
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<LoopEndDynamicNodeModel> createNodeView(final int viewIndex,
        final LoopEndDynamicNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    /**
     * Use the WebUINodeConfiguration to generate the node description (replacing the XML file).
     */
    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        Collection<PortDescription> inPortDescriptions = List.of(//
            new PortDescription("inId", "Input table", "Any data table", false), //
            new PortDescription("Collector", "Input table", "Any data table", true));
        Collection<PortDescription> outPortDescriptions = List.of(//
            new PortDescription("outId", "Collected results", "Collected results from the loop body", false), //
            new PortDescription("Collector", "Collected results", "Collected results from the loop body", true));

        return DefaultNodeDescriptionUtil.createNodeDescription("Loop End", //
            "loop_end.png", //
            inPortDescriptions, //
            outPortDescriptions, //
            "Node at the end of a loop", //
            """
                    <p>
                    Node at the end of a loop. It is used to mark the end of a workflow loop and collects the
                    intermediate results by row-wise concatenation of the incoming tables. The start of the loop
                    is defined by the loop start node, in which you can define how often the loop should be executed
                    (either fixed or derived from data, e.g. the "group loop start").
                    All nodes in between are executed that many times.
                    </p>
                    <p>
                        You can add more input and ouput tables using the &#8220;&#8230;&#8221; menu.
                    </p>
                    """, //
            List.of(), // resources
            LoopEndDynamicNodeWebUISettings.class, //
            List.of(), // view descriptions
            NodeType.LoopEnd, //
            List.of(), // keywords
            null);
    }
}
