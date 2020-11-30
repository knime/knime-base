package org.knime.core.data.v2.time;

import java.time.Duration;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.time.duration.DurationCell;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.ObjectAccess.DurationAccessSpec;
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectWriteAccess;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link DurationCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class DurationValueFactory
    implements ValueFactory<ObjectReadAccess<Duration>, ObjectWriteAccess<Duration>> {

    /** A stateless instance of {@link DurationValueFactory} */
    public static final DurationValueFactory INSTANCE = new DurationValueFactory();

    @Override
    public DurationReadValue createReadValue(final ObjectReadAccess<Duration> access) {
        return new DefaultDurationReadValue(access);
    }

    @Override
    public DurationWriteValue createWriteValue(final ObjectWriteAccess<Duration> access) {
        return new DefaultDurationWriteValue(access);
    }

    @Override
    public DurationAccessSpec getSpec() {
        return DurationAccessSpec.INSTANCE;
    }

    /**
     * {@link ReadValue} equivalent to {@link DurationCell}.
     *
     * @since 4.3
     */
    public static interface DurationReadValue extends ReadValue, DurationValue, BoundedValue, StringValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link DurationCell}.
     *
     * @since 4.3
     */
    public static interface DurationWriteValue extends WriteValue<DurationValue> {

        /**
         * @param duration the duration to set
         */
        void setDuration(Duration duration);
    }

    private static final class DefaultDurationReadValue implements DurationReadValue {

        private final ObjectReadAccess<Duration> m_access;

        private DefaultDurationReadValue(final ObjectReadAccess<Duration> access) {
            m_access = access;
        }

        @Override
        public DataCell getDataCell() {
            return DurationCellFactory.create(m_access.getObject());
        }

        @Override
        public Duration getDuration() {
            return m_access.getObject();
        }

        @Override
        public String getStringValue() {
            return m_access.getObject().toString();
        }
    }

    private static final class DefaultDurationWriteValue implements DurationWriteValue {

        private final ObjectWriteAccess<Duration> m_access;

        private DefaultDurationWriteValue(final ObjectWriteAccess<Duration> access) {
            m_access = access;
        }

        @Override
        public void setValue(final DurationValue value) {
            m_access.setObject(value.getDuration());
        }

        @Override
        public void setDuration(final Duration duration) {
            m_access.setObject(duration);
        }
    }
}