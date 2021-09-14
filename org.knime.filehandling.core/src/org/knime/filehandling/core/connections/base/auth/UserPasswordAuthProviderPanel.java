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
 *   2020-10-15 (Vyacheslav Soldatov): created
 */

package org.knime.filehandling.core.connections.base.auth;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.JLabel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFlowVariableNameSelection2;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.CredentialsType;

/**
 * Authentication settings dialog panel.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
@SuppressWarnings("serial")
public class UserPasswordAuthProviderPanel extends AuthProviderPanel<UserPasswordAuthProviderSettings> {
    private static final int LEFT_INSET = 23;

    private final JLabel m_usernameLabel;

    private final JLabel m_passwordLabel;

    private DialogComponentBoolean m_useCredentials; // NOSONAR not using serialization

    private DialogComponentFlowVariableNameSelection2 m_credentialsFlowVarChooser; // NOSONAR not using serialization

    private DialogComponentString m_username; // NOSONAR not using serialization

    private DialogComponentPasswordField m_password; // NOSONAR not using serialization

    private final Supplier<Map<String, FlowVariable>> m_flowVariablesSupplier; // NOSONAR not using serialization

    /**
     * Constructor.
     *
     * @param settings Authentication settings.
     * @param parentDialog The parent dialog pane (required by flow variable dialog component to list all flow
     *            variables).
     */
    public UserPasswordAuthProviderPanel(final UserPasswordAuthProviderSettings settings,
        final NodeDialogPane parentDialog) {
        this(settings, parentDialog, "Username", "Password");
    }

    /**
     * Constructor.
     *
     * @param settings Authentication settings.
     * @param parentDialog The parent dialog pane (required by flow variable dialog component to list all flow
     *            variables).
     * @param usernameLabel custom username field label
     * @param passwordLabel custom password field label
     *
     */
    public UserPasswordAuthProviderPanel(final UserPasswordAuthProviderSettings settings,
        final NodeDialogPane parentDialog, final String usernameLabel, final String passwordLabel) {

        super(new GridBagLayout(), settings);
        m_flowVariablesSupplier = () -> parentDialog.getAvailableFlowVariables(CredentialsType.INSTANCE);
        m_usernameLabel = new JLabel(usernameLabel + ":");
        m_passwordLabel = new JLabel(passwordLabel + ":");
        initFields();
        initLayout();
    }

    private void initLayout() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        add(m_usernameLabel, gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        add(m_username.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        add(m_passwordLabel, gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        add(m_password.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, LEFT_INSET - 5, 0, 5);
        add(m_useCredentials.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        add(m_credentialsFlowVarChooser.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(Box.createHorizontalGlue(), gbc);
    }

    private void initFields() {
        m_useCredentials = new DialogComponentBoolean(getSettings().getUseCredentialsModel(), "Use credentials:");
        m_credentialsFlowVarChooser = new DialogComponentFlowVariableNameSelection2(
            getSettings().getCredentialsNameModel(), "", m_flowVariablesSupplier);
        m_username = new DialogComponentString(getSettings().getUserModel(), "", false, 45);
        m_password = new DialogComponentPasswordField(getSettings().getPasswordModel(), "", 45);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void updateComponentsEnablement() {
        m_usernameLabel.setEnabled(isEnabled() && !getSettings().useCredentials());
        m_passwordLabel.setEnabled(isEnabled() && !getSettings().useCredentials());

        if (m_flowVariablesSupplier.get().isEmpty()) {
            getSettings().getUseCredentialsModel().setBooleanValue(false);
            m_useCredentials.setEnabled(false);
        } else {
            m_useCredentials.setEnabled(isEnabled());
        }
    }

    /**
     * @param settings
     * @param specs
     * @throws NotConfigurableException
     */
    @Override
    protected void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {

        m_credentialsFlowVarChooser.loadSettingsFrom(settings, specs);

        // for some reason we need to do this, otherwise DialogComponentBoolean does not properly
        // display the enabledness of the underlying SettingsModelBoolean
        m_useCredentials.loadSettingsFrom(settings, specs);
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    @Override
    protected void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_credentialsFlowVarChooser.saveSettingsTo(settings);
    }
}
