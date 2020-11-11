package org.knime.core.data.v2.time;

import java.time.LocalTime;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.LocalTimeAccess.LocalTimeAccessSpec;
import org.knime.core.data.v2.access.LocalTimeAccess.LocalTimeReadAccess;
import org.knime.core.data.v2.access.LocalTimeAccess.LocalTimeWriteAccess;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link LocalTimeCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class LocalTimeValueFactory implements ValueFactory<LocalTimeReadAccess, LocalTimeWriteAccess> {

    /** A stateless instance of {@link LocalTimeValueFactory} */
    public static final LocalTimeValueFactory INSTANCE = new LocalTimeValueFactory();

    @Override
    public LocalTimeReadValue createReadValue(final LocalTimeReadAccess access) {
        return new DefaultLocalTimeReadValue(access);
    }

    @Override
    public LocalTimeWriteValue createWriteValue(final LocalTimeWriteAccess access) {
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
    public static interface LocalTimeReadValue extends ReadValue, LocalTimeValue {
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

        private final LocalTimeReadAccess m_access;

        private DefaultLocalTimeReadValue(final LocalTimeReadAccess access) {
            m_access = access;
        }

        @Override
        public DataCell getDataCell() {
            return LocalTimeCellFactory.create(m_access.getLocalTime());
        }

        @Override
        public LocalTime getLocalTime() {
            return m_access.getLocalTime();
        }
    }

    private static final class DefaultLocalTimeWriteValue implements LocalTimeWriteValue {

        private final LocalTimeWriteAccess m_access;

        private DefaultLocalTimeWriteValue(final LocalTimeWriteAccess access) {
            m_access = access;
        }

        @Override
        public void setValue(final LocalTimeValue value) {
            m_access.setLocalTime(value.getLocalTime());
        }

        @Override
        public void setLocalTime(final LocalTime time) {
            m_access.setLocalTime(time);
        }
    }
}