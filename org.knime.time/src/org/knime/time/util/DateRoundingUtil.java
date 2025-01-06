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
 *   Nov 25, 2024 (Tobias Kampmann): created
 */
package org.knime.time.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjusters;
import java.util.function.UnaryOperator;

import org.knime.time.node.manipulate.datetimeround.DateRoundNodeSettings;

/**
 * Utility class for rounding date-based temporal values to a specified precision.
 */
public final class DateRoundingUtil {

    private DateRoundingUtil() {
        // utility class
    }

    /**
     * Rounds a temporal value based on the specified rounding strategy, interval, and shift mode.
     *
     * @param temporal the temporal value to round, must be of type {@link LocalDate}, {@link LocalDateTime}, or
     *            {@link ZonedDateTime}
     * @param strategy the rounding strategy, i.e., whether to round to the first point in time of the precision or the
     *            next
     * @param precision the rounding precision
     * @param shiftMode the shift mode, i.e., whether to shift the rounded value to the previous, next, or this
     *            precision
     * @param dayOrWeekday
     * @return the rounded temporal value
     */
    public static Temporal roundDateBasedTemporal(final Temporal temporal, final DateRoundingStrategy strategy,
        final RoundDatePrecision precision, final ShiftMode shiftMode, final DayOrWeekday dayOrWeekday) {

        if (temporal instanceof LocalDate localDate) {
            return roundLocalDate(localDate, strategy, precision, shiftMode, dayOrWeekday);
        } else if (temporal instanceof LocalDateTime localDateTime) {
            return roundLocalDateTime(localDateTime, strategy, precision, shiftMode, dayOrWeekday);
        } else if (temporal instanceof ZonedDateTime zonedDateTime) {
            return roundZonedDateTime(zonedDateTime, strategy, precision, shiftMode, dayOrWeekday);
        } else {
            throw new IllegalArgumentException("Unsupported Temporal type: " + temporal.getClass());
        }
    }

    /**

     * @param settings the settings to use for rounding
     * @return the rounding operator, i.e., a function that takes a temporal value and returns the rounded value
     */
    public static UnaryOperator<Temporal> createDateRounder(final DateRoundNodeSettings settings) {

        return temporal -> roundDateBasedTemporal(temporal, settings.m_dateRoundingStrategy,
            settings.m_dateRoundingPrecision, settings.m_shiftMode, settings.m_dayOrWeekDay);
    }

    private static LocalDate roundLocalDate(final LocalDate date, final DateRoundingStrategy strategy,
        final RoundDatePrecision roundingPrecision, final ShiftMode shiftMode, final DayOrWeekday dayOrWeekday) {

        LocalDate shiftedDate = shiftDate(date, roundingPrecision, shiftMode);
        LocalDate roundedDate = getRoundedDate(shiftedDate, roundingPrecision, strategy);

        if (dayOrWeekday == DayOrWeekday.WEEKDAY) {
            return adjustForWeekday(roundedDate, roundingPrecision, strategy);
        } else {
            return roundedDate;
        }
    }

    private static LocalDate shiftDate(final LocalDate date, final RoundDatePrecision precision,
        final ShiftMode shiftMode) {

        return switch (shiftMode) {
            case NEXT -> date.plus(precision.getPeriod());
            case PREVIOUS -> date.minus(precision.getPeriod());
            default -> date;
        };
    }

    /**
     * Returns the rounded date based on the specified precision and strategy. DayAndWeekday is not considered and need
     * to be adjusted separately.
     *
     * @param date the date to round
     * @param precision the precision to round to
     * @param strategy the rounding strategy, i.e., whether to round down or up.
     * @return the rounded date
     */
    private static LocalDate getRoundedDate(final LocalDate date, final RoundDatePrecision precision,
        final DateRoundingStrategy strategy) {

        return switch (precision) {

            case WEEK -> strategy == DateRoundingStrategy.FIRST
                ? date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            case MONTH -> strategy == DateRoundingStrategy.FIRST //
                ? date.withDayOfMonth(1) //
                : date.withDayOfMonth(date.lengthOfMonth());

            case QUARTER -> roundToQuaters(date, strategy);

            case YEAR -> strategy == DateRoundingStrategy.FIRST //
                ? date.withDayOfYear(1) //
                : date.withDayOfYear(date.lengthOfYear());

            case DECADE -> roundToDecades(date, strategy);

            default -> throw new IllegalArgumentException("Unexpected value: " + precision);
        };
    }

