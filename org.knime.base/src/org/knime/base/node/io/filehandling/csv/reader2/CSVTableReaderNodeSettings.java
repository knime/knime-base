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

import java.util.Arrays;
import java.util.Objects;

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.ColumnAndDataTypeDetection.IfSchemaChanges;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.ColumnAndDataTypeDetection.LimitMemoryPerColumn;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.ColumnAndDataTypeDetection.LimitScannedRows;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.ColumnAndDataTypeDetection.MaximumNumberOfColumns;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.DataArea.FirstColumnContainsRowIds;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.DataArea.FirstRowContainsColumnNames;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.DataArea.IfRowHasLessColumns;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.DataArea.LimitNumberOfRows;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.DataArea.MaximumNumberOfRows;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.DataArea.SkipFirstDataRows;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.File.CustomEncoding;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.File.FileEncoding;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.File.Source;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.ColumnDelimiter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.CommentLineCharacter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.CustomRowDelimiter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.NumberOfCharactersForAutodetection;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.QuoteCharacters.QuoteCharacter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.QuoteCharacters.QuoteEscapeCharacter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.QuotedStringsContainNoRowDelimiters;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.RowDelimiter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.SkipFirstLines;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.MultipleFileHandling.AppendFilePathColumn;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.MultipleFileHandling.FilePathColumnName;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.Values.DecimalSeparator;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.Values.QuotedStrings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.Values.ReplaceEmptyQuotedStringsByMissingValues;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.Values.ThousandsSeparator;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.IfRowHasLessColumnsOption.IfRowHasLessColumnsOptionPersistor;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOption.IsCustomRowDelimiter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOption.RowDelimiterPersistor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPreserverPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.rule.TrueCondition;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class CSVTableReaderNodeSettings implements DefaultNodeSettings {

    @Persist(configKey = "settings")
    Settings m_settings = new Settings();

    @Persist(configKey = "advanced_settings")
    AdvancedSettings m_advancedSettings = new AdvancedSettings();

    @Persist(configKey = "limit_rows")
    LimitRows m_limitRows = new LimitRows();

    @Persist(configKey = "encoding")
    Encoding m_encoding = new Encoding();

    @Persist(configKey = "table_spec_config", hidden = true, customPersistor = NodeSettingsPreserverPersistor.class)
    Void m_tableSpecConfig;

    static class Settings implements WidgetGroup, PersistableSettings {

        @Widget(title = "Source", description = Source.DESCRIPTION)
        @Layout(Source.class)
        @Persist(configKey = "file_selection", settingsModel = SettingsModelReaderFileChooser.class)
        FileChooser m_source = new FileChooser();

        @Persist(configKey = "file_selection", hidden = true)
        FileSelectionInternal m_fileSelectionInternal = new FileSelectionInternal();

        @Widget(title = "First row contains column names", description = FirstRowContainsColumnNames.DESCRIPTION)
        @Layout(FirstRowContainsColumnNames.class)
        @Persist(configKey = "has_column_header")
        boolean m_firstRowContainsColumnNames = true;

        @Widget(title = "First column contains RowIDs", description = FirstColumnContainsRowIds.DESCRIPTION)
        @Layout(FirstColumnContainsRowIds.class)
        @Persist(configKey = "has_row_id")
        boolean m_firstColumnContainsRowIds;

        enum IfRowHasLessColumnsOption {
                @Label(value = "Insert missing", description = IfRowHasLessColumns.DESCRIPTION_INSERT_MISSING) //
                INSERT_MISSING, //
                @Label(value = "Fail", description = IfRowHasLessColumns.DESCRIPTION_FAIL) //
                FAIL; //

            static final class IfRowHasLessColumnsOptionPersistor
                extends NodeSettingsPersistorWithConfigKey<IfRowHasLessColumnsOption> {

                @Override
                public IfRowHasLessColumnsOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
                    return settings.getBoolean(getConfigKey()) ? INSERT_MISSING : FAIL;
                }

                @Override
                public void save(final IfRowHasLessColumnsOption ifRowHasLessColumnsOption,
                    final NodeSettingsWO settings) {
                    settings.addBoolean(getConfigKey(), ifRowHasLessColumnsOption == INSERT_MISSING);
                }
            }
        }

        @Widget(title = "If row has less columns", description = IfRowHasLessColumns.DESCRIPTION)
        @ValueSwitchWidget
        @Layout(IfRowHasLessColumns.class)
        @Persist(configKey = "support_short_data_rows", customPersistor = IfRowHasLessColumnsOptionPersistor.class)
        IfRowHasLessColumnsOption m_ifRowHasLessColumnsOption = IfRowHasLessColumnsOption.INSERT_MISSING;
        // TODO defaults are currently not applied when the node is created anew; will be addressed in UIEXT-1740

        @Persist(configKey = "skip_empty_data_rows")
        boolean m_skipEmptyDataRows;

        // @Widget(title = "Prepend file index to RowID", description = PrependFileIndexToRowId.DESCRIPTION)
        // @Layout(PrependFileIndexToRowId.class)
        @Persist(configKey = "prepend_file_idx_to_row_id")
        boolean m_prependFileIndexToRowId;
        // TODO this setting should be shown when reading multiple files; currently blocked by UIEXT-1805

        @Widget(title = "Comment line character", description = CommentLineCharacter.DESCRIPTION)
        @TextInputWidget(maxLength = 1)
        @Layout(CommentLineCharacter.class)
        @Persist(configKey = "comment_char")
        String m_commentLineCharacter = "#";

        @Widget(title = "Column delimiter", description = ColumnDelimiter.DESCRIPTION)
        @TextInputWidget(minLength = 1)
        @Layout(ColumnDelimiter.class)
        @Persist(configKey = "column_delimiter", customPersistor = StringEscapePersistor.class)
        String m_columnDelimiter = ",";

        @Widget(title = "Quote character", description = QuoteCharacter.DESCRIPTION)
        @TextInputWidget(maxLength = 1)
        @Layout(QuoteCharacter.class)
        @Persist(configKey = "quote_char")
        String m_quoteCharacter = "\"";
        // TODO  we get occasional hard crashes when trying to persist invalid settings like this, addressed in NXT-2480

        @Widget(title = "Quote escape character", description = QuoteEscapeCharacter.DESCRIPTION)
        @TextInputWidget(maxLength = 1)
        @Layout(QuoteEscapeCharacter.class)
        @Persist(configKey = "quote_escape_char")
        String m_quoteEscapeCharacter = "\"";

        enum RowDelimiterOption {
                @Label(value = "Line break", description = RowDelimiter.DESCRIPTION_LINE_BREAK) //
                LINE_BREAK, //
                @Label(value = "Custom", description = RowDelimiter.DESCRIPTION_CUSTOM) //
                CUSTOM; //

            static final class IsCustomRowDelimiter extends OneOfEnumCondition<RowDelimiterOption> {

                @Override
                public RowDelimiterOption[] oneOf() {
                    return new RowDelimiterOption[]{RowDelimiterOption.CUSTOM};
                }
            }

            static final class RowDelimiterPersistor extends NodeSettingsPersistorWithConfigKey<RowDelimiterOption> {

                @Override
                public RowDelimiterOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
                    return settings.getBoolean(getConfigKey()) ? LINE_BREAK : CUSTOM;
                }

                @Override
                public void save(final RowDelimiterOption rowDelimiteroption, final NodeSettingsWO settings) {
                    settings.addBoolean(getConfigKey(), rowDelimiteroption == LINE_BREAK);
                }
            }
        }

        @Widget(title = "Row delimiter", description = RowDelimiter.DESCRIPTION)
        @ValueSwitchWidget
        @Layout(RowDelimiter.class)
        @Signal(condition = IsCustomRowDelimiter.class)
        @Persist(configKey = "use_line_break_row_delimiter", customPersistor = RowDelimiterPersistor.class)
        RowDelimiterOption m_rowDelimiterOption = RowDelimiterOption.LINE_BREAK;

        @Widget(title = "Custom row delimiter", description = CustomRowDelimiter.DESCRIPTION)
        @TextInputWidget(minLength = 1, pattern = ".|[\\t\\r\\n]|\\r\\n")
        @Layout(CustomRowDelimiter.class)
        @Effect(signals = IsCustomRowDelimiter.class, type = EffectType.SHOW)
        @Persist(configKey = "row_delimiter", customPersistor = StringEscapePersistor.class)
        String m_customRowDelimiter = "\n";

        @Widget(title = "Number of characters for autodetection",
            description = NumberOfCharactersForAutodetection.DESCRIPTION)
        @NumberInputWidget(min = 1)
        @Layout(NumberOfCharactersForAutodetection.class)
        @Persist(configKey = "autodetect_buffer_size")
        int m_numberOfCharactersForAutodetection = CSVTableReaderConfig.DEFAULT_AUTODETECTION_BUFFER_SIZE;
        // TODO will be moved into a settings panel in UIEXT-1739

        static class FileSelectionInternal implements WidgetGroup, PersistableSettings {

            @Persist(configKey = "SettingsModelID")
            String m_settingsModelID = "SMID_ReaderFileChooser";

            @Persist(configKey = "EnabledStatus")
            boolean m_enabledStatus = true;
        }
    }

    static class AdvancedSettings implements WidgetGroup, PersistableSettings {

        @Persist(configKey = "spec_merge_mode", hidden = true)
        String m_specMergeMode = "UNION";

        // @Widget(title = "Fail if specs differ", description = FailIfSpecsDiffer.DESCRIPTION)
        // @Layout(FailIfSpecsDiffer.class)
        @Persist(configKey = "fail_on_differing_specs")
        boolean m_failOnDifferingSpecs = true;
        // TODO this setting should be shown when reading multiple files; currently blocked by UIEXT-1805
        // TODO this setting will be replaced by a value seitch Fail if different / Union / Intersection in UIEXT-1800

        @Widget(title = "Append file path column", description = AppendFilePathColumn.DESCRIPTION)
        @Layout(AppendFilePathColumn.class)
        @Signal(id = AppendFilePathColumn.class, condition = TrueCondition.class)
        @Persist(configKey = "append_path_column", hidden = true)
        boolean m_appendPathColumn;

        @Widget(title = "File path column name", description = FilePathColumnName.DESCRIPTION)
        @Layout(FilePathColumnName.class)
        @Effect(signals = AppendFilePathColumn.class, type = EffectType.SHOW)
        @Persist(configKey = "path_column_name", hidden = true)
        String m_filePathColumnName = "File Path";

        @Widget(title = "Limit scanned rows", description = LimitScannedRows.DESCRIPTION)
        @Layout(LimitScannedRows.class)
        @Signal(id = LimitScannedRows.class, condition = TrueCondition.class)
        @Persist(configKey = "limit_data_rows_scanned")
        boolean m_limitScannedRows = true;
        // TODO merge into a single widget with UIEXT-1742

        @Widget(title = "", description = "", hideTitle = true)
        @NumberInputWidget(min = 0)
        @Layout(LimitScannedRows.class)
        @Effect(signals = LimitScannedRows.class, type = EffectType.SHOW)
        @Persist(configKey = "max_data_rows_scanned")
        long m_maxDataRowsScanned = 10000;

        @Widget(title = "Limit memory per column", description = LimitMemoryPerColumn.DESCRIPTION)
        @Layout(LimitMemoryPerColumn.class)
        @Persist(configKey = "limit_memory_per_column")
        boolean m_limitMemoryPerColumn = true;

        @Widget(title = "Maximum number of columns", description = MaximumNumberOfColumns.DESCRIPTION)
        @NumberInputWidget(min = 0)
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

        @Widget(title = "Quoted strings", description = QuotedStrings.DESCRIPTION, advanced = true)
        @RadioButtonsWidget
        @Layout(QuotedStrings.class)
        @Persist(configKey = "quote_option")
        QuotedStringsOption m_quotedStringsOption = QuotedStringsOption.REMOVE_QUOTES_AND_TRIM;

        @Widget(title = "Replace empty quoted string by missing values",
            description = ReplaceEmptyQuotedStringsByMissingValues.DESCRIPTION)
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

        @Widget(title = "Thousands separator", description = ThousandsSeparator.DESCRIPTION)
        @Layout(ThousandsSeparator.class)
        @Persist(configKey = "thousands_separator")
        String m_thousandsSeparator = "";

        @Widget(title = "Decimal separator", description = DecimalSeparator.DESCRIPTION)
        @TextInputWidget(minLength = 1)
        @Layout(DecimalSeparator.class)
        @Persist(configKey = "decimal_separator")
        String m_decimalSeparator = ".";

        enum IfSchemaChangesOption {
                @Label(value = "Fail", description = IfSchemaChanges.DESCRIPTION_FAIL) //
                FAIL, //
                @Label(value = "Use new schema", description = IfSchemaChanges.DESCRIPTION_USE_NEW_SCHEMA) //
                USE_NEW_SCHEMA, //
                @Label(value = "Ignore (deprecated)", description = IfSchemaChanges.DESCRIPTION_IGNORE, disabled = true)
                IGNORE; //
        }

        static final class IfSchemaChangesPersistor implements FieldNodeSettingsPersistor<IfSchemaChangesOption> {

            private static final String CFG_SAVE_TABLE_SPEC_CONFIG = "save_table_spec_config";

            private static final String CFG_CHECK_TABLE_SPEC = "check_table_spec";

            @Override
            public IfSchemaChangesOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
                final var saveTableSpecConfig = settings.getBoolean(CFG_SAVE_TABLE_SPEC_CONFIG, true);
                if (saveTableSpecConfig) {
                    if (settings.getBoolean(CFG_CHECK_TABLE_SPEC, false)) {
                        return IfSchemaChangesOption.FAIL;
                    } else {
                        return IfSchemaChangesOption.IGNORE;
                    }
                }
                return IfSchemaChangesOption.USE_NEW_SCHEMA;

            }

            @Override
            public void save(final IfSchemaChangesOption ifSchemaChangesOption, final NodeSettingsWO settings) {
                settings.addBoolean(CFG_SAVE_TABLE_SPEC_CONFIG,
                    ifSchemaChangesOption != IfSchemaChangesOption.USE_NEW_SCHEMA);
                settings.addBoolean(CFG_CHECK_TABLE_SPEC, ifSchemaChangesOption == IfSchemaChangesOption.FAIL);
            }

            @Override
            public String[] getConfigKeys() {
                return new String[]{CFG_SAVE_TABLE_SPEC_CONFIG, CFG_CHECK_TABLE_SPEC};
            }
        }

        @Widget(title = "If schema changes", description = IfSchemaChanges.DESCRIPTION)
        @RadioButtonsWidget
        @Layout(IfSchemaChanges.class)
        @Persist(customPersistor = IfSchemaChangesPersistor.class)
        IfSchemaChangesOption m_ifSchemaChangesOption = IfSchemaChangesOption.FAIL;
    }

    static class LimitRows implements WidgetGroup, PersistableSettings {

        static final class SkipFirstLinesPersistor implements FieldNodeSettingsPersistor<Long> {

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
            public String[] getConfigKeys() {
                return new String[]{CFG_SKIP_LINES, CFG_NUMBER_OF_LINES_TO_SKIP};
            }
        }

        @Widget(title = "Skip first lines of file", description = SkipFirstLines.DESCRIPTION)
        @NumberInputWidget(min = 0)
        @Layout(SkipFirstLines.class)
        @Persist(customPersistor = SkipFirstLinesPersistor.class)
        long m_skipFirstLines;

        static final class SkipFirstDataRowsPersistor implements FieldNodeSettingsPersistor<Long> {

            private static final String CFG_SKIP_DATA_ROWS = "skip_data_rows";

            private static final String CFG_NUMBER_OF_DATA_ROWS_TO_SKIP = "number_of_rows_to_skip";

            @Override
            public Long load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return settings.getBoolean(CFG_SKIP_DATA_ROWS) ? settings.getLong(CFG_NUMBER_OF_DATA_ROWS_TO_SKIP) : 0;
            }

            @Override
            public void save(final Long skipFirstDataRows, final NodeSettingsWO settings) {
                settings.addBoolean(CFG_SKIP_DATA_ROWS, skipFirstDataRows > 0);
                settings.addLong(CFG_NUMBER_OF_DATA_ROWS_TO_SKIP, skipFirstDataRows);
            }

            @Override
            public String[] getConfigKeys() {
                return new String[]{CFG_SKIP_DATA_ROWS, CFG_NUMBER_OF_DATA_ROWS_TO_SKIP};
            }
        }

        @Widget(title = "Skip first data rows", description = SkipFirstDataRows.DESCRIPTION)
        @NumberInputWidget(min = 0)
        @Layout(SkipFirstDataRows.class)
        @Persist(customPersistor = SkipFirstDataRowsPersistor.class)
        long m_skipFirstDataRows;

        @Widget(title = "Limit number of rows", description = LimitNumberOfRows.DESCRIPTION, advanced = true)
        @Layout(LimitNumberOfRows.class)
        @Signal(id = LimitNumberOfRows.class, condition = TrueCondition.class)
        @Persist(configKey = "limit_data_rows")
        boolean m_limitNumberOfRows;
        // TODO merge into a single widget with UIEXT-1742

        @Widget(title = "Maximum number of rows", description = MaximumNumberOfRows.DESCRIPTION)
        @NumberInputWidget(min = 0)
        @Layout(MaximumNumberOfRows.class)
        @Effect(signals = LimitNumberOfRows.class, type = EffectType.SHOW)
        @Persist(configKey = "max_rows")
        long m_maximumNumberOfRows = 50;
    }

    static class Encoding implements WidgetGroup, PersistableSettings {

        static class Charset implements WidgetGroup, PersistableSettings {

            enum FileEncodingOption {
                    @Label(value = "", description = FileEncoding.DESCRIPTION_DEFAULT) //
                    DEFAULT(null, false, "OS default (" + java.nio.charset.Charset.defaultCharset().name() + ")"), //
                    @Label(value = "", description = FileEncoding.DESCRIPTION_ISO_8859_1) //
                    ISO_8859_1("ISO-8859-1", true), //
                    @Label(value = "", description = FileEncoding.DESCRIPTION_US_ASCII) //
                    US_ASCII("US-ASCII", true), //
                    @Label(value = "", description = FileEncoding.DESCRIPTION_UTF_8) //
                    UTF_8("UTF-8", true), //
                    @Label(value = "", description = FileEncoding.DESCRIPTION_UTF_16) //
                    UTF_16("UTF-16", true), //
                    @Label(value = "", description = FileEncoding.DESCRIPTION_UTF_16BE) //
                    UTF_16BE("UTF-16BE", true), //
                    @Label(value = "", description = FileEncoding.DESCRIPTION_UTF_16LE) //
                    UTF_16LE("UTF-16LE", true), //
                    @Label(value = "", description = FileEncoding.DESCRIPTION_OTHER) //
                    OTHER("", false, "Other"); //

                final String m_persistId;

                final boolean m_isUnambigous;

                final String m_displayText;

                FileEncodingOption(final String persistId, final boolean isUnambigous) {
                    this(persistId, isUnambigous, persistId);
                }

                FileEncodingOption(final String persistId, final boolean isUnambigous, final String displayText) {
                    m_persistId = persistId;
                    m_isUnambigous = isUnambigous;
                    m_displayText = displayText;
                }

                static FileEncodingOption fromPersistId(final String persistId) {
                    return Arrays.stream(FileEncodingOption.values())
                        .filter(fileEncoding -> Objects.equals(fileEncoding.m_persistId, persistId)).findFirst()
                        .orElse(OTHER);
                }
            }

            static final class EncodingChoicesProvider implements ChoicesProvider {
                @Override
                public IdAndText[] choicesWithIdAndText(final DefaultNodeSettingsContext context) {
                    return Arrays.stream(FileEncodingOption.values())
                        .map(fileEncoding -> new IdAndText(fileEncoding.name(), fileEncoding.m_displayText))
                        .toArray(IdAndText[]::new);
                }
            }

            static final class IsOtherEncoding extends OneOfEnumCondition<FileEncodingOption> {
                @Override
                public FileEncodingOption[] oneOf() {
                    return new FileEncodingOption[]{FileEncodingOption.OTHER};
                }
            }

            @Widget(title = "File encoding", description = FileEncoding.DESCRIPTION, advanced = true)
            @ChoicesWidget(choices = EncodingChoicesProvider.class)
            @Layout(FileEncoding.class)
            @Signal(condition = IsOtherEncoding.class)
            FileEncodingOption m_fileEncoding;

            @Widget(title = "Custom encoding", description = CustomEncoding.DESCRIPTION, advanced = true)
            @Layout(CustomEncoding.class)
            @Effect(signals = IsOtherEncoding.class, type = EffectType.SHOW)
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
        }

        static final class CharsetPersistor extends NodeSettingsPersistorWithConfigKey<Charset> {

            @Override
            public Charset load(final NodeSettingsRO settings) throws InvalidSettingsException {
                final var persistId = settings.getString(getConfigKey(), null);
                final var fileEncoding = FileEncodingOption.fromPersistId(persistId);
                if (fileEncoding == FileEncodingOption.OTHER) {
                    return new Charset(fileEncoding, persistId);
                }
                return new Charset(fileEncoding);
            }

            @Override
            public void save(final Charset charset, final NodeSettingsWO settings) {
                if (charset.m_fileEncoding == FileEncodingOption.OTHER) {
                    settings.addString(getConfigKey(), charset.m_customEncoding);
                } else {
                    settings.addString(getConfigKey(), charset.m_fileEncoding.m_persistId);
                }
            }
        }

        @Persist(configKey = "charset", customPersistor = CharsetPersistor.class)
        Charset m_charset = new Charset();
    }
}
