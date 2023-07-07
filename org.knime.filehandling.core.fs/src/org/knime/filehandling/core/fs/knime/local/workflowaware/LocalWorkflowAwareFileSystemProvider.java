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
package org.knime.filehandling.core.fs.knime.local.workflowaware;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.output.NullOutputStream;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.connections.workflowaware.Entity;
import org.knime.filehandling.core.connections.workflowaware.WorkflowAware;
import org.knime.filehandling.core.connections.workflowaware.WorkflowAwareErrorHandling;
import org.knime.filehandling.core.connections.workflowaware.WorkflowAwareErrorHandling.Operation;
import org.knime.filehandling.core.util.MountPointFileSystemAccessService;
import org.knime.filehandling.core.util.TempPathCloseable;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * File system provider implementation for a workflow-aware file system that is backed by a folder in the local file
 * system.
 *
 * @author Sascha Wolke, KNIME GmbH
 * @param <F> provided file system
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class LocalWorkflowAwareFileSystemProvider<F extends LocalWorkflowAwareFileSystem>
    extends BaseFileSystemProvider<LocalWorkflowAwarePath, F> implements WorkflowAware {

    @SuppressWarnings("resource") // the file system has to stay open for further use
    private Path toLocalPathWithAccessibilityCheck(final LocalWorkflowAwarePath path) throws IOException {
        return getFileSystemInternal().toLocalPathWithAccessibilityCheck(path);
    }

    @Override
    protected InputStream newInputStreamInternal(final LocalWorkflowAwarePath path, final OpenOption... options)
        throws IOException {

        checkSupport(path, Operation.NEW_INPUT_STREAM);
        return Files.newInputStream(toLocalPathWithAccessibilityCheck(path), options);
    }

    @SuppressWarnings("resource")// the file system has to stay open for further use
    private boolean isPartOfWorkflow(final LocalWorkflowAwarePath path) {
        return getFileSystemInternal().isPartOfWorkflow(path);
    }

    private boolean isReservedForMetadataFolder(final LocalWorkflowAwarePath path) {
        return getFileSystemInternal().isReservedForMetadataFolder(path);
    }

    private boolean isReservedForMetainfoFile(final LocalWorkflowAwarePath path) {
        return getFileSystemInternal().isReservedForMetainfoFile(path);
    }

    private void checkSupport(final LocalWorkflowAwarePath path, final Operation operation) throws IOException {
        final Optional<Entity> entity = getEntity(path);
        // an empty entity means there is nothing at the provided path and we let the caller handle this case
        if (entity.isPresent()) {
            entity.get().checkSupport(path.toString(), operation);
        }
    }

    @SuppressWarnings("resource")// the file system has to stay open for further use
    private Optional<Entity> getEntity(final LocalWorkflowAwarePath path) throws IOException {
        return getFileSystemInternal().getEntity(path);
    }

    @Override
    protected OutputStream newOutputStreamInternal(final LocalWorkflowAwarePath path, final OpenOption... options)
        throws IOException {

        if (isReservedForMetainfoFile(path)) {
            return NullOutputStream.NULL_OUTPUT_STREAM;
        }
        checkSupport(path, Operation.NEW_OUTPUT_STREAM);
        return Files.newOutputStream(toLocalPathWithAccessibilityCheck(path), options);
    }

    @Override
    protected Iterator<LocalWorkflowAwarePath> createPathIterator(final LocalWorkflowAwarePath path, final Filter<? super Path> filter) throws IOException {
        checkSupport(path, Operation.LIST_FOLDER_CONTENT);
        return new LocalWorkflowAwarePathIterator(path, filter);
    }

    @SuppressWarnings("resource") // the file system has to stay open for further use
    @Override
    protected boolean exists(final LocalWorkflowAwarePath path) throws IOException {
        return getFileSystemInternal().existsWithAccessibilityCheck(path);
    }

    @Override
    protected void deleteInternal(final LocalWorkflowAwarePath path) throws IOException {
        if (isWorkflow(path)) {
            FSFiles.deleteRecursively(toLocalPathWithAccessibilityCheck(path));
        } else if (isPartOfWorkflow(path)) {
            throw new IOException(path.toString()  + " points to/into a workflow. Cannot delete data from a workflow");
        } else {
            Files.delete(toLocalPathWithAccessibilityCheck(path));
        }
    }

    @Override
    protected SeekableByteChannel newByteChannelInternal(final LocalWorkflowAwarePath path,
        final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {

        if (isPartOfWorkflow(path)) {
            throw new IOException(
                path.toString() + " points to/into a workflow. Workflows cannot be opened for reading/writing");
        }
        if (isReservedForMetadataFolder(path)) {
            throw new FileSystemException(path.toString(), null, "Path is reserved for internal use");
        }

        // AP-20346 we silently ignore writes to workflowset.meta
        if (isReservedForMetainfoFile(path) && options.contains(StandardOpenOption.WRITE)) {
            return new NullByteChannel();
        }

        checkSupport(path, options.contains(StandardOpenOption.READ)//
            ? Operation.NEW_INPUT_STREAM//
            : Operation.NEW_OUTPUT_STREAM);

        return Files.newByteChannel(toLocalPathWithAccessibilityCheck(path), options);
    }

    @Override
    protected void createDirectoryInternal(final LocalWorkflowAwarePath dir, final FileAttribute<?>... attrs)
        throws IOException {

        if (isReservedForMetadataFolder(dir)) {
            throw new FileSystemException(dir.toString(), null, "Path is reserved for internal use");
        }

        // AP-20346 we silently ignore writes to workflowset.meta
        if (isReservedForMetainfoFile(dir)) {
            return;
        }

        checkSupport(dir, Operation.CREATE_FOLDER);
        Files.createDirectory(toLocalPathWithAccessibilityCheck(checkCastAndAbsolutizePath(dir)), attrs);
    }

    @Override
    protected void copyInternal(final LocalWorkflowAwarePath source, final LocalWorkflowAwarePath target,
        final CopyOption... options) throws IOException {

        final var sourceIsWorkflow = isWorkflow(source);
        if (!sourceIsWorkflow && isPartOfWorkflow(source)) {
            throw new IOException(
                source.toString() + " points into a workflow. Cannot copy files from inside workflows.");
        }

        if (!isWorkflow(target) && isPartOfWorkflow(target)) {
            throw new IOException(source.toString() + " points into a workflow. Cannot copy files into workflows.");
        }

        Files.deleteIfExists(target); // this also deletes a workflow/metanode/component

        if (sourceIsWorkflow) {
            FSFiles.copyRecursively(toLocalPathWithAccessibilityCheck(source),
                toLocalPathWithAccessibilityCheck(target));
        } else {
            Files.copy(toLocalPathWithAccessibilityCheck(source), toLocalPathWithAccessibilityCheck(target), options);
        }
    }

    @Override
    protected void moveInternal(final LocalWorkflowAwarePath source, final LocalWorkflowAwarePath target,
        final CopyOption... options) throws IOException {

        final var sourceIsWorkflow = isWorkflow(source);
        if (!sourceIsWorkflow && isPartOfWorkflow(source)) {
            throw new IOException(
                source.toString() + " points into a workflow. Cannot move files from inside workflows.");
        }

        if (!isWorkflow(target) && isPartOfWorkflow(target)) {
            throw new IOException(source.toString() + " points into a workflow. Cannot move files into workflows.");
        }

        Files.deleteIfExists(target); // this also deletes a workflow/metanode/component
        Files.move(toLocalPathWithAccessibilityCheck(source), toLocalPathWithAccessibilityCheck(target), options);
    }

    @Override
    public boolean isSameFileInternal(final LocalWorkflowAwarePath path, final LocalWorkflowAwarePath path2) throws IOException {
        return Files.isSameFile(toLocalPathWithAccessibilityCheck(path), toLocalPathWithAccessibilityCheck(path2));
    }

    @Override
    protected void checkAccessInternal(final LocalWorkflowAwarePath path, final AccessMode... modes) throws IOException {
        // do nothing here
    }

    @Override
    protected BaseFileAttributes fetchAttributesInternal(final LocalWorkflowAwarePath path, final Class<?> type) throws IOException {
        final boolean isWorkflow = isWorkflow(path);
        if (isPartOfWorkflow(path) && !isWorkflow) {
            throw WorkflowAwareErrorHandling.createAccessInsideWorkflowException(path.toString());
        }

        if (type == BasicFileAttributes.class) {
            final Path realPath = toLocalPathWithAccessibilityCheck(path);

            final BasicFileAttributes localAttributes =
                Files.readAttributes(realPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

            // AP-15972: Workflows are non-regular files
            final boolean isOther = isWorkflow || localAttributes.isOther();

            return new BaseFileAttributes(localAttributes.isRegularFile(), //
                path, //
                localAttributes.lastModifiedTime(), //
                localAttributes.lastAccessTime(), //
                localAttributes.creationTime(), //
                localAttributes.size(), //
                localAttributes.isSymbolicLink(), //
                isOther,
                null);
        }

        throw new UnsupportedOperationException(String.format("only %s supported", BasicFileAttributes.class));
    }

    @SuppressWarnings("resource")// the file system has to stay open for further use
    private boolean isWorkflow(final LocalWorkflowAwarePath path) {
        return getFileSystemInternal().isWorkflow(path);
    }

    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options)
        throws IOException {
        final LocalWorkflowAwarePath checkedPath = checkCastAndAbsolutizePath(path);
        final Path realPath = toLocalPathWithAccessibilityCheck(checkedPath);
        Files.setAttribute(realPath, attribute, value, options);
    }

    @Override
    public void deployWorkflow(final Path source, final FSPath dest, final boolean overwrite, final boolean attemptOpen)
        throws IOException {

        checkFileSystemOpenAndNotClosing();
        final var absoluteDest = checkCastAndAbsolutizePath(dest);

        if (isReservedForMetadataFolder(absoluteDest) || isReservedForMetainfoFile(absoluteDest)) {
            throw new FileSystemException(dest.toString(), null, "Path is reserved for internal use");
        }

        final String localMountId = getMountID().orElseThrow(() -> new IOException("Cannot deploy workflow because target file system does not have a mount ID"));
        try {
            MountPointFileSystemAccessService.instance().deployWorkflow( //
                source.toFile(), //
                new URI("knime", localMountId, absoluteDest.toString(), null), //
               overwrite, //
               attemptOpen);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private static String getCurrentMountpoint() {
        final WorkflowContext context = WorkflowContextUtil.getWorkflowContext();
        return context.getMountpointURI() //
                .orElseThrow(() -> new IllegalStateException("Cannot determine name of mountpoint to deploy workflow.")) //
                .getAuthority();
    }

    @Override
    public TempPathCloseable toLocalWorkflowDir(final FSPath src) throws IOException {
        checkFileSystemOpenAndNotClosing();

        var checkedPath = checkCastAndAbsolutizePath(src);
        checkSupport(checkedPath, Operation.GET_WORKFLOW);

        final var realPath = toLocalPathWithAccessibilityCheck(checkedPath);
        return new TempPathCloseable(realPath) {
            @Override
            public void close() {
                // do nothing
            }
        };
    }

    @Override
    public Entity getEntityOf(final FSPath path) throws IOException {
        checkFileSystemOpenAndNotClosing();
        var checkedPath = checkCastAndAbsolutizePath(path);
        return getEntity(checkedPath).orElseThrow(() -> new NoSuchFileException(path.toString()));
    }

    private static class NullByteChannel implements SeekableByteChannel {

        private boolean open;

        private NullByteChannel() {
            open = true;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() throws IOException {
            open = false;
        }

        @Override
        public int read(final ByteBuffer dst) throws IOException {
            return 0; // No data to read
        }

        @Override
        public int write(final ByteBuffer src) throws IOException {
            // Discard the written bytes
            int remaining = src.remaining();
            src.position(src.position() + remaining);
            return remaining;
        }

        @Override
        public long position() throws IOException {
            return 0; // Always at position 0
        }

        @Override
        public SeekableByteChannel position(final long newPosition) throws IOException {
            return this; // Ignore the new position
        }

        @Override
        public long size() throws IOException {
            return 0; // Always size 0
        }

        @Override
        public SeekableByteChannel truncate(final long size) throws IOException {
            return this; // Ignore truncation
        }
    }
}
