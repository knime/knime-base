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
 *   Jun 18, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.util;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Contains utility methods for dealing with {@link NodeSettings}, as well as {@link NodeSettingsRO} and
 * {@link NodeSettingsWO} objects.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 */
public final class SettingsUtils {

    /**
     * Config key for the settings tab.
     *
     * @noreference not intended to be used by clients
     */
    public static final String CFG_SETTINGS_TAB = "settings";

    private SettingsUtils() {
        // static utility class
    }

    /**
     * Retrieves the {@link NodeSettingsRO} with the provided <b>key</b> or creates an empty {@link NodeSettings} object
     * with the <b>key</b>.
     *
     * @param settings the {@link NodeSettingsRO} from which to retrieve the child settings
     * @param key the key of the child settings
     * @return a {@link NodeSettingsRO} with the provided <b>key</b>
     */
    public static NodeSettingsRO getOrEmpty(final NodeSettingsRO settings, final String key) {
        try {
            return settings.getNodeSettings(key);
        } catch (InvalidSettingsException ex) {
            return new NodeSettings(key);
        }
    }

    /**
     * If {@link NodeSettingsWO settingsWO} is an instance of {@link NodeSettings}, the child with the provided
     * <b>key</b> are returned if present or a new child is added. If {@link NodeSettingsWO settingsWO} is not an
     * instance of {@link NodeSettings}, a new child settings object is added and any existing child settings object is
     * overwritten.
     *
     * @param settingsWO to retrieve the child from
     * @param key of the child
     * @return the child {@link NodeSettingsWO}
     * @noreference not intended to be used by clients
     */
    public static NodeSettingsWO getOrAdd(final NodeSettingsWO settingsWO, final String key) {
        if (settingsWO instanceof NodeSettings) {
            final NodeSettings settings = (NodeSettings)settingsWO;
            try {
                return settings.getNodeSettings(key);
            } catch (InvalidSettingsException ex) {
                return settings.addNodeSettings(key);
            }
        } else {
            return settingsWO.addNodeSettings(key);
        }
    }
}
