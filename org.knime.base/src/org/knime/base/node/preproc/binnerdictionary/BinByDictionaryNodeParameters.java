/*
 * ------------------------------------------------------------------------
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
 *
 */

package org.knime.base.node.preproc.binnerdictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
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
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * Node parameters for Binner (Dictionary).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class BinByDictionaryNodeParameters implements NodeParameters {

    static final Class<? extends DataValue>[] TYPE_CANDIDATES = new Class[] {DateAndTimeValue.class, IntValue.class,
        DoubleValue.class, LongValue.class, BoundedValue.class, DataValue.class};

    interface BeforeBoundChoices {}

    @After(BeforeBoundChoices.class)
    interface BoundChoiceAndLowerColumn{}

    @After(BoundChoiceAndLowerColumn.class)
    interface LowerIncluded{}

    @After(LowerIncluded.class)
    interface UpperColumn{}

    @After(UpperColumn.class)
    interface UpperIncluded{}

    @After(UpperIncluded.class)
    interface AfterBoundChoices{}

    @Layout(BeforeBoundChoices.class)
    @Persist(configKey = BinByDictionaryConfiguration.CFG_VALUE_COLUMN_PORT0)
    @Widget(title = "Value column to bin (1st port)",
        description = "Select the column in the first input table containing the values to be categorized.")
    @ChoicesProvider(AllColumnsProvider.class)
    @ValueReference(ValueColumnPort0Ref.class)
    @ValueProvider(ValueColumnPort0Provider.class)
    String m_valueColumnPort0 = "";

    @Persistor(BoundChoicePersistor.class)
    BoundChoiceParameters boundChoices = new BoundChoiceParameters();

    static final class BoundChoiceParameters implements NodeParameters {

        @Layout(BoundChoiceAndLowerColumn.class)
        @Widget(title = "Active bounds", description = """
            Choose what bounds to enable:
            """)
        @ValueReference(BoundChoiceRef.class)
        @ValueSwitchWidget
        BoundChoice m_boundChoice = BoundChoice.BOTH_BOUNDS;

        @Layout(BoundChoiceAndLowerColumn.class)
        @Widget(title = "Lower bound column (2nd port)",
        description = "Select the column containing the lower bound values.")
        @ChoicesProvider(Port1DataValueColumnsProvider.class)
        @ValueReference(LowerBoundColumnPort1Ref.class)
        @ValueProvider(LowerBoundColumnPort1Provider.class)
        @Effect(predicate = LowerBoundEnabledPredicate.class, type = EffectType.SHOW)
        String m_lowerColumn = "";

        @Layout(UpperColumn.class)
        @Widget(title = "Upper bound column (2nd port)",
        description = "Select the column containing the upper bound values.")
        @ChoicesProvider(Port1DataValueColumnsProvider.class)
        @ValueReference(UpperBoundColumnPort1Ref.class)
        @ValueProvider(UpperBoundColumnPort1Provider.class)
        @Effect(predicate = UpperBoundEnabledPredicate.class, type = EffectType.SHOW)
        String m_upperColumn = "";

        BoundChoiceParameters() {
            this(BoundChoice.BOTH_BOUNDS, "", "");
        }

        BoundChoiceParameters(final BoundChoice boundChoice, final String lowerColumn, final String upperColumn) {
            m_boundChoice = boundChoice;
            m_lowerColumn = lowerColumn;
            m_upperColumn = upperColumn;
        }

    }

    @Persist(configKey = BinByDictionaryConfiguration.CFG_LOWER_BOUND_INCLUSIVE)
    @Layout(LowerIncluded.class)
    @Widget(title = "Lower bound inclusive", description = """
                Choose whether a value must be strictly smaller or smaller or equal than the lower bound value by
                selecting the "Inclusive" checkbox (if checked it will be smaller or equal).
                """)
    @Effect(predicate = LowerBoundEnabledPredicate.class, type = EffectType.SHOW)
    boolean m_lowerBoundInclusive;

    @Persist(configKey = BinByDictionaryConfiguration.CFG_UPPER_BOUND_INCLUSIVE)
    @Layout(UpperIncluded.class)
    @Widget(title = "Upper bound inclusive", description = """
            Choose whether a value must be strictly larger or larger or equal than the upper bound value by selecting
            the "Inclusive" checkbox (if checked it will be larger or equal).
            """)
    @Effect(predicate = UpperBoundEnabledPredicate.class, type = EffectType.SHOW)
    boolean m_upperBoundInclusive = true;

    @Layout(AfterBoundChoices.class)
    @Persist(configKey = BinByDictionaryConfiguration.CFG_LABEL_COLUMN_PORT1)
    @Widget(title = "Label column (2nd port)", description = """
            Select the label column from the 2nd input that will be appended as category column to the output table.
            """)
    @ChoicesProvider(LabelColumnPort1ChoicesProvider.class)
    @ValueReference(LabelColumnPort1Ref.class)
    @ValueProvider(LabelColumnPort1Provider.class)
    String m_labelColumnPort1 = "";

    @Layout(AfterBoundChoices.class)
    @Persistor(NoRuleMatchBehaviorPersistor.class)
    @Widget(title = "If no rule matches", description = """
            Choose the behavior if none of the rules in the 2nd input fires for a given value:
            """)
    @ValueSwitchWidget
    NoRuleMatchBehavior m_noRuleMatchBehavior = NoRuleMatchBehavior.INSERT_MISSING;

    @Layout(AfterBoundChoices.class)
    @Persistor(SearchPatternPersistor.class)
    @Widget(title = "Search pattern", description = """
            Linear Search scans all rules sequentially in the order they are defined in the rule table and returns the
            label of the first rule that matches. Binary search only works if both limits are specified (and not
            missing); it sorts the rules based on their lower and upper limits and performs a binary search to find the
            matching label. Binary search might not be deterministic if rules overlap. It is much faster if there is a
            large rule set (tens of thousands or millions). If in doubt, use linear search.
            """)
    @ValueSwitchWidget
    @ValueReference(SearchPatternRef.class)
    @ValueProvider(SearchPatternProvider.class)
    @Effect(predicate = IsEitherUpperOrLowerBoundEnabled.class, type = EffectType.DISABLE)
    SearchPattern m_searchPattern = SearchPattern.LINEAR;

    static final class BoundChoiceRef implements ParameterReference<BoundChoice> {
    }

    static final class ValueColumnPort0Ref implements ParameterReference<String> {
    }

    static final class LowerBoundColumnPort1Ref implements ParameterReference<String> {
    }

    static final class UpperBoundColumnPort1Ref implements ParameterReference<String> {
    }

    static final class LabelColumnPort1Ref implements ParameterReference<String> {
    }

    static final class SearchPatternRef implements ParameterReference<SearchPattern> {
    }

    static final class LowerBoundEnabledPredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(BoundChoiceRef.class).isOneOf(BoundChoice.LOWER_BOUND, BoundChoice.BOTH_BOUNDS);
        }

    }

    static final class UpperBoundEnabledPredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(BoundChoiceRef.class).isOneOf(BoundChoice.UPPER_BOUND, BoundChoice.BOTH_BOUNDS);
        }

    }

    static final class IsEitherUpperOrLowerBoundEnabled implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return not(i.getPredicate(LowerBoundEnabledPredicate.class))
                .or(not(i.getPredicate(UpperBoundEnabledPredicate.class)));
        }

    }

    static final class Port1DataValueColumnsProvider extends AllColumnsProvider {

        @Override
        public int getInputTableIndex() {
            return 1;
        }

    }

    static final class LabelColumnPort1ChoicesProvider extends CompatibleColumnsProvider {

        protected LabelColumnPort1ChoicesProvider() {
            super(NominalValue.class);
        }

        @Override
        public int getInputTableIndex() {
            return 1;
        }

    }

    static final class ValueColumnPort0Provider extends ColumnNameAutoGuessValueProvider {

        protected ValueColumnPort0Provider() {
            super(ValueColumnPort0Ref.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return getCompatibleColumnOfFirstPort(parametersInput);
        }

        private static final Optional<DataColumnSpec> getCompatibleColumnOfFirstPort(
            final NodeParametersInput parametersInput) {
            final var inSpec0Opt = parametersInput.getInTableSpec(0);
            if (inSpec0Opt.isEmpty()) {
                return Optional.empty();
            }
            final var insSpec0 = inSpec0Opt.get();

            for (Class<? extends DataValue> valueClass : TYPE_CANDIDATES) {
                for (int i = 0; i < insSpec0.getNumColumns(); i++) {
                    DataColumnSpec c = insSpec0.getColumnSpec(i);
                    if (c.getType().isCompatible(valueClass)) {
                        return Optional.of(c);
                    }
                }
            }

            return Optional.empty();
        }

    }

    static final class LowerBoundColumnPort1Provider extends ColumnNameAutoGuessValueProvider {

        protected LowerBoundColumnPort1Provider() {
            super(LowerBoundColumnPort1Ref.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var candidateCols = getCompatibleColumnsOfSecondPort(parametersInput);

            if (candidateCols.isEmpty()) {
                return Optional.empty();
            } else if (candidateCols.size() == 1) {
                return Optional.of(candidateCols.get(0));
            } else {
                return Optional.of(candidateCols.get(1));
            }
        }

    }

    static final class UpperBoundColumnPort1Provider extends ColumnNameAutoGuessValueProvider {

        protected UpperBoundColumnPort1Provider() {
            super(UpperBoundColumnPort1Ref.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var candidateCols = getCompatibleColumnsOfSecondPort(parametersInput);
            return candidateCols.isEmpty() ? Optional.empty() : Optional.of(candidateCols.get(0));
        }

    }

    private static final List<DataColumnSpec> getCompatibleColumnsOfSecondPort(
        final NodeParametersInput parametersInput) {
        final var inSpec1Opt = parametersInput.getInTableSpec(1);
        if (inSpec1Opt.isEmpty()) {
            return List.of();
        }
        final var insSpec1 = inSpec1Opt.get();

        List<DataColumnSpec> candidateCols = new ArrayList<>();
        for (Class<? extends DataValue> valueClass : TYPE_CANDIDATES) {
            for (int i = insSpec1.getNumColumns(); --i >= 0;) {
                DataColumnSpec c = insSpec1.getColumnSpec(i);
                if (c.getType().isCompatible(valueClass)) {
                    if (candidateCols.isEmpty()) {
                        candidateCols.add(c);
                    } else if (!c.equals(candidateCols.get(0))) {
                        candidateCols.add(c);
                        // Stop since we found two candidates
                        break;
                    }
                }
            }
        }

        return candidateCols;
    }

    static final class LabelColumnPort1Provider extends ColumnNameAutoGuessValueProvider {

        protected LabelColumnPort1Provider() {
            super(LabelColumnPort1Ref.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var firstColumnOfFirstPort = ColumnSelectionUtil.getFirstColumn(parametersInput, 1);
            final var inputColumns = ColumnSelectionUtil.getCompatibleColumns(parametersInput, 1, NominalValue.class);

            return inputColumns.isEmpty() ?
                firstColumnOfFirstPort : Optional.of(inputColumns.get(inputColumns.size() - 1));
        }

    }

    static final class SearchPatternProvider implements StateProvider<SearchPattern> {

        Supplier<BoundChoice> m_boundChoiceSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_boundChoiceSupplier = initializer.computeFromValueSupplier(BoundChoiceRef.class);
        }

        @Override
        public SearchPattern computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_boundChoiceSupplier.get() != BoundChoice.BOTH_BOUNDS) {
                return SearchPattern.LINEAR;
            }
            throw new StateComputationFailureException();
        }

    }

    static final class BoundChoicePersistor implements NodeParametersPersistor<BoundChoiceParameters> {

        @Override
        public BoundChoiceParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var lowerBound = settings.getString(BinByDictionaryConfiguration.CFG_LOWER_BOUND_COLUMN_PORT1, null);
            final var upperBound = settings.getString(BinByDictionaryConfiguration.CFG_UPPER_BOUND_COLUMN_PORT1, null);

            BoundChoice boundChoice;
            if (lowerBound == null) {
                boundChoice = BoundChoice.UPPER_BOUND;
            } else if (upperBound == null) {
                boundChoice = BoundChoice.LOWER_BOUND;
            } else {
                boundChoice = BoundChoice.BOTH_BOUNDS;
            }

            return new BoundChoiceParameters(boundChoice, lowerBound, upperBound);
        }

        @Override
        public void save(final BoundChoiceParameters param, final NodeSettingsWO settings) {
            final var boundChoice = param.m_boundChoice;
            switch (boundChoice) {
                case LOWER_BOUND:
                    settings.addString(BinByDictionaryConfiguration.CFG_LOWER_BOUND_COLUMN_PORT1, param.m_lowerColumn);
                    settings.addString(BinByDictionaryConfiguration.CFG_UPPER_BOUND_COLUMN_PORT1, null);
                    break;
                case UPPER_BOUND:
                    settings.addString(BinByDictionaryConfiguration.CFG_LOWER_BOUND_COLUMN_PORT1, null);
                    settings.addString(BinByDictionaryConfiguration.CFG_UPPER_BOUND_COLUMN_PORT1, param.m_upperColumn);
                    break;
                case BOTH_BOUNDS:
                    settings.addString(BinByDictionaryConfiguration.CFG_LOWER_BOUND_COLUMN_PORT1, param.m_lowerColumn);
                    settings.addString(BinByDictionaryConfiguration.CFG_UPPER_BOUND_COLUMN_PORT1, param.m_upperColumn);
                    break;
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{BinByDictionaryConfiguration.CFG_LOWER_BOUND_COLUMN_PORT1},
                {BinByDictionaryConfiguration.CFG_UPPER_BOUND_COLUMN_PORT1}};
        }

    }

    static final class NoRuleMatchBehaviorPersistor extends EnumBooleanPersistor<NoRuleMatchBehavior> {

        public NoRuleMatchBehaviorPersistor() {
            super("failIfNoRuleMatches", NoRuleMatchBehavior.class, NoRuleMatchBehavior.FAIL);
        }

    }

    static final class SearchPatternPersistor extends EnumBooleanPersistor<SearchPattern> {

        public SearchPatternPersistor() {
            super(BinByDictionaryConfiguration.CFG_USE_BINARY_SEARCH, SearchPattern.class, SearchPattern.BINARY);
        }

    }

    enum BoundChoice {

        @Label(value = "Both", description = "Enable both bounds for binning.")
        BOTH_BOUNDS, //
        @Label(value = "Lower", description = "Enable lower bound for binning.")
        LOWER_BOUND, //
        @Label(value = "Upper", description = "Enable upper bound for binning.")
        UPPER_BOUND;

    }

    enum NoRuleMatchBehavior {

        @Label(value = "Insert Missing", description = """
                Insert missing values as result.
                """)
        INSERT_MISSING, //
        @Label(value = "Fail", description = """
                Make this node fail during execution (reasonable when the rule table is assumed to cover the entire
                domain)
                """)
        FAIL;

    }

    enum SearchPattern {

        LINEAR, //
        BINARY;

    }

}
