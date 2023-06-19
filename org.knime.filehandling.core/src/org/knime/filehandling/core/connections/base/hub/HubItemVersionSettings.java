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
 *   Nov 15, 2022 (bjoern): created
 */
package org.knime.filehandling.core.connections.base.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The settings class containing a flag whether to use a Item version or not, as well as the Item version to use.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class HubItemVersionSettings {

    private static final String CFG_ITEM_VERSION = "itemVersion";

    private String m_itemVersion = null; //NOSONAR

    private final List<ChangeListener> m_listeners = new ArrayList<>();

    /**
     * @return an empty {@link Optional}, if the "latest" version is set, a number (as String), if a specific version is
     *         set.
     */
    public Optional<String> getItemVersion() {
        return Optional.ofNullable(m_itemVersion);
    }

    /**
     * Sets the Item version to use.
     *
     * @param version A number (as String) to specify a concrete version, or null to set the "latest" version.
     */
    public void setItemVersion(final String version) {
        var oldVersion = m_itemVersion;
        m_itemVersion = StringUtils.isBlank(version) ? null : version;

        if (!Objects.equals(oldVersion, m_itemVersion)) {
            fireChangeListeners();
        }
    }

    /**
     * Adds change listener.
     *
     * @param listener The listener to add.
     */
    public void addChangeListener(final ChangeListener listener) {
        m_listeners.add(listener);
    }

    /**
     * Removes change listener.
     *
     * @param listener The listener to remove.
     */
    public void removeChangeListener(final ChangeListener listener) {
        m_listeners.remove(listener);//NOSONAR efficiency is not critical here.
    }

    private void fireChangeListeners() {
        var evt = new ChangeEvent(this);
        for (ChangeListener l : m_listeners) {
            l.stateChanged(evt);
        }
    }

    /**
     * Loads the settings.
     *
     * @param settings The settings object to load from.
     * @throws InvalidSettingsException Thrown when settings are missing and settings are required (see constructor).
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {// NOSONAR
        m_itemVersion = settings.getString(CFG_ITEM_VERSION);
        fireChangeListeners();
    }

    /**
     * Saves the settings.
     *
     * @param settings The settings object to save settings to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {// NOSONAR
        settings.addString(CFG_ITEM_VERSION, m_itemVersion);
    }

    /**
     * Validate the settings stored in the provided settings object without actually loading them.
     *
     * @param settings The settings object to validate.
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {// NOSONAR
        settings.getString(CFG_ITEM_VERSION);
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (StringUtils.isBlank(m_itemVersion)) {
            throw new InvalidSettingsException("No version selected");
        }
    }
}
