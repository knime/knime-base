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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.base.node.io.filehandling.csv.reader.api.QuoteOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.DecimalSeparatorRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.LimitScannedRowsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.MaxDataRowsScannedRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.MaxNumChunksPerFileRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.MaximumNumberOfColumnsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.MinChunkSizeInBytesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.QuotedStringsOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.QuotedStringsOptionRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.ReplaceEmptyQuotedStringsByMissingValuesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.ThousandsSeparatorRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.CustomEncodingRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.LimitRows.LimitNumberOfRowsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.LimitRows.MaximumNumberOfRowsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.LimitRows.SkipFirstDataRowsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.LimitRows.SkipFirstLinesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.BufferSizeRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.ColumnDelimiterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.CommentStartRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.CustomRowDelimiterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.FileChooserRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.FirstColumnContainsRowIdsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.FirstRowContainsColumnNamesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.IfRowHasLessColumnsOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.IfRowHasLessColumnsOptionRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.PrependFileIndexToRowIdRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.QuoteCharacterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.QuoteEscapeCharacterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOptionRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.SkipEmptyDataRowsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableSpec.CSVTableSpecProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableSpec.TableReadConfigProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableSpec.TypedReaderTableSpecProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ButtonReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider.StateProviderInitializer;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
class CSVTableSpecTest extends LocalWorkflowContextTest {

    @TempDir
    Path m_tempFolder;

