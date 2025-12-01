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
 *
 */
package org.knime.base.node.preproc.binnerdictionary;

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
 * {@link NodeFactory} for the "Binner (Dictionary)" Node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class BinByDictionaryNodeFactory extends
        NodeFactory<BinByDictionaryNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public BinByDictionaryNodeModel createNodeModel() {
        return new BinByDictionaryNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<BinByDictionaryNodeModel> createNodeView(
            final int viewIndex, final BinByDictionaryNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Binner (Dictionary)";

    private static final String NODE_ICON = "bindictionary.png";

    private static final String SHORT_DESCRIPTION = """
            Categorizes values in a column according to a dictionary table with min/max values.
            """;

    private static final String FULL_DESCRIPTION = """
            <p> Categorizes values in a column according to a dictionary table with min/max values. The table at the
                first input contains a column with values to be categorized. The second table contains a column with
                lower bound values, a column with upper bound values and a column with label values. The latter will be
                used as outcome in case a given value is between the corresponding lower and upper bound. Each row in
                the second table represents a rule, whereby the rules are evaluated top-down, i.e. rules with low row
                index have higher priority than rules in the subsequent rows. </p> <p> Either the lower or upper bound
                test can be disabled by switching the corresponding value in the dialog. Missing values in the
                columns containing upper and lower bounds will always evaluate the bound check to true. That is, a
                missing value in the lower bound column will always be smaller and a missing value in the upper bound
                column will always be larger than the value. Missing values in the value column (1st input) will result
                in a missing cell output (no categorization). </p> <p> <b>Note:</b> The table containing bound and label
                information (2nd input) will be read into memory during execution; it must be a relatively small table!
                </p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Input to be categorized", """
                Arbitrary input data with column to be binned.
                """),
            fixedPort("Rule/Dictionary table", """
                Table containing categorization rules with lower and upper bound and the label column.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Categorized data", """
                Input table amended by column with categorization values.
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
        return new DefaultNodeDialog(SettingsType.MODEL, BinByDictionaryNodeParameters.class);
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
            BinByDictionaryNodeParameters.class,
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
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, BinByDictionaryNodeParameters.class));
    }

}
