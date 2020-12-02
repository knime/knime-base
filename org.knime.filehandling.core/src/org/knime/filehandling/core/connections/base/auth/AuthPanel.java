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
 *   2020-08-07 (Vyacheslav Soldatov): created
 */

package org.knime.filehandling.core.connections.base.auth;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * @author Bjoern Lohrmann, KNIME GmbH
 */
@SuppressWarnings("serial")
public class AuthPanel extends JPanel {

    private static final int LEFT_INSET = 15;

    private final AuthSettings m_settings; // NOSONAR we are not using serialization

    private final ButtonGroup m_authTypeGroup = new ButtonGroup();

    private final Map<AuthType, JRadioButton> m_typeRadioButtons;
    private final Map<AuthType, AuthProviderPanel<?>> m_providerPanels;

    /**
     * Constructor.
     *
     * @param settings SSH authentication settings.
     * @param authProviderPanels The auth provider panels to display (they are displayed in the order of the list).
     */
    public AuthPanel(final AuthSettings settings, final List<AuthProviderPanel<?>> authProviderPanels) {
        super(new BorderLayout());
        m_settings = settings;

        m_typeRadioButtons = new HashMap<>();
        m_providerPanels = new HashMap<>();

        for (AuthProviderPanel<?> panel : authProviderPanels) {
            final AuthType authType = panel.getAuthType();
            final JRadioButton radioButton = createAuthTypeButton(authType, m_authTypeGroup);
            radioButton.addActionListener(makeListener(radioButton, authType));
            m_typeRadioButtons.put(authType, radioButton);
            m_providerPanels.put(authType, panel);
        }

        initLayout(authProviderPanels);
    }

    private static JRadioButton createAuthTypeButton(final AuthType type, final ButtonGroup group) {
        final JRadioButton button = new JRadioButton(type.getText());
        button.setToolTipText(type.getTooltip());
        group.add(button);
        return button;
    }

    private void initLayout(final List<AuthProviderPanel<?>> authProviderPanels) {
        setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (AuthProviderPanel<?> panel : authProviderPanels) {
            gbc.insets = new Insets(0, 5, 0, 5);
            add(m_typeRadioButtons.get(panel.getAuthType()), gbc);

            gbc.gridy++;
            gbc.insets = new Insets(0, LEFT_INSET, 0, 5);
            add(panel, gbc);

            gbc.gridy++;
        }
    }

    private ActionListener makeListener(final JRadioButton radioButton, final AuthType authType) {
        return e -> {
            if (radioButton.isSelected()) {
                m_settings.setAuthType(authType);
                updateComponentsEnablement();
            }
        };
    }

    private void updateComponentsEnablement() {
        final AuthType authType = m_settings.getAuthType();
        for (AuthProviderPanel<?> panel : m_providerPanels.values()) {
            panel.setEnabled(panel.getAuthType() == authType);
        }
    }

    /**
     * Initializes UI from settings.
     **/
    public void onSettingsLoaded() {
        // init from settings
        final AuthType authType = m_settings.getAuthType();
        m_typeRadioButtons.get(authType).setSelected(true);
        updateComponentsEnablement();
    }

    /**
     * Method which should be called in the onClose method of the node dialog.
     */
    public void onClose() {
        for (AuthProviderPanel<?> panel : m_providerPanels.values()) {
            panel.onClose();
        }
    }

    /**
     * @param authSettings
     * @param specs
     * @throws NotConfigurableException
     */
    public void loadSettingsFrom(final NodeSettingsRO authSettings, final PortObjectSpec[] specs)
            throws NotConfigurableException {

        m_settings.loadSettingsForDialog(authSettings, specs, m_providerPanels);
        onSettingsLoaded();
    }

    /**
     * Saves settings to the given {@link NodeSettingsWO}.
     *
     * @param authSettings
     * @throws InvalidSettingsException
     */
    public void saveSettingsTo(final NodeSettingsWO authSettings) throws InvalidSettingsException {
        m_settings.saveSettingsForDialog(authSettings, m_providerPanels);
    }
}
