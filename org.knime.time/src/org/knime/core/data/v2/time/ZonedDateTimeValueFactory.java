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
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectWriteAccess;
import org.knime.core.data.v2.access.ObjectAccess.ZonedDateTimeAccessSpec;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link ZonedDateTimeCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class ZonedDateTimeValueFactory
    implements ValueFactory<ObjectReadAccess<ZonedDateTime>, ObjectWriteAccess<ZonedDateTime>> {

    /** A stateless instance of {@link ZonedDateTimeValueFactory} */
    public static final ZonedDateTimeValueFactory INSTANCE = new ZonedDateTimeValueFactory();

    @Override
    public ZonedDateTimeReadValue createReadValue(final ObjectReadAccess<ZonedDateTime> access) {
        return new DefaultZonedDateTimeReadValue(access);
    }

    @Override
    public ZonedDateTimeWriteValue createWriteValue(final ObjectWriteAccess<ZonedDateTime> access) {
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

        private final ObjectReadAccess<ZonedDateTime> m_access;

        private DefaultZonedDateTimeReadValue(final ObjectReadAccess<ZonedDateTime> access) {
            m_access = access;
        }

        @Override
        public DataCell getDataCell() {
            return ZonedDateTimeCellFactory.create(m_access.getObject());
        }

        @Override
        public ZonedDateTime getZonedDateTime() {
            return m_access.getObject();
        }

        @Override
        public String toString() {
            return m_access.getObject().toString();
        }
    }

    private static final class DefaultZonedDateTimeWriteValue implements ZonedDateTimeWriteValue {

        private final ObjectWriteAccess<ZonedDateTime> m_access;

        private DefaultZonedDateTimeWriteValue(final ObjectWriteAccess<ZonedDateTime> access) {
            m_access = access;
        }

        @Override
        public void setValue(final ZonedDateTimeValue value) {
            m_access.setObject(value.getZonedDateTime());
        }

        @Override
        public void setZonedDateTime(final ZonedDateTime date) {
            m_access.setObject(date);
        }
    }
}