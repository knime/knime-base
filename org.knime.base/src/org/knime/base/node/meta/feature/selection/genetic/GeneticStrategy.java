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
 *   Oct 1, 2018 (simon): created
 */
package org.knime.base.node.meta.feature.selection.genetic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.base.node.meta.feature.selection.FeatureSelectionStrategy;
import org.knime.core.node.util.CheckUtils;

import io.jenetics.BitChromosome;
import io.jenetics.BitGene;
import io.jenetics.EliteSelector;
import io.jenetics.Genotype;
import io.jenetics.Mutator;
import io.jenetics.Optimize;
import io.jenetics.Selector;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.Limits;
import io.jenetics.util.Factory;
import io.jenetics.util.RandomRegistry;

/**
 * Genetic Algorithm for Feature Selection. Uses the Jenetics library and runs a separate thread for the genetic
 * algorithm.
 *
 * @see <a href="http://jenetics.io/">Jenetics</a>
 *
 * @author Simon Schmid, KNIME, Austin, USA
 */
public class GeneticStrategy implements FeatureSelectionStrategy {

    private static final Integer POISON_PILL = -1;

    private final int m_numIterations;

    private boolean m_isMinimize;

    private boolean m_continueLoop = true;

    private final ExecutorService m_executor;

    private final Evaluator m_evaluator = new Evaluator();

    private final Engine<BitGene, Double> m_engine;

    // if not empty, the genetic algorithm waits for a genotype to be scored
    private final BlockingQueue<Integer> m_queueGenotypeReadyToScore;

    // if not empty, the loop start node waits for a new genotype to score
    private final BlockingQueue<Integer> m_queueGenotypeRequested;

    // if not empty, the requested genotype has been scored and the genetic algorithm can continue
    private final BlockingQueue<Integer> m_queueScoreReceived;

