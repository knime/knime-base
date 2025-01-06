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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.time.node.manipulate.datetimeround.DateRoundNodeSettings.DateRoundingStrategy;
import org.knime.time.node.manipulate.datetimeround.DateRoundNodeSettings.DayOrWeekday;
import org.knime.time.node.manipulate.datetimeround.DateRoundNodeSettings.RoundDatePrecision;
import org.knime.time.util.DateRoundingUtil;
import org.knime.time.util.ShiftMode;

@SuppressWarnings({"squid:S5960", "squid:S1192"})
class DateRoundingUtilTest {

    /**
     * Test the rounding of date-based temporal values. Tests all combination of rounding strategies
     * ({@link DateRoundingStrategy#FIRST}, {@link DateRoundingStrategy#LAST}) and shift modes ({@link ShiftMode#THIS},
     * {@link ShiftMode#PREVIOUS}, {@link ShiftMode#NEXT}) for {@link DayOrWeekday#DAY}.}
     *
     * @param label the label for the test
     * @param input the input to round, can be LocalDate or LocalDateTime or ZonedDateTime
     * @param granularity the granularity to round to; e.g. WEEK, MONTH, QUARTER, YEAR, DECADE
     * @param expected the expected result after rounding for settings FIRST and THIS
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDateRoundingTestCases")
    void testRoundDateBasedTemporal(final String label, final Temporal input, final RoundDatePrecision granularity,
        final Temporal expected) {

        Period period = granularity.getPeriod();

        Temporal expectedForFirstAndThis = getExpectedForFirstAndThis(expected);
        Temporal expectedForLast = getExpectedForLast(expectedForFirstAndThis.plus(period).minus(Period.ofDays(1)));

        Temporal resultFirstThis = DateRoundingUtil.roundDateBasedTemporal(input, DateRoundingStrategy.FIRST,
            granularity, ShiftMode.THIS, DayOrWeekday.DAY);
        assertEquals(expectedForFirstAndThis, resultFirstThis, "Failed test (FIRST, THIS): " + label);

        Temporal resultLastThis = DateRoundingUtil.roundDateBasedTemporal(input, DateRoundingStrategy.LAST, granularity,
            ShiftMode.THIS, DayOrWeekday.DAY);
        assertEquals(expectedForLast, resultLastThis, "Failed test (LAST, THIS): " + label);

        Temporal resultFirstPrevious = DateRoundingUtil.roundDateBasedTemporal(input, DateRoundingStrategy.FIRST,
            granularity, ShiftMode.PREVIOUS, DayOrWeekday.DAY);
        assertEquals(expectedForFirstAndThis.minus(period), resultFirstPrevious,
            "Failed test (FIRST, PREVIOUS): " + label);

        Temporal resultLastPrevious = DateRoundingUtil.roundDateBasedTemporal(input.plus(period),
            DateRoundingStrategy.LAST, granularity, ShiftMode.PREVIOUS, DayOrWeekday.DAY);
        assertEquals(expectedForLast, resultLastPrevious, "Failed test (LAST, PREVIOUS): " + label);

        Temporal resultFirstNext = DateRoundingUtil.roundDateBasedTemporal(input, DateRoundingStrategy.FIRST,
            granularity, ShiftMode.NEXT, DayOrWeekday.DAY);
        assertEquals(expectedForFirstAndThis.plus(period), resultFirstNext, "Failed test (FIRST, NEXT): " + label);

        Temporal resultLastNext = DateRoundingUtil.roundDateBasedTemporal(input.minus(period),
            DateRoundingStrategy.LAST, granularity, ShiftMode.NEXT, DayOrWeekday.DAY);
        assertEquals(expectedForLast, resultLastNext, "Failed test (LAST, NEXT): " + label);

    }

    private static Stream<Arguments> provideDateRoundingTestCases() { // NOSONAR - This function is just a list

        var now = LocalTime.now();
        var zoneId = ZoneId.of("America/Nuuk");
        return Stream.of(

            Arguments.of( //
                "LocalDate: Round to Week", //
                LocalDate.of(2024, 11, 27), //
                RoundDatePrecision.WEEK, //
                LocalDate.of(2024, 11, 25) //
            ), //
            Arguments.of( //
                "LocalDate: Round to Month", //
                LocalDate.of(2024, 11, 15), //
                RoundDatePrecision.MONTH, //
                LocalDate.of(2024, 11, 1) //
            ), //
            Arguments.of( //
                "LocalDate: Round to Quarter", //
                LocalDate.of(2024, 11, 15), //
                RoundDatePrecision.QUARTER, //
                LocalDate.of(2024, 10, 1) //
            ), //
            Arguments.of( //
                "LocalDate: Round to year", //
                LocalDate.of(2024, 6, 15), //
                RoundDatePrecision.YEAR, //
                LocalDate.of(2024, 1, 1) //
            ), //
            Arguments.of( //
                "LocalDate: Round to Decade", //
                LocalDate.of(2024, 6, 15), //
                RoundDatePrecision.DECADE, //
                LocalDate.of(2020, 1, 1) //
            ), //
            Arguments.of( //
                "LocalDate: Round to ancient Week", //
                LocalDate.of(-1, 6, 15), //
                RoundDatePrecision.WEEK, //
                LocalDate.of(-1, 6, 14) //
            ), //
            Arguments.of( //
                "LocalDate: Round to ancient Month", //
                LocalDate.of(-1, 6, 15), //
                RoundDatePrecision.MONTH, //
                LocalDate.of(-1, 6, 1) //
            ), //
            Arguments.of( //
                "LocalDate: Round to ancient Quarter", //
                LocalDate.of(-1, 6, 15), //
                RoundDatePrecision.QUARTER, //
                LocalDate.of(-1, 4, 1) //
            ), //
            Arguments.of( //
                "LocalDate: Round to ancient year", //
                LocalDate.of(-1, 6, 15), //
                RoundDatePrecision.YEAR, //
                LocalDate.of(-1, 1, 1) //
            ), //
            Arguments.of( //
                "LocalDate: Round to ancient Decade", //
                LocalDate.of(-1, 6, 15), //
                RoundDatePrecision.DECADE, //
                LocalDate.of(-10, 1, 1) //
            ), //

            Arguments.of( //
                "LocalDateTime: Round to Week", //
                LocalDateTime.of(LocalDate.of(2024, 11, 27), now), //
                RoundDatePrecision.WEEK, //
                LocalDateTime.of(LocalDate.of(2024, 11, 25), now) //
            ), //
            Arguments.of( //
                "LocalDateTime: Round to Month", //
                LocalDateTime.of(LocalDate.of(2024, 11, 15), now), //
                RoundDatePrecision.MONTH, //
                LocalDateTime.of(LocalDate.of(2024, 11, 1), now) //
            ), //
            Arguments.of( //
                "LocalDateTime: Round to Quarter", //
                LocalDateTime.of(LocalDate.of(2024, 9, 15), now), //
                RoundDatePrecision.QUARTER, //
                LocalDateTime.of(LocalDate.of(2024, 7, 1), now) //
            ), //
            Arguments.of( //
                "LocalDateTime: Round to year", //
                LocalDateTime.of(LocalDate.of(2024, 6, 15), now), //
                RoundDatePrecision.YEAR, //
                LocalDateTime.of(LocalDate.of(2024, 1, 1), now) //
            ), //
            Arguments.of( //
                "LocalDateTime: Round to Decade", //
                LocalDateTime.of(LocalDate.of(2024, 6, 15), now), //
                RoundDatePrecision.DECADE, //
                LocalDateTime.of(LocalDate.of(2020, 1, 1), now) //
            ), //

            Arguments.of( //
                "ZonedDateTime: Round to Week", //
                ZonedDateTime.of(LocalDate.of(2024, 11, 27), now, zoneId), //
                RoundDatePrecision.WEEK, //
                ZonedDateTime.of(LocalDate.of(2024, 11, 25), now, zoneId) //
            ), //
            Arguments.of( //
                "ZonedDateTime: Round to Month", //
                ZonedDateTime.of(LocalDate.of(2024, 8, 15), now, zoneId), //
                RoundDatePrecision.MONTH, //
                ZonedDateTime.of(LocalDate.of(2024, 8, 1), now, zoneId) //
            ), //
            Arguments.of( //
                "ZonedDateTime: Round to Quarter", //
                ZonedDateTime.of(LocalDate.of(2024, 1, 15), now, zoneId), //
                RoundDatePrecision.QUARTER, //
                ZonedDateTime.of(LocalDate.of(2024, 1, 1), now, zoneId) //
            ), //
            Arguments.of( //
                "ZonedDateTime: Round to year", //
                ZonedDateTime.of(LocalDate.of(2024, 2, 15), now, zoneId), //
                RoundDatePrecision.YEAR, //
                ZonedDateTime.of(LocalDate.of(2024, 1, 1), now, zoneId) //
            ), //
            Arguments.of( //
                "ZonedDateTime: Round to Decade", //
                ZonedDateTime.of(LocalDate.of(2024, 6, 15), now, zoneId), //
                RoundDatePrecision.DECADE, //
                ZonedDateTime.of(LocalDate.of(2020, 1, 1), now, zoneId) //
            ) //
        );
    }

    /**
     * Test the rounding of date-based temporal values for weekdays. Tests all combination of shift modes
     * ({@link ShiftMode#THIS}, {@link ShiftMode#PREVIOUS}, {@link ShiftMode#NEXT})
     *
     * @param label the label for the test
     * @param input the input to round, can be LocalDate or LocalDateTime or ZonedDateTime
     * @param granularity the granularity to round to; e.g. WEEK, MONTH, QUARTER, YEAR, DECADE
     * @param strategy the rounding strategy, i.e., whether to round to the first point in time of the interval or the
     * @param expectedForPrevious the expected result after rounding for settings FIRST and THIS
     * @param expectedForThis the expected result after rounding for settings FIRST and THIS
     * @param expectedForNext the expected result after rounding for settings FIRST and THIS
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDateRoundingTestCasesForWeekdays")
    void testRoundDateBasedTemporalForWeekdays(final String label, final Temporal input,
        final RoundDatePrecision granularity, final DateRoundingStrategy strategy,
        final Temporal expectedForPrevious, final Temporal expectedForThis, final Temporal expectedForNext) {

        Temporal resultThis =
            DateRoundingUtil.roundDateBasedTemporal(input, strategy, granularity, ShiftMode.THIS, DayOrWeekday.WEEKDAY);
        assertEquals(expectedForThis, resultThis, "Failed test (THIS): " + label);

        Temporal resultPrevious = DateRoundingUtil.roundDateBasedTemporal(input, strategy, granularity,
            ShiftMode.PREVIOUS, DayOrWeekday.WEEKDAY);
        assertEquals(expectedForPrevious, resultPrevious, "Failed test (PREVIOUS): " + label);

        Temporal resultNext =
            DateRoundingUtil.roundDateBasedTemporal(input, strategy, granularity, ShiftMode.NEXT, DayOrWeekday.WEEKDAY);
        assertEquals(expectedForNext, resultNext, "Failed test (NEXT): " + label);
    }

    private static Stream<Arguments> provideDateRoundingTestCasesForWeekdays() { // NOSONAR - This function is just a list
        return Stream.of( //
            Arguments.of( //
                "Round to Week (Weekday, FIRST)", //
                LocalDate.of(2024, 11, 27), //
                RoundDatePrecision.WEEK, //
                DateRoundingStrategy.FIRST, //
                LocalDate.of(2024, 11, 18), // previous
                LocalDate.of(2024, 11, 25), // this
                LocalDate.of(2024, 12, 2) // next
            ), //
            Arguments.of( //
                "Round to Month (Weekday, FIRST)", //
                LocalDate.of(2024, 11, 15), //
                RoundDatePrecision.MONTH, //
                DateRoundingStrategy.FIRST, //
                LocalDate.of(2024, 10, 7), //
                LocalDate.of(2024, 11, 4), //
                LocalDate.of(2024, 12, 2) //
            ), //
            Arguments.of( //
                "Round to Quarter (Weekday, FIRST)", //
                LocalDate.of(2024, 11, 15), //
                RoundDatePrecision.QUARTER, //
                DateRoundingStrategy.FIRST, //
                LocalDate.of(2024, 7, 1), //
                LocalDate.of(2024, 10, 7), //
                LocalDate.of(2025, 1, 6) //
            ), //
            Arguments.of( //
                "Round to Year (Weekday, FIRST)", //
                LocalDate.of(2024, 6, 15), //
                RoundDatePrecision.YEAR, //
                DateRoundingStrategy.FIRST, //
                LocalDate.of(2023, 1, 2), //
                LocalDate.of(2024, 1, 1), //
                LocalDate.of(2025, 1, 6) //
            ), //
            Arguments.of( //
                "Round to Decade (Weekday, FIRST)", //
                LocalDate.of(2024, 6, 15), //
                RoundDatePrecision.DECADE, //
                DateRoundingStrategy.FIRST, //
                LocalDate.of(2010, 1, 4), //
                LocalDate.of(2020, 1, 6), //
                LocalDate.of(2030, 1, 7) //
            ), //

            Arguments.of( //
                "Round to Week (Weekday, LAST)", //
                LocalDate.of(2024, 11, 27), //
                RoundDatePrecision.WEEK, //
                DateRoundingStrategy.LAST, //
                LocalDate.of(2024, 11, 22), //
                LocalDate.of(2024, 11, 29), //
                LocalDate.of(2024, 12, 6) //
            ), //
            Arguments.of( //
                "Round to Month (Weekday, LAST)", //
                LocalDate.of(2024, 11, 15), //
                RoundDatePrecision.MONTH, //
                DateRoundingStrategy.LAST, //
                LocalDate.of(2024, 10, 25), //
                LocalDate.of(2024, 11, 29), //
                LocalDate.of(2024, 12, 27) //
            ), //
            Arguments.of( //
                "Round to Quarter (Weekday, LAST)", //
                LocalDate.of(2024, 11, 15), //
                RoundDatePrecision.QUARTER, //
                DateRoundingStrategy.LAST, //
                LocalDate.of(2024, 9, 27), //
                LocalDate.of(2024, 12, 27), //
                LocalDate.of(2025, 3, 28) //
            ), //
            Arguments.of( //
                "Round to Year (Weekday, LAST)", //
                LocalDate.of(2024, 6, 15), //
                RoundDatePrecision.YEAR, //
                DateRoundingStrategy.LAST, //
                LocalDate.of(2023, 12, 29), //
                LocalDate.of(2024, 12, 27), //
                LocalDate.of(2025, 12, 26) //
            ), //
            Arguments.of( //
                "Round to Decade (Weekday, LAST)", //
                LocalDate.of(2024, 6, 15), //
                RoundDatePrecision.DECADE, //
                DateRoundingStrategy.LAST, //
                LocalDate.of(2019, 12, 27), //
                LocalDate.of(2029, 12, 28), //
                LocalDate.of(2039, 12, 30) //
            ) //

        // testing the other types of Temporal is not necessary as
        // types are thoroughly tested in the other test
        // and the weekday logic is the same for all Temporal types
        );
    }

    @Test
    void testUnsupportedTemporalType() {
        assertThrows(IllegalArgumentException.class, () -> DateRoundingUtil.roundDateBasedTemporal(LocalTime.now(),
            DateRoundingStrategy.FIRST, RoundDatePrecision.MONTH, ShiftMode.THIS, DayOrWeekday.DAY));
    }

    private static Temporal getExpectedForFirstAndThis(final Temporal expected) {
        if (expected instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate().atTime(LocalTime.MIN);
        } else if (expected instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toLocalDate().atTime(LocalTime.MIN).atZone(zonedDateTime.getZone());
        } else {
            return expected;
        }
    }

    private static Temporal getExpectedForLast(final Temporal expectedForFirstAndThisPlusPeriodMinusOneDay) {

        if (expectedForFirstAndThisPlusPeriodMinusOneDay instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate().atTime(LocalTime.MAX);
        } else if (expectedForFirstAndThisPlusPeriodMinusOneDay instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime.toLocalDate().atTime(LocalTime.MAX).atZone(zonedDateTime.getZone());
        } else {
            return expectedForFirstAndThisPlusPeriodMinusOneDay;
        }
    }
}
