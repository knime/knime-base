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
 */
package org.knime.base.node.preproc.manipulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.base.node.io.filehandling.webui.testing.reader2.TransformationParametersStateProviderTestUtils.assertTableSpec;
import static org.knime.base.node.io.filehandling.webui.testing.reader2.TransformationParametersStateProviderTestUtils.assertTransformationElementSettings;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.knime.base.node.io.filehandling.webui.reader.DataTypeStringSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.ColumnSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader2.DataTypeSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderSpecific.ExternalDataTypeSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.TableSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationElementSettings;
import org.knime.base.node.io.filehandling.webui.testing.LocalWorkflowContextTest;
import org.knime.base.node.io.filehandling.webui.testing.reader2.TransformationParametersStateProviderTestUtils.TransformationParametersUpdatesTestDataTypeBased;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.datatype.convert.ProductionPathUtils;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.IndexedValue;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ProductionPathSerializer;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.testing.node.dialog.updates.DialogUpdateSimulator;
import org.knime.testing.node.dialog.updates.UpdateSimulator;
import org.knime.testing.node.dialog.updates.UpdateSimulator.UpdateSimulatorResult;

/**
 * Tests for the state providers in {@link TableManipulatorTransformationParameters}. This is mostly copied & adapted
 * from the {@link TransformationParametersUpdatesTestDataTypeBased}, but with the code from the abstract base & util
 * classes inlined to adapt to the incompatible types outside the reader framework.
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH, Germany
 */
@SuppressWarnings("restriction")
final class TableManipulatorTransformationParametersStateProvidersTest extends LocalWorkflowContextTest {

    // --- Inlined from TransformationParametersUpdatesTestClassBased / TransformationParametersUpdatesTest ---

    private static final String INT_COL = "intCol";

    private static final String STRING_COL = "stringCol";

    private static final String DEFAULT_COLUMNTYPE = "<default-columntype>";

    private TableManipulatorNodeParameters m_settings;

    private UpdateSimulator m_simulator;

