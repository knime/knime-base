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

package org.knime.base.data.filter.row.dialog.component.renderer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.knime.base.data.filter.row.dialog.component.tree.model.TreeElement;
import org.knime.core.node.util.SharedIcons;

/**
 * Tree cell renderer for condition tree.
 *
 * @author Mor Kalla
 */
public class TreeElementCellRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = 2927287043246204928L;

    private final Color m_defaultBorderSelectionColor;

    private final Color m_defaultTextNonSelectionColor;

    private final Color m_defaultTextSelectionColor;

    /**
     * Constructs a {@link TreeElementCellRenderer} object.
     */
    public TreeElementCellRenderer() {
        m_defaultBorderSelectionColor = getBorderSelectionColor();
        m_defaultTextNonSelectionColor = getTextNonSelectionColor();
        m_defaultTextSelectionColor = getTextSelectionColor();
        final Icon groupIcon = SharedIcons.ADD_PLUS_FILLED.get();
        setLeafIcon(SharedIcons.FILTER.get());
        setOpenIcon(groupIcon);
        setClosedIcon(groupIcon);
    }

    @Override
    public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel,
        final boolean expanded, final boolean leaf, final int row, @SuppressWarnings("hiding") final boolean hasFocus) {

        setBorderSelectionColor(m_defaultBorderSelectionColor);
        setTextNonSelectionColor(m_defaultTextNonSelectionColor);
        setTextSelectionColor(m_defaultTextSelectionColor);

        setToolTipText(value.toString());

        final Object userObject = ((DefaultMutableTreeNode)value).getUserObject();

        if (userObject instanceof TreeElement<?>) {
            final TreeElement<?> view = (TreeElement<?>)userObject;

            view.getValidationResult().ifPresent(result -> {
                if (result.hasErrors()) {
                    setBorderSelectionColor(Color.RED);
                    setTextNonSelectionColor(Color.RED);
                    setTextSelectionColor(Color.RED);

                    final StringBuilder tooltip = new StringBuilder("<html><body>");
                    tooltip.append("<b>Errors:</b><ul>");
                    result.getErrors().forEach(error -> {
                        tooltip.append("<li>").append(error.getError()).append("</li>");
                    });
                    tooltip.append("</ul>");
                    tooltip.append("</body></html>");
                    setToolTipText(tooltip.toString());
                }
            });
        }
        return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    }

}
