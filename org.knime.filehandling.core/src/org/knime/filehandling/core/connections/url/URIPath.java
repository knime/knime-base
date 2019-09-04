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
import java.util.Arrays;
import java.util.Iterator;

import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.base.GenericPathUtil;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;

/**
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class URIPath implements Path {

    private final URIFileSystem m_fileSystem;

    private final URI m_uri;

    private final String[] m_pathComponents;

    private final boolean m_isAbsolute;

    private final boolean m_hasRootPathComponent;

    URIPath(final URIFileSystem fileSystem, final URI uri) {
        m_fileSystem = fileSystem;
        m_uri = uri;

        // FIXME this needs to be moved elsewhere because we have to allow relative URIs
        // make sure we have a proper URI with an absolute path
        //        if (m_uri.getScheme() == null) {
        //            throw new IllegalArgumentException(String.format("Custom URIs must start with a scheme"));
        //        }
        //
        //        if (m_uri.getAuthority() == null) {
        //            throw new IllegalArgumentException(
        //                String.format("Custom URIs have an authority component (e.g. host and port)"));
        //        }
        //
        //        if (m_uri.getPath() == null) {
        //            throw new IllegalArgumentException(String.format("Custom URIs must specify a path"));
        //        }

        m_hasRootPathComponent = UnixStylePathUtil.hasRootComponent(uri.getPath());

        m_isAbsolute = m_uri.getScheme() != null && m_uri.getAuthority() != null && m_uri.getPath() != null
            && m_hasRootPathComponent;

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
        URIPath toReturn = null;

        if (m_hasRootPathComponent) {
            try {
                return new URIPath(m_fileSystem,
                    new URI(m_uri.getScheme(), m_uri.getAuthority(), UnixStylePathUtil.SEPARATOR, null, null));
            } catch (URISyntaxException ex) {
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
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Failed to get file name of custom URI", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getParent() {
        if (m_pathComponents.length < 2) {
            return null;
        }

        final String parent = m_pathComponents[m_pathComponents.length - 2];
        try {
            return new URIPath(m_fileSystem, new URI(m_uri.getScheme(), m_uri.getAuthority(), parent, null, null));
        } catch (URISyntaxException ex) {
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
        } catch (URISyntaxException ex) {
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
        } catch (URISyntaxException ex) {
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
        } catch (URISyntaxException ex) {
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
        } catch (URISyntaxException ex) {
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

        } catch (URISyntaxException ex) {
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

        } catch (URISyntaxException ex) {
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
    public URLConnection openURLConnection(final int timeoutMillis) throws IOException {
        final URL url = m_uri.toURL();
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
}
