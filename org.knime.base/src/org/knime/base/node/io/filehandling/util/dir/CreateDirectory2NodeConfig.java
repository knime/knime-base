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
 *   July 23, 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.util.dir;

import java.util.EnumSet;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * Settings configuration for "Create Directory" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class CreateDirectory2NodeConfig {

    private static final String DEFAULT_DIR_PATH_VAR_NAME = "dir_path";

    private static final String CFG_DIR_PARENT = "dir_location";

    private static final String CFG_DIR_PATH_VARIABLE_NAME = "dir_path_variable_name";

    private static final String CFG_ADDITIONAL_VARIABLE_NAMES = "additional_variable_names";

    private static final String CFG_ADDITIONAL_VARIABLE_VALUES = "additional_variable_values";

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private SettingsModelWriterFileChooser m_parentDirChooserModel;

    private String m_dirPathVariableName;

    private String[] m_additionalVarNames;

    private String[] m_additionalVarValues;

    /**
     * Constructor
     *
     * @param portsConfig {@link PortsConfiguration} of the node
     */
    CreateDirectory2NodeConfig(final PortsConfiguration portsConfig) {
        m_parentDirChooserModel = new SettingsModelWriterFileChooser(CFG_DIR_PARENT, portsConfig,
            CreateDirectory2NodeFactory.CONNECTION_INPUT_PORT_GRP_NAME, FilterMode.FOLDER, FileOverwritePolicy.APPEND,
            EnumSet.of(FileOverwritePolicy.APPEND), EnumSet.of(FSCategory.LOCAL, FSCategory.MOUNTPOINT, FSCategory.RELATIVE));
        // set the default directory to be the workflow data directory (relative -> knime.workflow.data -> New Folder)
        if (!portsConfig.getInputPortLocation()
            .containsKey(CreateDirectory2NodeFactory.CONNECTION_INPUT_PORT_GRP_NAME)) {
            m_parentDirChooserModel
                .setLocation(new FSLocation(FSCategory.RELATIVE, RelativeTo.WORKFLOW_DATA.getSettingsValue(), "New Folder"));
        }

        m_dirPathVariableName = DEFAULT_DIR_PATH_VAR_NAME;

        m_additionalVarNames = EMPTY_STRING_ARRAY;
        m_additionalVarValues = EMPTY_STRING_ARRAY;
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_parentDirChooserModel.validateSettings(settings);

        settings.getString(CFG_DIR_PATH_VARIABLE_NAME);

        settings.getStringArray(CFG_ADDITIONAL_VARIABLE_NAMES);
        settings.getStringArray(CFG_ADDITIONAL_VARIABLE_VALUES);

        validateSettings();
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_dirPathVariableName = settings.getString(CFG_DIR_PATH_VARIABLE_NAME, DEFAULT_DIR_PATH_VAR_NAME);

        m_additionalVarNames = settings.getStringArray(CFG_ADDITIONAL_VARIABLE_NAMES, EMPTY_STRING_ARRAY);
        m_additionalVarValues = settings.getStringArray(CFG_ADDITIONAL_VARIABLE_VALUES, EMPTY_STRING_ARRAY);
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) {
        save(settings);
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_parentDirChooserModel.loadSettingsFrom(settings);

        m_dirPathVariableName = settings.getString(CFG_DIR_PATH_VARIABLE_NAME);

        m_additionalVarNames = settings.getStringArray(CFG_ADDITIONAL_VARIABLE_NAMES);
        m_additionalVarValues = settings.getStringArray(CFG_ADDITIONAL_VARIABLE_VALUES);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_parentDirChooserModel.saveSettingsTo(settings);
        save(settings);
    }

    private void save(final NodeSettingsWO settings) {
        settings.addString(CFG_DIR_PATH_VARIABLE_NAME, m_dirPathVariableName);

        settings.addStringArray(CFG_ADDITIONAL_VARIABLE_NAMES, m_additionalVarNames);
        settings.addStringArray(CFG_ADDITIONAL_VARIABLE_VALUES, m_additionalVarValues);
    }

    private void validateSettings() {
        CheckUtils.checkArgument((m_dirPathVariableName != null) && (m_dirPathVariableName.trim().length() > 0),
            "The path variable name must not be empty!");
        CheckUtils.checkArgument(m_additionalVarNames.length == m_additionalVarValues.length,
            "The number of names for addtional variables must be equal to that of values!");
    }

    /**
     * Returns the {@link SettingsModelWriterFileChooser} used to select a directory where the created directory lives.
     *
     * @return the {@link SettingsModelWriterFileChooser} used to select a directory
     */
    SettingsModelWriterFileChooser getParentDirChooserModel() {
        return m_parentDirChooserModel;
    }

    /**
     * Sets the {@link SettingsModelWriterFileChooser} used to select a directory where the created directory lives.
     *
     * @param the {@link SettingsModelWriterFileChooser} used to select a directory
     */
    void setParentDirChooserModel(final SettingsModelWriterFileChooser dirChooserModel) {
        m_parentDirChooserModel = dirChooserModel;
    }

    /**
     * Returns the name of the variable used for the directory being created.
     *
     * @return the name of the variable used for the directory being created
     */
    String getDirVariableName() {
        return m_dirPathVariableName;
    }

    /**
     * Sets the name of the variable used for the directory being created.
     *
     * @param variableName the name of the variable used for the directory being created
     */
    void setDirVariableName(final String variableName) {
        m_dirPathVariableName = variableName;
    }

    /**
     * Returns an array of variable names that are exported together with a variable holding the directory to
     * be created. The array should always be of same length as {@link #getAdditionalVarValues()}.
     *
     * @return an array of variable names that are exported
     */
    String[] getAdditionalVarNames() {
        return m_additionalVarNames;
    }

    /**
     * Sets an array of variable names that are exported together with a variable holding the directory to be
     * created. The array should always be of same length as {@link #getAdditionalVarValues()}.
     *
     * @param varNames an array of variable names that are exported
     */
    void setAdditionalVarNames(final String[] varNames) {
        m_additionalVarNames = varNames;
    }

    /**
     * Returns an array of strings used as filenames of files that are going to be created. The array should always be
     * of same length as {@link #getAdditionalVarName()}.
     *
     * @return an array of filenames that are exported as variables values
     */
    String[] getAdditionalVarValues() {
        return m_additionalVarValues;
    }

    /**
     * Sets an array of strings used as filenames of files that are going to be created. The array should always be of
     * same length as {@link #getAdditionalVarName()}.
     *
     * @param varNames an array of filenames that are exported as variables values
     */
    void setAdditionalVarValues(final String[] varNames) {
        m_additionalVarValues = varNames;
    }

}
