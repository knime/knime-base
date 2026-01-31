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
package org.knime.base.node.mine.knn;

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

/**
 * This factory creates all necessary object for the kNN node.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 * @since 3.7
 */
@SuppressWarnings("restriction")
public class KnnNodeFactory2 extends NodeFactory<KnnNodeModel2> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public KnnNodeModel2 createNodeModel() {
        return new KnnNodeModel2();
    }

    @Override
    public NodeView<KnnNodeModel2> createNodeView(final int viewIndex,
            final KnnNodeModel2 nodeModel) {
        return null;
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }
    private static final String NODE_NAME = "K Nearest Neighbor";
    private static final String NODE_ICON = "./knn.png";
    private static final String SHORT_DESCRIPTION = """
            Classifies a set of test data based on the k Nearest Neighbor algorithm using the training data.
            """;
    private static final String FULL_DESCRIPTION = """
            Classifies a set of test data based on the k Nearest Neighbor algorithm using the training data. The
                underlying algorithm uses a KD tree and should therefore exhibit reasonable performance. However, this
                type of classifier is still only suited for a few thousand to ten thousand or so training instances. All
                (and only) numeric columns and the Euclidean distance are used in this implementation. All other columns
                (of non-numeric type) in the test data are being forwarded as-is to the output.
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Training Data", """
                Input port for the training data
                """),
            fixedPort("Test Data", """
                Input port for the test data
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Classified Data", """
                Output data with class labels
                """)
    );

    private static final List<String> KEYWORDS = List.of( //
        "classification", //
        "nearest neighbor", //
        "knn" //
    );

    /**
     * @since 5.11
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.11
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, KnnNodeFactory2Parameters.class);
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
            KnnNodeFactory2Parameters.class, //
            null, //
            NodeType.Learner, //
            KEYWORDS, //
            null //
        );
    }

    /**
     * @since 5.11
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, KnnNodeFactory2Parameters.class));
    }
}
