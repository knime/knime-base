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
 *   Nov 11, 2021 (Alexander Bondaletov): created
 */
package org.knime.filehandling.utility.nodes.permissions;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilter;
import org.knime.filehandling.core.data.location.FSLocationValue;

/**
 * The node dialog for the {@link SetPosixPermissionsNodeModel} node.
 *
 * @author Alexander Bondaletov
 */
public class SetPosixPermissionsNodeDialog extends NodeDialogPane {

    private final SetPosixPermissionsNodeSettings m_settings;

    private final DialogComponentColumnNameSelection m_colSelection;

    private final DialogComponentBoolean m_failIfFilePathNotExists;

    private final DialogComponentBoolean m_failIfSetPermissionsFails;

    private Map<PosixFilePermission, JCheckBox> m_permissions;

    SetPosixPermissionsNodeDialog(final PortsConfiguration portsConfiguration) {
        m_settings = new SetPosixPermissionsNodeSettings();

        final ColumnFilter filter = new ColumnFilter() {
            @Override
            public final boolean includeColumn(final DataColumnSpec colSpec) {
                return colSpec.getType().isCompatible(FSLocationValue.class);
            }

            @Override
            public final String allFilteredMsg() {
                return "No applicable column available";
            }
        };

        m_colSelection = new DialogComponentColumnNameSelection(m_settings.getSelectedColumnModel(), "Column name",
            portsConfiguration.getInputPortLocation()
                .get(SetPosixPermissionsNodeFactory.DATA_TABLE_INPUT_PORT_GRP_NAME)[0],
            true, filter);
        m_colSelection
            .setToolTipText("Select the column containing the files/folders whose permissions must be changed.");

        m_failIfFilePathNotExists =
            new DialogComponentBoolean(m_settings.getFailIfFileDoesNotExistModel(), "Fail if file/folder does not exist");

        m_failIfSetPermissionsFails = new DialogComponentBoolean(m_settings.getFailIfSetPermissionsFailsModel(),
            "Fail if setting POSIX permissions on a file/folder fails");

        m_permissions = new EnumMap<>(PosixFilePermission.class);
        for (PosixFilePermission p : PosixFilePermission.values()) {
            m_permissions.put(p, createPermissionCheckBox(p));
        }

        addTab("Settings", createSettingsTab());
    }

    private JCheckBox createPermissionCheckBox(final PosixFilePermission permission) {
        final var cb = new JCheckBox();
        cb.addActionListener(e -> {
            if (cb.isSelected()) {
                m_settings.getPermissions().add(permission);
            } else {
                m_settings.getPermissions().remove(permission);
            }
        });
        return cb;
    }

    private JComponent createSettingsTab() {
        final var panel = new JPanel(new GridBagLayout());

        final var gbc = new GridBagConstraints();
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridy = 0;
        gbc.insets = new Insets(3, 3, 0, 0);
        panel.add(m_colSelection.getComponentPanel(), gbc);

        gbc.insets = new Insets(0, 0, 0, 0);
        ++gbc.gridy;
        panel.add(m_failIfFilePathNotExists.getComponentPanel(), gbc);

        ++gbc.gridy;
        panel.add(m_failIfSetPermissionsFails.getComponentPanel(), gbc);

        ++gbc.gridy;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        panel.add(createPermissionsPanel(), gbc);

        return panel;
    }

    private JComponent createPermissionsPanel() {
        final var panel = new JPanel(new GridBagLayout());

        final var gbc = new GridBagConstraints();
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Owner"), gbc);
        gbc.gridy += 1;
        panel.add(new JLabel("Group"), gbc);
        gbc.gridy += 1;
        panel.add(new JLabel("Others"), gbc);

        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1;
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(new JLabel("Read"), gbc);
        gbc.gridx += 1;
        panel.add(new JLabel("Write"), gbc);
        gbc.gridx += 1;
        panel.add(new JLabel("Execute/Browse"), gbc);

        final var permissions = new PosixFilePermission[][]{
            {PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE}, //
            {PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE}, //
            {PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE} //
        };
        for (var i = 0; i < 3; i++) {
            for (var j = 0; j < 3; j++) {
                gbc.gridx = j + 1;
                gbc.gridy = i + 1;
                panel.add(m_permissions.get(permissions[i][j]), gbc);
            }
        }

        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createHorizontalBox(), gbc);

        panel.setBorder(BorderFactory.createTitledBorder("Permissions"));

        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            m_settings.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {// NOSONAR
            // ignore
        }

        m_colSelection.loadSettingsFrom(settings, specs);

        for (Entry<PosixFilePermission, JCheckBox> e : m_permissions.entrySet()) {
            e.getValue().setSelected(m_settings.getPermissions().contains(e.getKey()));
        }
    }
}
