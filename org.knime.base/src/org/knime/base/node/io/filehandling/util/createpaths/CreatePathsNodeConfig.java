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
package org.knime.base.node.io.filehandling.util.createpaths;

import java.util.EnumSet;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * The NodeConfig for the "Create Paths" node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class CreatePathsNodeConfig {

    private static final String CFG_DIR_PARENT = "base_folder";

    private static final String CFG_ADDITIONAL_VARIABLE_NAMES = "additional_variable_names";

    private static final String CFG_ADDITIONAL_VARIABLE_VALUES = "additional_variable_values";

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private SettingsModelCreatorFileChooser m_dirChooserModel;

    private String[] m_additionalVarNames;

    private String[] m_additionalVarValues;

    CreatePathsNodeConfig(final PortsConfiguration portsConfig) {
        m_dirChooserModel = new SettingsModelCreatorFileChooser(CFG_DIR_PARENT, portsConfig,
            CreatePathsNodeFactory.CONNECTION_INPUT_PORT_GRP_NAME, FilterMode.FOLDER,
            EnumSet.of(FSCategory.LOCAL, FSCategory.MOUNTPOINT, FSCategory.RELATIVE));
        // set the default directory to be the workflow data directory (relative -> knime.workflow.data -> .)
        if (!portsConfig.getInputPortLocation().containsKey(CreatePathsNodeFactory.CONNECTION_INPUT_PORT_GRP_NAME)) {
            m_dirChooserModel
                .setLocation(new FSLocation(FSCategory.RELATIVE, RelativeTo.WORKFLOW_DATA.getSettingsValue(), "."));
        }

        m_additionalVarNames = EMPTY_STRING_ARRAY;
        m_additionalVarValues = EMPTY_STRING_ARRAY;
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dirChooserModel.validateSettings(settings);

        settings.getStringArray(CFG_ADDITIONAL_VARIABLE_NAMES);
        settings.getStringArray(CFG_ADDITIONAL_VARIABLE_VALUES);

        validateSettings();
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_additionalVarNames = settings.getStringArray(CFG_ADDITIONAL_VARIABLE_NAMES, EMPTY_STRING_ARRAY);
        m_additionalVarValues = settings.getStringArray(CFG_ADDITIONAL_VARIABLE_VALUES, EMPTY_STRING_ARRAY);
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) {
        save(settings);
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dirChooserModel.loadSettingsFrom(settings);

        m_additionalVarNames = settings.getStringArray(CFG_ADDITIONAL_VARIABLE_NAMES);
        m_additionalVarValues = settings.getStringArray(CFG_ADDITIONAL_VARIABLE_VALUES);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_dirChooserModel.saveSettingsTo(settings);
        save(settings);
    }

    private void save(final NodeSettingsWO settings) {
        settings.addStringArray(CFG_ADDITIONAL_VARIABLE_NAMES, m_additionalVarNames);
        settings.addStringArray(CFG_ADDITIONAL_VARIABLE_VALUES, m_additionalVarValues);
    }

    private void validateSettings() {
        CheckUtils.checkArgument(m_additionalVarNames.length == m_additionalVarValues.length,
            "The number of names for addtional variables must be equal to that of values!");
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
        return m_additionalVarNames;
    }

    /**
     * Sets an array of variable names that are exported containing the file paths. The array should always be of same
     * length as {@link #getAdditionalVarValues()}.
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
