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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection.Type;

/**
 * Local KNIME relative to File System provider.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public class LocalRelativeToFileSystemProvider extends BaseFileSystemProvider<LocalRelativeToFileSystem> {

    private static final String SCHEME = "knime";

    @Override
    protected LocalRelativeToFileSystem createFileSystem(final URI uri, final Map<String, ?> env)
        throws IOException {

        final Type connectionType = KNIMEConnection.connectionTypeForHost(uri.getHost());
        switch (connectionType) {
            case WORKFLOW_RELATIVE:
            case MOUNTPOINT_RELATIVE:
                return new LocalRelativeToFileSystem(this, uri, connectionType);
            default:
                throw new IllegalArgumentException("Unsupported file system type: '" + uri.getHost() + "'.");
        }
    }

    /**
     * Gets or creates a new {@link FileSystem} based on the input URI.
     *
     * @param uri the URI that either retrieves or creates a new file system.
     * @return a file system for the URI
     * @throws IOException if I/O error occurs
     */
    public static LocalRelativeToFileSystem getOrCreateFileSystem(final URI uri) throws IOException {
        final LocalRelativeToFileSystemProvider provider = new LocalRelativeToFileSystemProvider();
        return (LocalRelativeToFileSystem)provider.getOrCreateFileSystem(uri, null);
    }

    private static LocalRelativeToPath toRelativePath(final Path path) {
        if (path instanceof LocalRelativeToPath) {
            return (LocalRelativeToPath)path;
        } else {
            throw new IllegalArgumentException("Unsupported path implementation: " + path.getClass());
        }
    }

    private Path toLocalPathWithAccessibilityCheck(final Path path) throws NoSuchFileException {
        final Path localPath = toRelativePath(path).toAbsoluteLocalPath();

        if (!getFileSystemInternal().isLocalPathAccessible(localPath)) {
            throw new NoSuchFileException(path.toString());
        }

        return localPath;
    }

    @Override
    protected InputStream newInputStreamInternal(final Path path, final OpenOption... options) throws IOException {
        final Path localPath = toLocalPathWithAccessibilityCheck(path);

        if (LocalRelativeToFileSystem.isLocalWorkflowFolder(localPath)) {
            throw new IOException("Workflows cannot be opened for reading");
        }

        return Files.newInputStream(localPath, options);
    }

    @Override
    protected OutputStream newOutputStreamInternal(final Path path, final OpenOption... options) throws IOException {
        final Path localPath = toLocalPathWithAccessibilityCheck(path);

        if (LocalRelativeToFileSystem.isLocalWorkflowFolder(localPath)) {
            throw new IOException("Workflows cannot be opened for writing");
        }

        return Files.newOutputStream(localPath, options);
    }

    @Override
    protected Iterator<Path> createPathIterator(final Path dir, final Filter<? super Path> filter) throws IOException {
        return new LocalRelativeToPathIterator(toRelativePath(dir), filter);
    }

    @Override
    public boolean exists(final Path path) throws IOException {
        final Path localPath = toRelativePath(path).toAbsoluteLocalPath();
        return getFileSystemInternal().isLocalPathAccessible(localPath);
    }

    @Override
    protected void deleteInternal(final Path path) throws IOException {
        Files.delete(toLocalPathWithAccessibilityCheck(path));
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public Path getPath(final URI uri) {
        return new LocalRelativeToPath(getFileSystemInternal(), uri.getPath());
    }

    @Override
    protected SeekableByteChannel newByteChannelInternal(final Path path, final Set<? extends OpenOption> options,
        final FileAttribute<?>... attrs) throws IOException {

        final Path localPath = toLocalPathWithAccessibilityCheck(path);

        if (LocalRelativeToFileSystem.isLocalWorkflowFolder(localPath)) {
            throw new IOException("Workflows cannot be opened for reading/writing");
        }

        return Files.newByteChannel(localPath, options);
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        Files.createDirectories(toLocalPathWithAccessibilityCheck(dir), attrs);
    }

    @Override
    protected void copyInternal(final Path source, final Path target, final CopyOption... options) throws IOException {
        Files.copy(toLocalPathWithAccessibilityCheck(source), toLocalPathWithAccessibilityCheck(target), options);
    }

    @Override
    protected void moveInternal(final Path source, final Path target, final CopyOption... options) throws IOException {
        Files.move(toLocalPathWithAccessibilityCheck(source), toLocalPathWithAccessibilityCheck(target), options);
    }

    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        return Files.isSameFile(toLocalPathWithAccessibilityCheck(path), toLocalPathWithAccessibilityCheck(path2));
    }

    @Override
    public boolean isHidden(final Path path) throws IOException {
        if (path.getFileName().toString().equals(WorkflowPersistor.METAINFO_FILE)) {
            return true;
        } else {
            return Files.isHidden(toLocalPathWithAccessibilityCheck(path));
        }
    }

    @Override
    public FileStore getFileStore(final Path path) throws IOException {
        return getFileSystemInternal().getDefaultFileStore();
    }

    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        final Path localPath = toLocalPathWithAccessibilityCheck(path);
        localPath.getFileSystem().provider().checkAccess(localPath);
    }

    @Override
    protected BaseFileAttributes fetchAttributesInternal(final Path path, final Class<?> type) throws IOException {
        if (type == BasicFileAttributes.class) {
            final Path localPath = toLocalPathWithAccessibilityCheck(path);
            final LocalRelativeToPath relativePath = toRelativePath(path);

            final boolean isRegularFile = getFileSystemInternal().isRegularFile(relativePath);

            final BasicFileAttributes localAttributes =
                Files.readAttributes(localPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

            return new BaseFileAttributes(
                isRegularFile, //
                path, //
                localAttributes.lastModifiedTime(), //
                localAttributes.lastAccessTime(), //
                localAttributes.creationTime(), //
                localAttributes.size(), //
                localAttributes.isSymbolicLink(), //
                localAttributes.isOther(), //
                null);
        }

        throw new UnsupportedOperationException(String.format("only %s supported", BasicFileAttributes.class));
    }

    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options)
        throws IOException {
        throw new UnsupportedOperationException();
    }
}
