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
 *   Mar 5, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.VariableType;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.utility.nodes.transfer.policy.TransferPolicy;
import org.knime.filehandling.utility.nodes.truncator.TruncatePathOption;
import org.knime.filehandling.utility.nodes.truncator.TruncationPanel;

/**
 * Abstract node dialog of the Transfer Files/Folder node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @param <T> an instance of {@link AbstractTransferFilesNodeConfig}
 */
public abstract class AbstractTransferFilesNodeDialog<T extends AbstractTransferFilesNodeConfig>
    extends NodeDialogPane {

    /** The label for the destination file folder name mode. */
    public static final String DESTINATION_FILE_FOLDER_NAME_MODE = "Destination file/folder name mode";

    private final DialogComponentWriterFileChooser m_destinationFilePanel;

    private final TruncationPanel m_truncationPanel;

    private final DialogComponentBoolean m_deleteSourceFilesCheckbox;

    private final DialogComponentBoolean m_failOnDeletion;

    private final DialogComponentBoolean m_verboseOutput;

    private final DialogComponentButtonGroup m_transferPolicy;

    private final T m_config;

    /**
     * Constructor.
     *
     * @param config the {@link AbstractTransferFilesNodeConfig}
     */
    protected AbstractTransferFilesNodeDialog(final T config) {
        m_config = config;

        final SettingsModelWriterFileChooser destinationFileChooserConfig = m_config.getDestinationFileChooserModel();

        final FlowVariableModel writeFvm = createFlowVariableModel(destinationFileChooserConfig.getKeysForFSLocation(),
            FSLocationVariableType.INSTANCE);

        m_destinationFilePanel =
            new DialogComponentWriterFileChooser(destinationFileChooserConfig, "destination_chooser", writeFvm);

        m_truncationPanel = new TruncationPanel(DESTINATION_FILE_FOLDER_NAME_MODE, config.getTruncationSettings(),
            AbstractTransferFilesNodeDialog::getTruncatePathOptionLabel);

        m_deleteSourceFilesCheckbox =
            new DialogComponentBoolean(m_config.getDeleteSourceFilesModel(), "Delete source files / folders");

        m_failOnDeletion =
            new DialogComponentBoolean(m_config.getFailOnDeletionModel(), "Fail on unsuccessful deletion");
        m_deleteSourceFilesCheckbox.getModel().addChangeListener(l -> updateFailOnDeletion());

        m_verboseOutput = new DialogComponentBoolean(m_config.getVerboseOutputModel(), "Verbose output");

        m_transferPolicy =
            new DialogComponentButtonGroup(m_config.getTransferPolicyModel(), null, false, TransferPolicy.values());
    }

    private static String getTruncatePathOptionLabel(final TruncatePathOption opt) {
        switch (opt) {
            case KEEP:
                return "Append absolute source path";
            case RELATIVE:
                return "Append relative source path";
            case REMOVE_FOLDER_PREFIX:
                return "Append source path without folder prefix";
            default:
                throw new IllegalArgumentException(String.format("Unsupported option %s", opt));
        }
    }

    @Override
    public final FlowVariableModel createFlowVariableModel(final String[] keys, final VariableType<?> type) {
        return super.createFlowVariableModel(keys, type);
    }

    /**
     * Returns the configuration.
     *
     * @return the configuration
     */
    protected final T getConfig() {
        return m_config;
    }

    /**
     * Enables/disables the destination file chooser panel.
     *
     * @param enabled flag indicating whether to enable or disable the panel
     */
    protected final void enableDestFileChooserPanel(final boolean enabled) {
        m_truncationPanel.setEnabled(enabled);
        m_destinationFilePanel.getModel().setEnabled(enabled);
    }

    /**
     * Listener method for the fail on deletion checkbox.
     */
    private void updateFailOnDeletion() {
        m_failOnDeletion.getModel().setEnabled(m_deleteSourceFilesCheckbox.isSelected());
    }

    /**
     * Method to create the panel.
     */
    protected final void createPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.add(createSourcePanel(), gbc);
        gbc.gridy++;
        panel.add(createDestinationPanel(), gbc);
        gbc.gridy++;
        panel.add(getTransferPolicyPanel(), gbc);
        gbc.gridy++;
        panel.add(createOptionPanel(), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.gridy++;
        panel.add(new JPanel(), gbc);

        addTab("Settings", panel);
    }

    /**
     * Creates the source file chooser panel.
     */
    private JPanel createSourcePanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Source"));
        panel.add(getSourceLocationPanel(), gbc);
        return panel;
    }

    /**
     * Returns the panel containing the source location.
     *
     * @return the source location panel
     */
    protected abstract Component getSourceLocationPanel();

    /**
     * Creates the option file chooser panel.
     */
    private JPanel createOptionPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Options"));

        final GridBagConstraints gbc = createAndInitGBC();
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(m_deleteSourceFilesCheckbox.getComponentPanel(), gbc);

        gbc.gridx++;
        panel.add(m_failOnDeletion.getComponentPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(m_verboseOutput.getComponentPanel(), gbc);

        gbc.gridy++;
        addAdditionalOptions(panel, gbc);

        gbc.gridy++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    private JPanel getTransferPolicyPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Transfer policy"));

        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(m_transferPolicy.getComponentPanel(), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx++;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    /**
     * Allows to add additional components to the options panel.
     *
     * @param panel the options panel
     * @param gbc the {@link GridBagConstraints}
     */
    protected void addAdditionalOptions(final JPanel panel, final GridBagConstraints gbc) {
        // nothing to do
    }

    /**
     * Creates the destination selection panel.
     *
     * @return the destination selection panel
     */
    private JPanel createDestinationPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Destination"));
        panel.add(getDestinationPanel(), gbc);
        return panel;
    }

    /**
     * Creates the destination panel containing a {@link DialogComponentWriterFileChooser}
     *
     * @return the destination panel
     */
    protected JPanel getDestinationPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.add(m_destinationFilePanel.getComponentPanel(), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(10,0,0,0);
        panel.add(m_truncationPanel.getPanel(), gbc);
        return panel;
    }

    /**
     * Creates the initial {@link GridBagConstraints}.
     */
    private static final GridBagConstraints createAndInitGBC() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.weightx = 1;
        return gbc;
    }

    @Override
    public void onClose() {
        m_destinationFilePanel.onClose();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_destinationFilePanel.saveSettingsTo(settings);
        m_truncationPanel.saveSettingsTo(settings);
        m_transferPolicy.saveSettingsTo(settings);
        m_deleteSourceFilesCheckbox.saveSettingsTo(settings);
        m_failOnDeletion.saveSettingsTo(settings);
        m_verboseOutput.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_verboseOutput.loadSettingsFrom(settings, specs);
        m_truncationPanel.loadSettings(settings, specs);
        m_deleteSourceFilesCheckbox.loadSettingsFrom(settings, specs);
        m_failOnDeletion.loadSettingsFrom(settings, specs);
        m_destinationFilePanel.loadSettingsFrom(settings, specs);
        m_transferPolicy.loadSettingsFrom(settings, specs);
        //update the checkbox after loading the settings
        updateFailOnDeletion();
    }
}
