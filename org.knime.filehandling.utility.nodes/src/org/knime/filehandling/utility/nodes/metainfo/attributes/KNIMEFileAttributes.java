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
 *   Sep 11, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.metainfo.attributes;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.knime.core.node.util.CheckUtils;

/**
 * Abstract class wrapping {@link BasicFileAttributes} and exposing additional functionality required by the
 * {@link KNIMEFileAttributesConverter}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public abstract class KNIMEFileAttributes {

    private final BasicFileAttributes m_fileAttributes;

    private final long m_size;

    /**
     * Constructor allowing to calculate the overall size of the provided path if it is a folder.
     *
     * @param p the path
     * @param calcFolderSize if {@code true} the size of overall size of the folder will be calculated
     * @param fileAttributes the path's {@link BasicFileAttributes}
     * @throws IOException - If anything went wrong while calculating the folders size
     */
    KNIMEFileAttributes(final Path p, final boolean calcFolderSize, final BasicFileAttributes fileAttributes)
        throws IOException {
        CheckUtils.checkNotNull(p, "The path cannot be null");
        CheckUtils.checkNotNull(fileAttributes, "The fileAttributes cannot be null");
        m_fileAttributes = fileAttributes;
        if (m_fileAttributes.isDirectory() && calcFolderSize) {
            m_size = size(p);
        } else {
            m_size = m_fileAttributes.size();
        }
    }

    private static long size(final Path path) throws IOException {
        final AtomicLong size = new AtomicLong(0);

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                throws IOException {
                size.addAndGet(attrs.size());
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }

        });
        return size.get();
    }

    /**
     * Returns the time of last modification.
     *
     * <p>
     * If the file system implementation does not support a time stamp to indicate the time of last modification then
     * this method returns an implementation specific default value, typically a {@code FileTime} representing the epoch
     * (1970-01-01T00:00:00Z).
     *
     * @return a {@code FileTime} representing the time the file was last modified
     */
    final FileTime lastModifiedTime() {
        return m_fileAttributes.lastModifiedTime();
    }

    /**
     * Returns the creation time. The creation time is the time that the file was created.
     *
     * <p>
     * If the file system implementation does not support a time stamp to indicate the time when the file was created
     * then this method returns an implementation specific default value, typically the {@link #lastModifiedTime()
     * last-modified-time} or a {@code FileTime} representing the epoch (1970-01-01T00:00:00Z).
     *
     * @return a {@code FileTime} representing the time the file was created
     */
    final FileTime creationTime() {
        return m_fileAttributes.creationTime();
    }

    /**
     * Tells whether the file is a directory.
     *
     * @return {@code true} if the file is a directory
     */
    final boolean isDirectory() {
        return m_fileAttributes.isDirectory();
    }

    /**
     * Returns the size of the file/folder (in bytes). The size may differ from the actual size on the file system due
     * to compression, support for sparse files, or other reasons. The size of files that are not {@link #isRegularFile
     * regular} files is implementation specific and therefore unspecified. Depending on the intialization the size of a
     * folder might refer to its size or the size of itself plus the size of all contained files/folders.
     *
     * @return the file/folder size, in bytes
     */
    final long size() {
        return m_size;
    }

    /**
     * @return The {@link PosixFileAttributes} for the file, if ones are available. Throws an exception otherwise.
     */
    final PosixFileAttributes getPosixAttributes() {
        if (m_fileAttributes instanceof PosixFileAttributes) {
            return (PosixFileAttributes)m_fileAttributes;
        } else {
            throw new UnsupportedOperationException("POSIX attributes are not available.");
        }
    }

    /**
     * Returns the is readable flag for the given file/folder.
     *
     * @return the is readable flag
     */
    abstract Optional<Boolean> isReadable();

    /**
     * Returns the is writable flag for the given file/folder.
     *
     * @return the is readable flag
     */
    abstract Optional<Boolean> isWritable();

    /**
     * Returns the is executable flag for the given file/folder.
     *
     * @return the is readable flag
     */
    abstract Optional<Boolean> isExecutable();
}
