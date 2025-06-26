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
 *   Oct 1, 2024 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.webui.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviderTestUtils.assertTableSpec;
import static org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviderTestUtils.assertTransformationElementSettings;
import static org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviderTestUtils.setTransformationElementSettings;
import static org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviderTestUtils.setTransformationElementSettingsWithExisting;
import static org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviderTestUtils.setTransformationElementSettingsWithUnknown;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.knime.base.node.io.filehandling.webui.LocalWorkflowContextTest;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.AdvancedSettingsWithMultipleFileHandling.HowToCombineColumnsOption;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.TypeChoicesProvider;
import org.knime.base.node.io.filehandling.webui.reader.ReaderSpecific.ExternalDataTypeSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.IndexedValue;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.StringChoice;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.testing.node.dialog.updates.DialogUpdateSimulator;
import org.knime.testing.node.dialog.updates.UpdateSimulator;
import org.knime.testing.node.dialog.updates.UpdateSimulator.UpdateSimulatorResult;

/**
 * Extend this test in order to test the reader-dependent state providers in the transformation settings.
 *
 * @author Paul Bärnreuther
 * @param <R> the type of the settings
 * @param <S> the type to which external data types are serialized
 * @param <T> the type by which external data types are identified
 */
@SuppressWarnings("restriction")
abstract class CommonReaderTransformationSettingsUpdatesTest<R extends WidgetGroup, S, T>
    extends LocalWorkflowContextTest {
    @TempDir
    Path m_tempFolder;

    /**
     * protected for modification within specialized tests.
     */
    protected R m_settings;

    /**
     * protected for adapting the file within specialized tests.
     */
    protected String m_filePath;

    /**
     * This simulator is initialized with {@link #m_settings}. Modifying the settings and then invoking a simulator will
     * work as expected.
     */
    protected UpdateSimulator m_simulator;

    /**
     * @return the initial value of {@link #m_settings}
     */
    protected abstract R constructNewSettings();

    /**
     * @param settings
     * @return the common reader settings within the settings
     */
    protected abstract CommonReaderNodeSettings.SettingsWithRowId getSettings(R settings);

    private CommonReaderNodeSettings.SettingsWithRowId getSettings() {
        return getSettings(m_settings);
    }

    /**
     * @param settings
     * @return the advanced settings within the settings
     */
    protected abstract CommonReaderNodeSettings.AdvancedSettingsWithMultipleFileHandling
        getAdvancedSettings(R settings);

    private CommonReaderNodeSettings.AdvancedSettingsWithMultipleFileHandling getAdvancedSettings() {
        return getAdvancedSettings(m_settings);
    }

    /**
     * @param settings
     * @return the transformation settings within the settings
     */
    protected abstract CommonReaderTransformationSettings<?, S> getTransformationSettings(R settings);

    private CommonReaderTransformationSettings<?, S> getTransformationSettings() {
        return getTransformationSettings(m_settings);
    }

    /**
     * When this method was called, a future read of the file at the file path should result in a spec with an integer
     * column "intCol" followed by a string column "stringCol".
     *
     * @param filePath the path to the file that should be written to / will be read from.
     *
     * @throws IOException
     */
    protected abstract void writeFileWithIntegerAndStringColumn(String filePath) throws IOException;

    private void writeFileWithIntegerAndStringColumn() throws IOException {
        writeFileWithIntegerAndStringColumn(m_filePath);
    }

    /**
     * Optionally override this method to provide a reader specific file name (usually not necessary).
     *
     * @return the name of the file that will be written to / read from.
     */
    protected String getFileName() {
        return "testFile";
    }

    abstract ExternalDataTypeSerializer<S, T> getExternalDataTypeSerializer();

    @BeforeEach
    void setUpSettingsAndFile() {
        m_settings = constructNewSettings();
        m_filePath = m_tempFolder.resolve(getFileName()).toAbsolutePath().toString();
        getSettings().m_source.m_path = new FSLocation(FSCategory.LOCAL, m_filePath);
        m_simulator = new DialogUpdateSimulator(m_settings, null);
    }

    @ParameterizedTest
    @ArgumentsSource(OnFileChooserChangeOrAfterDialogOpened.class)
    void testSourceIdAndFsLocationProvider(final Function<UpdateSimulator, UpdateSimulatorResult> simulate) {
        final var simulatorResult = simulate.apply(m_simulator);
        final var sourceId = getSourceIdVaueUpdate(simulatorResult);
        final var fsLocations = getFsLocationsValueUpdate(simulatorResult);

        assertThat(sourceId).isEqualTo(m_filePath);
        assertThat(fsLocations).isEqualTo(new FSLocation[0]);
    }

    @ParameterizedTest
    @ArgumentsSource(OnFileChooserChangeOrAfterDialogOpened.class)
    void testFsLocationProviderWithPresentFile(final Function<UpdateSimulator, UpdateSimulatorResult> simulate)
        throws IOException {
        writeFileWithIntegerAndStringColumn();
        final var simulatorResult = simulate.apply(m_simulator);
        final var fsLocations = getFsLocationsValueUpdate(simulatorResult);

        assertThat(fsLocations).isEqualTo(new FSLocation[]{new FSLocation(FSCategory.LOCAL, m_filePath)});
    }

    @ParameterizedTest
    @ArgumentsSource(OnFileChooserChangeOrAfterDialogOpened.class)
    void testTableSpecSettingsProvider(final Function<UpdateSimulator, UpdateSimulatorResult> simulate)
        throws IOException {
        writeFileWithIntegerAndStringColumn();

        final var simulatorResult = simulate.apply(m_simulator);
        final var specs = getSpecsValueUpdate(simulatorResult);

        assertIntegerAndStringColumn(specs);

    }

    @Test
    void textTableSpecSettingsProviderOnConfigIdChange() throws IOException {
        writeFileWithIntegerAndStringColumn();

        final var simulatorResult =
            m_simulator.simulateValueChange(combineWithPathToTransformationSettings("persistorSettings", "configId"));
        final var specs = getSpecsValueUpdate(simulatorResult);

        assertIntegerAndStringColumn(specs);
    }

    protected void assertIntegerAndStringColumn(final Object specs) {
        final var serializer = getExternalDataTypeSerializer();
        assertTableSpec(specs, m_filePath, new String[]{"intCol", "stringCol"},
            List.of(serializer.toSerializableType(getIntType()), serializer.toSerializableType(getStringType())));
    }

    @ParameterizedTest
    @ArgumentsSource(OnFileChooserChangeOrAfterDialogOpened.class)
    void testTransformationElementSettingsProvider(final Function<UpdateSimulator, UpdateSimulatorResult> simulate)
        throws IOException {
        writeFileWithIntegerAndStringColumn();

        final var transformationElements = getUpdatedTransformationElementSettings(simulate);

        assertStandardTransformationElementSettings(transformationElements);
    }

    @Test
    void testTransformationElementSettingsProviderOnConfigIdChange() throws IOException {
        writeFileWithIntegerAndStringColumn();

        final var simulatorResult =
            m_simulator.simulateValueChange(combineWithPathToTransformationSettings("persistorSettings", "configId"));
        final var transformationElements = getTransformationElementsValueUpdate(simulatorResult);

        assertStandardTransformationElementSettings(transformationElements);
    }

    @ParameterizedTest
    @ArgumentsSource(HowToCombineColumnsOptionArgumentsProvider.class)
    void testTransformationElementSettingsProviderHowToCombineColumnsOption(
        final HowToCombineColumnsOption howToCombineColumns) throws IOException {
        getAdvancedSettings().m_howToCombineColumns = howToCombineColumns;
        testTransformationElementSettingsProvider(UpdateSimulator::simulateAfterOpenDialog);
    }

    static final class HowToCombineColumnsOptionArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext context) throws Exception {
            return getOptions().map(Arguments::of);
        }

        private static Stream<HowToCombineColumnsOption> getOptions() {
            return Stream.of(HowToCombineColumnsOption.UNION, HowToCombineColumnsOption.INTERSECTION);
        }
    }

    private void assertStandardTransformationElementSettings(final Object transformationElements) {
        assertTransformationElementSettings(transformationElements, new String[]{"intCol", "stringCol", null},
            new boolean[]{true, true, true}, new String[]{"intCol", "stringCol", null},
            new String[]{getDefaultPathIdentifier(getIntType()), getDefaultPathIdentifier(getStringType()),
                "<default-columntype>"});
    }

    @Test
    void testTransformationElementSettingsProviderUnknownColumns() throws IOException {
        setTransformationElementSettingsWithUnknown(getTransformationSettings(), StringCell.TYPE);
        writeFileWithIntegerAndStringColumn();

        final var transformationElements = getUpdatedTransformationElementSettings();

        assertTransformationElementSettings(transformationElements, new String[]{"intCol", "stringCol", null},
            new boolean[]{false, false, false}, new String[]{"intCol", "stringCol", null}, //
            new String[]{ //
                getPathIdentifier(getIntType(), StringCell.TYPE), //
                getDefaultPathIdentifier(getStringType()), //
                DataTypeStringSerializer.typeToString(StringCell.TYPE) //
            }, new String[]{ //
                getDefaultPathIdentifier(getIntType()), //
                getDefaultPathIdentifier(getStringType()), //
                "<default-columntype>" //
            });

    }

    @Test
    void testTransformationElementSettingsProviderUnknownColumnsWithUnreachableType() throws IOException {
        final var pair = getUnreachableType();
        final var unreachableType = pair.getFirst();
        setTransformationElementSettingsWithUnknown(getTransformationSettings(), unreachableType);
        writeFileWithIntegerAndStringColumn();

        final var transformationElements = getUpdatedTransformationElementSettings();

        assertTransformationElementSettings(transformationElements, new String[]{"intCol", "stringCol", null},
            new boolean[]{false, false, false}, new String[]{"intCol", "stringCol", null}, //
            new String[]{ //
                getTypeIdentifier(IntOrString.INT, pair.getSecond(), unreachableType), //
                getTypeIdentifier(IntOrString.STRING, pair.getSecond(), unreachableType), //
                DataTypeStringSerializer.typeToString(unreachableType) //
            }, new String[]{ //
                getDefaultPathIdentifier(getIntType()), //
                getDefaultPathIdentifier(getStringType()), //
                "<default-columntype>" //
            });

    }

    String getTypeIdentifier(final IntOrString intOrString, final Collection<IntOrString> unReachableFrom,
        final DataType unreachableType) {
        if (unReachableFrom.contains(intOrString)) {
            return getDefaultPathIdentifier(getDataType(intOrString));
        }
        return getPathIdentifier(getDataType(intOrString), unreachableType);
    }

    public enum IntOrString {
            INT, STRING;

    }

    T getDataType(final IntOrString intOrString) {
        switch (intOrString) {
            case INT:
                return getIntType();
            case STRING:
                return getStringType();
            default:
                throw new IllegalArgumentException("Unknown type");
        }
    }

    /**
     * @return a pair. The second element is the collection of types the first element can not be converted to. The data
     *         type must be available in the production path provider.
     */
    protected abstract Pair<DataType, Collection<IntOrString>> getUnreachableType();

    @Test
    void testTransformationElementSettingsProviderExistingColumns() throws IOException {

        final var externalDataTypeSerializer = getExternalDataTypeSerializer();
        CommonReaderTransformationSettingsStateProviderTestUtils.setExistingTableSpecs(getTransformationSettings(),
            List.of("intCol", "stringCol"), List.of(externalDataTypeSerializer.toSerializableType(getDoubleType()),
                externalDataTypeSerializer.toSerializableType(getStringType())));

        // Previously
        setTransformationElementSettingsWithExisting(getTransformationSettings(),
            // there were two columns stringCol and intCol (in that order)
            new String[]{"stringCol", "intCol"},
            // that were both not included
            new boolean[]{false, false},
            // and both renamed
            new String[]{"Renamed stringCol", "Renamed intCol"},
            // and both transformed to a different type.
            new String[]{//
                getPathIdentifier(getStringType(), XMLCell.TYPE), //
                getPathIdentifier(getDoubleType(), DoubleCell.TYPE) //
            }, //
            // Note that intCol was previously a double.
            new String[]{ //
                getDefaultPathIdentifier(getStringType()), //
                getDefaultPathIdentifier(getDoubleType())//
            });
        /**
         * We write intCol and stringCol to a file. Here intCol will not match with the previous column, because it does
         * not share the type.
         */
        writeFileWithIntegerAndStringColumn();

        final var transformationElements = getUpdatedTransformationElementSettings();

        assertTransformationElementSettings(transformationElements,
            // The order is different to the one in the written csv, since intCol is unknown.
            new String[]{"stringCol", "intCol", null},
            // stringCol is still false, intCol is an unknown column and thus the previous boolean is disregarded
            new boolean[]{false, true, true},
            // Same here: Previously, intCol was also renamed
            new String[]{"Renamed stringCol", "intCol", null},
            // Same here: Previously, the intCol type was DoubleCell
            new String[]{ //
                getPathIdentifier(getStringType(), XMLCell.TYPE), //
                getDefaultPathIdentifier(getIntType()), //
                "<default-columntype>"//
            }, //
            new String[]{//
                getDefaultPathIdentifier(getStringType()), //
                getDefaultPathIdentifier(getIntType()), //
                "<default-columntype>"//
            });
    }

    abstract T getIntType();

    abstract T getStringType();

    abstract T getDoubleType();

    private String getDefaultPathIdentifier(final T typeClass) {
        return getProductionPathProvider().getDefaultProductionPath(typeClass).getConverterFactory().getIdentifier();
    }

    private String getPathIdentifier(final T typeClass, final DataType targetDataType) {
        return getProductionPathProvider().getAvailableProductionPaths(typeClass).stream()
            .filter(path -> path.getDestinationType().equals(targetDataType)).findFirst().orElseThrow()
            .getConverterFactory().getIdentifier();
    }

    protected abstract ProductionPathProvider<T> getProductionPathProvider();

    private Object getUpdatedTransformationElementSettings() {
        return getUpdatedTransformationElementSettings(UpdateSimulator::simulateAfterOpenDialog);
    }

    private Object
        getUpdatedTransformationElementSettings(final Function<UpdateSimulator, UpdateSimulatorResult> simulate) {
        final var simulatorResult = simulate.apply(m_simulator);
        return getTransformationElementsValueUpdate(simulatorResult);
    }

    @Test
    void testTypeChoicesProvider() throws IOException {
        writeFileWithIntegerAndStringColumn();

        final var simulatorResult = getSimulatorResultForUpdatesInElementSettingsArray();
        final var typeChoicesProviderResult = simulatorResult.getMultiUiStateUpdateAt(
            List.of(List.of(combineWithPathToTransformationSettings("columnTransformation")), List.of("type")),
            "possibleValues");

        assertSizeAndIndices(typeChoicesProviderResult, 3);
        assertThat(typeChoicesProviderResult.get(0).value()).isEqualTo(typeChoices(getIntType()));
        assertThat(typeChoicesProviderResult.get(1).value()).isEqualTo(typeChoices(getStringType()));
        assertThat(((List<StringChoice>)typeChoicesProviderResult.get(2).value()).get(0).id())
            .isEqualTo("<default-columntype>");

    }

    @Test
    void testTitlesAndSubTitles() throws IOException {
        final var simulatorResult = getSimulatorResultForUpdatesInElementSettingsArray();
        final var titles = simulatorResult.getMultiUiStateUpdateAt(
            List.of(List.of(combineWithPathToTransformationSettings("columnTransformation"))), "arrayElementTitle");
        final var subTitles = simulatorResult.getMultiUiStateUpdateAt(
            List.of(List.of(combineWithPathToTransformationSettings("columnTransformation"))), "elementSubTitle");
        assertSizeAndIndices(titles, 3);
        assertSizeAndIndices(subTitles, 3);
        assertThat(titles.get(0).value()).isEqualTo("intCol");
        assertThat(titles.get(1).value()).isEqualTo("stringCol");
        assertThat(titles.get(2).value()).isEqualTo("Any unknown column");
        assertThat(subTitles.get(0).value()).isEqualTo(IntCell.TYPE.toPrettyString());
        assertThat(subTitles.get(1).value()).isEqualTo(StringCell.TYPE.toPrettyString());
        assertThat(subTitles.get(2).value()).isEqualTo("Default columntype");
    }

    @Test
    void testElementReset() throws IOException {
        simulateSetTransformationElementSettings();
        final var simulatorResult = m_simulator.simulateButtonClick(ArrayWidgetInternal.ElementResetButton.class);
        final var typeReset = getMultiResultInTransformationElementSettings(simulatorResult, "type");
        final var columnNameReset = getMultiResultInTransformationElementSettings(simulatorResult, "columnRename");

        assertSizeAndIndices(typeReset, 3);
        assertSizeAndIndices(columnNameReset, 3);
        assertThat(typeReset.get(0).value()).isEqualTo(getDefaultPathIdentifier(getIntType()));
        assertThat(typeReset.get(1).value()).isEqualTo(getDefaultPathIdentifier(getStringType()));
        assertThat(typeReset.get(2).value()).isEqualTo("<default-columntype>");
        assertThat(columnNameReset.get(0).value()).isEqualTo("intCol");
        assertThat(columnNameReset.get(1).value()).isEqualTo("stringCol");
        assertThat(columnNameReset.get(2).value()).isNull();
    }

    private List<IndexedValue<Integer>> getMultiResultInTransformationElementSettings(
        final UpdateSimulatorResult simulatorResult, final String fieldName) {
        final var typeReset = simulatorResult.getMultiValueUpdatesInArrayAt(List
            .of(Arrays.asList(combineWithPathToTransformationSettings("columnTransformation")), List.of(fieldName)));
        return typeReset;
    }

    void assertSizeAndIndices(final List<IndexedValue<Integer>> listOfIndexedValues, final int expectedSize) {
        assertThat(listOfIndexedValues).hasSize(expectedSize);

        for (int i = 0; i < expectedSize; i++) {
            assertThat(listOfIndexedValues.get(i).indices()).isEqualTo(List.of(i));
        }
    }

    private UpdateSimulatorResult getSimulatorResultForUpdatesInElementSettingsArray() throws IOException {
        final var setElementSettings = simulateSetTransformationElementSettings();
        final var simulatorResult = setElementSettings.apply(m_simulator);
        return simulatorResult;
    }

    private Function<UpdateSimulator, UpdateSimulatorResult> simulateSetTransformationElementSettings()
        throws IOException {
        writeFileWithIntegerAndStringColumn();
        final var transformationElements = getUpdatedTransformationElementSettings();

        return setTransformationElementSettings(getTransformationSettings(), transformationElements,
            this::combineWithPathToTransformationSettings);

    }

    protected abstract Class<? extends TypeChoicesProvider<T>> getTypeChoicesProviderClass();

    private List<StringChoice> typeChoices(final T type) {
        final var productionPaths = getProductionPathProvider().getAvailableProductionPaths(type);
        return productionPaths.stream().map(path -> new StringChoice(path.getConverterFactory().getIdentifier(),
            path.getDestinationType().toPrettyString())).toList();
    }

    protected Object getSpecsValueUpdate(final UpdateSimulatorResult simulatorResult) {
        return simulatorResult.getValueUpdateAt(combineWithPathToTransformationSettings("persistorSettings", "specs"));

    }

    private String getSourceIdVaueUpdate(final UpdateSimulatorResult simulatorResult) {
        return (String)simulatorResult
            .getValueUpdateAt(combineWithPathToTransformationSettings("persistorSettings", "sourceId"));
    }

    private Object getFsLocationsValueUpdate(final UpdateSimulatorResult simulatorResult) {
        return simulatorResult
            .getValueUpdateAt(combineWithPathToTransformationSettings("persistorSettings", "fsLocations"));
    }

    protected Object getTransformationElementsValueUpdate(final UpdateSimulatorResult simulatorResult) {
        return simulatorResult.getValueUpdateAt(combineWithPathToTransformationSettings("columnTransformation"));

    }

    private String[] combineWithPathToTransformationSettings(final String... nestedFields) {
        return Stream.concat(getPathToTransformationSettings().stream(), Arrays.stream(nestedFields))
            .toArray(String[]::new);

    }

    /**
     * @return the names of the fields leading to the transformation settings in the settings object
     */
    protected abstract List<String> getPathToTransformationSettings();

    // an abstract class that I can use to deduplicat the two classes right above:
    abstract static class RunSimulationForCommonSpecChangesProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext context) throws Exception {
            return getSimulations().map(Arguments::of);
        }

        abstract Stream<Function<UpdateSimulator, UpdateSimulatorResult>> getSimulations();
    }

    static final class OnFileChooserChangeOrAfterDialogOpened extends RunSimulationForCommonSpecChangesProvider {
        @Override
        Stream<Function<UpdateSimulator, UpdateSimulatorResult>> getSimulations() {
            return Stream.of(UpdateSimulator::simulateAfterOpenDialog,
                simulator -> simulator.simulateValueChange(new String[]{"settings", "source"}));
        }
    }

}
