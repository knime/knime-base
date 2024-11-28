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
package org.knime.time.node.convert.datetimetostring;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ComprehensiveDateTimeFormatProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.CompatibleColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.time.util.LocaleStateProvider;
import org.knime.time.util.ReplaceOrAppend;

/**
 * Settings for the date-time to string node
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class DateTimeToStringNodeSettings implements DefaultNodeSettings {

    @Widget(title = "Date&time columns", description = "Only the included columns will be converted.")
    @ChoicesWidget(choices = DateAndTimeColumnProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Output columns", description = """
            Depending on the selection, the selected columns will be \
            replaced or appended to the input table.
            """)
    @ValueSwitchWidget
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    ReplaceOrAppend m_appendOrReplace = ReplaceOrAppend.REPLACE;

    @Widget(title = "Output column suffix", description = "The selected columns will be appended to the input table.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    String m_outputColumnSuffix = " (String)";

    @Widget(title = "Locale", description = """
            The locale to use for formatting the date and time. \
            Names of months and days of the week will be translated \
            according to the selected locale.
            """)
    @ChoicesWidget(choicesProvider = LocaleStateProvider.class)
    String m_locale = Locale.getDefault().toLanguageTag();

    @Widget(title = "DateTime format", description = """
            A format string (defined by <a href="
            https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html
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
            """ + ComprehensiveDateTimeFormatProvider.DATE_FORMAT_LIST_FOR_DOCS)
    @DateTimeFormatPickerWidget()
    String m_format = "yyyy-MM-dd HH:mm:ss";

    DateTimeToStringNodeSettings() {
        this((DataTableSpec)null);
    }

    DateTimeToStringNodeSettings(final DefaultNodeSettingsContext context) {
        this(context.getDataTableSpec(0).orElse(null));
    }

    DateTimeToStringNodeSettings(final DataTableSpec spec) {
        if (spec != null) {
            m_columnFilter = new ColumnFilter(spec.stream() //
                .filter(DateAndTimeColumnProvider.IS_COMPATIBLE_COLUMN) //
                .map(DataColumnSpec::getName) //
                .toArray(String[]::new) //
            );
        }
    }

    static final class DateAndTimeColumnProvider extends CompatibleColumnChoicesProvider {
        /**
         * Supported column types
         */
        static final List<Class<? extends DataValue>> DATE_TIME_COLUMN_TYPES =
            List.of(LocalDateValue.class, LocalTimeValue.class, ZonedDateTimeValue.class, LocalDateTimeValue.class);

        static final Predicate<DataColumnSpec> IS_COMPATIBLE_COLUMN =
            c -> DATE_TIME_COLUMN_TYPES.stream().anyMatch(c.getType()::isCompatible);

        DateAndTimeColumnProvider() {
            super(DATE_TIME_COLUMN_TYPES);
        }
    }

}
