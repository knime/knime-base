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

import java.awt.LayoutManager;

import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Abstract dialog panel for auth providers.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @param <T> The concrete {@link AuthProviderSettings} to display in the panel.
 */
@SuppressWarnings("serial")
public abstract class AuthProviderPanel<T extends AuthProviderSettings> extends JPanel {

    private final T m_settings; // NOSONAR we are not using serialization

    /**
     * @param layout
     * @param settings
     */
    public AuthProviderPanel(final LayoutManager layout, final T settings) {
        super(layout);
        m_settings = settings;
    }

    /**
     * @return the settings
     */
    public final T getSettings() {
        return m_settings;
    }

    /**
     * @return the authentication type for this panel.
     */
    public final AuthType getAuthType() {
        return m_settings.getAuthType();
    }

    @Override
    public final void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        m_settings.setEnabled(enabled);
        updateComponentsEnablement();
    }

    /**
     * Updates the enabledness of UI componenets, based on the current settings.
     */
    protected abstract void updateComponentsEnablement();

    /**
     * Initializes from settings.
     **/
    public void onOpen() {
        updateComponentsEnablement();
    }

    /**
     * Override this method if actions need to be performed when closing the dialog.
     */
    public void onClose() {
        // do nothing, can be overriden
    }

    /**
     * @param settings
     * @param specs
     * @throws NotConfigurableException
     */
    public final void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {

        m_settings.loadSettingsForDialog(settings);
        loadAdditionalSettingsFrom(settings, specs);
        updateComponentsEnablement();
    }

    /**
     * Load additional settings, e.g. settings that must be loaded with a {@link DialogComponent}..
     *
     * @param settings
     * @param specs
     * @throws NotConfigurableException
     */
    protected abstract void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException;

    /**
     * Saves settings to the given {@link NodeSettingsWO}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public final void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.saveSettingsForDialog(settings);
        saveAdditionalSettingsTo(settings);
    }

    /**
     * Saves additional settings, e.g. settings that must be saved with a {@link DialogComponent}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    protected abstract void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException;
}
