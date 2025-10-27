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
package org.knime.base.node.preproc.groupby;

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
import org.knime.node.impl.description.ExternalResource;
import org.knime.node.impl.description.PortDescription;
import org.xml.sax.SAXException;

/**
 * Factory class of the group by node.
 *
 * @author Tobias Koetter, University of Konstanz
 */
@SuppressWarnings("restriction")
public class GroupByNodeFactory extends NodeFactory<GroupByNodeModel> implements NodeDialogFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GroupByNodeFactory.class);

    private static final String WEBUI_DIALOG_DISABLED_PROPERTY = "org.knime.base.node.preproc.groupby.webui.disabled";

    private static final boolean WEBUI_DIALOG_DISABLED = Boolean.getBoolean(WEBUI_DIALOG_DISABLED_PROPERTY);

    private static final String SHORT_DESCRIPTION = """
            Groups the table by the selected column(s) and aggregates the remaining
            columns using the selected aggregation method.
            """;

    private static final String FULL_DESCRIPTION = """
           <p>
            Groups the rows of a table by the unique values in the selected group columns.
            A row is created for each unique set of values of the selected group column.
            The remaining columns are aggregated based on the specified aggregation settings.
            The output table contains one row for each unique value combination of the selected
            group columns.
           </p>
           <p>
           The columns to aggregate can be either defined by selecting the columns directly,
           by name based on a search pattern or based on the data type. Input columns are handled in
           this order and only considered once e.g. columns that are added directly on the
           "Manual Aggregation" tab are ignored even if their name matches a search pattern on the
           "Pattern Based Aggregation" tab or their type matches a defined type on the
           "Type Based Aggregation" tab. The same holds for columns that are added based on a search pattern.
           They are ignored even if they match a criterion that has been defined in the "Type Based Aggregation" tab.
           </p>
           <p>
            The "Manual Aggregation" tab allows you to change the aggregation method of more than one
            column. In order to do so select the columns to change, open the context menu with a right mouse click
            and select the aggregation method to use.
           </p>
           <p>
            In the "Pattern Based Aggregation" tab you can assign aggregation methods to columns based on a
            search pattern. The pattern can be either a string with wildcards or a
            <a href="http://www.java.sun.com/javase/6/docs/api/java/util/regex/Pattern.html#sum">regular expression</a>.
            Columns where the name matches the pattern but where the data type is not compatible with the
            selected aggregation method are ignored. Only columns that have not been selected as group column or
            that have not been selected as aggregation column on the "Manual Aggregation" tab are considered.
           </p>
           <p>
            The "Type Based Aggregation" tab allows to select an aggregation method for all columns of a certain
            data type e.g. to compute the mean for all decimal columns (DoubleCell). Only columns that have not
            been handled by the other tabs e.g. group, column based and pattern based are considered.
            The data type list to choose from contains basic types e.g String, Double, etc. and all data types
            the current input table contains.
           </p>
           <p>
            A detailed description of the available aggregation methods can be
            found on the 'Description' tab in the node dialog.
           </p>
            """;

    static {
        if (WEBUI_DIALOG_DISABLED) {
            LOGGER.infoWithFormat("""
                    Modern dialog for GroupBy node is disabled via system property "%s".
                    Note: This property will be removed in a future version.
                    """, WEBUI_DIALOG_DISABLED_PROPERTY);
        }
    }

    @Override
    public GroupByNodeModel createNodeModel() {
        return new GroupByNodeModel();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated
     */
    @Override
    @Deprecated(since = "5.6", forRemoval = true)
    public NodeView<GroupByNodeModel> createNodeView(final int viewIndex, final GroupByNodeModel nodeModel) {
        return null;
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated
     */
    @Override
    @Deprecated(since = "5.6", forRemoval = true)
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        if (WEBUI_DIALOG_DISABLED) {
            return super.createNodeDescription();
        }
        return DefaultNodeDescriptionUtil.createNodeDescription("GroupBy", "groupBy.png",
            List.of(PortDescription.fixedPort("Data table", "The input table to group")),
            List.of(PortDescription.fixedPort("Group table",
                "Result table with one row for each existing value combination of the selected data")),
            SHORT_DESCRIPTION, FULL_DESCRIPTION,
            List.of(new ExternalResource(
                "https://www.knime.com/knime-introductory-course/chapter3/section2/classic-aggregations-with-groupby-node", // NOSONAR href
                "KNIME E-Learning Course: Classic Aggregations with GroupBy node")),
            GroupByNodeParameters.class, List.of(), NodeType.Manipulator,
            List.of("Summarize", "Aggregate", "group by", "maximum", "correlation", "count", "deviation", "mean",
                "median", "minimum", "quantile", "range", "set", "sum", "variance"),
            null);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated
     */
    @Override
    @Deprecated(since = "5.6", forRemoval = true)
    protected NodeDialogPane createNodeDialogPane() {
        if (!WEBUI_DIALOG_DISABLED) {
            return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
        }
        return new GroupByNodeDialog();
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.9
     */
    @Override
    public boolean hasNodeDialog() {
        // indicates that a webui dialog is available
        return !WEBUI_DIALOG_DISABLED;
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.9
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, GroupByNodeParameters.class);
    }
}