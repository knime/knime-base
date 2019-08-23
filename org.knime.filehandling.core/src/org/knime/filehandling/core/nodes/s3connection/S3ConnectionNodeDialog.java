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

import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.knime.base.filehandling.remote.connectioninformation.node.TestConnectionDialog;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.cloud.aws.s3.filehandler.S3RemoteFileHandler;
import org.knime.cloud.aws.util.AWSConnectionInformationSettings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentString;

import com.amazonaws.services.s3.AmazonS3;

/**
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class S3ConnectionNodeDialog extends NodeDialogPane {

    S3ConnectionConfig m_config = new S3ConnectionConfig(AmazonS3.ENDPOINT_PREFIX);

    DialogComponentString m_connectionName =
        new DialogComponentString(m_config.getConnectionNameModel(), "Connection name");

    private final S3DialogComponents m_awsComp = new S3DialogComponents(m_config, S3ConnectionNodeModel.getNameMap());

    /**
     *
     */
    public S3ConnectionNodeDialog() {
        final JPanel panel = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        panel.add(m_connectionName.getComponentPanel());

        gbc.gridy++;

        panel.add(m_awsComp.getDialogPanel(), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.ABOVE_BASELINE_LEADING;
        final JButton testConnectionButton = new JButton("Test connection");
        testConnectionButton.addActionListener(new TestConnectionListener());
        panel.add(testConnectionButton, gbc);
        addTab("Options", panel);

        final JPanel encryptionPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(5, 5, 5, 5);
        gbc2.anchor = GridBagConstraints.NORTHWEST;
        gbc2.weightx = 0;
        gbc2.weighty = 0;
        gbc2.gridx = 0;
        gbc2.gridy = 0;
        encryptionPanel.add(m_awsComp.getEncryptionDialogPanel(), gbc);
        addTab("Encryption", encryptionPanel);
    }

    /**
     * Listener that opens the test connection dialog.
     *
     *
     * @author Patrick Winter, KNIME AG, Zurich, Switzerland
     */
    private class TestConnectionListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            // Get frame
            Frame frame = null;
            Container container = getPanel().getParent();
            while (container != null) {
                if (container instanceof Frame) {
                    frame = (Frame)container;
                    break;
                }
                container = container.getParent();
            }

            // Get connection information to current settings
            final AWSConnectionInformationSettings model = m_awsComp.getSettings();
            final ConnectionInformation connectionInformation =
                model.createConnectionInformation(getCredentialsProvider(), S3RemoteFileHandler.PROTOCOL);

            new TestConnectionDialog(connectionInformation).open(frame);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_config.saveSettingsTo(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        try {
            m_config.loadValidatedSettings(settings);
        } catch (final InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage());
        }
    }
}
