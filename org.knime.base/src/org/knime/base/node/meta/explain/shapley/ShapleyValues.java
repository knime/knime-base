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
 *   Apr 2, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shapley;

import java.util.Iterator;
import java.util.function.Consumer;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.knime.base.node.meta.explain.feature.FeatureVector;
import org.knime.base.node.meta.explain.feature.PerturbableFeatureVector;
import org.knime.core.node.util.CheckUtils;

/**
 * Implements the first Shapley Values approximation algorithm proposed by Strumbelj and Kononenko in their paper
 * <i>Explaining prediction models and individual predictions with feature contributions</i>.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ShapleyValues {

    private final int m_iterationsPerFeature;

    private final RandomDataGenerator m_random;

    /**
     * @param iterationsPerFeature the number of estimates to draw for a single feature
     * @param seed used to ensure reproducibility of results
     */
    public ShapleyValues(final int iterationsPerFeature, final long seed) {
        m_iterationsPerFeature = iterationsPerFeature;
        m_random = new RandomDataGenerator();
        m_random.getRandomGenerator().setSeed(seed);
    }

    /**
     * Produces perturbed feature vectors for the current <b>foi</b> (feature of interest) of feature vector <b>x</b>
     *
     * @param x the feature vector of interest
     * @param foi the feature of interest
     * @param keyGenerator creates the keys for the perturbed feature vectors
     * @param sink consumes the prepared rows
     */
    public void prepare(final FeatureVector x, final int foi, final ShapleyValuesKeyGen keyGenerator,
        final Consumer<FeatureVector> sink) {
        keyGenerator.setFoi(foi);
        for (int j = 0; j < m_iterationsPerFeature; j++) {
            keyGenerator.setIteration(j);
            addPerturbationPair(x, foi, keyGenerator, sink);
        }
    }

    /**
     * @param x the feature vector of interest
     * @param foi the feature of interest
     * @param keyPrefix should identify the current foi and iteration
     * @param sink consumes the perturbed rows
     */
    private void addPerturbationPair(final FeatureVector x, final int foi, final ShapleyValuesKeyGen keyGenerator,
        final Consumer<FeatureVector> sink) {
        final int[] perm = getPermutation(x.size());
        keyGenerator.setFoiIntact(true);
        final PerturbableFeatureVector foiIntact = x.getPerturbable(keyGenerator.createKey());
        for (int toPerturb : perm) {
            if (toPerturb == foi) {
                break;
            }
            foiIntact.perturb(toPerturb);
        }
        keyGenerator.setFoiIntact(false);
        final PerturbableFeatureVector foiReplaced = foiIntact.getPerturbable(keyGenerator.createKey());
        foiReplaced.perturb(foi);
        sink.accept(foiIntact);
        sink.accept(foiReplaced);
    }

    interface ShapleyValuesKeyGen {
        void setFoi(final int foi);

        void setIteration(int iteration);

        void setFoiIntact(boolean foiIntact);

        String createKey();
    }

    private int[] getPermutation(final int size) {
        return m_random.nextPermutation(size, size);
    }

    public double[] consumePredictionsPerFoi(final Iterator<PredictionVector> predictions) {
        CheckUtils.checkArgument(predictions.hasNext(), "The iterator must provide at least two predictions.");
        PredictionVector foiIntact = predictions.next();
        final double[] accumulated = new double[foiIntact.size()];
        int iterations = 0;
        while (true) {
            CheckUtils.checkArgument(predictions.hasNext(),
                "Missing the prediction where the feature of interest is replaced.");
            final PredictionVector foiReplaced = predictions.next();
            for (int i = 0; i < accumulated.length; i++) {
                accumulated[i] += foiIntact.get(i) - foiReplaced.get(i);
            }
            iterations++;
            if (!predictions.hasNext()) {
                break;
            }
            foiIntact = predictions.next();
        }
        CheckUtils.checkState(iterations == m_iterationsPerFeature, "Expected %s predictions but only received %s.",
            2 * m_iterationsPerFeature, 2 * iterations);
        for (int i = 0; i < accumulated.length; i++) {
            accumulated[i] /= m_iterationsPerFeature;
        }
        return accumulated;
    }
}
