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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
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
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for Feature Selection Filter.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class FeatureSelectionFilterNodeParameters implements NodeParameters {

    @TextMessage(OptimizationInfoProvider.class)
    Void m_optimizationInfo;

    @Widget(title = "Feature selection mode", description = """
            Choose how the features are selected from the available feature sets computed during the feature selection
            loop.
            """)
    @ValueSwitchWidget
    @ValueReference(SelectionModeRef.class)
    @Persistor(SelectionModePersistor.class)
    SelectionMode m_selectionMode = SelectionMode.NUMBER_OF_FEATURES;

    interface SelectionModeRef extends ParameterReference<SelectionMode> {
    }

    @Persist(configKey = FeatureSelectionFilterSettings.NR_OF_FEATURES_KEY)
    @Widget(title = "Number of features", description = """
            Specify the number of features to include.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class,
        maxValidationProvider = NrOfFeaturesMaxValidationProvider.class)
    @ValueReference(NrOfFeaturesRef.class)
    @Effect(predicate = IsManualMode.class, type = EffectType.SHOW)
    int m_nrOfFeatures = 1;

    static final class NrOfFeaturesRef implements ParameterReference<Integer> {
    }

    @Persist(configKey = FeatureSelectionFilterSettings.ERROR_THRESHOLD_KEY)
    @Widget(title = "Prediction score threshold", description = """
            Enter a prediction score threshold here. The smallest feature set whose score meets the threshold
            requirement is selected.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class,
        maxValidation = IsAtMostOneValidation.class, stepSize = 0.01)
    @ValueReference(ErrorThresholdRef.class)
    @Effect(predicate = IsThresholdMode.class, type = EffectType.SHOW)
    double m_errorThreshold = 0.5;

    static final class ErrorThresholdRef implements ParameterReference<Double> {
    }

    static final class IsAtMostOneValidation extends MaxValidation {

        @Override
        protected double getMax() {
            return 1.0;
        }

    }

    @Persist(configKey = FeatureSelectionFilterSettings.INCLUDE_TARGET_KEY)
    @Widget(title = "Include static columns", description = """
            If checked, any static columns (columns that were not part of the feature selection, i.e. constant columns
            kept throughout the selection loop) are included in the output table. Otherwise, they are filtered out.
            """)
    @ValueReference(IncludeStaticColumns.class)
    boolean m_includeStaticColumns;

    static final class IncludeStaticColumns implements BooleanReference {
    }

    @Persist(configKey = FeatureSelectionFilterSettings.SELECTED_FEATURES_KEY)
    @Widget(title = "Included features", description = """
            This shows the columns that are included in the output table. The list is automatically updated based on
            the selected feature set and whether static columns are included. You can also manually add or remove
            columns from this list. Manually added columns must be present in the input table and will be ignored if
            they are not. Manually removed columns will be excluded from the output even if they are part of the
            selected feature set or are static columns.
            """)
    @TwinlistWidget
    @ChoicesProvider(IncludedColumnsChoicesProvider.class)
    @ValueProvider(IncludedColumnsProvider.class)
    @ValueReference(IncludedColumnsRef.class)
    String[] m_selectedFeatures = new String[0];

    static final class IncludedColumnsRef implements ParameterReference<String[]> {
    }

    @TextMessage(FeatureSelectionFilterInfoProvider.class)
    Void m_featureSelectionFilterInfo;

    static final class IsManualMode implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SelectionModeRef.class).isOneOf(SelectionMode.NUMBER_OF_FEATURES);
        }

    }

    static final class IsThresholdMode implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SelectionModeRef.class).isOneOf(SelectionMode.THRESHOLD);
        }

    }

    static final class OptimizationInfoProvider implements StateProvider<Optional<TextMessage.Message>> {

        Supplier<Optional<Pair<FeatureSelectionModel, List<Pair<Double, Collection<String>>>>>>
            m_featureSelectionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_featureSelectionSupplier = initializer.computeFromProvidedState(FeatureSelectionModelProvider.class);
        }

        @Override
        public Optional<TextMessage.Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var featureSelectionOpt = m_featureSelectionSupplier.get();
            if (featureSelectionOpt.isEmpty()) {
                return Optional.empty();
            }
            final var featureSelection = featureSelectionOpt.get();
            return Optional.of(new TextMessage.Message("Optimization Criterion",
                featureSelection.getFirst().isMinimize() ?
                    "The score is being minimized." : "The score is being maximized.",
                TextMessage.MessageType.INFO));
        }

    }

    static final class FeatureSelectionFilterInfoProvider implements StateProvider<Optional<TextMessage.Message>> {

        Supplier<Pair<FeatureSelectionModel, Collection<String>>> m_featureSelectionSupplier;
        Supplier<SelectionMode> m_selectionModeSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_featureSelectionSupplier = initializer.computeFromProvidedState(FeatureSelectionProvider.class);
            m_selectionModeSupplier = initializer.computeFromValueSupplier(SelectionModeRef.class);
        }

        @Override
        public Optional<TextMessage.Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var featureSelection = m_featureSelectionSupplier.get();
            final var fsModel = featureSelection.getFirst();
            final var selectedFeatures = featureSelection.getSecond();
            if (fsModel == null || selectedFeatures == null) {
                return Optional.empty();
            }
            final var inSpecOpt = parametersInput.getInTableSpec(1);
            if (inSpecOpt.isEmpty()) {
                return Optional.empty();
            }

            if (m_selectionModeSupplier.get() == SelectionMode.THRESHOLD && selectedFeatures.isEmpty()) {
                return Optional.of(new TextMessage.Message("Empty feature selection",
                    "No feature combination with prediction error below the threshold does exist",
                    TextMessage.MessageType.INFO));
            }

            final var inSpec = inSpecOpt.get();
            if (selectedFeatures.stream().allMatch(inSpec::containsName)) {
                return Optional.of(new TextMessage.Message("Input table does not contain all selected features",
                    "Some features are missing in the input table", TextMessage.MessageType.INFO));
            }
            return Optional.empty();
        }

    }

    static final class NrOfFeaturesMaxValidationProvider implements StateProvider<MaxValidation> {

        Supplier<SelectionMode> m_selectionModeSupllier;

        Supplier<Integer> m_nrOfFeaturesSupplier;

        Supplier<Pair<FeatureSelectionModel, Collection<String>>> m_featureSelectionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_selectionModeSupllier = initializer.computeFromValueSupplier(SelectionModeRef.class);
            m_nrOfFeaturesSupplier = initializer.computeFromValueSupplier(NrOfFeaturesRef.class);
            m_featureSelectionSupplier = initializer.computeFromProvidedState(FeatureSelectionProvider.class);
        }

        @Override
        public MaxValidation computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_nrOfFeaturesSupplier.get() == null) {
                return null;
            }
            final var featureSelection = m_featureSelectionSupplier.get();
            final var selectedFeatures = featureSelection.getSecond();
            if (m_selectionModeSupllier.get() == SelectionMode.NUMBER_OF_FEATURES
                && (selectedFeatures == null || selectedFeatures.isEmpty())) {
                return new MaxValidation() {

                    @Override
                    protected double getMax() {
                        return m_nrOfFeaturesSupplier.get() - 1.0;
                    }

                    @Override
                    public String getErrorMessage() {
                        return "There exists no feature level with the specified length: %s."
                            .formatted(m_nrOfFeaturesSupplier.get());
                    }

                };
            }
            return null;
        }

    }

    static final class IncludedColumnsProvider implements StateProvider<String[]> {

        Supplier<Pair<FeatureSelectionModel, Collection<String>>> m_featureSelectionSupplier;
        Supplier<Boolean> m_includeStaticColumnsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_featureSelectionSupplier = initializer.computeFromProvidedState(FeatureSelectionProvider.class);
            m_includeStaticColumnsSupplier = initializer.computeFromValueSupplier(IncludeStaticColumns.class);
        }

        @Override
        public String[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var featureSelection = m_featureSelectionSupplier.get();
            final var fsModel = featureSelection.getFirst();
            final var selectedFeatures = featureSelection.getSecond();
            if (fsModel == null || selectedFeatures == null || selectedFeatures.isEmpty()) {
                return new String[0];
            }
            final var inSpecOpt = parametersInput.getInTableSpec(1);
            if (inSpecOpt.isEmpty()) {
                return new String[0];
            }
            final var inSpec = inSpecOpt.get();
            return addStaticColumnsAndFilterByInputSpec(
                selectedFeatures, fsModel, inSpec, m_includeStaticColumnsSupplier.get());
        }

        private static String[] addStaticColumnsAndFilterByInputSpec(final Collection<String> selectedFeatures,
            final FeatureSelectionModel fsModel, final DataTableSpec inSpec, final boolean includeStatic) {
            if (selectedFeatures != null && includeStatic) {
                selectedFeatures.addAll(Arrays.asList(fsModel.getConstantColumns()));
            }
            return selectedFeatures != null ?
                selectedFeatures.stream().filter(inSpec::containsName).toArray(String[]::new)
                : new String[0];
        }

    }

    static final class FeatureSelectionProvider
        implements StateProvider<Pair<FeatureSelectionModel, Collection<String>>> {

        Supplier<Optional<Pair<FeatureSelectionModel, List<Pair<Double, Collection<String>>>>>> m_fsModelSupplier;
        Supplier<SelectionMode> m_selectionModeSupplier;
        Supplier<Integer> m_nrOfFeaturesSupplier;
        Supplier<Double> m_errorThresholdSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_fsModelSupplier = initializer.computeFromProvidedState(FeatureSelectionModelProvider.class);
            m_selectionModeSupplier = initializer.computeFromValueSupplier(SelectionModeRef.class);
            m_nrOfFeaturesSupplier = initializer.computeFromValueSupplier(NrOfFeaturesRef.class);
            m_errorThresholdSupplier = initializer.computeFromValueSupplier(ErrorThresholdRef.class);
        }

        @Override
        public Pair<FeatureSelectionModel, Collection<String>> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_nrOfFeaturesSupplier.get() == null || m_errorThresholdSupplier.get() == null) {
                return new Pair<>(null, List.of());
            }
            final var featureSelectionOpt = m_fsModelSupplier.get();
            if (featureSelectionOpt.isEmpty()) {
                return new Pair<>(null, List.of());
            }
            final var featureSelection = featureSelectionOpt.get();
            return switch (m_selectionModeSupplier.get()) {
                case NUMBER_OF_FEATURES -> filterByNumberOfFeatures(featureSelection, m_nrOfFeaturesSupplier.get());
                case BEST_SCORE -> filterByBestScore(featureSelection.getFirst());
                case THRESHOLD -> filterByThreshold(featureSelection.getFirst(), m_errorThresholdSupplier.get());
            };
        }

        private static Pair<FeatureSelectionModel, Collection<String>> filterByNumberOfFeatures(
            final Pair<FeatureSelectionModel, List<Pair<Double, Collection<String>>>> featureSelection,
            final int nrOfFeatures) {
            final var thresholdToNrFeaturesList = featureSelection.getSecond();
            if (thresholdToNrFeaturesList == null || thresholdToNrFeaturesList.isEmpty()) {
                return new Pair<>(featureSelection.getFirst(), List.of());
            }
            Optional<Collection<String>> selectedFeatureSubSet = thresholdToNrFeaturesList.stream().map(Pair::getSecond)
                .filter(c -> c.size() == nrOfFeatures).findFirst();
            return new Pair<>(featureSelection.getFirst(),
                    selectedFeatureSubSet.isEmpty() ? List.of() : selectedFeatureSubSet.get());
        }

        private static Pair<FeatureSelectionModel, Collection<String>> filterByBestScore(
            final FeatureSelectionModel fsModel) {
            return new Pair<>(fsModel, fsModel.getBestScore());
        }

        private static Pair<FeatureSelectionModel, Collection<String>> filterByThreshold(
            final FeatureSelectionModel fsModel, final double threshold) {
            return new Pair<>(fsModel, fsModel.getNamesOfMinimialSet(threshold));
        }

    }

    static final class IncludedColumnsChoicesProvider implements StringChoicesProvider {

        @Override
        public List<String> choices(final NodeParametersInput context) {
            return ColumnSelectionUtil.getAllColumns(context, 1).stream().map(col -> col.getName()).toList();
        }

    }

    static final class FeatureSelectionModelProvider implements
        StateProvider<Optional<Pair<FeatureSelectionModel, List<Pair<Double, Collection<String>>>>>> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Optional<Pair<FeatureSelectionModel, List<Pair<Double, Collection<String>>>>>
            computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            final var modelObjOpt = parametersInput.getInPortObject(0);
            if (modelObjOpt.isEmpty()) {
                return Optional.empty();
            }
            final var fsModel = (FeatureSelectionModel)modelObjOpt.get();
            if (fsModel == null) {
                return Optional.empty();
            }

            List<Pair<Double, Collection<String>>> thresholdToNrFeaturesList = fsModel.featureLevels().stream()
                    .map(lvl -> new Pair<>(lvl.getFirst(), lvl.getSecond())).collect(Collectors.toList());
            Collections.sort(thresholdToNrFeaturesList, createComparator(fsModel));
            return Optional.of(new Pair<>(fsModel, thresholdToNrFeaturesList));
        }

        private static Comparator<Pair<Double, Collection<String>>> createComparator(
            final FeatureSelectionModel fsModel) {
            return (o1, o2) -> {
                final int diff = o1.getFirst().compareTo(o2.getFirst());
                if (diff != 0) {
                    return fsModel.isMinimize() ? diff : -diff;
                }
                return -o1.getSecond().size() + o2.getSecond().size();
            };
        }

    }

    static final class SelectionModePersistor implements NodeParametersPersistor<SelectionMode> {

        @Override
        public SelectionMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean(FeatureSelectionFilterSettings.THRESHOLD_MODE_KEY, false)) {
                return SelectionMode.THRESHOLD;
            } else if (settings.getBoolean(FeatureSelectionFilterSettings.BEST_SCORE_MODE_KEY, false)) {
                return SelectionMode.BEST_SCORE;
            } else {
                return SelectionMode.NUMBER_OF_FEATURES;
            }
        }

        @Override
        public void save(final SelectionMode mode, final NodeSettingsWO settings) {
            settings.addBoolean(FeatureSelectionFilterSettings.THRESHOLD_MODE_KEY, mode == SelectionMode.THRESHOLD);
            settings.addBoolean(FeatureSelectionFilterSettings.BEST_SCORE_MODE_KEY, mode == SelectionMode.BEST_SCORE);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{FeatureSelectionFilterSettings.THRESHOLD_MODE_KEY},
                {FeatureSelectionFilterSettings.BEST_SCORE_MODE_KEY}};
        }

    }

    enum SelectionMode {

        @Label(value = "Features", description = """
                Choose a set of features by specifying the number of features to include. The feature set with the
                matching number of features from the trained model is selected.
                """)
        NUMBER_OF_FEATURES, //
        @Label(value = "Best score", description = """
                The feature set with the best prediction score is chosen. If the score is being maximized, the best
                score is the highest score value; if the score is being minimized, the best score is the lowest
                score value.
                """)
        BEST_SCORE, //
        @Label(value = "Threshold", description = """
                Set a prediction score threshold. The smallest feature set whose score meets the threshold requirement
                is selected: below the threshold if the score is minimized, above the threshold if the score is
                maximized.
                """)
        THRESHOLD;

    }

}
