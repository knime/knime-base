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
 *   May 8, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shap;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class WeightVector {

    private final double[] m_weights;

    private final int m_numPairedSubsetSizes;

    private double m_scaler = 1.0;


    WeightVector(final int numFeatures) {
        final double featuresMinus1 = numFeatures - 1.0;
        final int numSubsetSizes = (int)Math.ceil(featuresMinus1 / 2.0);
        m_numPairedSubsetSizes = (int)Math.floor(featuresMinus1 / 2.0);
        final double[] weights = new double[numSubsetSizes];
        double weightSum = 0.0;
        for (int i = 0; i < weights.length; i++) {
            final int currentSubsetSize = i + 1;
            double weight = featuresMinus1 / (currentSubsetSize * (numFeatures - currentSubsetSize));
            if (i < m_numPairedSubsetSizes) {
                weight *= 2;
            }
            weightSum += weight;
            weights[i] = weight;
        }
        assert weightSum > 0.0;
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= weightSum;
        }
        m_weights = weights;
    }

    double getScaled(final int subsetSize) {
        return getInternal(subsetSize) * m_scaler;
    }

    private double getInternal(final int subsetSize) {
        final int idx = subsetSize - 1;
        return m_weights[idx];
    }

    double get(final int subsetSize) {
        final double weight = getInternal(subsetSize);
        return isPairedSubsetSize(subsetSize) ? weight / 2 : weight;
    }

    double[] getTailDistribution(final int from) {
        final double[] probs = new double[m_weights.length - from];
        double sum = 0.0;
        for (int i = 0; i < probs.length; i++) {
            final int subsetSize = from + i + 1;
            final double val = get(subsetSize);
            sum += val;
            probs[i] = val;
        }
        assert sum > 0.0 : "The sum of the remaining weights is 0 in which case this method should never be invoked.";
        for (int i = 0; i < probs.length; i++) {
            probs[i] /= sum;
        }
        return probs;
    }

    double getWeightLeft(final int from) {
        double sum = 0.0;
        for (int i = from; i < m_weights.length; i++) {
            sum += m_weights[i];
        }
        return sum;
    }

    boolean isPairedSubsetSize(final int subsetSize) {
        return subsetSize <= m_numPairedSubsetSizes;
    }

    int getNumSubsetSizes() {
        return m_weights.length;
    }

    void rescale(final double scaler) {
        m_scaler *= scaler;
    }

    void resetScale() {
        m_scaler = 1.0;
    }

}
