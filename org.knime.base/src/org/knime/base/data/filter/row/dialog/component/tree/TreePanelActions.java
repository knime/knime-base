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

package org.knime.base.data.filter.row.dialog.component.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.knime.base.data.filter.row.dialog.component.config.TreePanelConfig;
import org.knime.base.data.filter.row.dialog.component.tree.model.TreeCondition;
import org.knime.base.data.filter.row.dialog.component.tree.model.TreeElement;
import org.knime.base.data.filter.row.dialog.component.tree.model.TreeGroup;
import org.knime.base.data.filter.row.dialog.model.AbstractElement;
import org.knime.base.data.filter.row.dialog.model.Condition;
import org.knime.base.data.filter.row.dialog.model.Group;
import org.knime.base.data.filter.row.dialog.util.NodeValidationHelper;

/**
 * UI actions for the {@link TreePanel}.
 *
 * @author Viktor Buria
 */
public class TreePanelActions {

    private final TreePanelConfig m_config;

    private final JTree m_tree;

    /**
     * Creates {@link JTree} element wrapper for the chosen element.
     *
     * @param element the element to be wrapped.
     * @return the wrapped element.
     */
    public static DefaultMutableTreeNode createTreeNode(final AbstractElement element) {
        final boolean isGroup = element instanceof Group;
        return new DefaultMutableTreeNode(
            isGroup ? new TreeGroup((Group)element) : new TreeCondition((Condition)element),
            isGroup);
    }

    /**
     * Constructs a {@link TreePanelActions}.
     *
     * @param config the {@link TreePanelConfig} object
     * @param tree the {@linkplain JTree tree} structure
     */
    public TreePanelActions(final TreePanelConfig config, final JTree tree) {
        m_config = Objects.requireNonNull(config, "config");
        m_tree = Objects.requireNonNull(tree, "tree");
    }

    /**
     * Creates new {@link Condition} and adds then to the element tree.
     */
    public void addCondition() {
        final DefaultTreeModel model = (DefaultTreeModel)m_tree.getModel();

        final DefaultMutableTreeNode newCondition = createNewConditionTreeNode();

        final DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
        if (root == null) {

            model.setRoot(newCondition);

            refreshTreeAndExpandRows();

            selectTreeNode(newCondition);

            return;
        }

        final TreeElement<?> rootView = (TreeElement<?>)root.getUserObject();

        if (rootView instanceof TreeCondition) {
            final DefaultMutableTreeNode newRoot = createTreeNode(m_config.getElementFactory().createGroup());

            newRoot.add(root);

            newRoot.add(newCondition);

            model.setRoot(newRoot);

        } else if (rootView instanceof TreeGroup) {
            final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)m_tree.getLastSelectedPathComponent();

            final DefaultMutableTreeNode node;

            if (selectedNode == null) {
                node = root;
            } else if (selectedNode.getUserObject() instanceof TreeCondition) {
                node = (DefaultMutableTreeNode)selectedNode.getParent();
            } else {
                node = selectedNode;
            }
            node.add(newCondition);
        } else {
            throw new UnsupportedOperationException("Unknown type of view: " + rootView.getClass());
        }

        validateAllTreeNodes();

        refreshTreeAndExpandRows();

