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
package org.knime.time.node.format.datetimeformatmanager;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import org.knime.base.node.viz.format.AlignmentSuggestionOption;
import org.knime.core.data.DataColumnSpec;
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
import org.knime.time.util.LocaleStateProvider;

/**
 * @author Tobias Kampmann
 */
@SuppressWarnings("restriction")
final class DateTimeFormatManagerNodeSettings implements DefaultNodeSettings {

    DateTimeFormatManagerNodeSettings() {
    }

    DateTimeFormatManagerNodeSettings(final DefaultNodeSettingsContext context) {
        var spec = context.getDataTableSpec(0);

        if (spec.isPresent()) {
            m_columnFilter = new ColumnFilter(spec.get().stream() //
                .filter(IS_COMPATIBLE_TYPE) //
                .map(DataColumnSpec::getName) //
                .toArray(String[]::new) //
            );
        }
    }

    @Widget(title = "Date&Time columns", description = "Only the included columns will be converted.")
    @ChoicesWidget(choices = DateAndTimeColumnProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Locale",
        description = "The locale to use for formatting the date and time. "
            + "Names of months and days of the week will be translated according to the selected locale.")
    @ChoicesWidget(choicesProvider = LocaleStateProvider.class)
    String m_locale = Locale.ENGLISH.toLanguageTag();

    @Widget(title = "Date&Time format", description = """
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
                <li>"GGGG: yyyy-MMMM-dd ' at 'HH:mm[:ss[.SSS]] 'in' VV[' [also known as 'zzzz']']"
                produces dates such as "Anno Domini: 2024-November-26  at 22:37:59.674 in Europe/Berlin
                [also known as Central European Standard Time]"
                </li>
            </ul>
            <b>Supported placeholders in the pattern are:</b>
            """ + ComprehensiveDateTimeFormatProvider.DATE_FORMAT_LIST_FOR_DOCS)
    @DateTimeFormatPickerWidget(formatProvider = ComprehensiveDateTimeFormatProvider.class)
    String m_format = "yyyy-MM-dd HH:mm:ss";

    @Widget(title = "Alignment suggestion",
        description = "To be considered by views like Table View, Tile View, and for Spreadsheet export.")
    @ValueSwitchWidget
    AlignmentSuggestionOption m_alignmentSuggestion = AlignmentSuggestionOption.LEFT;

    /**
     * Supported column types
     */
    public static final List<Class<? extends DataValue>> DATE_TIME_COLUMN_TYPES =
        List.of(LocalDateValue.class, LocalTimeValue.class, ZonedDateTimeValue.class, LocalDateTimeValue.class);

    private static final Predicate<DataColumnSpec> IS_COMPATIBLE_TYPE =
        spec -> DATE_TIME_COLUMN_TYPES.stream().anyMatch(spec.getType()::isCompatible);

    static final class DateAndTimeColumnProvider extends CompatibleColumnChoicesProvider {
        DateAndTimeColumnProvider() {
            super(DATE_TIME_COLUMN_TYPES);
        }
    }

}