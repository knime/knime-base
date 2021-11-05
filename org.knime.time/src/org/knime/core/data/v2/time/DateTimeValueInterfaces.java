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
 *   Nov 5, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.v2.time;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.time.duration.DurationCell;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.period.PeriodCell;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.WriteValue;

/**
 * Defines all the {@link ReadValue} and {@link WriteValue} interfaces for date & time data types.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DateTimeValueInterfaces {

    /**
     * {@link ReadValue} equivalent to {@link DurationCell}.
     *
     * @since 4.5
     */
    public static interface DurationReadValue extends ReadValue, DurationValue, BoundedValue, StringValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link DurationCell}.
     *
     * @since 4.5
     */
    public static interface DurationWriteValue extends WriteValue<DurationValue> {

        /**
         * @param duration the duration to set
         */
        void setDuration(Duration duration);

    }

    /**
     * {@link ReadValue} equivalent to {@link LocalDateTimeCell}.
     *
     * @since 4.5
     */
    public static interface LocalDateTimeReadValue extends ReadValue, LocalDateTimeValue, StringValue, BoundedValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link LocalDateTimeCell}.
     *
     * @since 4.5
     */
    public static interface LocalDateTimeWriteValue extends WriteValue<LocalDateTimeValue> {

        /**
         * @param dateTime the date and time to set
         */
        void setLocalDateTime(LocalDateTime dateTime);

    }

    /**
     * {@link ReadValue} equivalent to {@link LocalDateCell}.
     *
     * @since 4.5
     */
    public static interface LocalDateReadValue extends ReadValue, LocalDateValue, BoundedValue, StringValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link LocalDateCell}.
     *
     * @since 4.5
     */
    public static interface LocalDateWriteValue extends WriteValue<LocalDateValue> {

        /**
         * @param date the date to set
         */
        void setLocalDate(LocalDate date);

    }

    /**
     * {@link ReadValue} equivalent to {@link LocalTimeCell}.
     *
     * @since 4.5
     */
    public static interface LocalTimeReadValue extends ReadValue, LocalTimeValue, BoundedValue, StringValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link LocalTimeCell}.
     *
     * @since 4.5
     */
    public static interface LocalTimeWriteValue extends WriteValue<LocalTimeValue> {

        /**
         * @param time the time to set
         */
        void setLocalTime(LocalTime time);

    }

    /**
     * {@link ReadValue} equivalent to {@link PeriodCell}.
     *
     * @since 4.5
     */
    public static interface PeriodReadValue extends ReadValue, PeriodValue, StringValue, BoundedValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link PeriodCell}.
     *
     * @since 4.5
     */
    public static interface PeriodWriteValue extends WriteValue<PeriodValue> {

        /**
         * @param period the period to set
         */
        void setPeriod(Period period);

    }

    /**
     * {@link WriteValue} equivalent to {@link ZonedDateTimeCell}.
     *
     * @since 4.5
     */
    public interface ZonedDateTimeWriteValue extends WriteValue<ZonedDateTimeValue> {

        /**
         * @param date the date to set
         */
        void setZonedDateTime(ZonedDateTime date);

    }

    /**
     * {@link ReadValue} equivalent to {@link ZonedDateTimeCell}.
     *
     * @since 4.3
     */
    public interface ZonedDateTimeReadValue extends ReadValue, ZonedDateTimeValue {
    }

}
