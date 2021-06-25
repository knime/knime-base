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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.WorkflowAwarePath;
import org.knime.filehandling.core.connections.base.UnixStylePath;
import org.knime.filehandling.core.util.MountPointFileSystemAccessService;

/**
 * Paths to a remote KNIME Server.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
class KNIMERemotePath extends UnixStylePath implements WorkflowAwarePath {

    /**
     * Constructs a {@code KNIMERemotePath} from a path string, or a sequence of strings that when joined form a path
     * string.
     *
     * @param fileSystem the filesystem the path belongs to
     * @param first the path string or initial part of the path string
     * @param more additional strings to be joined to form the path string
     */
    KNIMERemotePath(final KNIMERemoteFileSystem fileSystem, final String first, final String... more) {
        super(fileSystem, first, more);
    }

    /**
     * Constructs a {@code KNIMERemotePath} from a URI.
     *
     * @param fileSystem the paths file system
     * @param uri the uri to be wrapped
     */
    KNIMERemotePath(final KNIMERemoteFileSystem fileSystem, final URI uri) {
        super(fileSystem, uri.getPath());
    }

    @Override
    public KNIMERemoteFileSystem getFileSystem() {
        return (KNIMERemoteFileSystem)super.getFileSystem();
    }

    URI toKNIMEProtocolURI() {
        try {
            return LegacyKNIMEUrlExporterFactory.getInstance().getExporter().toUri(this);
        } catch (final URISyntaxException ex) {
            throw new IllegalStateException("Failed to create valid URL: " + ex.getMessage(), ex);
        }
    }

    /**
     * Opens a {@link URLConnection} to the resource.
     *
     * @return an already connected {@link URLConnection}.
     * @throws IOException
     */
    URLConnection openURLConnection() throws IOException {
        return openURLConnection(FileUtil.getDefaultURLTimeoutMillis());
    }

    /**
     * Opens a {@link URLConnection} to the resource.
     *
     * @param timeoutMillis Timeout in millis for the connect and read operations.
     * @return an already connected {@link URLConnection}.
     * @throws IOException
     */
    URLConnection openURLConnection(final int timeoutMillis) throws IOException {
        final URL url = toKNIMEProtocolURI().toURL();
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis);
        connection.connect();
        return connection;
    }

    @Override
    public boolean isWorkflow() {
        return MountPointFileSystemAccessService.instance().isWorkflow(toKNIMEProtocolURI());
    }
}
