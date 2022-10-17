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
 *   Sep 26, 2022 (bjoern): created
 */
package org.knime.filehandling.core.connections.config;

import java.net.URI;
import java.util.Locale;

import org.knime.core.util.auth.Authenticator;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.meta.FSConnectionConfig;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.connections.meta.base.TimeoutFSConnectionConfig;

/**
 * {@link FSConnectionConfig} for the KNIME Hub file system that connects to a KNIME Hub deployment via {@link URI} and
 * {@link Authenticator}. The KNIME Hub file system connects to a KNIME Hub instance and allows to access the whole file
 * tree of the Hub. It is unlikely that you will have to use this class directly, please see
 * {@link DefaultFSConnectionFactory#createHubConnection(URI, Authenticator)}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class HubFSConnectionConfig extends TimeoutFSConnectionConfig {

    /**
     * Path separator character
     */
    public static final String PATH_SEPARATOR = "/";

    private final URI m_repositoryAddress;

    private final Authenticator m_authenticator;

    /**
     * Constructor for a {@link FSCategory#CONNECTED} Hub file system with the default working directory.
     *
     * @param repositoryAddress http(s) URL of the Hub repository/catalog REST API, e.g.
     *            https://api.hub.knime.com/repository/
     * @param authenticator {@link Authenticator} to use to authenticate against the REST API.
     */
    public HubFSConnectionConfig(final URI repositoryAddress, final Authenticator authenticator) {
        this(PATH_SEPARATOR, repositoryAddress, authenticator);
    }

    /**
     * Constructor for a {@link FSCategory#CONNECTED} Hub file system.
     *
     * @param workingDir The working directory to use.
     * @param repositoryAddress http(s) URL of the Hub repository/catalog REST API, e.g.
     *            https://api.hub.knime.com/repository/
     * @param authenticator {@link Authenticator} to use to authenticate against the REST API.
     */
    public HubFSConnectionConfig(final String workingDir, final URI repositoryAddress,
        final Authenticator authenticator) {
        super(workingDir);
        m_repositoryAddress = repositoryAddress;
        m_authenticator = authenticator;
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
     * @return the {@link FSLocationSpec} for this file system configuration
     */
    public FSLocationSpec createFSLocationSpec() {
        return new DefaultFSLocationSpec(FSCategory.CONNECTED, String.format("%s:%s", //
            FSType.HUB.getTypeId(), //
            getRepositoryAddress().getHost().toLowerCase(Locale.ENGLISH)));
    }
}
