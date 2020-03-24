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

import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributeView;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.connections.base.attributes.BasicFileAttributesUtil;

/**
 * Base implementation of the {@link FileSystemProvider} class.
 *
 * @param <T> {@link BaseFileSystem} implementation of this provider
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public abstract class BaseFileSystemProvider<T extends BaseFileSystem> extends FileSystemProvider {

    private static final String PATH_FROM_DIFFERENT_PROVIDER_MESSAGE = "Path is from a different file system provider";

    private T m_fileSystem;

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized FileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {

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
    protected abstract T createFileSystem(URI uri, Map<String, ?> env) throws IOException;

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
    public synchronized FileSystem getOrCreateFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        return m_fileSystem != null ? m_fileSystem : newFileSystem(uri, env);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options,
        final FileAttribute<?>... attrs) throws IOException {
        if (exists(path) && options.contains(StandardOpenOption.CREATE_NEW)) {
            throw new FileAlreadyExistsException(path.toString());
        }
        if (!exists(path) && options.contains(StandardOpenOption.READ)) {
            throw new NoSuchFileException(path.toString());
        }
        if ((options.contains(StandardOpenOption.CREATE_NEW) || options.contains(StandardOpenOption.CREATE))
            && !exists(path.getParent())) {
            throw new NoSuchFileException(path.getParent().toString());
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
        checkPath(path);
        checkOpenOptionsForReading(options);

        return new BaseInputStream(newInputStreamInternal(path, options), getFileSystemInternal());
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        checkPath(source);
        checkPath(target);
        if (exists(target) && !Arrays.asList(options).contains(StandardCopyOption.REPLACE_EXISTING)) {
            throw new FileAlreadyExistsException(target.toString());
        }
        if (!exists(target.getParent())) {
            throw new NoSuchFileException(target.getParent().toString());
        }

        moveInternal(source, target, options);
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
    protected abstract void moveInternal(Path source, Path target, final CopyOption... options) throws IOException;

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        checkPath(source);
        checkPath(target);
        if (isSameFile(source, target)) {
            return;
        }
        if (exists(target) && !Arrays.asList(options).contains(StandardCopyOption.REPLACE_EXISTING)) {
            throw new FileAlreadyExistsException(String.format("Target file %s already exists.", target.toString()));
        }
        copyInternal(source, target, options);
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
    protected abstract void copyInternal(final Path source, final Path target, final CopyOption... options)
        throws IOException;

    /**
     * Opens a file, returning an input stream to read from the file.
     *
     * @param path the path to the file to open
     * @param options options specifying how the file is opened
     * @return a new input stream
     * @throws IOException if I/O error occurs
     */
    protected abstract InputStream newInputStreamInternal(Path path, OpenOption... options) throws IOException;

    @Override
    public OutputStream newOutputStream(final Path path, final OpenOption... options) throws IOException {
        checkPath(path);
        final OpenOption[] validatedOpenOptions = ensureValidAndDefaultOpenOptionsForWriting(options);

        return new BaseOutputStream(newOutputStreamInternal(path, validatedOpenOptions), getFileSystemInternal());
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
    protected abstract OutputStream newOutputStreamInternal(Path path, OpenOption... options) throws IOException;

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir, final Filter<? super Path> filter)
        throws IOException {
        checkPath(dir);
        if (!exists(dir)) {
            throw new NoSuchFileException(dir.toString());
        }
        return new BaseDirectoryStream(createPathIterator(dir, filter), getFileSystemInternal());
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
    protected abstract Iterator<Path> createPathIterator(final Path dir, final Filter<? super Path> filter)
        throws IOException;

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized FileSystem getFileSystem(final URI uri) {
        return getFileSystemInternal();
    }

    /**
     * Returns the {@code FileSystem} created by this provider if it exists. If no {@code FileSystem} was created yet,
     * or the {@code FileSystem} is closed a {@link FileSystemNotFoundException} is thrown.
     *
     * @return the {@code FileSystem} created by this provider if it exists.
     */
    protected final synchronized T getFileSystemInternal() {
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

    /**
     * Returns whether the given path exists.
     *
     * @param path the path to check
     * @return whether the path exists
     * @throws IOException if IO error occurs that prevents determining whether the path exists or not.
     */
    protected abstract boolean exists(final Path path) throws IOException;

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

        checkPath(path);

        if (type == BasicFileAttributes.class || type == PosixFileAttributes.class) {

            BaseFileAttributes attributes;
            final Optional<BaseFileAttributes> cachedAttributes = getFileSystemInternal().getCachedAttributes(path);

            if (!cachedAttributes.isPresent()) {
                if (!exists(path)) {
                    throw new NoSuchFileException(String.format("No such file %s", path.toString()));
                }
                attributes = fetchAttributesInternal(path, type);
                getFileSystemInternal().addToAttributeCache(path, attributes);
            } else {
                attributes = cachedAttributes.get();
                if (type == PosixFileAttributes.class && !attributes.hasPosixAttributesSet()) {
                    attributes = attributes.generatePosixAttributes();
                    getFileSystemInternal().addToAttributeCache(path, attributes);
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
    protected abstract BaseFileAttributes fetchAttributesInternal(final Path path, final Class<?> type)
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

    @Override
    public void delete(final Path path) throws IOException {
        checkPath(path);
        if (!exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
        deleteInternal(path);
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
    protected abstract void deleteInternal(Path path) throws IOException;

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
}
