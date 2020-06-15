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
 *   May 27, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.table.csv.simple;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.ClosedFileSystemException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.JFileChooser;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice;
import org.knime.filehandling.core.defaultnodesettings.FileSystemHelper;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.FileFilterStatistic;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;

/**
 * Wrapper class for {@link FilesHistoryPanel} that implements {@link PathSettings}.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class PathAwareFileHistoryPanel implements PathSettings {

    private FilesHistoryPanel m_filePanel;

    private static final String DEFAULT_FILE = "";

    private String m_selectedFile = DEFAULT_FILE;

    private final String m_configKey;

    PathAwareFileHistoryPanel(final String configKey) {
        m_configKey = configKey;
    }

    void createFileHistoryPanel(final FlowVariableModel fvm, final String historyID) {
        if (m_filePanel == null) {
            m_filePanel = new FilesHistoryPanel(fvm, historyID, LocationValidation.FileInput, "");
            m_filePanel.setDialogType(JFileChooser.OPEN_DIALOG);
            m_filePanel.setToolTipText("Enter an URL of an ASCII datafile, select from recent files, or browse");
        }
    }

    FilesHistoryPanel getFileHistoryPanel() {
        return m_filePanel;
    }

    void setPath(final String url) {
        if (m_filePanel == null) {
            m_selectedFile = url;
        } else {
            m_filePanel.setSelectedFile(url);
        }
    }

    @Override
    public String getPath() {
        if (m_filePanel == null) {
            // accessing from node model
            return m_selectedFile;
        }
        return m_filePanel.getSelectedFile();
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(m_configKey, getPath().trim());
        if (m_filePanel != null) {
            m_filePanel.addToHistory();
        }
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selectedFile = settings.getString(m_configKey, DEFAULT_FILE);
        if (m_filePanel != null) {
            m_filePanel.setSelectedFile(m_selectedFile);
        }
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(m_configKey);
    }

    /**
     * @return the configKey
     */
    public String getConfigKey() {
        return m_configKey;
    }

    @Override
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer)
        throws InvalidSettingsException {
        if (!hasPath()) {
            throw new InvalidSettingsException("Please specify a location");
        }
    }

    @Override
    public ReadPathAccessor createReadPathAccessor() {
        return new PathAwareReadAccessor(getPath());
    }

    private boolean hasPath() {
        final String p = getPath();
        return p != null && !p.trim().isEmpty();
    }

    private static class PathAwareReadAccessor implements ReadPathAccessor {

        private final String m_path;

        private FSConnection m_connection;

        private FSLocation m_fsLocation;

        private boolean m_wasClosed = false;

        PathAwareReadAccessor(final String path) {
            m_path = path;
        }

        @Override
        public void close() throws IOException {
            if (m_connection != null) {
                m_connection.close();
            }
            m_wasClosed = true;
            m_connection = null;
        }

        @Override
        public List<FSPath> getFSPaths(final Consumer<StatusMessage> statusMessageConsumer)
            throws IOException, InvalidSettingsException {
            return Arrays.asList(getRootPath(statusMessageConsumer));
        }

        @SuppressWarnings("resource")
        @Override
        public FSPath getRootPath(final Consumer<StatusMessage> statusMessageConsumer) {
            if (m_wasClosed) {
                throw new ClosedFileSystemException();
            }
            if (m_connection == null) {
                if (isURL()) {
                    m_fsLocation = new FSLocation(FileSystemChoice.Choice.CUSTOM_URL_FS.toString(), "5000", m_path);
                } else {
                    m_fsLocation = new FSLocation(FileSystemChoice.Choice.LOCAL_FS.toString(), m_path);
                }
            }
            m_connection = FileSystemHelper.retrieveFSConnection(Optional.empty(), m_fsLocation)
                .orElseThrow(IllegalStateException::new);
            return m_connection.getFileSystem().getPath(m_fsLocation);
        }

        @SuppressWarnings("unused")
        private boolean isURL() {
            try {
                new URL(m_path.replace(" ", "%20"));
                return true;
            } catch (final MalformedURLException e) {
                return false;
            }
        }

        @Override
        public FileFilterStatistic getFileFilterStatistic() {
            CheckUtils.checkState(m_connection != null,
                "No statistic available. Call getFSPaths() or getPaths() first.");
            return new FileFilterStatistic(0, 1, 0, 0);
        }

    }

}
