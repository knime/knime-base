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
 *   Feb 11, 2020 (Sascha Wolke, KNIME GmbH): created
 */
package org.knime.filehandling.core.connections.knimerelativeto;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.commons.lang3.Validate;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.filehandling.core.connections.base.BaseFileStore;
import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection.Type;

/**
 * Local KNIME relative to File System implementation.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public class LocalRelativeToFileSystem extends BaseFileSystem {
    private static final long CACHE_TTL = 0; // = disabled

    private static final String FS_NAME = "KNIME relative to FS";

    private static final String FS_TYPE = FS_NAME;

    private final KNIMEConnection.Type m_type;

    private final String m_scheme;

    private final String m_hostString;

    /**
     * A local path (from the default FS provider) that points at the
     * folder of the current mountpoint.
     */
    private final Path m_localMountpointFolder;

    /**
     * A local path (from the default FS provider) that points at the folder
     * of the current workflow.
     */
    private final Path m_localWorkflowFolder;

    /**
     * A path (from the relative-to FS provider) that specifies the current working
     * directory to resolve relative paths against.
     */
    private final LocalRelativeToPath m_workingDirectory;


    /**
     * Default constructor.
     *
     * @param fileSystemProvider Creator of this FS, holding a reference.
     * @param uri URI without a path
     * @param connectionType {@link Type#MOUNTPOINT_RELATIVE} or {@link Type#WORKFLOW_RELATIVE} connection type
     */
    protected LocalRelativeToFileSystem(final LocalRelativeToFileSystemProvider fileSystemProvider,
        final URI uri, final KNIMEConnection.Type connectionType) {

        super(fileSystemProvider, uri, FS_NAME, FS_TYPE, CACHE_TTL);
        m_type = connectionType;
        m_scheme = uri.getScheme();
        m_hostString = uri.getHost();

        final NodeContext nodeContext = NodeContext.getContext();
        Validate.notNull(nodeContext, "Node context required.");

        final WorkflowContext workflowContext = nodeContext.getWorkflowManager().getContext();
        Validate.notNull(workflowContext, "Workflow context required.");

        if (isServerContext(workflowContext)) {
            throw new UnsupportedOperationException(
                "Unsupported temporary copy of workflow detected. Relative to does not support server execution.");
        }

        m_localMountpointFolder = workflowContext.getMountpointRoot().toPath().toAbsolutePath().normalize();
        m_localWorkflowFolder = workflowContext.getCurrentLocation().toPath().toAbsolutePath().normalize();

        if (isWorkflowRelativeFileSystem()) {
            m_workingDirectory = getPath(getSeparator(), m_localMountpointFolder.relativize(m_localWorkflowFolder).toString());
        } else {
            m_workingDirectory = getPath(getSeparator());
        }
    }




    /**
     * @return a local, absolute, normalized path (from the default FS provider) that points to the folder of the
     *         current mountpoint.
     */
    public Path getCurrentMountpointFolder() {
        return m_localMountpointFolder;
    }

    /**
     * @return a local, absolute, normalized path (from the default FS provider) that points to the folder of the
     *         current workflow.
     */
    public Path getCurrentWorkflowFolder() {
        return m_localWorkflowFolder;
    }

    /**
     * Provides the current working directory (from the relative-to FS). For a workflow-relative file system this points
     * to the workflow folder, whereas for the mountpoint-relative file system this is simply "/".
     *
     * @return the current working directory (from the relative-to FS).
     */
    protected LocalRelativeToPath getWorkingDirectory() {
        return m_workingDirectory;
    }

    @Override
    public String getSeparator() {
        return UnixStylePathUtil.SEPARATOR;
    }

    private static boolean isServerContext(final WorkflowContext context) {
        return context.getRemoteRepositoryAddress().isPresent() && context.getServerAuthToken().isPresent();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(getPath(getSeparator()));
    }


    @Override
    public LocalRelativeToPath getPath(final String first, final String... more) {
        return new LocalRelativeToPath(this, first, more);
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        try {
            return Collections.singletonList(getDefaultFileStore());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * @return file store of local file system holding the mount point
     * @throws IOException
     */
    protected FileStore getDefaultFileStore() throws IOException {
        final FileStore localFileStore = m_localMountpointFolder.getFileSystem().getFileStores().iterator().next();
        return new BaseFileStore(FS_TYPE, FS_NAME, localFileStore.isReadOnly(), localFileStore.getTotalSpace(),
            localFileStore.getUsableSpace());
    }

    /**
     * Utility method whether the given local path (from the default FS provider) can be accessed with the relative-to
     * file system, e.g. files outside the current mountpoint are not accessible.
     *
     * @param localPath A local path (from the default FS provider).
     * @return true when the given local path can be accessed with the relative-to file system, false otherwise.
     */
    public boolean isLocalPathAccessible(final Path localPath) {
        // we must not access files outside of the mountpoint
        if (!isInMountPoint(localPath)) {
            return false;
        }

        // we must be able to view workflows (whether we display them as files or folders)
        if (isLocalWorkflowFolder(localPath)) {
            return true;
        }

        // when not workflow-relative, we must not see files inside the current workflow
        if (!isWorkflowRelativeFileSystem() && !isCurrentWorkflowFolder(localPath) && isInCurrentWorkflowFolder(localPath)) {
            return false;
        }

        // we must never be able to see files inside workflows other than the current one
        if (!isInCurrentWorkflowFolder(localPath) && isPartOfWorkflow(localPath)) {
            return false;
        }

        return true;
    }

    /**
     * @param path path to check
     * @return {@code true} if the path represents a workflow directory.
     */
    public boolean isWorkflow(final LocalRelativeToPath path) {
        return isLocalWorkflowFolder(path.toAbsoluteLocalPath());
    }

    /**
     *
     * @param localPath
     * @return true when localPath is a workflow directory.
     */
    public static boolean isLocalWorkflowFolder(final Path localPath) {
        if (!Files.exists(localPath)) {
            return false;
        }

        if (Files.exists(localPath.resolve(WorkflowPersistor.TEMPLATE_FILE))) { // metanode
            return false;
        }

        return Files.exists(localPath.resolve(WorkflowPersistor.WORKFLOW_FILE));
    }

    /**
     * Validate recursive if given local path (from the default FS provider) or a parent is part of a workflow.
     *
     * @param localPath local file system path to check
     * @return {@code true} if given path or a parent path is part of a workflow
     */
    private boolean isPartOfWorkflow(final Path localPath) {
        Path current = localPath.toAbsolutePath().normalize();

        while (isInMountPoint(current)) {
            if (isLocalWorkflowFolder(current)) {
                return true;
            } else {
                current = current.getParent();
            }
        }

        return false;
    }

    /**
     * Test if given path is a child of or the current workflow directory.
     *
     * @param localPath local file system path to test
     * @return {@code true} if given path is child of or the current workflow directory
     */
    private boolean isInCurrentWorkflowFolder(final Path localPath) {
        final Path toCheck = localPath.toAbsolutePath().normalize();
        return toCheck.startsWith(m_localWorkflowFolder);
    }

    /**
     * Test if given path is the current workflow directory.
     *
     * @param localPath local file system path to test
     * @return {@code true} if given path equals the current workflow directory
     */
    private boolean isCurrentWorkflowFolder(final Path localPath) {
        final Path toCheck = localPath.toAbsolutePath().normalize();
        return toCheck.equals(m_localWorkflowFolder);
    }

    /**
     * Test if given path is a child of or the current mount point directory.
     *
     * @param localPath local file system path to test
     * @return {@code} if given path is a child of or the mount point directory
     */
    private boolean isInMountPoint(final Path localPath) {
        return localPath.toAbsolutePath().normalize().startsWith(m_localMountpointFolder);
    }

    /**
     * Check if given path represent a regular file. Workflow directories are files, with the exception of the current
     * workflow dir and a workflow relative path.
     *
     * @param path relative path to check
     * @return {@code true} if path is a normal file or a workflow directory
     * @throws IOException
     */
    protected boolean isRegularFile(final LocalRelativeToPath path) throws IOException {
        final Path localPath = path.toAbsoluteLocalPath();

        if (!Files.isDirectory(localPath)) {
            return true; // normal file
        } else if (isWorkflowRelativeFileSystem() && (isCurrentWorkflowFolder(localPath) || isInCurrentWorkflowFolder(localPath))) {
            return false; // workflow folder, or a folder within current workflow folder
        } else {
            return isWorkflow(path); // workflow or normal directory
        }
    }

    /**
     * @return {@code true} if this is a workflow relative and {@code false} if this is a mount point relative file
     *         system
     */
    public boolean isWorkflowRelativeFileSystem() {
        return m_type == Type.WORKFLOW_RELATIVE;
    }

    /**
     * @return {@code true} if this is a workflow relative and {@code false} if this is a mount point relative file
     *         system
     */
    public boolean isMountpointRelativeFileSystem() {
        return m_type == Type.MOUNTPOINT_RELATIVE;
    }

    @Override
    public String getSchemeString() {
        return m_scheme;
    }

    @Override
    public String getHostString() {
        return m_hostString;
    }

    @Override
    protected void prepareClose() {
        // Nothing to do
    }
}
