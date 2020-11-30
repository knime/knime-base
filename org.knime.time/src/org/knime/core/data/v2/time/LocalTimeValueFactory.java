package org.knime.core.data.v2.time;

import java.time.LocalTime;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.ObjectAccess.LocalTimeAccessSpec;
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectWriteAccess;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link LocalTimeCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class LocalTimeValueFactory
    implements ValueFactory<ObjectReadAccess<LocalTime>, ObjectWriteAccess<LocalTime>> {

    /** A stateless instance of {@link LocalTimeValueFactory} */
    public static final LocalTimeValueFactory INSTANCE = new LocalTimeValueFactory();

    @Override
    public LocalTimeReadValue createReadValue(final ObjectReadAccess<LocalTime> access) {
        return new DefaultLocalTimeReadValue(access);
    }

    @Override
    public LocalTimeWriteValue createWriteValue(final ObjectWriteAccess<LocalTime> access) {
        return new DefaultLocalTimeWriteValue(access);
    }

    @Override
    public LocalTimeAccessSpec getSpec() {
        return LocalTimeAccessSpec.INSTANCE;
    }

    /**
     * {@link ReadValue} equivalent to {@link LocalTimeCell}.
     *
     * @since 4.3
     */
    public static interface LocalTimeReadValue extends ReadValue, LocalTimeValue, BoundedValue, StringValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link LocalTimeCell}.
     *
     * @since 4.3
     */
    public static interface LocalTimeWriteValue extends WriteValue<LocalTimeValue> {

        /**
         * @param time the time to set
         */
        void setLocalTime(LocalTime time);
    }

    private static final class DefaultLocalTimeReadValue implements LocalTimeReadValue {

        private final ObjectReadAccess<LocalTime> m_access;

        private DefaultLocalTimeReadValue(final ObjectReadAccess<LocalTime> access) {
            m_access = access;
        }

        @Override
        public DataCell getDataCell() {
            return LocalTimeCellFactory.create(m_access.getObject());
        }

        @Override
        public LocalTime getLocalTime() {
            return m_access.getObject();
        }

        @Override
        public String getStringValue() {
            return m_access.getObject().toString();
        }
    }

    private static final class DefaultLocalTimeWriteValue implements LocalTimeWriteValue {

        private final ObjectWriteAccess<LocalTime> m_access;

        private DefaultLocalTimeWriteValue(final ObjectWriteAccess<LocalTime> access) {
            m_access = access;
        }

        @Override
        public void setValue(final LocalTimeValue value) {
            m_access.setObject(value.getLocalTime());
        }

        @Override
        public void setLocalTime(final LocalTime time) {
            m_access.setObject(time);
        }
    }
}