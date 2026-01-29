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
 * ---------------------------------------------------------------------
 *
 * History
 *   12.05.2010 (hofer): created
. */
package org.knime.base.node.viz.crosstable;

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
 * This is the factory for the Crosstab node.
 *
 * @author Heiko Hofer
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class CrosstabNodeFactory extends NodeFactory<CrosstabNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public CrosstabNodeModel createNodeModel() {
        return new CrosstabNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    @Override
    public NodeView<CrosstabNodeModel> createNodeView(final int viewIndex,
            final CrosstabNodeModel nodeModel) {
        switch (viewIndex) {
        case 0:
            return new CrosstabNodeView(nodeModel);
        default:
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Crosstab";

    private static final String NODE_ICON = "./crosstable.png";

    private static final String SHORT_DESCRIPTION = """
            Creates a cross-tabulation (also referred as contingency table or cross-tab).
            """;

    private static final String FULL_DESCRIPTION = """
            <p>Creates a cross table (also referred as <a href="http://en.wikipedia.org/wiki/Contingency_table">
                contingency table</a> or cross tab). It can be used to analyze the relation of two columns with
                categorical data and does display the frequency distribution of the categorical variables in a
                table.</p> <p>This node provides chi-square test statistics and, in case of a cross tabulation of 2x2
                dimension, Fisher's exact test. Both statistics test the null hypothesis of no association between the
                row variable and the column variable. The p-values are provided in the view and in the second output
                port.</p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Input Table", """
                Input table containing columns with categorical data.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Cross-Table", """
                The cross table in list form.
                """),
            fixedPort("Statistics Table", """
                The table with the statistics.
                """)
    );

    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Cross tabulation", """
                The following properties are displayed in the cross tabulation view: <br /> <i>Frequency:</i> The cell
                frequency. <br /> <i>Expected:</i> The expected frequency which is computed as (<i>column total</i> /
                <i>total</i>) * <i>row total</i>. <br /> <i>Deviation:</i> The deviation is computed as <i>Frequency</i>
                - <i>Expected</i>. <br /> <i>Percent:</i> The percent is the relative frequency computed as
                <i>Frequency</i> / <i>total</i>. <br /> <i>Row Percent:</i> The row percent is computed as
                <i>Frequency</i> / <i>row total</i>. <br /> <i>Column Percent:</i> The column percent is computed as
                <i>Frequency</i> / <i>column total</i>. <br /> <i>Cell Chi-Square:</i> The contribution of this cell to
                the value of the Chi-Square statistic. The Cell Chi-Square sums up to the value of the Chi-Square
                statistic. <br /> <br /> For some properties the row totals and column totals are displayed beside the
                table and underneath the table, respectively. <br /> You can control the size of the displayed table
                with the <i>Max rows</i> and the <i>Max columns</i> controls. <br /> <br /> The statistics table
                provides chi-square test statistics and, in case of a cross tabulation of 2x2 dimension, Fisher's exact
                test. Both statistics test the null hypothesis of no association between the row variable and the column
                variable. You can reject the null hypothesis when the p-value (Prop) is less than a significance value
                which is typically 0.01 or 0.05. In this case the result is said to be statistically significant. Please
                bear in mind that the Chi-Square test is based on <a
                href="http://en.wikipedia.org/wiki/Pearson%27s_chi-square_test#Assumptions"> some assumptions</a>.
                """)
    );

    /**
     * {@inheritDoc}
     * @since 5.11
     */
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
        return new DefaultNodeDialog(SettingsType.MODEL, CrosstabNodeParameters.class);
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
            CrosstabNodeParameters.class, //
            VIEWS, //
            NodeType.Manipulator, //
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
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, CrosstabNodeParameters.class));
    }

}
