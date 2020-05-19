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
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.util.FileUtil;
import org.knime.core.util.Pair;
import org.knime.filehandling.core.FSPluginConfig;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;
import org.knime.filehandling.core.filefilter.FileFilter;

/**
 * Class used to scan files in directories.
 *
 * @author Julian Bunzel, KNIME GmbH, Berlin
 */
public final class FileChooserHelper {

    /** FileSystem used to resolve the path (lazily initialized) */
    private FSConnection m_fsConnection;

    /** Settings object containing necessary information about e.g. file filtering */
    private final SettingsModelFileChooser2 m_settings;

    /** Optional containing a {@link FileFilter} if selected */
    private final FileFilter m_filter;

    /** Pair of integer containing the number of listed files and the number of filtered files. */
    private Pair<Integer, Integer> m_counts;

    final Optional<FSConnection> m_portObjectFSConnection;

    final int m_timeoutInMillis;

    /**
     * Creates a new instance of {@link FileChooserHelper} that uses the default url timeout
     * {@link FileUtil#getDefaultURLTimeoutMillis()} for custom URLs.
     *
     * @param portObjectFSConnection the {@link FSConnection} used to retrieve a file system if necessary.
     * @param settings the settings object containing necessary information about e.g. file filtering
     * @throws IOException thrown when the file system could not be retrieved.
     */
    public FileChooserHelper(final Optional<FSConnection> portObjectFSConnection, final SettingsModelFileChooser2 settings)
        throws IOException {
        this(portObjectFSConnection, settings, FileUtil.getDefaultURLTimeoutMillis());
    }

    /**
     * Creates a new instance of {@link FileChooserHelper}.
     *
     * @param portObjectFSConnection Optional FSConnection coming from a connected port object.
     * @param settings the settings object containing necessary information about e.g. file filtering
     * @param timeoutInMillis timeout in milliseconds, or -1 if not applicable.
     */
    public FileChooserHelper(final Optional<FSConnection> portObjectFSConnection, final SettingsModelFileChooser2 settings,
        final int timeoutInMillis) {
        m_filter = new FileFilter(settings.getFileFilterSettings());
        m_settings = settings;
        m_portObjectFSConnection = portObjectFSConnection;
        m_timeoutInMillis = timeoutInMillis;
    }

    /**
     * Returns the file system.
     *
     * @return the file system
     * @throws IOException thrown when the file system could not be retrieved.
     */
    public final FSFileSystem<?> getFileSystem() throws IOException {
        if (m_fsConnection == null) {
            m_fsConnection = FileSystemHelper.retrieveFSConnection(m_portObjectFSConnection, m_settings, m_timeoutInMillis);
        }
        return m_fsConnection.getFileSystem();
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
        final Path dirPath = getFileSystem().getPath(m_settings.getPathOrURL());
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
        final Path pathOrUrl = getPathFromSettings();
        final List<Path> toReturn;

        final BasicFileAttributes fileAttrs = Files.readAttributes(pathOrUrl, BasicFileAttributes.class);

        if (m_settings.readFilesFromFolder()) {
            if (!fileAttrs.isDirectory()) {
                throw new InvalidSettingsException(pathOrUrl.toString() + " is not a folder. Please specify a folder.");
            }
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
     * @throws InvalidSettingsException If the path could not be constructed because of an invalid setting, or if
     *             accessing the path is not allowed in the current context (e.g. on the server).
     * @throws IOException
     */
    public Path getPathFromSettings() throws InvalidSettingsException, IOException {
        if (m_settings.getPathOrURL() == null || m_settings.getPathOrURL().isEmpty()) {
            throw new InvalidSettingsException("No path specified");
        }

        final Path pathOrUrl;
        final Choice fileSystemChoice = m_settings.getFileSystemChoice().getType();
        switch (fileSystemChoice) {
            case CONNECTED_FS:
                pathOrUrl = getFileSystem().getPath(m_settings.getPathOrURL());
                break;
            case CUSTOM_URL_FS:
                final URI uri = URI.create(m_settings.getPathOrURL().replace(" ", "%20"));
                validateCustomURL(uri);
                pathOrUrl = getFileSystem().getPath(getURIPathQueryAndFragment(uri));
                break;
            case KNIME_FS:
                pathOrUrl = getFileSystem().getPath(m_settings.getPathOrURL());
                if (pathOrUrl.isAbsolute()) {
                    throw new InvalidSettingsException("The path must be relative, i.e. it must not start with '/'.");
                }
                break;
            case KNIME_MOUNTPOINT:
                pathOrUrl = getFileSystem().getPath(m_settings.getPathOrURL());
                if (!pathOrUrl.isAbsolute()) {
                    throw new InvalidSettingsException("The path must be absolute, i.e. it must start with '/'.");
                }
                break;
            case LOCAL_FS:
                validateLocalFsAccess();
                pathOrUrl = getFileSystem().getPath(m_settings.getPathOrURL());
                if (!pathOrUrl.isAbsolute()) {
                    throw new InvalidSettingsException("The path must be absolute.");
                }
                break;
            default:
                final String errMsg =
                    String.format("Unknown choice enum '%s', make sure the switch covers all cases!", fileSystemChoice);
                throw new RuntimeException(errMsg);
        }



        return pathOrUrl;
    }

    private String getURIPathQueryAndFragment(final URI uri) {
        final StringBuilder toReturn = new StringBuilder(uri.getPath());

        if (uri.getQuery() != null) {
            toReturn.append("?");
            toReturn.append(uri.getQuery());
        }

        if (uri.getFragment() != null) {
            toReturn.append("#");
            toReturn.append(uri.getFragment());
        }
        return toReturn.toString();
    }

    private void validateCustomURL(final URI uri) throws InvalidSettingsException {
        // validate scheme
        if (!uri.isAbsolute()) {
            throw new InvalidSettingsException("URL must start with a scheme, e.g. http:");
        }

        if (uri.getScheme().equals("file")) {
            validateLocalFsAccess();
        }

        if (uri.isOpaque()) {
            throw new InvalidSettingsException("URL must have forward slash ('/') after scheme, e.g. http://");
        }

        if (uri.getPath() == null) {
            throw new InvalidSettingsException("URL must specify a path, as in https://host/path/to/file");
        }
    }

    private void validateLocalFsAccess() throws InvalidSettingsException {
        final NodeContext nodeContext = NodeContext.getContext();

        if (nodeContext == null) {
            throw new InvalidSettingsException("No node context available");
        }

        final WorkflowContext workflowContext = nodeContext.getWorkflowManager().getContext();
        if (workflowContext == null) {
            throw new InvalidSettingsException("No workflow context available");
        }

        if (isOnServer(workflowContext) && !FSPluginConfig.load().allowLocalFsAccessOnServer()) {
            throw new InvalidSettingsException("Direct access to the local file system is not allowed on KNIME Server.");
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

    private static boolean isOnServer(final WorkflowContext context) {
        return context.getRemoteRepositoryAddress().isPresent() && context.getServerAuthToken().isPresent();
    }

}
