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
 *   Aug 28, 2019 (julian): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.node.FSConnectionFlowVariableProvider;
import org.knime.core.util.Pair;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.filefilter.FileFilter;

/**
 * Class used to scan files in directories.
 *
 * @author Julian Bunzel, KNIME GmbH, Berlin
 */
public final class FileChooserHelper {

    /** FileSystem used to resolve the path */
    private final FileSystem m_fileSystem;

    /** Settings object containing necessary information about e.g. file filtering */
    private final SettingsModelFileChooser2 m_settings;

    /** Optional containing a {@link FileFilter} if selected */
    private final Optional<FileFilter> m_filter;

    /** Pair of integer containing the number of listed files and the number of filtered files. */
    private Pair<Integer, Integer> m_counts;

    /**
     * Creates a new instance of {@link FileChooserHelper}.
     *
     * @param fs the {@link FSConnectionFlowVariableProvider} used to retrieve a file system from a flow variable
     *            if necessary
     * @param settings the settings object containing necessary information about e.g. file filtering
     * @throws IOException thrown when the file system could not be retrieved.
     */
    public FileChooserHelper(final Optional<FSConnection> fs, final SettingsModelFileChooser2 settings)
        throws IOException {

        m_filter = settings.getFilterFiles() ? Optional.of(new FileFilter(settings)) : Optional.empty();
        m_settings = settings;
        m_fileSystem = FileSystemHelper.retrieveFileSystem(fs, settings);
    }

    /**
     * Returns the file system.
     *
     * @return the file system
     */
    public final FileSystem getFileSystem() {
        return m_fileSystem;
    }

    /**
     * Assumes that the file specified in the settings model is a folder, scans the folder for files matching the filter
     * from the settings model, and returns a list of matching {@link Path}s.
     *
     * @return a list of paths that matched the filter from the settings model.
     * @throws IOException thrown if directory could not be scanned
     */
    public final List<Path> scanDirectoryTree() throws IOException {
        setCounts(0, 0);
        final Path dirPath = m_fileSystem.getPath(m_settings.getPathOrURL());
        final boolean includeSubfolders = m_settings.getIncludeSubfolders();

        final List<Path> paths;
        try (final Stream<Path> stream = includeSubfolders
            ? Files.walk(dirPath, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS) : Files.list(dirPath)) {
            if (m_filter.isPresent()) {
                final FileFilter filter = m_filter.get();
                filter.resetCount();
                paths = stream.filter(filter::isSatisfied).collect(Collectors.toList());
                setCounts(paths.size(), filter.getNumberOfFilteredFiles());
            } else {
                paths = stream.filter(p -> !Files.isDirectory(p)).collect(Collectors.toList());
                setCounts(paths.size(), 0);
            }
        }
        return paths;
    }

    /**
     * Returns a list of {@link Path} if the input String represents a directory its contents are scanned, otherwise the
     * list contains the file, if it is readable.
     *
     * @return a list of path to read
     * @throws IOException if an I/O error occurs
     */
    public final List<Path> getPaths() throws IOException {

        final Path pathOrUrl;
        if (m_settings.getFileSystemChoice() == FileSystemChoice.getCustomFsUrlChoice()) {
            final URI uri = URI.create(m_settings.getPathOrURL());
            pathOrUrl = m_fileSystem.provider().getPath(uri);
        } else {
            pathOrUrl = m_fileSystem.getPath(m_settings.getPathOrURL());
        }

        final List<Path> toReturn;

        if (Files.isDirectory(pathOrUrl)) {
            toReturn = scanDirectoryTree();
        } else {
            toReturn = Collections.singletonList(pathOrUrl);
        }

        return toReturn;
    }

    /**
     * Sets a pair of integers containing the number of listed files and the number of filtered files
     *
     * @param numberOfRemainingFiles number of remaining files
     * @param numberOfFilteredFiles number of filtered files
     */
    private final void setCounts(final int numberOfRemainingFiles, final int numberOfFilteredFiles) {
        m_counts = new Pair<>(numberOfRemainingFiles, numberOfFilteredFiles);
    }

    /**
     * Returns the number of files that matched the filter, and the number of files that did not match the filter. The
     * sum of the two numbers is the total amount of files that were scanned.
     *
     * @return pair of integer containing the number of files that matched the filter, and the number of files that did
     *         not match the filter.
     */
    public final Pair<Integer, Integer> getCounts() {
        return m_counts;
    }
}
