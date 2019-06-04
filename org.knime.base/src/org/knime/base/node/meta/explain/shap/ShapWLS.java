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
 *   May 21, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shap;

import java.util.BitSet;
import java.util.stream.IntStream;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.knime.base.node.meta.explain.util.iter.IntIterator;
import org.knime.base.node.mine.regression.glmnet.ElasticNet;
import org.knime.base.node.mine.regression.glmnet.ElasticNetBuilder;
import org.knime.base.node.mine.regression.glmnet.LinearModel;
import org.knime.base.node.mine.regression.glmnet.data.Data;
import org.knime.base.node.mine.regression.glmnet.data.DefaultData;
import org.knime.base.node.mine.regression.glmnet.data.Feature;
import org.knime.base.node.mine.regression.glmnet.data.VariableWeightContainer;
import org.knime.base.node.mine.regression.glmnet.data.WeightContainer;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ShapWLS {

    /**
     *
     */
    private static final double EPSILON = 1e-5;

    private final RealVector m_linkedNullPrediction;

    private final UnivariateFunction m_link;

    private final int m_maxActiveFeatures;

    private final double m_alpha;

    ShapWLS(final double[] nullPredictions, final UnivariateFunction link, final double alpha,
        final int maxActiveFeatures) {
        CheckUtils.checkArgument(maxActiveFeatures > 0, "At least one feature must be allowed to be active");
        m_link = link;
        m_linkedNullPrediction = MatrixUtils.createRealVector(nullPredictions).map(link);
        m_maxActiveFeatures = maxActiveFeatures;
        m_alpha = alpha;
    }

    double[] getWLSCoefficients(final Mask[] masks, final double[] pred, final int dim, final double fx,
        final double[] weights) {
        final RealVector y = MatrixUtils.createRealVector(pred);
        y.mapToSelf(m_link);
        final double linkedFx = m_link.value(fx);
        final double linkedNullPrediction = m_linkedNullPrediction.getEntry(dim);

        final double deviationFromNullFx = linkedFx - linkedNullPrediction;
        final RealVector eyAdj = y.mapSubtractToSelf(linkedNullPrediction);

        final int[] activeFeatures;
        final int nCols = masks[0].getNumberOfFeatures();
        if (nCols > m_maxActiveFeatures) {
            activeFeatures = getActiveFeatures(masks, eyAdj, weights, deviationFromNullFx);
        } else {
            activeFeatures = IntStream.range(0, nCols).toArray();
        }

        final RealVector lastNonZero = createLastActive(masks, activeFeatures[activeFeatures.length - 1]);

        final RealVector eyAdj2 = eyAdj.subtract(lastNonZero.mapMultiply(deviationFromNullFx));

        RealMatrix etmp = filterInActiveColumns(masks, activeFeatures);

        ShapMatrixUtils.subtractVec(etmp, lastNonZero);

        RealMatrix tmp = etmp.copy();

        ShapMatrixUtils.scaleVec(tmp, weights);

        RealMatrix tmp2 = MatrixUtils.inverse(tmp.transpose().multiply(etmp));

        RealVector w = tmp2.operate(tmp.transpose().operate(eyAdj2));

        final double[] phi = new double[nCols];

        double sumW = 0.0;
        for (int i = 0; i < w.getDimension(); i++) {
            final double e = w.getEntry(i);
            sumW += e;
            phi[activeFeatures[i]] = e;
        }
        phi[activeFeatures[activeFeatures.length - 1]] = deviationFromNullFx - sumW;

        return phi;
    }

    private static RealVector createLastActive(final Mask[] masks, final int lastActive) {
        final double[] values = new double[masks.length];
        for (int i = 0; i < masks.length; i++) {
            final IntIterator iter = masks[i].iterator();
            while (iter.hasNext()) {
                if (iter.next() == lastActive) {
                    values[i] = 1.0;
                }
            }
        }
        return MatrixUtils.createRealVector(values);
    }

    private static RealMatrix filterInActiveColumns(final Mask[] masks, final int[] activeFeatures) {
        final RealMatrix x = MatrixUtils.createRealMatrix(masks.length, activeFeatures.length - 1);

        for (int i = 0; i < masks.length; i++) {
            final IntIterator iter = masks[i].iterator();
            int currentNonzero = iter.next();
            for (int j = 0; j < activeFeatures.length - 1; j++) {
                final int active = activeFeatures[j];
                while (iter.hasNext() && currentNonzero < active) {
                    currentNonzero = iter.next();
                }
                if (currentNonzero == active) {
                    x.setEntry(i, j, 1.0);
                }
            }
        }
        return x;
    }

    private int[] getActiveFeatures(final Mask[] masks, final RealVector adjY, final double[] weights,
        final double deviationFromNullFx) {

        final Data data = createData(masks, adjY, weights, deviationFromNullFx);

        final ElasticNetBuilder builder = new ElasticNetBuilder(data);
        builder.setMaxActiveFeatures(m_maxActiveFeatures);
        builder.setAlpha(m_alpha);
        builder.setMaxActiveFeaturesEpsilon(EPSILON);
        builder.setMaxIterationsPerLambda(100);
        final ElasticNet elasticNet = builder.build();
        elasticNet.fit();
        final LinearModel model = elasticNet.getLastModel();

        return getActiveFeatures(model);
    }

    private static int[] getActiveFeatures(final LinearModel model) {
        return IntStream.range(0, model.getNumCoefficients()).filter(i -> Math.abs(model.getCoefficient(i)) > EPSILON)
            .toArray();
    }

    private static Data createData(final Mask[] masks, final RealVector adjY, final double[] weights,
        final double deviationFromNullFx) {
        return new DefaultData(createFeatures(masks), createTarget(adjY, deviationFromNullFx),
            createWeights(weights, masks));
    }

    private static Feature[] createFeatures(final Mask[] masks) {
        final int nRows = masks.length;
        final int nCols = masks[0].getNumberOfFeatures();
        final Feature[] features = new Feature[nCols];
        final BitSet[] storage = new BitSet[nCols];
        for (int i = 0; i < nCols; i++) {
            storage[i] = new BitSet(nRows);
        }

        for (int i = 0; i < nRows; i++) {
            final IntIterator iter = masks[i].iterator();
            while (iter.hasNext()) {
                final int nonZero = iter.next();
                storage[nonZero].set(i);
            }
        }

        for (int i = 0; i < nCols; i++) {
            features[i] = new AugmentedMaskFeature(storage[i]);
        }

        return features;
    }

    private static class AugmentedMaskFeature implements Feature {

        private final BitSet m_features;

        AugmentedMaskFeature(final BitSet features) {
            m_features = features;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FeatureIterator getIterator() {
            return new AugmentedMaskFeatureIterator();
        }

        private class AugmentedMaskFeatureIterator implements FeatureIterator {

            private final int m_maxIdx = 2 * m_features.length();

            private final int m_augmentedThreshold = m_features.length();

            private int m_idx = -1;


            /**
             * {@inheritDoc}
             */
            @Override
            public boolean next() {
                if (m_idx < m_maxIdx) {
                    m_idx++;
                }
                return m_idx < m_maxIdx;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int getRowIdx() {
                return m_idx;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public double getValue() {
                if (m_idx < m_augmentedThreshold) {
                    return m_features.get(m_idx) ? 1.0 : 0.0;
                } else {
                    return m_features.get(m_idx - m_augmentedThreshold) ? 0.0 : -1.0;
                }
            }

        }

    }

    private static double[] createTarget(final RealVector adjY, final double deviationFromNullFx) {
        int nRows = adjY.getDimension();
        final double[] target = new double[2 * nRows];
        for (int i = 0; i < nRows; i++) {
            final double value = adjY.getEntry(i);
            target[i] = value;
            target[nRows + i] = value - deviationFromNullFx;
        }
        return target;
    }

    private static WeightContainer createWeights(final double[] weights, final Mask[] masks) {
        final int[] s = countIntactFeatures(masks);
        final int nRows = weights.length;
        final int nCols = masks[0].getNumberOfFeatures();
        final double[] weightValues = new double[2 * nRows];
        for (int i = 0; i < nRows; i++) {
            final double weight = weights[i];
            weightValues[i] = weight * (nCols - s[i]);
            weightValues[nRows + i] = weight * s[i];
        }
        return new VariableWeightContainer(weightValues, false);
    }

    private static int[] countIntactFeatures(final Mask[] masks) {
        final int nRows = masks.length;
        final int[] counts = new int[nRows];
        for (int i = 0; i < nRows; i++) {
            counts[i] = masks[i].getCardinality();
        }
        return counts;
    }

}
