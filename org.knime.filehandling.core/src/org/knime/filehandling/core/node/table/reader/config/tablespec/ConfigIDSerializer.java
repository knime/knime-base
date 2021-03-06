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
 *   Feb 9, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Helper class for serializing {@link ConfigID ConfigIDs}.<br>
 * It provides backwards compatible loading and saving.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ConfigIDSerializer {

    private static final String CFG_CONFIG_ID = "config_id";

    private final ConfigIDLoader m_configIDLoader;

    ConfigIDSerializer(final ConfigIDLoader configIDLoader) {
        m_configIDLoader = configIDLoader;
    }

    static void saveID(final ConfigID id, final NodeSettingsWO topLevelSettings) {
        if (id == EmptyConfigID.INSTANCE) {
            // the TableSpecConfig being serialized was loaded from an old workflow and therefore has no ConfigID
            // this is done for backwards compatibility
        } else {
            id.save(topLevelSettings.addNodeSettings(CFG_CONFIG_ID));
        }
    }

    ConfigID loadID(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(CFG_CONFIG_ID)) {
            return m_configIDLoader.createFromSettings(settings.getNodeSettings(CFG_CONFIG_ID));
        } else {
            // the node was last saved before 4.4 -> no config id is available therefore we return the empty config id
            return EmptyConfigID.INSTANCE;
        }
    }

    /**
     * Special configID for loading old (pre 4.4) settings.<br>
     * Package private for testing purposes, DON'T USE this class FOR ANYTHING ELSE.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    enum EmptyConfigID implements ConfigID {

            INSTANCE;

        @Override
        public void save(final NodeSettingsWO settings) {
            throw new IllegalStateException(
                "EmptyConfigID only exist for backwards compatibility and should be handled differently.");
        }

        @Override
        public boolean isCompatible(final ConfigID configID) {
            // Before 4.4.0, there was no ConfigID, and this ConfigID is loaded instead
            // In order to avoid warnings on all nodes created pre 4.4.0, this ConfigID needs to be compatible
            // with any other ConfigID.
            return configID != null;
        }

    }
}
