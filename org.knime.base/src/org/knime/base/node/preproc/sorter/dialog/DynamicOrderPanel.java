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
 *   8 Nov 2019 (Timmo Waller-Ehrat): created
 */
package org.knime.base.node.preproc.sorter.dialog;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.SharedIcons;

/**
 * This panel can hold multiple panels in a dedicated panel. It also contains a button to add new panels to the
 * designated panel.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @param <T> DynamicPanelItem
 */
final class DynamicOrderPanel<T extends DynamicPanelItem> {

    private final JPanel m_panel;

    private final JPanel m_addItemArea;

    private final JPanel m_outerPanelsArea;

    private final JButton m_addButton;

    private DynamicItemContext<T> m_itemContext;

    private List<DynamicOuterPanel<T>> m_outerPanels;

    /**
     * This panel can hold multiple panels in a dedicated panel. It also contains a button to add new panels to the
     * designated panel.
     *
     * @param itemContext the context containing information about the displayed items
     */
    public DynamicOrderPanel(final DynamicItemContext<T> itemContext) {
        m_itemContext = itemContext;
        m_outerPanels = new ArrayList<>();

        m_panel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0;
        c.weighty = 0;
        m_outerPanelsArea = new JPanel();
        m_outerPanelsArea.setLayout(new BoxLayout(m_outerPanelsArea, BoxLayout.Y_AXIS));
        m_panel.add(m_outerPanelsArea, c);

        c.gridy = 1;
        c.weighty = 1;
        m_addItemArea = new JPanel(new GridBagLayout());
        m_panel.add(m_addItemArea, c);

        m_addButton = new JButton();
        m_addButton.setIcon(SharedIcons.ADD_PLUS.get());
        m_addButton.setText("Add Rule");
        m_addButton.addActionListener(e -> {
            if (m_addButton.isEnabled()) {
                addItem();
            }
        });
        m_addButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(20, 0, 20, 0);
        m_addItemArea.add(m_addButton, c);
    }

    /**
     * @return the actual panel for display
     */
    public JPanel getPanel() {
        return m_panel;
    }

    private void enableAddButton(final boolean enabled) {
        m_addButton.setEnabled(enabled);
    }

    /**
     * Sets a new context
     *
     * @param itemContext the new context to be set
     */
    public void setItemContext(final DynamicItemContext<T> itemContext) {
        CheckUtils.checkArgumentNotNull(m_itemContext);

        m_itemContext = itemContext;

        m_outerPanelsArea.removeAll();
        m_outerPanels.clear();

        final List<T> items = m_itemContext.getItems();
        if (items.isEmpty()) {
            createDefault();
        } else {
            createPanelFromInclude(items);
        }

        final int numOfOuterPanels = m_outerPanels.size();
        m_outerPanels.get(0).enableUpButton(false);
        m_outerPanels.get(0).enableDownButton(numOfOuterPanels > 1);
        m_outerPanels.get(0).enableDeleteButton(numOfOuterPanels > 1);
        m_outerPanels.get(numOfOuterPanels - 1).enableDownButton(false);

        enableAddButton(m_itemContext.canCreateItem());

        m_panel.revalidate();
        m_panel.repaint();
    }

    private void createDefault() {
        T item = m_itemContext.createItem();

        final DynamicOuterPanel<T> outerPanel = new DynamicOuterPanel<>(this, item, 0);
        m_outerPanels.add(outerPanel);

        final int numOfOuterPanels = m_outerPanels.size();
        outerPanel.setIdx(numOfOuterPanels - 1);

        m_outerPanelsArea.add(outerPanel.getPanel());
    }

    private void createPanelFromInclude(final List<T> items) {
        final Iterator<T> iterator = items.iterator();

        for (int i = 0; iterator.hasNext(); i++) {
            final DynamicOuterPanel<T> outerPanel = new DynamicOuterPanel<>(this, iterator.next(), i);
            m_outerPanels.add(outerPanel);
            m_outerPanelsArea.add(outerPanel.getPanel());
        }
    }

    private void addItem() {
        createDefault();
        enableAddButton(m_itemContext.canCreateItem());

        final int numOfOuterPanels = m_outerPanels.size();
        m_outerPanels.get(numOfOuterPanels - 2).enableDownButton(true);
        m_outerPanels.get(numOfOuterPanels - 1).enableDownButton(false);
        m_outerPanels.get(0).enableDeleteButton(numOfOuterPanels > 1);

        m_panel.revalidate();
        m_panel.repaint();
    }

    /**
     * Moves an item up or down in the list
     *
     * @param item the item which should be moved
     * @param moveUp <code>true</code> for moving the item up, <code>false</code> for moving the item down
     */
    void moveItem(final DynamicOuterPanel<T> outerPanel, final boolean moveUp) {
        final int index = moveUp ? outerPanel.getIdx() - 1 : outerPanel.getIdx() + 1;

        DynamicOuterPanel<T> swapPartner = m_outerPanels.get(index);
        T swapItem = swapPartner.getItem();
        T item = outerPanel.getItem();

        m_outerPanels.get(outerPanel.getIdx()).setItem(swapItem);
        m_outerPanels.get(index).setItem(item);

        m_itemContext.swap(item, swapItem);
    }

    /**
     * Remove an item from the list
     *
     * @param item the item which should be removed
     */
    void removeItem(final DynamicOuterPanel<T> outerPanel) {
        int numOfOuterPanels = m_outerPanels.size();
        if (numOfOuterPanels > 1) {
            m_itemContext.remove(outerPanel.getItem());

            m_outerPanelsArea.remove(outerPanel.getIdx());
            m_outerPanels.remove(outerPanel.getIdx());
            numOfOuterPanels -= 1;

            final Iterator<DynamicOuterPanel<T>> iterator = m_outerPanels.listIterator(outerPanel.getIdx());
            for (int i = outerPanel.getIdx(); iterator.hasNext(); i++) {
                iterator.next().setIdx(i);
            }

            m_outerPanels.get(0).enableUpButton(false);
            m_outerPanels.get(0).enableDownButton(numOfOuterPanels > 1);
            m_outerPanels.get(0).enableDeleteButton(numOfOuterPanels > 1);
            m_outerPanels.get(numOfOuterPanels - 1).enableDownButton(false);

            enableAddButton(m_itemContext.canCreateItem());
            m_panel.requestFocusInWindow();

            m_panel.revalidate();
            m_panel.repaint();
        }
    }
}
