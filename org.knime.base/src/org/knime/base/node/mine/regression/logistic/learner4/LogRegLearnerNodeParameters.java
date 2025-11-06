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

package org.knime.base.node.mine.regression.logistic.learner4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings.LearningRateStrategies;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings.Prior;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings.Solver;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.probability.nominal.NominalDistributionValue;
import org.knime.core.data.probability.nominal.NominalDistributionValueMetaData;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.persistence.legacy.LongAsStringPersistor;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveDoubleValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Logistic Regression Learner.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class LogRegLearnerNodeParameters implements NodeParameters {

    @SuppressWarnings("java:S2245") // RNG not security-relevant
    private static final Random randomSeedGenerator = new Random();

    @Section(title = "Solver Options")
    @Advanced
    interface SolverOptionsSection {
    }

    @Section(title = "Termination Conditions")
    @After(SolverOptionsSection.class)
    @Advanced
    interface TerminationConditionsSection {
    }

    @Section(title = "Learning Rate / Step Size")
    @After(TerminationConditionsSection.class)
    @Advanced
    @Effect(predicate = SolverIsSAG.class, type = EffectType.SHOW)
    interface LearningRateAndStepSizeSection {
    }

    @Section(title = "Regularization")
    @After(LearningRateAndStepSizeSection.class)
    @Advanced
    @Effect(predicate = SolverIsSAG.class, type = EffectType.SHOW)
    interface RegularizationSection {
    }

    @Section(title = "Data Handling")
    @After(RegularizationSection.class)
    @Advanced
    interface DataHandlingSection {
    }

    @Persist(configKey = LogRegLearnerSettings.CFG_TARGET)
    @Widget(title = "Target column", description = """
            Select the target column. Only columns with nominal data are allowed. The reference category is
            empty if the domain of the target column is not available. In this case the node determines the
            domain values right before computing the logistic regression model and chooses the last domain
            value as the targets reference category.
            """)
    @ChoicesProvider(NominalColumnChoicesProvider.class)
    @ValueReference(TargetColumnRef.class)
    String m_targetColumn;

    @Persistor(TargetReferenceCategoryPersistor.class)
    @ChoicesProvider(TargetReferenceCategoryChoicesProvider.class)
    @Widget(title = "Reference category", description = """
            The reference category is the category for which the probability is obtained as 1 minus the sum
            of all other probabilities. In a two class scenario this is usually the class for which you don't
            explicitly want to model the probability.
            """)
    @ValueReference(TargetReferenceCategoryRef.class)
    @ValueProvider(TargetReferenceCategoryProvider.class)
    String m_targetReferenceCategory;

    @Persist(configKey = LogRegLearnerSettings.CFG_USE_ORDER_TARGET_DOMAIN)
    @Widget(title = "Use order from target column domain", description = """
            By default the target domain values are sorted lexicographically in the output, but you can
            enforce the order of the target column domain to be preserved by checking the box. Note, if a
            target reference column is selected in the dropdown, the checkbox will have no influence on the
            coefficients of the model except that the output representation (e.g. order of rows in the
            coefficient table) may vary.
            """)
    boolean m_useTargetDomainOrder;

    @Persist(configKey = LogRegLearnerSettings.CFG_SOLVER)
    @Widget(title = "Solver", description = """
            Select the solver to use. Either Iteratively reweighted least squares or Stochastic average
            gradient.
            """)
    @ValueReference(SolverRef.class)
    @Migration(SolverMigration.class)
    Solver m_solver = LogRegLearnerSettings.DEFAULT_SOLVER;

    @Widget(title = "Feature selection", description = """
            Specify the independent columns that should be included in the regression model. Numeric and
            nominal data can be included.
            """)
    @ChoicesProvider(value = LogRegCompatibleColumnFilterProvider.class)
    @Persistor(IncludedColumnsPersistor.class)
    ColumnFilter m_includedColumns = new ColumnFilter().withIncludeUnknownColumns();

    @Persist(configKey = LogRegLearnerSettings.CFG_USE_ORDER_FEATURE_DOMAIN)
    @Widget(title = "Use order from column domain", description = """
            By default the domain values (categories) of nominal valued columns are sorted lexicographically,
            but you can check that the order from the column domain is used. Please note that the first
            category is used as a reference when creating the dummy variables.
            """)
    boolean m_useFeatureDomainOrder;

    @Layout(SolverOptionsSection.class)
    @Persist(configKey = LogRegLearnerSettings.CFG_PERFORM_LAZY)
    @Widget(title = "Perform calculations lazily", description = """
            If selected, the optimization is performed lazily i.e. the coefficients are only updated if their
            corresponding feature is actually present in the current sample. Usually faster than the normal
            version especially for sparse data (that is data where for the most rows the most values are
            zero). Currently only supported by the SAG solver.
            """)
    @Effect(predicate = SolverSupportsLazy.class, type = EffectType.SHOW)
    boolean m_performLazy = LogRegLearnerSettings.DEFAULT_PERFORM_LAZY;

    @Layout(SolverOptionsSection.class)
    @Persist(configKey = LogRegLearnerSettings.CFG_CALC_COVMATRIX)
    @Widget(title = "Calculate statistics for coefficients", description = """
            If selected, the node calculates the standard errors, z-score and P>|z| values for the
            coefficients. Note that those are affected by regularization in case of the Gauss prior.
            Calculating those statistics is expensive if the model is learned on many features and can be
            responsible for a significant part of the node runtime.
            """)
    boolean m_calcCovMatrix = LogRegLearnerSettings.DEFAULT_CALC_COVMATRIX;

    @Layout(TerminationConditionsSection.class)
    @Persist(configKey = LogRegLearnerSettings.CFG_MAX_EPOCH)
    @Widget(title = "Maximal number of epochs", description = """
            Here you can specify the maximal number of learning epochs you want to perform. That is the
            number of times you want to iterate over the full table. This value determines to a large extend
            how long learning will take. The solver will stop early if it reaches convergence therefore it is
            recommended to set a relatively high value for this parameter in order to give the solver enough
            time to find a good solution.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_maxEpoch = LogRegLearnerSettings.DEFAULT_MAX_EPOCH;

    @Layout(TerminationConditionsSection.class)
    @Persist(configKey = LogRegLearnerSettings.CFG_EPSILON)
    @Widget(title = "Epsilon", description = """
            This value is used to determine whether the model converged. If the relative change of all
            coefficients is smaller than epsilon, the training is stopped.
            """)
    @NumberInputWidget(minValidation = IsPositiveDoubleValidation.class)
    double m_epsilon = LogRegLearnerSettings.DEFAULT_EPSILON;

    @Layout(LearningRateAndStepSizeSection.class)
    @Persist(configKey = LogRegLearnerSettings.CFG_LEARNING_RATE_STRATEGY)
    @Widget(title = "Learning rate strategy", description = """
            The strategy provides the learning rates for the optimization process. Only important for the SAG
            solver. For more information see the paragraph on learning rate strategies above.
            """)
    @ValueSwitchWidget
    @Effect(predicate = SolverIsSAG.class, type = EffectType.ENABLE)
    @ValueReference(LearningRateStrategyRef.class)
    LearningRateStrategies m_learningRateStrategy = LogRegLearnerSettings.DEFAULT_LEARNINGRATE_STRATEGY;

    @Layout(LearningRateAndStepSizeSection.class)
    @Persist(configKey = LogRegLearnerSettings.CFG_STEP_SIZE)
    @Widget(title = "Step size", description = """
            The step size (learning rate) to use in case of the fixed learning rate strategy.
            """)
    @NumberInputWidget(minValidation = IsPositiveDoubleValidation.class)
    @Effect(predicate = LearningRateStrategyIsFixed.class, type = EffectType.ENABLE)
    double m_initialLearningRate = LogRegLearnerSettings.DEFAULT_INITIAL_LEARNING_RATE;

    @Layout(RegularizationSection.class)
    @Persist(configKey = LogRegLearnerSettings.CFG_PRIOR)
    @Widget(title = "Prior", description = """
            The prior distribution for the coefficients. See the paragraph on regularization above for more details.
            """)
    @ValueSwitchWidget
    @Effect(predicate = SolverIsSAG.class, type = EffectType.ENABLE)
    @ValueReference(PriorRef.class)
    Prior m_prior = LogRegLearnerSettings.DEFAULT_PRIOR;

    @Layout(RegularizationSection.class)
    @Persist(configKey = LogRegLearnerSettings.CFG_PRIOR_VARIANCE)
    @Widget(title = "Variance", description = """
            The variance of the prior distribution. A larger variance corresponds to less regularization.
            """)
    @NumberInputWidget(minValidation = IsPositiveDoubleValidation.class)
    @Effect(predicate = PriorHasVariance.class, type = EffectType.ENABLE)
    double m_priorVariance = LogRegLearnerSettings.DEFAULT_PRIOR_VARIANCE;

    @Layout(DataHandlingSection.class)
    @Persist(configKey = LogRegLearnerSettings.CFG_IN_MEMORY)
    @Widget(title = "Hold data in memory", description = """
            If selected, the data is read into an internal data structure which results into a tremendous
            speed up. It is highly recommended to use this option if you have enough main memory available
            especially if you use the SAG solver as their convergence rate highly depends on random access to
            individual samples.
            """)
    @ValueReference(InMemoryRef.class)
    boolean m_inMemory = LogRegLearnerSettings.DEFAULT_IN_MEMORY;

    @Layout(DataHandlingSection.class)
    @Persist(configKey = LogRegLearnerSettings.CFG_CHUNK_SIZE)
    @Widget(title = "Chunk size", description = """
            If the data is not held completely in memory, the node reads chunks of data into memory to
            emulate random access for the SAG solver. This parameter specifies how large those chunks should
            be. The chunk size directly affects the convergence rate of the SAG solver, as those work best
            with complete random access and a larger chunk size will better approximate that. This especially
            means that the solver may need many epochs to converge if the chunk size is chosen too small.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = InMemoryIsFalse.class, type = EffectType.ENABLE)
    int m_chunkSize = LogRegLearnerSettings.DEFAULT_CHUNK_SIZE;

    @Layout(DataHandlingSection.class)
    @Persistor(SeedParametersPersistor.class)
    SeedParameters m_seedParameters = new SeedParameters();

    static final class SeedParameters implements NodeParameters {

        @Persist(hidden = true)
        @Widget(title = "Use seed", description = """
                Check if you want to use a static seed. Recommended for reproducible results if you use the SAG solver.
                """)
        @ValueReference(UseRandomSeedRef.class)
        boolean m_useRandomSeed;

        @Widget(title = "Random seed", description = """
                The seed value for the random number generator.
                """)
        @TextInputWidget(patternValidation = LongAsStringPersistor.IsLongInteger.class)
        @Effect(predicate = IsUseRandomSeed.class, type = EffectType.SHOW)
        @ValueProvider(NewSeedValueProvider.class)
        String m_randomSeed;

        @Widget(title = "New",
            description = "Generate a random seed and set it in the Random seed input above for reproducible runs.")
        @SimpleButtonWidget(ref = NewSeedButtonRef.class)
        @Effect(predicate = IsUseRandomSeed.class, type = EffectType.SHOW)
        Void m_newSeed;

    }

    static final class TargetColumnRef implements ParameterReference<String> {
    }

    static final class TargetReferenceCategoryRef implements ParameterReference<String> {
    }

    static final class SolverRef implements ParameterReference<Solver> {
    }

    static final class LearningRateStrategyRef implements ParameterReference<LearningRateStrategies> {
    }

    static final class PriorRef implements ParameterReference<Prior> {
    }

    static final class InMemoryRef implements ParameterReference<Boolean> {
    }

    static final class UseRandomSeedRef implements ParameterReference<Boolean> {
    }

    static final class NewSeedButtonRef implements ButtonReference {
    }

    static final class SolverIsSAG implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SolverRef.class).isOneOf(Solver.SAG);
        }

    }

    static final class SolverSupportsLazy implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SolverRef.class).isOneOf(Solver.SAG);
        }

    }

    static final class LearningRateStrategyIsFixed implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(LearningRateStrategyRef.class).isOneOf(LearningRateStrategies.Fixed);
        }

    }

    static final class PriorHasVariance implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(PriorRef.class).isOneOf(Prior.Gauss, Prior.Laplace);
        }

    }

    static final class IsUseRandomSeed implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UseRandomSeedRef.class).isTrue();
        }

    }

    static final class InMemoryIsFalse implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(InMemoryRef.class).isFalse();
        }

    }

    static final class NominalColumnChoicesProvider extends CompatibleColumnsProvider {
        NominalColumnChoicesProvider() {
            super(List.of(NominalValue.class, NominalDistributionValue.class));
        }
    }

    static final class IncludedColumnsPersistor extends LegacyColumnFilterPersistor {

        protected IncludedColumnsPersistor() {
            super("column-filter");
        }

    }

    static final class LogRegCompatibleColumnFilterProvider implements ColumnChoicesProvider {

        private Supplier<String> m_classColumnSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_classColumnSupplier = initializer.computeFromValueSupplier(TargetColumnRef.class);
        }

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            String classCol = m_classColumnSupplier.get();
            if (classCol == null || classCol.isEmpty()) {
                return List.of();
            }

            final var specOpt = context.getInTableSpec(0);
            if (specOpt.isEmpty()) {
                return List.of();
            }

            return specOpt.get().stream().filter(col -> !col.getName().equals(classCol))
                .filter(LogRegCompatibleColumnFilterProvider::isIncluded).toList();
        }

        private static boolean isIncluded(final DataColumnSpec col) {
            return hasCompatibleType(col,
                List.of(DoubleValue.class, BitVectorValue.class, ByteVectorValue.class, NominalValue.class));
        }

        private static boolean hasCompatibleType(final DataColumnSpec col,
            final Collection<Class<? extends DataValue>> valueClasses) {
            return valueClasses.stream().anyMatch(valueClass -> col.getType().isCompatible(valueClass));
        }

    }

    static final class TargetReferenceCategoryProvider implements StateProvider<String> {

        private Supplier<String> m_targetColumnSupplier;

        private Supplier<String> m_referenceCategorySupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_targetColumnSupplier = initializer.computeFromValueSupplier(TargetColumnRef.class);
            m_referenceCategorySupplier = initializer.getValueSupplier(TargetReferenceCategoryRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            final var targetColumn = m_targetColumnSupplier.get();
            if (targetColumn == null || targetColumn.isEmpty()) {
                return null;
            }

            final var specOpt = context.getInTableSpec(0);
            if (specOpt.isEmpty()) {
                return null;
            }

            final var targetSpec = specOpt.get().getColumnSpec(targetColumn);
            final List<String> possibleValues;
            if (targetSpec.getType().isCompatible(NominalDistributionValue.class)) {
                possibleValues =
                    NominalDistributionValueMetaData.extractFromSpec(targetSpec).getValues().stream().toList();
            } else {
                final var domain = targetSpec.getDomain();
                possibleValues =
                    domain.hasValues() ? domain.getValues().stream().map(DataCell::toString).toList() : List.of();
            }
            if (possibleValues.isEmpty()) {
                return null;
            }
            final var referenceCategory = m_referenceCategorySupplier.get();
            if (referenceCategory == null || referenceCategory.isEmpty()
                || !possibleValues.contains(referenceCategory)) {
                return possibleValues.get(possibleValues.size() - 1);
            }
            return referenceCategory;
        }

    }

    static final class TargetReferenceCategoryChoicesProvider implements StringChoicesProvider {

        Supplier<String> m_targetColumnSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            StringChoicesProvider.super.init(initializer);
            m_targetColumnSupplier = initializer.computeFromValueSupplier(TargetColumnRef.class);
        }

        @Override
        public List<String> choices(final NodeParametersInput context) {
            if (m_targetColumnSupplier.get() == null || m_targetColumnSupplier.get().isEmpty()) {
                return List.of();
            }

            final var specOpt = context.getInTableSpec(0);
            if (specOpt.isEmpty()) {
                return List.of();
            }

            final var targetColumn = m_targetColumnSupplier.get();
            final var targetSpec = specOpt.get().getColumnSpec(targetColumn);
            if (targetSpec.getType().isCompatible(NominalDistributionValue.class)) {
                return new ArrayList<>(NominalDistributionValueMetaData.extractFromSpec(targetSpec).getValues());
            } else {
                final var domain = targetSpec.getDomain();
                return domain.hasValues() ? domain.getValues().stream().map(DataCell::toString).toList() : List.of();
            }
        }

    }

    static final class NewSeedValueProvider implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(NewSeedButtonRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return Long.toString(randomSeedGenerator.nextLong());
        }

    }

    static final class TargetReferenceCategoryPersistor implements NodeParametersPersistor<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var cell = settings.getDataCell(LogRegLearnerSettings.CFG_TARGET_REFERENCE_CATEGORY, null);
            return cell == null ? null : cell.toString();
        }

        @Override
        public void save(final String value, final NodeSettingsWO settings) {
            if (value != null) {
                settings.addDataCell(LogRegLearnerSettings.CFG_TARGET_REFERENCE_CATEGORY,
                    new DataCellFactory().createDataCellOfType(DataType.getType(StringCell.class), value));
            } else {
                settings.addDataCell(LogRegLearnerSettings.CFG_TARGET_REFERENCE_CATEGORY, null);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{LogRegLearnerSettings.CFG_TARGET_REFERENCE_CATEGORY}};
        }

    }

    static final class SeedParametersPersistor implements NodeParametersPersistor<SeedParameters> {

        @Override
        public SeedParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var seedParameters = new SeedParameters();
            var seedSetting = settings.getString(LogRegLearnerSettings.CFG_SEED,
                Long.toString(LogRegLearnerNodeDialogPane.DEFAULT_RANDOM_SEED));
            seedParameters.m_useRandomSeed = seedSetting != null;
            seedParameters.m_randomSeed = seedSetting;
            return seedParameters;
        }

        @Override
        public void save(final SeedParameters param, final NodeSettingsWO settings) {
            if (param.m_useRandomSeed) {
                settings.addString(LogRegLearnerSettings.CFG_SEED, param.m_randomSeed);
            } else {
                settings.addString(LogRegLearnerSettings.CFG_SEED, null);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{LogRegLearnerSettings.CFG_SEED}};
        }

    }

    static final class SolverMigration implements DefaultProvider<Solver> {

        @Override
        public Solver getDefault() {
            return LogRegLearnerSettings.DEFAULT_SOLVER;
        }

    }

}
