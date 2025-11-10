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
 */
package org.knime.base.node.mine.cluster.kmeans;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.util.Version;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;
import org.knime.node.impl.description.ViewDescription;

/**
 * Create classes for k-means Clustering NodeModel, NodeView and NodeDialogPane.
 *
 * Successor of {@link ClusterNodeFactory2} since 5.9. The new node uses the modern column filter which allows for
 * pattern-based selection of columns and using "any unknown column" selection.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.0
 * @since 5.9
 */
@SuppressWarnings("restriction")
public class KMeansNodeFactory extends NodeFactory<KMeansNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private static final String NODE_NAME = "k-Means";

    private static final String NODE_ICON = "./kmeans.png";

    private static final String SHORT_DESCRIPTION = """
            Creates a crisp center based clustering.
            """;

    private static final String FULL_DESCRIPTION = """
            This node outputs the cluster centers for a predefined number of clusters (no dynamic number of
                clusters). K-means performs a crisp clustering that assigns a data vector to exactly one cluster. The
                algorithm terminates when the cluster assignments do not change anymore. The clustering algorithm uses
                the Euclidean distance on the selected attributes. The data is not normalized by the node (if required,
                you should consider to use the "Normalizer" as a preprocessing step).
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(PortDescription.fixedPort("Clustering input", """
            Input to clustering. All numerical values and only these are considered for clustering.
            """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(PortDescription.fixedPort("Labeled input", """
            The input data labeled with the cluster they are contained in.
            """), PortDescription.fixedPort("Clusters", """
            The created clusters.
            """), PortDescription.fixedPort("PMML Cluster Model", """
            PMML cluster model.
            """));

    private static final List<ViewDescription> VIEWS = List.of(new ViewDescription("Cluster View", """
            Displays the cluster prototypes in a tree-like structure, with each node containing the coordinates of
            the cluster center.
            """));

    private static final List<String> KEYWORDS = List.of("cluster", "unsupervised");

    @Override
    public final int getNrNodeViews() {
        return 1;
    }

    @Override
    public NodeView<KMeansNodeModel> createNodeView(final int viewIndex, final KMeansNodeModel nodeModel) {
        if (viewIndex != 0) {
            throw new IllegalStateException();
        }
        return new KMeansNodeView(nodeModel);
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
            List.of(), // no external resources
            KMeansNodeParameters.class, //
            VIEWS, //
            NodeType.Learner, //
            KEYWORDS, //
            new Version(5, 9, 0));
    }

    @Override
    public KMeansNodeModel createNodeModel() {
        return new KMeansNodeModel();
    }

    @Override
    public final boolean hasDialog() {
        return true;
    }

    @Override
    public final DefaultNodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, KMeansNodeParameters.class);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, KMeansNodeParameters.class));
    }

    @Override
    protected final NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

}