        selectTreeNode(newCondition);
    }

    private DefaultMutableTreeNode createNewConditionTreeNode() {
        final DefaultMutableTreeNode condition = createTreeNode(m_config.getElementFactory().createCondition());

        ((TreeCondition)condition.getUserObject()).setValidationResult(
            NodeValidationHelper.createSingleErrorResult(NodeValidationHelper.ERROR_COLUMN_NOT_DEFINED));
        return condition;
    }

    /**
     * Creates new {@link Group} and adds then to the element tree.
     *
     * @param node the tree element which will be included into the new group.
     */
    public void addGroup(final DefaultMutableTreeNode node) {
        if (node != null) {

            final DefaultMutableTreeNode newGroup = createTreeNode(m_config.getElementFactory().createGroup());

            final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();

            if (parent == null) {
                final DefaultTreeModel model = (DefaultTreeModel)m_tree.getModel();
                model.setRoot(newGroup);
            } else {
                final int childIndex = parent.getIndex(node);
                parent.insert(newGroup, childIndex);
            }
            newGroup.add(node); // do this after parent.getIndex!

            validateAllTreeNodes();

            refreshTreeAndExpandRows();

            selectTreeNode(newGroup);
        }
    }

    /**
     * Validates if {@link #ungroup(DefaultMutableTreeNode)} is supported by given node.
     *
     * @param node the group node to validate
     * @return <code>true</code> if node can be ungrouped
     */
    public boolean supportsUngroup(final DefaultMutableTreeNode node) {
        if (node != null && node.getUserObject() instanceof TreeGroup) {
            final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
            return parent != null || node.getChildCount() == 1;
        } else {
            return false;
        }
    }

    /**
     * Removes the chosen group and assign all children to the parent element.
     *
     * @param node the group to apply the ungrouping action.
     */
    public void ungroup(final DefaultMutableTreeNode node) {
        if (node != null && node.getUserObject() instanceof TreeGroup) {

            final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
            final TreeNode selected;

            if (parent == null) {
                final DefaultTreeModel model = (DefaultTreeModel)m_tree.getModel();
                final DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode)node.getFirstChild();
                firstChild.setParent(null);
                model.setRoot(firstChild);
                selected = firstChild;

            } else {
                // collect children
                @SuppressWarnings("unchecked")
                final Enumeration<MutableTreeNode> itertator = node.children();
                final List<MutableTreeNode> children = new ArrayList<>();
                while (itertator.hasMoreElements()) {
                    children.add(itertator.nextElement());
                }

                // insert children into parent, this will also remove them from node
                int childIndex = parent.getIndex(node);
                for (MutableTreeNode child : children) {
                    parent.insert(child, childIndex);
                    childIndex++;
                }

                // finally remove the node
                parent.remove(node);
                selected = children.isEmpty() ? parent : children.get(0);
            }

            validateAllTreeNodes();

            refreshTreeAndExpandRows();

            selectTreeNode(selected);
        }
    }

    /**
     * Removes condition or group element from a tree.
     *
     * @param node the element for removal
     */
    public void delete(final DefaultMutableTreeNode node) {
        DefaultMutableTreeNode parent = null;
        int childIndex = -1;

        if (node != null) {
            if (node.isRoot()) {
                resetTree();
            } else {
                parent = ((DefaultMutableTreeNode)node.getParent());
                childIndex = parent.getIndex(node);
                parent.remove(node);
            }
        }

        validateAllTreeNodes();

        refreshTreeAndExpandRows();

        if (parent != null) {
            if (parent.getChildCount() == 0) {
                selectTreeNode(parent);
            } else if (childIndex >= parent.getChildCount()) {
                selectTreeNode(parent.getChildAt(childIndex - 1));
            } else {
                selectTreeNode(parent.getChildAt(childIndex));
            }
        }
    }

    /**
     * Update selected element on the {@link TreePanel} UI.
     *
     * @param element the chosen element for refreshing
     */
    public void updateElementView(final TreeNode element) {
        final DefaultTreeModel model = (DefaultTreeModel)m_tree.getModel();
        model.nodeChanged(element);
    }

    /**
     * Repaints tree and expands all tree elements.
     */
    public void refreshTreeAndExpandRows() {
        final DefaultTreeModel model = (DefaultTreeModel)m_tree.getModel();
        model.reload();

        for (int i = 0; i < m_tree.getRowCount(); i++) {
            m_tree.expandRow(i);
        }
    }

    /**
     * Select root element of the tree model.
     */
    public void selectRootOfTree() {
        final DefaultTreeModel model = (DefaultTreeModel)m_tree.getModel();

        selectTreeNode((TreeNode)model.getRoot());
    }

    /**
     * Validate all nodes from a {@link TreePanel}.
     */
    public void validateAllTreeNodes() {
        final DefaultTreeModel model = (DefaultTreeModel)m_tree.getModel();

        final DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();

        NodeValidationHelper.validateTreeNode(root, m_config.getDataTableSpec(), m_config.getOperatorRegistry());
    }

    private void selectTreeNode(final TreeNode node) {
        if (node != null) {
            // Build path for a node selection in the tree.
            final List<TreeNode> path = new ArrayList<TreeNode>();
            path.add(node);
            TreeNode parent = node.getParent();
            while (parent != null) {
                path.add(parent);
                parent = parent.getParent();
            }
            Collections.reverse(path);

            m_tree.setSelectionPath(new TreePath(path.toArray(new Object[0])));
        }
    }

    private void resetTree() {
        m_tree.setModel(new DefaultTreeModel(null, true));
        refreshTreeAndExpandRows();
    }

}
