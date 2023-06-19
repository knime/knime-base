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
 *   Oct 10, 2022 (bjoern): created
 */
package org.knime.filehandling.core.connections.base.hub;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.filehandling.core.connections.ItemVersionAware.RepositoryItemVersion;
import org.knime.filehandling.core.connections.base.hub.HubItemVersionSelectionComboBox.ItemVersionComboItem;

/**
 * {@link JComboBox} subclass to select a Hub repository item version.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
final class HubItemVersionSelectionComboBox extends JComboBox<ItemVersionComboItem> {

    private static final DateTimeFormatter LONG_DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:SS z");

    private static final long serialVersionUID = 123872L;

    private ChangeListener m_changeListener; // NOSONAR

    public HubItemVersionSelectionComboBox() {
        super();
        setModel(new ItemVersionComboModel());
        setRenderer(new ItemVersionComboboxCellRenderer());
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        setPreferredSize(new Dimension(250, 25));

        addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                onSelectionChanged();
            }
        });
    }

    @Override
    public ItemVersionComboItem getSelectedItem() {
        return ((ItemVersionComboModel)getModel()).getSelectedItem();
    }

    void setChangeListener(final ChangeListener changeListener) {
        m_changeListener = changeListener;
    }

    /**
     * Event handler method for the combobox model (when selected item changed)
     *
     * @param e
     */
    private void onSelectionChanged() {
        if (m_changeListener != null) {
            m_changeListener.stateChanged(new ChangeEvent(this));
        }
    }

    void setItems(final List<RepositoryItemVersion> items, final String selectedItemVersion) {
        ((ItemVersionComboModel)getModel()).setItems(items, selectedItemVersion);
    }

    public void clearItemsAndSelection() {
        ((ItemVersionComboModel)getModel()).clearItemsAndSelection();
    }

    /**
     * {@link ComboBoxModel} that maintains a list of (resolved) {@link ItemVersionComboItem}s.
     */
    private static class ItemVersionComboModel extends AbstractListModel<ItemVersionComboItem>
        implements ComboBoxModel<ItemVersionComboItem> {
        private static final long serialVersionUID = 1L;

        private List<ItemVersionComboItem> m_items = new ArrayList<>(); //NOSONAR not intended for serialization

        private ItemVersionComboItem m_selectedItem; //NOSONAR not intended for serialization

        void setItems(final List<RepositoryItemVersion> itemVersions, final String selectedItemVersion) {
            m_items.clear();

            var sortedVersionItems = itemVersions.stream() //
                .sorted((l, r) -> Long.compare(r.getVersion(), l.getVersion())) // sort in descending order
                .map(ItemVersionComboItem::new)//
                .collect(Collectors.toList());
            m_items.addAll(sortedVersionItems);

            if (selectedItemVersion == null && !m_items.isEmpty()) {
                setSelectedItem(m_items.get(0)); // pick latest
            } else {
                setSelectedItem(selectedItemVersion);
            }
        }

        public void clearItemsAndSelection() {
            m_items.clear();
            if (!m_items.isEmpty()) {
                setSelectedItem(m_items.get(0));
            }
        }

        @Override
        public void setSelectedItem(final Object newItem) {
            final var previouslySelectedItem = m_selectedItem;

            if (newItem instanceof String) {
                m_selectedItem = m_items.stream() //
                    .filter(i -> i.getVersion().equals(newItem)) //
                    .findFirst().orElse(null);
            } else if (newItem instanceof ItemVersionComboItem) {
                final var comboItem = (ItemVersionComboItem)newItem;
                m_selectedItem = m_items.stream() //
                    .filter(i -> Objects.equals(i.getVersion(), comboItem.getVersion())) //
                    .findFirst().orElse(m_items.get(0));
            } else if (newItem == null) {
                m_selectedItem = null;
            }

            if (!Objects.equals(previouslySelectedItem, m_selectedItem)) {
                fireContentsChanged(this, 0, getSize() - 1);
            }
        }

        @Override
        public ItemVersionComboItem getSelectedItem() {
            return m_selectedItem;
        }

        @Override
        public int getSize() {
            return m_items.size();
        }

        @Override
        public ItemVersionComboItem getElementAt(final int index) {
            return m_items.get(index);
        }
    }

    /**
     * {@link ListCellRenderer} that displays each {@link ItemVersionComboItem} in a single line.
     */
    private class ItemVersionComboboxCellRenderer implements ListCellRenderer<ItemVersionComboItem> {

        private final DefaultListCellRenderer m_versionInfoLabel = new DefaultListCellRenderer();

        ItemVersionComboboxCellRenderer() {
        }

        @Override
        public Component getListCellRendererComponent(final JList<? extends ItemVersionComboItem> list, //
            final ItemVersionComboItem item, //
            final int index, //
            final boolean isSelected, //
            final boolean cellHasFocus) {

            m_versionInfoLabel.getListCellRendererComponent(list, item, index, isSelected, cellHasFocus);

            if (item != null) {
                m_versionInfoLabel.setToolTipText(item.getToolTip());
            } else {
                m_versionInfoLabel.setToolTipText("");
            }

            return m_versionInfoLabel;
        }
    }

    static class ItemVersionComboItem {

        private final RepositoryItemVersion m_itemVersion;

        ItemVersionComboItem(final RepositoryItemVersion itemVersion) {
            m_itemVersion = itemVersion;
        }

        /**
         * @return the version number as a String.
         */
        public String getVersion() {
            return String.valueOf(m_itemVersion.getVersion());
        }

        RepositoryItemVersion getItemVersion() {
            return m_itemVersion;
        }

        @Override
        public String toString() {
            return truncateName(createName());
        }

        private String createName() {
            return String.format("#%d %s", m_itemVersion.getVersion(), m_itemVersion.getTitle());
        }

        public static String truncateName(final String name) {
            if (name.length() > 100) {
                return name.substring(0, 80).concat("...");
            } else {
                return name;
            }
        }

        public String getToolTip() {
            return String.format(
                "<html><b>Version number:</b> %d<br><b>Title:</b> %s<br><b>Created on:</b> %s<br><b>Author:</b> %s<br><b>Description:</b> %s</html>", //
                m_itemVersion.getVersion(), //
                m_itemVersion.getTitle(), //
                LONG_DATE_TIME_FORMATTER.format(toZonedDateTime(m_itemVersion.getCreatedOn())), //
                m_itemVersion.getAuthor(), //
                m_itemVersion.getDescription());
        }

        private static ZonedDateTime toZonedDateTime(final Instant instant) {
            return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        @Override
        public int hashCode() {
            return Objects.hash(m_itemVersion);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ItemVersionComboItem other = (ItemVersionComboItem)obj;
            return Objects.equals(m_itemVersion, other.m_itemVersion);
        }
    }
}