    @Test
    void testTableReadConfigProvider() {
        final var settings = new CSVTableReaderNodeSettings();
        settings.m_settings.m_firstRowContainsColumnNames = false;
        settings.m_settings.m_firstColumnContainsRowIds = true;
        settings.m_settings.m_commentLineCharacter = "?";
        settings.m_settings.m_columnDelimiter = ";";
        settings.m_settings.m_quoteCharacter = "'";
        settings.m_settings.m_quoteEscapeCharacter = "'";
        settings.m_settings.m_rowDelimiterOption = RowDelimiterOption.CUSTOM;
        settings.m_settings.m_customRowDelimiter = "\r\n";
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
        settings.m_settings.m_numberOfCharactersForAutodetection = 1;
        settings.m_advancedSettings.m_maximumNumberOfColumns = 1;
        settings.m_advancedSettings.m_minChunkSizeInBytes = 1;
        settings.m_advancedSettings.m_maxNumChunksPerFile = 1;
        settings.m_settings.m_ifRowHasLessColumnsOption = IfRowHasLessColumnsOption.FAIL;
        settings.m_limitRows.m_limitNumberOfRows = true;
        settings.m_limitRows.m_maximumNumberOfRows = 51;
        settings.m_settings.m_prependFileIndexToRowId = true;
        settings.m_settings.m_skipEmptyDataRows = true;

        final var tableReadConfigProvider = new TableReadConfigProvider();
        tableReadConfigProvider.init(getTableReadConfigProviderStateProviderInitializer(settings));
        final var config = tableReadConfigProvider.computeState(null);
        final var csvConfig = config.getReaderSpecificConfig();

        assertThat(config.useColumnHeaderIdx()).isEqualTo(settings.m_settings.m_firstRowContainsColumnNames);
        assertThat(config.useRowIDIdx()).isEqualTo(settings.m_settings.m_firstColumnContainsRowIds);
        assertThat(csvConfig.getComment()).isEqualTo(settings.m_settings.m_commentLineCharacter);
        assertThat(csvConfig.getDelimiter()).isEqualTo(settings.m_settings.m_columnDelimiter);
        assertThat(csvConfig.getQuote()).isEqualTo(settings.m_settings.m_quoteCharacter);
        assertThat(csvConfig.getQuoteEscape()).isEqualTo(settings.m_settings.m_quoteEscapeCharacter);
        assertThat(csvConfig.useLineBreakRowDelimiter())
            .isEqualTo(settings.m_settings.m_rowDelimiterOption == RowDelimiterOption.LINE_BREAK);
        assertThat(csvConfig.getLineSeparator()).isEqualTo(settings.m_settings.m_customRowDelimiter);
        assertThat(csvConfig.getQuoteOption())
            .isEqualTo(QuoteOption.valueOf(settings.m_advancedSettings.m_quotedStringsOption.name()));
        assertThat(csvConfig.replaceEmptyWithMissing())
            .isEqualTo(settings.m_advancedSettings.m_replaceEmptyQuotedStringsByMissingValues);
        assertThat(config.limitRowsForSpec()).isEqualTo(settings.m_advancedSettings.m_limitScannedRows);
        assertThat(config.getMaxRowsForSpec()).isEqualTo(settings.m_advancedSettings.m_maxDataRowsScanned);
        assertThat(csvConfig.getThousandsSeparator()).isEqualTo(settings.m_advancedSettings.m_thousandsSeparator);
        assertThat(csvConfig.getDecimalSeparator()).isEqualTo(settings.m_advancedSettings.m_decimalSeparator);
        assertThat(csvConfig.getCharSetName()).isEqualTo(settings.m_encoding.m_charset.m_customEncoding);
        assertThat(csvConfig.skipLines()).isEqualTo(settings.m_limitRows.m_skipFirstLines > 0);
        assertThat(csvConfig.getNumLinesToSkip()).isEqualTo(settings.m_limitRows.m_skipFirstLines);
        assertThat(config.skipRows()).isEqualTo(settings.m_limitRows.m_skipFirstDataRows > 0);
        assertThat(config.getNumRowsToSkip()).isEqualTo(settings.m_limitRows.m_skipFirstDataRows);
        assertThat(csvConfig.getAutoDetectionBufferSize())
            .isEqualTo(settings.m_settings.m_numberOfCharactersForAutodetection);
        assertThat(csvConfig.getMaxColumns()).isEqualTo(settings.m_advancedSettings.m_maximumNumberOfColumns);
        assertThat(csvConfig.getMinChunkSizeInBytes()).isEqualTo(settings.m_advancedSettings.m_minChunkSizeInBytes);
        assertThat(csvConfig.getMaxNumChunksPerFile()).isEqualTo(settings.m_advancedSettings.m_maxNumChunksPerFile);
        assertThat(config.allowShortRows())
            .isEqualTo(settings.m_settings.m_ifRowHasLessColumnsOption == IfRowHasLessColumnsOption.INSERT_MISSING);
        assertThat(config.limitRows()).isEqualTo(settings.m_limitRows.m_limitNumberOfRows);
        assertThat(config.getMaxRows()).isEqualTo(settings.m_limitRows.m_maximumNumberOfRows);
        assertThat(config.prependSourceIdxToRowID()).isEqualTo(settings.m_settings.m_prependFileIndexToRowId);
        assertThat(config.skipEmptyRows()).isEqualTo(settings.m_settings.m_skipEmptyDataRows);
    }

