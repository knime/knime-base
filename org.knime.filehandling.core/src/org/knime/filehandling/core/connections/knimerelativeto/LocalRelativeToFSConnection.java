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
package org.knime.filehandling.core.connections.knimerelativeto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.EnumSet;

import org.knime.core.node.util.FileSystemBrowser;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection.Type;

/**
 * {@link FSConnection} implementation for the local relative-to file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class LocalRelativeToFSConnection implements FSConnection {

    private final LocalRelativeToFileSystem m_fileSystem;

    private final RelativeToFileSystemBrowser m_browser;

    private static final EnumSet<Type> SUPPORTED_TYPES = EnumSet.of(Type.MOUNTPOINT_RELATIVE, Type.WORKFLOW_RELATIVE);

    /**
     * Constructor.
     *
     * @param type The type of the file system (mountpoint- or workflow relative).
     * @param isConnected {@code true} if it is a connected file system, {@code false} otherwise
     */
    public LocalRelativeToFSConnection(final Type type, final boolean isConnected) {
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported file system type: '" + type + "'.");
        }

        final WorkflowContext workflowContext = RelativeToUtil.getWorkflowContext();
        if (RelativeToUtil.isServerContext(workflowContext)) {
            throw new UnsupportedOperationException(
                "Unsupported temporary copy of workflow detected. Relative to does not support server execution.");
        }

        final Path mountpointRoot = workflowContext.getMountpointRoot().toPath().toAbsolutePath().normalize();
        final Path workflowLocation = workflowContext.getCurrentLocation().toPath().toAbsolutePath().normalize();

        try {
            if (type == Type.MOUNTPOINT_RELATIVE) {
                m_fileSystem = createMountpointRelativeFs(mountpointRoot, isConnected);
            } else {
                m_fileSystem = createWorkflowRelativeFs(mountpointRoot, workflowLocation, isConnected);
            }
        } catch (IOException ex) {
            // should never happen
            throw new UncheckedIOException(ex);
        }

        final FSPath browsingHomeAndDefault;
        if (type == Type.WORKFLOW_RELATIVE) {
            // in the workflow-relative file system the working "dir" is the workflow, but it is not a directory,
            // so we need to take the parent
            browsingHomeAndDefault = (FSPath)m_fileSystem.getWorkingDirectory().getParent();
        } else {
            browsingHomeAndDefault = m_fileSystem.getWorkingDirectory();
        }
        m_browser = new RelativeToFileSystemBrowser(m_fileSystem, browsingHomeAndDefault, browsingHomeAndDefault);
    }

    private static LocalRelativeToFileSystem createWorkflowRelativeFs(final Path mountpointRoot,
        final Path workflowLocation, final boolean isConnected) throws IOException {

        final String workingDir = LocalRelativeToFileSystemProvider
            .localToRelativeToPathSeperator(mountpointRoot.relativize(workflowLocation));

        final FSLocationSpec fsLocationSpec;
        if (isConnected) {
            fsLocationSpec = BaseRelativeToFileSystem.CONNECTED_WORKFLOW_RELATIVE_FS_LOCATION_SPEC;
        } else {
            fsLocationSpec = BaseRelativeToFileSystem.CONVENIENCE_WORKFLOW_RELATIVE_FS_LOCATION_SPEC;
        }

        final URI uri = URI.create(Type.WORKFLOW_RELATIVE.getSchemeAndHost());
        return new LocalRelativeToFileSystem(uri, //
            mountpointRoot, //
            Type.WORKFLOW_RELATIVE, //
            workingDir, //
            fsLocationSpec);
    }

    private static LocalRelativeToFileSystem createMountpointRelativeFs(final Path mountpointRoot,
        final boolean isConnected) throws IOException {

        final FSLocationSpec fsLocationSpec;
        if (isConnected) {
            fsLocationSpec = BaseRelativeToFileSystem.CONNECTED_MOUNTPOINT_RELATIVE_FS_LOCATION_SPEC;
        } else {
            fsLocationSpec = BaseRelativeToFileSystem.CONVENIENCE_MOUNTPOINT_RELATIVE_FS_LOCATION_SPEC;
        }

        final URI uri = URI.create(Type.MOUNTPOINT_RELATIVE.getSchemeAndHost());
        return new LocalRelativeToFileSystem(uri, //
            mountpointRoot, //
            Type.MOUNTPOINT_RELATIVE, //
            BaseRelativeToFileSystem.PATH_SEPARATOR, //
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
