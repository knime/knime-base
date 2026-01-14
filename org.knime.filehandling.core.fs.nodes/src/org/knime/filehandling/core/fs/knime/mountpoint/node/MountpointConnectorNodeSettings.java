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
 *   2021-08-04 (Alexander Bondaletov): created
 */
package org.knime.filehandling.core.fs.knime.mountpoint.node;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.config.MountpointFSConnectionConfig;
import org.knime.filehandling.core.connections.meta.base.TimeoutFSConnectionConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.MountpointSpecificConfig;

/**
 * Settings for the Mountpoint Connector node.
 *
 * @author Alexander Bondaletov
 */
final class MountpointConnectorNodeSettings {

    enum MountpointMode {
        CURRENT("CURRENT", "Current mountpoint"),
        OTHER("OTHER", "Other mountpoint");

        private final String m_settingsValue;
        private final String m_label;

        private MountpointMode(final String settingsValue, final String label) {
            m_settingsValue = settingsValue;
            m_label = label;
        }

        /**
         * @return the settingsValue
         */
        public String getSettingsValue() {
            return m_settingsValue;
        }

        /**
         * @return the label
         */
        public String getLabel() {
            return m_label;
        }
    }

    private static final String KEY_MODE = "mode";

    private static final String KEY_CURRENT_WORKFLOW_AS_WORKING_DIR = "currentWorkflowAsworkingDirectory";

    private static final String KEY_WORKING_DIRECTORY = "workingDirectory";

    private static final String KEY_CONNECTION_TIMEOUT = "connectionTimeout";

    private static final String KEY_READ_TIMEOUT = "readTimeout";

    private final SettingsModelString m_mountpointMode;

    private final SettingsModelBoolean m_currentWorkflowAsWorkingDir;

    private final MountpointSpecificConfig m_mountpoint;

    private final SettingsModelString m_workingDirectory;

    private final SettingsModelIntegerBounded m_connectionTimeout;

    private final SettingsModelIntegerBounded m_readTimeout;

    /**
     * Creates new instance
     */
    public MountpointConnectorNodeSettings() {
        m_mountpointMode = new SettingsModelString(KEY_MODE, MountpointMode.CURRENT.getSettingsValue());
        m_mountpoint = new MountpointSpecificConfig(true);
        m_workingDirectory =
            new SettingsModelString(KEY_WORKING_DIRECTORY, MountpointFSConnectionConfig.PATH_SEPARATOR);
        m_currentWorkflowAsWorkingDir = new SettingsModelBoolean(KEY_CURRENT_WORKFLOW_AS_WORKING_DIR, false);
        m_connectionTimeout = new SettingsModelIntegerBounded(KEY_CONNECTION_TIMEOUT, //
            TimeoutFSConnectionConfig.DEFAULT_TIMEOUT_SECONDS, //
            0, //
            Integer.MAX_VALUE);
        m_readTimeout = new SettingsModelIntegerBounded(KEY_READ_TIMEOUT, //
            TimeoutFSConnectionConfig.DEFAULT_TIMEOUT_SECONDS, //
            0, //
            Integer.MAX_VALUE);
    }

    /**
     * @param mode The new mode to use.
     */
    public void setMountpointMode(final MountpointMode mode) {
        m_mountpointMode.setStringValue(mode.getSettingsValue());
    }

    /**
     * @return the currently configured mountpoint mode
     */
    public MountpointMode getMountpointMode() {
        final var modeString = m_mountpointMode.getStringValue();

        if (MountpointMode.CURRENT.getSettingsValue().equalsIgnoreCase(modeString)) {
            return MountpointMode.CURRENT;
        } else if (MountpointMode.OTHER.getSettingsValue().equalsIgnoreCase(modeString)) {
            return MountpointMode.OTHER;
        } else {
            throw new IllegalStateException("Invalid mode: " + modeString);
        }
    }

    /**
     * @return the mountpoint mode model.
     */
    public SettingsModelString getMountpointModeModel() {
        return m_mountpointMode;
    }

