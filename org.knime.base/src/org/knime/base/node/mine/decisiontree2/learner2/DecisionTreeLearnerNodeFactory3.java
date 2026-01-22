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
 */
package org.knime.base.node.mine.decisiontree2.learner2;

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
 * The Factory for the {@link DecisionTreeLearnerNodeModel2} algorithm.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 * @since 2.6
 */
@SuppressWarnings("restriction")
public class DecisionTreeLearnerNodeFactory3 extends NodeFactory<DecisionTreeLearnerNodeModel2>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public DecisionTreeLearnerNodeModel2 createNodeModel() {
        return new DecisionTreeLearnerNodeModel2(false);
    }

    @Override
    public int getNrNodeViews() {
        return 2;
    }

    @Override
    public NodeView<DecisionTreeLearnerNodeModel2> createNodeView(
                final int i, final DecisionTreeLearnerNodeModel2 nodeModel) {
        if (i == 0) {
            return new DecTreeLearnerGraphView2(nodeModel);
        } else {
            return new DecTreeNodeView2(nodeModel);
        }
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Decision Tree Learner";

    private static final String NODE_ICON = "./dectree.png";

    private static final String SHORT_DESCRIPTION = """
            Decision tree induction performed in memory.
            """;

    private static final String FULL_DESCRIPTION = """
            This node induces a classification decision tree in main memory. The target attribute must be nominal.
                The other attributes used for decision making can be either nominal or numerical. Numeric splits are
                always binary (two outcomes), dividing the domain in two partitions at a given split point. Nominal
                splits can be either binary (two outcomes) or they can have as many outcomes as nominal values. In the
                case of a binary split the nominal values are divided into two subsets. The algorithm provides two
                quality measures for split calculation; the gini index and the gain ratio. Further, there exist a post
                pruning method to reduce the tree size and increase prediction accuracy. The pruning method is based on
                the minimum description length principle.<br /> The algorithm can be run in multiple threads, and thus,
                exploit multiple processors or cores.<br /> Most of the techniques used in this decision tree
                implementation can be found in "C4.5 Programs for machine learning", by J.R. Quinlan and in "SPRINT: A
                Scalable Parallel Classifier for Data Mining", by J. Shafer, R. Agrawal, M. Mehta (<a
                href="https://www.vldb.org/conf/1996/P544.PDF">https://www.vldb.org/conf/1996/P544.PDF</a>)<br />
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Input data", """
                The pre-classified data that should be used to induce the decision tree. At least one attribute must be
                nominal.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Decision Tree Model", """
                The induced decision tree. The model can be used to classify data with unknown target (class) attribute.
                To do so, connect the model out port to the "Decision Tree Predictor" node.
                """)
    );

    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Decision Tree View", """
                Visualizes the learned decision tree. The tree can be expanded and collapsed with the plus/minus signs.
                """),
            new ViewDescription("Decision Tree View (simple)", """
                Visualizes the learned decision tree. The tree can be expanded and collapsed with the plus/minus signs.
                The squared brackets show the splitting criteria. This is the attribute name on which the parent node
                was split and the value (numeric) and nominal value (set) that has led to this child. The class value in
                single quotes states the majority class in this node. The value in round brackets states (x of y) where
                x is the quantity of the majority class and y is the total count of examples in this node. The bar with
                the black border and partly filled with yellow color represents the amount of records that belongs to
                the node relatively to the parent node. The colored pie chart renders the distribution of the color
                attribute associated with the input data table. NOTE: the colors not necessarily reflect the class
                attribute. If the color distribution and the target attribute should correspond to each other, ensure
                that the "Color Manager" node colors the same attribute as selected in this decision tree node as target
                attribute.
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
        return new DefaultNodeDialog(SettingsType.MODEL, DecisionTreeLearnerNodeFactory3Parameters.class);
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
            DecisionTreeLearnerNodeFactory3Parameters.class, //
            VIEWS, //
            NodeType.Learner, //
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
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, DecisionTreeLearnerNodeFactory3Parameters.class));
    }

}
