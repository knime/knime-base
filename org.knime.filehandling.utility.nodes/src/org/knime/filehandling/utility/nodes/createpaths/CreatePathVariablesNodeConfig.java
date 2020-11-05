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
 *   27 Jul 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.createpaths;

import java.util.EnumSet;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.utility.nodes.dialog.variables.FSLocationVariableTableModel;

/**
 * The NodeConfig for the "Create File/Folder Variables" node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class CreatePathVariablesNodeConfig {

    // If we change the config key we need to ensure that the DEFAULT_VAR_NAME stays as before
    private static final String CFG_DIR_PARENT = "base_folder";

    private static final String CFG_FILE_FOLDER_VARIABLES = "file_folder_variables";

    private static final String DEFAULT_VAR_NAME = CFG_DIR_PARENT;

    private final FSLocationVariableTableModel m_fsLocationTableModel;

    private SettingsModelCreatorFileChooser m_dirChooserModel;

    CreatePathVariablesNodeConfig(final PortsConfiguration portsConfig) {
        m_dirChooserModel = new SettingsModelCreatorFileChooser(CFG_DIR_PARENT, portsConfig,
            CreatePathVariablesNodeFactory.CONNECTION_INPUT_PORT_GRP_NAME, FilterMode.FOLDER,
            EnumSet.of(FSCategory.LOCAL, FSCategory.MOUNTPOINT, FSCategory.RELATIVE));
        // set the default directory to be the workflow data directory (relative -> knime.workflow.data -> .)
        if (!portsConfig.getInputPortLocation().containsKey(CreatePathVariablesNodeFactory.CONNECTION_INPUT_PORT_GRP_NAME)) {
            m_dirChooserModel
                .setLocation(new FSLocation(FSCategory.RELATIVE, RelativeTo.WORKFLOW_DATA.getSettingsValue(), "."));
        }

        m_fsLocationTableModel = new FSLocationVariableTableModel(CFG_FILE_FOLDER_VARIABLES);
        m_fsLocationTableModel.setEntries(new String[]{DEFAULT_VAR_NAME}, new String[]{""}, new String[]{""});
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dirChooserModel.validateSettings(settings);
        m_fsLocationTableModel.validateSettingsForModel(settings, false);
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dirChooserModel.loadSettingsFrom(settings);
        m_fsLocationTableModel.loadSettingsForModel(settings);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_dirChooserModel.saveSettingsTo(settings);
        m_fsLocationTableModel.saveSettingsForModel(settings);
    }

    /**
     * Returns the {@link SettingsModelReaderFileChooser} used to select a directory where the new file path relates to.
     *
     * @return the {@link SettingsModelReaderFileChooser} used to select a directory
     */
    SettingsModelCreatorFileChooser getDirChooserModel() {
        return m_dirChooserModel;
    }

    /**
     * Returns an array of variable names that are exported. The array should always be of same length as
     * {@link #getAdditionalVarValues()}.
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

    /**
     * Returns the {@link FSLocationVariableTableModel}.
     *
     * @return the {@link FSLocationVariableTableModel}
     */
    FSLocationVariableTableModel getFSLocationTableModel() {
        return m_fsLocationTableModel;
    }
}
