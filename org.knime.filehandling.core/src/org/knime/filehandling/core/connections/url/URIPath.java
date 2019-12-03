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

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Iterator;

import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.attributes.FSBasicAttributes;
import org.knime.filehandling.core.connections.attributes.FSFileAttributes;
import org.knime.filehandling.core.connections.base.GenericPathUtil;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;

/**
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class URIPath implements FSPath {

    /**
     * The file system of this path.
     */
    protected final FileSystem m_fileSystem;

    /**
     * The URI this path wraps.
     */
    protected final URI m_uri;

    private final String[] m_pathComponents;

    private final boolean m_isAbsolute;

    private final boolean m_hasRootPathComponent;

    /**
     * Constructs a new URIPath.
     *
     * @param fileSystem the paths file system
     * @param uri the uri to be wrapped
     */
    protected URIPath(final FileSystem fileSystem, final URI uri) {
        m_fileSystem = fileSystem;

        m_uri = uri;

        m_hasRootPathComponent = UnixStylePathUtil.hasRootComponent(uri.getPath());

        m_isAbsolute = m_uri.getPath() != null && m_hasRootPathComponent;

        m_pathComponents = UnixStylePathUtil.toPathComponentsArray(m_uri.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        return m_fileSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAbsolute() {
        return m_isAbsolute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getRoot() {
        final URIPath toReturn = null;

        if (m_hasRootPathComponent) {
            try {
                return new URIPath(m_fileSystem,
                    new URI(m_uri.getScheme(), m_uri.getAuthority(), UnixStylePathUtil.SEPARATOR, null, null));
            } catch (final URISyntaxException ex) {
                throw new RuntimeException("Failed to get root of custom URI file system", ex);
            }
        }

        return toReturn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getFileName() {
        if (m_pathComponents.length == 0) {
            return null;
        }

        final String filename = m_pathComponents[m_pathComponents.length - 1];
        try {
            return new URIPath(m_fileSystem, new URI(null, null, filename, null, null));
        } catch (final URISyntaxException ex) {
            throw new RuntimeException("Failed to get file name of custom URI", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getParent() {
        if (m_pathComponents.length == 0) {
            return null;
        }
        if (m_pathComponents.length == 1) {
            return getRoot();
        }

        final StringBuilder parentBuilder = new StringBuilder(m_fileSystem.getSeparator());
        for (int i = 0; i < m_pathComponents.length - 1; i++) {
            parentBuilder.append(m_pathComponents[i]);
            parentBuilder.append(m_fileSystem.getSeparator());
        }

        try {
            return new URIPath(m_fileSystem,
                new URI(m_uri.getScheme(), m_uri.getAuthority(), parentBuilder.toString(), null, null));
        } catch (final URISyntaxException ex) {
            throw new RuntimeException("Failed to get parent of custom URI", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNameCount() {
        return m_pathComponents.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getName(final int index) {
        try {
            return new URIPath(m_fileSystem, new URI(null, null, m_pathComponents[index], null, null));
        } catch (final URISyntaxException ex) {
            throw new RuntimeException("Failed to get path component of custom URI", ex);
        }
    }

    /**
     * any watching on on URIs {@inheritDoc}
     */
    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        try {
            final String relativeSubpath = String.join("/", Arrays.copyOfRange(m_pathComponents, beginIndex, endIndex));
            return new URIPath(m_fileSystem, new URI(null, null, relativeSubpath, null, null));
        } catch (final URISyntaxException ex) {
            throw new RuntimeException("Failed to get path component of custom URI", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startsWith(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            return false;
        }
        return GenericPathUtil.startsWith(this, other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startsWith(final String other) {
        final Path otherPath = makeCustomURIPathOnCurrentFileSystem(other);
        return GenericPathUtil.startsWith(this, otherPath);
    }

    private URIPath makeCustomURIPathOnCurrentFileSystem(final String otherPath) {
        try {
            return new URIPath(m_fileSystem, new URI(m_uri.getScheme(), m_uri.getAuthority(), otherPath, null, null));
        } catch (final URISyntaxException ex) {
            throw new RuntimeException("Failed make path", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean endsWith(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            return false;
        }

        return GenericPathUtil.endsWith(this, other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean endsWith(final String other) {
        final Path otherPath = makeCustomURIPathOnCurrentFileSystem(other);
        return GenericPathUtil.endsWith(this, otherPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path normalize() {
        return new URIPath(m_fileSystem, m_uri.normalize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot resolve paths across different file systems");
        }

        final URIPath otherUriPath = (URIPath)other;

        if (other.isAbsolute()) {
            return other;
        }

        if (other.getNameCount() == 0) {
            return this;
        }

        final String resolvedPathString =
            UnixStylePathUtil.resolve(m_pathComponents, otherUriPath.m_pathComponents, m_hasRootPathComponent);

        try {
            return new URIPath(m_fileSystem, new URI(m_uri.getScheme(), m_uri.getAuthority(), resolvedPathString,
                otherUriPath.m_uri.getQuery(), otherUriPath.m_uri.getFragment()));
        } catch (final URISyntaxException ex) {
            throw new RuntimeException("Failed to resolve path", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(final String other) {
        if (other.isEmpty()) {
            return this;
        }

        try {
            final Path otherPath =
                new URIPath(m_fileSystem, new URI(m_uri.getScheme(), m_uri.getAuthority(), other, null, null));

            return resolve(otherPath);

        } catch (final URISyntaxException ex) {
            throw new RuntimeException("Failed to resolve path", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolveSibling(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot resolve sibling paths across different file systems");
        }

        if (other.isAbsolute()) {
            return other;
        }

        final Path parent = getParent();
        if (parent == null) {
            return other;
        } else {
            return parent.resolve(other);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolveSibling(final String other) {
        if (other.isEmpty()) {
            return this;
        }

        try {
            final Path otherPath =
                new URIPath(m_fileSystem, new URI(m_uri.getScheme(), m_uri.getAuthority(), other, null, null));

            return resolveSibling(otherPath);

        } catch (final URISyntaxException ex) {
            throw new RuntimeException("Failed to resolve path", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path relativize(final Path other) {
        // FIXME probably not important, but should be there for completeness
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI toUri() {
        return m_uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toAbsolutePath() {
        if (m_isAbsolute) {
            return this;
        } else {
            throw new IllegalStateException(format("Relative URI %s cannot be made absolute", m_uri));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        // just applying normalization here, because there is nothing else that we can do
        return toAbsolutePath().normalize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File toFile() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>[] events, final Modifier... modifiers)
        throws IOException {
        // cannot do any watching on on URIs
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>... events) throws IOException {
        // cannot do any watching on on URIs
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Path> iterator() {
        // cannot do any file listings on URIs
        throw new UnsupportedOperationException();
    }

    /**
     * Opens a {@link URLConnection} to the resource.
     *
     * @param openOutputConnection whether the connection should be opened for writing
     * @return an already connected {@link URLConnection}.
     * @throws IOException
     */
    public URLConnection openURLConnection() throws IOException {
        return openURLConnection(FileUtil.getDefaultURLTimeoutMillis());
    }

    /**
     * Opens a {@link URLConnection} to the resource.
     *
     * @param timeoutMillis Timeout in millis for the connect and read operations.
     * @return an already connected {@link URLConnection}.
     * @throws IOException
     */
    public URLConnection openURLConnection(final int timeoutMillis)
        throws IOException {
        final URL url = FileUtil.toURL(m_uri.toString());
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis);
        connection.connect();
        return connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot compare paths across different file systems");
        }

        final URIPath otherUriPath = (URIPath)other;
        return m_uri.compareTo(otherUriPath.m_uri);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final URIPath other = (URIPath)o;
        if (other.m_fileSystem != m_fileSystem) {
            return false;
        }

        return m_uri.equals(other.m_uri);
    }

    @Override
    public int hashCode() {
        int result = m_fileSystem.hashCode();
        result = 31 * result + m_uri.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return m_uri.toString();
    }

    @Override
    public FSFileAttributes getFileAttributes(final Class<?> type) {
        if (type == BasicFileAttributes.class) {
            return new FSFileAttributes(!isDirectory(), this, p -> new FSBasicAttributes(FileTime.fromMillis(0L),
                FileTime.fromMillis(0L), FileTime.fromMillis(0L), 0L, false, false));
        }
        throw new UnsupportedOperationException(String.format("only %s supported", BasicFileAttributes.class));
    }

    /**
     * @return whether this URI is assumed to be a directory (i.e. if the path ends with the path separator)
     */
    public boolean isDirectory() {
        return m_uri.getPath().endsWith(m_fileSystem.getSeparator());
    }
}
