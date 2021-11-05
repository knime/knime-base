package org.knime.core.data.v2.time;

import java.time.Duration;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.time.duration.DurationCell;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.time.DateTimeValueInterfaces.DurationReadValue;
import org.knime.core.data.v2.time.DateTimeValueInterfaces.DurationWriteValue;
import org.knime.core.table.access.IntAccess.IntReadAccess;
import org.knime.core.table.access.IntAccess.IntWriteAccess;
import org.knime.core.table.access.LongAccess.LongReadAccess;
import org.knime.core.table.access.LongAccess.LongWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.StructDataSpec;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link DurationCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class DurationValueFactory
    implements ValueFactory<StructReadAccess, StructWriteAccess> {

    /** A stateless instance of {@link DurationValueFactory} */
    public static final DurationValueFactory INSTANCE = new DurationValueFactory();

    @Override
    public DurationReadValue createReadValue(final StructReadAccess access) {
        return new DefaultDurationReadValue(access);
    }

    @Override
    public DurationWriteValue createWriteValue(final StructWriteAccess access) {
        return new DefaultDurationWriteValue(access);
    }

    @Override
    public StructDataSpec getSpec() {
        return new StructDataSpec(DataSpec.longSpec(), DataSpec.intSpec());
    }

    private static final class DefaultDurationReadValue implements DurationReadValue {

        private final LongReadAccess m_seconds;

        private final IntReadAccess m_nanos;

        private DefaultDurationReadValue(final StructReadAccess access) {
            m_seconds = access.getAccess(0);
            m_nanos = access.getAccess(1);
        }

        @Override
        public DataCell getDataCell() {
            return DurationCellFactory.create(getDuration());
        }

        @Override
        public Duration getDuration() {
            return Duration.ofSeconds(m_seconds.getLongValue(), m_nanos.getIntValue());
        }

        @Override
        public String getStringValue() {
            return getDuration().toString();
        }

    }

    private static final class DefaultDurationWriteValue implements DurationWriteValue {

        private final LongWriteAccess m_seconds;

        private final IntWriteAccess m_nanos;

        private DefaultDurationWriteValue(final StructWriteAccess access) {
            m_seconds = access.getWriteAccess(0);
            m_nanos = access.getWriteAccess(1);
        }

        @Override
        public void setValue(final DurationValue value) {
            setDuration(value.getDuration());
        }

        @Override
        public void setDuration(final Duration duration) {
            m_seconds.setLongValue(duration.getSeconds());
            m_nanos.setIntValue(duration.getNano());
        }

    }
}
