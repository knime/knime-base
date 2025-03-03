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
 *   Dec 12, 2024 (david): created
 */
package org.knime.time.util;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;

/**
 * A unit of time that can be used to represent a duration in a specific unit, either truncated to an integer or exactly
 * as a double.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public enum TimeBasedGranularityUnit {
        @Label("Hours")
        HOURS(TimeUnit.HOURS), //
        @Label("Minutes")
        MINUTES(TimeUnit.MINUTES), //
        @Label("Seconds")
        SECONDS(TimeUnit.SECONDS), //
        @Label("Milliseconds")
        MILLISECONDS(TimeUnit.MILLISECONDS), //
        @Label("Microseconds")
        MICROSECONDS(TimeUnit.MICROSECONDS), //
        @Label("Nanoseconds")
        NANOSECONDS(TimeUnit.NANOSECONDS);

    private final TimeUnit m_timeUnit;

    TimeBasedGranularityUnit(final TimeUnit timeUnit) {
        this.m_timeUnit = timeUnit;
    }

    /**
     * Get the granularity
     *
     * @return the granularity
     */
    public Granularity getGranularity() {
        return Arrays.stream(Granularity.values()).filter(value -> this == value.getTimeBasedGranularityUnit())
            .findFirst().orElseThrow();
    }

    /**
     * Get the duration in the specified unit, as a double.
     *
     * @param duration
     * @return the duration in the specified unit, as a double
     */
    public double getConversionExact(final Duration duration) {
        var convertedDuration = convertDuration(m_timeUnit, duration);

        return convertedDuration.integerPart + convertedDuration.decimalPart;
    }

    /**
     * Get the duration in the specified unit, truncated to the integer part.
     *
     * @param duration
     * @return the duration in the specified unit, truncated to the integer part
     */
    public long getConversionFloored(final Duration duration) {
        var convertedDuration = convertDuration(m_timeUnit, duration);

        return convertedDuration.integerPart;
    }

    /**
     * A record that represents a number as an integer part and a decimal part.
     *
     * @param integerPart the integer part
     * @param decimalPart the decimal part, between [0, 1).
     */
    public static record IntegerAndDecimalPart(long integerPart, double decimalPart) {
    }

    /**
     * Convert the duration to the specified unit, returning the integer part and the decimal part separately so as to
     * avoid floating point precision loss where possible.
     *
     * @param unit
     * @param duration
     * @return the duration in the specified unit, as an integer and a decimal part
     * @throws ArithmeticException if numeric overflow occurs
     */
    public static IntegerAndDecimalPart convertDuration(final TimeUnit unit, final Duration duration)
        throws ArithmeticException {
        // Get the duration in the specified unit as an integer, e.g. '3 hours'
        long durationInSpecifiedUnit = unit.convert(duration);
        validateConversionResult(durationInSpecifiedUnit);

        // Figure out the fractional part
        var leftoverDuration = duration.minus(durationInSpecifiedUnit, unit.toChronoUnit());
        long leftoverNanos = TimeUnit.NANOSECONDS.convert(leftoverDuration);
        validateConversionResult(leftoverNanos);

        double leftOverDurationAsFractionOfSpecifiedUnit = leftoverNanos / (double)unit.toNanos(1);
        return new IntegerAndDecimalPart(durationInSpecifiedUnit, leftOverDurationAsFractionOfSpecifiedUnit);
    }

    private static void validateConversionResult(final long conversionResult) throws ArithmeticException {
        if (conversionResult == Long.MIN_VALUE || conversionResult == Long.MAX_VALUE) {
            throw new ArithmeticException("Numeric overflow during conversion.");
        }
    }
}
