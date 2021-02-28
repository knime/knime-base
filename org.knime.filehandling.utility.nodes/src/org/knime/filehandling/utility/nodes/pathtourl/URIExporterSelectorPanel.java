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
package org.knime.filehandling.utility.nodes.pathtourl;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * A custom implementation of JCombobox wrapped in JPanel. This component implements the functionality for displaying a
 * dropdown for available URI exporters
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
public class URIExporterSelectorPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private Map<URIExporterID, URIExporter> m_uriExportersMap; //NOSONAR

    private final DefaultComboBoxModel<String> m_comboModel;

    private final JComboBox<String> m_combobox;

    private final JLabel m_uriSelectorLabel;

    private static final String URL_FORMAT_LABEL = "URL Format";

    private SettingsModelString m_selectedUriExporterModel; //NOSONAR

    private final URIExporterChangeListener m_uriChangeListener; //NOSONAR

    /**
     * Initialize components
     */
    public URIExporterSelectorPanel() {

        m_uriExportersMap =
            Collections.singletonMap(URIExporterIDs.DEFAULT, new PathToUrlNodeConfig.DefaultURIExporter());

        m_comboModel = new DefaultComboBoxModel<>(
            m_uriExportersMap.keySet().stream().map(URIExporterID::toString).toArray(String[]::new));
        m_combobox = new JComboBox<>(m_comboModel);
        m_combobox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings("rawtypes")
            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                final boolean isSelected, final boolean cellHasFocus) {

                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }

                final URIExporterID selectedURIExportedId = new URIExporterID((String)value);
                setText(m_uriExportersMap.get(selectedURIExportedId).getLabel());
                return this;
            }
        });

        m_uriSelectorLabel = new JLabel(URL_FORMAT_LABEL);
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
            new GBCBuilder().resetPos().setWeightX(0).setWeightY(0).anchorFirstLineStart().fillNone().anchorCenter();

        gbc.insetLeft(5).insetTop(5);

        add(m_uriSelectorLabel, gbc.build());

        gbc.setWeightX(1).incX().fillHorizontal();

        add(m_combobox, gbc.build());

    }

    /**
     * Removes the previous elements from combo box and add items from the incoming uriExporters map. Pre-selects item
     * incase the settings are non-empty and updated the description label with the selected URI exporter description.
     *
     *
     * @param uriExporters Map of URI Exporters
     * @param selectedURIModel SettingsModel for selected URI Exporter
     * @param selectedUriExporterDescComponent DialogComponentLabel displaying the description for selected URI Exporter
     *
     */
    public void updateComponent(final Map<URIExporterID, URIExporter> uriExporters,
        final SettingsModelString selectedURIModel, final DialogComponentLabel selectedUriExporterDescComponent) {
        m_combobox.removeItemListener(m_uriChangeListener);
        m_comboModel.removeAllElements();
        m_selectedUriExporterModel = selectedURIModel;
        m_uriExportersMap = uriExporters;
        m_uriExportersMap.keySet().forEach(uriExportedId -> m_comboModel.addElement(uriExportedId.toString()));
        if (!StringUtils.isEmpty(m_selectedUriExporterModel.getStringValue())) {
            m_comboModel.setSelectedItem(m_selectedUriExporterModel.getStringValue());
        }
        m_combobox.addItemListener(m_uriChangeListener);

        String uriExportDesc =
            uriExporters.get(new URIExporterID((String)m_comboModel.getSelectedItem())).getDescription();
        selectedUriExporterDescComponent.setText(uriExportDesc);

    }

    /**
     * Returns the selected URI Exporter object
     *
     * @return URIExporter An instance of {@link URIExporter} object
     */
    public URIExporter getSelectedURIExporter() {
        final URIExporterID selectedURIExportedId = new URIExporterID((String)m_comboModel.getSelectedItem());
        return m_uriExportersMap.get(selectedURIExportedId);
    }

    /**
     * Custom implementation of ItemListener, which updated the Settings model of selected URI Exporter
     */
    private final class URIExporterChangeListener implements ItemListener {
        @Override
        public void itemStateChanged(final ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                final URIExporterID selectedURIExportedId = new URIExporterID((String)event.getItem());
                m_selectedUriExporterModel.setStringValue(selectedURIExportedId.toString());
            }
        }
    }

}
