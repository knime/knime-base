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
 *   24 Jan 2024 (albrecht): created
 */
package org.knime.time.node.extract.datetime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation.Builder;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DefaultFieldNodeSettingsPersistorFactory;
import org.knime.time.node.extract.datetime.ExtractDateTimeFieldsSettings.DateTimeField;
import org.knime.time.node.extract.datetime.ExtractDateTimeFieldsSettings.ExtractField;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class DateTimeFieldsPersistor extends NodeSettingsPersistorWithConfigKey<ExtractField[]> {

    // used legacy keys (and subsecond values)
    static final String YEAR = "Year";
    static final String YEAR_WEEK_BASED = "Year (week-based)";
    static final String QUARTER = "Quarter";
    static final String MONTH_NUMBER = "Month (number)";
    static final String MONTH_NAME = "Month (name)";
    static final String WEEK = "Week";
    static final String DAY_OF_YEAR = "Day of year";
    static final String DAY_OF_MONTH = "Day of month";
    static final String DAY_OF_WEEK_NUMBER = "Day of week (number)";
    static final String DAY_OF_WEEK_NAME = "Day of week (name)";
    static final String HOUR = "Hour";
    static final String MINUTE = "Minute";
    static final String SECOND = "Second";
    static final String SUBSECOND = "Subsecond in";
    static final String SUBSECOND_UNITS = "subsecond_units";
    static final String MILLISECOND = "milliseconds";
    static final String MICROSECOND = "microseconds";
    static final String NANOSECOND = "nanoseconds";
    static final String TIME_ZONE_NAME = "Time zone name";
    static final String TIME_ZONE_OFFSET = "Time zone offset";

    private static String[] topLevelKeys = new String[] {
        YEAR, YEAR_WEEK_BASED, QUARTER, MONTH_NUMBER, MONTH_NAME, WEEK, DAY_OF_YEAR, DAY_OF_MONTH, DAY_OF_WEEK_NUMBER,
        DAY_OF_WEEK_NAME, HOUR, MINUTE, SECOND, SUBSECOND, TIME_ZONE_NAME, TIME_ZONE_OFFSET
    };

    private static Map<String, DateTimeField> fieldMap = new HashMap<>();
    static {
        fieldMap.put(YEAR, DateTimeField.YEAR);
        fieldMap.put(QUARTER, DateTimeField.QUARTER);
        fieldMap.put(MONTH_NUMBER, DateTimeField.MONTH_NUMBER);
        fieldMap.put(MONTH_NAME, DateTimeField.MONTH_NAME);
        fieldMap.put(WEEK, DateTimeField.WEEK);
        fieldMap.put(YEAR_WEEK_BASED, DateTimeField.YEAR_WEEK_BASED);
        fieldMap.put(DAY_OF_YEAR, DateTimeField.DAY_OF_YEAR);
        fieldMap.put(DAY_OF_MONTH, DateTimeField.DAY_OF_MONTH);
        fieldMap.put(DAY_OF_WEEK_NUMBER, DateTimeField.DAY_OF_WEEK_NUMBER);
        fieldMap.put(DAY_OF_WEEK_NAME, DateTimeField.DAY_OF_WEEK_NAME);
        fieldMap.put(HOUR, DateTimeField.HOUR);
        fieldMap.put(MINUTE, DateTimeField.MINUTE);
        fieldMap.put(SECOND, DateTimeField.SECOND);
        fieldMap.put(MILLISECOND, DateTimeField.MILLISECOND);
        fieldMap.put(MICROSECOND, DateTimeField.MICROSECOND);
        fieldMap.put(NANOSECOND, DateTimeField.NANOSECOND);
        fieldMap.put(TIME_ZONE_NAME, DateTimeField.TIME_ZONE_NAME);
        fieldMap.put(TIME_ZONE_OFFSET, DateTimeField.TIME_ZONE_OFFSET);
    }

    private NodeSettingsPersistor<ExtractField[]> m_persistor;

    @Override
    public void setConfigKey(final String configKey) {
        super.setConfigKey(configKey);
        m_persistor =
            DefaultFieldNodeSettingsPersistorFactory.createDefaultPersistor(ExtractField[].class, getConfigKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtractField[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (oldSettingsUsed(settings)) {
            return constructExtractFieldsFromLegacy(settings);
        } else {
            return m_persistor.load(settings);
        }
    }

    private static boolean oldSettingsUsed(final NodeSettingsRO settings) {
        for (String settingKey : topLevelKeys) {
            if (settings.containsKey(settingKey)) {
                return true;
            }
        }
        return false;
    }

    private static ExtractField[] constructExtractFieldsFromLegacy(final NodeSettingsRO settings) {
        List<ExtractField> fieldList = new ArrayList<>();
        // implicit order and names of output columns with legacy settings needs to be preserved
        for (String settingsKey : topLevelKeys) {
            // load with default as we detected if legacy keys are used in general beforehand
            if (settings.getBoolean(settingsKey, false)) {
                if (settingsKey.equals(SUBSECOND)) {
                    // subseconds were stored with unit as string setting
                    String subsecondUnit = settings.getString(SUBSECOND_UNITS, "");
                    DateTimeField field = fieldMap.get(subsecondUnit);
                    if (field != null) {
                        String colName = "Subsecond (in " + subsecondUnit + ")";
                        fieldList.add(new ExtractField(field, colName));
                    }
                } else {
                    // column name and legacy settings key was the same
                    fieldList.add(new ExtractField(fieldMap.get(settingsKey), settingsKey));
                }
            }
        }
        return fieldList.toArray(ExtractField[]::new);
    }

    @Override
    public ConfigsDeprecation[] getConfigsDeprecations() {
        Builder configBuilder =
            new ConfigsDeprecation.Builder().forNewConfigPath(getConfigKey());
        for (String settingKey : topLevelKeys) {
            configBuilder.forDeprecatedConfigPath(settingKey);
        }
        configBuilder.forDeprecatedConfigPath(SUBSECOND_UNITS);
        return new ConfigsDeprecation[]{configBuilder.build()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final ExtractField[] obj, final NodeSettingsWO settings) {
        m_persistor.save(obj, settings);
    }

}
