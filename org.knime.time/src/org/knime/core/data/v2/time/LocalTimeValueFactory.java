package org.knime.core.data.v2.time;

import java.time.LocalTime;
import java.time.temporal.ChronoField;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.time.DateTimeValueInterfaces.LocalTimeReadValue;
import org.knime.core.data.v2.time.DateTimeValueInterfaces.LocalTimeWriteValue;
import org.knime.core.table.access.LongAccess.LongReadAccess;
import org.knime.core.table.access.LongAccess.LongWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.LongDataSpec;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link LocalTimeCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class LocalTimeValueFactory implements ValueFactory<LongReadAccess, LongWriteAccess> {

    /** A stateless instance of {@link LocalTimeValueFactory} */
    public static final LocalTimeValueFactory INSTANCE = new LocalTimeValueFactory();

    @Override
    public LocalTimeReadValue createReadValue(final LongReadAccess access) {
        return new DefaultLocalTimeReadValue(access);
    }

    @Override
    public LocalTimeWriteValue createWriteValue(final LongWriteAccess access) {
        return new DefaultLocalTimeWriteValue(access);
    }

    @Override
    public LongDataSpec getSpec() {
        return DataSpec.longSpec();
    }

    private static final class DefaultLocalTimeReadValue implements LocalTimeReadValue {

        private final LongReadAccess m_nanoOfDay;

        private DefaultLocalTimeReadValue(final LongReadAccess access) {
            m_nanoOfDay = access;
        }

        @Override
        public DataCell getDataCell() {
            return LocalTimeCellFactory.create(getLocalTime());
        }

        @Override
        public LocalTime getLocalTime() {
            return LocalTime.ofNanoOfDay(m_nanoOfDay.getLongValue());
        }

        @Override
        public String getStringValue() {
            return getLocalTime().toString();
        }

    }

    private static final class DefaultLocalTimeWriteValue implements LocalTimeWriteValue {

        private final LongWriteAccess m_nanoOfDay;

        private DefaultLocalTimeWriteValue(final LongWriteAccess access) {
            m_nanoOfDay = access;
        }

        @Override
        public void setValue(final LocalTimeValue value) {
            setLocalTime(value.getLocalTime());
        }

        @Override
        public void setLocalTime(final LocalTime time) {
            m_nanoOfDay.setLongValue(time.getLong(ChronoField.NANO_OF_DAY));
        }

    }
}