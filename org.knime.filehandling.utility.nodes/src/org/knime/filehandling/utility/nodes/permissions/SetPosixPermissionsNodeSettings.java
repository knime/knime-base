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
 *   Nov 10, 2021 (Alexander Bondaletov): created
 */
package org.knime.filehandling.utility.nodes.permissions;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node settings for the {@link SetPosixPermissionsNodeModel} node.
 *
 * @author Alexander Bondaletov
 */
public class SetPosixPermissionsNodeSettings {

    private static final String KEY_SELECTED_COLUMN = "column_selection";

    private static final String KEY_FAIL_IF_FILE_DOES_NOT_EXIST = "fail_if_file_does_not_exist";

    private static final String KEY_FAIL_IF_SET_PERMISSIONS_FAILS = "fail_if_set_permission_fails";

    private static final String KEY_PERMISSIONS = "permissions";

    private final SettingsModelString m_column;

    private final SettingsModelBoolean m_failIfFileDoesNotExist;

    private final SettingsModelBoolean m_failIfSetPermissionsFails;

    private final Set<PosixFilePermission> m_permissions;

    /**
     * Creates new instance
     */
    public SetPosixPermissionsNodeSettings() {
        m_column = new SettingsModelString(KEY_SELECTED_COLUMN, "");
        m_failIfFileDoesNotExist = new SettingsModelBoolean(KEY_FAIL_IF_FILE_DOES_NOT_EXIST, true);
        m_failIfSetPermissionsFails = new SettingsModelBoolean(KEY_FAIL_IF_SET_PERMISSIONS_FAILS, true);
        m_permissions = new HashSet<>();
    }

    /**
     * @return the selected column model
     */
    public SettingsModelString getSelectedColumnModel() {
        return m_column;
    }

    /**
     * @return the selected column
     */
    public String getColumn() {
        return m_column.getStringValue();
    }

    /**
     * @return the model for whether to fail if the file does not exist
     */
    public SettingsModelBoolean getFailIfFileDoesNotExistModel() {
        return m_failIfFileDoesNotExist;
    }

    /**
     * @return whether to fail if the file does not exist.
     */
    public boolean failIfFileDoesExist() {
        return m_failIfFileDoesNotExist.getBooleanValue();
    }

    /**
     * @return whether to fail if setting permissions on the file fails
     */
    public boolean failIfSetPermissionsFails() {
        return m_failIfSetPermissionsFails.getBooleanValue();
    }

    /**
     * @return the model for whether to fail if setting permissions on the file fails
     */
    public SettingsModelBoolean getFailIfSetPermissionsFailsModel() {
        return m_failIfSetPermissionsFails;
    }


    /**
     * @return the permissions
     */
    public Set<PosixFilePermission> getPermissions() {
        return m_permissions;
    }

    /**
     * Saves current settings into the provided {@link NodeSettingsRO}.
     *
     * @param settings The node settings
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_column.saveSettingsTo(settings);
        m_failIfFileDoesNotExist.saveSettingsTo(settings);
        m_failIfSetPermissionsFails.saveSettingsTo(settings);

        final var permissions =
            m_permissions.stream() //
                .map(PosixFilePermission::name) //
                .collect(Collectors.toList()) //
                .toArray(String[]::new);
        settings.addStringArray(KEY_PERMISSIONS, permissions);
    }

    /**
     * Validates the settings stored in the provided {@link NodeSettingsRO}.
     *
     * @param settings The node settings.
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_column.validateSettings(settings);
        m_failIfFileDoesNotExist.validateSettings(settings);
        m_failIfSetPermissionsFails.validateSettings(settings);
        settings.getStringArray(KEY_PERMISSIONS);

        final var temp = new SetPosixPermissionsNodeSettings();
        temp.loadSettingsFrom(settings);
        temp.validate();
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (m_column.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("Column is not selected");
        }
    }

    /**
     * Loads the settings from the provided {@link NodeSettingsRO}.
     *
     * @param settings The node settings
     * @throws InvalidSettingsException
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_column.loadSettingsFrom(settings);
        m_failIfFileDoesNotExist.loadSettingsFrom(settings);
        m_failIfSetPermissionsFails.loadSettingsFrom(settings);

        final var permissions = settings.getStringArray(KEY_PERMISSIONS);
        m_permissions.clear();
        Arrays.stream(permissions) //
            .map(PosixFilePermission::valueOf) //
            .forEach(m_permissions::add);
    }
}
