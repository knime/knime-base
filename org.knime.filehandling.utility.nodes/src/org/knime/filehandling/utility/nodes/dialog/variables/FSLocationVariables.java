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
 *   Nov 17, 2025 (hornm): created
 */
package org.knime.filehandling.utility.nodes.dialog.variables;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSLocation;

/**
 * Flow variables created based on {@link FSLocation}s. Mostly provides a re-usable way to save and load them.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @param names variable names
 * @param paths variable paths
 * @param extensions file extensions
 */
public record FSLocationVariables(String[] names, String[] paths, String[] extensions) {

    private static final String CFG_VAR_NAMES = "variable_names";

    private static final String CFG_VAR_PATH_VALUE = "path_values";

    private static final String CFG_VAR_PATH_EXTENSION = "file_extensions";

    private static final FSLocationVariables EMPTY =
        new FSLocationVariables(new String[0], new String[0], new String[0]);

    /**
     * Loads the variables from the settings.
     *
     * @param settings the settings to load from
     * @return a new instance
     * @throws InvalidSettingsException if the variables could not be loaded
     */
    public static FSLocationVariables load(final NodeSettingsRO settings) throws InvalidSettingsException {
        return new FSLocationVariables(settings.getStringArray(CFG_VAR_NAMES),
            settings.getStringArray(CFG_VAR_PATH_VALUE), settings.getStringArray(CFG_VAR_PATH_EXTENSION));
    }

    /**
     * Loads the variables from the settings in a backwards compatible way (AP-15025).
     *
     * @param settings
     * @param namesCfgKey the key to load the names from
     * @param pathsCfgKey the key to load the paths from
     * @return a new instance
     * @throws InvalidSettingsException if the variables could not be loaded
     */
    public static FSLocationVariables loadBackwardsCompatible(final NodeSettingsRO settings, final String namesCfgKey,
        final String pathsCfgKey) throws InvalidSettingsException {
        final String[] names = settings.getStringArray(namesCfgKey);
        final String[] paths = settings.getStringArray(pathsCfgKey);
        final String[] extensions = new String[paths.length];
        splitPathsAndExtension(paths, extensions);
        return new FSLocationVariables(names, paths, extensions);
    }

    private static void splitPathsAndExtension(final String[] paths, final String[] extensions) {
        for (int i = 0; i < paths.length; i++) {
            final int delIdx = paths[i].lastIndexOf('.');
            final String path;
            final String extension;
            if (delIdx > 0) {
                path = paths[i].substring(0, delIdx);
                extension = paths[i].substring(delIdx, paths[i].length());
            } else {
                path = paths[i];
                extension = "";
            }
            paths[i] = path;
            extensions[i] = extension;
        }
    }

    /**
     * @return an instance representing an empty list of variables
     */
    public static FSLocationVariables empty() {
        return EMPTY;
    }

    /**
     * Validates the settings.
     *
     * @param emptyAllowed if {@code true} an exception is thrown if no variables have been specified
     * @throws InvalidSettingsException - If the settings are invalid
     */
    void validate(final boolean emptyAllowed) throws InvalidSettingsException {
        for (final String varName : names) {
            CheckUtils.checkSetting(!varName.isEmpty(), "Please assign names to each flow variable to be created");
        }
        if (!emptyAllowed) {
            CheckUtils.checkSetting(names.length > 0, "Please specify at least one variable to be created");
        }
        CheckUtils.checkSetting(names.length * 2 == paths.length + extensions.length,
            "The number of variable names, body and extensions must be equal");
    }

    /**
     * Saves the variables to the settings.
     *
     * @param settings the settings to save to
     */
    public void save(final NodeSettingsWO settings) {
        settings.addStringArray(CFG_VAR_NAMES, names);
        settings.addStringArray(CFG_VAR_PATH_VALUE, paths);
        settings.addStringArray(CFG_VAR_PATH_EXTENSION, extensions);
    }

    /**
     * @return the number of variables
     */
    public int count() {
        return names.length;
    }

    FSLocationVariables add(final String name, final String path, final String extension) {
        return new FSLocationVariables(ArrayUtils.add(names, name), ArrayUtils.add(paths, path),
            ArrayUtils.add(extensions, extension));
    }

    FSLocationVariables remove(final int... toRemove) {
        return new FSLocationVariables(ArrayUtils.removeAll(names, toRemove), ArrayUtils.removeAll(paths, toRemove),
            ArrayUtils.removeAll(extensions, toRemove));
    }

}
