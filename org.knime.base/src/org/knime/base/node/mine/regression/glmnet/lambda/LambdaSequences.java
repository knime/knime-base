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
 *   26.05.2019 (Adrian): created
 */
package org.knime.base.node.mine.regression.glmnet.lambda;

import org.knime.base.node.mine.regression.glmnet.data.Data;

/**
 * Static factory class for creating {@link LambdaSequence} objects.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class LambdaSequences {

    /**
     *
     */
    private LambdaSequences() {
        // static factory class
    }

    /**
     * Creates a log scale {@link LambdaSequence} with <code>lambdaMin = epsilon * lambdaMax</code>.
     *
     * @param epsilon a small positive value e.g. 0.0001 used to define lambdaMin
     * @param steps the number of steps to take in the sequence
     * @param alpha the weight of the L1 term in the elastic net penalty
     * @param data the training data
     * @return a log scale {@link LambdaSequence}
     */
    public static LambdaSequence epsilonLogScale(final double epsilon, final int steps, final double alpha,
        final Data data) {
        final double lambdaMax = computeLambdaMax(alpha, data);
        final double lambdaMin = epsilon * lambdaMax;
        return logScaleSequence(lambdaMax, lambdaMin, steps);
    }

    /**
     * @param lambdaMin the minimum value of lambda to end the sequence at
     * @param steps the number of steps to take in the sequence
     * @param alpha the weight of the L1 term in the elastic net penalty
     * @param data the training data
     * @return a log scale {@link LambdaSequence}
     */
    public static LambdaSequence lambdaMinLogScale(final double lambdaMin, final int steps, final double alpha,
        final Data data) {
        final double lambdaMax = computeLambdaMax(alpha, data);
        return logScaleSequence(lambdaMax, lambdaMin, steps);
    }

    private static LambdaSequence logScaleSequence(final double lambdaMax, final double lambdaMin, final int steps) {
        final double[] lambdas = createDecreasingSequence(Math.log(lambdaMax), Math.log(lambdaMin), steps);
        inplaceExp(lambdas);
        // set lambdaMin to the exact values to correct rounding errors
        lambdas[lambdas.length - 1] = lambdaMin;
        return new ArrayLambdaSequence(lambdas, LambdaSequences::logInterpolate);
    }

    private static double logInterpolate(final double lower, final double upper) {
        final double logLower = Math.log(lower);
        final double logUpper = Math.log(upper);
        final double logMidpoint = (logUpper - logLower) / 2;
        return Math.exp(logMidpoint);
    }

    private static void inplaceExp(final double[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = Math.exp(array[i]);
        }
    }

    private static double[] createDecreasingSequence(final double lambdaMax, final double lambdaMin, final int steps) {
        final double stepSize = (lambdaMax - lambdaMin) / steps;
        final double[] lambdas = new double[steps];
        for (int i = 0; i < steps; i++) {
            lambdas[i] = lambdaMax - i * stepSize;
        }
        return lambdas;
    }

    private static double computeLambdaMax(final double alpha, final Data data) {
        final double maxInnerProduct = findMaxInnerProduct(data);
        return maxInnerProduct / (alpha * data.getTotalWeight());
    }

    private static double findMaxInnerProduct(final Data data) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < data.getNumFeatures(); i++) {
            final double innerProduct = data.getWeightedInnerFeatureTargetProduct(i);
            final double absInnerProduct = Math.abs(innerProduct);
            if (absInnerProduct > max) {
                max = absInnerProduct;
            }
        }
        return max;
    }

}
