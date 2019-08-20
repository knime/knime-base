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
 *   Aug 15, 2019 (bjoern): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.node.FSConnectionFlowVariableProvider;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.filehandling.core.filefilter.FileFilterDialog;

/**
 *
 * @author bjoern
 */
public class DialogComponentFileChooserGen3 extends DialogComponent {

    private FSConnectionFlowVariableProvider m_connectionFlowVariableProvider;

    private final SettingsModelFileChooserGen2 m_settingsModel;

    private final NodeDialogPane m_dialogPane;

    private final FlowVariableModel m_pathFlowVariableModel;

    private final String[] m_defaultSuffixes;

    private final JCheckBox m_useConnection;

    private final JComboBox<String> m_connections;

    private final JPanel m_connectionSettingsPanel = new JPanel();

    private final CardLayout m_connectionSettingsCardLayout = new CardLayout();

    private final JLabel m_connectionHostLabel;

    private final JTextField m_connectionHost;

    private final JLabel m_connectionPortLabel;

    private final JTextField m_connectionPort;

    private final JCheckBox m_searchSubfolders;

    private final JCheckBox m_filterFiles;

    private final JLabel m_fileFolderLabel;

    private final FilesHistoryPanel m_fileHistoryPanel;

    private final JButton m_configureFilter;

    public DialogComponentFileChooserGen3(final SettingsModelFileChooserGen2 settingsModel,
        final NodeDialogPane dialogPane, final String... suffixes) {

        super(settingsModel);

        m_settingsModel = settingsModel;
        m_dialogPane = dialogPane;
        m_pathFlowVariableModel = m_dialogPane.createFlowVariableModel(m_settingsModel.getPath().getKey(), Type.STRING);
        m_defaultSuffixes = suffixes;

        m_useConnection = new JCheckBox("Read from: ");

        m_connections = new JComboBox<>(new String[0]);
        m_connections.setEnabled(false);

        m_connectionHostLabel = new JLabel("Host:");
        m_connectionHost = new JTextField(15);

        m_connectionPortLabel = new JLabel("Port:");
        m_connectionPort = new JTextField(4);

        m_fileFolderLabel = new JLabel("File/Folder:");

        m_fileHistoryPanel = new FilesHistoryPanel(m_pathFlowVariableModel, "filechoosergen2", LocationValidation.None, suffixes);
        m_fileHistoryPanel.setDialogType(JFileChooser.OPEN_DIALOG);
        m_fileHistoryPanel.setAllowRemoteURLs(false);
        m_fileHistoryPanel.setShowConnectTimeoutField(false);

        m_searchSubfolders = new JCheckBox("Search subfolders");
        m_filterFiles = new JCheckBox("Filter files in folder");
        m_configureFilter = new JButton("Configure");

        initEventHandlers();
        initLayout();
        updateEnabledness(null);
    }

    private void initLayout() {
        final JPanel panel = getComponentPanel();
        panel.setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 0, 5, 5);
        panel.add(m_useConnection, gbc);

        gbc.gridx++;
        panel.add(m_connections, gbc);

        gbc.gridx++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        initConnectionSettingsPanelLayout();
        panel.add(m_connectionSettingsPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = new Insets(5, 5, 5, 0);
        panel.add(m_fileFolderLabel, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(m_fileHistoryPanel, gbc);

        gbc.gridx = 1;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(m_searchSubfolders, gbc);

        gbc.gridx++;
        panel.add(m_filterFiles, gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor= GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 0);
        panel.add(m_configureFilter, gbc);
    }

    private void initConnectionSettingsPanelLayout() {
        m_connectionSettingsPanel.setLayout(m_connectionSettingsCardLayout);

        final JPanel settingsCard = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 10, 5, 5);
        settingsCard.add(m_connectionHostLabel, gbc);

        gbc.gridx++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(5, 0, 5, 0);
        settingsCard.add(m_connectionHost, gbc);

        gbc.gridx++;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 10, 5, 5);
        settingsCard.add(m_connectionPortLabel, gbc);

        gbc.gridx++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 0);
        settingsCard.add(m_connectionPort, gbc);

        m_connectionSettingsPanel.add(settingsCard, "settings");
        m_connectionSettingsPanel.add(new JPanel(), "empty");
    }

    private void initEventHandlers() {
        m_useConnection.addChangeListener(this::updateEnabledness);
        m_connections.addActionListener(this::updateEnabledness);
        m_fileHistoryPanel.addChangeListener(this::updateEnabledness);
        m_filterFiles.addChangeListener(this::updateEnabledness);
        m_configureFilter.addActionListener((e) -> showFileFilterConfigurationDialog());
    }

    private void showFileFilterConfigurationDialog() {
        Frame f = null;
        Container c = getComponentPanel().getParent();
        while (c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
                break;
            }
            c = c.getParent();
        }

        final FileFilterDialog dialog = new FileFilterDialog(f, m_defaultSuffixes);
        //center the dialog
        dialog.setLocationRelativeTo(c);
        //show it
        dialog.setVisible(true);
    }

    private void updateEnabledness(final Object event) {
        m_connections.setEnabled(m_useConnection.isSelected());

        boolean connectionSettingsVisible = m_useConnection.isSelected()
            && Arrays.asList("HTTP", "HTTPS", "SSH").contains(m_connections.getSelectedItem());

        if (connectionSettingsVisible) {
            m_connectionSettingsCardLayout.show(m_connectionSettingsPanel, "settings");
        } else {
            m_connectionSettingsCardLayout.show(m_connectionSettingsPanel, "empty");
        }

        // FIXME: this is a small hack in order to demonstrate when the folder options get
        // activated. We actually need some way to check for file or folder
        final boolean folderSelected = Files.isDirectory(Paths.get(m_fileHistoryPanel.getSelectedFile()));
        m_searchSubfolders.setEnabled(folderSelected);
        m_filterFiles.setEnabled(folderSelected);
        m_configureFilter.setEnabled(folderSelected && m_filterFiles.isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        m_connectionFlowVariableProvider = new FSConnectionFlowVariableProvider(m_dialogPane);

        final DefaultComboBoxModel<String> comboBoxModel = (DefaultComboBoxModel<String>)m_connections.getModel();
        comboBoxModel.removeAllElements();

        comboBoxModel.addElement("Mountpoint-relative");
        comboBoxModel.addElement("Workflow-relative");
        comboBoxModel.addElement("Node-relative");
        comboBoxModel.addElement("Mountpoint (Testflows)");
        comboBoxModel.addElement("HTTP");
        comboBoxModel.addElement("HTTPS");
        comboBoxModel.addElement("JAR");
        comboBoxModel.addElement("SSH");

        for (final String connectionName : m_connectionFlowVariableProvider.allConnectionNames()) {
            comboBoxModel.addElement(connectionName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        // TODO Auto-generated method stub
    }
}
