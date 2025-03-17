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
import org.knime.core.data.def.LongCell.LongCellFactory;
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

        @Label(value = "Years", description = "Extracts the years component of a date-based duration.")
        YEARS(Period.class, (PeriodToLongCellExtractor)Period::getYears, "Years"), //
        @Label(value = "Months", description = "Extracts the months component of a date-based duration.")
        MONTHS(Period.class, (PeriodToLongCellExtractor)Period::getMonths, "Months"), //
        @Label(value = "Days", description = "Extracts the days component of a date-based duration.")
        DAYS(Period.class, (PeriodToLongCellExtractor)Period::getDays, "Days"), //
        @Label(value = "Hours", description = "Extracts the hours component of a time-based duration.")
        HOURS(Duration.class, (DurationToLongCellExtractor)Duration::toHours, "Hours"), //
        @Label(value = "Minutes", description = "Extracts the minutes component of a time-based duration.")
        MINUTES(Duration.class, (DurationToLongCellExtractor)Duration::toMinutesPart, "Minutes"), //
        @Label(value = "Seconds", description = "Extracts the seconds component of a time-based duration.")
        SECONDS(Duration.class, (DurationToLongCellExtractor)Duration::toSecondsPart, "Seconds"), //
        @Label(value = "Milliseconds", description = """
                Extracts the milliseconds component of a time-based duration. In other words, a duration of \
                10.123456789 seconds would have 123 milliseconds.
                """)
        MILLIS(Duration.class, (DurationToLongCellExtractor)d -> d.toMillisPart() % 1_000, "Millis"), //
        @Label(value = "Microseconds (all subseconds)", description = """
                Extracts the microseconds of a time-based duration, including all of the subseconds. In other words, \
                a duration of 10.123456789 seconds would have 123456 microseconds.
                """)
        MICROS_ALL(Duration.class, (DurationToLongCellExtractor)d -> d.toNanosPart() / 1_000, "Micros"), //
        @Label(value = "Microseconds", description = """
                Extracts the microseconds component of a time-based duration. In other words, a duration of \
                10.123456789 seconds would have 456 microseconds.
                """)
        MICROS_PART(Duration.class, (DurationToLongCellExtractor)d -> (d.toNanosPart() / 1_000) % 1_000, null), //
        @Label(value = "Nanoseconds (all subseconds)", description = """
                Extracts the Nanoseconds of a time-based duration, including all of the subseconds. In other words, \
                a duration of 10.123456789 seconds would have 123456789 nanoseconds.
                """)
        NANOS_ALL(Duration.class, (DurationToLongCellExtractor)Duration::toNanosPart, "Nanos"), //
        @Label(value = "Nanoseconds", description = """
                Extracts the nanoseconds component of a time-based duration. In other words, a duration of \
                10.123456789 seconds would have 789 nanoseconds.
                """)
        NANOS_PART(Duration.class, (DurationToLongCellExtractor)d -> d.toNanosPart() % 1_000, null);

    interface NumberCellExtractor {
        DataCell extractNumberCellFromTemporalAmount(TemporalAmount amount);
    }

    interface LongCellExtractor<T extends TemporalAmount> extends NumberCellExtractor, ToLongFunction<T> {

    }

    interface PeriodToLongCellExtractor extends LongCellExtractor<Period> {

        @Override
        default DataCell extractNumberCellFromTemporalAmount(final TemporalAmount amount) {
            return LongCellFactory.create(this.applyAsLong((Period)amount));
        }
    }

    interface DurationToLongCellExtractor extends LongCellExtractor<Duration> {

        @Override
        default DataCell extractNumberCellFromTemporalAmount(final TemporalAmount amount) {
            return LongCellFactory
                .create(this.applyAsLong(((Duration)amount).abs()) * (((Duration)amount).isNegative() ? -1 : 1));
        }
    }

    private Class<? extends TemporalAmount> m_extractionType;

    private final NumberCellExtractor m_extractor;

    private final String m_oldConfigValue;

    <T extends TemporalAmount> ExtractableField( //
        final Class<T> extractionType, //
        final NumberCellExtractor extractor, //
        final String oldConfigValue //
    ) {
        this.m_extractionType = extractionType;
        this.m_extractor = extractor;
        this.m_oldConfigValue = oldConfigValue;
    }

    /**
     * If this returns true, the field can be extracted from the given {@link DataCell} using
     * {@link ExtractableField#extractLongFrom(DataCell)}. If it returns false, the field cannot be extracted from the
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
    DataCell extractNumberCellFrom(final DataCell value) {
        var amount = extractFromCell(value);
        return this.m_extractor.extractNumberCellFromTemporalAmount(amount);
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

    boolean extractsLongCell() {
        return this.m_extractor instanceof LongCellExtractor;
    }

    static ExtractableField getByOldConfigValue(final String oldConfigValue) {
        String oldConfigValuesSeparatedByCommas = Arrays.stream(values()) //
            .map(f -> f.m_oldConfigValue) //
            .filter(v -> v != null) //
            .collect(Collectors.joining(", "));

        return Arrays.stream(values()) //
            .filter(f -> f.m_oldConfigValue != null && f.m_oldConfigValue.equals(oldConfigValue)) //
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
