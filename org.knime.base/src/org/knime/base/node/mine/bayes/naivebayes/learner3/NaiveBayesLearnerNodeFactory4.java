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
 *   Nov 27, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.mine.bayes.naivebayes.learner3;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;
import org.knime.node.impl.description.ViewDescription;
import org.xml.sax.SAXException;

/**
 * <code>NodeFactory</code> for the "Naive Bayes Learner" node.
 *
 * @author Tobias Koetter
 * @author Carsten Haubold
 */
@SuppressWarnings("restriction")
public final class NaiveBayesLearnerNodeFactory4 extends NodeFactory<NaiveBayesLearnerNodeModel3>
    implements NodeDialogFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public NaiveBayesLearnerNodeModel3 createNodeModel() {
        return new NaiveBayesLearnerNodeModel3();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NaiveBayesLearnerNodeView3 createNodeView(final int viewIndex, final NaiveBayesLearnerNodeModel3 nodeModel) {
        if (viewIndex != 0) {
            throw new IllegalArgumentException();
        }
        return new NaiveBayesLearnerNodeView3(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.6
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, NaiveBayesLearnerNodeParameters.class);
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * Use the WebUINodeConfiguration to generate the node description (replacing the XML file).
     */
    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {

        Collection<PortDescription> inPortDescriptions = List.of(fixedPort("The training data", "Training data"));
        Collection<PortDescription> outPortDescriptions = List.of(//
            fixedPort("PMML Naive Bayes Model",
                "Learned naive Bayes model. The model can be used to classify data with unknown target (class) "
                    + "attribute. To do so, connect the model out port to the \"Naive Bayes Predictor\" node."), //
            fixedPort("Statistics table", "Data table with attribute statistics e.g. counts per attribute class pair, "
                + "mean and standard deviation.") //
        );

        return DefaultNodeDescriptionUtil.createNodeDescription("Naive Bayes Learner", //
            "naiveBayesLearner.png", //
            inPortDescriptions, //
            outPortDescriptions, //
            "Creates a naive Bayes model from the given classified data.", //
            """
                    The node creates a <a href="http://en.wikipedia.org/wiki/Naive_Bayes_classifier">Bayesian model</a>
                    from the given training data. It
                    calculates the number of rows per attribute value per class for
                    nominal attributes and the Gaussian distribution for numerical
                    attributes. The created model could be used in the naive Bayes
                    predictor to predict the class membership of unclassified data.

                    <p>
                    The node displays a warning message if any columns are ignored due to unsupported data types.
                    For example Bit Vector columns are ignored when the PMML compatibility flag is enabled since they
                    are not supported by the PMML standard.
                    </p>
                    """, List.of(), //
            NaiveBayesLearnerNodeParameters.class, //
            List.of(new ViewDescription("Naive Bayes Learner View", """
                    The view displays the learned model with the number of rows per class
                    attribute. The number of rows per attribute per class for nominal
                    attributes and the Gaussian distribution per class
                    for numerical attributes.
                    """)), //
            NodeType.Learner, //
            List.of(), //
            null);
    }
}