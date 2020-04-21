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
 *   Mar 16, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location.settingsmodel;

import java.util.Objects;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.location.internal.FSLocationUtils;

/**
 * SettingsModel that stores an {@link FSLocation} object.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class SettingsModelFSLocation extends SettingsModel {

    private static final String MODEL_TYPE_ID = "SMID_FSLocation";

    private final String m_configName;

    private FSLocation m_location;

    /**
     * Constructor.
     *
     * @param configName the name of the settings model, must not be {@code null} or empty
     * @param defaultLocation the default location
     * @throws IllegalArgumentException if <b>configName</b> is {@code null} or empty or defaultLocation is {@code null}
     */
    public SettingsModelFSLocation(final String configName, final FSLocation defaultLocation) {
        CheckUtils.checkArgument(configName != null && !configName.equals(""),
            "The configName must be a non-empty string.");
        m_configName = configName;
        m_location = CheckUtils.checkArgumentNotNull(defaultLocation, "The default location must not be null.");
    }

    /**
     * Copy constructor. Creates a deep copy of {@link SettingsModelFSLocation toCopy}.
     *
     * @param toCopy the {@link SettingsModelFSLocation} to copy
     */
    private SettingsModelFSLocation(final SettingsModelFSLocation toCopy) {
        m_location = toCopy.m_location;
        m_configName = toCopy.m_configName;
    }

    /**
     * Retrieves the {@link FSLocation} stored in this settings model.
     *
     * @return the location stored by this settings model (may be {@code null})
     */
    public FSLocation getLocation() {
        return m_location;
    }

    /**
     * Sets the stored {@link FSLocation} to {@link FSLocation location}.
     *
     * @param location the new {@link FSLocation} (may be {@code null}
     */
    public void setLocation(final FSLocation location) {
        if (!Objects.equals(location, m_location)) {
            m_location = location;
            notifyChangeListeners();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelFSLocation createClone() {
        return new SettingsModelFSLocation(this);
    }

    @Override
    protected String getModelTypeID() {
        return MODEL_TYPE_ID;
    }

    @Override
    protected String getConfigName() {
        return m_configName;
    }

    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            setLocation(loadLocation(settings));
        } catch (InvalidSettingsException ex) {
            // keep the old value
        }

    }

    private FSLocation loadLocation(final NodeSettingsRO settings) throws InvalidSettingsException {
        return FSLocationUtils.load(settings.getNodeSettings(m_configName));
    }

    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        loadLocation(settings);
    }

    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setLocation(loadLocation(settings));
    }

    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        FSLocationUtils.save(getLocation(), settings.addNodeSettings(m_configName));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + m_configName + ")";
    }

}
