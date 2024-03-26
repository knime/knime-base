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
package org.knime.base.node.io.filehandling.csv.reader2;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.FileSystemHelper;
import org.knime.filehandling.core.defaultnodesettings.ValidationUtils;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractSettingsModelFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * Allows access to the {@link FSPath FSPaths} referred to by a {@link FileChooser} provided in the constructor. The
 * paths are also validated and respective exceptions are thrown if the settings yield invalid paths.
 *
 * <p>
 * It is largely taken from the respective accessor for {@link AbstractSettingsModelFileChooser legacy file choosers}
 * ({@link org.knime.filehandling.core.defaultnodesettings.filechooser.FileChooserPathAccessor legacy
 * FileChooserPathAccessor})
 * </p>
 *
 * @author Paul Bärnreuther
 */
final class FileChooserPathAccessor implements Closeable {

    /**
     * The root location i.e. the location to start scanning the file tree from
     */
    private final FSLocation m_location;

    /**
     * The connection used to create the {@link FSFileSystem} used to create the {@link FSPath paths}.</br>
     * If m_rootLocation isn't empty, this will be initialized with m_rootLocation.get().
     */
    private FSConnection m_connection;

    private FSFileSystem<?> m_fileSystem;

    private final FilterMode m_filterMode;

    /**
     * Creates a new FileChooserAccessor for the provided location.</br>
     * The settings are not validated in this constructor but instead if {@link #getPaths()} is called.
     *
     * @param fileChooser provided by the user
     */
    public FileChooserPathAccessor(final FileChooser fileChooser) { //NOSONAR
        m_location = fileChooser.getFSLocation();
        m_filterMode = FilterMode.FILE;
    }

    private FSPath getPathFromConvenienceFs() throws InvalidSettingsException {
        switch (m_location.getFSCategory()) {
            case CONNECTED:
                throw new IllegalStateException("The file system is not connected.");
            case CUSTOM_URL:
                ValidationUtils.validateCustomURLLocation(m_location);
                return m_fileSystem.getPath(m_location);
            case RELATIVE:
                final FSPath path = m_fileSystem.getPath(m_location);
                ValidationUtils.validateKnimeFSPath(path);
                return path;
            case MOUNTPOINT:
                return m_fileSystem.getPath(m_location);
            case LOCAL:
                ValidationUtils.validateLocalFsAccess();
                return m_fileSystem.getPath(m_location);
            case HUB_SPACE:
                return m_fileSystem.getPath(m_location);
            default:
                throw new IllegalStateException("Unsupported file system category: " + m_location.getFSCategory());
        }
    }

    List<Path> getPaths() throws IOException, InvalidSettingsException {
        return getFSPaths().stream().map(Path.class::cast).toList();
    }

    private List<FSPath> getFSPaths() throws IOException, InvalidSettingsException {
        return handleSinglePath(getRootPath());
    }

    private List<FSPath> handleSinglePath(final FSPath rootPath) throws IOException, InvalidSettingsException {
        final BasicFileAttributes attr = Files.readAttributes(rootPath, BasicFileAttributes.class);
        String workflowOrFile = m_filterMode.getTextLabel();
        CheckUtils.checkSetting(!attr.isDirectory(), "%s is a folder. Please specify a " + workflowOrFile + ".",
            rootPath);

        return Collections.singletonList(rootPath);
    }

    public FSPath getRootPath() throws IOException, InvalidSettingsException {
        final String errorSuffix = m_filterMode.getTextLabel();
        CheckUtils.checkSetting(!m_location.getPath().trim().isEmpty(),
            String.format("Please specify a %s", errorSuffix));
        final FSPath rootPath = getPath();

        CheckUtils.checkSetting(FSFiles.exists(rootPath), "The specified %s %s does not exist.", errorSuffix, rootPath);
        if (!Files.isReadable(rootPath)) {
            throw ExceptionUtil.createAccessDeniedException(rootPath);
        }
        return rootPath;
    }

    private FSPath getPath() throws InvalidSettingsException {
        initializeFileSystem();
        return getPathFromConvenienceFs();
    }

    private void initializeFileSystem() {
        if (m_connection == null) {
            m_connection = getConnection();
            m_fileSystem = m_connection.getFileSystem();
        }
    }

    private FSConnection getConnection() {
        return FileSystemHelper.retrieveFSConnection(Optional.empty(), m_location).orElseThrow(
            () -> new IllegalStateException("No file system connection available. Execute connector node."));
    }

    @Override
    public void close() throws IOException {
        if (m_fileSystem != null) {
            m_fileSystem.close();
            m_connection.close();
        }
    }

}
