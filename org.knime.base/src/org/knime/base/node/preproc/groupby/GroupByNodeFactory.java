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

import java.util.Map;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;

/**
 * Factory class of the group by node.
 *
 * @author Tobias Koetter, University of Konstanz
 */
@SuppressWarnings("restriction")
public class GroupByNodeFactory extends NodeFactory<GroupByNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GroupByNodeFactory.class);

    private static final String WEBUI_DIALOG_DISABLED_PROPERTY = "org.knime.base.node.preproc.groupby.webui.disabled";

    private static final boolean WEBUI_DIALOG_DISABLED = Boolean.getBoolean(WEBUI_DIALOG_DISABLED_PROPERTY);

    static {
        if (WEBUI_DIALOG_DISABLED) {
            LOGGER.infoWithFormat("""
                    Modern dialog for GroupBy node is disabled via system property "%s".
                    Note: This property will be removed in a future version.
                    """, WEBUI_DIALOG_DISABLED_PROPERTY);
        }
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     *
     * @since 5.9
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        if (!WEBUI_DIALOG_DISABLED) {
            return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, GroupByNodeParameters.class));
        }
        throw new UnsupportedOperationException("KAI interface not available when WebUI dialog is disabled");

    }

}