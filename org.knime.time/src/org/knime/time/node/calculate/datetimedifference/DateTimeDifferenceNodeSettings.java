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
 *   Dec 12, 2024 (Tobias Kampmann): created
 */
package org.knime.time.node.calculate.datetimedifference;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.TextInputWidgetValidation.PatternValidation.ColumnNameValidation;
import org.knime.time.util.DateTimeUtils;
import org.knime.time.util.Granularity;

/**
 * Settings for the Date&Time Difference node.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class DateTimeDifferenceNodeSettings implements DefaultNodeSettings {

    @Section(title = "Differencing")
    interface DifferencingSettingsSection {
    }

    @Section(title = "Output")
    @After(DifferencingSettingsSection.class)
    interface OutputSettingsSection {
    }

    @Widget(title = "First date&time column", description = """
            The first date&amp;time column. The first and second column will be subtracted \
            from each other, with the direction depending on the 'mode' option.
            """)
    @ValueReference(FirstColumnSelectionRef.class)
    @Layout(DifferencingSettingsSection.class)
    @ChoicesWidget(choices = DateTimeUtils.DateTimeColumnProvider.class)
    ColumnSelection m_firstColumnSelection = new ColumnSelection();

    @Widget(title = "Second date&time value", description = "The source of the second date&amp;time value.")
    @ValueReference(SecondDateTimeValueRef.class)
    @Layout(DifferencingSettingsSection.class)
    @RadioButtonsWidget
    SecondDateTimeValueType m_secondDateTimeValueType = SecondDateTimeValueType.COLUMN;

    @Widget(title = "Second date&time column", description = """
            The second date&amp;time column to calculate the difference to.
            """)
    @ChoicesWidget(choicesProvider = SecondColumnProvider.class)
    @Layout(DifferencingSettingsSection.class)
    @Effect(predicate = SecondDateTimeValueTypeIsColumn.class, type = EffectType.SHOW)
    ColumnSelection m_secondColumnSelection = new ColumnSelection();

    @Widget(title = "Date", description = "The fixed date to calculate the difference to.")
    @Effect(predicate = FirstColumnIsDateAndFixedDateTime.class, type = EffectType.SHOW)
    @Layout(DifferencingSettingsSection.class)
    LocalDate m_localDateFixed = LocalDate.now();

    @Widget(title = "Time", description = "The fixed time to calculate the difference to.")
    @Effect(predicate = FirstColumnIsTimeAndFixedDateTime.class, type = EffectType.SHOW)
    @Layout(DifferencingSettingsSection.class)
    LocalTime m_localTimeFixed = LocalTime.now();

    @Widget(title = "Local date&time", description = "The fixed local date&amp;time to calculate the difference to.")
    @Effect(predicate = FirstColumnIsDateTimeAndFixedDateTime.class, type = EffectType.SHOW)
    @Layout(DifferencingSettingsSection.class)
    LocalDateTime m_localDateTimeFixed = LocalDateTime.now();

    @Widget(title = "Zoned date&time", description = "The fixed zoned date&amp;time to calculate the difference to.")
    @Effect(predicate = FirstColumnIsZonedDateTimeAndFixedDateTime.class, type = EffectType.SHOW)
    @Layout(DifferencingSettingsSection.class)
    ZonedDateTime m_zonedDateTimeFixed = ZonedDateTime.now();

    @Widget(title = "Mode", description = """
            The mode in which the date&amp;time values are subtracted from each other: \
            either subtract the second value from the first, or subtract the first from \
            the second.
            """)
    @Layout(DifferencingSettingsSection.class)
    @ValueSwitchWidget
    Mode m_mode = Mode.SECOND_MINUS_FIRST;

    @Widget(title = "Output type", description = "The type of the output column.")
    @Layout(OutputSettingsSection.class)
    @ValueSwitchWidget
    @ValueReference(OutputType.Ref.class)
    OutputType m_outputType = OutputType.DURATION_OR_PERIOD;

    @Widget(title = "Granularity", description = "The unit of the output column when converting to a number.")
    @Layout(OutputSettingsSection.class)
    @Effect(predicate = OutputType.IsNumber.class, type = EffectType.SHOW)
    @ChoicesWidget(choicesProvider = ApplicableGranularitiesChoicesProvider.class)
    @ValueProvider(FirstChoiceOnChoicesChangeWhenMissing.class)
    @ValueReference(GranularityRef.class)
    Granularity m_granularity = Granularity.SECOND;

    @Widget(title = "Output number type", description = "The type of the numeric output column.")
    @Layout(OutputSettingsSection.class)
    @Effect(predicate = OutputTypeIsNumberAndSelectedGranularitySupportsDecimals.class, type = EffectType.SHOW)
    @ValueSwitchWidget
    OutputNumberType m_outputNumberType = OutputNumberType.NO_DECIMALS;

    @Widget(title = "Output column name", description = "The name of the output column.")
    @Layout(OutputSettingsSection.class)
    @TextInputWidget(validation = ColumnNameValidation.class)
    String m_outputColumnName = "Date&Time Difference";

    DateTimeDifferenceNodeSettings() {
        this((DataTableSpec)null);
    }

    DateTimeDifferenceNodeSettings(final DefaultNodeSettingsContext context) {
        this(context.getDataTableSpec(0).orElse(null));
    }

    DateTimeDifferenceNodeSettings(final DataTableSpec spec) {
        if (spec == null) {
            return;
        }

        var availableColumns =
            Arrays.stream(DateTimeUtils.getCompatibleColumns(spec, DateTimeUtils.DATE_TIME_COLUMN_TYPES)) //
                .map(spec::getColumnSpec) //
                .toList();

        if (availableColumns.isEmpty()) {
            return;
        }

        m_firstColumnSelection = new ColumnSelection(availableColumns.get(0));

        var compatibleSecondColumns = spec.stream() //
            .filter(c -> availableColumns.get(0).getType().equals(c.getType())) //
            .toArray(DataColumnSpec[]::new);

        if (compatibleSecondColumns.length > 1) {
            m_secondColumnSelection = new ColumnSelection(compatibleSecondColumns[1]);
        } else if (compatibleSecondColumns.length == 1) {
            m_secondColumnSelection = new ColumnSelection(compatibleSecondColumns[0]);
        }

    }

    interface SecondDateTimeValueRef extends Reference<SecondDateTimeValueType> {
    }

    static final class SecondDateTimeValueTypeIsColumn implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(SecondDateTimeValueRef.class).isOneOf(SecondDateTimeValueType.COLUMN);
        }
    }

    static final class SecondDateTimeValueTypeIsFixedDateTime implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(SecondDateTimeValueRef.class).isOneOf(SecondDateTimeValueType.FIXED_DATE_TIME);
        }
    }

    interface FirstColumnSelectionRef extends Reference<ColumnSelection> {
    }

    static final class FirstColumnIsDate implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getColumnSelection(FirstColumnSelectionRef.class).hasColumnType(LocalDateValue.class);
        }
    }

    static final class FirstColumnIsTime implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getColumnSelection(FirstColumnSelectionRef.class).hasColumnType(LocalTimeValue.class);
        }
    }

    static final class FirstColumnIsDateTime implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getColumnSelection(FirstColumnSelectionRef.class).hasColumnType(LocalDateTimeValue.class);
        }
    }

    static final class FirstColumnIsZonedDateTime implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getColumnSelection(FirstColumnSelectionRef.class).hasColumnType(ZonedDateTimeValue.class);
        }
    }

    static final class FirstColumnIsDateAndFixedDateTime implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            var isFixedDateTime = i.getPredicate(SecondDateTimeValueTypeIsFixedDateTime.class);
            return i.getPredicate(FirstColumnIsDate.class).and(isFixedDateTime);
        }
    }

    static final class FirstColumnIsTimeAndFixedDateTime implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            var isFixedDateTime = i.getPredicate(SecondDateTimeValueTypeIsFixedDateTime.class);
            return i.getPredicate(FirstColumnIsTime.class).and(isFixedDateTime);
        }
    }

    static final class FirstColumnIsDateTimeAndFixedDateTime implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            var isFixedDateTime = i.getPredicate(SecondDateTimeValueTypeIsFixedDateTime.class);
            return i.getPredicate(FirstColumnIsDateTime.class).and(isFixedDateTime);
        }
    }

    static final class FirstColumnIsZonedDateTimeAndFixedDateTime implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            var isFixedDateTime = i.getPredicate(SecondDateTimeValueTypeIsFixedDateTime.class);
            return i.getPredicate(FirstColumnIsZonedDateTime.class).and(isFixedDateTime);
        }
    }

    /**
     * Provides columns that have the same type as the currently selected first column.
     */
    static final class SecondColumnProvider implements ColumnChoicesStateProvider {

        private Supplier<ColumnSelection> m_firstColumnSelection;

        @Override
        public void init(final StateProviderInitializer initializer) {
            ColumnChoicesStateProvider.super.init(initializer);
            m_firstColumnSelection = initializer.computeFromValueSupplier(FirstColumnSelectionRef.class);
        }

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            var spec = context.getDataTableSpec(0);

            if (spec.isEmpty()) {
                return new DataColumnSpec[0];
            }

            var firstColumnName = m_firstColumnSelection.get().m_selected;

            if (firstColumnName == null) {
                return new DataColumnSpec[0];
            }

            var firstColumnSpec = spec.get().getColumnSpec(firstColumnName);

            if (firstColumnSpec == null) {
                return new DataColumnSpec[0];
            }

            var typeOfFirstColumn = firstColumnSpec.getType();
            return spec.map(DataTableSpec::stream) //
                .orElseGet(Stream::empty) //
                .filter(c -> typeOfFirstColumn.equals(c.getType())) //
                .toArray(DataColumnSpec[]::new);
        }
    }

    enum SecondDateTimeValueType {
            @Label(value = "Column", description = """
                    Calculates the difference between the selected second column and the first \
                    column. Differences can only be calculated between columns of the same \
                    type. Using the <i>Modify Time</i>, <i>Modify Date</i>, and <i>Modify \
                    Time Zone</i> nodes you can adjust the date&amp;time formats beforehand.
                    """)
            COLUMN, //
            @Label(value = "Fixed date&time", description = """
                    Calculates the difference between the selected fixed date&amp;time and the \
                    first column.
                    """)
            FIXED_DATE_TIME, //
            @Label(value = "Execution date&time", description = """
                    Calculates the difference between the execution date&amp;time and the chosen \
                    first column.
                    """)
            EXECUTION_DATE_TIME, //
            @Label(value = "Previous row", description = """
                    Calculates the difference between the date&amp;time in row <i>n</i> and \
                    row <i>n-1</i> in the chosen row. Note that the first row in the output will \
                    be MISSING as there is no previous row to calculate the difference with.
                    """)
            PREVIOUS_ROW,
    }

    enum Mode {
            @Label("Second minus first")
            SECOND_MINUS_FIRST, //
            @Label("First minus second")
            FIRST_MINUS_SECOND
    }

    enum OutputType {
            @Label("Duration")
            DURATION_OR_PERIOD, //
            @Label("Number")
            NUMBER;

        static final class Ref implements Reference<OutputType> {
        }

        static final class IsNumber implements PredicateProvider {

            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getEnum(Ref.class).isOneOf(NUMBER);
            }
        }
    }

    enum OutputNumberType {
            @Label(value = "No decimals", description = """
                    The output column will be an integer. Applicable for all \
                    granularities.
                    """)
            NO_DECIMALS, //
            @Label(value = "Decimals", description = """
                    The output column will be a floating decimal number. Not applicable \
                    for date-based granularities.
                    """)
            DECIMALS; //
    }

    static final class ApplicableGranularitiesProvider implements StateProvider<Granularity[]> {

        private Supplier<ColumnSelection> m_firstColumn;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_firstColumn = initializer.computeFromValueSupplier(FirstColumnSelectionRef.class);
        }

        @Override
        public Granularity[] computeState(final DefaultNodeSettingsContext context) {
            var firstSelectedColumn = m_firstColumn.get().m_selected;

            // if the first selected column exists in the table and is a local date,
            // filter to only date-based granularities that. If it's a local time,
            // filter to only time-based granularities. Otherwise, anything goes.
            var spec = context.getDataTableSpec(0);
            var firstColumnSpec = spec.map(s -> s.getColumnSpec(firstSelectedColumn));

            if (firstColumnSpec.isPresent()) {
                var firstColumnType = firstColumnSpec.get().getType();

                if (firstColumnType.equals(LocalDateCellFactory.TYPE)) {
                    return Arrays.stream(Granularity.values()) //
                        .filter(g -> g.getChronoUnit().isDateBased()) //
                        .toArray(Granularity[]::new);
                } else if (firstColumnType.equals(LocalTimeCellFactory.TYPE)) {
                    return Arrays.stream(Granularity.values()) //
                        .filter(g -> g.getChronoUnit().isTimeBased()) //
                        .toArray(Granularity[]::new);
                }
            }
            return Granularity.values();

        }

    }

    static final class ApplicableGranularitiesChoicesProvider implements StringChoicesStateProvider {
        private Supplier<Granularity[]> m_applicableGranularities;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_applicableGranularities = initializer.computeFromProvidedState(ApplicableGranularitiesProvider.class);
        }

        private static IdAndText fromGranularity(final Granularity granularity) {
            return new IdAndText(granularity.name(), granularity.toString());
        }

        @Override
        public IdAndText[] computeState(final DefaultNodeSettingsContext context) {
            return Arrays.stream(m_applicableGranularities.get()) //
                .map(ApplicableGranularitiesChoicesProvider::fromGranularity) //
                .toArray(IdAndText[]::new);
        }

    }

    static final class FirstChoiceOnChoicesChangeWhenMissing implements StateProvider<Granularity> {
        private Supplier<Granularity[]> m_applicableGranularities;

        private Supplier<Granularity> m_currentGranularity;

        @Override
        public void init(final StateProviderInitializer i) {
            m_applicableGranularities = i.computeFromProvidedState(ApplicableGranularitiesProvider.class);
            m_currentGranularity = i.getValueSupplier(GranularityRef.class);
        }

        @Override
        public Granularity computeState(final DefaultNodeSettingsContext context) {
            final var currentGranularity = m_currentGranularity.get();
            final var granularities = m_applicableGranularities.get();
            return ArrayUtils.contains(granularities, currentGranularity) ? currentGranularity : granularities[0];
        }
    }

    static final class GranularityRef implements Reference<Granularity> {
    }

    static final class OutputTypeIsNumberAndSelectedGranularitySupportsDecimals implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            var isNumber = i.getPredicate(OutputType.IsNumber.class);
            var granularitySupportsDecimals = i.getEnum(GranularityRef.class).isOneOf(//
                Arrays.stream(Granularity.values()) //
                    .filter(Granularity::supportsExactDifferences)//
                    .toArray(Granularity[]::new));
            return and(isNumber, granularitySupportsDecimals);
        }
    }
}
