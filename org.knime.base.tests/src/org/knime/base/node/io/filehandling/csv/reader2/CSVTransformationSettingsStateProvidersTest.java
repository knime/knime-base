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
package org.knime.base.node.io.filehandling.csv.reader2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.PRODUCTION_PATH_PROVIDER;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.LimitMemoryPerColumnRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.MaximumNumberOfColumnsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.QuotedStringsOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.FileChooserRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.ConfigIdSettings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.PersistorSettings.ConfigIdReference;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.TransformationElementSettings.ColumnNameRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.DependenciesProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.FSLocationsProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.SourceIdProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.TableSpecSettingsProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.TransformationElementSettingsProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.TypeChoicesProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.TypedReaderTableSpecsProvider;
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
final class CSVTransformationSettingsStateProvidersTest extends LocalWorkflowContextTest {

    @TempDir
    Path m_tempFolder;

    abstract static class NoopStateProviderInitializer implements StateProviderInitializer {
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

    private static CSVTableReaderNodeSettings initSettings() {
        final var settings = new CSVTableReaderNodeSettings();
        settings.m_settings.m_source.m_path = new FSLocation(FSCategory.LOCAL, "foo");

        settings.m_settings.m_firstRowContainsColumnNames = false;
        settings.m_settings.m_firstColumnContainsRowIds = true;
        settings.m_settings.m_commentLineCharacter = "?";
        settings.m_settings.m_columnDelimiter = "\\t";
        settings.m_settings.m_quoteCharacter = "'";
        settings.m_settings.m_quoteEscapeCharacter = "'";
        settings.m_settings.m_rowDelimiterOption = RowDelimiterOption.CUSTOM;
        settings.m_settings.m_customRowDelimiter = "\\r\\n";
        settings.m_advancedSettings.m_quotedStringsOption = QuotedStringsOption.KEEP_QUOTES;
        settings.m_advancedSettings.m_replaceEmptyQuotedStringsByMissingValues = false;
        settings.m_advancedSettings.m_limitScannedRows = false;
        settings.m_advancedSettings.m_maxDataRowsScanned = 100;
        settings.m_advancedSettings.m_thousandsSeparator = ".";
        settings.m_advancedSettings.m_decimalSeparator = ",";
        settings.m_encoding.m_charset.m_fileEncoding = FileEncodingOption.OTHER;
        settings.m_encoding.m_charset.m_customEncoding = "foo";
        settings.m_limitRows.m_skipFirstLines = 1;
        settings.m_limitRows.m_skipFirstDataRows = 1;

        settings.m_advancedSettings.m_limitMemoryPerColumn = false;
        settings.m_advancedSettings.m_maximumNumberOfColumns = 1;

        return settings;
    }

    private static CSVTableReaderNodeSettings createDefaultSettingsAndWriteCSV(final String file, final String... lines)
        throws IOException {
        try (final var writer = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }

        final var settings = new CSVTableReaderNodeSettings();
        settings.m_settings.m_source.m_path = new FSLocation(FSCategory.LOCAL, file);
        return settings;
    }

    private static DependenciesProvider createDependenciesProvider(final CSVTableReaderNodeSettings settings) {
        final var dependenciesProvider = new DependenciesProvider();
        dependenciesProvider.init(getDependenciesProviderInitializer(settings));
        return dependenciesProvider;
    }

