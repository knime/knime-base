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
 *   20.08.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.nodes.s3connection;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.files.Protocol;
import org.knime.cloud.aws.util.AWSConnectionInformationSettings;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;
import org.knime.filehandling.core.connections.s3.S3CloudConnectionInformation;

/**
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class S3ConnectionConfig extends AWSConnectionInformationSettings {

    /**
     * @param prefix
     */
    public S3ConnectionConfig(final String prefix) {
        super(prefix);

    }

    private static final String CONNECTION_NAME = "ConnectionName";

    private final SettingsModelString m_connectionNameModel =
        new SettingsModelString(CONNECTION_NAME, "NewS3Connection");

    /**
     * @return the connectionNameModel
     */
    public SettingsModelString getConnectionNameModel() {
        return m_connectionNameModel;

    }

    /**
     * @return the connection name
     */
    public String getConnectionName() {
        return m_connectionNameModel.getStringValue();
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_connectionNameModel.saveSettingsTo(settings);
        super.saveSettingsTo(settings);

    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_connectionNameModel.validateSettings(settings);
        if (m_connectionNameModel.getStringValue() == null || m_connectionNameModel.getStringValue().trim().isEmpty()) {
            throw new InvalidSettingsException("Connection name must not be empty");
        }
        super.validateSettings(settings);
    }


    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_connectionNameModel.loadSettingsFrom(settings);
        super.loadValidatedSettings(settings);
    }

    /**
     * Copied from AWSConnectionInformationSettings, to use new auth method.
     *
     * Returns the {@link ConnectionInformation} resulting from the settings
     *
     * @param credentialsProvider the credentials provider
     * @param protocol the protocol
     * @return this settings ConnectionInformation
     */
    @Override
    public S3CloudConnectionInformation createConnectionInformation(final CredentialsProvider credentialsProvider,
        final Protocol protocol) {

        // Create connection information object
        final S3CloudConnectionInformation connectionInformation = new S3CloudConnectionInformation();

        connectionInformation.setProtocol(protocol.getName());
        connectionInformation.setHost(getRegion());
        connectionInformation.setPort(protocol.getPort());
        connectionInformation.setTimeout(getTimeout());

        // Set the field "useKerberos" to true if "Default Credentials Provider Chain" should be used,
        // otherwise to false
        connectionInformation
            .setUseKeyChain(getAuthenticationModel().getAuthenticationType().equals(AuthenticationType.KERBEROS));

        if (getAuthenticationModel().getAuthenticationType().equals(AuthenticationType.KERBEROS)) {
            connectionInformation.setUser("*****");
            connectionInformation.setPassword(null);
        } else if (getAuthenticationModel().getAuthenticationType().equals(AuthenticationType.NONE)) {
            connectionInformation.setUseAnonymous(true);
        } else {
            // Put accessKeyId as user and secretAccessKey as password
            if (useWorkflowCredential()) {
                // Use credentials
                final ICredentials credentials = credentialsProvider.get(getWorkflowCredential());
                connectionInformation.setUser(credentials.getLogin());
                connectionInformation.setPassword(credentials.getPassword());
            } else {
                connectionInformation.setUser(getUserValue());
                connectionInformation.setPassword(getPasswordValue());
            }
        }

        connectionInformation.setUseSSEncryption(getSSEncryptionModel().getBooleanValue());

        connectionInformation.setSwitchRole(getSwitchRoleModel().getBooleanValue());
        connectionInformation.setSwitchRoleAccount(getSwitchRoleAccountModel().getStringValue());
        connectionInformation.setSwitchRoleName(getSwitchRoleNameModel().getStringValue());
        return connectionInformation;
    }

}
