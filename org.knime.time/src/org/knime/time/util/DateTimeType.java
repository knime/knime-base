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
 *   Oct 10, 2016 (simon): created
 */
package org.knime.time.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.widget.choices.Label;

/**
 * An enumeration that contains all different Date&Time types and holds its {@link DataType} and a representing name.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public enum DateTimeType {
        /**
         * Contains only a date.
         */
        @Label(value = "Date", description = "Outputs values of type Date")
        LOCAL_DATE("Date", LocalDateCellFactory.TYPE, LocalDate::from),
        /**
         * Contains only a time.
         */
        @Label(value = "Time", description = "Outputs values of type Time")
        LOCAL_TIME("Time", LocalTimeCellFactory.TYPE, LocalTime::from),
        /**
         * Contains a date and a time.
         */
        @Label(value = "Date & time", description = "Outputs values of type Date&amp;time (Local)")
        LOCAL_DATE_TIME("Date&time", LocalDateTimeCellFactory.TYPE, LocalDateTime::from),
        /**
         * Contains a date, a time and a time zone.
         */
        @Label(value = "Date & time & zone", description = "Outputs values of type Date&amp;time (Zoned)")
        ZONED_DATE_TIME("Date&time with zone", ZonedDateTimeCellFactory.TYPE, ZonedDateTime::from);

    private final String m_name;

    private final DataType m_dataType;

    private final TemporalQuery<TemporalAccessor> m_query;

    DateTimeType(final String name, final DataType dataType, final TemporalQuery<TemporalAccessor> query) {
        m_name = name;
        m_dataType = dataType;
        m_query = query;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_name;
    }

    /**
     * Returns the {@link DataType} of this enum constant.
     *
     * @return the representing {@link DataType}
     */
    public DataType getDataType() {
        return m_dataType;
    }

    /**
     * Returns the temporal query of this enum
     *
     * @return the temporal query
     */
    public TemporalQuery<TemporalAccessor> getQuery() {
        return m_query;
    }

    /**
     * Reference interface for the DateTimeType enum
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public interface Ref extends ParameterReference<DateTimeType> {
    }

    /**
     * Abstract Predicate for a DateTimeType. Using {@link Ref}
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public abstract static class IsDateTimeType implements EffectPredicateProvider {
        private DateTimeType m_type;

        /**
         * @param type The chosen DateTimeType that results in a True predicate
         */
        protected IsDateTimeType(final DateTimeType type) {
            this.m_type = type;
        }

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(Ref.class).isOneOf(this.m_type);
        }
    }

    /**
     * Abstract Predicate for a DateTimeType and a (negated) Boolean Reference. Using {@link Ref}
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     * @param <T> Reference class
     */
    public abstract static class IsDateTimeTypeAndNotDisabled<T extends ParameterReference<Boolean>> extends IsDateTimeType {
        private Class<T> m_checkbox;

        /**
         * @param type The chosen DateTimeType
         * @param checkbox A reference to a boolean field
         */
        protected IsDateTimeTypeAndNotDisabled(final DateTimeType type, final Class<T> checkbox) {
            super(type);
            this.m_checkbox = checkbox;
        }

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return super.init(i).and(i.getBoolean(m_checkbox).isFalse());
        }
    }

    /**
     * Predicate for the LOCAL_TIME value. Using {@link Ref}
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public static final class IsLocalTime extends IsDateTimeType {
        IsLocalTime() {
            super(DateTimeType.LOCAL_TIME);
        }
    }

    /**
     * Predicate for the LOCAL_DATE value. Using {@link Ref}
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public static final class IsLocalDate extends IsDateTimeType {
        IsLocalDate() {
            super(DateTimeType.LOCAL_DATE);
        }
    }

    /**
     * Predicate for the LOCAL_DATE_TIME value. Using {@link Ref}
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public static final class IsLocalDateTime extends IsDateTimeType {
        IsLocalDateTime() {
            super(DateTimeType.LOCAL_DATE_TIME);
        }
    }

    /**
     * Predicate for the ZONED_DATE_TIME value. Using {@link Ref}
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public static final class IsZonedDateTime extends IsDateTimeType {
        IsZonedDateTime() {
            super(DateTimeType.ZONED_DATE_TIME);
        }
    }
}
