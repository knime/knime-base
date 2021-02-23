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
 *   Aug 3, 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.deletepaths;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;

/**
 * Node dialog of the "Delete Files/Folders" node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class DeleteFilesAndFoldersNodeDialog extends NodeDialogPane {

    private static final String HISTORY_ID = "delete_files_history";

    private final DeleteFilesAndFoldersNodeConfig m_config;

    private final DialogComponentReaderFileChooser m_fileChooser;

    private final JCheckBox m_abortIfFails;

    /**
     *
     * @param portsConfig this nodes ports configuration
     */
    DeleteFilesAndFoldersNodeDialog(final PortsConfiguration portsConfig) {
        m_config = new DeleteFilesAndFoldersNodeConfig(portsConfig);

        final FlowVariableModel readFvm = createFlowVariableModel(
            m_config.getFileChooserSettings().getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_fileChooser = new DialogComponentReaderFileChooser(m_config.getFileChooserSettings(), HISTORY_ID, readFvm);

        m_abortIfFails = new JCheckBox("Abort if delete fails");

        addTab("Settings", initLayout());
    }

    private JPanel initLayout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(createFilePanel(), gbc);
        gbc.gridy++;
        panel.add(createOptionsPanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(new JPanel(), gbc);

        return panel;
    }

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

    private JPanel createFilePanel() {
        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Select location"));
        filePanel.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, m_fileChooser.getComponentPanel().getPreferredSize().height));
        filePanel.add(m_fileChooser.getComponentPanel());
        filePanel.add(Box.createHorizontalGlue());
        return filePanel;
    }

    private JPanel createOptionsPanel() {
        final GridBagConstraints gbc = createAndInitGBC();
        final JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Options"));
        optionsPanel.add(m_abortIfFails, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        optionsPanel.add(new JPanel(), gbc);
        return optionsPanel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_fileChooser.saveSettingsTo(settings);

        m_config.setAbortIfFails(m_abortIfFails.isSelected());
        m_config.saveSettingsForDialog(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_fileChooser.loadSettingsFrom(settings, specs);

        m_abortIfFails.setSelected(m_config.isAbortedIfFails());
        m_config.loadSettingsForDialog(settings);
    }

    @Override
    public void onClose() {
        m_fileChooser.onClose();
    }
}
