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
 *   Feb 17, 2007 (wiswedel): created
 */
package org.knime.base.node.preproc.correlation.compute2;

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
 * Node factory for the Linear Correlation node.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Timothy Crundall, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 * @since 4.1
 */
public final class CorrelationCompute2NodeFactory extends NodeFactory<CorrelationCompute2NodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public CorrelationCompute2NodeModel createNodeModel() {
        return new CorrelationCompute2NodeModel();
    }

    @Override
    public NodeView<CorrelationCompute2NodeModel> createNodeView(final int viewIndex,
        final CorrelationCompute2NodeModel nodeModel) {
        return new CorrelationCompute2NodeView(nodeModel);
    }

    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Linear Correlation";

    private static final String NODE_ICON = "correlation.png";

    private static final String SHORT_DESCRIPTION = """
            Computes correlation coefficients for pairs of numeric or nominal columns.
            """;

    private static final String FULL_DESCRIPTION = """
            <p> Calculates for each pair of selected columns a correlation coefficient, i.e. a measure of the
                correlation of the two variables. </p> <p> Which correlation measure is applied depends on the types of
                the underlying variables: <br /> <tt>numeric &lt;-&gt; numeric</tt>: <a
                href="http://en.wikipedia.org/wiki/Pearson_product-moment_correlation_coefficient"> Pearson's
                product-moment coefficient</a>. Missing values in a column are ignored in such a way that for the
                computation of the correlation between two columns only complete records are taken into account. For
                instance, if there are three columns A, B and C and a row contains a missing value in column A but not
                in B and C, then the row will be ignored for computing the correlation between (A, B) and (A, C). It
                will not be ignored for the correlation between (B, C). This corresponds to the function
                <i>cor(&lt;data.frame&gt;, use="pairwise.complete.obs")</i> in the R statistics package. <br /> The
                value of this measure ranges from -1 (strong negative correlation) to 1 (strong positive correlation). A
                value of 0 represents no linear correlation (the columns might still be highly dependent on each other,
                though). <br /> The p-value for these columns indicates the probability of an uncorrelated system
                producing a correlation at least as extreme, if the mean of the correlation is zero and it follows a
                t-distribution with <i>df</i> degrees of freedom. <br /> <tt>nominal &lt;-&gt; nominal</tt>: <a
                href="http://en.wikipedia.org/wiki/Pearson%27s_chi-square_test"> Pearson's chi square test on the
                contingency table</a>. This value is then normalized to a range [0,1] using <a
                href="http://en.wikipedia.org/wiki/Cram%C3%A9r%27s_V"> Cramer's V</a>, whereby 0 represents no
                correlation and 1 a strong correlation. Missing values in nominal columns are treated such as they were
                a self-contained possible value. If one of the two columns contains more possible values than specified
                in the dialog (default 50), the correlation will not be computed. <br /> The p-value for these columns
                indicates the probability of independent variables showing as extreme level of dependence. The value is
                the same as for a chi-square test of independence of variables in a contingency table. <br />
                Correlation measures for other pairs of columns are not available, they are represented by missing
                values in the output table and crosses in the accompanying view. </p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(fixedPort("Numeric input data", """
            Numeric input data to evaluate
            """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("Correlation measure", """
            Correlation variables, p-values and degrees of freedom.
            """), fixedPort("Correlation matrix", """
            Correlation variables in a matrix representation.
            """), fixedPort("Correlation model", """
            A model containing the correlation measures. This model is appropriate to be read by the Correlation
            Filter node.
            """));

    private static final List<ViewDescription> VIEWS = List.of(new ViewDescription("Correlation Matrix", """
            Squared table view showing the pair-wise correlation values of all columns. The color range varies from
            dark red (strong negative correlation), over white (no correlation) to dark blue (strong positive
            correlation). If a correlation value for a pair of column is not available, the corresponding cell
            contains a missing value (shown as cross in the color view).
            """));

    private static final List<String> KEYWORDS = List.of( //
        "association analysis", //
        "pearson correlation", //
        "cramer" //
    );

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, CorrelationCompute2NodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
            SHORT_DESCRIPTION, FULL_DESCRIPTION, List.of(), CorrelationCompute2NodeParameters.class, VIEWS,
            NodeType.Other, KEYWORDS, null);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, CorrelationCompute2NodeParameters.class));
    }
}
