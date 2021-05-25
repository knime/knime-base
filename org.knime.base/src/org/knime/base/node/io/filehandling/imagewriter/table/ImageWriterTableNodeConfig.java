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
 *   15 Mar 2021 (Laurin Siefermann, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.imagewriter.table;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * Node config of the image writer table node.
 *
 * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
 */
final class ImageWriterTableNodeConfig {

    private static final String CFG_OUTPUT_LOCATION = "output_location";

    private static final String CFG_IMG_COLUMN_NAME = "image_column";

    private static final String CFG_REMOVE_IMG_COLUMN = "remove_image_column";

    private static final String CFG_USER_DEFINED_OUTPUT_FILENAME_NAME = "filename_pattern";

    private static final String CFG_OUTPUT_FILENAME_COLUMN_NAME = "filename_column";

    private static final String CFG_GENERATE_FILE_NAMES = "generate_file_names";

    private final SettingsModelWriterFileChooser m_folderChooserModel;

    private final SettingsModelString m_imgColSelectionModel;

    private final SettingsModelBoolean m_removeImgColModel;

    private final SettingsModelString m_userDefinedOutputFilenameModel;

    private final SettingsModelColumnName m_filenameColSelectionModel;

    private boolean m_isGenerateFilenameRadio;

    // Allows one instance of ?, file extension will be automatically detected, no spaces are allowed in filenames.
    private static final Pattern CFG_USER_DEFINED_FILENAME_PATTERN = Pattern.compile("^[\\w,.-]*\\?{1}[\\w,.-]*$");

    /**
     * Constructor.
     *
     * @param portsConfig
     * @param connectionInputPortGrouptName
     */
    public ImageWriterTableNodeConfig(final PortsConfiguration portsConfig,
        final String connectionInputPortGrouptName) {
        m_folderChooserModel = new SettingsModelWriterFileChooser(CFG_OUTPUT_LOCATION, portsConfig,
            connectionInputPortGrouptName, EnumConfig.create(FilterMode.FOLDER),
            EnumConfig.create(FileOverwritePolicy.FAIL, FileOverwritePolicy.OVERWRITE, FileOverwritePolicy.IGNORE),
            EnumSet.of(FSCategory.LOCAL, FSCategory.MOUNTPOINT, FSCategory.RELATIVE));

        m_imgColSelectionModel = new SettingsModelString(CFG_IMG_COLUMN_NAME, null);

        m_removeImgColModel = new SettingsModelBoolean(CFG_REMOVE_IMG_COLUMN, false);

        m_userDefinedOutputFilenameModel = new SettingsModelString(CFG_USER_DEFINED_OUTPUT_FILENAME_NAME, "File_?") {

            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                super.validateSettingsForModel(settings);
                if (!isValidUserDefinedFilename(settings.getString(CFG_USER_DEFINED_OUTPUT_FILENAME_NAME))) {
                    throw new InvalidSettingsException("The file name pattern is not valid.");
                }
            }
        };

        m_filenameColSelectionModel = new SettingsModelColumnName(CFG_OUTPUT_FILENAME_COLUMN_NAME, null);

        m_isGenerateFilenameRadio = true;
        m_userDefinedOutputFilenameModel.setEnabled(m_isGenerateFilenameRadio);
        m_filenameColSelectionModel.setEnabled(!m_isGenerateFilenameRadio);
    }

    SettingsModelWriterFileChooser getFolderChooserModel() {
        return m_folderChooserModel;
    }

    SettingsModelString getImgColSelectionModel() {
        return m_imgColSelectionModel;
    }

    SettingsModelBoolean getRemoveImgColModel() {
        return m_removeImgColModel;
    }

    SettingsModelString getUserDefinedOutputFilenameModel() {
        return m_userDefinedOutputFilenameModel;
    }

    SettingsModelColumnName getFilenameColSelectionModel() {
        return m_filenameColSelectionModel;
    }

    private void setGenerateFilenameRadio(final boolean generateFileNameRadio) {
        m_isGenerateFilenameRadio = generateFileNameRadio;
    }

    boolean isGenerateFilenameRadioSelected() {
        return m_isGenerateFilenameRadio;
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_folderChooserModel.loadSettingsFrom(settings);
        m_imgColSelectionModel.loadSettingsFrom(settings);
        m_removeImgColModel.loadSettingsFrom(settings);

        setGenerateFilenameRadio(settings.getBoolean(CFG_GENERATE_FILE_NAMES));
        m_userDefinedOutputFilenameModel.loadSettingsFrom(settings);
        m_filenameColSelectionModel.loadSettingsFrom(settings);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_folderChooserModel.saveSettingsTo(settings);
        m_imgColSelectionModel.saveSettingsTo(settings);
        m_removeImgColModel.saveSettingsTo(settings);

        saveFileNameRadioSelectionToSettings(settings);
        m_userDefinedOutputFilenameModel.saveSettingsTo(settings);
        m_filenameColSelectionModel.saveSettingsTo(settings);
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_folderChooserModel.validateSettings(settings);
        m_imgColSelectionModel.validateSettings(settings);
        m_removeImgColModel.validateSettings(settings);

        settings.getBoolean(CFG_GENERATE_FILE_NAMES);
        m_userDefinedOutputFilenameModel.validateSettings(settings);
        m_filenameColSelectionModel.validateSettings(settings);
    }

    void saveFileNameRadioSelectionForDialog(final NodeSettingsWO settings, final boolean generateFileNameRadio) {
        setGenerateFilenameRadio(generateFileNameRadio);
        saveFileNameRadioSelectionToSettings(settings);
    }

    void loadFileNameRadioSelectionForDialog(final NodeSettingsRO settings) {
        setGenerateFilenameRadio(settings.getBoolean(CFG_GENERATE_FILE_NAMES, true));
    }

    private void saveFileNameRadioSelectionToSettings(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_GENERATE_FILE_NAMES, isGenerateFilenameRadioSelected());
    }

    /**
     * Returns bool if enclosed string matches the regex for valid file name
     *
     * @param String A string value of the user input field
     * @return true or false value
     */
    private static boolean isValidUserDefinedFilename(final String incomingFilename) {
        final Matcher regexPatternMatcher = CFG_USER_DEFINED_FILENAME_PATTERN.matcher(incomingFilename);
        return regexPatternMatcher.matches();
    }
}
