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
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFlowVariableNameSelection2;
import org.knime.core.node.defaultnodesettings.DialogComponentPasswordField;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Authentication settings dialog panel to be used with {@link IDWithSecretAuthProviderSettings}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
@SuppressWarnings("serial")
public class IDWithSecretAuthProviderPanel extends AuthProviderPanel<IDWithSecretAuthProviderSettings> {
    private static final int LEFT_INSET = 23;

    private final JLabel m_idLabel;

    private final JLabel m_secretLabel;

    private DialogComponentBoolean m_useCredentials; // NOSONAR not using serialization

    private DialogComponentFlowVariableNameSelection2 m_credentialsFlowVarChooser; // NOSONAR not using serialization

    private DialogComponentString m_id; // NOSONAR not using serialization

    private DialogComponentPasswordField m_secret; // NOSONAR not using serialization

    private final Supplier<Map<String, FlowVariable>> m_flowVariablesSupplier; // NOSONAR not using serialization

    private boolean m_ignoreEvents = false;

    /**
     * Constructor.
     *
     * @param settings Authentication settings.
     * @param credentialsSupplier The supplier of {@link CredentialsProvider} (required by flow variable
     * dialog component to list all credentials flow variables).
     */
    public IDWithSecretAuthProviderPanel(final IDWithSecretAuthProviderSettings settings,
        final Supplier<CredentialsProvider> credentialsSupplier) {
        this(settings, credentialsSupplier, "ID", "Secret");
    }

    /**
     * Constructor.
     *
     * @param settings Authentication settings.
     * @param credentialsSupplier The supplier of {@link CredentialsProvider} (required by flow variable
     * dialog component to list all credentials flow variables).
     * @param idLabel custom ID field label
     * @param secretLabel custom secret field label
     */
    public IDWithSecretAuthProviderPanel(final IDWithSecretAuthProviderSettings settings,
        final Supplier<CredentialsProvider> credentialsSupplier, final String idLabel, final String secretLabel) {

        super(new GridBagLayout(), settings);

        m_flowVariablesSupplier = () -> CredentialsFlowVariableUtil.fetchFlowVariables(credentialsSupplier.get());
        m_idLabel = new JLabel(idLabel + ":");
        m_secretLabel = new JLabel(secretLabel + ":");
        initFields();
        initLayout();
    }

    private void initLayout() {
        final var gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        add(m_idLabel, gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        add(m_id.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(Box.createHorizontalGlue(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
        add(m_secretLabel, gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 0, 0, 5);
        add(m_secret.getComponentPanel(), gbc);

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
        m_id = new DialogComponentString(getSettings().getIDModel(), "", false, 45);
        m_secret = new DialogComponentPasswordField(getSettings().getSecretModel(), "", 45);

        getSettings().getUseCredentialsModel().addChangeListener(e -> updateComponentsEnablement());
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void updateComponentsEnablement() {
        if (m_ignoreEvents) {
            return;
        }

        m_ignoreEvents = true;
        m_idLabel.setEnabled(isEnabled() && !getSettings().useCredentials());
        m_secretLabel.setEnabled(isEnabled() && !getSettings().useCredentials());

        if (m_flowVariablesSupplier.get().isEmpty()) {
            getSettings().getUseCredentialsModel().setBooleanValue(false);
            m_useCredentials.setEnabled(false);
        } else {
            m_useCredentials.setEnabled(isEnabled());
        }
        m_ignoreEvents = false;
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
        updateComponentsEnablement();
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
