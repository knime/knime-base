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
 *   Apr 28, 2022 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.core.connections.config;

import java.time.Duration;

import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.meta.FSConnectionConfig;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.connections.meta.base.BaseFSConnectionConfig;

/**
 * {@link FSConnectionConfig} for the Space file system. It is unlikely that you will have to use this class directly.
 * To create a configured Space file system, please use {@link DefaultFSConnectionFactory}.
 *
 * @author Zkriya Rakhimberdiyev
 */
public class SpaceFSConnectionConfig extends BaseFSConnectionConfig {

    /**
     * Path separator character
     */
    public static final String PATH_SEPARATOR = "/";

    private final String m_mountID;

    private final String m_spaceId;

    private final Duration m_connectionTimeout;

    private final Duration m_readTimeout;

    /**
     * Constructor that creates a CONNECTED file system with the given working directory.
     *
     * @param workingDirectory the working directory to use.
     * @param mountID the mount ID to connect to.
     * @param spaceId the unique ID of the space repository item (e.g. *YK_q3iKGm3WUlAzo)
     * @param connectionTimeout the connectionTimeout.
     * @param readTimeout the readTimeout.
     */
    public SpaceFSConnectionConfig(final String workingDirectory, final String mountID,
        final String spaceId, final Duration connectionTimeout, final Duration readTimeout) {
        super(workingDirectory, true);
        m_mountID = mountID;
        m_spaceId = spaceId;
        m_connectionTimeout = connectionTimeout;
        m_readTimeout = readTimeout;
    }

    /**
     * @return the mount ID to connect to.
     */
    public String getMountID() {
        return m_mountID;
    }

    /**
     * @return the space path to connect to.
     */
    public String getSpaceID() {
        return m_spaceId;
    }

    /**
     * @return the connectionTimeout.
     */
    public Duration getConnectionTimeout() {
        return m_connectionTimeout;
    }

    /**
     * @return the readTimeout.
     */
    public Duration getReadTimeout() {
        return m_readTimeout;
    }

    /**
     * @return the {@link FSLocationSpec} for the Space file system which uses this configuration.
     */
    public FSLocationSpec createFSLocationSpec() {
        final FSCategory category;
        final String specifier;

        if (isConnectedFileSystem()) {
            category = FSCategory.CONNECTED;
            specifier = String.format("%s:%s:%s", //
                FSType.SPACE.getTypeId(), //
                getMountID(), //
                getSpaceID());
        } else {
            throw new UnsupportedOperationException("Connecting to a space using a convenience file system is not supported yet");
        }

        return new DefaultFSLocationSpec(category, specifier);
    }
}
