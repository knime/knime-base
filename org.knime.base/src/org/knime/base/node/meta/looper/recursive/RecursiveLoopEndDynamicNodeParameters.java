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

package org.knime.base.node.meta.looper.recursive;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
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
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation;

/**
 * Node parameters for Recursive Loop End.
 *
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
final class RecursiveLoopEndDynamicNodeParameters implements NodeParameters {

    @Section(title = "Recursion")
    interface RecursionSettingsSection {
    }

    @Section(title = "Data Collection")
    @After(RecursionSettingsSection.class)
    interface DataCollectionSection {
    }

    @Section(title = "Flow Variables")
    @After(DataCollectionSection.class)
    interface FlowVariablesSection {
    }

    @Widget(title = "Minimum number of rows", description = """
            Configure the minimal number of rows required on each recursion input table before the loop continues.
            """)
    @Layout(RecursionSettingsSection.class)
    @ArrayWidget(elementTitle = "Recursion port", hasFixedSize = true)
    @Persistor(RecursionPortSettingsPersistor.class)
    @ValueReference(RecursionPortSettingsParameterReference.class)
    @ValueProvider(RecursionPortSettingsValueProvider.class)
    RecursionPortSettings[] m_recursionPortSettings = new RecursionPortSettings[0];

    private static final class RecursionPortSettingsPersistor
        implements NodeParametersPersistor<RecursionPortSettings[]> {

        // single widget in this group
        private static final String CONFIG_KEY = "minNumberOfRows";

        @Override
        public RecursionPortSettings[] load(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
            return Arrays.stream(nodeSettings.getLongArray(CONFIG_KEY, new long[0])) //
                .mapToObj(minNrOfRows -> {
                    var v = new RecursionPortSettings();
                    v.m_minNrOfRows = minNrOfRows;
                    return v;
                }) //
                .toArray(RecursionPortSettings[]::new); //
        }

        @Override
        public void save(final RecursionPortSettings[] recursionPortSettings, final NodeSettingsWO nodeSettings) {
            var values = Arrays.stream(recursionPortSettings) //
                .mapToLong(recursionPortSetting -> recursionPortSetting.m_minNrOfRows) //
                .toArray();
            nodeSettings.addLongArray(CONFIG_KEY, values);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CONFIG_KEY}};
        }
    }

    private static final class RecursionPortSettingsParameterReference
        implements ParameterReference<RecursionPortSettings[]> {
    }

    private static final class RecursionPortSettings implements NodeParameters {
        @Widget(title = "Minimal number of rows",
            description = "Minimal number of rows for each recursion input table required to continue iterating.")
        @NumberInputWidget(minValidation = NumberInputWidgetValidation.MinValidation.IsPositiveDoubleValidation.class)
        long m_minNrOfRows = 1;
    }

    private abstract static class PerPortValueProvider<V> implements StateProvider<V[]> {

        enum PortGroupSide {
                INPUT, OUTPUT
        }

        private final String m_portGroupId;

        private final PortGroupSide m_portGroupSide;

        private Supplier<V[]> m_widgetSettings;

        PerPortValueProvider(final String portGroupId, final PortGroupSide portGroupSide) {
            m_portGroupId = portGroupId;
            m_portGroupSide = portGroupSide;
        }

        abstract Supplier<V[]> supplier(StateProviderInitializer initializer);

        abstract V[] newArray(int size);

        abstract V newInstance();

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_widgetSettings = supplier(initializer);
        }

        @Override
        public V[] computeState(final NodeParametersInput parametersInput) {
            var numberOfPorts = getNumberOfPorts(parametersInput);
            var currentSettings = m_widgetSettings.get();

            if (currentSettings.length == numberOfPorts) {
                return currentSettings;
            }

            var newSettings = newArray(numberOfPorts);

            var numCurrentSettings = Math.min(currentSettings.length, numberOfPorts);
            System.arraycopy(currentSettings, 0, newSettings, 0, numCurrentSettings);

            for (var i = numCurrentSettings; i < numberOfPorts; i++) {
                newSettings[i] = newInstance();
            }

            return newSettings;
        }

        private int getNumberOfPorts(final NodeParametersInput parametersInput) {
            try {
                var portsConfig = parametersInput.getPortsConfiguration();
                var portLocations = m_portGroupSide == PortGroupSide.INPUT ? portsConfig.getInputPortLocation()
                    : portsConfig.getOutputPortLocation();
                var indices = portLocations.get(m_portGroupId);
                if (indices != null) {
                    return indices.length;
                }
            } catch (IllegalStateException ex) {
                // fall through to the fallback below if no ports configuration is available
            }
            return m_portGroupSide == PortGroupSide.INPUT ? parametersInput.getInPortTypes().length
                : parametersInput.getOutPortTypes().length;
        }
    }

    private static final class RecursionPortSettingsValueProvider extends PerPortValueProvider<RecursionPortSettings> {

        RecursionPortSettingsValueProvider() {
            super(RecursiveLoopEndDynamicNodeFactory.RECURSION_PORT_GROUP_ID, PortGroupSide.INPUT);
        }

        @Override
        Supplier<RecursionPortSettings[]> supplier(final StateProviderInitializer initializer) {
            return initializer.getValueSupplier(RecursionPortSettingsParameterReference.class);
        }

        @Override
        RecursionPortSettings[] newArray(final int size) {
            return new RecursionPortSettings[size];
        }

        @Override
        RecursionPortSettings newInstance() {
            return new RecursionPortSettings();
        }
    }

    @Widget(title = "Maximum number of iterations",
        description = "The maximum number of iterations the loop will run. Prevents endless loops.")
    @Layout(RecursionSettingsSection.class)
    @NumberInputWidget(minValidation = NumberInputWidgetValidation.MinValidation.IsPositiveDoubleValidation.class)
    @Persist(configKey = "maxNrIterations")
    int m_maxNrIterations = 100;

    static final class HasNoValidVariables implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getConstant(input -> RecursiveLoopEndDynamicNodeModel
                .getValidVariablesSupplier(input::getAvailableInputFlowVariables).get().isEmpty());
        }
    }

    interface UseFlowVariableRef extends ParameterReference<Boolean> {
    }

    @Widget(title = "End loop with variable", description = """
            The loop ends if the option is enabled and the value of the selected variable equals “true”.
            """)
    @Layout(RecursionSettingsSection.class)
    @ValueReference(UseFlowVariableRef.class)
    @Effect(predicate = HasNoValidVariables.class, type = EffectType.HIDE)
    boolean m_useFlowVariable;

    static final class HasNoValidVariablesOrNotUsingVariable implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(HasNoValidVariables.class).or(i.getBoolean(UseFlowVariableRef.class).isFalse());
        }
    }

    static final class FlowVariableNameChoicesProvider implements FlowVariableChoicesProvider {
        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput input) {
            return RecursiveLoopEndDynamicNodeModel.getValidVariablesSupplier(input::getAvailableInputFlowVariables)
                .get().values().stream().toList();
        }
    }

    @Widget(title = "Flow variable to end loop with",
        description = "Select the flow variable that will end the loop if its value is \"true\".")
    @Layout(RecursionSettingsSection.class)
    @Effect(predicate = HasNoValidVariablesOrNotUsingVariable.class, type = EffectType.HIDE)
    @ChoicesProvider(FlowVariableNameChoicesProvider.class)
    String m_flowVariableName = "";

    @Widget(title = "Collection port settings",
        description = "Configure how data from each collection port is collected across iterations.")
    @Layout(DataCollectionSection.class)
    @ArrayWidget(elementTitle = "Collection port", hasFixedSize = true)
    @Persistor(DataCollectionPortSettingsPersistor.class)
    @ValueReference(DataCollectionPortSettingsParameterReference.class)
    @ValueProvider(DataCollectionPortSettingsValueProvider.class)
    DataCollectionPortSettings[] m_dataCollectionPortSettings = new DataCollectionPortSettings[0];

    private static final class DataCollectionPortSettingsValueProvider
        extends PerPortValueProvider<DataCollectionPortSettings> {

        DataCollectionPortSettingsValueProvider() {
            super(RecursiveLoopEndDynamicNodeFactory.COLLECTOR_PORT_GROUP_ID, PortGroupSide.INPUT);
        }

        @Override
        Supplier<DataCollectionPortSettings[]> supplier(final StateProviderInitializer initializer) {
            return initializer.getValueSupplier(DataCollectionPortSettingsParameterReference.class);
        }

        @Override
        DataCollectionPortSettings[] newArray(final int size) {
            return new DataCollectionPortSettings[size];
        }

        @Override
        DataCollectionPortSettings newInstance() {
            return new DataCollectionPortSettings();
        }
    }

    private static final class DataCollectionPortSettingsParameterReference
        implements ParameterReference<DataCollectionPortSettings[]> {
    }

    private static final class DataCollectionPortSettingsPersistor
        implements NodeParametersPersistor<DataCollectionPortSettings[]> {

        private static final String CFG_KEY_ONLY_LAST_DATA = "onlyLastData";

        private static final String CFG_KEY_ADD_ITERATION_COLUMN = "addIterationColumn";

        @Override
        public DataCollectionPortSettings[] load(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
            final var collectData = nodeSettings.getBooleanArray(CFG_KEY_ONLY_LAST_DATA, new boolean[0]);
            final var addIterationColumn = nodeSettings.getBooleanArray(CFG_KEY_ADD_ITERATION_COLUMN, new boolean[0]);
            final var numberOfItems = Math.max(collectData.length, addIterationColumn.length);
            var result = new DataCollectionPortSettings[numberOfItems];
            for (var i = 0; i < numberOfItems; i++) {
                result[i] = new DataCollectionPortSettings();
                result[i].m_collectDataFromLastIterationOnly = i < collectData.length && collectData[i];
                result[i].m_addIterationColumn = i < addIterationColumn.length && addIterationColumn[i];
            }
            return result;
        }

        @Override
        public void save(final DataCollectionPortSettings[] widgetSettings, final NodeSettingsWO nodeSettings) {
            final var collectData = new boolean[widgetSettings.length];
            final var addIterationColumn = new boolean[widgetSettings.length];
            for (var i = 0; i < widgetSettings.length; i++) {
                var widgetSetting = widgetSettings[i];
                collectData[i] = widgetSetting != null && widgetSetting.m_collectDataFromLastIterationOnly;
                addIterationColumn[i] = widgetSetting != null && widgetSetting.m_addIterationColumn;
            }
            nodeSettings.addBooleanArray(CFG_KEY_ONLY_LAST_DATA, collectData);
            nodeSettings.addBooleanArray(CFG_KEY_ADD_ITERATION_COLUMN, addIterationColumn);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_KEY_ONLY_LAST_DATA}, {CFG_KEY_ADD_ITERATION_COLUMN}};
        }
    }

    private static final class DataCollectionPortSettings implements NodeParameters {

        private interface CollectDataFromLastIterationOnlyRef extends ParameterReference<Boolean> {
        }

        private static final class CollectDataFromLastIterationOnlyPredicate implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer initializer) {
                return initializer.getBoolean(CollectDataFromLastIterationOnlyRef.class).isTrue();
            }
        }

        @Widget(title = "Collect data from last iteration only",
            description = "Return only the output of the final iteration and "
                + "disable accumulation from earlier iterations.")
        @ValueReference(CollectDataFromLastIterationOnlyRef.class)
        boolean m_collectDataFromLastIterationOnly;

        @Widget(title = "Add iteration column",
            description = "Append an iteration index column to label rows by iteration. "
                + "Disabled when only the last iteration is collected.")
        @Effect(predicate = CollectDataFromLastIterationOnlyPredicate.class, type = EffectType.DISABLE)
        boolean m_addIterationColumn;
    }

    @Widget(title = "Propagate modified loop variables", description = "Export variables modified within the loop.")
    @Layout(FlowVariablesSection.class)
    @Persist(configKey = "propagateFlowVariables")
    boolean m_propagateFlowVariables;

}
