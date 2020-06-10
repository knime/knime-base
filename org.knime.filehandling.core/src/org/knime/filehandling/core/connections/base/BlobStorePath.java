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
 *   20.01.2020 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.base;

import java.nio.file.Path;
import java.util.List;

/**
 * Base implementation for blob store paths.
 *
 * @author Mareike Hoeger, KNIME GmbH
 * @since 4.2
 */
public abstract class BlobStorePath extends UnixStylePath {

    private final boolean m_isDirectory;

    /**
     * Creates a BlobStorePath from the given path name components.
     *
     * Paths that end with a path separator, or contain a relative notation element (like "{@code ..}", "{@code .}" or the empty string)
     * as the last component are considered to be directories. This behavior can be adapted by
     * overriding the {@link #lastComponentUsesRelativeNotation()} method.
     *
     * @param fileSystem the file system.
     * @param first The first name component.
     * @param more More name components.
     */
    public BlobStorePath(final BaseFileSystem<?> fileSystem, final String first, final String[] more) {
        super(fileSystem, first, more);
        m_isDirectory =
            concatenatePathSegments(fileSystem.getSeparator(), first, more).endsWith(fileSystem.getSeparator())
                || lastComponentUsesRelativeNotation();
    }

    /**
     * Creates a BlobStorePath from the given bucket name and object name.
     *
     * Paths that contain a symbolic link (like "{@code ..}" for the parent folder, or "{@code .}" and empty string for
     * the current folder) as the last component are considered to be directories. This behavior can be adapted by
     * overriding the {@link #lastComponentUsesRelativeNotation()} method.
     *
     * @param fileSystem the file system
     * @param bucketName the bucket
     * @param blobName the object key
     */
    public BlobStorePath(final BaseFileSystem<?> fileSystem, final String bucketName, final String blobName) {
        super(fileSystem, fileSystem.getSeparator() + bucketName + fileSystem.getSeparator() + blobName);
        m_isDirectory = blobName.endsWith(fileSystem.getSeparator()) || lastComponentUsesRelativeNotation();
    }

    /**
     * @return whether the last component is considered to by a symbolic link e.g. "{@code ..}" for the parent folder.
     */
    protected boolean lastComponentUsesRelativeNotation() {
        final String lastComponent = m_pathParts.get(m_pathParts.size() - 1);
        return lastComponent.equals(".") || lastComponent.equals("..") || lastComponent.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path subpath(final int beginIndex, final int endIndex) {

        if (beginIndex >= endIndex || beginIndex < 0 || beginIndex >= m_pathParts.size()
            || endIndex > m_pathParts.size()) {
            throw new IllegalArgumentException();
        }

        String pathString = String.join(m_pathSeparator, m_pathParts.subList(beginIndex, endIndex));
        if (endIndex < m_pathParts.size() || isDirectory()) {
            pathString = pathString + m_pathSeparator;
        }

        return getFileSystem().getPath(pathString);
    }

    /**
     * @return the bucket/container name
     */
    public String getBucketName() {
        if (!isAbsolute()) {
            throw new IllegalStateException("Bucket name cannot be determined for relative paths.");
        }
        if (m_pathParts.isEmpty()) {
            return null;
        }
        return m_pathParts.get(0);
    }

    /**
     * @return the name of the object i.e. the path without the bucket name.
     */
    public String getBlobName() {
        if (!isAbsolute()) {
            throw new IllegalStateException("Blob name cannot be determined for relative paths.");
        }
        if (m_pathParts.size() <= 1) {
            return null;
        } else {
            return subpath(1, getNameCount()).toString();
        }
    }

    /**
     * @return whether the path is a directory
     */
    public boolean isDirectory() {
        return m_isDirectory;
    }

    /**
     * @return a {@link BlobStorePath} for which {@link #isDirectory()} returns true.
     */
    @SuppressWarnings("resource")
    public BlobStorePath toDirectoryPath() {
        if (!isDirectory()) {
            return (BlobStorePath)getFileSystem().getPath(toString(), getFileSystem().getSeparator());
        } else {
            return this;
        }
    }

    @Override
    public String toString() {
        String toString = super.toString();

        if (isDirectory() && !isRoot() && !isEmptyPath()) {
            toString = toString.concat(m_pathSeparator);
        }

        return toString;
    }

    @Override
    public Path getFileName() {
        if (m_pathParts.isEmpty()) {
            return null;
        }

        String name = m_pathParts.get(m_pathParts.size() - 1);
        if (name.isEmpty()) {
            return this;
        }

        if (isDirectory()) {
            name = name.concat(m_pathSeparator);
        }

        return getFileSystem().getPath(name);
    }

    @Override
    public Path getName(final int index) {
        if (index < 0 || index >= m_pathParts.size()) {
            throw new IllegalArgumentException();
        }
        String name = m_pathParts.get(index);
        if (index < m_pathParts.size() - 1 || isDirectory()) {
            //unless it is the last element and not a directory add separator
            name = name.concat(m_pathSeparator);
        }
        return getFileSystem().getPath(name);
    }

    @Override
    public Path normalize() {
        if (m_pathParts.isEmpty()) {
            return this;
        }

        final List<String> normalized = getNormalizedPathParts();

        if (isDirectory() && !normalized.isEmpty()) {
            normalized.add(m_pathSeparator);
        }
        if (normalized.isEmpty() && !m_isAbsolute) {
            return getFileSystem().getPath(".");
        }

        //Ensure absolute paths stay absolute
        final String first = m_isAbsolute ? m_pathSeparator : "";

        return getFileSystem().getPath(first, normalized.toArray(new String[normalized.size()]));
    }
}
