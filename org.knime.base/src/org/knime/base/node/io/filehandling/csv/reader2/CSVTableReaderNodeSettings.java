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
 *   Dec 11, 2023 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.knime.base.node.io.filehandling.csv.reader.OSIndependentNewLineReader;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.EscapeUtils;
import org.knime.base.node.io.filehandling.csv.reader2.CSVFormatProvider.ProviderFromCSVFormat;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.ColumnAndDataTypeDetection.LimitMemoryPerColumn;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.ColumnAndDataTypeDetection.LimitScannedRows;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.ColumnAndDataTypeDetection.MaximumNumberOfColumns;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.DataArea.FirstColumnContainsRowIds;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.DataArea.FirstRowContainsColumnNames;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.DataArea.IfRowHasLessColumns;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.File.CustomEncoding;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.File.FileEncoding;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.AutodetectFormat;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.ColumnDelimiter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.CommentLineCharacter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.CustomRowDelimiter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.NumberOfCharactersForAutodetection;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.QuoteCharacters.QuoteCharacter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.QuoteCharacters.QuoteEscapeCharacter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.QuotedStringsContainNoRowDelimiters;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.RowDelimiter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.SkipFirstLines;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.MulitpleFileHandling.PrependFileIndexToRowId;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.Values.DecimalSeparator;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.Values.QuotedStrings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.Values.ReplaceEmptyQuotedStringsByMissingValues;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.Values.ThousandsSeparator;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.IfRowHasLessColumnsOption.IfRowHasLessColumnsOptionPersistor;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.SetCSVExtensions;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.SetTitleAndDescriptionForUseExistingRowIds;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout.DataArea.LimitNumberOfRows;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout.DataArea.MaximumNumberOfRows;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout.DataArea.SkipFirstDataRows;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.Icon;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoice;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.MaxLengthValidation.HasAtMaxOneCharValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsSingleCharacterValidation;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class CSVTableReaderNodeSettings implements NodeParameters {

    @Persist(configKey = "settings")
    Settings m_settings = new Settings();

    @Persist(configKey = "advanced_settings")
    AdvancedSettings m_advancedSettings = new AdvancedSettings();

    @Persist(configKey = "limit_rows")
    LimitRows m_limitRows = new LimitRows();

    @Persist(configKey = "encoding")
    Encoding m_encoding = new Encoding();

    @Persistor(CSVTransformationSettingsPersistor.class)
    CSVTransformationSettings m_tableSpecConfig = new CSVTransformationSettings();

    @Modification({SetCSVExtensions.class, SetTitleAndDescriptionForUseExistingRowIds.class})
    static class Settings extends CommonReaderNodeSettings.SettingsWithRowId {

        static final class SetCSVExtensions
            extends CommonReaderNodeSettings.BaseSettings.SetFileReaderWidgetExtensions {

            @Override
            protected String[] getExtensions() {
                return new String[]{"csv", "tsv", "txt", "gz"};
            }

        }

        static final class SetTitleAndDescriptionForUseExistingRowIds implements Modification.Modifier {

            @Override
            public void modify(final Modification.WidgetGroupModifier group) {
                group.find(UseExistingRowIdWidgetRef.class).modifyAnnotation(Widget.class)
                    .withProperty("title", FirstColumnContainsRowIds.TITLE)
                    .withProperty("description", FirstColumnContainsRowIds.DESCRIPTION).modify();
            }

        }

        static class FirstRowContainsColumnNamesRef extends ReferenceStateProvider<Boolean> {
        }

        @Widget(title = "First row contains column names", description = FirstRowContainsColumnNames.DESCRIPTION)
        @ValueReference(FirstRowContainsColumnNamesRef.class)
        @Layout(FirstRowContainsColumnNames.class)
        @Persist(configKey = "has_column_header")
        boolean m_firstRowContainsColumnNames = true;

        enum IfRowHasLessColumnsOption {
                @Label(value = "Fail", description = IfRowHasLessColumns.DESCRIPTION_FAIL) //
                FAIL, //
                @Label(value = "Insert missing", description = IfRowHasLessColumns.DESCRIPTION_INSERT_MISSING) //
                INSERT_MISSING; //

            static final class IfRowHasLessColumnsOptionPersistor
                implements NodeParametersPersistor<IfRowHasLessColumnsOption> {

                static final String CFG_KEY = "support_short_data_rows";

                @Override
                public IfRowHasLessColumnsOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
                    return settings.getBoolean(CFG_KEY) ? INSERT_MISSING : FAIL;
                }

                @Override
                public void save(final IfRowHasLessColumnsOption ifRowHasLessColumnsOption,
                    final NodeSettingsWO settings) {
                    settings.addBoolean(CFG_KEY, ifRowHasLessColumnsOption == INSERT_MISSING);
                }

                @Override
                public String[][] getConfigPaths() {
                    return new String[][]{{CFG_KEY}};
                }
            }
        }

        @Widget(title = "If row has fewer columns", description = IfRowHasLessColumns.DESCRIPTION)
        @ValueSwitchWidget
        @Layout(IfRowHasLessColumns.class)
        @Persistor(IfRowHasLessColumnsOptionPersistor.class)
        IfRowHasLessColumnsOption m_ifRowHasLessColumnsOption = IfRowHasLessColumnsOption.FAIL;
        // TODO NOSONAR defaults are currently not applied when the node is created anew; will be addressed in UIEXT-1740

        @Persist(configKey = "skip_empty_data_rows")
        boolean m_skipEmptyDataRows;

        @Widget(title = "Prepend file index to RowID", description = PrependFileIndexToRowId.DESCRIPTION)
        @Layout(PrependFileIndexToRowId.class)
        @Persist(configKey = "prepend_file_idx_to_row_id")
        boolean m_prependFileIndexToRowId;
        // TODO NOSONAR this setting should be shown when reading multiple files; currently blocked by UIEXT-1805

        static final class CommentStartRef extends ReferenceStateProvider<String> {
        }

        @Widget(title = "Comment line character", description = CommentLineCharacter.DESCRIPTION)
        @ValueReference(CommentStartRef.class)
        @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
        @Layout(CommentLineCharacter.class)
        @Persist(configKey = "comment_char")
        String m_commentLineCharacter = "#";

        static final class ColumnDelimiterProvider extends ProviderFromCSVFormat<String> {

            @Override
            public String computeState(final NodeParametersInput context) {
                return EscapeUtils.escape(getCsvFormat().getDelimiterString());
            }

        }

        static class ColumnDelimiterRef extends ReferenceStateProvider<String> {
        }

        static final class ColumnDelimiterPersistor extends StringEscapePersistor {

            ColumnDelimiterPersistor() {
                super("column_delimiter");
            }
        }

        @Widget(title = "Column delimiter", description = ColumnDelimiter.DESCRIPTION)
        @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
        @Layout(ColumnDelimiter.class)
        @Persistor(ColumnDelimiterPersistor.class)
        @ValueReference(ColumnDelimiterRef.class)
        @ValueProvider(ColumnDelimiterProvider.class)
        String m_columnDelimiter = ",";

        static final class QuoteCharacterProvider extends ProviderFromCSVFormat<String> {
            @Override
            public String computeState(final NodeParametersInput context) {
                return Character.toString(getCsvFormat().getQuote());
            }
        }

        static class QuoteCharacterRef extends ReferenceStateProvider<String> {
        }

        @Widget(title = "Quote character", description = QuoteCharacter.DESCRIPTION)
        @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
        @Layout(QuoteCharacter.class)
        @Persist(configKey = "quote_char")
        @ValueReference(QuoteCharacterRef.class)
        @ValueProvider(QuoteCharacterProvider.class)
        String m_quoteCharacter = "\"";

        static final class QuoteEscapeCharacterProvider extends ProviderFromCSVFormat<String> {
            @Override
            public String computeState(final NodeParametersInput context) {
                return Character.toString(getCsvFormat().getQuoteEscape());
            }
        }

        static class QuoteEscapeCharacterRef extends ReferenceStateProvider<String> {
        }

        @Widget(title = "Quote escape character", description = QuoteEscapeCharacter.DESCRIPTION)
        @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
        @Layout(QuoteEscapeCharacter.class)
        @Persist(configKey = "quote_escape_char")
        @ValueReference(QuoteEscapeCharacterRef.class)
        @ValueProvider(QuoteEscapeCharacterProvider.class)
        String m_quoteEscapeCharacter = "\"";

        enum RowDelimiterOption {
                @Label(value = "Line break", description = RowDelimiter.DESCRIPTION_LINE_BREAK) //
                LINE_BREAK, //
                @Label(value = "Custom", description = RowDelimiter.DESCRIPTION_CUSTOM) //
                CUSTOM; //

            static final class RowDelimiterPersistor implements NodeParametersPersistor<RowDelimiterOption> {

                static final String CFG_KEY = "use_line_break_row_delimiter";

                @Override
                public RowDelimiterOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
                    return settings.getBoolean(CFG_KEY) ? LINE_BREAK : CUSTOM;
                }

                @Override
                public void save(final RowDelimiterOption rowDelimiteroption, final NodeSettingsWO settings) {
                    settings.addBoolean(CFG_KEY, rowDelimiteroption == LINE_BREAK);
                }

                @Override
                public String[][] getConfigPaths() {
                    return new String[][]{{CFG_KEY}};
                }
            }
        }

        static final class RowDelimiterOptionProvider extends ProviderFromCSVFormat<RowDelimiterOption> {

            @Override
            public RowDelimiterOption computeState(final NodeParametersInput context) {
                if (OSIndependentNewLineReader.isLineBreak(getCsvFormat().getLineSeparatorString())) {
                    return RowDelimiterOption.LINE_BREAK;
                }
                return RowDelimiterOption.CUSTOM;
            }

        }

        static class RowDelimiterOptionRef extends ReferenceStateProvider<RowDelimiterOption> {
        }

        static final class HasCustomRowDelimiter implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(RowDelimiterOptionRef.class).isOneOf(RowDelimiterOption.CUSTOM);
            }

        }

        @Widget(title = "Row delimiter", description = RowDelimiter.DESCRIPTION)
        @ValueSwitchWidget
        @Layout(RowDelimiter.class)
        @Persistor(RowDelimiterOption.RowDelimiterPersistor.class)
        @ValueReference(RowDelimiterOptionRef.class)
        @ValueProvider(RowDelimiterOptionProvider.class)
        RowDelimiterOption m_rowDelimiterOption = RowDelimiterOption.LINE_BREAK;

        static final class CustomRowDelimiterProvider extends ProviderFromCSVFormat<String> {
            @Override
            public String computeState(final NodeParametersInput context) {
                return EscapeUtils.escape(getCsvFormat().getLineSeparatorString());
            }

        }

        static class CustomRowDelimiterRef extends ReferenceStateProvider<String> {
        }

        static final class CustomRowDelimiterPersistor extends StringEscapePersistor {
            CustomRowDelimiterPersistor() {
                super("row_delimiter");
            }
        }

        static final class CustomRowDelimiterPatternValidation extends PatternValidation {
            @Override
            protected String getPattern() {
                return ".|\\\\(t|r|n)|\\\\r\\\\n";
            }

            @Override
            public String getErrorMessage() {
                return "The value must be a single character, “\\t”, ”\\n”, “\\r”, or “\\r\\n”.";
            }
        }

        @Widget(title = "Custom row delimiter", description = CustomRowDelimiter.DESCRIPTION)
        @TextInputWidget(patternValidation = CustomRowDelimiterPatternValidation.class)
        @Layout(CustomRowDelimiter.class)
        @Effect(predicate = HasCustomRowDelimiter.class, type = EffectType.SHOW)
        @Persistor(CustomRowDelimiterPersistor.class)
        @ValueReference(CustomRowDelimiterRef.class)
        @ValueProvider(CustomRowDelimiterProvider.class)
        String m_customRowDelimiter = "\n";

        static final class BufferSizeRef implements ParameterReference<Integer> {
        }

        @Widget(title = "Number of characters for autodetection",
            description = NumberOfCharactersForAutodetection.DESCRIPTION, advanced = true)
        @ValueReference(BufferSizeRef.class)
        @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
        @Layout(NumberOfCharactersForAutodetection.class)
        @Persist(configKey = "autodetect_buffer_size")
        int m_numberOfCharactersForAutodetection = CSVTableReaderConfig.DEFAULT_AUTODETECTION_BUFFER_SIZE;
        // TODO will be moved into a settings panel in UIEXT-1739

        static final class AutoDetectButtonRef implements ButtonReference {
        }

        @Widget(title = "Autodetect format", description = AutodetectFormat.DESCRIPTION)
        @Layout(AutodetectFormat.class)
        @SimpleButtonWidget(ref = AutoDetectButtonRef.class, icon = Icon.RELOAD)
        @Effect(predicate = FileSystemPortConnectionUtil.ConnectedWithoutFileSystemSpec.class,
            type = EffectType.DISABLE)
        Void m_autoDetectButton;

        static class FileSelectionInternal implements WidgetGroup, Persistable {
            @Persist(configKey = "SettingsModelID")
            String m_settingsModelID = "SMID_ReaderFileChooser";

            @Persist(configKey = "EnabledStatus")
            boolean m_enabledStatus = true;
        }
    }

    static class AdvancedSettings extends CommonReaderNodeSettings.AdvancedSettingsWithMultipleFileHandling {

        static class LimitScannedRowsRef extends ReferenceStateProvider<Boolean> {
        }

        static final class LimitScannedRowsPredicate implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getBoolean(LimitScannedRowsRef.class).isTrue();
            }

        }

        @Widget(title = "Limit scanned rows", description = LimitScannedRows.DESCRIPTION)
        @ValueReference(LimitScannedRowsRef.class)
        @Layout(LimitScannedRows.class)
        @Persist(configKey = "limit_data_rows_scanned")
        boolean m_limitScannedRows = true;
        // TODO merge into a single widget with UIEXT-1742

        static class MaxDataRowsScannedRef extends ReferenceStateProvider<Long> {
        }

        @Widget(title = "Max data rows scanned", description = "") // Not shown in the dialog but required for the Node Description
        @WidgetInternal(hideControlHeader = true)
        @ValueReference(MaxDataRowsScannedRef.class)
        @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
        @Layout(LimitScannedRows.class)
        @Effect(predicate = LimitScannedRowsPredicate.class, type = EffectType.SHOW)
        @Persist(configKey = "max_data_rows_scanned")
        long m_maxDataRowsScanned = 10000;

        static class LimitMemoryPerColumnRef extends ReferenceStateProvider<Boolean> {
        }

        @Widget(title = "Limit memory per column", description = LimitMemoryPerColumn.DESCRIPTION)
        @ValueReference(LimitMemoryPerColumnRef.class)
        @Layout(LimitMemoryPerColumn.class)
        @Persist(configKey = "limit_memory_per_column")
        boolean m_limitMemoryPerColumn = true;

        static class MaximumNumberOfColumnsRef extends ReferenceStateProvider<Integer> {
        }

        @Widget(title = "Maximum number of columns", description = MaximumNumberOfColumns.DESCRIPTION)
        @ValueReference(MaximumNumberOfColumnsRef.class)
        @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
        @Layout(MaximumNumberOfColumns.class)
        @Persist(configKey = "maximum_number_of_columns")
        int m_maximumNumberOfColumns = 8192;

        enum QuotedStringsOption {
                @Label(value = "Remove quotes and trim whitespace",
                    description = QuotedStrings.DESCRIPTION_REMOVE_QUOTES_AND_TRIM) //
                REMOVE_QUOTES_AND_TRIM, //
                @Label(value = "Keep quotes", description = QuotedStrings.DESCRIPTION_KEEP_QUOTES) //
                KEEP_QUOTES; //
        }

        static class QuotedStringsOptionRef extends ReferenceStateProvider<QuotedStringsOption> {
        }

        @Widget(title = "Quoted strings", description = QuotedStrings.DESCRIPTION, advanced = true)
        @ValueReference(QuotedStringsOptionRef.class)
        @RadioButtonsWidget
        @Layout(QuotedStrings.class)
        @Persist(configKey = "quote_option")
        QuotedStringsOption m_quotedStringsOption = QuotedStringsOption.REMOVE_QUOTES_AND_TRIM;

        static class ReplaceEmptyQuotedStringsByMissingValuesRef extends ReferenceStateProvider<Boolean> {
        }

        @Widget(title = "Replace empty quoted string by missing values",
            description = ReplaceEmptyQuotedStringsByMissingValues.DESCRIPTION)
        @ValueReference(ReplaceEmptyQuotedStringsByMissingValuesRef.class)
        @Layout(ReplaceEmptyQuotedStringsByMissingValues.class)
        @Persist(configKey = "replace_empty_quotes_with_missing")
        boolean m_replaceEmptyQuotedStringsByMissingValues = true;

        @Widget(title = "Quoted strings contain no row delimiters",
            description = QuotedStringsContainNoRowDelimiters.DESCRIPTION)
        @Layout(QuotedStringsContainNoRowDelimiters.class)
        @Persist(configKey = "no_row_delimiters_in_quotes")
        boolean m_quotedStringsContainNoRowDelimiters;

        @Persist(configKey = "min_chunk_size_in_bytes")
        long m_minChunkSizeInBytes = 67108864;

        @Persist(configKey = "max_num_chunks_per_file")
        int m_maxNumChunksPerFile = 8;

        static class ThousandsSeparatorRef extends ReferenceStateProvider<String> {
        }

        @Widget(title = "Thousands separator", description = ThousandsSeparator.DESCRIPTION)
        @ValueReference(ThousandsSeparatorRef.class)
        @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
        @Layout(ThousandsSeparator.class)
        @Persist(configKey = "thousands_separator")
        String m_thousandsSeparator = "";

        static class DecimalSeparatorRef extends ReferenceStateProvider<String> {
        }

        @Widget(title = "Decimal separator", description = DecimalSeparator.DESCRIPTION)
        @ValueReference(DecimalSeparatorRef.class)
        @TextInputWidget(patternValidation = IsSingleCharacterValidation.class)
        @Layout(DecimalSeparator.class)
        @Persist(configKey = "decimal_separator")
        String m_decimalSeparator = ".";

    }

    static class LimitRows implements WidgetGroup, Persistable {

        static final class SkipFirstLinesPersistor implements NodeParametersPersistor<Long> {

            private static final String CFG_SKIP_LINES = "skip_lines";

            private static final String CFG_NUMBER_OF_LINES_TO_SKIP = "number_of_lines_to_skip";

            @Override
            public Long load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return settings.getBoolean(CFG_SKIP_LINES) ? settings.getLong(CFG_NUMBER_OF_LINES_TO_SKIP) : 0;
            }

            @Override
            public void save(final Long skipFirstLines, final NodeSettingsWO settings) {
                settings.addBoolean(CFG_SKIP_LINES, skipFirstLines > 0);
                settings.addLong(CFG_NUMBER_OF_LINES_TO_SKIP, skipFirstLines);
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{CFG_SKIP_LINES}, {CFG_NUMBER_OF_LINES_TO_SKIP}};
            }
        }

        static final class SkipFirstLinesRef extends ReferenceStateProvider<Long> {
        }

        @Widget(title = "Skip first lines of file", description = SkipFirstLines.DESCRIPTION)
        @ValueReference(SkipFirstLinesRef.class)
        @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
        @Layout(SkipFirstLines.class)
        @Persistor(SkipFirstLinesPersistor.class)
        long m_skipFirstLines;

        static class SkipFirstDataRowsRef extends ReferenceStateProvider<Long> {
        }

        @Widget(title = "Skip first data rows", description = SkipFirstDataRows.DESCRIPTION)
        @ValueReference(SkipFirstDataRowsRef.class)
        @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
        @Layout(SkipFirstDataRows.class)
        @Persistor(CommonReaderNodeSettings.SkipFirstDataRowsPersistor.class)
        long m_skipFirstDataRows;

        @Widget(title = "Limit number of rows", description = LimitNumberOfRows.DESCRIPTION, advanced = true)
        @Layout(LimitNumberOfRows.class)
        @ValueReference(CommonReaderNodeSettings.LimitNumberOfRowsRef.class)
        @Persist(configKey = "limit_data_rows")
        boolean m_limitNumberOfRows;
        // TODO merge into a single widget with UIEXT-1742

        @Widget(title = "Maximum number of rows", description = MaximumNumberOfRows.DESCRIPTION)
        @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
        @Layout(MaximumNumberOfRows.class)
        @Effect(predicate = CommonReaderNodeSettings.LimitNumberOfRowsPredicate.class, type = EffectType.SHOW)
        @Persist(configKey = "max_rows")
        long m_maximumNumberOfRows = 50;
    }

    static class Encoding implements WidgetGroup, Persistable {

        static class Charset implements WidgetGroup, Persistable {

            enum FileEncodingOption {
                    @Label(value = "OS default", description = FileEncoding.DESCRIPTION_DEFAULT) //
                    DEFAULT(null, false, "OS default (" + java.nio.charset.Charset.defaultCharset().name() + ")"), //
                    @Label(value = "ISO-8859-1", description = FileEncoding.DESCRIPTION_ISO_8859_1) //
                    ISO_8859_1("ISO-8859-1", true), //
                    @Label(value = "US-ASCII", description = FileEncoding.DESCRIPTION_US_ASCII) //
                    US_ASCII("US-ASCII", true), //
                    @Label(value = "UTF-8", description = FileEncoding.DESCRIPTION_UTF_8) //
                    UTF_8("UTF-8", true), //
                    @Label(value = "UTF-16", description = FileEncoding.DESCRIPTION_UTF_16) //
                    UTF_16("UTF-16", true), //
                    @Label(value = "UTF-16BE", description = FileEncoding.DESCRIPTION_UTF_16BE) //
                    UTF_16BE("UTF-16BE", true), //
                    @Label(value = "UTF-16LE", description = FileEncoding.DESCRIPTION_UTF_16LE) //
                    UTF_16LE("UTF-16LE", true), //
                    @Label(value = "Other", description = FileEncoding.DESCRIPTION_OTHER) //
                    OTHER("", false); //

                final String m_persistId;

                final String m_nonConstantDisplayText;

                FileEncodingOption(final String persistId, final boolean isUnambigous) {
                    this(persistId, isUnambigous, persistId);
                }

                FileEncodingOption(final String persistId, final boolean isUnambigous, final String displayText) {
                    m_persistId = persistId;
                    m_nonConstantDisplayText = displayText;
                }

                static FileEncodingOption fromPersistId(final String persistId) {
                    return Arrays.stream(FileEncodingOption.values())
                        .filter(fileEncoding -> Objects.equals(fileEncoding.m_persistId, persistId)).findFirst()
                        .orElse(OTHER);
                }

                EnumChoice<FileEncodingOption> toEnumChoice() {
                    if (m_nonConstantDisplayText == null) {
                        return EnumChoice.fromEnumConst(this);
                    }
                    return new EnumChoice<>(this, m_nonConstantDisplayText);
                }
            }

            /**
             * This provider is needed to display the non-constant display text of the default option.
             */
            static final class EncodingChoicesProvider implements EnumChoicesProvider<FileEncodingOption> {

                @Override
                public List<EnumChoice<FileEncodingOption>> computeState(final NodeParametersInput context) {
                    return Arrays.stream(FileEncodingOption.values()).map(FileEncodingOption::toEnumChoice).toList();
                }
            }

            static class FileEncodingRef extends ReferenceStateProvider<FileEncodingOption> {
            }

            static final class IsOtherEncoding implements EffectPredicateProvider {

                @Override
                public EffectPredicate init(final PredicateInitializer i) {
                    return i.getEnum(FileEncodingRef.class).isOneOf(FileEncodingOption.OTHER);
                }

            }

            @Widget(title = "File encoding", description = FileEncoding.DESCRIPTION, advanced = true)
            @ValueReference(FileEncodingRef.class)
            @ChoicesProvider(EncodingChoicesProvider.class)
            @Layout(FileEncoding.class)
            FileEncodingOption m_fileEncoding;

            static class CustomEncodingRef extends ReferenceStateProvider<String> {
            }

            @Widget(title = "Custom encoding", description = CustomEncoding.DESCRIPTION, advanced = true)
            @ValueReference(CustomEncodingRef.class)
            @Layout(CustomEncoding.class)
            @Effect(predicate = IsOtherEncoding.class, type = EffectType.SHOW)
            String m_customEncoding;

            Charset() {
                this(FileEncodingOption.DEFAULT);
            }

            Charset(final FileEncodingOption fileEncoding) {
                this(fileEncoding, "");
            }

            Charset(final FileEncodingOption fileEncoding, final String customEncoding) {
                m_fileEncoding = fileEncoding;
                m_customEncoding = customEncoding;
            }

            String toPersistString() {
                if (this.m_fileEncoding == FileEncodingOption.OTHER) {
                    return this.m_customEncoding;
                }
                return this.m_fileEncoding.m_persistId;
            }

            java.nio.charset.Charset toNioCharset() throws IllegalCharsetNameException, UnsupportedCharsetException {
                final var persistString = this.toPersistString();
                return persistString == null //
                    ? java.nio.charset.Charset.defaultCharset() //
                    : java.nio.charset.Charset.forName(persistString);
            }

        }

        static final class CharsetPersistor implements NodeParametersPersistor<Charset> {

            static final String CFG_KEY = "charset";

            @Override
            public Charset load(final NodeSettingsRO settings) throws InvalidSettingsException {
                final var persistId = settings.getString(CFG_KEY, null);
                final var fileEncoding = FileEncodingOption.fromPersistId(persistId);
                if (fileEncoding == FileEncodingOption.OTHER) {
                    return new Charset(fileEncoding, persistId);
                }
                return new Charset(fileEncoding);
            }

            @Override
            public void save(final Charset charset, final NodeSettingsWO settings) {
                settings.addString(CFG_KEY, charset.toPersistString());
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{CFG_KEY}};
            }
        }

        static final class CharsetRef implements ParameterReference<CSVTableReaderNodeSettings.Encoding.Charset> {
        }

        @Persistor(CharsetPersistor.class)
        @ValueReference(CharsetRef.class)
        Charset m_charset = new Charset();
    }
}
