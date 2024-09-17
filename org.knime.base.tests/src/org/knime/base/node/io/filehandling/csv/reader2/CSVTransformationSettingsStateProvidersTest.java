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
import static org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviderTestUtils.getConfigIdSettings;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.DecimalSeparatorRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.LimitMemoryPerColumnRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.LimitScannedRowsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.MaxDataRowsScannedRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.MaximumNumberOfColumnsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.QuotedStringsOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.QuotedStringsOptionRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.ReplaceEmptyQuotedStringsByMissingValuesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.ThousandsSeparatorRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.CustomEncodingRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.LimitRows.SkipFirstDataRowsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.LimitRows.SkipFirstLinesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.ColumnDelimiterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.CommentStartRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.CustomRowDelimiterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.FirstRowContainsColumnNamesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.QuoteCharacterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.QuoteEscapeCharacterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOptionRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.ConfigIdSettings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.DependenciesProvider.ConfigIdRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.TypeChoicesProvider;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.AdvancedSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.Settings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.Settings.FirstColumnContainsRowIdsRef;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviderTestUtils.CommonReaderTransformationSettingsUpdatesTestClassBased;
import org.knime.core.data.DataType;
import org.knime.core.data.def.LongCell;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.testing.node.dialog.updates.DialogUpdateSimulator;
import org.knime.testing.node.dialog.updates.UpdateSimulator;
import org.knime.testing.node.dialog.updates.UpdateSimulator.UpdateSimulatorResult;

