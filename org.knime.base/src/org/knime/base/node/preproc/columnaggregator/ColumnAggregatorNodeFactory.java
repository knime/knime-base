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
 */

package org.knime.base.node.preproc.columnaggregator;

import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.io.IOException;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;
import org.xml.sax.SAXException;

/**
 * {@link NodeFactory} implementation of the column aggregator.
 *
 * @author Tobias Koetter, University of Konstanz
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public class ColumnAggregatorNodeFactory extends NodeFactory<ColumnAggregatorNodeModel> implements NodeDialogFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ColumnAggregatorNodeFactory.class);

    private static final String SYSPROP_KEY = "org.knime.base.node.preproc.groupby.dialog";

    /**
     * Feature flag for webUI configuration dialogs in local AP.
     */
    private static final boolean SYSPROP_WEBUI_DIALOG_AP = "js".equals(System.getProperty(SYSPROP_KEY));

    /**
     * If we are headless and a dialog is required (i.e. remote workflow editing), we enforce webUI dialogs.
     */
    private static final boolean SYSPROP_HEADLESS = Boolean.getBoolean("java.awt.headless");

    private static final boolean HAS_WEBUI_DIALOG = SYSPROP_HEADLESS || SYSPROP_WEBUI_DIALOG_AP;

    static {
        if (!HAS_WEBUI_DIALOG) {
            LOGGER.infoWithFormat("""
                    Modern dialog for Column Aggregator node can be enabled via system property "%s". \
                    Note: This property will be removed in a future version.
                    """, SYSPROP_KEY);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnAggregatorNodeModel createNodeModel() {
        return new ColumnAggregatorNodeModel();
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
    @Deprecated(since = "5.6", forRemoval = true)
    @Override
    public NodeView<ColumnAggregatorNodeModel> createNodeView(final int viewIndex,
        final ColumnAggregatorNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Column Aggregator";

    private static final String NODE_ICON = "columnAggregator.png";

    private static final String SHORT_DESCRIPTION = """
            Groups the selected columns per row and aggregates their cells using the selected aggregation method.
            """;

    private static final String FULL_DESCRIPTION = """
            <p> Groups the selected columns per row and aggregates their cells using the selected aggregation
                method. </p> <p> To change the name of the new created aggregation column double click on the name
                column. </p> <p> A detailed description of the available aggregation methods can be found on the
                'Description' tab in the node dialog. </p>
            """;

    private static final List<PortDescription> INPUT_PORTS = List.of(fixedPort("Data table", """
            The input table to aggregate.
            """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("Table with aggregation columns", """
            Result table with the aggregation columns per row.
            """));

    private static final List<String> KEYWORDS = List.of( //
        "Combine cells", //
        "deviation", //
        "maximum", //
        "correlation", //
        "count", //
        "mean", //
        "minimum", //
        "mode", //
        "percentile", //
        "quantile", //
        "range", //
        "set", //
        "unique" //
    );

    @Deprecated(since = "5.6", forRemoval = true)
    @Override
    public NodeDialogPane createNodeDialogPane() {
        if (HAS_WEBUI_DIALOG) {
            return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
        }
        return new ColumnAggregatorNodeDialog();
    }

    @Override
    public boolean hasNodeDialog() {
        return HAS_WEBUI_DIALOG;
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, ColumnAggregatorNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        if (!HAS_WEBUI_DIALOG) {
            return super.createNodeDescription();
        }
        return DefaultNodeDescriptionUtil.createNodeDescription(//
            NODE_NAME, //
            NODE_ICON, //
            INPUT_PORTS, //
            OUTPUT_PORTS, //
            SHORT_DESCRIPTION, //
            FULL_DESCRIPTION, //
            List.of(), //
            ColumnAggregatorNodeParameters.class, //
            null, //
            NodeType.Manipulator, //
            KEYWORDS, //
            null //
        );
    }

}
