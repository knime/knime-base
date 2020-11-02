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
 *   June 08, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.util.tempdir;

import java.util.EnumSet;

import org.knime.base.node.io.filehandling.util.dialogs.variables.FSLocationVariableTableModel;
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
 * Settings configuration for "Create Temp Dir" node
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany (re-factored)
 */
final class CreateTempDir2NodeConfig {

    private static final boolean DEFAULT_ON_RESET = true;

    private static final String DEFAULT_TEMP_PATH_VAR_NAME = "temp_dir_path";

    private static final String DEFAULT_TEMP_DIR_PREFIX = "knimetemp-";

    private static final String CFG_TEMP_DIR_PARENT = "temp_dir_location";

    private static final String CFG_TEMP_DIR_PREFIX = "temp_dir_prefix";

    private static final String CFG_TEMP_DIR_PATH_VARIABLE_NAME = "temp_dir_path_variable_name";

    private static final String CFG_DELETE_ON_RESET = "delete_on_reset";

    private static final String CFG_ADDITIONAL_PATH_VARIABLES = "additional_path_variables";

    private final SettingsModelWriterFileChooser m_parentDirChooserModel;

    private String m_tempDirPrefix;

    private String m_tempDirPathVariableName;

    private boolean m_deleteDirOnReset;

    private final FSLocationVariableTableModel m_fsLocationTableModel;

    /**
     * Constructor
     *
     * @param portsConfig {@link PortsConfiguration} of the node
     */
    CreateTempDir2NodeConfig(final PortsConfiguration portsConfig) {
        m_parentDirChooserModel = new SettingsModelWriterFileChooser(CFG_TEMP_DIR_PARENT, portsConfig,
            CreateTempDir2NodeFactory.CONNECTION_INPUT_PORT_GRP_NAME, FilterMode.FOLDER, FileOverwritePolicy.APPEND,
            EnumSet.of(FileOverwritePolicy.APPEND),
            EnumSet.of(FSCategory.LOCAL, FSCategory.MOUNTPOINT, FSCategory.RELATIVE));
        // set the default directory to be the workflow data directory (relative -> knime.workflow.data -> .)
        if (!portsConfig.getInputPortLocation().containsKey(CreateTempDir2NodeFactory.CONNECTION_INPUT_PORT_GRP_NAME)) {
            m_parentDirChooserModel
                .setLocation(new FSLocation(FSCategory.RELATIVE, RelativeTo.WORKFLOW_DATA.getSettingsValue(), "."));
        }

        m_deleteDirOnReset = DEFAULT_ON_RESET;

        m_tempDirPrefix = DEFAULT_TEMP_DIR_PREFIX;
        m_tempDirPathVariableName = DEFAULT_TEMP_PATH_VAR_NAME;

        m_fsLocationTableModel = new FSLocationVariableTableModel(CFG_ADDITIONAL_PATH_VARIABLES);
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_parentDirChooserModel.validateSettings(settings);
        settings.getBoolean(CFG_DELETE_ON_RESET);
        m_fsLocationTableModel.validateSettingsForModel(settings, true);
        validateSettings(settings);
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_deleteDirOnReset = settings.getBoolean(CFG_DELETE_ON_RESET, DEFAULT_ON_RESET);

        setTempDirPrefix(settings.getString(CFG_TEMP_DIR_PREFIX, DEFAULT_TEMP_DIR_PREFIX));
        setTempDirVariableName(settings.getString(CFG_TEMP_DIR_PATH_VARIABLE_NAME, DEFAULT_TEMP_PATH_VAR_NAME));
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) {
        save(settings);
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_parentDirChooserModel.loadSettingsFrom(settings);

        m_deleteDirOnReset = settings.getBoolean(CFG_DELETE_ON_RESET);

        m_tempDirPrefix = settings.getString(CFG_TEMP_DIR_PREFIX);
        m_tempDirPathVariableName = settings.getString(CFG_TEMP_DIR_PATH_VARIABLE_NAME);

        m_fsLocationTableModel.loadSettingsForModel(settings);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_parentDirChooserModel.saveSettingsTo(settings);
        save(settings);
        m_fsLocationTableModel.saveSettingsForModel(settings);
    }

    private void save(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_DELETE_ON_RESET, m_deleteDirOnReset);

        settings.addString(CFG_TEMP_DIR_PREFIX, m_tempDirPrefix);
        settings.addString(CFG_TEMP_DIR_PATH_VARIABLE_NAME, m_tempDirPathVariableName);
    }

    private static void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final String tmpDirPathVarName = settings.getString(CFG_TEMP_DIR_PATH_VARIABLE_NAME);
        CheckUtils.checkArgument((tmpDirPathVarName != null && !tmpDirPathVarName.trim().isEmpty()),
            "The path variable name must not be empty!");

        final String tmpDirPrefix = settings.getString(CFG_TEMP_DIR_PREFIX);
        CheckUtils.checkArgument((tmpDirPrefix != null && !tmpDirPrefix.trim().isEmpty()),
            "The prefix for temporary directory to be created must not be empty!");
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
     * Whether or not the created temporary directory has to be deleted upon resetting a node.
     *
     * @return {@code true} if created directory is deleted when node get reset.
     */
    boolean deleteDirOnReset() {
        return m_deleteDirOnReset;
    }

    /**
     * Sets or not the created temporary directory has to be deleted upon resetting a node.
     *
     * @param deleteOnReset the boolean flag to set
     */
    void setDeleteDirOnReset(final boolean deleteOnReset) {
        m_deleteDirOnReset = deleteOnReset;
    }

    /**
     * Returns the prefix of the temporary directory to be created.
     *
     * @return the prefix of the temporary directory to be created
     */
    String getTempDirPrefix() {
        return m_tempDirPrefix;
    }

    /**
     * Sets the prefix of the temporary directory to be created.
     *
     * @param prefix the constant prefix of the temporary directory to be created
     */
    void setTempDirPrefix(final String prefix) {
        m_tempDirPrefix = prefix;
    }

    /**
     * Returns the name of the variable used for the temporary directory being created.
     *
     * @return the name of the variable used for the temporary directory being created
     */
    String getTempDirVariableName() {
        return m_tempDirPathVariableName;
    }

    /**
     * Sets the name of the variable used for the temporary directory being created.
     *
     * @param variableName the name of the variable used for the temporary directory being created
     */
    void setTempDirVariableName(final String variableName) {
        m_tempDirPathVariableName = variableName;
    }

    /**
     * Returns the {@link FSLocationVariableTableModel}.
     *
     * @return the {@link FSLocationVariableTableModel}
     */
    FSLocationVariableTableModel getFSLocationTableModel() {
        return m_fsLocationTableModel;
    }

    /**
     * Returns an array of variable names that are exported together with a variable holding the temporary directory to
     * be created. The array should always be of same length as {@link #getAdditionalVarValues()}.
     *
     * @return an array of variable names that are exported
     */
    String[] getAdditionalVarNames() {
        return getFSLocationTableModel().getVarNames();
    }

    /**
     * Returns an array of strings used as filenames of files that are going to be created. The array should always be
     * of same length as {@link #getAdditionalVarName()}.
     *
     * @return an array of filenames that are exported as variables values
     */
    String[] getAdditionalVarValues() {
        return getFSLocationTableModel().getLocations();
    }

}
