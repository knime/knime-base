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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.knime.base.data.filter.row.dialog.component.config.TreePanelConfig;
import org.knime.base.data.filter.row.dialog.component.handler.TreeConditionHandler;
import org.knime.base.data.filter.row.dialog.component.handler.TreeGroupHandler;
import org.knime.base.data.filter.row.dialog.component.renderer.TreeElementCellRenderer;
import org.knime.base.data.filter.row.dialog.component.tree.model.TreeGroup;

/**
 * Condition panel for database row filter node.
 *
 * @author Viktor Buria
 */
public class TreePanel extends JPanel {

    private static final long serialVersionUID = 1646576859900309912L;

    private static final Dimension TREE_PREFERRED_SIZE = new Dimension(400, 350);

    private final JTree m_tree;

    private JButton m_addButton;

    private JButton m_deleteButton;

    private JButton m_groupButton;

    private JButton m_ungroupButton;

    private TreePanelActions m_panelActions;

    private final TreePanelConfig m_config;

    private TreeGroupHandler asTreeGroup(final DefaultMutableTreeNode node) {
        return new TreeGroupHandler(node, m_panelActions, m_config.getOperatorRegistry());
    }

    private TreeConditionHandler asTreeCondition(final DefaultMutableTreeNode node) {
        return new TreeConditionHandler(node, m_panelActions, m_config.getOperatorRegistry());
    }

    /**
     * Constructs a {@link TreePanel} object.
     *
     * @param config the {@link TreePanelConfig} object
     */
    public TreePanel(final TreePanelConfig config) {
        m_config = Objects.requireNonNull(config, "config");

        m_tree = new JTree(new DefaultTreeModel(null, true));

        m_panelActions = new TreePanelActions(config, m_tree);

        setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        // Tree panel
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(getTreePanel(), gbc);
        // Buttons panel
        gbc.weighty = 1;
        gbc.gridy++;
        add(getButtonsPanel(), gbc);
    }

    /**
     * Gets the root element from a tree structure.
     *
     * @return the root element
     */
    public DefaultMutableTreeNode getRoot() {
        return (DefaultMutableTreeNode)m_tree.getModel().getRoot();
    }

    /**
     * Sets the root element into a tree structure.
     *
     * @param root the root element
     */
    public void setRoot(final DefaultMutableTreeNode root) {
        m_tree.setModel(new DefaultTreeModel(root, true));

        // Add new condition if the dialog has been opened for the first time.
        if (root == null) {
            m_panelActions.addCondition();
        } else {
            // Otherwise validate all existing conditions.
            m_panelActions.validateAllTreeNodes();
        }

        m_panelActions.refreshTreeAndExpandRows();

        m_panelActions.selectRootOfTree();
    }

    private JPanel getTreePanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;

        final JPanel previewPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.SOUTHWEST;
        final JLabel label = new JLabel("Query View");
        previewPanel.add(label);
        panel.add(previewPanel, c);

        ToolTipManager.sharedInstance().registerComponent(m_tree);
        m_tree.setToggleClickCount(0);
        m_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        m_tree.setExpandsSelectedPaths(false);
        m_tree.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        m_tree.setCellRenderer(new TreeElementCellRenderer());

        m_tree.addTreeSelectionListener(e -> {
            m_config.getTreeChangedListener().ifPresent(Runnable::run);

            final DefaultMutableTreeNode node = (DefaultMutableTreeNode)m_tree.getLastSelectedPathComponent();

            if (node == null) {
                m_deleteButton.setEnabled(false);

                m_groupButton.setEnabled(false);

                m_ungroupButton.setEnabled(false);

                m_config.getNoSelectionListener().ifPresent(Runnable::run);

                return;
            }

            final boolean isGroup = isGroup(node);

            m_deleteButton.setEnabled(true);

            m_groupButton.setEnabled(!isGroup);

            m_ungroupButton.setEnabled(m_panelActions.supportsUngroup(node));

            if (isGroup) {
                m_config.getSelectGroupListener().ifPresent(consumer -> consumer.accept(asTreeGroup(node)));
            } else {
                m_config.getSelectConditionListener().ifPresent(consumer -> consumer.accept(asTreeCondition(node)));
            }
            revalidate();
            repaint();
        });

        gbc.weighty = 10;
        gbc.gridy++;

        final JScrollPane scrollPanel = new JScrollPane(m_tree);
        scrollPanel.setPreferredSize(TREE_PREFERRED_SIZE);
        panel.add(scrollPanel, gbc);

        for (int i = 0; i < m_tree.getRowCount(); i++) {
            m_tree.expandRow(i);
        }

        return panel;
    }

    private JPanel getButtonsPanel() {
        final JPanel panel = new JPanel();

        m_addButton = new JButton("Add Condition");
        m_addButton.addActionListener(e -> {
            m_config.getTreeChangedListener().ifPresent(Runnable::run);

            m_panelActions.addCondition();
        });
        panel.add(m_addButton);

        m_groupButton = new JButton("Add Group");
        m_groupButton.setEnabled(false);
        m_groupButton.addActionListener(e -> {
            m_config.getTreeChangedListener().ifPresent(Runnable::run);

            final DefaultMutableTreeNode node = (DefaultMutableTreeNode)m_tree.getLastSelectedPathComponent();
            m_panelActions.addGroup(node);
        });
        panel.add(m_groupButton);

        m_ungroupButton = new JButton("Remove Group");
        m_ungroupButton.setEnabled(false);
        m_ungroupButton.addActionListener(e -> {
            m_config.getTreeChangedListener().ifPresent(Runnable::run);

            final DefaultMutableTreeNode node = (DefaultMutableTreeNode)m_tree.getLastSelectedPathComponent();
            m_panelActions.ungroup(node);
        });
        panel.add(m_ungroupButton);

        m_deleteButton = new JButton("Delete");
        m_deleteButton.setEnabled(false);
        m_deleteButton.addActionListener(e -> {
            m_config.getTreeChangedListener().ifPresent(Runnable::run);

            final DefaultMutableTreeNode node = (DefaultMutableTreeNode)m_tree.getLastSelectedPathComponent();
            m_panelActions.delete(node);
        });
        panel.add(m_deleteButton);

        return panel;
    }

    private static boolean isGroup(final DefaultMutableTreeNode node) {
        return node != null && node.getUserObject() instanceof TreeGroup;
    }

}
