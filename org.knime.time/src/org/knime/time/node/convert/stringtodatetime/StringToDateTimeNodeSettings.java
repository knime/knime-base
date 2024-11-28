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
 *   Nov 28, 2024 (Tobias Kampmann): created
 */
package org.knime.time.node.convert.stringtodatetime;

import java.util.List;

import org.knime.core.data.StringValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.CompatibleColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.time.util.ActionIfExtractionFails;
import org.knime.time.util.LocaleStateProvider;
import org.knime.time.util.ReplaceOrAppend;

/**
 * @author Tobias Kampmann
 */
@SuppressWarnings("restriction")
public class StringToDateTimeNodeSettings implements DefaultNodeSettings {

    @Widget(title = "DateTime columns", description = "Only the included columns will be converted.")
    @Persist(configKey = "col_select", customPersistor = LegacyColumnFilterPersistor.class)
    @ChoicesWidget(choices = StringColumnProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Section(title = "Type&Format")
    interface TypeFormatSection {
    }

    @Widget(title = "Type", description = "The type of the output column.")
    @Persist(configKey = "typeEnum")
    @Layout(TypeFormatSection.class)
    @ValueSwitchWidget
    Formats m_selectedType = Formats.LOCAL_DATE;

    @Widget(title = "Locale",
        description = "A <i>locale</i> can be chosen, which determines the language "
            + "and geographic region for terms such as months or weekdays.")
    @ChoicesWidget(choicesProvider = LocaleStateProvider.class)
    @Layout(TypeFormatSection.class)
    String m_locale;

    @Widget(title = "DateTime format",
        description = """
                A format string (defined by <a href="
                https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
                ">DateTimeFormatter</a>).
                <br />
                <b>Examples:</b>
                <ul>
                    <li>"yyyy.MM.dd HH:mm:ss.SSS" produces dates such as "2001.07.04 12:08:56.000"
                    </li>
                    <li>"yyyy-MM-dd'T'HH:mm:ss.SSSZ" produces dates such as "2001-07-04T12:08:56.235-0700"
                    </li>
                    <li>"yyyy-MM-dd'T'HH:mm:ss.SSSXXX'['VV']'" produces dates such as
                    "2001-07-04T12:08:56.235+02:00[Europe/Berlin]"
                    </li>
                </ul>
                <b>Supported placeholders in the pattern are:</b>
                <ul>
                    <li>G: era</li>
                    <li>u: year</li>
                    <li>y: year of era</li>
                    <li>D: day of year</li>
                    <li>M: month in year (context sensitive)</li>
                    <li>L: month in year (standalone form)</li>
                    <li>d: day of month</li>
                    <li>Q: quarter of year</li>
                    <li>q: quarter of year</li>
                    <li>Y: week based year</li>
                    <li>w: week of week based year</li>
                    <li>W: week of month</li>
                    <li>E: day of week</li>
                    <li>e: localized day of week</li>
                    <li>c: localized day of week</li>
                    <li>F: week of month</li>
                    <li>a: am/pm of day</li>
                    <li>h: clock hour of am/pm (1-12)</li>
                    <li>K: hour of am/pm (0-11)</li>
                    <li>k: clock hour of am/pm (1-24)</li>
                    <li>H: hour of day (0-23)</li>
                    <li>m: minute of hour</li>
                    <li>s: second of minute</li>
                    <li>S: fraction of second</li>
                    <li>A: milli of day</li>
                    <li>n: nano of second</li>
                    <li>N: nano of second</li>
                    <li>V: time zone ID</li>
                    <li>z: time zone name</li>
                    <li>O: localized zone offset</li>
                    <li>X zone offset ('Z' for zero)</li>
                    <li>x: zone offset</li>
                    <li>Z: zone offset</li>
                    <li>p: pad next</li>
                    <li>' : escape for text</li>
                    <li>'': single quote</li>
                    <li>[: optional section start</li>
                    <li>]: optional section end</li>
                </ul>
                """)
    @Persist(configKey = "date_format")
    @Layout(TypeFormatSection.class)
    String m_format = "yyyy-MM-dd HH:mm:ss";

    @Section(title = "Output")
    @After(TypeFormatSection.class)
    interface OutputSection {
    }

    @Widget(title = "Output columns",
        description = "Depending on the selection, the selected columns will be replaced "
            + "or appended to the input table.")
    @ValueSwitchWidget
    @Persist(customPersistor = ReplaceOrAppend.Persistor.class)
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    @Layout(OutputSection.class)
    ReplaceOrAppend m_appendOrReplace = ReplaceOrAppend.REPLACE;

    @Widget(title = "Output column suffix", description = "The selected columns will be appended to the input table.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    @Persist(configKey = "suffix")
    @Layout(OutputSection.class)
    String m_outputColumnSuffix = "(modified time)";

    @Widget(title = "If extraction fails",
        description = "If checked, the node will abort the execution and fail on errors. "
            + "If unchecked, missing values will be generated instead.")
    @ValueSwitchWidget
    @Persist(configKey = "cancel_on_fail", customPersistor = ActionIfExtractionFails.Persistor.class)
    @Layout(OutputSection.class)
    ActionIfExtractionFails m_onError = ActionIfExtractionFails.SET_MISSING;

    enum Formats {
            @Label(value = "Date", description = "The output column will be of type LocalDate.")
            LOCAL_DATE, //
            @Label(value = "Time", description = "The output column will be of type LocalTime.")
            LOCAL_TIME, //
            @Label(value = "DateTime", description = "The output column will be of type LocalDateTime.")
            LOCAL_DATE_TIME, //
            @Label(value = "DateTimeZone", description = "The output column will be of type ZonedDateTime.")
            ZONED_DATE_TIME,
    }

    static final class StringColumnProvider extends CompatibleColumnChoicesProvider {
        StringColumnProvider() {
            super(List.of(StringValue.class));
        }
    }

}
