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
 *   Aug 28, 2019 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.probability.nominal.creator;

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
 * Node factory of the node that creates probability distributions of probability values.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class NominalDistributionCreatorNodeFactory
    extends NodeFactory<NominalDistributionCreatorNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public NominalDistributionCreatorNodeModel createNodeModel() {
        return new NominalDistributionCreatorNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<NominalDistributionCreatorNodeModel> createNodeView(final int viewIndex,
        final NominalDistributionCreatorNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Nominal Probability Distribution Creator";

    private static final String NODE_ICON = "./probability_distribution_creator.png";

    private static final String SHORT_DESCRIPTION = """
            Creates a Nominal Probability Distribution cell out of numeric cells.
            """;
    private static final String FULL_DESCRIPTION = """
            Creates a column containing a probability distribution either from numeric columns or a single string
                column. In case of numeric columns, one or more columns that contain probability values can be picked.
                The probability values must be non-negative and must sum up to 1. <br /> In case of a string column, one
                single column can be selected. The probability distribution of the string column produces a one-hot
                encoding of the string column. In order to do this, the column must have a valid domain, i.e., the
                possible values of the column must be known. You can use a <i>Domain Calculator</i> to calculate these
                values if they are not present. Each of the possible values will be treated as a separate class, i.e.,
                the number of distinct values in the string column will be the number of classes in the created
                probability distribution. The string value of a cell will have a probability of 1 whereby all the other
                possible string values of the column will have a probability of 0. The same output can be achieved by
                creating a probability distribution of the numeric output columns of the <i>One to Many</i> node applied
                to the same string column.
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Probability Values", """
                Data with columns containing probability values or a column containing string values.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Input Data with Nominal Probability Distribution", """
                Input data with an appended column that contains the nominal probability distribution.
                """)
    );

    /**
     * @since 5.10
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.10
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, NominalDistributionCreatorNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(
            NODE_NAME,
            NODE_ICON,
            INPUT_PORTS,
            OUTPUT_PORTS,
            SHORT_DESCRIPTION,
            FULL_DESCRIPTION,
            List.of(),
            NominalDistributionCreatorNodeParameters.class,
            null,
            NodeType.Manipulator,
            List.of(),
            null
        );
    }

    /**
     * @since 5.10
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, NominalDistributionCreatorNodeParameters.class));
    }

}
