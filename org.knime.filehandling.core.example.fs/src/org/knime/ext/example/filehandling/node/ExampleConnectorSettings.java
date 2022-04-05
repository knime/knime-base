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
package org.knime.ext.example.filehandling.node;

import java.time.Duration;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;
import org.knime.ext.example.filehandling.fs.ExampleFSConnectionConfig;
import org.knime.ext.example.filehandling.fs.ExampleFSConnectionConfig.ConnectionMode;
import org.knime.ext.example.filehandling.fs.ExampleFileSystem;
import org.knime.filehandling.core.connections.base.auth.AuthSettings;
import org.knime.filehandling.core.connections.base.auth.EmptyAuthProviderSettings;
import org.knime.filehandling.core.connections.base.auth.StandardAuthTypes;
import org.knime.filehandling.core.connections.base.auth.UserPasswordAuthProviderSettings;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Node settings for the Example Connector.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
class ExampleConnectorSettings {

    private static final String KEY_CONNECTION_MODE = "connectionMode";
    private static final String KEY_FILESERVER_HOST = "fileserver.host";
    private static final String KEY_FILESERVER_PORT = "fileserver.port";
    private static final String KEY_FILESERVER_SHARE = "fileserver.share";
    private static final String KEY_DOMAIN_NAME = "domain.name";
    private static final String KEY_DOMAIN_NAMESPACE = "domain.namespace";
    private static final String KEY_WORKING_DIRECTORY = "workingDirectory";
    private static final String KEY_TIMEOUT = "timeout";

    private static final int DEFAULT_PORT = 445;
    private static final int DEFAULT_TIMEOUT = 30;

    private ConnectionMode m_connectionMode;
    private final SettingsModelString m_fileserverHost;
    private final SettingsModelIntegerBounded m_fileserverPort;
    private final SettingsModelString m_fileserverShare;
    private final SettingsModelString m_domainName;
    private final SettingsModelString m_domainNamespace;
    private final AuthSettings m_authSettings;
    private final SettingsModelString m_workingDirectory;
    private final SettingsModelIntegerBounded m_timeout;

    /**
     * Creates new instance
     */
    public ExampleConnectorSettings() {
        m_connectionMode = ConnectionMode.FILESERVER;

        m_fileserverHost = new SettingsModelString(KEY_FILESERVER_HOST, "");
        m_fileserverPort = new SettingsModelIntegerBounded(KEY_FILESERVER_PORT, DEFAULT_PORT, 1, 65535);
        m_fileserverShare = new SettingsModelString(KEY_FILESERVER_SHARE, "");

        m_domainName = new SettingsModelString(KEY_DOMAIN_NAME, "");
        m_domainNamespace = new SettingsModelString(KEY_DOMAIN_NAMESPACE, "");

        m_authSettings = new AuthSettings.Builder() //
                .add(new UserPasswordAuthProviderSettings(StandardAuthTypes.USER_PASSWORD, true)) //
                .add(new EmptyAuthProviderSettings(ExampleFSConnectionConfig.KERBEROS_AUTH_TYPE)) //
                .add(new EmptyAuthProviderSettings(ExampleFSConnectionConfig.GUEST_AUTH_TYPE)) //
                .add(new EmptyAuthProviderSettings(StandardAuthTypes.ANONYMOUS)) //
                .defaultType(StandardAuthTypes.USER_PASSWORD) //
                .build();

        m_workingDirectory = new SettingsModelString(KEY_WORKING_DIRECTORY, ExampleFileSystem.SEPARATOR);
        m_timeout = new SettingsModelIntegerBounded(KEY_TIMEOUT, DEFAULT_TIMEOUT, 0, Integer.MAX_VALUE);
    }

    /**
     * @return the connectionMode
     */
    public ConnectionMode getConnectionMode() {
        return m_connectionMode;
    }

    /**
     * @param connectionMode
     *            the connectionMode to set
     */
    public void setConnectionMode(final ConnectionMode connectionMode) {
        m_connectionMode = connectionMode;
    }

