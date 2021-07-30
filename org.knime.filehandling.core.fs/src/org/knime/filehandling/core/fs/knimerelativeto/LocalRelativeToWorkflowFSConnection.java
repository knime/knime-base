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
 *   Mar 12, 2021 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.core.fs.knimerelativeto;

import java.io.IOException;
import java.nio.file.Path;

import org.knime.core.node.util.FileSystemBrowser;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.config.LocalRelativeToFSConnectionConfig;
import org.knime.filehandling.core.fs.knimerelativeto.export.RelativeToFileSystemConstants;
import org.knime.filehandling.core.fs.knimerelativeto.export.RelativeToFileSystemBrowser;
import org.knime.filehandling.core.util.CheckNodeContextUtil;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * {@link FSConnection} for the Relative-to workflow file system. It is possible to create a connected or convenience
 * file system, but the working directory is fixed. The location of the current workflow in the underyling local file
 * system is determined using the KNIME {@link WorkflowContext}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public class LocalRelativeToWorkflowFSConnection implements FSConnection {

    private final LocalRelativeToFileSystem m_fileSystem;

    private final RelativeToFileSystemBrowser m_browser;

    /**
     * Creates a new connection using the given config. Note that the working directory of the given config is ignored,
     * as it is always the location of the current workflow within the mountpoint.
     *
     * @param config The config to use.
     * @throws IOException If the folder for the workflow data area could not be created.
     */
    public LocalRelativeToWorkflowFSConnection(final LocalRelativeToFSConnectionConfig config) throws IOException {
        if (CheckNodeContextUtil.isInComponentProject()) {
            throw new IllegalStateException(
                "Nodes in a shared component don't have access to workflow-relative locations.");
        }

        final WorkflowContext workflowContext = WorkflowContextUtil.getWorkflowContext();

        if (WorkflowContextUtil.isServerContext(workflowContext)) {
            throw new UnsupportedOperationException(
                "Unsupported temporary copy of workflow detected. LocalRelativeTo does not support server execution.");
        }

        final Path localMountpointRoot = workflowContext.getMountpointRoot().toPath().toAbsolutePath().normalize();
        final Path localWorkflowLocation = workflowContext.getCurrentLocation().toPath().toAbsolutePath().normalize();
        m_fileSystem = createWorkflowRelativeFs(localMountpointRoot, localWorkflowLocation, config.isConnectedFileSystem());

        // in the workflow-relative file system the working "dir" is the workflow, but it is not a directory,
        // so we need to take the parent
        final FSPath browsingHomeAndDefault = (FSPath)m_fileSystem.getWorkingDirectory().getParent();
        m_browser = new RelativeToFileSystemBrowser(m_fileSystem, browsingHomeAndDefault, browsingHomeAndDefault);
    }

    private static LocalRelativeToFileSystem createWorkflowRelativeFs(final Path localMountpointRoot,
        final Path workflowLocation, final boolean isConnected) {

        final String workingDir = LocalRelativeToFileSystemProvider
            .localToRelativeToPathSeperator(localMountpointRoot.relativize(workflowLocation));

        final FSLocationSpec fsLocationSpec;
        if (isConnected) {
            fsLocationSpec = RelativeToFileSystemConstants.CONNECTED_WORKFLOW_RELATIVE_FS_LOCATION_SPEC;
        } else {
            fsLocationSpec = RelativeToFileSystemConstants.CONVENIENCE_WORKFLOW_RELATIVE_FS_LOCATION_SPEC;
        }

        return new LocalRelativeToFileSystem(localMountpointRoot, //
            RelativeTo.WORKFLOW, //
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
}
