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
 *  WITHOUT ANY WARRANTY; without even the implied warranty of˝
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/˝licenses>.
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
 *   Oct 31, 2024 (david): created
 */
package org.knime.time.node.create.createdatetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.DateInterval;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.dialog.defaultdialog.widget.IntervalWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.TextInputWidgetValidation.PatternValidation.ColumnNameValidationV2;
import org.knime.time.util.DateTimeType;
import org.knime.time.util.DateTimeType.IsDateTimeTypeAndNotDisabled;

/**
 * Settings for the new Create Date Time webUI node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class CreateDateTimeNodeSettings implements DefaultNodeSettings {

    @Section(title = "Starting Point")
    interface StartingPointSettingsSection {
    }

    @Section(title = "Range")
    @After(StartingPointSettingsSection.class)
    interface RangeSettingsSection {
    }

    @Section(title = "Ending Point")
    @After(RangeSettingsSection.class)
    @Effect(predicate = FixedSteps.RequiresEnd.class, type = EffectType.SHOW)
    interface EndSettingsSection {
    }

    @Section(title = "Output")
    @After(EndSettingsSection.class)
    interface OutputSettingsSection {
    }

    @Widget(title = "Output type", description = """
            The type to create can be selected. It can be either a Date, Time, Date&amp;time (Local), \
            or Date&amp;time (Zoned).
            """)
    @ValueReference(DateTimeType.Ref.class)
    @ValueSwitchWidget
    DateTimeType m_outputType = DateTimeType.LOCAL_DATE;

    @Widget(title = "Start date", description = """
            The date at which the date&amp;time creation should start, inclusive.
            """)
    @Effect(predicate = IsLocalDateAndNotDisabledStart.class, type = EffectType.SHOW)
    @Layout(StartingPointSettingsSection.class)
    LocalDate m_localDateStart = LocalDate.now().minusYears(1);

    @Widget(title = "Start time", description = """
            The time at which the date&amp;time creation should start, inclusive.
            """)
    @Effect(predicate = IsLocalTimeAndNotDisabledStart.class, type = EffectType.SHOW)
    @Layout(StartingPointSettingsSection.class)
    LocalTime m_localTimeStart = LocalTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(1);

    @Widget(title = "Start local date&time", description = """
            The local date&amp;time at which the date&amp;time creation should start, inclusive.
            """)
    @Effect(predicate = IsLocalDateTimeAndNotDisabledStart.class, type = EffectType.SHOW)
    @Layout(StartingPointSettingsSection.class)
    LocalDateTime m_localDateTimeStart = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusYears(1);

    @Widget(title = "Start zoned date&time", description = """
            The zoned date&amp;time at which the date&amp;time creation should start, \
            inclusive.
            """)
    @Effect(predicate = IsZonedDateTimeAndNotDisabledStart.class, type = EffectType.SHOW)
    @Layout(StartingPointSettingsSection.class)
    ZonedDateTime m_zonedDateTimeStart = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusYears(1);

    @Widget(title = "Use execution date & time",
        description = "If checked, the execution date &amp; time will be used as starting point.")
    @Layout(StartingPointSettingsSection.class)
    @ValueReference(UseExecutionTimeStartRef.class)
    boolean m_useExecutionTimeStart;

    @Layout(RangeSettingsSection.class)
    @Widget(title = "Fixed steps", description = "How the rows are created.")
    @ValueReference(FixedSteps.FixedStepsValueReference.class)
    @ValueSwitchWidget
    FixedSteps m_fixedSteps = FixedSteps.NUMBER_AND_END;

    @Layout(value = RangeSettingsSection.class)
    @Widget(title = "Number of rows", description = """
            The number of rows to create.
            """)
    @Effect(predicate = FixedSteps.IncludesNumber.class, type = EffectType.SHOW)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    long m_numberOfRows = 1000;

    @Layout(value = RangeSettingsSection.class)
    @Widget(title = "Interval between rows", description = """
            The interval between the created rows.
            """)
    @IntervalWidget(typeProvider = DurationTypeStateProvider.class)
    @Effect(predicate = FixedSteps.IncludesDuration.class, type = EffectType.SHOW)
    Interval m_interval = DateInterval.of(0, 0, 0, 1);

    @Widget(title = "End date", description = """
            The date at which the date&amp;time creation should stop. The end date is inclusive, except \
            if the output mode is 'Number and Duration' and the provided interval does not divide \
            evenly between the start and end date.
            """)
    @Effect(predicate = IsLocalDateAndNotDisabledEnd.class, type = EffectType.SHOW)
    @Layout(value = EndSettingsSection.class)
    LocalDate m_localDateEnd = LocalDate.now();

    @Widget(title = "End time", description = """
            The time at which the date&amp;time creation should stop. The end time is inclusive, except \
            if the output mode is 'Number and Duration' and the provided interval does not divide \
            evenly between the start and end time.
            """)
    @Effect(predicate = IsLocalTimeAndNotDisabledEnd.class, type = EffectType.SHOW)
    @Layout(value = EndSettingsSection.class)
    LocalTime m_localTimeEnd = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);

    @Widget(title = "End local date&time", description = """
            The local date&amp;time at which the date&amp;time creation should stop. The end date&amp;time is \
            inclusive, except if the output mode is 'Number and Duration' and the provided interval does not divide \
            evenly between the start and end date&amp;time.
            """)
    @Effect(predicate = IsLocalDateTimeAndNotDisabledEnd.class, type = EffectType.SHOW)
    @Layout(value = EndSettingsSection.class)
    LocalDateTime m_localDateTimeEnd = LocalDateTime.now();

    @Widget(title = "End zoned date&time", description = """
            The zoned date&amp;time at which the date&amp;time creation should stop. The end date&amp;time \
            is inclusive, except if the output mode is 'Number and Duration' and the provided interval \
            does not divide evenly between the start and end date&amp;time.
            """)
    @Effect(predicate = IsZonedDateTimeAndNotDisabledEnd.class, type = EffectType.SHOW)
    @Layout(value = EndSettingsSection.class)
    ZonedDateTime m_zonedDateTimeEnd = ZonedDateTime.now();

    @Widget(title = "Use execution date & time",
        description = "If checked, the execution date &amp; time will be used as ending point.")
    @Layout(value = EndSettingsSection.class)
    @ValueReference(UseExecutionTimeEndRef.class)
    boolean m_useExecutionTimeEnd;

    @Layout(value = OutputSettingsSection.class)
    @Widget(title = "Output column name", description = "The name of the output column.")
    @TextInputWidget(patternValidation = ColumnNameValidationV2.class)
    String m_outputColumnName = "Date";

    enum FixedSteps {

            @Label(value = "Count to end", description = """
                    Outputs evenly spaced date&amp;time rows, with the specified number of output values, starting at \
                    the the start date&amp;time and ending exactly at the specified end date&amp;time. The number of \
                    rows produced in this case is fixed to the number of steps specified.
                    """)
            NUMBER_AND_END, //
            @Label(value = "Count with interval", description = """
                    Outputs evenly spaced date&amp;time rows, with the specified interval between them, starting at \
                    the start date&amp;time and ending once the specified number of rows have been added. The number \
                    of rows produced in this case is fixed to the number of steps specified.
                    """)
            NUMBER_AND_INTERVAL, //
            @Label(value = "Interval to end", description = """
                    Outputs evenly spaced date&amp;time rows, with the specified interval between them, starting at \
                    the start date&amp;time and ending at or before the end date&amp;time, depending on whether the \
                    interval divides exactly into the range. The number of rows produced in this case is variable and \
                    depends on the interval and the range.
                    """)
            INTERVAL_AND_END;

        interface FixedStepsValueReference extends Reference<FixedSteps> {
        }

        static final class IncludesDuration implements PredicateProvider {
            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getEnum(FixedSteps.FixedStepsValueReference.class) //
                    .isOneOf(FixedSteps.INTERVAL_AND_END, FixedSteps.NUMBER_AND_INTERVAL);
            }
        }

        static final class IncludesNumber implements PredicateProvider {
            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getEnum(FixedSteps.FixedStepsValueReference.class) //
                    .isOneOf(FixedSteps.NUMBER_AND_END, FixedSteps.NUMBER_AND_INTERVAL);
            }
        }

        static final class RequiresEnd implements PredicateProvider {
            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getEnum(FixedSteps.FixedStepsValueReference.class) //
                    .isOneOf(FixedSteps.NUMBER_AND_END, FixedSteps.INTERVAL_AND_END);
            }
        }
    }

    static final class UseExecutionTimeStartRef implements Reference<Boolean> {
    }

    static final class UseExecutionTimeEndRef implements Reference<Boolean> {
    }

    static final class DurationTypeStateProvider implements StateProvider<IntervalWidget.IntervalType> {

        private Supplier<DateTimeType> m_fixedStepsValueSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            this.m_fixedStepsValueSupplier = initializer.computeFromValueSupplier(DateTimeType.Ref.class);
        }

        @Override
        public IntervalWidget.IntervalType computeState(final DefaultNodeSettingsContext context) {
            return switch (m_fixedStepsValueSupplier.get()) {
                case LOCAL_DATE -> IntervalWidget.IntervalType.DATE;
                case LOCAL_TIME -> IntervalWidget.IntervalType.TIME;
                case LOCAL_DATE_TIME, ZONED_DATE_TIME -> IntervalWidget.IntervalType.DATE_OR_TIME;
            };
        }
    }

    private static final class IsLocalTimeAndNotDisabledStart
        extends IsDateTimeTypeAndNotDisabled<UseExecutionTimeStartRef> {
        IsLocalTimeAndNotDisabledStart() {
            super(DateTimeType.LOCAL_TIME, UseExecutionTimeStartRef.class);
        }
    }

    private static final class IsLocalDateAndNotDisabledStart
        extends IsDateTimeTypeAndNotDisabled<UseExecutionTimeStartRef> {
        IsLocalDateAndNotDisabledStart() {
            super(DateTimeType.LOCAL_DATE, UseExecutionTimeStartRef.class);
        }
    }

    private static final class IsLocalDateTimeAndNotDisabledStart
        extends IsDateTimeTypeAndNotDisabled<UseExecutionTimeStartRef> {
        IsLocalDateTimeAndNotDisabledStart() {
            super(DateTimeType.LOCAL_DATE_TIME, UseExecutionTimeStartRef.class);
        }
    }

    private static final class IsZonedDateTimeAndNotDisabledStart
        extends IsDateTimeTypeAndNotDisabled<UseExecutionTimeStartRef> {
        IsZonedDateTimeAndNotDisabledStart() {
            super(DateTimeType.ZONED_DATE_TIME, UseExecutionTimeStartRef.class);
        }
    }

    private static final class IsLocalTimeAndNotDisabledEnd
        extends IsDateTimeTypeAndNotDisabled<UseExecutionTimeEndRef> {
        IsLocalTimeAndNotDisabledEnd() {
            super(DateTimeType.LOCAL_TIME, UseExecutionTimeEndRef.class);
        }
    }

    private static final class IsLocalDateAndNotDisabledEnd
        extends IsDateTimeTypeAndNotDisabled<UseExecutionTimeEndRef> {
        IsLocalDateAndNotDisabledEnd() {
            super(DateTimeType.LOCAL_DATE, UseExecutionTimeEndRef.class);
        }
    }

    private static final class IsLocalDateTimeAndNotDisabledEnd
        extends IsDateTimeTypeAndNotDisabled<UseExecutionTimeEndRef> {
        IsLocalDateTimeAndNotDisabledEnd() {
            super(DateTimeType.LOCAL_DATE_TIME, UseExecutionTimeEndRef.class);
        }
    }

    private static final class IsZonedDateTimeAndNotDisabledEnd
        extends IsDateTimeTypeAndNotDisabled<UseExecutionTimeEndRef> {
        IsZonedDateTimeAndNotDisabledEnd() {
            super(DateTimeType.ZONED_DATE_TIME, UseExecutionTimeEndRef.class);
        }
    }
}
