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

import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * Generic interface for classes that hold settings related to the authentication scheme of a connector node.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 */
public interface AuthProviderSettings {

    /**
     * Enables/disables the auth settings. To be used in dialogs to enable/disable UI components.
     *
     * @param enabled
     */
    void setEnabled(boolean enabled);

    /**
     * @return true if these auth settings are enabled, false otherwise.
     */
    boolean isEnabled();

    /**
     * @return The auth type of these auth settings.
     */
    AuthType getAuthType();

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
    void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer, final CredentialsProvider credentialsProvider)
        throws InvalidSettingsException;

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the dialog).
     *
     * @param settings Node settings to load from.
     * @throws NotConfigurableException
     */
    void loadSettingsForDialog(final NodeSettingsRO settings) throws NotConfigurableException;

    /**
     * Loads settings from the given {@link NodeSettingsRO} (to be called by the node model).
     *
     * @param settings Node settings to load from.
     * @throws InvalidSettingsException
     */
    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Validates the settings in the given {@link NodeSettingsRO}.
     *
     * @param settings Node settings to validate.
     * @throws InvalidSettingsException
     */
    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    void validate() throws InvalidSettingsException;

    /**
     * Saves the settings (to be called by node dialog).
     *
     * @param settings Node settings to save to.
     * @throws InvalidSettingsException
     */
    void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException;

    /**
     * Saves the settings (to be called by node model).
     *
     * @param settings Node settings to save to.
     */
    void saveSettingsForModel(final NodeSettingsWO settings);

    /**
     * @return a (deep) clone of this node settings object.
     */
    AuthProviderSettings createClone();
}
