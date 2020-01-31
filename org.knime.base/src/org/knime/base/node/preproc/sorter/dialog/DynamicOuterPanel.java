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
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
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

    private final JPanel m_panel;

    private final JPanel m_contentPanel;

    private final JLabel m_arrowUp;

    private final JLabel m_arrowDown;

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
        m_contentPanel.add(m_item.getPanel());
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

        this.m_parent = parent;
        this.m_item = item;
        this.m_idx = idx;

        this.m_arrowUp = new JLabel(SharedIcons.SMALL_ARROW_UP_DARK.get());
        this.m_arrowDown = new JLabel(SharedIcons.SMALL_ARROW_DOWN_DARK.get());

        this.m_panel = new JPanel(new GridBagLayout());

        m_panel.setMinimumSize(new Dimension(Integer.MAX_VALUE, 70));
        m_panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        this.m_contentPanel = new JPanel();
        this.m_contentPanel.setLayout(new BoxLayout(m_contentPanel, BoxLayout.Y_AXIS));
        m_contentPanel.add(item.getPanel());

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.insets = new Insets(0, 10, 0, 10);

        c.anchor = GridBagConstraints.NORTHEAST;
        c.fill = GridBagConstraints.NONE;
        c.gridy = 0;
        m_panel.add(createControlPanel(), c);

        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 1;
        m_panel.add(m_contentPanel, c);

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 2;
        c.insets = new Insets(5, 10, 5, 10);
        m_panel.add(separator, c);
    }

    private JPanel createControlPanel() {
        final JPanel controlPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        final DynamicOuterPanel<T> outerPanel = this;
        m_arrowUp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                m_parent.moveItem(outerPanel, true);
            }
        });

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        controlPanel.add(m_arrowUp, c);


        m_arrowDown.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                m_parent.moveItem(outerPanel, false);
            }
        });

        c.gridx = 1;
        controlPanel.add(m_arrowDown, c);

        JLabel delete = new JLabel(SharedIcons.DELETE_CROSS.get());
        delete.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                m_parent.removeItem(outerPanel);
            }
        });

        c.gridx = 2;
        controlPanel.add(delete, c);

        return controlPanel;
    }

    /**
     * @return the panel
     */
    public JPanel getPanel() {
        return m_panel;
    }

    /**
     * Set the visibility of the arrow to move an item up
     *
     * @param visible <code>true</code> for visible, <code>false</code> for invisible
     */
    public void showArrowUp(final boolean visible) {
        m_arrowUp.setVisible(visible);
    }

    /**
     * Set the visibility of the arrow to move an item down
     *
     * @param visible <code>true</code> for visible, <code>false</code> for invisible
     */
    public void showArrowDown(final boolean visible) {
        m_arrowDown.setVisible(visible);
    }
}
