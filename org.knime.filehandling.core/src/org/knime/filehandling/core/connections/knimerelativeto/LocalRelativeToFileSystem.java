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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.base.BaseFileStore;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection.Type;

/**
 * Local KNIME relative to File System implementation.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public class LocalRelativeToFileSystem extends BaseRelativeToFileSystem {

    private final FileStore m_localFileStore;

    private final List<FileStore> m_fileStores;

    /**
     * A local path (from the default FS provider) that points to the
     * folder of the current mountpoint.
     */
    private final Path m_localMountpointDirectory;

    /**
     * Default constructor.
     *
     * @param uri URI without a path
     * @param localMountpointRoot mountpoint root in the local file system
     * @param virtualWorkflowDirectory workflow directory in the relative-to file system
     * @param type {@link Type#MOUNTPOINT_RELATIVE} or {@link Type#WORKFLOW_RELATIVE} connection type
     * @param isConnectedFs Whether this file system is a {@link FSCategory#CONNECTED} or a convenience file system
     *            ({@link FSCategory#RELATIVE})
     * @throws IOException
     */
    protected LocalRelativeToFileSystem(final URI uri, //
        final Path localMountpointRoot, //
        final Type type, //
        final String workingDir, //
        final FSLocationSpec fsLocationSpec) throws IOException {

        super(new LocalRelativeToFileSystemProvider(), //
            uri, //
            type, //
            workingDir, //
            fsLocationSpec);

        m_localMountpointDirectory = localMountpointRoot;
        m_localFileStore = getFileStore(localMountpointRoot, getFileStoreType(), "default_file_store");

        final String fsType = type == Type.MOUNTPOINT_RELATIVE ? MOUNTPOINT_REL_SCHEME
            : WORKFLOW_REL_SCHEME;
        m_fileStores =
                Collections.unmodifiableList(Collections.singletonList(new BaseFileStore(fsType, "default_file_store",
                    m_localFileStore.isReadOnly(), m_localFileStore.getTotalSpace(), m_localFileStore.getUsableSpace())));
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return m_fileStores;
    }

    @Override
    protected FileStore getFileStore(final RelativeToPath path) throws IOException {
        return m_localFileStore;
    }

    @Override
    public boolean isWorkflowDirectory(final RelativeToPath path) throws IOException {
        return isLocalWorkflowDirectory(toAbsoluteLocalPath(path));
    }

    @Override
    public Path toRealPathWithAccessibilityCheck(final RelativeToPath path) throws IOException {
        if (!isPathAccessible(path)) {
            throw new NoSuchFileException(path.toString());
        } else {
            return toAbsoluteLocalPath(path);
        }
    }

    /**
     * Maps a path from relative-to file system to a path in the local file system.
     *
     * @param path a relative-to path inside relative-to file system
     * @return an absolute path in the local file system (default FS provider) that corresponds to this path.
     */
    private Path toAbsoluteLocalPath(final RelativeToPath path) {
        final RelativeToPath absolutePath = (RelativeToPath) path.toAbsolutePath().normalize();
        final Path realPath =  absolutePath.appendToBaseDir(m_localMountpointDirectory);
        return realPath;
    }

    /**
     * Check if given path represent a regular file. Workflow directories are files, with the exception of the current
     * workflow dir and a workflow relative path.
     *
     * @param path relative-to path to check
     * @return {@code true} if path is a normal file or a workflow directory
     * @throws IOException
     */
    @Override
    protected boolean isRegularFile(final RelativeToPath path) throws IOException {
        if (!isPathAccessible(path)) {
            throw new NoSuchFileException(path.toString()); // not allowed
        } else if (isWorkflowDirectory(path)) {
            return true;
        } else {
            return !Files.isDirectory(toAbsoluteLocalPath(path));
        }
    }

    @Override
    protected boolean existsWithAccessibilityCheck(final RelativeToPath path) throws IOException {
        final Path localPath = toAbsoluteLocalPath(path);
        return isPathAccessible(path) && Files.exists(localPath);
    }

    @Override
    protected void prepareClose() {
        // Nothing to do
    }
}
