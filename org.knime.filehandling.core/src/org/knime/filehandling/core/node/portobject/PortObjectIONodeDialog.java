/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.filehandling.core.node.portobject;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.filehandling.core.defaultnodesettings.DialogComponentFileChooser2;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice;
import org.knime.filehandling.core.defaultnodesettings.FilesHistoryPanel;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;
import org.knime.filehandling.core.node.portobject.reader.PortObjectReaderNodeDialog;
import org.knime.filehandling.core.node.portobject.writer.PortObjectWriterNodeDialog;

/**
 * Node dialog for port object reader and writer nodes that can be extended with additional settings components.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @param <C> the node config of the node
 * @noextend extend either {@link PortObjectReaderNodeDialog} or {@link PortObjectWriterNodeDialog}
 */
@SuppressWarnings("deprecation")
public abstract class PortObjectIONodeDialog<C extends PortObjectIONodeConfig> extends NodeDialogPane {

    /** The config. */
    private final C m_config;

    private final List<JPanel> m_additionalPanels = new ArrayList<>();

    private final DialogComponentFileChooser2 m_filePanel;

    private final DialogComponentNumber m_timeoutSpinner;

    /**
     * Constructor.
     *
     * @param portsConfig the ports configuration
     * @param config the config
     * @param fileChooserHistoryId id used to store file history used by {@link FilesHistoryPanel}
     * @param fileChooserDialogType integer defining the file chooser dialog type (see {@link JFileChooser#OPEN_DIALOG},
     *            {@link JFileChooser#SAVE_DIALOG})
     * @param fileChooserSelectionMode integer defining the file chooser dialog type (see
     *            {@link JFileChooser#FILES_ONLY}, {@link JFileChooser#FILES_AND_DIRECTORIES} and
     *            {@link JFileChooser#DIRECTORIES_ONLY}
     */
    protected PortObjectIONodeDialog(final PortsConfiguration portsConfig, final C config,
        final String fileChooserHistoryId, final int fileChooserDialogType, final int fileChooserSelectionMode) {
        m_config = config;
        final SettingsModelFileChooser2 fileChooserModel = m_config.getFileChooserModel();
        final FlowVariableModel fvm = createFlowVariableModel(
            new String[]{fileChooserModel.getConfigName(), SettingsModelFileChooser2.PATH_OR_URL_KEY}, Type.STRING);

        // TODO: has to be changed to -1 once we fixed AP-13672
        final int fsPortIdx = Optional
            .ofNullable(portsConfig.getInputPortLocation().get(PortObjectIONodeModel.CONNECTION_INPUT_PORT_GRP_NAME)) //
            .map(arr -> arr[0]).orElse(0); // correctness ensured by framework

        m_filePanel = new DialogComponentFileChooser2(fsPortIdx, fileChooserModel,
            fileChooserHistoryId, fileChooserDialogType, fileChooserSelectionMode, fvm);
        final String[] defaultSuffixes = fileChooserModel.getDefaultSuffixes();
        final boolean isReaerdDialog = fileChooserDialogType == JFileChooser.OPEN_DIALOG;
        if (!isReaerdDialog && (defaultSuffixes != null && defaultSuffixes.length > 0)) {
            m_filePanel.setForceExtensionOnSave(defaultSuffixes[0]);
        }

        SettingsModelIntegerBounded timeoutModel = m_config.getTimeoutModel();
        m_timeoutSpinner = new DialogComponentNumber(timeoutModel, "Timeout (ms)", 500, 6);
        m_timeoutSpinner.setToolTipText("Timeout to connect to the server in milliseconds");
        timeoutModel.addChangeListener(l -> m_filePanel.setTimeout(timeoutModel.getIntValue()));
        fileChooserModel.addChangeListener(l -> updateTimeoutEnabledness());
        m_additionalPanels.add(createInputLocationPanel(isReaerdDialog ? "Input location" : "Output location"));
        m_additionalPanels.add(createConnectionPanel());
    }

    /**
     * Adds a panel that to the list of panels that will be added to the dialog. The order of the list defines the order
     * of the panels, i.e., the first added panel will appear the topmost.
     *
     * @param panel the panel to add
     */
    protected final void addAdditionalPanel(final JPanel panel) {
        m_additionalPanels.add(panel);
    }

    /**
     * Adds a panel that to the list of panels that will be added to the dialog at the specified position. Shifts the
     * panel currently at that position (if any) and any subsequent elements to the right (adds one to their
     * indices).The order of the list defines the order of the panels, i.e., the first added panel will appear the
     * topmost.
     *
     * @param index index at which the specified element is to be inserted
     * @param panel the panel to add
     */
    protected final void addAdditionalPanel(final int index, final JPanel panel) {
        m_additionalPanels.add(index, panel);
    }

    private void updateTimeoutEnabledness() {
        // Enable/Disable timeout spinner
        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)m_filePanel.getModel();
        final FileSystemChoice choice = model.getFileSystemChoice();
        m_timeoutSpinner.getModel().setEnabled(FileSystemChoice.getCustomFsUrlChoice().equals(choice)
            || FileSystemChoice.getKnimeFsChoice().equals(choice));
    }

    final void finalizeOptionsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        while (m_additionalPanels.size() > 1) {
            gbc.gridy++;
            panel.add(m_additionalPanels.remove(0), gbc);
        }
        gbc.gridy++;
        gbc.weighty = 1;
        final JPanel additionalSettingsPanel =
            m_additionalPanels.isEmpty() ? new JPanel() : m_additionalPanels.remove(0);
        panel.add(additionalSettingsPanel, gbc);
        addTab("Options", panel);
    }

    private JPanel createInputLocationPanel(final String borderTitle) {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), borderTitle));
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.weighty = 1;
        panel.add(m_filePanel.getComponentPanel(), gbc);
        return panel;
    }

    private JPanel createConnectionPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Connection"));
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.weighty = 1;
        panel.add(m_timeoutSpinner.getComponentPanel(), gbc);
        return panel;
    }

    /**
     * Helper method to create and initialize {@link GridBagConstraints}.
     *
     * @return initialized {@link GridBagConstraints}
     */
    protected static final GridBagConstraints createAndInitGBC() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        return gbc;
    }

    /**
     * Method to obtain the PortObjectIONodeConfig of this node dialog.
     *
     * @return the config of this node dialog
     */
    protected C getConfig() {
        return m_config;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        // save dialog settings
        m_config.saveConfigurationForDialog(settings);

        // save file panel and timeout settings
        m_filePanel.saveSettingsTo(settings);
        m_timeoutSpinner.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        // load dialog settings
        m_config.loadConfigurationForDialog(settings, specs);

        // load file panel and timeout settings
        m_filePanel.loadSettingsFrom(settings, specs);
        m_timeoutSpinner.loadSettingsFrom(settings, specs);

        // update the spinner
        updateTimeoutEnabledness();
    }

}
