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
 *   24.03.2019 (Adrian): created
 */
package org.knime.base.node.mine.regression.glmnet;

import org.knime.base.node.mine.regression.glmnet.cycle.FeatureCycle;
import org.knime.base.node.mine.regression.glmnet.cycle.FeatureCycleFactory;
import org.knime.base.node.mine.regression.glmnet.data.Data;
import org.knime.base.node.mine.regression.glmnet.data.DataIterator;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class GlmNet {

    private double[] m_coefficients;

    private double m_intercept = 0.0f;

    private final Data m_data;

    private final double m_alpha;

    private double m_lambda;

    private final double m_epsilon;

    private final FeatureCycleFactory m_featureCycleFactory;

    private final Snapshoter m_snapshoter;

    private Updater m_updater;

    GlmNet(final Data data, final Updater updater, final double alpha, final double epsilon,
        final FeatureCycleFactory featureCycleFactory) {
        CheckUtils.checkNotNull(data);
        CheckUtils.checkNotNull(featureCycleFactory);
        CheckUtils.checkArgument(alpha >= 0 && alpha <= 1, "Alpha must be in the interval [0, 1].");
        CheckUtils.checkNotNull(updater);
        m_data = data;
        m_coefficients = new double[data.getNumFeatures()];
        m_alpha = alpha;
        m_epsilon = epsilon;
        m_featureCycleFactory = featureCycleFactory;
        m_updater = updater;
        m_snapshoter = new Snapshoter(data);
//        calculateIntercept();
    }

    LinearModel fit(final double lambda) {
        m_lambda = lambda;
        for (final FeatureCycle featureCycle = m_featureCycleFactory.create(); featureCycle.hasNext();) {
            final int featureIdx = featureCycle.next();
            if (performFeatureIteration(featureIdx)) {
                featureCycle.betaChanged();
            }
        }
        return createSnapshot();
    }

    void setModel(final LinearModel model) {
        CheckUtils.checkArgument(model.getNumCoefficients() == m_coefficients.length,
            "The linear model has the wrong number of coefficients.");
        // model's coefficient are denormalized, so we need to normalize them
        m_coefficients = m_snapshoter.standardize(model);
        recalculateResiduals();
    }

    private void recalculateResiduals() {
        final double[] residual = new double[m_data.getNumRows()];
        for (int i = 0; i < m_data.getNumFeatures(); i++) {
            final double beta = m_coefficients[i];
            final DataIterator iter = m_data.getIterator(i);
            while (iter.next()) {
                int rowIdx = iter.getIdx();
                residual[rowIdx] += iter.getFeature() * beta;
            }
        }
        final DataIterator iter = m_data.getIterator(0);
        while (iter.next()) {
            final double res = residual[iter.getIdx()];
            iter.setResidual(res);
        }
    }

    private LinearModel createSnapshot() {
        return m_snapshoter.destandardize(m_coefficients);
    }

    private boolean performFeatureIteration(final int featureIdx) {
        final double oldBeta = m_coefficients[featureIdx];
        final double newBeta = calculateUpdate(featureIdx);
        final double betaDiff = newBeta - oldBeta;
        if (abs(betaDiff) > m_epsilon) {
            m_coefficients[featureIdx] = newBeta;
            m_updater.betaChanged(m_data.getIterator(featureIdx), betaDiff);
            return true;
        }
        return false;
    }

    private double calculateUpdate(final int featureIdx) {
        final double weightedSquaredMean = m_data.getWeightedSquaredMean(featureIdx);
        final double residualProduct = m_updater.calculateGradient(m_data.getIterator(featureIdx));
//        final double grad = m_updater.calculateGradient(m_data.getIterator(featureIdx))
//            + m_coefficients[featureIdx] * weightedSquaredMean;
        final double grad = residualProduct + m_coefficients[featureIdx];
        final double thresholded = softThresholding(grad, m_lambda * m_alpha);
        return thresholded / (weightedSquaredMean + m_lambda * (1 - m_alpha));
    }

    private static double softThresholding(final double z, final double gamma) {
        assert gamma >= 0;
        final double absZ = abs(z);
        if (z > 0 && gamma < absZ) {
            return z - gamma;
        } else if (z < 0 && gamma < absZ) {
            return z + gamma;
        } else {
            return 0;
        }
    }

    private void calculateIntercept() {
        double meanTarget = m_data.getTargetMean();
        final double delta = meanTarget - m_intercept;
        m_intercept = meanTarget;
        m_data.updateResidual(delta);
    }

    private static double abs(final double x) {
        return x < 0 ? -x : x;
    }

}
