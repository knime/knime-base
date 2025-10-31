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

package org.knime.base.node.mine.cluster.fuzzycmeans;

import java.util.List;
import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveDoubleValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for Fuzzy c-Means.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class FuzzyClusterNodeParameters implements NodeParameters {

    @Section(title = "Noise Clustering")
    interface NoiseClusteringSection {
    }

    @Widget(title = "Number of clusters", description = "Number of clusters to use for the algorithm.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class,
        maxValidation = IsIterationAndClusterMaxValidation.class)
    @Persist(configKey = FuzzyClusterNodeModel.NRCLUSTERS_KEY)
    int m_nrClusters = 3;

    @Widget(title = "Maximum number of iterations",
        description = "This is the maximum number of iterations to be performed.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class,
        maxValidation = IsIterationAndClusterMaxValidation.class)
    @Persist(configKey = FuzzyClusterNodeModel.MAXITERATIONS_KEY)
    int m_maxNrIterations = 99;

    @Widget(title = "Fuzzifier", description = "Indicates how much the clusters are allowed to overlap.")
    @NumberInputWidget(minValidation = IsFuzzifierMinValidation.class, maxValidation = IsFuzzifierMaxValidation.class)
    @Persist(configKey = FuzzyClusterNodeModel.FUZZIFIER_KEY)
    double m_fuzzifier = 2.0;

    @Widget(title = "Use seed for random initialization",
        description = "If this checkbox is set, a seed can be set for initializing the cluster prototypes.")
    @ValueReference(UseSeedRef.class)
    @Persist(configKey = FuzzyClusterNodeModel.USE_SEED_KEY)
    boolean m_useRandomSeed;

    @Widget(title = "Random seed", description = """
            The seed value used for random number generation. Use the same seed to get identical clustering results
            across multiple executions.
            """)
    @NumberInputWidget
    @Effect(predicate = UseSeedPredicate.class, type = EffectType.SHOW)
    @ValueProvider(SeedValueProvider.class)
    @Persist(configKey = FuzzyClusterNodeModel.SEED_KEY)
    int m_randomSeed;

    @Widget(title = "Draw seed",
        description = "Generate a random seed and set it in the Random seed input above for reproducible runs.")
    @SimpleButtonWidget(ref = DrawSeedButtonRef.class)
    @Effect(predicate = UseSeedPredicate.class, type = EffectType.SHOW)
    Void m_drawSeed;

    static final class KeepAllColumnsSelectedRef implements BooleanReference {
    }

    @Persist(configKey = FuzzyClusterNodeModel.CFGKEY_KEEPALL)
    @Widget(title = "Always include all columns", description = """
            If checked, node behaves as if all columns were moved to the Include list.
            """)
    @ValueReference(KeepAllColumnsSelectedRef.class)
    boolean m_keepAllColumnsSelected;

    @Persist(configKey = FuzzyClusterNodeModel.INCLUDELIST_KEY)
    @Widget(title = "Used attributes", description = """
            Select the columns that are to be used for clustering. Make sure that the input data is
            normalized to obtain better clustering results.
            """)
    @ValueProvider(UsedAttributesProvider.class)
    @ValueReference(UsedAttributesFilterRef.class)
    @ChoicesProvider(DoubleColumnsProvider.class)
    @TwinlistWidget
    String[] m_usedAttributes = new String[0];

    @Layout(NoiseClusteringSection.class)
    @Widget(title = "Induce noise cluster", description = "Whether to induce a noise cluster or not.")
    @ValueReference(NoiseClusterRef.class)
    @Persist(configKey = FuzzyClusterNodeModel.NOISE_KEY)
    boolean m_noise;

    @Layout(NoiseClusteringSection.class)
    @Widget(title = "Delta computation method",
        description = "Choose whether to set a fixed delta value or to compute it automatically based on lambda.")
    @Effect(predicate = IsNoiseCluster.class, type = EffectType.SHOW)
    @ValueReference(DeltaMethodReference.class)
    DeltaMethod m_deltaMethod = DeltaMethod.SET_DELTA;

    @Layout(NoiseClusteringSection.class)
    @Widget(title = "Delta", description = "The delta value.")
    @NumberInputWidget(minValidation = IsPositiveDoubleValidation.class,
        maxValidation = IsDeltaAndLambdaMaxValidation.class)
    @Effect(predicate = IsSetDeltaAndNoiseCluster.class, type = EffectType.SHOW)
    @Persistor(DeltaPersistor.class)
    double m_delta = 0.2;

    @Layout(NoiseClusteringSection.class)
    @Widget(title = "Lambda", description = "The lambda value.")
    @NumberInputWidget(minValidation = IsPositiveDoubleValidation.class,
        maxValidation = IsDeltaAndLambdaMaxValidation.class)
    @Effect(predicate = IsAutoDeltaAndNoiseCluster.class, type = EffectType.SHOW)
    @Persistor(LambdaPersistor.class)
    double m_lambda = 0.1;

    @Layout(NoiseClusteringSection.class)
    @Advanced
    @Widget(title = "Perform the clustering in memory", description = """
            If this option is selected, the clustering is performed in the memory, which speeds up the process.
            """)
    @Persist(configKey = FuzzyClusterNodeModel.MEMORY_KEY)
    boolean m_memory = true;

    @Layout(NoiseClusteringSection.class)
    @Advanced
    @Widget(title = "Compute cluster quality measures", description = """
            Whether to calculate quality measures for the clustering. This can be time and memory consuming with large
            datasets.
            """)
    @Persist(configKey = FuzzyClusterNodeModel.MEASURES_KEY)
    boolean m_measures = true;

    static final class UseSeedRef implements BooleanReference {
    }

    static final class DrawSeedButtonRef implements ButtonReference {
    }

    static final class UsedAttributesFilterRef implements ParameterReference<String[]> {
    }

    static final class NoiseClusterRef implements BooleanReference {
    }

    static final class DeltaMethodReference implements ParameterReference<DeltaMethod> {
    }

    private static final class UseSeedPredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(UseSeedRef.class);
        }

    }

    private static final class IsNoiseCluster implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(NoiseClusterRef.class).isTrue();
        }

    }

    private static final class IsSetDeltaAndNoiseCluster implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(DeltaMethodReference.class).isOneOf(DeltaMethod.SET_DELTA)
                .and(i.getBoolean(NoiseClusterRef.class).isTrue());
        }

    }

    private static final class IsAutoDeltaAndNoiseCluster implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(DeltaMethodReference.class).isOneOf(DeltaMethod.AUTO_DELTA)
                .and(i.getBoolean(NoiseClusterRef.class).isTrue());
        }

    }

    static final class SeedValueProvider implements StateProvider<Integer> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(DrawSeedButtonRef.class);
        }

        @Override
        public Integer computeState(final NodeParametersInput parametersInput) {
            return (int)(2 * (Math.random() - 0.5) * Integer.MAX_VALUE);
        }

    }

    static final class UsedAttributesProvider implements StateProvider<String[]> {

        Supplier<String[]> m_usedAttributesFilterSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_usedAttributesFilterSupplier = initializer.getValueSupplier(UsedAttributesFilterRef.class);
        }

        @Override
        public String[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_usedAttributesFilterSupplier.get() != null && m_usedAttributesFilterSupplier.get().length != 0) {
                throw new StateComputationFailureException();
            }

            return parametersInput.getInTableSpec(0).stream()//
                .map(ColumnSelectionUtil::getDoubleColumns).flatMap(List::stream)//
                .map(DataColumnSpec::getName)//
                .toArray(String[]::new);
        }

    }

    static final class IsIterationAndClusterMaxValidation extends MaxValidation {

        @Override
        protected double getMax() {
            return 9999;
        }

    }

    static final class IsDeltaAndLambdaMaxValidation extends MaxValidation {

        @Override
        protected double getMax() {
            return 1.0;
        }

    }

    static final class IsFuzzifierMinValidation extends MinValidation {

        @Override
        protected double getMin() {
            return 1.0;
        }

        @Override
        public boolean isExclusive() {
            return true;
        }

    }

    static final class IsFuzzifierMaxValidation extends MaxValidation {

        @Override
        protected double getMax() {
            return 10.0;
        }

    }

    static final class DeltaPersistor implements NodeParametersPersistor<Double> {

        @Override
        public Double load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(FuzzyClusterNodeModel.DELTAVALUE_KEY)) {
                double delta = settings.getDouble(FuzzyClusterNodeModel.DELTAVALUE_KEY);
                return delta > 0 ? delta : 0.2;
            }
            return 0.2;
        }

        @Override
        public void save(final Double param, final NodeSettingsWO settings) {
            settings.addDouble(FuzzyClusterNodeModel.DELTAVALUE_KEY, param);
            settings.addDouble(FuzzyClusterNodeModel.LAMBDAVALUE_KEY, -1);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{FuzzyClusterNodeModel.DELTAVALUE_KEY}};
        }
    }

    static final class LambdaPersistor implements NodeParametersPersistor<Double> {

        @Override
        public Double load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(FuzzyClusterNodeModel.LAMBDAVALUE_KEY)) {
                double lambda = settings.getDouble(FuzzyClusterNodeModel.LAMBDAVALUE_KEY);
                return lambda > 0 ? lambda : 0.1;
            }
            return 0.1;
        }

        @Override
        public void save(final Double param, final NodeSettingsWO settings) {
            settings.addDouble(FuzzyClusterNodeModel.LAMBDAVALUE_KEY, param);
            settings.addDouble(FuzzyClusterNodeModel.DELTAVALUE_KEY, -1);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{FuzzyClusterNodeModel.LAMBDAVALUE_KEY}};
        }
    }

    enum DeltaMethod {

            @Label(value = "Automatic delta, specify lambda", description = """
                    Delta is updated in each iteration, based on the average interpoint distances. However, a lambda
                    paramater has to be set, according to the shape of the clusters.
                    """)
            AUTO_DELTA, //
            @Label(value = "Set delta",
                description = "Delta is the fixed distance from every datapoint to the noise cluster.")
            SET_DELTA;

    }

}
