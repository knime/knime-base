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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
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

    private final JLabel m_addItemLabel;

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

        m_addItemLabel = new JLabel(SharedIcons.ADD_PLUS_FILLED.get());
        m_addItemLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (m_addItemLabel.isEnabled()) {
                    addItem();
                }
            }
        });
        m_addItemLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        m_panel = new JPanel();
        m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.Y_AXIS));

        m_outerPanelsArea = new JPanel();
        m_outerPanelsArea.setLayout(new BoxLayout(m_outerPanelsArea, BoxLayout.Y_AXIS));
        m_panel.add(m_outerPanelsArea);

        m_addItemArea = new JPanel();
        m_addItemArea.setLayout(new BoxLayout(m_addItemArea, BoxLayout.Y_AXIS));
        m_addItemArea.add(Box.createVerticalStrut(10));
        m_addItemArea.add(m_addItemLabel);
        m_addItemArea.add(Box.createVerticalStrut(30));
        m_panel.add(m_addItemArea);
    }

    /**
     * @return the actual panel for display
     */
    public JPanel getPanel() {
        return m_panel;
    }

    /**
     *
     * @param enabled set the enabled state of the button to add new criteria to this values
     */
    private void setAddButtonEnabled(final boolean enabled) {
        m_addItemLabel.setEnabled(enabled);
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
        m_outerPanels.get(0).showArrowUp(false);
        m_outerPanels.get(0).showArrowDown(numOfOuterPanels > 1);
        m_outerPanels.get(numOfOuterPanels - 1).showArrowDown(false);

        setAddButtonEnabled(m_itemContext.canCreateItem());

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
        setAddButtonEnabled(m_itemContext.canCreateItem());

        final int numOfOuterPanels = m_outerPanels.size();
        m_outerPanels.get(numOfOuterPanels - 2).showArrowDown(true);
        m_outerPanels.get(numOfOuterPanels - 1).showArrowDown(false);

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

            m_outerPanels.get(0).showArrowUp(false);
            m_outerPanels.get(0).showArrowDown(numOfOuterPanels > 1);
            m_outerPanels.get(numOfOuterPanels - 1).showArrowDown(false);

            setAddButtonEnabled(m_itemContext.canCreateItem());
        }
    }
}
