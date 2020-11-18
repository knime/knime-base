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
import org.knime.core.data.v2.access.LocalDateAccess.LocalDateAccessSpec;
import org.knime.core.data.v2.access.LocalDateAccess.LocalDateReadAccess;
import org.knime.core.data.v2.access.LocalDateAccess.LocalDateWriteAccess;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link LocalDateCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class LocalDateValueFactory implements ValueFactory<LocalDateReadAccess, LocalDateWriteAccess> {

    /** A stateless instance of {@link LocalDateValueFactory} */
    public static final LocalDateValueFactory INSTANCE = new LocalDateValueFactory();

    @Override
    public LocalDateReadValue createReadValue(final LocalDateReadAccess access) {
        return new DefaultLocalDateReadValue(access);
    }

    @Override
    public LocalDateWriteValue createWriteValue(final LocalDateWriteAccess access) {
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

        private final LocalDateReadAccess m_access;

        private DefaultLocalDateReadValue(final LocalDateReadAccess access) {
            m_access = access;
        }

        @Override
        public DataCell getDataCell() {
            return LocalDateCellFactory.create(m_access.getLocalDate());
        }

        @Override
        public LocalDate getLocalDate() {
            return m_access.getLocalDate();
        }

        @Override
        public String getStringValue() {
            return m_access.getLocalDate().toString();
        }
    }

    private static final class DefaultLocalDateWriteValue implements LocalDateWriteValue {

        private final LocalDateWriteAccess m_access;

        private DefaultLocalDateWriteValue(final LocalDateWriteAccess access) {
            m_access = access;
        }

        @Override
        public void setValue(final LocalDateValue value) {
            m_access.setLocalDate(value.getLocalDate());
        }

        @Override
        public void setLocalDate(final LocalDate date) {
            m_access.setLocalDate(date);
        }
    }
}