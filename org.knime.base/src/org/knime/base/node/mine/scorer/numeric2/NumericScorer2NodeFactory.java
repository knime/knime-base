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
 *
 * History
 *   23.10.2013 (gabor): created
 */
package org.knime.base.node.mine.scorer.numeric2;

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
import org.knime.node.impl.description.ViewDescription;

/**
 * <code>NodeFactory</code> for the "NumericScorer" Node. Computes the distance between the a numeric column's values
 * and predicted values.
 *
 * @author Gabor Bakos
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 * @since 4.0
 */
@SuppressWarnings("restriction")
public class NumericScorer2NodeFactory extends NodeFactory<NumericScorer2NodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public NumericScorer2NodeModel createNodeModel() {
        return new NumericScorer2NodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 1;
    }

    @Override
    public NodeView<NumericScorer2NodeModel> createNodeView(final int viewIndex,
        final NumericScorer2NodeModel nodeModel) {
        return new NumericScorer2NodeView(nodeModel);
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Numeric Scorer";

    private static final String NODE_ICON = "./scorer.png";

    private static final String SHORT_DESCRIPTION = """
            Computes certain statistics between the a numeric column's values and predicted values.
            """;

    private static final String FULL_DESCRIPTION = """
            This node computes certain statistics between the a numeric column's values (r<sub>i</sub>) and
                predicted (p<sub>i</sub>) values. It computes <a
                href="http://en.wikipedia.org/wiki/Coefficient_of_determination">R²</a>=1-SS<sub>res</sub>/SS<sub>tot
                </sub>=1-Σ(p<sub>i</sub>-r<sub>i</sub>)²/Σ(r<sub>i</sub>-1/n*Σr<sub>i</sub>)²
                (can be negative!), <a href="http://en.wikipedia.org/wiki/Mean_absolute_error">Mean absolute error</a>
                (1/n*Σ|p<sub>i</sub>-r<sub>i</sub>|), <a
                href="http://en.wikipedia.org/wiki/Residual_sum_of_squares">Mean squared error</a>
                (1/n*Σ(p<sub>i</sub>-r<sub>i</sub>)²), <a
                href="http://en.wikipedia.org/wiki/Root-mean-square_deviation">Root mean squared error</a>
                (sqrt(1/n*Σ(p<sub>i</sub>-r<sub>i</sub>)²)), <a
                href="http://en.wikipedia.org/wiki/Mean_signed_difference">Mean signed difference</a>
                (1/n*Σ(p<sub>i</sub>-r<sub>i</sub>)), <a
                href="https://en.wikipedia.org/wiki/Mean_absolute_percentage_error">Mean absolute percentage error</a>
                1/n * Σ((|r<sub>i</sub> - p<sub>i</sub>|)/ |r<sub>i</sub>|), <a
                href="https://en.wikipedia.org/wiki/Coefficient_of_determination#Adjusted_R2">Adjusted
                R²</a>=1-(1-R²)(n-1)/(n-p-1) (can be negative!). The computed values can be inspected in the node's view
                and/or further processed using the output table.
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Table", """
                Table with predicted and reference numerical data.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Statistics", """
                The computed statistical measures: <ul> <li>R² - <a
                href="http://en.wikipedia.org/wiki/Coefficient_of_determination">coefficient of determination</a>,
                1-SS_res/SS_tot</li> <li><a href="http://en.wikipedia.org/wiki/Residual_sum_of_squares">Mean squared
                error</a> - 1/n*Σ((p_i-r_i)²)</li> <li><a href="http://en.wikipedia.org/wiki/Mean_absolute_error">Mean
                absolute error</a> - 1/n*Σ|p_i-r_i|</li> <li><a
                href="http://en.wikipedia.org/wiki/Root-mean-square_deviation">Root mean squared error</a> -
                Sqrt(1/n*Σ((p_i-r_i)²))</li> <li><a href="http://en.wikipedia.org/wiki/Mean_signed_difference">Mean
                signed difference</a> - 1/n*Σ(p_i - r_i)</li> <li><a
                href="https://en.wikipedia.org/wiki/Mean_absolute_percentage_error">Mean absolute percentage error</a>
                1/n * Σ((|r_i - p_i|)/|r_i|)</li> <li><a
                href="https://en.wikipedia.org/wiki/Coefficient_of_determination#Adjusted_R2">Adjusted
                R²</a>1-(1-R²)(n-1)/(n-p-1)</li> </ul>
                """)
    );

    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Statistics", """
                A table with the statistical measures
                """)
    );

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * {@inheritDoc}
     * @since 5.10
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, NumericScorer2NodeParameters.class);
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
            NumericScorer2NodeParameters.class, //
            VIEWS, //
            NodeType.Other, //
            List.of(), //
            null //
        );
    }

    /**
     * {@inheritDoc}
     * @since 5.10
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, NumericScorer2NodeParameters.class));
    }

}
