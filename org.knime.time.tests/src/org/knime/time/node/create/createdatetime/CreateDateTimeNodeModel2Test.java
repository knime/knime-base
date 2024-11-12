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
 *   Nov 25, 2024 (david): created
 */
package org.knime.time.node.create.createdatetime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.data.DataCell;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.testing.util.WorkflowManagerUtil;
import org.knime.time.node.create.createdatetime.CreateDateTimeNodeSettings.FixedSteps;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class CreateDateTimeNodeModel2Test {

    private static final String NODE_NAME = "CreateDateTimeNode";

    private static final Class<? extends DefaultNodeSettings> SETTINGS_CLASS = CreateDateTimeNodeSettings.class;

    private static final ZoneId BERLIN_TZ = ZoneId.of("Europe/Berlin");

    private static final String DATE_PFX = "date";

    private static final String TIME_PFX = "time";

    private static final String DATE_TIME_PFX = "date-time";

    private static final String ZONED_PFX = "zoned";

    @SuppressWarnings("unchecked")
    private static record DurationAndEndTestCase<T extends Temporal>(String name, T start, T end, Interval interval,
        T... expectedResults) {

        DurationAndEndTestCase<T> withNamePrefix(final String prefix) {
            return DurationAndEndTestCase.of("[%s] %s".formatted(prefix, name), start, end, interval, expectedResults);
        }

        static <T extends Temporal> DurationAndEndTestCase<T> of(final String name, final T start, final T end,
            final Interval interval, final T... expectedResults) {
            return new DurationAndEndTestCase<T>(name, start, end, interval, expectedResults);
        }
    }

    @SuppressWarnings("unchecked")
    private static record DurationAndNumberTestCase<T extends Temporal>(String name, T start, Interval interval,
        T... expectedResults) {

        DurationAndNumberTestCase<T> withNamePrefix(final String prefix) {
            return DurationAndNumberTestCase.of("[%s] %s".formatted(prefix, name), start, interval, expectedResults);
        }

        static <T extends Temporal> DurationAndNumberTestCase<T> of(final String name, final T start,
            final Interval interval, final T... expectedResults) {
            return new DurationAndNumberTestCase<T>(name, start, interval, expectedResults);
        }
    }

    @SuppressWarnings("unchecked")
    private static record NumberAndEndTestCase<T extends Temporal>(String name, T start, T end, T... expectedResults) {

        NumberAndEndTestCase<T> withNamePrefix(final String prefix) {
            return NumberAndEndTestCase.of("[%s] %s".formatted(prefix, name), start, end, expectedResults);
        }

        static <T extends Temporal> NumberAndEndTestCase<T> of(final String name, final T start, final T end,
            final T... expectedResults) {
            return new NumberAndEndTestCase<>(name, start, end, expectedResults);
        }
    }

    final static List<DurationAndEndTestCase<LocalDate>> DURATION_AND_END_DATE_TEST_CASES = List.of( //
        DurationAndEndTestCase.of("1 day period", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3),
            Interval.parseISO("P1D"), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3)), //
        DurationAndEndTestCase.of("1 month period", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1),
            Interval.parseISO("P1M"), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1)), //
        DurationAndEndTestCase.of("1 week period", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15),
            Interval.parseISO("P1W"), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 8), LocalDate.of(2024, 1, 15)), //
        DurationAndEndTestCase.of("1 year period", LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1),
            Interval.parseISO("P1Y"), LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)), //
        DurationAndEndTestCase.of("Leap year edge case", LocalDate.of(2024, 2, 28), LocalDate.of(2024, 3, 1),
            Interval.parseISO("P1D"), LocalDate.of(2024, 2, 28), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 1)), //
        DurationAndEndTestCase.of("Non-leap-year", LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 1),
            Interval.parseISO("P1D"), LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 1)), //
        DurationAndEndTestCase.of("Negative period", LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 1),
            Interval.parseISO("-P1D"), LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 1)), //
        DurationAndEndTestCase.of("Period isn't a multiple of end-start", LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 4), Interval.parseISO("P2D"), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3)) //
    );

    final static List<DurationAndEndTestCase<LocalTime>> DURATION_AND_END_TIME_TEST_CASES = List.of(
        DurationAndEndTestCase.of("1 hour duration", LocalTime.of(12, 0), LocalTime.of(14, 0),
            Interval.parseISO("PT1H"), LocalTime.of(12, 0), LocalTime.of(13, 0), LocalTime.of(14, 0)), //
        DurationAndEndTestCase.of("30 minute duration", LocalTime.of(12, 0), LocalTime.of(14, 0),
            Interval.parseISO("PT30M"), LocalTime.of(12, 0), LocalTime.of(12, 30), LocalTime.of(13, 0),
            LocalTime.of(13, 30), LocalTime.of(14, 0)), //
        DurationAndEndTestCase.of("Time should loop around", LocalTime.of(23, 59), LocalTime.of(0, 1),
            Interval.parseISO("PT1M"), LocalTime.of(23, 59), LocalTime.of(0, 0), LocalTime.of(0, 1)), //
        DurationAndEndTestCase.of("2 second duration", LocalTime.of(10, 0, 0), LocalTime.of(10, 0, 6),
            Interval.parseISO("PT2S"), //
            LocalTime.of(10, 0, 0), LocalTime.of(10, 0, 2), LocalTime.of(10, 0, 4), LocalTime.of(10, 0, 6)), //
        DurationAndEndTestCase.of("1 millisecond duration", LocalTime.of(11, 59, 59, 999_000_000),
            LocalTime.of(12, 0, 0, 0), Interval.parseISO("PT0.001S"), LocalTime.of(11, 59, 59, 999_000_000),
            LocalTime.of(12, 0, 0, 0)), //
        DurationAndEndTestCase.of("Negative duration", LocalTime.of(14, 0), LocalTime.of(12, 0),
            Interval.parseISO("-PT1H"), LocalTime.of(14, 0), LocalTime.of(13, 0), LocalTime.of(12, 0)), //
        DurationAndEndTestCase.of("Duration isn't a multiple of end-start", LocalTime.of(12, 0), LocalTime.of(14, 0),
            Interval.parseISO("PT1H30M"), LocalTime.of(12, 0), LocalTime.of(13, 30)) //
    );

    /**
     * For these we can directly reuse the date test cases, and the time test cases we can reuse with some adjustment
     */
    final static List<DurationAndEndTestCase<LocalDateTime>> DURATION_AND_END_DATE_TIME_TEST_CASES = Stream.concat( //
        DURATION_AND_END_DATE_TEST_CASES.stream().map(ld -> {
            var start = ld.start.atStartOfDay();
            var end = ld.end.atStartOfDay();
            return DurationAndEndTestCase.of(ld.name, start, end, ld.interval, Arrays.stream(ld.expectedResults)
                .map(LocalDate.class::cast).map(LocalDate::atStartOfDay).toArray(LocalDateTime[]::new));
        }), //
        Stream.of( //
            DurationAndEndTestCase.of("1 hour duration", LocalTime.of(12, 0).atDate(LocalDate.EPOCH),
                LocalTime.of(14, 0).atDate(LocalDate.EPOCH), Interval.parseISO("PT1H"),
                LocalTime.of(12, 0).atDate(LocalDate.EPOCH), LocalTime.of(13, 0).atDate(LocalDate.EPOCH),
                LocalTime.of(14, 0).atDate(LocalDate.EPOCH)), //
            DurationAndEndTestCase.of("30 minute duration", LocalTime.of(12, 0).atDate(LocalDate.EPOCH),
                LocalTime.of(14, 0).atDate(LocalDate.EPOCH), Interval.parseISO("PT30M"),
                LocalTime.of(12, 0).atDate(LocalDate.EPOCH), LocalTime.of(12, 30).atDate(LocalDate.EPOCH),
                LocalTime.of(13, 0).atDate(LocalDate.EPOCH), LocalTime.of(13, 30).atDate(LocalDate.EPOCH),
                LocalTime.of(14, 0).atDate(LocalDate.EPOCH)), //
            DurationAndEndTestCase.of("time should loop and day should increment",
                LocalTime.of(23, 59).atDate(LocalDate.EPOCH), LocalTime.of(0, 1).atDate(LocalDate.EPOCH.plusDays(1)),
                Interval.parseISO("PT1M"), LocalTime.of(23, 59).atDate(LocalDate.EPOCH),
                LocalTime.of(0, 0).atDate(LocalDate.EPOCH.plusDays(1)),
                LocalTime.of(0, 1).atDate(LocalDate.EPOCH.plusDays(1))), //
            DurationAndEndTestCase.of("2 second duration", LocalTime.of(10, 0, 0).atDate(LocalDate.EPOCH),
                LocalTime.of(10, 0, 6).atDate(LocalDate.EPOCH), Interval.parseISO("PT2S"), //
                LocalTime.of(10, 0, 0).atDate(LocalDate.EPOCH), LocalTime.of(10, 0, 2).atDate(LocalDate.EPOCH),
                LocalTime.of(10, 0, 4).atDate(LocalDate.EPOCH), LocalTime.of(10, 0, 6).atDate(LocalDate.EPOCH)), //
            DurationAndEndTestCase.of("1 millisecond duration",
                LocalTime.of(11, 59, 59, 999_000_000).atDate(LocalDate.EPOCH),
                LocalTime.of(12, 0, 0, 0).atDate(LocalDate.EPOCH), Interval.parseISO("PT0.001S"),
                LocalTime.of(11, 59, 59, 999_000_000).atDate(LocalDate.EPOCH),
                LocalTime.of(12, 0, 0, 0).atDate(LocalDate.EPOCH)), //
            DurationAndEndTestCase.of("Negative duration", LocalTime.of(14, 0).atDate(LocalDate.EPOCH),
                LocalTime.of(12, 0).atDate(LocalDate.EPOCH), Interval.parseISO("-PT1H"),
                LocalTime.of(14, 0).atDate(LocalDate.EPOCH), LocalTime.of(13, 0).atDate(LocalDate.EPOCH),
                LocalTime.of(12, 0).atDate(LocalDate.EPOCH)), //
            DurationAndEndTestCase.of("Interval isn't a multiple of end-start",
                LocalTime.of(12, 0).atDate(LocalDate.EPOCH), LocalTime.of(14, 0).atDate(LocalDate.EPOCH),
                Interval.parseISO("PT1H30M"), LocalTime.of(12, 0).atDate(LocalDate.EPOCH),
                LocalTime.of(13, 30).atDate(LocalDate.EPOCH)) //
        )).toList();

    /**
     * For these we just reuse the date and time test cases (plus a couple of edge cases)
     */
    final static List<DurationAndEndTestCase<ZonedDateTime>> DURATION_AND_END_ZONED_DATE_TIME_TEST_CASES =
        Stream.concat( //
            DURATION_AND_END_DATE_TIME_TEST_CASES.stream().map(tc -> DurationAndEndTestCase.of( //
                tc.name, //
                tc.start.atZone(ZoneId.of("UTC")), //
                tc.end.atZone(ZoneId.of("UTC")), //
                tc.interval, //
                Arrays.stream(tc.expectedResults).map(ldt -> ldt.atZone(ZoneId.of("UTC"))).toArray(ZonedDateTime[]::new) //
            )), //
            Stream.of( //
                DurationAndEndTestCase.of("Daylight saving edge case, spring",
                    ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 1, 0, 0), BERLIN_TZ),
                    ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 4, 0, 0), BERLIN_TZ), Interval.parseISO("PT1H"),
                    ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 1, 0, 0), BERLIN_TZ),
                    ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 3, 0, 0), BERLIN_TZ),
                    ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 4, 0, 0), BERLIN_TZ)), //
                DurationAndEndTestCase.of("Daylight saving edge case, autumn",
                    ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 1, 0, 0), BERLIN_TZ),
                    ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 3, 0, 0), BERLIN_TZ), Interval.parseISO("PT1H"),
                    ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 1, 0, 0), BERLIN_TZ),
                    ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 2, 0, 0), BERLIN_TZ),
                    ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 2, 0, 0), BERLIN_TZ).withLaterOffsetAtOverlap(), //
                    ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 3, 0, 0), BERLIN_TZ)) //
            )).toList();

    final static List<DurationAndNumberTestCase<LocalDate>> DURATION_AND_NUMBER_DATE_TEST_CASES = List.of( //
        DurationAndNumberTestCase.of("1 day period", LocalDate.of(2024, 1, 1), Interval.parseISO("P1D"),
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2)), //
        DurationAndNumberTestCase.of("1 month period", LocalDate.of(2024, 1, 1), Interval.parseISO("P1M"),
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1)), //
        DurationAndNumberTestCase.of("1 week period", LocalDate.of(2024, 1, 1), Interval.parseISO("P1W"),
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 8), LocalDate.of(2024, 1, 15)), //
        DurationAndNumberTestCase.of("1 year period", LocalDate.of(2024, 1, 1), Interval.parseISO("P1Y"),
            LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)), //
        DurationAndNumberTestCase.of("Leap year edge case", LocalDate.of(2024, 2, 28), Interval.parseISO("P1D"),
            LocalDate.of(2024, 2, 28), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 1)), //
        DurationAndNumberTestCase.of("Non-leap-year", LocalDate.of(2025, 2, 28), Interval.parseISO("P1D"),
            LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 1)), //
        DurationAndNumberTestCase.of("Negative duration", LocalDate.of(2024, 1, 3), Interval.parseISO("-P1D"),
            LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 1)) //
    );

    final static List<DurationAndNumberTestCase<LocalTime>> DURATION_AND_NUMBER_TIME_TEST_CASES = List.of( //
        DurationAndNumberTestCase.of("1 hour duration", LocalTime.of(12, 0), Interval.parseISO("PT1H"),
            LocalTime.of(12, 0), LocalTime.of(13, 0)), //
        DurationAndNumberTestCase.of("30 minute duration", LocalTime.of(12, 0), Interval.parseISO("PT30M"),
            LocalTime.of(12, 0), LocalTime.of(12, 30), LocalTime.of(13, 0)), //
        DurationAndNumberTestCase.of("Time should loop around", LocalTime.of(23, 59), Interval.parseISO("PT1M"),
            LocalTime.of(23, 59), LocalTime.of(0, 0), LocalTime.of(0, 1)), //
        DurationAndNumberTestCase.of("2 second duration", LocalTime.of(10, 0, 0), Interval.parseISO("PT2S"),
            LocalTime.of(10, 0, 0), LocalTime.of(10, 0, 2), LocalTime.of(10, 0, 4), LocalTime.of(10, 0, 6)), //
        DurationAndNumberTestCase.of("1 millisecond duration", LocalTime.of(11, 59, 59, 999_000_000),
            Interval.parseISO("PT0.001S"), LocalTime.of(11, 59, 59, 999_000_000), LocalTime.of(12, 0, 0, 0)), //
        DurationAndNumberTestCase.of("Negative duration", LocalTime.of(14, 0), Interval.parseISO("-PT1H"),
            LocalTime.of(14, 0), LocalTime.of(13, 0), LocalTime.of(12, 0)) //
    );

    final static List<DurationAndNumberTestCase<LocalDateTime>> DURATION_AND_NUMBER_DATE_TIME_TEST_CASES =
        Stream.concat( //
            DURATION_AND_NUMBER_DATE_TEST_CASES.stream().map(ld -> {
                var start = ld.start.atStartOfDay();
                return DurationAndNumberTestCase.of(ld.name, start, ld.interval, Arrays.stream(ld.expectedResults)
                    .map(LocalDate.class::cast).map(LocalDate::atStartOfDay).toArray(LocalDateTime[]::new));
            }), //
            Stream.of( //
                DurationAndNumberTestCase.of("Time should loop and date should increment",
                    LocalDateTime.of(2024, 12, 31, 23, 59), Interval.parseISO("PT1M"),
                    LocalDateTime.of(2024, 12, 31, 23, 59), LocalDateTime.of(2025, 1, 1, 0, 0),
                    LocalDateTime.of(2025, 1, 1, 0, 1)), //
                DurationAndNumberTestCase.of("Negative duration", LocalDateTime.of(2024, 1, 3, 12, 0),
                    Interval.parseISO("-PT1H"), LocalDateTime.of(2024, 1, 3, 12, 0),
                    LocalDateTime.of(2024, 1, 3, 11, 0), LocalDateTime.of(2024, 1, 3, 10, 0)) //
            ) //
        ).toList();

    /**
     * Reuse the date time test cases and add a couple of edge cases
     */
    final static List<DurationAndNumberTestCase<ZonedDateTime>> DURATION_AND_NUMBER_ZONED_DATE_TIME_TEST_CASES =
        Stream.concat( //
            DURATION_AND_NUMBER_DATE_TIME_TEST_CASES.stream().map(tc -> DurationAndNumberTestCase.of( //
                tc.name, //
                tc.start.atZone(ZoneId.of("UTC")), //
                tc.interval, //
                Arrays.stream(tc.expectedResults).map(ldt -> ldt.atZone(ZoneId.of("UTC"))).toArray(ZonedDateTime[]::new) //
            )), //
            Stream.of( //
                DurationAndNumberTestCase.of("Daylight saving edge case, spring", //
                    ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 1, 0, 0), BERLIN_TZ), //
                    Interval.parseISO("PT1H"), //
                    ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 1, 0, 0), BERLIN_TZ),
                    ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 3, 0, 0), BERLIN_TZ),
                    ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 4, 0, 0), BERLIN_TZ)), //
                DurationAndNumberTestCase.of("Daylight saving edge case, autumn", //
                    ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 1, 0, 0), BERLIN_TZ), //
                    Interval.parseISO("PT1H"), //
                    ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 1, 0, 0), BERLIN_TZ),
                    ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 2, 0, 0), BERLIN_TZ),
                    ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 2, 0, 0), BERLIN_TZ).withLaterOffsetAtOverlap(), //
                    ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 3, 0, 0), BERLIN_TZ)) //
            ) //
        ).toList();

    final static List<NumberAndEndTestCase<LocalDate>> NUMBER_AND_END_DATE_TEST_CASES = List.of( //
        NumberAndEndTestCase.of("Basic 3-date test", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3),
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3)), //
        NumberAndEndTestCase.of("Leap year edge case", LocalDate.of(2024, 2, 28), LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 2, 28), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 1)), //
        NumberAndEndTestCase.of("Non-leap-year", LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 1),
            LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 1)), //
        NumberAndEndTestCase.of("Interval isn't a multiple of end-start", LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 1), //
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2)), //
        NumberAndEndTestCase.of("Same start and end date", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)) //
    );

    final static List<NumberAndEndTestCase<LocalTime>> NUMBER_AND_END_TIME_TEST_CASES = List.of( //
        NumberAndEndTestCase.of("Basic 3-time test", LocalTime.of(12, 0), LocalTime.of(14, 0), LocalTime.of(12, 0),
            LocalTime.of(13, 0), LocalTime.of(14, 0)), //
        NumberAndEndTestCase.of("Negative duration", LocalTime.of(23, 0), LocalTime.of(22, 0), LocalTime.of(23, 0),
            LocalTime.of(22, 30), LocalTime.of(22, 0)), //
        NumberAndEndTestCase.of("Same start and end time", LocalTime.of(12, 0), LocalTime.of(12, 0),
            LocalTime.of(12, 0), LocalTime.of(12, 0)) //
    );

    /**
     * For these we can directly reuse the time test cases, and the date test cases we can reuse with some adjustment
     */
    final static List<NumberAndEndTestCase<LocalDateTime>> NUMBER_AND_END_DATE_TIME_TEST_CASES = Stream.concat( //
        Stream.of( //
            NumberAndEndTestCase.of("Basic 3-date-time test", LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 3, 0, 0), LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 2, 0, 0), LocalDateTime.of(2024, 1, 3, 0, 0)), //
            NumberAndEndTestCase.of("Leap year edge case", LocalDateTime.of(2024, 2, 28, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0), LocalDateTime.of(2024, 2, 28, 0, 0),
                LocalDateTime.of(2024, 2, 29, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0)), //
            NumberAndEndTestCase.of("Non-leap-year", LocalDateTime.of(2025, 2, 28, 0, 0),
                LocalDateTime.of(2025, 3, 1, 0, 0), LocalDateTime.of(2025, 2, 28, 0, 0),
                LocalDateTime.of(2025, 3, 1, 0, 0)), //
            NumberAndEndTestCase.of("Interval isn't a multiple of end-start", LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 2, 0, 0), LocalDateTime.of(2024, 1, 1, 0, 0), //
                LocalDateTime.of(2024, 1, 1, 8, 0), LocalDateTime.of(2024, 1, 1, 16, 0),
                LocalDateTime.of(2024, 1, 2, 0, 0)), //
            NumberAndEndTestCase.of("Same start and end date", LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 1, 0, 0)) //
        ), //
        NUMBER_AND_END_TIME_TEST_CASES.stream().map(lt -> {
            var start = lt.start.atDate(LocalDate.EPOCH);
            var end = lt.end.atDate(LocalDate.EPOCH);
            return NumberAndEndTestCase.of(lt.name, start, end, Arrays.stream(lt.expectedResults)
                .map(LocalTime.class::cast).map(LocalDate.EPOCH::atTime).toArray(LocalDateTime[]::new));
        }) //
    ).toList();

    /**
     * Reuse the date time test cases and add a couple of edge cases
     */
    final static List<NumberAndEndTestCase<ZonedDateTime>> NUMBER_AND_END_ZONED_DATE_TIME_TEST_CASES = Stream.concat( //
        NUMBER_AND_END_DATE_TIME_TEST_CASES.stream().map(tc -> NumberAndEndTestCase.of( //
            tc.name, //
            tc.start.atZone(ZoneId.of("UTC")), //
            tc.end.atZone(ZoneId.of("UTC")), //
            Arrays.stream(tc.expectedResults).map(ldt -> ldt.atZone(ZoneId.of("UTC"))).toArray(ZonedDateTime[]::new) //
        )), //
        Stream.of( //
            NumberAndEndTestCase.of("daylight saving edge case, spring", //
                ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 1, 0, 0), BERLIN_TZ), //
                ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 4, 0, 0), BERLIN_TZ), //
                ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 1, 0, 0), BERLIN_TZ), //
                ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 3, 0, 0), BERLIN_TZ), //
                ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 4, 0, 0), BERLIN_TZ)), //
            NumberAndEndTestCase.of("daylight saving edge case, autumn", //
                ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 1, 0, 0), BERLIN_TZ), //
                ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 3, 0, 0), BERLIN_TZ), //
                ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 1, 0, 0), BERLIN_TZ), //
                ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 2, 0, 0), BERLIN_TZ), //
                ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 2, 0, 0), BERLIN_TZ).withLaterOffsetAtOverlap(),
                ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 3, 0), BERLIN_TZ)) //
        ) //
    ).toList();

    final static Stream<Arguments> provideDurationAndEndNumericalTestCases() {
        return Stream.concat( //
            Stream.concat( //
                DURATION_AND_END_DATE_TEST_CASES.stream().map(tc -> tc.withNamePrefix(DATE_PFX)), //
                DURATION_AND_END_TIME_TEST_CASES.stream().map(tc -> tc.withNamePrefix(TIME_PFX)) //
            ), //
            Stream.concat( //
                DURATION_AND_END_DATE_TIME_TEST_CASES.stream().map(tc -> tc.withNamePrefix(DATE_TIME_PFX)), //
                DURATION_AND_END_ZONED_DATE_TIME_TEST_CASES.stream().map(tc -> tc.withNamePrefix(ZONED_PFX)) //
            ) //
        ) //
            .map(tc -> Arguments.of(tc.name, tc.start, tc.interval, tc.end, tc.expectedResults));
    }

    final static Stream<Arguments> provideDurationAndNumberNumericalTestCases() {
        return Stream.concat( //
            Stream.concat( //
                DURATION_AND_NUMBER_DATE_TEST_CASES.stream().map(tc -> tc.withNamePrefix(DATE_PFX)), //
                DURATION_AND_NUMBER_TIME_TEST_CASES.stream().map(tc -> tc.withNamePrefix(TIME_PFX)) //
            ), //
            Stream.concat( //
                DURATION_AND_NUMBER_DATE_TIME_TEST_CASES.stream().map(tc -> tc.withNamePrefix(DATE_TIME_PFX)), //
                DURATION_AND_NUMBER_ZONED_DATE_TIME_TEST_CASES.stream().map(tc -> tc.withNamePrefix(ZONED_PFX)) //
            ) //
        ) //
            .map(tc -> Arguments.of(tc.name, tc.start, tc.interval, tc.expectedResults));
    }

    final static Stream<Arguments> provideNumberAndEndTestCases() {
        return Stream.concat( //
            Stream.concat( //
                NUMBER_AND_END_DATE_TEST_CASES.stream().map(tc -> tc.withNamePrefix(DATE_PFX)), //
                NUMBER_AND_END_TIME_TEST_CASES.stream().map(tc -> tc.withNamePrefix(TIME_PFX)) //
            ), //
            Stream.concat( //
                NUMBER_AND_END_DATE_TIME_TEST_CASES.stream().map(tc -> tc.withNamePrefix(DATE_TIME_PFX)), //
                NUMBER_AND_END_ZONED_DATE_TIME_TEST_CASES.stream().map(tc -> tc.withNamePrefix(ZONED_PFX)) //
            ) //
        ) //
            .map(tc -> Arguments.of(tc.name, tc.start, tc.end, tc.expectedResults));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDurationAndEndNumericalTestCases")
    void testDurationAndEnd(@SuppressWarnings("unused") final String name, final Temporal start,
        final Interval interval, final Temporal end, final Temporal[] expectedResults)
        throws InvalidSettingsException, IOException {

        var settings = new CreateDateTimeNodeSettings();
        settings.m_fixedSteps = FixedSteps.INTERVAL_AND_END;
        writeOutputModeIntoSettings(settings, start);
        writeStartPointIntoSettings(settings, start);
        settings.m_interval = interval;
        writeEndPointIntoSettings(settings, end);

        var outputTable = setupAndExecuteWorkflow(settings);

        assertEquals(expectedResults.length, outputTable.size(),
            "Unexpected number of rows in table; expected %s but got %s".formatted(expectedResults.length,
                outputTable.size()));

        try (var it = outputTable.iterator()) {
            for (int i = 0; i < expectedResults.length; ++i) {
                var expected = expectedResults[i];
                var actual = extractCellValue(it.next().getCell(0));
                assertEquals(expected, actual,
                    "Unexpected value in table at at index %s; expected %s but got %s".formatted(i, expected, actual));
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDurationAndNumberNumericalTestCases")
    void testDurationAndNumber(@SuppressWarnings("unused") final String name, final Temporal start,
        final Interval interval, final Temporal[] expectedResults) throws InvalidSettingsException, IOException {

        var settings = new CreateDateTimeNodeSettings();
        settings.m_fixedSteps = FixedSteps.NUMBER_AND_INTERVAL;
        writeOutputModeIntoSettings(settings, start);
        writeStartPointIntoSettings(settings, start);
        settings.m_interval = interval;
        settings.m_numberOfRows = expectedResults.length;

        var outputTable = setupAndExecuteWorkflow(settings);

        assertEquals(expectedResults.length, outputTable.size(),
            "Unexpected number of rows in table; expected %s but got %s".formatted(expectedResults.length,
                outputTable.size()));

        try (var it = outputTable.iterator()) {
            for (int i = 0; i < expectedResults.length; ++i) {
                var expected = expectedResults[i];
                var actual = extractCellValue(it.next().getCell(0));
                assertEquals(expected, actual,
                    "Unexpected value in table at at index %s; expected %s but got %s".formatted(i, expected, actual));
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideNumberAndEndTestCases")
    void testNumberAndEnd(@SuppressWarnings("unused") final String name, final Temporal start, final Temporal end,
        final Temporal[] expectedResults) throws InvalidSettingsException, IOException {

        var settings = new CreateDateTimeNodeSettings();
        settings.m_fixedSteps = FixedSteps.NUMBER_AND_END;
        writeOutputModeIntoSettings(settings, start);
        writeStartPointIntoSettings(settings, start);
        settings.m_numberOfRows = expectedResults.length;
        writeEndPointIntoSettings(settings, end);

        var outputTable = setupAndExecuteWorkflow(settings);

        assertEquals(expectedResults.length, outputTable.size(),
            "Unexpected number of rows in table; expected %s but got %s".formatted(expectedResults.length,
                outputTable.size()));

        try (var it = outputTable.iterator()) {
            for (int i = 0; i < expectedResults.length; ++i) {
                var expected = expectedResults[i];
                var actual = extractCellValue(it.next().getCell(0));
                assertEquals(expected, actual,
                    "Unexpected value in table at at index %s; expected %s but got %s".formatted(i, expected, actual));
            }
        }
    }

    private static BufferedDataTable setupAndExecuteWorkflow(final CreateDateTimeNodeSettings settings)
        throws InvalidSettingsException, IOException {

        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new CreateDateTimeNodeFactory2());

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(SETTINGS_CLASS, settings, modelSettings);

        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // execute and wait...
        workflowManager.executeAllAndWaitUntilDone();
        var outputTable = (BufferedDataTable)node.getOutPort(1).getPortObject();
        return outputTable;
    }

    private static Temporal extractCellValue(final DataCell cell) {
        if (cell instanceof LocalDateCell) {
            return ((LocalDateCell)cell).getLocalDate();
        } else if (cell instanceof LocalTimeCell) {
            return ((LocalTimeCell)cell).getLocalTime();
        } else if (cell instanceof LocalDateTimeCell) {
            return ((LocalDateTimeCell)cell).getLocalDateTime();
        } else if (cell instanceof ZonedDateTimeCell) {
            return ((ZonedDateTimeCell)cell).getZonedDateTime();
        } else {
            throw new IllegalArgumentException("Unknown cell type: " + cell.getClass());
        }
    }

    private static void writeStartPointIntoSettings(final CreateDateTimeNodeSettings settings, final Temporal start) {
        var datePart = extractDatePartFromTemporal(start);
        var timePart = extractTimePartFromTemporal(start);
        var zoneId = extractZoneIdFromTemporal(start);

        if (datePart.isPresent()) {
            settings.m_localDateStart = datePart.get();
        }
        if (timePart.isPresent()) {
            settings.m_localTimeStart = timePart.get();
        }
        if (zoneId.isPresent()) {
            settings.m_timezone = zoneId.get();
        }
    }

    private static void writeEndPointIntoSettings(final CreateDateTimeNodeSettings settings, final Temporal end) {
        var datePart = extractDatePartFromTemporal(end);
        var timePart = extractTimePartFromTemporal(end);
        var zoneId = extractZoneIdFromTemporal(end);

        if (datePart.isPresent()) {
            settings.m_localDateEnd = datePart.get();
        }
        if (timePart.isPresent()) {
            settings.m_localTimeEnd = timePart.get();
        }
        if (zoneId.isPresent()) {
            settings.m_timezone = zoneId.get();
        }
    }

    private static void writeOutputModeIntoSettings(final CreateDateTimeNodeSettings settings, final Temporal start) {
        var datePart = extractDatePartFromTemporal(start);
        var timePart = extractTimePartFromTemporal(start);
        var zoneId = extractZoneIdFromTemporal(start);

        if (zoneId.isPresent()) {
            settings.m_outputType = CreateDateTimeNodeSettings.OutputType.DATE_TIME_WITH_TIMEZONE;
        } else if (datePart.isPresent() && timePart.isPresent()) {
            settings.m_outputType = CreateDateTimeNodeSettings.OutputType.DATE_TIME;
        } else if (datePart.isPresent()) {
            settings.m_outputType = CreateDateTimeNodeSettings.OutputType.DATE;
        } else if (timePart.isPresent()) {
            settings.m_outputType = CreateDateTimeNodeSettings.OutputType.TIME;
        } else {
            throw new IllegalArgumentException("Cannot determine output type from temporal value: " + start);
        }
    }

    private static Optional<LocalDate> extractDatePartFromTemporal(final Temporal t) {
        if (t instanceof LocalDate) {
            return Optional.of((LocalDate)t);
        } else if (t instanceof LocalDateTime) {
            return Optional.of(((LocalDateTime)t).toLocalDate());
        } else if (t instanceof ZonedDateTime) {
            return Optional.of(((ZonedDateTime)t).toLocalDate());
        } else {
            return Optional.empty();
        }
    }

    private static Optional<LocalTime> extractTimePartFromTemporal(final Temporal t) {
        if (t instanceof LocalTime) {
            return Optional.of((LocalTime)t);
        } else if (t instanceof LocalDateTime) {
            return Optional.of(((LocalDateTime)t).toLocalTime());
        } else if (t instanceof ZonedDateTime) {
            return Optional.of(((ZonedDateTime)t).toLocalTime());
        } else {
            return Optional.empty();
        }
    }

    private static Optional<ZoneId> extractZoneIdFromTemporal(final Temporal t) {
        if (t instanceof ZonedDateTime) {
            return Optional.of(((ZonedDateTime)t).getZone());
        } else {
            return Optional.empty();
        }
    }
}