    /**
     * @return the host model
     */
    public SettingsModelString getFileserverHostModel() {
        return m_fileserverHost;
    }

    /**
     * @return the hostname
     */
    public String getFileserverHost() {
        return m_fileserverHost.getStringValue();
    }

    /**
     * @return the port model
     */
    public SettingsModelIntegerBounded getFileserverPortModel() {
        return m_fileserverPort;
    }

    /**
     * @return the port
     */
    public int getFileserverPort() {
        return m_fileserverPort.getIntValue();
    }

    /**
     * @return the fileShare model
     */
    public SettingsModelString getFileserverShareModel() {
        return m_fileserverShare;
    }

    /**
     * @return the fileShare
     */
    public String getFileserverShare() {
        return m_fileserverShare.getStringValue();
    }

    /**
     * @return the domain model
     */
    public SettingsModelString getDomainNameModel() {
        return m_domainName;
    }

    /**
     * @return the domain
     */
    public String getDomainName() {
        return m_domainName.getStringValue();
    }

    String getDomainNamespace() {
        return m_domainNamespace.getStringValue();
    }

    SettingsModelString getDomainNamespaceModel() {
        return m_domainNamespace;
    }

    /**
     * @return The hostname or domain depending on the connection mode.
     */
    public String getHostnameOrDomain() {
        if (m_connectionMode == ConnectionMode.FILESERVER) {
            return getFileserverHost();
        } else {
            return getDomainName();
        }
    }

    /**
     * @return the authSettings
     */
    public AuthSettings getAuthSettings() {
        return m_authSettings;
    }

    /**
     * @return the workingDirectory model
     */
    public SettingsModelString getWorkingDirectoryModel() {
        return m_workingDirectory;
    }

    /**
     * @return the workingDirectory
     */
    public String getWorkingDirectory() {
        return m_workingDirectory.getStringValue();
    }

    /**
     * @return the timeout model
     */
    public SettingsModelIntegerBounded getTimeoutModel() {
        return m_timeout;
    }

    /**
     * @return the timeout
     */
    public Duration getTimeout() {
        return Duration.ofSeconds(m_timeout.getIntValue());
    }

    private void save(final NodeSettingsWO settings) {
        settings.addString(KEY_CONNECTION_MODE, m_connectionMode.getSettingsValue());
        m_fileserverHost.saveSettingsTo(settings);
        m_fileserverPort.saveSettingsTo(settings);
        m_fileserverShare.saveSettingsTo(settings);
        m_domainName.saveSettingsTo(settings);
        m_domainNamespace.saveSettingsTo(settings);
        m_workingDirectory.saveSettingsTo(settings);
        m_timeout.saveSettingsTo(settings);
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO} (to be called by the node
     * model).
     *
     * @param settings
     *            The settings.
     */
    public void saveForModel(final NodeSettingsWO settings) {
        save(settings);
        m_authSettings.saveSettingsForModel(settings.addNodeSettings(AuthSettings.KEY_AUTH));
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO} (to be called by the node
     * dialog).
     *
     * @param settings
     *            The settings.
     */
    public void saveForDialog(final NodeSettingsWO settings) {
        save(settings);
        // m_authSettings are also saved by AuthenticationDialog
    }

