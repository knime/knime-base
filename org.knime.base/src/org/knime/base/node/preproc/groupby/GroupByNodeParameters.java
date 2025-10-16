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

package org.knime.base.node.preproc.groupby;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.node.preproc.groupby.GroupByNodeModel.TypeMatch;
import org.knime.base.node.util.regex.PatternType;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.FallbackDialogNodeParameters;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DefaultClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.DataTypeChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for GroupBy.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction") // internal API use
class GroupByNodeParameters implements NodeParameters {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GroupByNodeParameters.class);

    private interface Sections {
        @Section(title = "Group", description = "...")
        interface Group {
        }

        @Section(title = "Aggregation", description = "...")
        @After(Group.class)
        interface Aggregation {
        }

        @Section(title = "Type- and pattern-based aggregation", description = "...", sideDrawer = true,
            sideDrawerSetText = "Type/Pattern")
        @After(Aggregation.class)
        @Advanced
        interface TypeAndPatternAggregations {
        }

        @Section(title = "Output", description = "...")
        @After(TypeAndPatternAggregations.class)
        interface Output {
        }

        @Section(title = "Performance", description = "...")
        @After(Output.class)
        @Advanced
        interface Performance {
        }
    }

    static final class GroupByColumnsModification extends LegacyStringFilter.LegacyStringFilterModification {
        GroupByColumnsModification() {
            super(false, "Group settings", "DESCRIPTION", "Available column(s)", "Group column(s)",
                AllColumnsProvider.class);
        }
    }

    @Layout(Sections.Group.class)
    @Persist(configKey = GroupByNodeModel.CFG_GROUP_BY_COLUMNS)
    @Modification(GroupByColumnsModification.class)
    LegacyStringFilter m_groupByColumns = new LegacyStringFilter(new String[0], new String[0]);

    @Layout(Sections.Aggregation.class)
    @Widget(title = "Manual", description = "...")
    @ArrayWidget(addButtonText = "Add manual")
    @Persistor(LegacyColumnAggregatorsPersistor.class)
    ColumnAggregatorElement[] m_columnAggregators = new ColumnAggregatorElement[0];

    enum MissingValueOption {
            @Label("Exclude")
            EXCLUDE, @Label("Include")
            INCLUDE
    }

    /**
     * Common interface for legacy fallback parameters and new extension point-based parameters
     * for aggregation operators that have custom settings.
     */
    interface AggregationOperatorParameters extends DynamicParameters.DynamicNodeParameters {
    }

    /**
     * Parameters to display legacy operator settings in "fallback style".
     */
    static final class LegacyAggregationOperatorParameters extends FallbackDialogNodeParameters
        implements AggregationOperatorParameters {
        public LegacyAggregationOperatorParameters(final NodeSettingsRO nodeSettings) {
            super(nodeSettings);
        }
    }

    /**
     * Placeholder for new extension point-based parameters, such that we can upgrade often-used operator
     * settings to proper node parameters with a default node dialog.
     */
    // TODO define via extension point
    static final class ViaExtensionPointAggregationOperatorParameters implements AggregationOperatorParameters {
        @Widget(title = "New param 1", description = "Coming from extension point.")
        String m_newParam1 = "placeholder";
    }

    static final class NoOperatorParameters implements AggregationOperatorParameters {
        // empty, no settings
    }

    static final class AggregationoperatorParametersRef
        implements ParameterReference<AggregationOperatorParameters> {
    }


    static final class AggregationOperatorParametersProvider
            implements DynamicParameters.DynamicParametersWithFallbackProvider<AggregationOperatorParameters> {

        private Supplier<AggregationOperatorParameters> m_currentValueSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_currentValueSupplier = initializer.getValueSupplier(AggregationoperatorParametersRef.class);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public ClassIdStrategy getClassIdStrategy() {
            // TODO from extension point
            return new DefaultClassIdStrategy<>(List.of(NoOperatorParameters.class,
                LegacyAggregationOperatorParameters.class, ViaExtensionPointAggregationOperatorParameters.class));
        }

        @Override
        public AggregationOperatorParameters computeParameters(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var currentValue = m_currentValueSupplier.get();
            if (currentValue instanceof LegacyAggregationOperatorParameters fallbackParams) {
//                final var nodeSettings = fallbackParams.getNodeSettings();
//                try {
//                    return NodeParametersUtil.loadSettings(nodeSettings, LegacyAggregationOperatorParameters.class);
//                } catch (InvalidSettingsException ex) {
//                    return new MyDynamicParametersNodeParameters();
//                }
                LOGGER.debug("instanceof fallback params");
            } else if (currentValue != null) {
                return currentValue;
            }
            return new ViaExtensionPointAggregationOperatorParameters();
        }

        @Override
        public NodeSettings computeFallbackSettings(final NodeParametersInput parametersInput)
                throws StateComputationFailureException {
            // TODO only if no new params defined in ext point (then return null)
            final var currentValue = m_currentValueSupplier.get();
            if (currentValue instanceof LegacyAggregationOperatorParameters legacy) {
                return legacy.getNodeSettings();
            }
            final var settings = new NodeSettings("settings");
            NodeParametersUtil.saveSettings(currentValue.getClass(), currentValue, settings);
            return settings;
        }

        @Override
        public FallbackDialogNodeParameters getParametersFromFallback(final NodeSettingsRO fallbackSettings) {
            return new LegacyAggregationOperatorParameters(fallbackSettings);
        }

    }

    static class AggregationColumnsProvider extends AllColumnsProvider {
        // TODO should we remove group columns?
    }

    static class ColumnAggregatorElement implements NodeParameters {
        @Widget(title = "Column", description = "The column to aggregate")
        @ChoicesProvider(AggregationColumnsProvider.class)
        String m_column;

        @Widget(title = "Aggregation", description = "The aggregation method to use")
        String m_aggregationMethod;

        @Widget(title = "Missing values", description = "")
        @ValueSwitchWidget
        MissingValueOption m_includeMissing = MissingValueOption.EXCLUDE;

        // TODO show new parameters via extension point if defined
        @DynamicParameters(value = AggregationOperatorParametersProvider.class,
            widgetAppearingInNodeDescription = @Widget(title = "Operator settings", description = "...",
                advanced = true))
        @ValueReference(AggregationoperatorParametersRef.class)
        AggregationOperatorParameters m_parameters = new NoOperatorParameters();
    }

    @Layout(Sections.TypeAndPatternAggregations.class)
    @Widget(title = "Pattern", description = "...")
    @ArrayWidget(addButtonText = "Add pattern")
    @Persistor(LegacyPatternAggregatorsPersistor.class)
    PatternAggregatorElement[] m_patternAggregators = new PatternAggregatorElement[0];

    static class PatternAggregatorElement implements NodeParameters {
        @Widget(title = "Search pattern", description = "The search pattern to match column names")
        String m_pattern;

        static final class PatternTypeChoices implements EnumChoicesProvider<PatternType> {
            @Override
            public List<PatternType> choices(final NodeParametersInput context) {
                return List.of(PatternType.WILDCARD, PatternType.REGEX);
            }
        }

        @Widget(title = "Pattern type", description = "...")
        @ValueSwitchWidget
        @ChoicesProvider(PatternTypeChoices.class)
        PatternType m_isRegex = PatternType.WILDCARD;

        @Widget(title = "Aggregation", description = "The aggregation method to use")
        String m_aggregationMethod;

        @Widget(title = "Missing values", description = "")
        @ValueSwitchWidget
        MissingValueOption m_includeMissing = MissingValueOption.EXCLUDE;

        @Widget(title = "Parameter", description = "...")
        String m_parameters = "";
    }

    static final class TypeAggregationsRef implements ParameterReference<DataTypeAggregatorElement[]> {
    }

    @Layout(Sections.TypeAndPatternAggregations.class)
    @Widget(title = "Type", description = "...")
    @ArrayWidget(addButtonText = "Add type")
    @Persistor(LegacyDataTypeAggregatorsPersistor.class)
    @ValueReference(TypeAggregationsRef.class)
    DataTypeAggregatorElement[] m_dataTypeAggregators = new DataTypeAggregatorElement[0];

    static final class RegisteredTypesChoicesProvider implements DataTypeChoicesProvider {
        @Override
        public List<DataType> choices(final NodeParametersInput context) {
            final var registered = DataTypeRegistry.getInstance().availableDataTypes();
            return registered instanceof List<DataType> list ? list : new ArrayList<>(registered);
        }
    }

    static class DataTypeAggregatorElement implements NodeParameters {
        @Widget(title = "Data Type", description = "The data type to aggregate")
        @ChoicesProvider(RegisteredTypesChoicesProvider.class)
        DataType m_dataType;

        @Widget(title = "Aggregation", description = "The aggregation method to use")
        String m_aggregationMethod;

        @Widget(title = "Missing values", description = "")
        @ValueSwitchWidget
        MissingValueOption m_includeMissing = MissingValueOption.EXCLUDE;

        @Widget(title = "Parameter", description = "...")
        String m_parameters = "";
    }

    static final class TypeMatchChoicesProvider implements EnumChoicesProvider<TypeMatch> {
        @Override
        public List<TypeMatch> choices(final NodeParametersInput context) {
            return List.of(TypeMatch.values());
        }
    }

    static final class HasTypeAggregations implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            // neither TrueCondition, nor ConstantPredicate is exported
            return i.getArray(TypeAggregationsRef.class).containsElementSatisfying(el -> el.always());
        }
    }

    @Layout(Sections.TypeAndPatternAggregations.class)
    @Effect(predicate = HasTypeAggregations.class, type = EffectType.SHOW)
    @Widget(title = "Type matching", description = "...", advanced = true)
    @ChoicesProvider(TypeMatchChoicesProvider.class)
    @ValueSwitchWidget
    @Persistor(LegacyTypeMatchPersistor.class)
    TypeMatch m_typeMatch = TypeMatch.STRICT;

    static final class ColumnNamePolicyChoicesProvider implements EnumChoicesProvider<ColumnNamePolicy> {
        @Override
        public List<ColumnNamePolicy> choices(final NodeParametersInput context) {
            return List.of(ColumnNamePolicy.values());
        }
    }

    @Layout(Sections.Output.class)
    @Widget(title = "Column naming", description = "...")
    @ChoicesProvider(ColumnNamePolicyChoicesProvider.class)
    @Persistor(LegacyColumnNamePolicyPersistor.class)
    ColumnNamePolicy m_columnNamePolicy = ColumnNamePolicy.getDefault();

    @Layout(Sections.Output.class)
    @Widget(title = "Maximum unique values per group", description = "...")
    @NumberInputWidget()
    @Persist(configKey = GroupByNodeModel.CFG_MAX_UNIQUE_VALUES)
    int m_maxUniqueValues = 100000;

    @Layout(Sections.Output.class)
    @Widget(title = "Value delimiter",
        description = "The value delimiter used by aggregation methods such as concatenate.")
    @TextInputWidget
    @Persist(configKey = GroupByNodeModel.CFG_VALUE_DELIMITER)
    String m_valueDelimiter = GlobalSettings.STANDARD_DELIMITER;

    @Layout(Sections.Performance.class)
    @Widget(title = "Enable hiliting", description = "...")
    @Persist(configKey = GroupByNodeModel.CFG_ENABLE_HILITE)
    boolean m_enableHiliting;

    static final class ProcessInMemoryRef implements ParameterReference<Boolean> {
        // empty class used for EffectPredicateProvider
    }

    @Layout(Sections.Performance.class)
    @Widget(title = "Process in memory", description = "...")
    @ValueReference(ProcessInMemoryRef.class)
    @Persist(configKey = GroupByNodeModel.CFG_IN_MEMORY)
    boolean m_processInMemory;

    static final class RetainOrderRef implements ParameterReference<Boolean> {
    }

    @Layout(Sections.Performance.class)
    @Widget(title = "Retain row order", description = "...")
    @Effect(predicate = ProcessInMemoryEffect.class, type = Effect.EffectType.DISABLE)
    @ValueProvider(ProcessInMemoryEffect.class)
    @ValueReference(RetainOrderRef.class)
    @Persist(configKey = GroupByNodeModel.CFG_RETAIN_ORDER)
    boolean m_retainOrder;

    /**
     * Effect class for when process in memory setting changes.
     */
    static final class ProcessInMemoryEffect implements EffectPredicateProvider, StateProvider<Boolean> {

        private Supplier<Boolean> m_processInMemoryChange;

        private Supplier<Boolean> m_currentRetain;

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(ProcessInMemoryRef.class).isTrue();
        }

        @Override
        public void init(final StateProviderInitializer init) {
            m_processInMemoryChange = init.computeFromValueSupplier(ProcessInMemoryRef.class);
            m_currentRetain = init.getValueSupplier(RetainOrderRef.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput input) {
            // in-memory implies retain order
            // otherwise, we keep its current value
            return m_processInMemoryChange.get() || m_currentRetain.get();
        }
    }
}