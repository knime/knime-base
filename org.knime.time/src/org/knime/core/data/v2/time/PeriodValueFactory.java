package org.knime.core.data.v2.time;

import java.time.Period;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.time.period.PeriodCell;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.table.access.IntAccess.IntReadAccess;
import org.knime.core.table.access.IntAccess.IntWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.StructDataSpec;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link PeriodCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class PeriodValueFactory implements ValueFactory<StructReadAccess, StructWriteAccess> {

    /** A stateless instance of {@link PeriodValueFactory} */
    public static final PeriodValueFactory INSTANCE = new PeriodValueFactory();

    @Override
    public PeriodReadValue createReadValue(final StructReadAccess access) {
        return new DefaultPeriodReadValue(access);
    }

    @Override
    public PeriodWriteValue createWriteValue(final StructWriteAccess access) {
        return new DefaultPeriodWriteValue(access);
    }

    @Override
    public StructDataSpec getSpec() {
        // years, months, days
        return new StructDataSpec(DataSpec.intSpec(), DataSpec.intSpec(), DataSpec.intSpec());
    }

    /**
     * {@link ReadValue} equivalent to {@link PeriodCell}.
     *
     * @since 4.3
     */
    public static interface PeriodReadValue extends ReadValue, PeriodValue, StringValue, BoundedValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link PeriodCell}.
     *
     * @since 4.3
     */
    public static interface PeriodWriteValue extends WriteValue<PeriodValue> {

        /**
         * @param period the period to set
         */
        void setPeriod(Period period);

    }

    private static final class DefaultPeriodReadValue implements PeriodReadValue {

        private final IntReadAccess m_years;

        private final IntReadAccess m_months;

        private final IntReadAccess m_days;

        private DefaultPeriodReadValue(final StructReadAccess access) {
            m_years = access.getAccess(0);
            m_months = access.getAccess(1);
            m_days = access.getAccess(2);
        }

        @Override
        public DataCell getDataCell() {
            return PeriodCellFactory.create(getPeriod());
        }

        @Override
        public Period getPeriod() {
            return Period.of(m_years.getIntValue(), m_months.getIntValue(), m_days.getIntValue());
        }

        @Override
        public String getStringValue() {
            return getPeriod().toString();
        }

    }

    private static final class DefaultPeriodWriteValue implements PeriodWriteValue {

        private final IntWriteAccess m_years;

        private final IntWriteAccess m_months;

        private final IntWriteAccess m_days;

        private DefaultPeriodWriteValue(final StructWriteAccess access) {
            m_years = access.getWriteAccess(0);
            m_months = access.getWriteAccess(1);
            m_days = access.getWriteAccess(2);
        }

        @Override
        public void setValue(final PeriodValue value) {
            setPeriod(value.getPeriod());
        }

        @Override
        public void setPeriod(final Period period) {
            m_years.setIntValue(period.getYears());
            m_months.setIntValue(period.getMonths());
            m_days.setIntValue(period.getDays());
        }

    }
}