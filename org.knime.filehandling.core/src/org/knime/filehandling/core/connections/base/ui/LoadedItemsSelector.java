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
 *   Apr 21, 2022 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.core.connections.base.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.SwingWorkerWithContext;

/**
 * Editor component for selecting items consist of 2 parts - id and title (like
 * subsites and user groups). Provides an ability to fetch possible options by
 * user's request.
 *
 * @author Alexander Bondaletov
 */
@SuppressWarnings("java:S1948") // ignore Sonar's transient/serializable warnings
public abstract class LoadedItemsSelector extends JPanel {
    private static final long serialVersionUID = 1L;

    private final SettingsModelString m_idModel;

    private final SettingsModelString m_titleModel;

    private final SettingsModelBoolean m_checkedModel;

    private final DefaultComboBoxModel<IdComboboxItem> m_comboModel;

    private final JComboBox<IdComboboxItem> m_combobox;

    private final JButton m_fetchBtn;

    private final JButton m_cancelBtn;

    private final JLabel m_warningLabel;

    private LoadedItemSelectorSwingWorker m_fetchWorker;

    private boolean m_enabled = true;

    private boolean m_ignoreListeners = false;

    private final boolean m_isEditable;

    // Pattern for editable LoadedItemSelectors for which we save the settings in
    // the form of <displayName> (<internalName>) like the Sharepoint list
    private static final Pattern INTERNAL_NAME_PATTERN = Pattern.compile(".*\\(([^)]+)\\)");

    // Matches everything until the last "(" to get the display name
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("(.*(?=\\())");

    /**
     * @param idModel
     *            Settings model holding id value.
     * @param titleModel
     *            Settings model holding title value.
     * @param caption
     *            The caption label
     * @param checkedModel
     *            Optional settings model holding "checked" value.
     *
     */
    protected LoadedItemsSelector(final SettingsModelString idModel, final SettingsModelString titleModel,
            final String caption, final SettingsModelBoolean checkedModel) {
        this(idModel, titleModel, caption, checkedModel, false);
    }

    /**
     * @param idModel
     *            Settings model holding id value.
     * @param titleModel
     *            Settings model holding title value.
     * @param caption
     *            The caption label
     * @param checkedModel
     *            Optional settings model holding "checked" value.
     * @param isEditable
     *            flag whether the dropdown is editable or not
     *
     */
    protected LoadedItemsSelector(final SettingsModelString idModel, final SettingsModelString titleModel,
            final String caption, final SettingsModelBoolean checkedModel, final boolean isEditable) {
        m_idModel = idModel;
        m_titleModel = titleModel;
        m_checkedModel = checkedModel;
        m_isEditable = isEditable;

        m_comboModel = new DefaultComboBoxModel<>(new IdComboboxItem[] {});
        m_combobox = new JComboBox<>(m_comboModel);
        m_combobox.addActionListener(e -> onSelectionChanged());
        m_combobox.setRenderer(new IdComboboxCellRenderer());
        m_combobox.setEditable(m_isEditable);

        m_fetchBtn = new JButton("Refresh");
        m_fetchBtn.addActionListener(e -> onFetch());

        m_cancelBtn = new JButton("Cancel");
        m_cancelBtn.addActionListener(e -> {
            if (m_fetchWorker != null) {
                m_fetchWorker.cancel(true);
            }
        });
        m_cancelBtn.setVisible(false);
        m_cancelBtn.setPreferredSize(m_fetchBtn.getPreferredSize());

        m_warningLabel = new JLabel(
                "<html><font color='orange'>Only technical IDs could be displayed due to limited permissions being granted.</html>");
        m_warningLabel.setVisible(false);

        final Component labelOrCheckbox;
        if (m_checkedModel != null) {
            final var checkedInput = new JCheckBox(caption);
            checkedInput.addActionListener(e -> {
                boolean checked = checkedInput.isSelected();
                m_checkedModel.setBooleanValue(checked);

                if (checked) {
                    onFetch();
                }
            });
            m_checkedModel.addChangeListener(e -> {
                final var checked = m_checkedModel.getBooleanValue();
                checkedInput.setSelected(checked);
                setEnabled(checked);
            });

            checkedInput.setSelected(m_checkedModel.getBooleanValue());
            setEnabled(m_checkedModel.getBooleanValue());
            labelOrCheckbox = checkedInput;
        } else {
            labelOrCheckbox = createLabelBox(caption);
        }

        final var c = new GridBagConstraints();
        setLayout(new GridBagLayout());
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 5);

