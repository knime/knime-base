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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileReaderParameters.HowToCombineColumnsOption;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParameters;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProviderTestUtils.TransformationParametersUpdatesTestClassBased;
import org.knime.core.data.DataType;
import org.knime.core.data.def.LongCell;
import org.knime.core.util.Pair;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;

/**
 *
 * @author Paul Bärnreuther
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
final class CSVTableReaderTransformationParametersStateProvidersTest
    extends TransformationParametersUpdatesTestClassBased<CSVTableReaderNodeParameters> {

    static final List<List<String>> TRIGGER_PATHS = List.of( //
        List.of("csvReaderParameters", "firstRowContainsColumnNamesParams", "firstRowContainsColumnNames"), //
        List.of("csvReaderParameters", "firstColumnContainsRowIdsParams", "firstColumnContainsRowIds"), //
        List.of("csvReaderParameters", "csvFormatParams", "commentLineCharacter"), //
        List.of("csvReaderParameters", "csvFormatParams", "columnDelimiter"), //
        List.of("csvReaderParameters", "csvFormatParams", "quoteCharacter"), //
        List.of("csvReaderParameters", "csvFormatParams", "quoteEscapeCharacter"), //
        List.of("csvReaderParameters", "csvFormatParams", "rowDelimiterOption"), //
        List.of("csvReaderParameters", "csvFormatParams", "customRowDelimiter"), //
        List.of("csvReaderParameters", "quotedStringsParams", "quotedStringsOption"), //
        List.of("csvReaderParameters", "quotedStringsParams", "replaceEmptyQuotedStringsByMissingValues"), //
        List.of("csvReaderParameters", "limitScannedRowsParams", "maxDataRowsScanned"), //
        List.of("csvReaderParameters", "csvFormatParams", "thousandsSeparator"), //
        List.of("csvReaderParameters", "csvFormatParams", "decimalSeparator"), //
        List.of("csvReaderParameters", "fileEncodingParams", "fileEncoding"), //
        List.of("csvReaderParameters", "fileEncodingParams", "customEncoding"), //
        List.of("csvReaderParameters", "skipFirstLinesParams", "skipFirstLines"), //
        List.of("csvReaderParameters", "skipFirstDataRowsParams", "skipFirstDataRows"), //
        List.of("csvReaderParameters", "limitMemoryParams", "limitMemoryPerColumn"), //
        List.of("csvReaderParameters", "maxColumnsParams", "maximumNumberOfColumns") //
    );

    @Override
    protected TransformationParameters<Class<?>>
        getTransformationSettings(final CSVTableReaderNodeParameters settings) {
        return settings.m_transformationParameters;
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
        return CSVTableReaderSpecific.PRODUCTION_PATH_PROVIDER;
    }

    @Override
    protected Pair<DataType, Collection<IntOrString>> getUnreachableType() {
        return new Pair<>(LongCell.TYPE, List.of(IntOrString.STRING));
    }

    @Override
    protected List<String> getPathToTransformationSettings() {
        return List.of("transformationParameters");
    }

    @Override
    protected CSVTableReaderNodeParameters constructNewSettings() {
        return new CSVTableReaderNodeParameters();
    }

    @Override
    protected String getFileName() {
        return "test.csv";
    }

    @Test
    void testTableSpecsProviderUnescapeDelimiters() throws IOException {
        m_settings.m_csvReaderParameters.m_csvFormatParams.m_columnDelimiter = "\\t";
        m_settings.m_csvReaderParameters.m_csvFormatParams.m_customRowDelimiter = "\\r\\n";
        writeCSV(m_filePath, "intCol\tstringCol\r\n", "1\ttwo\r\n");

        final var simulatorResult = m_simulator.simulateAfterOpenDialog();
        final var specs = getTableSpecsValueUpdate(simulatorResult);

        assertIntegerAndStringColumn(specs);
    }

    @ParameterizedTest
    @MethodSource("getDependencyTriggerReferences")
    void testTableSpecSettingsProvider(final List<String> triggerPath) throws IOException {
        testTableSpecSettingsProvider(sim -> sim.simulateValueChange(triggerPath));
    }

    static Stream<Arguments> getDependencyTriggerReferences() {
        return TRIGGER_PATHS.stream().map(Arguments::of);
    }

    @Override
    protected void setSourcePath(final CSVTableReaderNodeParameters settings, final FSLocation fsLocation) {
        settings.m_csvReaderParameters.m_multiFileSelectionParams.m_source.m_path = fsLocation;
    }

    @Override
    protected void setHowToCombineColumns(final CSVTableReaderNodeParameters settings,
        final HowToCombineColumnsOption howToCombineColumns) {
        settings.m_csvReaderParameters.m_multiFileReaderParams.m_howToCombineColumns = howToCombineColumns;
    }

}
