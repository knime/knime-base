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
 *   15.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

import org.knime.base.node.meta.feature.selection.FeatureSelectionStrategies.Strategy;
import org.knime.base.node.meta.feature.selection.genetic.CrossoverStrategy;
import org.knime.base.node.meta.feature.selection.genetic.SelectionStrategy;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;

/**
 *
 * @author Adrian Nembach, KNIME.com
 * @author Simon Schmid, KNIME, Austin, USA
 */
public class FeatureSelectionLoopStartSettings {

    // ===== Config keys =====

    // General

    private static final String CFG_CONSTANT_COLUMNS_FILTER_CONFIG = "constantColumnsFilterConfig";

    // Sequential Algorithm

    private static final String CFG_NR_FEATURES_THRESHOLD = "nrFeatureThreshold";

    // Genetic Algorithm

    private static final String CFG_NR_FEATURES_LOWER_BOUND = "nrFeatureLowerBound";

    private static final String CFG_NR_FEATURES_UPPER_BOUND = "nrFeatureUpperBound";

    private static final String CFG_POP_SIZE = "popSize";

    private static final String CFG_MAX_NUM_GENERATIONS = "maxNumGenerations";

    private static final String CFG_USE_RANDOM_SEED = "useRandomSeed";

    private static final String CFG_RANDOM_SEED = "randomSeed";

    private static final String CFG_SURVIVORS_FRACTION = "survivorsFraction";

    private static final String CFG_CROSSOVER_RATE = "crossoverRate";

    private static final String CFG_MUTATION_RATE = "mutationRate";

    private static final String CFG_ELITISM_RATE = "elitismRate";

    private static final String CFG_EARLY_STOPPING = "earlyStopping";

    // ===== Default values =====

    // General

    private static final Strategy DEF_STRATEGY = Strategy.ForwardFeatureSelection;

    // Sequential Algorithm

    // -1 stands for no threshold!
    private static final int DEF_NR_FEATURES_THRESHOLD = -1;

    // Genetic Algorithm

    // -1 stands for no upper bound!
    private static final int DEF_NR_FEATURES_LOWER_BOUND = -1;

    // -1 stands for no upper bound!
    private static final int DEF_NR_FEATURES_UPPER_BOUND = -1;

    private static final int DEF_POP_SIZE = 20;

    private static final int DEF_MAX_NUM_GENERATIONS = 10;

    private static final long DEF_RANDOM_SEED = System.currentTimeMillis();

    private static final boolean DEF_USE_RANODM_SEED = false;

    private static final double DEF_SURVIVORS_FRACTION = 0.4;

    private static final double DEF_CROSSOVER_RATE = 0.6;

    private static final double DEF_MUTATION_RATE = 0.01;

    private static final double DEF_ELITISM_RATE = 0.1;

    private static final int DEF_EARLY_STOPPING = 3;

    private static final CrossoverStrategy DEF_CROSSOVER_STRATEGY = CrossoverStrategy.UNIFORM_CROSSOVER;

    private static final SelectionStrategy DEF_SELECTION_STRATEGY = SelectionStrategy.TOURNAMENT_SELECTION;

    // ===== Variables =====

    // General

    private Strategy m_strategy = DEF_STRATEGY;

    // Sequential Algorithm

    private int m_nrFeaturesThreshold = DEF_NR_FEATURES_THRESHOLD;

    // Genetic Algorithm

    private int m_nrFeaturesLowerBound = DEF_NR_FEATURES_LOWER_BOUND;

    private int m_nrFeaturesUpperBound = DEF_NR_FEATURES_UPPER_BOUND;

    private int m_popSize = DEF_POP_SIZE;

    private int m_maxNumGenerations = DEF_MAX_NUM_GENERATIONS;

    private boolean m_useRandomSeed = DEF_USE_RANODM_SEED;

    private long m_randomSeed = DEF_RANDOM_SEED;

    private double m_survivorsFraction = DEF_SURVIVORS_FRACTION;

    private double m_crossoverRate = DEF_CROSSOVER_RATE;

    private double m_mutationRate = DEF_MUTATION_RATE;

    private double m_elitismRate = DEF_ELITISM_RATE;

    private int m_earlyStopping = DEF_EARLY_STOPPING;

