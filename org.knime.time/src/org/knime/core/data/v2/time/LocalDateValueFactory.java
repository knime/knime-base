package org.knime.core.data.v2.time;

import java.time.LocalDate;
import java.time.temporal.ChronoField;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.time.DateTimeValueInterfaces.LocalDateReadValue;
import org.knime.core.data.v2.time.DateTimeValueInterfaces.LocalDateWriteValue;
import org.knime.core.table.access.LongAccess.LongReadAccess;
import org.knime.core.table.access.LongAccess.LongWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.LongDataSpec;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link LocalDateCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class LocalDateValueFactory
    implements ValueFactory<LongReadAccess, LongWriteAccess> {

    /** A stateless instance of {@link LocalDateValueFactory} */
    public static final LocalDateValueFactory INSTANCE = new LocalDateValueFactory();

    @Override
    public LocalDateReadValue createReadValue(final LongReadAccess access) {
        return new DefaultLocalDateReadValue(access);
    }

    @Override
    public LocalDateWriteValue createWriteValue(final LongWriteAccess access) {
        return new DefaultLocalDateWriteValue(access);
    }

    @Override
    public LongDataSpec getSpec() {
        return DataSpec.longSpec();
    }

    private static final class DefaultLocalDateReadValue implements LocalDateReadValue {

        private final LongReadAccess m_dayOfEpoch;

        private DefaultLocalDateReadValue(final LongReadAccess access) {
            m_dayOfEpoch = access;
        }

        @Override
        public DataCell getDataCell() {
            return LocalDateCellFactory.create(getLocalDate());
        }

        @Override
        public LocalDate getLocalDate() {
            return LocalDate.ofEpochDay(m_dayOfEpoch.getLongValue());
        }

        @Override
        public String getStringValue() {
            return getLocalDate().toString();
        }

    }

    private static final class DefaultLocalDateWriteValue implements LocalDateWriteValue {

        private final LongWriteAccess m_dayOfEpoch;

        private DefaultLocalDateWriteValue(final LongWriteAccess access) {
            m_dayOfEpoch = access;
        }

        @Override
        public void setValue(final LocalDateValue value) {
            setLocalDate(value.getLocalDate());
        }

        @Override
        public void setLocalDate(final LocalDate date) {
            m_dayOfEpoch.setLongValue(date.getLong(ChronoField.EPOCH_DAY));
        }

    }
}