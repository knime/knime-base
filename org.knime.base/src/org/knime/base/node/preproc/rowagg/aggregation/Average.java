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
import org.knime.core.data.def.DoubleCell;

/**
 * Calculate average of double-compatible values using double arithmetic. The result is always of type
 * {@link DoubleValue}.
 *
 * The implementation is based on updating with differential to avoid accumulating a large intermediate sum.
 *
 * @param <V> type of data values to average
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 */
public class Average<V extends DataValue> implements Accumulator<V, DoubleValue> {

    private double m_mean;
    private long m_count;

    // In case of NaN
    private boolean m_shortCircuit;

    /**
     * Create a new average accumulator for types which are double-compatible.
     *
     * @param inputType type of input data to average
     */
    public Average(final DataType inputType) {
        if (!inputType.isCompatible(DoubleValue.class)) {
            throw new IllegalArgumentException("Average cannot aggregate data not compatible with double values.");
        }
    }

    @Override
    public boolean apply(final V v) {
        if (m_shortCircuit) {
            return true;
        }
        final var dv = (DoubleValue)v;
        final var val = dv.getDoubleValue();
        m_count++;
        // We use the differential update since the current MeanOperator uses Apache Math3's
        // org.apache.commons.math3.stat.descriptive.moment.Mean class which uses this formula as well when values
        // are added one-by-one.
        m_mean = m_mean + (val - m_mean) / m_count;
        // NB: This differential updating has a different Inf/NaN behavior compared to the formula `sum(x_i) / n` since
        // addition of infinite values behaves different to subtraction:
        // e.g. when m_mean:= +inf, then
        //   sum(m_mean, +inf) / 2 => +inf
        //   m_mean + (+inf - m_mean) / 2 => NaN (since +inf - m_mean = NaN)

        final var isNaN = Double.isNaN(m_mean);
        if (isNaN) {
            // once we get a NaN, we stay there (not true for +/-Inf!)
            m_shortCircuit = true;
        }
        return m_shortCircuit;
    }

    @Override
    public Optional<DoubleValue> getResult() {
        return Optional.of(new DoubleCell(m_mean));
    }

    @Override
    public DataType getResultType() {
        return DoubleCell.TYPE;
    }

    @Override
    public void reset() {
        m_shortCircuit = false;
        m_mean = 0;
        m_count = 0;
    }
}
