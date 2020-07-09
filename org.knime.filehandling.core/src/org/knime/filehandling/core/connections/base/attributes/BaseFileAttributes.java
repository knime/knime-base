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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;
import java.util.function.Function;

import org.knime.filehandling.core.util.CheckedExceptionFunction;

/**
 * Class for file attributes that uses {@link Function} implementations to generate the necessary metadata lazy
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noextend non-public API
 * @noinstantiate non-public API
 */
public class BaseFileAttributes implements PosixFileAttributes {

    private final boolean m_isRegularFile;

    private final Path m_fileKey;

    private final FileTime m_lastModifiedTime;

    private final FileTime m_lastAccessTime;

    private final FileTime m_creationTime;

    private final long m_size;

    private final boolean m_isSymbolicLink;

    private final boolean m_isOther;

    private final UserPrincipal m_owner;

    private final GroupPrincipal m_group;

    private final Set<PosixFilePermission> m_permissions;

    private final CheckedExceptionFunction<Path, PosixAttributes, IOException> m_posixMetaDataFunction;

    private final boolean m_hasPosixAttributesSet;

    private final long m_fetchTime;

    /**
     * Constructor for BasicAttributes with lazy fetched {@link PosixFileAttributes}.
     *
     * @param isRegularFile whether this path represents a regular file
     * @param fileKey the Path of the file
     * @param lastModifiedTime last modified time of the file
     * @param lastAccessTime last access time of the file
     * @param creationTime creation time of the file
     * @param size size of the file
     * @param isSymbolicLink whether file is a symbolic link
     * @param isOther whether file is other
     * @param posixAttributesFunction the function to fetch POSIX metadata
     */
    public BaseFileAttributes(final boolean isRegularFile, final Path fileKey, final FileTime lastModifiedTime,
        final FileTime lastAccessTime, final FileTime creationTime, final long size, final boolean isSymbolicLink,
        final boolean isOther,
        final CheckedExceptionFunction<Path, PosixAttributes, IOException> posixAttributesFunction) {
        m_isRegularFile = isRegularFile;
        m_fileKey = fileKey;
        m_lastModifiedTime = lastModifiedTime;
        m_lastAccessTime = lastAccessTime;
        m_creationTime = creationTime;
        m_size = size;
        m_isSymbolicLink = isSymbolicLink;
        m_isOther = isOther;
        m_owner = null;
        m_group = null;
        m_permissions = null;
        m_posixMetaDataFunction = posixAttributesFunction;
        m_hasPosixAttributesSet = false;
        m_fetchTime = System.currentTimeMillis();
    }

    /**
     * Constructor for {@link PosixFileAttributes}.
     *
     * @param isRegularFile whether this path represents a regular file
     * @param fileKey the Path of the file
     * @param lastModifiedTime last modified time of the file
     * @param lastAccessTime last access time of the file
     * @param creationTime creation time of the file
     * @param size size of the file
     * @param isSymbolicLink whether file is a symbolic link
     * @param isOther whether file is other
     * @param owner the owner of the files
     * @param group the group of the file
     * @param permissions the permissions to the file
     */
    public BaseFileAttributes(final boolean isRegularFile, final Path fileKey, final FileTime lastModifiedTime,
        final FileTime lastAccessTime, final FileTime creationTime, final long size, final boolean isSymbolicLink,
        final boolean isOther, final UserPrincipal owner, final GroupPrincipal group,
        final Set<PosixFilePermission> permissions) {
        m_isRegularFile = isRegularFile;
        m_fileKey = fileKey;
        m_lastModifiedTime = lastModifiedTime;
        m_lastAccessTime = lastAccessTime;
        m_creationTime = creationTime;
        m_size = size;
        m_isSymbolicLink = isSymbolicLink;
        m_isOther = isOther;
        m_owner = owner;
        m_group = group;
        m_permissions = permissions;
        m_posixMetaDataFunction = null;
        m_hasPosixAttributesSet = true;
        m_fetchTime = System.currentTimeMillis();
    }

    /**
     * @return lastModifiedTime
     */
    @Override
    public FileTime lastModifiedTime() {
        return m_lastModifiedTime;
    }

    /**
     * @return lastAccessTime
     */
    @Override
    public FileTime lastAccessTime() {
        return m_lastAccessTime;
    }

    /**
     * @return m_creationTime
     */
    @Override
    public FileTime creationTime() {
        return m_creationTime;
    }

    /**
     * @return isSymbolicLink
     */
    @Override
    public boolean isSymbolicLink() {
        return m_isSymbolicLink;
    }

    /**
     * @return isOther
     */
    @Override
    public boolean isOther() {
        return m_isOther;
    }

    /**
     * @return size
     */
    @Override
    public long size() {
        return m_size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegularFile() {
        return m_isRegularFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() {
        return !m_isRegularFile && !(m_isSymbolicLink || m_isOther);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object fileKey() {
        return m_fileKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserPrincipal owner() {
        return m_owner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GroupPrincipal group() {
        return m_group;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<PosixFilePermission> permissions() {
        return m_permissions;
    }

    /**
     * Generates a BaseFileAttribute with POSIX attributes. If this object already contains the POSIX attributes the
     * object is return.
     *
     * @return BaseFileAttribues with POSIX attributes
     * @throws IOException
     */
    public synchronized BaseFileAttributes generatePosixAttributes() throws IOException {

        if (!hasPosixAttributesSet()) {
            if (m_posixMetaDataFunction == null) {
                throw new UnsupportedOperationException("POSIX attributes not supported");
            }
            final PosixAttributes posixAttributes = m_posixMetaDataFunction.apply(m_fileKey);

            return new BaseFileAttributes(m_isRegularFile, m_fileKey, m_lastModifiedTime, m_lastAccessTime,
                m_creationTime, m_size, m_isSymbolicLink, m_isOther, posixAttributes.owner(), posixAttributes.group(),
                posixAttributes.permissions());
        }
        return this;
    }

    /**
     * @return the fetchTime
     */
    public long getFetchTime() {
        return m_fetchTime;
    }

    /**
     * @return whether the POSIX attributes have been set
     */
    public boolean hasPosixAttributesSet() {
        return m_hasPosixAttributesSet;
    }

}
