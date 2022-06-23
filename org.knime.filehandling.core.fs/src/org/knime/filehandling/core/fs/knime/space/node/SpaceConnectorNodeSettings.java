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
 *   Apr 17, 2022 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.core.fs.knime.space.node;

import java.time.Duration;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.config.MountpointFSConnectionConfig;
import org.knime.filehandling.core.connections.config.SpaceFSConnectionConfig;
import org.knime.filehandling.core.connections.meta.base.TimeoutFSConnectionConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.MountpointSpecificConfig;
import org.knime.filehandling.core.util.MountPointFileSystemAccessService;

/**
 * Settings for the Space Connector node.
 *
 * @author Zkriya Rakhimberdiyev
 */
public class SpaceConnectorNodeSettings {

    private static final Set<String> HUB_PROVIDER_FACTORY_IDS = Set.of(
        "com.knime.explorer.server.knime_hub", "com.knime.explorer.server.workflow_hub");

    enum SpaceMode {
        CURRENT("CURRENT", "Current space"),
        OTHER("OTHER", "Other space");

        private final String m_settingsValue;
        private final String m_label;

        private SpaceMode(final String settingsValue, final String label) {
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

    private static final String KEY_WORKING_DIRECTORY = "workingDirectory";

    private static final String KEY_SPACE_ID = "spaceId";

    private static final String KEY_SPACE_NAME = "spaceName";

    private static final String KEY_CONNECTION_TIMEOUT = "connectionTimeout";

    private static final String KEY_READ_TIMEOUT = "readTimeout";

    private final SettingsModelString m_spaceMode;

    private final MountpointSpecificConfig m_mountpoint;

    private final SettingsModelString m_workingDirectory;

    private final SettingsModelString m_spaceId;

    private final SettingsModelString m_spaceName;

    private final SettingsModelIntegerBounded m_connectionTimeout;

    private final SettingsModelIntegerBounded m_readTimeout;

    /**
     * Creates new instance
     */
    public SpaceConnectorNodeSettings() {
        m_spaceMode = new SettingsModelString(KEY_MODE, SpaceMode.OTHER.getSettingsValue());
        m_mountpoint = new MountpointSpecificConfig(true,
            () -> MountPointFileSystemAccessService.instance().getAllMountedIDs(HUB_PROVIDER_FACTORY_IDS));
        m_workingDirectory =
                new SettingsModelString(KEY_WORKING_DIRECTORY, MountpointFSConnectionConfig.PATH_SEPARATOR);
        m_spaceId = new SettingsModelString(KEY_SPACE_ID, "");
        m_spaceName = new SettingsModelString(KEY_SPACE_NAME, "");
        m_connectionTimeout = new SettingsModelIntegerBounded(KEY_CONNECTION_TIMEOUT, //
            TimeoutFSConnectionConfig.DEFAULT_TIMEOUT_SECONDS,
            0,
            Integer.MAX_VALUE);
        m_readTimeout = new SettingsModelIntegerBounded(KEY_READ_TIMEOUT, //
            TimeoutFSConnectionConfig.DEFAULT_TIMEOUT_SECONDS, //
            0, //
            Integer.MAX_VALUE);
    }

    /**
     * @param mode The new mode to use.
     */
    public void setSpaceMode(final SpaceMode mode) {
        m_spaceMode.setStringValue(mode.getSettingsValue());
    }

    /**
     * @return the currently configured space mode
     */
    public SpaceMode getSpaceMode() {
        final var modeString = m_spaceMode.getStringValue();

        if (SpaceMode.CURRENT.getSettingsValue().equalsIgnoreCase(modeString)) {
            return SpaceMode.CURRENT;
        } else if (SpaceMode.OTHER.getSettingsValue().equalsIgnoreCase(modeString)) {
            return SpaceMode.OTHER;
        } else {
            throw new IllegalStateException("Invalid mode: " + modeString);
        }
    }

    /**
     * @return the space mode model.
     */
    public SettingsModelString getSpaceModeModel() {
        return m_spaceMode;
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
     * @return the working directory
     */
    public String getWorkingDirectory() {
        return m_workingDirectory.getStringValue();
    }

    /**
     * @return the space id model
     */
    public SettingsModelString getSpaceIdModel() {
        return m_spaceId;
    }

    /**
     * @return the space name model
     */
    public SettingsModelString getSpaceNameModel() {
        return m_spaceName;
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
        m_spaceMode.saveSettingsTo(settings);
        m_mountpoint.save(settings);
        m_workingDirectory.saveSettingsTo(settings);
        m_spaceId.saveSettingsTo(settings);
        m_spaceName.saveSettingsTo(settings);
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
        m_spaceMode.validateSettings(settings);
        m_mountpoint.validateInModel(settings);
        m_workingDirectory.validateSettings(settings);
        m_spaceId.validateSettings(settings);
        m_spaceName.validateSettings(settings);
        m_connectionTimeout.validateSettings(settings);
        m_readTimeout.validateSettings(settings);
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
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    void validate() throws InvalidSettingsException {

        final SpaceMode spaceMode;
        try {
            spaceMode = getSpaceMode();
        } catch (IllegalStateException e) { // NOSONAR is rethrown as InvalidSettingsException
            throw new InvalidSettingsException(e.getMessage());
        }

        if (spaceMode == SpaceMode.CURRENT) {
            throw new InvalidSettingsException("Connecting to CURRENT is not implemented yet");
        }

        if (spaceMode == SpaceMode.OTHER && StringUtils.isBlank(m_spaceId.getStringValue())) {
            throw new InvalidSettingsException("Please select a space.");
        }

        var workDir = m_workingDirectory.getStringValue();
        if (StringUtils.isBlank(workDir) || !workDir.startsWith(MountpointFSConnectionConfig.PATH_SEPARATOR)) {
            throw new InvalidSettingsException("Working directory must be set to an absolute path.");
        }
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
        m_spaceMode.loadSettingsFrom(settings);
        m_workingDirectory.loadSettingsFrom(settings);
        m_spaceId.loadSettingsFrom(settings);
        m_connectionTimeout.loadSettingsFrom(settings);
        m_readTimeout.loadSettingsFrom(settings);
    }

    SpaceFSConnectionConfig createSpaceFSConnectionConfig() {
        return new SpaceFSConnectionConfig( //
            m_workingDirectory.getStringValue(), //
            m_mountpoint.getMountpoint().getId(),
            m_spaceId.getStringValue(),
            getConnectionTimeout(), //
            getReadTimeout());
    }
}