    @Test
    void testDependenciesProvider() {
        final var settings = initSettings();
        final var dependencies = createDependenciesProvider(settings).computeState(null);

        final var configId = dependencies.m_configId;
        assertThat(configId.m_firstRowContainsColumnNames).isEqualTo(settings.m_settings.m_firstRowContainsColumnNames);
        assertThat(configId.m_firstColumnContainsRowIds).isEqualTo(settings.m_settings.m_firstColumnContainsRowIds);
        assertThat(configId.m_commentLineCharacter).isEqualTo(settings.m_settings.m_commentLineCharacter);
        assertThat(configId.m_columnDelimiter).isEqualTo(settings.m_settings.m_columnDelimiter);
        assertThat(configId.m_quoteCharacter).isEqualTo(settings.m_settings.m_quoteCharacter);
        assertThat(configId.m_quoteEscapeCharacter).isEqualTo(settings.m_settings.m_quoteEscapeCharacter);
        assertThat(configId.m_rowDelimiterOption).isEqualTo(settings.m_settings.m_rowDelimiterOption);
        assertThat(configId.m_customRowDelimiter).isEqualTo(settings.m_settings.m_customRowDelimiter);
        assertThat(configId.m_quotedStringsOption).isEqualTo(settings.m_advancedSettings.m_quotedStringsOption);
        assertThat(configId.m_replaceEmptyQuotedStringsByMissingValues)
            .isEqualTo(settings.m_advancedSettings.m_replaceEmptyQuotedStringsByMissingValues);
        assertThat(configId.m_limitScannedRows).isEqualTo(settings.m_advancedSettings.m_limitScannedRows);
        assertThat(configId.m_maxDataRowsScanned).isEqualTo(settings.m_advancedSettings.m_maxDataRowsScanned);
        assertThat(configId.m_thousandsSeparator).isEqualTo(settings.m_advancedSettings.m_thousandsSeparator);
        assertThat(configId.m_decimalSeparator).isEqualTo(settings.m_advancedSettings.m_decimalSeparator);
        assertThat(configId.m_fileEncoding).isEqualTo(settings.m_encoding.m_charset.m_fileEncoding);
        assertThat(configId.m_customEncoding).isEqualTo(settings.m_encoding.m_charset.m_customEncoding);
        assertThat(configId.m_skipFirstLines).isEqualTo(settings.m_limitRows.m_skipFirstLines);
        assertThat(configId.m_skipFirstDataRows).isEqualTo(settings.m_limitRows.m_skipFirstDataRows);

        assertThat(dependencies.m_source).isEqualTo(settings.m_settings.m_source);
        assertThat(dependencies.m_limitMemoryPerColumn).isEqualTo(settings.m_advancedSettings.m_limitMemoryPerColumn);
        assertThat(dependencies.m_maximumNumberOfColumns)
            .isEqualTo(settings.m_advancedSettings.m_maximumNumberOfColumns);
    }

