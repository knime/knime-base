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
package org.knime.time.node.manipulate.datetimeround;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

import org.knime.time.node.manipulate.datetimeround.TimeRoundNodeSettings.TimeRoundingStrategy;

/**
 * Utility class for rounding time-based temporal values to a specified interval.
 */
public final class TimeRoundingUtil {

    private TimeRoundingUtil() {
        // Utility class
    }

    /**
     * Rounds a temporal value based on the specified rounding strategy, interval, and shift mode.
     *
     * @param temporal the temporal value to round, e.g., a {@link LocalTime}, {@link LocalDateTime}, or
     *            {@link ZonedDateTime}
     * @param strategy the rounding strategy, i.e., whether to round to the first point in time of the interval or the
     *            next
     * @param roundingInterval the interval to which the temporal value should be rounded, i.e., "round to full 2 hours"
     * @return the rounded temporal value
     * @throws ArithmeticException
     */
    public static Temporal roundTimeBasedTemporal(final Temporal temporal, final TimeRoundingStrategy strategy,
        final Duration roundingInterval) throws ArithmeticException {

        if (temporal instanceof LocalTime localTime) {
            return roundLocalTime(localTime, strategy, roundingInterval);
        } else if (temporal instanceof LocalDateTime localDateTime) {
            return roundLocalDateTime(localDateTime, strategy, roundingInterval);
        } else if (temporal instanceof ZonedDateTime zonedDateTime) {
            return roundZonedDateTime(zonedDateTime, strategy, roundingInterval);
        } else {
            throw new IllegalArgumentException("Unsupported Temporal type: " + temporal.getClass());
        }
    }

    private static LocalDateTime roundLocalDateTime(final LocalDateTime dateTime, final TimeRoundingStrategy strategy,
        final Duration roundingInterval) {

        Instant instant = dateTime.toInstant(ZoneOffset.UTC);
        Instant roundedInstant = roundInstant(instant, strategy, roundingInterval);
        return LocalDateTime.ofInstant(roundedInstant, ZoneOffset.UTC);
    }

    private static LocalTime roundLocalTime(final LocalTime time, final TimeRoundingStrategy strategy,
        final Duration roundingInterval) {

        // We could handle this separately since nanoseconds of a day fit very well into a long
        // but we keep it consistent with the other methods to reduce complexity
        LocalDateTime localDateTime = time.atDate(LocalDate.ofEpochDay(0));
        LocalDateTime roundedLocalDateTime = roundLocalDateTime(localDateTime, strategy, roundingInterval);
        return roundedLocalDateTime.toLocalTime();
    }

    private static ZonedDateTime roundZonedDateTime(final ZonedDateTime dateTime, final TimeRoundingStrategy strategy,
        final Duration roundingInterval) {

        LocalDateTime localDateTime = dateTime.toLocalDateTime();
        LocalDateTime roundedLocalDateTime = roundLocalDateTime(localDateTime, strategy, roundingInterval);

        return roundedLocalDateTime.atZone(dateTime.getZone());
    }

    private static final BigInteger ONE_MILLION = BigInteger.valueOf(1_000_000L);

    private static BigInteger getNanoSecondsSinceEpoch(final Instant instant) {
        return BigInteger.valueOf(instant.toEpochMilli()) //
            .multiply(ONE_MILLION) //
            .add(BigInteger.valueOf(instant.getNano() % 1_000_000)); // getNanos include milliseconds
    }

    private static Instant roundInstant(final Instant instant, final TimeRoundingStrategy strategy,
        final Duration roundingInterval) {

        BigInteger epochNanos = getNanoSecondsSinceEpoch(instant);
        BigInteger intervalNanos = BigInteger.valueOf(roundingInterval.toNanos());
        BigInteger roundedNanos = roundRegardingStrategy(epochNanos, intervalNanos, strategy);

        long roundedMillis = roundedNanos.divide(ONE_MILLION).longValue();
        return Instant.ofEpochMilli(roundedMillis);
    }

    private static BigInteger roundRegardingStrategy(final BigInteger current, final BigInteger interval,
        final TimeRoundingStrategy strategy) {
        switch (strategy) {
            case FIRST_POINT_IN_TIME:
                return current.divide(interval).multiply(interval);
            case NEAREST_POINT_IN_TIME:
                return current.add(interval.divide(BigInteger.valueOf(2))).divide(interval).multiply(interval);
            case LAST_POINT_IN_TIME:
                return current.add(interval).subtract(BigInteger.ONE).divide(interval).multiply(interval);
        }
        throw new IllegalArgumentException("Unsupported rounding strategy: " + strategy);
    }

}
