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

package org.knime.base.node.mine.scorer.accuracy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.base.util.SortingStrategy;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.Message;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * Node parameters for Scorer.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class AccuracyScorer2NodeParameters implements NodeParameters {

    AccuracyScorer2NodeParameters() {
    }

    AccuracyScorer2NodeParameters(final NodeParametersInput context) {
        final var compatibleCols = ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(context,
            StringValue.class, LongValue.class, NominalValue.class);
        m_columnSelection = new ColumnSelectionParameters();
        if (!compatibleCols.isEmpty()) {
            m_columnSelection.m_firstColumn = compatibleCols.get(compatibleCols.size() - 1).getName();
            if (compatibleCols.size() > 1) {
                m_columnSelection.m_secondColumn = compatibleCols.get(compatibleCols.size() - 2).getName();
            }
        }
    }

    @ValueReference(ColumnSelectionRef.class)
    @ValueProvider(ColumnSelectionProvider.class)
    @Persistor(ColumnSelectionPersistor.class)
    ColumnSelectionParameters m_columnSelection = new ColumnSelectionParameters();

    static final class ColumnSelectionParameters implements NodeParameters {

        ColumnSelectionParameters() {
        }

        ColumnSelectionParameters(final String firstColumn, final String secondColumn) {
            m_firstColumn = firstColumn;
            m_secondColumn = secondColumn;
        }

        @Widget(title = "First column",
        description = "The first column represents the real classes of the data.")
        @TextInputWidget(patternValidation = IsNotBlankValidation.class)
        @ChoicesProvider(ColumnSelectionChoicesProvider.class)
        String m_firstColumn;

        @Widget(title = "Second column",
        description = "The second column represents the predicted classes of the data.")
        @TextInputWidget(patternValidation = IsNotBlankValidation.class)
        @ChoicesProvider(ColumnSelectionChoicesProvider.class)
        String m_secondColumn;

        @TextMessage(SameColumnWarning.class)
        Void m_sameColumnWarning;

    }

    @Widget(title = "Sorting strategy",
        description = "Whether to sort the labels according to their appearance, or use the lexical/numeric ordering.")
    @RadioButtonsWidget
    @Persistor(SortingStrategyPersistor.class)
    @ChoicesProvider(ScorerSortingStrategyChoicesProvider.class)
    ScorerSortingStrategy m_sortingStrategy = ScorerSortingStrategy.INSERTION_ORDER;

    @Widget(title = "Reverse order",
        description = "Reverse the order of the elements.")
    @Persist(configKey = AbstractAccuracyScorerNodeModel.SORTING_REVERSED)
    boolean m_reverseOrder = false;

    @Persistor(UsePrefixPersistor.class)
    UsePrefixParameters m_usePrefixParameters = new UsePrefixParameters();

    static final class UsePrefixParameters implements NodeParameters {

        @Widget(title = "Use name prefix",
            description = "The scores (i.e. accuracy, error rate, number of correct and wrong classification) are "
                + "exported as flow variables with a hard coded name. This option allows you to define a prefix for "
                + "these variable identifiers so that name conflicts are resolved.")
        @ValueReference(UsePrefixRef.class)
        boolean m_usePrefix = false;

        @Widget(title = "Prefix for flow variables",
            description = "The prefix to use for flow variable names.")
        @TextInputWidget
        @Effect(predicate = IsUsePrefix.class, type = EffectType.SHOW)
        String m_flowVariablePrefix = "";

    }

    @Widget(title = "Missing values",
        description = "Choose how to treat missing values in either the reference or prediction column. "
            + "Default is to ignore them (treat them as if the row did not exist). Alternatively, you can "
            + "expect the table to not contain missing values in these two columns. If they do, the node "
            + "will fail during execution.")
    @ValueSwitchWidget
    @Persistor(MissingValueHandlingPersistor.class)
    MissingValueHandling m_missingValueHandling = MissingValueHandling.IGNORE;

    static final class ColumnSelectionRef implements ParameterReference<ColumnSelectionParameters> {
    }

    static final class UsePrefixRef implements ParameterReference<Boolean> {
    }

    static final class IsUsePrefix implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UsePrefixRef.class).isTrue();
        }

    }

    static final class ColumnSelectionChoicesProvider extends CompatibleColumnsProvider {

        protected ColumnSelectionChoicesProvider() {
            super(List.of(StringValue.class, LongValue.class, NominalValue.class));
        }

    }

    static final class ColumnSelectionProvider implements StateProvider<ColumnSelectionParameters> {

        Supplier<ColumnSelectionParameters> m_columnSelectionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_columnSelectionSupplier = initializer.getValueSupplier(ColumnSelectionRef.class);
        }

        @Override
        public ColumnSelectionParameters computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var columnSelection = m_columnSelectionSupplier.get();
            if (columnSelection.m_firstColumn != null && columnSelection.m_secondColumn != null) {
                return columnSelection;
            }

            final var compatibleColNames = ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput,
                StringValue.class, LongValue.class, NominalValue.class).stream().map(DataColumnSpec::getName).toList();

            if (compatibleColNames.isEmpty()) {
                return new ColumnSelectionParameters();
            }

            final var firstColumn = compatibleColNames.get(compatibleColNames.size() - 1);
            return new ColumnSelectionParameters(firstColumn, compatibleColNames.size() > 1 ?
                compatibleColNames.get(compatibleColNames.size() - 2) : firstColumn);
        }

    }

    static final class SameColumnWarning implements StateProvider<Optional<TextMessage.Message>> {

        Supplier<ColumnSelectionParameters> m_columnSelectionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_columnSelectionSupplier = initializer.computeFromValueSupplier(ColumnSelectionRef.class);
        }

        @Override
        public Optional<Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var columnSelection = m_columnSelectionSupplier.get();
            final var firstCol = columnSelection.m_firstColumn;
            final var secondCol = columnSelection.m_secondColumn;

            if (firstCol != null && firstCol.equals(secondCol)) {
                final var numberOfCompatibleCols = ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput,
                    StringValue.class, LongValue.class, NominalValue.class).size();
                return numberOfCompatibleCols > 1 ? Optional.of(new TextMessage.Message(
                    "Same column selected.",
                    "The confusion matrix requires two distinct columns.",
                    TextMessage.MessageType.WARNING)) : Optional.empty();
            } else {
                return Optional.empty();
            }
        }

    }

    static final class ScorerSortingStrategyChoicesProvider implements EnumChoicesProvider<ScorerSortingStrategy> {

        Supplier<ColumnSelectionParameters> m_columnSelectionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_columnSelectionSupplier = initializer.getValueSupplier(ColumnSelectionRef.class);
        }

        @Override
        public List<ScorerSortingStrategy> choices(final NodeParametersInput context) {
            final var inSpec = context.getInTableSpec(0).orElse(null);
            if (inSpec == null) {
                return List.of(ScorerSortingStrategy.INSERTION_ORDER);
            }

            final var columnSelection = m_columnSelectionSupplier.get();
            if (isEmpty(columnSelection.m_firstColumn) || isEmpty(columnSelection.m_secondColumn)) {
                return List.of(ScorerSortingStrategy.INSERTION_ORDER);
            }

            final var firstColumnSpec = inSpec.getColumnSpec(inSpec.findColumnIndex(columnSelection.m_firstColumn));
            final var secondColumnSpec = inSpec.getColumnSpec(inSpec.findColumnIndex(columnSelection.m_secondColumn));

            if (firstColumnSpec.getType().isCompatible(DoubleValue.class)
                && secondColumnSpec.getType().isCompatible(DoubleValue.class)) {
                return List.of(ScorerSortingStrategy.INSERTION_ORDER, ScorerSortingStrategy.NUMERIC,
                    ScorerSortingStrategy.LEXICAL);
            } else if (firstColumnSpec.getType().isCompatible(StringValue.class)
                && secondColumnSpec.getType().isCompatible(StringValue.class)) {
                return List.of(ScorerSortingStrategy.INSERTION_ORDER, ScorerSortingStrategy.LEXICAL);
            } else {
                return List.of(ScorerSortingStrategy.INSERTION_ORDER);
            }

        }

        static boolean isEmpty(final String value) {
            return value == null || value.isEmpty();
        }

    }

    static final class ColumnSelectionPersistor implements NodeParametersPersistor<ColumnSelectionParameters> {

        @Override
        public ColumnSelectionParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            ColumnSelectionParameters param = new ColumnSelectionParameters();
            param.m_firstColumn = settings.getString(AbstractAccuracyScorerNodeModel.FIRST_COMP_ID, null);
            param.m_secondColumn = settings.getString(AbstractAccuracyScorerNodeModel.SECOND_COMP_ID, null);
            return param;
        }

        @Override
        public void save(final ColumnSelectionParameters param, final NodeSettingsWO settings) {
            settings.addString(AbstractAccuracyScorerNodeModel.FIRST_COMP_ID, param.m_firstColumn);
            settings.addString(AbstractAccuracyScorerNodeModel.SECOND_COMP_ID, param.m_secondColumn);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{AbstractAccuracyScorerNodeModel.FIRST_COMP_ID},
                {AbstractAccuracyScorerNodeModel.SECOND_COMP_ID}};
        }

    }

    static final class SortingStrategyPersistor implements NodeParametersPersistor<ScorerSortingStrategy> {
        @Override
        public ScorerSortingStrategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final int strategyOrdinal = settings.getInt(AbstractAccuracyScorerNodeModel.SORTING_STRATEGY,
                SortingStrategy.InsertionOrder.ordinal());
            return switch (strategyOrdinal) {
                case 0 -> ScorerSortingStrategy.INSERTION_ORDER;
                case 1 -> ScorerSortingStrategy.NUMERIC;
                case 2 -> ScorerSortingStrategy.LEXICAL;
                case 3 -> ScorerSortingStrategy.UNSORTED;
                default -> throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(strategyOrdinal));
            };
        }

        @Override
        public void save(final ScorerSortingStrategy obj, final NodeSettingsWO settings) {
            final int ordinal = switch (obj) {
                case INSERTION_ORDER -> SortingStrategy.InsertionOrder.ordinal();
                case NUMERIC -> SortingStrategy.Numeric.ordinal();
                case LEXICAL -> SortingStrategy.Lexical.ordinal();
                case UNSORTED -> SortingStrategy.Unsorted.ordinal();
            };
            settings.addInt(AbstractAccuracyScorerNodeModel.SORTING_STRATEGY, ordinal);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{AbstractAccuracyScorerNodeModel.SORTING_STRATEGY}};
        }

        private static String createInvalidSettingsExceptionMessage(final Integer name) {
            var values = Map.of(SortingStrategy.InsertionOrder.toString(), 0, SortingStrategy.Numeric.toString(), 1,
                SortingStrategy.Lexical.toString(), 2, SortingStrategy.Unsorted.toString(), 3)
                    .entrySet().stream()
                    .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", name, values);

        }

    }

    static final class UsePrefixPersistor implements NodeParametersPersistor<UsePrefixParameters> {

        @Override
        public UsePrefixParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            UsePrefixParameters param = new UsePrefixParameters();
            final var prefix = settings.getString(AbstractAccuracyScorerNodeModel.FLOW_VAR_PREFIX, "");
            param.m_flowVariablePrefix = prefix;
            param.m_usePrefix = prefix != null;
            return param;
        }

        @Override
        public void save(final UsePrefixParameters param, final NodeSettingsWO settings) {
            if (param.m_usePrefix) {
                settings.addString(AbstractAccuracyScorerNodeModel.FLOW_VAR_PREFIX, param.m_flowVariablePrefix);
            } else {
                settings.addString(AbstractAccuracyScorerNodeModel.FLOW_VAR_PREFIX, null);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{AbstractAccuracyScorerNodeModel.FLOW_VAR_PREFIX}};
        }

    }

    static final class MissingValueHandlingPersistor extends EnumBooleanPersistor<MissingValueHandling> {

        protected MissingValueHandlingPersistor() {
            super(AbstractAccuracyScorerNodeModel.ACTION_ON_MISSING_VALUES,
                MissingValueHandling.class,
                MissingValueHandling.IGNORE);
        }

    }

    enum ScorerSortingStrategy {
        @Label(value = "Insertion order", description = "Keep the original order as they appear in the data.")
        INSERTION_ORDER,

        @Label(value = "Numeric", description = "Sort values numerically.")
        NUMERIC,

        @Label(value = "Lexical", description = "Sort values lexicographically.")
        LEXICAL,

        @Label(value = "Unsorted", description = "Do not modify the current order.")
        UNSORTED
    }

    enum MissingValueHandling {
        @Label(value = "Ignore", description = "Ignore missing values (treat them as if the row did not exist).")
        IGNORE,

        @Label(value = "Fail", description = "Fail execution if missing values are encountered.")
        FAIL
    }
}
