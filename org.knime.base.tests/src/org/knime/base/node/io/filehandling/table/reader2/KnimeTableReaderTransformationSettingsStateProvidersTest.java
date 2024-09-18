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
 *   May 28, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.table.reader2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.PRODUCTION_PATH_PROVIDER;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.ColumnSpecSettings;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.TransformationElementSettings;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.TransformationElementSettings.ColumnNameRef;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.TransformationElementSettingsReference;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettingsStateProviders.DependenciesProvider;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettingsStateProviders.FSLocationsProvider;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettingsStateProviders.SourceIdProvider;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettingsStateProviders.TableSpecSettingsProvider;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettingsStateProviders.TransformationElementSettingsProvider;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettingsStateProviders.TypeChoicesProvider;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettingsStateProviders.TypedReaderTableSpecsProvider;
import org.knime.base.node.io.filehandling.webui.LocalWorkflowContextTest;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.AdvancedSettings.HowToCombineColumnsOption;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.AdvancedSettings.HowToCombineColumnsOptionRef;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.Settings.FileChooserRef;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ButtonReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider.StateProviderInitializer;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings({"unchecked", "restriction", "static-method"})
final class KnimeTableReaderTransformationSettingsStateProvidersTest extends LocalWorkflowContextTest {

    @TempDir
    Path m_tempFolder;

    public abstract static class NoopStateProviderInitializer implements StateProviderInitializer {
        @Override
        public <T> Supplier<T> getValueSupplier(final Class<? extends Reference<T>> ref) {
            throw new IllegalAccessError("Should not be called within this test");
        }

        @Override
        public <T> void computeOnValueChange(final Class<? extends Reference<T>> ref) {
            throw new IllegalAccessError("Should not be called within this test");
        }

        @Override
        public void computeOnButtonClick(final Class<? extends ButtonReference> ref) {
            throw new IllegalAccessError("Should not be called within this test");
        }

        @Override
        public <T> Supplier<T> computeFromValueSupplier(final Class<? extends Reference<T>> ref) {
            throw new IllegalAccessError("Should not be called within this test");
        }

        @Override
        public <T> Supplier<T> computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
            throw new IllegalAccessError("Should not be called within this test");
        }

        @Override
        public void computeBeforeOpenDialog() {
            throw new IllegalAccessError("Should not be called within this test");
        }

