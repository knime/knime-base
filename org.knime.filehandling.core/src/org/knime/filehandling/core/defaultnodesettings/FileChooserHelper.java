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

    private final FSConnectionFlowVariableProvider m_fsConnectionFlowVarProvider;

    private FileSystem m_fileSystem;

    private SettingsModelFileChooser2 m_settings;

    final FileFilter m_filter;

    private Pair<Integer, Integer> m_counts;

    /**
     * @param provider
     * @param settings
     */
    public FileChooserHelper(final FSConnectionFlowVariableProvider provider,
        final SettingsModelFileChooser2 settings) {
        if (settings.getFilterFiles()) {
            m_filter = new FileFilter(settings);
        } else {
            m_filter = null;
        }
        m_fsConnectionFlowVarProvider = provider;
        m_settings = settings;
    }

    private static final FileSystem setFileSystem(final FSConnectionFlowVariableProvider provider,
        final SettingsModelFileChooser2 settings) {
        final FSConnection connection;
        // FIXME: Hardcoded "Local File System" for testing... Other FSConnections not implemented yet!
        // final Optional<String> connectionKey = provider.connectionKeyOf(settings.getFileSystem());
        final Optional<String> connectionKey = provider.connectionKeyOf("Local File System");
        if (connectionKey.isPresent()) {
            final Optional<FSConnection> optConn = FSConnectionRegistry.getInstance().retrieve(connectionKey.get());
            if (optConn.isPresent()) {
                return optConn.get().getFileSystem();
            } else {
                // Throw something
                throw new IllegalArgumentException();
            }
        } else {
            // Throw something
            throw new IllegalArgumentException();
        }
    }

    public FileSystem getFileSystem() {
        if (m_fileSystem == null) {
            m_fileSystem = setFileSystem(m_fsConnectionFlowVarProvider, m_settings);
        }
        return m_fileSystem;
    }

    /**
     * @param dir
     * @return
     * @throws IOException
     */
    public final List<Path> scanDirectories(final String dir) throws IOException {
        setCounts(0, 0);
        final Path dirPath = m_fileSystem.getPath(dir);
        final boolean includeSubfolders = m_settings.getIncludeSubfolders();
        List<Path> paths = new ArrayList<>();
        if (Files.isDirectory(dirPath) && Files.isReadable(dirPath)) {
            try (final Stream<Path> stream = includeSubfolders ? Files.walk(dirPath) : Files.list(dirPath)) {
                if (m_filter != null) {
                    m_filter.resetCount();
                    paths = stream.filter(p -> m_filter.isSatisfied(p)).collect(Collectors.toList());
                    setCounts(paths.size(), m_filter.getNumberOfFilteredFiles());
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
     * @param numberOfRemainingFiles
     * @param numberOfFilteredFiles
     */
    private void setCounts(final int numberOfRemainingFiles, final int numberOfFilteredFiles) {
        m_counts = new Pair<>(numberOfRemainingFiles, numberOfFilteredFiles);
    }

    public Pair<Integer, Integer> getCounts() {
        return m_counts;
    }

    public void setSettings(final SettingsModelFileChooser2 settings) {
        m_settings = settings;
        m_fileSystem = setFileSystem(m_fsConnectionFlowVarProvider, settings);
        setCounts(0, 0);
    }

}
