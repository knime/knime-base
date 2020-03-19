/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   20 Nov 2019 (Timmo Waller-Ehrat): created
 */
package org.knime.base.node.preproc.sorter.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.knime.core.node.util.SharedIcons;

/**
 * Holds a DynamicPanelItem and the buttons to move up/down and delete a DynamicPanelItem.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @param <T> DynamicPanelItem
 */
final class DynamicOuterPanel<T extends DynamicPanelItem> {
    private final DynamicOrderPanel<T> m_parent;

    private T m_item;

    private final JPanel m_panel = new JPanel(new GridBagLayout());

    private final JPanel m_contentPanel = new JPanel(new GridBagLayout());

    private final JButton m_upButton;

    private final JButton m_downButton;

    private final JButton m_deleteButton;

    private int m_idx;

    /**
     * @return the item
     */
    public T getItem() {
        return m_item;
    }

    /**
     * @param item the item to set
     */
    public void setItem(final T item) {
        m_contentPanel.removeAll();
        m_item = item;

        final GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        m_contentPanel.add(m_item.getPanel(), c);
        m_contentPanel.repaint();
    }

    /**
     * @return the current index
     */
    public int getIdx() {
        return m_idx;
    }

    /**
     * @param idx the index to set
     */
    public void setIdx(final int idx) {
        m_idx = idx;
    }

    /**
     * Create a panel including the buttons to move up/down and delete, which can hold an DynamicPanelItem
     *
     * @param parent the DynamicOrderPanel which holds all the DynamicOuterPanels
     * @param item the DynamicPanelItem which is held by the outer panel
     * @param idx the current index of the DynamicOuterPanel
     */
    public DynamicOuterPanel(final DynamicOrderPanel<T> parent, final T item, final int idx) {
        m_parent = parent;
        m_item = item;
        m_idx = idx;

        m_upButton = new JButton();
        m_upButton.setIcon(SharedIcons.MOVE_UP.get());
        m_upButton.setFocusable(false);

        m_downButton = new JButton();
        m_downButton.setIcon(SharedIcons.MOVE_DOWN.get());
        m_downButton.setFocusable(false);

        m_deleteButton = new JButton();
        m_deleteButton.setIcon(SharedIcons.DELETE_TRASH.get());

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;

        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        m_contentPanel.add(item.getPanel(), c);

        c.anchor = GridBagConstraints.NORTHEAST;
        c.fill = GridBagConstraints.NONE;
        c.gridy = 0;
        c.insets = new Insets(20, 10, 0, 10);
        m_panel.add(createControlPanel(), c);

        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 1;
        c.insets = new Insets(0, 10, 20, 10);
        m_panel.add(m_contentPanel, c);

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 2;
        c.insets = new Insets(0, 10, 0, 10);
        m_panel.add(separator, c);
    }

    private JPanel createControlPanel() {
        final DynamicOuterPanel<T> outerPanel = this;

        m_upButton.addActionListener(e -> {
            if (m_upButton.isEnabled()) {
                m_parent.moveItem(outerPanel, true);
            }
        });

        m_downButton.addActionListener(e -> {
            if (m_downButton.isEnabled()) {
                m_parent.moveItem(outerPanel, false);
            }
        });

        m_deleteButton.addActionListener(e -> {
            if (m_deleteButton.isEnabled()) {
                m_parent.removeItem(outerPanel);
            }
        });

        final JPanel controlPanel = new JPanel(new GridLayout(0, 3));
        controlPanel.add(m_upButton);
        controlPanel.add(m_downButton);
        controlPanel.add(m_deleteButton);
        controlPanel.setPreferredSize(new Dimension(80, 20));

        return controlPanel;
    }

    /**
     * @return the panel
     */
    public JPanel getPanel() {
        return m_panel;
    }

    /**
     * Set the button to move items upwards enabled/disabled.
     *
     * @param enabled <code>true</code> for enabled, <code>false</code> for disabled
     */
    public void enableUpButton(final boolean enabled) {
        m_upButton.setEnabled(enabled);
    }

    /**
     * Set the button to move items downwards enabled/disabled.
     *
     * @param enabled <code>true</code> for enabled, <code>false</code> for disabled
     */
    public void enableDownButton(final boolean enabled) {
        m_downButton.setEnabled(enabled);
    }

    /**
     * Set the button to remove items enabled/disabled.
     *
     * @param enabled <code>true</code> for enabled, <code>false</code> for disabled
     */
    public void enableDeleteButton(final boolean enabled) {
        m_deleteButton.setEnabled(enabled);
    }
}
