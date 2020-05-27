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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JFileChooser;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;

/**
 *
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

    @Override
    public String getPathOrURL() {
        if (m_filePanel == null) {
            // accessing from node model
            return m_selectedFile;
        }
        return m_filePanel.getSelectedFile();
    }

    @Override
    public boolean hasPathOrURL() {
        final String p = getPathOrURL();
        return p != null && !p.trim().isEmpty();
    }

    @Override
    public List<Path> getPaths(final Optional<FSConnection> fsConnection) throws IOException, InvalidSettingsException {
        final String p = getPathOrURL();
        try {
            final Path path = FileUtil.resolveToPath(FileUtil.toURL(p));
            if (path == null) {
                throw new IllegalArgumentException("Remote files not yet supported."); // FIXME
            }
            final List<Path> list = new ArrayList<>();
            list.add(path);
            return list;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(m_configKey, getPathOrURL().trim());
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

}
