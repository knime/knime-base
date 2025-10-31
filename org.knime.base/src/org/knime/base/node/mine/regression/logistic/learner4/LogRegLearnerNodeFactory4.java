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
package org.knime.base.node.mine.regression.logistic.learner4;

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
import org.knime.node.impl.description.ExternalResource;
import org.knime.node.impl.description.PortDescription;

/**
 * Factory class for logistic regression node.
 *
 * @author Heiko Hofer
 * @author Adrian Nembach, KNIME.com
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 * @since 3.3
 */
@SuppressWarnings("restriction")
public final class LogRegLearnerNodeFactory4
    extends NodeFactory<LogRegLearnerNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public LogRegLearnerNodeModel createNodeModel() {
        return new LogRegLearnerNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return /*1*/0;
    }

    @Override
    public NodeView<LogRegLearnerNodeModel> createNodeView(
            final int index, final LogRegLearnerNodeModel model) {
        throw new IllegalStateException();
    }

    @Override
    @SuppressWarnings("java:S5738")
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Logistic Regression Learner";
    private static final String NODE_ICON = "./logistic_regression_learn.png";
    private static final String SHORT_DESCRIPTION = """
            Performs a multinomial logistic regression.
            """;

    private static final String FULL_DESCRIPTION = """
            Performs a multinomial logistic regression. Select in the dialog a target column (combo box on top), i.e.
            the response. The solver combo box allows you to select which solver should be used for the problem
            (see below for details on the different solvers). The two lists in the center of the dialog allow you to
            include only certain columns which represent the (independent) variables. Make sure the columns you want to
            have included being in the right "include" list. See article in wikipedia about
            <a href="http://en.wikipedia.org/wiki/Logistic_regression">logistic regression</a>
            for an overview about the topic.

            <h4>Important Note on Normalization</h4>
            The SAG solver works best with z-score normalized data.
            That means that the columns are normalized to have zero mean and a standard deviation of one. This can be
            achieved by using a normalizer node before learning. If you have very sparse data (lots of zero values),
            this normalization will destroy the sparsity. In this case it is recommended to only normalize the dense
            features to exploit the sparsity during the calculations (SAG solver with lazy calculation). Note, however,
            that the normalization will lead to different coefficients and statistics of those
            (standard error, z-score, etc.). Hence if you want to use the learner for statistics
            (obtaining the mentioned statistics) rather than machine learning (obtaining a classifier), you should
            carefully consider if normalization makes sense for your task at hand. If the node outputs missing values
            for the parameter statistics, this is very likely caused by insufficient normalization and you will have to
            use the IRLS solver if you can't normalize your data. <br/>

            <h4>Solvers</h4>
            The solver is the most important choice you make as it will dictate which algorithm is used to solve the
            problem.
            <ul>
                <li>
                    <b>Iteratively reweighted least squares</b> This solver uses an iterative optimization approach
                    which is also sometimes termed Fisher's scoring, to calculate the model. It works well for small
                    tables with only view columns but fails on larger tables. Note that it is the most error prone
                    solver because it can't calculate a model if the data is linearly separable (see Potential Errors
                    and Error Handling for more information). This solver is also not capable of dealing with tables
                    where there are more columns than rows because it does not support regularization.
                </li>
                <li>
                    <b>Stochastic average gradient (SAG)</b> This solver implements a variant of stochastic gradient
                    descent which tends to converge considerably faster than vanilla stochastic gradient descent. For
                    more information on the algorithm see the following
                    <a href="https://arxiv.org/abs/1309.2388">paper</a>. It works well for large tables and also tables
                    with more columns than rows. Note that in the later case a regularization prior other than "uniform"
                     must be selected. The default learning rate of 0.1 was selected because it often works well but
                     ultimately the optimal learning rate always depends on the data and should be treated as a
                     hyperparameter.
                </li>
            </ul>

            <h4>Learning Rate/Step Size Strategy</h4>
            Only relevant for the SAG solver. The learning rate strategy provides the learning rates for the
            gradient descent. When selecting a learning rate strategy and initial learning rate keep in mind that
            there is always a trade off between the size of the learning rate and the number of epochs that are
            required to converge to a solution. With a smaller learning rate the solver will take longer to find a
            solution but if the learning rate is too large it might skip over the optimal solution and diverge in
            the worst case.
            <ul>
                <li>
                    <b>Fixed</b> The provided step size is used for the complete training. This strategy works well for
                    the SAG solver, even if relatively large learning rates are used.
                </li>
                <li>
                    <b>Line Search</b> Experimental learning rate strategy that tries to find the optimal learning rate
                    for the SAG solver.
                </li>
            </ul>

            <h4>Regularization</h4>
            The SAG solver optimizes the problem using
            <a href="https://en.wikipedia.org/wiki/Maximum_a_posteriori_estimation"> maximum a posteriori estimation</a>
             which allows to specify a prior distribution for the coefficients of the resulting model. This form of
             regularization is the Bayesian version of other regularization approaches such as Ridge or LASSO. Currently
              the following priors are supported:
            <ul>
                <li>
                    <b>Uniform</b> This prior corresponds to no regularization at all and is the default. It essentially
                     means that all values are equally likely for the coefficients.
                </li>
                <li>
                    <b>Gauss</b> The coefficients are assumed to be normally distributed. This prior keeps the
                    coefficients from becoming too large but does not force them to be zero. Using this prior is
                    equivalent to using ridge regression (L2) with a lambda of 1/prior_variance.
                </li>
                <li>
                    <b>Laplace</b> The coefficients are assumed to follow a Laplace or double exponential distribution.
                    It tends to produce sparse solutions by forcing unimportant coefficients to be zero. It is therefore
                     related to the LASSO (also known as L1 regularization).
                </li>
            </ul>

            <h4>Potential Errors and Error Handling</h4>
            The computation of the model is an iterative optimization process that requires some properties of the data
            set. This requires a reasonable distribution of the target values and non-constant, uncorrelated columns.
            While some of these properties are checked during the node execution you may still run into errors during
            the computation. The list below gives some ideas what might go wrong and how to avoid such situations.
            <ul>
                <li>
                    <b>Insufficient Information</b> This is the case when the data does not provide enough information
                    about one or more target categories. Try to get more data or remove rows for target categories that
                    may cause the error. If you are interested in a model for one target category make sure to group the
                     target column before. For instance, if your data contains as target categories the values
                     "A", "B", ..., "Z" but you are only interested in getting a model for class "A" you can use a rule
                     engine node to convert your target into "A" and "not A".
                </li>
                <li>
                    <b>Violation of Independence</b> Logistic Regression is based on the assumption of statistical
                    independence. A common preprocessing step is to us a correlation filter to remove highly correlated
                    learning columns. Use a "Linear Correlation" along with a "Correlation Filter" node to remove
                    redundant columns, whereby often it's sufficient to compute the correlation model on a subset of the
                     data only.
                </li>
                <li>
                    <b>Separation</b> Please see <a href="http://en.wikipedia.org/wiki/Separation_(statistics)"> this
                    article about separation</a> for more information.
                </li>
                </ul>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Input data", """
                Table on which to perform regression. The input must not contain missing values, you have to fix them by
                e.g. using the Missing Values node.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Model for Predictor", """
                Model to connect to a predictor node.
                """),
            fixedPort("Coefficients and Statistics", """
                Coefficients and statistics (if calculated) of the logistic regression model.
                """),
            fixedPort("Model and Learning Properties", """
                Global learning and model properties like the number of iterations until convergence.
                """)
    );

    private static final List<ExternalResource> LINKS = List.of(
         new ExternalResource(
            "https://www.knime.com/knime-introductory-course/chapter6/section2/logistic-regression", """
                KNIME E-Learning Course: Logistic Regression
                """)
    );

    /**
     * @since 5.9
     */
    @Override
    @SuppressWarnings("java:S5738")
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.9
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, LogRegLearnerNodeParameters.class);
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
            LINKS,
            LogRegLearnerNodeParameters.class,
            null,
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
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, LogRegLearnerNodeParameters.class));
    }
}
