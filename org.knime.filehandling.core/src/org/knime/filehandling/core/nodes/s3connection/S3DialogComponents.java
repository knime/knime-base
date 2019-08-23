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
 *   03.09.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.nodes.s3connection;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.cloud.aws.util.AWSConnectionInformationSettings;
import org.knime.cloud.core.util.ConnectionInformationCloudComponents;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentAuthentication;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelAuthentication.AuthenticationType;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.util.Pair;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

/**
 * Copy of final class in cloud repo to override the auth methods
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class S3DialogComponents extends ConnectionInformationCloudComponents<AWSConnectionInformationSettings> {

    private final DialogComponentStringSelection m_region;

    private final DialogComponentBoolean m_sSEncrpytion;

    private final DialogComponentBoolean m_switchRole;

    private final DialogComponentString m_switchRoleAccount;

    private final DialogComponentString m_switchRoleName;

    private final boolean m_useEncryptionDialog;

    /**
     * Creates the DialogComponents including the s3 specific region chooser.
     *
     * @param settings The corresponding {@link AWSConnectionInformationSettings}
     * @param nameMap The {@link HashMap} containing the names for the radio buttons for the authentication part
     */
    public S3DialogComponents(final AWSConnectionInformationSettings settings,
        final HashMap<AuthenticationType, Pair<String, String>> nameMap) {
        this(settings, nameMap, true);
    }

    /**
     * Creates the DialogComponents including the s3 specific region chooser.
     *
     * @param settings The corresponding {@link AWSConnectionInformationSettings}
     * @param nameMap The {@link HashMap} containing the names for the radio buttons for the authentication part
     * @param encryptionDialog whether to use the encryption dialog
     */
    public S3DialogComponents(final AWSConnectionInformationSettings settings,
        final HashMap<AuthenticationType, Pair<String, String>> nameMap, final boolean encryptionDialog) {
        super(settings, nameMap);
        final ArrayList<String> regions = loadRegions();
        m_region = new DialogComponentStringSelection(settings.getRegionModel(), "Region", regions, false);
        m_sSEncrpytion = encryptionDialog
            ? new DialogComponentBoolean(settings.getSSEncryptionModel(), "Use SSE (Server Side Encryption)") : null;
        m_switchRole = new DialogComponentBoolean(settings.getSwitchRoleModel(), "Switch Role");
        m_switchRoleAccount = new DialogComponentString(settings.getSwitchRoleAccountModel(), "Account: ", false, 20);
        m_switchRoleName = new DialogComponentString(settings.getSwitchRoleNameModel(), "Role:       ", false, 20);
        m_useEncryptionDialog = encryptionDialog;
    }

    private ArrayList<String> loadRegions() {
        final AWSConnectionInformationSettings model = getSettings();
        final ArrayList<String> regionNames = new ArrayList<>();
        final String prefix = model.getPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            for (final Regions regions : Regions.values()) {
                final Region region = Region.getRegion(regions);
                if (region.isServiceSupported(model.getPrefix())) {
                    final String reg = region.getName();
                    regionNames.add(reg);
                }
            }
        } else {
            Stream.of(Regions.values()).forEach(reg -> regionNames.add(reg.getName()));
        }
        return regionNames;
    }

    /**
     * Get the {@link JPanel} for the Cloud connector dialog
     *
     * @return The panel for the cloud connector dialog
     */
    @Override
    public JPanel getDialogPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(super.getDialogPanel(), gbc);
        return panel;
    }

    /**
     * Get the {@link JPanel} for the cloud connector encryption dialog.
     *
     * @return The panel for the cloud connector encryption dialog
     */
    public JPanel getEncryptionDialogPanel() {
        if (!m_useEncryptionDialog) {
            return null;
        }
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(m_sSEncrpytion.getComponentPanel(), gbc);
        return panel;
    }

    @Override
    protected JPanel getAuthenticationPanel() {
        final JPanel auth = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        auth.add(getAuthenticationComponent().getComponentPanel());
        gbc.gridy++;
        auth.add(getSwitchRolePanel(), gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++;
        auth.add(m_region.getComponentPanel(), gbc);
        return auth;
    }

    private JPanel getSwitchRolePanel() {
        final JPanel rolePanel = new JPanel(new GridBagLayout());
        rolePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Switch Role "));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        rolePanel.add(m_switchRole.getComponentPanel(), gbc);
        m_switchRole.getModel().addChangeListener(e -> updateEnabledStatus());
        gbc.gridy++;
        gbc.insets = new Insets(0, 30, 0, 0);
        rolePanel.add(m_switchRoleAccount.getComponentPanel(), gbc);
        gbc.gridy++;
        rolePanel.add(m_switchRoleName.getComponentPanel(), gbc);
        return rolePanel;
    }

    private void updateEnabledStatus() {
        m_switchRoleAccount.getModel().setEnabled(m_switchRole.isSelected());
        m_switchRoleName.getModel().setEnabled(m_switchRole.isSelected());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DialogComponentAuthentication defineAuthenticationComponent() {
        final DialogComponentAuthentication authComponent = new DialogComponentAuthentication(
            m_settings.getAuthenticationModel(), "Authentication", getNameMap(), AuthenticationType.USER_PWD,
            AuthenticationType.CREDENTIALS, AuthenticationType.KERBEROS, AuthenticationType.NONE);
        authComponent.setUsernameLabel("Access Key ID");
        authComponent.setPasswordLabel("Secret Key");
        return authComponent;
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs,
        final CredentialsProvider cp) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs, cp);
        m_region.loadSettingsFrom(settings, specs);
        if (m_useEncryptionDialog) {
            m_sSEncrpytion.loadSettingsFrom(settings, specs);
        }
        m_switchRole.loadSettingsFrom(settings, specs);
        m_switchRoleAccount.loadSettingsFrom(settings, specs);
        m_switchRoleName.loadSettingsFrom(settings, specs);

        updateEnabledStatus();
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        m_region.saveSettingsTo(settings);
        if (m_useEncryptionDialog) {
            m_sSEncrpytion.saveSettingsTo(settings);
        }
        m_switchRole.saveSettingsTo(settings);
        m_switchRoleAccount.saveSettingsTo(settings);
        m_switchRoleName.saveSettingsTo(settings);
    }
}
