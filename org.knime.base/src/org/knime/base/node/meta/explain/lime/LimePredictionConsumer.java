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
 *   Jun 3, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.lime;

import java.util.Iterator;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.knime.base.node.meta.explain.DefaultExplanation.DefaultExplanationBuilder;
import org.knime.base.node.meta.explain.DoubleVector;
import org.knime.base.node.meta.explain.Explanation;
import org.knime.base.node.meta.explain.util.iter.DoubleIterable;
import org.knime.base.node.meta.explain.util.iter.DoubleIterator;
import org.knime.base.node.mine.regression.glmnet.ElasticNet;
import org.knime.base.node.mine.regression.glmnet.ElasticNetBuilder;
import org.knime.base.node.mine.regression.glmnet.LinearModel;
import org.knime.base.node.mine.regression.glmnet.data.Data;
import org.knime.base.node.mine.regression.glmnet.data.DefaultData;
import org.knime.base.node.mine.regression.glmnet.data.DenseFeature;
import org.knime.base.node.mine.regression.glmnet.data.Feature;
import org.knime.base.node.mine.regression.glmnet.data.VariableWeightContainer;
import org.knime.base.node.mine.regression.glmnet.data.WeightContainer;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class LimePredictionConsumer {

    private final boolean m_regularize;

    private final double m_alpha;

    private final int m_numActiveFeatures;

    private final int m_numFeatures;

    private final int m_numPredictions;

    private final int m_numRows;

    LimePredictionConsumer(final int numRows, final int numFeatures, final int numPredictions,
        final boolean regularize, final int numActiveFeatures, final double alpha) {
        m_regularize = regularize;
        m_alpha = alpha;
        m_numActiveFeatures = numActiveFeatures;
        m_numPredictions = numPredictions;
        m_numFeatures = numFeatures;
        m_numRows = numRows;
    }

    LimePredictionConsumer(final int numRows, final int numFeatures, final int numPredictions) {
        this(numRows, numFeatures, numPredictions, false, numFeatures, 0.0);
    }

    LimePredictionConsumer(final int numRows, final int numFeatures, final int numPredictions,
        final int numActiveFeatures, final double alpha) {
        this(numRows, numFeatures, numPredictions, true, numActiveFeatures, alpha);
    }

    Explanation evaluatePredictions(final DoubleIterable[] predictions, final Iterable<DoubleVector> features,
        final DoubleIterable weights, final String roiKey) {
        final DefaultExplanationBuilder explanationBuilder =
            new DefaultExplanationBuilder(roiKey, m_numPredictions, m_numFeatures);
        final RealMatrix featureMatrix = readIntoMatrix(features);
        final RealVector weightVector = readIntoVector(weights);
        final RealVector sqrtWeights = weightVector.map(Math::sqrt);
        for (int i = 0; i < m_numPredictions; i++) {
            final RealVector predictionVector = readIntoVector(predictions[i]);
            final int[] activeFeatures;
            if (m_regularize) {
                activeFeatures = getActiveFeatures(predictionVector, featureMatrix, weightVector);
            } else {
                activeFeatures = IntStream.range(0, m_numFeatures).toArray();
            }
            final RealMatrix activeFeatureMatrix = filterAndAddIntercept(featureMatrix, activeFeatures);
            scaleInPlace(activeFeatureMatrix, sqrtWeights);
            final RealVector w = ls(predictionVector.ebeMultiply(sqrtWeights), activeFeatureMatrix);
            for (int j = 0; j < activeFeatures.length; j++) {
                // j + 1 because the first entry in w corresponds to the intercept
                // TODO figure out what to do with the intercept..
                explanationBuilder.setExplanationValue(i, activeFeatures[j], w.getEntry(j + 1));
            }
        }
        return explanationBuilder.build();
    }

    private static void scaleInPlace(final RealMatrix matrix, final RealVector weights) {
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            final double weight = weights.getEntry(i);
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                matrix.multiplyEntry(i, j, weight);
            }
        }
    }

    private static RealVector ls(final RealVector y, final RealMatrix x) {
        final RealMatrix xT = x.transpose();
        final RealMatrix inverse = MatrixUtils.inverse(xT.multiply(x));
        return inverse.operate(xT.operate(y));
    }

    private RealMatrix filterAndAddIntercept(final RealMatrix features, final int[] activeFeatures) {
        final RealMatrix filtered = MatrixUtils.createRealMatrix(m_numRows, activeFeatures.length + 1);
        for (int i = 0; i < m_numRows; i++) {
            filtered.setEntry(i, 0, 1.0);
        }
        if (activeFeatures.length != features.getColumnDimension()) {
            for (int i = 0; i < m_numRows; i++) {
                for (int j = 0; j < activeFeatures.length; j++) {
                    filtered.setEntry(i, j + 1, features.getEntry(i, activeFeatures[j]));
                }
            }
        } else {
            filtered.setSubMatrix(features.getData(), 0, 1);
        }
        return filtered;
    }

    private RealMatrix readIntoMatrix(final Iterable<DoubleVector> features) {
        final RealMatrix matrix = MatrixUtils.createRealMatrix(m_numRows, m_numFeatures);
        final Iterator<DoubleVector> iter = features.iterator();
        for (int i = 0; i < m_numRows; i++) {
            assert iter.hasNext() : "Fewer rows than expected.";
            final DoubleVector vec = iter.next();
            assert vec.size() == m_numFeatures : "Unexpected length of feature vector.";
            for (int j = 0; j < m_numFeatures; j++) {
                matrix.setEntry(i, j, vec.get(j));
            }
        }
        return matrix;
    }

    private RealVector readIntoVector(final DoubleIterable weights) {
        final DoubleIterator iter = weights.iterator();
        final double[] vec = new double[m_numRows];
        for (int i = 0; i < m_numRows; i++) {
            assert iter.hasNext() : "Fewer weights than rows.";
            vec[i] = iter.next();
        }
        assert !iter.hasNext() : "More weights than rows.";
        return MatrixUtils.createRealVector(vec);
    }

    private int[] getActiveFeatures(final RealVector predictions, final RealMatrix features, final RealVector weights) {
        final WeightContainer w = new VariableWeightContainer(weights.toArray(), false);
        final double[] y = predictions.toArray();
        final Feature[] x = IntStream.range(0, features.getColumnDimension())
            .mapToObj(i -> new DenseFeature(features.getColumn(i), true)).toArray(Feature[]::new);
        final Data data = new DefaultData(x, y, w);
        final ElasticNetBuilder builder = new ElasticNetBuilder(data);
        builder.setAlpha(m_alpha);
        builder.setMaxActiveFeaturesEpsilon(1e-4);
        builder.setMaxActiveFeatures(m_numActiveFeatures);
        final ElasticNet elasticNet = builder.build();
        elasticNet.fit();
        final LinearModel model = elasticNet.getLastModel();
        final int[] active = IntStream.range(0, m_numFeatures).filter(i -> Math.abs(model.getCoefficient(i)) > 0).toArray();
        return active;
    }

}
