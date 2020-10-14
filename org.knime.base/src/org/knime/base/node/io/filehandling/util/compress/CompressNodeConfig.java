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
 *   27 Aug 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.util.compress;

import java.util.Arrays;
import java.util.EnumSet;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * Node Config for the "Compress Files/Folder" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class CompressNodeConfig {

    static final String INVALID_EXTENSION_ERROR =
        "Invalid destination file extension. Please find the valid extensions in the node description.";

    private static final String CFG_INPUT_LOCATION = "input_location";

    private static final String CFG_OUTPUT_LOCATION = "output_location";

    private static final String CFG_INCLUDE_SELECTED_FOLDER = "include_selected_source_folder";

    private static final String CFG_FLATTEN_HIERARCHY = "flatten_hierarchy";

    private final SettingsModelReaderFileChooser m_inputLocationChooserModel;

    private final SettingsModelWriterFileChooser m_destinationFileChooserModel;

    private boolean m_flattenHierarchy;

    private boolean m_includeFolder;

    static final String[] FILE_EXTENSIONS = new String[]{".zip", ".jar", ".tar", ".tar.gz", ".tar.bz2", ".cpio", ".ar"};

    /**
     * Constructor
     *
     * @param portsConfig {@link PortsConfiguration} of the node
     */
    CompressNodeConfig(final PortsConfiguration portsConfig) {
        m_inputLocationChooserModel = new SettingsModelReaderFileChooser(CFG_INPUT_LOCATION, portsConfig,
            CompressNodeFactory.CONNECTION_INPUT_FILE_PORT_GRP_NAME, FilterMode.FILE);

        m_destinationFileChooserModel = new SettingsModelWriterFileChooser(CFG_OUTPUT_LOCATION, portsConfig,
            CompressNodeFactory.CONNECTION_OUTPUT_DIR_PORT_GRP_NAME, FilterMode.FILE, FileOverwritePolicy.FAIL,
            EnumSet.of(FileOverwritePolicy.FAIL, FileOverwritePolicy.OVERWRITE), FILE_EXTENSIONS);

        m_includeFolder = false;
        m_flattenHierarchy = false;
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) {
        includeSelectedFolder(settings.getBoolean(CFG_INCLUDE_SELECTED_FOLDER, false));
        flattenHierarchy(settings.getBoolean(CFG_FLATTEN_HIERARCHY, false));
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveNonSettingModelParameters(settings);
        validateFileExtension();
    }

    private void saveIncludeParentFolder(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_INCLUDE_SELECTED_FOLDER, includeParentFolder());
    }

    private void saveFlattenHierarchy(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_FLATTEN_HIERARCHY, flattenHierarchy());
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inputLocationChooserModel.validateSettings(settings);
        m_destinationFileChooserModel.validateSettings(settings);
        final FSLocation destination = m_destinationFileChooserModel.extractLocation(settings);
        CheckUtils.checkSetting(hasValidFileExtension(destination.getPath()), INVALID_EXTENSION_ERROR);

        settings.getBoolean(CFG_INCLUDE_SELECTED_FOLDER);
        settings.getBoolean(CFG_FLATTEN_HIERARCHY);
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        loadSettingsForDialog(settings);

        m_inputLocationChooserModel.loadSettingsFrom(settings);
        m_destinationFileChooserModel.loadSettingsFrom(settings);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        saveNonSettingModelParameters(settings);

        m_inputLocationChooserModel.saveSettingsTo(settings);
        m_destinationFileChooserModel.saveSettingsTo(settings);
    }

    private void saveNonSettingModelParameters(final NodeSettingsWO settings) {
        saveIncludeParentFolder(settings);
        saveFlattenHierarchy(settings);
    }

    /**
     * Returns the {@link SettingsModelWriterFileChooser} used to select where to save the archive file.
     *
     * @return the {@link SettingsModelWriterFileChooser} used to select a directory
     */
    SettingsModelWriterFileChooser getTargetFileChooserModel() {
        return m_destinationFileChooserModel;
    }

    /**
     * Returns the {@link SettingsModelReaderFileChooser} used to select a file, folder or files in folder which should
     * get compressed.
     *
     * @return the {@link SettingsModelReaderFileChooser} used to select a directory
     */
    SettingsModelReaderFileChooser getInputLocationChooserModel() {
        return m_inputLocationChooserModel;
    }

    boolean includeParentFolder() {
        return m_includeFolder;
    }

    void includeSelectedFolder(final boolean include) {
        m_includeFolder = include;
    }

    /**
     * Returns the flag deciding whether or not to flatten the hierarchy during compression.
     *
     * @return {code true} if the hierarchy has to be flattened during compression and {@code false} otherwise
     */
    boolean flattenHierarchy() {
        return m_flattenHierarchy;
    }

    /**
     * Sets the flag indicating whether or not to flatten the hierarchy during compression.
     *
     * @param flattenHierarchy {@code true} if the hierarchy has to be flattened during compression and {@code false}
     *            otherwise
     */
    void flattenHierarchy(final boolean flattenHierarchy) {
        m_flattenHierarchy = flattenHierarchy;
    }

    /**
     * Checks if a file path ends with a valid file extension
     *
     * @param path the file path to check
     * @return <code>true</code> if path ends with valid file extension, <code>false</code> otherwise
     */
    static boolean hasValidFileExtension(final String path) {
        final String lowerCase = path.toLowerCase();
        return Arrays.stream(FILE_EXTENSIONS).anyMatch(lowerCase::endsWith);
    }

    /**
     * Checks if the selected destination file path ends with a valid file extension
     *
     * @return <code>true</code> if path ends with valid file extension, <code>false</code> otherwise
     */
    boolean hasValidFileExtension() {
        return hasValidFileExtension(m_destinationFileChooserModel.getLocation().getPath());
    }

    private void validateFileExtension() throws InvalidSettingsException {
        CheckUtils.checkSetting(hasValidFileExtension(), INVALID_EXTENSION_ERROR);
    }
}
