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
import org.knime.core.data.v2.access.PeriodAccess.PeriodAccessSpec;
import org.knime.core.data.v2.access.PeriodAccess.PeriodReadAccess;
import org.knime.core.data.v2.access.PeriodAccess.PeriodWriteAccess;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link PeriodCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class PeriodValueFactory implements ValueFactory<PeriodReadAccess, PeriodWriteAccess> {

    /** A stateless instance of {@link PeriodValueFactory} */
    public static final PeriodValueFactory INSTANCE = new PeriodValueFactory();

    @Override
    public PeriodReadValue createReadValue(final PeriodReadAccess access) {
        return new DefaultPeriodReadValue(access);
    }

    @Override
    public PeriodWriteValue createWriteValue(final PeriodWriteAccess access) {
        return new DefaultPeriodWriteValue(access);
    }

    @Override
    public PeriodAccessSpec getSpec() {
        return PeriodAccessSpec.INSTANCE;
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

        private final PeriodReadAccess m_access;

        private DefaultPeriodReadValue(final PeriodReadAccess access) {
            m_access = access;
        }

        @Override
        public DataCell getDataCell() {
            return PeriodCellFactory.create(m_access.getPeriod());
        }

        @Override
        public Period getPeriod() {
            return m_access.getPeriod();
        }

        @Override
        public String getStringValue() {
            return m_access.getPeriod().toString();
        }
    }

    private static final class DefaultPeriodWriteValue implements PeriodWriteValue {

        private final PeriodWriteAccess m_access;

        private DefaultPeriodWriteValue(final PeriodWriteAccess access) {
            m_access = access;
        }

        @Override
        public void setValue(final PeriodValue value) {
            m_access.setPeriod(value.getPeriod());
        }

        @Override
        public void setPeriod(final Period period) {
            m_access.setPeriod(period);
        }
    }
}