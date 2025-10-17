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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.base.util.SortingStrategy;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
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
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.Message;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Scorer.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class AccuracyScorer2NodeParameters implements NodeParameters {

    private static final Class<? extends DataValue>[] COMPATIBLE_DATATYPES =
        new Class[]{LongValue.class, StringValue.class, NominalValue.class};

    AccuracyScorer2NodeParameters() {
    }

    AccuracyScorer2NodeParameters(final NodeParametersInput context) {
        final var compatibleCols = ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(context, COMPATIBLE_DATATYPES);
        if (!compatibleCols.isEmpty()) {
            m_firstColumn = compatibleCols.get(compatibleCols.size() - 1).getName();
            if (compatibleCols.size() > 1) {
                m_secondColumn = compatibleCols.get(compatibleCols.size() - 2).getName();
            }
        }
    }

    @ValueReference(FirstColumnRef.class)
    @ValueProvider(FirstColumnProvider.class)
    @Persist(configKey = AbstractAccuracyScorerNodeModel.FIRST_COMP_ID)
    @Widget(title = "First column", description = "The first column represents the real classes of the data.")
    @ChoicesProvider(ColumnSelectionChoicesProvider.class)
    String m_firstColumn;

    @ValueReference(SecondColumnRef.class)
    @ValueProvider(SecondColumnProvider.class)
    @Persist(configKey = AbstractAccuracyScorerNodeModel.SECOND_COMP_ID)
    @Widget(title = "Second column", description = "The second column represents the predicted classes of the data.")
    @ChoicesProvider(ColumnSelectionChoicesProvider.class)
    String m_secondColumn;

    @TextMessage(SameColumnWarning.class)
    Void m_sameColumnWarning;

    @Widget(title = "Sorting strategy",
        description = "Whether to sort the labels according to their appearance, or use the lexical/numeric ordering.")
    @Persistor(SortingStrategyPersistor.class)
    @ChoicesProvider(ScorerSortingStrategyChoicesProvider.class)
    SortingStrategy m_sortingStrategy = SortingStrategy.InsertionOrder;

    @Widget(title = "Reverse order", description = "Reverse the order of the elements.")
    @Persist(configKey = AbstractAccuracyScorerNodeModel.SORTING_REVERSED)
    boolean m_reverseOrder;

    @Persistor(UsePrefixPersistor.class)
    UsePrefixParameters m_usePrefixParameters = new UsePrefixParameters();

    static final class UsePrefixParameters implements NodeParameters {

        @Widget(title = "Use name prefix",
            description = "The scores (i.e. accuracy, error rate, number of correct and wrong classification) are "
                + "exported as flow variables with a hard coded name. This option allows you to define a prefix for "
                + "these variable identifiers so that name conflicts are resolved.")
        @ValueReference(UsePrefixRef.class)
        boolean m_usePrefix;

        @Widget(title = "Prefix for flow variables", description = "The prefix to use for flow variable names.")
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

    static final class FirstColumnRef implements ParameterReference<String> {
    }

    static final class SecondColumnRef implements ParameterReference<String> {
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
            super(Arrays.asList(COMPATIBLE_DATATYPES));
        }

    }

    static abstract class BaseNthFromEndColumnProvider implements StateProvider<String> {

        private final Class<? extends ParameterReference<String>> m_refClass;

        /** 0 = last, 1 = second last, etc. */
        private final int m_fromEndIndex;

        private Supplier<String> m_valueSupplier;

        protected BaseNthFromEndColumnProvider(final Class<? extends ParameterReference<String>> refClass,
            final int fromEndIndex) {
            m_refClass = refClass;
            m_fromEndIndex = Math.max(0, fromEndIndex);
        }

        @Override
        public final void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_valueSupplier = initializer.getValueSupplier(m_refClass);
        }

        @Override
        public final String computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var currentValue = m_valueSupplier.get();
            if (currentValue != null) {
                return currentValue;
            }
            final List<String> compatibleColNames =
                ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, COMPATIBLE_DATATYPES).stream()
                    .map(DataColumnSpec::getName).toList();
            if (compatibleColNames.isEmpty()) {
                return null;
            }
            final int idx = Math.max(compatibleColNames.size() - 1 - m_fromEndIndex, 0);
            return compatibleColNames.get(idx);
        }
    }

    static final class FirstColumnProvider extends BaseNthFromEndColumnProvider {
        FirstColumnProvider() {
            super(FirstColumnRef.class, 0);
        }
    }

    static final class SecondColumnProvider extends BaseNthFromEndColumnProvider {
        SecondColumnProvider() {
            super(SecondColumnRef.class, 1);
        }
    }

    static final class SameColumnWarning implements StateProvider<Optional<TextMessage.Message>> {

        Supplier<String> m_firstColumnSupplier;

        Supplier<String> m_secondColumnSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_firstColumnSupplier = initializer.computeFromValueSupplier(FirstColumnRef.class);
            m_secondColumnSupplier = initializer.computeFromValueSupplier(SecondColumnRef.class);
        }

        @Override
        public Optional<Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var firstCol = m_firstColumnSupplier.get();
            final var secondCol = m_secondColumnSupplier.get();

            if (firstCol != null && firstCol.equals(secondCol)) {
                final var numberOfCompatibleCols =
                    ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, COMPATIBLE_DATATYPES).size();
                return numberOfCompatibleCols > 1
                    ? Optional.of(new TextMessage.Message("Same column selected.",
                        "The confusion matrix requires two distinct columns.", TextMessage.MessageType.WARNING))
                    : Optional.empty();
            } else {
                return Optional.empty();
            }
        }

    }

    static final class ScorerSortingStrategyChoicesProvider implements EnumChoicesProvider<SortingStrategy> {

        Supplier<String> m_firstColumnSupplier;

        Supplier<String> m_secondColumnSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_firstColumnSupplier = initializer.computeFromValueSupplier(FirstColumnRef.class);
            m_secondColumnSupplier = initializer.computeFromValueSupplier(SecondColumnRef.class);
        }

        @Override
        public List<SortingStrategy> choices(final NodeParametersInput context) {
            final var inSpec = context.getInTableSpec(0).orElse(null);
            if (inSpec == null) {
                return List.of(SortingStrategy.InsertionOrder);
            }

            final var firstCol = m_firstColumnSupplier.get();
            final var secondCol = m_secondColumnSupplier.get();
            if (isEmpty(firstCol) || !inSpec.containsName(firstCol) || isEmpty(secondCol)
                || !inSpec.containsName(secondCol)) {
                return List.of(SortingStrategy.InsertionOrder);
            }

            final var firstColumnSpec = inSpec.getColumnSpec(firstCol);
            final var secondColumnSpec = inSpec.getColumnSpec(secondCol);

            if (firstColumnSpec.getType().isCompatible(DoubleValue.class)
                && secondColumnSpec.getType().isCompatible(DoubleValue.class)) {
                return List.of(SortingStrategy.InsertionOrder, SortingStrategy.Numeric, SortingStrategy.Lexical);
            } else if (firstColumnSpec.getType().isCompatible(StringValue.class)
                && secondColumnSpec.getType().isCompatible(StringValue.class)) {
                return List.of(SortingStrategy.InsertionOrder, SortingStrategy.Lexical);
            } else {
                return List.of(SortingStrategy.InsertionOrder);
            }

        }

        static boolean isEmpty(final String value) {
            return value == null || value.isEmpty();
        }

    }

    static final class SortingStrategyPersistor implements NodeParametersPersistor<SortingStrategy> {
        @Override
        public SortingStrategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            int ordinal = settings.getInt(AbstractAccuracyScorerNodeModel.SORTING_STRATEGY,
                SortingStrategy.InsertionOrder.ordinal());
            SortingStrategy[] values = SortingStrategy.values();
            if (ordinal < 0 || ordinal >= values.length) {
                throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(ordinal));
            }
            return values[ordinal];
        }

        @Override
        public void save(final SortingStrategy obj, final NodeSettingsWO settings) {
            settings.addInt(AbstractAccuracyScorerNodeModel.SORTING_STRATEGY, obj.ordinal());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{AbstractAccuracyScorerNodeModel.SORTING_STRATEGY}};
        }

        private static String createInvalidSettingsExceptionMessage(final Integer name) {
            var values = Map
                .of(SortingStrategy.InsertionOrder.toString(), 0, SortingStrategy.Numeric.toString(), 1,
                    SortingStrategy.Lexical.toString(), 2, SortingStrategy.Unsorted.toString(), 3)
                .entrySet().stream().map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
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
            super(AbstractAccuracyScorerNodeModel.ACTION_ON_MISSING_VALUES, MissingValueHandling.class,
                MissingValueHandling.IGNORE);
        }

    }

    enum MissingValueHandling {
            @Label(value = "Ignore", description = "Ignore missing values (treat them as if the row did not exist).")
            IGNORE,

            @Label(value = "Fail", description = "Fail execution if missing values are encountered.")
            FAIL
    }
}