    /**
     * @return the mountpoint config
     */
    public MountpointSpecificConfig getMountpoint() {
        return m_mountpoint;
    }

    /**
     * @return the workingDirectory model
     */
    public SettingsModelString getWorkingDirectoryModel() {
        return m_workingDirectory;
    }

    /**
     * @return the model that describes whether to set the working directory to the current workflow (only valid when
     *         mode = CURRENT).
     */
    public SettingsModelBoolean getWorkflowRelativeWorkingDirModel() {
        return m_currentWorkflowAsWorkingDir;
    }

    /**
     * @return connection time out settings model.
     */
    public SettingsModelIntegerBounded getConnectionTimeoutModel() {
        return m_connectionTimeout;
    }

    /**
     * @return read time out settings model.
     */
    public SettingsModelIntegerBounded getReadTimeoutModel() {
        return m_readTimeout;
    }

    /**
     * @return connection time out.
     */
    public Duration getConnectionTimeout() {
        return Duration.ofSeconds(m_connectionTimeout.getIntValue());
    }

    /**
     * @return socket read time out.
     */
    public Duration getReadTimeout() {
        return Duration.ofSeconds(m_readTimeout.getIntValue());
    }

    /**
     * Saves the settings in this instance to the given {@link NodeSettingsWO}
     *
     * @param settings Node settings.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_mountpointMode.saveSettingsTo(settings);
        m_currentWorkflowAsWorkingDir.saveSettingsTo(settings);
        m_mountpoint.save(settings);
        m_workingDirectory.saveSettingsTo(settings);
        m_connectionTimeout.saveSettingsTo(settings);
        m_readTimeout.saveSettingsTo(settings);
    }

    /**
     * Validates the settings in a given {@link NodeSettingsRO}
     *
     * @param settings Node settings.
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_mountpointMode.validateSettings(settings);
        m_currentWorkflowAsWorkingDir.validateSettings(settings);
        m_mountpoint.validateInModel(settings);
        m_workingDirectory.validateSettings(settings);

        // added with 4.6.1
        if (settings.containsKey(KEY_CONNECTION_TIMEOUT)) {
            m_connectionTimeout.validateSettings(settings);
            m_readTimeout.validateSettings(settings);
        }

        final var temp = new MountpointConnectorNodeSettings();
        temp.loadInModel(settings);
        temp.validate();
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    void validate() throws InvalidSettingsException {
        try {
            getMountpointMode();
        } catch (IllegalStateException e) { // NOSONAR is rethrown as InvalidSettingsException
            throw new InvalidSettingsException(e.getMessage());
        }

        if (getMountpointMode() == MountpointMode.OTHER || !m_currentWorkflowAsWorkingDir.getBooleanValue()) {
            var workDir = m_workingDirectory.getStringValue();
            if (StringUtils.isBlank(workDir) || !workDir.startsWith(MountpointFSConnectionConfig.PATH_SEPARATOR)) {
                throw new InvalidSettingsException("Working directory must be set to an absolute path.");
            }
        }
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO}
     *
     * @param settings Node settings.
     * @throws InvalidSettingsException
     */
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_mountpoint.loadInModel(settings);
        load(settings);
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO}
     *
     * @param settings Node settings.
     * @param specs Port objects specs.
     * @throws NotConfigurableException
     */
    public void loadInDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_mountpoint.loadInDialog(settings, specs);
        try {
            load(settings);
        } catch (InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage(), ex);
        }
    }

    private void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_mountpointMode.loadSettingsFrom(settings);
        m_currentWorkflowAsWorkingDir.loadSettingsFrom(settings);
        m_workingDirectory.loadSettingsFrom(settings);
        // added with 4.6.1
        if (settings.containsKey(KEY_CONNECTION_TIMEOUT)) {
            m_connectionTimeout.loadSettingsFrom(settings);
            m_readTimeout.loadSettingsFrom(settings);
        }
    }
}
