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
 *   5 Mar 2026 (Paul Bärnreuther): created
 */
package org.knime.time.node.filter.rowfilter;

import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.function.Supplier;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparatorDelegator;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.ValidationCallback;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Base class for range filter parameters for date/time types. Concrete subclasses specify the datetime type {@code T}
 * and granularity enum {@code G}, and use {@link Modification} to customize widget titles per type.
 *
 * @param <T> the temporal type (e.g. {@code LocalDate}, {@code LocalTime})
 * @param <G> the granularity enum type
 * @param <I> the interval type ({@code DateInterval} for date-based, {@code TimeInterval} for time-based, or
 *            {@code Interval} for types supporting both)
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
abstract class DateTimeRangeFilterParameters<T extends Temporal, G extends Enum<G> & DateTimeRangeFilterParameters.TemporalGranularity, I extends Interval>
    implements FilterValueParameters {

    abstract static class UseExecutionTimeTitleAndDescriptionModifier implements Modification.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            group.find(UseExecutionTimeStartRef.class).modifyAnnotation(Widget.class)
                .withProperty("title", "Use execution " + getTemporalTypeName())
                .withProperty("description",
                    String.format("Use the current execution %s as the starting point.", getTemporalTypeName()))
                .modify();
            group.find(UseExecutionTimeEndRef.class).modifyAnnotation(Widget.class)
                .withProperty("title", "Use execution " + getTemporalTypeName()).withProperty("description",
                    String.format("Use the current execution %s as the ending point.", getTemporalTypeName()))
                .modify();
        }

        abstract String getTemporalTypeName();

    }

    /**
     * Common interface for granularity enums that can produce a {@link TemporalAmount}.
     */
    public interface TemporalGranularity {
        /**
         * @param value the number of units
         * @return the temporal amount for the given number of units
         */
        TemporalAmount getPeriodOrDuration(long value);
    }

    enum EndMode {
            @Label(value = "Fixed", description = "End is specified as a concrete date/time value.")
            DATE_TIME, //
            @Label(value = "Duration", description = "End is specified as an interval relative to start.")
            DURATION, //
            @Label(value = "Number",
                description = "End is specified as a numerical value to add to the start, with a selected granularity unit.")
            NUMERICAL;

    }

    @Widget(title = "Starting point", description = "The start of the range")
    @Effect(predicate = UseExecutionTimeStartRef.class, type = Effect.EffectType.DISABLE)
    @Modification.WidgetReference(StartValueRef.class)
    T m_startValue;

    static final class StartValueRef implements Modification.Reference {
    }

    @Widget(title = "Use execution date and time", description = "Use the current execution time as the start value.")
    @Modification.WidgetReference(UseExecutionTimeStartRef.class)
    @ValueReference(UseExecutionTimeStartRef.class)
    boolean m_useExecutionTimeStart;

    static final class UseExecutionTimeStartRef implements Modification.Reference, BooleanReference {
    }

    @Widget(title = "Include starting point", description = "Whether the start value is included in the range.")
    boolean m_startInclusive = true;

    @Widget(title = "Range mode", description = "How the end of the range is determined.")
    @ValueSwitchWidget
    @ValueReference(EndModeRef.class)
    EndMode m_endMode = EndMode.DATE_TIME;

    interface EndModeRef extends ParameterReference<EndMode> {
    }

    static final class EndModeIsDateTime implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(EndModeRef.class).isOneOf(EndMode.DATE_TIME);
        }
    }

    static final class EndModeIsDuration implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(EndModeRef.class).isOneOf(EndMode.DURATION);
        }
    }

    static final class EndModeIsNumerical implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(EndModeRef.class).isOneOf(EndMode.NUMERICAL);
        }
    }

    @Widget(title = "Ending point", description = "The end of the range.")
    @Effect(predicate = EndModeIsDateTime.class, type = Effect.EffectType.SHOW)
    @Effect(predicate = UseExecutionTimeEndRef.class, type = Effect.EffectType.DISABLE)
    @Modification.WidgetReference(EndValueRef.class)
    T m_endValue;

    interface EndValueRef extends Modification.Reference {
    }

    @Widget(title = "Use execution date and time", description = "Use the current execution time as the end value.")
    @Modification.WidgetReference(UseExecutionTimeEndRef.class)
    @Effect(predicate = EndModeIsDateTime.class, type = Effect.EffectType.SHOW)
    @ValueReference(UseExecutionTimeEndRef.class)
    boolean m_useExecutionTimeEnd;

    static final class UseExecutionTimeEndRef implements Modification.Reference, BooleanReference {
    }

    @Widget(title = "Duration", description = "The duration/period to add to the start.")
    @Effect(predicate = EndModeIsDuration.class, type = Effect.EffectType.SHOW)
    I m_durationValue;

    @Widget(title = "Value", description = "The numerical value to add to the start.")
    @Effect(predicate = EndModeIsNumerical.class, type = Effect.EffectType.SHOW)
    int m_numericalValue = 1;

    @Widget(title = "Granularity", description = "The granularity unit for the numerical value.")
    @Effect(predicate = EndModeIsNumerical.class, type = Effect.EffectType.SHOW)
    G m_granularity;

    @Widget(title = "Include ending point", description = "Whether the end value is included in the range.")
    boolean m_endInclusive = true;

    // --- Stash / unstash ---

    /**
     * @param value a data value
     * @return the temporal value T extracted from the data value, or {@code null} if the value is not of type T
     */
    abstract T extractFromValueOrNull(DataValue value);

    @SuppressWarnings("unchecked")
    private I parseInterval(final String s) {
        return (I)Interval.parseISO(s);
    }

    @Override
    public DataValue[] stash() {
        if (m_startValue == null) {
            return new DataValue[0];
        }
        final var startCell = createCell(m_startValue);
        return switch (m_endMode) {
            case DATE_TIME -> new DataValue[]{startCell,
                createCell(m_endValue != null ? m_endValue : m_startValue)};
            case DURATION -> new DataValue[]{startCell,
                new StringCell(m_durationValue.toISOString())};
            case NUMERICAL -> new DataValue[]{startCell, new IntCell(m_numericalValue)};
        };
    }

    @Override
    public void applyStash(final DataValue[] stashedValues) {
        if (stashedValues.length == 0) {
            return;
        }
        final T startValue = extractFromValueOrNull(stashedValues[0]);
        if (startValue == null) {
            return;
        }
        m_startValue = startValue;
        if (stashedValues.length < 2) {
            m_endValue = m_startValue;
            m_endMode = EndMode.DATE_TIME;
            return;
        }
        final var second = stashedValues[1];
        final T endValue = extractFromValueOrNull(second);
        if (endValue != null) {
            m_endValue = endValue;
            m_endMode = EndMode.DATE_TIME;
        } else if (second instanceof IntValue iv) {
            m_numericalValue = iv.getIntValue();
            m_endMode = EndMode.NUMERICAL;
        } else if (second instanceof StringValue sv) {
            try {
                m_durationValue = parseInterval(sv.getStringValue());
                m_endMode = EndMode.DURATION;
            } catch (final Exception e) { // NOSONAR - best-effort: fall back to same point
                m_endValue = m_startValue;
                m_endMode = EndMode.DATE_TIME;
            }
        } else {
            m_endValue = m_startValue;
            m_endMode = EndMode.DATE_TIME;
        }
    }

    // --- Abstract methods for type-specific behavior ---

    /**
     * @param executionTime the current execution time
     * @return the execution time converted to the concrete temporal type {@code T}
     */
    abstract T extractFromExecutionTime(ZonedDateTime executionTime);

    /**
     * @param value the temporal value to wrap in a data cell
     * @return a data cell containing the given value
     */
    abstract DataCell createCell(T value);

    /**
     * @return the data type of the cells created by {@link #createCell(Temporal)}
     */
    abstract DataType getCellDataType();

    // --- Resolution logic ---

    /**
     * Resolves the start and end values (handling execution time substitution, duration, and granularity computation)
     * and returns them as data cells with inclusive/exclusive flags.
     *
     * @return the resolved range
     */
    ResolvedRange resolve() {
        final var now = ZonedDateTime.now();
        final var startVal = m_useExecutionTimeStart ? extractFromExecutionTime(now) : m_startValue;
        final T endVal;
        switch (m_endMode) {
            case DURATION:
                endVal = addDuration(startVal);
                break;
            case NUMERICAL:
                endVal = addGranularity(startVal, m_numericalValue, m_granularity);
                break;
            case DATE_TIME: // fall-through
            default:
                endVal = m_useExecutionTimeEnd ? extractFromExecutionTime(now) : m_endValue;
                break;
        }

        final var startCell = createCell(startVal);
        final var endCell = createCell(endVal);
        final var comparator = new DataValueComparatorDelegator<>(getCellDataType().getComparator());

        // If end < start due to duration/granularity computation, swap bounds
        final var swapped = m_endMode != EndMode.DATE_TIME && comparator.compare(endCell, startCell) < 0;
        if (swapped) {
            return new ResolvedRange(endCell, m_endInclusive, startCell, m_startInclusive);
        }
        return new ResolvedRange(startCell, m_startInclusive, endCell, m_endInclusive);
    }

    @SuppressWarnings("unchecked")
    private T addDuration(final T start) {
        return (T)start.plus(m_durationValue);
    }

    @SuppressWarnings("unchecked")
    private T addGranularity(final T start, final int value, final G granularity) {
        return (T)start.plus(granularity.getPeriodOrDuration(value));
    }

    /**
     * A resolved range with start and end data cells and inclusive/exclusive flags.
     */
    record ResolvedRange(DataCell start, boolean startInclusive, DataCell end, boolean endInclusive) {
    }

    /**
     * Starting point needs to be before ending point in case they are both specified.
     */
    @Override
    public void validate() throws InvalidSettingsException {
        if (m_endMode == EndMode.DATE_TIME && !m_useExecutionTimeStart && !m_useExecutionTimeEnd && m_startValue != null
            && m_endValue != null) {
            if (compareStartAndEnd(m_startValue, m_endValue) > 0) {
                throw new InvalidSettingsException("The ending point must not be before the starting point.");
            }
        }

    }

    protected abstract int compareStartAndEnd(T startValue, T endValue);

    abstract static class EndValueValidationModifier<T extends Temporal> implements Modification.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            group.find(StartValueRef.class).addAnnotation(ValueReference.class).withValue(getStartValueRef()).modify();
            group.find(EndValueRef.class).addAnnotation(CustomValidation.class).withValue(getValidationClass())
                .modify();
        }

        protected abstract Class<? extends ParameterReference<T>> getStartValueRef();

        protected abstract Class<? extends EndValueValidation<T>> getValidationClass();

        abstract static class EndValueValidation<T extends Temporal> implements CustomValidationProvider<T> {

            private Supplier<T> m_startValueSupplier;

            private Supplier<EndMode> m_endModeSupplier;

            private Supplier<Boolean> m_useExecutionTimeStartSupplier;

            private Supplier<Boolean> m_useExecutionTimeEndSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeBeforeOpenDialog();
                m_endModeSupplier = initializer.computeFromValueSupplier(EndModeRef.class);
                m_useExecutionTimeStartSupplier = initializer.computeFromValueSupplier(UseExecutionTimeStartRef.class);
                m_useExecutionTimeEndSupplier = initializer.computeFromValueSupplier(UseExecutionTimeEndRef.class);
                m_startValueSupplier = initializer.computeFromValueSupplier(getStartValueRef());
            }

            @Override
            public ValidationCallback<T> computeValidationCallback(final NodeParametersInput parametersInput) {
                if (m_endModeSupplier.get() != EndMode.DATE_TIME || m_useExecutionTimeStartSupplier.get()
                    || m_useExecutionTimeEndSupplier.get()) {
                    return null; // validation only applies when end is a date/time value
                }
                final var startValue = m_startValueSupplier.get();
                return endValue -> {
                    if (startValue != null && endValue != null && compareStartAndEnd(startValue, endValue) > 0) {
                        throw new InvalidSettingsException("The ending point must not be before the starting point.");
                    }
                };
            }

            protected abstract Class<? extends ParameterReference<T>> getStartValueRef();

            protected abstract int compareStartAndEnd(T startValue, T endValue);

        }
    }

}