    private static final StateProviderInitializer
        getDependenciesProviderInitializer(final CSVTableReaderNodeSettings settings) {
        return new NoopStateProviderInitializer() {
            @Override
            public <T> Supplier<T> getValueSupplier(final Class<? extends Reference<T>> ref) {
                if (ref.equals(ConfigIdReference.class)) {
                    final var configId = new ConfigIdSettings();
                    configId.m_firstRowContainsColumnNames = settings.m_settings.m_firstRowContainsColumnNames;
                    configId.m_firstColumnContainsRowIds = settings.m_settings.m_firstColumnContainsRowIds;
                    configId.m_commentLineCharacter = settings.m_settings.m_commentLineCharacter;
                    configId.m_columnDelimiter = settings.m_settings.m_columnDelimiter;
                    configId.m_quoteCharacter = settings.m_settings.m_quoteCharacter;
                    configId.m_quoteEscapeCharacter = settings.m_settings.m_quoteEscapeCharacter;
                    configId.m_rowDelimiterOption = settings.m_settings.m_rowDelimiterOption;
                    configId.m_customRowDelimiter = settings.m_settings.m_customRowDelimiter;
                    configId.m_quotedStringsOption = settings.m_advancedSettings.m_quotedStringsOption;
                    configId.m_replaceEmptyQuotedStringsByMissingValues =
                        settings.m_advancedSettings.m_replaceEmptyQuotedStringsByMissingValues;
                    configId.m_limitScannedRows = settings.m_advancedSettings.m_limitScannedRows;
                    configId.m_maxDataRowsScanned = settings.m_advancedSettings.m_maxDataRowsScanned;
                    configId.m_thousandsSeparator = settings.m_advancedSettings.m_thousandsSeparator;
                    configId.m_decimalSeparator = settings.m_advancedSettings.m_decimalSeparator;
                    configId.m_fileEncoding = settings.m_encoding.m_charset.m_fileEncoding;
                    configId.m_customEncoding = settings.m_encoding.m_charset.m_customEncoding;
                    configId.m_skipFirstLines = settings.m_limitRows.m_skipFirstLines;
                    configId.m_skipFirstDataRows = settings.m_limitRows.m_skipFirstDataRows;
                    return () -> (T)configId;
                }
                if (ref.equals(FileChooserRef.class)) {
                    return () -> (T)settings.m_settings.m_source;
                }
                if (ref.equals(LimitMemoryPerColumnRef.class)) {
                    return () -> (T)(Object)settings.m_advancedSettings.m_limitMemoryPerColumn;
                }
                if (ref.equals(MaximumNumberOfColumnsRef.class)) {
                    return () -> (T)(Object)settings.m_advancedSettings.m_maximumNumberOfColumns;
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
        final var file = m_tempFolder.resolve("file.csv").toAbsolutePath().toString();
        final var settings = createDefaultSettingsAndWriteCSV(file, "intCol,stringCol", "\"1,two");
        final var fsLocationsProvider = new FSLocationsProvider();
        fsLocationsProvider.init(getFileChooserRefDependentInitializer(createDependenciesProvider(settings)));

        assertThat(fsLocationsProvider.computeState(null))
            .isEqualTo(new FSLocation[]{new FSLocation(FSCategory.LOCAL, file)});
    }

    @Test
    void testTypedReaderTableSpecsProvider() throws IOException {
        final var file = m_tempFolder.resolve("file.csv").toAbsolutePath().toString();

        final var typedReaderTableSpecsProvider = createTypedReaderTableSpecsProvider(file);
        final var specs = typedReaderTableSpecsProvider.computeState(null);

        assertTypedReaderTableSpecs(file, specs);
    }

    static TypedReaderTableSpecsProvider createTypedReaderTableSpecsProvider(final String file) throws IOException {
        final var settings = createDefaultSettingsAndWriteCSV(file, "intCol,stringCol", "1,two");
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
        final Map<String, TypedReaderTableSpec<Class<?>>> specs) {
        assertThat(specs).containsKey(file);
        assertThat(specs).size().isEqualTo(1);

        final var spec = specs.get(file);
        assertThat(spec.size()).isEqualTo(2);

        final var col1 = spec.getColumnSpec(0);
        assertThat(col1.getName()).isPresent().hasValue("intCol");
        assertThat(col1.hasType()).isTrue();
        assertThat(col1.getType()).isEqualTo(Integer.class);

        final var col2 = spec.getColumnSpec(1);
        assertThat(col2.getName()).isPresent().hasValue("stringCol");
        assertThat(col2.hasType()).isTrue();
        assertThat(col2.getType()).isEqualTo(String.class);
    }

    @Test
    void testTypedReaderTableSpecsProviderUnescapeDelimiters() throws IOException {
        final var file = m_tempFolder.resolve("file.csv").toAbsolutePath().toString();
        final var settings = createDefaultSettingsAndWriteCSV(file, "intCol\tstringCol\r\n", "1\ttwo\r\n");
        settings.m_settings.m_columnDelimiter = "\\t";
        settings.m_settings.m_customRowDelimiter = "\\r\\n";

        final var typedReaderTableSpecsProvider = new TypedReaderTableSpecsProvider();
        typedReaderTableSpecsProvider
            .init(getTypedReaderTableSpecProviderStateProviderInitializer(createDependenciesProvider(settings)));
        final var specs = typedReaderTableSpecsProvider.computeState(null);

        assertTypedReaderTableSpecs(file, specs);
    }

    @Test
    void testTableSpecSettingsProvider() throws IOException {
        final var file = m_tempFolder.resolve("file.csv").toAbsolutePath().toString();
        final var typedReaderTableSpecsProvider = createTypedReaderTableSpecsProvider(file);

        final var tableSpecSettingsProvider = new TableSpecSettingsProvider();
        tableSpecSettingsProvider.init(getTableSpecSettingsProviderInitializer(typedReaderTableSpecsProvider));
        final var specs = tableSpecSettingsProvider.computeState(null);

        assertThat(specs).hasSize(1);
        assertThat(specs[0].m_spec).hasSize(2);
        assertThat(specs[0].m_sourceId).isEqualTo(file);

        assertThat(specs[0].m_spec[0].m_name).isEqualTo("intCol");
        assertThat(specs[0].m_spec[0].m_type).isEqualTo(Integer.class);

        assertThat(specs[0].m_spec[1].m_name).isEqualTo("stringCol");
        assertThat(specs[0].m_spec[1].m_type).isEqualTo(String.class);
    }

    static class DependsOnTypedReaderTableSpecProviderInitializer extends NoopStateProviderInitializer {

        private final TypedReaderTableSpecsProvider m_typedReaderTableSpecsProvider;

        @Override
        public <T> void computeOnValueChange(final Class<? extends Reference<T>> ref) {
            if (ref.equals(ConfigIdReference.class) || ref.equals(FileChooserRef.class)
                || ref.equals(LimitMemoryPerColumnRef.class) || ref.equals(MaximumNumberOfColumnsRef.class)) {
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

        DependsOnTypedReaderTableSpecProviderInitializer(
            final TypedReaderTableSpecsProvider typedReaderTableSpecsProvider) {
            m_typedReaderTableSpecsProvider = typedReaderTableSpecsProvider;
        }
    }

    private static final StateProviderInitializer
        getTableSpecSettingsProviderInitializer(final TypedReaderTableSpecsProvider typedReaderTableSpecsProvider) {
        return new DependsOnTypedReaderTableSpecProviderInitializer(typedReaderTableSpecsProvider) {
            @Override
            public void computeAfterOpenDialog() {
                // Do nothing
            }
        };
    }

    @Test
    void testTransformationElementSettingsProvider() throws IOException {
        final var file = m_tempFolder.resolve("file.csv").toAbsolutePath().toString();
        final var typedReaderTableSpecsProvider = createTypedReaderTableSpecsProvider(file);

        final var transformationElementSettingsProvider = new TransformationElementSettingsProvider();
        transformationElementSettingsProvider
            .init(getTransformationElementSettingsProviderInitializer(typedReaderTableSpecsProvider));

        final var transformationElements = transformationElementSettingsProvider.computeState(null);

        assertThat(transformationElements).hasSize(2);

        assertThat(transformationElements[0].m_columnName).isEqualTo("intCol");
        assertThat(transformationElements[0].m_includeInOutput).isTrue();
        assertThat(transformationElements[0].m_columnRename).isEqualTo("intCol");
        assertThat(transformationElements[0].m_type).isEqualTo(
            PRODUCTION_PATH_PROVIDER.getDefaultProductionPath(Integer.class).getConverterFactory().getIdentifier());

        assertThat(transformationElements[1].m_columnName).isEqualTo("stringCol");
        assertThat(transformationElements[1].m_includeInOutput).isTrue();
        assertThat(transformationElements[1].m_columnRename).isEqualTo("stringCol");
        assertThat(transformationElements[1].m_type).isEqualTo(
            PRODUCTION_PATH_PROVIDER.getDefaultProductionPath(String.class).getConverterFactory().getIdentifier());
    }

    private static final StateProviderInitializer getTransformationElementSettingsProviderInitializer(
        final TypedReaderTableSpecsProvider typedReaderTableSpecsProvider) {
        return new DependsOnTypedReaderTableSpecProviderInitializer(typedReaderTableSpecsProvider);
    }

    @Test
    void testTypeChoicesProviderIntCol() throws IOException {
        final var file = m_tempFolder.resolve("file.csv").toAbsolutePath().toString();
        final var typedReaderTableSpecsProvider = createTypedReaderTableSpecsProvider(file);

        testTypeChoicesProviderIntCol(typedReaderTableSpecsProvider, "intCol", Integer.class);
        testTypeChoicesProviderIntCol(typedReaderTableSpecsProvider, "stringCol", String.class);
    }

    private static void testTypeChoicesProviderIntCol(final TypedReaderTableSpecsProvider typedReaderTableSpecProvider,
        final String columnName, final Class<?> type) {
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
            public <T> Supplier<T> computeFromValueSupplier(final Class<? extends Reference<T>> ref) {
                if (ref.equals(ColumnNameRef.class)) {
                    return () -> (T)columnName;
                }
                throw new IllegalStateException(String.format("Unexpected dependency %s", ref.getSimpleName()));
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
