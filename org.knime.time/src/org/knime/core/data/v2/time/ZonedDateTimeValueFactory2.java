package org.knime.core.data.v2.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.time.DateTimeValueInterfaces.ZonedDateTimeReadValue;
import org.knime.core.data.v2.time.DateTimeValueInterfaces.ZonedDateTimeWriteValue;
import org.knime.core.table.access.IntAccess.IntReadAccess;
import org.knime.core.table.access.IntAccess.IntWriteAccess;
import org.knime.core.table.access.LongAccess.LongReadAccess;
import org.knime.core.table.access.LongAccess.LongWriteAccess;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.StructDataSpec;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link ZonedDateTimeCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.5
 */
public final class ZonedDateTimeValueFactory2
    implements ValueFactory<StructReadAccess, StructWriteAccess> {

    /** A stateless instance of {@link ZonedDateTimeValueFactory2} */
    public static final ZonedDateTimeValueFactory2 INSTANCE = new ZonedDateTimeValueFactory2();

    @Override
    public ZonedDateTimeReadValue createReadValue(final StructReadAccess access) {
        return new DefaultZonedDateTimeReadValue(access);
    }

    @Override
    public ZonedDateTimeWriteValue createWriteValue(final StructWriteAccess access) {
        return new DefaultZonedDateTimeWriteValue(access);
    }

    @Override
    public StructDataSpec getSpec() {
        // [day of epoch, nano of day, zone offset, zone id]
        return new StructDataSpec(DataSpec.longSpec(), DataSpec.longSpec(), DataSpec.intSpec(), DataSpec.stringSpec());
    }

    private static final class DefaultZonedDateTimeReadValue implements ZonedDateTimeReadValue {

        private final LongReadAccess m_dayOfEpoch;

        private final LongReadAccess m_nanoOfDay;

        private final IntReadAccess m_zoneOffset;

        private final StringReadAccess m_zoneId;

        private static final long NANOSECONDS_IN_A_DAY = 24 * 60 * 60 * 1_000_000_000L;

        private DefaultZonedDateTimeReadValue(final StructReadAccess access) {
            m_dayOfEpoch = access.getAccess(0);
            m_nanoOfDay = access.getAccess(1);
            m_zoneOffset = access.getAccess(2);
            m_zoneId = access.getAccess(3);
        }

        @Override
        public DataCell getDataCell() {
            return ZonedDateTimeCellFactory.create(getZonedDateTime());
        }

        @Override
        public ZonedDateTime getZonedDateTime() {
            var localDate = LocalDate.ofEpochDay(m_dayOfEpoch.getLongValue());
            LocalTime localTime;

            if (m_nanoOfDay.getLongValue() > NANOSECONDS_IN_A_DAY) {

                // Handle the case where nanoOfDay is out of bounds
                long remainderNano = m_nanoOfDay.getLongValue() % NANOSECONDS_IN_A_DAY;
                localTime = LocalTime.ofNanoOfDay(remainderNano);
            } else if (m_nanoOfDay.getLongValue() == NANOSECONDS_IN_A_DAY) {
                long lastNanoOfDay = m_nanoOfDay.getLongValue() - 1;
                localTime = LocalTime.ofNanoOfDay(lastNanoOfDay);
            } else {
                localTime = LocalTime.ofNanoOfDay(m_nanoOfDay.getLongValue());
            }
            var localDateTime = LocalDateTime.of(localDate, localTime);
            var zoneOffset = ZoneOffset.ofTotalSeconds(m_zoneOffset.getIntValue());
            var zoneId = ZoneId.of(m_zoneId.getStringValue());
            return ZonedDateTime.ofInstant(localDateTime, zoneOffset, zoneId);
        }

        @Override
        public String toString() {
            return getZonedDateTime().toString();
        }

        @Override
        public String getStringValue() {
            return getZonedDateTime().toString();
        }
    }

    private static final class DefaultZonedDateTimeWriteValue implements ZonedDateTimeWriteValue {

        private final LongWriteAccess m_dayOfEpoch;

        private final LongWriteAccess m_nanoOfDay;

        private final IntWriteAccess m_zoneOffset;

        private final StringWriteAccess m_zoneId;

        private DefaultZonedDateTimeWriteValue(final StructWriteAccess access) {
            m_dayOfEpoch = access.getWriteAccess(0);
            m_nanoOfDay = access.getWriteAccess(1);
            m_zoneOffset = access.getWriteAccess(2);
            m_zoneId = access.getWriteAccess(3);
        }

        @Override
        public void setValue(final ZonedDateTimeValue value) {
            setZonedDateTime(value.getZonedDateTime());
        }

        @Override
        public void setZonedDateTime(final ZonedDateTime date) {
            m_dayOfEpoch.setLongValue(date.getLong(ChronoField.EPOCH_DAY));
            m_nanoOfDay.setLongValue(date.getLong(ChronoField.NANO_OF_DAY));
            m_zoneOffset.setIntValue(date.getOffset().get(ChronoField.OFFSET_SECONDS));
            m_zoneId.setStringValue(date.getZone().getId());
        }

    }
}