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
 *   14.01.2021 (lars.schweikardt): created
 */
package org.knime.filehandling.utility.nodes.deletepaths;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Abstract super class for the configuration of the "Delete Files/Folders" nodes.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractDeleteFilesAndFoldersNodeConfig {

    /** The source file system connection port group name. */
    public static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    /** The destination file system connection port group name. */
    public static final String OUTPUT_PORT_GRP_NAME = "Output Port";

    private static final String CFG_FAIL_IF_DELETE_FAILS = "fail_if_delete_fails";

    private final SettingsModelFailIfDeleteFails m_failIfDeleteFails =
        new SettingsModelFailIfDeleteFails(CFG_FAIL_IF_DELETE_FAILS, true);

    /**
     * Returns the {@link SettingsModelBoolean} for the "Fail if delete fails" option.
     *
     * @return the {@link SettingsModelBoolean}
     */
    protected final SettingsModelBoolean failIfDeleteFails() {
        return m_failIfDeleteFails;
    }

    /**
     * Returns a boolean for the "Fail if file does not exist" option.
     *
     * @return a boolean whether this option is active or not
     */
    protected abstract boolean failIfFileDoesNotExist();

    /**
     * Loads the settings for the model.
     *
     * @param settings the {@link NodeSettingsRO}
     * @throws InvalidSettingsException
     */
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_failIfDeleteFails.loadSettingsFrom(settings);
    }

    /**
     * Saves the settings for the model.
     *
     * @param settings the {@link NodeSettingsWO}
     */
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        m_failIfDeleteFails.saveSettingsTo(settings);
    }

    /**
     * Validates the settings for the model.
     *
     * @param settings the {@link NodeSettingsRO}
     * @throws InvalidSettingsException
     */
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_failIfDeleteFails.validateSettings(settings);
    }

    /**
     * Ensures backwards compatibility as the old setting was renamed with AP-16391
     *
     * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
     */
    private static class SettingsModelFailIfDeleteFails extends SettingsModelBoolean {

        private static final String OLD_CONFIG_KEY = "abort_if_delete_fails";

        /**
         * Constructor.
         *
         * @param configName
         * @param defaultValue
         */
        public SettingsModelFailIfDeleteFails(final String configName, final boolean defaultValue) {
            super(configName, defaultValue);
        }

        @Override
        protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs) {
            if (settings.containsKey(getConfigName())) {
                setBooleanValue(settings.getBoolean(getConfigName(), true));
            } else {
                setBooleanValue(settings.getBoolean(OLD_CONFIG_KEY, true));
            }
        }

        @Override
        protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(getConfigName())) {
                setBooleanValue(settings.getBoolean(getConfigName()));
            } else {
                setBooleanValue(settings.getBoolean(OLD_CONFIG_KEY));
            }
        }

        @Override
        protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String key = getConfigName();
            if (settings.containsKey(key)) {
                super.validateSettingsForModel(settings);
            }
        }
    }
}
