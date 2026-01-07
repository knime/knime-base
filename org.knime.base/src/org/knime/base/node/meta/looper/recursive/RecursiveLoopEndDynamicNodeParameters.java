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
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.array.PerPortValueProvider;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.OptionalStringPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
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
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@LoadDefaultsForAbsentFields
final class RecursiveLoopEndDynamicNodeParameters implements NodeParameters {

    @Section(title = "End Recursion Settings")
    interface RecursionSettingsSection {
    }

    @Section(title = "Data Collection",
        description = "Configure how data from each collection port is collected across iterations.")
    @After(RecursionSettingsSection.class)
    interface DataCollectionSection {
    }

    @Section(title = "Flow Variables")
    @After(DataCollectionSection.class)
    interface FlowVariablesSection {
    }

    @Widget(title = "Minimal number of rows", description = """
            The minimal number of rows for each recursion input table required to continue iterating.
            If <b>one</b> of the tables falls below its configured minimum, the loop stops.
            The minimum can be set per recursion input table individually.
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
            return Arrays.stream(nodeSettings.getLongArray(CONFIG_KEY)) //
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
        @Widget(title = "Minimal number of rows for input port", description = """
                Minimal number of rows for the recursion input table required to continue iterating.
                If the number of rows falls below this threshold, the loop stops.
                """)
        @NumberInputWidget(minValidation = NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation.class)
        long m_minNrOfRows = 1;
    }

    private static final class RecursionPortSettingsValueProvider extends PerPortValueProvider<RecursionPortSettings> {

        RecursionPortSettingsValueProvider() {
            super(RecursiveLoopEndDynamicNodeFactory.RECURSION_PORT_GROUP_ID, PortGroupSide.INPUT);
        }

        @Override
        protected Supplier<RecursionPortSettings[]> supplier(final StateProviderInitializer initializer) {
            return initializer.getValueSupplier(RecursionPortSettingsParameterReference.class);
        }

        @Override
        protected RecursionPortSettings[] newArray(final int size) {
            return new RecursionPortSettings[size];
        }

        @Override
        protected RecursionPortSettings newInstance(int index) {
            return new RecursionPortSettings();
        }
    }

    @Widget(title = "Maximal number of iterations",
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

    private static final class FlowVariableNamePersistor extends OptionalStringPersistor {
        FlowVariableNamePersistor() {
            super("useFlowVariable", "flowVariableName");
        }
    }

    static final class FlowVariableNameDefaultProvider implements DefaultValueProvider<String> {
        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            return RecursiveLoopEndDynamicNodeModel
                .getValidVariablesSupplier(parametersInput::getAvailableInputFlowVariables).get().keySet().stream()
                .findFirst().orElse("");
        }
    }

    static final class FlowVariableNameChoicesProvider implements FlowVariableChoicesProvider {
        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput input) {
            return RecursiveLoopEndDynamicNodeModel.getValidVariablesSupplier(input::getAvailableInputFlowVariables)
                .get().values().stream().toList();
        }
    }

    @Widget(title = "End loop with variable", description = """
            Enables ending the loop based on a flow variable.
            When selected, choose the flow variable that terminates the loop when its value equals &#8220;true&#8221;.
            """)
    @Layout(RecursionSettingsSection.class)
    @Effect(predicate = HasNoValidVariables.class, type = EffectType.DISABLE)
    @Persistor(FlowVariableNamePersistor.class)
    @OptionalWidget(defaultProvider = FlowVariableNameDefaultProvider.class)
    @ChoicesProvider(FlowVariableNameChoicesProvider.class)
    Optional<String> m_flowVariableName = Optional.empty();

    @Widget(title = "Data collection",
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
        protected Supplier<DataCollectionPortSettings[]> supplier(final StateProviderInitializer initializer) {
            return initializer.getValueSupplier(DataCollectionPortSettingsParameterReference.class);
        }

        @Override
        protected DataCollectionPortSettings[] newArray(final int size) {
            return new DataCollectionPortSettings[size];
        }

        @Override
        protected DataCollectionPortSettings newInstance(int index) {
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
        @SuppressWarnings("java:S3878")
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

        @Widget(title = "Collect data from last iteration only", description = """
                If this option is checked, only the last input to the corresponding collecting data port is passed
                through to the outport. Hence, the data of earlier iterations is discarded. This option can be set for
                each collector port individually.
                """)
        @ValueReference(CollectDataFromLastIterationOnlyRef.class)
        boolean m_collectDataFromLastIterationOnly;

        @Widget(title = "Add iteration column", description = """
                Adds a column containing the iteration number to the corresponding collector output table.
                Disabled when only the last iteration is collected. Configurable per collector port.
                """)
        @Effect(predicate = CollectDataFromLastIterationOnlyPredicate.class, type = EffectType.DISABLE)
        boolean m_addIterationColumn;
    }

    @Widget(title = "Propagate modified loop variables", description = """
            If checked, variables whose values are modified within the loop are exported by this node. These variables
            must be declared outside the loop, i.e. injected into the loop from a side-branch or be available upstream
            of the corresponding loop start node. For the latter, any modification of a variable is passed back to the
            start node in subsequent iterations (e.g. moving sum calculation). Note that variables defined by the loop
            start node itself are excluded as these usually represent loop controls (e.g. <i>"currentIteration"</i>).
            """)
    @Layout(FlowVariablesSection.class)
    @Persist(configKey = "propagateFlowVariables")
    boolean m_propagateFlowVariables;

}
