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

package org.knime.base.data.filter.row.dialog.component.handler;

import java.util.Objects;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.knime.base.data.filter.row.dialog.ValidationResult;
import org.knime.base.data.filter.row.dialog.component.tree.TreePanel;
import org.knime.base.data.filter.row.dialog.component.tree.TreePanelActions;
import org.knime.base.data.filter.row.dialog.component.tree.model.TreeCondition;
import org.knime.base.data.filter.row.dialog.component.tree.model.TreeElement;
import org.knime.base.data.filter.row.dialog.component.tree.model.TreeGroup;
import org.knime.base.data.filter.row.dialog.model.AbstractElement;
import org.knime.base.data.filter.row.dialog.model.Condition;
import org.knime.base.data.filter.row.dialog.model.Group;
import org.knime.base.data.filter.row.dialog.registry.OperatorRegistry;

/**
 * Abstract class to handle common tree element's actions.
 *
 * @param <E> a type of the tree element, i.e. {@link Group} or {@link Condition}
 * @param <V> a type of the tree element view, i.e. {@link TreeGroup} or {@link TreeCondition}
 * @author Viktor Buria
 */
public abstract class TreeElementHandler<E extends AbstractElement, V extends TreeElement<E>> {

    private final DefaultMutableTreeNode m_treeNode;

    private final V m_elementView;

    private final TreePanelActions m_panelActions;

    private final OperatorRegistry<?> m_operatorRegistry;

    /**
     * Constructs a {@link TreeElementHandler} object.
     *
     * @param treeNode the tree element, not {@code null}.
     * @param panelActions the available {@link TreePanel}'s actions, not {@code null}.
     * @param operatorRegistry an instance for the {@link OperatorRegistry}, not {@code null}.
     */
    @SuppressWarnings("unchecked")
    public TreeElementHandler(final DefaultMutableTreeNode treeNode, final TreePanelActions panelActions,
        final OperatorRegistry<?> operatorRegistry) {
        m_treeNode = Objects.requireNonNull(treeNode, "treeNode");
        m_elementView = Objects.requireNonNull((V)treeNode.getUserObject(), "elementView");
        m_panelActions = Objects.requireNonNull(panelActions, "panelActions");
        m_operatorRegistry = Objects.requireNonNull(operatorRegistry, "operatorRegistry");
    }

    /**
     * Gets wrapped group or condition.
     *
     * @return the {@link Group} or {@link Condition}
     */
    public E get() {
        return m_elementView.getValue();
    }

    /**
     * Gets the operator validation result.
     *
     * @return the {@link ValidationResult} object, can be {@code null}
     */
    public ValidationResult getValidationResult() {
        return m_elementView.getValidationResult().orElse(null);
    }

    /**
     * Gets the "is root or not" state of the wrapped element.
     *
     * @return the @code true} if wrapped element is a root of the tree structure.
     */
    public boolean isRoot() {
        return m_treeNode.isRoot();
    }

    /**
     * Update wrapped element on the {@link TreePanel} UI.
     */
    public void updateView() {
        m_panelActions.updateElementView(m_treeNode);
    }

    /**
     * Removes wrapped condition or group element from a tree.
     */
    public void delete() {
        m_panelActions.delete(m_treeNode);
    }

    /**
     * Gets an associated {@link MutableTreeNode} object for the wrapped element.
     *
     * @return the {@link MutableTreeNode}
     */
    protected DefaultMutableTreeNode getTreeNode() {
        return m_treeNode;
    }

    /**
     * Gets group or condition view object.
     *
     * @return the {@link TreeCondition} or {@link TreeGroup} object
     */
    protected V getView() {
        return m_elementView;
    }

    /**
     * Gets available actions for the {@link TreePanel}.
     *
     * @return the {@link TreePanelActions}
     */
    protected TreePanelActions getPanelActions() {
        return m_panelActions;
    }

    /**
     * Gets an operator registry component.
     *
     * @return the {@link OperatorRegistry} object
     */
    protected OperatorRegistry<?> getOperatorRegistry() {
        return m_operatorRegistry;
    }

}
