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
 * Settings configuration for "Create Directory" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class CreateDirectory2NodeConfig {

    private static final String DEFAULT_DIR_PATH_VAR_NAME = "dir_path";

    private static final String CFG_DIR_PARENT = "dir_location";

    private static final String CFG_DIR_PATH_VARIABLE_NAME = "dir_path_variable_name";

    private static final String CFG_ADDITIONAL_PATH_VARIABLES = "additional_path_variables";

    private final SettingsModelWriterFileChooser m_parentDirChooserModel;

    private String m_dirPathVariableName;

    private final FSLocationVariableTableModel m_fsLocationTableModel;

    /**
     * Constructor
     *
     * @param portsConfig {@link PortsConfiguration} of the node
     */
    CreateDirectory2NodeConfig(final PortsConfiguration portsConfig) {
        m_parentDirChooserModel = new SettingsModelWriterFileChooser(CFG_DIR_PARENT, portsConfig,
            CreateDirectory2NodeFactory.CONNECTION_INPUT_PORT_GRP_NAME, FilterMode.FOLDER, FileOverwritePolicy.APPEND,
            EnumSet.of(FileOverwritePolicy.APPEND),
            EnumSet.of(FSCategory.LOCAL, FSCategory.MOUNTPOINT, FSCategory.RELATIVE));
        // set the default directory to be the workflow data directory (relative -> knime.workflow.data -> New Folder)
        if (!portsConfig.getInputPortLocation()
            .containsKey(CreateDirectory2NodeFactory.CONNECTION_INPUT_PORT_GRP_NAME)) {
            m_parentDirChooserModel.setLocation(
                new FSLocation(FSCategory.RELATIVE, RelativeTo.WORKFLOW_DATA.getSettingsValue(), "New Folder"));
        }
        m_dirPathVariableName = DEFAULT_DIR_PATH_VAR_NAME;
        m_fsLocationTableModel = new FSLocationVariableTableModel(CFG_ADDITIONAL_PATH_VARIABLES);
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_parentDirChooserModel.validateSettings(settings);

        // ensure backwards compatibility AP-15025
        if (settings.containsKey(CFG_ADDITIONAL_PATH_VARIABLES)) {
            m_fsLocationTableModel.validateSettingsForModel(settings, true);
        }
        validateSettings(settings);
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) {
        setDirVariableName(settings.getString(CFG_DIR_PATH_VARIABLE_NAME, DEFAULT_DIR_PATH_VAR_NAME));
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) {
        save(settings);
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_parentDirChooserModel.loadSettingsFrom(settings);
        m_dirPathVariableName = settings.getString(CFG_DIR_PATH_VARIABLE_NAME);

        // ensure backwards compatibility AP-15025
        if (settings.containsKey(CFG_ADDITIONAL_PATH_VARIABLES)) {
            m_fsLocationTableModel.loadSettingsForModel(settings);
        } else {
            m_fsLocationTableModel.loadBackwardsComptabile(settings, "additional_variable_names",
                "additional_variable_values");
        }
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_parentDirChooserModel.saveSettingsTo(settings);
        save(settings);
        m_fsLocationTableModel.saveSettingsForModel(settings);
    }

    private void save(final NodeSettingsWO settings) {
        settings.addString(CFG_DIR_PATH_VARIABLE_NAME, getDirVariableName());
    }

    private static void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final String dirPathVarName = settings.getString(CFG_DIR_PATH_VARIABLE_NAME);
        CheckUtils.checkArgument(dirPathVarName != null && !dirPathVarName.trim().isEmpty(),
            "The path variable name must not be empty!");
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
     * Returns the {@link FSLocationVariableTableModel}.
     *
     * @return the {@link FSLocationVariableTableModel}
     */
    FSLocationVariableTableModel getFSLocationTableModel() {
        return m_fsLocationTableModel;
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
     * Returns an array of variable names that are exported together with a variable holding the directory to be
     * created. The array should always be of same length as {@link #getAdditionalVarValues()}.
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
