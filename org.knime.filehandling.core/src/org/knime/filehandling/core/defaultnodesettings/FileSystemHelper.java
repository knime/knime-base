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

import java.nio.file.FileSystem;
import java.util.Optional;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.RelativeTo;

/**
 * Utility class to obtain a {@link FSConnection}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 */
public final class FileSystemHelper {

    private FileSystemHelper() {
        // static utility class
    }

    /**
     * Method to obtain the file system for a given settings model.
     *
     * @param portObjectConnection optional {@link FSConnection}.
     * @param settings {@link SettingsModelFileChooser2} instance.
     * @param timeoutInMillis timeout in milliseconds, or -1 if not applicable.
     * @return {@link FileSystem} to use.
     */
    public static final FSConnection retrieveFSConnection(final Optional<FSConnection> portObjectConnection,
        final SettingsModelFileChooser2 settings, final int timeoutInMillis) {

        final FileSystemChoice choice = settings.getFileSystemChoice();
        switch (choice.getType()) {
            case LOCAL_FS:
                return DefaultFSConnectionFactory.createLocalFSConnection();
            case CUSTOM_URL_FS:
                return DefaultFSConnectionFactory.createCustomURLConnection(settings.getPathOrURL(), timeoutInMillis);
            case KNIME_MOUNTPOINT:
                final var mountID = settings.getKnimeMountpointFileSystem();
                return DefaultFSConnectionFactory.createMountpointConnection(mountID);
            case KNIME_FS:
                final RelativeTo type = RelativeTo.fromSettingsValue(settings.getKNIMEFileSystem());
                return DefaultFSConnectionFactory.createRelativeToConnection(type);
            case CONNECTED_FS:
                return portObjectConnection.orElseThrow(() -> new IllegalArgumentException(
                    "No file system connection available for \"" + choice.getId() + "\""));
            default:
                throw new IllegalArgumentException("Unsupported file system choice: " + choice.getType());
        }
    }

    /**
     * Method to obtain the file system for a given {@link FSLocation}.
     *
     * @param portObjectConnection optional {@link FSConnection}.
     * @param location {@link FSLocation} instance.
     * @return {@link FileSystem} to use.
     */
    @SuppressWarnings("resource")
    public static Optional<FSConnection> retrieveFSConnection(final Optional<FSConnection> portObjectConnection,
        final FSLocation location) {
        final FSCategory category = location.getFSCategory();
        switch (category) {
            case CONNECTED:
                return portObjectConnection;
            case CUSTOM_URL:
                return Optional.of(DefaultFSConnectionFactory.createCustomURLConnection(location));
            case RELATIVE:
                final RelativeTo type = extractRelativeToType(location);
                return Optional.of(DefaultFSConnectionFactory.createRelativeToConnection(type));
            case MOUNTPOINT:
                final KNIMEConnection connection = extractMountpoint(location);
                checkMountpointCanCreateConnection(location, connection);
                return Optional.of(DefaultFSConnectionFactory.createMountpointConnection(connection.getId()));
            case LOCAL:
                return Optional.of(DefaultFSConnectionFactory.createLocalFSConnection());
            default:
                throw new IllegalArgumentException("Unknown file system choice: " + category);

        }
    }

    private static void checkMountpointCanCreateConnection(final FSLocation location,
        final KNIMEConnection connection) {
        // we would already have failed if there was no mountpoint in the specifier
        String mountpoint = location.getFileSystemSpecifier().orElseThrow(IllegalStateException::new);
        CheckUtils.checkArgument(connection.isValid(), "The selected mountpoint '%s' is unknown.", mountpoint);
        CheckUtils.checkArgument(connection.isConnected(), "The selected mountpoint '%s' is not connected.",
            mountpoint);
    }

    /**
     * Checks if a connection can be retrieved for the provided parameters.
     *
     * @param portObjectConnection connection provided by the optional input port of fs nodes (use
     *            {@link Optional#empty()} in case the node has no fs port)
     * @param location {@link FSLocation} for which a connection should be created
     * @return {@code true} if a connection can be retrieved for the provided parameters
     */
    public static boolean canRetrieveFSConnection(final Optional<FSConnection> portObjectConnection,
        final FSLocation location) {
        final FSCategory category = location.getFSCategory();
        if (category == FSCategory.CONNECTED) {
            return portObjectConnection.isPresent();
        } else if (category == FSCategory.MOUNTPOINT) {
            final KNIMEConnection connection = extractMountpoint(location);
            return connection.isValid() && connection.isConnected();
        } else {
            // for the other fs types, it is always possible to create a connection
            return true;
        }
    }

    /**
     * Indicates whether browsing is supported by the file system of {@link FSLocation location}.
     *
     * @param portObjectConnection {@link Optional} that contains the file system provided via the file system port of a
     *            node
     * @param location to browse
     * @return {@code true} if browsing is supported, {@code false} otherwise
     */
    public static boolean canBrowse(final Optional<FSConnection> portObjectConnection, final FSLocation location) {
        final FSCategory category = location.getFSCategory();
        if (category == FSCategory.CUSTOM_URL) {
            return false;
        } else if (category == FSCategory.CONNECTED) {
            return portObjectConnection.map(FSConnection::supportsBrowsing).orElse(false);
        } else {
            return canRetrieveFSConnection(portObjectConnection, location);
        }
    }

    private static RelativeTo extractRelativeToType(final FSLocation location) {
        final String specifier =
            location.getFileSystemSpecifier().orElseThrow(() -> new IllegalArgumentException(String
                .format("The provided relative to location '%s' does not specify the relative to type.", location)));
        return RelativeTo.fromSettingsValue(specifier);
    }

    private static KNIMEConnection extractMountpoint(final FSLocation location) {
        final String knimeFileSystem = location.getFileSystemSpecifier().orElseThrow(() -> new IllegalArgumentException(
            String.format("The provided mountpoint location '%s' does not specify a mountpoint.", location)));
        return KNIMEConnection.getOrCreateMountpointAbsoluteConnection(knimeFileSystem);
    }
}
