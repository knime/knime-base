/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   10 Jan 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.rowagg.aggregation;

import java.util.Optional;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;

/**
 * Creates a type-specific sum aggregate for int, long, and double values.
 *
 * @param <V> data value type to accumulate and use as the result
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 */
public class Sum<V extends DataValue> implements Accumulator<V, V> {

    /*
     * Input Type – Accumulator Type → Output Type
     * ------------------------------------------
     * int        – long             → long
     * long       – long             → long
     * double     – double           → double
     *
     * */
    private Accumulator<V, V> m_sum;

    /**
     * Creates a new sum accumulator for {@link IntValue}, {@link LongValue}, or {@link DoubleValue}.
     * @param inputType type to create sum accumulator for
     */
    public Sum(final DataType inputType) {
        m_sum = getAggregateFor(inputType);
    }

    @SuppressWarnings("unchecked")
    private static <V extends DataValue> Accumulator<V, V> getAggregateFor(final DataType inputType) {
        if (IntCell.TYPE.equals(inputType)) {
            return (Accumulator<V, V>)new IntSum();
        }
        if (LongCell.TYPE.equals(inputType)) {
            return (Accumulator<V, V>)new LongSum();
        }
        if (DoubleCell.TYPE.equals(inputType)) {
            return (Accumulator<V, V>)new DoubleSum();
        }
        throw new IllegalArgumentException(
            String.format("Sum aggregate does not support cells of type + \"%s\"", inputType));
    }

    @Override
    public DataType getResultType() {
        return m_sum.getResultType();
    }

    @Override
    public boolean apply(final V dv) {
        return m_sum.apply(dv);
    }

    @Override
    public Optional<V> getResult() {
        return m_sum.getResult();
    }

    @Override
    public void reset() {
        m_sum.reset();
    }


    /**
     * Check whether the sum of {@code x} and {@code y} would overflow the numeric range of {@link java.lang.Long}.
     * @param x first value
     * @param y second value
     * @return {@code true} if the sum would overflow the numeric range of {@code long}, {@code false} otherwise
     */
    static boolean checkSumOverflows(final long x, final long y) {
        return y > 0 ? (x > Long.MAX_VALUE - y) : (x < Long.MIN_VALUE - y);
    }
    /**
     * Int summation with overflow and missing cell handling.
     *
     * A missing cell is returned if the result would overflow the representable numeric range of
     * {@link java.lang.Integer}. This accumulator allows intermediate overflows inside the numeric range of
     * {@link java.lang.Long}.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    static final class IntSum implements Accumulator<IntValue, IntValue> {

        // use wider type to aggregate (good performance and head-room to accumulate briefly outside of result type
        // range)
        private long m_sum;
        // signal overflow while aggregating
        private boolean m_invalid;

        @Override
        public boolean apply(final IntValue v) {
            // check for previous overflow or missing cell
            if (m_invalid) {
                return true;
            }
            final var i = v.getIntValue();
            // check for current overflow while aggregating
            if (checkSumOverflows(m_sum, i)) {
                m_invalid = true;
                return true;
            }
            m_sum += i;
            return false;
        }

        @Override
        public Optional<IntValue> getResult() {
            if (m_invalid || m_sum < Integer.MIN_VALUE || m_sum > Integer.MAX_VALUE) {
                return Optional.empty();
            }
            return Optional.of(new IntCell((int) m_sum));
        }

        @Override
        public DataType getResultType() {
            return IntCell.TYPE;
        }

        @Override
        public void reset() {
            m_sum = 0;
            m_invalid = false;
        }
    }

    /**
     * Long summation with overflow and missing cell handling.
     *
     * A missing cell may be returned if the result at any point overflowed the representable numeric range of
     * {@link java.lang.Long} or if a {@link MissingCell} is added to the sum at any point.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    static final class LongSum implements Accumulator<LongValue, LongValue> {

        // use same type to aggregate long values (2.5x-7.5x thrpt vs. BigInteger on an M1 Pro)
        private long m_sum;
        // signal overflow while aggregating
        private boolean m_invalid;

        @Override
        public boolean apply(final LongValue v) {
            if (m_invalid) {
                return true;
            }
            final var val = v.getLongValue();
            if (checkSumOverflows(m_sum, val)) {
                m_invalid = true;
                return true;
            }
            m_sum += val;
            return false;
        }

        @Override
        public Optional<LongValue> getResult() {
            if (m_invalid) {
                return Optional.empty();
            }
            return Optional.of(new LongCell(m_sum));
        }

        @Override
        public DataType getResultType() {
            return LongCell.TYPE;
        }

        @Override
        public void reset() {
            m_sum = 0;
            m_invalid = false;
        }

    }

    /**
     * Double summation with overflow handling subject to floating-point arithmetic (therefore, {@link MissingCell} is
     * <i>not</i> used to signal overflow).
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    static final class DoubleSum implements Accumulator<DoubleValue, DoubleValue> {

        // use same type for internal aggregate (up to 50x thrpt vs. BigDecimal on M1 Pro)
        private double m_sum;


        @Override
        public boolean apply(final DoubleValue v) {
            final var val = v.getDoubleValue();
            // Use double arithmetic for overflow handling
            m_sum += val;
            return false;
        }

        @Override
        public Optional<DoubleValue> getResult() {
            return Optional.of(new DoubleCell(m_sum));
        }

        @Override
        public DataType getResultType() {
            return DoubleCell.TYPE;
        }

        @Override
        public void reset() {
            m_sum = 0;
        }

    }
}
