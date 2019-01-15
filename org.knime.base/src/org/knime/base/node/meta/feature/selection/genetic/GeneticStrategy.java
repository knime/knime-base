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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.base.node.meta.feature.selection.FeatureSelectionStrategy;
import org.knime.core.node.util.CheckUtils;

import io.jenetics.BitChromosome;
import io.jenetics.BitGene;
import io.jenetics.Chromosome;
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
     * Builder class for a GeneticStrategy object.
     */
    public static class Builder {

        //Required parameters

        private final int m_popSize;

        private final int m_numGenerations;

        private final int m_numFeatures;

        // Optional parameters

        private boolean m_useSeed = false;

        private long m_seed = 0;

        private int m_nrFeaturesLowerBound = -1;

        private int m_nrFeaturesUpperBound = -1;

        private int m_earlyStopping = -1;

        private double m_survivorsFraction = 0.4;

        private double m_crossoverRate = 0.6;

        private double m_mutationRate = 0.1;

        private double m_elitismRate = 0.1;

        private SelectionStrategy m_selectionStrategy = SelectionStrategy.TOURNAMENT_SELECTION;

        private CrossoverStrategy m_crossoverStrategy = CrossoverStrategy.UNIFORM_CROSSOVER;

        /**
         * @param popSize population size
         * @param numGenerations max number of generations
         * @param numFeatures the number of features/genes
         */
        public Builder(final int popSize, final int numGenerations, final int numFeatures) {
            m_popSize = popSize;
            m_numGenerations = numGenerations;
            m_numFeatures = numFeatures;
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
         * @param survivorsFraction the survivors fraction
         * @return the updated Builder object
         */
        public Builder survivorsFraction(final double survivorsFraction) {
            m_survivorsFraction = survivorsFraction;
            return this;
        }

        /**
         * @param crossoverRate the crossover rate
         * @return the updated Builder object
         */
        public Builder crossoverRate(final double crossoverRate) {
            m_crossoverRate = crossoverRate;
            return this;
        }

        /**
         * @param mutationRate the mutation rate
         * @return the updated Builder object
         */
        public Builder mutationRate(final double mutationRate) {
            m_mutationRate = mutationRate;
            return this;
        }

        /**
         * @param elitismRate the elitism rate
         * @return the updated Builder object
         */
        public Builder elitismRate(final double elitismRate) {
            m_elitismRate = elitismRate;
            return this;
        }

        /**
         * @param selectionStrategy the selection strategy
         * @return the updated Builder object
         */
        public Builder selectionStrategy(final SelectionStrategy selectionStrategy) {
            m_selectionStrategy = selectionStrategy;
            return this;
        }

        /**
         * @param crossoverStrategy the crossover strategy
         * @return the updated Builder object
         */
        public Builder crossoverStrategy(final CrossoverStrategy crossoverStrategy) {
            m_crossoverStrategy = crossoverStrategy;
            return this;
        }

        /**
         * Constructs a new {@link GeneticStrategy} object with the configured settings. Starts the thread for the
         * genetic algorithm to be able to give an output of the first selected feature set during configure.
         *
         * @return a GeneticStrategy object with the set parameters
         */
        public GeneticStrategy build() {
            return new GeneticStrategy(this);
        }
    }

    /**
     * Constructor. Starts the thread for the genetic algorithm to be able to give an output of the first selected
     * feature set during configure.
     *
     * @param builder the builder
     * @throws IllegalArgumentException if the passed arguments are invalid
     */
    private GeneticStrategy(final Builder builder) {
        final int numGenerations = builder.m_numGenerations;
        final int popSize = builder.m_popSize;
        final int numFeatures = builder.m_numFeatures;
        final int nrFeaturesLowerBound = builder.m_nrFeaturesLowerBound;
        final int nrFeaturesUpperBound = builder.m_nrFeaturesUpperBound;
        final boolean useSeed = builder.m_useSeed;
        final long seed = useSeed ? builder.m_seed : System.currentTimeMillis();
        final double survivorsFraction = builder.m_survivorsFraction;
        final double crossoverRate = builder.m_crossoverRate;
        final double mutationRate = builder.m_mutationRate;
        final double elitismRate = builder.m_elitismRate;
        final int earlyStopping = builder.m_earlyStopping;
        final SelectionStrategy selectionStrategy = builder.m_selectionStrategy;
        final CrossoverStrategy crossoverStrategy = builder.m_crossoverStrategy;

        CheckUtils.checkArgument(
            !(nrFeaturesLowerBound > 0 && nrFeaturesUpperBound > 0 && nrFeaturesLowerBound > nrFeaturesUpperBound),
            "The lower bound of number of features must be less than or equal the upper bound.");
        CheckUtils.checkArgument(nrFeaturesLowerBound <= numFeatures,
            "The lower bound of number of features must less than or equal the actual number of features.");
        CheckUtils.checkArgument(numGenerations > 0, "The number of generations must be at least 1.");
        CheckUtils.checkArgument(popSize >= 2, "The population size must be at least 2.");
        // probably it's going to be less, this is just an upper bound (+1, because initial generation is generation 0)
        m_numIterations = (numGenerations + 1) * popSize;

        m_queueGenotypeReadyToScore = new ArrayBlockingQueue<>(1);
        m_queueScoreReceived = new ArrayBlockingQueue<>(1);
        m_queueGenotypeRequested = new ArrayBlockingQueue<>(1);
        try {
            // request the first genotype
            m_queueGenotypeRequested.put(0);
        } catch (InterruptedException e1) {
            throw new IllegalStateException("The genetic algorithm thread has been interrupted.", e1);
        }

        final Random random;
        if (useSeed) {
            random = new Random(seed);
        } else {
            random = new Random();
        }

        // 1.) Define the genotype (factory) suitable for the problem.
        final double probOfTrues = initializationProbability(nrFeaturesLowerBound, nrFeaturesUpperBound, numFeatures);
        final Factory<Genotype<BitGene>> gtf = Genotype.of(BitChromosome.of(numFeatures, probOfTrues));

        // 2.) Define a validator for the genotype which ensures the maximal number of selected features.
        final Predicate<? super Genotype<BitGene>> validator = gt -> {
            final int bitCount = gt.get(0).as(BitChromosome.class).bitCount();
            if (nrFeaturesLowerBound <= 0 && nrFeaturesUpperBound <= 0) {
                return bitCount > 1;
            }
            if (nrFeaturesLowerBound > 0 && nrFeaturesUpperBound > 0) {
                return bitCount >= nrFeaturesLowerBound && bitCount <= nrFeaturesUpperBound;
            }
            if (nrFeaturesLowerBound > 0) {
                return bitCount >= nrFeaturesLowerBound;
            }
            // if subsetUpperBound > 0
            return bitCount > 0 && bitCount <= nrFeaturesUpperBound;
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
                .genotypeValidator(validator)//
                .individualCreationRetries(100)
                // build it
                .build();
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
                    throw new IllegalStateException("The genetic algorithm thread has been interrupted.", e);
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
        if (eliteCount > 0) {
            return new EliteSelector<BitGene, Double>(eliteCount, nonElitistSelector);
        }
        return nonElitistSelector;
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
                meanSize = subsetUpperBound / 2.0;
            } else if (subsetLowerBound > 0 && subsetUpperBound <= 0) {
                // if just a lower bound is defined, the mean is the middle of the lower bound the number of features
                meanSize = (numFeatures + subsetLowerBound) / 2.0;
            } else {
                // if lower and upper bound is defined, the mean is in the middle of those bounds
                meanSize = (subsetLowerBound + subsetUpperBound) / 2.0;
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
            throw new IllegalStateException("The genetic algorithm thread has been interrupted.", e);
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
    public List<Integer> getLastChangedFeatures() {
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

        private List<Integer> m_currentGenotype = new ArrayList<>();

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
            final Chromosome<BitGene> chromosome = genotype.getChromosome(0);
            // collect indices of positive genes
            m_currentGenotype = IntStream.range(0, chromosome.length())
                .filter(i ->  chromosome.getGene(i).booleanValue()).boxed().collect(Collectors.toList());
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
