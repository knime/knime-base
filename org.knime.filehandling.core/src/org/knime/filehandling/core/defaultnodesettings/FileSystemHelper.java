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
 *   Sep 6, 2019 (bjoern): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Optional;

import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.knimerelativeto.LocalRelativeToFileSystemProvider;
import org.knime.filehandling.core.connections.knimeremote.KNIMERemoteFileSystemProvider;
import org.knime.filehandling.core.connections.url.URIFileSystemProvider;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection.Type;

/**
 * Utility class to obtain a NIO {@link FileSystem}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class FileSystemHelper {

    /**
     * Method to obtain the file system for a given settings model.
     *
     * @param fs optional {@link FSConnection}.
     * @param settings {@link SettingsModelFileChooser2} instance.
     * @param timeoutInMillis timeout in milliseconds, or -1 if not applicable.
     * @return {@link FileSystem} to use.
     * @throws IOException if custom URL is invalid.
     */
    @SuppressWarnings("resource")
    public static final FileSystem retrieveFileSystem(final Optional<FSConnection> fs,
        final SettingsModelFileChooser2 settings, final int timeoutInMillis) throws IOException {

        final FileSystemChoice choice = settings.getFileSystemChoice();
        final FileSystem toReturn;

        switch (choice.getType()) {
            case LOCAL_FS:
                toReturn = FileSystems.getDefault();
                break;
            case CUSTOM_URL_FS:
                final URI uri = URI.create(settings.getPathOrURL().replace(" ", "%20"));
                toReturn =
                    new URIFileSystemProvider(timeoutInMillis).newFileSystem(uri, null);
                break;
            case KNIME_MOUNTPOINT:
                final String knimeFileSystem = settings.getKnimeMountpointFileSystem();
                if (knimeFileSystem == null) {
                    throw new IOException("Invalid Mountpoint selection");
                }
                final KNIMEConnection connection =
                    KNIMEConnection.getOrCreateMountpointAbsoluteConnection(knimeFileSystem);

                final URI remoteFsKey = URI.create(connection.getSchemeAndHost());
                toReturn = new KNIMERemoteFileSystemProvider().getOrCreateFileSystem(remoteFsKey, null);
                break;
            case KNIME_FS:
                final String knimeFileSystemHost = settings.getKNIMEFileSystem();
                final Type connectionTypeForHost = KNIMEConnection.connectionTypeForHost(knimeFileSystemHost);

                final URI fsKey = URI.create(connectionTypeForHost.getSchemeAndHost());
                toReturn = LocalRelativeToFileSystemProvider.getOrCreateFileSystem(fsKey);
                break;
            case CONNECTED_FS:
                toReturn = fs
                    .orElseThrow(
                        () -> new IOException("No file system connection available for \"" + choice.getId() + "\""))
                    .getFileSystem();
                break;
            default:
                throw new IOException("Unsupported file system choice: " + choice.getType());
        }

        return toReturn;
    }

}
