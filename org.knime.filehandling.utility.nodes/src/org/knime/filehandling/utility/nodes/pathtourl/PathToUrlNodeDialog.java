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
 *   Dec 22, 2020 (Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.pathtourl;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeListener;

import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
public class PathToUrlNodeDialog extends NodeDialogPane {

    private final PathToUrlNodeConfig m_config;

    private final DialogComponentColumnNameSelection m_pathColumnName;

    private final DialogComponentString m_appendColumnName;

    private final DialogComponentColumnNameSelection m_replaceColumnName;

    private final URIExporterSelectorPanel m_uriExporterSelectorPanel;

    private final ChangeListener m_pathColumnListener = (e) -> {
            refreshExporterPanels();
            displayCurrentExporterPanel();
    };

    private final ChangeListener m_exporterSelectionListener = (e) -> {
        displayCurrentExporterPanel();
    };

    private final JPanel m_exporterPanelParent;

    private JLabel m_exporterDescription;

    private final JRadioButton m_appendColumnRadio;

    private final JRadioButton m_replaceColumnRadio;

    private static final String SELECTED_COLUMN_LABEL = "Path Column: ";

    /**
     * Initialize component and attach appropriate event listeners
     *
     * @param portsConfig PortsConfiguration object
     * @param nodeConfig {@link PathToUrlNodeConfig} object
     */
    @SuppressWarnings("unchecked")
    public PathToUrlNodeDialog(final PortsConfiguration portsConfig, final PathToUrlNodeConfig nodeConfig) {
        m_config = nodeConfig;

        m_uriExporterSelectorPanel = new URIExporterSelectorPanel(m_config.getURIExporterModel());
        m_pathColumnName = new DialogComponentColumnNameSelection(nodeConfig.getPathColumnNameModel(),
            SELECTED_COLUMN_LABEL, m_config.getDataTablePortIndex(), FSLocationValue.class);

        m_appendColumnName = new DialogComponentString(nodeConfig.getAppendColumnNameModel(), "", true, 25);
        m_replaceColumnName = new DialogComponentColumnNameSelection(nodeConfig.getReplaceColumnNameModel(), "",
            m_config.getDataTablePortIndex(), false, StringValue.class);
        m_appendColumnRadio = new JRadioButton("Append Column: ");
        m_replaceColumnRadio = new JRadioButton("Replace Column: ");
        final ButtonGroup buttonGrp = new ButtonGroup();
        buttonGrp.add(m_appendColumnRadio);
        buttonGrp.add(m_replaceColumnRadio);

        m_exporterDescription = new JLabel("");
        m_exporterPanelParent = new JPanel(new CardLayout());
        m_exporterPanelParent.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "URL Format Settings"));

        addTab("Settings", createSettingsDialog());
        wireUIEvents();
    }

    private void wireUIEvents() {
        m_appendColumnRadio.addActionListener(l -> toggleColumnAppendAndReplace());
        m_replaceColumnRadio.addActionListener(l -> toggleColumnAppendAndReplace());
    }


    private void toggleColumnAppendAndReplace() {
        m_config.getAppendColumnModel().setBooleanValue(m_appendColumnRadio.isSelected());
    }

    /**
     * Update the panel for displaying URI Exporter specific settings, also updates the description.
     */
    private void refreshExporterPanels() {
        final URIExporterDialogHelper dialogHelper = m_config.getExporterDialogHelper();
        final Map<URIExporterID, URIExporterFactory> availableFactories = dialogHelper.getAvailableExporterFactories();

        m_uriExporterSelectorPanel.updateAvailableExporterFactories(availableFactories);

        m_exporterPanelParent.removeAll();
        for (URIExporterID exporterID : availableFactories.keySet()) {
            final String cardName = "exporter-" + exporterID.toString();
            m_exporterPanelParent.add(dialogHelper.getExporterPanel(exporterID), cardName);
        }

        m_exporterPanelParent.add(new InvalidURIExporterPanel(), "invalid");
    }

    private void displayCurrentExporterPanel() {
        final URIExporterDialogHelper dialogHelper = m_config.getExporterDialogHelper();
        final URIExporterID currExporter = m_config.getURIExporterID();

        final String exporterPanelCardName;
        final String exporterDescription;

        if (dialogHelper.isAvailable(currExporter)) {
            exporterPanelCardName = "exporter-" + m_config.getURIExporterID().toString();
            exporterDescription = dialogHelper.getExporterMetaInfo(currExporter).getDescription();
        } else {
            exporterPanelCardName = "invalid";
            exporterDescription = "";
        }


        final CardLayout cardLayout = (CardLayout)m_exporterPanelParent.getLayout();
        cardLayout.show(m_exporterPanelParent, exporterPanelCardName);

        m_exporterDescription.setText(exporterDescription);
    }

    private Component createSettingsDialog() {
        final JPanel p = new JPanel(new GridBagLayout());

        final GBCBuilder gbc =
            new GBCBuilder().resetPos().setWeightX(1).setWeightY(0).anchorFirstLineStart().fillNone();

        p.add(m_pathColumnName.getComponentPanel(), gbc.build());

        gbc.setWeightY(1).incY();

        p.add(createURIExporterSelectorSettings(), gbc.build());

        gbc.incY().fillHorizontal();

        p.add(createOutputSettings(), gbc.build());

        gbc.incY().fillBoth();

        p.add(m_exporterPanelParent, gbc.build());

        p.add(Box.createGlue());

        return p;
    }

    private Component createOutputSettings() {

        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output"));

        final GBCBuilder gbc = new GBCBuilder().resetPos().setWeightX(0).setWeightY(0).anchorLineStart().fillNone();

        gbc.insetLeft(0).insets(0, 0, 0, -30);
        p.add(m_appendColumnRadio, gbc.build());

        gbc.setWeightX(1).incX().insets(0, -30, 0, 0);
        p.add(m_appendColumnName.getComponentPanel(), gbc.build());

        gbc.setWeightY(1).incY().resetX().insets(0, 0, 0, -30);
        p.add(m_replaceColumnRadio, gbc.build());

        gbc.incX().insets(0, -30, 0, 0);
        p.add(m_replaceColumnName.getComponentPanel(), gbc.build());

        p.add(Box.createGlue());

        return p;
    }

    private Component createURIExporterSelectorSettings() {

        final JPanel p = new JPanel(new GridBagLayout());

        final GBCBuilder gbc =
            new GBCBuilder().resetPos().setWeightX(0).setWeightY(1).anchorLineStart().fillHorizontal();

        //the label is embedded within this JPanel component
        p.add(m_uriExporterSelectorPanel, gbc.build());

        gbc.setWeightX(0).resetX().setWeightY(1).incY().fillNone();
        p.add(m_exporterDescription, gbc.build());

        p.add(Box.createGlue(), gbc.build());

        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_config.saveSettingsForDialog(settings);

        m_pathColumnName.saveSettingsTo(settings);
        m_appendColumnName.saveSettingsTo(settings);
        m_replaceColumnName.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {

        try {
            m_pathColumnName.loadSettingsFrom(settings, specs);
            m_replaceColumnName.loadSettingsFrom(settings, specs);
            m_appendColumnName.loadSettingsFrom(settings, specs);
            m_config.loadSettingsForDialog(settings, specs);

            settingsLoaded();
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException("Could not load settings: " + e.getMessage(), e);
        }
    }

    private void settingsLoaded() {
        //select relevant radio buttons
        m_appendColumnRadio.setSelected(m_config.shouldAppendColumn());
        m_replaceColumnRadio.setSelected(!m_config.shouldAppendColumn());
        m_uriExporterSelectorPanel.setSelectedItem(m_config.getURIExporterID());
    }

    @Override
    public void onClose() {
        m_config.getPathColumnNameModel().removeChangeListener(m_pathColumnListener);
        m_config.getURIExporterModel().removeChangeListener(m_exporterSelectionListener);
    }

    @Override
    public void onOpen() {
        refreshExporterPanels();
        displayCurrentExporterPanel();
        m_config.getPathColumnNameModel().addChangeListener(m_pathColumnListener);
        m_config.getURIExporterModel().addChangeListener(m_exporterSelectionListener);
    }
}