    private static final StateProviderInitializer
        getTableReadConfigProviderStateProviderInitializer(final CSVTableReaderNodeSettings settings) {
        return new StateProviderInitializer() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Supplier<T> getValueSupplier(final Class<? extends Reference<T>> ref) {
                if (ref.equals(FirstRowContainsColumnNamesRef.class)) {
                    return () -> (T)(Object)settings.m_settings.m_firstRowContainsColumnNames;
                }
                if (ref.equals(FirstColumnContainsRowIdsRef.class)) {
                    return () -> (T)(Object)settings.m_settings.m_firstColumnContainsRowIds;
                }
                if (ref.equals(CommentStartRef.class)) {
                    return () -> (T)settings.m_settings.m_commentLineCharacter;
                }
                if (ref.equals(ColumnDelimiterRef.class)) {
                    return () -> (T)settings.m_settings.m_columnDelimiter;
                }
                if (ref.equals(QuoteCharacterRef.class)) {
                    return () -> (T)settings.m_settings.m_quoteCharacter;
                }
                if (ref.equals(QuoteEscapeCharacterRef.class)) {
                    return () -> (T)settings.m_settings.m_quoteEscapeCharacter;
                }
                if (ref.equals(RowDelimiterOptionRef.class)) {
                    return () -> (T)settings.m_settings.m_rowDelimiterOption;
                }
                if (ref.equals(CustomRowDelimiterRef.class)) {
                    return () -> (T)settings.m_settings.m_customRowDelimiter;
                }
                if (ref.equals(QuotedStringsOptionRef.class)) {
                    return () -> (T)settings.m_advancedSettings.m_quotedStringsOption;
                }
                if (ref.equals(ReplaceEmptyQuotedStringsByMissingValuesRef.class)) {
                    return () -> (T)(Object)settings.m_advancedSettings.m_replaceEmptyQuotedStringsByMissingValues;
                }
                if (ref.equals(LimitScannedRowsRef.class)) {
                    return () -> (T)(Object)settings.m_advancedSettings.m_limitScannedRows;
                }
                if (ref.equals(MaxDataRowsScannedRef.class)) {
                    return () -> (T)(Object)settings.m_advancedSettings.m_maxDataRowsScanned;
                }
                if (ref.equals(ThousandsSeparatorRef.class)) {
                    return () -> (T)settings.m_advancedSettings.m_thousandsSeparator;
                }
                if (ref.equals(DecimalSeparatorRef.class)) {
                    return () -> (T)settings.m_advancedSettings.m_decimalSeparator;
                }
                if (ref.equals(FileEncodingRef.class)) {
                    return () -> (T)settings.m_encoding.m_charset.m_fileEncoding;
                }
                if (ref.equals(CustomEncodingRef.class)) {
                    return () -> (T)settings.m_encoding.m_charset.m_customEncoding;
                }
                if (ref.equals(SkipFirstLinesRef.class)) {
                    return () -> (T)(Object)settings.m_limitRows.m_skipFirstLines;
                }
                if (ref.equals(SkipFirstDataRowsRef.class)) {
                    return () -> (T)(Object)settings.m_limitRows.m_skipFirstDataRows;
                }
                if (ref.equals(BufferSizeRef.class)) {
                    return () -> (T)(Object)settings.m_settings.m_numberOfCharactersForAutodetection;
                }
                if (ref.equals(MaximumNumberOfColumnsRef.class)) {
                    return () -> (T)(Object)settings.m_advancedSettings.m_maximumNumberOfColumns;
                }
                if (ref.equals(MinChunkSizeInBytesRef.class)) {
                    return () -> (T)(Object)settings.m_advancedSettings.m_minChunkSizeInBytes;
                }
                if (ref.equals(MaxNumChunksPerFileRef.class)) {
                    return () -> (T)(Object)settings.m_advancedSettings.m_maxNumChunksPerFile;
                }
                if (ref.equals(IfRowHasLessColumnsOptionRef.class)) {
                    return () -> (T)settings.m_settings.m_ifRowHasLessColumnsOption;
                }
                if (ref.equals(LimitNumberOfRowsRef.class)) {
                    return () -> (T)(Object)settings.m_limitRows.m_limitNumberOfRows;
                }
                if (ref.equals(MaximumNumberOfRowsRef.class)) {
                    return () -> (T)(Object)settings.m_limitRows.m_maximumNumberOfRows;
                }
                if (ref.equals(PrependFileIndexToRowIdRef.class)) {
                    return () -> (T)(Object)settings.m_settings.m_prependFileIndexToRowId;
                }
                if (ref.equals(SkipEmptyDataRowsRef.class)) {
                    return () -> (T)(Object)settings.m_settings.m_skipEmptyDataRows;
                }
                throw new IllegalStateException(String.format("Unexpected dependency %s", ref.getSimpleName()));
            }

            @Override
            public <T> void computeOnValueChange(final Class<? extends Reference<T>> id) {
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
            public <T> Supplier<T>
                computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
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
        };
    }

