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
 *   27.07.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree2.predictor2;

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
import org.knime.node.impl.description.ViewDescription;

/**
 * Node factory for the Decision Tree Predictor node.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 * @since 2.9
 */
@SuppressWarnings("restriction")
public class DecTreePredictorNodeFactory extends NodeFactory<DecTreePredictorNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public DecTreePredictorNodeModel createNodeModel() {
        return new DecTreePredictorNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 2;
    }

    @Override
    public NodeView<DecTreePredictorNodeModel> createNodeView(final int viewIndex,
        final DecTreePredictorNodeModel nodeModel) {
        if (viewIndex == 0) {
            return new DecTreePredictorGraphView(nodeModel);
        } else {
            return new DecTreePredictorNodeView(nodeModel);
        }
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Decision Tree Predictor";

    private static final String NODE_ICON = "./dectree_predictor.png";

    private static final String SHORT_DESCRIPTION = """
            Uses an existing decision tree to compute class labels for input vectors.
            """;

    private static final String FULL_DESCRIPTION = """
            This node uses an existing decision tree (passed in through the model port) to predict the class value
                for new patterns. The Node can be configured as follows:
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Decision Tree Model", """
                A previously learned decision tree model.
                """),
            fixedPort("Data to classify", """
                Input data to classify.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Classified Data", """
                The input table with one column added containing the classification and the probabilities depending on
                the options.
                """)
    );

    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Decision Tree View", """
                The decision tree as given in the model port along with the classified data. The tree can be expanded
                and collapsed with the plus/minus signs.
                """),
            new ViewDescription("Decision Tree View (simple)", """
                The decision tree as given in the model port along with the classified data.
                """)
    );

    private static final List<ExternalResource> LINKS = List.of(
         new ExternalResource(
            "https://www.knime.com/knime-introductory-course/chapter6/section3/decision-tree", """
                KNIME E-Learning Course: Decision Tree
                """)
    );

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
        return new DefaultNodeDialog(SettingsType.MODEL, DecTreePredictorNodeParameters.class);
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
            LINKS, //
            DecTreePredictorNodeParameters.class, //
            VIEWS, //
            NodeType.Predictor, //
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
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, DecTreePredictorNodeParameters.class));
    }

}
