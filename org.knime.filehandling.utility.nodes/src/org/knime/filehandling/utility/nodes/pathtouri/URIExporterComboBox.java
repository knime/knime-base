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
 *   Jan 21, 2021 (Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.pathtouri;

import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * A custom implementation of JCombobox wrapped in JPanel. This component implements the functionality for displaying a
 * dropdown for available URI exporters.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
final class URIExporterComboBox extends JPanel {

    private static final long serialVersionUID = 1L;

    private final DefaultComboBoxModel<URIExporterComboboxItem> m_comboModel;

    private final JComboBox<URIExporterComboboxItem> m_combobox;

    private final URIExporterChangeListener m_uriChangeListener; //NOSONAR

    private SettingsModelString m_selectedUriExporterModel; //NOSONAR

    private static class URIExporterComboboxItem implements Comparable<URIExporterComboboxItem> {

        private URIExporterID m_id;

        private String m_title;

        URIExporterComboboxItem(final URIExporterID id, final String title) {
            m_id = id;
            m_title = title;
        }

        URIExporterID getId() {
            return m_id;
        }

        @Override
        public String toString() {
            return m_title;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj != null && obj.getClass() == getClass()) {
                URIExporterComboboxItem other = (URIExporterComboboxItem)obj;
                return m_id.equals(other.m_id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return m_id.hashCode();
        }

        @Override
        public int compareTo(final URIExporterComboboxItem o) {
            if (m_id.equals(URIExporterIDs.DEFAULT)) {
                return -1;
            } else {
                return toString().compareTo(o.toString());
            }
        }
    }

    /**
     * Initialize components
     *
     * @param uriExporterModel {@link SettingsModel} that stored the {@link URIExporterID}.
     */
    public URIExporterComboBox(final SettingsModelString uriExporterModel) {
        m_selectedUriExporterModel = uriExporterModel;
        m_comboModel = new DefaultComboBoxModel<>();
        m_combobox = new JComboBox<>(m_comboModel);
        m_combobox.setFocusable(false);
        m_uriChangeListener = new URIExporterChangeListener();
        initComponents();
    }

    /**
     * Add event listener to the combo box and add components to the parent layout
     */
    private void initComponents() {
        m_combobox.addItemListener(m_uriChangeListener);
        setLayout(new GridBagLayout());
        final GBCBuilder gbc =
            new GBCBuilder().resetPos().setWeightX(1).setWeightY(1).anchorFirstLineStart().fillBoth();
        add(m_combobox, gbc.build());
    }

    private static URIExporterComboboxItem toComboBoxItem(final URIExporterID id, final URIExporterFactory factory) {
        if (id.equals(URIExporterIDs.DEFAULT)) {
            return new URIExporterComboboxItem(id, "Default");
        } else {
            return new URIExporterComboboxItem(id, factory.getMetaInfo().getLabel());
        }
    }

    void updateAvailableExporterFactories(final Map<URIExporterID, URIExporterFactory> exporterFactories) {

        final List<URIExporterComboboxItem> items = createNewItems(exporterFactories);

        m_combobox.removeItemListener(m_uriChangeListener);
        m_comboModel.removeAllElements();
        for (URIExporterComboboxItem item : items) {
            m_comboModel.addElement(item);
            if (m_selectedUriExporterModel.getStringValue().equals(item.getId().toString())) {
                m_comboModel.setSelectedItem(item);
            }
        }
        m_combobox.addItemListener(m_uriChangeListener);
    }

    private static List<URIExporterComboboxItem> createNewItems(final Map<URIExporterID, URIExporterFactory> exporterFactories) {

        return exporterFactories.entrySet().stream()
            // don't add DEFAULT_HADOOP because it is an internal ID and of little use to the user.
            .filter(entry -> !entry.getKey().equals(URIExporterIDs.DEFAULT_HADOOP))
            .map(entry -> toComboBoxItem(entry.getKey(), entry.getValue()))
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * Custom implementation of ItemListener, which updates the settings model of the selected URI Exporter
     */
    private final class URIExporterChangeListener implements ItemListener {
        @Override
        public void itemStateChanged(final ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                final URIExporterID selectedURIExportedId = ((URIExporterComboboxItem)event.getItem()).getId();
                m_selectedUriExporterModel.setStringValue(selectedURIExportedId.toString());
            }
        }
    }

    void setSelectedItem(final URIExporterID uriExporterID) {
        final URIExporterComboboxItem tmpItem = new URIExporterComboboxItem(uriExporterID, null);
        int index = m_comboModel.getIndexOf(tmpItem);
        if (index != -1) {
            m_combobox.setSelectedIndex(index);
        } else {
            m_combobox.setSelectedItem(0);
        }
    }
}
