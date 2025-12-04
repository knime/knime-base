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
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 3, 2025 (paulbaernreuther): created
 */
package org.knime.base.node.preproc.pmml.missingval.compute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.knime.base.node.preproc.pmml.missingval.MVSettings;
import org.knime.base.node.preproc.pmml.missingval.compute.MissingValueDataTypeParameters.DataTypeMissingValueTreatment.DataTypeTreatmentTitleProvider;
import org.knime.base.node.preproc.pmml.missingval.compute.MissingValueTreatment.FactoryChoicesProvider;
import org.knime.base.node.preproc.pmml.missingval.compute.MissingValueTreatment.FactoryDynamicSettingsProvider;
import org.knime.base.node.preproc.pmml.missingval.compute.MissingValueTreatment.FactoryIDRef;
import org.knime.base.node.preproc.pmml.missingval.compute.MissingValueTreatment.FactorySettingsRef;
import org.knime.base.node.preproc.pmml.missingval.compute.MissingValueTreatment.MissingValueTreatmentModifier;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal.ElementIsEdited;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal.ElementResetButton;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;

/**
 * We want to show only treatments for data types that are actually present in the input table. But we also want to keep
 * settings of data types that are not currently present, so that when the user adds such columns again later, the
 * settings are still there.
 *
 * So we load to a hidden separate list of all loaded treatments, and then show only those treatments that correspond to
 * data types present in the input table.
 *
 * A caveat of this approach is that the dialog is always dirty initially, because the visible list is computed newly
 * each time the dialog is opened, and thus differs from the initial empty state (since we do not have access to the
 * input specs when loading in a persistor).
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
@Persistor(MissingValueDataTypeParameters.Persistor.class)
class MissingValueDataTypeParameters implements NodeParameters {

    @ValueReference(AllLoadedTreatmentsReference.class)
    LoadedMissingValueTreatment[] m_allLoadedTreatments = new LoadedMissingValueTreatment[0];

    interface AllLoadedTreatmentsReference extends ParameterReference<LoadedMissingValueTreatment[]> {
    }

    @ArrayWidget(hasFixedSize = true)
    @ArrayWidgetInternal(titleProvider = DataTypeTreatmentTitleProvider.class, withEditAndReset = true)
    @Widget(title = "Treatment by Data Type",
        description = "Specify how to treat missing values for all columns of a certain data type.")
    @ValueProvider(ShowPresentDataTypesProvider.class)
    DataTypeMissingValueTreatment[] m_treatmentsForPresentDataTypes = new DataTypeMissingValueTreatment[0];

    static final class ShowPresentDataTypesProvider implements StateProvider<DataTypeMissingValueTreatment[]> {

        private Supplier<LoadedMissingValueTreatment[]> m_allLoadedTreatments;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_allLoadedTreatments = initializer.getValueSupplier(AllLoadedTreatmentsReference.class);
        }

        @Override
        public DataTypeMissingValueTreatment[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var table = parametersInput.getInTableSpec(0);
            if (table.isEmpty()) {
                return new DataTypeMissingValueTreatment[0]; // TODO: Warn the user in this case with a TextMessage.
            }
            List<DataTypeMissingValueTreatment> visibleTreatments = new ArrayList<>();
            final var loadedTreatmentsMap = getLoadedTreatmentsMap();
            final Set<String> encounteredDataTypeKeys = new HashSet<>();
            table.get().stream().forEach(colSpec -> {
                final var dataTypeKey = MVSettings.getTypeKey(colSpec.getType());
                if (encounteredDataTypeKeys.contains(dataTypeKey)) {
                    return; // Already processed this data type.
                }
                encounteredDataTypeKeys.add(dataTypeKey);
                final var treatment = loadedTreatmentsMap.getOrDefault(dataTypeKey, new MissingValueTreatment());
                visibleTreatments.add(new DataTypeMissingValueTreatment(treatment, colSpec.getType()));
            });
            return visibleTreatments.toArray(new DataTypeMissingValueTreatment[0]);

        }

        private Map<String, MissingValueTreatment> getLoadedTreatmentsMap() {
            return Arrays.stream(m_allLoadedTreatments.get()).collect(Collectors.toMap(
                LoadedMissingValueTreatment::getDataTypeKey, LoadedMissingValueTreatment::getTreatment, (l, r) -> r));
        }

    }

    static final class LoadedMissingValueTreatment {

        LoadedMissingValueTreatment() {
            // Default constructor  for serialization
        }

        private LoadedMissingValueTreatment(final String dataTypeKey, final MissingValueTreatment treatment) {
            m_dataTypeKey = dataTypeKey;
            m_treatment = treatment;
        }

        String m_dataTypeKey;

        MissingValueTreatment m_treatment;

        private String getDataTypeKey() {
            return m_dataTypeKey;
        }

        private MissingValueTreatment getTreatment() {
            return m_treatment;
        }

    }

    static final class DataTypeMissingValueTreatment implements NodeParameters {

        DataTypeMissingValueTreatment() {
        }

        DataTypeMissingValueTreatment(final MissingValueTreatment treatment, final DataType dataType) {
            m_treatment = treatment;
            m_dataType = dataType;
        }

        @ValueReference(DataTypeReference.class)
        DataType m_dataType;

        interface DataTypeReference extends ParameterReference<DataType> {
        }

        @ValueProvider(ResetTreatment.class)
        @Effect(predicate = ElementIsEdited.class, type = EffectType.SHOW)
        @Modification(DataTypeTreatmentModifier.class)
        @ValueReference(MissingValueTreatmentRef.class)
        MissingValueTreatment m_treatment = new MissingValueTreatment();

        interface MissingValueTreatmentRef extends ParameterReference<MissingValueTreatment> {
        }

        static final class DataTypeTreatmentModifier extends MissingValueTreatmentModifier {

            @Override
            Class<? extends FactoryChoicesProvider> getChoicesProviderClass() {
                return DataTypeFactoryChoicesProvider.class;
            }

            @Override
            Class<? extends FactoryDynamicSettingsProvider> getDynamicSettingsProviderClass() {
                return DataTypeFactoryDynamicSettingsProvider.class;
            }

            @Override
            Class<? extends FactoryIDRef> getFactoryIDParameterRefClass() {
                return DataTypeFactoryIDRef.class;
            }

            @Override
            Class<? extends FactorySettingsRef> getParameterRefClass() {
                return DataTypeFactorySettingsRef.class;
            }

        }

        static final class DataTypeFactoryChoicesProvider extends FactoryChoicesProvider {

            private Supplier<DataType> m_dataTypeSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                super.init(initializer);
                m_dataTypeSupplier = initializer.getValueSupplier(DataTypeReference.class);
            }

            @Override
            DataType[] getDataTypes(final NodeParametersInput context) {
                return new DataType[]{m_dataTypeSupplier.get()};
            }

        }

        static final class DataTypeFactoryDynamicSettingsProvider extends FactoryDynamicSettingsProvider {

            @Override
            protected Class<? extends FactorySettingsRef> getParameterRefClass() {
                return DataTypeFactorySettingsRef.class;
            }

            @Override
            protected Class<? extends FactoryIDRef> getFactoryIDParameterRefClass() {
                return DataTypeFactoryIDRef.class;
            }

            @Override
            Class<? extends FactoryChoicesProvider> getChoicesProviderClass() {
                return DataTypeFactoryChoicesProvider.class;
            }

        }

        interface DataTypeFactorySettingsRef extends FactorySettingsRef {
        }

        interface DataTypeFactoryIDRef extends FactoryIDRef {
        }

        static final class ResetTreatment implements StateProvider<MissingValueTreatment> {
            private Supplier<MissingValueTreatment> m_currentValueSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeOnButtonClick(ElementResetButton.class);
                m_currentValueSupplier = initializer.getValueSupplier(MissingValueTreatmentRef.class);
            }

            @Override
            public MissingValueTreatment computeState(final NodeParametersInput parametersInput)
                throws StateComputationFailureException {
                if (m_currentValueSupplier.get().isDefault()) {
                    throw new StateComputationFailureException();
                }

                return new MissingValueTreatment();
            }
        }

        static final class DataTypeTreatmentTitleProvider implements StateProvider<String> {

            private static final Pattern COLLECTION_OF_END_PATTERN = Pattern.compile(" \\(Collection of:.*");

            private Supplier<DataType> m_typeSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
                m_typeSupplier = initializer.getValueSupplier(DataTypeReference.class);
            }

            @Override
            public String computeState(final NodeParametersInput parametersInput)
                throws StateComputationFailureException {
                final var dataType = m_typeSupplier.get();
                if (dataType == null) {
                    throw new IllegalStateException("No data type available within a data type treatment.");
                }
                if (dataType.isCollectionType()) {
                    return COLLECTION_OF_END_PATTERN.matcher(dataType.toPrettyString()).replaceFirst("");
                }
                return dataType.toPrettyString();
            }
        }

    }

    static final class Persistor implements NodeParametersPersistor<MissingValueDataTypeParameters> {

        @Override
        public MissingValueDataTypeParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final List<LoadedMissingValueTreatment> loadedTreatments = new ArrayList<>();
            for (var key : settings.keySet()) {
                final var treatmentSettings = settings.getNodeSettings(key);
                final var treatment = NodeParametersUtil.loadSettings(treatmentSettings, MissingValueTreatment.class);
                loadedTreatments.add(new LoadedMissingValueTreatment(key, treatment));
            }
            final var loaded = new MissingValueDataTypeParameters();
            loaded.m_allLoadedTreatments = loadedTreatments.toArray(new LoadedMissingValueTreatment[0]);
            return loaded;

        }

        @Override
        public void save(final MissingValueDataTypeParameters param, final NodeSettingsWO settings) {
            for (var loadedTreatment : param.m_allLoadedTreatments) {
                final var treatmentSettings = settings.addNodeSettings(loadedTreatment.getDataTypeKey());
                saveTreatment(loadedTreatment.getTreatment(), treatmentSettings);
            }
            for (var dataTypeTreatment : param.m_treatmentsForPresentDataTypes) {
                final var key = MVSettings.getTypeKey(dataTypeTreatment.m_dataType);
                final var treatmentSettings = settings.addNodeSettings(key);
                saveTreatment(dataTypeTreatment.m_treatment, treatmentSettings);
            }
        }

        private static void saveTreatment(final MissingValueTreatment treatment,
            final NodeSettingsWO treatmentSettings) {
            NodeParametersUtil.saveSettings(MissingValueTreatment.class, treatment, treatmentSettings);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[0][]; // No static config paths available
        }

    }

}
