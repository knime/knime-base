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

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.SpaceAware;
import org.knime.filehandling.core.connections.SpaceAware.Space;
import org.knime.filehandling.core.connections.base.ui.LoadedItemsSelector;
import org.knime.filehandling.core.connections.base.ui.LoadedItemsSelector.IdComboboxItem;
import org.knime.filehandling.core.connections.base.ui.WorkingDirectoryChooser;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.MountpointFileSystemDialog;
import org.knime.filehandling.core.fs.knime.space.node.SpaceConnectorNodeSettings.SpaceMode;

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

    private final JLabel m_fetchingErrorLabel;

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

        m_spaceSelector = new LoadedItemsSelector(m_settings.getSpaceIdModel(), m_settings.getSpaceNameModel(),
            "", null, false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void beforeFetchItems() {
                m_fetchingErrorLabel.setText("");
            }

            @Override
            public List<IdComboboxItem> fetchItems() throws Exception {
                return fetchSpaces();
            }

            @Override
            protected String fetchExceptionMessage(final Exception ex) {
                return ExceptionUtil.unpack(ex).getMessage();
            }

            @Override
            protected void displayErrorMessage(final String message) {
                m_fetchingErrorLabel.setText(String.format("<html>%s</html>", message));
            }
        };

        m_workingDirChooser = new WorkingDirectoryChooser("space.workingDir", this::createSpaceConnection);
        m_workingDirChooser.addListener(e -> {
            final var workDir = m_workingDirChooser.getSelectedWorkingDirectory();
            m_settings.getWorkingDirectoryModel().setStringValue(workDir);
        });

        m_mountpointSelector = new MountpointFileSystemDialog(m_settings.getMountpoint());

        m_settings.getMountpoint().addChangeListener(this::onMountpointChanged);
        m_ignoreChangeEvents = false;

        m_fetchingErrorLabel = new JLabel("");
        m_fetchingErrorLabel.setForeground(Color.RED);

        addTab("Settings", createSettingsPanel());
        addTab("Advanced", createAdvancedPanel());
    }

    private JRadioButton createRadioButton(final ButtonGroup group, final SpaceMode mode) {
        var rb = new JRadioButton(mode.getLabel());
        rb.addActionListener(e -> m_settings.setSpaceMode(mode));
        group.add(rb);
        return rb;
    }

    private void onMountpointChanged(@SuppressWarnings("unused") final ChangeEvent e) {
        if (m_ignoreChangeEvents) {
            return;
        }
        m_spaceSelector.fetch();
    }

    @SuppressWarnings("resource")
    private List<IdComboboxItem> fetchSpaces() throws IOException {
        var mountpointConnection = DefaultFSConnectionFactory
            .createMountpointConnection(m_settings.getMountpoint().getMountpoint().getId());
        final var fileSystemProvider = mountpointConnection.getFileSystem().provider();

        if (fileSystemProvider instanceof SpaceAware) {
            return ((SpaceAware)fileSystemProvider).getSpaces() //
                .stream() //
                .map(SpaceConnectorNodeDialog::toComboBoxItem) //
                .sorted((l, r) -> l.getTitle().compareTo(r.getTitle())) //
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private static IdComboboxItem toComboBoxItem(final Space space) {
        return new IdComboboxItem(space.getSpaceId(), space.getName());
    }


    private FSConnection createSpaceConnection() {
        return SpaceConnectorFSConnectionFactory.create(m_settings).createFSConnection();
    }

    private JComponent createSettingsPanel() {
        var box = new Box(BoxLayout.Y_AXIS);
        box.add(createSpaceSelectionPanel());
        m_workingDirChooser.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5),
            BorderFactory.createTitledBorder("Working directory")));
        box.add(m_workingDirChooser);
        return box;
    }

    private JComponent createSpaceSelectionPanel() {
        var panel = new JPanel(new GridBagLayout());
        var outerInsets = new Insets(5, 10, 0, 0);
        var innerInsets = new Insets(5, 30, 0, 0);

        var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = outerInsets;
        panel.add(m_radioCurrentSpace, gbc);

        gbc.gridx++;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = outerInsets;
        panel.add(m_radioOtherSpace, gbc);

        gbc.gridx++;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);


        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = innerInsets;
        panel.add(new JLabel("Hub mountpoint:"), gbc);

        gbc.gridx++;
        gbc.insets = new Insets(5, 15, 0, 0);
        panel.add(m_mountpointSelector.getSpecifierComponent(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = innerInsets;
        panel.add(new JLabel("Space:"), gbc);

        gbc.gridx++;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(5, 5, 0, 0);
        panel.add(m_spaceSelector, gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = innerInsets;
        panel.add(m_fetchingErrorLabel, gbc);

        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5),
            BorderFactory.createTitledBorder("Space selection")));
        return panel;
    }

    private JComponent createAdvancedPanel() {
        final var panel = new JPanel(new GridBagLayout());

        final var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 10, 5);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(createAdvancedConnectionSettingsPanel(), gbc);

        gbc.gridy++;

        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private Component createAdvancedConnectionSettingsPanel() {
        final var panel = new JPanel(new GridBagLayout());

        panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.RAISED), "Connection settings"));

        final var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.anchor = GridBagConstraints.LINE_START;

        panel.add(new JLabel("Connection timeout (seconds):"), gbc);
        gbc.gridx++;
        panel.add(new DialogComponentNumber(m_settings.getConnectionTimeoutModel(), "", 1).getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;

        panel.add(new JLabel("Read timeout (seconds):"), gbc);
        gbc.gridx++;
        panel.add(new DialogComponentNumber(m_settings.getReadTimeoutModel(), "", 1).getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(Box.createHorizontalGlue(), gbc);

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
            m_ignoreChangeEvents = true;
            m_settings.loadInDialog(settings, specs);
            settingsLoaded();
        } finally {
            m_ignoreChangeEvents = false;
        }
    }

    private void settingsLoaded() {
        m_spaceSelector.onSettingsLoaded();
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
    public void onOpen() {
        m_spaceSelector.fetch();
    }

    @Override
    public void onClose() {
        m_workingDirChooser.onClose();
    }
}
