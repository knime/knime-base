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
 *   Oct 13, 2020 (Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.binaryobjects.writer;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * A centralized class for encapsulating the SettingsModel Objects. The save, validate and load in NodeModel simply
 * invokes this functionality
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
final class BinaryObjectsToFilesNodeConfig {

    private static final String CFG_BINARY_COLUMN_NAME = "binary_object_column";

    private static final String CFG_WRITER_FILE_CHOOSER_NAME = "output_location";

    private static final String CFG_REMOVE_BINARY_COLUMN_NAME = "remove_binary_object_column";

    private static final String CFG_GENERATE_FILE_NAMES = "generate_file_names";

    private static final String CFG_OUTPUT_FILENAME_COLUMN_NAME = "filename_column";

    private static final String CFG_USER_DEFINED_OUTPUT_FILENAME_NAME = "filename_pattern";

    private final SettingsModelString m_binaryObjectsSelectionColumnModel;

    private final SettingsModelWriterFileChooser m_fileWriterSelectionModel;

    private final SettingsModelBoolean m_removeBinaryObjColumnModel;

    private boolean m_generateFileNames;

    private final SettingsModelString m_outputFilenameColumnModel;

    private final SettingsModelString m_userDefinedOutputFilename;

    // Allows one instance of ? and a file extension of at least 1 character, no spaces are allowed in filenames
    private static final Pattern userDefinedFilenamePattern = Pattern.compile("^[\\w,-]*\\?{1}[\\w,-]*\\.[A-Za-z]+$");

    /**
     * Constructor for the Configuration class
     *
     * @param portsConfiguration An object of PortsConfiguration Class
     */
    BinaryObjectsToFilesNodeConfig(final PortsConfiguration portsConfiguration) {
        m_binaryObjectsSelectionColumnModel = new SettingsModelString(CFG_BINARY_COLUMN_NAME, null);
        m_removeBinaryObjColumnModel = new SettingsModelBoolean(CFG_REMOVE_BINARY_COLUMN_NAME, false);
        m_fileWriterSelectionModel = new SettingsModelWriterFileChooser(CFG_WRITER_FILE_CHOOSER_NAME,
            portsConfiguration, BinaryObjectsToFilesNodeFactory.CONNECTION_INPUT_PORT_GRP_NAME, FilterMode.FOLDER,
            FileOverwritePolicy.IGNORE,
            EnumSet.of(FileOverwritePolicy.FAIL, FileOverwritePolicy.OVERWRITE, FileOverwritePolicy.IGNORE),
            EnumSet.of(FSCategory.LOCAL, FSCategory.MOUNTPOINT, FSCategory.RELATIVE));
        m_generateFileNames = true;
        m_outputFilenameColumnModel = new SettingsModelString(CFG_OUTPUT_FILENAME_COLUMN_NAME, null);
        m_outputFilenameColumnModel.setEnabled(!m_generateFileNames);
        m_userDefinedOutputFilename = new SettingsModelString(CFG_USER_DEFINED_OUTPUT_FILENAME_NAME, "File_?.dat") {

            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                super.validateSettingsForModel(settings);
                //check if user input matches the regex for a valid filename, otherwise throw InvalidSettingsException
                if (!BinaryObjectsToFilesNodeConfig
                    .isValidUserDefinedFilename(settings.getString(CFG_USER_DEFINED_OUTPUT_FILENAME_NAME))) {
                    throw new InvalidSettingsException("The file name pattern field is not valid");
                }
            }
        };
        m_userDefinedOutputFilename.setEnabled(m_generateFileNames);
    }

    SettingsModelString getBinaryObjectsSelectionColumnModel() {
        return m_binaryObjectsSelectionColumnModel;
    }

    SettingsModelWriterFileChooser getFileSettingsModelWriterFileChooser() {
        return m_fileWriterSelectionModel;
    }

    SettingsModelBoolean getRemoveBinaryObjColumnModel() {
        return m_removeBinaryObjColumnModel;
    }

    SettingsModelString getOutputFilenameColumnModel() {
        return m_outputFilenameColumnModel;
    }

    SettingsModelString getUserDefinedOutputFilename() {
        return m_userDefinedOutputFilename;
    }

    String getStringValSelectedBinaryObjectColumnModel() {
        return m_binaryObjectsSelectionColumnModel.getStringValue();
    }

    void setStringValSelectedBinaryObjectColumnModel(final String inputStr) {
        m_binaryObjectsSelectionColumnModel.setStringValue(inputStr);
    }

    String getStringValOutputFilenameColumnModel() {
        return m_outputFilenameColumnModel.getStringValue();
    }

    void setStringValOutputFilenameColumnModel(final String inputStr) {
        m_outputFilenameColumnModel.setStringValue(inputStr);
    }

    String getStringValUserDefinedOutputFilenameModel() {
        return m_userDefinedOutputFilename.getStringValue();
    }

    boolean isRemoveBinaryColumnModelEnabled() {
        return m_removeBinaryObjColumnModel.getBooleanValue();
    }

    private void generateFileNames(final boolean generateFileNames) {
        m_generateFileNames = generateFileNames;
    }

    boolean generateFileNames() {
        return m_generateFileNames;
    }

    /**
     * Implements save settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsWO object from NodeModel
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        m_binaryObjectsSelectionColumnModel.saveSettingsTo(settings);
        m_removeBinaryObjColumnModel.saveSettingsTo(settings);
        m_fileWriterSelectionModel.saveSettingsTo(settings);
        saveGenerateFileNames(settings);
        m_outputFilenameColumnModel.saveSettingsTo(settings);
        m_userDefinedOutputFilename.saveSettingsTo(settings);
    }

    /**
     * Implements validate settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsRO object from NodeModel
     */
    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_binaryObjectsSelectionColumnModel.validateSettings(settings);
        m_removeBinaryObjColumnModel.validateSettings(settings);
        m_fileWriterSelectionModel.validateSettings(settings);
        settings.getBoolean(CFG_GENERATE_FILE_NAMES);
        m_outputFilenameColumnModel.validateSettings(settings);
        m_userDefinedOutputFilename.validateSettings(settings);
    }

    /**
     * Implements load validated settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsRO object from NodeModel
     */
    void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_binaryObjectsSelectionColumnModel.loadSettingsFrom(settings);
        m_removeBinaryObjColumnModel.loadSettingsFrom(settings);
        m_fileWriterSelectionModel.loadSettingsFrom(settings);
        generateFileNames(settings.getBoolean(CFG_GENERATE_FILE_NAMES));
        m_outputFilenameColumnModel.loadSettingsFrom(settings);
        m_userDefinedOutputFilename.loadSettingsFrom(settings);
    }

    void loadGenerateFileNamesForDialog(final NodeSettingsRO settings) {
        generateFileNames(settings.getBoolean(CFG_GENERATE_FILE_NAMES, true));
    }

    void saveGenerateFileNamesForDialog(final NodeSettingsWO settings, final boolean generateFileNames) {
        generateFileNames(generateFileNames);
        saveGenerateFileNames(settings);
    }

    private void saveGenerateFileNames(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_GENERATE_FILE_NAMES, generateFileNames());
    }

    /**
     * Returns bool if enclosed string matches the regex for valid filename
     *
     * @param String A string value of the user input field
     * @return true or false value
     */
    static boolean isValidUserDefinedFilename(final String incomingFilename) {
        Matcher regexPatternMatcher = userDefinedFilenamePattern.matcher(incomingFilename);
        return regexPatternMatcher.matches();
    }

}