    private static NodeParametersInput createNodeParametersInput() {
        final var inputSpec = new DataTableSpec(new DataColumnSpecCreator(INT_COL, IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator(STRING_COL, StringCell.TYPE).createSpec());
        return NodeParametersInputImpl.createDefaultNodeSettingsContext(new PortType[]{BufferedDataTable.TYPE},
            new PortObjectSpec[]{inputSpec}, null, null);
    }

    private TableManipulatorTransformationParameters getTransformationSettings() {
        return getTransformationSettings(m_settings);
    }

    private static ExternalDataTypeSerializer<DataType> getExternalDataTypeSerializer() {
        return new DataTypeSerializer() {
        };
    }

    private static DataType getIntType() {
        return IntCell.TYPE;
    }

    private static DataType getStringType() {
        return StringCell.TYPE;
    }

    private static DataType getDoubleType() {
        return DoubleCell.TYPE;
    }

    @BeforeEach
    void setUpSettings() {
        m_settings = constructNewSettings();
        m_simulator = new DialogUpdateSimulator(m_settings, createNodeParametersInput());
    }

    @Test
    void testTableSpecSettingsProvider() {
        testTableSpecSettingsProvider(UpdateSimulator::simulateAfterOpenDialog);
    }

    private void testTableSpecSettingsProvider(final Function<UpdateSimulator, UpdateSimulatorResult> simulate) {
        final var simulatorResult = simulate.apply(m_simulator);
        final var specs = getTableSpecsValueUpdate(simulatorResult);
        assertIntegerAndStringColumn(specs);
    }

    private static void assertIntegerAndStringColumn(final TableSpecSettings[] specs) {
        final var serializer = getExternalDataTypeSerializer();
        assertTableSpec(specs, new String[]{INT_COL, STRING_COL},
            List.of(serializer.toSerializableType(getIntType()), serializer.toSerializableType(getStringType())));
    }

    @Test
    void testTransformationElementSettingsProvider() {
        testTransformationElementSettingsProvider(UpdateSimulator::simulateAfterOpenDialog);
    }

    void testTransformationElementSettingsProvider(final Function<UpdateSimulator, UpdateSimulatorResult> simulate) {
        final var transformationElements = getUpdatedTransformationElementSettings(simulate);
        assertStandardTransformationElementSettings(transformationElements);
    }

    @ParameterizedTest
    @EnumSource(TableManipulatorParameters.ColumnFilterModeOption.class)
    void testTransformationElementSettingsProviderColumnFilterModeOption(
        final TableManipulatorParameters.ColumnFilterModeOption columnFilterMode) {
        setColumnFilterMode(m_settings, columnFilterMode);
        testTransformationElementSettingsProvider(UpdateSimulator::simulateAfterOpenDialog);
    }

    private void assertStandardTransformationElementSettings(final Object transformationElements) {
        assertTransformationElementSettings(transformationElements, new String[]{INT_COL, STRING_COL, null},
            new boolean[]{true, true, true}, new String[]{INT_COL, STRING_COL, null}, new String[]{
                getDefaultPathIdentifier(getIntType()), getDefaultPathIdentifier(getStringType()), DEFAULT_COLUMNTYPE});
    }

    @Test
    @SuppressWarnings("deprecation")
    void testTransformationElementSettingsProviderUnknownColumns() {
        setTransformationElementSettingsWithUnknown(getTransformationSettings(), StringCell.TYPE);
        final var transformationElements = getUpdatedTransformationElementSettings();
        assertTransformationElementSettings(transformationElements, new String[]{INT_COL, STRING_COL, null},
            new boolean[]{false, false, false}, new String[]{INT_COL, STRING_COL, null}, //
            new String[]{ //
                getPathIdentifier(getIntType(), StringCell.TYPE), //
                getDefaultPathIdentifier(getStringType()), //
                DataTypeStringSerializer.typeToString(StringCell.TYPE) //
            }, new String[]{ //
                getDefaultPathIdentifier(getIntType()), //
                getDefaultPathIdentifier(getStringType()), //
                DEFAULT_COLUMNTYPE //
            });
    }

    @Test
    void testTransformationElementSettingsProviderExistingColumns() {
        final var externalDataTypeSerializer = getExternalDataTypeSerializer();
        setExistingTableSpecs(getTransformationSettings(), List.of(INT_COL, STRING_COL),
            List.of(externalDataTypeSerializer.toSerializableType(getDoubleType()),
                externalDataTypeSerializer.toSerializableType(getStringType())));
        setTransformationElementSettingsWithExisting(getTransformationSettings(), new String[]{STRING_COL, INT_COL},
            new boolean[]{false, false}, new String[]{"Renamed stringCol", "Renamed intCol"}, new String[]{//
                getPathIdentifier(getStringType(), XMLCell.TYPE), //
                getPathIdentifier(getDoubleType(), DoubleCell.TYPE) //
            }, //
            new String[]{ //
                getDefaultPathIdentifier(getStringType()), //
                getDefaultPathIdentifier(getDoubleType())//
            });
        final var transformationElements = getUpdatedTransformationElementSettings();
        assertTransformationElementSettings(transformationElements, new String[]{STRING_COL, INT_COL, null},
            new boolean[]{false, true, true}, new String[]{"Renamed stringCol", INT_COL, null}, new String[]{ //
                getPathIdentifier(getStringType(), XMLCell.TYPE), //
                getDefaultPathIdentifier(getIntType()), //
                DEFAULT_COLUMNTYPE//
            }, //
            new String[]{//
                getDefaultPathIdentifier(getStringType()), //
                getDefaultPathIdentifier(getIntType()), //
                DEFAULT_COLUMNTYPE//
            });
    }

    private static String getDefaultPathIdentifier(final DataType typeClass) {
        return ProductionPathUtils.getPathIdentifier(getProductionPathProvider().getDefaultProductionPath(typeClass),
            getProductionPathSerializer());
    }

    private static String getPathIdentifier(final DataType typeClass, final DataType targetDataType) {
        final var productionPath = getProductionPathProvider().getAvailableProductionPaths(typeClass).stream()
            .filter(path -> path.getDestinationType().equals(targetDataType)).findFirst().orElseThrow();
        return ProductionPathUtils.getPathIdentifier(productionPath, getProductionPathSerializer());
    }

    private Object getUpdatedTransformationElementSettings() {
        return getUpdatedTransformationElementSettings(UpdateSimulator::simulateAfterOpenDialog);
    }

    private Object
        getUpdatedTransformationElementSettings(final Function<UpdateSimulator, UpdateSimulatorResult> simulate) {
        final var simulatorResult = simulate.apply(m_simulator);
        return getTransformationElementsValueUpdate(simulatorResult);
    }

    @Test
    void testTypeChoicesProvider() {
        final var simulatorResult = getSimulatorResultForUpdatesInElementSettingsArray();
        final var typeChoicesProviderResult = simulatorResult.getMultiUiStateUpdateAt(List
            .of(List.of(combineWithPathToTransformationSettings("columnTransformation")), List.of("productionPath")),
            "possibleValues");
        assertSizeAndIndices(typeChoicesProviderResult, 3);
        assertThat(typeChoicesProviderResult.get(0).value()).isEqualTo(typeChoices(getIntType()));
        assertThat(typeChoicesProviderResult.get(1).value()).isEqualTo(typeChoices(getStringType()));
        assertThat(((List<StringChoice>)typeChoicesProviderResult.get(2).value()).get(0).id())
            .isEqualTo(DEFAULT_COLUMNTYPE);
    }

    @Test
    void testTitlesAndSubTitles() {
        final var simulatorResult = getSimulatorResultForUpdatesInElementSettingsArray();
        final var titles = simulatorResult.getMultiUiStateUpdateAt(
            List.of(List.of(combineWithPathToTransformationSettings("columnTransformation"))), "arrayElementTitle");
        final var subTitles = simulatorResult.getMultiUiStateUpdateAt(
            List.of(List.of(combineWithPathToTransformationSettings("columnTransformation"))), "elementSubTitle");
        assertSizeAndIndices(titles, 3);
        assertSizeAndIndices(subTitles, 3);
        assertThat(titles.get(0).value()).isEqualTo(INT_COL);
        assertThat(titles.get(1).value()).isEqualTo(STRING_COL);
        assertThat(titles.get(2).value()).isEqualTo("Any unknown column");
        assertThat(subTitles.get(0).value()).isEqualTo(IntCell.TYPE.toPrettyString());
        assertThat(subTitles.get(1).value()).isEqualTo(StringCell.TYPE.toPrettyString());
        assertThat(subTitles.get(2).value()).isEqualTo("Default columntype");
    }

    @Test
    void testElementReset() {
        simulateSetTransformationElementSettings();
        final var simulatorResult = m_simulator.simulateButtonClick(ArrayWidgetInternal.ElementResetButton.class);
        final var typeReset = getMultiResultInTransformationElementSettings(simulatorResult, "productionPath");
        final var columnNameReset = getMultiResultInTransformationElementSettings(simulatorResult, "columnRename");
        assertSizeAndIndices(typeReset, 3);
        assertSizeAndIndices(columnNameReset, 3);
        assertThat(typeReset.get(0).value()).isEqualTo(getDefaultPathIdentifier(getIntType()));
        assertThat(typeReset.get(1).value()).isEqualTo(getDefaultPathIdentifier(getStringType()));
        assertThat(typeReset.get(2).value()).isEqualTo(DEFAULT_COLUMNTYPE);
        assertThat(columnNameReset.get(0).value()).isEqualTo(INT_COL);
        assertThat(columnNameReset.get(1).value()).isEqualTo(STRING_COL);
        assertThat(columnNameReset.get(2).value()).isNull();
    }

    private List<IndexedValue<Integer>> getMultiResultInTransformationElementSettings(
        final UpdateSimulatorResult simulatorResult, final String fieldName) {
        return simulatorResult.getMultiValueUpdatesInArrayAt(List
            .of(Arrays.asList(combineWithPathToTransformationSettings("columnTransformation")), List.of(fieldName)));
    }

    private static void assertSizeAndIndices(final List<IndexedValue<Integer>> listOfIndexedValues, final int expectedSize) {
        assertThat(listOfIndexedValues).hasSize(expectedSize);
        for (int i = 0; i < expectedSize; i++) {
            assertThat(listOfIndexedValues.get(i).indices()).isEqualTo(List.of(i));
        }
    }

    private UpdateSimulatorResult getSimulatorResultForUpdatesInElementSettingsArray() {
        final var setElementSettings = simulateSetTransformationElementSettings();
        return setElementSettings.apply(m_simulator);
    }

    private Function<UpdateSimulator, UpdateSimulatorResult> simulateSetTransformationElementSettings() {
        final var transformationElements = getUpdatedTransformationElementSettings();
        return setTransformationElementSettings(getTransformationSettings(), transformationElements,
            this::combineWithPathToTransformationSettings);
    }

    private static List<StringChoice> typeChoices(final DataType type) {
        final var productionPaths = getProductionPathProvider().getAvailableProductionPaths(type);
        return productionPaths.stream()
            .map(path -> new StringChoice(ProductionPathUtils.getPathIdentifier(path, getProductionPathSerializer()),
                path.getDestinationType().toPrettyString()))
            .sorted(Comparator.comparing(StringChoice::text)).toList();
    }

    private TableSpecSettings[] getTableSpecsValueUpdate(final UpdateSimulatorResult simulatorResult) {
        return (TableSpecSettings[])simulatorResult.getValueUpdateAt(combineWithPathToTransformationSettings("specs"));
    }

    private TransformationElementSettings[]
        getTransformationElementsValueUpdate(final UpdateSimulatorResult simulatorResult) {
        return (TransformationElementSettings[])simulatorResult
            .getValueUpdateAt(combineWithPathToTransformationSettings("columnTransformation"));
    }

    private String[] combineWithPathToTransformationSettings(final String... nestedFields) {
        return Stream.concat(getPathToTransformationSettings().stream(), Arrays.stream(nestedFields))
            .toArray(String[]::new);
    }

    // --- End inlined code ---

    private static void setColumnFilterMode(final TableManipulatorNodeParameters settings,
        final TableManipulatorParameters.ColumnFilterModeOption columnFilterMode) {
        settings.m_parameters.m_columnFilterMode = columnFilterMode;
    }

    private static TableManipulatorTransformationParameters
        getTransformationSettings(final TableManipulatorNodeParameters params) {
        return params.m_transformationParameters;
    }

    private static ProductionPathProvider<DataType> getProductionPathProvider() {
        return TableManipulatorSpecific.getProductionPathProvider();
    }

    private static ProductionPathSerializer getProductionPathSerializer() {
        return new TableManipulatorTransformationParameters().getProductionPathSerializer();
    }

    private static List<String> getPathToTransformationSettings() {
        return List.of("transformationParameters");
    }

    private static TableManipulatorNodeParameters constructNewSettings() {
        return new TableManipulatorNodeParameters();
    }

    public static void setExistingTableSpecs(final TableManipulatorTransformationParameters transformationSettings,
        final List<String> columnNames, final List<String> columnTypes) {
        final var columnSpecSettings = IntStream.range(0, columnNames.size())
            .mapToObj(i -> new ColumnSpecSettings(columnNames.get(i), columnTypes.get(i), true))
            .toArray(ColumnSpecSettings[]::new);
        transformationSettings.m_specs = new TableSpecSettings[]{new TableSpecSettings("0", columnSpecSettings)};
    }

    private static void setTransformationElementSettingsWithUnknown(
        final TableManipulatorTransformationParameters transformationSettings, final DataType unknownColumnsDataType) {

        final var unknownElement = TransformationElementSettings.createUnknownElement();
        unknownElement.m_includeInOutput = false;
        unknownElement.m_productionPath = DataTypeSerializer.typeToString(unknownColumnsDataType);

        transformationSettings.m_columnTransformation =
            new TransformationElementSettings[]{createDummyElement("previousColumn"), unknownElement};
    }

    private static void setTransformationElementSettingsWithExisting(
        final TableManipulatorTransformationParameters transformationSettings, final String[] columnNames,
        final boolean[] includeInOutputs, final String[] columnRenames, final String[] types,
        final String[] originalTypes) {
        transformationSettings.m_columnTransformation = IntStream
            .range(0, columnNames.length).mapToObj(i -> new TransformationElementSettings(columnNames[i],
                includeInOutputs[i], columnRenames[i], types[i], originalTypes[i], null))
            .toArray(TransformationElementSettings[]::new);

    }

    private static Function<UpdateSimulator, UpdateSimulatorResult> setTransformationElementSettings(
        final TableManipulatorTransformationParameters transformationSettings,
        final Object transformationElementSettings, final UnaryOperator<String[]> combineWithPathToTableSpecSettings) {
        transformationSettings.m_columnTransformation = (TransformationElementSettings[])transformationElementSettings;
        return simulator -> simulator
            .simulateValueChange(combineWithPathToTableSpecSettings.apply(new String[]{"specs"}));
    }

    private static TransformationElementSettings createDummyElement(final String name) {
        return new TransformationElementSettings(name, true, null, null, null, null);
    }
}
