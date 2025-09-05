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
package org.knime.base.node.util.sampledata;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
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
 * Factory to instantiate new model.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Daniel Bogenrieder, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.0
 */
@SuppressWarnings("restriction")
public class SampleDataNodeFactory extends NodeFactory<SampleDataNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {
    /**
     * {@inheritDoc}
     *
     * @since 5.7
     */
    @Override
    public SampleDataNodeModel createNodeModel() {
        return new SampleDataNodeModel();
    }

    /**
     * This node has no view.
     *
     * @see NodeFactory#getNrNodeViews()
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * Throws exception as there is no view to create.
     *
     * @see NodeFactory#createNodeView(int, NodeModel)
     * @deprecated
     * @since 5.7
     */
    @Deprecated
    @Override
    public NodeView createNodeView(final int viewIndex, final SampleDataNodeModel nodeModel) {
        throw new IndexOutOfBoundsException("Invalid index: " + viewIndex);
    }

    /**
     * No Dialog available.
     *
     * @return <code>false</code>
     * @deprecated
     */
    @Deprecated
    @Override
    public boolean hasDialog() {
        return false;
    }

    private static final String NODE_NAME = "Data Generator";

    private static final String NODE_ICON = "./sampler.png";

    private static final String SHORT_DESCRIPTION = """
            Creates random data with clusters.
            """;

    private static final String FULL_DESCRIPTION = """
            Creates random data containing some clusters for Parallel Universes. The data contains a certain
                fraction of noise patterns and data that is generated to clusters (all clusters have the same size). The
                data is normalized in [0, 1].
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of();

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("Random data with cluster ID", """
            Contains the data with the cluster id as last column
            """), fixedPort("Cluster centers", """
            Contains the cluster centers. The attributes in the universes where the cluster is not located, are
            filled with missing values.
            """));

    @Deprecated
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.7
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, SampleDataNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
            SHORT_DESCRIPTION, FULL_DESCRIPTION, List.of(), SampleDataNodeParameters.class, null, NodeType.Source,
            List.of(), null);
    }

    /**
     * @since 5.7
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, SampleDataNodeParameters.class));
    }
}
