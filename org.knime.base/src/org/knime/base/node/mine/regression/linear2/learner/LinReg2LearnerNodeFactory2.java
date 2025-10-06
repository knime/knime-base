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
 * History
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.linear2.learner;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.base.node.mine.regression.linear2.view.LinReg2LineNodeView;
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
import org.knime.node.impl.description.ExternalResource;
import org.knime.node.impl.description.PortDescription;
import org.knime.node.impl.description.ViewDescription;

/**
 * Factory class for linear regression node.
 *
 * @author Heiko Hofer
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings("restriction")
public final class LinReg2LearnerNodeFactory2 extends NodeFactory<LinReg2LearnerNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public LinReg2LearnerNodeModel createNodeModel() {
        return new LinReg2LearnerNodeModel(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"removal", "java:S5738"})
    @Override
    public NodeView<LinReg2LearnerNodeModel> createNodeView(final int index, final LinReg2LearnerNodeModel model) {
        return switch (index) {
            case 0 -> new LinReg2LearnerNodeView(model);
            case 1 -> new LinReg2LineNodeView(model);
            default -> throw new IndexOutOfBoundsException();
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"removal", "java:S5738"})
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Linear Regression Learner";

    private static final String NODE_ICON = "./linear_regression_learn.png";

    private static final String SHORT_DESCRIPTION = """
            Computes a multiple linear regression model.
            """;

    private static final String FULL_DESCRIPTION = """
            Computes a multiple linear regression model. Select a target column that represents the response variable
            and a set of columns that represent the independent variables.
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(fixedPort("Input data", """
            Table containing the data used for the regression.
            """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of( //
        fixedPort( //
            "Model for Predictor", //
            "The computed regression model."), //
        fixedPort( //
            "Coefficients and Statistics", //
            "Coefficients and statistics of the linear regression model." //
        ) //
    );

    private static final List<ViewDescription> VIEWS = List.of( //
        new ViewDescription( //
            "Linear Regression Result View", //
            "Displays the estimated coefficients and error statistics." //
        ), //
        new ViewDescription( //
            "Linear Regression Scatterplot View", //
            """
            Displays the input data along with the regression line in a scatterplot. The y-coordinate is fixed to
            the response column (the column that has been approximated) while the x-column can be chosen among the
            independent variables with numerical values. Note: If you have multiple input variables, this view is
            only an approximation. It will fix the value of each variable that is not shown in the view to its mean.
            Thus, this view generally only makes sense if you only have a few input variables.
            """ //
        ) //
    );

    private static final List<ExternalResource> EXTERNAL_RESOURCES = List
        .of(new ExternalResource("https://en.wikipedia.org/wiki/Linear_regression", "Wikipedia: Linear Regression"));

    private static final List<String> KEYWORDS = List.of("lm");

    @Override
    @SuppressWarnings({"removal", "java:S5738"})
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, LinReg2LearnerNodeFactory2Parameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
            SHORT_DESCRIPTION, FULL_DESCRIPTION, EXTERNAL_RESOURCES, LinReg2LearnerNodeFactory2Parameters.class, VIEWS,
            NodeType.Learner, KEYWORDS, null);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, LinReg2LearnerNodeFactory2Parameters.class));
    }
}
