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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;

import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.base.UnixStylePath;

/**
 *  The {@link Path} implementation for custom URLs
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
class URIPath extends UnixStylePath {

    private final URI m_uri;

    /**
     * Constructs a new URIPath, passing the URI directly (which can include query and fragment).
     *
     * @param fileSystem the paths file system.
     * @param pathString The (decoded) path of the URI.
     * @param uri The URI.
     */
    URIPath(final URIFileSystem fileSystem, final String pathString, final URI uri) {
        super(fileSystem, pathString);
        m_uri = uri;
    }

    /**
     * Constructs a new URIPath from name components. This constructor does not allow passing a query or fragment.
     *
     * @param fileSystem The file system.
     * @param first The path string or initial part of the path string
     * @param more Additional strings to be joined to form the path string
     */
    URIPath(final URIFileSystem fileSystem, final String first, final String... more) {
        super(fileSystem, concatenatePathSegments(fileSystem.getSeparator(), first, more));
        m_uri = buildURI(fileSystem, concatenatePathSegments(fileSystem.getSeparator(), first, more));
    }

    private static URI buildURI(final URIFileSystem fileSystem, final String pathString) {
        final URI baseURI = URI.create(fileSystem.getBaseURI() + fileSystem.getSeparator());

        final String absolutePathString = pathString.startsWith(fileSystem.getSeparator()) //
                ? pathString //
                : (fileSystem.getSeparator() + pathString);

        try {
            return new URI(baseURI.getScheme(), baseURI.getAuthority(), absolutePathString, null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Failed to instantiate URI, this is probably a bug.", ex);
        }
    }

    /**
     * @return the underlying URI of this path, including query and fragment.
     */
    URI getURI() {
        return m_uri;
    }

    @Override
    public URIFileSystem getFileSystem() {
        return (URIFileSystem) super.getFileSystem();
    }

    /**
     * Opens a {@link URLConnection} to the resource.
     *
     * @param timeoutMillis Timeout in millis for the connect and read operations.
     * @return an already connected {@link URLConnection}.
     * @throws IOException
     */
    public URLConnection openURLConnection(final int timeoutMillis) throws IOException {
        final URL url = FileUtil.toURL(m_uri.toString());
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis);
        connection.connect();
        return connection;
    }

    /**
     * Opens a {@link URLConnection} to the resource.
     *
     * @param timeoutMillis Timeout in millis for the connect and read operations.
     * @param httpMethod the HTTP method in case the url is http
     * @return an already connected {@link URLConnection}.
     * @throws IOException
     */
    public URLConnection openURLConnection(final int timeoutMillis, final String httpMethod) throws IOException {
        final URL url = FileUtil.toURL(m_uri.toString());
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis);
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection)connection).setRequestMethod(httpMethod);
        }
        connection.connect();
        return connection;
    }

    @Override
    public Path normalize() {
        // do not normalize URIPaths as this breaks URLs like knime://knime.workflow/../bla.csv,
        // which gets parsed into a relative path ../bla.csv. However, when trying to read this
        // file, the base file system does toAbsolutePath().normalize(), which results in /bla.csv
        // Path normalization should anyway be done be the URL handler, that opens the input/output streams.
        return this;
    }

    @SuppressWarnings("resource")
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

    /**
     * @return whether this URI is assumed to be a directory (i.e. if the path ends with the path separator)
     */
    public boolean isDirectory() {
        return m_uri.getPath().endsWith(m_fileSystem.getSeparator());
    }

    @SuppressWarnings("resource")
    @Override
    public FSLocation toFSLocation() {
        final FSFileSystem<?> fs = getFileSystem();
        return new FSLocation(fs.getFileSystemCategory(), //
            fs.getFileSystemSpecifier().orElseGet(() -> null),
            m_uri.toString());
    }
}
