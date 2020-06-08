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
 *   May 24, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is the FS*-specific companion class of {@link Files}, i.e. it consists exclusively of static methods that
 * operate on {@link FSFileSystem}s and {@link FSPath}s.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @since 4.2
 */
public final class FSFiles {

    private FSFiles() {
    }

    @SuppressWarnings({"unchecked", "resource"})
    private static FSFileSystemProvider<FSPath, FSFileSystem<FSPath>> provider(final FSPath path) {
        return (FSFileSystemProvider<FSPath, FSFileSystem<FSPath>>)path.getFileSystem().provider();
    }

    /**
     * Creates a new temporary directory in the working directory of the given {@link FSFileSystem}. The temp directory
     * and everything in it will be automatically deleted when the given {@link FSFileSystem} is closed.
     *
     * @param fileSystem The file system in whose working directory to create the temp directory.
     * @return the path of the newly created temporary directory.
     *
     * @throws NoSuchFileException when the parent directory does not exist.
     * @throws IOException When something else went wrong while creating the directory.
     */
    public static FSPath createTempDirectory(final FSFileSystem<? extends FSPath> fileSystem) throws IOException {
        return createTempDirectory(fileSystem.getWorkingDirectory());
    }

    /**
     * Creates a new temporary directory in the given parent directory. The temp directory and everything in it will be
     * automatically deleted when the underlying {@link FSFileSystem} is closed.
     *
     * @param parent The parent directory in which to create the temp directory.
     * @return the path of the newly created temporary directory.
     *
     * @throws NoSuchFileException when the parent directory does not exist.
     * @throws IOException When something else went wrong while creating the directory.
     */
    public static FSPath createTempDirectory(final FSPath parent) throws IOException {
        return createTempDirectory(parent, "knimetmp-", "");
    }

    /**
     * Creates a new temporary directory in the given parent directory. The directory and everything in it will be
     * automatically deleted when the underlying {@link FSFileSystem} is closed.
     *
     * @param parent The parent directory in which to create the temp directory.
     * @param prefix A string prefix for the directory name.
     * @param suffix A string suffix for the directory name.
     * @return the path of the newly created temporary directory.
     *
     * @throws NoSuchFileException when the parent directory does not exist.
     * @throws IOException When something else went wrong while creating the directory.
     */
    public static FSPath createTempDirectory(final FSPath parent, final String prefix, final String suffix)
        throws IOException {
        return provider(parent).createTempDirectory(parent, prefix, suffix);
    }

    /**
     * Creates a new temporary file in the working directory of the given {@link FSFileSystem}. The temp file will be
     * automatically deleted when the given {@link FSFileSystem} is closed.
     *
     * @param fileSystem The file system in whose working directory to create the temp file.
     * @return the path of the newly created temporary file.
     *
     * @throws NoSuchFileException when the parent directory does not exist.
     * @throws IOException When something else went wrong while creating the temp file.
     */
    public static FSPath createTempFile(final FSFileSystem<? extends FSPath> fileSystem) throws IOException {
        return createTempFile(fileSystem.getWorkingDirectory());
    }

    /**
     * Creates a new temporary file in the given parent directory. The temp file will be automatically deleted when the
     * underlying {@link FSFileSystem} is closed.
     *
     * @param parent The parent directory in which to create the temp file.
     * @return the path of the newly created temporary file.
     *
     * @throws NoSuchFileException when the parent directory does not exist.
     * @throws IOException When something else went wrong while creating the temp file.
     */
    public static FSPath createTempFile(final FSPath parent) throws IOException {
        return createTempFile(parent, "knime-", ".tmp");
    }

    /**
     * Creates a new temporary file in the given parent directory. The file will be automatically deleted when the
     * underlying {@link FSFileSystem} is closed.
     *
     * @param parent The parent directory in which to create the temp file.
     * @param prefix A string prefix for the file name.
     * @param suffix A string suffix for the file name.
     * @return the path of the newly created temp file.
     *
     * @throws NoSuchFileException when the parent directory does not exist.
     * @throws IOException When something else went wrong while creating the temp file.
     */
    public static FSPath createTempFile(final FSPath parent, final String prefix, final String suffix)
        throws IOException {
        return provider(parent).createTempFile(parent, prefix, suffix);
    }

    /**
     * Recursively deletes the given directory. When deletion of any of the contained files or directories fails, then
     * recursive deletion will continue, but the exception will be thrown at the end as an {@link IOException}. If the
     * deletion of multiple files fails, then only the exception of the first failed deletion will be thrown.
     *
     * @param toDelete The directory to delete.
     * @throws IOException When something went wrong while recursively listing the files, or the exception of the first
     *             failed deletion.
     */
    public static void deleteRecursively(final Path toDelete) throws IOException {
        AtomicReference<IOException> ioeRef = new AtomicReference<>();
        Files.walk(toDelete).sorted(Comparator.reverseOrder()).forEach((p) -> deleteSafely(p, ioeRef));

        if (ioeRef.get() != null) {
            throw ioeRef.get();
        }
    }

    /**
     * Deletes the given file or directory (must be empty for successful deletion). Any exceptions occuring during
     * deletion will be silently ignored.
     *
     * @param toDelete The file or (empty) directory to delete.
     */
    public static void deleteSafely(final Path toDelete) {
        deleteSafely(toDelete, null);
    }

    private static void deleteSafely(final Path toDelete, final AtomicReference<IOException> ioeRef) {
        try {
            Files.deleteIfExists(toDelete);
        } catch (Exception e) {
            if (ioeRef != null) {
                if (e instanceof IOException) {
                    ioeRef.compareAndSet(null, (IOException)e);
                } else {
                    ioeRef.compareAndSet(null, new IOException(e));
                }
            }
        }
    }
}
