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

import static java.lang.String.format;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Optional;

import org.knime.core.node.FSConnectionFlowVariableProvider;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.connections.url.URIFileSystemProvider;

/**
 * Utility class to obtain a NIO {@link FileSystem}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class FileSystemHelper {

    /**
     * Method to obtain the file system for a given settings model.
     *
     * @param provider
     * @param settings
     * @return
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public static final FileSystem retrieveFileSystem(final FSConnectionFlowVariableProvider provider,
        final SettingsModelFileChooser2 settings) throws IOException {

        final FileSystemChoice choice = FileSystemChoice.getChoiceFromId(settings.getFileSystem());
        final FileSystem toReturn;

        switch (choice.getType()) {
            case LOCAL_FS:
                toReturn = FileSystems.getDefault();
                break;
            case CUSTOM_URL_FS:
                toReturn = URIFileSystemProvider.getInstance().newFileSystem(URI.create(settings.getPathOrURL()), null);
                break;
            case KNIME_FS:
                // FIXME: Return correct FileSystem
                toReturn = FileSystems.getDefault();
                break;
            case FLOW_VARIABLE_FS:
                final String flowVariableName = choice.getId();
                toReturn = mapFlowVariableToFileSystem(provider, flowVariableName);
                break;
            default:
                throw new IOException("Unsupported file system choice: " + choice.getType());
        }

        return toReturn;
    }

    private static FileSystem mapFlowVariableToFileSystem(final FSConnectionFlowVariableProvider provider,
        final String flowVariableName) {

        final Optional<String> connectionKey = provider.connectionKeyOf(flowVariableName);
        if (!connectionKey.isPresent()) {
            throw new IllegalArgumentException(format("%s is not a connection flow variable", flowVariableName));
        }

        final Optional<FSConnection> optConn = FSConnectionRegistry.getInstance().retrieve(connectionKey.get());
        if (!optConn.isPresent()) {
            throw new IllegalArgumentException(format("No connection found for flow variable %s", flowVariableName));
        }

        return optConn.get().getFileSystem();
    }
}
