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
 *   May 15, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReader;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.QuoteOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVFormatProvider.BufferSizeRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVFormatProvider.CommentStartRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVFormatProvider.FileChooserRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVFormatProvider.SkipFirstLinesRef;
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
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.ColumnDelimiterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.CustomRowDelimiterRef;
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
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class CSVTableSpec implements WidgetGroup, PersistableSettings {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CSVTableSpec.class);

    static final class TableReadConfigProvider implements StateProvider<TableReadConfig<CSVTableReaderConfig>> {

        private Supplier<Boolean> m_firstRowContainsColumnNamesSupplier;

        private Supplier<Boolean> m_firstColumnContainsRowIdsSupplier;

        private Supplier<String> m_commentLineCharacterSupplier;

        private Supplier<String> m_columnDelimiterSupplier;

        private Supplier<String> m_quoteCharacterSupplier;

        private Supplier<String> m_quoteEscapeCharacterSupplier;

        private Supplier<RowDelimiterOption> m_rowDelimiterOptionSupplier;

        private Supplier<String> m_customRowDelimiterSupplier;

        private Supplier<QuotedStringsOption> m_quotedStringsOptionSupplier;

        private Supplier<Boolean> m_replaceEmptyQuotedStringsByMissingValuesSupplier;

        private Supplier<Boolean> m_limitScannedRowsSupplier;

        private Supplier<Long> m_maxDataRowsScannedSupplier;

        private Supplier<String> m_thousandsSeparatorSupplier;

        private Supplier<String> m_decimalSeparatorSupplier;

        private Supplier<FileEncodingOption> m_fileEncodingSupplier;

        private Supplier<String> m_customEncodingSupplier;

        private Supplier<Long> m_skipFirstLinesSupplier;

        private Supplier<Long> m_skipFirstDataRowsSupplier;

        private Supplier<Integer> m_numberOfCharactersForAutodetectionSupplier;

        private Supplier<Integer> m_maximumNumberOfColumnsSupplier;

        private Supplier<Long> m_minChunkSizeInBytesSupplier;

        private Supplier<Integer> m_maxNumChunksPerFileSupplier;

        private Supplier<IfRowHasLessColumnsOption> m_ifRowHasLessColumnsOptionSupplier;

        private Supplier<Boolean> m_limitNumberOfRowsSupplier;

        private Supplier<Long> m_maximumNumberOfRowsSupplier;

        private Supplier<Boolean> m_prependFileIndexToRowIdSupplier;

        private Supplier<Boolean> m_skipEmptyDataRowsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            // fields contained in the config id
            m_firstRowContainsColumnNamesSupplier = initializer.getValueSupplier(FirstRowContainsColumnNamesRef.class);
            m_firstColumnContainsRowIdsSupplier = initializer.getValueSupplier(FirstColumnContainsRowIdsRef.class);
            m_commentLineCharacterSupplier = initializer.getValueSupplier(CommentStartRef.class);
            m_columnDelimiterSupplier = initializer.getValueSupplier(ColumnDelimiterRef.class);
            m_quoteCharacterSupplier = initializer.getValueSupplier(QuoteCharacterRef.class);
            m_quoteEscapeCharacterSupplier = initializer.getValueSupplier(QuoteEscapeCharacterRef.class);
            m_rowDelimiterOptionSupplier = initializer.getValueSupplier(RowDelimiterOptionRef.class);
            m_customRowDelimiterSupplier = initializer.getValueSupplier(CustomRowDelimiterRef.class);
            m_quotedStringsOptionSupplier = initializer.getValueSupplier(QuotedStringsOptionRef.class);
            m_replaceEmptyQuotedStringsByMissingValuesSupplier =
                initializer.getValueSupplier(ReplaceEmptyQuotedStringsByMissingValuesRef.class);
            m_limitScannedRowsSupplier = initializer.getValueSupplier(LimitScannedRowsRef.class);
            m_maxDataRowsScannedSupplier = initializer.getValueSupplier(MaxDataRowsScannedRef.class);
            m_thousandsSeparatorSupplier = initializer.getValueSupplier(ThousandsSeparatorRef.class);
            m_decimalSeparatorSupplier = initializer.getValueSupplier(DecimalSeparatorRef.class);
            m_fileEncodingSupplier = initializer.getValueSupplier(FileEncodingRef.class);
            m_customEncodingSupplier = initializer.getValueSupplier(CustomEncodingRef.class);
            m_skipFirstLinesSupplier = initializer.getValueSupplier(SkipFirstLinesRef.class);
            m_skipFirstDataRowsSupplier = initializer.getValueSupplier(SkipFirstDataRowsRef.class);

            // other fields
            m_numberOfCharactersForAutodetectionSupplier = initializer.getValueSupplier(BufferSizeRef.class);
            m_maximumNumberOfColumnsSupplier = initializer.getValueSupplier(MaximumNumberOfColumnsRef.class);
            m_minChunkSizeInBytesSupplier = initializer.getValueSupplier(MinChunkSizeInBytesRef.class);
            m_maxNumChunksPerFileSupplier = initializer.getValueSupplier(MaxNumChunksPerFileRef.class);
            m_ifRowHasLessColumnsOptionSupplier = initializer.getValueSupplier(IfRowHasLessColumnsOptionRef.class);
            m_limitNumberOfRowsSupplier = initializer.getValueSupplier(LimitNumberOfRowsRef.class);
            m_maximumNumberOfRowsSupplier = initializer.getValueSupplier(MaximumNumberOfRowsRef.class);
            m_prependFileIndexToRowIdSupplier = initializer.getValueSupplier(PrependFileIndexToRowIdRef.class);
            m_skipEmptyDataRowsSupplier = initializer.getValueSupplier(SkipEmptyDataRowsRef.class);
        }

        @Override
        public TableReadConfig<CSVTableReaderConfig> computeState(final DefaultNodeSettingsContext context) {
            final var csvConfig = new CSVTableReaderConfig();
            csvConfig.setAutoDetectionBufferSize(m_numberOfCharactersForAutodetectionSupplier.get());
            final var encoding = m_fileEncodingSupplier.get();
            csvConfig.setCharSetName(
                encoding == FileEncodingOption.OTHER ? m_customEncodingSupplier.get() : encoding.m_persistId);
            csvConfig.setComment(m_commentLineCharacterSupplier.get());
            csvConfig.setDecimalSeparator(m_decimalSeparatorSupplier.get());
            csvConfig.setDelimiter(m_columnDelimiterSupplier.get());
            csvConfig.setLineSeparator(m_customRowDelimiterSupplier.get());
            csvConfig.setMaxColumns(m_maximumNumberOfColumnsSupplier.get());
            csvConfig.setMaxNumChunksPerFile(m_maxNumChunksPerFileSupplier.get());
            csvConfig.setMinChunkSizeInBytes(m_minChunkSizeInBytesSupplier.get());
            final var skipFirstLines = m_skipFirstLinesSupplier.get();
            csvConfig.setSkipLines(skipFirstLines > 0);
            csvConfig.setNumLinesToSkip(skipFirstLines);
            csvConfig.setQuote(m_quoteCharacterSupplier.get());
            csvConfig.setQuoteEscape(m_quoteEscapeCharacterSupplier.get());
            csvConfig.setQuoteOption(QuoteOption.valueOf(m_quotedStringsOptionSupplier.get().name()));
            csvConfig.setReplaceEmptyWithMissing(m_replaceEmptyQuotedStringsByMissingValuesSupplier.get());
            csvConfig.setThousandsSeparator(m_thousandsSeparatorSupplier.get());
            csvConfig.useLineBreakRowDelimiter(m_rowDelimiterOptionSupplier.get() == RowDelimiterOption.LINE_BREAK);

            final var tc = new DefaultTableReadConfig<>(csvConfig);
            tc.setAllowShortRows(m_ifRowHasLessColumnsOptionSupplier.get() == IfRowHasLessColumnsOption.INSERT_MISSING);
            tc.setColumnHeaderIdx(0);
            tc.setLimitRows(m_limitNumberOfRowsSupplier.get());
            tc.setLimitRowsForSpec(m_limitScannedRowsSupplier.get());
            tc.setMaxRows(m_maximumNumberOfRowsSupplier.get());
            tc.setMaxRowsForSpec(m_maxDataRowsScannedSupplier.get());
            final var skipFirstDataRows = m_skipFirstDataRowsSupplier.get();
            tc.setSkipRows(skipFirstDataRows > 0);
            tc.setNumRowsToSkip(skipFirstDataRows);
            tc.setPrependSourceIdxToRowId(m_prependFileIndexToRowIdSupplier.get());
            tc.setRowIDIdx(0);
            tc.setSkipEmptyRows(m_skipEmptyDataRowsSupplier.get());
            tc.setUseColumnHeaderIdx(m_firstRowContainsColumnNamesSupplier.get());
            tc.setUseRowIDIdx(m_firstColumnContainsRowIdsSupplier.get());

            return tc;
        }
    }

    abstract static class DependsOnTableReadConfigProvider<S> implements StateProvider<S> {

        Supplier<Map<String, TypedReaderTableSpec<Class<?>>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_specSupplier = initializer.computeFromProvidedState(TypedReaderTableSpecProvider.class);
            initializer.computeOnValueChange(FileChooserRef.class);

            initializer.computeOnValueChange(FirstRowContainsColumnNamesRef.class);
            initializer.computeOnValueChange(FirstColumnContainsRowIdsRef.class);
            initializer.computeOnValueChange(CommentStartRef.class);
            initializer.computeOnValueChange(ColumnDelimiterRef.class);
            initializer.computeOnValueChange(QuoteCharacterRef.class);
            initializer.computeOnValueChange(QuoteEscapeCharacterRef.class);
            initializer.computeOnValueChange(RowDelimiterOptionRef.class);
            initializer.computeOnValueChange(CustomRowDelimiterRef.class);
            initializer.computeOnValueChange(QuotedStringsOptionRef.class);

            initializer.computeOnValueChange(ReplaceEmptyQuotedStringsByMissingValuesRef.class);
            initializer.computeOnValueChange(LimitScannedRowsRef.class);
            initializer.computeOnValueChange(MaxDataRowsScannedRef.class);
            initializer.computeOnValueChange(ThousandsSeparatorRef.class);
            initializer.computeOnValueChange(DecimalSeparatorRef.class);
            initializer.computeOnValueChange(FileEncodingRef.class);
            initializer.computeOnValueChange(CustomEncodingRef.class);
            initializer.computeOnValueChange(SkipFirstLinesRef.class);
            initializer.computeOnValueChange(SkipFirstDataRowsRef.class);

            // other fields
            initializer.computeOnValueChange(BufferSizeRef.class);
            initializer.computeOnValueChange(MaximumNumberOfColumnsRef.class);
            initializer.computeOnValueChange(MinChunkSizeInBytesRef.class);
            initializer.computeOnValueChange(MaxNumChunksPerFileRef.class);
            initializer.computeOnValueChange(IfRowHasLessColumnsOptionRef.class);
            initializer.computeOnValueChange(LimitNumberOfRowsRef.class);
            initializer.computeOnValueChange(MaximumNumberOfRowsRef.class);
            initializer.computeOnValueChange(PrependFileIndexToRowIdRef.class);
            initializer.computeOnValueChange(SkipEmptyDataRowsRef.class);
        }
    }

    static final class TypedReaderTableSpecProvider
        implements StateProvider<Map<String, TypedReaderTableSpec<Class<?>>>> {

        private Supplier<FileChooser> m_fileChooserSupplier;

        private Supplier<TableReadConfig<CSVTableReaderConfig>> m_tableReaderConfigSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_fileChooserSupplier = initializer.getValueSupplier(FileChooserRef.class);
            m_tableReaderConfigSupplier = initializer.computeFromProvidedState(TableReadConfigProvider.class);
        }

        @Override
        public Map<String, TypedReaderTableSpec<Class<?>>> computeState(final DefaultNodeSettingsContext context) {

            final var fileChooser = m_fileChooserSupplier.get();
            if (fileChooser.getFSLocation().equals(new FSLocation(FSCategory.LOCAL, ""))) {
                return Collections.emptyMap(); // no file selected (yet)
            }

            try (final FileChooserPathAccessor accessor = new FileChooserPathAccessor(fileChooser)) {
                final var paths = accessor.getFSPaths(s -> {
                    switch (s.getType()) {
                        case INFO -> LOGGER.info(s.getMessage());
                        case WARNING -> LOGGER.info(s.getMessage());
                        case ERROR -> LOGGER.error(s.getMessage());
                    }
                });

                final var csvTableReader = new CSVTableReader();
                final var tc = m_tableReaderConfigSupplier.get();
                final var exec = new ExecutionMonitor();
                final var specs = new HashMap<String, TypedReaderTableSpec<Class<?>>>();
                for (var path : paths) {
                    specs.put(path.toFSLocation().getPath(),
                        MultiTableUtils.assignNamesIfMissing(csvTableReader.readSpec(path, tc, exec)));
                }
                return specs;
            } catch (IOException | InvalidSettingsException e) {
                LOGGER.error(e);
                return Collections.emptyMap();
            }
        }
    }

    static class CSVTableSpecProvider extends DependsOnTableReadConfigProvider<CSVTableSpec[]> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public CSVTableSpec[] computeState(final DefaultNodeSettingsContext context) {
            return m_specSupplier.get().entrySet().stream()
                .map(e -> new CSVTableSpec(e.getKey(), e.getValue().stream()
                    .map(spec -> new ColumnSpec(spec.getName().get(), spec.getType())).toArray(ColumnSpec[]::new)))
                .toArray(CSVTableSpec[]::new);
        }
    }

    static class ColumnSpec implements WidgetGroup, PersistableSettings {

        String m_name;

        Class<?> m_type;

        ColumnSpec(final String name, final Class<?> type) {
            m_name = name;
            m_type = type;
        }

        ColumnSpec() {
        }
    }

    String m_sourceId;

    ColumnSpec[] m_spec;

    CSVTableSpec(final String sourceId, final ColumnSpec[] spec) {
        m_sourceId = sourceId;
        m_spec = spec;
    }

    CSVTableSpec() {
    }
}
