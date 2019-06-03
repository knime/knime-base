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
 *   31.03.2019 (Adrian): created
 */
package org.knime.base.node.mine.regression.glmnet.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.NominalValue;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class NominalFeatureBuilder extends AbstractFeatureBuilder<NominalValue> {

    private final Map<NominalValue, Integer> m_indices = new HashMap<>();
    private final List<Integer> m_counts = new ArrayList<>();
    private final int[] m_values;
    private int m_idx;

    /**
     * @param colIdx
     */
    public NominalFeatureBuilder(final int colIdx, final int numRows) {
        super(colIdx, NominalValue.class);
        m_values = new int[numRows];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Feature> build() {
        final List<Feature> features = new ArrayList<>(m_counts.size());
        for (int i = 0; i < m_counts.size(); i++) {
            features.add(createFeature(i));
        }
        return features;
    }

    private Feature createFeature(final int featureIdx) {
        final int count = m_counts.get(featureIdx);
        final int[] matching = new int[count];
        int matchingIdx = 0;
        for (int i = 0; i < m_values.length; i++) {
            if (m_values[i] == featureIdx) {
                matching[matchingIdx] = i;
                matchingIdx++;
            }
        }
        // TODO possibly scale the value by the standard deviation
        final ConstantValueHolder valueHolder = new ConstantValueHolder(1.0f, count);
        return new SparseFeature(matching, valueHolder);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void accept(final NominalValue value) {
        final int valIdx = getValueIndex(value);
        incrementCount(valIdx);
        m_values[m_idx] = valIdx;
        m_idx++;
    }

    private int getValueIndex(final NominalValue value) {
        Integer valIdx = m_indices.get(value);
        if (valIdx == null) {
            valIdx = m_indices.size();
            m_indices.put(value, valIdx);
            m_counts.add(0);
        }

        return valIdx;
    }

    private void incrementCount(final int valIdx) {
        final int currentCount = m_counts.get(valIdx);
        m_counts.set(valIdx, currentCount + 1);
    }

}
