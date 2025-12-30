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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.meta.feature.selection;

import java.util.Optional;

import org.knime.base.node.meta.feature.selection.FeatureSelectionStrategies.Strategy;
import org.knime.base.node.meta.feature.selection.genetic.CrossoverStrategy;
import org.knime.base.node.meta.feature.selection.genetic.SelectionStrategy;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for Feature Selection Loop Start (2:2).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class FeatureSelectionLoopStartNodeParameters implements NodeParameters {

    @Widget(title = "Static and variable features", description = """
            Columns can be selected manually or by means of regular expressions. The columns in the left
            pane are the static columns, those in the right pane the variable columns. If you want to
            learn a supervised model (e.g. classification or regression), at least one static column and
            more than one variable column will be needed. For an unsupervised model (e.g. clustering), no
            constant column but only variable columns will be needed. Columns can be moved from one pane
            to the other by clicking on the appropriate button in the middle.
            """)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    @TwinlistWidget(excludedLabel = "Static columns", includedLabel = "Variable columns")
    @ChoicesProvider(AllColumnsProvider.class)
    @Persistor(ConstantColumnsFilterPersistor.class)
    ColumnFilter m_constantColumnsFilterConfig;

    @Widget(title = "Feature selection strategy", description = """
            Here you can choose between the selection strategies: Forward Feature Selection, Backward Feature
            Elimination, <a href="https://en.wikipedia.org/wiki/Genetic_algorithm">Genetic Algorithm</a> and Random.
            """)
    @ValueReference(StrategyRef.class)
    @Persistor(StrategyPersistor.class)
    Strategy m_strategy = Strategy.Random;

    static final class StrategyRef implements ParameterReference<Strategy> {
    }

    @Persistor(NrFeaturesThresholdPersistor.class)
    @Widget(title = "Threshold for number of features", description = """
            [Forward Feature Selection, Backward Feature Elimination] Check this option if you want to set a bound
            for the number of selected features. Since Forward Feature Selection adds features while Backward
            Feature Elimination subtracts them, this will be an upper bound for Forward Feature Selection and a
            lower bound for Backward Feature Elimination.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @OptionalWidget(defaultProvider = NrFeaturesThresholdDefaultProvider.class)
    @Effect(predicate = IsSequentialStrategy.class, type = EffectType.SHOW)
    // Note: Default in settings is -1 (disabled), 20 is UI default when enabled
    Optional<Integer> m_nrFeaturesThreshold = Optional.of(20);

    static final class NrFeaturesThresholdDefaultProvider implements DefaultValueProvider<Integer> {

        @Override
        public Integer computeState(final NodeParametersInput parametersInput) {
            return 20;
        }

    }

    @Persistor(NrFeaturesLowerBoundPersistor.class)
    @Widget(title = "Lower bound for number of features", description = """
            [Genetic Algorithm, Random] Check this option if you want to set a lower bound for the number of
            selected features.
            """)
    @NumberInputWidget(minValidation = IsMinimumTwoValidation.class)
    @OptionalWidget(defaultProvider = NrFeaturesLowerBoundDefaultProvider.class)
    @Effect(predicate = IsGeneticOrRandomStrategy.class, type = EffectType.SHOW)
    // Note: Default in settings is -1 (disabled), 2 is UI default when enabled
    Optional<Integer> m_nrFeaturesLowerBound = Optional.of(2);

    static final class NrFeaturesLowerBoundDefaultProvider implements DefaultValueProvider<Integer> {

        @Override
        public Integer computeState(final NodeParametersInput parametersInput) {
            return 2;
        }

    }

    @Persistor(NrFeaturesUpperBoundPersistor.class)
    @Widget(title = "Upper bound for number of features", description = """
            [Genetic Algorithm, Random] Check this option if you want to set an upper bound for the number of
            selected features.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @OptionalWidget(defaultProvider = NrFeaturesThresholdDefaultProvider.class)
    @Effect(predicate = IsGeneticOrRandomStrategy.class, type = EffectType.SHOW)
    // Note: Default in settings is -1 (disabled), 20 is UI default when enabled
    Optional<Integer> m_nrFeaturesUpperBound = Optional.of(20);

    @Widget(title = "Population size", description = """
            [Genetic Algorithm] Set the number of individuals in each population. Changing this value directly
            influences the maximal number of loop iterations which is <i>Population size * (Number of generations + 1)
            </i>. This is just an upper bound, usually less iterations will be necessary.
            """)
    @NumberInputWidget(minValidation = IsMinimumTwoValidation.class)
    @Effect(predicate = IsGeneticStrategy.class, type = EffectType.SHOW)
    @Persist(configKey = FeatureSelectionLoopStartSettings.CFG_POP_SIZE)
    int m_popSize = FeatureSelectionLoopStartSettings.DEF_POP_SIZE;

    @Widget(title = "Max. number of generations", description = """
            [Genetic Algorithm] Set the number of generations. Changing this value directly influences the maximal
            number of loop iterations which is <i>Population size * (Number of generations + 1)</i>. This is just an
            upper bound, usually less iterations will be necessary.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = IsGeneticStrategy.class, type = EffectType.SHOW)
    @Persist(configKey = FeatureSelectionLoopStartSettings.CFG_MAX_NUM_GENERATIONS)
    int m_maxNumGenerations = FeatureSelectionLoopStartSettings.DEF_MAX_NUM_GENERATIONS;

    @Widget(title = "Max. number of iterations", description = """
            [Random] Set the number of iterations. This is an upper bound. If the same feature subset is randomly
            generated for a second time, it won't be processed again but will be counted as iteration.
            Furthermore, if early stopping is enabled, the algorithm may stop before the max. number of
            iterations is reached.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = IsRandomStrategy.class, type = EffectType.SHOW)
    @Persist(configKey = FeatureSelectionLoopStartSettings.CFG_MAX_NUM_ITERATIONS)
    int m_maxNumIterations = FeatureSelectionLoopStartSettings.DEF_MAX_NUM_ITERATIONS;

    @Widget(title = "Use static random seed", description = """
            [Genetic Algorithm, Random] Choose a seed to get reproducible results.
            """)
    @ValueReference(UseRandomSeedRef.class)
    @Effect(predicate = IsGeneticOrRandomStrategy.class, type = EffectType.SHOW)
    @Persist(configKey = FeatureSelectionLoopStartSettings.CFG_USE_RANDOM_SEED)
    boolean m_useRandomSeed;

    static final class UseRandomSeedRef implements BooleanReference {
    }

    @Widget(title = "Random seed", description = """
            The random seed value for reproducible results.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = IsGeneticOrRandomStrategyAndUseRandomSeed.class, type = EffectType.SHOW)
    @Persist(configKey = FeatureSelectionLoopStartSettings.CFG_RANDOM_SEED)
    long m_randomSeed = FeatureSelectionLoopStartSettings.DEF_RANDOM_SEED;

    @Advanced
    @Widget(title = "Selection strategy", description = """
            [Genetic Algorithm] Choose the strategy to use for the
            <a href="https://en.wikipedia.org/wiki/Selection_(genetic_algorithm)">selection of offspring</a>.
            """)
    @Effect(predicate = IsGeneticStrategy.class, type = EffectType.SHOW)
    @Persistor(SelectionStrategyPersistor.class)
    SelectionStrategy m_selectionStrategy = SelectionStrategy.TOURNAMENT_SELECTION;

    @Advanced
    @Widget(title = "Fraction of survivors", description = """
            [Genetic Algorithm] Set the fraction of survivors during evaluation of the next generation.
            <i>1 - fraction of survivors</i> defines the fraction of offspring which is evaluated for the next
            generation.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class,
        maxValidation = IsMaximumDoubleOneValidation.class, stepSize = 0.1)
    @Effect(predicate = IsGeneticStrategy.class, type = EffectType.SHOW)
    @Persist(configKey = FeatureSelectionLoopStartSettings.CFG_SURVIVORS_FRACTION)
    double m_survivorsFraction = FeatureSelectionLoopStartSettings.DEF_SURVIVORS_FRACTION;

    @Advanced
    @Widget(title = "Elitism rate", description = """
            [Genetic Algorithm] Set the fraction of the best individuals within a generation that are transferred to the
             next generation without alternation.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class,
        maxValidation = IsMaximumDoubleOneValidation.class, stepSize = 0.1)
    @Effect(predicate = IsGeneticStrategy.class, type = EffectType.SHOW)
    @Persist(configKey = FeatureSelectionLoopStartSettings.CFG_ELITISM_RATE)
    double m_elitismRate = FeatureSelectionLoopStartSettings.DEF_ELITISM_RATE;

    @Advanced
    @Widget(title = "Crossover strategy", description = """
            [Genetic Algorithm] Choose the strategy to use for
            <a href="https://en.wikipedia.org/wiki/Crossover_(genetic_algorithm)"> crossover</a>.
            """)
    @Effect(predicate = IsGeneticStrategy.class, type = EffectType.SHOW)
    @Persistor(CrossoverStrategyPersistor.class)
    CrossoverStrategy m_crossoverStrategy = CrossoverStrategy.UNIFORM_CROSSOVER;

    @Advanced
    @Widget(title = "Crossover rate", description = """
            [Genetic Algorithm] Set the crossover rate used to alter offspring.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class,
        maxValidation = IsMaximumDoubleOneValidation.class, stepSize = 0.1)
    @Effect(predicate = IsGeneticStrategy.class, type = EffectType.SHOW)
    @Persist(configKey = FeatureSelectionLoopStartSettings.CFG_CROSSOVER_RATE)
    double m_crossoverRate = FeatureSelectionLoopStartSettings.DEF_CROSSOVER_RATE;

    @Advanced
    @Widget(title = "Mutation rate", description = """
            [Genetic Algorithm] Set the mutation rate used to alter offspring.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class,
        maxValidation = IsMaximumDoubleOneValidation.class, stepSize = 0.1)
    @Effect(predicate = IsGeneticStrategy.class, type = EffectType.SHOW)
    @Persist(configKey = FeatureSelectionLoopStartSettings.CFG_MUTATION_RATE)
    double m_mutationRate = FeatureSelectionLoopStartSettings.DEF_MUTATION_RATE;

    @Advanced
    @Persistor(EarlyStoppingNumGenerationsPersistor.class)
    @Widget(title = "Number of generations without improvement", description = """
            [Genetic Algorithm] Check this option if you want to enable early stopping which means that the algorithm
            stops after a specified number of generations without improvement. This is based on a moving average whereby
             the size of the moving window is the same number as the specified number of iterations.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @OptionalWidget(defaultProvider = EarlyStoppingNumGenerationsDefaultProvider.class)
    @Effect(predicate = IsGeneticStrategy.class, type = EffectType.SHOW)
    // Note: Default in settings is -1 (disabled), 3 is UI default when enabled
    Optional<Integer> m_earlyStoppingNumGenerations = Optional.of(3);

    static final class EarlyStoppingNumGenerationsDefaultProvider implements DefaultValueProvider<Integer> {

        @Override
        public Integer computeState(final NodeParametersInput parametersInput) {
            return 3;
        }

    }

    @Advanced
    @Persistor(EarlyStoppingRoundsRandomPersistor.class)
    @Widget(title = "Number of iterations without improvement", description = """
            [Random] Check this option if you want to enable early stopping which means that the algorithm stops after
            a specified number of iterations without improvement. This is based on a moving average whereby the size of
            the moving window is the same number as the specified number of iterations.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @OptionalWidget(defaultProvider = EarlyStoppingRoundsRandomDefaultProvider.class)
    @Effect(predicate = IsRandomStrategy.class, type = EffectType.SHOW)
    // Note: Default in settings is -1 (disabled), 5 is UI default when enabled
    Optional<Integer> m_earlyStoppingRoundsRandom = Optional.of(5);

    static final class EarlyStoppingRoundsRandomDefaultProvider implements DefaultValueProvider<Integer> {

        @Override
        public Integer computeState(final NodeParametersInput parametersInput) {
            return 5;
        }

    }

    @Advanced
    @Persistor(EarlyStoppingTolerancePersistor.class)
    @Widget(title = "Tolerance", description = """
            [Random] Check this option if you want to enable early stopping which means that the algorithm stops after
            a specified number of iterations without improvement. If the ratio of improvement is lower than a specified
            tolerance, the search stops.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class,
        maxValidation = IsMaximumToleranceValidation.class, stepSize = 0.01)
    @OptionalWidget(defaultProvider = EarlyStoppingToleranceDefaultProvider.class)
    @Effect(predicate = IsRandomStrategy.class, type = EffectType.SHOW)
    // Note: Default in settings is -1 (disabled), 0.01 is UI default when enabled
    Optional<Double> m_earlyStoppingTolerance =
        Optional.of(FeatureSelectionLoopStartSettings.DEF_EARLY_STOPPING_TOLERANCE);

    static final class EarlyStoppingToleranceDefaultProvider implements DefaultValueProvider<Double> {

        @Override
        public Double computeState(final NodeParametersInput parametersInput) {
            return FeatureSelectionLoopStartSettings.DEF_EARLY_STOPPING_TOLERANCE;
        }

    }

    static final class IsSequentialStrategy implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(StrategyRef.class) //
                .isOneOf(Strategy.ForwardFeatureSelection, Strategy.BackwardFeatureElimination);
        }

    }

    static final class IsGeneticStrategy implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(StrategyRef.class).isOneOf(Strategy.GeneticAlgorithm);
        }

    }

    static final class IsRandomStrategy implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(StrategyRef.class).isOneOf(Strategy.Random);
        }

    }

    static final class IsGeneticOrRandomStrategy implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(StrategyRef.class).isOneOf(Strategy.GeneticAlgorithm, Strategy.Random);
        }

    }

    static final class IsGeneticOrRandomStrategyAndUseRandomSeed implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(IsGeneticOrRandomStrategy.class).and(i.getBoolean(UseRandomSeedRef.class).isTrue());
        }

    }

    static final class IsMinimumTwoValidation extends MinValidation {

        @Override
        public double getMin() {
            return 2;
        }

    }

    static final class IsMaximumDoubleOneValidation extends MaxValidation {

        @Override
        public double getMax() {
            return 1.0;
        }

    }

    static final class IsMaximumToleranceValidation extends MaxValidation {

        @Override
        public double getMax() {
            return 100.0;
        }

    }

    static final class ConstantColumnsFilterPersistor extends LegacyColumnFilterPersistor {

        protected ConstantColumnsFilterPersistor() {
            super(FeatureSelectionLoopStartSettings.CFG_CONSTANT_COLUMNS_FILTER_CONFIG);
        }

    }

    static final class StrategyPersistor implements NodeParametersPersistor<Strategy> {

        @Override
        public Strategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return Strategy.load(settings);
        }

        @Override
        public void save(final Strategy value, final NodeSettingsWO settings) {
            value.save(settings);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"selectionStrategy"}};
        }

    }

    static final class NrFeaturesThresholdPersistor extends OptionalIntegerToMinusOneOrIntegerPersistor {

        protected NrFeaturesThresholdPersistor() {
            super(FeatureSelectionLoopStartSettings.CFG_NR_FEATURES_THRESHOLD);
        }

    }

    static final class NrFeaturesLowerBoundPersistor extends OptionalIntegerToMinusOneOrIntegerPersistor {

        protected NrFeaturesLowerBoundPersistor() {
            super(FeatureSelectionLoopStartSettings.CFG_NR_FEATURES_LOWER_BOUND);
        }

    }

    static final class NrFeaturesUpperBoundPersistor extends OptionalIntegerToMinusOneOrIntegerPersistor {

        protected NrFeaturesUpperBoundPersistor() {
            super(FeatureSelectionLoopStartSettings.CFG_NR_FEATURES_UPPER_BOUND);
        }

    }

    static final class SelectionStrategyPersistor implements NodeParametersPersistor<SelectionStrategy> {

        @Override
        public SelectionStrategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return SelectionStrategy.load(settings);
        }

        @Override
        public void save(final SelectionStrategy value, final NodeSettingsWO settings) {
            value.save(settings);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"ga_selectionStrategy"}};
        }

    }

    static final class CrossoverStrategyPersistor implements NodeParametersPersistor<CrossoverStrategy> {

        @Override
        public CrossoverStrategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return CrossoverStrategy.load(settings);
        }

        @Override
        public void save(final CrossoverStrategy value, final NodeSettingsWO settings) {
            value.save(settings);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"ga_crossoverStrategy"}};
        }

    }

    static final class EarlyStoppingNumGenerationsPersistor extends OptionalIntegerToMinusOneOrIntegerPersistor {

        protected EarlyStoppingNumGenerationsPersistor() {
            super(FeatureSelectionLoopStartSettings.CFG_EARLY_STOPPING_GENETIC);
        }

    }

    static final class EarlyStoppingRoundsRandomPersistor extends OptionalIntegerToMinusOneOrIntegerPersistor {

        protected EarlyStoppingRoundsRandomPersistor() {
            super(FeatureSelectionLoopStartSettings.CFG_EARLY_STOPPING_RANDOM);
        }

    }

    static final class EarlyStoppingTolerancePersistor extends OptionalDoubleToMinusOneOrDoublePersistor {

        protected EarlyStoppingTolerancePersistor() {
            super(FeatureSelectionLoopStartSettings.CFG_EARLY_STOPPING_TOLERANCE);
        }

    }

    abstract static class OptionalDoubleToMinusOneOrDoublePersistor
        implements NodeParametersPersistor<Optional<Double>> {

        private String m_configKey;

        protected OptionalDoubleToMinusOneOrDoublePersistor(final String configKey) {
            m_configKey = configKey;
        }

        @Override
        public Optional<Double> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var value = settings.getDouble(m_configKey, -1);
            return value == -1 ? Optional.empty() : Optional.of(value);
        }

        @Override
        public void save(final Optional<Double> value, final NodeSettingsWO settings) {
            if (value.isPresent()) {
                settings.addDouble(m_configKey, value.get());
            } else {
                settings.addDouble(m_configKey, -1);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{m_configKey}};
        }

    }

    abstract static class OptionalIntegerToMinusOneOrIntegerPersistor
        implements NodeParametersPersistor<Optional<Integer>> {

        private String m_configKey;

        protected OptionalIntegerToMinusOneOrIntegerPersistor(final String configKey) {
            m_configKey = configKey;
        }

        @Override
        public Optional<Integer> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var value = settings.getInt(m_configKey, -1);
            return value == -1 ? Optional.empty() : Optional.of(value);
        }

        @Override
        public void save(final Optional<Integer> value, final NodeSettingsWO settings) {
            if (value.isPresent()) {
                settings.addInt(m_configKey, value.get());
            } else {
                settings.addInt(m_configKey, -1);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{m_configKey}};
        }

    }

}
