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
 *   2022-04-05 (Bjoern Lohrmann): created
 */
package org.knime.ext.example.filehandling.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.Set;

import org.knime.filehandling.core.connections.base.BaseFileSystemProvider;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;

/**
 * File system provider for the {@link ExampleFileSystem}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
class ExampleFileSystemProvider extends BaseFileSystemProvider<ExamplePath, ExampleFileSystem> {

    @Override
    protected SeekableByteChannel newByteChannelInternal(final ExamplePath path, final Set<? extends OpenOption> options,
            final FileAttribute<?>... attrs) throws IOException {

        // FIXME: create byte channel here. In many cases you can subclass
        // TempFileSeekableByteChannel.
        // See ExampleSeekableFileChannel
        return null;
    }

    @Override
    protected void copyInternal(final ExamplePath source, final ExamplePath target, final CopyOption... options) throws IOException {
        try {
            // FIXME: copy file or create empty target directory here.
            // FIXME: Don't implement recursive copying of a directory here, this is done by
            // calling code.
            // FIXME: Copy options: Only handle StandardCopyOption.REPLACE_EXISTING here
            // (ATOMIC and COPY_ATTRIBUTES are usually not applicable)

        } catch (Exception ex) { // FIXME handle custom exceptions
            // FIXME: throw appropriate IOE subtype here:
            // source does not exist => NoSuchFileException
            // source cannot be copied or target cannot be created due to permissions =>
            // AccessDeniedException (attach original exception as cause)
            // otherwise => create generic IOE and attach original exception as cause
            throw new IOException();
        }
    }

    @Override
    protected void moveInternal(final ExamplePath source, final ExamplePath target, final CopyOption... options)
            throws IOException {
        try {
            // FIXME: move file or create empty target directory here.
            // FIXME: Don't implement recursive moving of a directory here, this is done by
            // calling code.
            // FIXME: Copy options: Only handle StandardCopyOption.REPLACE_EXISTING here
            // (ATOMIC and COPY_ATTRIBUTES are usually not applicable)

        } catch (Exception ex) { // FIXME handle custom exceptions
            // FIXME: throw appropriate IOE subtype here:
            // source does not exist => NoSuchFileException
            // source cannot be copied or deleted, or target cannot be created due to
            // permissions =>
            // AccessDeniedException (attach original exception as cause)
            // otherwise => create generic IOE and attach original exception as cause
            throw new IOException();
        }
    }

    @Override
    protected InputStream newInputStreamInternal(final ExamplePath path, final OpenOption... options) throws IOException {
        try {
            // FIXME: open new input stream here.
            return null;
        } catch (Exception ex) { // FIXME handle custom exceptions
            // FIXME: throw appropriate IOE subtype here:
            // file does not exist => NoSuchFileException
            // file cannot be read due to permissions => AccessDeniedException (attach
            // original exception as cause)
            // otherwise => create generic IOE and attach original exception as cause
            throw new IOException();
        }
    }

    @Override
    protected OutputStream newOutputStreamInternal(final ExamplePath path, final OpenOption... options) throws IOException {
        try {
            // FIXME: open new output stream here.
            // FIXME: Handle OpenOption properly (see StandardOpenOption: WRITE, APPEND,
            // CREATE, TRUNCATE_EXISTING)
            return null;
        } catch (Exception ex) { // FIXME handle custom exceptions
            // FIXME: throw appropriate IOE subtype here:
            // parent does not exist => NoSuchFileException
            // file cannot be written due to permissions => AccessDeniedException (attach
            // original exception as cause)
            // otherwise => create generic IOE and attach original exception as cause
            throw new IOException();
        }
    }

    @Override
    protected Iterator<ExamplePath> createPathIterator(final ExamplePath dir, final Filter<? super Path> filter) throws IOException {
        return new ExamplePathIterator(dir, filter);
    }

    @Override
    protected void createDirectoryInternal(final ExamplePath dir, final FileAttribute<?>... attrs) throws IOException {
        try {
            // FIXME: create directory
        } catch (Exception ex) { // FIXME handle custom exceptions
            // FIXME: throw appropriate IOE subtype here:
            // parent does not exist => NoSuchFileException
            // dir cannot be created due to permissions => AccessDeniedException (attach
            // original exception as cause)
            // otherwise => create generic IOE and attach original exception as cause
            throw new IOException();
        }
    }

    @Override
    protected BaseFileAttributes fetchAttributesInternal(final ExamplePath path, final Class<?> type) throws IOException {
        try {
            // FIXME: fetch file attributes and convert them into BaseFileAttributes
            // If lastModifiedTime/lastAccessTime/creationTime cannot be determined, set
            // them to FileTime.fromMillis(0)
            return null;
        } catch (Exception ex) { // FIXME handle custom exceptions
            // FIXME: throw appropriate IOE subtype here:
            // path does not exist => NoSuchFileException
            // path attributes cannot be created due to permissions => AccessDeniedException
            // (attach
            // original exception as cause)
            // otherwise => create generic IOE and attach original exception as cause
            throw new IOException();
        }
    }

    @Override
    protected void checkAccessInternal(final ExamplePath path, final AccessMode... modes) throws IOException {
        // FIXME: check if you can perform the requested type of access on the file.
        // In most cases however there is nothing useful you can do here.
    }

    @Override
    protected void deleteInternal(final ExamplePath path) throws IOException {
        try {
            // FIXME: delete file or empty directory. Don't implement recursive deletion
            // here, as only empty directories should be deletable with this method.
        } catch (Exception ex) { // FIXME handle custom exceptions
            // FIXME: throw appropriate IOE subtype here:
            // path does not exist => NoSuchFileException
            // cannot be deleted due to permissions => AccessDeniedException (attach
            // original exception as cause)
            // otherwise => create generic IOE and attach original exception as cause
            throw new IOException();
        }
    }
}
