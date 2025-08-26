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
package org.knime.base.node.preproc.targetshuffling;

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
 * This class is the factory for the y-scrambling node that creates all necessary objects.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Ali Asghar Marvi, KNIME AG, Zurich, Switzerland
 */
public class TargetShufflingNodeFactory extends NodeFactory<TargetShufflingNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private static final String NODE_NAME = "Target Shuffling";

    private static final String NODE_ICON = "TargetShuffling.png";

    private static final String SHORT_DESCRIPTION = """
            Randomly shuffles the values in one column
            """;

    private static final String FULL_DESCRIPTION =
        "This node performs Target Shuffling by randomly permuting the values in one column of the input table. "
            + "This will break any connection between input variables (learning columns) and response variable "
            + "(target column) while retaining the overall distribution of the target variable. Target shuffling is "
            + "used to estimate the baseline performance of a predictive model. It's expected that the quality of a "
            + "model (accuracy, area under the curve, R², ...) will decrease drastically if the target values were "
            + "shuffled as any relationship between input and target was removed. It's advisable to repeat this "
            + "process (target shuffling + model building + model evaluation) many times and record the bogus result "
            + "in order to receive good estimates on how well the real model performs in comparison to randomized "
            + "data. Target shuffling is sometimes called randomization test or y-scrambling. For more information "
            + "see also Handbook of Statistical Analysis and Data Mining Applications by Gary Miner, Robert Nisbet, "
            + "John Elder IV.";

    private static final List<PortDescription> INPUT_PORTS = List.of(fixedPort("Input data", """
            Any data table
            """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("Shuffled data", """
            Input table with values shuffled in one column
            """));

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, TargetShufflingNodeParameters.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TargetShufflingNodeModel createNodeModel() {
        return new TargetShufflingNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<TargetShufflingNodeModel> createNodeView(final int viewIndex,
        final TargetShufflingNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
            SHORT_DESCRIPTION, FULL_DESCRIPTION, List.of(), TargetShufflingNodeParameters.class, null,
            NodeType.Manipulator, List.of(), null);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, TargetShufflingNodeParameters.class));
    }
}
