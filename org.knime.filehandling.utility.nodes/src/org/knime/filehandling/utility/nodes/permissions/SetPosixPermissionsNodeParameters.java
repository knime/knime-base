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
 * ------------------------------------------------------------------------
 */

package org.knime.filehandling.utility.nodes.permissions;

import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.base.node.io.filehandling.webui.FileSystemManagedByPortMessage;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.utility.nodes.permissions.SetPosixPermissionsNodeParameters.Permissions.PermissionRole;
import org.knime.filehandling.utility.nodes.permissions.SetPosixPermissionsNodeParameters.Permissions.PermissionRoleRef;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.message.TextMessage;

/**
 * Node parameters for Set Files/Folders Permissions.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class SetPosixPermissionsNodeParameters implements NodeParameters {

    @Section(title = "Permissions", description = """
            Select read/write/execute permissions for the owner/group/other categories.
            """)
    interface PermissionsSection {
    }

    @TextMessage(value = FileSystemManagedByPortMessage.class)
    Void m_fileSystemManagedByPortMessage;

    @Widget(title = "Column name",
        description = "Column containing the paths to the files/folders whose permissions must be edited.")
    @ChoicesProvider(FSLocationColumnProvider.class)
    @Persist(configKey = SetPosixPermissionsNodeSettings.KEY_SELECTED_COLUMN)
    @ValueProvider(SelectedColumnProvider.class)
    @ValueReference(SelectedColumnRef.class)
    String m_selectedColumn;

    static final class SelectedColumnRef implements ParameterReference<String> {
    }

    @Widget(title = "Fail if file/folder does not exist",
        description = "If checked, the node fails if a file/folder from the input table does not exist.")
    @Persist(
        configKey = SetPosixPermissionsNodeSettings.KEY_FAIL_IF_FILE_DOES_NOT_EXIST)
    boolean m_failIfFileDoesNotExist = true;

    @Widget(title = "Fail if setting POSIX permissions on a file/folder fails", description = """
            If checked, the node fails if there is a failure while setting POSIX permissions for a file/folder from
            the input table.
            """)
    @Persist(
        configKey = SetPosixPermissionsNodeSettings.KEY_FAIL_IF_SET_PERMISSIONS_FAILS)
    boolean m_failIfSetPermissionsFails = true;

    @Layout(PermissionsSection.class)
    @Persistor(PermissionsArrayPersistor.class)
    @ArrayWidget(hasFixedSize = true)
    @ArrayWidgetInternal(titleProvider = PermissionGroupTitleProvider.class)
    @Widget(title = "Permissions", description = "Set POSIX permissions for owner, group and others.")
    @ValueReference(PermissionsArrayRef.class)
    Permissions[] m_permissions = new Permissions[]{new Permissions(PermissionRole.OWNER),
        new Permissions(PermissionRole.GROUP), new Permissions(PermissionRole.OTHERS)};

    static final class PermissionsArrayRef implements ParameterReference<Permissions[]> {
    }

    static final class FSLocationColumnProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            return context //
                .getInTableSpec(FileSystemPortConnectionUtil.hasFileSystemPort(context) ? 1 : 0) //
                .map(spec -> spec.stream().filter(FSLocationColumnProvider::isIncluded)) //
                .orElseGet(Stream::empty) //
                .toList();
        }

        private static boolean isIncluded(final DataColumnSpec col) {
            return List.of(FSLocationValue.class).stream() //
                    .anyMatch(valueClass -> col.getType().isCompatible(valueClass));
        }

    }

    static final class SelectedColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected SelectedColumnProvider() {
            super(SelectedColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return parametersInput //
                    .getInTableSpec(FileSystemPortConnectionUtil.hasFileSystemPort(parametersInput) ? 1 : 0) //
                    .map(spec -> spec.stream().filter(SelectedColumnProvider::isIncluded)) //
                    .orElseGet(Stream::empty) //
                    .findFirst();
        }

        private static boolean isIncluded(final DataColumnSpec col) {
            return List.of(FSLocationValue.class).stream() //
                    .anyMatch(valueClass -> col.getType().isCompatible(valueClass));
        }

    }

    static final class PermissionGroupTitleProvider implements StateProvider<String> {

        private Supplier<PermissionRole> m_permissionRoleSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_permissionRoleSupplier = initializer.getValueSupplier(PermissionRoleRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput context) throws StateComputationFailureException {
            if (m_permissionRoleSupplier.get() == null) {
                throw new StateComputationFailureException();
            }
            return m_permissionRoleSupplier.get().getValue();
        }

    }

    static final class PermissionsArrayPersistor implements NodeParametersPersistor<Permissions[]> {

        @Override
        public Permissions[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var permStrings =
                settings.getStringArray(SetPosixPermissionsNodeSettings.KEY_PERMISSIONS, new String[0]);
            if (permStrings == null || permStrings.length == 0) {
                return new Permissions[]{
                    new Permissions(PermissionRole.OWNER),
                    new Permissions(PermissionRole.GROUP),
                    new Permissions(PermissionRole.OTHERS)};
            } else {
                return new Permissions[]{
                    new Permissions(PermissionRole.OWNER,
                        hasPermission(permStrings, PosixFilePermission.OWNER_READ),
                        hasPermission(permStrings, PosixFilePermission.OWNER_WRITE),
                        hasPermission(permStrings, PosixFilePermission.OWNER_EXECUTE)),
                    new Permissions(PermissionRole.GROUP,
                        hasPermission(permStrings, PosixFilePermission.GROUP_READ),
                        hasPermission(permStrings, PosixFilePermission.GROUP_WRITE),
                        hasPermission(permStrings, PosixFilePermission.GROUP_EXECUTE)),
                    new Permissions(PermissionRole.OTHERS,
                        hasPermission(permStrings, PosixFilePermission.OTHERS_READ),
                        hasPermission(permStrings, PosixFilePermission.OTHERS_WRITE),
                        hasPermission(permStrings, PosixFilePermission.OTHERS_EXECUTE))};
            }
        }

        private static boolean hasPermission(final String[] permStrings, final PosixFilePermission posixPermission) {
            for (String permString : permStrings) {
                if (posixPermission.name().equals(permString)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void save(final Permissions[] param, final NodeSettingsWO settings) {
            var ownerPermissions = getPermissions(param[0], new PosixFilePermission[]{PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE});
            var groupPermissions = getPermissions(param[1], new PosixFilePermission[]{PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE});
            var othersPermissions = getPermissions(param[2], new PosixFilePermission[]{PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE});

            var permStrings = new ArrayList<String>();
            permStrings.addAll(ownerPermissions);
            permStrings.addAll(groupPermissions);
            permStrings.addAll(othersPermissions);
            settings.addStringArray(SetPosixPermissionsNodeSettings.KEY_PERMISSIONS,
                permStrings.toArray(new String[0]));
        }

        private static List<String> getPermissions(final Permissions permissions,
            final PosixFilePermission[] permission) {
            final var permStrings = new ArrayList<String>();
            if (permissions.m_read) {
                permStrings.add(permission[0].name());
            }
            if (permissions.m_write) {
                permStrings.add(permission[1].name());
            }
            if (permissions.m_execute) {
                permStrings.add(permission[2].name());
            }
            return permStrings;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{SetPosixPermissionsNodeSettings.KEY_PERMISSIONS}};
        }

    }

    static final class Permissions implements NodeParameters {

        public Permissions() {
            this(null, false, false, false);
        }

        public Permissions(final PermissionRole role) {
            this(role, false, false, false);
        }

        public Permissions(final PermissionRole role, final boolean read, final boolean write, final boolean execute) {
            m_role = role;
            m_read = read;
            m_write = write;
            m_execute = execute;
        }

        @ValueReference(PermissionRoleRef.class)
        PermissionRole m_role;

        static final class PermissionRoleRef implements ParameterReference<PermissionRole> {
        }

        @Widget(title = "Read", description = """
                Allow the owner/group/others to read the file or list the directory contents.
                """)
        boolean m_read;

        @Widget(title = "Write", description = "Allow the owner/group/others to modify the file or directory.")
        boolean m_write;

        @Widget(title = "Execute/Browse", description = """
                Allow the owner/group/others to execute the file or browse the directory.
                """)
        boolean m_execute;

        enum PermissionRole {

            OWNER("Owner"), //
            GROUP("Group"), //
            OTHERS("Others"); //

            private String m_value;

            PermissionRole(final String value) {
                m_value = value;
            }

            String getValue() {
                return m_value;
            }

        }

    }

}
