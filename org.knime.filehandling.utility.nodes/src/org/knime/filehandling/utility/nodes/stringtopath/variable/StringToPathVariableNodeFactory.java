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
 *   Nov 30, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.stringtopath.variable;

import static org.knime.node.impl.description.PortDescription.dynamicPort;
import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * The node factory for the "String to Path (Variable)" node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class StringToPathVariableNodeFactory extends ConfigurableNodeFactory<StringToPathVariableNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder builder = new PortsConfigurationBuilder();
        builder.addOptionalInputPortGroup(StringToPathVariableNodeModel.CONNECTION_INPUT_PORT_GRP_NAME,
            FileSystemPortObject.TYPE);
        builder.addFixedInputPortGroup(StringToPathVariableNodeModel.VARIABLE_INPUT_PORT_GRP_NAME,
            FlowVariablePortObject.TYPE);
        builder.addFixedOutputPortGroup("Appended variables", FlowVariablePortObject.TYPE);
        return Optional.of(builder);
    }

    @Override
    protected StringToPathVariableNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new StringToPathVariableNodeModel(
            creationConfig.getPortConfig().orElseThrow(IllegalStateException::new));
    }


    @Override
    public NodeView<StringToPathVariableNodeModel> createNodeView(final int viewIndex,
        final StringToPathVariableNodeModel nodeModel) {
        return null;
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }
    private static final String NODE_NAME = "String to Path (Variable)";

    private static final String NODE_ICON = "../stringtopath.png";

    private static final String SHORT_DESCRIPTION = """
            Converts string variables into path variables.
            """;

    private static final String FULL_DESCRIPTION = """
            <p>This node converts the selected string variables into <a href="https://docs.knime.com/2021-06/
            analytics_platform_file_handling_guide/index.html#path"> path variables</a>.</p>
            <p> <i>This node can access a variety of different</i>
            <a href="https://docs.knime.com/2021-06/
            analytics_platform_file_handling_guide/index.html#analytics-platform-file-systems"><i>file systems.</i></a>
             <i>More information about file handling in KNIME can be found in the official</i>
             <a href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html"><i>File Handling
             Guide.</i></a> </p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            dynamicPort(StringToPathVariableNodeModel.CONNECTION_INPUT_PORT_GRP_NAME,
                StringToPathVariableNodeModel.CONNECTION_INPUT_PORT_GRP_NAME, """
                The file system connection.
                """),
            fixedPort(StringToPathVariableNodeModel.VARIABLE_INPUT_PORT_GRP_NAME, """
                Input variables.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Appended variables", """
                Input variables with additional converted Path variables.
                """)
    );

    @Override
    public NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, StringToPathVariableNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription( //
            NODE_NAME, //
            NODE_ICON, //
            INPUT_PORTS, //
            OUTPUT_PORTS, //
            SHORT_DESCRIPTION, //
            FULL_DESCRIPTION, //
            List.of(), //
            StringToPathVariableNodeParameters.class, //
            null, //
            NodeType.Manipulator, //
            List.of(), //
            null //
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, StringToPathVariableNodeParameters.class));
    }

}
