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
 *   Mar 6, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Optional;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;

/**
 * Abstract super class implemented by all NIO file systems provided in KNIME. This class adds the following features on
 * top of the vanilla NIO {@link FileSystem}:
 *
 * <ul>
 * <li>Usage of generics (makes file systems more convenient to implement)</li>
 * <li>Support for a working directory (see {@link #getWorkingDirectory()})</li>
 * <li>Support for {@link FSLocation} (see {@link #getPath(FSLocation)} and
 * {@link #checkCompatibility(FSLocationSpec)})</li>
 * </ul>
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @param <T> The type of path that this file system works with.
 * @since 4.2
 */
public abstract class FSFileSystem<T extends FSPath> extends FileSystem {

    /**
     * The {@link FSLocationSpec} that describes this file system.
     */
    private final FSLocationSpec m_fsLocationSpec;

    /**
     * The working directory, which allows the file system provider methods to resolve paths to absolute ones.
     */
    private final String m_workingDirectory;

    /**
     * Creates a new instance.
     *
     * @param fsLocationSpec An {@link FSLocationSpec} that characterizes this file system.
     * @param workingDir The working directory to use (see {@link #getWorkingDirectory()}).
     */
    public FSFileSystem(final FSLocationSpec fsLocationSpec, final String workingDir) {
        m_fsLocationSpec = fsLocationSpec;
        m_workingDirectory = workingDir;
    }

    /**
     * Returns the {@link Choice file system choice}.
     *
     * @return the file system choice.
     */
    public final Choice getFileSystemChoice() {
        return m_fsLocationSpec.getFileSystemChoice();
    }

    /**
     * Returns the optional file system specifier.
     *
     * @return the file system specifier.
     */
    public final Optional<String> getFileSystemSpecifier() {
        return m_fsLocationSpec.getFileSystemSpecifier();
    }

    /**
     * Does nothing, since a file system must only be closed by the connection node that instantiated it. Nodes that
     * only *use* a file system should invoke {@link FSConnection#close()} on the respective {@link FSConnection} object
     * to release any blocked resources.
     */
    @Override
    public final void close() throws IOException {
        // do nothing
    }

    /**
     * Actually closes this file system and releases any blocked resources (streams, etc). This method must only be
     * called by the connection node, which has control of the file system lifecycle (hence the reduced visibility).
     * Implementations are free to increase method visibility for their purposes.
     *
     * @throws IOException when something went wrong while closing the file system.
     */
    protected abstract void ensureClosed() throws IOException;

    /**
     * Each file system has a working directory, aka current directory. The working directory allows users of the file
     * system to supply relative paths to many of the provider methods, e.g. to open an input stream. The working
     * directory will be used to resolve such relative paths to absolute ones. The working directory of a file system
     * instance is final and does not change over the lifetime of the file system.
     *
     * @return the working directory, aka current directory.
     */
    public T getWorkingDirectory() {
        return getPath(m_workingDirectory);
    }

    /**
     * Checks whether this this file system instance is compatible with {@link FSLocation} objects that have the given
     * {@link FSLocationSpec}.
     *
     * @param fsLocationSpec The {@link FSLocationSpec} to check for compatibility.
     */
    public void checkCompatibility(final FSLocationSpec fsLocationSpec) {
        CheckUtils.checkArgument(
            fsLocationSpec.getFileSystemType() != null && fsLocationSpec.getFileSystemChoice() == getFileSystemChoice(),
            String.format("Only FSLocations of type %s are allowed with this file system.", getFileSystemChoice()));

        CheckUtils.checkArgument(
            getFileSystemSpecifier().equals(fsLocationSpec.getFileSystemSpecifier()),
            String.format("Only FSLocations with specifier %s are allowed with this file system.", getFileSystemSpecifier()));
    }

    /**
     * Converts the given {@link FSLocation} to a path object.
     *
     * @param fsLocation The {@link FSLocation} to convert.
     * @return the path object.
     */
    public T getPath(final FSLocation fsLocation) {
        checkCompatibility(fsLocation);
        return getPath(fsLocation.getPath());
    }

    @Override
    public abstract FSFileSystemProvider<T, ?> provider();

    @Override
    public abstract T getPath(String first, String... more);
}
