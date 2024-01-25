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
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.IsSpecificStringCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Or;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

import com.fasterxml.jackson.annotation.JsonIgnore;
/**
 *
 * @author Steffen Fissler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class ConstantValueColumnNodeSettings implements DefaultNodeSettings {

    @JsonIgnore
    final String VALUE_DESCRIPTION = "In the left combo box you choose the datacell\n"
            + "            implementation\n"
            + "            of the\n"
            + "            column and in the text field the\n"
            + "            actual column\n"
            + "            value is entered. You can also choose a flow-variable to provide the value using the button on the right, \n"
            + "            however the datacell implementation configuration is still necessary. \n"
            + "            <br />\n"
            + "            <br />\n"
            + "\n"
            + "            <b>Note on Double values</b>\n"
            + "            <br />\n"
            + "            Make sure that you use the '.' as the decimal\n"
            + "            mark in a double value.\n"
            + "            <br />\n"
            + "            <br />\n"
            + "            <b>Note on Date formats</b>\n"
            + "            <br />\n"
            + "            Note, the date parser uses localization settings so in order to\n"
            + "            parse\n"
            + "            foreign language date formats you will need to either convert\n"
            + "            these\n"
            + "            formats to the localized representation manually, or change\n"
            + "            the\n"
            + "            localization of your system to match that of your data source.\n"
            + "            A\n"
            + "            format string as required by the\n"
            + "            <tt>java.text.SimpleDateFormat</tt>\n"
            + "            .\n"
            + "            <b>Examples:</b>\n"
            + "            <ul>\n"
            + "                <li>\"yyyy.MM.dd HH:mm:ss.SSS\" parses dates like \"2001.07.04\n"
            + "                    12:08:56.000\"\n"
            + "                </li>\n"
            + "                <li>\"yyyy-MM-dd'T'HH:mm:ss.SSSZ\" parses dates like\n"
            + "                    \"2001-07-04T12:08:56.235-0700\"\n"
            + "                </li>\n"
            + "            </ul>\n"
            + "            <b>Valid pattern elements are:</b>\n"
            + "            <ul>\n"
            + "                <li>G: era designator</li>\n"
            + "                <li>y: year</li>\n"
            + "                <li>M: month in year</li>\n"
            + "                <li>w: Week in year</li>\n"
            + "                <li>W: week in month</li>\n"
            + "                <li>D: Day in year</li>\n"
            + "                <li>d: day in month</li>\n"
            + "                <li>F: Day of week in month</li>\n"
            + "                <li>E: day in week</li>\n"
            + "                <li>a: Am/pm marker</li>\n"
            + "                <li>H: hour in day (0-23)</li>\n"
            + "                <li>k: hour in day (1-24)</li>\n"
            + "                <li>K: hour in am/pm (0-11)</li>\n"
            + "                <li>h: hour in am/pm (1-12)</li>\n"
            + "                <li>m: minute in hour</li>\n"
            + "                <li>s: Second in minute</li>\n"
            + "                <li>S: millisecond</li>\n"
            + "                <li>z: Timezone (General time zone)</li>\n"
            + "                <li>Z: RFC 822 time zone</li>\n"
            + "            </ul>";
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

    @Widget(title = "Value settings", description = VALUE_DESCRIPTION)
//    @DateTimeWidget(showTime = true, showSeconds = true)
    String m_value = ""; // TODO Maybe have another "Time Value Settings", which just g




    private static final class SimpleDataTypes implements ChoicesProvider {
        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            return DataTypeRegistry.getInstance().availableDataTypes().stream()
                .filter(d -> {
                    DataCellFactory f = d.getCellFactory(null).orElse(null);
                    var name = d.getName();
                    var isUnacceptedDateFormat = name.equals("Local Date") || name.equals("Local Time") || name.equals("Period") || name.equals("Zoned Date Time");

                    System.out.println("Data type");
                    System.out.println(d);
                    System.out.println(isUnacceptedDateFormat);
                    return (f instanceof FromSimpleString) && !(isUnacceptedDateFormat);
//                    return (f instanceof FromSimpleString) && !(f instanceof );
            })
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .map(type -> type.toString())
            .toArray(String[]::new);//
        }
    }


    static final class IsLocalDateCondition extends IsSpecificStringCondition {
        @Override
        public String getValue() {
            return "Local Date";
        }
    }

    static final class IsLocalDateTimeCondition extends IsSpecificStringCondition {
        @Override
        public String getValue() {
            return "Local Date Time";
        }
    }

    static final class IsLocalTimeCondition extends IsSpecificStringCondition {
        @Override
        public String getValue() {
            return "Local Time";
        }
    }

    // TODO Is that even a time thing?
    static final class IsPeriodCondition extends IsSpecificStringCondition {
        @Override
        public String getValue() {
            return "Period";
        }
    }

    static final class IsZonedDateTimeCondition extends IsSpecificStringCondition {
        @Override
        public String getValue() {
            return "Zoned Date Time";
        }
    }


    // FIXME Simplify sometime, when API allows for it
    @Widget(title = "Data Type", description = "The data type of the new column.")
    @ChoicesWidget(choices = SimpleDataTypes.class)
    @Signal(condition = IsLocalDateCondition.class)
    @Signal(condition = IsLocalDateTimeCondition.class)
    @Signal(condition = IsLocalTimeCondition.class)
    @Signal(condition = IsPeriodCondition.class)
    @Signal(condition = IsZonedDateTimeCondition.class)
    String m_cellFactory = "";

    @Widget(title = "Date Format")
    @Effect(operation = Or.class, signals = {IsLocalDateCondition.class, IsLocalDateTimeCondition.class, IsLocalTimeCondition.class, IsPeriodCondition.class, IsZonedDateTimeCondition.class}, type = EffectType.SHOW)
    String m_dateFormat = "";
}
