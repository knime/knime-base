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

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;
import org.knime.node.impl.description.ViewDescription;

/**
 * Create classes for k-means Clustering NodeModel, NodeView and NodeDialogPane.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.0
 * @since 5.9
 */
@SuppressWarnings("restriction")
public class ClusterNodeFactory3 extends WebUINodeFactory<ClusterNodeModel2>
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
    private static final List<PortDescription> INPUT_PORTS = List.of(
            PortDescription.fixedPort("Clustering input", """
                Input to clustering. All numerical values and only these are considered for clustering.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
        PortDescription.fixedPort("Labeled input", """
                The input data labeled with the cluster they are contained in.
                """),
        PortDescription.fixedPort("Clusters", """
                The created clusters.
                """),
        PortDescription.fixedPort("PMML Cluster Model", """
                PMML cluster model.
                """)
    );
    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Cluster View", """
                Displays the cluster prototypes in a tree-like structure, with each node containing the coordinates of
                the cluster center.
                """)
    );

    @Override
    public final int getNrNodeViews() {
        return 1;
    }

    @Override
    public AbstractNodeView<ClusterNodeModel2> createAbstractNodeView(
        final int viewIndex, final ClusterNodeModel2 nodeModel) {
        if (viewIndex != 0) {
            throw new IllegalStateException();
        }
        return new ClusterNodeView2(nodeModel);
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
            ClusterNodeParameters.class,
            VIEWS,
            NodeType.Learner,
            List.of(),
            null
        );
    }

    /**
     * Creates a new factory for the k-Means node.
     */
    public ClusterNodeFactory3() {
        super(CONFIGURATION);
    }

    @Override
    public ClusterNodeModel2 createNodeModel() {
        return new ClusterNodeModel2(CONFIGURATION);
    }

    static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name(NODE_NAME) //
        .icon(NODE_ICON) //
        .shortDescription(SHORT_DESCRIPTION) //
        .fullDescription(FULL_DESCRIPTION) //
        .modelSettingsClass(ClusterNodeParameters.class) //
        .nodeType(NodeType.Learner) //
        .addInputTable("Clustering input", """
            Input to clustering. All numerical values and only these are considered for clustering.
            """) //
        .addOutputTable("Labeled input", """
            The input data labeled with the cluster they are contained in.
            """) //
        .addOutputTable("Clusters", """
            The created clusters
            """) //
        .addOutputPort("PMML Cluster Model", PMMLPortObject.TYPE, """
            PMML cluster model
            """)
        .build();

}
