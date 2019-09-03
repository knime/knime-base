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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.node.FSConnectionFlowVariableProvider;
import org.knime.core.util.Pair;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
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
     * @param provider the {@link FSConnectionFlowVariableProvider} used to retrieve a file system from a flow variable
     *            if necessary
     * @param settings the settings object containing necessary information about e.g. file filtering
     */
    public FileChooserHelper(final FSConnectionFlowVariableProvider provider,
        final SettingsModelFileChooser2 settings) {
        m_filter = settings.getFilterFiles() ? Optional.of(new FileFilter(settings)) : Optional.empty();
        m_settings = settings;
        m_fileSystem = setFileSystem(provider, settings);
    }

    /** Method to set the file system */
    private static final FileSystem setFileSystem(final FSConnectionFlowVariableProvider provider,
        final SettingsModelFileChooser2 settings) {
        final FileSystemChoice choice = FileSystemChoice.getChoiceFromId(settings.getFileSystem());

        // Set the file system
        if (choice.equals(FileSystemChoice.getLocalFsChoice())) {
            return FileSystems.getDefault();
        } else if (choice.equals(FileSystemChoice.getCustomFsUrlChoice())) {
            // FIXME: Return correct FileSystem
            return FileSystems.getDefault();
        } else if (choice.equals(FileSystemChoice.getKnimeFsChoice())) {
            // FIXME: Return correct FileSystem
            return FileSystems.getDefault();
        } else {
            final Optional<String> connectionKey = provider.connectionKeyOf(settings.getFileSystem());
            if (connectionKey.isPresent()) {
                final Optional<FSConnection> optConn = FSConnectionRegistry.getInstance().retrieve(connectionKey.get());
                if (optConn.isPresent()) {
                    return optConn.get().getFileSystem();
                } else {
                    // FIXME: Throw something more fitting
                    throw new IllegalArgumentException();
                }
            } else {
                // FIXME: Throw something more fitting
                throw new IllegalArgumentException();
            }
        }
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
     * Scans the given directory and returns a list of {@link Path Paths}.
     *
     * @param dir the directory
     * @return a list of paths
     * @throws IOException thrown if directory could not be scanned
     */
    public final List<Path> scanDirectories(final String dir) throws IOException {
        setCounts(0, 0);
        final Path dirPath = m_fileSystem.getPath(dir);
        final boolean includeSubfolders = m_settings.getIncludeSubfolders();
        List<Path> paths = new ArrayList<>();
        if (Files.isDirectory(dirPath) && Files.isReadable(dirPath)) {
            try (final Stream<Path> stream = includeSubfolders ? Files.walk(dirPath) : Files.list(dirPath)) {
                if (m_filter.isPresent()) {
                    final FileFilter filter = m_filter.get();
                    filter.resetCount();
                    paths = stream.filter(p -> filter.isSatisfied(p)).collect(Collectors.toList());
                    setCounts(paths.size(), filter.getNumberOfFilteredFiles());
                } else {
                    paths = stream.collect(Collectors.toList());
                    setCounts(paths.size(), 0);
                }
            }
        } else {
            throw new IOException(dirPath + " is not a directory.");
        }
        return paths;
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
     * Returns Pair of integer containing the number of listed files and the number of filtered files.
     *
     * @return pair of integer containing the number of listed files and the number of filtered files.
     */
    public final Pair<Integer, Integer> getCounts() {
        return m_counts;
    }
}
