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
 * History
 *   Mar 29, 2024 (wiswedel): created
 */
package org.knime.base.node.flowvariable.filter;

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
 * This factory create all necessary classes for the Variable Filter node.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
@SuppressWarnings("restriction")
public final class VariableFilterNodeFactory extends
        ConfigurableNodeFactory<VariableFilterNodeModel> implements NodeDialogFactory {

    private static final String FULL_DESCRIPTION = """
            <p>
                Filters flow variables so that they will not be available in downstream nodes. Flow variables
                in KNIME Analytics Platform are passed along all connections, independent of the connection type
                (data, data base connection, predictive model, etc). Once defined, variables will be available in all
                downstream nodes except in two cases:
            </p>
            <ul>
                <li>A variable is defined in a "scope context" such as within a loop or within component.</li>
                <li>A variable is removed using this node.</li>
            </ul>
            <p>
                Note that only variables can be filtered that are created within the same 'context'. For instance,
                if this node is used with a loop, it can only apply a filtering of variables created within the loop.
                Variables that were passed into the loop (available upstream the loop) cannot be filtered out.
            </p>
            <p>
                The input ports are just passed through to the output ports. You can set an arbitrary number of
                port pairs by using the &#8220;&#8230;&#8221; menu.
            </p>
            """;

    static final String PORT_GROUP = "Pass through";

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
            .name("Variable Filter") //
            .icon("variable_filter.png") //
            .shortDescription("Filters flow variables by name.") //
            .fullDescription(
                FULL_DESCRIPTION)
            .modelSettingsClass(VariableFilterSettings.class) //
            .addInputPort(PORT_GROUP, PortObject.TYPE,
                "The input data, which can be a data table or any other arbitrary port object.", true) //
            .addOutputPort(PORT_GROUP, PortObject.TYPE, "The unaltered input object", true) //
            .nodeType(NodeType.Other) //
            .sinceVersion(5, 3, 0) //
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
    protected VariableFilterNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portsConfig = creationConfig.getPortConfig().orElseThrow();
        return new VariableFilterNodeModel(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<VariableFilterNodeModel> createNodeView(final int viewIndex,
        final VariableFilterNodeModel nodeModel) {
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

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, VariableFilterSettings.class);
    }
}
