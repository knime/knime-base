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
package org.knime.base.node.preproc.discretization.caim2.modelcreator;

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
 * The Factory for the CAIM Discretizer.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class CAIMDiscretization2NodeFactory extends
        NodeFactory<CAIMDiscretizationNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public CAIMDiscretizationNodeModel createNodeModel() {
        return new CAIMDiscretizationNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 1;
    }

    @Override
    public NodeView<CAIMDiscretizationNodeModel> createNodeView(
            final int viewIndex, final CAIMDiscretizationNodeModel nodeModel) {
        return new BinModelNodeView(nodeModel, new BinModelPlotter());
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "CAIM Binner";

    private static final String NODE_ICON = "./classbinner.png";

    private static final String SHORT_DESCRIPTION = """
            This node implements the CAIM discretization algorithm according to Kurgan and Cios (2004). The
                discretization is performed with respect to a selected class column.
            """;

    private static final String FULL_DESCRIPTION = """
            This node implements the CAIM binning (discretization) algorithm according to Kurgan and Cios (2004). The
            binning (discretization) is performed with respect to a selected class column. CAIM creates all possible
            binning boundaries and chooses those that minimize the class interdependancy measure. To reduce the runtime,
             this implementation creates only those boundaries where the value and the class changes. The algorithm
             finds a minimum number of bins (guided by the number of possible class values) and labels them
             "Interval_X". Only columns compatible with double values are binned and the column's type of the output
             table is changed to "String".
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Input Data", """
                The data table to bin (discretize).
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Binned Data", """
                The binned data table.
                """),
            fixedPort("Binning Model", """
                The model representing the binning. Contains the intervals for each bin of each column.
                """)
    );

    private static final List<ViewDescription> VIEWS = List.of(
            new ViewDescription("Binning Model", """
                The view shows the column's binning scheme. For each column a ruler is displayed on which the bin
                boundaries are marked.
                """)
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
        return new DefaultNodeDialog(SettingsType.MODEL, CAIMDiscretization2NodeParameters.class);
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
            CAIMDiscretization2NodeParameters.class,
            VIEWS,
            NodeType.Manipulator,
            List.of(),
            null
        );
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.10
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, CAIMDiscretization2NodeParameters.class));
    }

}
