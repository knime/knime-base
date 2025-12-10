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

package org.knime.base.node.meta.looper.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.knime.base.node.meta.looper.condition.LoopEndConditionDynamicSettings.Operator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ArrayPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ElementFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArray;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArrayElement;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.booleanhelpers.DoNotPersistBoolean;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
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
import org.knime.node.parameters.updates.legacy.AutoGuessValueProvider;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;

/**
 * Node parameters for Variable Condition Loop End.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class LoopEndConditionDynamicNodeParameters implements NodeParameters {

    static final VariableType<?>[] COMPATIBLE_VARIABLE_TYPES = new VariableType<?>[]{VariableType.StringType.INSTANCE,
        VariableType.IntType.INSTANCE, VariableType.LongType.INSTANCE, VariableType.DoubleType.INSTANCE,
        VariableType.BooleanType.INSTANCE};

    @Widget(title = "Flow variables", description = """
            Select one of the available flow variables to check the condition against.
            The loop execution will be finished when the selected variable meets the specified condition.
            """)
    @ChoicesProvider(CompatibleFlowVariablesProvider.class)
    @ValueProvider(FlowVariableNameProvider.class)
    @ValueReference(FlowVariableNameRef.class)
    @Persist(configKey = LoopEndConditionDynamicSettings.CFG_VARIABLE_NAME)
    String m_flowVariableName;

    static final class FlowVariableNameRef implements ParameterReference<String> {
    }

    @Persist(configKey = LoopEndConditionDynamicSettings.CFG_VARIABLE_TYPE)
    @ValueProvider(FlowVariableTypeProvider.class)
    @ValueReference(FlowVariableTypeRef.class)
    String m_flowVariableType;

    static final class FlowVariableTypeRef implements ParameterReference<String> {
    }

    @Widget(title = "Confirm variable type change", description = """
            This option is automatically set when the flow variable type changes initially requiring the node to be
            reconfigured. Manual changes to this setting are ignored.
            """)
    @WidgetInternal(hideControlInNodeDescription = "This is a helper setting to make the dialog dirty.")
    @Persistor(DoNotPersistBoolean.class)
    @Effect(predicate = MakeDialogDirtyInitialValue.class, type = EffectType.SHOW)
    @ValueReference(MakeDialogDirty.class)
    @ValueProvider(MakeDialogDirtyProvider.class)
    boolean m_makeDialogDirty;

    static final class MakeDialogDirty implements BooleanReference {
    }

    @Widget(title = "Comparison operator", description = """
            Choose the comparison operator for the condition which finishes the loop's execution. For string and
            boolean variables, only equality and inequality checks are possible.
            """)
    @ChoicesProvider(OperatorChoicesProvider.class)
    @Persist(configKey = LoopEndConditionDynamicSettings.CFG_OPERATOR)
    @Migration(LoadEQWhenNull.class)
    Operator m_operator = Operator.EQ;

    static final class LoadEQWhenNull implements NodeParametersMigration<Operator> {

        @Override
        public List<ConfigMigration<Operator>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(settings -> Operator.EQ)
                .withMatcher(settings -> settings.getString(LoopEndConditionDynamicSettings.CFG_OPERATOR, null) == null)
                .build());
        }

    }

    @Widget(title = "Condition value", description = """
            Select the value to compare the chosen flow variable value against using the defined comparison operator.
            The loop will end when the comparison evaluates to true.
            """)
    @Persist(configKey = LoopEndConditionDynamicSettings.CFG_VALUE)
    @Migration(LoadEmptyStringWhenNull.class)
    String m_flowVariableValue;

    static final class FlowVariableValueRef implements ParameterReference<String> {
    }

    static final class LoadEmptyStringWhenNull implements NodeParametersMigration<String> {

        @Override
        public List<ConfigMigration<String>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(settings -> "")
                .withMatcher(settings -> settings.getString(LoopEndConditionDynamicSettings.CFG_VALUE, null) == null)
                .build());
        }

    }

    @Widget(title = "Propagate modified loop variables", description = """
            If checked, variables whose values are modified within the loop are exported by this node.
            These variables must be declared outside the loop, i.e. injected into the loop from a side-branch
            or be available upstream of the corresponding loop start node. For the latter, any modification of a
            variable is passed back to the start node in subsequent iterations (e.g. moving sum calculation).
            Note that variables defined by the loop start node itself are excluded as these usually represent
            loop controls (e.g. "currentIteration").
            """)
    @Persist(configKey = LoopEndConditionDynamicSettings.CFG_PROPAGATE_LOOP_VARIABLES)
    boolean m_propagateLoopVariables;

    @Widget(title = "Port settings", description = """
            Configure settings for each collector port.
            """)
    @ArrayWidget(elementTitle = "Port", hasFixedSize = true)
    @ValueProvider(PortSettingsArrayProvider.class)
    @ValueReference(PortSettingsArrayRef.class)
    @PersistArray(PortSettingsArrayPersistor.class)
    PortSettings[] m_portSettings = new PortSettings[0];

    static final class PortSettingsArrayRef implements ParameterReference<PortSettings[]> {
    }

    /**
     * The initial computed value for the change internal dialog state. The effect of m_changeInternalDialogState is
     * based on this setting to reduce the flickering when the user clicks the checkbox. Otherwise, the checkbox would
     * disappear and reappear on each click. Also, it is needed to detect whether the user clicked the checkbox or not
     * to check the checkbox again.
     */
    @Persistor(DoNotPersistBoolean.class)
    @ValueReference(MakeDialogDirtyInitialValue.class)
    @ValueProvider(MakeDialogDirtyInitialValueProvider.class)
    boolean m_makeDialogDirtyInitialValue;

    static final class MakeDialogDirtyInitialValue implements BooleanReference {
    }

    /**
     * Indicates whether the initialization of the "change internal dialog state" has been done. Needed to detect
     * whether the used clicked the "change internal dialog state" checkbox (as the value provider listens to
     * beforeOpenDialog and to a user change, but we cannot detect that in the provider).
     */
    @Persistor(DoNotPersistBoolean.class)
    @ValueProvider(InitializationDoneProvider.class)
    @ValueReference(InitializationDoneReference.class)
    boolean m_initializationDone;

    static final class InitializationDoneReference implements BooleanReference {
    }

    static final class CompatibleFlowVariablesProvider implements FlowVariableChoicesProvider {

        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            return context.getAvailableInputFlowVariables(COMPATIBLE_VARIABLE_TYPES).values().stream().toList();
        }

    }

    static final class FlowVariableNameProvider extends AutoGuessValueProvider<String> {

        protected FlowVariableNameProvider() {
            super(FlowVariableNameRef.class);
        }

        @Override
        protected boolean isEmpty(final String name) {
            return name == null || name.isEmpty();
        }

        @Override
        protected String autoGuessValue(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return parametersInput.getAvailableInputFlowVariables(COMPATIBLE_VARIABLE_TYPES).values().stream()
                    .findFirst().map(v -> v.getName()).orElse(null);
        }

    }

    static final class FlowVariableTypeProvider implements StateProvider<String> {

        Supplier<String> m_flowVariableNameSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_flowVariableNameSupplier = initializer.computeFromValueSupplier(FlowVariableNameRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            final var flowVarOpt = parametersInput.getAvailableInputFlowVariables(COMPATIBLE_VARIABLE_TYPES).values()
                    .stream().filter(v -> v.getName().equals(m_flowVariableNameSupplier.get()))
                    .findFirst();
            if (flowVarOpt.isEmpty()) {
                return null;
            } else {
                return flowVarOpt.get().getVariableType().getIdentifier();
            }
        }

    }

    static final class OperatorChoicesProvider implements EnumChoicesProvider<Operator> {

        private static final List<Operator> NUMERIC_OPERATORS =
            List.of(Operator.EQ, Operator.LE, Operator.LT, Operator.GE, Operator.GT, Operator.NE);

        private static final List<Operator> NON_NUMERIC_OPERATORS = List.of(Operator.EQ, Operator.NE);

        Supplier<String> m_flowVariableTypeSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_flowVariableTypeSupplier = initializer.computeFromValueSupplier(FlowVariableTypeRef.class);
        }

        @Override
        public List<Operator> choices(final NodeParametersInput context) {
            final var flowVarType = m_flowVariableTypeSupplier.get();
            if (flowVarType == null) {
                return Arrays.asList(Operator.values());
            }

            final var varType = Arrays.stream(VariableTypeRegistry.getInstance().getAllTypes())
                    .filter(t -> flowVarType.equals(t.getIdentifier())).findAny().orElse(null);

            if (varType == null) {
                return Arrays.asList(Operator.values());
            }

            if (varType == VariableType.StringType.INSTANCE || varType == VariableType.BooleanType.INSTANCE) {
                return NON_NUMERIC_OPERATORS;
            }
            return NUMERIC_OPERATORS;
        }

    }

    static final class PortSettingsArrayProvider implements StateProvider<PortSettings[]> {

        Supplier<PortSettings[]> m_portSettingsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_portSettingsSupplier = initializer.getValueSupplier(PortSettingsArrayRef.class);
        }

        @Override
        public PortSettings[] computeState(final NodeParametersInput parametersInput) {
            final var nrOfInputPorts = parametersInput.getInPortSpecs().length;
            final var currentPortSettings = m_portSettingsSupplier.get();

            var newPortSettings = new ArrayList<>();
            for (int i = 0; i < nrOfInputPorts; i++) {
                if (i < currentPortSettings.length) {
                    newPortSettings.add(currentPortSettings[i]);
                } else {
                    newPortSettings.add(new PortSettings());
                }
            }

            return newPortSettings.toArray(new PortSettings[0]);
        }

    }

    static final class MakeDialogDirtyProvider implements StateProvider<Boolean> {

        private Supplier<Boolean> m_makeDialogDirty;

        private Supplier<Boolean> m_makeDialogDirtyInitialValue;

        private Supplier<Boolean> m_initializationDone;

        private Supplier<String> m_oldFlowVariableTypeSupplier;

        private Supplier<String> m_newFlowVariableTypeSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_initializationDone = initializer.getValueSupplier(InitializationDoneReference.class);
            m_makeDialogDirty = initializer.computeFromValueSupplier(MakeDialogDirty.class);
            m_makeDialogDirtyInitialValue = initializer.getValueSupplier(MakeDialogDirtyInitialValue.class);
            m_oldFlowVariableTypeSupplier = initializer.getValueSupplier(FlowVariableTypeRef.class);
            m_newFlowVariableTypeSupplier = initializer.computeFromProvidedState(FlowVariableTypeProvider.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            if (m_initializationDone.get()) {
                final var changeInternalDialogStateInitialValue = m_makeDialogDirtyInitialValue.get();
                if (m_makeDialogDirty.get() != changeInternalDialogStateInitialValue) {
                    return changeInternalDialogStateInitialValue;
                }
                throw new StateComputationFailureException();
            }

            final var oldType = m_oldFlowVariableTypeSupplier.get();
            final var newType = m_newFlowVariableTypeSupplier.get();

            if (oldType == null || newType == null) {
                return false;
            }

            return !oldType.equals(newType);
        }

    }

    static final class MakeDialogDirtyInitialValueProvider implements StateProvider<Boolean> {

        private Supplier<Boolean> m_changeInternalDialogState;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_changeInternalDialogState = initializer.computeFromProvidedState(MakeDialogDirtyProvider.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return m_changeInternalDialogState.get();
        }

    }

    static final class InitializationDoneProvider implements StateProvider<Boolean> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeFromProvidedState(MakeDialogDirtyProvider.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return true;
        }

    }

    static final class PortSettingsArrayPersistor implements ArrayPersistor<Integer, PortSettings> {

        @Override
        public int getArrayLength(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
            final var addLastRows = nodeSettings.getBooleanArray(
                LoopEndConditionDynamicSettings.CFG_ADD_LAST_ROWS, new boolean[0]);
            return addLastRows.length;
        }

        @Override
        public Integer createElementLoadContext(final int index) {
            return index;
        }

        @Override
        public PortSettings createElementSaveDTO(final int index) {
            return new PortSettings();
        }

        @Override
        public void save(final List<PortSettings> savedElements, final NodeSettingsWO nodeSettings) {
            boolean[] addLastRows = new boolean[savedElements.size()];
            boolean[] addLastRowsOnly = new boolean[savedElements.size()];
            boolean[] addIterationColumn = new boolean[savedElements.size()];

            for (int i = 0; i < savedElements.size(); i++) {
                addLastRows[i] = savedElements.get(i).m_addLastRows;
                addLastRowsOnly[i] = savedElements.get(i).m_addLastRowsOnly;
                addIterationColumn[i] = savedElements.get(i).m_addIterationColumn;
            }

            nodeSettings.addBooleanArray(LoopEndConditionDynamicSettings.CFG_ADD_LAST_ROWS, addLastRows);
            nodeSettings.addBooleanArray(LoopEndConditionDynamicSettings.CFG_ADD_LAST_ROWS_ONLY, addLastRowsOnly);
            nodeSettings.addBooleanArray(LoopEndConditionDynamicSettings.CFG_ADD_ITERATION_COLUMN, addIterationColumn);
        }

    }

    static final class PortSettings implements NodeParameters {

        @Widget(title = "Collect rows from last iteration", description = """
                The default is to collect the rows from all the loop's iterations, including the last one after
                which the loop is stopped. If you de-select this option, the rows from the last iteration are not
                added to this output table.
                """)
        @PersistArrayElement(AddLastRowsPersistor.class)
        @ValueReference(AddLastRowsRef.class)
        @ValueProvider(AddLastRowsProvider.class)
        @Effect(predicate = IsAddLastRowsOnly.class, type = EffectType.DISABLE)
        boolean m_addLastRows = true;

        @Widget(title = "Collect rows from last iteration only", description = """
                If only the rows from the very last iteration should be collected in this table,
                you have to enable this option.
                """)
        @PersistArrayElement(AddLastRowsOnlyPersistor.class)
        @ValueReference(AddLastRowsOnlyRef.class)
        boolean m_addLastRowsOnly;

        @Widget(title = "Add iteration column", description = """
                Allows you to add a column containing the iteration number to this output table.
                """)
        @PersistArrayElement(AddIterationColumnPersistor.class)
        boolean m_addIterationColumn = true;

        static final class AddLastRowsRef implements ParameterReference<Boolean> {
        }

        static final class AddLastRowsOnlyRef implements ParameterReference<Boolean> {
        }

        static final class IsAddLastRowsOnly implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getBoolean(AddLastRowsOnlyRef.class).isTrue();
            }

        }

        static final class AddLastRowsProvider implements StateProvider<Boolean> {

            Supplier<Boolean> m_addLastRowsSupplier;

            Supplier<Boolean> m_addLastRowsOnlySupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_addLastRowsOnlySupplier = initializer.computeFromValueSupplier(AddLastRowsOnlyRef.class);
                m_addLastRowsSupplier = initializer.getValueSupplier(AddLastRowsRef.class);
            }

            @Override
            public Boolean computeState(final NodeParametersInput parametersInput) {
                return m_addLastRowsOnlySupplier.get() || m_addLastRowsSupplier.get();
            }

        }

        static final class AddLastRowsPersistor implements ElementFieldPersistor<Boolean, Integer, PortSettings> {

            @Override
            public Boolean load(final NodeSettingsRO nodeSettings, final Integer loadContext)
                throws InvalidSettingsException {
                boolean[] array = nodeSettings.getBooleanArray(
                    LoopEndConditionDynamicSettings.CFG_ADD_LAST_ROWS, new boolean[]{true});
                return loadContext < array.length ? array[loadContext] : true;
            }

            @Override
            public void save(final Boolean param, final PortSettings saveDTO) {
                saveDTO.m_addLastRows = param;
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{LoopEndConditionDynamicSettings.CFG_ADD_LAST_ROWS}};
            }

        }

        static final class AddLastRowsOnlyPersistor implements ElementFieldPersistor<Boolean, Integer, PortSettings> {

            @Override
            public Boolean load(final NodeSettingsRO nodeSettings, final Integer loadContext)
                throws InvalidSettingsException {
                boolean[] array = nodeSettings.getBooleanArray(
                    LoopEndConditionDynamicSettings.CFG_ADD_LAST_ROWS_ONLY, new boolean[]{false});
                return loadContext < array.length ? array[loadContext] : false;
            }

            @Override
            public void save(final Boolean param, final PortSettings saveDTO) {
                saveDTO.m_addLastRowsOnly = param;
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{LoopEndConditionDynamicSettings.CFG_ADD_LAST_ROWS_ONLY}};
            }

        }

        static final class AddIterationColumnPersistor
            implements ElementFieldPersistor<Boolean, Integer, PortSettings> {

            @Override
            public Boolean load(final NodeSettingsRO nodeSettings, final Integer loadContext)
                throws InvalidSettingsException {
                boolean[] array = nodeSettings.getBooleanArray(LoopEndConditionDynamicSettings.CFG_ADD_ITERATION_COLUMN,
                    new boolean[]{true});
                return loadContext < array.length ? array[loadContext] : true;
            }

            @Override
            public void save(final Boolean param, final PortSettings saveDTO) {
                saveDTO.m_addIterationColumn = param;
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{LoopEndConditionDynamicSettings.CFG_ADD_ITERATION_COLUMN}};
            }

        }

    }

}
