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
 *   May 6, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.mine.transformation.pca.compute;

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
 * The PCA compute node factory.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class PCA2ComputeNodeFactory extends NodeFactory<PCA2ComputeNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public PCA2ComputeNodeModel createNodeModel() {
        return new PCA2ComputeNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<PCA2ComputeNodeModel> createNodeView(final int viewIndex, final PCA2ComputeNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }
    private static final String NODE_NAME = "PCA Compute";

    private static final String NODE_ICON = "./../../../pca/pca_compute.png";

    private static final String SHORT_DESCRIPTION = """
            Principal component analysis computation
            """;

    private static final String FULL_DESCRIPTION = """
            This node performs a <a href="http://en.wikipedia.org/wiki/Principal_component_analysis"> principal
                component analysis (PCA)</a> on the given input data. The directions of maximal variance (the principal
                components) are extracted and can be used in the PCA Apply node to project the input into a space of
                lower dimension while preserving a maximum of information.
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Table to transform", """
                Input data for the PCA
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Covariance matrix", """
                Covariance matrix of the input columns
                """),
            fixedPort("Spectral decomposition", """
                Table containing parameters extracted from the PCA. Each row in the table represents one principal
                component, whereby the rows are sorted with decreasing eigenvalues, i.e. variance along the
                corresponding principal axis. The first column in the table contains the component's eigenvalue, a high
                value indicates a high variance (or in other words, the respective component dominates the orientation
                of the input data). <br /> Each subsequent column (labeled with the name of the selected input column)
                contains a coefficient representing the influence of the respective input dimension to the principal
                component. The higher the absolute value, the higher the influence of the input dimension on the
                principal component. <br /> The mapping of the input rows to, e.g. the first principal axis, is computed
                as follows (all done in the PCA Apply node): For each dimension in the original space subtract the
                dimension's mean value and then multiply the resulting vector with the vector given by this table (the
                first row in the spectral decomposition table to get the value on the first PC, the second row for the
                second PC and so on).
                """),
            fixedPort("Transformation model", """
                Model holding the PCA transformation used by the PCA Apply node to apply the transformation to, e.g.
                another validation set.
                """)
    );

    private static final List<String> KEYWORDS = List.of( //
        "principal component analysis" //
    );

    /**
     * @since 5.9
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.9
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, PCA2ComputeNodeParameters.class);
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
            PCA2ComputeNodeParameters.class,
            null,
            NodeType.Manipulator,
            KEYWORDS,
            null
        );
    }

    /**
     * @since 5.9
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, PCA2ComputeNodeParameters.class));
    }


}
