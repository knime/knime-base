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
import java.util.Optional;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import com.google.common.base.Objects;

/**
 * The settings class containing a flag whether to use a Space version or not, as well as the Space version to use.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class HubSpaceVersionSettings {

    private static final String CFG_USE_SPACE_VERSION = "useSpaceVersion";

    private static final String CFG_SPACE_VERSION = "spaceVersion";

    private boolean m_useSpaceVersion = false;

    private String m_spaceVersion = null;

    private final List<ChangeListener> m_listeners = new ArrayList<>();

    /**
     * @return true, if a Space version is set, false otherwise.
     */
    public boolean useSpaceVersion() {
        return m_useSpaceVersion;
    }

    /**
     * Sets whether a Space version shall be set or not.
     *
     * @param useSpaceVersion
     */
    public void setUseSpaceVersion(final boolean useSpaceVersion) {
        boolean changed = m_useSpaceVersion != useSpaceVersion;
        m_useSpaceVersion = useSpaceVersion;

        if (changed) {
            fireChangeListeners();
        }
    }

    /**
     * @return an empty {@link Optional}, if the "latest" version is set, a number (as String), if a specific version is
     *         set.
     */
    public Optional<String> getSpaceVersion() {
        return Optional.ofNullable(m_spaceVersion);
    }

    /**
     * Sets the Space version to use.
     *
     * @param version A number (as String) to specify a concrete version, or null to set the "latest" version.
     */
    public void setSpaceVersion(final String version) {
        var oldVersion = m_spaceVersion;
        m_spaceVersion = StringUtils.isBlank(version) ? null : version;

        if (!Objects.equal(oldVersion, m_spaceVersion)) {
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
        m_useSpaceVersion = settings.getBoolean(CFG_USE_SPACE_VERSION);
        m_spaceVersion = settings.getString(CFG_SPACE_VERSION);
        fireChangeListeners();
    }

    /**
     * Saves the settings.
     *
     * @param settings The settings object to save settings to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_USE_SPACE_VERSION, m_useSpaceVersion);
        settings.addString(CFG_SPACE_VERSION, m_spaceVersion);
    }

    /**
     * Validate the settings stored in the provided settings object without actually loading them.
     *
     * @param settings The settings object to validate.
     * @throws InvalidSettingsException
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_USE_SPACE_VERSION);
        settings.getString(CFG_SPACE_VERSION);
    }

    /**
     * Validates the current settings.
     *
     * @throws InvalidSettingsException
     */
    public void validate() throws InvalidSettingsException {
        if (m_useSpaceVersion && m_spaceVersion != null && m_spaceVersion.isEmpty()) {
            throw new InvalidSettingsException("No version selected");
        }
    }
}
