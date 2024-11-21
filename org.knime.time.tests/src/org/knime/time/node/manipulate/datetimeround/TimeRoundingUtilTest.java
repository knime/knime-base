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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.time.node.manipulate.datetimeround.TimeRoundNodeSettings.TimeRoundingStrategy;

@SuppressWarnings({"squid:S5960", "squid:S1192"})
class TimeRoundingUtilTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTemporalRoundingTestCases")
    void testRoundTimeBasedTemporal(final String label, final Temporal input, final TimeRoundingStrategy strategy,
        final Duration interval, final Temporal expected) {

        Temporal result = TimeRoundingUtil.roundTimeBasedTemporal(input, strategy, interval, ShiftMode.THIS);
        assertEquals(expected, result, "Failed test: shift mode(THIS): " + label);

        Temporal resultShiftedPrevious =
            TimeRoundingUtil.roundTimeBasedTemporal(input, strategy, interval, ShiftMode.PREVIOUS);
        assertEquals(expected.minus(interval), resultShiftedPrevious, "Failed test: shift mode(PREVIOUS): " + label);

        Temporal resultShiftedNext = TimeRoundingUtil.roundTimeBasedTemporal(input, strategy, interval, ShiftMode.NEXT);
        assertEquals(expected.plus(interval), resultShiftedNext, "Failed test: shift mode(NEXT): " + label);

        if (strategy == TimeRoundingStrategy.FIRST_POINT_IN_TIME) {
            Temporal resultShiftedThis = TimeRoundingUtil.roundTimeBasedTemporal(input,
                TimeRoundingStrategy.LAST_POINT_IN_TIME, interval, ShiftMode.THIS);
            if (!expected.equals(input)) {
                assertEquals(expected.plus(interval), resultShiftedThis,
                    "Failed test: shift mode(THIS) rounding strategy(LAST): " + label);
            } else {
                assertEquals(expected, resultShiftedThis,
                    "Failed test case for LAST (input was already rounded): " + label);
            }
        }
    }

    // different shift modes are tested automatically. The expected result should refer to ShiftMode.THIS
    // LAST_POINT_IN_TIME will be tested when RoundStrategy is FIRST_POINT_IN_TIME
    private static Stream<Arguments> provideTemporalRoundingTestCases() { // NOSONAR - this function is a list
        return Stream.of( //

            Arguments.of("LocalTime: Rounding to 24 Hours (First/Last)", //
                LocalTime.of(10, 15), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(24), //
                LocalTime.MIDNIGHT //
            ), //
            Arguments.of("LocalTime: Rounding to 12 Hours (First/Last)", //
                LocalTime.of(14, 45), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(12), //
                LocalTime.of(12, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 6 Hours (First/Last)", //
                LocalTime.of(17, 30), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(6), //
                LocalTime.of(12, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 4 Hours (First/Last)", //
                LocalTime.of(22, 10), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(4), //
                LocalTime.of(20, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 3 Hours (First/Last)", //
                LocalTime.of(10, 15), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(3), //
                LocalTime.of(9, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 2 Hours (First/Last)", //
                LocalTime.of(11, 59), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(2), //
                LocalTime.of(10, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 1 Hour (First/Last)", //
                LocalTime.of(10, 29), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(1), //
                LocalTime.of(10, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 30 Minutes (First/Last)", //
                LocalTime.of(10, 45), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMinutes(30), //
                LocalTime.of(10, 30) //
            ), //
            Arguments.of("LocalTime: Rounding to 15 Minutes (First/Last)", //
                LocalTime.of(10, 14), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMinutes(15), //
                LocalTime.of(10, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 10 Minutes (First/Last)", //
                LocalTime.of(10, 26), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMinutes(10), //
                LocalTime.of(10, 20) //
            ), //
            Arguments.of("LocalTime: Rounding to 5 Minutes (First/Last)", //
                LocalTime.of(10, 7), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMinutes(5), //
                LocalTime.of(10, 5) //
            ), //
            Arguments.of("LocalTime: Rounding to 2 Minutes (First/Last)", //
                LocalTime.of(10, 3), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMinutes(2), //
                LocalTime.of(10, 2) //
            ), //
            Arguments.of("LocalTime: Rounding to 1 Minute (First/Last)", //
                LocalTime.of(10, 59), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMinutes(1), //
                LocalTime.of(10, 59) //
            ), //
            Arguments.of("LocalTime:  Rounding to 30 Seconds (First/Last)", //
                LocalTime.of(10, 15, 45), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofSeconds(30), //
                LocalTime.of(10, 15, 30) //
            ), //
            Arguments.of("LocalTime: Rounding to 15 Seconds (First/Last)", //
                LocalTime.of(10, 15, 13), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofSeconds(15), //
                LocalTime.of(10, 15, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 10 Seconds (First/Last)", //
                LocalTime.of(10, 15, 27), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofSeconds(10), //
                LocalTime.of(10, 15, 20) //
            ), //
            Arguments.of("LocalTime: Rounding to 5 Seconds (First/Last)", //
                LocalTime.of(10, 15, 8), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofSeconds(5), //
                LocalTime.of(10, 15, 5) //
            ), //
            Arguments.of("LocalTime: Rounding to 2 Seconds (First/Last)", //
                LocalTime.of(10, 15, 3), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofSeconds(2), //
                LocalTime.of(10, 15, 2) //
            ), //
            Arguments.of("LocalTime: Rounding to 1 Second (First/Last)", //
                LocalTime.of(10, 15, 59), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofSeconds(1), //
                LocalTime.of(10, 15, 59) //
            ), //
            Arguments.of("LocalTime: Rounding to 500 Milliseconds (First/Last)", //
                LocalTime.of(10, 15, 30, 750_000_000), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMillis(500), //
                LocalTime.of(10, 15, 30, 500_000_000) //
            ), //
            Arguments.of("LocalTime: Rounding to 250 Milliseconds (First/Last)", //
                LocalTime.of(10, 15, 30, 375_000_000), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMillis(250), //
                LocalTime.of(10, 15, 30, 250_000_000) //
            ), //
            Arguments.of("LocalTime: Rounding to 125 Milliseconds (First/Last)", //
                LocalTime.of(10, 15, 30, 187_500_000), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMillis(125), //
                LocalTime.of(10, 15, 30, 125_000_000) //
            ), //
            Arguments.of("LocalTime: Rounding to 100 Milliseconds (First/Last)", //
                LocalTime.of(10, 15, 30, 140_000_000), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMillis(100), //
                LocalTime.of(10, 15, 30, 100_000_000) //
            ), //
            Arguments.of("LocalTime: Rounding to 50 Milliseconds (First/Last)", //
                LocalTime.of(10, 15, 30, 75_000_000), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMillis(50), //
                LocalTime.of(10, 15, 30, 50_000_000) //
            ), //
            Arguments.of("LocalTime: Rounding to 25 Milliseconds (First/Last)", //
                LocalTime.of(10, 15, 30, 37_500_000), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMillis(25), //
                LocalTime.of(10, 15, 30, 25_000_000) //
            ), //
            Arguments.of("LocalTime: Rounding to 1 Millisecond (First/Last)", //
                LocalTime.of(10, 15, 30, 1_234_567), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMillis(1), //
                LocalTime.of(10, 15, 30, 1_000_000) //
            ), //
            Arguments.of("LocalTime: Time with nanoseconds (First/Last)", //
                LocalTime.of(0, 0, 0, 1), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(1), //
                LocalTime.of(0, 0) //
            ), //

            // LocalTime test cases NEAREST point in time
            Arguments.of("LocalTime: Midnight rounding to hour (nearest in time)", //
                LocalTime.of(0, 0), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofHours(1), //
                LocalTime.of(0, 0) //
            ), //
            Arguments.of("LocalTime: Last second rounding to nearest half hour", //
                LocalTime.of(23, 59, 59), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofMinutes(30), //
                LocalTime.of(0, 0) //
            ), //
            Arguments.of("LocalTime: Time with nanoseconds and round to nearest", //
                LocalTime.of(0, 0, 0, 1), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofHours(1), //
                LocalTime.of(0, 0) //
            ), //
            Arguments.of("LocalTime: Time with 500k nanoseconds and round to nearest", //
                LocalTime.of(0, 0, 0, 500_000), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofHours(1), //
                LocalTime.of(0, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 3 Hours - Nearest Down", //
                LocalTime.of(10, 15, 30), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofHours(3), //
                LocalTime.of(9, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 3 Hours - Nearest Up", //
                LocalTime.of(11, 45, 0), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofHours(3), //
                LocalTime.of(12, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 15 Minutes - Nearest Down", //
                LocalTime.of(10, 7, 29, 999_999_999), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofMinutes(15), //
                LocalTime.of(10, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 15 Minutes - Nearest Up", //
                LocalTime.of(10, 8, 30), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofMinutes(15), //
                LocalTime.of(10, 15) //
            ), //
            Arguments.of("LocalTime: Rounding to 1 Second - Nearest Down", //
                LocalTime.of(10, 0, 0, 499_999_999), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofSeconds(1), //
                LocalTime.of(10, 0, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 1 Second - Nearest Up", //
                LocalTime.of(10, 0, 0, 500_000_000), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofSeconds(1), //
                LocalTime.of(10, 0, 1) //
            ), //
            Arguments.of("LocalTime: Rounding to 500 Milliseconds - Nearest Down", //
                LocalTime.of(10, 0, 0, 249_999_999), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofMillis(500), //
                LocalTime.of(10, 0, 0) //
            ), //
            Arguments.of("LocalTime: Rounding to 500 Milliseconds - Nearest Up", //
                LocalTime.of(10, 0, 0, 750_000_000), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofMillis(500), //
                LocalTime.of(10, 0, 1) //
            ), //
            Arguments.of("LocalTime: Rounding to 10 Milliseconds - Edge Case", //
                LocalTime.of(10, 0, 0, 5_000_000), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofMillis(10), //
                LocalTime.of(10, 0, 0, 10_000_000) //
            ), //

            Arguments.of("LocalDateTime: End of day rounding to day start", //
                LocalDateTime.of(2024, 11, 25, 23, 59), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(24), //
                LocalDateTime.of(2024, 11, 25, 0, 0) //
            ), //
            Arguments.of("LocalDateTime: Noon rounding to next hour", //
                LocalDateTime.of(2024, 11, 25, 12, 0), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofHours(1), //
                LocalDateTime.of(2024, 11, 25, 12, 0) //
            ), //
            Arguments.of("LocalDateTime: Time with nanoseconds", //
                LocalDateTime.of(2024, 11, 25, 0, 0, 0, 1), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(1), //
                LocalDateTime.of(2024, 11, 25, 0, 0) //
            ), //
            Arguments.of("LocalDateTime: Date edge case with nanoseconds involved", //
                LocalDateTime.of(2024, 11, 25, 23, 59, 59, 999_999_999), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(24), //
                LocalDateTime.of(2024, 11, 25, 0, 0) //
            ), //
            Arguments.of("LocalDateTime: Rounding to 24 Hours - Leap Day", //
                LocalDateTime.of(2024, 2, 29, 10, 15), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofHours(24), //
                LocalDateTime.of(2024, 2, 29, 0, 0) //
            ), //
            Arguments.of("LocalDateTime: Rounding to 24 Hours - Date Shift to Next Day", //
                LocalDateTime.of(2024, 2, 28, 18, 0), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofHours(24), //
                LocalDateTime.of(2024, 2, 29, 0, 0) //
            ), //
            Arguments.of("LocalDateTime: Rounding to 1 Hour - Rounding Over Midnight", //
                LocalDateTime.of(2024, 11, 25, 23, 45), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofHours(1), //
                LocalDateTime.of(2024, 11, 26, 0, 0) //
            ), //

            Arguments.of("ZonedDateTime: Rounding to next Day", //
                ZonedDateTime.of(2024, 11, 25, 15, 0, 0, 0, ZoneId.of("Europe/Berlin")), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofHours(24), //
                ZonedDateTime.of(2024, 11, 26, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")) //
            ), //
            Arguments.of("ZonedDateTime: Rounding to 1 Hour - UTC Offset Handling", //
                ZonedDateTime.of(2024, 11, 25, 23, 30, 0, 0, ZoneId.of("UTC+05:30")), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofHours(1), //
                ZonedDateTime.of(2024, 11, 26, 0, 0, 0, 0, ZoneId.of("UTC+05:30")) //
            ),
            Arguments.of("ZonedDateTime: Midnight UTC rounding to day start",
                ZonedDateTime.of(2024, 11, 25, 0, 0, 0, 0, ZoneId.of("UTC")), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(24), //
                ZonedDateTime.of(2024, 11, 25, 0, 0, 0, 0, ZoneId.of("UTC")) //
            ), //
            Arguments.of("ZonedDateTime: ZonedDateTime with Nanoseconds", //
                ZonedDateTime.of(2024, 11, 25, 0, 0, 0, 1, ZoneId.of("UTC")), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofHours(24), //
                ZonedDateTime.of(2024, 11, 25, 0, 0, 0, 0, ZoneId.of("UTC")) //
            ), //
            Arguments.of("ZonedDateTime: DST Start - Time Skipped (2:30 AM)", //
                ZonedDateTime.of(2024, 3, 31, 2, 30, 0, 0, ZoneId.of("Europe/Berlin")), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofMinutes(30), //
                ZonedDateTime.of(2024, 3, 31, 2, 30, 0, 0, ZoneId.of("Europe/Berlin")) //
            ), //
            Arguments.of("ZonedDateTime: DST Start - Pre-Transition", //
                ZonedDateTime.of(2024, 3, 31, 1, 59, 0, 0, ZoneId.of("Europe/Berlin")), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofMinutes(30), //
                ZonedDateTime.of(2024, 3, 31, 2, 0, 0, 0, ZoneId.of("Europe/Berlin")) //
            ), //
            Arguments.of("ZonedDateTime: DST End - First Instance", //
                ZonedDateTime.of(2024, 10, 27, 2, 15, 0, 0, ZoneId.of("Europe/Berlin"))
                    .withZoneSameLocal(ZoneOffset.ofHours(2)), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofMinutes(30), //
                ZonedDateTime.of(2024, 10, 27, 2, 30, 0, 0, ZoneId.of("Europe/Berlin"))
                    .withZoneSameLocal(ZoneOffset.ofHours(2)) //
            ), //
            Arguments.of("ZonedDateTime: DST End - Second Instance", //
                ZonedDateTime.of(2024, 10, 27, 2, 15, 0, 0, ZoneId.of("Europe/Berlin"))
                    .withZoneSameLocal(ZoneOffset.ofHours(1)), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofMinutes(30), //
                ZonedDateTime.of(2024, 10, 27, 2, 30, 0, 0, ZoneId.of("Europe/Berlin"))
                    .withZoneSameLocal(ZoneOffset.ofHours(1)) //
            ), //
            Arguments.of("ZonedDateTime: DST End - Post-Transition", //
                ZonedDateTime.of(2024, 10, 27, 3, 1, 0, 0, ZoneId.of("Europe/Berlin")), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofMinutes(30), //
                ZonedDateTime.of(2024, 10, 27, 3, 0, 0, 0, ZoneId.of("Europe/Berlin")).withLaterOffsetAtOverlap() //
            ), //
            Arguments.of("ZonedDateTime: DST End - Post-Transition - First ", //
                ZonedDateTime.of(2024, 10, 27, 3, 1, 0, 0, ZoneId.of("Europe/Berlin")), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMinutes(30), //
                ZonedDateTime.of(2024, 10, 27, 3, 0, 0, 0, ZoneId.of("Europe/Berlin")).withLaterOffsetAtOverlap() //
            ), //
            Arguments.of("ZonedDateTime: DST edge case with nanoseconds", //
                ZonedDateTime.of(2024, 10, 27, 2, 59, 59, 999_999_999, ZoneId.of("Europe/Berlin"))
                    .withZoneSameLocal(ZoneOffset.ofHours(2)), //
                TimeRoundingStrategy.FIRST_POINT_IN_TIME, //
                Duration.ofMinutes(30), //
                ZonedDateTime.of(2024, 10, 27, 2, 30, 0, 0, ZoneId.of("Europe/Berlin"))
                    .withZoneSameLocal(ZoneOffset.ofHours(2)) //
            ), //
            Arguments.of("ZonedDateTime: DST edge case with nanoseconds - Nearest", //
                ZonedDateTime.of(2024, 10, 27, 2, 59, 59, 999_999_999, ZoneId.of("Europe/Berlin"))
                    .withZoneSameLocal(ZoneOffset.ofHours(2)), //
                TimeRoundingStrategy.NEAREST_POINT_IN_TIME, //
                Duration.ofMinutes(30), //
                ZonedDateTime.of(2024, 10, 27, 3, 0, 0, 0, ZoneId.of("Europe/Berlin"))
                    .withZoneSameLocal(ZoneOffset.ofHours(2)) //
            ));

    }

    @Test
    void testUnsupportedTemporalType() {
        assertThrows(IllegalArgumentException.class, () -> TimeRoundingUtil.roundTimeBasedTemporal(LocalDate.now(),
            TimeRoundingStrategy.FIRST_POINT_IN_TIME, Duration.ofHours(24), ShiftMode.THIS));
    }

}
