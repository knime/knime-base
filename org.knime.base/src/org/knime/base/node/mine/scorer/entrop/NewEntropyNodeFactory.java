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
 * History
 *   Apr 13, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.scorer.entrop;

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
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings("restriction")
public class NewEntropyNodeFactory extends NodeFactory<EntropyNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private final boolean m_enableOutput;

    /**
     * @param enableOutput whether node should have output port (it didn't have one in 1.x.x)
     */
    protected NewEntropyNodeFactory(final boolean enableOutput) {
        m_enableOutput = enableOutput;
    }

    /** Instantiates class with enabled output. */
    public NewEntropyNodeFactory() {
        this(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntropyNodeModel createNodeModel() {
        return new EntropyNodeModel(m_enableOutput);
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
    public EntropyNodeView createNodeView(final int viewIndex, final EntropyNodeModel nodeModel) {
        return new EntropyNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Entropy Scorer";

    private static final String NODE_ICON = "./entropyscore.png";

    private static final String SHORT_DESCRIPTION = "Scorer for clustering results given a reference clustering.";

    private static final String FULL_DESCRIPTION =
        """
                Scorer for clustering results given a reference clustering. Connect the table containing the reference
                clustering to the first input port (the table should contain a column with the cluster IDs) and the
                table with the clustering results to the second input port (it should also contain a column with some
                cluster IDs). Select the respective columns in both tables from the dialog. After successful execution,
                the view will show entropy values (the smaller the better) and some quality value (in [0,1] - with 1
                being the best possible value, as used in <a href="
                http://www.uni-konstanz.de/bioml/bioml2/publications/Papers2007/WiBe07_fcum_ijar/Fuzzy%20Clustering%20in%20Parallel%20Universes_submitted.pdf
                ">Fuzzy Clustering in Parallel Universes</a>, section 6: Experimental results).""";

    private static final List<PortDescription> INPUT_PORTS = List.of( //
        fixedPort("Reference clustering", "Table containing reference clustering."), //
        fixedPort("Clustering to score", "Table containing clustering (to score)."));

    private static final List<PortDescription> OUTPUT_PORTS = List.of( //
        fixedPort("Quality Table", """
                Table containing entropy values for each cluster. The last row contains statistics on the entire
                clustering. It corresponds to the table show in the Statistics View.
                """));

    private static final List<ViewDescription> VIEWS = List.of(new ViewDescription("Statistics View", """
                Simple statistics on the clustering such as number of clusters being found,
            number of objects in clusters, number of reference clusters, and total number of objects. Further
            statistics include:
            <ul>
             <li>
               Entropy: The accumulated entropy of all identified clusters, weighted by the relative cluster size. The
            entropy is not normalized and may be greater than 1.
             </li>
             <li>
               Quality: The quality value according to the formula referenced above. It is the sum of the weighted
               qualities of the individual clusters, whereby the quality of a single cluster is calculated as (1 -
               normalized_entropy). The domain of the quality value is [0,1].
             </li>
            </ul>
            The table at the bottom of the view provides statistics on <i>cluster size</i>, <i>cluster entropy</i>,
             <i>normalized cluster entropy</i> and <i>quality</i>. The <i>entropy</i> of a clusters is based on the
             reference clustering (provided at the first input port) and the <i>normalized entropy</i> is this value
             scaled to an interval [0, 1]. More precisely, it is the entropy divided by log2(number of different
             clusters in the  reference set). The quality value is only available in the last row (showing the overall
             statistics).
                """));

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * @since 5.9
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, NewEntropyNodeParameters.class);
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
            NewEntropyNodeParameters.class, //
            VIEWS, //
            NodeType.Other, //
            List.of(), //
            null);
    }

    /**
     * @since 5.9
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, NewEntropyNodeParameters.class));
    }

}
