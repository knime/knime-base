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
 *   24 Jan 2024 (Steffen Fissler, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.constantvalue;

import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataCellFactory.FromSimpleString;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.IsSpecificStringCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
/**
 *
 * @author Steffen Fissler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class ConstantValueColumnNodeSettings implements DefaultNodeSettings {

    /**
     * Constructor for persistence and conversion from JSON.
     */
    public ConstantValueColumnNodeSettings() {
        System.out.println("START");
//        getAllDataTypes();
    }

    enum ColumnOptions {
        @Label("Replace")
        REPLACE,

        @Label("Append")
        APPEND;

        static class IsReplace extends OneOfEnumCondition<ColumnOptions> {

            @Override
            public ColumnOptions[] oneOf() {
                return new ColumnOptions[] {REPLACE};
            }

        }
    }


    static final class ReplacementColumns implements ChoicesProvider {
        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).map(DataTableSpec::getColumnNames).orElse(new String[0]);
        }
    }

    @Widget(title = "Column Settings")
    @ValueSwitchWidget()
    @Signal(condition = ColumnOptions.IsReplace.class)
    ColumnOptions m_colOptions = ColumnOptions.REPLACE;

    @Widget(title = "Replace", description = "Select a column which is replaced with the new constant value column.", hideTitle = true)
    @ChoicesWidget(choices = ReplacementColumns.class)
    @Effect(signals = ColumnOptions.IsReplace.class, type = EffectType.SHOW)
    String m_replacedColumn;

    @Widget(title = "Append", description = "Add the constant value column as a new column with the given name.", hideTitle = true)
    @Effect(signals = ColumnOptions.IsReplace.class, type = EffectType.HIDE)
    String m_newColumnName;


    // TODO Maybe change to give DataType array and us their classes as identifiers instead of Strings?
    private static final class SimpleDataTypes implements ChoicesProvider {
        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            return DataTypeRegistry.getInstance().availableDataTypes().stream()
                .filter(d -> {
                    DataCellFactory f = d.getCellFactory(null).orElse(null);
                    var n = d.getName();
                    // Former dialog did only accept date&time and no other temporal format
                    // It is rather complicated implementing them, so will continue to not allow them
                    var isUnacceptedDateFormat = n.equals("Local Date") || n.equals("Local Time") || n.equals("Period") || n.equals("Zoned Date Time");
                    return (f instanceof FromSimpleString) && !(isUnacceptedDateFormat);
            })
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .map(type -> type.toString())
            .toArray(String[]::new);//
        }
    }



    static final class IsDateAndTimeCondition extends IsSpecificStringCondition {

        @Override
        public String getValue() {
            return "Local Date Time";
        }

    }



    @Widget(title = "Data Type", description = "The data type of the new column.")
    @ChoicesWidget(choices = SimpleDataTypes.class)
    @Signal(condition = IsDateAndTimeCondition.class)
    String m_cellFactory = "";

    static class StringValuePersistor extends NodeSettingsPersistorWithConfigKey<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey("column-value")) {
                return settings.getString("column-value");
            }
            return settings.getString(getConfigKey());
        }

        @Override
        public void save(final String obj, final NodeSettingsWO settings) {
            settings.addString(getConfigKey(), obj);
        }

    }

    @Persist(customPersistor = StringValuePersistor.class)
    @Widget(title = "Value settings", description = "The constant value to be used.<br /> Note that double values need the '.' as decimal mark.")
    @Effect(signals = IsDateAndTimeCondition.class, type = EffectType.HIDE)
    String m_stringValue = "";


    @Persist(customPersistor = StringValuePersistor.class)
    @Widget(title = "Value settings", description = "The date.")
    @DateTimeWidget(showTime = true, showSeconds = true)
    @Effect(signals = IsDateAndTimeCondition.class, type = EffectType.SHOW)
    String m_dateAndTimeValue = "";

}
