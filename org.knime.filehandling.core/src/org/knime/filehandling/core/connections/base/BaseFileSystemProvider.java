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
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
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
import java.nio.file.attribute.FileOwnerAttributeView;
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
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributeView;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.connections.base.attributes.BasicFileAttributesUtil;

/**
 * Base implementation of the {@link FileSystemProvider} class.
 *
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public abstract class BaseFileSystemProvider<P extends FSPath, F extends BaseFileSystem<P>> extends FSFileSystemProvider<P,F> {

    private static final String PATH_FROM_DIFFERENT_PROVIDER_MESSAGE = "Path is from a different file system provider";

    private F m_fileSystem;

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized F newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {

        if (m_fileSystem != null) {
            throw new FileSystemAlreadyExistsException();
        }
        m_fileSystem = createFileSystem(uri, env);
        return m_fileSystem;
    }

    /**
     * Constructs a new FileSystem object identified by an URI. This method is called from the
     * {@link #newFileSystem(URI, Map)} method, that takes care of adding the file system to the map of file systems.
     *
     * @param uri URI reference
     * @param env a map of provider specific properties to configure the file system; may be empty
     * @return the new file system
     * @throws IOException if I/O error occurs
     */
    protected abstract F createFileSystem(URI uri, Map<String, ?> env) throws IOException;

    /**
     *
     *
     * Gets or creates a new {@link FileSystem} based on the input uri.
     *
     * @param uri the URI that either retrieves or creates a new file system.
     * @param env A map of provider specific properties to configure the file system; may be empty
     * @return a file system for the URI
     * @throws IOException if I/O error occurs
     */
    public synchronized F getOrCreateFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        return m_fileSystem != null ? m_fileSystem : newFileSystem(uri, env);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options,
        final FileAttribute<?>... attrs) throws IOException {

        final P checkedPath = checkCastAndAbsolutizePath(path);

        if (existsCached(checkedPath) && options.contains(StandardOpenOption.CREATE_NEW)) {
            throw new FileAlreadyExistsException(path.toString());
        }
        if (!existsCached(checkedPath) && options.contains(StandardOpenOption.READ)) {
            throw new NoSuchFileException(path.toString());
        }
        if ((options.contains(StandardOpenOption.CREATE_NEW) || options.contains(StandardOpenOption.CREATE))
            && !existsCached((P) path.toAbsolutePath().getParent())) {
            throw new NoSuchFileException(path.toAbsolutePath().getParent().toString());
        }
        if ((options.contains(StandardOpenOption.READ) || options.contains(StandardOpenOption.TRUNCATE_EXISTING))
            && options.contains(StandardOpenOption.APPEND)) {
            throw new IllegalArgumentException("APPEND is not allowed with READ/TRUNCATE_EXISTING.");
        }
        return new BaseSeekableByteChannel(newByteChannelInternal(path, options, attrs), m_fileSystem);
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
    protected abstract SeekableByteChannel newByteChannelInternal(final Path path,
        final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException;

    @Override
    public InputStream newInputStream(final Path path, final OpenOption... options) throws IOException {
        checkOpenOptionsForReading(options);
        final P checkedPath = checkCastAndAbsolutizePath(path);
        return new BaseInputStream(newInputStreamInternal(checkedPath, options), getFileSystemInternal());
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

    @SuppressWarnings("unchecked")
    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        final P checkedSource = checkCastAndAbsolutizePath(source);
        final P checkedTarget = checkCastAndAbsolutizePath(target);

        if (!existsCached(checkedSource)) {
            throw new NoSuchFileException(source.toString());
        }

        if (existsCached(checkedTarget) && !Arrays.asList(options).contains(StandardCopyOption.REPLACE_EXISTING)) {
            throw new FileAlreadyExistsException(target.toString());
        }
        if (!existsCached((P) checkedTarget.getParent())) {
            throw new NoSuchFileException(target.getParent().toString());
        }

        moveInternal(checkedSource, checkedTarget, options);
        getFileSystemInternal().removeFromAttributeCache(source);
    }

    /**
     * Move or rename a file to a target file. This method works in exactly the manner specified by the
     * {@link Files#move} method except that both the source and target paths must be associated with this provider.
     *
     * @param source the path to the file to move
     * @param target the path to the target file
     * @param options options specifying how the move should be done
     * @throws IOException if I/O error occurs
     */
    protected abstract void moveInternal(P source, P target, final CopyOption... options) throws IOException;

    @SuppressWarnings("unchecked")
    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        final P checkedSource = checkCastAndAbsolutizePath(source);
        final P checkedTarget = checkCastAndAbsolutizePath(target);

        if (!existsCached(checkedSource)) {
            throw new NoSuchFileException(source.toString());
        }

        try {
            if (isSameFile(checkedSource, checkedTarget)) {
                return;
            }
        } catch (NoSuchFileException e) {
            // target file might not exists
        }

        if (!existsCached((P)checkedTarget.getParent())) {
            throw new NoSuchFileException(checkedTarget.getParent().toString());
        }

        if (existsCached(checkedTarget) && !Arrays.asList(options).contains(StandardCopyOption.REPLACE_EXISTING)) {
            throw new FileAlreadyExistsException(String.format("Target file %s already exists.", target.toString()));
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
        final OpenOption[] validatedOpenOptions = ensureValidAndDefaultOpenOptionsForWriting(options);
        final P checkedPath = checkCastAndAbsolutizePath(path);

        return new BaseOutputStream(newOutputStreamInternal(checkedPath, validatedOpenOptions), getFileSystemInternal());
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

        final P checkedDir = checkCastAndAbsolutizePath(dir);

        // readAttributes() will also throw NoSuchFileException when file does not exist.
        if (!readAttributes(checkedDir, BasicFileAttributes.class).isDirectory()) {
            throw new NotDirectoryException(checkedDir.toString());
        }

        return new BaseDirectoryStream((Iterator<Path>)createPathIterator(checkedDir, filter), getFileSystemInternal());
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

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized F getFileSystem(final URI uri) {
        return getFileSystemInternal();
    }

    /**
     * Returns the {@code FileSystem} created by this provider if it exists. If no {@code FileSystem} was created yet,
     * or the {@code FileSystem} is closed a {@link FileSystemNotFoundException} is thrown.
     *
     * @return the {@code FileSystem} created by this provider if it exists.
     */
    protected final synchronized F getFileSystemInternal() {
        if (m_fileSystem == null) {
            throw new FileSystemNotFoundException();
        }
        return m_fileSystem;
    }

    /**
     * Removes the file system for the given URI from the list of file systems.
     *
     * @param uri the URI to the file system
     */
    protected synchronized void removeFileSystem(final URI uri) {
        m_fileSystem = null;
    }

    /**
     * Returns whether a file system for the given URI exists in the list of file systems.
     *
     * @param uri the URI to the file system
     * @return whether a file system for the uri exists
     */
    public synchronized boolean isOpen(final URI uri) {
        return m_fileSystem != null;
    }

    @SuppressWarnings("unchecked")
    protected P checkCastAndAbsolutizePath(final Path path) {
        checkPath(path);
        return (P) path.toAbsolutePath();
    }

    /**
     * Tests whether the given path (after toAbsolute().normalize()) exists, by
     * first checking for a cache entry, and then invoking {@link #exists(FSPath)}.
     *
     * @param path The path to check.
     * @return whether the path exists or not.
     * @throws IOException if IO error occurs that prevents determining whether the path exists or not.
     */
    final protected boolean existsCached(final P path) throws IOException {
        final P normalizedAbsolute = (P) path.toAbsolutePath().normalize();
        return getFileSystemInternal().hasCachedAttributes(normalizedAbsolute) || exists(normalizedAbsolute);
    }

    /**
     * Tests whether the given absolute, normalized path exists in the backing file system. Implementations of this
     * method must not perform a cache lookup (this is done in {@link #existsCached(FSPath)}.
     *
     * @param path An absolute and normalized path to check for existence.
     * @return whether the path exists or not.
     * @throws IOException if IO error occurs that prevents determining whether the path exists or not.
     */
    protected abstract boolean exists(final P path) throws IOException;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type,
        final LinkOption... options) {
        checkPath(path);
        if (type == BasicFileAttributeView.class || type == PosixFileAttributeView.class
            || type == FileOwnerAttributeView.class) {

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

        final P checkedPath = checkCastAndAbsolutizePath(path);

        if (type == BasicFileAttributes.class || type == PosixFileAttributes.class) {

            BaseFileAttributes attributes;
            final Optional<BaseFileAttributes> cachedAttributes = getFileSystemInternal().getCachedAttributes(checkedPath);

            if (!cachedAttributes.isPresent()) {
                if (!existsCached(checkedPath)) {
                    throw new NoSuchFileException(checkedPath.toString());
                }
                attributes = fetchAttributesInternal(checkedPath, type);
                getFileSystemInternal().addToAttributeCache(checkedPath, attributes);
            } else {
                attributes = cachedAttributes.get();
                if (type == PosixFileAttributes.class && !attributes.hasPosixAttributesSet()) {
                    attributes = attributes.generatePosixAttributes();
                    getFileSystemInternal().addToAttributeCache(checkedPath, attributes);
                }
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
    protected abstract BaseFileAttributes fetchAttributesInternal(final P path, final Class<?> type)
        throws IOException;

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options)
        throws IOException {
        checkPath(path);
        return BasicFileAttributesUtil.attributesToMap(readAttributes(path, BasicFileAttributes.class, options),
            attributes);
    }


    @SuppressWarnings("unchecked")
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        final P checkedPath = (P) checkCastAndAbsolutizePath(path).normalize();
        if (!existsCached(checkedPath)) {
            throw new NoSuchFileException(path.toString());
        }
        checkAccessInternal(checkedPath, modes);
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
        final P checkedPath = checkCastAndAbsolutizePath(path);
        if (!existsCached(checkedPath)) {
            throw new NoSuchFileException(path.toString());
        }
        deleteInternal(checkedPath);
        getFileSystemInternal().removeFromAttributeCache(path);
    }

    /**
     * Deletes a file. This method works in exactly the manner specified by the {@link Files#delete} method.
     *
     * @param path the path to the file to delete
     *
     * @throws NoSuchFileException if the file does not exist <i>(optional specific exception)</i>
     * @throws DirectoryNotEmptyException if the file is a directory and could not otherwise be deleted because the
     *             directory is not empty <i>(optional specific exception)</i>
     * @throws IOException if an I/O error occurs
     */
    protected abstract void deleteInternal(P path) throws IOException;

    /**
     * Checks whether the given path belongs to this provider.
     *
     * @param path the path to check
     */
    protected void checkPath(final Path path) {
        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException(PATH_FROM_DIFFERENT_PROVIDER_MESSAGE);
        }
    }

    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        if (path.getFileSystem().provider() != this) {
            return false;
        }

        if (path2.getFileSystem().provider() != this) {
            return false;
        }

        final P checkedPath = checkCastAndAbsolutizePath(path);
        final P checkedPath2 = checkCastAndAbsolutizePath(path2);

        if (checkedPath == checkedPath2 || checkedPath.normalize().equals(checkedPath2.normalize())) {
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

    @SuppressWarnings("resource")
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
}
