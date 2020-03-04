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
 *   Feb 21, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.data.location.cell.FSLocationCell;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;

/**
 * Object encapsulation all information required to convert between {@link Path} and {@link FSLocationCell}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class FSLocation {

    /** The file system type. */
    private final String m_fileSystemType;

    /** The optional file system specifier. */
    private final Optional<String> m_fileSystemSpecifier;

    /** The actual path to the file/folder. */
    private final String m_path;

    /**
     * Constructor.
     *
     * @param fsType the file system type
     * @param path the path to the file/folder
     */
    public FSLocation(final String fsType, final String path) {
        this(fsType, null, path);
    }

    /**
     * Constructor.
     *
     * @param fsType the file system type
     * @param fsSpecifier the file system specifier, can be {@code null}.
     * @param path the path to the file/folder
     * @throws IllegalArgumentException if {@code fsType} or {@code path} is {@code null}
     */
    public FSLocation(final String fsType, final String fsSpecifier, final String path) {
        CheckUtils.checkArgumentNotNull(fsType, "The file system type must not be null.");
        CheckUtils.checkArgumentNotNull(fsType, "The path must not be null.");
        m_fileSystemType = fsType;
        m_fileSystemSpecifier = Optional.ofNullable(fsSpecifier);
        m_path = path;
    }

    /**
     * Returns the path to the file/folder.
     *
     * @return path to the file/folder.
     */
    public String getPath() {
        return m_path;
    }

    /**
     * Returns the optional file system specifier.
     *
     * @return the file system specifier
     */
    public Optional<String> getFileSystemSpecifier() {
        return m_fileSystemSpecifier;
    }

    /**
     * Returns the file system type.
     *
     * @return the file system type
     */
    public String getFileSystemType() {
        return m_fileSystemType;
    }

    /**
     * Returns the {@link Choice file system choice}.
     *
     * @return the file system choice
     */
    public Choice getFileSystemChoice() {
        return FileSystemChoice.Choice.valueOf(m_fileSystemType);
    }

    @Override
    public String toString() {
        return m_path;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + m_fileSystemSpecifier.map(s -> s.hashCode()).orElse(0);
        result = prime * result + ((m_fileSystemType == null) ? 0 : m_fileSystemType.hashCode());
        result = prime * result + ((m_path == null) ? 0 : m_path.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FSLocation fsLocation = (FSLocation)obj;
        return Objects.equals(m_fileSystemType, fsLocation.m_fileSystemType)
            && Objects.equals(m_fileSystemSpecifier, fsLocation.m_fileSystemSpecifier)
            && Objects.equals(m_path, fsLocation.m_path);
    }

}
