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
package org.knime.base.node.mine.cluster.hierarchical;


import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotter;
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
import org.knime.node.impl.description.ViewDescription;

/**
 * The Factory for the hierarchical clustering node.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class HierarchicalClusterNodeFactory extends NodeFactory<HierarchicalClusterNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public HierarchicalClusterNodeModel createNodeModel() {
        return new HierarchicalClusterNodeModel(); // new ManhattanDist());
    }

    @Override
    public int getNrNodeViews() {
        return 1;
    }

    @Override
    public NodeView<HierarchicalClusterNodeModel> createNodeView(final int i,
        final HierarchicalClusterNodeModel nodeModel) {
        if (i != 0) {
            throw new IllegalArgumentException();
        }
        HierarchicalClusterNodeView view = new HierarchicalClusterNodeView(
                nodeModel, new DendrogramPlotter());
        return view;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Hierarchical Clustering";

    private static final String NODE_ICON = "./dendrogram.png";

    private static final String SHORT_DESCRIPTION = """
            Performs Hierarchical Clustering.
            """;

    private static final String FULL_DESCRIPTION = """
            <p> Hierarchically clusters the input data. <br /> Note: This node works only on small data sets. It
                keeps the entire data in memory and has cubic complexity.<br /> There are two methods to do hierarchical
                clustering: <ul> <li> Top-down or divisive, i.e. the algorithm starts with all data points in one huge
                cluster and the most dissimilar datapoints are divided into subclusters until each cluster consists of
                exactly one data point. </li> <li> Bottom-up or agglomerative, i.e. the algorithm starts with every
                datapoint as one single cluster and tries to combine the most similar ones into superclusters until it
                ends up in one huge cluster containing all subclusters. </li> </ul> This algorithm works agglomerative.
                </p> <p>In order to determine the distance between clusters a measure has to be defined. Basically,
                there exist three methods to compare two clusters: <ul> <li>Single Linkage: defines the distance between
                two clusters c1 and c2 as the minimal distance between any two points x, y with x in c1 and y in
                c2.</li> <li>Complete Linkage: defines the distance between two clusters c1 and c2 as the maximal
                distance between any two points x, y with x in c1 and y in c2.</li> <li>Average Linkage: defines the
                distance between two clusters c1 and c2 as the mean distance between all points in c1 and c2.</li> </ul>
                </p> <p> In order to measure the distance between two points a distance measure is necessary. You can
                choose between the Manhattan distance and the Euclidean distance, which corresponds to the L1 and the L2
                norm. </p> <p> The output is the same data as the input with one additional column with the clustername
                the data point is assigned to. Since a hierarchical clustering algorithm produces a series of cluster
                results, the number of clusters for the output has to be defined in the dialog. </p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Data to cluster", """
                The data that should be clustered using hierarchical clustering. Only numeric columns are considered,
                nominal columns are ignored.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Clustered data", """
                The input data with an extra column with the cluster name where the data point is assigned to.
                """)
    );

    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Dendrogram/Distance View", """
                <ul> <li>Dendrogram: The view shows a dendrogram which displays the whole cluster hierarchy. At the
                bottom are all datapoints. The closest data points are connected, where the height of the connection
                shows the distance between them. Thus, the y coordinate displays the distance of the fusions and thereby
                also the hierarchy level. The x axis is nominal and displays the single data points with their RowID.
                Each cluster can be selected and hilited. All contained subclusters will be hilited, too. </li>
                <li>Distance plot: The distance plot displays the distances between the cluster for each number of
                clusters. This view can help to determine a "good" number of clusters, since there will be sudden jumps
                in the level of similarity as dissimilar groups are fused. The y coordinate is the distance of the
                fusion, the x axis the number of the fusion, i.e. the hierarchy level. The tooltip over the datapoints
                provides detailed information about that point, where "x" is the hierarchy level and "y" the distance of
                that fusion. The points can not be hilited, since the distances correspond to the height of the
                dendrogram not to any data points. The appearance tab let you adjust the view by hiding or displaying
                the dots, change the line thickness and the dot size. </li> </ul>
                """)
    );

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * {@inheritDoc}
     * @since 5.11
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, HierarchicalClusterNodeParameters.class);
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
            HierarchicalClusterNodeParameters.class, //
            VIEWS, //
            NodeType.Learner, //
            List.of(), //
            null //
        );
    }

    /**
     * {@inheritDoc}
     * @since 5.11
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, HierarchicalClusterNodeParameters.class));
    }

}