    private CrossoverStrategy m_crossoverStrategy = DEF_CROSSOVER_STRATEGY;

    private SelectionStrategy m_selectionStrategy = DEF_SELECTION_STRATEGY;

    @SuppressWarnings("unchecked")
    private DataColumnSpecFilterConfiguration m_constantColumnsFilterConfig = new DataColumnSpecFilterConfiguration(
        CFG_CONSTANT_COLUMNS_FILTER_CONFIG, new DataTypeColumnFilter(DataValue.class));

    /**
     * This method should be called before the getNrFeaturesThreshold() method
     *
     * @return true if a threshold for the number of features is set
     */
    public boolean useNrFeaturesThreshold() {
        return m_nrFeaturesThreshold >= 0;
    }

    /**
     *
     * @return the currently set threshold for the number of features (-1 if no threshold is set)
     */
    public int getNrFeaturesThreshold() {
        return m_nrFeaturesThreshold;
    }

    /**
     * @param nrFeaturesThreshold the threshold that should be set
     */
    public void setNrFeaturesThreshold(final int nrFeaturesThreshold) {
        m_nrFeaturesThreshold = nrFeaturesThreshold;
    }

    /**
     * @return the {@link DataColumnSpecFilterConfiguration} for the static column filter
     */
    public DataColumnSpecFilterConfiguration getStaticColumnsFilterConfiguration() {
        return m_constantColumnsFilterConfig;
    }

    /**
     * @return the name of the feature selection strategy
     */
    public Strategy getStrategy() {
        return m_strategy;
    }

    /**
     * @param strategy the name of the feature selection strategy
     */
    public void setStrategy(final Strategy strategy) {
        m_strategy = strategy;
    }

    /**
     * @return the nrFeaturesLowerBound
     */
    public int getNrFeaturesLowerBound() {
        return m_nrFeaturesLowerBound;
    }

    /**
     * @param nrFeaturesLowerBound the nrFeaturesLowerBound to set
     */
    public void setNrFeaturesLowerBound(final int nrFeaturesLowerBound) {
        m_nrFeaturesLowerBound = nrFeaturesLowerBound;
    }

    /**
     * @return true if a lower bound for the number of features is set
     */
    public boolean useNrFeaturesLowerBound() {
        return m_nrFeaturesLowerBound >= 0;
    }

    /**
     * @return the nrFeaturesUpperBound
     */
    public int getNrFeaturesUpperBound() {
        return m_nrFeaturesUpperBound;
    }

    /**
     * @param nrFeaturesUpperBound the nrFeaturesUpperBound to set
     */
    public void setNrFeaturesUpperBound(final int nrFeaturesUpperBound) {
        m_nrFeaturesUpperBound = nrFeaturesUpperBound;
    }

    /**
     * @return true if a upper bound for the number of features is set
     */
    public boolean useNrFeaturesUpperBound() {
        return m_nrFeaturesUpperBound >= 0;
    }

    /**
     * @return the popSize
     */
    public int getPopSize() {
        return m_popSize;
    }

    /**
     * @param popSize the popSize to set
     */
    public void setPopSize(final int popSize) {
        m_popSize = popSize;
    }

    /**
     * @return the maxNumGenerations
     */
    public int getMaxNumGenerations() {
        return m_maxNumGenerations;
    }

    /**
     * @param maxNumGenerations the maxNumGenerations to set
     */
    public void setMaxNumGenerations(final int maxNumGenerations) {
        m_maxNumGenerations = maxNumGenerations;
    }

    /**
     * @return the useRandomSeed
     */
    public boolean isUseRandomSeed() {
        return m_useRandomSeed;
    }

    /**
     * @param useRandomSeed the useRandomSeed to set
     */
    public void setUseRandomSeed(final boolean useRandomSeed) {
        m_useRandomSeed = useRandomSeed;
    }

    /**
     * @return the randomSeed
     */
    public long getRandomSeed() {
        return m_randomSeed;
    }

    /**
     * @param randomSeed the randomSeed to set
     */
    public void setRandomSeed(final long randomSeed) {
        m_randomSeed = randomSeed;
    }

    /**
     * @return the survivorsFraction
     */
    public double getSurvivorsFraction() {
        return m_survivorsFraction;
    }

