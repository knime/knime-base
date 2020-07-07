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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;

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

    /**
     * Wraps {@link Files#newInputStream(Path, OpenOption...)} and decorates {@link IOException IOExceptions} via
     * {@link ExceptionUtil#wrapIOException(IOException)}.
     *
     * @param path the path to the file to open
     * @param options options specifying how the file is opened
     *
     * @return a new input stream
     *
     * @throws IllegalArgumentException if an invalid combination of options is specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     * @throws IOException if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is installed, the
     *             {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read access to the
     *             file.
     */
    public static InputStream newInputStream(final Path path, final OpenOption... options) throws IOException {
        try {
            return Files.newInputStream(path, options);
        } catch (final IOException e) {
            throw ExceptionUtil.wrapIOException(e);
        }
    }

    /**
     * Wraps {@link Files#newOutputStream(Path, OpenOption...)} and decorates {@link IOException IOExceptions} via
     * {@link ExceptionUtil#wrapIOException(IOException)}.
     *
     * @param path the path to the file to open
     * @param options options specifying how the file is opened
     *
     * @return a new input stream
     * @throws IllegalArgumentException if an invalid combination of options is specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     * @throws IOException if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is installed, the
     *             {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read access to the
     *             file.
     */
    public static OutputStream newOutputStream(final Path path, final OpenOption... options) throws IOException {
        try {
            return Files.newOutputStream(path, options);
        } catch (final IOException e) {
            throw ExceptionUtil.wrapIOException(e);
        }
    }

    /**
     * Wraps {@link Files#createDirectories(Path, FileAttribute...)} and decorates {@link IOException IOExceptions} via
     * {@link ExceptionUtil#wrapIOException(IOException)}.
     *
     * @param dir the directory to create
     *
     * @param attrs an optional list of file attributes to set atomically when creating the directory
     *
     * @return the directory
     *
     * @throws UnsupportedOperationException if the array contains an attribute that cannot be set atomically when
     *             creating the directory
     * @throws FileAlreadyExistsException if {@code dir} exists but is not a directory <i>(optional specific
     *             exception)</i>
     * @throws IOException if an I/O error occurs
     * @throws SecurityException in the case of the default provider, and a security manager is installed, the
     *             {@link SecurityManager#checkWrite(String) checkWrite} method is invoked prior to attempting to create
     *             a directory and its {@link SecurityManager#checkRead(String) checkRead} is invoked for each parent
     *             directory that is checked. If {@code
     *          dir} is not an absolute path then its {@link Path#toAbsolutePath toAbsolutePath} may need to be invoked
     *             to get its absolute path. This may invoke the security manager's
     *             {@link SecurityManager#checkPropertyAccess(String) checkPropertyAccess} method to check access to the
     *             system property {@code user.dir}
     */
    public static Path createDirectories(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        try {
            return Files.createDirectories(dir, attrs);
        } catch (final IOException e) {
            throw ExceptionUtil.wrapIOException(e);
        }
    }

    @SuppressWarnings({"unchecked", "resource"})
    private static FSFileSystemProvider<FSPath, FSFileSystem<FSPath>> provider(final FSPath path) {
        return (FSFileSystemProvider<FSPath, FSFileSystem<FSPath>>)path.getFileSystem().provider();
    }

    /**
     * Creates a new directory with a randomized name in the given parent directory.
     *
     * @param parent The parent directory in which to create the temp directory.
     * @param prefix A string prefix for the directory name.
     * @param suffix A string suffix for the directory name.
     * @return the path of the newly created directory.
     *
     * @throws NoSuchFileException when the parent directory does not exist.
     * @throws IOException When something else went wrong while creating the directory.
     */
    public static FSPath createRandomizedDirectory(final FSPath parent, final String prefix, final String suffix)
        throws IOException {
        return provider(parent).createRandomizedDirectory(parent, prefix, suffix);
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
     * Similar to {@link Files#exists(Path, LinkOption...)} with the important distinction that it doesn't return
     * {@code false} for paths that it can't access but instead throws an {@link AccessDeniedException}.
     *
     * @param path the path to the file to test
     * @param linkOptions for resolving symbolic links
     * @return {@code true} if the file exists; {@code false} if the file does not exist or its existence cannot be
     *         determined.
     * @throws AccessDeniedException if the access to <b>path</b> is denied
     * @throws SecurityException In the case of the default provider, the {@link SecurityManager#checkRead(String)} is
     *             invoked to check read access to the file.
     */
    public static boolean exists(final Path path, final LinkOption... linkOptions) throws AccessDeniedException {
        try {
            Files.readAttributes(path, BasicFileAttributes.class, linkOptions);
            return true;
        } catch (AccessDeniedException ade) {
            throw ExceptionUtil.createAccessDeniedException(path);
        } catch (IOException ex) {
            return false;
        }
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

        final Path[] pathsToDelete;
        try (Stream<Path> stream = Files.walk(toDelete)) {
            pathsToDelete = stream.sorted(Comparator.reverseOrder()).toArray(Path[]::new);
        }

        for (Path currToDelete : pathsToDelete) {
            deleteSafely(currToDelete, ioeRef);
        }

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
