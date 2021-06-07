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
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;

/**
 * This class is the FS*-specific companion class of {@link Files}, i.e. it consists exclusively of static methods that
 * operate on {@link FSFileSystem}s and {@link FSPath}s.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 */
public final class FSFiles {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FSFiles.class);

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
            final AccessDeniedException rephrased = ExceptionUtil.createAccessDeniedException(path);
            rephrased.initCause(ade);
            throw rephrased;
        } catch (NoSuchFileException ex) { // NOSONAR
            return false;
        } catch (IOException ex) {
            LOGGER.debug(String.format("An IOException occurred while accessing the attributes of %s.", path), ex);
            return false;
        }
    }

    /**
     * Similar to {@link Files#isDirectory(Path, LinkOption...)} with the important distinction that it doesn't return
     * {@code false} for paths that it can't access but instead throws an {@link AccessDeniedException}.
     *
     * @param path the path to the file to test
     * @param linkOptions for resolving symbolic links
     * @return {@code true} if the file is a directory; {@code false} otherwise
     * @throws AccessDeniedException if the access to <b>path</b> is denied
     * @throws SecurityException In the case of the default provider, the {@link SecurityManager#checkRead(String)} is
     *             invoked to check read access to the file.
     */
    public static boolean isDirectory(final Path path, final LinkOption... linkOptions) throws AccessDeniedException {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class, linkOptions).isDirectory();
        } catch (AccessDeniedException ade) {
            final AccessDeniedException accessDeniedExp = ExceptionUtil.createAccessDeniedException(path);
            accessDeniedExp.initCause(ade);
            throw accessDeniedExp;
        } catch (IOException ex) {
            LOGGER.debug(String.format("An IOException occurred while accessing the attributes of %s.", path), ex);
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
     * Deletes the given file or directory (must be empty for successful deletion). Any exceptions occurring during
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
        } catch (final IOException ioe) {
            if (ioeRef != null) {
                ioeRef.compareAndSet(null, ioe);
            }
        } catch (final Exception e) { // NOSONAR
            if (ioeRef != null) {
                ioeRef.compareAndSet(null, new IOException(e));
            }
        }
    }

    /**
     * Sorts the given list of paths in lexicographic order.
     *
     * @param paths the list to be sorted
     */
    public static void sortPathsLexicographically(final List<FSPath> paths) {
        paths.sort((final FSPath p1, final FSPath p2) -> p1.toString().compareTo(p2.toString()));
    }

    /**
     * Walks a file tree while following links.
     *
     * @param start the starting file
     * @param visitor the file visitor to invoke for each file
     * @return the starting file
     * @throws IllegalArgumentException if the {@code maxDepth} parameter is negative
     * @throws SecurityException If the security manager denies access to the starting file. In the case of the default
     *             provider, the {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read
     *             access to the directory.
     * @throws IOException if an I/O error is thrown by a visitor method
     */
    public static Path walkFileTree(final Path start, final FileVisitor<? super Path> visitor) throws IOException {
        return walkFileTree(start, Integer.MAX_VALUE, visitor);
    }

    /**
     * Walks a file tree up to a certain depth while following links.
     *
     * @param start the starting file
     * @param maxDepth the maximum number of directory levels to visit
     * @param visitor the file visitor to invoke for each file
     * @return the starting file
     * @throws IllegalArgumentException if the {@code maxDepth} parameter is negative
     * @throws SecurityException If the security manager denies access to the starting file. In the case of the default
     *             provider, the {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read
     *             access to the directory.
     * @throws IOException if an I/O error is thrown by a visitor method
     */
    public static Path walkFileTree(final Path start, final int maxDepth, final FileVisitor<? super Path> visitor)
        throws IOException {
        return Files.walkFileTree(start, EnumSet.of(FileVisitOption.FOLLOW_LINKS), maxDepth, visitor);
    }

    /**
     * Returns a {@link List} of {@link FSPath}s of a all files in a single folder.
     *
     * @param source the {@link Path} of the source folder
     * @return a {@link List} of {@link Path} from files in a folder
     * @throws IOException - If something went wrong while accessing the path
     */
    public static List<FSPath> getFilePathsFromFolder(final FSPath source) throws IOException {
        final List<FSPath> paths = new ArrayList<>();
        CheckUtils.checkArgument(FSFiles.isDirectory(source), "%s is not a folder. Please specify a folder.", source);

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                paths.add((FSPath)file);
                return FileVisitResult.CONTINUE;
            }
        });
        sortPathsLexicographically(paths);
        return paths;
    }

    /**
     * Returns a {@link List} of {@link FSPath}s of files and folder in a single folder.
     *
     * @param source the {@link Path} of the source folder
     * @param includeSourceFolder flag to incloude the source path in the output or not
     * @return a {@link List} of {@link Path} from files and folder in a folder
     * @throws IOException
     */
    public static List<FSPath> getFilesAndFolders(final FSPath source, final boolean includeSourceFolder)
        throws IOException {
        final List<FSPath> paths = new ArrayList<>();
        CheckUtils.checkArgument(FSFiles.isDirectory(source), "%s is not a folder. Please specify a folder.", source);

        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                throws IOException {
                if (!source.equals(dir) || includeSourceFolder) {
                    paths.add((FSPath)dir);
                }
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                paths.add((FSPath)file);
                return FileVisitResult.CONTINUE;
            }

        });
        sortPathsLexicographically(paths);
        return paths;
    }

    /**
     * Recursively copies the given source directory to the given target directory.
     *
     * <p>
     * The provided options will be forwarded to {@link FileSystemProvider#copy(Path, Path, CopyOption...)}. Note that
     * {@link StandardCopyOption#REPLACE_EXISTING} can be used to do a recursive "merge" copy into an already existing
     * target directory.
     * </p>
     *
     * <p>
     * This method does not perform a rollback in case of a failure. If a recursive copy fails, the target directory may
     * be in an inconsistent state insofar, that not all data from the source directory has been copied.
     * </p>
     *
     * @param source The directory to copy recursively.
     * @param target The directory to copy to.
     * @param options Options that specify how the copy should be done.
     * @throws IOException
     * @throws IllegalArgumentException if the given source path is not a directory.
     */
    public static void copyRecursively(final FSPath source, final FSPath target, final CopyOption... options)
        throws IOException {
        if (!Files.readAttributes(source, BasicFileAttributes.class).isDirectory()) {
            throw new IllegalArgumentException("Only directories can be copied recursively");
        }

        Files.walkFileTree(source, new RecursiveCopyVisitor(source, target, options));
    }

    private static final class RecursiveCopyVisitor implements FileVisitor<Path> {

        private final FSPath m_source;

        private final FSPath m_target;

        private final CopyOption[] m_options;

        private final boolean m_replaceExisting;

        private RecursiveCopyVisitor(final FSPath source, final FSPath target, final CopyOption[] options) {
            m_source = source;
            m_target = target;
            m_options = options;
            m_replaceExisting = Arrays.stream(options) //
                .anyMatch(o -> o == StandardCopyOption.REPLACE_EXISTING);
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {

            final Path targetDir = toTargetPath(dir);

            try {
                Files.createDirectory(targetDir);
            } catch (FileAlreadyExistsException e) {
                if (!m_replaceExisting) {
                    throw e;
                }

                if (!Files.readAttributes(targetDir, BasicFileAttributes.class).isDirectory()) {
                    throw new IOException(
                        String.format("Cannot replace non-directory %s with a directory", targetDir.toString()));
                }
            }

            return FileVisitResult.CONTINUE;
        }

        private FSPath toTargetPath(final Path sourcePath) {
            Path targetPath = m_target;
            for (Path sourceComp : m_source.relativize(sourcePath)) {
                targetPath = targetPath.resolve(sourceComp.toString());
            }
            return (FSPath)targetPath;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            if (!attrs.isDirectory() && !attrs.isOther()) {
                final Path targetFile = toTargetPath(file);
                Files.copy(file, targetFile, m_options);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
            return FileVisitResult.TERMINATE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }
    }
}