    private void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_connectionMode = ConnectionMode.fromSettingsValue(settings.getString(KEY_CONNECTION_MODE));
        m_fileserverHost.loadSettingsFrom(settings);
        m_fileserverPort.loadSettingsFrom(settings);
        m_fileserverShare.loadSettingsFrom(settings);
        m_domainName.loadSettingsFrom(settings);
        m_domainNamespace.loadSettingsFrom(settings);
        m_workingDirectory.loadSettingsFrom(settings);
        m_timeout.loadSettingsFrom(settings);
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the
     * node model).
     *
     * @param settings
     *            The settings.
     * @throws InvalidSettingsException
     */
    public void loadForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
        m_authSettings.loadSettingsForModel(settings.getNodeSettings(AuthSettings.KEY_AUTH));
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the
     * node dialog).
     *
     * @param settings
     *            The settings.
     * @throws InvalidSettingsException
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) throws InvalidSettingsException {
        load(settings);
        // m_authSettings are loaded by AuthenticationDialog
    }

    void configureInModel(final PortObjectSpec[] inSpecs, final Consumer<StatusMessage> statusConsumer,
            final CredentialsProvider credentialsProvider) throws InvalidSettingsException {
        m_authSettings.configureInModel(inSpecs, statusConsumer, credentialsProvider);
    }

    /**
     * Validates the settings stored in the given {@link NodeSettingsRO}.
     *
     * @param settings
     *            The settings.
     * @throws InvalidSettingsException
     */
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        ConnectionMode.fromSettingsValue(settings.getString(KEY_CONNECTION_MODE));
        m_fileserverHost.validateSettings(settings);
        m_fileserverPort.validateSettings(settings);
        m_fileserverShare.validateSettings(settings);
        m_domainName.validateSettings(settings);
        m_domainNamespace.validateSettings(settings);
        m_authSettings.validateSettings(settings.getNodeSettings(AuthSettings.KEY_AUTH));
        m_workingDirectory.validateSettings(settings);
        m_timeout.validateSettings(settings);
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    void validate() throws InvalidSettingsException {
        if (m_connectionMode == ConnectionMode.FILESERVER) {
            validateFileserverSettings();
        } else {
            validateDomainSettings();
        }

        m_authSettings.validate();

        String workDir = m_workingDirectory.getStringValue();
        if (workDir.isEmpty() || !workDir.startsWith(ExampleFileSystem.SEPARATOR)) {
            throw new InvalidSettingsException("Working directory must be set to an absolute path.");
        }
    }

    /**
     * @throws InvalidSettingsException
     */
    private void validateDomainSettings() throws InvalidSettingsException {
        if (StringUtils.isBlank(getDomainName())) {
            throw new InvalidSettingsException("Domain name must be specified");
        }

        if (StringUtils.isBlank(getDomainNamespace())) {
            throw new InvalidSettingsException("Share/Namespace must be specified");
        }
    }

    private void validateFileserverSettings() throws InvalidSettingsException {
        if (StringUtils.isBlank(getFileserverHost())) {
            throw new InvalidSettingsException("Host must be specified");
        }

        if (getFileserverPort() < 1 || getFileserverPort() > 65535) {
            throw new InvalidSettingsException("Port must be between 1 and 65535");
        }

        if (StringUtils.isBlank(getFileserverShare())) {
            throw new InvalidSettingsException("Share name on fileserver must be specified");
        }
    }

    ExampleFSConnectionConfig createFSConnectionConfig(final Function<String, ICredentials> credentialsProvider) {
        final ExampleFSConnectionConfig config = new ExampleFSConnectionConfig(getWorkingDirectory());

        config.setConnectionMode(m_connectionMode);
        if (m_connectionMode == ConnectionMode.DOMAIN) {
            config.setDomainName(getDomainName().trim().toUpperCase(Locale.US));
            config.setDomainNamespace(getDomainNamespace().trim());
        } else {
            config.setFileserverHost(getFileserverHost().trim().toUpperCase(Locale.US));
            config.setFileserverPort(getFileserverPort());
            config.setFileserverShare(getFileserverShare().trim());
        }

        config.setAuthType(getAuthSettings().getAuthType());
        if (getAuthSettings().getAuthType() == StandardAuthTypes.USER_PASSWORD) {
            final UserPasswordAuthProviderSettings userPassSettings = getAuthSettings()
                    .getSettingsForAuthType(StandardAuthTypes.USER_PASSWORD);
            config.setUser(userPassSettings.getUser(credentialsProvider));
            config.setPassword(userPassSettings.getPassword(credentialsProvider));
        }

        config.setTimeout(getTimeout());

        return config;
    }
}
