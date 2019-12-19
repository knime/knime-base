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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.node.FSConnectionFlowVariableProvider;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.util.FileUtil;
import org.knime.core.util.Pair;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.knime.KNIMEFileSystem;
import org.knime.filehandling.core.connections.knime.KNIMEPath;
import org.knime.filehandling.core.connections.knimeremote.KNIMERemotePath;
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
    private final FileFilter m_filter;

    /** Pair of integer containing the number of listed files and the number of filtered files. */
    private Pair<Integer, Integer> m_counts;

    /**
     * Creates a new instance of {@link FileChooserHelper} that uses the default url timeout
     * {@link FileUtil#getDefaultURLTimeoutMillis()} for custom URLs.
     *
     * @param fs the {@link FSConnectionFlowVariableProvider} used to retrieve a file system from a flow variable if
     *            necessary
     * @param settings the settings object containing necessary information about e.g. file filtering
     * @throws IOException thrown when the file system could not be retrieved.
     */
    public FileChooserHelper(final Optional<FSConnection> fs, final SettingsModelFileChooser2 settings)
        throws IOException {
        this(fs, settings, FileUtil.getDefaultURLTimeoutMillis());
    }

    /**
     * Creates a new instance of {@link FileChooserHelper}.
     *
     * @param fs the {@link FSConnectionFlowVariableProvider} used to retrieve a file system from a flow variable if
     *            necessary
     * @param settings the settings object containing necessary information about e.g. file filtering
     * @param timeoutInMillis timeout in milliseconds for the custom URL file system
     * @throws IOException thrown when the file system could not be retrieved.
     */
    public FileChooserHelper(final Optional<FSConnection> fs, final SettingsModelFileChooser2 settings,
        final int timeoutInMillis) throws IOException {
        m_filter = new FileFilter(settings.getFileFilterSettings());
        m_settings = settings;
        m_fileSystem = FileSystemHelper.retrieveFileSystem(fs, settings, timeoutInMillis);
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

            m_filter.resetCount();
            final Path[] pathArray = stream.filter(m_filter).toArray(Path[]::new);
            // enforce lexicographic sorting on the paths
            Arrays.sort(pathArray);
            paths = Collections.unmodifiableList(Arrays.asList(pathArray));
            setCounts(paths.size(), m_filter.getNumberOfFilteredFiles());
        }
        return paths;
    }

    /**
     * Returns a list of {@link Path} if the input String represents a directory its contents are scanned, otherwise the
     * list contains the file, if it is readable.
     *
     * @return a list of path to read
     * @throws IOException if an I/O error occurs
     * @throws InvalidSettingsException
     */
    public final List<Path> getPaths() throws IOException, InvalidSettingsException {
        Path pathOrUrl = getPathFromSettings();
        final List<Path> toReturn;

        if (Files.isDirectory(pathOrUrl) && m_settings.readFilesFromFolder()) {
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

    /**
     * Creates and returns a new Path object according to the path or URL provided by the underlying settings model.
     *
     * @return Path leading to the path or url provided by the underlying settings model
     * @throws InvalidSettingsException
     */
    public Path getPathFromSettings() throws InvalidSettingsException {
        final Path pathOrUrl;
        if (FileSystemChoice.getCustomFsUrlChoice().equals(m_settings.getFileSystemChoice())) {
            final URI uri = URI.create(m_settings.getPathOrURL());
            pathOrUrl = m_fileSystem.provider().getPath(uri);
        } else {
            pathOrUrl = m_fileSystem.getPath(m_settings.getPathOrURL());
        }
        validateKNIMERelativePath(pathOrUrl);

        return pathOrUrl;
    }

    private static void validateKNIMERelativePath(final Path path) throws InvalidSettingsException {
        if (path instanceof KNIMEPath) {
            final KNIMEPath knimePath = (KNIMEPath) path;
            final URL url = knimePath.getURL();
            try {
                // This called to check if the URL can be resolved, will throw an exception if not!
                FileUtil.resolveToPath(url);
            } catch (IOException | URISyntaxException ex) {
                throw new InvalidSettingsException(ex.getMessage());
            }
        }
    }

    /**
     * Returns a clone of the underlying {@link SettingsModelFileChooser2}.
     *
     * @return a clone of the underlying {@code SettingsModelFileChooser2}
     */
    public final SettingsModelFileChooser2 getSettingsModel() {
        return m_settings.clone();
    }

    /**
     * Checks whether a given path is a {@link KNIMEPath}.
     *
     * @param path path under test
     * @return true if the path is an instance of KNIMEPath
     */
    @SuppressWarnings("static-method")
    public boolean isKNIMERelativePath(final Path path) {
        return path instanceof KNIMEPath;
    }

    /**
     * Determines if the provided path can be executed in the given context.
     *
     * @param path path under test
     * @param context the workflow context, either local or on a server
     * @throws InvalidSettingsException if the path cannot be executed in the context
     */
    @SuppressWarnings({"resource", "static-method"})
    public void canExecuteOnServer(final Path path, final WorkflowContext context) throws InvalidSettingsException {
        if (isOnServer(context) && path instanceof KNIMEPath) {
            final KNIMEPath knimePath = (KNIMEPath) path;
            final KNIMEFileSystem fileSystem = (KNIMEFileSystem) knimePath.getFileSystem();
            if (fileSystem.getConnectionType().equals(KNIMEConnection.Type.NODE_RELATIVE)) {
                throw new InvalidSettingsException("Executing node relative paths on KNIME server is not supported.");
            }
        }

        if (isOnServer(context) && path instanceof KNIMERemotePath) {
            throw new InvalidSettingsException("Executing mountpoint absolute paths on KNIME server is not supported.");
        }
    }

    private static boolean isOnServer(final WorkflowContext context) {
        return context.getRemoteRepositoryAddress().isPresent() && context.getServerAuthToken().isPresent();
    }

}