    /**
     * @param survivorsFraction the survivorsFraction to set
     */
    public void setSurvivorsFraction(final double survivorsFraction) {
        m_survivorsFraction = survivorsFraction;
    }

    /**
     * @return the crossoverRate
     */
    public double getCrossoverRate() {
        return m_crossoverRate;
    }

    /**
     * @param crossoverRate the crossoverRate to set
     */
    public void setCrossoverRate(final double crossoverRate) {
        m_crossoverRate = crossoverRate;
    }

    /**
     * @return the mutationRate
     */
    public double getMutationRate() {
        return m_mutationRate;
    }

    /**
     * @param mutationRate the mutationRate to set
     */
    public void setMutationRate(final double mutationRate) {
        m_mutationRate = mutationRate;
    }

    /**
     * @return the elitismRate
     */
    public double getElitismRate() {
        return m_elitismRate;
    }

    /**
     * @param elitismRate the elitismRate to set
     */
    public void setElitismRate(final double elitismRate) {
        m_elitismRate = elitismRate;
    }

    /**
     * @return the earlyStopping
     */
    public int getEarlyStopping() {
        return m_earlyStopping;
    }

    /**
     * @param earlyStopping the earlyStopping to set
     */
    public void setEarlyStopping(final int earlyStopping) {
        m_earlyStopping = earlyStopping;
    }

    /**
     * @return true if a upper bound for the number of features is set
     */
    public boolean useEarlyStopping() {
        return m_earlyStopping > 0;
    }

    /**
     * @return the crossoverStrategy
     */
    public CrossoverStrategy getCrossoverStrategy() {
        return m_crossoverStrategy;
    }

    /**
     * @param crossoverStrategy the crossoverStrategy to set
     */
    public void setCrossoverStrategy(final CrossoverStrategy crossoverStrategy) {
        m_crossoverStrategy = crossoverStrategy;
    }

    /**
     * @return the selectionStrategy
     */
    public SelectionStrategy getSelectionStrategy() {
        return m_selectionStrategy;
    }

    /**
     * @param selectionStrategy the selectionStrategy to set
     */
    public void setSelectionStrategy(final SelectionStrategy selectionStrategy) {
        m_selectionStrategy = selectionStrategy;
    }

    /**
     * Saves the settings in the <b>settings</b> object
     *
     * @param settings
     */
    public void save(final NodeSettingsWO settings) {
        m_strategy.save(settings);
        m_crossoverStrategy.save(settings);
        m_selectionStrategy.save(settings);
        m_constantColumnsFilterConfig.saveConfiguration(settings);
        settings.addInt(CFG_NR_FEATURES_THRESHOLD, m_nrFeaturesThreshold);
        settings.addInt(CFG_NR_FEATURES_LOWER_BOUND, m_nrFeaturesLowerBound);
        settings.addInt(CFG_NR_FEATURES_UPPER_BOUND, m_nrFeaturesUpperBound);
        settings.addInt(CFG_POP_SIZE, m_popSize);
        settings.addInt(CFG_MAX_NUM_GENERATIONS, m_maxNumGenerations);
        settings.addBoolean(CFG_USE_RANDOM_SEED, m_useRandomSeed);
        settings.addLong(CFG_RANDOM_SEED, m_randomSeed);
        settings.addDouble(CFG_SURVIVORS_FRACTION, m_survivorsFraction);
        settings.addDouble(CFG_CROSSOVER_RATE, m_crossoverRate);
        settings.addDouble(CFG_MUTATION_RATE, m_mutationRate);
        settings.addDouble(CFG_ELITISM_RATE, m_elitismRate);
        settings.addInt(CFG_EARLY_STOPPING, m_earlyStopping);
    }

