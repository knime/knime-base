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
 *   Sep 13, 2021 (bjoern): created
 */
package org.knime.filehandling.core.connections.base.attributes;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.base.BaseFileSystem;

/**
 * Abstract base implementation of a {@link PosixFileAttributeView}. This class is meant to be extended by classes that
 * offer the capability to get/set POSIX file attributes.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @param <P> the path type
 * @param <F> the base file system type
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class BasePosixFileAttributeView<P extends FSPath, F extends BaseFileSystem<P>>
    implements PosixFileAttributeView {

    private final P m_path;

    private final LinkOption[] m_linkOptions;

    /**
     * Constructs a file attribute view for a path.
     *
     * @param path the path this attributes view belongs to.
     * @param linkOptions whether to following symbolic links or not.
     */
    protected BasePosixFileAttributeView(final P path, final LinkOption[] linkOptions) {
        m_path = path;
        m_linkOptions = linkOptions;
    }

    /**
     * @return the path this attributes view belongs to.
     */
    protected final P getPath() {
        return m_path;
    }

    /**
     * @return the underlying file system.
     */
    @SuppressWarnings("unchecked")
    protected final F getFileSystem() {
        return (F)m_path.getFileSystem();
    }

    /**
     * Clears the file system attribute cache from file attributes for the path this attributes view belongs to.
     */
    @SuppressWarnings("resource")
    protected void clearAttributeCache() {
        getFileSystem().removeFromAttributeCache(m_path);
    }

    @Override
    public final UserPrincipal getOwner() throws IOException {
        return readAttributes().owner();
    }

    @Override
    public final String name() {
        return "posix";
    }

    @SuppressWarnings("resource")
    @Override
    public final PosixFileAttributes readAttributes() throws IOException {
        return getFileSystem().provider().readAttributes(m_path, PosixFileAttributes.class, m_linkOptions);
    }

    @Override
    public final void setTimes(final FileTime lastModifiedTime, final FileTime lastAccessTime,
        final FileTime createTime) throws IOException {
        setTimesInternal(lastModifiedTime, lastAccessTime, createTime);
        clearAttributeCache();
    }

    /**
     * See {@link #setTimes(FileTime, FileTime, FileTime)}.
     *
     * @param lastModifiedTime The new last modified time, or null to not change the value.
     * @param lastAccessTime The new last access time, or null to not change the value.
     * @param createTime The new last creation time, or null to not change the value.
     * @throws IOException If an I/O error occurs.
     */
    protected abstract void setTimesInternal(final FileTime lastModifiedTime, final FileTime lastAccessTime,
        final FileTime createTime) throws IOException;

    @Override
    public final void setOwner(final UserPrincipal owner) throws IOException {
        setOwnerInternal(owner);
        clearAttributeCache();
    }

    /**
     * See {@link #setOwner(UserPrincipal)}.
     *
     * @param owner The new file owner
     * @throws IOException If an I/O error occurs
     */
    protected abstract void setOwnerInternal(UserPrincipal owner) throws IOException;

    @Override
    public final void setPermissions(final Set<PosixFilePermission> perms) throws IOException {
        setPermissionsInternal(perms);
        clearAttributeCache();
    }

    /**
     * See {@link #setPermissions(Set)}.
     *
     * @param perms The new set of permissions.
     * @throws IOException If an I/O error occurs.
     */
    protected abstract void setPermissionsInternal(Set<PosixFilePermission> perms) throws IOException;

    @Override
    public void setGroup(final GroupPrincipal group) throws IOException {
        setGroupInternal(group);
        clearAttributeCache();
    }

    /**
     * See {@link #setGroup(GroupPrincipal)}.
     *
     * @param group The new file group-owner.
     * @throws IOException If an I/O error occurs.
     */
    protected abstract void setGroupInternal(GroupPrincipal group) throws IOException;
}
