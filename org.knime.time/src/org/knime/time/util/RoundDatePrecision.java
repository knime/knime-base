package org.knime.time.util;

import java.time.Period;

public enum RoundDatePrecision {

            DECADE(Period.ofYears(10)), //
            YEAR(Period.ofYears(1)), //
            QUARTER(Period.ofMonths(3)), //
            MONTH(Period.ofMonths(1)), //
            WEEK(Period.ofWeeks(1)); //

        private final Period m_period;

        RoundDatePrecision(final Period period) {
            this.m_period = period;
        }

        public Period getPeriod() {
            return m_period;
        }
    }
