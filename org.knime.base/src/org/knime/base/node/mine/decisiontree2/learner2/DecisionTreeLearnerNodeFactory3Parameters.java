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

package org.knime.base.node.mine.decisiontree2.learner2;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.base.node.mine.decisiontree2.PMMLMissingValueStrategy;
import org.knime.base.node.mine.decisiontree2.PMMLNoTrueChildStrategy;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.choices.util.FilteredInputTableColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for Decision Tree Learner.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class DecisionTreeLearnerNodeFactory3Parameters implements NodeParameters {

    @Section(title = "PMML Settings")
    interface PMMLSettingsSection {
    }

    @Persist(configKey = DecisionTreeLearnerNodeModel2.KEY_CLASSIFYCOLUMN)
    @Widget(title = "Class column", description = "Select the target attribute. Only nominal attributes are allowed.")
    @ChoicesProvider(NominalColumnsProvider.class)
    @ValueReference(ClassColumnRef.class)
    String m_classifyColumn;

    static final class ClassColumnRef implements ParameterReference<String> {
    }

    @Persistor(QualityMeasurePersistor.class)
    @Widget(title = "Quality measure", description = """
            Select the quality measure according to which the split is calculated.
            """)
    @ValueSwitchWidget
    QualityMeasure m_qualityMeasure = QualityMeasure.GINI_INDEX;

    @Persistor(PruningMethodPersistor.class)
    @Widget(title = "Pruning method", description = """
            Pruning reduces tree size and avoids overfitting which increases the generalization performance, and thus,
            the prediction quality (for predictions, use the "Decision Tree Predictor" node).
            """)
    @ValueSwitchWidget
    PruningMethod m_pruningMethod = PruningMethod.NO_PRUNING;

    @Persist(configKey = DecisionTreeLearnerNodeModel2.KEY_REDUCED_ERROR_PRUNING)
    @Widget(title = "Reduced error pruning", description = """
            If checked (default), a simple pruning method is used to cut the tree in a post-processing step: Starting
            at the leaves, each node is replaced with its most popular class, but only if the prediction accuracy
            doesn't decrease. Reduced error pruning has the advantage of simplicity and speed.
            """)
    boolean m_reducedErrorPruning = DecisionTreeLearnerNodeModel2.DEFAULT_REDUCED_ERROR_PRUNING;

    @Persist(configKey = DecisionTreeLearnerNodeModel2.KEY_MIN_NUMBER_RECORDS_PER_NODE)
    @Widget(title = "Minimum number of records per node", description = """
            Select the minimum number of records at least required in each node. If the number of records is smaller or
            equal to this number the tree is not grown any further. This corresponds to a stopping criteria (pre
            pruning).
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_minNumberRecordsPerNode = DecisionTreeLearnerNodeModel2.DEFAULT_MIN_NUM_RECORDS_PER_NODE;

    @Persist(configKey = DecisionTreeLearnerNodeModel2.KEY_NUMBER_VIEW_RECORDS)
    @Widget(title = "Number of records to store for view", description = """
            Select the number of records stored in the tree for the view. The records are necessary to enable
            highlighting.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class, stepSize = 100)
    int m_numberRecordsStoredForView = DecisionTreeLearnerNodeModel2.DEFAULT_NUMBER_RECORDS_FOR_VIEW;

    @Persist(configKey = DecisionTreeLearnerNodeModel2.KEY_SPLIT_AVERAGE)
    @Widget(title = "Average split point", description = """
            If checked (default), the split value for numeric attributes is determined according to the mean value of
            the two attribute values that separate the two partitions. If unchecked, the split value is set to the
            largest value of the lower partition (like C4.5).
            """)
    boolean m_averageSplitPoint = DecisionTreeLearnerNodeModel2.DEFAULT_SPLIT_AVERAGE;

    @Persist(configKey = DecisionTreeLearnerNodeModel2.KEY_NUM_PROCESSORS)
    @Widget(title = "Number threads", description = """
            This node can exploit multiple threads and thus multiple processors or cores. This can improve performance.
            The default value is set to the number of processors or cores that are available to KNIME. If set to 1, the
            algorithm is performed sequentially.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_numProcessors = getDefaultNumberOfProcessors();

    // for mocking in tests
    static int getDefaultNumberOfProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    @Persist(configKey = DecisionTreeLearnerNodeModel2.KEY_SKIP_COLUMNS)
    @Widget(title = "Skip nominal columns without domain information", description = """
            If checked, nominal columns containing no domain value information are skipped. This is generally the case
            for nominal columns that have too many different values.
            """)
    @Migration(LoadFalseForOldNodesMigration.class)
    boolean m_skipColumnsWithoutDomain = true;

    @Persist(configKey = DecisionTreeLearnerNodeModel2.KEY_USE_FIRST_SPLIT_COL)
    @Widget(title = "Force root split column", description = """
            If checked, the first split is calculated on the chosen column without evaluating any other column for
            possible splits. This is sometimes useful if the user has additional information as to which column is best
            to split on even if it does not have the numeric best split quality. In case the selected column does not
            contain valid splits (e.g. because it has a constant value in all rows) a warning message will be displayed.
            If uncertain, leave unselected.
            """)
    @ValueReference(IsUseFirstSplitColumnEnabled.class)
    @Migration(LoadFalseForOldNodesMigration.class)
    boolean m_useFirstSplitColumn;

    static final class IsUseFirstSplitColumnEnabled implements BooleanReference {
    }

    static final class LoadFalseForOldNodesMigration implements DefaultProvider<Boolean> {

        @Override
        public Boolean getDefault() {
            return false;
        }

    }

    @Persist(configKey = DecisionTreeLearnerNodeModel2.KEY_FIRST_SPLIT_COL)
    @Widget(title = "Root split column", description = """
            The column to perform the root split on.
            """)
    @ChoicesProvider(NominalOrNumericColumnsProvider.class)
    @Effect(predicate = IsUseFirstSplitColumnEnabled.class, type = EffectType.SHOW)
    @ValueProvider(FirstSplitColumnValueProvider.class)
    @ValueReference(FirstSplitColumnRef.class)
    String m_firstSplitColumn;

    static final class FirstSplitColumnRef implements ParameterReference<String> {
    }

    @Persist(configKey = DecisionTreeLearnerNodeModel2.KEY_BINARY_NOMINAL_SPLIT_MODE)
    @Widget(title = "Binary nominal splits", description = """
            If checked, nominal attributes are split in a binary fashion. Binary splits are more difficult to calculate
            but result also in more accurate trees. The nominal values are divided in two subsets (one for each child).
            If unchecked, for each nominal value one child is created.
            """)
    @ValueReference(IsBinaryNominalSplitEnabled.class)
    boolean m_binaryNominalSplitMode = DecisionTreeLearnerNodeModel2.DEFAULT_BINARY_NOMINAL_SPLIT_MODE;

    static final class IsBinaryNominalSplitEnabled implements BooleanReference {
    }

    @Persist(configKey = DecisionTreeLearnerNodeModel2.KEY_BINARY_MAX_NUM_NOMINAL_VALUES)
    @Widget(title = "Maximum number of nominal values", description = """
            The subsets for the binary nominal splits are difficult to calculate. To find the best subsets for n
            nominal values there must be performed 2^n calculations. In case of many different nominal values this can
            be prohibitive expensive. Thus the maximum number of nominal values can be defined for which all possible
            subsets are calculated. Above this threshold, a heuristic is applied that first calculates the best nominal
            value for the second partition, then the second best value, and so on; until no improvement can be
            achieved.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = IsBinaryNominalSplitEnabled.class, type = EffectType.SHOW)
    int m_maxNumNominalValues = DecisionTreeLearnerNodeModel2.DEFAULT_MAX_BIN_NOMINAL_SPLIT_COMPUTATION;

    @Persist(configKey = DecisionTreeLearnerNodeModel2.KEY_FILTER_NOMINAL_VALUES_FROM_PARENT)
    @Widget(title = "Filter invalid attribute values in child nodes", description = """
            Binary splits on nominal values may lead to tests for attribute values, which have been filtered out by a
            parent tree node. This is due to the fact that the learning algorithm is consistently using the table's
            domain information instead of the data in a tree node to define the split sets. These duplicate checks do
            not harm (the tree is the same and and will classify unknown data the exact same way), though they are
            confusing when the tree is inspected in the tree viewer. Enabling this option will post-process the tree
            and filter invalid checks.
            """)
    @Effect(predicate = IsBinaryNominalSplitEnabled.class, type = EffectType.SHOW)
    boolean m_filterNominalValuesFromParent;

    @Layout(PMMLSettingsSection.class)
    @Persistor(NoTrueChildStrategyPersistor.class)
    @Widget(title = "No true child strategy", description = """
            If the scoring reaches a node, at which its attributes value is unknown, one of the two following strategies
            can be used.
            """)
    @ValueSwitchWidget
    PMMLNoTrueChildStrategy m_noTrueChildStrategy = PMMLNoTrueChildStrategy.RETURN_NULL_PREDICTION;

    @Layout(PMMLSettingsSection.class)
    @Persistor(MissingValueStrategyPersistor.class)
    @Widget(title = "Missing value strategy", description = """
            If there are missing values in the data to be predicted, a strategy can be chosen how to handle them. <br/>
            <ul>
                <li>
                    <b>Last Prediction</b>
                    If a Node's predicate evaluates to UNKNOWN while traversing the tree, evaluation is stopped and the
                    current winner is returned as the final prediction.
                </li>
                <li>
                    <b>None</b>
                    Comparisons with missing values other than checks for missing values always evaluate to FALSE. If
                    no rule fires, then use the noTrueChildStrategy to decide on a result. This option requires that
                    missing values be handled after all rules at the Node have been evaluated. Note: In contrast to
                    lastPrediction, evaluation is carried on instead of stopping immediately upon first discovery of a
                    Node who's predicate value cannot be determined due to missing values.
                </li>
            </ul>
            """)
    @ValueSwitchWidget
    @ChoicesProvider(MissingValueStrategyProvider.class)
    PMMLMissingValueStrategy m_missingValueStrategy = PMMLMissingValueStrategy.LAST_PREDICTION;

    static final class NominalColumnsProvider extends CompatibleColumnsProvider {

        NominalColumnsProvider() {
            super(NominalValue.class);
        }

    }

    static final class NominalOrNumericColumnsProvider implements FilteredInputTableColumnsProvider {

        Supplier<String> m_classColumnSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_classColumnSupplier = initializer.computeFromValueSupplier(ClassColumnRef.class);
        }

        @Override
        public boolean isIncluded(final DataColumnSpec col) {
            return hasCompatibleType(col, List.of(NominalValue.class, DoubleValue.class))
                && !col.getName().equals(m_classColumnSupplier.get());
        }

        private static boolean hasCompatibleType(final DataColumnSpec col,
            final Collection<Class<? extends DataValue>> valueClasses) {
            return valueClasses.stream().anyMatch(valueClass -> col.getType().isCompatible(valueClass));
        }

    }

    static final class FirstSplitColumnValueProvider implements StateProvider<String> {

        Supplier<String> m_classColumnSupplier;

        Supplier<String> m_firstSplitColumnSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_classColumnSupplier = initializer.computeFromValueSupplier(ClassColumnRef.class);
            m_firstSplitColumnSupplier = initializer.getValueSupplier(FirstSplitColumnRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            final var compatibleColumns = ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput,
                NominalValue.class, DoubleValue.class);
            if (compatibleColumns.isEmpty()) {
                return null;
            }
            final var classColumn = m_classColumnSupplier.get();
            final var firstSplitColumn = m_firstSplitColumnSupplier.get();
            if (firstSplitColumn != null && !firstSplitColumn.isEmpty() && !firstSplitColumn.equals(classColumn)) {
                throw new StateComputationFailureException();
            }
            final var lastColumn = compatibleColumns.get(compatibleColumns.size() - 1);
            if (classColumn == null || classColumn.isEmpty()) {
                return lastColumn.getName();
            }
            if (lastColumn.getName().equals(classColumn) && compatibleColumns.size() > 1) {
                return compatibleColumns.get(compatibleColumns.size() - 2).getName();
            }
            return compatibleColumns.size() == 1 ? null : lastColumn.getName();
        }

    }

    static final class MissingValueStrategyProvider implements EnumChoicesProvider<PMMLMissingValueStrategy> {

        @Override
        public List<PMMLMissingValueStrategy> choices(final NodeParametersInput context) {
            return List.of(PMMLMissingValueStrategy.LAST_PREDICTION, PMMLMissingValueStrategy.NONE);
        }

    }

    static final class QualityMeasurePersistor implements NodeParametersPersistor<QualityMeasure> {

        @Override
        public QualityMeasure load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return QualityMeasure.getFromValue(settings.getString(
                DecisionTreeLearnerNodeModel2.KEY_SPLIT_QUALITY_MEASURE, QualityMeasure.GINI_INDEX.getValue()));
        }

        @Override
        public void save(final QualityMeasure param, final NodeSettingsWO settings) {
            settings.addString(DecisionTreeLearnerNodeModel2.KEY_SPLIT_QUALITY_MEASURE, param.getValue());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{DecisionTreeLearnerNodeModel2.KEY_SPLIT_QUALITY_MEASURE}};
        }

    }

    static final class PruningMethodPersistor implements NodeParametersPersistor<PruningMethod> {

        @Override
        public PruningMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return PruningMethod.getFromValue(settings.getString(DecisionTreeLearnerNodeModel2.KEY_PRUNING_METHOD,
                PruningMethod.NO_PRUNING.getValue()));
        }

        @Override
        public void save(final PruningMethod param, final NodeSettingsWO settings) {
            settings.addString(DecisionTreeLearnerNodeModel2.KEY_PRUNING_METHOD, param.getValue());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{DecisionTreeLearnerNodeModel2.KEY_PRUNING_METHOD}};
        }

    }

    static final class NoTrueChildStrategyPersistor implements NodeParametersPersistor<PMMLNoTrueChildStrategy> {

        @Override
        public PMMLNoTrueChildStrategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return PMMLNoTrueChildStrategy
                .getFromString(settings.getString(DecisionTreeLearnerNodeModel2.KEY_NOTRUECHILD,
                    PMMLNoTrueChildStrategy.RETURN_NULL_PREDICTION.toString()));
        }

        @Override
        public void save(final PMMLNoTrueChildStrategy param, final NodeSettingsWO settings) {
            settings.addString(DecisionTreeLearnerNodeModel2.KEY_NOTRUECHILD, param.toString());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{DecisionTreeLearnerNodeModel2.KEY_NOTRUECHILD}};
        }

    }

    static final class MissingValueStrategyPersistor implements NodeParametersPersistor<PMMLMissingValueStrategy> {

        @Override
        public PMMLMissingValueStrategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var strategy = settings.getString(DecisionTreeLearnerNodeModel2.KEY_MISSINGSTRATEGY,
                PMMLMissingValueStrategy.LAST_PREDICTION.toString());
            if (strategy.equals(PMMLMissingValueStrategy.DEFAULT_CHILD.toString())) {
                // bug 4780 for backward compatibility the missing value strategy is set to the
                // default last prediction.
                return PMMLMissingValueStrategy.LAST_PREDICTION;
            }
            return PMMLMissingValueStrategy
                .getFromString(settings.getString(DecisionTreeLearnerNodeModel2.KEY_MISSINGSTRATEGY,
                    PMMLMissingValueStrategy.LAST_PREDICTION.toString()));
        }

        @Override
        public void save(final PMMLMissingValueStrategy param, final NodeSettingsWO settings) {
            settings.addString(DecisionTreeLearnerNodeModel2.KEY_MISSINGSTRATEGY, param.toString());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{DecisionTreeLearnerNodeModel2.KEY_MISSINGSTRATEGY}};
        }

    }

    enum QualityMeasure {

            @Label(value = "Gini index", description = """
                    The Gini index is a measure of inequality that quantifies how unevenly a resource
                    (such as income or wealth) is distributed within a population, ranging from 0 (perfect equality)
                    to 1 (maximum inequality).
                    """)
            GINI_INDEX(DecisionTreeLearnerNodeModel2.SPLIT_QUALITY_GINI), //
            @Label(value = "Gain ratio", description = """
                    The Gini ratio is simply another name for the Gini index, expressing the same inequality measure
                    as a ratio derived from the area between the Lorenz curve and the line of perfect equality.
                    """)
            GAIN_RATIO(DecisionTreeLearnerNodeModel2.SPLIT_QUALITY_GAIN_RATIO);

        private String m_value;

        QualityMeasure(final String value) {
            m_value = value;
        }

        String getValue() {
            return m_value;
        }

        static QualityMeasure getFromValue(final String value) throws InvalidSettingsException {
            for (final QualityMeasure measure : values()) {
                if (measure.getValue().equals(value)) {
                    return measure;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final String name) {
            var values =
                Arrays.stream(QualityMeasure.values()).map(QualityMeasure::getValue).collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", name, values);
        }

    }

    enum PruningMethod {

            @Label(value = "No pruning", description = "No pruning is applied")
            NO_PRUNING(DecisionTreeLearnerNodeModel2.PRUNING_NO), //
            @Label(value = "MDL", description = "Minimal Description Length pruning")
            MDL(DecisionTreeLearnerNodeModel2.PRUNING_MDL);

        private String m_value;

        PruningMethod(final String value) {
            m_value = value;
        }

        String getValue() {
            return m_value;
        }

        static PruningMethod getFromValue(final String value) throws InvalidSettingsException {
            for (final PruningMethod method : values()) {
                if (method.getValue().equals(value)) {
                    return method;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final String name) {
            var values =
                Arrays.stream(PruningMethod.values()).map(PruningMethod::getValue).collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", name, values);
        }

    }

}
