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
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
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
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation;

/**
 * Node parameters for Recursive Loop End.
 * 
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
// TODO consider marking some as @Advanced?
// TODO descriptions
public final class RecursiveLoopEndDynamicNodeParameters implements NodeParameters {

    // Section "Recursion Settings"
    @Section(title = "Recursion")
    protected interface RecursionSettingsSection {
    }

    @Section(title = "Data collection")
    @After(RecursionSettingsSection.class)
    protected interface DataCollectionSection {
    }

    @Section(title = "Flow variables")
    @After(DataCollectionSection.class)
    protected interface FlowVariablesSection {
    }

    @Widget(title = "Minimum number of rows", description = "...")
    @Layout(RecursionSettingsSection.class)
    @ArrayWidget(elementTitle = "Recursion Port", hasFixedSize = true)
    @Persistor(RecursionPortSettingsPersistor.class)
    @ValueReference(RecursionPortSettingsParameterReference.class)
    @ValueProvider(RecursionPortSettingsValueProvider.class)
    RecursionPortSettings[] m_recursionPortSettings = new RecursionPortSettings[0];

    private static final class RecursionPortSettingsPersistor
        implements NodeParametersPersistor<RecursionPortSettings[]> {

        // single widget in this group
        private static final String CONFIG_KEY = "minNrOfRows";

        @Override
        public RecursionPortSettings[] load(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
            return Arrays.stream(nodeSettings.getIntArray(CONFIG_KEY, new int[0])) //
                .mapToObj(entry -> {
                    var v = new RecursionPortSettings();
                    v.m_minNrOfRows = entry;
                    return v;
                }) //
                .toArray(RecursionPortSettings[]::new); //
        }

        @Override
        public void save(final RecursionPortSettings[] widgetSettings, final NodeSettingsWO nodeSettings) {
            var values = Arrays.stream(widgetSettings) //
                .mapToLong(widget -> widget.m_minNrOfRows) //
                .toArray();
            nodeSettings.addLongArray(CONFIG_KEY, values);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[0][];
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

    // V would be RecursionPortSettings here, so does not care about included widgets
    private abstract static class PerPortValueProvider<V> implements StateProvider<V[]> {

        // todo more precise name
        private Supplier<V[]> m_widgetSettings;

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
            var numberOfPorts = parametersInput.getInPortSpecs().length;
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
    }

    private static final class RecursionPortSettingsValueProvider extends PerPortValueProvider<RecursionPortSettings> {

        @Override
        Supplier<RecursionPortSettings[]> supplier(final StateProviderInitializer initializer) {
            return initializer.getValueSupplier(RecursionPortSettingsParameterReference.class);
        }

        @Override
        RecursionPortSettings[] newArray(final int size) {
            return new RecursionPortSettings[0];
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

    @Widget(title = "...", description = "...")
    @Layout(DataCollectionSection.class)
    @ArrayWidget(elementTitle = "Collection Port", hasFixedSize = true)
    @Persistor(DataCollectionPortSettingsPersistor.class)
    @ValueReference(DataCollectionPortSettingsParameterReference.class)
    @ValueProvider(DataCollectionPortSettingsValueProvider.class)
    DataCollectionPortSettings[] m_dataCollectionPortSettings = new DataCollectionPortSettings[0];

    private static final class DataCollectionPortSettingsValueProvider
        extends PerPortValueProvider<DataCollectionPortSettings> {
        @Override
        Supplier<DataCollectionPortSettings[]> supplier(final StateProviderInitializer initializer) {
            return initializer.getValueSupplier(DataCollectionPortSettingsParameterReference.class);
        }

        @Override
        DataCollectionPortSettings[] newArray(final int size) {
            return new DataCollectionPortSettings[0];
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
        @Override
        public DataCollectionPortSettings[] load(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
            final var collectData = nodeSettings.getBooleanArray("onlyLastData", new boolean[0]);
            final var addIterationColumn = nodeSettings.getBooleanArray("addIterationColumn", new boolean[0]);
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
            if (widgetSettings == null || widgetSettings.length == 0) {
                nodeSettings.addBooleanArray("onlyLastData");
                nodeSettings.addBooleanArray("addIterationColumn");
                return;
            }
            final var collectData = new boolean[widgetSettings.length];
            final var addIterationColumn = new boolean[widgetSettings.length];
            for (var i = 0; i < widgetSettings.length; i++) {
                var widgetSetting = widgetSettings[i];
                collectData[i] = widgetSetting != null && widgetSetting.m_collectDataFromLastIterationOnly;
                addIterationColumn[i] = widgetSetting != null && widgetSetting.m_addIterationColumn;
            }
            nodeSettings.addBooleanArray("onlyLastData", collectData);
            nodeSettings.addBooleanArray("addIterationColumn", addIterationColumn);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"onlyLastData"}, {"addIterationColumn"}};
        }
    }

    private static final class DataCollectionPortSettings implements NodeParameters {
        @Widget(title = "Collect data from last iteration only", description = "...")
        boolean m_collectDataFromLastIterationOnly;

        // TODO in the original dialog, this is disabled if "collect data" is checked
        @Widget(title = "Add iteration column", description = "...")
        boolean m_addIterationColumn;
    }

    // TODO this parameter is marked as "deprecated" and disabled in legacy dialog, how to deal with it?
    //    @Widget(title = "End loop with variable", description = "End loop if selected flow variable equals true.")
    //    @ChoicesProvider(BooleanConvertibleFlowVariablesProvider.class)
    //    @Persist(configKey = "useFlowVariable")
    //    boolean m_useFlowVariable = false;
    //
    //    private static final class BooleanConvertibleFlowVariablesProvider implements FlowVariableChoicesProvider {
    //        @Override
    //        public List<FlowVariable> flowVariableChoices(NodeParametersInput context) {
    //            return ((NodeParametersInputImpl)context)
    //                    .getAvailableInputFlowVariables(VariableType.BooleanType.INSTANCE) //
    //                    .values().stream().toList();
    //        }
    //    }

    @Widget(title = "Propagate modified loop variables", description = "Export variables modified within the loop.")
    @Layout(FlowVariablesSection.class)
    @Persist(configKey = "propagateFlowVariables")
    boolean m_propagateFlowVariables;

}
