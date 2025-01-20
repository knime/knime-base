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
 *   Oct 10, 2016 (simon): created
 */
package org.knime.time.util;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.function.LongFunction;

/**
 * An enumeration that contains all different granularities for Date&Time shifting.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public enum Granularity {
        /** Year granularity. */
        YEAR(ChronoUnit.YEARS, null, value -> Period.ofYears(Math.toIntExact(value))), //
        /** Month granularity. */
        MONTH(ChronoUnit.MONTHS, null, value -> Period.ofMonths(Math.toIntExact(value))), //
        /** Week granularity. */
        WEEK(ChronoUnit.WEEKS, null, value -> Period.ofWeeks(Math.toIntExact(value))), //
        /** Day granularity. */
        DAY(ChronoUnit.DAYS, null, value -> Period.ofDays(Math.toIntExact(value))), //
        /** Hour granularity. */
        HOUR(ChronoUnit.HOURS, TimeBasedGranularityUnit.HOURS, Duration::ofHours), //
        /** Minute granularity. */
        MINUTE(ChronoUnit.MINUTES, TimeBasedGranularityUnit.MINUTES, Duration::ofMinutes), //
        /** Second granularity. */
        SECOND(ChronoUnit.SECONDS, TimeBasedGranularityUnit.SECONDS, Duration::ofSeconds), //
        /** Millisecond granularity. */
        MILLISECOND(ChronoUnit.MILLIS, TimeBasedGranularityUnit.MILLISECONDS, Duration::ofMillis), //
        /** Microsecond granularity. */
        MICROSECOND(ChronoUnit.MICROS, TimeBasedGranularityUnit.MICROSECONDS, Granularity::ofMicros), //
        /** Nanosecond granularity. */
        NANOSECOND(ChronoUnit.NANOS, TimeBasedGranularityUnit.NANOSECONDS, Duration::ofNanos);

    private final ChronoUnit m_chronoUnit;

    private final TimeBasedGranularityUnit m_timeBasedGranularityUnit;

    private final LongFunction<? extends TemporalAmount> m_getPeriodOrDuration;

    Granularity(final ChronoUnit chronoUnit, final TimeBasedGranularityUnit timeBasedGranularityUnit,
        final LongFunction<? extends TemporalAmount> getPeriodOrDuration) {
        m_chronoUnit = chronoUnit;
        m_timeBasedGranularityUnit = timeBasedGranularityUnit;
        m_getPeriodOrDuration = getPeriodOrDuration;
    }

    @Override
    public String toString() {
        return m_chronoUnit.toString();
    }

    /**
     * @return true if granularity belongs to a date, and false if it belongs to a time
     */
    public boolean isPartOfDate() {
        return m_chronoUnit.isDateBased();
    }


    /**
     * @return the timeBasedGranularityUnit
     */
    public TimeBasedGranularityUnit getTimeBasedGranularityUnit() {
        return m_timeBasedGranularityUnit;
    }

    /**
     * @return true if it is possible to use {@link #betweenExact(Temporal, Temporal)} with this granularity
     */
    public boolean supportsExactDifferences() {
        return m_timeBasedGranularityUnit != null;
    }

    /**
     * @return a string array containing all string representations of the enums
     */
    public static String[] strings() {
        final Granularity[] granularities = values();
        final String[] strings = new String[granularities.length];

        for (int i = 0; i < granularities.length; i++) {
            strings[i] = granularities[i].toString();
        }

        return strings;
    }

    /**
     * @param name name of the enum
     * @return the {@link Granularity}
     */
    public static Granularity fromString(final String name) {
        if (name != null) {
            for (Granularity granularity : Granularity.values()) {
                if (name.equals(granularity.toString())) {
                    return granularity;
                }
            }
        }
        throw new IllegalArgumentException("No constant with text " + name + " found");
    }

    /**
     * @param value input parameter for {@link Period} or {@link Duration}
     * @return {@link Period} or {@link Duration}
     * @throws ArithmeticException if the input overflows an integer
     */
    public TemporalAmount getPeriodOrDuration(final long value) throws ArithmeticException {
        return m_getPeriodOrDuration.apply(value);
    }

    /**
     * Calculates the amount of time between two temporal objects, truncating it to a long and disregarding the decimal
     * part.
     *
     * @param temporal1Inclusive the base temporal object, not null
     * @param temporal2Exclusive the other temporal object, exclusive, not null
     * @return the amount of time between temporal1Inclusive and temporal2Exclusive in terms of this unit; positive if
     *         temporal2Exclusive is later than temporal1Inclusive, negative if earlier
     */
    public long between(final Temporal temporal1Inclusive, final Temporal temporal2Exclusive) {
        try {
            return m_chronoUnit.between(temporal1Inclusive, temporal2Exclusive);
        } catch (DateTimeException ex) {
            throw new DateTimeException("Cannot calculate difference between " + temporal1Inclusive + " and "
                + temporal2Exclusive + " using granularity " + this.name(), ex);
        }
    }

    /**
     * Calculates the amount of time between two temporal objects, returning it as a double.
     *
     * @param start the start temporal
     * @param end the end temporal
     * @return the amount of time between start and end in terms of this unit; positive if end is later than start
     */
    public double betweenExact(final Temporal start, final Temporal end) {
        if (m_timeBasedGranularityUnit == null) {
            throw new IllegalStateException("This granularity does not support exact differences.");
        }
        return m_timeBasedGranularityUnit.getConversionExact(Duration.between(start, end));
    }

    /**
     * @return the {@link ChronoUnit} of this {@link Granularity}
     */
    public ChronoUnit getChronoUnit() {
        return m_chronoUnit;
    }

    private static Duration ofMicros(final long value) {
        return Duration.ofMillis(1000 * value);
    }
}
