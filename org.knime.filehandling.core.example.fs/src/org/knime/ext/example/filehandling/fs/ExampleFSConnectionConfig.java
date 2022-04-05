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
 *   2022-04-05 (Bjoern Lohrmann): created
 */
package org.knime.ext.example.filehandling.fs;

import java.time.Duration;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.base.auth.AuthType;
import org.knime.filehandling.core.connections.base.auth.StandardAuthTypes;
import org.knime.filehandling.core.connections.meta.base.BaseFSConnectionConfig;

/**
 * Configuration for the {@link ExampleFSConnection}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class ExampleFSConnectionConfig extends BaseFSConnectionConfig {

    private ConnectionMode m_connectionMode;

    private String m_fileserverHost;

    private int m_fileserverPort;

    private String m_fileserverShare;

    private String m_domainName;

    private String m_domainNamespace;

    private AuthType m_authType;

    private String m_user;

    private String m_password;

    private Duration m_timeout = Duration.ofSeconds(30);

    /**
     * The {@link AuthType} for Kerberos authentication mode.
     */
    public static final AuthType KERBEROS_AUTH_TYPE = new AuthType("kerberos", "Kerberos",
            "Authenticate using Kerberos");

    /**
     * The {@link AuthType} for password-less authentication as the Guest user.
     */
    public static final AuthType GUEST_AUTH_TYPE = new AuthType("guest", "Guest",
            "Authenticate as the Guest user (without password)");

    /**
     *
     * Enum representing different connection modes.
     */
    public enum ConnectionMode {
        /**
         * Connect to Fileserver
         */
        FILESERVER("fileserver", "File server"),
        /**
         *
         * Connect to Domain
         */
        DOMAIN("domain", "Domain");

        private String m_settingsValue;
        private String m_title;

        private ConnectionMode(final String settingsValue, final String title) {
            m_settingsValue = settingsValue;
            m_title = title;
        }

        /**
         * @return the settings value under which to store this mode.
         */
        public String getSettingsValue() {
            return m_settingsValue;
        }

        @Override
        public String toString() {
            return m_title;
        }

        /**
         * Loads the mode from the give {@link NodeSettingsRO} where it is stored under
         * the given key.
         *
         * @param settingsValue
         *            A settings value to map to a {@link ConnectionMode} enum.
         * @return The corresponding {@link ConnectionMode} enum.
         * @throws InvalidSettingsException
         *             In case settingsValue does not correspond to any of the existing
         *             modes.
         */
        public static ConnectionMode fromSettingsValue(final String settingsValue) throws InvalidSettingsException {
            for (ConnectionMode m : values()) {
                if (m.getSettingsValue().equals(settingsValue)) {
                    return m;
                }
            }

            throw new InvalidSettingsException("Unknown connection mode: " + settingsValue);
        }

    }

    /**
     * @param workingDirectory
     *            the working directory to set
     */
    public ExampleFSConnectionConfig(final String workingDirectory) {
        super(workingDirectory);
    }

    /**
     * @return the connection mode
     */
    public ConnectionMode getConnectionMode() {
        return m_connectionMode;
    }

    /**
     * @param mode
     *            the connection mode to set
     */
    public void setConnectionMode(final ConnectionMode mode) {
        m_connectionMode = mode;
    }

    /**
     * @return the host to connect to, when using {@link ConnectionMode#FILESERVER}
     *         mode)
     */
    public String getFileserverHost() {
        return m_fileserverHost;
    }

    /**
     * @param fileserverHost
     *            the host to connect to, when using
     *            {@link ConnectionMode#FILESERVER} mode)
     */
    public void setFileserverHost(final String fileserverHost) {
        m_fileserverHost = fileserverHost;
    }

    /**
     * @return the port to connect to, when using {@link ConnectionMode#FILESERVER}
     *         mode)
     */
    public int getFileserverPort() {
        return m_fileserverPort;
    }

    /**
     * @param fileserverPort
     *            the port to connect to, when using
     *            {@link ConnectionMode#FILESERVER} mode)
     */
    public void setFileserverPort(final int fileserverPort) {
        m_fileserverPort = fileserverPort;
    }

    /**
     * @return the fileserverShare the share to connect to, when using
     *         {@link ConnectionMode#FILESERVER} mode)
     */
    public String getFileserverShare() {
        return m_fileserverShare;
    }

    /**
     * @param fileserverShare
     *            the share to connect to, when using
     *            {@link ConnectionMode#FILESERVER} mode)
     */
    public void setFileserverShare(final String fileserverShare) {
        m_fileserverShare = fileserverShare;
    }

    /**
     * @return the domain to connect to, when using {@link ConnectionMode#DOMAIN}
     *         mode)
     */
    public String getDomainName() {
        return m_domainName;
    }

    /**
     * @param domainName
     *            the domain to connect to, when using {@link ConnectionMode#DOMAIN}
     *            mode)
     */
    public void setDomainName(final String domainName) {
        m_domainName = domainName;
    }

    /**
     * @return the namespace to connect to, when using {@link ConnectionMode#DOMAIN}
     *         mode)
     */
    public String getDomainNamespace() {
        return m_domainNamespace;
    }

    /**
     * @param domainNamespace
     *            the namespace to connect to, when using
     *            {@link ConnectionMode#DOMAIN} mode)
     */
    public void setDomainNamespace(final String domainNamespace) {
        m_domainNamespace = domainNamespace;
    }

    /**
     * @return the authentication type to use (one of {@link #KERBEROS_AUTH_TYPE} or
     *         {@link StandardAuthTypes#USER_PASSWORD}.
     */
    public AuthType getAuthType() {
        return m_authType;
    }

    /**
     * @param authType
     *            the authentication type to use (one of
     *            {@link StandardAuthTypes#USER_PASSWORD},
     *            {@link #KERBEROS_AUTH_TYPE} or
     *            {@link StandardAuthTypes#ANONYMOUS})..
     */
    public void setAuthType(final AuthType authType) {
        m_authType = authType;
    }

    /**
     * @return the username to use when {@link StandardAuthTypes#USER_PASSWORD}
     *         authentication is used.
     */
    public String getUser() {
        return m_user;
    }

    /**
     * @param user
     *            the username to use when {@link StandardAuthTypes#USER_PASSWORD}
     *            authentication is used.
     */
    public void setUser(final String user) {
        m_user = user;
    }

    /**
     * @return the password to use when {@link StandardAuthTypes#USER_PASSWORD}
     *         authentication is used.
     */
    public String getPassword() {
        return m_password;
    }

    /**
     * @param password
     *            the password to use when {@link StandardAuthTypes#USER_PASSWORD}
     *            authentication is used.
     */
    public void setPassword(final String password) {
        m_password = password;
    }

    /**
     * @return the timeout to use for read/write operations.
     */
    public Duration getTimeout() {
        return m_timeout;
    }

    /**
     * @param timeout
     *            the timeout to use for read/write operations.
     */
    public void setTimeout(final Duration timeout) {
        m_timeout = timeout;
    }

    /**
     * Generates a {@link FSLocationSpec} for the current Example file system
     * configuration.
     *
     * @return the {@link FSLocationSpec} for the current Example file system
     *         configuration.
     */
    public DefaultFSLocationSpec createFSLocationSpec() {
        final String hostOrDomain = getConnectionMode() == ConnectionMode.FILESERVER //
                ? getFileserverHost() //
                : getDomainName();

        final String shareOrNamespace = getConnectionMode() == ConnectionMode.FILESERVER //
                ? getFileserverShare() //
                : getDomainNamespace();

        return new DefaultFSLocationSpec(FSCategory.CONNECTED, //
                String.format("%s:%s:%s", //
                        ExampleFSDescriptorProvider.FS_TYPE, //
                        hostOrDomain, //
                        shareOrNamespace));
    }
}
