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

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import org.knime.core.util.auth.Authenticator;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.meta.FSConnectionConfig;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.connections.meta.base.TimeoutFSConnectionConfig;

/**
 * {@link FSConnectionConfig} for the KNIME Hub Space file system. The KNIME Hub Space file system connects to a KNIME
 * Hub instance and allows to access the file tree of a single Space. It is unlikely that you will have to use this
 * class directly, please see {@link DefaultFSConnectionFactory#createHubSpaceConnection(HubSpaceFSConnectionConfig)}.
 *
 * @author Zkriya Rakhimberdiyev
 */
public class HubSpaceFSConnectionConfig extends TimeoutFSConnectionConfig {

    /**
     * Path separator character
     */
    public static final String PATH_SEPARATOR = "/";

    private final URI m_repositoryAddress;

    private final Authenticator m_authenticator;

    private final String m_spaceId;

    private final boolean m_useSpaceVersion;

    private final String m_spaceVersion;

    /**
     * Creates a config for a {@link FSCategory#CONNECTED} Hub Space file system.
     *
     * @param workingDir The working directory to use.
     * @param repositoryAddress The http(s) URL of the Hub repository/catalog REST API, e.g.
     *            https://api.hub.knime.com/repository/.
     * @param authenticator {@link Authenticator} to use to authenticate against the REST API.
     * @param spaceId The unique KNIME ID (repository item ID) of the space, e.g. *YK_q3iKGm3WUlAzo.
     * @param useSpaceVersion Whether to connect to a particular version of the Space, or not. If true, the file system
     *            will be read-only.
     * @param spaceVersion Specifies the Space version to connect to. May be null to indicate the newest available
     *            version.
     * @param connectionTimeout The timeout to use when opening a TCP/IP connection to a the target host.
     * @param readTimeout The per-request timeout that defines how long to wait for an answer after having sent a
     *            request/command to the target host.
     */
    public HubSpaceFSConnectionConfig(final String workingDir, // NOSONAR
        final URI repositoryAddress, //
        final Authenticator authenticator, //
        final String spaceId, //
        final boolean useSpaceVersion,//
        final String spaceVersion,
        final Duration connectionTimeout, //
        final Duration readTimeout) {

        super(workingDir);
        m_repositoryAddress = repositoryAddress;
        m_authenticator = authenticator;
        m_spaceId = spaceId;
        m_useSpaceVersion = useSpaceVersion;
        m_spaceVersion = spaceVersion;
        setConnectionTimeout(connectionTimeout);
        setReadTimeout(readTimeout);
    }

    /**
     * Creates a config for a convenience {@link FSCategory#HUB_SPACE} Hub Space file system.
     *
     * @param repositoryAddress The http(s) URL of the Hub repository/catalog REST API, e.g.
     *            https://api.hub.knime.com/repository/.
     * @param authenticator {@link Authenticator} to use to authenticate against the REST API.
     * @param spaceId the unique ID of the space repository item (e.g. *YK_q3iKGm3WUlAzo)
     */
    public HubSpaceFSConnectionConfig(final URI repositoryAddress, //
        final Authenticator authenticator, //
        final String spaceId) {

        super(PATH_SEPARATOR, false);
        m_repositoryAddress = repositoryAddress;
        m_authenticator = authenticator;
        m_spaceId = spaceId;
        m_useSpaceVersion = false;
        m_spaceVersion = null;
    }

    /**
     * @return the http(s) URL of the Hub repository/catalog REST API, e.g. https://api.hub.knime.com/repository/
     */
    public URI getRepositoryAddress() {
        return m_repositoryAddress;
    }

    /**
     * @return the {@link Authenticator} to use to authenticate against the REST API.
     */
    public Authenticator getAuthenticator() {
        return m_authenticator;
    }

    /**
     * @return the optional containing space id to connect to. Empty optional in case of relative-to current space file
     *         system.
     */
    public String getSpaceId() {
        return m_spaceId;
    }

    /**
     * @return whether or not to connect to a specific version of the Hub Space. If true, the file system will be
     *         read-only.
     */
    public boolean useSpaceVersion() {
        return m_useSpaceVersion;
    }

    /**
     * @return the version of the Space to connect to. If empty, but {@link #useSpaceVersion()} returns true, this
     *         indicates that the newest available version should be connected to.
     */
    public Optional<String> getSpaceVersion() {
        return Optional.ofNullable(m_spaceVersion);
    }

    /**
     * @return the {@link FSLocationSpec} for the Hub Space file system which uses this configuration.
     */
    public FSLocationSpec createFSLocationSpec() {
        final FSCategory category;
        final String specifier;
        if (isConnectedFileSystem()) {
            category = FSCategory.CONNECTED;
            specifier = String.format("%s:%s:%s", //
                FSType.HUB_SPACE, //
                getRepositoryAddress().getHost().toLowerCase(Locale.ENGLISH), //
                m_spaceId);
        } else {
            category = FSCategory.HUB_SPACE;
            specifier = m_spaceId;
        }

        return new DefaultFSLocationSpec(category, specifier);
    }
}
