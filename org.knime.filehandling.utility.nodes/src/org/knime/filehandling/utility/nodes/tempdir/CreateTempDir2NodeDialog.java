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
 *   June 08, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.tempdir;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractSettingsModelFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.StatusMessageReporter;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessageUtils;
import org.knime.filehandling.utility.nodes.dialog.variables.BaseLocationListener;
import org.knime.filehandling.utility.nodes.dialog.variables.FSLocationVariablePanel;
import org.knime.filehandling.utility.nodes.dialog.variables.FSLocationVariableTableModel;

/**
 * The NodeDialog for the "Create Temp Dir" Node.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
final class CreateTempDir2NodeDialog extends NodeDialogPane {

    private static final String FILE_HISTORY_ID = "create_temp_dir_history";

    private static final int TEXT_FIELD_WIDTH = 20;

    private final JTextField m_tempDirPrefixField;

    private final JTextField m_tempDirPathVariableNameField;

    private final JCheckBox m_deleteDirOnResetChecker;

    private final DialogComponentWriterFileChooser m_parentDirChooserPanel;

    private final CreateTempDir2NodeConfig m_config;

    private final FSLocationVariablePanel m_locationPanel;

    public CreateTempDir2NodeDialog(final PortsConfiguration portsConfig) {
        m_config = new CreateTempDir2NodeConfig(portsConfig);

        final SettingsModelWriterFileChooser baseLocationModel = m_config.getParentDirChooserModel();
        final FlowVariableModel writeFvm =
            createFlowVariableModel(baseLocationModel.getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_parentDirChooserPanel = new DialogComponentWriterFileChooser(baseLocationModel, FILE_HISTORY_ID, writeFvm,
            FolderStatusMessageReporter::new);

        m_deleteDirOnResetChecker = new JCheckBox("Delete temp folder on reset");

        m_tempDirPrefixField = new JTextField(TEXT_FIELD_WIDTH);
        m_tempDirPathVariableNameField = new JTextField(TEXT_FIELD_WIDTH);

        final FSLocationVariableTableModel varTableModel = m_config.getFSLocationTableModel();
        m_locationPanel = new FSLocationVariablePanel(varTableModel);

        final TempDirLocationListener l =
            new TempDirLocationListener(varTableModel, baseLocationModel, m_tempDirPrefixField);
        baseLocationModel.addChangeListener(l);
        m_tempDirPrefixField.getDocument().addDocumentListener(l);

        addTab("Settings", initLayout());
    }

    /**
     * Helper method to create and initialize {@link GridBagConstraints}.
     *
     * @return initialized {@link GridBagConstraints}
     */
    private static final GridBagConstraints createAndInitGBC() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        return gbc;
    }

    private JPanel initLayout() {
        final GridBagConstraints gbc = createAndInitGBC();
        final JPanel panel = new JPanel(new GridBagLayout());

        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        panel.add(createFilePanel(), gbc);
        gbc.gridy++;
        panel.add(createFileOptionsPanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(createAdditionalVariablesPanel(), gbc);

        return panel;
    }

    private JPanel createFilePanel() {
        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output location"));
        filePanel.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, m_parentDirChooserPanel.getComponentPanel().getPreferredSize().height));
        filePanel.add(m_parentDirChooserPanel.getComponentPanel());
        filePanel.add(Box.createHorizontalGlue());
        return filePanel;
    }

    private JPanel createFileOptionsPanel() {
        final GridBagConstraints gbc = createAndInitGBC();
        final JPanel fileOptionsPanel = new JPanel(new GridBagLayout());
        fileOptionsPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "File options"));

        gbc.insets = new Insets(5, 5, 5, 15);

        fileOptionsPanel.add(m_deleteDirOnResetChecker, gbc);

        gbc.gridy++;
        fileOptionsPanel.add(new JLabel("Temp folder prefix"), gbc);
        gbc.gridx++;
        fileOptionsPanel.add(m_tempDirPrefixField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        fileOptionsPanel.add(new JLabel("Export path as (variable name)"), gbc);
        gbc.gridx++;
        fileOptionsPanel.add(m_tempDirPathVariableNameField, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        fileOptionsPanel.add(Box.createHorizontalBox(), gbc);
        return fileOptionsPanel;
    }

    private JPanel createAdditionalVariablesPanel() {
        final GridBagConstraints gbc = createAndInitGBC();
        final JPanel additionalVarPanel = new JPanel(new GridBagLayout());
        additionalVarPanel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Additional path variables"));

        gbc.insets = new Insets(5, 0, 3, 0);
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        additionalVarPanel.add(m_locationPanel, gbc);
        return additionalVarPanel;
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_parentDirChooserPanel.loadSettingsFrom(settings, specs);
        m_config.loadSettingsForDialog(settings);

        m_deleteDirOnResetChecker.setSelected(m_config.deleteDirOnReset());

        m_tempDirPrefixField.setText(m_config.getTempDirPrefix());
        m_tempDirPathVariableNameField.setText(m_config.getTempDirVariableName());

        m_locationPanel.loadSettings(settings);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_config.setTempDirPrefix(m_tempDirPrefixField.getText());
        m_config.setTempDirVariableName(m_tempDirPathVariableNameField.getText());
        m_config.setDeleteDirOnReset(m_deleteDirOnResetChecker.isSelected());
        m_parentDirChooserPanel.saveSettingsTo(settings);
        m_config.saveSettingsForDialog(settings);
        m_locationPanel.saveSettings(settings);
    }

    @Override
    public void onClose() {
        m_locationPanel.onClose();
    }

    private static class TempDirLocationListener extends BaseLocationListener implements DocumentListener {

        private static final String UNIQUE_ID = "<unique_id>";

        private final JTextField m_tempDirPrefix;

        /**
         * Constructor.
         *
         * @param varTableModel the {@link FSLocationVariableTableModel}
         * @param baseLocationModel the {@link AbstractSettingsModelFileChooser}
         * @param textField the {@link JTextField} holding the temporary directory name prefix
         */
        public TempDirLocationListener(final FSLocationVariableTableModel varTableModel,
            final AbstractSettingsModelFileChooser<?> baseLocationModel, final JTextField textField) {
            super(varTableModel, baseLocationModel);
            m_tempDirPrefix = textField;
        }

        @Override
        public void insertUpdate(final DocumentEvent e) {
            updateLocation();
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            updateLocation();
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
            updateLocation();
        }

        @Override
        protected String getLocation() {
            String base = super.getLocation();
            base = base.endsWith(getSeparator()) ? base : (base + getSeparator());
            return base + m_tempDirPrefix.getText() + UNIQUE_ID;
        }
    }

    /**
     * {@link StatusMessageReporter} that throws an exception if the selected folder does not exist.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private static class FolderStatusMessageReporter implements StatusMessageReporter {

        private final SettingsModelWriterFileChooser m_settings;

        /**
         * Constructor.
         *
         * @param settings the writer file chooser settings
         */
        FolderStatusMessageReporter(final SettingsModelWriterFileChooser settings) {
            m_settings = settings;
        }

        @Override
        public StatusMessage report() throws IOException, InvalidSettingsException {
            try (final WritePathAccessor accessor = m_settings.createWritePathAccessor()) {
                final FSPath path = accessor.getOutputPath(StatusMessageUtils.NO_OP_CONSUMER);
                if (m_settings.isCreateMissingFolders()) {
                    return DefaultStatusMessage.SUCCESS_MSG;
                }
                if (FSFiles.exists(path)) {
                    if (!Files.isWritable(path)) {
                        throw ExceptionUtil.createAccessDeniedException(path);
                    }
                    return DefaultStatusMessage.SUCCESS_MSG;
                } else {
                    return StatusMessageUtils.MISSING_FOLDERS_MSG;
                }
            }

        }

    }

}
