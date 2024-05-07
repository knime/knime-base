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
 *   22 Jan 2024 (albrecht): created
 */
package org.knime.time.node.extract.datetime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.StringToColumnSelectionPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesUpdateHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.CompatibleColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class ExtractDateTimeFieldsSettings implements DefaultNodeSettings {

    @Section(title = "Column selection")
    interface ColumnSection {}

    @Widget(title = "Date&Time column", description = "A Local Date, Local Time, Local Date Time or " +
            "Zoned Date Time column whose fields to extract.")
    @Persist(configKey = "col_select", customPersistor = StringToColumnSelectionPersistor.class)
    @ChoicesWidget(choices = DateTimeColumnChoices.class)
    @Layout(ColumnSection.class)
    public ColumnSelection m_selectedColumn = new ColumnSelection();

    @Section(title = "Date and time fields")
    @After(ColumnSection.class)
    interface DateTimeFieldsSection {}

    @Widget(title = "Extracted fields", description = "Define date or time fields to extract and set column names.")
    @Persist(customPersistor = DateTimeFieldsPersistor.class)
    @ArrayWidget(addButtonText = "Add field", showSortButtons = true)
    @Layout(DateTimeFieldsSection.class)
    public ExtractField[] m_extractFields = new ExtractField[0];

    @Section(title = "Localization (month and day names, etc.)")
    @After(DateTimeFieldsSection.class)
    interface LocaleSection {}

    @Widget(title = "Locale", description = "The locale that governs the localization of output strings "
            + "(month, day of week, time zone offset) and takes care of local calendrical characteristics "
            + "(week and day of week numbering).")
    @ChoicesWidget(choices = LocaleChoices.class)
    @Layout(LocaleSection.class)
    public String m_locale = getDefaultLocale().toLanguageTag();

    /**
     * The supported date and time types
     */
    public static final List<Class<? extends DataValue>> compatibleDateTimeColumns =
        List.of(LocalDateValue.class, LocalTimeValue.class, LocalDateTimeValue.class, ZonedDateTimeValue.class);

    static final class DateTimeColumnChoices extends CompatibleColumnChoicesProvider {
        DateTimeColumnChoices() {
            super(compatibleDateTimeColumns);
        }
    }

    static final class ExtractField implements DefaultNodeSettings {

        public ExtractField() { /* default constructor for serialization */ }

        public ExtractField(final DateTimeField field, final String columnName) {
            m_field = field;
            m_columnName = columnName;
        }

        @HorizontalLayout
        interface ExtractFieldLayout {}

        @Widget(title = "Field", description = "The type of field to extract.")
        @ChoicesWidget(choicesUpdateHandler = FilteredPossibleFieldsChoices.class)
        @Layout(ExtractFieldLayout.class)
        public DateTimeField m_field;

        @Widget(title= "Column name",
                description = "The name of the column populated with the values of the selected field.")
        @Layout(ExtractFieldLayout.class)
        public String m_columnName;
    }


    enum DateTimeField {

        @Label(value = "Year", description = "The year number will be extracted and appended as an integer column.")
        YEAR,

        @Label(value = "Year (Week-based)",
            description = "The year based on the week will be "
                + "extracted and appended as an integer column. Depending on the selected locale, week 1 of a "
                + " year may already start in the previous year, or week 52 of a year may last until the next "
                + " year (e.g., 30th Dec 2010 belongs to week 1 of year 2011 (locale en-US), so the extracted "
                + " <i>Year (week-based)</i> would be 2011 while the extracted <i>Year</i> would be 2010).")
        YEAR_WEEK_BASED,

        @Label(value = "Quarter",
            description = "If checked, the quarter of year will be extracted as a number in range  [1-4] and appended "
                + "as an integer column.")
        QUARTER,

        @Label(value = "Month (Number)",
            description = "If checked, the month of year will be extracted as a number in range [1-12] and appended "
                + "as an integer column.")
        MONTH_NUMBER,

        @Label(value = "Month (Name)",
            description = "If checked, the month of year will be extracted as a localized name and appended "
                + "as a string column.")
        MONTH_NAME,

        @Label(value = "Week",
            description = "If checked, the week of year will be extracted as a number in range [1-52] and appended as "
                + "an integer column. A partial week at the beginning of a year is handled according to the "
                + "chosen locale.")
        WEEK,

        @Label(value = "Day of Year",
            description = "If checked, the day of year will be extracted as a number in range [1-366] and appended "
                + "as an integer column.")
        DAY_OF_YEAR,

        @Label(value = "Day of Month",
            description = "If checked, the day of month will be extracted as a number in range [1-31] and appended "
                + "as an integer column.")
        DAY_OF_MONTH,

        @Label(value = "Day of Week (Number)",
            description = "If checked, the day of week will be extracted as a number in range [1-7] and appended as "
                + "an integer column. The numbering is based on the chosen locale.")
        DAY_OF_WEEK_NUMBER,

        @Label(value = "Day of Week (Name)", description = "If checked, the day of week will be extracted as a "
            + "localized name and appended as a string column.")
        DAY_OF_WEEK_NAME,

        @Label(value = "Hour", description = "If checked, the hour of day will be extracted as a number in range "
            + "[0-23] and appended as an integer column.")
        HOUR,

        @Label(value = "Minute", description = "If checked, the minute of hour will be extracted as a number in "
            + "range [0-59] and appended as an integer column.")
        MINUTE,

        @Label(value = "Second", description = "If checked, the second of minute will be extracted as a number in "
            + "range [0-59] and appended as an integer column.")
        SECOND,

        @Label(value = "Millisecond", description = "If checked, the millisecond fraction of second will be extracted "
            + "as number in range [0-999] and appended as an integer column.")
        MILLISECOND,

        @Label(value = "Microsecond", description = "If checked, the microsecond fraction of second will be extracted "
            + "as number in range [0-999,999] and appended as an integer column.")
        MICROSECOND,

        @Label(value = "Nanosecond", description = "If checked, the nanosecond fraction of second will be extracted "
            + "as number in range [0-999,999,999] and appended as an integer column.")
        NANOSECOND,

        @Label(value = "Time Zone Name", description = "If checked, the unique time zone name will be extracted as a "
            + "non-localized name and appended as a string column.")
        TIME_ZONE_NAME,

        @Label(value = "Time Zone Offset", description = "If checked, the time zone offset will be extracted as a "
            + "localized, formatted number and appended as a string column.")
        TIME_ZONE_OFFSET
    }

    private static IdAndText toIdAndText(final DateTimeField dateTimeField) {
        String fieldName = dateTimeField.name();
        try {
            Label fieldLabel = DateTimeField.class.getField(fieldName).getAnnotation(Label.class);
            return new IdAndText(fieldName, fieldLabel.value());
        } catch (NoSuchFieldException | SecurityException ex) {
            return new IdAndText(fieldName, fieldName);
        }
    }

    private static DateTimeField[] dateFields = new DateTimeField[] {
        DateTimeField.YEAR,
        DateTimeField.YEAR_WEEK_BASED,
        DateTimeField.QUARTER,
        DateTimeField.MONTH_NUMBER,
        DateTimeField.MONTH_NAME,
        DateTimeField.WEEK,
        DateTimeField.DAY_OF_YEAR,
        DateTimeField.DAY_OF_MONTH,
        DateTimeField.DAY_OF_WEEK_NUMBER,
        DateTimeField.DAY_OF_WEEK_NAME
    };

    private static DateTimeField[] timeFields = new DateTimeField[] {
        DateTimeField.HOUR,
        DateTimeField.MINUTE,
        DateTimeField.SECOND,
        DateTimeField.MILLISECOND,
        DateTimeField.MICROSECOND,
        DateTimeField.NANOSECOND
    };

    private static DateTimeField[] timeZoneFields = new DateTimeField[] {
        DateTimeField.TIME_ZONE_NAME,
        DateTimeField.TIME_ZONE_OFFSET
    };

    static final class ColumnUpdateFilterTrigger {
        public ColumnSelection m_selectedColumn;
    }

    static final class FilteredPossibleFieldsChoices implements ChoicesUpdateHandler<ColumnUpdateFilterTrigger> {

        FilteredPossibleFieldsChoices() {
        }

        @Override
        public IdAndText[] update(final ColumnUpdateFilterTrigger update, final DefaultNodeSettingsContext context)
            throws WidgetHandlerException {
            List<IdAndText> filteredChoices = new ArrayList<>();
            if (update.m_selectedColumn != null) {
                var colSpec = context.getDataTableSpec(0).get().getColumnSpec(update.m_selectedColumn.getSelected());
                if (isDateType(colSpec)) {
                    filteredChoices
                        .addAll(Arrays.stream(dateFields).map(ExtractDateTimeFieldsSettings::toIdAndText).toList());
                }
                if (isTimeType(colSpec)) {
                    filteredChoices
                        .addAll(Arrays.stream(timeFields).map(ExtractDateTimeFieldsSettings::toIdAndText).toList());
                }
                if (isZonedType(colSpec)) {
                    filteredChoices
                        .addAll(Arrays.stream(timeZoneFields).map(ExtractDateTimeFieldsSettings::toIdAndText).toList());
                }
            }
            return filteredChoices.toArray(IdAndText[]::new);
        }

        @Override
        public boolean setFirstValueOnUpdate() {
            return false;
        }

    }

    static boolean isDateType(final DataColumnSpec colSpec) {
        if (colSpec != null) {
            var type = colSpec.getType();
            if (type.isCompatible(LocalDateValue.class) ||
                type.isCompatible(LocalDateTimeValue.class) ||
                type.isCompatible(ZonedDateTimeValue.class)) {
                return true;
            }
        }
        return false;
    }

    static boolean isTimeType(final DataColumnSpec colSpec) {
        if (colSpec != null) {
            var type = colSpec.getType();
            if (type.isCompatible(LocalTimeValue.class) ||
                type.isCompatible(LocalDateTimeValue.class) ||
                type.isCompatible(ZonedDateTimeValue.class)) {
                return true;
            }
        }
        return false;
    }

    static boolean isZonedType(final DataColumnSpec colSpec) {
        if (colSpec != null) {
            var type = colSpec.getType();
            if (type.isCompatible(ZonedDateTimeValue.class)) {
                return true;
            }
        }
        return false;
    }

    static final class LocaleChoices implements ChoicesProvider {

        @Override
        public IdAndText[] choicesWithIdAndText(final DefaultNodeSettingsContext context) {
            return Arrays.stream(LocaleProvider.JAVA_11.getLocales()).map(
                    locale -> new IdAndText(locale.toLanguageTag(), locale.getDisplayName())
                ).toArray(IdAndText[]::new);
        }
    }

    private static final Locale FALLBACK_LOCALE = Locale.US;

    static Locale getDefaultLocale() {
        final Locale defaultLocale = Locale.getDefault();
        if (Arrays.stream(LocaleProvider.JAVA_11.getLocales()).anyMatch(defaultLocale::equals)) {
            return defaultLocale;
        }
        return FALLBACK_LOCALE;
    }

}