    /**
     * Constructor. Starts the thread for the genetic algorithm to be able to give an output of the first selected
     * feature set during configure.
     *
     * @param subsetLowerBound the minimum number of selected features, is <= 0 if undefined
     * @param subsetUpperBound the maximum number of selected features, is <= 0 if undefined
     * @param popSize population size
     * @param numGenerations max number of generations
     * @param useSeed if seed should be used
     * @param seed the seed
     * @param survivorsFraction the survivors fraction
     * @param crossoverRate the crossover rate
     * @param mutationRate the mutation rate
     * @param elitismRate the elitism rate
     * @param earlyStopping the number of generations for early stopping, is <= 0 if disabled
     * @param selectionStrategy the selection strategy
     * @param crossoverStrategy the crossover strategy
     * @param features ids of the features
     *
     */
    public GeneticStrategy(final int subsetLowerBound, final int subsetUpperBound, final int popSize,
        final int numGenerations, final boolean useSeed, final long seed, final double survivorsFraction,
        final double crossoverRate, final double mutationRate, final double elitismRate, final int earlyStopping,
        final SelectionStrategy selectionStrategy, final CrossoverStrategy crossoverStrategy,
        final List<Integer> features) {
        CheckUtils.checkArgument(!(subsetLowerBound > 0 && subsetUpperBound > 0 && subsetLowerBound > subsetUpperBound),
            "The lower bound of number of features must not be greater than the upper bound!");
        CheckUtils.checkArgument(subsetLowerBound <= features.size(),
            "The lower bound of number of features must not be greater than the actual number of features!");
        CheckUtils.checkArgument(numGenerations > 0, "The number of generations must be at least 1!");
        CheckUtils.checkArgument(popSize >= 2, "The population size must be at least 2!");
        // probably it's going to be less, this is just an upper bound (+1, because initial generation is generation 0)
        m_numIterations = (numGenerations + 1) * popSize;

        m_queueGenotypeReadyToScore = new ArrayBlockingQueue<>(1);
        m_queueScoreReceived = new ArrayBlockingQueue<>(1);
        m_queueGenotypeRequested = new ArrayBlockingQueue<>(1);
        try {
            // request the first genotype
            m_queueGenotypeRequested.put(0);
        } catch (InterruptedException e1) {
            throw new IllegalStateException("The genetic algorithm thread has been interrupted!", e1);
        }

        final Random random;
        if (useSeed) {
            random = new Random(seed);
        } else {
            random = new Random();
        }

        // 1.) Define the genotype (factory) suitable for the problem.
        final double probOfTrues = initializationProbability(subsetLowerBound, subsetUpperBound, features.size());
        final Factory<Genotype<BitGene>> gtf = Genotype.of(BitChromosome.of(features.size(), probOfTrues));

        // 2.) Define a validator for the genotype which ensures the maximal number of selected features.
        final Predicate<? super Genotype<BitGene>> validator = gt -> {
            final int bitCount = gt.get(0).as(BitChromosome.class).bitCount();
            if (subsetLowerBound <= 0 && subsetUpperBound <= 0) {
                return bitCount > 1;
            }
            if (subsetLowerBound > 0 && subsetUpperBound > 0) {
                return bitCount >= subsetLowerBound && bitCount <= subsetUpperBound;
            }
            if (subsetLowerBound > 0) {
                return bitCount >= subsetLowerBound;
            }
            // if subsetUpperBound > 0
            return bitCount > 0 && bitCount <= subsetUpperBound;
        };

        // 3.) Create the execution environment.
        m_engine = RandomRegistry.with(new Random(random.nextLong()), f -> {
            return Engine.builder(m_evaluator, gtf).executor(Runnable::run).populationSize(popSize)
                .alterers(CrossoverStrategy.getCrossover(crossoverStrategy, crossoverRate), new Mutator<>(mutationRate))
                // most likely we don't know yet whether to minimize oder maximize, must be changed
                // later using reflection (see #setIsMinimize)
                .optimize(m_isMinimize ? Optimize.MINIMUM : Optimize.MAXIMUM)
                // use elitism, if specified, only for the selection of the survivors
                .survivorsFraction(survivorsFraction)
                .survivorsSelector(selector(selectionStrategy, (int)(elitismRate * popSize + 0.5)))
                .offspringSelector(selector(selectionStrategy, 0))
                // after 100 retries, the feature subset boundaries will be ignored
                .genotypeValidator(validator).individualCreationRetries(100).build();
        });

        /*
         * This thread will run the genetic algorithm (GA) which will then run in parallel with the workflow thread.
         * Those two threads will have to wait for each other. The workflow thread needs to wait for the GA thread
         * to produce a feature subset to run and the GA thread needs to wait for the workflow loop to score this
         * subset.
         */
        final Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {

                    // 4.) Start the execution (evolution) and collect the result.
                    RandomRegistry.with(new Random(random.nextLong()), f -> m_engine.stream()
                        // limit by steady fitness (early stopping) if enabled
                        .limit(earlyStopping > 0 ? Limits.bySteadyFitness(earlyStopping) : Limits.infinite())
                        // always limit by max. number of generations
                        .limit(numGenerations)
                        // collect results
                        .collect(EvolutionResult.toBestEvolutionResult()));

                    m_continueLoop = false;
                    // take care that last call of #finishRound is not blocked
                    m_queueGenotypeRequested.take();
                    m_queueGenotypeReadyToScore.put(0);
                } catch (RuntimeException e) {
                    // dispose the streaming thread, throw the exception
                    onDispose();
                    throw e;
                } catch (InterruptedException e) {
                    // dispose the streaming thread, throw the exception
                    onDispose();
                    throw new IllegalStateException("The genetic algorithm thread has been interrupted!", e);
                }
            }
        };

        m_executor = Executors.newSingleThreadExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(r, "Genetic Algorithm");
            }
        });
        m_executor.execute(runnable);
    }

    /**
     * Returns the Selector according to user settings.
     */
    private static Selector<BitGene, Double> selector(final SelectionStrategy selectionStrategy, final int eliteCount) {
        final Selector<BitGene, Double> nonElitistSelector = SelectionStrategy.getSelector(selectionStrategy);
        final Selector<BitGene, Double> selector;
        if (eliteCount > 0) {
            selector = new EliteSelector<BitGene, Double>(eliteCount, nonElitistSelector);
        } else {
            selector = nonElitistSelector;
        }
        return selector;
    }

    /**
     * Returns the probability used to initialize the genotypes of the first population regarding the defined bounds.
     */
    private static double initializationProbability(final int subsetLowerBound, final int subsetUpperBound,
        final int numFeatures) {
        // if no bounds are set, return 0.5
        if (subsetLowerBound <= 0 && subsetUpperBound <= 0) {
            return 0.5;
        } else {
            // if one or two bounds are set, define the mean number of possibly selected features
            final double meanSize;
            if (subsetLowerBound <= 0 && subsetUpperBound > 0) {
                // if just an upper bound is set, the mean is in the middle of 0 and the upper bound
                meanSize = (double)subsetUpperBound / 2;
            } else if (subsetLowerBound > 0 && subsetUpperBound <= 0) {
                // if just a lower bound is defined, the mean is the middle of the lower bound the number of features
                meanSize = (double)(numFeatures + subsetLowerBound) / 2;
            } else {
                // if lower and upper bound is defined, the mean is in the middle of those bounds
                meanSize = (double)(subsetLowerBound + subsetUpperBound) / 2;
            }
            // return a probability which leads to subsets of approximately mean size
            return meanSize / numFeatures;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean continueLoop() {
        if (!m_continueLoop) {
            onDispose();
        }
        return m_continueLoop;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> getIncludedFeatures() {
        return m_evaluator.getCurrent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addScore(final double score) {
        m_evaluator.setScore(score);
        try {
            // a new score has been added
            m_queueScoreReceived.put(0);
        } catch (InterruptedException e) {
            throw new IllegalStateException("The genetic algorithm thread has been interrupted!", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIsMinimize(final boolean isMinimize) {
        try {
            // The only way to change whether to minimize or maximize after the genetic algorithm has been started
            // is reflection. Since we know how to optimize not before the Loop End has been configured and the genetic
            // algorithm needs to be started during configure of the Loop Start to give out the first feature set, the
            // optimization method may change.
            final Field f = m_engine.getClass().getDeclaredField("_optimize");
            f.setAccessible(true);
            // Remove final modifier.
            final Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
            // Set optimization method.
            f.set(m_engine, isMinimize ? Optimize.MINIMUM : Optimize.MAXIMUM);
            m_isMinimize = isMinimize;
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldAddFeatureLevel() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCurrentlyBestScore() {
        return m_evaluator.getScore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareNewRound() {
        // nothing to prepare
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Integer> getFeatureLevel() {
        return m_evaluator.getCurrent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameForLastChange() {
        return "Selected features";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> getLastChange() {
        return m_evaluator.getCurrent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfIterations() {
        return m_numIterations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getCurrentFeature() {
        // just needed for flow variables, this strategy does not have a current feature
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishRound() {
        try {
            // request a new genotype
            m_queueGenotypeRequested.put(0);
            // wait until the next genotype is ready
            m_queueGenotypeReadyToScore.take();
        } catch (InterruptedException e) {
            throw new IllegalStateException("The genetic algorithm thread has been interrupted!", e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDispose() {
        // shut down executor
        m_executor.shutdown();

        // offer the running GA streaming thread a poison pill to finish it's computation
        m_queueScoreReceived.offer(POISON_PILL);
        m_queueGenotypeRequested.offer(POISON_PILL);
    }

    /**
     * Evaluates genotypes and assigns their fitness.
     */
    private final class Evaluator implements Function<Genotype<BitGene>, Double> {

        // caches the scores of already scored feature subsets
        private final HashMap<Integer, Double> m_scoreLookUpMap = new HashMap<>();

        private final List<Integer> m_currentGenotype = new ArrayList<>();

        private boolean m_isInitialized = false;

        private boolean m_isInterrupted = false;

        private Double m_score;

        /**
         * {@inheritDoc}
         */
        @Override
        public Double apply(final Genotype<BitGene> genotype) {
            // if the running node has been reseted or disposed, simply return 0 to let the thread terminate quickly
            if (m_isInterrupted) {
                return stopAndReturn();
            }

            // If the same genotype has already been processed before, return the cached score.
            // There is a small chance that a different genotype has the same hash code and gets labeled with the wrong
            // score, but it will be altered sooner or later by the GA, i.e., it will not affect the results too much.
            final int hashCode = genotype.get(0).as(BitChromosome.class).toBitSet().hashCode();
            if (m_scoreLookUpMap.containsKey(hashCode)) {
                return m_scoreLookUpMap.get(hashCode);
            }

            try {
                // wait until a new genotype is requested
                if (m_queueGenotypeRequested.take().equals(POISON_PILL)) {
                    return stopAndReturn();
                }
                setCurrent(genotype);
                // signal that new genotype is ready to score
                m_queueGenotypeReadyToScore.put(0);

                // wait for the score
                if (m_queueScoreReceived.take().equals(POISON_PILL)) {
                    return stopAndReturn();
                }
                m_scoreLookUpMap.put(hashCode, m_score);
                return m_score;
            } catch (InterruptedException e) {
                throw new IllegalStateException("The genetic algorithm thread has been interrupted!", e);
            }
        }

        private double stopAndReturn() {
            // simply return 0 from now on to let the thread terminate quickly
            m_isInterrupted = true;
            return 0d;
        }

        private void setCurrent(final Genotype<BitGene> genotype) {
            m_currentGenotype.clear();
            for (int i = 0; i < genotype.getChromosome(0).length(); i++) {
                if (genotype.getChromosome(0).getGene(i).booleanValue()) {
                    m_currentGenotype.add(i);
                }
            }
        }

        /**
         * @return the currentGenotype
         */
        private List<Integer> getCurrent() {
            // wait before the initial genotype is set before the Loop Start can ask for the first feature subset
            // m_queueGenotypeReadyToScore will be taken in this function only in the first iteration, afterwards in #finishRound
            if (!m_isInitialized) {
                try {
                    m_isInitialized = true;
                    m_queueGenotypeReadyToScore.take();
                } catch (InterruptedException e) {
                    throw new IllegalStateException("The genetic algorithm thread has been interrupted!", e);
                }
            }
            return new ArrayList<>(m_currentGenotype);
        }

        /**
         * @return the score
         */
        private double getScore() {
            return m_score;
        }

        /**
         * @param score the score to set
         */
        private void setScore(final double score) {
            m_score = score;
        }

    }

}
