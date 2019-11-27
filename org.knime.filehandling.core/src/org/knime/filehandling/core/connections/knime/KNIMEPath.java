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
 *   Sep 3, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.knime;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.base.GenericPathUtil;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;

/**
 * Path implementation needed for browsing KNIME mount points.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class KNIMEPath implements Path {

    private final KNIMEFileSystem m_fileSystem;

    private Path m_path;

    /**
     * Creates a new KNIMEPath.
     *
     * @param fileSystem the file system
     * @param first first part of the path
     * @param more subsequent parts of the path
     */
    public KNIMEPath(final KNIMEFileSystem fileSystem, final String first, final String... more) {
        m_fileSystem = fileSystem;
        m_path = Paths.get(first, more);
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
        return m_path.isAbsolute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getRoot() {
        return m_fileSystem.getBasePath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getFileName() {
        Path name = m_path.getFileName();
        return name == null ? null: new KNIMEPath(m_fileSystem, name.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getParent() {
        Path parent = m_path.getParent();
        return parent == null? null : new KNIMEPath(m_fileSystem, parent.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNameCount() {
        return m_path.getNameCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getName(final int index) {
        return new KNIMEPath(m_fileSystem, m_path.getName(index).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        return new KNIMEPath(m_fileSystem, m_path.subpath(beginIndex, endIndex).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startsWith(final Path other) {
        if (!other.getFileSystem().equals(m_fileSystem)) {
            return false;
        }

        return GenericPathUtil.startsWith(this, other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startsWith(final String other) {
        KNIMEPath knimePath = new KNIMEPath(m_fileSystem, other);
        return GenericPathUtil.startsWith(this, knimePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean endsWith(final Path other) {
        if (!other.getFileSystem().equals(m_fileSystem)) {
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
        return new KNIMEPath(m_fileSystem, m_path.normalize().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(final Path other) {
        checkIfValidForResolving(other);

        KNIMEPath otherKNIMEPath = (KNIMEPath) other;
        return new KNIMEPath(m_fileSystem, m_path.resolve(otherKNIMEPath.m_path).toString());
    }

    private void checkIfValidForResolving(final Path path) {
        if (!(path instanceof KNIMEPath)) {
            throw new IllegalArgumentException("Can only resolve path against another KNIMEPath");
        }

        if (path.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot resolve paths across different file systems");
        }
    }

    private String pathAsString() {
        return m_path.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(final String other) {
        return new KNIMEPath(m_fileSystem, m_path.resolve(other).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolveSibling(final Path other) {
        checkIfValidForResolving(other);

        KNIMEPath otherKNIMEPath = (KNIMEPath) other;
        return new KNIMEPath(m_fileSystem, m_path.resolveSibling(otherKNIMEPath.m_path).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolveSibling(final String other) {

        return new KNIMEPath(m_fileSystem, m_path.resolveSibling(other).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path relativize(final Path other) {
        if (other.getFileSystem() != m_fileSystem) {
            throw new IllegalArgumentException("Cannot relativize paths across different file systems");
        }
        KNIMEPath otherPath = (KNIMEPath) other;

        Path relativized = m_path.relativize(otherPath.m_path);
        return new KNIMEPath(m_fileSystem, relativized.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI toUri() {
        return m_path.toUri();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toAbsolutePath() {
        if (m_path.isAbsolute()) {
            return new KNIMEPath(m_fileSystem, pathAsString());
        } else {
            return new KNIMEPath(m_fileSystem, m_fileSystem.getBasePath().toString(), pathAsString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        return toAbsolutePath().normalize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File toFile() {
        return new File(pathAsString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>[] events, final Modifier... modifiers)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Path> iterator() {
        List<Path> pathComponents = //
            Stream.of(m_path.iterator()) //
                .map(path -> new KNIMEPath(m_fileSystem, path.toString())) //
                .collect(Collectors.toList()); //

        return pathComponents.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Path other) {
        if (other instanceof KNIMEPath) {
            KNIMEPath otherPath = (KNIMEPath) other;
            return m_path.compareTo(otherPath.m_path);
        }

        return -1;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final KNIMEPath other = (KNIMEPath)o;
        return m_path.equals(other.m_path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_path.hashCode();
    }

    @Override
    public String toString() {
        return pathAsString();
    }

    /**
     * Creates a local path using the JVMs default file system. If the path is relative, it is resolved against the
     * base location of the underlying file system.
     *
     * @return a local equivalent of this path
     */
    public Path toLocalPath() {
        return m_fileSystem.getBasePath().resolve(m_path);
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
        final URL url = getURL();
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis);
        connection.connect();
        return connection;
    }

    /**
     * Creates a KNIME URL of this path, concatenating the underlying file systems scheme and host with
     * the relative path.
     *
     * @return the full KNIME URL of this path
     */
    public URL getURL() {
        String schemeAndHost = m_fileSystem.getConnectionType().getSchemeAndHost();

        // TODO TU: check apache for URL encoding
        String unixStylePath = UnixStylePathUtil.asUnixStylePath(m_path.toString()).replaceAll(" ", "%20");
        URI create = URI.create(schemeAndHost + m_fileSystem.getSeparator() + unixStylePath);
        try {
            URL url = create.toURL();
            return url;
        } catch (MalformedURLException ex) {
            return null;
        }
    }

}
