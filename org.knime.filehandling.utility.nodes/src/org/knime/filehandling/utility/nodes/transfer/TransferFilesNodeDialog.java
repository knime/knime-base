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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractDialogComponentFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.StatusSwingWorker;
import org.knime.filehandling.core.defaultnodesettings.status.StatusView;
import org.knime.filehandling.utility.nodes.dialog.swingworker.IncludeSourceFolderSwingWorker;
import org.knime.filehandling.utility.nodes.dialog.swingworker.SwingWorkerManager;

/**
 * Node dialog of the Transfer Files node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class TransferFilesNodeDialog extends NodeDialogPane {

    private final DialogComponentReaderFileChooser m_sourceFilePanel;

    private final DialogComponentWriterFileChooser m_destinationFilePanel;

    private final DialogComponentBoolean m_deleteSourceFilesCheckbox;

    private final DialogComponentBoolean m_includeSourceFolderCheckbox;

    private final DialogComponentBoolean m_failOnDeletion;

    private final SwingWorkerManager m_includeSourceFolderSwingWorkerManager;

    private final StatusView m_includeSourceFolderStatusView;

    private final TransferFilesNodeConfig m_config;

    private boolean m_isLoading;

    /**
     * Constructor.
     *
     * @param config the CopyMoveFilesNodeConfig
     */
    TransferFilesNodeDialog(final TransferFilesNodeConfig config) {
        m_config = config;

        final SettingsModelReaderFileChooser sourceFileChooserConfig = m_config.getSourceFileChooserModel();
        final SettingsModelWriterFileChooser destinationFileChooserConfig = m_config.getDestinationFileChooserModel();

        final FlowVariableModel sourceFvm =
            createFlowVariableModel(sourceFileChooserConfig.getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        final FlowVariableModel writeFvm = createFlowVariableModel(destinationFileChooserConfig.getKeysForFSLocation(),
            FSLocationVariableType.INSTANCE);

        m_sourceFilePanel = new DialogComponentReaderFileChooser(sourceFileChooserConfig, "source_chooser", sourceFvm,
            FilterMode.FILE, FilterMode.FILES_IN_FOLDERS, FilterMode.FOLDER);

        m_destinationFilePanel = new DialogComponentWriterFileChooser(destinationFileChooserConfig,
            "destination_chooser", writeFvm, s -> new TransferFilesStatusMessageReporter(s,
                sourceFileChooserConfig.createClone(), config.getSettingsModelIncludeSourceFolder().getBooleanValue()),
            FilterMode.FOLDER);

        m_includeSourceFolderStatusView = new StatusView();

        m_deleteSourceFilesCheckbox =
            new DialogComponentBoolean(m_config.getDeleteSourceFilesModel(), "Delete source files / folders");

        m_includeSourceFolderCheckbox = new DialogComponentBoolean(m_config.getSettingsModelIncludeSourceFolder(),
            "Include selected source folder");

        m_config.getSourceFileChooserModel().addChangeListener(c -> updateIncludeSourceCheckbox());

        m_failOnDeletion =
            new DialogComponentBoolean(m_config.getFailOnDeletionModel(), "Fail on unsuccessful deletion");
        m_deleteSourceFilesCheckbox.getModel().addChangeListener(l -> updateFailOnDeletion());

        m_includeSourceFolderSwingWorkerManager = new SwingWorkerManager(this::createIncludeSourceFolderSwingWorker);

        //Update the component in case something changes so that the status message will be updated accordingly
        sourceFileChooserConfig.addChangeListener(l -> m_destinationFilePanel.updateComponent());
        config.getSettingsModelIncludeSourceFolder().addChangeListener(l -> m_destinationFilePanel.updateComponent());

        sourceFileChooserConfig.addChangeListener(l -> startCancelSwingWorker());

        m_includeSourceFolderCheckbox.getModel().addChangeListener(l -> startCancelSwingWorker());

        createPanel();
    }

    /**
     * Creates the {@link IncludeSourceFolderSwingWorker}.
     *
     * @return the {@link StatusSwingWorker}
     */
    private StatusSwingWorker createIncludeSourceFolderSwingWorker() {
        return new StatusSwingWorker(m_includeSourceFolderStatusView::setStatus,
            new IncludeSourceFolderSwingWorker(m_config.getSourceFileChooserModel().createClone()), false);
    }

    /**
     * Starts and cancels the {@link StatusSwingWorker}.
     */
    private void startCancelSwingWorker() {
        m_includeSourceFolderStatusView.clearStatus();
        if (m_isLoading) {
            return;
        }
        if (m_includeSourceFolderCheckbox.isSelected()
            && m_config.getSourceFileChooserModel().getFilterMode() != FilterMode.FILE) {
            m_includeSourceFolderSwingWorkerManager.startSwingWorker();
        } else {
            m_includeSourceFolderSwingWorkerManager.cancelSwingWorker();
        }
    }

    /**
     * Listener method for the include source folder checkbox.
     */
    private void updateIncludeSourceCheckbox() {
        m_includeSourceFolderCheckbox.getModel()
            .setEnabled((m_config.getSourceFileChooserModel().getFilterMode()) != FilterMode.FILE);
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
    private void createPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.add(createSourcePanel(), gbc);
        gbc.gridy++;
        panel.add(createDestinationPanel(), gbc);
        gbc.gridy++;
        panel.add(createOptionPanel(), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
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
        panel.add(m_sourceFilePanel.getComponentPanel(), gbc);
        return panel;
    }

    /**
     * Creates the option file chooser panel.
     */
    private JPanel createOptionPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Options"));
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.weighty = 0;
        gbc.weightx = 0;
        panel.add(m_includeSourceFolderCheckbox.getComponentPanel(), gbc);
        gbc.gridx++;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridheight = 2;
        panel.add(m_includeSourceFolderStatusView.getLabel(), gbc);
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridy++;
        panel.add(m_deleteSourceFilesCheckbox.getComponentPanel(), gbc);
        gbc.gridx++;
        panel.add(m_failOnDeletion.getComponentPanel(), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JPanel(), gbc);
        return panel;
    }

    /**
     * Creates the destination file chooser panel.
     */
    private JPanel createDestinationPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Destination"));
        panel.add(m_destinationFilePanel.getComponentPanel(), gbc);

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
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.weightx = 1;
        return gbc;
    }

    /**
     * {@inheritDoc}
     *
     * Cancels the {@link IncludeSourceFolderSwingWorker} when the dialog will be closed. And the
     * {@link StatusSwingWorker} of the source and destination {@link AbstractDialogComponentFileChooser}.
     */
    @Override
    public void onClose() {
        m_includeSourceFolderSwingWorkerManager.cancelSwingWorker();
        m_sourceFilePanel.onClose();
        m_destinationFilePanel.onClose();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_sourceFilePanel.saveSettingsTo(settings);
        m_destinationFilePanel.saveSettingsTo(settings);
        m_includeSourceFolderCheckbox.saveSettingsTo(settings);
        m_deleteSourceFilesCheckbox.saveSettingsTo(settings);
        m_failOnDeletion.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_isLoading = true;
        m_sourceFilePanel.loadSettingsFrom(settings, specs);
        m_destinationFilePanel.loadSettingsFrom(settings, specs);
        m_includeSourceFolderCheckbox.loadSettingsFrom(settings, specs);
        m_deleteSourceFilesCheckbox.loadSettingsFrom(settings, specs);
        m_failOnDeletion.loadSettingsFrom(settings, specs);
        m_isLoading = false;

        //update the checkbox after loading the settings
        updateIncludeSourceCheckbox();
        updateFailOnDeletion();

        startCancelSwingWorker();
    }
}
