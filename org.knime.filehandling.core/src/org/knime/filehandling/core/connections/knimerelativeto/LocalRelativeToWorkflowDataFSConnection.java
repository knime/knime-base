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
 *   Jun 24, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections.knimerelativeto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.knime.core.node.util.FileSystemBrowser;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection.Type;

/**
 * {@link FSConnection} for the Relative-to workflow data area file system. It is possible to create a connected or
 * convenience file system, also the working directory is configurable.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class LocalRelativeToWorkflowDataFSConnection implements FSConnection {

    private final LocalRelativeToFileSystem m_fileSystem;

    private final RelativeToFileSystemBrowser m_browser;

    /**
     * Creates a new {@link FSConnection} with a {@link FSCategory#RELATIVE} (convenience) file system, where the
     * working directory is the root of the workflow data area.
     *
     * @throws UncheckedIOException If the folder for the workflow data area could not be created.
     */
    public LocalRelativeToWorkflowDataFSConnection() {
        this(false, BaseRelativeToFileSystem.PATH_SEPARATOR);
    }

    /**
     * Creates a new {@link FSConnection} with a {@link FSCategory#CONNECTED} file system, where the working directory
     * is as provided.
     *
     * @param workingDir The working directory of the file system to create.
     * @throws UncheckedIOException If the folder for the workflow data area could not be created.
     */
    public LocalRelativeToWorkflowDataFSConnection(final String workingDir) {
        this(true, workingDir);
    }

    /**
     * Internal constructor.
     *
     * @throws IOException
     */
    private LocalRelativeToWorkflowDataFSConnection(final boolean isConnected, final String workingDir) {

        final WorkflowContext workflowContext = RelativeToUtil.getWorkflowContext();
        final Path workflowLocation = workflowContext.getCurrentLocation().toPath().toAbsolutePath().normalize();

        m_fileSystem = createWorkflowDataRelativeFs(workflowLocation, isConnected, workingDir);

        final FSPath browsingHomeAndDefault = m_fileSystem.getWorkingDirectory();
        m_browser = new RelativeToFileSystemBrowser(m_fileSystem, browsingHomeAndDefault, browsingHomeAndDefault);
    }

    private static LocalRelativeToFileSystem createWorkflowDataRelativeFs(final Path workflowLocation,
        final boolean isConnected, final String workingDir) {

        final Path workflowDataDir = workflowLocation.resolve("data");

        try {
            Files.createDirectories(workflowDataDir);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        final FSLocationSpec fsLocationSpec;
        if (isConnected) {
            fsLocationSpec = BaseRelativeToFileSystem.CONNECTED_WORKFLOW_DATA_RELATIVE_FS_LOCATION_SPEC;
        } else {
            fsLocationSpec = BaseRelativeToFileSystem.CONVENIENCE_WORKFLOW_DATA_RELATIVE_FS_LOCATION_SPEC;
        }

        final URI uri = URI.create(Type.WORKFLOW_DATA_RELATIVE.getSchemeAndHost());
        return new LocalRelativeToFileSystem(uri, //
            workflowDataDir, //
            Type.WORKFLOW_DATA_RELATIVE, //
            workingDir, //
            fsLocationSpec);
    }

    @Override
    public LocalRelativeToFileSystem getFileSystem() {
        return m_fileSystem;
    }

    @Override
    public FileSystemBrowser getFileSystemBrowser() {
        return m_browser;
    }

    @Override
    public Map<URIExporterID, URIExporterFactory> getURIExporterFactories() {
        return RelativeToUtil.RELATIVE_TO_URI_EXPORTERS;
    }
}
