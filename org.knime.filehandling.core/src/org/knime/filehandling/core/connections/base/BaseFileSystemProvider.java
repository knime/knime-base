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
 *   18.12.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */

package org.knime.filehandling.core.connections.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.filehandling.core.connections.FSFileSystemProvider;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSInputStream;
import org.knime.filehandling.core.connections.FSOutputStream;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.FSSeekableByteChannel;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributeView;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.connections.base.attributes.BasicFileAttributesUtil;

/**
 * Base implementation of the {@link FileSystemProvider} class.
 *
 * @author Mareike Hoeger, KNIME GmbH
 * @param <P> the path type
 * @param <F> the base file system type
 * @noreference non-public API
 * @noextend non-public API
 * @noinstantiate non-public API
 */
public abstract class BaseFileSystemProvider<P extends FSPath, F extends BaseFileSystem<P>>
    extends FSFileSystemProvider<P, F> {

    private static final String PATH_FROM_DIFFERENT_PROVIDER_MESSAGE = "Path is from a different file system provider";

    private F m_fileSystem;

    @SuppressWarnings("unchecked")
    void setFileSystem(final BaseFileSystem<?> fileSystem) {
        m_fileSystem = (F)fileSystem;
    }

    @Override
    public final F newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        // file system always exists, so we throw FileSystemAlreadyExistsException
        throw new FileSystemAlreadyExistsException();
    }

    /**
     * Checks whether the underlying file system is still open and not in the process of closing. Throws a
     * {@link ClosedFileSystemException} if not.
     *
     * @throws ClosedFileSystemException when the file system has already been closed or is closing right now.
     */
    protected void checkFileSystemOpenAndNotClosing() {
        if (!m_fileSystem.isOpen() || m_fileSystem.isClosing()) {
            throw new ClosedFileSystemException();
        }
    }

    /**
     * Checks whether the underlying file system is either open or in the process of closing. Throws a
     * {@link ClosedFileSystemException} if the file system has already been closed.
     *
     * @throws ClosedFileSystemException if the file system has already been closed.
     */
    private void checkFileSystemOpenOrClosing() {
        if (!m_fileSystem.isOpen() && !m_fileSystem.isClosing()) {
            throw new ClosedFileSystemException();
        }
    }

    @SuppressWarnings({"resource"})
    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options,
        final FileAttribute<?>... attrs) throws IOException {

        checkFileSystemOpenAndNotClosing();

        final Set<OpenOption> sanitizedOptions = validateAndSanitizeChannelOpenOptions(options);

        final P checkedPath = checkCastAndAbsolutizePath(path);

        try {
            final BasicFileAttributes fileAttrs = readAttributes(checkedPath, BasicFileAttributes.class);

            // we can neither read nor write a directory
            if (fileAttrs.isDirectory()) {
                throw new IOException("Is a directory");
            }

            if (options.contains(StandardOpenOption.CREATE_NEW)) {
                throw new FileAlreadyExistsException(path.toString());
            }

        } catch (NoSuchFileException e) {
            // if the file does not exist we need a CREATE open option
            if (!options.contains(StandardOpenOption.CREATE) && !options.contains(StandardOpenOption.CREATE_NEW)) {
                throw e;
            }

            // if the file does not exist and we will create it, we need to check whether the parent directory
            // exists
            checkParentDirectoryExists(checkedPath);
        }

        return new FSSeekableByteChannel(newByteChannelInternal(checkedPath, sanitizedOptions, attrs), m_fileSystem);
    }

    /**
     * Checks whether the parent directory (if any) of the given path exists and throws an exception if not.
     *
     * @param path The path whose parent directory (if any) should be checked for existence.
     * @throws NoSuchFileException if the parent directory does not exist.
     * @throws FileSystemException if the parent path exists but is not a directory.
     * @throws IOException if something went wrong while checking.
     */
    protected void checkParentDirectoryExists(final P path) throws IOException {
        @SuppressWarnings("unchecked")
        final P checkedPathParent = (P)path.getParent();
        if (checkedPathParent != null) {
            // already fails with NoSuchFileException if it does not exist
            final BasicFileAttributes parentAttrs = readAttributes(checkedPathParent, BasicFileAttributes.class);

            if (!parentAttrs.isDirectory()) {
                // additionally we fail if the parent path is not a directory
                throw new FileSystemException(checkedPathParent.toString(), null, "Not a directory");
            }
        }
    }

    private static Set<OpenOption> validateAndSanitizeChannelOpenOptions(final Set<? extends OpenOption> options) { // NOSONAR it is best to sanitize the options in one place, also the method has comments
        final Set<OpenOption> sanitized = new HashSet<>(options);

        // APPEND and TRUNCATE_EXISTING imply WRITE
        if ((options.contains(StandardOpenOption.APPEND) || options.contains(StandardOpenOption.TRUNCATE_EXISTING))
            && !options.contains(StandardOpenOption.WRITE)) {
            sanitized.add(StandardOpenOption.WRITE);
        }

        // default to READ if nothing else is specified
        if (!sanitized.contains(StandardOpenOption.READ) && !sanitized.contains(StandardOpenOption.WRITE)) {
            sanitized.add(StandardOpenOption.READ);
        }

        // ignore CREATE if CREATE_NEW is present
        if (sanitized.contains(StandardOpenOption.CREATE) && sanitized.contains(StandardOpenOption.CREATE_NEW)) {
            sanitized.remove(StandardOpenOption.CREATE);
        }

        // ignore CREATE_NEW and CREATE if file is only opened for reading
        if ((sanitized.contains(StandardOpenOption.CREATE) || sanitized.contains(StandardOpenOption.CREATE_NEW))
            && !sanitized.contains(StandardOpenOption.WRITE)) {
            sanitized.remove(StandardOpenOption.CREATE);
            sanitized.remove(StandardOpenOption.CREATE_NEW);
        }

        if ((sanitized.contains(StandardOpenOption.READ) || sanitized.contains(StandardOpenOption.TRUNCATE_EXISTING))
            && sanitized.contains(StandardOpenOption.APPEND)) {
            throw new IllegalArgumentException("APPEND is not allowed with READ/TRUNCATE_EXISTING.");
        }

        return sanitized;
    }

    /**
     * Opens or creates a file, returning a seekable byte channel to access the file. This method works in exactly the
     * manner specified by the {@link Files#newByteChannel(Path,Set,FileAttribute[])} method.
     *
     * @param path the path to the file to open or create
     * @param options options specifying how the file is opened
     * @param attrs an optional list of file attributes to set atomically when creating the file
     *
     * @return a new seekable byte channel
     */
    protected abstract SeekableByteChannel newByteChannelInternal(final P path, final Set<? extends OpenOption> options,
        final FileAttribute<?>... attrs) throws IOException;

    @Override
    public InputStream newInputStream(final Path path, final OpenOption... options) throws IOException {
        checkFileSystemOpenAndNotClosing();
        checkOpenOptionsForReading(options);
        final P checkedPath = checkCastAndAbsolutizePath(path);

        if (!existsCached(checkedPath)) {
            throw new NoSuchFileException(checkedPath.toString());
        }

        return new FSInputStream(newInputStreamInternal(checkedPath, options), getFileSystemInternal());
    }

    /**
     * Checks whether the open options are valid for reading.
     *
     * @param options the options to check
     */
    protected static void checkOpenOptionsForReading(final OpenOption[] options) {
        for (final OpenOption option : options) {
            if (option == StandardOpenOption.APPEND || option == StandardOpenOption.WRITE) {
                throw new UnsupportedOperationException("'" + option + "' not allowed");
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {

        checkFileSystemOpenAndNotClosing();

        final P checkedSource = checkCastAndAbsolutizePath(source);
        final P checkedTarget = checkCastAndAbsolutizePath(target);

        if (checkedSource.equals(checkedTarget)) {
            return;
        }

        if (!existsCached(checkedSource)) {
            throw new NoSuchFileException(source.toString());
        }

        if (existsCached(checkedTarget)) {
            if (!Arrays.asList(options).contains(StandardCopyOption.REPLACE_EXISTING)) {
                throw new FileAlreadyExistsException(
                    String.format("Target file %s already exists.", target.toString()));
            } else if (FSFiles.isNonEmptyDirectory(checkedTarget)) {
                throw new DirectoryNotEmptyException(target.toString());
            }
        }

        final P targetParent = (P)checkedTarget.getParent();
        if (targetParent != null) {
            // throws already NoSuchFileException
            final BasicFileAttributes parentAttrs = readAttributes(targetParent, BasicFileAttributes.class);
            if (!parentAttrs.isDirectory()) {
                throw new FileSystemException(targetParent.toString() + " is not a directory");
            }
        }

        moveInternal(checkedSource, checkedTarget, options);
        getFileSystemInternal().removeFromAttributeCacheDeep(checkedSource);
    }

    /**
     * Move or rename a file to a target file. This method works in exactly the manner specified by the
     * {@link Files#move} method except that both the source and target paths must be associated with this provider.
     *
     * <p>
     * Implementations can assume that
     *
     * <ul>
     * <li>The source path exists</li>
     * <li>The target path either (1) does not exist, or (2) it exists and {@link StandardCopyOption#REPLACE_EXISTING}
     * and it is a file or an empty directory.</li>
     * <li>If the target path does not exist, then its parent exists and is a directory.</li>
     * </ul>
     * </p>
     *
     * <p>
     * This method has a default implementation that emulates moving using (recursive) copy and delete. Subclasses are
     * encouraged to override this method, if the underlying backend provides a O(1) operation for moving directories
     * and/or files.
     * </p>
     *
     * @param source An absolute, normalized path that specifies the source file/directory to move.
     * @param target An absolute, normalized path that specifies the target file/directory.
     * @param options options specifying how the move should be done
     * @throws IOException if something prevents moving the source file to the target location (I/O errors, access
     *             denied, ...)
     */
    protected void moveInternal(final P source, final P target, final CopyOption... options) throws IOException {
        final BasicFileAttributes sourceAttrs = readAttributes(source, BasicFileAttributes.class);
        if (sourceAttrs.isDirectory()) {
            moveDirectoryInternal(source, target, options);
        } else {
            moveFileInternal(source, target, options);
        }
    }

    /**
     * Default implementation that moves a file.
     *
     * @param source An absolute, normalized path that specifies the source file to move.
     * @param target An absolute, normalized path that specifies the target file.
     * @param options options specifying how the move should be done
     * @throws IOException if something prevents moving the source file to the target location (I/O errors, access
     *             denied, ...)
     */
    protected void moveFileInternal(final P source, final P target, final CopyOption[] options) throws IOException {
        copy(source, target, options);
        delete(source);
    }

    /**
     * Default implementation that recursively moves a directory.
     *
     * @param source An absolute, normalized path that specifies the source directory to move.
     * @param target An absolute, normalized path that specifies the target directory.
     * @param options options specifying how the move should be done
     * @throws IOException if something prevents moving the source directory to the target location (I/O errors, access
     *             denied, ...)
     */
    protected void moveDirectoryInternal(final P source, final P target, final CopyOption[] options)
        throws IOException {

        FSFiles.copyRecursively(source, target, options);
        FSFiles.deleteRecursively(source);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {

        checkFileSystemOpenAndNotClosing();

        final P checkedSource = checkCastAndAbsolutizePath(source);
        final P checkedTarget = checkCastAndAbsolutizePath(target);

        if (!existsCached(checkedSource)) {
            throw new NoSuchFileException(source.toString());
        }

        try {
            if (isSameFile(checkedSource, checkedTarget)) {
                return;
            }
        } catch (NoSuchFileException e) { // NOSONAR target file might not exist
        }

        if (!existsCached((P)checkedTarget.getParent())) {
            throw new NoSuchFileException(checkedTarget.getParent().toString());
        }

        if (existsCached(checkedTarget)) {
            if (!Arrays.asList(options).contains(StandardCopyOption.REPLACE_EXISTING)) {
                throw new FileAlreadyExistsException(
                    String.format("Target file %s already exists.", target.toString()));
            } else if (FSFiles.isNonEmptyDirectory(checkedTarget)) {
                throw new DirectoryNotEmptyException(target.toString());
            }
        }

        copyInternal(checkedSource, checkedTarget, options);
    }

    /**
     * Copy a file to a target file. This method works in exactly the manner specified by the
     * {@link Files#copy(Path,Path,CopyOption[])} method except that both the source and target paths must be associated
     * with this provider.
     *
     * @param source the path to the file to copy
     * @param target the path to the target file
     * @param options options specifying how the copy should be done
     * @throws IOException if I/O error occurs
     */
    protected abstract void copyInternal(final P source, final P target, final CopyOption... options)
        throws IOException;

    /**
     * Opens a file, returning an input stream to read from the file.
     *
     * @param path the path to the file to open
     * @param options options specifying how the file is opened
     * @return a new input stream
     * @throws IOException if I/O error occurs
     */
    protected abstract InputStream newInputStreamInternal(P path, OpenOption... options) throws IOException;

    @Override
    public OutputStream newOutputStream(final Path path, final OpenOption... options) throws IOException {
        checkFileSystemOpenAndNotClosing();

        final OpenOption[] validatedOpenOptions = ensureValidAndDefaultOpenOptionsForWriting(options);
        final P checkedPath = checkCastAndAbsolutizePath(path);

        if (Arrays.stream(validatedOpenOptions).anyMatch(StandardOpenOption.CREATE_NEW::equals) // NOSONAR OpenOption is related to StandardOpenOption
            && existsCached(checkedPath)) {
            throw new FileAlreadyExistsException(path.toString());
        }

        return new FSOutputStream(newOutputStreamInternal(checkedPath, validatedOpenOptions), getFileSystemInternal());
    }

    /**
     * Checks whether the open options are valid for writing and adds default options if necessary.
     *
     * @param options the options to check
     * @return validated open options
     */
    protected static OpenOption[] ensureValidAndDefaultOpenOptionsForWriting(final OpenOption[] options) {

        if (options.length == 0) {
            return new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING};
        } else {
            final Set<OpenOption> opts = new HashSet<>(options.length + 3);
            for (final OpenOption option : options) {
                if (option == StandardOpenOption.READ) {
                    throw new IllegalArgumentException("READ not allowed");
                }
                opts.add(option);
            }
            opts.add(StandardOpenOption.WRITE);
            return opts.toArray(new OpenOption[opts.size()]);
        }
    }

    /**
     * Opens or creates a file, returning an output stream that may be used to write bytes to the file.
     *
     * @param path the path to the file to open or create
     * @param options options specifying how the file is opened
     *
     * @return a new output stream
     * @throws IOException if an I/O error occurs
     */
    protected abstract OutputStream newOutputStreamInternal(P path, OpenOption... options) throws IOException;

    @SuppressWarnings("unchecked")
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir, final Filter<? super Path> filter)
        throws IOException {

        // allowed during closing (part of recursive temp dir deletion)
        checkFileSystemOpenOrClosing();

        final P checkedDir = checkCastAndAbsolutizePath(dir);

        // readAttributes() will also throw NoSuchFileException when file does not exist.
        if (!readAttributes(checkedDir, BasicFileAttributes.class).isDirectory()) {
            throw new NotDirectoryException(checkedDir.toString());
        }

        final Iterator<Path> pathIterator = (Iterator<Path>)createPathIterator(checkedDir, filter);

        return new BaseDirectoryStream(new RelativizingPathIterator(pathIterator, dir), getFileSystemInternal());
    }

    /**
     * Creates the file system specific path iterator. This method is called from
     * {@link #newDirectoryStream(Path, Filter)} to create a directory stream with the given iterator.
     *
     * @param dir the path to the directory
     * @param filter the directory stream filter
     *
     * @return a new {@code Iterator<Path>} object
     * @throws IOException if I/O error occurs
     */
    protected abstract Iterator<P> createPathIterator(final P dir, final Filter<? super Path> filter)
        throws IOException;

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        checkFileSystemOpenAndNotClosing();

        final P checkedDir = checkCastAndAbsolutizePath(dir);
        checkParentDirectoryExists(checkedDir);

        try {
            // fails with NoSuchFileException if it does not exist
            readAttributes(checkedDir, BasicFileAttributes.class);
            throw new FileAlreadyExistsException(checkedDir.toString());
        } catch (NoSuchFileException e) { // NOSONAR exception is dealt with properly
            createDirectoryInternal(checkedDir, attrs);
        }
    }

    /**
     * Creates a directory for the given path. Implementations can assume that the parent directory has been checked for
     * existence and that no file exists, which matches the given path.
     *
     * @param dir an absolute, normalized path that specifies the directory to create.
     * @param attrs an optional list of file attributes to set atomically when creating the directory
     * @throws IOException If something went wrong while creating the directory.
     */
    protected abstract void createDirectoryInternal(final P dir, final FileAttribute<?>... attrs) throws IOException;

    /**
     * Returns the internal file system instance (does not look at the URI).
     */
    @Override
    public final F getFileSystem(final URI uri) {
        return m_fileSystem;
    }

    /**
     * Returns the {@code FileSystem} created by this provider if it exists. If no {@code FileSystem} was created yet,
     * or the {@code FileSystem} is closed a {@link FileSystemNotFoundException} is thrown.
     *
     * @return the {@code FileSystem} created by this provider if it exists.
     */
    protected final F getFileSystemInternal() {
        return m_fileSystem;
    }

    @SuppressWarnings("resource")
    @Override
    public final FileStore getFileStore(final Path path) throws IOException {
        return getFileSystemInternal().getFileStores().get(0);
    }

    /**
     * Removes the file system for the given URI from the list of file systems.
     *
     * @param uri the URI to the file system
     */
    protected synchronized void removeFileSystem(final URI uri) {
        // do nothing
    }

    @SuppressWarnings("unchecked")
    protected P checkCastAndAbsolutizePath(final Path path) {
        checkPathProvider(path);
        return (P)path.toAbsolutePath().normalize();
    }

    /**
     * Tests whether the given path (after toAbsolute().normalize()) exists, by first checking for a cache entry, and
     * then invoking {@link #exists(FSPath)}.
     *
     * @param path The path to check.
     * @return whether the path exists or not.
     * @throws IOException if IO error occurs that prevents determining whether the path exists or not.
     */
    protected final boolean existsCached(final P path) throws IOException {
        final P normalizedAbsolute = (P)path.toAbsolutePath().normalize();
        return getFileSystemInternal().hasCachedAttributes(normalizedAbsolute) || exists(normalizedAbsolute);
    }

    /**
     * Tests whether the given absolute, normalized path exists in the backing file system. Subclasses can override
     * this method, if there is a more efficient method than invoking {@link #fetchAttributesInternal(FSPath, Class)}
     * to determine the existence of the given path. Note that implementations of this method must not perform a
     * cache lookup (this is already done in {@link #existsCached(FSPath)}.
     *
     * @param path An absolute and normalized path to check for existence.
     * @return whether the path exists or not.
     * @throws IOException if IO error occurs that prevents determining whether the path exists or not.
     */
    @SuppressWarnings("resource")
    protected boolean exists(final P path) throws IOException {
        try {
            final BaseFileAttributes fileAttrs = fetchAttributesInternal(path, BasicFileAttributes.class);
            getFileSystemInternal().addToAttributeCache(path, fileAttrs);
            return true;
        } catch (NoSuchFileException e) { // NOSONAR ignore because indicates file does not exist
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type,
        final LinkOption... options) {

        // allowed during closing (part of recursive temp dir deletion)
        checkFileSystemOpenOrClosing();

        checkPathProvider(path);
        if (type == BasicFileAttributeView.class || type == PosixFileAttributeView.class) {
            return (V)new BaseFileAttributeView(path, type);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> type,
        final LinkOption... options) throws IOException {

        // allowed during closing (part of recursive temp dir deletion)
        checkFileSystemOpenOrClosing();

        final P checkedPath = checkCastAndAbsolutizePath(path);

        if (type == BasicFileAttributes.class || type == PosixFileAttributes.class) {

            BaseFileAttributes attributes;

            if (!existsCached(checkedPath)) {
                throw new NoSuchFileException(checkedPath.toString());
            }

            final Optional<BaseFileAttributes> cachedAttributes =
                getFileSystemInternal().getCachedAttributes(checkedPath);

            if (!cachedAttributes.isPresent()) {
                attributes = fetchAttributesInternal(checkedPath, type);
                getFileSystemInternal().addToAttributeCache(checkedPath, attributes);
            } else {
                attributes = cachedAttributes.get();
            }

            if (type == PosixFileAttributes.class && !attributes.hasPosixAttributesSet()) {
                attributes = attributes.generatePosixAttributes();
                getFileSystemInternal().addToAttributeCache(checkedPath, attributes);
            }

            return (A)attributes;
        }

        throw new UnsupportedOperationException(String.format("only %s and %s supported",
            BasicFileAttributes.class.getName(), PosixFileAttributes.class.getName()));
    }

    /**
     * Returns the {@link BaseFileAttributes} for this path.
     *
     * @param path the Path to fetch the attributes for
     * @param type the type of the requested FileAttributes
     *
     * @return FSFileAttribute for this path
     * @throws IOException if an I/O error occurs while fetching the attributes.
     */
    protected abstract BaseFileAttributes fetchAttributesInternal(final P path, final Class<?> type) throws IOException;

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options)
        throws IOException {

        // allowed during closing (part of recursive temp dir deletion)
        checkFileSystemOpenOrClosing();

        checkPathProvider(path);
        return BasicFileAttributesUtil.attributesToMap(readAttributes(path, BasicFileAttributes.class, options),
            attributes);
    }

    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {

        checkFileSystemOpenAndNotClosing();

        final P checkedPath = checkCastAndAbsolutizePath(path);

        if (!existsCached(checkedPath)) {
            throw new NoSuchFileException(path.toString());
        }

        if (modes.length > 0) {
            checkAccessInternal(checkedPath, modes);
        }
    }

    /**
     * Optional method that implementations can implement to check the accessibility of a file. The given file can
     * already be assumed to exist, so it is not necessary to do an additional existence check.
     *
     * @param path The file to check (path can be assumed to be absolute and normalized).
     * @param modes The access modes to check (may have zero elements).
     * @throws AccessDeniedException If the requested access would be denied or the access cannot be determined.
     * @throws IOException If an I/O error occurs.
     */
    protected abstract void checkAccessInternal(final P path, final AccessMode... modes) throws IOException;

    @Override
    public void delete(final Path path) throws IOException {

        // allowed during closing (part of recursive temp dir deletion)
        checkFileSystemOpenOrClosing();

        final P checkedPath = checkCastAndAbsolutizePath(path);
        if (!existsCached(checkedPath)) {
            throw new NoSuchFileException(path.toString());
        }
        deleteInternal(checkedPath);
        getFileSystemInternal().removeFromAttributeCache(path);
    }

    /**
     * Deletes a file or directory that is provided as an absolute, normalized path, which has been checked for
     * existence.
     *
     * @param path The absolute, normalized path to and existing file or directory
     *
     * @throws NoSuchFileException if the file does not exist <i>(optional specific exception)</i>
     * @throws DirectoryNotEmptyException if the file is a directory and could not otherwise be deleted because the
     *             directory is not empty <i>(optional specific exception)</i>
     * @throws IOException if an I/O error occurs
     * @see Files#delete
     */
    protected abstract void deleteInternal(P path) throws IOException;

    /**
     * Checks whether the given path belongs to this provider.
     *
     * @param path the path to check
     */
    protected void checkPathProvider(final Path path) {
        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException(PATH_FROM_DIFFERENT_PROVIDER_MESSAGE);
        }
    }

    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        checkFileSystemOpenAndNotClosing();

        if (path.getFileSystem().provider() != this || path2.getFileSystem().provider() != this) {
            return false;
        }

        final P checkedPath = checkCastAndAbsolutizePath(path);
        final P checkedPath2 = checkCastAndAbsolutizePath(path2);

        if (checkedPath.equals(checkedPath2)) {
            return true;
        }

        return isSameFileInternal(checkedPath, checkedPath2);
    }

    /**
     * Tests if two paths locate the same file. Assumes that the two paths are from this provider, but not equal (even
     * when normalized). Subclasses may want to override the default implementation of this method which always returns
     * false.
     *
     * @param path One path.
     * @param path2 Another path.
     * @return {@code true} if the two paths locate the same file, false otherwise.
     * @throws IOException If an I/O error occurs.
     * @see Files#isSameFile(Path, Path)
     */
    protected boolean isSameFileInternal(final P path, final P path2) throws IOException {
        // Dummy implementation. Most file system won't have to do anything special here.
        return false;
    }

    @Override
    public P getPath(final URI uri) {
        final F fileSystem = getFileSystemInternal();

        if (fileSystem.getSchemeString().equalsIgnoreCase(uri.getScheme())
            && fileSystem.getHostString().equalsIgnoreCase(uri.getHost())) {
            return fileSystem.getPath(uri.getPath());
        } else {
            throw new IllegalArgumentException(String.format("Cannot create path for URI: %s", uri.toString()));
        }
    }

    @Override
    public boolean isHidden(final Path path) throws IOException {
        checkFileSystemOpenAndNotClosing();
        final P checkedPath = checkCastAndAbsolutizePath(path);
        return isHiddenInternal(checkedPath);
    }

    /**
     * Tells whether or not a file is considered <em>hidden</em>. Depending on the file system, this method may require
     * to do I/O if the file is considered hidden. This "internal" method has a default implementation that simply
     * returns false. Subclasses may choose to override the method if they have a concept of hidden files.
     *
     * @param path The absolute, normalized path.
     * @return true if the file is considered hidden, false otherwise.
     * @throws NoSuchFileException if the file does not exist <i>(optional specific exception)</i>
     * @throws IOException if an I/O error occurs
     * @see Files#isHidden(Path)
     */
    protected boolean isHiddenInternal(final P path) throws IOException {
        return false;
    }

    @Override
    public void setAttribute(final Path arg0, final String arg1, final Object arg2, final LinkOption... arg3)
        throws IOException {
        throw new UnsupportedOperationException();
    }
}
