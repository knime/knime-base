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
 *   23.03.2019 (Adrian): created
 */
package org.knime.base.node.mine.regression.glmnet.data;

import java.util.Arrays;

import org.knime.base.node.mine.regression.glmnet.data.Feature.FeatureIterator;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultData implements Data {

    private final Feature[] m_features;

    private final double[] m_target;

    private final double[] m_residuals;

    private final WeightContainer m_weights;

    private final int m_numFeatures;

    private final double[] m_stdv;

    private final double[] m_weightedSquaredMeans;

    private final double[] m_weightedMeanDiffs;

    private final double[] m_means;

    private double m_weightedMeanTarget;

    private double m_targetStdv;

    private double m_totalWeightedResidual;

    private final FeatureTargetProducts m_innerFeatureTargetProducts;

    /**
     * @param features
     * @param target
     * @param weights
     *
     */
    public DefaultData(final Feature[] features, final double[] target, final WeightContainer weights) {
        CheckUtils.checkNotNull(weights);
        CheckUtils.checkNotNull(features);
        m_features = features.clone();
        final int numFeatures = features.length;
        m_target = target;
        m_numFeatures = numFeatures;
        m_stdv = new double[numFeatures];
        m_means = new double[numFeatures];
        m_weightedSquaredMeans = new double[numFeatures];
        m_weightedMeanDiffs = new double[numFeatures];
        m_weights = weights;
        calculateWeightedFeatureStatistics();
        normalizeTarget();
        m_residuals = target.clone();
        m_innerFeatureTargetProducts = new FeatureTargetProducts(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateResidual(final double interceptDelta) {
        for (int i = 0; i < m_target.length; i++) {
            m_residuals[i] -= interceptDelta;
            m_totalWeightedResidual -= m_weights.get(i) * interceptDelta;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getWeightedMeanDiff(final int featureIdx) {
        return m_weightedMeanDiffs[featureIdx];
    }

    private void calculateWeightedFeatureStatistics() {
        Arrays.fill(m_stdv, 0);
        for (int f = 0; f < m_numFeatures; f++) {
            calculateStats(f);
        }
    }

    private void calculateStats(final int f) {
        final Feature feature = m_features[f];
        double wm = 0;
        final FeatureIterator iter = feature.getIterator();
        while (iter.next()) {
            final double value = iter.getValue();
            final double weight = m_weights.get(iter.getRowIdx());
            wm += weight * value;
        }
        final double std = calculateWeightedStdv(wm, feature.getIterator());
        assert std > 0 : "Zero standard deviation detected. This can only happen for constant columns.";
        m_weightedSquaredMeans[f] = calculateWeightedSquaredMean(feature.getIterator(), wm, std);
        m_stdv[f] = std;
        m_means[f] = wm;
    }

    private double calculateWeightedStdv(final double weightedMean, final FeatureIterator iter) {
        double weightedVariance = 0.0;
        while (iter.next()) {
            double x = iter.getValue();
            double w = m_weights.get(iter.getRowIdx());
            double xm = x - weightedMean;
            weightedVariance += w * xm * xm;
        }
        return Math.sqrt(weightedVariance);
    }

    private double calculateWeightedSquaredMean(final FeatureIterator iter, final double mean, final double std) {
        double ret = 0.0;
        while (iter.next()) {
            final double w = m_weights.get(iter.getRowIdx());
            final double x = (iter.getValue() - mean) / std;
            ret += w * x * x;
        }
        return ret;
    }

    private void normalizeTarget() {
        m_weightedMeanTarget = 0F;
        for (int i = 0; i < getNumRows(); i++) {
            m_weightedMeanTarget += m_target[i] * m_weights.get(i);
        }
        m_weightedMeanTarget /= m_weights.getTotal();
        double variance = 0.0;
        for (int i = 0; i < getNumRows(); i++) {
            double y = m_target[i] - m_weightedMeanTarget;
            variance += m_weights.get(i) * y * y;
        }
        m_targetStdv = Math.sqrt(variance);
        for (int i = 0; i < getNumRows(); i++) {
            m_target[i] = (m_target[i] - m_weightedMeanTarget) / m_targetStdv;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumRows() {
        return m_target.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumFeatures() {
        return m_numFeatures;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getTotalWeight() {
        return m_weights.getTotal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getStdv(final int featureIdx) {
        return m_stdv[featureIdx];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getWeightedSquaredMean(final int featureIdx) {
        return m_weightedSquaredMeans[featureIdx];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataIterator getIterator(final int featureIdx) {
        return new DefaultDataIterator(featureIdx, m_features[featureIdx].getIterator(), m_means[featureIdx]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMean(final int featureIdx) {
        return m_means[featureIdx];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getTargetMean() {
        return m_weightedMeanTarget;
    }

    private class DefaultDataIterator implements DataIterator {
        private final FeatureIterator m_featureIterator;

        private final double m_featureMean;

        private final int m_featureIdx;

        public DefaultDataIterator(final int featureIdx, final FeatureIterator featureIterator,
            final double featureMean) {
            m_featureIterator = featureIterator;
            m_featureMean = featureMean;
            m_featureIdx = featureIdx;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean next() {
            return m_featureIterator.next();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getWeight() {
            return m_weights.get(m_featureIterator.getRowIdx());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getFeature() {
            double x = m_featureIterator.getValue();
            return (x - m_featureMean) / m_stdv[m_featureIdx];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getTarget() {
            int rowIdx = m_featureIterator.getRowIdx();
            return m_target[rowIdx];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getResidual() {
            return m_residuals[m_featureIterator.getRowIdx()];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setResidual(final double value) {
            final double oldResidual = getResidual();
            final double diff = value - oldResidual;
            final double weight = getWeight();
            m_totalWeightedResidual += weight * diff;
            m_residuals[m_featureIterator.getRowIdx()] = value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getWeightedResidual() {
            return getWeight() * getResidual();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getFeatureMean() {
            return m_featureMean;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getTotalWeightedResidual() {
            return m_totalWeightedResidual;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getIdx() {
            return m_featureIterator.getRowIdx();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getWeightedInnerFeatureTargetProduct(final int featureIdx) {
        return m_innerFeatureTargetProducts.getFeatureTargetProduct(featureIdx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getTargetStdv() {
        return m_targetStdv;
    }

}
