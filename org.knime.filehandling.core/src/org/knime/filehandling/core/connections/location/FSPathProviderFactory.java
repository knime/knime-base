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
 *   Apr 24, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections.location;

import java.io.IOException;
import java.util.Optional;

import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.knimerelativeto.LocalRelativeToFSConnection;
import org.knime.filehandling.core.connections.knimeremote.KNIMERemoteFSConnection;
import org.knime.filehandling.core.connections.local.LocalFSConnection;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection.Type;

/**
 * Abstract factory superclass to obtain {@link FSPathProvider} instances. To get a concrete factory you can use
 * {@link #newFactory(Optional, FSLocationSpec)}. Note that instances of this class are {@link AutoCloseable}. The
 * intended usage pattern to map {@link FSLocation} to {@link FSPath} is as follows:
 *
 * <pre>
 * FSLocationSpec locSpec = ...
 * try (FSPathProviderFactory factory = FSPathProviderFactory.newFactory(optionalPortObjectConnection, locSpec)) {
 *   for (FSLocation loc : locations) {
 *     try (FSPathProvider pathProvider = factory.create(loc)) {
 *       FSPath path = pathProvider.getPath();
 *       [...]
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @since 4.2
 */
public abstract class FSPathProviderFactory implements AutoCloseable {

    /**
     * Creates a {@link FSPathProvider} that maps the given {@link FSLocation} to an {@link FSPath}.
     *
     * @param fsLocation The {@link FSLocation} to map.
     * @return a {@link FSPathProvider} that maps the given {@link FSLocation} to an {@link FSPath}.
     */
    public abstract FSPathProvider create(FSLocation fsLocation);

    @Override
    public abstract void close() throws IOException;

    /**
     * Factory method to obtain a concrete {@link FSPathProviderFactory} that matches a given {@link FSLocationSpec}.
     *
     * @param portObjectConnection Optional port object connection.
     * @param fsLocationSpec
     * @return a concrete factory instance.
     */
    @SuppressWarnings("resource")
    public static FSPathProviderFactory newFactory(final Optional<FSConnection> portObjectConnection,
        final FSLocationSpec fsLocationSpec) {

        final Choice choice = Choice.valueOf(fsLocationSpec.getFileSystemType());
        switch (choice) {
            case LOCAL_FS:
                return new DefaultFSPathProviderFactory(new LocalFSConnection());
            case KNIME_FS:
                return new DefaultFSPathProviderFactory(createRelativeToFSConnection(fsLocationSpec));
            case KNIME_MOUNTPOINT:
                return new DefaultFSPathProviderFactory(createMountpointConnection(fsLocationSpec));
            case CUSTOM_URL_FS:
                return new URLFSPathProviderFactory();
            case CONNECTED_FS:
                return createConnectedFSPathProviderFactory(portObjectConnection, fsLocationSpec);
            default:
                throw new IllegalArgumentException(
                    String.format("Cannot create FSPathProviderFactory for NULL FSLocation."));
        }
    }

    @SuppressWarnings("resource")
    private static FSPathProviderFactory createConnectedFSPathProviderFactory(
        final Optional<FSConnection> portObjectConnection, final FSLocationSpec fsLocationSpec) {

        if (portObjectConnection.isPresent()) {
            final FSConnection fsConnection = portObjectConnection.get();
            fsConnection.getFileSystem().checkCompatibility(fsLocationSpec);
            return new DefaultFSPathProviderFactory(fsConnection);
        } else {
            throw new IllegalArgumentException(
                "No FSConnection was provided although FSLocationSpec indicates a CONNECTED_FS");
        }
    }

    private static FSConnection createMountpointConnection(final FSLocationSpec fsLocationSpec) {
        final Optional<String> specifier = fsLocationSpec.getFileSystemSpecifier();

        if (!specifier.isPresent()) {
            throw new IllegalArgumentException(
                "Invalid FSLocation for 'Mountpoint'. It must specify the name of the mountpoint.");
        }

        final KNIMEConnection con = KNIMEConnection.getOrCreateMountpointAbsoluteConnection(specifier.get());
        return new KNIMERemoteFSConnection(con);
    }

    private static FSConnection createRelativeToFSConnection(final FSLocationSpec fsLocationSpec) {
        if (!fsLocationSpec.getFileSystemSpecifier().isPresent()) {
            throw new IllegalArgumentException(
                "Invalid FSLocation for 'Relative to'. It must specify either workflow or mountpoint");
        }
        final Type type = KNIMEConnection.connectionTypeForHost(fsLocationSpec.getFileSystemSpecifier().get());
        return new LocalRelativeToFSConnection(type);
    }
}