/**
 *
 * @author Paul Bärnreuther
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class CSVTransformationSettingsStateProvidersTest {

    record ConfigIdFieldSpec<T>(String fieldName, BiConsumer<ConfigIdSettings, T> setter,
        Function<CSVTableReaderNodeSettings, T> dependency, Class<? extends Reference<T>> referenceClass) {
    }

    @SuppressWarnings("rawtypes")
    private static final List<ConfigIdFieldSpec> depedencies = List.of(
        new ConfigIdFieldSpec<>("firstRowContainsColumnNames", //
            (c, v) -> c.m_firstRowContainsColumnNames = v, //
            s -> s.m_settings.m_firstRowContainsColumnNames, //
            FirstRowContainsColumnNamesRef.class), //
        new ConfigIdFieldSpec<>("firstColumnContainsRowIds", //
            (c, v) -> c.m_firstColumnContainsRowIds = v, //
            s -> s.m_settings.m_firstColumnContainsRowIds, //
            FirstColumnContainsRowIdsRef.class), //
        new ConfigIdFieldSpec<>("commentLineCharacter", //
            (c, v) -> c.m_commentLineCharacter = v, //
            s -> s.m_settings.m_commentLineCharacter, //
            CommentStartRef.class), //
        new ConfigIdFieldSpec<>("columnDelimiter", //
            (c, v) -> c.m_columnDelimiter = v, //
            s -> s.m_settings.m_columnDelimiter, //
            ColumnDelimiterRef.class), //
        new ConfigIdFieldSpec<>("quoteCharacter", //
            (c, v) -> c.m_quoteCharacter = v, //
            s -> s.m_settings.m_quoteCharacter, //
            QuoteCharacterRef.class), //
        new ConfigIdFieldSpec<>("quoteEscapeCharacter", //
            (c, v) -> c.m_quoteEscapeCharacter = v, //
            s -> s.m_settings.m_quoteEscapeCharacter, //
            QuoteEscapeCharacterRef.class), //
        new ConfigIdFieldSpec<>("rowDelimiterOption", //
            (c, v) -> c.m_rowDelimiterOption = v, //
            s -> s.m_settings.m_rowDelimiterOption, //
            RowDelimiterOptionRef.class), //
        new ConfigIdFieldSpec<>("customRowDelimiter", //
            (c, v) -> c.m_customRowDelimiter = v, //
            s -> s.m_settings.m_customRowDelimiter, //
            CustomRowDelimiterRef.class), //
        new ConfigIdFieldSpec<>("quotedStringsOption", //
            (c, v) -> c.m_quotedStringsOption = v, //
            s -> s.m_advancedSettings.m_quotedStringsOption, //
            QuotedStringsOptionRef.class), //
        new ConfigIdFieldSpec<>("replaceEmptyQuotedStringsByMissingValues", //
            (c, v) -> c.m_replaceEmptyQuotedStringsByMissingValues = v, //
            s -> s.m_advancedSettings.m_replaceEmptyQuotedStringsByMissingValues, //
            ReplaceEmptyQuotedStringsByMissingValuesRef.class), //
        new ConfigIdFieldSpec<>("limitScannedRows", //
            (c, v) -> c.m_limitScannedRows = v, //
            s -> s.m_advancedSettings.m_limitScannedRows, //
            LimitScannedRowsRef.class), //
        new ConfigIdFieldSpec<>("maxDataRowsScanned", //
            (c, v) -> c.m_maxDataRowsScanned = v, //
            s -> s.m_advancedSettings.m_maxDataRowsScanned, //
            MaxDataRowsScannedRef.class), //
        new ConfigIdFieldSpec<>("thousandsSeparator", //
            (c, v) -> c.m_thousandsSeparator = v, //
            s -> s.m_advancedSettings.m_thousandsSeparator, //
            ThousandsSeparatorRef.class), //
        new ConfigIdFieldSpec<>("decimalSeparator", //
            (c, v) -> c.m_decimalSeparator = v, //
            s -> s.m_advancedSettings.m_decimalSeparator, //
            DecimalSeparatorRef.class), //
        new ConfigIdFieldSpec<>("fileEncoding", //
            (c, v) -> c.m_fileEncoding = v, //
            s -> s.m_encoding.m_charset.m_fileEncoding, //
            FileEncodingRef.class), //
        new ConfigIdFieldSpec<>("customEncoding", //
            (c, v) -> c.m_customEncoding = v, //
            s -> s.m_encoding.m_charset.m_customEncoding, //
            CustomEncodingRef.class), //
        new ConfigIdFieldSpec<>("skipFirstLines", //
            (c, v) -> c.m_skipFirstLines = v, //
            s -> s.m_limitRows.m_skipFirstLines, //
            SkipFirstLinesRef.class), //
        new ConfigIdFieldSpec<>("skipFirstDataRows", //
            (c, v) -> c.m_skipFirstDataRows = v, //
            s -> s.m_limitRows.m_skipFirstDataRows, //
            SkipFirstDataRowsRef.class) //
    );

    @Nested
    static final class DependenciesUpdateConfigIdSettingsTest {

        static final CSVTableReaderNodeSettings m_settings = initSettings();

        private static CSVTableReaderNodeSettings initSettings() {
            final var settings = new CSVTableReaderNodeSettings();

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

        static final UpdateSimulator simulator = new DialogUpdateSimulator(m_settings, null);

        static final UpdateSimulatorResult beforeOpenDialogResults = simulator.simulateBeforeOpenDialog();

        @ParameterizedTest
        @MethodSource
        @SuppressWarnings("static-method")
        void testConfigId(final String fieldName, final Function<CSVTableReaderNodeSettings, Object> dependency,
            final Class<? extends Reference<?>> referenceClass) {
            final var onValueChangeResult = simulator.simulateValueChange(referenceClass);
            List.of(onValueChangeResult, beforeOpenDialogResults).forEach(result -> {
                assertThat(result.getValueUpdateAt("tableSpecConfig", "persistorSettings", "configId", fieldName))
                    .isEqualTo(dependency.apply(m_settings));
            });
        }

        static Stream<Arguments> testConfigId() {
            return depedencies.stream().map(d -> Arguments.of(d.fieldName, d.dependency, d.referenceClass));
        }
    }

    @Nested
    static final class TransformationSettingsUpdatesTest
        extends CommonReaderTransformationSettingsUpdatesTestClassBased<CSVTableReaderNodeSettings> {

        @Override
        protected Settings getSettings(final CSVTableReaderNodeSettings settings) {
            return settings.m_settings;
        }

        @Override
        protected AdvancedSettings getAdvancedSettings(final CSVTableReaderNodeSettings settings) {
            return settings.m_advancedSettings;
        }

        @Override
        protected CommonReaderTransformationSettings<?, Class<?>>
            getTransformationSettings(final CSVTableReaderNodeSettings settings) {
            return settings.m_tableSpecConfig;
        }

        @Override
        protected void writeFileWithIntegerAndStringColumn(final String filePath) throws IOException {
            writeCSV(filePath, "intCol,stringCol", "1,two");
        }

        private static void writeCSV(final String filePath, final String... lines) throws IOException {
            try (final var writer = new BufferedWriter(new FileWriter(filePath))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

        }

        @Override
        protected ProductionPathProvider<Class<?>> getProductionPathProvider() {
            return CSVReaderSpecific.PRODUCTION_PATH_PROVIDER;
        }

        @Override
        protected Pair<DataType, Collection<IntOrString>> getUnreachableType() {
            return new Pair<>(LongCell.TYPE, List.of(IntOrString.STRING));
        }

        @Override
        protected List<String> getPathToTransformationSettings() {
            return List.of("tableSpecConfig");
        }

        @Override
        protected CSVTableReaderNodeSettings constructNewSettings() {
            return new CSVTableReaderNodeSettings();
        }

        @Override
        protected Class<TypeChoicesProvider> getTypeChoicesProviderClass() {
            return TypeChoicesProvider.class;
        }

        @Override
        protected String getFileName() {
            return "test.csv";
        }

        @SuppressWarnings("unchecked")
        private void updateConfigIdFromSettings() {
            depedencies.forEach(d -> d.setter.accept(getConfigIdSettings(getTransformationSettings(m_settings)),
                d.dependency.apply(m_settings)));
        }

        @ParameterizedTest
        @ArgumentsSource(RunSimulationForAdditionalCSVSpecChangesProvider.class)
        void testTableSpecSettingsProviderCSVSpecific(final Function<UpdateSimulator, UpdateSimulatorResult> simulate)
            throws IOException {
            testTableSpecSettingsProvider(simulate);
        }

        @Test
        void testTableSpecsProviderUnescapeDelimiters() throws IOException {
            m_settings.m_settings.m_columnDelimiter = "\\t";
            m_settings.m_settings.m_customRowDelimiter = "\\r\\n";
            updateConfigIdFromSettings();
            writeCSV(m_filePath, "intCol\tstringCol\r\n", "1\ttwo\r\n");

            final var simulatorResult = m_simulator.simulateAfterOpenDialog();
            final var specs = getSpecsValueUpdate(simulatorResult);

            assertIntegerAndStringColumn(specs);
        }

        @ParameterizedTest
        @ArgumentsSource(RunSimulationForAdditionalCSVSpecChangesProvider.class)
        void testTransformationElementSettingsProviderCSVSpecific(
            final Function<UpdateSimulator, UpdateSimulatorResult> simulate) throws IOException {
            testTableSpecSettingsProvider(simulate);
        }

        static final class RunSimulationForAdditionalCSVSpecChangesProvider implements ArgumentsProvider {

            @Override
            public Stream<? extends Arguments> provideArguments(final ExtensionContext context) throws Exception {
                return getSimulations().map(Arguments::of);
            }

            static Stream<Function<UpdateSimulator, UpdateSimulatorResult>> getSimulations() {
                return Stream.of(simulator -> simulator.simulateValueChange(ConfigIdRef.class),
                    simulator -> simulator.simulateValueChange(LimitMemoryPerColumnRef.class),
                    simulator -> simulator.simulateValueChange(MaximumNumberOfColumnsRef.class));
            }
        }

    }

}