    @Test
    void testTypedReaderTableSpecProvider() throws IOException {
        final var file = m_tempFolder.resolve("file.csv").toAbsolutePath().toString();
        final var typedReaderTableSpecProvider = createTypedReaderTableSpecProvider(file);
        final var specs = typedReaderTableSpecProvider.computeState(null);

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

    @SuppressWarnings("restriction")
    static TypedReaderTableSpecProvider createTypedReaderTableSpecProvider(final String file)
        throws IOException {
        try (final var writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("intCol,stringCol\n");
            writer.write("1,two\n");
        }
        final var settings = new CSVTableReaderNodeSettings();
        settings.m_settings.m_source.m_path = new FSLocation(FSCategory.LOCAL, file);

        final var tableReadConfigProvider = new TableReadConfigProvider();
        tableReadConfigProvider.init(getTableReadConfigProviderStateProviderInitializer(settings));

        final var typedReaderTableSpecProvider = new TypedReaderTableSpecProvider();
        typedReaderTableSpecProvider
            .init(getTypedReaderTableSpecProviderStateProviderInitializer(settings, tableReadConfigProvider));

        return typedReaderTableSpecProvider;
    }

    private static final StateProviderInitializer getTypedReaderTableSpecProviderStateProviderInitializer(
        final CSVTableReaderNodeSettings settings, final TableReadConfigProvider tableReadConfigProvider) {
        return new StateProviderInitializer() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Supplier<T> getValueSupplier(final Class<? extends Reference<T>> ref) {
                if (ref.equals(FileChooserRef.class)) {
                    return () -> (T)settings.m_settings.m_source;
                }
                throw new IllegalStateException(String.format("Unexpected dependency %s", ref.getSimpleName()));
            }

            @Override
            public <T> void computeOnValueChange(final Class<? extends Reference<T>> id) {
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
            @SuppressWarnings("unchecked")
            public <T> Supplier<T>
                computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
                if (stateProviderClass.equals(TableReadConfigProvider.class)) {
                    return () -> (T)tableReadConfigProvider.computeState(null);
                }
                throw new IllegalStateException(
                    String.format("Unexpected dependency %s", stateProviderClass.getSimpleName()));
            }

            @Override
            public void computeBeforeOpenDialog() {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public void computeAfterOpenDialog() {
                throw new IllegalAccessError("Should not be called within this test");
            }
        };
    }

    @Test
    void testCSVTableSpecProvider() throws IOException {
        final var file = m_tempFolder.resolve("file.csv").toAbsolutePath().toString();
        final var typedReaderTableSpecProvider = createTypedReaderTableSpecProvider(file);

        final var tableSpecProvider = new CSVTableSpecProvider();
        tableSpecProvider.init(getCSVTableSpecProviderStateProviderInitializer(typedReaderTableSpecProvider));
        final var specs = tableSpecProvider.computeState(null);

        assertThat(specs).hasSize(1);
        assertThat(specs[0].m_spec).hasSize(2);
        assertThat(specs[0].m_sourceId).isEqualTo(file);

        assertThat(specs[0].m_spec[0].m_name).isEqualTo("intCol");
        assertThat(specs[0].m_spec[0].m_type).isEqualTo(Integer.class);

        assertThat(specs[0].m_spec[1].m_name).isEqualTo("stringCol");
        assertThat(specs[0].m_spec[1].m_type).isEqualTo(String.class);
    }

    private static final StateProviderInitializer getCSVTableSpecProviderStateProviderInitializer(
        final TypedReaderTableSpecProvider typedReaderTableSpecProvider) {
        return new StateProviderInitializer() {
            @Override
            public <T> Supplier<T> getValueSupplier(final Class<? extends Reference<T>> ref) {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public <T> void computeOnValueChange(final Class<? extends Reference<T>> ref) {
                // Do nothing
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
            @SuppressWarnings("unchecked")
            public <T> Supplier<T>
                computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
                if (stateProviderClass.equals(TypedReaderTableSpecProvider.class)) {
                    return () -> (T)typedReaderTableSpecProvider.computeState(null);
                }
                throw new IllegalStateException(
                    String.format("Unexpected dependency %s", stateProviderClass.getSimpleName()));
            }

            @Override
            public void computeBeforeOpenDialog() {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public void computeAfterOpenDialog() {
                // Do nothing
            }
        };
    }
}
