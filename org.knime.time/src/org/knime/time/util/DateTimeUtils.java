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
 *   May 4, 2017 (Simon Schmid): created
 */
package org.knime.time.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * This class contains several useful functions for the the new date&time types.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
public final class DateTimeUtils {

    /**
     * Parses a string as {@link ZonedDateTime}, {@link LocalDateTime}, {@link LocalDate} or {@link LocalTime} depending
     * on its content.
     *
     * @param s string to parse
     * @return parsed temporal
     * @throws DateTimeParseException if the string could not be parsed as any {@link Temporal}
     */
    public static Temporal parseTemporal(final String s) throws DateTimeParseException {
        final Temporal temporal =
            asLocalDate(s).map(e -> (Temporal)e).orElse(asLocalTime(s).map(e -> (Temporal)e).orElse(asLocalDateTime(s)
                .map(e -> (Temporal)e).orElse(asZonedDateTime(s).map(e -> (Temporal)e).orElse(null))));
        if (temporal == null) {
            throw new DateTimeParseException("String '" + s + "' could not be parsed as a temporal.", s, 0);
        }
        return temporal;
    }

    /**
     * Tries to parse a string as a {@link ZonedDateTime}.
     *
     * @param s string to parse
     * @return an {@link Optional} holding a {@link ZonedDateTime}, if parsing was successful, otherwise an empty
     *         {@link Optional}
     */
    public static Optional<ZonedDateTime> asZonedDateTime(final String s) {
        try {
            return Optional.ofNullable(ZonedDateTime.parse(s));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    /**
     * Tries to parse a string as a {@link LocalDateTime}.
     *
     * @param s string to parse
     * @return an {@link Optional} holding a {@link LocalDateTime}, if parsing was successful, otherwise an empty
     *         {@link Optional}
     */
    public static Optional<LocalDateTime> asLocalDateTime(final String s) {
        try {
            return Optional.ofNullable(LocalDateTime.parse(s));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    /**
     * Tries to parse a string as a {@link LocalDate}.
     *
     * @param s string to parse
     * @return an {@link Optional} holding a {@link LocalDate}, if parsing was successful, otherwise an empty
     *         {@link Optional}
     */
    public static Optional<LocalDate> asLocalDate(final String s) {
        try {
            return Optional.ofNullable(LocalDate.parse(s));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    /**
     * Tries to parse a string as a {@link LocalTime}.
     *
     * @param s string to parse
     * @return an {@link Optional} holding a {@link LocalTime}, if parsing was successful, otherwise an empty
     *         {@link Optional}
     */
    public static Optional<LocalTime> asLocalTime(final String s) {
        try {
            return Optional.ofNullable(LocalTime.parse(s));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    /**
     * Tries to parse a string as a {@link ZoneId}.
     *
     * @param s string to parse
     * @return an {@link Optional} holding a {@link ZoneId}, if parsing was successful, otherwise an empty
     *         {@link Optional}
     */
    public static Optional<ZoneId> asTimezone(final String s) {
        try {
            return Optional.ofNullable(ZoneId.of(s));
        } catch (DateTimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * @return {@link LocalTime#now()} with precision reduced to milliseconds
     */
    public static LocalTime nowLocalTimeMillis() {
        final LocalTime now = LocalTime.now();
        return now.minusNanos(now.getNano() % 1000000);
    }

    /**
     * @return {@link LocalDateTime#now()} with precision reduced to milliseconds
     */
    public static LocalDateTime nowLocalDateTimeMillis() {
        final LocalDateTime now = LocalDateTime.now();
        return now.minusNanos(now.getNano() % 1000000);
    }

    /**
     * @return {@link ZonedDateTime#now()} with precision reduced to milliseconds
     */
    public static ZonedDateTime nowZonedDateTimeMillis() {
        final ZonedDateTime now = ZonedDateTime.now();
        return now.minusNanos(now.getNano() % 1000000);
    }

    /**
     * List of Date Column types
     */
    public static final List<Class<? extends DataValue>> DATE_COLUMN_TYPES =
        List.of(LocalDateValue.class, ZonedDateTimeValue.class, LocalDateTimeValue.class);

    /**
     * List of Time Column types
     */
    public static final List<Class<? extends DataValue>> TIME_COLUMN_TYPES =
        List.of(LocalTimeValue.class, ZonedDateTimeValue.class, LocalDateTimeValue.class);

    /**
     * List of DateTime Column types
     */
    public static final List<Class<? extends DataValue>> DATE_TIME_COLUMN_TYPES =
        List.of(LocalDateValue.class, LocalTimeValue.class, LocalDateTimeValue.class, ZonedDateTimeValue.class);

    /**
     * List of Interval Column types
     */
    public static final List<Class<? extends DataValue>> INTERVAL_COLUMN_TYPES =
        List.of(PeriodValue.class, DurationValue.class);

    /**
     * Decides if a DataType is compatible to DateTime columns
     *
     * @param type DataType to evaluate
     * @return true if at least one date time type is compatible, false otherwise
     */
    public static boolean isDateTimeCompatible(final DataType type) {
        return DATE_TIME_COLUMN_TYPES.stream().anyMatch(type::isCompatible);
    }

    /**
     * Decides if a DataType is compatible to Time columns
     *
     * @param type DataType to evaluate
     * @return true if at least one time type is compatible, false otherwise
     */
    public static boolean isTimeCompatible(final DataType type) {
        return TIME_COLUMN_TYPES.stream().anyMatch(type::isCompatible);
    }

    /**
     * Decides if a DataType is compatible to Date columns
     *
     * @param type DataType to evaluate
     * @return true if at least one date type is compatible, false otherwise
     */
    public static boolean isDateCompatible(final DataType type) {
        return DATE_COLUMN_TYPES.stream().anyMatch(type::isCompatible);
    }

    /**
     * A column provider for Date Columns
     */
    public static final class DateColumnProvider extends CompatibleColumnsProvider {
        DateColumnProvider() {
            super(DATE_COLUMN_TYPES);
        }

        /**
         * Is the given spec compatible with any of the Date Column types?
         *
         * @param columnSpec
         * @return true if compatible with at least one of the Date Column types
         */
        public static boolean isCompatibleType(final DataColumnSpec columnSpec) {
            return DATE_COLUMN_TYPES.stream().anyMatch(columnSpec.getType()::isCompatible);
        }
    }

    /**
     * A column provider for Time Columns
     */
    public static final class TimeColumnProvider extends CompatibleColumnsProvider {
        TimeColumnProvider() {
            super(TIME_COLUMN_TYPES);
        }

        /**
         * Is the given spec compatible with any of the Time Column types?
         *
         * @param columnSpec
         * @return true if compatible with at least one of the Time Column types
         */
        public static boolean isCompatibleType(final DataColumnSpec columnSpec) {
            return TIME_COLUMN_TYPES.stream().anyMatch(columnSpec.getType()::isCompatible);
        }
    }

    /**
     * A column provider for DateTime Columns
     */
    public static final class DateTimeColumnProvider extends CompatibleColumnsProvider {
        DateTimeColumnProvider() {
            super(DATE_TIME_COLUMN_TYPES);
        }

        /**
         * Is the given spec compatible with any of the DateTime Column types?
         *
         * @param columnSpec
         * @return true if compatible with at least one of the DateTime Column types
         */
        public static boolean isCompatibleType(final DataColumnSpec columnSpec) {
            return DATE_TIME_COLUMN_TYPES.stream().anyMatch(columnSpec.getType()::isCompatible);
        }
    }

    /**
     * A column provider for Interval Columns
     */
    public static final class IntervalColumnProvider extends CompatibleColumnsProvider {
        IntervalColumnProvider() {
            super(INTERVAL_COLUMN_TYPES);
        }

        /**
         * Is the given spec compatible with any of the Interval Column types?
         *
         * @param columnSpec
         * @return true if compatible with at least one of the Interval Column types
         */
        public static boolean isCompatibleType(final DataColumnSpec columnSpec) {
            return INTERVAL_COLUMN_TYPES.stream().anyMatch(columnSpec.getType()::isCompatible);
        }
    }

    /**
     * A column provider for Duration columns. Supports anything compatible to {@link DurationValue} and only
     * {@link DurationValue}.
     */
    public static final class DurationColumnProvider extends CompatibleColumnsProvider {
        DurationColumnProvider() {
            super(List.of(DurationValue.class));
        }

        /**
         * Is the given spec compatible with {@link DurationValue}?
         *
         * @param columnSpec
         * @return true if compatible with {@link DurationValue}
         */
        public static boolean isCompatibleType(final DataColumnSpec columnSpec) {
            return columnSpec.getType().isCompatible(DurationValue.class);
        }
    }

    /**
     * A column provider for Period columns. Supports anything compatible to {@link PeriodValue} and only
     * {@link PeriodValue}.
     */
    public static final class PeriodColumnProvider extends CompatibleColumnsProvider {
        PeriodColumnProvider() {
            super(List.of(PeriodValue.class));
        }

        /**
         * Is the given spec compatible with {@link PeriodValue}?
         *
         * @param columnSpec
         * @return true if compatible with {@link PeriodValue}
         */
        public static boolean isCompatibleType(final DataColumnSpec columnSpec) {
            return columnSpec.getType().isCompatible(PeriodValue.class);
        }
    }
}
