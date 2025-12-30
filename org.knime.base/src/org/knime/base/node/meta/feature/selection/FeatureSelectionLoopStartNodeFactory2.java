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
 *   15.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

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
 * Factory for a Selection Loop Start node with two in and out ports.
 *
 * @author Adrian Nembach, KNIME.com
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class FeatureSelectionLoopStartNodeFactory2 extends NodeFactory<FeatureSelectionLoopStartNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public FeatureSelectionLoopStartNodeModel createNodeModel() {
        return new FeatureSelectionLoopStartNodeModel(2);
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FeatureSelectionLoopStartNodeModel> createNodeView(final int viewIndex,
        final FeatureSelectionLoopStartNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Feature Selection Loop Start (2:2)";

    private static final String NODE_ICON = "featureselection-start.png";

    private static final String SHORT_DESCRIPTION = """
            Start node for a feature selection loop
            """;

    private static final String FULL_DESCRIPTION = """
            This node is the start of the feature selection loop. The feature selection loop allows you to select,
                from all the features in the input data set, the subset of features that is best for model construction.
                With this node you determine (i) which features/columns are to be held fixed in the selection process.
                These constant or "static" features/columns are included in each loop iteration and are exempt from
                elimination; (ii) which selection strategy is to be used on the other (variable) features/columns and
                its settings; and (iii) the specific settings of the selected strategy. This node has two in and out
                ports. The respective first port is intended for training data and the second port for test data. The
                same filter is applied to both tables and they will therefore always contain the same columns. <br />
                <br /> The following feature selection strategies are available: <br /> <ul> <li> <b>Forward Feature
                Selection</b> is an iterative approach. It starts with having no feature selected. In each iteration,
                the feature that improves the model the most is added to the feature set. </li> <li> <b>Backward Feature
                Elimination</b> is an iterative approach. It starts with having all features selected. In each
                iteration, the feature that has on its removal the least impact on the models performance is removed.
                </li> <li> <b>Genetic Algorithm</b> is a stochastic approach that bases its optimization on the
                mechanics of biological evolution and genetics. Similar to natural selection, different solutions
                (individuals) are carried and mutated from generation to generation based on their performance
                (fitness). This approach converges into a local optimum and enabling early stopping might be
                recommended. See, e.g., <a href="https://en.wikipedia.org/wiki/Genetic_algorithm">this article</a> for
                more insights. </li> <li> <b>Random</b> is a simple approach that selects feature combinations randomly.
                There is no converging and by chance (one of) the best feature combination will be drawn in an early
                iteration, so that early stopping might be recommended. </li> </ul>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Table containing features and constant columns", """
                A data table containing all features and static columns needed for the feature selection. (Trainingdata)
                """),
            fixedPort("Table with same structure as the first one", """
                A data table containing all features and static columns needed for the feature selection. (Testdata)
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Filtered table", """
                The input table with some columns filtered out. (Training data)
                """),
            fixedPort("Filtered table", """
                The input table with some columns filtered out. (Test data)
                """)
    );

    private static final List<ExternalResource> LINKS = List.of(
         new ExternalResource(
            "https://docs.knime.com/latest/analytics_platform_flow_control_guide/index.html#loops", """
                KNIME Flow Control Guide: Section Loops
                """)
    );

    /**
     * {@inheritDoc}
     * @since 5.10
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, FeatureSelectionLoopStartNodeParameters.class);
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
            FeatureSelectionLoopStartNodeParameters.class, //
            null, //
            NodeType.LoopStart, //
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
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL,
            FeatureSelectionLoopStartNodeParameters.class));
    }

}
