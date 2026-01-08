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
 *   07.04.2008 (Kilian Thiel): created
 */
package org.knime.base.node.mine.mds.mdsprojection;

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
 * The node factory of the mds projection node.
 *
 * @author Kilian Thiel, University of Konstanz
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class MDSProjectionNodeFactory extends NodeFactory<MDSProjectionNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    /**
     * {@inheritDoc}
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public MDSProjectionNodeModel createNodeModel() {
        return new MDSProjectionNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<MDSProjectionNodeModel> createNodeView(final int index, final MDSProjectionNodeModel model) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "MDS Projection";

    private static final String NODE_ICON = "./mds.png";

    private static final String SHORT_DESCRIPTION = """
            Multi dimensional scaling node, mapping data of a high dimensional space onto a lower dimensional space
                by applying a modified Sammons mapping with respect to a given set of fixed points.
            """;

    private static final String FULL_DESCRIPTION = """
            This node maps data of a high dimensional space onto a lower (usually 2 or 3) dimensional space with
                respect to a set of fixed data points. Therefore modified Sammons mapping is applied, which iteratively
                decreases the difference of the distances of high and low dimensional data. When adjusting the position
                a low dimensional data point by default not its neighbors (or all other data points) are taken into
                account but a specified set of fixed data points which are not modified. Additionally the data points
                (and not only the fixed points) can be taken into account when adjusting its positions, therefore the
                setting "Project only" has to be unchecked. If the setting is checked the data points will be mapped
                only with respect to the fixed data, which we call a projection. The algorithm converges like a common
                mds algorithm due to a decreasing learning rate.
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(fixedPort("Any input table", """
            Data table containing the fixed data points.
            """), fixedPort("Any input table", """
            Data table containing the data to map.
            """));

    private static final List<PortDescription> OUTPUT_PORTS =
        List.of(fixedPort("The input data and the mapped data", """
                The input data and the mapped data.
                """));

    /**
     * @since 5.10
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.10
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, MDSProjectionNodeParameters.class);
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
            MDSProjectionNodeParameters.class, //
            null, //
            NodeType.Learner, //
            List.of(), //
            null //
        );
    }

    /**
     * @since 5.10
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, MDSProjectionNodeParameters.class));
    }
}
