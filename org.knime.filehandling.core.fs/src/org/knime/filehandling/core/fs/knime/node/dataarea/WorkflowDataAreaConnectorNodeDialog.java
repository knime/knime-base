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
 *   2021-07-25 (Alexander Bondaletov): created
 */
package org.knime.filehandling.core.fs.knime.node.dataarea;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileView;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.base.BaseFSConnection;
import org.knime.filehandling.core.connections.base.WorkflowAwareFileView;
import org.knime.filehandling.core.connections.base.ui.WorkingDirectoryChooser;
import org.knime.filehandling.core.connections.config.RelativeToFSConnectionConfig;
import org.knime.filehandling.core.filechooser.AbstractFileChooserBrowser;
import org.knime.filehandling.core.filechooser.NioFileSystemBrowser;
import org.knime.filehandling.core.filechooser.NioFileSystemView;

/**
 * Relative To Connector node dialog.
 *
 * @author Alexander Bondaletov
 */
public class WorkflowDataAreaConnectorNodeDialog extends NodeDialogPane {

    private final WorkflowDataAreaConnectorNodeSettings m_settings;

    private final WorkingDirectoryChooser m_workingDirChooser;
    private final ChangeListener m_workdirListener;

    /**
     * Creates new instance.
     */
    public WorkflowDataAreaConnectorNodeDialog() {
        m_settings = new WorkflowDataAreaConnectorNodeSettings();

        m_workingDirChooser = new WorkingDirectoryChooser("local.workingDir", this::createFSConnection);
        m_workdirListener = e -> {
            String workDir = m_workingDirChooser.getSelectedWorkingDirectory();
            if(!workDir.startsWith(RelativeToFSConnectionConfig.PATH_SEPARATOR)) {
                workDir = RelativeToFSConnectionConfig.PATH_SEPARATOR + workDir;
            }

            m_settings.getWorkingDirectoryModel().setStringValue(workDir);
        };

        addTab("Settings", createSettingsPanel());
    }

    @SuppressWarnings("resource")
    private FSConnection createFSConnection() {
        String workDir = m_settings.getWorkingDirectory();
        if (StringUtils.isBlank(workDir) || !workDir.startsWith(RelativeToFSConnectionConfig.PATH_SEPARATOR)) {
            workDir = RelativeToFSConnectionConfig.PATH_SEPARATOR;
        }

        final var actualFSConnection =
            DefaultFSConnectionFactory.createRelativeToConnection(RelativeTo.WORKFLOW_DATA, workDir);
        return new BaseFSConnection() {

            @Override
            public FSFileSystem<?> getFileSystem() {
                return actualFSConnection.getFileSystem();
            }

            @Override
            protected AbstractFileChooserBrowser createFileSystemBrowser() {
                // File system browser that is workflow area (displays workflows as little workflow icons), but does not relativize the
                // selected path.
                return new NioFileSystemBrowser(new NioFileSystemView(actualFSConnection)) {
                    @Override
                    protected FileView getFileView() {
                        return new WorkflowAwareFileView();
                    }
                };
            }
        };
    }

    private JComponent createSettingsPanel() {
        final var panel = new JPanel(new GridBagLayout());
        final var c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 10, 0, 0);
        panel.add(m_workingDirChooser, c);

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy += 1;
        panel.add(Box.createVerticalGlue(), c);

        panel.setBorder(BorderFactory.createTitledBorder("File system settings"));
        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_workingDirChooser.addCurrentSelectionToHistory();
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException {
        try {
            m_settings.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) { // NOSONAR
            // ignore
        }

        settingsLoaded();
    }

    private void settingsLoaded() {
        m_workingDirChooser.setSelectedWorkingDirectory(m_settings.getWorkingDirectoryModel().getStringValue());
        m_workingDirChooser.addListener(m_workdirListener);
    }

    @Override
    public void onClose() {
        m_workingDirChooser.removeListener(m_workdirListener);
        m_workingDirChooser.onClose();
    }
}
