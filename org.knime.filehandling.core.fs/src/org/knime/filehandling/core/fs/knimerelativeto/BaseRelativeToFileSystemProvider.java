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
package org.knime.filehandling.core.fs.knimerelativeto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.filehandling.core.connections.WorkflowAwareErrorHandling;
import org.knime.filehandling.core.connections.WorkflowAwareErrorHandling.Entity;
import org.knime.filehandling.core.connections.WorkflowAwareErrorHandling.Operation;
import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

/**
 * Abstract relative-to file system provider.
 *
 * @author Sascha Wolke, KNIME GmbH
 * @param <F> provided file system
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class BaseRelativeToFileSystemProvider<F extends BaseRelativeToFileSystem>
    extends BaseFileSystemProvider<RelativeToPath, F> {

    @SuppressWarnings("resource") // the file system has to stay open for further use
    private Path toRealPathWithAccessibilityCheck(final RelativeToPath path) throws IOException {
        return getFileSystemInternal().toRealPathWithAccessibilityCheck(path);
    }

    @Override
    protected InputStream newInputStreamInternal(final RelativeToPath path, final OpenOption... options) throws IOException {
        checkSupport(path, Operation.NEW_INPUT_STREAM);
        return Files.newInputStream(toRealPathWithAccessibilityCheck(path), options);
    }

    @SuppressWarnings("resource")// the file system has to stay open for further use
    private boolean isPartOfWorkflow(final RelativeToPath path) throws IOException {
        return getFileSystemInternal().isPartOfWorkflow(path);
    }

    private void checkSupport(final RelativeToPath path, final Operation operation) throws IOException {
        final Optional<Entity> entity = getEntity(path);
        // an empty entity means there is nothing at the provided path and we let the caller handle this case
        if (entity.isPresent()) {
            entity.get().checkSupport(path.toString(), operation);
        }
    }

    @SuppressWarnings("resource")// the file system has to stay open for further use
    private Optional<Entity> getEntity(final RelativeToPath path) throws IOException {
        return getFileSystemInternal().getEntity(path);
    }

    @Override
    protected OutputStream newOutputStreamInternal(final RelativeToPath path, final OpenOption... options) throws IOException {
        checkSupport(path, Operation.NEW_OUTPUT_STREAM);
        return Files.newOutputStream(toRealPathWithAccessibilityCheck(path), options);
    }

    @Override
    protected Iterator<RelativeToPath> createPathIterator(final RelativeToPath path, final Filter<? super Path> filter) throws IOException {
        checkSupport(path, Operation.LIST_FOLDER_CONTENT);
        return new RelativeToPathIterator(path, toRealPathWithAccessibilityCheck(path), filter);
    }

    @SuppressWarnings("resource") // the file system has to stay open for further use
    @Override
    protected boolean exists(final RelativeToPath path) throws IOException {
        return getFileSystemInternal().existsWithAccessibilityCheck(path);
    }

    @Override
    protected void deleteInternal(final RelativeToPath path) throws IOException {
        if (isPartOfWorkflow(path)) {
            throw new IOException(path.toString()  + " points to/into a workflow. Cannot delete data from a workflow");
        }

        Files.delete(toRealPathWithAccessibilityCheck(path));
    }

    @Override
    protected SeekableByteChannel newByteChannelInternal(final RelativeToPath path,
        final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
        if (isPartOfWorkflow(path)) {
            throw new IOException(path.toString()  + " points to/into a workflow. Workflows cannot be opened for reading/writing");
        }
        checkSupport(path, Operation.NEW_INPUT_STREAM);
        return Files.newByteChannel(toRealPathWithAccessibilityCheck(path), options);
    }

    @Override
    protected void createDirectoryInternal(final RelativeToPath dir, final FileAttribute<?>... attrs)
        throws IOException {
        checkSupport(dir, Operation.CREATE_FOLDER);
        Files.createDirectory(toRealPathWithAccessibilityCheck(checkCastAndAbsolutizePath(dir)), attrs);
    }

    @Override
    protected void copyInternal(final RelativeToPath source, final RelativeToPath target,
        final CopyOption... options) throws IOException {

        if (isPartOfWorkflow(source)) {
            throw new IOException(source.toString()  + " points to/into a workflow. Cannot copy files from workflows.");
        }

        if (isPartOfWorkflow(target)) {
            throw new IOException(source.toString()  + " points to/into a workflow. Cannot copy files to workflows.");
        }

        Files.copy(toRealPathWithAccessibilityCheck(source), toRealPathWithAccessibilityCheck(target), options);
    }

    @Override
    protected void moveInternal(final RelativeToPath source, final RelativeToPath target,
        final CopyOption... options) throws IOException {

        if (isPartOfWorkflow(source)) {
            throw new IOException(source.toString()  + " points to/into a workflow. Cannot move files from workflows.");
        }

        if (isPartOfWorkflow(target)) {
            throw new IOException(source.toString()  + " points to/into a workflow. Cannot move files to workflows.");
        }

        Files.move(toRealPathWithAccessibilityCheck(source), toRealPathWithAccessibilityCheck(target), options);
    }

    @Override
    public boolean isSameFileInternal(final RelativeToPath path, final RelativeToPath path2) throws IOException {
        return Files.isSameFile(toRealPathWithAccessibilityCheck(path), toRealPathWithAccessibilityCheck(path2));
    }

    @Override
    public boolean isHiddenInternal(final RelativeToPath path) throws IOException {
        return !path.isRoot() && path.getFileName().toString().equals(WorkflowPersistor.METAINFO_FILE);
    }

    @SuppressWarnings("resource")// the file system has to stay open for further use
    @Override
    protected void checkAccessInternal(final RelativeToPath path, final AccessMode... modes) throws IOException {
        // do nothing here
    }

    @Override
    protected BaseFileAttributes fetchAttributesInternal(final RelativeToPath path, final Class<?> type) throws IOException {
        final boolean isWorkflow = isWorkflow(path);
        if (isPartOfWorkflow(path) && !isWorkflow) {
            throw WorkflowAwareErrorHandling.createAccessInsideWorkflowException(path.toString());
        }

        if (type == BasicFileAttributes.class) {
            final Path realPath = toRealPathWithAccessibilityCheck(path);

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
    private boolean isWorkflow(final RelativeToPath path) throws IOException {
        return getFileSystemInternal().isWorkflowDirectory(path);
    }

    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options)
        throws IOException {
        final RelativeToPath checkedPath = checkCastAndAbsolutizePath(path);
        final Path realPath = toRealPathWithAccessibilityCheck(checkedPath);
        Files.setAttribute(realPath, attribute, value, options);
    }
}
