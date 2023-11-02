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
 *   2 Nov 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.rowagg.aggregation;

import java.util.Objects;
import java.util.Optional;

import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

/**
 * Calculates the weighted mean in a single pass over given data stream.
 *
 * Returns {@link Double#NaN} if the data stream is empty. Note, that
 * {@link Double#NaN} may also be returned if the input includes {@code NaN} and/or infinite values.
 *
 * All methods without weight arguments will use unit weight.
 *
 * Note: implements proposed algorithm (WV2) published in "Updating Mean and Variance Estimates: An Improved Method"
 * (D.H.D. West, 1979, ACM).
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class WeightedAverageNumeric
    implements BinaryAccumulator<DoubleValue, DoubleValue, DoubleValue> {

    private long m_count;

    private double m_sumW;

    private double m_m = Double.NaN;

    private boolean m_shortCircuit;

    /**
     * Creates a new accumulator which calculates the online weighted mean from double-compatible values and weights.
     *
     * @param valueType type of the data value
     * @param weightType type of the weight
     */
    public WeightedAverageNumeric(final DataType valueType, final DataType weightType) {
        if (!Objects.requireNonNull(valueType).isCompatible(DoubleValue.class)) {
            throw new IllegalArgumentException(
                "Weighted average cannot aggregate data not compatible with double type; "
                + "data column is of unsupported type \"%s\".".formatted(valueType));
        }
        if (weightType != null && !Objects.requireNonNull(weightType).isCompatible(DoubleValue.class)) {
            throw new IllegalArgumentException(
                "Weighted average cannot aggregate data not compatible with double type; "
                + "weight column is of unsupported type \"%s\".".formatted(weightType));
        }
    }

    private boolean increment(final double xi, final double wi) {
        if (wi < 0) {
            throw new IllegalArgumentException("Negative weights not supported");
        }
        if (m_count == 0) {
            m_m = 0;
        }
        m_count++;
        if (m_shortCircuit) {
            return true;
        }

        final var q = xi - m_m;
        final var temp = m_sumW + wi;
        final var r = q * wi / temp;
        m_m += r;
        m_sumW = temp;

        if (Double.isNaN(m_m)) {
            // once we get a NaN, we stay there (not true for +/-Inf!)
            m_shortCircuit = true;
        }
        return m_shortCircuit;
    }

    /**
     * Computes the current weighted average incrementally, given {@code value} and {@code weight} arguments.
     *
     * @param value value
     * @param weight weight
     *
     * @return {@code true} if the computation reached a fixed-point, e.g. {@link Double#NaN} and will not change any
     * more.
     */
    @Override
    public boolean apply(final DoubleValue value, final DoubleValue weight) {
        return increment(value.getDoubleValue(), weight.getDoubleValue());
    }

    @Override
    public DataType getResultType() {
        return DoubleCell.TYPE;
    }

    @Override
    public void reset() {
        m_count = 0;
        m_m = Double.NaN;
        m_sumW = 0;
        m_shortCircuit = false;
    }

    @Override
    public Optional<DoubleValue> getResult() {
        return Optional.of(new DoubleCell(m_m));
    }
}
