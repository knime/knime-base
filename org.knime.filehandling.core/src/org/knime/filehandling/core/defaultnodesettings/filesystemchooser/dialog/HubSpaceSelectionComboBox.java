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
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.commons.lang3.StringUtils;
import org.knime.filehandling.core.connections.SpaceAware.Space;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.HubSpaceSelectionComboBox.SpaceComboItem;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.core.util.Icons;

/**
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
final class HubSpaceSelectionComboBox extends JComboBox<SpaceComboItem> {

    private static final long serialVersionUID = 123872L;

    private boolean m_ignoreEditorTextEvents = false;

    private final DocumentListener m_editorDocumentListener = new DocumentListener() { // NOSONAR
        @Override
        public void changedUpdate(final DocumentEvent e) {
            onEditorTextEvent(e);
        }

        @Override
        public void insertUpdate(final DocumentEvent e) {
            onEditorTextEvent(e);
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            onEditorTextEvent(e);
        }
    };

    private ChangeListener m_changeListener; // NOSONAR

    public HubSpaceSelectionComboBox() {
        super();
        setModel(new SpaceComboModel());
        setEditable(true);
        setRenderer(new SpaceComboboxCellRenderer());
        setEditor(new SpaceComboBoxEditor());
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        setPreferredSize(new Dimension(250, 25));

        addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                onSelectionChanged();
            }
        });
    }

    @Override
    public SpaceComboItem getSelectedItem() {
        return ((SpaceComboModel)getModel()).getSelectedItem();
    }

    void setChangeListener(final ChangeListener changeListener) {
        m_changeListener = changeListener;
    }

    /**
     * Event handler method for the combobox editor {@link Document}.
     *
     * @param e
     */
    private void onEditorTextEvent(final DocumentEvent e) { // NOSONAR
        try {
            // m_ignoreEditorTextEvents is a circuit breaker flag, that avoids event loops where
            // text event -> combobox model change -> text event -> ...
            if (!m_ignoreEditorTextEvents) {
                setSelectedItem(e.getDocument().getText(0, e.getDocument().getLength()));
            }
        } catch (BadLocationException ex) { // NOSONAR
        }
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

    void setItems(final List<SpaceComboItem> items) {
        ((SpaceComboModel)getModel()).setItems(items);
    }

    /**
     * {@link ComboBoxModel} that maintains a list of (resolved) {@link SpaceComboItem}s, plus exactly one selected
     * item, which may come from manual text entry by the user. If the user enters some text, we interpret the text as
     * an ID and, if possible, map it to an existing item, otherwise a new (unresolved) {@link SpaceComboItem} is
     * created and added to the model.
     */
    private class SpaceComboModel extends AbstractListModel<SpaceComboItem> implements ComboBoxModel<SpaceComboItem> {
        private static final long serialVersionUID = 1L;

        private List<SpaceComboItem> m_items = new ArrayList<>(); //NOSONAR not intended for serialization

        private SpaceComboItem m_selectedItem; //NOSONAR not intended for serialization

        void setItems(final List<SpaceComboItem> comboBoxItems) {
            m_items.clear();
            m_items.addAll(comboBoxItems);
            m_items.sort((l, r) -> l.getName().compareToIgnoreCase(r.getName()));

            if (m_selectedItem == null && !m_items.isEmpty()) {
                setSelectedItem(m_items.get(0));
            } else {
                setSelectedItem(m_selectedItem);
            }
        }

        @Override
        public void setSelectedItem(final Object newItem) {
            final var previouslySelectedItem = m_selectedItem;

            if (newItem instanceof String) {
                var str = (String)newItem;
                m_selectedItem = m_items.stream() //
                    .filter(i -> i.getId().equals(newItem)) //
                    .findFirst().orElse(new SpaceComboItem(str, str));
            } else if (newItem instanceof SpaceComboItem) {
                var comboItem = (SpaceComboItem)newItem;
                m_selectedItem = m_items.stream() //
                    .filter(i -> i.getId().equals(comboItem.getId())) //
                    .findFirst().orElse(comboItem);
            } else if (newItem == null) {
                m_selectedItem = null;
            }

            if (!Objects.equals(previouslySelectedItem, m_selectedItem)) {
                // m_ignoreEditorTextEvents is a circuit breaker flag, that avoids event loops where
                // combobox model change -> text event -> combobox model change -> ...
                m_ignoreEditorTextEvents = true;
                fireContentsChanged(this, 0, getSize() - 1);
                m_ignoreEditorTextEvents = false;
            }
        }

        @Override
        public SpaceComboItem getSelectedItem() {
            return m_selectedItem;
        }

        @Override
        public int getSize() {
            var size = m_items.size();
            if (m_selectedItem != null && m_selectedItem.isResolved() && m_items.indexOf(m_selectedItem) == -1) {
                size++;
            }
            return size;
        }

        @Override
        public SpaceComboItem getElementAt(final int index) {
            if (index == m_items.size()) {
                return m_selectedItem;
            } else {
                return m_items.get(index);
            }
        }
    }

    /**
     * {@link ListCellRenderer} that displays each {@link SpaceComboItem} on two lines, including an icon.
     */
    private class SpaceComboboxCellRenderer implements ListCellRenderer<SpaceComboItem> {

        private final JPanel m_panel = new JPanel(new GridBagLayout());

        private final JLabel m_spaceIcon = new JLabel(Icons.getPublicSpaceIcon());

        private final DefaultListCellRenderer m_spaceName = new DefaultListCellRenderer();

        private final JLabel m_descriptionLabel = new JLabel();

        SpaceComboboxCellRenderer() {
            final var origFont = m_descriptionLabel.getFont();

            final var italicFont = origFont.deriveFont(Font.ITALIC, origFont.getSize() - 1f); // NOSONAR have to use float here
            m_descriptionLabel.setText("Owner: ? / ID: ?");
            m_descriptionLabel.setFont(italicFont);

            final var gbc = new GBCBuilder().resetPos().anchorLineStart().fillNone();
            m_panel.add(m_spaceIcon, gbc.build());

            gbc.incX().fillHorizontal().setWeightX(1);
            m_panel.add(m_spaceName, gbc.build());

            gbc.incY();
            m_panel.add(m_descriptionLabel, gbc.build());
        }

        @Override
        public Component getListCellRendererComponent(final JList<? extends SpaceComboItem> list, //
            final SpaceComboItem item, //
            final int index, //
            final boolean isSelected, //
            final boolean cellHasFocus) {

            m_spaceName.getListCellRendererComponent(list, item, index, isSelected, cellHasFocus);

            var spaceIcon = Icons.getPublicSpaceIcon();
            var spaceName = "";
            var description = "";
            String tooltip = null;

            if (item != null) {
                spaceName = item.getName();
                description = item.getDescription();
                tooltip = item.getToolTip();

                if (item.isResolved() && item.getSpace().isPrivate()) {
                    spaceIcon = Icons.getPrivateSpaceIcon();
                }
            }

            m_spaceIcon.setIcon(spaceIcon);
            m_spaceName.setText(spaceName);
            m_descriptionLabel.setText(description);

            m_spaceIcon.setToolTipText(tooltip);
            m_spaceName.setToolTipText(tooltip);
            m_descriptionLabel.setToolTipText(tooltip);

            var bgColor = m_spaceName.getBackground();
            m_panel.setBackground(bgColor);
            m_spaceIcon.setBackground(bgColor);
            m_descriptionLabel.setBackground(bgColor);

            return m_panel;
        }
    }

    /**
     * {@link BasicComboBoxEditor} that displays an icon and allows the user to edit the text.
     */
    private class SpaceComboBoxEditor extends BasicComboBoxEditor {

        private JPanel m_panel;

        private JLabel m_icon;

        @Override
        protected JTextField createEditorComponent() {
            m_panel = new JPanel() {
                private static final long serialVersionUID = 1L;

                @Override
                public void setEnabled(final boolean isEnabled) {
                    super.setEnabled(isEnabled);
                    m_icon.setEnabled(isEnabled);
                    editor.setEnabled(isEnabled);
                }
            };
            m_icon = new JLabel(Icons.getPublicSpaceIcon());
            editor = new JTextField(30);
            editor.setFont(m_icon.getFont());

            m_panel.setLayout(new GridBagLayout());

            var gbc = new GBCBuilder().resetPos().anchorLineStart().fillNone().setWeightX(0);
            m_panel.add(m_icon, gbc.build());

            gbc.incX().fillHorizontal().setWeightX(1);
            m_panel.add(editor, gbc.build());

            m_panel.setBackground(editor.getBackground());
            m_panel.setBorder(editor.getBorder());
            editor.setBorder(null);
            editor.getDocument().addDocumentListener(m_editorDocumentListener);

            return editor;
        }

        @Override
        public Component getEditorComponent() {
            return m_panel;
        }

        @Override
        public void setItem(final Object itemObj) {
            super.setItem(itemObj);

            // only refresh the icon and tooltip, the super method already takes care of the textfield contents
            if (itemObj != null) {
                var item = (SpaceComboItem)itemObj;
                if (item.isResolved() && item.getSpace().isPrivate()) {
                    m_icon.setIcon(Icons.getPrivateSpaceIcon());
                } else {
                    m_icon.setIcon(Icons.getPublicSpaceIcon());
                }
                editor.setToolTipText(item.getToolTip());
            } else {
                m_icon.setIcon(Icons.getPublicSpaceIcon());
                editor.setToolTipText(null);
            }
        }
    }

    static class SpaceComboItem {
        private final String m_id;

        private final String m_name;

        private final Space m_space;

        SpaceComboItem(final Space space) {
            m_id = space.getSpaceId();
            m_name = space.getName();
            m_space = space;
        }

        SpaceComboItem(final String id, final String name) {
            m_id = (id == null) ? "" : id;
            m_name = (name == null) ? "" : name;
            m_space = null;
        }

        /**
         * @return the id
         */
        public String getId() {
            return m_id;
        }

        /**
         * @return the title
         */
        public String getName() {
            return m_name;
        }

        public Space getSpace() {
            return m_space;
        }

        /**
         * @return the isResolved
         */
        public boolean isResolved() {
            return m_space != null;
        }

        @Override
        public String toString() {
            return truncateName();
        }

        public String truncateName() {
            if (m_name.length() > 100) {
                return m_name.substring(0, 50).concat("...");
            } else {
                return m_name;
            }
        }

        public String getToolTip() {
            return String.format(
                "<html><b>ID:</b> %s<br><b>Name:</b> %s<br><b>Location:</b> %s<br><b>Owner:</b> %s</html>", //
                m_id, //
                StringUtils.isBlank(m_name) ? "?" : m_name, //
                m_space == null ? "?" : m_space.getPath(), //
                m_space == null ? "?" : m_space.getOwner());
        }

        public String getDescription() {
            return String.format(" Owner: %s", //
                m_space == null ? "?" : m_space.getOwner());
        }

        @Override
        public int hashCode() {
            return Objects.hash(m_id, m_name, m_space);
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
            SpaceComboItem other = (SpaceComboItem)obj;
            return Objects.equals(m_id, other.m_id) //
                && Objects.equals(m_name, other.m_name) //
                && Objects.equals(m_space, other.m_space);
        }
    }
}
