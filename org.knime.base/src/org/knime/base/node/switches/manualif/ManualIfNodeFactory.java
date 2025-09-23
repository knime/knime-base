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
package org.knime.base.node.switches.manualif;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
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
 * @author M. Berthold, University of Konstanz
 * @author Kai Franze, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings("restriction")
public final class ManualIfNodeFactory
    extends NodeFactory<ManualIfNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    /**
     * Create factory, that instantiates nodes.
     */
    public ManualIfNodeFactory() {
    }

    @Override
    public ManualIfNodeModel createNodeModel() {
        return new ManualIfNodeModel();
    }

    @Override
    public NodeView<ManualIfNodeModel> createNodeView(final int index, final ManualIfNodeModel model) {
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

    private static final String NODE_NAME = "IF Switch";

    private static final String NODE_ICON = "switches_if.png";

    private static final String SHORT_DESCRIPTION = """
            Allows to manually control which branch the data will flow into.
            """;

    private static final String FULL_DESCRIPTION = """
            This node passes the data either to the top, bottom, or both output ports, depending on the
                configuration.
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(fixedPort("Input table", """
            The input table.
            """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("First output table", """
            The first choice.
            """), fixedPort("Second output table", """
            The second choice.
            """));

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, ManualIfNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(//
            NODE_NAME, //
            NODE_ICON, //
            INPUT_PORTS, //
            OUTPUT_PORTS, //
            SHORT_DESCRIPTION, //
            FULL_DESCRIPTION, //
            List.of(), //
            ManualIfNodeParameters.class, //
            null, //
            NodeType.Manipulator, //
            List.of(), //
            null);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, ManualIfNodeParameters.class));
    }

}
