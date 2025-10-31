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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.QuotedStringsOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationParameters.ConfigIdSettings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationParametersStateProviders.TypeChoicesProvider;
import org.knime.base.node.io.filehandling.csv.reader2.common.CommonReaderTransformationParameters;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.AdvancedSettingsWithMultipleFileHandling;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.SettingsWithRowId;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviderTestUtils.CommonReaderTransformationSettingsUpdatesTestClassBased;
import org.knime.core.data.DataType;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
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
        Function<CSVTableReaderNodeSettings, T> dependency, List<String> pathToSettings) {
    }

    @SuppressWarnings("rawtypes")
    private static final List<ConfigIdFieldSpec> depedencies = List.of(//
        new ConfigIdFieldSpec<>("firstRowContainsColumnNames", //
            (c, v) -> c.m_firstRowContainsColumnNames = v, //
            s -> s.m_settings.m_firstRowContainsColumnNames, //
            List.of("settings", "firstRowContainsColumnNames") //
        ), //
        new ConfigIdFieldSpec<>("firstColumnContainsRowIds", //
            (c, v) -> c.m_firstColumnContainsRowIds = v, //
            s -> s.m_settings.m_firstColumnContainsRowIds, //
            List.of("settings", "firstColumnContainsRowIds") //
        ), //
        new ConfigIdFieldSpec<>("commentLineCharacter", //
            (c, v) -> c.m_commentLineCharacter = v, //
            s -> s.m_settings.m_commentLineCharacter, //
            List.of("settings", "commentLineCharacter")//
        ), //
        new ConfigIdFieldSpec<>("columnDelimiter", //
            (c, v) -> c.m_columnDelimiter = v, //
            s -> s.m_settings.m_columnDelimiter, //
            List.of("settings", "columnDelimiter") //
        ), //
        new ConfigIdFieldSpec<>("quoteCharacter", //
            (c, v) -> c.m_quoteCharacter = v, //
            s -> s.m_settings.m_quoteCharacter, //
            List.of("settings", "quoteCharacter") //
        ), //
        new ConfigIdFieldSpec<>("quoteEscapeCharacter", //
            (c, v) -> c.m_quoteEscapeCharacter = v, //
            s -> s.m_settings.m_quoteEscapeCharacter, //
            List.of("settings", "quoteEscapeCharacter") //
        ), //
        new ConfigIdFieldSpec<>("rowDelimiterOption", //
            (c, v) -> c.m_rowDelimiterOption = v, //
            s -> s.m_settings.m_rowDelimiterOption, //
            List.of("settings", "rowDelimiterOption") //
        ), //
        new ConfigIdFieldSpec<>("customRowDelimiter", //
            (c, v) -> c.m_customRowDelimiter = v, //
            s -> s.m_settings.m_customRowDelimiter, //
            List.of("settings", "customRowDelimiter") //
        ), //
        new ConfigIdFieldSpec<>("quotedStringsOption", //
            (c, v) -> c.m_quotedStringsOption = v, //
            s -> s.m_advancedSettings.m_quotedStringsOption, //
            List.of("advancedSettings", "quotedStringsOption") //
        ), //
        new ConfigIdFieldSpec<>("replaceEmptyQuotedStringsByMissingValues", //
            (c, v) -> c.m_replaceEmptyQuotedStringsByMissingValues = v, //
            s -> s.m_advancedSettings.m_replaceEmptyQuotedStringsByMissingValues, //
            List.of("advancedSettings", "replaceEmptyQuotedStringsByMissingValues") //
        ), //
        new ConfigIdFieldSpec<>("limitScannedRows", //
            (c, v) -> c.m_limitScannedRows = v, //
            s -> s.m_advancedSettings.m_limitScannedRows, //
            List.of("advancedSettings", "limitScannedRows") //
        ), //
        new ConfigIdFieldSpec<>("maxDataRowsScanned", //
            (c, v) -> c.m_maxDataRowsScanned = v, //
            s -> s.m_advancedSettings.m_maxDataRowsScanned, //
            List.of("advancedSettings", "maxDataRowsScanned") //
        ), //
        new ConfigIdFieldSpec<>("thousandsSeparator", //
            (c, v) -> c.m_thousandsSeparator = v, //
            s -> s.m_advancedSettings.m_thousandsSeparator, //
            List.of("advancedSettings", "thousandsSeparator") //
        ), //
        new ConfigIdFieldSpec<>("decimalSeparator", //
            (c, v) -> c.m_decimalSeparator = v, //
            s -> s.m_advancedSettings.m_decimalSeparator, //
            List.of("advancedSettings", "decimalSeparator") //
        ), //
        new ConfigIdFieldSpec<>("fileEncoding", //
            (c, v) -> c.m_fileEncoding = v, //
            s -> s.m_encoding.m_charset.m_fileEncoding, //
            List.of("encoding", "charset", "fileEncoding") //
        ), //
        new ConfigIdFieldSpec<>("customEncoding", //
            (c, v) -> c.m_customEncoding = v, //
            s -> s.m_encoding.m_charset.m_customEncoding, //
            List.of("encoding", "charset", "customEncoding") //
        ), //
        new ConfigIdFieldSpec<>("skipFirstLines", //
            (c, v) -> c.m_skipFirstLines = v, //
            s -> s.m_limitRows.m_skipFirstLines, //
            List.of("limitRows", "skipFirstLines") //
        ), //
        new ConfigIdFieldSpec<>("skipFirstDataRows", //
            (c, v) -> c.m_skipFirstDataRows = v, //
            s -> s.m_limitRows.m_skipFirstDataRows, //
            List.of("limitRows", "skipFirstDataRows") //
        ), //
        new ConfigIdFieldSpec<>("limitMemoryPerColumn", //
            (c, v) -> c.m_limitMemoryPerColumn = v, //
            s -> s.m_advancedSettings.m_limitMemoryPerColumn, //
            List.of("advancedSettings", "limitMemoryPerColumn") //
        ), //
        new ConfigIdFieldSpec<>("maximumNumberOfColumns", //
            (c, v) -> c.m_maximumNumberOfColumns = v, //
            s -> s.m_advancedSettings.m_maximumNumberOfColumns, //
            List.of("advancedSettings", "maximumNumberOfColumns") //
        ) //

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

        static final UpdateSimulator simulator = new DialogUpdateSimulator(m_settings, NodeParametersInputImpl
            .createDefaultNodeSettingsContext(new PortType[0], new PortObjectSpec[0], null, null));

        static final UpdateSimulatorResult beforeOpenDialogResults = simulator.simulateBeforeOpenDialog();

        @ParameterizedTest
        @MethodSource
        @SuppressWarnings("static-method")
        void testConfigId(final String fieldName, final Function<CSVTableReaderNodeSettings, Object> dependency,
            final List<String> referenceClass) {
            final var onValueChangeResult = simulator.simulateValueChange(referenceClass);
            List.of(onValueChangeResult, beforeOpenDialogResults).forEach(result -> {
                assertThat(result.getValueUpdateAt("tableSpecConfig", "persistorSettings", "configId", fieldName))
                    .isEqualTo(dependency.apply(m_settings));
            });
        }

        static Stream<Arguments> testConfigId() {
            return depedencies.stream().map(d -> Arguments.of(d.fieldName, d.dependency, d.pathToSettings));
        }
    }

    @Nested
    static final class TransformationSettingsUpdatesTest
        extends CommonReaderTransformationSettingsUpdatesTestClassBased<CSVTableReaderNodeSettings> {

        @Override
        protected SettingsWithRowId getSettings(final CSVTableReaderNodeSettings settings) {
            return settings.m_settings;
        }

        @Override
        protected AdvancedSettingsWithMultipleFileHandling
            getAdvancedSettings(final CSVTableReaderNodeSettings settings) {
            return settings.m_advancedSettings;
        }

        @Override
        protected CommonReaderTransformationParameters<?, Class<?>>
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

    }

}
