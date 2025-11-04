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
package org.knime.base.node.mine.regression.polynomial.learner2;

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
 * This factory creates all necessary objects for the polynomial regression
 * learner node.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 * @since 2.10
 */
@SuppressWarnings("restriction")
public class PolyRegLearnerNodeFactory2 extends
        NodeFactory<PolyRegLearnerNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public PolyRegLearnerNodeModel createNodeModel() {
        return new PolyRegLearnerNodeModel(false);
    }

    @Override
    public NodeView<PolyRegLearnerNodeModel> createNodeView(
            final int viewIndex, final PolyRegLearnerNodeModel nodeModel) {
        if (viewIndex == 0) {
            return new PolyRegCoefficientView(nodeModel);
        } else if (viewIndex == 1) {
            return new PolyRegLineNodeView(nodeModel);
        } else {
            return null;
        }
    }

    @Override
    protected int getNrNodeViews() {
        return 2;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Polynomial Regression Learner";

    private static final String NODE_ICON = "polyRegLearner.png";

    private static final String SHORT_DESCRIPTION = """
            Learner that builds a polynomial regression model from the input data
            """;

    private static final String FULL_DESCRIPTION = """
            This node performs polynomial regression on the input data and computes the coefficients that minimize
                the squared error. The user must choose one column as target (dependent variable) and a number of
                independent variables. By default, polynomials with degree 2 are computed, which can be changed in the
                dialog.
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Training data", """
                The input samples, which of the columns are used as independent variables can be configured in the
                dialog. The input must not contain missing values, you have to fix them by e.g. using the Missing Values
                node.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Regression model", """
                The computed regression coefficients as a PMML model for use in the Regression Predictor.
                """),
            fixedPort("Data with training error", """
                Training data classified with the learned model and the corresponding errors.
                """),
            fixedPort("Coefficients and Statistics", """
                The computed regression coefficients as a table with statistics related to the training data.
                """)
    );

    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Learned Coefficients", """
                Shows all learned coefficients all attributes.
                """),
            new ViewDescription("Scatter Plot", """
                Shows the data points and the regression function in one dimension.
                """)
    );

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
        return new DefaultNodeDialog(SettingsType.MODEL, PolyRegLearnerNodeParameters.class);
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
            PolyRegLearnerNodeParameters.class,
            VIEWS,
            NodeType.Learner,
            List.of(),
            null
        );
    }

    /**
     * @since 5.10
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, PolyRegLearnerNodeParameters.class));
    }
}