        @Override
        public void computeAfterOpenDialog() {
            throw new IllegalAccessError("Should not be called within this test");
        }
    }

    private static KnimeTableReaderNodeSettings initSettings() {
        final var settings = new KnimeTableReaderNodeSettings();
        settings.m_settings.m_source.m_path = new FSLocation(FSCategory.LOCAL, "foo");
        return settings;
    }

    private static KnimeTableReaderNodeSettings createDefaultSettingsAndWriteKnimeTable(final String file)
        throws IOException {
        createTableFile(file);
        final var settings = new KnimeTableReaderNodeSettings();
        settings.m_settings.m_source.m_path = new FSLocation(FSCategory.LOCAL, file);
        return settings;
    }

    static void createTableFile(final String file) throws IOException {
        final var spec = new DataTableSpec(new DataColumnSpecCreator("intCol", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("stringCol", StringCell.TYPE).createSpec());
        final var cont = new DataContainer(spec);
        for (int i = 0; i < 2; i++) {
            final var s = Integer.toString(i);
            cont.addRowToTable(new DefaultRow(s, new IntCell(i), new StringCell(s)));
        }
        cont.close();

        try (var table = cont.getCloseableTable()) {
            DataContainer.writeToZip(table, new File(file), new ExecutionMonitor());
        } catch (CanceledExecutionException e) {
            // do nothing
        }
    }

    private static DependenciesProvider createDependenciesProvider(final KnimeTableReaderNodeSettings settings) {
        final var dependenciesProvider = new DependenciesProvider();
        dependenciesProvider.init(getDependenciesProviderInitializer(settings));
        return dependenciesProvider;
    }

    @Test
    void testDependenciesProvider() {
        final var settings = initSettings();
        final var dependencies = createDependenciesProvider(settings).computeState(null);
        assertThat(dependencies.m_source).isEqualTo(settings.m_settings.m_source);
    }

    private static final StateProviderInitializer
        getDependenciesProviderInitializer(final KnimeTableReaderNodeSettings settings) {
        return new NoopStateProviderInitializer() {
            @Override
            public <T> Supplier<T> getValueSupplier(final Class<? extends Reference<T>> ref) {
                if (ref.equals(FileChooserRef.class)) {
                    return () -> (T)settings.m_settings.m_source;
                }
                throw new IllegalStateException(String.format("Unexpected dependency %s", ref.getSimpleName()));
            }
        };
    }

    @Test
    void testSourceIdProvider() {
        final var settings = initSettings();
        final var sourceIdProvider = new SourceIdProvider();
        sourceIdProvider.init(getFileChooserRefDependentInitializer(createDependenciesProvider(settings)));

        assertThat(sourceIdProvider.computeState(null))
            .isEqualTo(settings.m_settings.m_source.getFSLocation().getPath());
    }

    private static final StateProviderInitializer
        getFileChooserRefDependentInitializer(final DependenciesProvider dependenciesProvider) {
        return new NoopStateProviderInitializer() {

            @Override
            public <T> void computeOnValueChange(final Class<? extends Reference<T>> ref) {
                if (ref.equals(FileChooserRef.class)) {
                    // Do nothing
                } else {
                    throw new IllegalStateException(String.format("Unexpected dependency %s", ref.getSimpleName()));
                }
            }

            @Override
            public <T> Supplier<T>
                computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
                if (stateProviderClass.equals(DependenciesProvider.class)) {
                    return () -> (T)dependenciesProvider.computeState(null);
                }
                throw new IllegalStateException(
                    String.format("Unexpected dependency %s", stateProviderClass.getSimpleName()));
            }

            @Override
            public void computeAfterOpenDialog() {
                // Do nothing
            }
        };
    }

    @Test
    void testFSLocationsProvider() throws IOException {
        final var file = m_tempFolder.resolve("file.KnimeTableReader").toAbsolutePath().toString();
        final var settings = createDefaultSettingsAndWriteKnimeTable(file);
        final var fsLocationsProvider = new FSLocationsProvider();
        fsLocationsProvider.init(getFileChooserRefDependentInitializer(createDependenciesProvider(settings)));

        assertThat(fsLocationsProvider.computeState(null))
            .isEqualTo(new FSLocation[]{new FSLocation(FSCategory.LOCAL, file)});
    }

    @Test
    void testTypedReaderTableSpecsProvider() throws IOException {
        final var file = m_tempFolder.resolve("file.KnimeTableReader").toAbsolutePath().toString();

        final var typedReaderTableSpecsProvider = createTypedReaderTableSpecsProvider(file);
        final var specs = typedReaderTableSpecsProvider.computeState(null);

        assertTypedReaderTableSpecs(file, specs);
    }

    static TypedReaderTableSpecsProvider createTypedReaderTableSpecsProvider(final String file) throws IOException {
        final var settings = createDefaultSettingsAndWriteKnimeTable(file);
        final var typedReaderTableSpecsProvider = new TypedReaderTableSpecsProvider();
        typedReaderTableSpecsProvider
            .init(getTypedReaderTableSpecProviderStateProviderInitializer(createDependenciesProvider(settings)));
        return typedReaderTableSpecsProvider;
    }

    private static final StateProviderInitializer
        getTypedReaderTableSpecProviderStateProviderInitializer(final DependenciesProvider dependenciesProvider) {
        return new NoopStateProviderInitializer() {
            @Override
            public <T> Supplier<T>
                computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
                if (stateProviderClass.equals(DependenciesProvider.class)) {
                    return () -> (T)dependenciesProvider.computeState(null);
                }
                throw new IllegalStateException(
                    String.format("Unexpected dependency %s", stateProviderClass.getSimpleName()));
            }
        };
    }

    private void assertTypedReaderTableSpecs(final String file,
        final Map<String, TypedReaderTableSpec<DataType>> specs) {
        assertThat(specs).containsKey(file);
        assertThat(specs).size().isEqualTo(1);

        final var spec = specs.get(file);
        assertThat(spec.size()).isEqualTo(2);

        final var col1 = spec.getColumnSpec(0);
        assertThat(col1.getName()).isPresent().hasValue("intCol");
        assertThat(col1.hasType()).isTrue();
        assertThat(col1.getType()).isEqualTo(IntCell.TYPE);

        final var col2 = spec.getColumnSpec(1);
        assertThat(col2.getName()).isPresent().hasValue("stringCol");
        assertThat(col2.hasType()).isTrue();
        assertThat(col2.getType()).isEqualTo(StringCell.TYPE);
    }

    @Test
    void testTableSpecSettingsProvider() throws IOException, InvalidSettingsException {
        final var file = m_tempFolder.resolve("file.KnimeTableReader").toAbsolutePath().toString();
        final var typedReaderTableSpecsProvider = createTypedReaderTableSpecsProvider(file);

        final var tableSpecSettingsProvider = new TableSpecSettingsProvider();
        tableSpecSettingsProvider.init(getTableSpecSettingsProviderInitializer(typedReaderTableSpecsProvider));
        final var specs = tableSpecSettingsProvider.computeState(null);

        assertThat(specs).hasSize(1);
        assertThat(specs[0].m_spec).hasSize(2);
        assertThat(specs[0].m_sourceId).isEqualTo(file);

        assertThat(specs[0].m_spec[0].m_name).isEqualTo("intCol");
        assertThat(ColumnSpecSettings.stringToType(specs[0].m_spec[0].m_type)).isEqualTo(IntCell.TYPE);

        assertThat(specs[0].m_spec[1].m_name).isEqualTo("stringCol");
        assertThat(ColumnSpecSettings.stringToType(specs[0].m_spec[1].m_type)).isEqualTo(StringCell.TYPE);
    }

    static class DependsOnTypedReaderTableSpecProviderInitializer extends NoopStateProviderInitializer {

        private final TypedReaderTableSpecsProvider m_typedReaderTableSpecsProvider;

        @Override
        public <T> void computeOnValueChange(final Class<? extends Reference<T>> ref) {
            if (ref.equals(FileChooserRef.class)) {
                // Do nothing
            } else {
                throw new IllegalStateException(String.format("Unexpected dependency %s", ref.getSimpleName()));
            }
        }

        @Override
        public <T> Supplier<T> computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
            if (stateProviderClass.equals(TypedReaderTableSpecsProvider.class)) {
                return () -> (T)m_typedReaderTableSpecsProvider.computeState(null);
            }
            throw new IllegalStateException(
                String.format("Unexpected dependency %s", stateProviderClass.getSimpleName()));
        }

        @Override
        public void computeAfterOpenDialog() {
            // Do nothing
        }

        DependsOnTypedReaderTableSpecProviderInitializer(
            final TypedReaderTableSpecsProvider typedReaderTableSpecsProvider) {
            m_typedReaderTableSpecsProvider = typedReaderTableSpecsProvider;
        }
    }

    private static final StateProviderInitializer
        getTableSpecSettingsProviderInitializer(final TypedReaderTableSpecsProvider typedReaderTableSpecsProvider) {
        return new DependsOnTypedReaderTableSpecProviderInitializer(typedReaderTableSpecsProvider);
    }

    @ParameterizedTest
    @MethodSource
    void testTransformationElementSettingsProvider(final HowToCombineColumnsOption howToCombineColumnsOption)
        throws IOException {
        final var file = m_tempFolder.resolve("file.KnimeTableReader").toAbsolutePath().toString();
        final var typedReaderTableSpecsProvider = createTypedReaderTableSpecsProvider(file);

        final var transformationElementSettingsProvider = new TransformationElementSettingsProvider();
        transformationElementSettingsProvider.init(getTransformationElementSettingsProviderInitializer(
            typedReaderTableSpecsProvider, howToCombineColumnsOption, new TransformationElementSettings[0]));

        final var transformationElements = transformationElementSettingsProvider.computeState(null);

        assertThat(transformationElements).hasSize(3);

        assertThat(transformationElements[0].m_columnName).isEqualTo("intCol");
        assertThat(transformationElements[0].m_includeInOutput).isTrue();
        assertThat(transformationElements[0].m_columnRename).isEqualTo("intCol");
        assertThat(transformationElements[0].m_type).isEqualTo(getDefaultPathIdentifier(IntCell.TYPE));

        assertThat(transformationElements[1].m_columnName).isEqualTo("stringCol");
        assertThat(transformationElements[1].m_includeInOutput).isTrue();
        assertThat(transformationElements[1].m_columnRename).isEqualTo("stringCol");
        assertThat(transformationElements[1].m_type)
            .isEqualTo(getDefaultPathIdentifier(StringCell.TYPE));

        assertThat(transformationElements[2].m_columnName).isNull();
        assertThat(transformationElements[2].m_includeInOutput).isTrue();
        assertThat(transformationElements[2].m_type).isEqualTo("<default-columntype>");
    }

    private static Stream<HowToCombineColumnsOption> testTransformationElementSettingsProvider() {
        return Stream.of(HowToCombineColumnsOption.UNION, HowToCombineColumnsOption.INTERSECTION);
    }

    @Test
    void testTransformationElementSettingsProviderUnknownColumns() throws IOException, InvalidSettingsException {
        final var file = m_tempFolder.resolve("file.KnimeTableReader").toAbsolutePath().toString();
        final var typedReaderTableSpecsProvider = createTypedReaderTableSpecsProvider(file);

        final var transformationElementSettingsProvider = new TransformationElementSettingsProvider();
        final var unknownElement = TransformationElementSettings.createUnknownElement();
        unknownElement.m_includeInOutput = false;
        unknownElement.m_type = getTypeIdentifierForUnknown(LongCell.TYPE);
        transformationElementSettingsProvider
            .init(getTransformationElementSettingsProviderInitializer(typedReaderTableSpecsProvider,
                HowToCombineColumnsOption.UNION, new TransformationElementSettings[]{//
                    createDummyElement("previousColumn"), //
                    unknownElement, //
                }));

        final var transformationElements = transformationElementSettingsProvider.computeState(null);

        assertThat(transformationElements).hasSize(3);

        assertThat(transformationElements[0].m_columnName).isEqualTo("intCol");
        assertThat(transformationElements[0].m_includeInOutput).isFalse();
        assertThat(transformationElements[0].m_columnRename).isEqualTo("intCol");
        assertThat(transformationElements[0].m_type)
            .isEqualTo(getPathIdentifier(IntCell.TYPE, LongCell.TYPE));
        assertThat(transformationElements[0].m_originalType)
            .isEqualTo(getDefaultPathIdentifier(IntCell.TYPE));

        assertThat(transformationElements[1].m_columnName).isEqualTo("stringCol");
        assertThat(transformationElements[1].m_includeInOutput).isFalse();
        assertThat(transformationElements[1].m_columnRename).isEqualTo("stringCol");
        assertThat(transformationElements[1].m_type)
            .isEqualTo(getPathIdentifier(StringCell.TYPE, LongCell.TYPE));
        assertThat(transformationElements[1].m_originalType)
            .isEqualTo(getDefaultPathIdentifier(StringCell.TYPE));

        assertThat(transformationElements[2].m_columnName).isNull();
        assertThat(transformationElements[2].m_includeInOutput).isFalse();
        assertThat(transformationElements[2].m_type).isEqualTo(getTypeIdentifierForUnknown(LongCell.TYPE));

    }

    private static TransformationElementSettings createDummyElement(final String name) {
        return new TransformationElementSettings(name, true, null, null, null, null);
    }

    @Test
    void testTransformationElementSettingsProviderExistingColumns() throws IOException {
        final var file = m_tempFolder.resolve("file.KnimeTableReader").toAbsolutePath().toString();
        final var typedReaderTableSpecsProvider = createTypedReaderTableSpecsProvider(file);

        final var transformationElementSettingsProvider = new TransformationElementSettingsProvider();
        transformationElementSettingsProvider
            .init(getTransformationElementSettingsProviderInitializer(typedReaderTableSpecsProvider,
                HowToCombineColumnsOption.UNION, new TransformationElementSettings[]{//
                    new TransformationElementSettings("stringCol", false, "Renamed stringCol",
                        getPathIdentifier(StringCell.TYPE, XMLCell.TYPE),
                        getDefaultPathIdentifier(StringCell.TYPE), "Integer"), //
                    new TransformationElementSettings("intCol", false, "Renamed intCol",
                        getPathIdentifier(DoubleCell.TYPE, DoubleCell.TYPE),
                        getDefaultPathIdentifier(DoubleCell.TYPE), "Double"), //
                }));

        final var transformationElements = transformationElementSettingsProvider.computeState(null);

        assertThat(transformationElements).hasSize(3);

        // Uses existing element since type and name match
        assertThat(transformationElements[0].m_columnName).isEqualTo("stringCol");
        assertThat(transformationElements[0].m_includeInOutput).isFalse();
        assertThat(transformationElements[0].m_columnRename).isEqualTo("Renamed stringCol");
        assertThat(transformationElements[0].m_type)
            .isEqualTo(getPathIdentifier(StringCell.TYPE, XMLCell.TYPE));
        assertThat(transformationElements[0].m_originalType)
            .isEqualTo(getDefaultPathIdentifier(StringCell.TYPE));
        assertThat(transformationElements[0].m_originalTypeLabel).isEqualTo("String");

        // Uses new created element since the type changed
        assertThat(transformationElements[1].m_columnName).isEqualTo("intCol");
        assertThat(transformationElements[1].m_includeInOutput).isTrue();
        assertThat(transformationElements[1].m_columnRename).isEqualTo("intCol");
        assertThat(transformationElements[1].m_type).isEqualTo(getDefaultPathIdentifier(IntCell.TYPE));
        assertThat(transformationElements[1].m_originalType)
            .isEqualTo(getDefaultPathIdentifier(IntCell.TYPE));
        assertThat(transformationElements[1].m_originalTypeLabel).isEqualTo("Number (integer)");

        assertThat(transformationElements[2].m_columnName).isNull();

    }

    private String getTypeIdentifierForUnknown(final DataType dataType) {
        return dataType.getName();
    }

    private String getDefaultPathIdentifier(final DataType type) {
        return PRODUCTION_PATH_PROVIDER.getDefaultProductionPath(type).getConverterFactory().getIdentifier();
    }

    private String getPathIdentifier(final DataType type, final DataType targetDataType) {
        return PRODUCTION_PATH_PROVIDER.getAvailableProductionPaths(type).stream()
            .filter(path -> path.getDestinationType().equals(targetDataType)).findFirst()
            .orElseGet(() -> PRODUCTION_PATH_PROVIDER.getDefaultProductionPath(type)).getConverterFactory()
            .getIdentifier();
    }

    private static final StateProviderInitializer getTransformationElementSettingsProviderInitializer(
        final TypedReaderTableSpecsProvider typedReaderTableSpecsProvider,
        final HowToCombineColumnsOption howToCombineColumnsOption,
        final TransformationElementSettings[] existingSettings) {
        return new DependsOnTypedReaderTableSpecProviderInitializer(typedReaderTableSpecsProvider) {

            @Override
            public <T> Supplier<T> getValueSupplier(final Class<? extends Reference<T>> ref) {
                if (ref.equals(TransformationElementSettingsReference.class)) {
                    return () -> (T)existingSettings;
                }
                throw new IllegalStateException(String.format("Unexpected dependency %s", ref.getSimpleName()));
            }

            @Override
            public <T> Supplier<T> computeFromValueSupplier(final Class<? extends Reference<T>> ref) {
                if (ref.equals(HowToCombineColumnsOptionRef.class)) {
                    return () -> (T)howToCombineColumnsOption;
                }
                throw new IllegalStateException(String.format("Unexpected dependency %s", ref.getSimpleName()));
            }
        };
    }

    @Test
    void testTypeChoicesProviderIntCol() throws IOException {
        final var file = m_tempFolder.resolve("file.KnimeTableReader").toAbsolutePath().toString();
        final var typedReaderTableSpecsProvider = createTypedReaderTableSpecsProvider(file);

        testTypeChoicesProvider(typedReaderTableSpecsProvider, "intCol", IntCell.TYPE);
        testTypeChoicesProvider(typedReaderTableSpecsProvider, "stringCol", StringCell.TYPE);
    }

    private static void testTypeChoicesProvider(final TypedReaderTableSpecsProvider typedReaderTableSpecProvider,
        final String columnName, final DataType type) {
        final var typeChoicesProvider = new TypeChoicesProvider();
        typeChoicesProvider
            .init(getTypeChoicesProviderStateProviderInitializer(columnName, typedReaderTableSpecProvider));

        final var productionPaths = PRODUCTION_PATH_PROVIDER.getAvailableProductionPaths(type);
        final var typesIdAndText = typeChoicesProvider.computeState(null);

        assertThat(typesIdAndText).hasSize(productionPaths.size());
        for (int i = 0; i < productionPaths.size(); i++) {
            assertThat(typesIdAndText[i].id()).isEqualTo(productionPaths.get(i).getConverterFactory().getIdentifier());
            assertThat(typesIdAndText[i].text())
                .isEqualTo(productionPaths.get(i).getDestinationType().toPrettyString());
        }
    }

    private static final StateProviderInitializer getTypeChoicesProviderStateProviderInitializer(
        final String columnName, final TypedReaderTableSpecsProvider typedReaderTableSpecProvider) {
        return new NoopStateProviderInitializer() {

            @Override
            public <T> Supplier<T> getValueSupplier(final Class<? extends Reference<T>> ref) {
                if (ref.equals(ColumnNameRef.class)) {
                    return () -> (T)columnName;
                }
                throw new IllegalStateException(String.format("Unexpected dependency %s", ref.getSimpleName()));
            }

            @Override
            public <T> void computeOnValueChange(final Class<? extends Reference<T>> ref) {
                // Do nothing
            }

            @Override
            public <T> Supplier<T>
                computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
                if (stateProviderClass.equals(TypedReaderTableSpecsProvider.class)) {
                    return () -> (T)typedReaderTableSpecProvider.computeState(null);
                }
                throw new IllegalStateException(
                    String.format("Unexpected dependency %s", stateProviderClass.getSimpleName()));
            }
        };
    }
}
