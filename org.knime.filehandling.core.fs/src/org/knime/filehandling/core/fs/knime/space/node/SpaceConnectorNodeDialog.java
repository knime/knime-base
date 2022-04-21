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
 *   Apr 17, 2022 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.core.fs.knime.space.node;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.base.ui.LoadedItemsSelector;
import org.knime.filehandling.core.connections.base.ui.WorkingDirectoryChooser;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.MountpointFileSystemDialog;
import org.knime.filehandling.core.fs.knime.space.node.SpaceConnectorNodeSettings.SpaceMode;
import org.knime.filehandling.core.util.MountPointFileSystemAccessService;
import org.knime.filehandling.core.util.WorkflowContextUtil;


/**
 * Space Connector node dialog.
 *
 * @author Zkriya Rakhimberdiyev
 */
public class SpaceConnectorNodeDialog extends NodeDialogPane {

    private final SpaceConnectorNodeSettings m_settings;

    private final WorkingDirectoryChooser m_workingDirChooser;

    private final MountpointFileSystemDialog m_mountpointSelector;

    private final LoadedItemsSelector m_spaceSelector;

    private final JRadioButton m_radioCurrentSpace;

    private final JRadioButton m_radioOtherSpace;

    private boolean m_ignoreChangeEvents;

    /**
     * Creates new instance.
     */
    public SpaceConnectorNodeDialog() {
        m_settings = new SpaceConnectorNodeSettings();

        m_settings.getSpaceModeModel().addChangeListener(e -> updateComponentsEnabled());

        var group = new ButtonGroup();
        m_radioCurrentSpace = createRadioButton(group, SpaceMode.CURRENT);
        m_radioOtherSpace = createRadioButton(group, SpaceMode.OTHER);

        m_workingDirChooser = new WorkingDirectoryChooser("mountpoint.workingDir", this::createFSConnection);
        m_workingDirChooser.addListener(e -> {
            if (!m_ignoreChangeEvents) {
                final var workDir = m_workingDirChooser.getSelectedWorkingDirectory();
                m_settings.getWorkingDirectoryModel().setStringValue(workDir);
            }
        });

        m_mountpointSelector = new MountpointFileSystemDialog(m_settings.getMountpoint());

        m_settings.getMountpoint().addChangeListener(this::onMountpointChanged);
        m_settings.getSpaceModeModel().addChangeListener(this::onMountpointChanged);
        m_ignoreChangeEvents = false;

        m_spaceSelector = new LoadedItemsSelector(m_settings.getSpaceListModel(), m_settings.getSpaceNameModel(),
            "Space:", null, false) {

            private static final long serialVersionUID = 1L;

            @Override
            public List<IdComboboxItem> fetchItems() throws Exception {
                return Collections.emptyList();
            }

            @Override
            public String fetchExceptionMessage(final Exception ex) {
                return ex.getMessage();
            }
        };

        addTab("Settings", createSettingsPanel());
    }

    private void onMountpointChanged(@SuppressWarnings("unused") final ChangeEvent e) {
        if (m_ignoreChangeEvents) {
            return;
        }

        KNIMEConnection currMountpoint = null;
        if (m_settings.getSpaceMode() == SpaceMode.CURRENT) {
            currMountpoint = WorkflowContextUtil.getWorkflowContextOptional() //
                .map(ctx -> ctx.getMountpointURI().orElse(null)) //
                .map(URI::getHost) //
                .map(KNIMEConnection::getConnection).orElse(null);
        } else if (m_settings.getSpaceMode() == SpaceMode.OTHER) {
            currMountpoint = m_settings.getMountpoint().getMountpoint();
        }

        if (currMountpoint != null) {
            final var defaultDirUri = MountPointFileSystemAccessService.instance() //
                .getDefaultDirectory(URI.create(currMountpoint.getSchemeAndHost()));

            m_settings.getWorkingDirectoryModel().setStringValue(defaultDirUri.getPath());
            m_workingDirChooser.setSelectedWorkingDirectory(defaultDirUri.getPath());
        }
    }

    private JRadioButton createRadioButton(final ButtonGroup group, final SpaceMode mode) {
        var rb = new JRadioButton(mode.getLabel());
        rb.addActionListener(e -> m_settings.setSpaceMode(mode));
        group.add(rb);
        return rb;
    }

    private FSConnection createFSConnection() {
        return SpaceConnectorFSConnectionFactory.create(m_settings).createFSConnection();
    }

    private JComponent createSettingsPanel() {
        var box = new Box(BoxLayout.Y_AXIS);
        box.add(createHubMountpointSelectionPanel());
        m_workingDirChooser.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5),
            BorderFactory.createTitledBorder("Working directory")));
        box.add(m_workingDirChooser);
        return box;
    }

    private JComponent createHubMountpointSelectionPanel() {
        var panel = new JPanel(new GridBagLayout());
        var outerInsets = new Insets(0, 10, 0, 0);
        var innerInsets = new Insets(0, 30, 0, 0);

        var c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = outerInsets;
        panel.add(m_radioCurrentSpace, c);

        c.gridy += 1;
        c.insets = outerInsets;
        panel.add(m_radioOtherSpace, c);

        c.gridy += 1;
        c.insets = innerInsets;
        panel.add(createHubMountpointSelector(), c);

        c.gridy += 1;
        c.insets = innerInsets;
        panel.add(m_spaceSelector, c);

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy += 1;
        panel.add(Box.createVerticalGlue(), c);

        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5),
            BorderFactory.createTitledBorder("Space selection")));
        return panel;
    }

    private JComponent createHubMountpointSelector() {
        var panel = new JPanel();
        panel.add(new JLabel("Hub mountpoint: "));
        panel.add(m_mountpointSelector.getSpecifierComponent());
        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_workingDirChooser.addCurrentSelectionToHistory();
        m_settings.validate();
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        try {
            m_ignoreChangeEvents = true;
            m_settings.loadInDialog(settings, specs);
            settingsLoaded();
        } finally {
            m_ignoreChangeEvents = false;
        }
    }

    private void settingsLoaded() {
        m_workingDirChooser.setSelectedWorkingDirectory(m_settings.getWorkingDirectoryModel().getStringValue());
        updateComponentsEnabled();
    }

    private void updateComponentsEnabled() {
        var mode = m_settings.getSpaceMode();

        m_radioCurrentSpace.setSelected(mode == SpaceMode.CURRENT);
        m_radioOtherSpace.setSelected(mode == SpaceMode.OTHER);

        m_mountpointSelector.setEnabled(mode == SpaceMode.OTHER);
        m_spaceSelector.setEnabled(mode == SpaceMode.OTHER);
    }

    @Override
    public void onClose() {
        m_workingDirChooser.onClose();
    }
}
