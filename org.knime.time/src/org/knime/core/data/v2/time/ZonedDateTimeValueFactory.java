package org.knime.core.data.v2.time;

import java.time.ZonedDateTime;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.ZonedDateTimeAccess.ZonedDateTimeAccessSpec;
import org.knime.core.data.v2.access.ZonedDateTimeAccess.ZonedDateTimeReadAccess;
import org.knime.core.data.v2.access.ZonedDateTimeAccess.ZonedDateTimeWriteAccess;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link ZonedDateTimeCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class ZonedDateTimeValueFactory
    implements ValueFactory<ZonedDateTimeReadAccess, ZonedDateTimeWriteAccess> {

    /** A stateless instance of {@link ZonedDateTimeValueFactory} */
    public static final ZonedDateTimeValueFactory INSTANCE = new ZonedDateTimeValueFactory();

    @Override
    public ZonedDateTimeReadValue createReadValue(final ZonedDateTimeReadAccess access) {
        return new DefaultZonedDateTimeReadValue(access);
    }

    @Override
    public ZonedDateTimeWriteValue createWriteValue(final ZonedDateTimeWriteAccess access) {
        return new DefaultZonedDateTimeWriteValue(access);
    }

    @Override
    public ZonedDateTimeAccessSpec getSpec() {
        return ZonedDateTimeAccessSpec.INSTANCE;
    }

    /**
     * {@link ReadValue} equivalent to {@link ZonedDateTimeCell}.
     *
     * @since 4.3
     */
    public static interface ZonedDateTimeReadValue extends ReadValue, ZonedDateTimeValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link ZonedDateTimeCell}.
     *
     * @since 4.3
     */
    public static interface ZonedDateTimeWriteValue extends WriteValue<ZonedDateTimeValue> {

        /**
         * @param date the date to set
         */
        void setZonedDateTime(ZonedDateTime date);
    }

    private static final class DefaultZonedDateTimeReadValue implements ZonedDateTimeReadValue {

        private final ZonedDateTimeReadAccess m_access;

        private DefaultZonedDateTimeReadValue(final ZonedDateTimeReadAccess access) {
            m_access = access;
        }

        @Override
        public DataCell getDataCell() {
            return ZonedDateTimeCellFactory.create(m_access.getZonedDateTime());
        }

        @Override
        public ZonedDateTime getZonedDateTime() {
            return m_access.getZonedDateTime();
        }
    }

    private static final class DefaultZonedDateTimeWriteValue implements ZonedDateTimeWriteValue {

        private final ZonedDateTimeWriteAccess m_access;

        private DefaultZonedDateTimeWriteValue(final ZonedDateTimeWriteAccess access) {
            m_access = access;
        }

        @Override
        public void setValue(final ZonedDateTimeValue value) {
            m_access.setZonedDateTime(value.getZonedDateTime());
        }

        @Override
        public void setZonedDateTime(final ZonedDateTime date) {
            m_access.setZonedDateTime(date);
        }
    }
}