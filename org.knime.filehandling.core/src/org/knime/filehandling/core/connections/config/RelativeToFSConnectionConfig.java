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
 *   Jun 3, 2021 (bjoern): created
 */
package org.knime.filehandling.core.connections.config;

import java.time.Duration;
import java.util.Optional;

import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.meta.FSConnectionConfig;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.connections.meta.base.TimeoutFSConnectionConfig;

/**
 * {@link FSConnectionConfig} for the local Relative-to file systems. It is unlikely that you will have to use this
 * class directly. To create a configured Relative-to file system, please use {@link DefaultFSConnectionFactory}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 */
public class RelativeToFSConnectionConfig extends TimeoutFSConnectionConfig {

    /**
     * {@link FSLocationSpec} for the convenience relative-to workflow file system.
     */
    public static final FSLocationSpec CONVENIENCE_WORKFLOW_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.RELATIVE, RelativeTo.WORKFLOW.getSettingsValue());

    /**
     * {@link FSLocationSpec} for the convenience relative-to workflow data area file system.
     */
    public static final FSLocationSpec CONVENIENCE_WORKFLOW_DATA_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.RELATIVE, RelativeTo.WORKFLOW_DATA.getSettingsValue());

    /**
     * {@link FSLocationSpec} for the convenience relative-to mountpoint file system.
     */
    public static final FSLocationSpec CONVENIENCE_MOUNTPOINT_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.RELATIVE, RelativeTo.MOUNTPOINT.getSettingsValue());

    /**
     * {@link FSLocationSpec} for the convenience relative-to Hub Space file system.
     */
    public static final FSLocationSpec CONVENIENCE_SPACE_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.RELATIVE, RelativeTo.SPACE.getSettingsValue());

    /**
     * {@link FSLocationSpec} for the connected relative-to workflow file system.
     */
    public static final FSLocationSpec CONNECTED_WORKFLOW_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.CONNECTED, FSType.RELATIVE_TO_WORKFLOW.getTypeId());

    /**
     * {@link FSLocationSpec} for the connected relative-to mountpoint file system.
     */
    public static final FSLocationSpec CONNECTED_MOUNTPOINT_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.CONNECTED, FSType.RELATIVE_TO_MOUNTPOINT.getTypeId());

    /**
     * {@link FSLocationSpec} for the connected relative-to workflow data area file system.
     */
    public static final FSLocationSpec CONNECTED_WORKFLOW_DATA_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.CONNECTED, FSType.RELATIVE_TO_WORKFLOW_DATA_AREA.getTypeId());

    /**
     * {@link FSLocationSpec} for the connected relative-to Hub Space file system.
     */
    public static final FSLocationSpec CONNECTED_SPACE_RELATIVE_FS_LOCATION_SPEC =
        new DefaultFSLocationSpec(FSCategory.CONNECTED, FSType.RELATIVE_TO_SPACE.getTypeId());

    /**
     * Separator used among all Relative-to file systems to separate the name componenets in a path.
     */
    public static final String PATH_SEPARATOR = "/";

    private final RelativeTo m_type;

    private FSLocationSpec m_customFSLocationSpec;

    /**
     * Constructor for a convenience file system with the default working directory.
     *
     * @param type Relative To type
     */
    public RelativeToFSConnectionConfig(final RelativeTo type) {
        super(PATH_SEPARATOR, false);
        m_type = type;
    }

    /**
     * Constructor for a connected file system with the given working directory.
     *
     * @param workingDirectory The working directory to use.
     * @param type Relative To type
     */
    public RelativeToFSConnectionConfig(final String workingDirectory, final RelativeTo type) {
        super(workingDirectory, true);
        m_type = type;
    }

    /**
     * Constructor for a connected file system with the given working directory and custom timeouts.
     *
     * @param workingDirectory The working directory to use.
     * @param type Relative To type
     * @param connectionTimeout the connectionTimeout.
     * @param readTimeout the readTimeout.
     */
    public RelativeToFSConnectionConfig(final String workingDirectory, final RelativeTo type,
        final Duration connectionTimeout, final Duration readTimeout) {
        super(workingDirectory, true);
        m_type = type;
        setConnectionTimeout(connectionTimeout);
        setReadTimeout(readTimeout);
    }

    /**
     * Copy constructor.
     *
     * @param toCopy The other config to copy.
     */
    public RelativeToFSConnectionConfig(final RelativeToFSConnectionConfig toCopy) {
        super(toCopy.getWorkingDirectory(), //
            toCopy.isConnectedFileSystem(), //
            toCopy.getConnectionTimeout(), //
            toCopy.getReadTimeout());
        m_type = toCopy.m_type;
        m_customFSLocationSpec = toCopy.m_customFSLocationSpec;
    }

    /**
     * @return the {@link RelativeTo} type of the file system.
     */
    public RelativeTo getType() {
        return m_type;
    }

    /**
     * Sets a custom {@link FSLocationSpec} to use.
     *
     * @param customFSLocationSpec the custom {@link FSLocationSpec} to use
     */
    public void setCustomFSLocationSpec(final FSLocationSpec customFSLocationSpec) {
        m_customFSLocationSpec = customFSLocationSpec;
    }

    /**
     * @return an optional custom {@link FSLocationSpec} to use.
     */
    public Optional<FSLocationSpec> getCustomFSLocationSpec() {
        return Optional.ofNullable(m_customFSLocationSpec);
    }

    /**
     * @return the {@link FSLocationSpec} to use.
     */
    public FSLocationSpec getFSLocationSpec() {
        return getCustomFSLocationSpec() //
            .orElseGet(() -> determineFSLocationSpec(getType(), isConnectedFileSystem()));
    }

    private static FSLocationSpec determineFSLocationSpec(final RelativeTo type, final boolean isConnected) {
        final FSLocationSpec toReturn;

        switch (type) {
            case MOUNTPOINT: // NOSONAR this is fine
                if (isConnected) {
                    toReturn = CONNECTED_MOUNTPOINT_RELATIVE_FS_LOCATION_SPEC;
                } else {
                    toReturn = CONVENIENCE_MOUNTPOINT_RELATIVE_FS_LOCATION_SPEC;
                }
                break;
            case SPACE: // NOSONAR this is fine
                if (isConnected) {
                    toReturn = CONNECTED_SPACE_RELATIVE_FS_LOCATION_SPEC;
                } else {
                    toReturn = CONVENIENCE_SPACE_RELATIVE_FS_LOCATION_SPEC;
                }
                break;
            case WORKFLOW: // NOSONAR this is fine
                if (isConnected) {
                    toReturn = CONNECTED_WORKFLOW_RELATIVE_FS_LOCATION_SPEC;
                } else {
                    toReturn = CONVENIENCE_WORKFLOW_RELATIVE_FS_LOCATION_SPEC;
                }
                break;
            case WORKFLOW_DATA: // NOSONAR this is fine
                if (isConnected) {
                    toReturn = CONNECTED_WORKFLOW_DATA_RELATIVE_FS_LOCATION_SPEC;
                } else {
                    toReturn = CONVENIENCE_WORKFLOW_DATA_RELATIVE_FS_LOCATION_SPEC;
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown Relative-to file type %s", type));
        }

        return toReturn;
    }
}
