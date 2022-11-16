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
 *   Sep 24, 2022 (Alexander Bondaletov): created
 */
package org.knime.filehandling.core.connections.base.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The settings object containing Hub Space ID and Space Name.
 *
 * @author Alexander Bondaletov
 */
public class HubSpaceSettings {

    private static final String CFG_SPACE_ID = "spaceId";

    private static final String CFG_SPACE_NAME = "spaceName";

    private final boolean m_requireSettings;

    private String m_spaceId = "";

    private String m_spaceName = "";

    private final List<ChangeListener> m_listeners = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param requireSettings If true, then {@link #loadSettingsFrom(NodeSettingsRO)} and
     *            {@link #validateSettings(NodeSettingsRO)} will fail, if settings are not present, otherwise they will
     *            not fail and use default values. This flag controls backwards compatibility, when added to an existing
     *            node.
     */
    public HubSpaceSettings(final boolean requireSettings) {
        m_requireSettings = requireSettings;
    }

    /**
     * @return the spaceId
     */
    public String getSpaceId() {
        return m_spaceId;
    }

    /**
     * @return the spaceName
     */
    public String getSpaceName() {
        return m_spaceName;
    }

    /**
     * Sets spaceId and SpaceName.
     *
     * @param spaceId The space ID.
     * @param spaceName The space name.
     */
    public void set(final String spaceId, final String spaceName) {
        var prevSpaceId = m_spaceId;

        m_spaceId = Optional.ofNullable(spaceId).orElse("");
        m_spaceName = Optional.ofNullable(spaceName).orElse("");

        if (!prevSpaceId.equals(m_spaceId)) {
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
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (m_requireSettings) {
            set(settings.getString(CFG_SPACE_ID), settings.getString(CFG_SPACE_NAME));
        } else {
            set(settings.getString(CFG_SPACE_ID, ""), settings.getString(CFG_SPACE_NAME, ""));
        }
    }

    /**
     * Saves the settings.
     *
     * @param settings The settings object to save settings to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_SPACE_ID, m_spaceId);
        settings.addString(CFG_SPACE_NAME, m_spaceName);
    }

    /**
     * Validate the settings stored in the provided settings object without actually loading them.
     *
     * @param settings The settings object to validate.
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (m_requireSettings) {
            settings.getString(CFG_SPACE_ID);
            settings.getString(CFG_SPACE_NAME);
        }
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (StringUtils.isBlank(m_spaceId)) {
            throw new InvalidSettingsException("No Hub Space selected");
        }
    }
}
