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
 *   28.08.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.base.attributes;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashSet;
import java.util.Set;

/**
 * This class provides POSIX file attributes for the {@link BaseFileAttributes}.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class PosixAttributes {

    private final UserPrincipal m_owner;

    private final GroupPrincipal m_group;

    private final Set<PosixFilePermission> m_permissions;

    /**
     * Constructs a {@link PosixAttributes} class with the given owner, group and permissions.
     *
     * @param owner the owner of the files
     * @param group the group of the file
     * @param permissions the permissions to the file
     */
    public PosixAttributes(final UserPrincipal owner, final GroupPrincipal group,
        final Set<PosixFilePermission> permissions) {
        m_owner = owner;
        m_group = group;
        m_permissions = new HashSet<>(permissions);
    }

    /**
     * @return the file owner
     */
    public UserPrincipal owner() {
        return m_owner;
    }

    /**
     * @return the file group owner
     */
    public GroupPrincipal group() {
        return m_group;
    }

    /**
     * Returns the permissions of the file. The file permissions are returned
     * as a set of {@link PosixFilePermission} elements. The returned set is a
     * copy of the file permissions and is modifiable. This allows the result
     * to be modified and passed to the {@link PosixFileAttributeView#setPermissions
     * setPermissions} method to update the file's permissions.
     *
     * @return  the file permissions
     */
    public Set<PosixFilePermission> permissions() {
        return new HashSet<>(m_permissions);
    }

}
