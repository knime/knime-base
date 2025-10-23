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
import java.util.function.Supplier;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.array.ArrayWidget.ElementLayout;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoice;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;
import org.knime.time.node.extract.datetime.ExtractDateTimeFieldsSettings.ColumnNameProvider.DateTimeFieldReference;
import org.knime.time.util.DateTimeUtils.DateTimeColumnProvider;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
class ExtractDateTimeFieldsSettings implements NodeParameters {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExtractDateTimeFieldsSettings.class);

    ExtractDateTimeFieldsSettings() {
    }

    ExtractDateTimeFieldsSettings(final NodeParametersInput context) {
        m_selectedColumn = context.getInTableSpec(0).map(ExtractDateTimeFieldsSettings::autoGuessColumn).orElse(null);
    }

    static String autoGuessColumn(final DataTableSpec spec) {
        if (spec == null) {
            return null;
        }
        return spec.stream().filter(colSpec -> isDateTimeCompatible(colSpec)).findFirst().map(DataColumnSpec::getName)
            .orElse(null);
    }

    static final class SelectedColumnRef implements ParameterReference<String> {
    }

    static final class AutoGuessColumnValueProvider implements StateProvider<String> {

        private Supplier<String> m_valueSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_valueSupplier = initializer.getValueSupplier(SelectedColumnRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            if (m_valueSupplier.get() == null || m_valueSupplier.get().isEmpty()) {
                return context.getInTableSpec(0).map(ExtractDateTimeFieldsSettings::autoGuessColumn).orElse(null);
            }
            return m_valueSupplier.get();
        }
    }

    @Widget(title = "Date&time column",
        description = "Select the column containing Date, Time, Date&amp;time (Local), or Date&amp;time (Zoned) values "
            + "from which you want to extract specific fields.")
    @Persist(configKey = "col_select")
    @ChoicesProvider(DateTimeColumnProvider.class)
    @ValueReference(SelectedColumnRef.class)
    @ValueProvider(AutoGuessColumnValueProvider.class)
    public String m_selectedColumn;

    @Section(title = "Date and time fields")

    static final class DefaultExtractFieldProvider implements StateProvider<ExtractField> {

        private Supplier<List<EnumChoice<DateTimeField>>> m_choicesSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_choicesSupplier = initializer.computeFromProvidedState(FilteredPossibleFieldsChoices.class);
        }

        @Override
        public ExtractField computeState(final NodeParametersInput context) {
            final var choices = m_choicesSupplier.get();
            if (choices == null || choices.size() == 0) {
                return new ExtractField();
            }
            return new ExtractField(choices.get(0).id(), "");
        }
    }

    @Widget(title = "Extracted fields", description = "Define date or time fields to extract and their column names.")
    @Migration(DateTimeFieldsMigration.class)
    @ArrayWidget(elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "Add field",
        showSortButtons = true, elementDefaultValueProvider = DefaultExtractFieldProvider.class)
    public ExtractField[] m_extractFields = new ExtractField[0];

    @Widget(title = "Locale",
        description = "The locale that governs the localization of output strings "
            + "(month, day of week, time zone offset) and takes care of local calendrical characteristics "
            + "(week and day of week numbering).")
    @ChoicesProvider(LocaleChoices.class)
    public String m_locale = getDefaultLocale().toLanguageTag();

    static final class ExtractField implements NodeParameters {

        public ExtractField() {
            /* default constructor for serialization */ }

        public ExtractField(final DateTimeField field, final String columnName) {
            m_field = field;
            m_columnName = columnName;
        }

        @HorizontalLayout
        interface ExtractFieldLayout {
        }

        @Widget(title = "Field", description = "The type of field to extract.")
        @ChoicesProvider(FilteredPossibleFieldsChoices.class)
        @Layout(ExtractFieldLayout.class)
        @ValueReference(DateTimeFieldReference.class)
        public DateTimeField m_field;

        @Widget(title = "Column name",
            description = "The name of the column populated with the values of the selected field. "
                + "The field cannot be empty (it must contain at least one character).")
        @Layout(ExtractFieldLayout.class)
        @TextInputWidget(placeholderProvider = ColumnNameProvider.class,
            patternValidation = ColumnNameValidationUtils.EmptyOrColumnNameValidation.class)
        public String m_columnName;
    }

    enum DateTimeField {

            @Label(value = "Year", description = "Extracts the year as number and appends it as an integer column.")
            YEAR,

            @Label(value = "Year (Week-based)",
                description = "Extracts the year based on the week"
                    + " and appends it as an integer column. Depending on the selected locale, week 1 of a "
                    + " year may already start in the previous year, or week 52 of a year may last until the next "
                    + " year (e.g., 30th Dec 2010 belongs to week 1 of year 2011 (locale en-US), so the extracted "
                    + " <i>Year (week-based)</i> would be 2011 while the extracted <i>Year</i> would be 2010).")
            YEAR_WEEK_BASED,

            @Label(value = "Quarter", description = "Extracts the quarter of the year as a number "
                + "in range [1-4] and appends it as an integer column.")
            QUARTER,

            @Label(value = "Month (Number)", description = "Extracts the month of the year as a number "
                + "in range [1-12] and appends it as an integer column.")
            MONTH_NUMBER,

            @Label(value = "Month (Name)",
                description = "Extracts the month of the year as a localized name and appends it "
                    + "as a string column.")
            MONTH_NAME,

            @Label(value = "Week",
                description = "Extracts the week of the year as a number "
                    + "in range [1-52] and appends it as an integer column. A partial week at "
                    + "the beginning of a year is handled according to the chosen locale.")
            WEEK,

            @Label(value = "Day of Year",
                description = "Extracts the day of the year as a number in range [1-366] and appends it "
                    + "as an integer column.")
            DAY_OF_YEAR,

            @Label(value = "Day of Month",
                description = "Extracts the day of the month as a number in range [1-31] and appends it "
                    + "as an integer column.")
            DAY_OF_MONTH,

            @Label(value = "Day of Week (Number)",
                description = "Extracts the day of the week as a number in range [1-7] and appends "
                    + "it as an integer column. The numbering is based on the chosen locale.")
            DAY_OF_WEEK_NUMBER,

            @Label(value = "Day of Week (Name)", description = "Extracts the day of the week as a "
                + "localized name and appends it as a string column.")
            DAY_OF_WEEK_NAME,

            @Label(value = "Hour", description = "Extracts the hour of the day as a number in range "
                + "[0-23] and appends it as an integer column.")
            HOUR,

            @Label(value = "Minute", description = "Extracts the minute of the hour as a number in "
                + "range [0-59] and appends it as an integer column.")
            MINUTE,

            @Label(value = "Second", description = "Extracts the second of the minute as a number in "
                + "range [0-59] and appends it as an integer column.")
            SECOND,

            @Label(value = "Millisecond", description = "Extracts the millisecond fraction of the second "
                + "as number in range [0-999] and appends it as an integer column.")
            MILLISECOND,

            @Label(value = "Microsecond", description = "Extracts the microsecond fraction of the second "
                + "as number in range [0-999,999] and appends it as an integer column.")
            MICROSECOND,

            @Label(value = "Nanosecond", description = "Extracts the nanosecond fraction of the second "
                + "as number in range [0-999,999,999] and appends it as an integer column.")
            NANOSECOND,

            @Label(value = "Time Zone Name", description = "Extracts the unique time zone name as a "
                + "non-localized name and appends it as a string column.")
            TIME_ZONE_NAME,

            @Label(value = "Time Zone Offset", description = "Extracts the time zone offset as a "
                + "localized, formatted number and appends it as a string column.")
            TIME_ZONE_OFFSET;

        String getLabelValue() {
            try {
                Label label = DateTimeField.class.getField(this.name()).getAnnotation(Label.class);
                if (label == null) {
                    throw new NoSuchFieldException("DateTimeField must provide 'Label' annotation");
                }
                return label.value();
            } catch (NoSuchFieldException | SecurityException ex) {
                throw new NotImplementedException(ex);
            }
        }
    }

    private static DateTimeField[] dateFields =
        new DateTimeField[]{DateTimeField.YEAR, DateTimeField.YEAR_WEEK_BASED, DateTimeField.QUARTER,
            DateTimeField.MONTH_NUMBER, DateTimeField.MONTH_NAME, DateTimeField.WEEK, DateTimeField.DAY_OF_YEAR,
            DateTimeField.DAY_OF_MONTH, DateTimeField.DAY_OF_WEEK_NUMBER, DateTimeField.DAY_OF_WEEK_NAME};

    private static DateTimeField[] timeFields = new DateTimeField[]{DateTimeField.HOUR, DateTimeField.MINUTE,
        DateTimeField.SECOND, DateTimeField.MILLISECOND, DateTimeField.MICROSECOND, DateTimeField.NANOSECOND};

    private static DateTimeField[] timeZoneFields =
        new DateTimeField[]{DateTimeField.TIME_ZONE_NAME, DateTimeField.TIME_ZONE_OFFSET};

    static final class FilteredPossibleFieldsChoices implements EnumChoicesProvider<DateTimeField> {

        private Supplier<String> m_selectedColumnSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            EnumChoicesProvider.super.init(initializer);
            m_selectedColumnSupplier = initializer.computeFromValueSupplier(SelectedColumnRef.class);
        }

        @Override
        public List<DateTimeField> choices(final NodeParametersInput context) throws WidgetHandlerException {
            List<DateTimeField> filteredChoices = new ArrayList<>();
            var spec = context.getInTableSpec(0);
            var selectedColumn = m_selectedColumnSupplier.get();
            if (selectedColumn != null && spec.isPresent() && spec.get() != null) {
                var colSpec = spec.get().getColumnSpec(selectedColumn);
                if (isDateType(colSpec)) {
                    filteredChoices.addAll(Arrays.stream(dateFields).toList());
                }
                if (isTimeType(colSpec)) {
                    filteredChoices.addAll(Arrays.stream(timeFields).toList());
                }
                if (isZonedType(colSpec)) {
                    filteredChoices.addAll(Arrays.stream(timeZoneFields).toList());
                }
            }
            return filteredChoices;
        }
    }

    static final class ColumnNameProvider implements StateProvider<String> {

        static final class DateTimeFieldReference implements ParameterReference<DateTimeField> {
        }

        private Supplier<DateTimeField> m_valueSupplier;

        /**
         * {@inheritDoc}
         */
        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_valueSupplier = initializer.computeFromValueSupplier(DateTimeFieldReference.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String computeState(final NodeParametersInput context) {
            final var dateTimeField = m_valueSupplier.get();
            return dateTimeField == null ? "" : dateTimeField.getLabelValue();
        }

    }

    static boolean isDateTimeCompatible(final DataColumnSpec colSpec) {
        if (colSpec != null) {
            var type = colSpec.getType();
            if (type.isCompatible(LocalDateValue.class) || type.isCompatible(LocalTimeValue.class)
                || type.isCompatible(LocalDateTimeValue.class) || type.isCompatible(ZonedDateTimeValue.class)) {
                return true;
            }
        }
        return false;
    }

    static boolean isDateType(final DataColumnSpec colSpec) {
        if (colSpec != null) {
            var type = colSpec.getType();
            if (type.isCompatible(LocalDateValue.class) || type.isCompatible(LocalDateTimeValue.class)
                || type.isCompatible(ZonedDateTimeValue.class)) {
                return true;
            }
        }
        return false;
    }

    static boolean isTimeType(final DataColumnSpec colSpec) {
        if (colSpec != null) {
            var type = colSpec.getType();
            if (type.isCompatible(LocalTimeValue.class) || type.isCompatible(LocalDateTimeValue.class)
                || type.isCompatible(ZonedDateTimeValue.class)) {
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

    static final class LocaleChoices implements StringChoicesProvider {

        @Override
        public List<StringChoice> computeState(final NodeParametersInput context) {
            return Arrays.stream(getLocaleChoices())
                .map(locale -> new StringChoice(locale.toLanguageTag(), locale.getDisplayName(Locale.US))).toList();
        }
    }

    // for mocking in tests
    static Locale[] getLocaleChoices() {
        return LocaleProvider.JAVA_11.getLocales();
    }

    private static final Locale FALLBACK_LOCALE = Locale.US;

    static Locale getDefaultLocale() {
        final var defaultLocale = Locale.getDefault();
        if (Arrays.stream(LocaleProvider.JAVA_11.getLocales()).anyMatch(defaultLocale::equals)) {
            return defaultLocale;
        }
        return FALLBACK_LOCALE;
    }

}
