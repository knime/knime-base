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
 *   13 Jan 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
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
 * Creates a multiplication for numeric int, long, and double operands.
 *
 * @param <T> type of first operand
 * @param <U> type of second operand
 * @param <R> multiplication result type
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 */
public class Multiply<T extends DataValue, U extends DataValue, R extends DataValue> implements Combiner<T, U, R> {

    private Combiner<T, U, R> m_comb;

    private static final DataType INT = IntCell.TYPE;
    private static final DataType LONG = LongCell.TYPE;
    private static final DataType DOUBLE = DoubleCell.TYPE;

    /**
     * Creates a multiplication combiner for the given numeric data types (int, long, or double).
     *
     * @param left left operand type
     * @param right right operand type
     */
    public Multiply(final DataType left, final DataType right) {
        m_comb = getCombinerFor(left, right);
    }

    @SuppressWarnings("unchecked")
    private static <T extends DataValue, U extends DataValue, R extends DataValue>
            Combiner<T, U, R> getCombinerFor(final DataType left, final DataType right) {
        if (typeAndCompatible(left, right, DOUBLE, DoubleValue.class)) {
            return (Combiner<T, U, R>)new DoubleMul();
        }
        if (typeAndCompatible(left, right, LONG, LongValue.class)) {
            return (Combiner<T, U, R>)new LongMul();
        }
        if (typeAndCompatible(left, right, INT, IntValue.class)) {
            return (Combiner<T, U, R>)new IntMul();
        }
        throw new IllegalArgumentException(
            String.format("Unknown multiplication for operands of type \"%s\" and \"%s\"", left, right));

    }

    private static <T extends DataValue> boolean typeAndCompatible(final DataType type, final DataType otherType,
        final DataType equal, final Class<T> clazz) {
        return (type.equals(equal) && otherType.isCompatible(clazz))
                || (otherType.equals(equal) && type.isCompatible(clazz));
    }

    @Override
    public Optional<R> apply(final T left, final U right) {
        return m_comb.apply(left,  right);
    }

    @Override
    public DataType getResultDataType() {
        return m_comb.getResultDataType();
    }

    private static final int MAX_INT = Integer.MAX_VALUE;
    private static final int MIN_INT = Integer.MIN_VALUE;

    private static final class IntMul implements Combiner<IntValue, IntValue, IntValue> {

        @Override
        public Optional<IntValue> apply(final IntValue t, final IntValue u) {
            final long res = Math.multiplyFull(t.getIntValue(), u.getIntValue());
            if (res < MIN_INT || res > MAX_INT) {
                return Optional.empty();
            }
            // downcast is safe
            return Optional.of(new IntCell((int) res));
        }

        @Override
        public DataType getResultDataType() {
            return IntCell.TYPE;
        }
    }

    private static final class LongMul implements Combiner<LongValue, LongValue, LongValue> {

        @Override
        public Optional<LongValue> apply(final LongValue t, final LongValue u) {
            try {
                final var res = Math.multiplyExact(t.getLongValue(), u.getLongValue());
                return Optional.of(new LongCell(res));
            } catch (final ArithmeticException e) { // NOSONAR performance
                return Optional.empty();
            }
        }

        @Override
        public DataType getResultDataType() {
            return LongCell.TYPE;
        }

    }

    /**
     * Multiplication of two double values. Multiplication is subject to the floating-point arithmetic of
     * {@code double} and as such does not return a {@link MissingCell} on overflow, since this is handled by the
     * standard itself (rules of ±∞, NaN, etc.).
     */
    private static final class DoubleMul implements Combiner<DoubleValue, DoubleValue, DoubleValue> {

        @Override
        public Optional<DoubleValue> apply(final DoubleValue t, final DoubleValue u) {
            return Optional.of(new DoubleCell(t.getDoubleValue() * u.getDoubleValue()));
        }

        @Override
        public DataType getResultDataType() {
            return DoubleCell.TYPE;
        }
    }
}
