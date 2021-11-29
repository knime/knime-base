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
 *   Apr 6, 2020 (bjoern): created
 */
package org.knime.filehandling.core.fs.knime.local.mountpoint;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;

import org.knime.core.node.util.FileSystemBrowser;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.base.WorkflowAwareFileSystemBrowser;
import org.knime.filehandling.core.connections.config.MountpointFSConnectionConfig;

/**
 * {@link FSConnection} for the Explorer-based Mountpoint file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class LocalMountpointFSConnection implements FSConnection {

    private final LocalMountpointFileSystem m_fileSystem;

    private final WorkflowAwareFileSystemBrowser m_browser;

    /**
     * Constructor.
     *
     * @param config The config for this {@link FSConnection}.
     * @throws IOException
     */
    public LocalMountpointFSConnection(final MountpointFSConnectionConfig config) throws IOException {

        final Path localRoot = determineLocalRootFolder(config.getMountID());

        m_fileSystem = new LocalMountpointFileSystem(config, localRoot);
        m_browser = new WorkflowAwareFileSystemBrowser(m_fileSystem, //
            m_fileSystem.getWorkingDirectory(), //
            m_fileSystem.getWorkingDirectory());
    }

    private static Path determineLocalRootFolder(final String mountID) throws IOException {

        try {
            final var knimeUrl = new URI(String.format("knime://%s/", mountID));
            final var localFile = ResolverUtil.resolveURItoLocalFile(knimeUrl);

            return Optional.ofNullable(localFile) //
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Mountpoint %s is unknown or a remote mountpoint", mountID))) //
                .toPath();
        } catch (IOException | URISyntaxException ex) {
            throw new IOException("Could not determine local folder of mountpoint " + mountID, ex);
        }
    }

    @Override
    public FSFileSystem<?> getFileSystem() {
        return m_fileSystem;
    }

    @Override
    public FileSystemBrowser getFileSystemBrowser() {
        return m_browser;
    }
}