    private static LocalDate roundToQuaters(final LocalDate date, final DateRoundingStrategy strategy) {
        int startMonthOfQuarter = (date.getMonthValue()+2)/3 * 3 - 2;
        LocalDate startOfQuarter = date.withMonth(startMonthOfQuarter).withDayOfMonth(1);
        return strategy == DateRoundingStrategy.FIRST //
            ? startOfQuarter //
            : startOfQuarter.plusMonths(3).minusDays(1);
    }

    private static LocalDate roundToDecades(final LocalDate date, final DateRoundingStrategy strategy) {
        int startYearOfDecade = Math.floorDiv(date.getYear(), 10) * 10;
        LocalDate startOfDecade = LocalDate.of(startYearOfDecade, 1, 1);
        return strategy == DateRoundingStrategy.FIRST //
            ? startOfDecade //
            : startOfDecade.plusYears(10).minusDays(1);
    }

    private static LocalDate adjustForWeekday(final LocalDate date, final RoundDatePrecision roundingInterval,
        final DateRoundingStrategy strategy) {

        if (roundingInterval == RoundDatePrecision.WEEK) {
            // At this point,
            // if the strategy is LAST, the date is the last day of the week, so we need to find the previous Friday.
            // if the strategy is FIRST, the date is the first day of the week, so we dont need to do anything.
            return strategy == DateRoundingStrategy.FIRST //
                ? date //
                : date.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        }

        // All other precision are rounded to the first or last day of the month, i.e., month, quarter, year, decade.
        return strategy == DateRoundingStrategy.FIRST //
            ? date.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)) //
            : date.with(TemporalAdjusters.lastInMonth(DayOfWeek.FRIDAY));
    }

    private static LocalDateTime roundLocalDateTime(final LocalDateTime date, final DateRoundingStrategy strategy,
        final RoundDatePrecision roundingInterval, final ShiftMode shiftMode, final DayOrWeekday dayOrWeekday) {

        return switch (strategy) {
            case FIRST -> roundLocalDate(date.toLocalDate(), strategy, roundingInterval, shiftMode, dayOrWeekday)
                .atStartOfDay();
            case LAST -> roundLocalDate(date.toLocalDate(), strategy, roundingInterval, shiftMode, dayOrWeekday)
                .atTime(23, 59, 59, 999_999_999);
            default -> throw new IllegalArgumentException("Unexpected value: " + strategy);
        };
    }

    private static ZonedDateTime roundZonedDateTime(final ZonedDateTime date, final DateRoundingStrategy strategy,
        final RoundDatePrecision roundingInterval, final ShiftMode shiftMode, final DayOrWeekday dayOrWeekday) {

        return switch (strategy) {
            case FIRST -> roundLocalDate(date.toLocalDate(), strategy, roundingInterval, shiftMode, dayOrWeekday)
                .atStartOfDay(date.getZone());
            case LAST -> roundLocalDate(date.toLocalDate(), strategy, roundingInterval, shiftMode, dayOrWeekday)
                .atTime(23, 59, 59, 999_999_999).atZone(date.getZone());
            default -> throw new IllegalArgumentException("Unexpected value: " + strategy);
        };
    }

    public enum DateRoundingStrategy {
        FIRST, LAST;
}

public enum DayOrWeekday {
        DAY, WEEKDAY;
}

public enum RoundDatePrecision {

        DECADE(Period.ofYears(10)), //
        YEAR(Period.ofYears(1)), //
        QUARTER(Period.ofMonths(3)), //
        MONTH(Period.ofMonths(1)), //
        WEEK(Period.ofWeeks(1)); //

    private final Period m_period;

    RoundDatePrecision(final Period period) {
        this.m_period = period;
    }

    public Period getPeriod() {
        return m_period;
    }
}

}
