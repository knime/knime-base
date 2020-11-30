package org.knime.core.data.v2.time;

import java.time.LocalDate;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.ObjectAccess.LocalDateAccessSpec;
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectWriteAccess;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link LocalDateCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class LocalDateValueFactory
    implements ValueFactory<ObjectReadAccess<LocalDate>, ObjectWriteAccess<LocalDate>> {

    /** A stateless instance of {@link LocalDateValueFactory} */
    public static final LocalDateValueFactory INSTANCE = new LocalDateValueFactory();

    @Override
    public LocalDateReadValue createReadValue(final ObjectReadAccess<LocalDate> access) {
        return new DefaultLocalDateReadValue(access);
    }

    @Override
    public LocalDateWriteValue createWriteValue(final ObjectWriteAccess<LocalDate> access) {
        return new DefaultLocalDateWriteValue(access);
    }

    @Override
    public LocalDateAccessSpec getSpec() {
        return LocalDateAccessSpec.INSTANCE;
    }

    /**
     * {@link ReadValue} equivalent to {@link LocalDateCell}.
     *
     * @since 4.3
     */
    public static interface LocalDateReadValue extends ReadValue, LocalDateValue, BoundedValue, StringValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link LocalDateCell}.
     *
     * @since 4.3
     */
    public static interface LocalDateWriteValue extends WriteValue<LocalDateValue> {

        /**
         * @param date the date to set
         */
        void setLocalDate(LocalDate date);
    }

    private static final class DefaultLocalDateReadValue implements LocalDateReadValue {

        private final ObjectReadAccess<LocalDate> m_access;

        private DefaultLocalDateReadValue(final ObjectReadAccess<LocalDate> access) {
            m_access = access;
        }

        @Override
        public DataCell getDataCell() {
            return LocalDateCellFactory.create(m_access.getObject());
        }

        @Override
        public LocalDate getLocalDate() {
            return m_access.getObject();
        }

        @Override
        public String getStringValue() {
            return m_access.getObject().toString();
        }
    }

    private static final class DefaultLocalDateWriteValue implements LocalDateWriteValue {

        private final ObjectWriteAccess<LocalDate> m_access;

        private DefaultLocalDateWriteValue(final ObjectWriteAccess<LocalDate> access) {
            m_access = access;
        }

        @Override
        public void setValue(final LocalDateValue value) {
            m_access.setObject(value.getLocalDate());
        }

        @Override
        public void setLocalDate(final LocalDate date) {
            m_access.setObject(date);
        }
    }
}