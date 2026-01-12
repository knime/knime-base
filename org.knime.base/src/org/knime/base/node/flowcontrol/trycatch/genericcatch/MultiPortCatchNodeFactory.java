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
package org.knime.base.node.flowcontrol.trycatch.genericcatch;

import static org.knime.node.impl.description.PortDescription.dynamicPort;
import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * Node factory for the Catch Errors (Var Ports) node.
 *
 * @author M. Berthold, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class MultiPortCatchNodeFactory extends ConfigurableNodeFactory<GenericCatchNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private static final String ORIGINAL_INPUT_PORT_GROUP = "Original input";
    private static final String DEFAULT_INPUT_PORT_GROUP = "Default input";
    private static final String OUTPUTS_PORT_GROUP = "Output";
    private static final String FAILURE_PORT_GROUP = "Failure";

    private static final String NODE_NAME = "Catch Errors (Multi-Port)";
    private static final String NODE_ICON = "catch.png";
    private static final String SHORT_DESCRIPTION = """
            End of Try-Catch construct. Use second input if execution leading to first input failed.
            """;
    private static final String FULL_DESCRIPTION = """
            This node forwards the input from the first port if the execution was successful. If execution on the
                top branch failed (and a matching try node was connected before the failing node!) then the input from
                the second port will be forwarded and the second variable outport will contain information about the
                observed error.
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            dynamicPort(ORIGINAL_INPUT_PORT_GROUP, ORIGINAL_INPUT_PORT_GROUP, """
                The original input
                """),
            dynamicPort(DEFAULT_INPUT_PORT_GROUP, DEFAULT_INPUT_PORT_GROUP, """
                The input to be used when execution on the main branch failed.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            dynamicPort(ORIGINAL_INPUT_PORT_GROUP, ORIGINAL_INPUT_PORT_GROUP, """
                Original inputs or default if execution failed.
                """),
            fixedPort(FAILURE_PORT_GROUP, """
                Reasons for Failure (if any).
                """)
    );

    public MultiPortCatchNodeFactory() {
    }

    @Override
    protected GenericCatchNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final var portsConfig = creationConfig.getPortConfig() //
            .orElseThrow(() -> new IllegalStateException("Port configuration required"));
        final var inputPortTypes = portsConfig.getInputPorts();
        return new GenericCatchNodeModel(Arrays.copyOf(inputPortTypes, inputPortTypes.length / 2));
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var b = new PortsConfigurationBuilder();
        final Predicate<PortType> allTypes = t -> true;
        b.addExtendablePortGroup(ORIGINAL_INPUT_PORT_GROUP, allTypes);
        b.addBoundExtendableInputPortGroup(DEFAULT_INPUT_PORT_GROUP, ORIGINAL_INPUT_PORT_GROUP);
        b.addFixedOutputPortGroup(FAILURE_PORT_GROUP, FlowVariablePortObject.TYPE);
        return Optional.of(b);
    }

    @Override
    public NodeView<GenericCatchNodeModel> createNodeView(final int index, final GenericCatchNodeModel model) {
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

    /**
     * @since 5.8
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, GenericCatchNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(
            NODE_NAME,
            NODE_ICON,
            INPUT_PORTS,
            OUTPUT_PORTS,
            SHORT_DESCRIPTION,
            FULL_DESCRIPTION,
            List.of(),
            GenericCatchNodeParameters.class,
            null,
            NodeType.ScopeEnd,
            List.of(),
            null
        );
    }

    /**
     * @since 5.8
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, GenericCatchNodeParameters.class));
    }
}
