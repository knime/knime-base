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
package org.knime.base.node.io.filehandling.util.tempdir;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.KeyValuePanel;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;

/**
 * The NodeDialog for the "Create Temp Dir" Node.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
class CreateTempDir2NodeDialog extends NodeDialogPane {

    private static final String FILE_HISTORY_ID = "create_temp_dir_history";

    private static final int TEXT_FIELD_WIDTH = 20;

    private final JTextField m_tempDirPrefixField;

    private final JTextField m_tempDirPathVariableNameField;

    private final JCheckBox m_deleteDirOnResetChecker;

    private final KeyValuePanel m_additionalVariablePathPairPanel;

    private final DialogComponentWriterFileChooser m_parentDirChooserPanel;

    private final CreateTempDir2NodeConfig m_config;

    public CreateTempDir2NodeDialog(final PortsConfiguration portsConfig) {
        m_config = new CreateTempDir2NodeConfig(portsConfig);

        final FlowVariableModel writeFvm = createFlowVariableModel(
            m_config.getParentDirChooserModel().getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_parentDirChooserPanel =
            new DialogComponentWriterFileChooser(m_config.getParentDirChooserModel(), FILE_HISTORY_ID, writeFvm);

        m_deleteDirOnResetChecker = new JCheckBox("Delete temp dir on reset");

        m_tempDirPrefixField = new JTextField(TEXT_FIELD_WIDTH);
        m_tempDirPathVariableNameField = new JTextField(TEXT_FIELD_WIDTH);

        m_additionalVariablePathPairPanel = new KeyValuePanel();
        m_additionalVariablePathPairPanel.setKeyColumnLabel("Variable Name");
        m_additionalVariablePathPairPanel.setValueColumnLabel("Filename");

        addTab("Configuration", initLayout());
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
        fileOptionsPanel.add(new JLabel("Temp dir prefix"), gbc);
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
        additionalVarPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "More path variables"));

        gbc.insets = new Insets(5, 0, 3, 0);
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        m_additionalVariablePathPairPanel.getTable().setPreferredScrollableViewportSize(null);
        additionalVarPanel.add(m_additionalVariablePathPairPanel, gbc);
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

        m_additionalVariablePathPairPanel.setTableData(m_config.getAdditionalVarNames(),
            m_config.getAdditionalVarValues());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_config.setTempDirPrefix(m_tempDirPrefixField.getText());
        m_config.setTempDirVariableName(m_tempDirPathVariableNameField.getText());
        m_config.setDeleteDirOnReset(m_deleteDirOnResetChecker.isSelected());
        m_config.setAdditionalVarNames(m_additionalVariablePathPairPanel.getKeys());
        m_config.setAdditionalVarValues(m_additionalVariablePathPairPanel.getValues());

        m_parentDirChooserPanel.saveSettingsTo(settings);
        m_config.saveSettingsForDialog(settings);
    }

}
