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
 *   20.08.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.s3;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.util.HashMap;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.FileSystemBrowser;
import org.knime.core.node.util.LocalFileSystemBrowser;
import org.knime.filehandling.core.connections.FSConnection;

/**
 * The Amazon S3 implementation of the {@link FSConnection} interface.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class S3Connection implements FSConnection {

    S3CloudConnectionInformation m_connInfo;

    S3FileSystemProvider m_provider = new S3FileSystemProvider();

    /**
     * Creates a new {@link S3Connection} for the given connection information.
     *
     * @param connInfo the cloud connection information
     */
    public S3Connection(final S3CloudConnectionInformation connInfo) {
        m_connInfo = connInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem() {
        final HashMap<String, S3CloudConnectionInformation> env = new HashMap<>();
        env.put(S3FileSystemProvider.CONNECTION_INFORMATION, m_connInfo);
        try {
            final URI uri = getFileSystemURI();
            if (m_provider.isOpen(uri)) {
                return m_provider.getFileSystem(uri);
            } else {
                return m_provider.newFileSystem(uri, env);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the FileSystem for this connection
     *
     * @throws IOException if an I/O error occurs
     */
    public void closeFileSystem() throws IOException {
        m_provider.getFileSystem(getFileSystemURI()).close();
    }

    /**
     * @return a URI including switchroleInformation if necessary for identification of the fileSystem
     */
    public URI getFileSystemURI() {
        URI uri = m_connInfo.toURI();
        final StringBuilder sb = new StringBuilder();
        if (m_connInfo.switchRole()) {
            sb.append(m_connInfo.getSwitchRoleAccount());
            sb.append(":");
            sb.append(m_connInfo.getSwitchRoleName());
            sb.append(":");

            sb.append(m_connInfo.getUser());
            try {
                uri = new URI(m_connInfo.getProtocol(), sb.toString(), m_connInfo.getHost(), m_connInfo.getPort(), null,
                    null, null);
            } catch (final URISyntaxException e) {
                // Should not happen
                NodeLogger.getLogger(getClass()).coding(e.getMessage(), e);
            }
        }
        return uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystemBrowser getFileSystemBrowser() {
        // FIXME implement this
        return new LocalFileSystemBrowser();
    }

}
