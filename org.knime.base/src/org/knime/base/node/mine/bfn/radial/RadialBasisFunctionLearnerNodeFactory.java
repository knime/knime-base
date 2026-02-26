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
package org.knime.base.node.mine.bfn.radial;

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
 * {@link NodeFactory} for the PNN Learner (DDA) node.
 *
 * @author Thomas Gabriel, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class RadialBasisFunctionLearnerNodeFactory extends NodeFactory<RadialBasisFunctionLearnerNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public RadialBasisFunctionLearnerNodeModel createNodeModel() {
        return new RadialBasisFunctionLearnerNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 1;
    }

    @Override
    public NodeView<RadialBasisFunctionLearnerNodeModel> createNodeView(
            final int i,
            final RadialBasisFunctionLearnerNodeModel nodeModel) {
        return new RadialBasisFunctionLearnerNodeView(nodeModel);
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "PNN Learner (DDA)";

    private static final String NODE_ICON = "./rbf.png";

    private static final String SHORT_DESCRIPTION = """
            Trains a Probabilistic Neural Network (PNN) on labeled data.
            """;

    private static final String FULL_DESCRIPTION = """
            Trains a Probabilistic Neural Network (PNN) based on the DDA (Dynamic Decay Adjustment) method on
            labeled data using
            <a href="http://www.inf.uni-konstanz.de/bioml2/publications/Papers1998/BeDi98_dda_neurocomp.pdf">
            Constructive Training of Probabilistic Neural Networks</a> as the underlying algorithm.<br /> This
            algorithm generates rules based on numeric data. Each rule is defined as high-dimensional Gaussian
            function that is adjusted by two thresholds, theta minus and theta plus, to avoid conflicts with rules
            of different classes. Each Gaussian function is defined by a center vector (from the first covered
            instance) and a standard deviation which is adjusted during training to cover only non-conflicting
            instances. The selected numeric columns of the input data are used as input data for training and
            additional columns are used as classification target, either one column holding the class information or
            a number of numeric columns with class degrees between 0 and 1 can be selected. The data output contains
            the rules after execution along with a number of of rule measurements. The model output port contains
            the PNN model, which can be used for prediction in the PNN Predictor node.
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Training Data", """
                Numeric data as well as class information used for training.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("PNN Rules", """
                Rules as Gaussian functions, classification columns, and additional rule measures.
                """),
            fixedPort("PNN Model", """
                PNN model can be used for prediction.
                """)
    );

    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Learner Statistics", """
                Displays a summary of the learning process.
                """)
    );

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * {@inheritDoc}
     * @since 5.12
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, RadialBasisFunctionLearnerNodeParameters.class);
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
            RadialBasisFunctionLearnerNodeParameters.class, //
            VIEWS, //
            NodeType.Learner, //
            List.of(), //
            null //
        );
    }

    /**
     * {@inheritDoc}
     * @since 5.12
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, RadialBasisFunctionLearnerNodeParameters.class));
    }

}
