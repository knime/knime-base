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
 *   Nov 20, 2024 (david): created
 */
package org.knime.time.node.extract.durationperiod;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.time.duration.DurationCell;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.period.PeriodCell;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;

/**
 * Extractable field from a {@link DataCell} of type {@link DurationValue} or {@link PeriodValue}.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
enum ExtractableField {

        @Label(value = "Years", description = "The years component of a period.")
        YEARS(Period.class, Period::getYears, "Year", "Years"), //
        @Label(value = "Months", description = "The months component of a period.")
        MONTHS(Period.class, Period::getMonths, "Months", "Months"), //
        @Label(value = "Days", description = "The days component of a period.")
        DAYS(Period.class, Period::getDays, "Days", "Days"), //
        @Label(value = "Hours", description = "The hours component of a duration.")
        HOURS(Duration.class, Duration::toHours, "Hours", "Hours"), //
        @Label(value = "Minutes", description = "The minutes component of a duration.")
        MINUTES(Duration.class, Duration::toMinutesPart, "Minutes", "Minutes"), //
        @Label(value = "Seconds", description = "The seconds component of a duration.")
        SECONDS(Duration.class, Duration::toSecondsPart, "Seconds", "Seconds"), //
        @Label(value = "Millis", description = """
                The milliseconds component of a duration. In other words, a duration of 10.123456789 \
                seconds would have 123 milliseconds.
                """)
        MILLIS(Duration.class, d -> d.toMillisPart() % 1_000, "Millis", "Milliseconds"), //
        @Label(value = "Micros (all subseconds)", description = """
                Microseconds of a duration, including all of the subseconds. In other words, \
                a duration of 10.123456789 seconds would have 123456 microseconds.
                """)
        MICROS_ALL(Duration.class, d -> d.toNanosPart() / 1_000, "Micros", "Microseconds (all subseconds)"), //
        @Label(value = "Micros", description = """
                The microseconds component of a duration. In other words, a duration of 10.123456789 \
                seconds would have 456 microseconds.
                """)
        MICROS_PART(Duration.class, d -> (d.toNanosPart() / 1_000) % 1_000, null, "Microseconds"), //
        @Label(value = "Nanos (all subseconds)", description = """
                Nanoseconds of a duration, including all of the subseconds. In other words, \
                a duration of 10.123456789 seconds would have 123456789 nanoseconds.
                """)
        NANOS_ALL(Duration.class, Duration::toNanosPart, "Nanos", "Nanoseconds (all subseconds)"), //
        @Label(value = "Nanos", description = """
                The nanoseconds component of a duration. In other words, a duration of 10.123456789 \
                seconds would have 789 nanoseconds.
                """)
        NANOS_PART(Duration.class, d -> d.toNanosPart() % 1_000, null, "Nanoseconds");

    private Class<? extends TemporalAmount> m_extractionType;

    private final ToLongFunction<? extends TemporalAmount> m_extractor;

    private final String m_oldConfigValue;

    private final String m_niceName;

    <T extends TemporalAmount> ExtractableField( //
        final Class<T> extractionType, //
        final ToLongFunction<T> extractor, //
        final String oldConfigValue, //
        final String niceName //
    ) {
        this.m_extractionType = extractionType;
        this.m_extractor = extractor;
        this.m_oldConfigValue = oldConfigValue;
        this.m_niceName = niceName;
    }

    /**
     * If this returns true, the field can be extracted from the given {@link DataCell} using
     * {@link ExtractableField#extractFieldFrom(DataCell)}. If it returns false, the field cannot be extracted from the
     * given {@link DataCell} by this enum constant.
     *
     * @param dataValueType
     * @return
     */
    boolean isCompatibleWith(final DataType dataValueType) {
        return (m_extractionType == Duration.class && dataValueType.isCompatible(DurationValue.class)) //
            || (m_extractionType == Period.class && dataValueType.isCompatible(PeriodValue.class));
    }

    /**
     * Extract the field from the given {@link DataCell} and return it as a long. Will throw an
     * {@link IllegalArgumentException} if the field cannot be extracted, for example because you passed a
     * {@link PeriodCell} but this is a time-based field.
     *
     * @param value
     * @return
     */
    @SuppressWarnings("unchecked") // these casts should be fine
    long extractFieldFrom(final DataCell value) {
        var amount = extractFromCell(value);

        if (amount instanceof Duration d && m_extractionType == Duration.class) {
            return ((ToLongFunction<Duration>)this.m_extractor).applyAsLong(d.abs()) * (d.isNegative() ? -1 : 1);
        } else if (amount instanceof Period p && m_extractionType == Period.class) {
            return ((ToLongFunction<Period>)this.m_extractor).applyAsLong(p);
        } else {
            throw new IllegalArgumentException(
                "Cannot extract %s from %s".formatted(m_extractionType.getSimpleName(), amount));
        }
    }

    /**
     * A human-readable name for this field. Useful for user interface stuff.
     *
     * @return
     */
    String niceName() {
        return m_niceName;
    }

    /**
     * Get the value that was used for this field in the legacy node configuration. If this field wasn't supported in
     * the legacy node, this will return an empty optional.
     *
     * Note: this was also the default (and unchangeable) output column name that was used for this field in the legacy
     * node.
     *
     * @return
     */
    Optional<String> getOldConfigValue() {
        return Optional.ofNullable(m_oldConfigValue);
    }

    static ExtractableField getByOldConfigValue(final String oldConfigValue) {
        String oldConfigValuesSeparatedByCommas = Arrays.stream(values()) //
            .map(f -> f.m_oldConfigValue) //
            .filter(v -> v != null) //
            .collect(Collectors.joining(", "));

        return Arrays.stream(values()) //
            .filter(f -> f.m_oldConfigValue.equals(oldConfigValue)) //
            .findFirst() //
            .orElseThrow(() -> new IllegalArgumentException("Unknown old config value '%s'. Allowed values are: %s"
                .formatted(oldConfigValue, oldConfigValuesSeparatedByCommas)));
    }

    private static TemporalAmount extractFromCell(final DataCell value) {
        if (value instanceof DurationCell) {
            return ((DurationCell)value).getDuration();
        } else {
            return ((PeriodCell)value).getPeriod();
        }
    }
}
