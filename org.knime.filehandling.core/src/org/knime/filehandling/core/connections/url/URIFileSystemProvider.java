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
 *   Aug 28, 2019 (bjoern): created
 */
package org.knime.filehandling.core.connections.url;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.attributes.FSBasicFileAttributeView;
import org.knime.filehandling.core.connections.base.attributes.BasicFileAttributesUtil;

/**
 * Special file system provider that provides file handling functionality for a single (!) URL.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class URIFileSystemProvider extends FileSystemProvider {

    private final int m_timeoutInMillis;

    /**
     * This class is a singleton, hence private constructor.
     * @param timeoutInMillis read timeout in milliseconds
     */
    public URIFileSystemProvider(final int timeoutInMillis) {
        m_timeoutInMillis = timeoutInMillis;
    }

    /**
     * @return the timeout in milliseconds
     */
    public int getTimeout() {
        return m_timeoutInMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScheme() {
        return "*";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized FileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        return new URIFileSystem(uri, getTimeout());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem(final URI uri) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("resource")
    @Override
    public Path getPath(final URI uri) {
        return new URIPath(new URIFileSystem(uri, getTimeout()), uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options,
        final FileAttribute<?>... attrs) throws IOException {

        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream newInputStream(final Path path, final OpenOption... options) throws IOException {
        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException("Path is from a different file system provider");
        }

        final URIPath uriPath = (URIPath)path;
        return uriPath.openURLConnection(getTimeout()).getInputStream();

    }

    @Override
    public OutputStream newOutputStream(final Path path, final OpenOption... options) throws IOException {
        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException("Path is from a different file system provider");
        }

        final URIPath uriPath = (URIPath)path;
        return uriPath.openURLConnection(getTimeout()).getOutputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir, final Filter<? super Path> filter)
        throws IOException {

        throw new UnsupportedOperationException("Folders and folder listings are not supported for custom URLs");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("Folders and folder listings are not supported for custom URLs");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final Path path) throws IOException {
        throw new UnsupportedOperationException("Deleting files is not supported for custom URLs");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {

        try (final SeekableByteChannel sourceChannel = Files.newByteChannel(source, StandardOpenOption.READ)) {
            try (final SeekableByteChannel targetChannel = createTargetChannel(target, options)) {
                copy(sourceChannel, targetChannel);
            }
        }
    }

    private static void copy(final SeekableByteChannel from, final SeekableByteChannel targetChannel)
        throws IOException {

        final ByteBuffer buffer = ByteBuffer.allocate(8192);
        int bytesRead;
        while (true) {
            // fill buffer
            bytesRead = 0;
            while (bytesRead != -1 && buffer.hasRemaining()) {
                bytesRead = from.read(buffer);
            }

            buffer.flip();
            while (buffer.hasRemaining()) {
                targetChannel.write(buffer);
            }
            buffer.clear();

            if (bytesRead == -1) {
                break;
            }
        }
    }

    private static SeekableByteChannel createTargetChannel(final Path target, final CopyOption... options)
        throws IOException {

        if (Arrays.asList(options).contains(StandardCopyOption.REPLACE_EXISTING)) {
            return Files.newByteChannel(target, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } else {
            return Files.newByteChannel(target, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        throw new UnsupportedOperationException("Moving files is not supported with custom URLs");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        if (path.getFileSystem().provider() != this || path2.getFileSystem().provider() != this) {
            return false;
        }

        return path.toUri().normalize().equals(path2.toUri().normalize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHidden(final Path path) throws IOException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileStore getFileStore(final Path path) throws IOException {
        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException("Provided path is not a CustomURIPath");
        }
        // there is only every one file store
        return path.getFileSystem().getFileStores().iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException("Provided path is not a CustomURIPath");
        }

        if (Arrays.asList(modes).contains(AccessMode.READ)) {
            final URL url = path.toUri().toURL();
            try (final InputStream in = url.openStream()) {
                // do nothing
            } catch (IOException e) {
                throw new IOException("Cannot access file: " + e.getMessage(), e);
            }
        }
        // we are ignoring the other access modes because there is nothing we can do
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type,
        final LinkOption... options) {

        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException("Provided path is not a CustomURIPath");
        }

        try {
            return (V)new FSBasicFileAttributeView(path.getFileName().toString(),
                readAttributes(path, BasicFileAttributes.class));
        } catch (final IOException ex) {
        return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> type,
        final LinkOption... options) throws IOException {

        if (path.getFileSystem().provider() != this) {
            throw new IllegalArgumentException("Provided path is not a CustomURIPath");
        }

        final FSPath fsPath = (FSPath)path;
        if (type == BasicFileAttributes.class) {
            return (A)fsPath.getFileAttributes(type);
        } else {
            throw new UnsupportedOperationException("Only BasicFileAttributes are supported");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options)
        throws IOException {

        return BasicFileAttributesUtil.attributesToMap(readAttributes(path, BasicFileAttributes.class, options),
            attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options)
        throws IOException {
        throw new UnsupportedOperationException();
    }
}
