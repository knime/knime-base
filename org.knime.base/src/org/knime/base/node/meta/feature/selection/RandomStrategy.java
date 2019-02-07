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
 *   Feb 5, 2019 (Simon Schmid, KNIME Inc., Austin, TX, USA): created
 */
package org.knime.base.node.meta.feature.selection;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.knime.core.node.util.CheckUtils;

/**
 * Random Algorithm for Feature Selection.
 *
 * @author Simon Schmid, KNIME, Austin, TX, USA
 */
public class RandomStrategy extends AbstractNonSequentialFeatureSelectionStrategy {

    private final List<Integer> m_featureIndices;

    private final int m_nrFeaturesLowerBound;

    private final int m_nrFeaturesUpperBound;

    private final int m_earlyStopping;

    private final Random m_random;

    // caches the scores of already scored feature subsets
    private final HashSet<Integer> m_featureHashSet = new HashSet<>();

    private int m_iteration = 0;

    private boolean m_continueLoop = true;

    private List<Integer> m_currentFeatureColumns;

    private boolean m_isMinimize;

    private int m_numIterationsWithoutImprovement = 0;

    private double m_bestScore;

    private double m_score;

    /**
     * Builder class for a RandomStrategy object.
     */
    public static class Builder {

        // Required parameters

        private final int m_maxIterations;

        private final List<Integer> m_featureIndices;

        // Optional parameters

        private boolean m_useSeed = false;

        private long m_seed = 0;

        private int m_nrFeaturesLowerBound = -1;

        private int m_nrFeaturesUpperBound = -1;

        private int m_earlyStopping = -1;

        /**
         * @param maxIterations the maximal number of iterations
         * @param featureIndices the list of features indices
         */
        public Builder(final int maxIterations, final List<Integer> featureIndices) {
            m_featureIndices = featureIndices;
            m_maxIterations = maxIterations;
        }

        /**
         * @param seed the seed
         * @return the updated Builder object
         */
        public Builder seed(final long seed) {
            m_useSeed = true;
            m_seed = seed;
            return this;
        }

        /**
         * @param nrFeaturesLowerBound the minimum number of selected features, is <= 0 if undefined
         * @return the updated Builder object
         */
        public Builder nrFeaturesLowerBound(final int nrFeaturesLowerBound) {
            m_nrFeaturesLowerBound = nrFeaturesLowerBound;
            return this;
        }

        /**
         * @param nrFeaturesUpperBound the maximum number of selected features, is <= 0 if undefined
         * @return the updated Builder object
         */
        public Builder nrFeaturesUpperBound(final int nrFeaturesUpperBound) {
            m_nrFeaturesUpperBound = nrFeaturesUpperBound;
            return this;
        }

        /**
         * @param stoppingRounds the number of generations for early stopping, is <= 0 if disabled
         * @return the updated Builder object
         */
        public Builder earlyStopping(final int stoppingRounds) {
            m_earlyStopping = stoppingRounds;
            return this;
        }

        /**
         * Constructs a new {@link RandomStrategy} object with the configured settings.
         *
         * @return a GeneticStrategy object with the set parameters
         */
        public RandomStrategy build() {
            return new RandomStrategy(this);
        }
    }

    /**
     * Constructor.
     *
     * @param builder the builder
     * @throws IllegalArgumentException if the passed arguments are invalid
     */
    private RandomStrategy(final Builder builder) {
        super(builder.m_maxIterations);
        m_featureIndices = builder.m_featureIndices;
        m_earlyStopping = builder.m_earlyStopping;
        m_random = builder.m_useSeed ? new Random(builder.m_seed) : new Random();
        m_nrFeaturesLowerBound = builder.m_nrFeaturesLowerBound <= 0 ? 1 : builder.m_nrFeaturesLowerBound;
        m_nrFeaturesUpperBound =
            builder.m_nrFeaturesUpperBound <= 0 ? m_featureIndices.size() : builder.m_nrFeaturesUpperBound;
        CheckUtils.checkArgument(
            !(m_nrFeaturesLowerBound > 0 && m_nrFeaturesUpperBound > 0
                && m_nrFeaturesLowerBound > m_nrFeaturesUpperBound),
            "The lower bound of number of features must be less than or equal the upper bound.");
        CheckUtils.checkArgument(m_nrFeaturesLowerBound <= m_featureIndices.size(),
            "The lower bound of number of features must less than or equal the actual number of features.");

        generateNextFeatures();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> getCurrentFeatures() {
        return m_currentFeatureColumns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean continueLoop() {
        return m_continueLoop;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addScore(final double score) {
        m_score = score;

        // if early stopping is enabled, check for stop condition
        if (m_earlyStopping > 0) {
            if ((m_isMinimize && score < m_bestScore) || (!m_isMinimize && score > m_bestScore)) {
                m_numIterationsWithoutImprovement = 0;
                m_bestScore = score;
            } else {
                m_numIterationsWithoutImprovement++;
            }
            if (m_numIterationsWithoutImprovement >= m_earlyStopping) {
                m_continueLoop = false;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIsMinimize(final boolean isMinimize) {
        m_isMinimize = isMinimize;
        if (isMinimize) {
            m_bestScore = Integer.MAX_VALUE;
        } else {
            m_bestScore = Integer.MIN_VALUE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCurrentlyBestScore() {
        return m_score;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishRound() {
        generateNextFeatures();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDispose() {
        // nothing to dispose
    }

    /**
     * Generates a new, yet unseen, feature subset.
     */
    private void generateNextFeatures() {
        // generate new random feature subsets until one is generated that has not already been processed before
        do {
            // stop if max. number of iterations is reached
            if (m_iteration >= m_maxIterations) {
                m_continueLoop = false;
                return;
            }
            m_currentFeatureColumns = generateRandomFeatureList();
        } while (m_featureHashSet.contains(m_currentFeatureColumns.hashCode()));
        // remember hash of new feature subset
        m_featureHashSet.add(m_currentFeatureColumns.hashCode());
    }

    private List<Integer> generateRandomFeatureList() {
        m_iteration++;
        Collections.shuffle(m_featureIndices, m_random);
        final int nrFeatures =
            m_random.nextInt(m_nrFeaturesUpperBound - m_nrFeaturesLowerBound + 1) + m_nrFeaturesLowerBound;
        final List<Integer> subList = m_featureIndices.subList(0, nrFeatures);
        subList.sort((o1, o2) -> Integer.compare(o1, o2));
        return subList;
    }

}
