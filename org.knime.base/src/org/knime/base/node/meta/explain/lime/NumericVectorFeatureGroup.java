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
 *   May 3, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.lime;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.node.util.CheckUtils;

/**
 * Represents numerical vector columns such as ByteVector and BitVector columns. Note that we decided to treat
 * BitVectors as numerical features although they might just as well be treated as nominal features. We justify this
 * decision by referring to the Python implementation where a bit vector would also be treated as numerical features.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class NumericVectorFeatureGroup implements FeatureGroup {

    private final VectorHandler m_vectorHandler;

    private final Collection<? extends NumericSampler> m_samplers;

    private final Collection<? extends Normalizer> m_normalizers;

    private final Function<double[], DataCell> m_toInverse;

    private final Function<double[], Iterable<DataCell>> m_toData;

    NumericVectorFeatureGroup(final VectorHandler vectorHandler, final Collection<? extends NumericSampler> samplers,
        final Collection<? extends Normalizer> normalizers, final Function<double[], DataCell> toInverse,
        final Function<double[], Iterable<DataCell>> toData) {
        CheckUtils.checkArgument(samplers.size() == normalizers.size(),
            "The same number of samplers and normalizers is required.");
        m_vectorHandler = vectorHandler;
        m_samplers = samplers;
        m_normalizers = normalizers;
        m_toInverse = toInverse;
        m_toData = toData;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LimeCellSample sample(final DataCell original) {
        final int length = m_vectorHandler.getLength(original);
        checkLength(length);
        final Iterator<? extends NumericSampler> samplerIterator = m_samplers.iterator();
        final Iterator<? extends Normalizer> normalizerIterator = m_normalizers.iterator();
        final double[] denormalized = new double[length];
        final double[] normalized = new double[length];
        for (int i = 0; i < normalized.length; i++) {
            final double sample = samplerIterator.next().sample(m_vectorHandler.getValue(original, i));
            denormalized[i] = sample;
            normalized[i] = normalizerIterator.next().normalize(sample);
        }
        return new VectorLimeCellSample(normalized, m_toData.apply(denormalized), m_toInverse.apply(denormalized));
    }

    private void checkLength(final int length) {
        CheckUtils.checkArgument(length == m_samplers.size(),
            "The vector length (%s) doesn't match the expected length (%s).", length, m_samplers.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LimeCellSample createForOriginal(final DataCell original) {
        checkLength(m_vectorHandler.getLength(original));
        final double[] values = getValues(original);
        final Iterable<DataCell> data = m_toData.apply(values);
        final double[] normalized = normalize(values);
        return new VectorLimeCellSample(normalized, data, original);
    }

    private double[] getValues(final DataCell original) {
        final double[] values = new double[m_vectorHandler.getLength(original)];
        for (int i = 0; i < values.length; i++) {
            values[i] = m_vectorHandler.getValue(original, i);
        }
        return values;
    }

    private double[] normalize(final double[] original) {
        final Iterator<? extends Normalizer> normalizerIterator = m_normalizers.iterator();
        for (int i = 0; i < original.length; i++) {
            original[i] = normalizerIterator.next().normalize(original[i]);
        }
        return original;
    }

}
