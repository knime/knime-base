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
 *   Apr 24, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections;

import java.util.Optional;

import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;

/**
 * Interface that provides information about the kind of file system that a {@link FSLocation} requires.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @see FSLocation
 * @since 4.2
 */
public interface FSLocationSpec {

    /**
     * {@code NULL} instance.
     */
    public static FSLocationSpec NULL = new FSLocationSpec() {

        @Override
        public String getFileSystemType() {
            return null;
        }

        @Override
        public Optional<String> getFileSystemSpecifier() {
            return Optional.empty();
        }

    };

    /**
     * Returns the file system type.
     *
     * @return the file system type
     */
    String getFileSystemType();

    /**
     * Returns the {@link Choice file system choice}.
     *
     * @return the file system choice
     */
    default Choice getFileSystemChoice() {
        return FileSystemChoice.Choice.valueOf(getFileSystemType());
    }

    /**
     * Returns the optional file system specifier.
     *
     * @return the file system specifier
     */
    Optional<String> getFileSystemSpecifier();

    @Override
    String toString();

    /**
     * Checks if the provided {@link FSLocationSpec} objects are equal regarding there type and specifier.
     *
     * @param first {@link FSLocationSpec}
     * @param second {@link FSLocationSpec}
     * @return {@code true} if either both are {@code null} or have the same type and specifier
     */
    static boolean areEqual(final FSLocationSpec first, final FSLocationSpec second) {
        if (first == second) {
            // both are identical (including null)
            return true;
        }
        if (first == null || second == null) {
            // both can't be null because then the previous if-switch would have returned already
            return false;
        }
        return first.getFileSystemType().equals(second.getFileSystemType())
            && first.getFileSystemSpecifier().equals(second.getFileSystemSpecifier());
    }
}
