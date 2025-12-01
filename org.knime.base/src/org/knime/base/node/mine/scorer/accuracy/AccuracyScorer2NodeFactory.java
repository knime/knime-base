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
package org.knime.base.node.mine.scorer.accuracy;

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
 * The factory for the hilite scorer node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 * @since 4.2
 */
@SuppressWarnings("restriction")
public final class AccuracyScorer2NodeFactory extends NodeFactory<AccuracyScorer2NodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public AccuracyScorer2NodeModel createNodeModel() {
        return new AccuracyScorer2NodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 1;
    }

    @Override
    public NodeView<AccuracyScorer2NodeModel> createNodeView(final int i, final AccuracyScorer2NodeModel nodeModel) {
        if (i == 0) {
            return new AccuracyScorerNodeView<AccuracyScorer2NodeModel>(nodeModel);
        } else {
            throw new IllegalArgumentException("No such view");
        }
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Scorer";

    private static final String NODE_ICON = "./scorer.png";

    private static final String SHORT_DESCRIPTION = "Compares two columns by their attribute value pairs.";

    private static final String FULL_DESCRIPTION = """
            Compares two columns by their attribute value pairs and shows the confusion matrix, i.e. how many rows
                of which attribute and their classification match. Additionally, it is possible to hilight cells of this
                matrix to determine the underlying rows. The dialog allows you to select two columns for comparison; the
                values from the first selected column are represented in the confusion matrix's rows and the values from
                the second column by the confusion matrix's columns. The output of the node is the confusion matrix with
                the number of matches in each cell. Additionally, the second out-port reports a number of <a
                href="https://en.wikipedia.org/wiki/Confusion_matrix"> accuracy statistics</a> such as True-Positives,
                False-Positives, True-Negatives, False-Negatives, Recall, Precision, Sensitivity, Specificity,
                F-measure, as well as the overall accuracy and <a
                href="https://en.wikipedia.org/wiki/Cohen%27s_kappa">Cohen's kappa</a>.
            """;

    private static final List<PortDescription> INPUT_PORTS =
        List.of(fixedPort("Input table", "Table containing the columns to compare."));

    private static final List<PortDescription> OUTPUT_PORTS =
        List.of(fixedPort("Confusion matrix", "The confusion matrix."),
            fixedPort("Accuracy statistics", "The accuracy statistics table."));

    private static final List<ViewDescription> VIEWS = List.of(new ViewDescription("Confusion Matrix", """
            Displays the confusion matrix in a table view. It is possible to hilight cells of the matrix which
            propagates highlighting to the corresponding rows. Therefore, it is possible for example to identify
            wrong predictions.
            """));

    private static final List<String> KEYWORDS = List.of( //
        "model comparison", //
        "cohens kappa", //
        "accuracy" //
    );

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.10
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, AccuracyScorer2NodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
            SHORT_DESCRIPTION, FULL_DESCRIPTION, List.of(), AccuracyScorer2NodeParameters.class, VIEWS, NodeType.Other,
            KEYWORDS, null);
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.10
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, AccuracyScorer2NodeParameters.class));
    }

}
