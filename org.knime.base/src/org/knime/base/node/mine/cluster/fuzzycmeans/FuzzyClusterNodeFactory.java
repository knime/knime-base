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
package org.knime.base.node.mine.cluster.fuzzycmeans;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
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
 * Create classes for fuzzy c-means Clustering NodeModel, NodeView and
 * NodeDialogPane.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Nicolas Cebron, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class FuzzyClusterNodeFactory extends
        NodeFactory<FuzzyClusterNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private boolean m_showPMMLInput;

    public FuzzyClusterNodeFactory() {
        this(true);
    }

    FuzzyClusterNodeFactory(final boolean showPMMLInput) {
        m_showPMMLInput = showPMMLInput;

    }

    @Override
    public FuzzyClusterNodeModel createNodeModel() {
        return new FuzzyClusterNodeModel(m_showPMMLInput);
    }

    @Override
    public int getNrNodeViews() {
        return 1;
    }

    @Override
    public FuzzyClusterNodeView createNodeView(final int i,
            final FuzzyClusterNodeModel nodeModel) {
        if (i == 0) {
            return new FuzzyClusterNodeView(nodeModel);
        } else {
            throw new IllegalArgumentException(
                    "FuzzyClusterNode has only one view!!");
        }
    }

    /**
     * @since 5.9
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Fuzzy c-Means";
    private static final String NODE_ICON = "./kmeans.png";

    private static final String SHORT_DESCRIPTION = """
            Performs fuzzy c-means clustering.
            """;

    private static final String FULL_DESCRIPTION = """
            <p> The fuzzy c-means algorithm is a well-known unsupervised learning technique that can be used to
                reveal the underlying structure of the data. Fuzzy clustering allows each data point to belong to
                several clusters, with a degree of membership to each one.<br /> <b>Make sure that the input data is
                normalized to obtain better clustering results.</b><br /> The first output datatable provides the
                original datatable with the cluster memberships to each cluster.
                The second datatable provides the values of the cluster prototypes.
                <br /> Additionally, it is possible to induce a noise cluster, to detect noise in the
                dataset, based on the approach from R. N. Dave: 'Characterization and detection of noise in clustering'.
                </p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Training data", """
                Datatable with training data. Make sure that the data are normalized!
                """),
            fixedPort("PMML Preprocessing", """
                Optional PMML port object containing preprocessing operations.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Cluster Memberships", """
                Input table extended by cluster membership
                """),
            fixedPort("Prototypes", """
                Cluster centers
                """)
    );

    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Statistics View", """
                Shows the WithinClusterVariation and the BetweenClusterVariation, which are indicators for 'good'
                clustering.
                """)
    );

    /**
     * @since 5.9
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, FuzzyClusterNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(
            NODE_NAME,
            NODE_ICON,
            m_showPMMLInput ? INPUT_PORTS : List.of(INPUT_PORTS.get(0)),
            OUTPUT_PORTS,
            SHORT_DESCRIPTION,
            FULL_DESCRIPTION,
            List.of(),
            FuzzyClusterNodeParameters.class,
            VIEWS,
            NodeType.Learner,
            List.of(),
            null
        );
    }

    /**
     * @since 5.9
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, FuzzyClusterNodeParameters.class));
    }
}
