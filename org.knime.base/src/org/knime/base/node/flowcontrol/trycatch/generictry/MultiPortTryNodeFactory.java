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
 *   9 Jan 2026 (leonard.woerteler): created
 */
package org.knime.base.node.flowcontrol.trycatch.generictry;

import java.util.List;
import java.util.Optional;

import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.util.Version;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 *
 * @author leonard.woerteler
 */
public class MultiPortTryNodeFactory extends ConfigurableNodeFactory<GenericTryNodeModel>
        implements NodeDialogFactory {

    static final String PORT_GROUP = "Pass through";

    /**
     * Create factory, that instantiates nodes.
     */
    public MultiPortTryNodeFactory() {
    }

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        b.addExtendablePortGroup(PORT_GROUP, t -> true);
        return Optional.of(b);
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    protected boolean hasDialog() {
        return false;
    }

    @Override
    public NodeView<GenericTryNodeModel> createNodeView(final int index, final GenericTryNodeModel model) {
        return null;
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(
            "Try (Multi-Port)",
            "try16.png",
            List.of(PortDescription.dynamicPort(PORT_GROUP, PORT_GROUP,
                "Input data, of arbitrary port types.")),
            List.of(PortDescription.dynamicPort(PORT_GROUP, PORT_GROUP,
                    "Output data, passed through from the inputs.")),
            "Start of Try-Catch construct.",
            "This node is the start of a try-catch enclosure to handle failing nodes.",
            List.of(),
            null,
            null,
            NodeType.ScopeStart,
            List.of(),
            new Version(5, 10, 0)
        );
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog();
    }

    @Override
    protected GenericTryNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new GenericTryNodeModel(creationConfig.getPortConfig().orElse(null));
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }
}
