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
 *   Nov 11, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.knimeremote;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.httpclient.util.URIUtil;
import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.base.GenericPathUtil;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;
import org.knime.filehandling.core.filechooser.NioFile;
import org.knime.filehandling.core.util.MountPointFileSystemAccessService;

/**
 * Paths to a remote KNIME Server.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class KNIMERemotePath implements FSPath {

    /**
     * The file system of this path.
     */
    protected final FileSystem m_fileSystem;

    private final String m_path;

    private final String[] m_pathComponents;

    private final boolean m_isAbsolute;

    /**
     * Constructs a {@code KNIMERemotePath} from a path string, or a sequence of strings that when joined form a path
     * string.
     *
     * @param fileSystem the filesystem the path belongs to
     * @param first the path string or initial part of the path string
     * @param more additional strings to be joined to form the path string
     */
    protected KNIMERemotePath(final FileSystem fileSystem, final String first, final String... more) {
        m_fileSystem = fileSystem;

        // Called to check whether first or more contain illegal characters!
        Paths.get(first, more);

        String path = first;
        if (more.length > 0) {
            if (!first.endsWith(m_fileSystem.getSeparator()) && !more[0].startsWith(m_fileSystem.getSeparator())) {
                path = path + m_fileSystem.getSeparator();
            }
            path += String.join(UnixStylePathUtil.SEPARATOR, more);
        }
        m_path = path.isEmpty() ? m_fileSystem.getSeparator() : path;
        m_isAbsolute = UnixStylePathUtil.hasRootComponent(m_path);
        m_pathComponents = UnixStylePathUtil.toPathComponentsArray(m_path);
    }

    /**
     * Constructs a {@code KNIMERemotePath} from a URI.
     *
     * @param fileSystem the paths file system
     * @param uri the uri to be wrapped
     */
    protected KNIMERemotePath(final FileSystem fileSystem, final URI uri) {
        m_fileSystem = fileSystem;
        m_path = uri.getPath();

        m_isAbsolute = UnixStylePathUtil.hasRootComponent(m_path);
        m_pathComponents = UnixStylePathUtil.toPathComponentsArray(m_path);
    }

    @SuppressWarnings("unchecked")
    @Override
    public FSFileSystem<KNIMERemotePath> getFileSystem() {
        return (FSFileSystem<KNIMERemotePath>)m_fileSystem;
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
        final KNIMERemotePath toReturn = null;

        if (m_isAbsolute) {
            return new KNIMERemotePath(m_fileSystem, m_fileSystem.getSeparator());
        }

        return toReturn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getFileName() {
        if (m_pathComponents.length == 0) {
            return this;
        }

        final String filename = m_pathComponents[m_pathComponents.length - 1];
        try {
            return new KNIMERemotePath(m_fileSystem, new URI(null, null, filename, null, null));
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
        parentBuilder.deleteCharAt(parentBuilder.length() - 1);

        return new KNIMERemotePath(m_fileSystem, parentBuilder.toString());

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
            return new KNIMERemotePath(m_fileSystem, new URI(null, null, m_pathComponents[index], null, null));
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
            return new KNIMERemotePath(m_fileSystem, new URI(null, null, relativeSubpath, null, null));
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
        return m_path.startsWith(other);
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
        return m_path.endsWith(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path normalize() {
        return new KNIMERemotePath(m_fileSystem, m_path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot resolve paths across different file systems");
        }

        final KNIMERemotePath otherKNIMERemotePath = (KNIMERemotePath)other;

        if (other.isAbsolute()) {
            return other;
        }

        if (other.getNameCount() == 0) {
            return this;
        }

        final String resolvedPathString =
            UnixStylePathUtil.resolve(m_pathComponents, otherKNIMERemotePath.m_pathComponents, m_isAbsolute);

        return new KNIMERemotePath(m_fileSystem, resolvedPathString);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(final String other) {
        if (other.isEmpty()) {
            return this;
        }

        final Path otherPath = new KNIMERemotePath(m_fileSystem, other);

        return resolve(otherPath);

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

        final Path otherPath = new KNIMERemotePath(m_fileSystem, other);

        return resolveSibling(otherPath);

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
     * Creates a KNIME URL of the path of the form:
     *
     * knime://[mount-point-id]/path/to/resource
     *
     * @return the KNIME URL of this remote path
     */
    public URL toURL() {
        try {
            return toUri().toURL();
        } catch (final MalformedURLException ex) {
            throw new IllegalStateException("Failed to create valid URL: " + ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI toUri() {
        try {
            final KNIMERemoteFileSystem knimeFS = (KNIMERemoteFileSystem) m_fileSystem;
            final String mountpoint = knimeFS.getMountpoint();
            final String encodedPath = URIUtil.encodePath(UnixStylePathUtil.asUnixStylePath(m_path));
            return URI.create("knime://" + mountpoint + encodedPath);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create valid URI: " + ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toAbsolutePath() {
        if (m_isAbsolute) {
            return this;
        } else {
            throw new IllegalStateException(format("Relative Path %s cannot be made absolute", m_path));
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
        return new NioFile(m_path, m_fileSystem);
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
        final URL url = toUri().toURL();
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

        final KNIMERemotePath otherUriPath = (KNIMERemotePath)other;
        return m_path.compareTo(otherUriPath.m_path);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final KNIMERemotePath other = (KNIMERemotePath)o;
        if (other.m_fileSystem != m_fileSystem) {
            return false;
        }

        return m_path.equals(other.m_path);
    }

    @Override
    public int hashCode() {
        int result = m_fileSystem.hashCode();
        result = 31 * result + m_path.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return m_path.toString();
    }

    public boolean isWorkflow() {
        return MountPointFileSystemAccessService.instance().isWorkflow(toUri());
    }
}