    /**
     * Loads settings in the dialog
     *
     * @param settings the settings to load from
     * @param spec the input {@link DataTableSpec}
     */
    public void loadInDialog(final NodeSettingsRO settings, final DataTableSpec spec) {
        try {
            m_strategy = Strategy.load(settings);
        } catch (Exception e) {
            m_strategy = DEF_STRATEGY;
        }
        try {
            m_crossoverStrategy = CrossoverStrategy.load(settings);
        } catch (Exception e) {
            m_crossoverStrategy = DEF_CROSSOVER_STRATEGY;
        }
        try {
            m_selectionStrategy = SelectionStrategy.load(settings);
        } catch (Exception e) {
            m_selectionStrategy = DEF_SELECTION_STRATEGY;
        }
        m_constantColumnsFilterConfig.loadConfigurationInDialog(settings, spec);
        m_nrFeaturesThreshold = settings.getInt(CFG_NR_FEATURES_THRESHOLD, DEF_NR_FEATURES_THRESHOLD);
        m_nrFeaturesLowerBound = settings.getInt(CFG_NR_FEATURES_LOWER_BOUND, DEF_NR_FEATURES_LOWER_BOUND);
        m_nrFeaturesUpperBound = settings.getInt(CFG_NR_FEATURES_UPPER_BOUND, DEF_NR_FEATURES_UPPER_BOUND);
        m_popSize = settings.getInt(CFG_POP_SIZE, DEF_POP_SIZE);
        m_maxNumGenerations = settings.getInt(CFG_MAX_NUM_GENERATIONS, DEF_MAX_NUM_GENERATIONS);
        m_useRandomSeed = settings.getBoolean(CFG_USE_RANDOM_SEED, DEF_USE_RANODM_SEED);
        m_randomSeed = settings.getLong(CFG_RANDOM_SEED, DEF_RANDOM_SEED);
        m_survivorsFraction = settings.getDouble(CFG_SURVIVORS_FRACTION, DEF_SURVIVORS_FRACTION);
        m_crossoverRate = settings.getDouble(CFG_CROSSOVER_RATE, DEF_CROSSOVER_RATE);
        m_mutationRate = settings.getDouble(CFG_MUTATION_RATE, DEF_MUTATION_RATE);
        m_elitismRate = settings.getDouble(CFG_ELITISM_RATE, DEF_ELITISM_RATE);
        m_earlyStopping = settings.getInt(CFG_EARLY_STOPPING,DEF_EARLY_STOPPING);
    }

    /**
     * Loads settings in model
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_strategy = Strategy.load(settings);
        m_constantColumnsFilterConfig.loadConfigurationInModel(settings);
        m_nrFeaturesThreshold = settings.getInt(CFG_NR_FEATURES_THRESHOLD);

        // following fields have been added later (AP-10340) and load a default, if no setting is available,
        // for the sake of backwards compatibility
        try {
            m_crossoverStrategy = CrossoverStrategy.load(settings);
        } catch (InvalidSettingsException e) {
            m_crossoverStrategy = DEF_CROSSOVER_STRATEGY;
        }
        try {
            m_selectionStrategy = SelectionStrategy.load(settings);
        } catch (InvalidSettingsException e) {
            m_selectionStrategy = DEF_SELECTION_STRATEGY;
        }
        m_nrFeaturesLowerBound = settings.getInt(CFG_NR_FEATURES_LOWER_BOUND, DEF_NR_FEATURES_LOWER_BOUND);
        m_nrFeaturesUpperBound = settings.getInt(CFG_NR_FEATURES_UPPER_BOUND, DEF_NR_FEATURES_UPPER_BOUND);
        m_popSize = settings.getInt(CFG_POP_SIZE, DEF_POP_SIZE);
        m_maxNumGenerations = settings.getInt(CFG_MAX_NUM_GENERATIONS, DEF_MAX_NUM_GENERATIONS);
        m_useRandomSeed = settings.getBoolean(CFG_USE_RANDOM_SEED, DEF_USE_RANODM_SEED);
        m_randomSeed = settings.getLong(CFG_RANDOM_SEED, DEF_RANDOM_SEED);
        m_survivorsFraction = settings.getDouble(CFG_SURVIVORS_FRACTION, DEF_SURVIVORS_FRACTION);
        m_crossoverRate = settings.getDouble(CFG_CROSSOVER_RATE, DEF_CROSSOVER_RATE);
        m_mutationRate = settings.getDouble(CFG_MUTATION_RATE, DEF_MUTATION_RATE);
        m_elitismRate = settings.getDouble(CFG_ELITISM_RATE, DEF_ELITISM_RATE);
        m_earlyStopping = settings.getInt(CFG_EARLY_STOPPING,DEF_EARLY_STOPPING);
    }
}
