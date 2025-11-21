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
 *   Created on Nov 3, 2025 by Martin Horn, KNIME GmbH, Berlin, Germany
 *   Udpated on Nov 20, 2025 by Thomas Reifenberger, TNG Technology Consulting GmbH, Germany
 */
package org.knime.filehandling.utility.nodes.dialog.variables;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Custom persistor for {@link PathVariable} arrays that need backwards compatibility with
 * {@link FSLocationVariableTableModel}. This persistor converts between PathVariable[] and the legacy format using
 * separate arrays for names, paths, and extensions.
 *
 * @author AI Migration Pipeline v1.2
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH, Germany
 */
public abstract class AbstractPathVariableArrayPersistor implements NodeParametersPersistor<PathVariable[]> {

    private String m_configKey;

    private boolean m_allowBackwardsCompatibility = false;

    /**
     * Constructor.
     *
     * @param configKey Config key of nested configuration entries
     */
    public AbstractPathVariableArrayPersistor(final String configKey) {
        this.m_configKey = configKey;
    }

    /**
     * Constructor.
     *
     * @param configKey Config key of nested configuration entries
     * @param allowBackwardsCompatibility Whether to allow loading from legacy settings `additional_variable_names` and
     *            `additional_variable_values`
     */
    public AbstractPathVariableArrayPersistor(final String configKey, final boolean allowBackwardsCompatibility) {
        this.m_configKey = configKey;
        this.m_allowBackwardsCompatibility = allowBackwardsCompatibility;
    }

    @Override
    public PathVariable[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
        FSLocationVariables variables;
        if (settings.containsKey(m_configKey)) {
            final var varSettings = settings.getNodeSettings(m_configKey);
            variables = FSLocationVariables.load(varSettings);
        } else if (m_allowBackwardsCompatibility) {
            try {
                variables = FSLocationVariables.loadBackwardsCompatible(settings, "additional_variable_names",
                    "additional_variable_values");
            } catch (InvalidSettingsException e) {
                variables = FSLocationVariables.empty();
            }
        } else {
            variables = FSLocationVariables.empty();
        }

        final var pathVars = new PathVariable[variables.count()];
        for (int i = 0; i < pathVars.length; i++) {
            final var pathVar = new PathVariable();
            pathVar.m_variableName = variables.names()[i];
            pathVar.m_path = variables.paths()[i];
            pathVar.m_extension = variables.extensions()[i];
            pathVars[i] = pathVar;
        }
        return pathVars;
    }

    @Override
    public void save(final PathVariable[] pathVariables, final NodeSettingsWO settings) {
        final var names = new String[pathVariables.length];
        final var paths = new String[pathVariables.length];
        final var extensions = new String[pathVariables.length];

        for (int i = 0; i < pathVariables.length; i++) {
            names[i] = pathVariables[i].m_variableName;
            paths[i] = pathVariables[i].m_path;
            extensions[i] = pathVariables[i].m_extension;
        }

        final var varSettings = settings.addNodeSettings(m_configKey);
        new FSLocationVariables(names, paths, extensions).save(varSettings);
    }

    @Override
    public String[][] getConfigPaths() {
        return new String[][]{ //
            {m_configKey, "variable_names"}, //
            {m_configKey, "path_values"}, //
            {m_configKey, "file_extensions"} //
        };
    }
}
