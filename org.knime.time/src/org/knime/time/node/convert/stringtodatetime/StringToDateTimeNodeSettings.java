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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.List;
import java.util.Locale;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ComprehensiveDateTimeFormatProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DateTimeFormatPickerWidget;
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

    StringToDateTimeNodeSettings() {
    }

    StringToDateTimeNodeSettings(final DefaultNodeSettingsContext context) {
        var spec = context.getDataTableSpec(0);

        if (spec.isPresent()) {
            m_columnFilter = new ColumnFilter(spec.get().stream() //
                .filter(cs -> cs.getType().isCompatible(StringValue.class)) //
                .map(DataColumnSpec::getName) //
                .toArray(String[]::new) //
            );
        }
    }

    @Widget(title = "DateTime columns", description = "Only the included columns will be converted.")
    @ChoicesWidget(choices = StringColumnProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Section(title = "Type&Format")
    interface TypeFormatSection {
    }

    @Widget(title = "Type", description = "The type of the output column.")
    @Layout(TypeFormatSection.class)
    @ValueSwitchWidget
    TemporalType m_selectedType = TemporalType.LOCAL_DATE;

    @Widget(title = "Locale",
        description = "A <i>locale</i> can be chosen, which determines the language "
            + "and geographic region for terms such as months or weekdays.")
    @ChoicesWidget(choicesProvider = LocaleStateProvider.class)
    @Layout(TypeFormatSection.class)
    String m_locale = Locale.getDefault().toLanguageTag();

    @Widget(title = "DateTime format", description = """
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
            """ + ComprehensiveDateTimeFormatProvider.DATE_FORMAT_LIST_FOR_DOCS)
    @Layout(TypeFormatSection.class)
    @DateTimeFormatPickerWidget
    String m_format = "yyyy-MM-dd";

    @Section(title = "Output")
    @After(TypeFormatSection.class)
    interface OutputSection {
    }

    @Widget(title = "Output columns",
        description = "Depending on the selection, the selected columns will be replaced "
            + "or appended to the input table.")
    @ValueSwitchWidget
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    @Layout(OutputSection.class)
    ReplaceOrAppend m_appendOrReplace = ReplaceOrAppend.REPLACE;

    @Widget(title = "Output column suffix", description = "The selected columns will be appended to the input table.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    @Layout(OutputSection.class)
    String m_outputColumnSuffix = " (Date&Time)";

    @Widget(title = "If extraction fails",
        description = "If set to 'Fail', the node will abort the execution and fail on errors. "
            + "If unchecked, missing values will be generated instead.")
    @ValueSwitchWidget
    @Layout(OutputSection.class)
    ActionIfExtractionFails m_onError = ActionIfExtractionFails.SET_MISSING;

    enum TemporalType {
            @Label(value = "Date", description = "The output column will be of type LocalDate.")
            LOCAL_DATE(LocalDate::from, LocalDateCellFactory.TYPE), //
            @Label(value = "Time", description = "The output column will be of type LocalTime.")
            LOCAL_TIME(LocalTime::from, LocalTimeCellFactory.TYPE), //
            @Label(value = "DateTime", description = "The output column will be of type LocalDateTime.")
            LOCAL_DATE_TIME(LocalDateTime::from, LocalDateTimeCellFactory.TYPE), //
            @Label(value = "DateTimeZone", description = "The output column will be of type ZonedDateTime.")
            ZONED_DATE_TIME(ZonedDateTime::from, ZonedDateTimeCellFactory.TYPE);

        private final TemporalQuery<? extends TemporalAccessor> m_query;

        private final DataType m_cellType;

        TemporalType(final TemporalQuery<? extends TemporalAccessor> query, final DataType cellType) {
            m_query = query;
            m_cellType = cellType;
        }

        public TemporalQuery<? extends TemporalAccessor> getQuery() {
            return m_query;
        }

        public DataType getCellType() {
            return m_cellType;
        }
    }

    static final class StringColumnProvider extends CompatibleColumnChoicesProvider {
        StringColumnProvider() {
            super(List.of(StringValue.class));
        }
    }

}