        add(labelOrCheckbox, c);
        c.gridx += 1;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        add(m_combobox, c);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        c.gridx += 1;
        c.weightx = 0;
        add(m_fetchBtn, c);

        c.gridx += 1;
        add(m_cancelBtn, c);

        c.gridx = 1;
        c.gridy += 1;
        c.gridwidth = 4;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(5, 0, 10, 0);
        add(m_warningLabel, c);
    }

    private static Box createLabelBox(final String caption) {
        final var paddedBox = new Box(BoxLayout.X_AXIS);
        paddedBox.add(Box.createHorizontalStrut(5));
        paddedBox.add(new JLabel(caption));
        return paddedBox;
    }

    private void onSelectionChanged() {
        if (m_ignoreListeners) {
            return;
        }

        var id = "";
        var title = "";

        final var selectedItem = m_comboModel.getSelectedItem();

        if (selectedItem instanceof IdComboboxItem) {
            final var selected = (IdComboboxItem) selectedItem;
            id = selected.getId();
            title = selected.getTitle();
        } else if (m_comboModel.getSelectedItem() != null) {
            title = selectedItem.toString();
        }

        m_idModel.setStringValue(id);
        m_titleModel.setStringValue(title);
    }

    private class LoadedItemSelectorSwingWorker extends SwingWorkerWithContext<List<IdComboboxItem>, Void> {

        @Override
        protected List<IdComboboxItem> doInBackgroundWithContext() throws Exception {
            return fetchItems();
        }

        @Override
        protected void doneWithContext() {
            m_combobox.setEnabled(m_enabled);
            m_cancelBtn.setVisible(false);
            m_fetchBtn.setVisible(true);

            if (isCancelled()) {
                return;
            }

            try {
                onItemsLoaded(get());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) {
                if (m_checkedModel != null) {
                    m_checkedModel.setBooleanValue(false);
                }
                showError(ex);
            }
        }

        private void showError(final Exception ex) {
            final var message = fetchExceptionMessage(ex);

            JOptionPane.showMessageDialog(getRootPane(), message, "Error", JOptionPane.ERROR_MESSAGE);
        }

        protected void onItemsLoaded(final List<IdComboboxItem> comboBoxItems) {
            m_ignoreListeners = true;
            final var selectedItem = m_comboModel.getSelectedItem();

            m_comboModel.removeAllElements();
            m_comboModel.addAll(comboBoxItems);

            m_comboModel.setSelectedItem(selectedItem);

            updateComboFromSettings(true);
            m_ignoreListeners = false;

            onSelectionChanged();
            // In case we only have 1 element we will choose this
            if (comboBoxItems.size() == 1) {
                m_combobox.setSelectedIndex(0);
            }

            m_warningLabel.setVisible(
                    !comboBoxItems.isEmpty() && comboBoxItems.get(0).getId().equals(comboBoxItems.get(0).getTitle()));
        }
    }

    /**
     * Subclass of the {@link LoadedItemSelectorSwingWorker} to separate the logic
     * of the onItemsLoaded method in case the dropdown is editable.
     *
     */
    private final class EditableLoadedItemSelectorSwingWorker extends LoadedItemSelectorSwingWorker {

        @Override
        protected void onItemsLoaded(final List<IdComboboxItem> comboBoxItems) {
            m_ignoreListeners = true;
            final var selectedItem = (IdComboboxItem) m_comboModel.getSelectedItem();

            m_comboModel.removeAllElements();
            m_comboModel.addAll(comboBoxItems);

            if (selectedItem != null) {
                if (!selectedItem.getId().isEmpty()) {
                    m_comboModel.setSelectedItem(selectedItem);
                } else {
                    if (!containsItemWithSameDisplayName(comboBoxItems, selectedItem)) {
                        m_comboModel.addElement(selectedItem);
                        m_comboModel.setSelectedItem(selectedItem);
                    }
                }
            }

            updateComboFromSettings(true);
            m_ignoreListeners = false;

            onSelectionChanged();
            // In case we only have 1 element we will choose this
            if (comboBoxItems.size() == 1) {
                m_combobox.setSelectedIndex(0);
            }

            m_warningLabel.setVisible(
                    !comboBoxItems.isEmpty() && comboBoxItems.get(0).getId().equals(comboBoxItems.get(0).getTitle()));
        }

        private boolean containsItemWithSameDisplayName(final List<IdComboboxItem> comboBoxItems, final IdComboboxItem item) {
            for (final var i : comboBoxItems) {
                if (getDisplayName(i.getTitle()).equals(item.getTitle())) {
                    m_comboModel.setSelectedItem(i);
                    return true;
                }
            }
            return false;
        }

        private String getDisplayName(final String title) {
            if (INTERNAL_NAME_PATTERN.matcher(title).matches()) {
                final var m = DISPLAY_NAME_PATTERN.matcher(title);
                if (m.find()) {
                    final var displayName = m.group(1);
                    return displayName.substring(0, displayName.length() - 1);
                }
            }
            return "";
        }
    }

    private void onFetch() {
        m_fetchWorker = m_isEditable ? new EditableLoadedItemSelectorSwingWorker()
                : new LoadedItemSelectorSwingWorker();

        m_combobox.setEnabled(false);
        m_cancelBtn.setVisible(true);
        m_fetchBtn.setVisible(false);

        m_fetchWorker.execute();
    }

    /**
     * Fetches available options.
     *
     * @return The list of available options.
     * @throws Exception
     */
    public abstract List<IdComboboxItem> fetchItems() throws Exception;

    /**
     * Fetches error message from exception.
     *
     * @param ex
     *            the thrown {@link Exception}
     *
     * @return error message.
     */
    public abstract String fetchExceptionMessage(Exception ex);

    /**
     * @return the comboModel
     */
    public DefaultComboBoxModel<IdComboboxItem> getComboModel() {
        return m_comboModel;
    }

    /**
     * Should be called by the parent dialog after settings are loaded.
     */
    public void onSettingsLoaded() {
        updateComboFromSettings(false);
    }

    private void updateComboFromSettings(final boolean afterFetch) {
        final var title = m_titleModel.getStringValue();
        if (title.isEmpty()) {
            m_comboModel.setSelectedItem(null);
            return;
        }

        IdComboboxItem item;
        final var id = m_idModel.getStringValue();
        if (id.isEmpty() && m_comboModel.getSelectedItem() instanceof IdComboboxItem) {
            item = (IdComboboxItem) m_comboModel.getSelectedItem();
        } else {
            item = new IdComboboxItem(m_idModel.getStringValue(), m_titleModel.getStringValue());
        }

        if (m_comboModel.getIndexOf(item) < 0) {
            if (!afterFetch) {
                m_comboModel.addElement(item);
            } else {
                item = null;
            }
        }
        m_comboModel.setSelectedItem(item);
    }

    @Override
    public final void setEnabled(final boolean enabled) {
        m_enabled = enabled;
        if (!enabled) {
            m_combobox.setSelectedIndex(-1);
        }
        m_combobox.setEnabled(enabled);
        m_fetchBtn.setEnabled(enabled);
    }

    /**
     * Starts items fetching if no fetching was performed yet.
     */
    public void fetchOnce() {
        if (m_fetchWorker == null) {
            onFetch();
        }
    }

    /**
     * Triggers the fetching manually.
     */
    public void fetch() {
        onFetch();
    }

    public static final class IdComboboxItem {

        private String m_id;
        private String m_title;

        /**
         * @param id
         *            The site id.
         * @param title
         *            The site display name.
         *
         */
        public IdComboboxItem(final String id, final String title) {
            m_id = id;
            m_title = title;
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
        public String getTitle() {
            return m_title;
        }

        @Override
        public String toString() {
            if (m_title != null) {
                return m_title;
            }
            return m_id;
        }

        /**
         * Truncates the title in case it is too long.
         *
         * @return truncated title
         */
        public String truncateTitle() {
            if (m_title.length() > 50) {
                return m_title.substring(0, 50).concat("...");
            } else {
                return m_title;
            }
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof IdComboboxItem) {
                IdComboboxItem other = (IdComboboxItem) obj;
                return m_id.equals(other.m_id) && m_title.equals(other.m_title);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return m_id.hashCode();
        }
    }

    private class IdComboboxCellRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                final boolean isSelected, final boolean cellHasFocus) {
            if (m_fetchWorker != null && !m_fetchWorker.isDone()) {
                return new JLabel("Loading...");
            }

            String val = null;
            var toolTip = "";
            if (value instanceof IdComboboxItem) {
                var item = (IdComboboxItem)value;
                val = item.truncateTitle();
                toolTip = item.getTitle();
            }

            final var label = (JLabel) super.getListCellRendererComponent(list, val, index, isSelected, cellHasFocus);
            label.setToolTipText(toolTip);

            return label;
        }

    }
}
