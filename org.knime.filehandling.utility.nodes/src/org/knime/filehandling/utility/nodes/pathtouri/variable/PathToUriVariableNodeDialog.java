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
 *   Jan 10, 2023 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.utility.nodes.pathtouri.variable;

import java.awt.Component;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.variable.FlowVariableFilterPanel;
import org.knime.core.node.util.filter.variable.VariableTypeFilter;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.utility.nodes.pathtouri.exporter.URIExporterComboBox;
import org.knime.filehandling.utility.nodes.pathtouri.exporter.URIExporterDialogHelper;
import org.knime.filehandling.utility.nodes.pathtouri.exporter.URIExporterPanelParent;

/**
 * Node dialog of the Path to URI variable node.
 *
 * @author Zkriya Rakhimberdiyev
 */
final class PathToUriVariableNodeDialog extends NodeDialogPane {

    private final PathToUriVariableNodeConfig m_config;

    private final FlowVariableFilterPanel m_filter;

    private final DialogComponentString m_variableSuffixComponent;

    private final DialogComponentBoolean m_failIfPathNotExists;

    private final URIExporterComboBox m_uriExporterCombo;

    private final ChangeListener m_filterListener = e -> {
        updateFilterConfigurationModel();
        refreshExporterPanels();
        displayCurrentExporterPanel();
    };

    private final ChangeListener m_exporterSelectionListener = e -> displayCurrentExporterPanel();

    private final URIExporterPanelParent m_exporterPanelParent;

    private JLabel m_exporterDescription;

    public PathToUriVariableNodeDialog(final PortsConfiguration portsConfig) {
        m_config = new PathToUriVariableNodeConfig(portsConfig,
            () -> getAvailableFlowVariables(FSLocationVariableType.INSTANCE));
        m_filter = new FlowVariableFilterPanel(new VariableTypeFilter(FSLocationVariableType.INSTANCE));
        m_variableSuffixComponent = new DialogComponentString(m_config.getVariableSuffixModel(),
            "Suffix added to the new variables:", true, 15);
        m_failIfPathNotExists =
            new DialogComponentBoolean(m_config.getFailIfPathNotExistsModel(), "Fail if file/folder does not exist");

        m_uriExporterCombo = new URIExporterComboBox(m_config.getURIExporterModel());
        m_exporterDescription = new JLabel("-");
        m_exporterPanelParent = new URIExporterPanelParent();

        addTab("Settings", createPanel());
    }

    private JPanel createPanel() {
        final var p = new JPanel(new GridBagLayout());
        final GBCBuilder gbc =
            new GBCBuilder().resetX().resetY().setWeightX(1).setWeightY(1).fillBoth().anchorLineStart();
        p.add(getVariableSelectionPanel(), gbc.build());

        gbc.incY().setWeightY(0);
        p.add(getOutputPanel(), gbc.build());

        gbc.resetX().incY().setWeightY(1).fillBoth();
        p.add(createUrlFormatPanel(), gbc.build());

        return p;
    }

    private Component getVariableSelectionPanel() {
        final var p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Variable selection "));

        final GBCBuilder gbc =
            new GBCBuilder().resetX().resetY().setWeightX(1).setWeightY(1).fillBoth().anchorLineStart();
        p.add(m_filter, gbc.build());
        return p;
    }

    private JPanel getOutputPanel() {
        final var p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output "));

        final GBCBuilder gbc =
            new GBCBuilder().resetX().resetY().setWeightX(0).setWeightY(0).fillNone().anchorFirstLineStart();
        gbc.insets(5, 5, 0, 0);
        p.add(m_variableSuffixComponent.getComponentPanel(), gbc.build());

        gbc.resetX().incY().setWidth(2).setWeightX(1);
        p.add(m_failIfPathNotExists.getComponentPanel(), gbc.build());

        return p;
    }

    private Component createUrlFormatPanel() {
        final var p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "URI Format"));

        final GBCBuilder gbc = new GBCBuilder().resetPos().anchorWest().fillNone().insets(5, 5, 0, 5);

        p.add(new JLabel("Format: "), gbc.build());

        gbc.incX();
        p.add(m_uriExporterCombo, gbc.build());

        gbc.incX().setWeightX(1).fillBoth();
        p.add(m_exporterDescription, gbc.build());

        gbc.resetX().incY().setWeightX(0).fillNone().insetTop(20);
        p.add(new JLabel("Settings: "), gbc.build());

        gbc.resetX().incY().weight(1, 1).fillBoth().setWidth(3).insetTop(5);
        p.add(m_exporterPanelParent, gbc.build());

        return p;
    }

    private void displayCurrentExporterPanel() {
        final URIExporterDialogHelper dialogHelper = m_config.getExporterDialogHelper();
        final var currExporter = m_config.getURIExporterID();

        final String exporterDescription;
        if (dialogHelper.isAvailable(currExporter)) {
            exporterDescription = dialogHelper.getExporterMetaInfo(currExporter).getDescription();
        } else {
            exporterDescription = "";
        }

        m_exporterDescription.setText(String.format("<html><i>%s</i></html>", exporterDescription));
        m_exporterPanelParent.showExporterPanel(currExporter);
    }

    private void updateFilterConfigurationModel() {
        m_filter.saveConfiguration(m_config.getFilterConfigurationModel());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_filter.saveConfiguration(m_config.getFilterConfigurationModel());
        if (m_config.getFilteredFlowVariables().isEmpty()) {
            throw new InvalidSettingsException("No flow variables selected.");
        }
        m_variableSuffixComponent.saveSettingsTo(settings);
        m_failIfPathNotExists.saveSettingsTo(settings);
        m_config.saveSettingsForDialog(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            m_config.loadSettingsForDialog(settings, specs, getAvailableFlowVariables(FSLocationVariableType.INSTANCE));
            m_filter.loadConfiguration(m_config.getFilterConfigurationModel(),
                getAvailableFlowVariables(FSLocationVariableType.INSTANCE));
            m_variableSuffixComponent.loadSettingsFrom(settings, specs);
            m_failIfPathNotExists.loadSettingsFrom(settings, specs);

            settingsLoaded();
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException("Could not load settings: " + e.getMessage(), e);
        }
    }

    private void settingsLoaded() {
        //select relevant radio buttons
        m_uriExporterCombo.setSelectedItem(m_config.getURIExporterID());
    }

    @Override
    public void onClose() {
        m_filter.removeChangeListener(m_filterListener);
        m_config.getURIExporterModel().removeChangeListener(m_exporterSelectionListener);
    }

    private void refreshExporterPanels() {
        final URIExporterDialogHelper dialogHelper = m_config.getExporterDialogHelper();
        dialogHelper.initExporterConfigsAndPanels();
        m_uriExporterCombo.updateAvailableExporterFactories(dialogHelper.getAvailableExporterFactories());
        m_exporterPanelParent.updateAvailableExporterPanels(dialogHelper.getAvailableExporterPanels(),
            "Chosen URI format is not applicable for given file system connection or flow variables.");
    }

    @Override
    public void onOpen() {
        refreshExporterPanels();
        displayCurrentExporterPanel();
        m_filter.addChangeListener(m_filterListener);
        m_config.getURIExporterModel().addChangeListener(m_exporterSelectionListener);
    }
}
