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
 *   Created on Aug 14, 2021 by Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
package org.knime.filehandling.utility.nodes.tempdir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Custom persistor for PathVariable arrays that need backwards compatibility with FSLocationVariableTableModel.
 * This persistor converts between PathVariable[] and the legacy format using separate arrays for names, values, and extensions.
 *
 * @author GitHub Copilot
 */
final class PathVariableArrayPersistor implements NodeParametersPersistor<CreateTempDir2NodeParameters.PathVariable[]> {

    private static final String CFG_KEY = "additional_path_variables";
    private static final String CFG_VAR_NAMES = "variable_names";
    private static final String CFG_VAR_PATH_VALUE = "path_values";
    private static final String CFG_VAR_PATH_EXTENSION = "file_extensions";

    @Override
    public CreateTempDir2NodeParameters.PathVariable[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (!settings.containsKey(CFG_KEY)) {
            // Check for backwards compatibility with old format
            try {
                return loadBackwardsCompatible(settings);
            } catch (InvalidSettingsException e) {
                return new CreateTempDir2NodeParameters.PathVariable[0]; // Return empty array if no settings found
            }
        }

        final NodeSettingsRO varSettings = settings.getNodeSettings(CFG_KEY);
        final String[] varNames = varSettings.getStringArray(CFG_VAR_NAMES);
        final String[] varValues = varSettings.getStringArray(CFG_VAR_PATH_VALUE);
        final String[] varExtensions = varSettings.getStringArray(CFG_VAR_PATH_EXTENSION);

        if (varNames.length != varValues.length || varNames.length != varExtensions.length) {
            throw new InvalidSettingsException("Inconsistent array lengths for path variables");
        }

        final List<CreateTempDir2NodeParameters.PathVariable> pathVars = new ArrayList<>();
        for (int i = 0; i < varNames.length; i++) {
            if (!varNames[i].trim().isEmpty()) {
                final CreateTempDir2NodeParameters.PathVariable pathVar = new CreateTempDir2NodeParameters.PathVariable();
                pathVar.m_variableName = varNames[i];
                pathVar.m_path = varValues[i];
                pathVar.m_extension = varExtensions[i];
                pathVars.add(pathVar);
            }
        }

        return pathVars.toArray(new CreateTempDir2NodeParameters.PathVariable[0]);
    }

    private CreateTempDir2NodeParameters.PathVariable[] loadBackwardsCompatible(final NodeSettingsRO settings) throws InvalidSettingsException {
        final String[] varNames = settings.getStringArray("additional_variable_names");
        final String[] varValues = settings.getStringArray("additional_variable_values");
        
        final List<CreateTempDir2NodeParameters.PathVariable> pathVars = new ArrayList<>();
        for (int i = 0; i < Math.min(varNames.length, varValues.length); i++) {
            if (!varNames[i].trim().isEmpty()) {
                final CreateTempDir2NodeParameters.PathVariable pathVar = new CreateTempDir2NodeParameters.PathVariable();
                pathVar.m_variableName = varNames[i];
                
                // Split value into path and extension if contains dot
                final String value = varValues[i];
                final int lastDotIndex = value.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    pathVar.m_path = value.substring(0, lastDotIndex);
                    pathVar.m_extension = value.substring(lastDotIndex);
                } else {
                    pathVar.m_path = value;
                    pathVar.m_extension = "";
                }
                pathVars.add(pathVar);
            }
        }

        return pathVars.toArray(new CreateTempDir2NodeParameters.PathVariable[0]);
    }

    @Override
    public void save(final CreateTempDir2NodeParameters.PathVariable[] pathVariables, final NodeSettingsWO settings) {
        final NodeSettingsWO varSettings = settings.addNodeSettings(CFG_KEY);
        
        final List<String> names = new ArrayList<>();
        final List<String> values = new ArrayList<>();
        final List<String> extensions = new ArrayList<>();

        for (final CreateTempDir2NodeParameters.PathVariable pathVar : pathVariables) {
            if (pathVar != null && pathVar.m_variableName != null && !pathVar.m_variableName.trim().isEmpty()) {
                names.add(pathVar.m_variableName);
                values.add(pathVar.m_path != null ? pathVar.m_path : "");
                extensions.add(pathVar.m_extension != null ? pathVar.m_extension : "");
            }
        }

        varSettings.addStringArray(CFG_VAR_NAMES, names.toArray(new String[0]));
        varSettings.addStringArray(CFG_VAR_PATH_VALUE, values.toArray(new String[0]));
        varSettings.addStringArray(CFG_VAR_PATH_EXTENSION, extensions.toArray(new String[0]));
    }

    @Override
    public String[] getConfigPaths() {
        return new String[]{CFG_KEY};
    }
}
