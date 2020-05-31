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
import java.util.List;

import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.base.BaseFileStore;
import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection.Type;

/**
 * Local KNIME relative to File System implementation.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public class LocalRelativeToFileSystem extends BaseFileSystem<LocalRelativeToPath> {

    /**
     * Separator for name components of paths.
     */
    public static final String PATH_SEPARATOR = "/";

    private static final long CACHE_TTL = 0; // = disabled

    private static final String MOUNTPOINT_REL_FILE_STORE_TYPE = "knime-relative-mountpoint";

    private static final String WORKFLOW_REL_FILE_STORE_TYPE = "knime-relative-workflow";

    private final String m_scheme;

    private final String m_hostString;

    private final LocalRelativeToPathConfig m_pathConfig;

    private final List<FileStore> m_fileStores;

    /**
     * Default constructor.
     *
     * @param fileSystemProvider Creator of this FS, holding a reference.
     * @param uri URI without a path
     * @param pathConfig Provides underlying configuration, e.g. where this fs is rooted in the local file system.
     * @param isConnectedFs Whether this file system is a {@link Choice#CONNECTED_FS} or a convenience file system
     *            ({@link Choice#KNIME_FS})
     * @throws IOException
     */
    protected LocalRelativeToFileSystem(final LocalRelativeToFileSystemProvider fileSystemProvider, final URI uri,
        final LocalRelativeToPathConfig pathConfig, final boolean isConnectedFs) throws IOException {

        super(fileSystemProvider, //
            uri, //
            CACHE_TTL, //
            pathConfig.getWorkingDirectory(), //
            createFSLocationSpec(isConnectedFs, pathConfig.getType()));

        m_scheme = uri.getScheme();
        m_hostString = uri.getHost();
        m_pathConfig = pathConfig;

        final FileStore localFileStore = Files.getFileStore(pathConfig.getLocalMountpointFolder());
        final String fsType = pathConfig.getType() == Type.MOUNTPOINT_RELATIVE ? MOUNTPOINT_REL_FILE_STORE_TYPE
            : WORKFLOW_REL_FILE_STORE_TYPE;
        m_fileStores =
            Collections.unmodifiableList(Collections.singletonList(new BaseFileStore(fsType, "default_file_store",
                localFileStore.isReadOnly(), localFileStore.getTotalSpace(), localFileStore.getUsableSpace())));

    }

    /**
     *
     * @param isConnectedFs Whether this file system is a {@link Choice#CONNECTED_FS} or a convenience file system
     *            ({@link Choice#KNIME_FS})
     * @param type The type of the file system (mountpoint- or workflow relative).
     * @return the {@link FSLocationSpec}
     */
    public static FSLocationSpec createFSLocationSpec(final boolean isConnectedFs, final Type type) {
        final Choice choice = isConnectedFs ? Choice.CONNECTED_FS : Choice.KNIME_FS;
        final String specifier = type == Type.MOUNTPOINT_RELATIVE ? "knime.mountpoint" : "knime.workflow";
        return new DefaultFSLocationSpec(choice, specifier);
    }

    @Override
    public String getSeparator() {
        return PATH_SEPARATOR;
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
        return m_fileStores;
    }

    /**
     * @return file store of local file system holding the mount point
     * @throws IOException
     */
    protected FileStore getDefaultFileStore() throws IOException {
        return m_fileStores.get(0);
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
        } else if (m_pathConfig.isWorkflowRelativeFileSystem()
            && (m_pathConfig.isCurrentWorkflowFolder(localPath) || m_pathConfig.isInCurrentWorkflowFolder(localPath))) {
            return false; // workflow folder, or a folder within current workflow folder
        } else {
            return m_pathConfig.isWorkflow(path); // workflow or normal directory
        }
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

    /**
     * @return the underlying configuration, e.g. where this fs is rooted in the local file system.
     */
    public LocalRelativeToPathConfig getPathConfig() {
        return m_pathConfig;
    }

    /**
     * Convert a given local file system path into a absolute relative-to path.
     *
     * @param localPath path in local file system
     * @return absolute path in relative to file system
     */
    public LocalRelativeToPath toAbsoluteLocalRelativeToPath(final Path localPath) {
        return getPath(m_pathConfig.toAbsoluteLocalRelativeToPath(localPath));
    }
}
