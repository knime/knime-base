/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.flowvariable.tablerowtovariable3;

import java.util.Locale;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Settings class to generate the node dialog.
 *
 * Output as variables (twinlist)
 *
 * If value is not available (switch, default=Ignore, Use defaults or fail, Fail)
 *
 * [Only if value is not available=Use defaults or fail]
 *
 * Default string (text input, default=”missing”) Default boolean (switch, True, default=False) Default integer
 * (spinner, default=0) Default long (spinner, default=0) Default double (spinner, default=0)
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
public final class TableToVariable3NodeSettings implements DefaultNodeSettings {

    /** The columns selected for conversion to a flow variable. */
    static final String CFG_KEY_COLUMNS = "column_selection";

    /** The policy to use in case of an empty input table or a missing value in the first row of the input table. */
    static final String CFG_KEY_ON_MISSING = "missing_value_policy";

    static final String CFG_KEY_DEFAULT_VALUE_DOUBLE = "default_value_double";

    static final String CFG_KEY_DEFAULT_VALUE_INTEGER = "default_value_integer";

    static final String CFG_KEY_DEFAULT_VALUE_LONG = "default_value_long";

    static final String CFG_KEY_DEFAULT_VALUE_STRING = "default_value_string";

    static final String CFG_KEY_DEFAULT_VALUE_BOOLEAN = "default_value_boolean";

    @Section(title = "Output as variables")
    interface OutputAsVariablesSection {
    }

    @Section(title = "Missing Values")
    @After(OutputAsVariablesSection.class)
    interface MissingValuesSection {
    }

    @Persist(configKey = CFG_KEY_COLUMNS, settingsModel = SettingsModelColumnFilter2.class)
    @Widget(title = "Output as variables", description = """
            Select the columns to be converted to flow variables. For each selected column, a flow variable
            is created. The name of the flow variable corresponds to the column name and the value corresponds
            to the value of the first row in that column.
            """)
    @ChoicesWidget(choices = AllColumns.class)
    @Layout(OutputAsVariablesSection.class)
    ColumnFilter m_valueColumns = new ColumnFilter();

    static class UsesDefaultMissingValuesPolicy extends OneOfEnumCondition<MissingValuePolicy> {

        @Override
        public MissingValuePolicy[] oneOf() {
            return new MissingValuePolicy[]{MissingValuePolicy.DEFAULT};
        }
    }

    @Persist(configKey = CFG_KEY_ON_MISSING, settingsModel = SettingsModelString.class)
    @Widget(title = "If value in cell is missing", description = """
            Behavior in case of missing values in the first row or an input table with no rows.
            <ul>
                <li>
                    <b>Fail:</b> Ensures that the node will fail during execution if the input table is empty
                    or contains missing values in the columns to convert.
                </li>
                <li>
                    <b>Use defaults:</b> Replaces missing values with the configured defaults.
                    If a column has a type for which no flow variable type exists, the default value for missing strings
                    will be output. If the column holds lists or sets of string,
                    boolean, double, integer, or long, an empty list of the according type will be output.
                    If the column holds lists or sets of another type, an empty string list will be output.
                </li>
                <li>
                    <b>Ignore:</b> Missing cells will not be converted and therefore be omitted from the output.
                </li>
            </ul>
            """)
    @Layout(MissingValuesSection.class)
    @Signal(condition=UsesDefaultMissingValuesPolicy.class)
    @ValueSwitchWidget
    MissingValuePolicy m_onMissing = MissingValuePolicy.OMIT;

    @Persist(configKey = CFG_KEY_DEFAULT_VALUE_STRING)
    @Widget(title = "Default string", description = """
            The default flow variable value for string columns in case of an empty input table
            or a missing value in the first row of the input table.
            """)
    @Layout(MissingValuesSection.class)
    @Effect(signals= {UsesDefaultMissingValuesPolicy.class}, type = EffectType.SHOW)
    String m_defaultValueString = "missing";

    // used to be string, keeping it like that for the sake of backwards compatibility
    @Persist(customPersistor = Persistor.class)
    @Widget(title = "Default boolean", description = """
            The default flow variable value for boolean columns in case of an empty input table
            or a missing value in the first row of the input table.
            """)
    @Layout(MissingValuesSection.class)
    @Effect(signals= {UsesDefaultMissingValuesPolicy.class}, type = EffectType.SHOW)
    @ValueSwitchWidget
    BooleanStringBridge m_defaultValueBoolean = BooleanStringBridge.FALSE;

    @Persist(configKey = CFG_KEY_DEFAULT_VALUE_INTEGER)
    @Widget(title = "Default integer", description = """
            The default flow variable value for integer columns in case of an empty input table
            or a missing value in the first row of the input table.
            """)
    @Layout(MissingValuesSection.class)
    @Effect(signals= {UsesDefaultMissingValuesPolicy.class}, type = EffectType.SHOW)
    int m_defaultValueInteger;

    @Persist(configKey = CFG_KEY_DEFAULT_VALUE_LONG)
    @Widget(title = "Default long", description = """
            The default flow variable value for long columns in case of an empty input table
            or a missing value in the first row of the input table.
            """)
    @Layout(MissingValuesSection.class)
    @Effect(signals= {UsesDefaultMissingValuesPolicy.class}, type = EffectType.SHOW)
    long m_defaultValueLong;

    @Persist(configKey = CFG_KEY_DEFAULT_VALUE_DOUBLE)
    @Widget(title = "Default double", description = """
            The default flow variable value for double columns in case of an empty input table
            or a missing value in the first row of the input table.
            """)
    @Layout(MissingValuesSection.class)
    @Effect(signals= {UsesDefaultMissingValuesPolicy.class}, type = EffectType.SHOW)
    double m_defaultValueDouble;

    /**
     * Constructor.
     */
    TableToVariable3NodeSettings() {

    }

    private static final class AllColumns implements ColumnChoicesProvider {
        @Override
        public DataColumnSpec[] columnChoices(final SettingsCreationContext context) {
            return context.getDataTableSpec(0)//
                .stream()//
                .flatMap(DataTableSpec::stream)//
                .toArray(DataColumnSpec[]::new);

        }
    }

    private static final class Persistor implements FieldNodeSettingsPersistor<BooleanStringBridge> {
        @Override
        public String[] getConfigKeys() {
            return new String[]{CFG_KEY_DEFAULT_VALUE_BOOLEAN};
        }

        @Override
        public BooleanStringBridge load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return BooleanStringBridge.valueOf(settings.getString(CFG_KEY_DEFAULT_VALUE_BOOLEAN)//
                .toUpperCase(Locale.ROOT));
        }

        @Override
        public void save(final BooleanStringBridge value, final NodeSettingsWO settings) {
            settings.addString(CFG_KEY_DEFAULT_VALUE_BOOLEAN, value.name().toLowerCase(Locale.ROOT));
        }
    }

    private enum BooleanStringBridge {
            @Label("true")
            TRUE, @Label("false")
            FALSE;
    }
}
