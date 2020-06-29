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
 */
package org.knime.time.util;

import java.time.ZoneId;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The {@link SettingsModel} for the default time zone dialog component ({@link DialogComponentTimeZoneSelection}). This
 * is a reduced version of {@link SettingsModelDateTime}.
 *
 * @author Sascha Wolke, KNIME GmbH
 * @since 4.2
 */
public final class SettingsModelTimeZone extends SettingsModel {
    private final String m_configName;

    private ZoneId m_zone;

    /**
     * @param configName the identifier the value is stored with in the {@link org.knime.core.node.NodeSettings} object
     * @param defaultZoneId the initial value, if <code>null</code> the system default is used
     */
    public SettingsModelTimeZone(final String configName, final ZoneId defaultZoneId) {
        if ((configName == null) || configName.isEmpty()) {
            throw new IllegalArgumentException("The configName must be a non-empty string");
        }

        m_configName = configName;
        if (defaultZoneId != null) {
            m_zone = defaultZoneId;
        } else {
            m_zone = ZoneId.systemDefault();
        }
    }

    /**
     * @return the represented time zone
     */
    public ZoneId getZone() {
        return m_zone;
    }

    /**
     * @param zone {@link ZoneId}
     */
    public void setZone(final ZoneId zone) {
        boolean sameValue;
        if (zone == null) {
            sameValue = (m_zone == null);
        } else {
            sameValue = zone.equals(m_zone);
        }
        m_zone = zone;

        if (!sameValue) {
            notifyChangeListeners();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelTimeZone createClone() {
        return new SettingsModelTimeZone(m_configName, m_zone);
    }

    @Override
    protected String getModelTypeID() {
        return "settingsmodel.timezone";
    }

    @Override
    protected String getConfigName() {
        return m_configName;
    }

    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            loadSettingsForModel(settings);
        } catch (InvalidSettingsException ise) {
            // load current date and time
            setZone(ZoneId.systemDefault());
        }
    }

    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final String string = settings.getString(m_configName);

        if (StringUtils.isEmpty(string) || string.equals("missing")) {
            // table row to variable returns "missing" for flow variables when the node isn't executed yet
            setZone(ZoneId.systemDefault());
        } else if (DateTimeUtils.asTimezone(string).isPresent()) {
            setZone(DateTimeUtils.asTimezone(string).get());
        } else {
            throw new InvalidSettingsException("'" + string + "' could not be parsed as a time zone.");
        }
    }

    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final String string = settings.getString(m_configName);
        if (!StringUtils.isEmpty(string) && !string.equals("missing")
            && !DateTimeUtils.asTimezone(string).isPresent()) {
            throw new InvalidSettingsException("'" + string + "' could not be parsed as a time zone.");
        }
    }

    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        settings.addString(m_configName, m_zone.toString());
    }

    @Override
    public String toString() {
        return m_zone.toString();
    }
}
