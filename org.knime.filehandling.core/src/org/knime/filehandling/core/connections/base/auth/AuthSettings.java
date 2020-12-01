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
 *   Dec 1, 2020 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.core.connections.base.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Node settings class to handle all authentication related node settings of a connector node.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 */
public final class AuthSettings {

    /**
     * Key recommended to be used by connector nodes to store auth settings.
     */
    public static final String KEY_AUTH = "auth";

    private static final String KEY_AUTH_TYPE = "authType";

    private final Map<AuthType, AuthProviderSettings> m_providerSettings;

    private final SettingsModelString m_authType;

    private final AuthType m_defaultAuthType;

    /**
     * Builder for {@link AuthSettings} instances.
     *
     * @author Bjoern Lohrmann, KNIME GmbH
     */
    public static class Builder {
        private final Map<AuthType, AuthProviderSettings> m_buildingProviderSettings = new HashMap<>();

        private AuthType m_builderDefaultAuthType = null;

        /**
         * Add a new auth type.
         *
         * @param authType
         * @param providerSettings
         * @return this builder instance.
         */
        public Builder add(final AuthType authType, final AuthProviderSettings providerSettings) {
            if (m_buildingProviderSettings.containsKey(authType)) {
                throw new IllegalArgumentException("Already contains auth type " + authType.toString());
            }

            m_buildingProviderSettings.put(authType, providerSettings);
            return this;
        }

        /**
         * Sets the default auth type.
         *
         * @param authType
         * @return this builder instance.
         */
        public Builder defaultType(final AuthType authType) {
            CheckUtils.checkArgument(m_buildingProviderSettings.containsKey(authType),
                "Unknown default auth type" + authType.toString());
            m_builderDefaultAuthType = authType;
            return this;
        }

        /**
         * @return a new {@link AuthSettings} instance.
         */
        public AuthSettings build() {
            CheckUtils.checkArgumentNotNull(m_builderDefaultAuthType, "No default auth type set");
            return new AuthSettings(m_buildingProviderSettings, m_builderDefaultAuthType);
        }
    }

    private AuthSettings(final Map<AuthType, AuthProviderSettings> providerSettings, final AuthType defaultAuthType) {
        m_providerSettings = providerSettings;
        m_defaultAuthType = defaultAuthType;
        m_authType = new SettingsModelString(KEY_AUTH_TYPE, m_defaultAuthType.getSettingsKey());
        updateEnabledness();
    }

    /**
     * @return The auth type of these auth settings.
     */
    public AuthType getAuthType() {
        return m_providerSettings.keySet() // NOSONAR existence is checked elsewhere
            .stream() //
            .filter(authType -> authType.getSettingsKey().equalsIgnoreCase(m_authType.getStringValue())) //
            .findFirst() //
            .get();
    }

    /**
     * Updates the current {@link AuthType}.
     *
     * @param authType
     */
    public void setAuthType(final AuthType authType) {
        m_authType.setStringValue(authType.getSettingsKey());
        updateEnabledness();
    }

    /**
     * Provides the {@link AuthProviderSettings} for the given auth type.
     *
     * @param <T> The concrete subclass of {@link AuthProviderSettings} to expect.
     * @param authType
     * @return the node settings of the given auth type.
     */
    @SuppressWarnings("unchecked")
    public <T extends AuthProviderSettings> T getSettingsForAuthType(final AuthType authType) {
        return (T)m_providerSettings.get(authType);
    }

    private void updateEnabledness() {
        final AuthType currAuthType = getAuthType();

        for (AuthProviderSettings provSettings : m_providerSettings.values()) {
            provSettings.setEnabled(provSettings.getAuthType().equals(currAuthType));
        }
    }

