package org.knime.core.data.v2.time;

import java.time.LocalDateTime;

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
import org.knime.core.table.access.LocalDateTimeAccess.LocalDateTimeReadAccess;
import org.knime.core.table.access.LocalDateTimeAccess.LocalDateTimeWriteAccess;
import org.knime.core.table.schema.LocalDateTimeDataSpec;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link LocalDateTimeCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class LocalDateTimeValueFactory
    implements ValueFactory<LocalDateTimeReadAccess, LocalDateTimeWriteAccess> {

    /** A stateless instance of {@link LocalDateTimeValueFactory} */
    public static final LocalDateTimeValueFactory INSTANCE = new LocalDateTimeValueFactory();

    @Override
    public LocalDateTimeReadValue createReadValue(final LocalDateTimeReadAccess access) {
        return new DefaultLocalDateTimeReadValue(access);
    }

    @Override
    public LocalDateTimeWriteValue createWriteValue(final LocalDateTimeWriteAccess access) {
        return new DefaultLocalDateTimeWriteValue(access);
    }

    @Override
    public LocalDateTimeDataSpec getSpec() {
        return LocalDateTimeDataSpec.INSTANCE;
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

        private final LocalDateTimeReadAccess m_access;

        private DefaultLocalDateTimeReadValue(final LocalDateTimeReadAccess access) {
            m_access = access;
        }

        @Override
        public DataCell getDataCell() {
            return LocalDateTimeCellFactory.create(m_access.getLocalDateTimeValue());
        }

        @Override
        public LocalDateTime getLocalDateTime() {
            return m_access.getLocalDateTimeValue();
        }

        @Override
        public String getStringValue() {
            return m_access.getLocalDateTimeValue().toString();
        }

    }

    private static final class DefaultLocalDateTimeWriteValue implements LocalDateTimeWriteValue {

        private final LocalDateTimeWriteAccess m_access;

        private DefaultLocalDateTimeWriteValue(final LocalDateTimeWriteAccess access) {
            m_access = access;
        }

        @Override
        public void setValue(final LocalDateTimeValue value) {
            m_access.setLocalDateTimeValue(value.getLocalDateTime());
        }

        @Override
        public void setLocalDateTime(final LocalDateTime dateTime) {
            m_access.setLocalDateTimeValue(dateTime);
        }

    }
}