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
 *   27.09.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.learner;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
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
 * NodeFactory for the SVM Learner Node.
 *
 * @author cebron, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 * @since 3.0
 */
@SuppressWarnings("restriction")
public class SVMLearnerNodeFactory2 extends NodeFactory<SVMLearnerNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public SVMLearnerNodeModel createNodeModel() {
        return new SVMLearnerNodeModel(false);
    }

    @Override
    public SVMLearnerNodeView createNodeView(final int viewIndex,
            final SVMLearnerNodeModel nodeModel) {
        return new SVMLearnerNodeView(nodeModel);
    }

    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "SVM Learner";

    private static final String NODE_ICON = "./SVM_learn.png";

    private static final String SHORT_DESCRIPTION = """
            Trains a support vector machine.
            """;

    private static final String FULL_DESCRIPTION = """
            <p> This node trains a support vector machine on the input data. It supports a number of different
            kernels (HyperTangent, Polynomial and RBF). The SVM learner supports multiple class problems as well (by
            computing the hyperplane between each class and the rest), but note that this will increase the runtime.
            </p>
            <p> The SVM learning algorithm used is described in the following papers:
            <a href="https://www.microsoft.com/en-us/research/publication/
            fast-training-of-support-vector-machines-using-sequential-minimal-optimization/">Fast Training of Support
            Vector Machines using Sequential Minimal Optimization</a>, by John C. Platt and
            <a href="https://digilander.libero.it/sedekfx/papers_/smo_mod.pdf"> Improvements to Platt's SMO Algorithm
            for SVM Classifier Design</a>, by S. S. Keerthi et. al. </p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Training Data", """
                Datatable with training data.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Trained SVM", """
                Trained Support Vector Machine.
                """)
    );

    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("SVM View", """
                Shows the trained Support Vector Machines for each class with their corresponding support vectors.
                """)
    );

    private static final List<String> KEYWORDS = List.of( //
        "support vector machine" //
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
        return new DefaultNodeDialog(SettingsType.MODEL, SVMLearnerNodeParameters.class);
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
            SVMLearnerNodeParameters.class, //
            VIEWS, //
            NodeType.Learner, //
            KEYWORDS, //
            null //
        );
    }

    /**
     * {@inheritDoc}
     * @since 5.11
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, SVMLearnerNodeParameters.class));
    }

}