    /**
     * Validates the provided specs against the settings and either provides warnings via the
     * <b>statusMessageConsumer</b> if the issues are non fatal or throws an InvalidSettingsException if the current
     * configuration and the provided specs make a successful execution impossible.
     *
     * @param specs the input {@link PortObjectSpec specs} of the node
     * @param statusMessageConsumer consumer for status messages e.g. warnings
     * @param credentialsProvider The current credentials provider.
     * @throws InvalidSettingsException if the specs are not compatible with the settings
     */
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer, final CredentialsProvider credentialsProvider)
        throws InvalidSettingsException {

        for (AuthProviderSettings provSettings : m_providerSettings.values()) {
            provSettings.configureInModel(specs, statusMessageConsumer, credentialsProvider);
        }
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the dialog).
     *
     * @param authSettingsParent Parent node for these auth settings.
     * @param specs
     * @param providerPanels
     * @throws NotConfigurableException
     */
    public void loadSettingsForDialog(final NodeSettingsRO authSettingsParent, final PortObjectSpec[] specs,
        final Map<AuthType, AuthProviderPanel<?>> providerPanels) throws NotConfigurableException {

        try {
            load(authSettingsParent);

            for (AuthProviderSettings provSettings : m_providerSettings.values()) {
                final AuthType curr = provSettings.getAuthType();
                final NodeSettingsRO provSettingsRO = authSettingsParent.getNodeSettings(curr.getSettingsKey());

                final AuthProviderPanel<?> currPanel =  providerPanels.get(curr);
                CheckUtils.checkArgument(provSettings == currPanel.getSettings(), "Panel is using different settings.");

                currPanel.loadSettingsForDialog(provSettingsRO, specs);
            }
        } catch (InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage(), ex);
        }

        updateEnabledness();
    }

    private void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_authType.loadSettingsFrom(settings);
    }

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the node model).
     *
     * @param authSettingsParent Parent node for these auth settings.
     * @throws InvalidSettingsException
     */
    public void loadSettingsForModel(final NodeSettingsRO authSettingsParent) throws InvalidSettingsException {
        load(authSettingsParent);

        for (AuthProviderSettings provSettings : m_providerSettings.values()) {
            provSettings
                .loadSettingsForModel(authSettingsParent.getNodeSettings(provSettings.getAuthType().getSettingsKey()));
        }

        updateEnabledness();
    }

    /**
     * Validates the settings in the given {@link NodeSettingsRO}.
     *
     * @param authSettingsParent Parent node for these auth settings.
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO authSettingsParent) throws InvalidSettingsException {
        m_authType.validateSettings(authSettingsParent);

        for (AuthProviderSettings provSettings : m_providerSettings.values()) {
            provSettings
                .validateSettings(authSettingsParent.getNodeSettings(provSettings.getAuthType().getSettingsKey()));
        }
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (StringUtils.isBlank(m_authType.getStringValue())) {
            throw new InvalidSettingsException("Auth type not set");
        }

        try {
            getAuthType();
        } catch (NoSuchElementException e) { // NOSONAR handled appropriately
            throw new InvalidSettingsException("Unknown auth type: " + m_authType.getStringValue());
        }

        for (AuthProviderSettings provSettings : m_providerSettings.values()) {
            provSettings.validate();
        }
    }

    /**
     * Saves the settings (to be called by node dialog).
     *
     * @param authSettingsParent Parent node for these auth settings.
     * @param providerPanels
     * @throws InvalidSettingsException
     */
    public void saveSettingsForDialog(final NodeSettingsWO authSettingsParent, final Map<AuthType, AuthProviderPanel<?>> providerPanels) throws InvalidSettingsException {
        save(authSettingsParent);

        for (AuthProviderSettings provSettings : m_providerSettings.values()) {
            final AuthType curr = provSettings.getAuthType();
            final NodeSettingsWO providerSettingsWO = authSettingsParent.addNodeSettings(curr.getSettingsKey());

            final AuthProviderPanel<?> currPanel =  providerPanels.get(curr);
            CheckUtils.checkArgument(provSettings == currPanel.getSettings(), "Panel is using different settings.");

            // in the dialog, the panel loads/saves settings, not the settings object
            currPanel.saveSettingsForDialog(providerSettingsWO);
        }
    }

    private void save(final NodeSettingsWO nodeSettings) {
        m_authType.saveSettingsTo(nodeSettings);
    }

    /**
     * Saves the settings (to be called by node model).
     *
     * @param authSettingsParent Parent node for these auth settings.
     */
    public void saveSettingsForModel(final NodeSettingsWO authSettingsParent) {
        save(authSettingsParent);

        for (AuthProviderSettings provSettings : m_providerSettings.values()) {
            provSettings
                .saveSettingsForModel(authSettingsParent.addNodeSettings(provSettings.getAuthType().getSettingsKey()));
        }
    }

    /**
     * @return a (deep) clone of this node settings object.
     */
    public AuthSettings createClone() {
        final Map<AuthType, AuthProviderSettings> providerSettings = new HashMap<>();
        for (AuthProviderSettings provSettings : m_providerSettings.values()) {
            providerSettings.put(provSettings.getAuthType(), provSettings.createClone());
        }

        return new AuthSettings(providerSettings, m_defaultAuthType);
    }
}
