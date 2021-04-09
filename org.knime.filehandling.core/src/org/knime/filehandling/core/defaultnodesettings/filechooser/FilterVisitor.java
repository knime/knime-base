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
 *   May 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.FileFilterStatistic;
import org.knime.filehandling.core.defaultnodesettings.filtermode.FileAndFolderFilter;

/**
 * A {@link SimpleFileVisitor} that uses a {@link FileAndFolderFilter} to filter the observed files and folders.</br>
 * If a folder is filtered out, its subtree is skipped.</br>
 * The accepted files/folders can be retrieved via the getPaths() method, after traversal completes. Whether files
 * and/or folders should be included in the paths can be specified via the corresponding constructor arguments. The
 * FileVisitor keeps track of the number of visited files and folders, as well as the number of filtered out files and
 * folders. The {@link FileFilterStatistic} can be retrieved via the getFileFilterStatistic() method.<br>
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class FilterVisitor extends SimpleFileVisitor<Path> {

    private final FileAndFolderFilter m_filter;

    private final boolean m_includeFiles;

    private final boolean m_includeFolders;

    private final boolean m_includeSubfolders;

    private final boolean m_followLinks;

    private final List<Path> m_paths = new ArrayList<>();

    private int m_visitedFiles;

    private int m_visitedFolders = -1;

    /**
     * Constructor.
     *
     * @param filter for filtering files and folders
     * @param includeFiles whether files should be added to the paths returned by getPaths()
     * @param includeFolders whether folders should be added to the paths returned by getPaths()
     */
    FilterVisitor(final FileAndFolderFilter filter, final boolean includeFiles, final boolean includeFolders,
        final boolean includeSubfolders, final boolean followLinks) {
        m_filter = filter;
        m_includeFiles = includeFiles;
        m_includeFolders = includeFolders;
        m_includeSubfolders = includeSubfolders;
        m_followLinks = followLinks;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        final FileVisitResult result = super.visitFile(file, attrs);
        // also called for directories (if max depth is hit by Files.walkFileTree) for these directories
        // #preVisitDirectory is not being invoked
        if (!linkAwareIsDirectory(file, attrs) && m_filter.testFolderName(file.getParent())) {
            m_visitedFiles++;
            if (m_includeFiles && m_filter.test(file, attrs)) {
                m_paths.add(file);
            }
        } else if (attrs.isDirectory()) {
            m_visitedFolders++;
            if (m_includeFolders && m_filter.test(file, attrs)) {
                m_paths.add(file);
            }
        } else {
            // we only care for files and folders
        }
        return result;
    }

    private boolean linkAwareIsDirectory(final Path file, final BasicFileAttributes attrs)
        throws AccessDeniedException {
        if (!m_followLinks && attrs.isSymbolicLink()) {
            // if links aren't followed, then attrs.isDirectory returns false for a directory symlink
            // however, we don't ever want a directory symlink to be treated as a file, so we follow the link to see if
            // it points to a file or directory. Note that we don't walk the directory, we just check if the symlink
            // points to one. The main reason for this is that Files.newInput/OutputStream only works for symlinks to
            // files and throws an exception for directories.
            return FSFiles.isDirectory(file);
        } else {
            return attrs.isDirectory();
        }
    }

    @Override
    public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
        return super.visitFileFailed(file, ExceptionUtil.wrapIOException(exc));
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        super.preVisitDirectory(dir, attrs);
        m_visitedFolders++;
        // the root directory is ignored
        if (m_visitedFolders > 0 && m_includeSubfolders && m_includeFolders && m_filter.test(dir, attrs)) {
            m_paths.add(dir);
        }
        return m_visitedFolders == 0 || m_filter.visitFolder(dir) ? FileVisitResult.CONTINUE
            : FileVisitResult.SKIP_SUBTREE;
    }

    List<Path> getPaths() {
        return m_paths;
    }

    FileFilterStatistic getFileFilterStatistic() {
        return new FileFilterStatistic(m_filter.getNumberOfFilteredFiles(), m_filter.getNumberOfFilteredHiddenFiles(),
            m_filter.getNumberOfFilteredSpecialFiles(), m_visitedFiles, m_filter.getNumberOfFilteredFolders(),
            m_filter.getNumberOfFilteredHiddenFolders(), m_visitedFolders);
    }

}
