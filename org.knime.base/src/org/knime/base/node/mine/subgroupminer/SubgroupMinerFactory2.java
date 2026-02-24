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
 */
package org.knime.base.node.mine.subgroupminer;

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
 * The factory for the SubgroupMiner Node.
 *
 * @author Fabian Dill, University of Konstanz
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class SubgroupMinerFactory2 extends NodeFactory<SubgroupMinerModel2>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public SubgroupMinerModel2 createNodeModel() {
        return new SubgroupMinerModel2();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<SubgroupMinerModel2> createNodeView(
            final int viewIndex, final SubgroupMinerModel2 nodeModel) {
        return null;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Association Rule Learner";

    private static final String NODE_ICON = "./sgm_market.png";

    private static final String SHORT_DESCRIPTION = """
            Searches for frequent itemsets with a certain minimum support in a set of transactions and optionally
                generates association rules with a predefined confidence value from them.
            """;

    private static final String FULL_DESCRIPTION = """
            The association rule learner* searches for frequent itemsets meeting the user-defined minimum support
            criterion and, optionally, creates association rules from them. The column containing the transactions
            (BitVectors or Collections) has to be selected. The minimum support as an absolute number must be provided
            (therefore check the number of transactions to obtain a sensible criterion). If the frequent itemsets
            should be free (unconstrained) or closed or maximal has also be defined. Closed itemsets are frequent
            itemsets, which have no superset with the same support, thus providing all the information from free
            itemsets in a compressed form. Maximal itemsets are sets which have no frequent superset at all. The
            maximal itemset length must also be defined. If association rules are generated, a confidence value has to
            be provided. The confidence is a value to define how often the rule is right. Association rules generated
            here are in the form to have only one item in the consequence. The underlying data structure used by the
            algorithm can be either an ARRAY or a TIDList. Choose the former when there are many transactions and fewer
            items, and the latter if the structure of the input data is vice versa. <br/><br/>
            (*) RULE LEARNER is a registered trademark of Minitab, LLC and is used with Minitab’s permission.
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(
            fixedPort("Transactions", """
                Datatable containing transactions.
                """)
    );

    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Frequent itemsets/Association rules", """
                Datatable with discovered frequent itemsets or association rules.
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
        return new DefaultNodeDialog(SettingsType.MODEL, SubgroupMinerFactory2Parameters.class);
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
            SubgroupMinerFactory2Parameters.class, //
            null, //
            NodeType.Learner, //
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
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, SubgroupMinerFactory2Parameters.class));
    }

}
