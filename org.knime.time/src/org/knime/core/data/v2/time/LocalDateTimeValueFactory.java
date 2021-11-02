package org.knime.core.data.v2.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.table.access.LongAccess.LongReadAccess;
import org.knime.core.table.access.LongAccess.LongWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.StructDataSpec;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link LocalDateTimeCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class LocalDateTimeValueFactory
    implements ValueFactory<StructReadAccess, StructWriteAccess> {

    /** A stateless instance of {@link LocalDateTimeValueFactory} */
    public static final LocalDateTimeValueFactory INSTANCE = new LocalDateTimeValueFactory();

    @Override
    public LocalDateTimeReadValue createReadValue(final StructReadAccess access) {
        return new DefaultLocalDateTimeReadValue(access);
    }

    @Override
    public LocalDateTimeWriteValue createWriteValue(final StructWriteAccess access) {
        return new DefaultLocalDateTimeWriteValue(access);
    }

    @Override
    public StructDataSpec getSpec() {
        // TODO introduce time nano dataspec for second field
        // [epoch day, nano of day]
        return new StructDataSpec(DataSpec.longSpec(), DataSpec.longSpec());
    }

    /**
     * {@link ReadValue} equivalent to {@link LocalDateTimeCell}.
     *
     * @since 4.3
     */
    public static interface LocalDateTimeReadValue extends ReadValue, LocalDateTimeValue, StringValue, BoundedValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link LocalDateTimeCell}.
     *
     * @since 4.3
     */
    public static interface LocalDateTimeWriteValue extends WriteValue<LocalDateTimeValue> {

        /**
         * @param dateTime the date and time to set
         */
        void setLocalDateTime(LocalDateTime dateTime);

    }

    private static final class DefaultLocalDateTimeReadValue implements LocalDateTimeReadValue {


        private final LongReadAccess m_dayOfEpoch;

        private final LongReadAccess m_nanoOfDay;

        private DefaultLocalDateTimeReadValue(final StructReadAccess access) {
            m_dayOfEpoch = access.getAccess(0);
            m_nanoOfDay = access.getAccess(1);
        }

        @Override
        public DataCell getDataCell() {
            return LocalDateTimeCellFactory.create(getLocalDateTime());
        }

        @Override
        public LocalDateTime getLocalDateTime() {
            return LocalDateTime.of(LocalDate.ofEpochDay(m_dayOfEpoch.getLongValue()),
                LocalTime.ofNanoOfDay(m_nanoOfDay.getLongValue()));
        }

        @Override
        public String getStringValue() {
            return getLocalDateTime().toString();
        }

    }

    private static final class DefaultLocalDateTimeWriteValue implements LocalDateTimeWriteValue {


        private final LongWriteAccess m_dayOfEpoch;

        private final LongWriteAccess m_nanoOfDay;

        private DefaultLocalDateTimeWriteValue(final StructWriteAccess access) {
            m_dayOfEpoch = access.getWriteAccess(0);
            m_nanoOfDay = access.getWriteAccess(1);
        }

        @Override
        public void setValue(final LocalDateTimeValue value) {
            setLocalDateTime(value.getLocalDateTime());
        }

        @Override
        public void setLocalDateTime(final LocalDateTime dateTime) {
            m_dayOfEpoch.setLongValue(dateTime.getLong(ChronoField.EPOCH_DAY));
            m_nanoOfDay.setLongValue(dateTime.getLong(ChronoField.NANO_OF_DAY));
        }

    }
}