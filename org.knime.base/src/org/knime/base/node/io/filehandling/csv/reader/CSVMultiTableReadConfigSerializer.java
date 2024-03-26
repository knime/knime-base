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
 *   Jun 12, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import static org.knime.filehandling.core.util.SettingsUtils.getOrEmpty;

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.QuoteOption;
import org.knime.base.node.io.filehandling.csv.reader.api.StringReadAdapterFactory;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.ConfigSerializer;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.config.AbstractTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDFactory;
import org.knime.filehandling.core.node.table.reader.config.tablespec.NodeSettingsConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.NodeSettingsSerializer;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigSerializer;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * {@link ConfigSerializer} for CSV reader nodes.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
enum CSVMultiTableReadConfigSerializer
    implements ConfigSerializer<CSVMultiTableReadConfig>, ConfigIDFactory<CSVMultiTableReadConfig> {

        INSTANCE;


    private static final String CFG_MAX_NUM_CHUNKS_PER_FILE = "max_num_chunks_per_file";

    private static final String CFG_MIN_CHUNK_SIZE = "min_chunk_size_in_bytes";

    private static final String CFG_NO_ROW_DELIMITERS_IN_QUOTES = "no_row_delimiters_in_quotes";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CSVMultiTableReadConfigSerializer.class);

    private static final boolean DEFAULT_FAIL_ON_DIFFERING_SPECS = true;

    private static final String CFG_FAIL_ON_DIFFERING_SPECS = "fail_on_differing_specs";

    private static final String CFG_DECIMAL_SEPARATOR = "decimal_separator";

    private static final String CFG_THOUSANDS_SEPARATOR = "thousands_separator";

    private static final String CFG_NUMBER_OF_LINES_TO_SKIP = "number_of_lines_to_skip";

    private static final String CFG_SKIP_LINES = "skip_lines";

    private static final String CFG_CHARSET = "charset";

    private static final String CFG_MAX_ROWS = "max_rows";

    private static final String CFG_LIMIT_DATA_ROWS = "limit_data_rows";

    private static final String CFG_NUMBER_OF_ROWS_TO_SKIP = "number_of_rows_to_skip";

    private static final String CFG_SKIP_DATA_ROWS = "skip_data_rows";

    private static final String CFG_MAXIMUM_NUMBER_OF_COLUMNS = "maximum_number_of_columns";

    private static final String CFG_LIMIT_MEMORY_PER_COLUMN = "limit_memory_per_column";

    private static final String CFG_QUOTE_OPTION = "quote_option";

    private static final String CFG_REPLACE_EMPTY_QUOTES_WITH_MISSING = "replace_empty_quotes_with_missing";

    private static final String CFG_MAX_DATA_ROWS_SCANNED = "max_data_rows_scanned";

    private static final String CFG_LIMIT_DATA_ROWS_SCANNED = "limit_data_rows_scanned";

    private static final String CFG_SAVE_TABLE_SPEC_CONFIG = "save_table_spec_config" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_CHECK_TABLE_SPEC = "check_table_spec";

    private static final String CFG_SPEC_MERGE_MODE_OLD = "spec_merge_mode";

    private static final String CFG_SPEC_MERGE_MODE_NEW = CFG_SPEC_MERGE_MODE_OLD + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_AUTODETECT_BUFFER_SIZE = "autodetect_buffer_size";

    private static final String CFG_SKIP_EMPTY_DATA_ROWS = "skip_empty_data_rows";

    private static final String CFG_HAS_ROW_ID = "has_row_id";

    private static final String CFG_PREPEND_FILE_IDX_TO_ROWID = "prepend_file_idx_to_row_id";

    private static final String CFG_HAS_COLUMN_HEADER = "has_column_header";

    private static final String CFG_SUPPORT_SHORT_DATA_ROWS = "support_short_data_rows";

    private static final String CFG_ENCODING_TAB = "encoding";

    private static final String CFG_LIMIT_ROWS_TAB = "limit_rows";

    private static final String CFG_ADVANCED_SETTINGS_TAB = "advanced_settings";

    private static final String CFG_SETTINGS_TAB = "settings";

    private static final String CFG_TABLE_SPEC_CONFIG = "table_spec_config" + SettingsModel.CFGKEY_INTERNAL;

    /** string key used to save the value of column delimiter used to read CSV files */
    private static final String CFG_DELIMITER = "column_delimiter";

    /** string key used to save the value of the line break row delimiter to read CSV files. */
    private static final String CFG_USE_LINE_BREAK_ROW_DELIMITER = "use_line_break_row_delimiter";

    /** string key used to save the value of line separator used to read CSV files */
    private static final String CFG_ROW_DELIMITER = "row_delimiter";

    /** string key used to save the value of the character used as quote */
    private static final String CFG_QUOTE_CHAR = "quote_char";

    /** string key used to save the value of the character used as quote escape */
    private static final String CFG_QUOTE_ESCAPE_CHAR = "quote_escape_char";

    /** string key used to save the value of the character used as comment start */
    private static final String CFG_COMMENT_CHAR = "comment_char";

    private static final String CFG_APPEND_PATH_COLUMN = "append_path_column" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PATH_COLUMN_NAME = "path_column_name" + SettingsModel.CFGKEY_INTERNAL;

    private final TableSpecConfigSerializer<Class<?>> m_tableSpecConfigSerializer;

    private enum ClassTypeSerializer implements NodeSettingsSerializer<Class<?>> {
            SERIALIZER;

        @Override
        public void save(final Class<?> object, final NodeSettingsWO settings) {
            settings.addString("class", object.getName());
        }

        @Override
        public Class<?> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            try {
                return Class.forName(settings.getString("class"));
            } catch (ClassNotFoundException e) {
                throw new InvalidSettingsException("Loading the type failed.", e);
            }
        }

    }

    CSVMultiTableReadConfigSerializer() {
        ProducerRegistry<Class<?>, ?> producerRegistry = StringReadAdapterFactory.INSTANCE.getProducerRegistry();
        m_tableSpecConfigSerializer = TableSpecConfigSerializer.createStartingV42(producerRegistry, this,
            ClassTypeSerializer.SERIALIZER, String.class);
    }

    @Override
    public void loadInDialog(final CSVMultiTableReadConfig config, final NodeSettingsRO settings,
        final PortObjectSpec[] specs) throws NotConfigurableException {
        loadSettingsTabInDialog(config, getOrEmpty(settings, CFG_SETTINGS_TAB));
        NodeSettingsRO advancedSettings = getOrEmpty(settings, CFG_ADVANCED_SETTINGS_TAB);
        loadAdvancedSettingsTabInDialog(config, advancedSettings);
        loadLimitRowsTabInDialog(config, getOrEmpty(settings, CFG_LIMIT_ROWS_TAB));
        loadEncodingTabInDialog(config, getOrEmpty(settings, CFG_ENCODING_TAB));
        try {
            config.setTableSpecConfig(loadTableSpecConfig(settings));
        } catch (InvalidSettingsException ex) {// NOSONAR, see below
            /* Can only happen in TableSpecConfig#load, since we checked #NodeSettingsRO#getNodeSettings(String)
             * before. The framework takes care that #validate is called before load so we can assume that this
             * exception does not occur.
             */
        }
    }

    private TableSpecConfig<Class<?>> loadTableSpecConfig(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final NodeSettingsRO advancedSettings = getOrEmpty(settings, CFG_ADVANCED_SETTINGS_TAB);
        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            ColumnFilterMode columnFilterMode = loadColumnFilterModeForOldWorkflows(advancedSettings);
            return m_tableSpecConfigSerializer.load(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG),
                TableSpecConfigSerializer.AdditionalParameters.create().withColumnFilterMode(columnFilterMode));
        } else {
            return null;
        }

    }

    @SuppressWarnings("deprecation")
    private static ColumnFilterMode loadColumnFilterModeForOldWorkflows(final NodeSettingsRO advancedSettings) {
        final SpecMergeMode specMergeMode = loadSpecMergeModeForOldWorkflows(advancedSettings);
        return specMergeMode != null ? specMergeMode.getColumnFilterMode() : null;
    }

    @SuppressWarnings("deprecation")
    private static SpecMergeMode loadSpecMergeModeForOldWorkflows(final NodeSettingsRO advancedSettings) {// NOSONAR, stupid rule
        try {
            if (advancedSettings.containsKey(CFG_SPEC_MERGE_MODE_OLD)) {
                // Settings stored with 4.2 hold the SpecMergeMode as part of the advanced settings
                return SpecMergeMode.valueOf(advancedSettings.getString(CFG_SPEC_MERGE_MODE_OLD));
            } else if (advancedSettings.containsKey(CFG_SPEC_MERGE_MODE_NEW)) {
                // settings originated from 4.2 and were stored in 4.3
                return SpecMergeMode.valueOf(advancedSettings.getString(CFG_SPEC_MERGE_MODE_NEW));
            } else {
                // Settings stored with 4.3 or later, no longer store the SpecMergeMode in the advanced tab
                // but instead store the ColumnFilterMode as part of the TableSpecConfig
                return null;
            }
        } catch (InvalidSettingsException ise) {
            LOGGER.debug("Loading the SpecMergeMode failed unexpectedly, falling back to null.", ise);
            return null;
        }
    }

    private static void loadSettingsTabInDialog(final CSVMultiTableReadConfig config, final NodeSettingsRO settings) {
        final DefaultTableReadConfig<CSVTableReaderConfig> tc = config.getTableReadConfig();
        tc.setAllowShortRows(settings.getBoolean(CFG_SUPPORT_SHORT_DATA_ROWS, true));
        tc.setPrependSourceIdxToRowId(settings.getBoolean(CFG_PREPEND_FILE_IDX_TO_ROWID, false));
        tc.setColumnHeaderIdx(0);
        tc.setUseColumnHeaderIdx(settings.getBoolean(CFG_HAS_COLUMN_HEADER, true));
        tc.setRowIDIdx(0);
        tc.setUseRowIDIdx(settings.getBoolean(CFG_HAS_ROW_ID, true));
        tc.setSkipEmptyRows(settings.getBoolean(CFG_SKIP_EMPTY_DATA_ROWS, true));

        final CSVTableReaderConfig csvConfig = config.getReaderSpecificConfig();
        csvConfig.setAutoDetectionBufferSize(
            settings.getInt(CFG_AUTODETECT_BUFFER_SIZE, CSVTableReaderConfig.DEFAULT_AUTODETECTION_BUFFER_SIZE));
        csvConfig.setDelimiter(settings.getString(CFG_DELIMITER, ","));
        csvConfig.useLineBreakRowDelimiter(settings.getBoolean(CFG_USE_LINE_BREAK_ROW_DELIMITER, true));
        csvConfig.setLineSeparator(settings.getString(CFG_ROW_DELIMITER, "\n"));
        csvConfig.setQuote(settings.getString(CFG_QUOTE_CHAR, "\""));
        csvConfig.setQuoteEscape(settings.getString(CFG_QUOTE_ESCAPE_CHAR, "\""));
        csvConfig.setComment(settings.getString(CFG_COMMENT_CHAR, "\0"));
    }

    @SuppressWarnings("deprecation")
    private static boolean loadFailOnDifferingSpecsInModel(final NodeSettingsRO advancedSettings)
        throws InvalidSettingsException {
        try {
            // As of 4.3, we store this setting
            return advancedSettings.getBoolean(CFG_FAIL_ON_DIFFERING_SPECS);
        } catch (final InvalidSettingsException ise) {
            // In 4.2 we stored the SpecMergeMode instead
            final SpecMergeMode specMergeMode = loadSpecMergeModeForOldWorkflows(advancedSettings);
            if (specMergeMode != null) {
                return specMergeMode == SpecMergeMode.FAIL_ON_DIFFERING_SPECS;
            } else {
                // if there was no SpecMergeMode, then we have incomplete settings and need to fail
                throw ise;
            }
        }
    }

    private static boolean loadFailOnDifferingSpecsInDialog(final NodeSettingsRO advancedSettings) {
        try {
            return loadFailOnDifferingSpecsInModel(advancedSettings);
        } catch (InvalidSettingsException ise) {
            LOGGER.debug(String.format("An error occurred while loading %s", CFG_FAIL_ON_DIFFERING_SPECS), ise);
            return DEFAULT_FAIL_ON_DIFFERING_SPECS;
        }
    }

    @SuppressWarnings("deprecation")
    private static void loadAdvancedSettingsTabInDialog(final CSVMultiTableReadConfig config,
        final NodeSettingsRO settings) {

        config.setFailOnDifferingSpecs(loadFailOnDifferingSpecsInDialog(settings));

        config.setSpecMergeMode(loadSpecMergeModeForOldWorkflows(settings));

        final DefaultTableReadConfig<CSVTableReaderConfig> tc = config.getTableReadConfig();
        tc.setLimitRowsForSpec(settings.getBoolean(CFG_LIMIT_DATA_ROWS_SCANNED, true));
        tc.setMaxRowsForSpec(
            settings.getLong(CFG_MAX_DATA_ROWS_SCANNED, AbstractTableReadConfig.DEFAULT_ROWS_FOR_SPEC_GUESSING));

        config.setSaveTableSpecConfig(settings.getBoolean(CFG_SAVE_TABLE_SPEC_CONFIG, true));

        // added in 5.2.2 (AP-19239); we don't want existing workflows to make the check
        config.setCheckSavedTableSpec(settings.getBoolean(CFG_CHECK_TABLE_SPEC, false));

        // added in 4.4.0
        config.setAppendItemIdentifierColumn(
            settings.getBoolean(CFG_APPEND_PATH_COLUMN, config.appendItemIdentifierColumn()));
        config.setItemIdentifierColumnName(
            settings.getString(CFG_PATH_COLUMN_NAME, config.getItemIdentifierColumnName()));

        final CSVTableReaderConfig cc = tc.getReaderSpecificConfig();
        cc.setReplaceEmptyWithMissing(settings.getBoolean(CFG_REPLACE_EMPTY_QUOTES_WITH_MISSING, true));

        QuoteOption quoteOption;
        try {
            quoteOption = QuoteOption.valueOf(settings.getString(CFG_QUOTE_OPTION, QuoteOption.KEEP_QUOTES.name()));
        } catch (IllegalArgumentException ex) {
            LOGGER.debug("The configured quote option is invalid. Falling back onto the default.", ex);
            quoteOption = QuoteOption.KEEP_QUOTES;
        }
        cc.setQuoteOption(quoteOption);

        cc.noRowDelimitersInQuotes(settings.getBoolean(CFG_NO_ROW_DELIMITERS_IN_QUOTES, false));

        cc.setMinChunkSizeInBytes(settings.getLong(CFG_MIN_CHUNK_SIZE, cc.getMinChunkSizeInBytes()));
        cc.setMaxNumChunksPerFile(settings.getInt(CFG_MAX_NUM_CHUNKS_PER_FILE, cc.getMaxNumChunksPerFile()));

        cc.limitCharsPerColumn(settings.getBoolean(CFG_LIMIT_MEMORY_PER_COLUMN, true));
        cc.setMaxColumns(settings.getInt(CFG_MAXIMUM_NUMBER_OF_COLUMNS, CSVTableReaderConfig.DEFAULT_MAX_COLUMNS));

        cc.setThousandsSeparator(settings.getString(CFG_THOUSANDS_SEPARATOR, "\0"));
        cc.setDecimalSeparator(settings.getString(CFG_DECIMAL_SEPARATOR, "."));
    }

    private static void loadLimitRowsTabInDialog(final CSVMultiTableReadConfig config, final NodeSettingsRO settings) {
        final DefaultTableReadConfig<CSVTableReaderConfig> tc = config.getTableReadConfig();
        tc.setSkipRows(settings.getBoolean(CFG_SKIP_DATA_ROWS, false));
        tc.setNumRowsToSkip(settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP, 1L));
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS, false));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS, 50L));

        final CSVTableReaderConfig cc = config.getReaderSpecificConfig();
        cc.setSkipLines(settings.getBoolean(CFG_SKIP_LINES, false));
        cc.setNumLinesToSkip(settings.getLong(CFG_NUMBER_OF_LINES_TO_SKIP, 1));
    }

    private static void loadEncodingTabInDialog(final CSVMultiTableReadConfig config, final NodeSettingsRO settings) {
        config.getReaderSpecificConfig().setCharSetName(settings.getString(CFG_CHARSET, null));
    }

    @Override
    public void loadInModel(final CSVMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        loadSettingsTabInModel(config, settings.getNodeSettings(CFG_SETTINGS_TAB));
        loadAdvancedSettingsTabInModel(config, settings.getNodeSettings(CFG_ADVANCED_SETTINGS_TAB));
        loadLimitRowsTabInModel(config, settings.getNodeSettings(CFG_LIMIT_ROWS_TAB));
        loadEncodingTabInModel(config, settings.getNodeSettings(CFG_ENCODING_TAB));
        config.setTableSpecConfig(loadTableSpecConfig(settings));
    }

    private static void loadSettingsTabInModel(final CSVMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final DefaultTableReadConfig<CSVTableReaderConfig> tc = config.getTableReadConfig();
        tc.setAllowShortRows(settings.getBoolean(CFG_SUPPORT_SHORT_DATA_ROWS));
        tc.setColumnHeaderIdx(0);
        tc.setUseColumnHeaderIdx(settings.getBoolean(CFG_HAS_COLUMN_HEADER));
        tc.setRowIDIdx(0);
        tc.setUseRowIDIdx(settings.getBoolean(CFG_HAS_ROW_ID));
        // Added in 4.3
        tc.setPrependSourceIdxToRowId(settings.getBoolean(CFG_PREPEND_FILE_IDX_TO_ROWID, false));
        tc.setSkipEmptyRows(settings.getBoolean(CFG_SKIP_EMPTY_DATA_ROWS));

        final CSVTableReaderConfig csvConfig = config.getReaderSpecificConfig();
        csvConfig.setAutoDetectionBufferSize(settings.getInt(CFG_AUTODETECT_BUFFER_SIZE));
        csvConfig.setDelimiter(settings.getString(CFG_DELIMITER));

        // Added in 4.4 AP-14006
        if (settings.containsKey(CFG_USE_LINE_BREAK_ROW_DELIMITER)) {
            csvConfig.useLineBreakRowDelimiter(settings.getBoolean(CFG_USE_LINE_BREAK_ROW_DELIMITER));
        } else {
            csvConfig.useLineBreakRowDelimiter(false);
        }
        csvConfig.setLineSeparator(settings.getString(CFG_ROW_DELIMITER));
        csvConfig.setQuote(settings.getString(CFG_QUOTE_CHAR));
        csvConfig.setQuoteEscape(settings.getString(CFG_QUOTE_ESCAPE_CHAR));
        csvConfig.setComment(settings.getString(CFG_COMMENT_CHAR));
    }

    @SuppressWarnings("deprecation")
    private static void loadAdvancedSettingsTabInModel(final CSVMultiTableReadConfig config,
        final NodeSettingsRO settings) throws InvalidSettingsException {
        config.setFailOnDifferingSpecs(loadFailOnDifferingSpecsInModel(settings));
        config.setSpecMergeMode(loadSpecMergeModeForOldWorkflows(settings));

        final DefaultTableReadConfig<CSVTableReaderConfig> tc = config.getTableReadConfig();
        tc.setLimitRowsForSpec(settings.getBoolean(CFG_LIMIT_DATA_ROWS_SCANNED));
        tc.setMaxRowsForSpec(settings.getLong(CFG_MAX_DATA_ROWS_SCANNED));

        // added in 4.3.1
        if (settings.containsKey(CFG_SAVE_TABLE_SPEC_CONFIG)) {
            config.setSaveTableSpecConfig(settings.getBoolean(CFG_SAVE_TABLE_SPEC_CONFIG));
        }

        // added in 5.2.2 (AP-19239); we don't want existing workflows to make the check
        config.setCheckSavedTableSpec(settings.getBoolean(CFG_CHECK_TABLE_SPEC, false));

        // added in 4.4.0
        if (settings.containsKey(CFG_APPEND_PATH_COLUMN)) {
            config.setAppendItemIdentifierColumn(settings.getBoolean(CFG_APPEND_PATH_COLUMN));
            config.setItemIdentifierColumnName(settings.getString(CFG_PATH_COLUMN_NAME));
        }

        final CSVTableReaderConfig cc = tc.getReaderSpecificConfig();
        cc.setReplaceEmptyWithMissing(settings.getBoolean(CFG_REPLACE_EMPTY_QUOTES_WITH_MISSING));

        QuoteOption quoteOption;
        try {
            quoteOption = QuoteOption.valueOf(settings.getString(CFG_QUOTE_OPTION));
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException(ex);
        }
        cc.setQuoteOption(quoteOption);

        if (settings.containsKey(CFG_NO_ROW_DELIMITERS_IN_QUOTES)) {
            cc.noRowDelimitersInQuotes(settings.getBoolean(CFG_NO_ROW_DELIMITERS_IN_QUOTES));
        }

        if (settings.containsKey(CFG_MIN_CHUNK_SIZE)) {
            cc.setMinChunkSizeInBytes(settings.getLong(CFG_MIN_CHUNK_SIZE));
        }

        if (settings.containsKey(CFG_MAX_NUM_CHUNKS_PER_FILE)) {
            cc.setMaxNumChunksPerFile(settings.getInt(CFG_MAX_NUM_CHUNKS_PER_FILE));
        }

        cc.limitCharsPerColumn(settings.getBoolean(CFG_LIMIT_MEMORY_PER_COLUMN));
        cc.setMaxColumns(settings.getInt(CFG_MAXIMUM_NUMBER_OF_COLUMNS));

        cc.setThousandsSeparator(settings.getString(CFG_THOUSANDS_SEPARATOR, Character.toString('\0')));
        cc.setDecimalSeparator(settings.getString(CFG_DECIMAL_SEPARATOR, "."));
    }

    private static void loadLimitRowsTabInModel(final CSVMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final DefaultTableReadConfig<CSVTableReaderConfig> tc = config.getTableReadConfig();
        tc.setSkipRows(settings.getBoolean(CFG_SKIP_DATA_ROWS));
        tc.setNumRowsToSkip(settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP));
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS));

        final CSVTableReaderConfig cc = config.getReaderSpecificConfig();
        cc.setSkipLines(settings.getBoolean(CFG_SKIP_LINES));
        cc.setNumLinesToSkip(settings.getLong(CFG_NUMBER_OF_LINES_TO_SKIP));
    }

    private static void loadEncodingTabInModel(final CSVMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        config.getReaderSpecificConfig().setCharSetName(settings.getString(CFG_CHARSET));
    }

    @Override
    public void saveInModel(final CSVMultiTableReadConfig config, final NodeSettingsWO settings) {
        if (config.hasTableSpecConfig()) {
            m_tableSpecConfigSerializer.save(config.getTableSpecConfig(),
                settings.addNodeSettings(CFG_TABLE_SPEC_CONFIG));
        }
        saveSettingsTab(config, SettingsUtils.getOrAdd(settings, CFG_SETTINGS_TAB));
        saveAdvancedSettingsTab(config, settings.addNodeSettings(CFG_ADVANCED_SETTINGS_TAB));
        saveLimitRowsTab(config, settings.addNodeSettings(CFG_LIMIT_ROWS_TAB));
        saveEncodingTab(config, settings.addNodeSettings(CFG_ENCODING_TAB));
    }

    private static void saveSettingsTab(final CSVMultiTableReadConfig config, final NodeSettingsWO settings) {
        final TableReadConfig<?> tc = config.getTableReadConfig();
        settings.addBoolean(CFG_HAS_COLUMN_HEADER, tc.useColumnHeaderIdx());
        settings.addBoolean(CFG_HAS_ROW_ID, tc.useRowIDIdx());
        settings.addBoolean(CFG_SUPPORT_SHORT_DATA_ROWS, tc.allowShortRows());
        settings.addBoolean(CFG_SKIP_EMPTY_DATA_ROWS, tc.skipEmptyRows());
        settings.addBoolean(CFG_PREPEND_FILE_IDX_TO_ROWID, tc.prependSourceIdxToRowID());

        final CSVTableReaderConfig cc = config.getReaderSpecificConfig();
        settings.addString(CFG_COMMENT_CHAR, cc.getComment());
        settings.addString(CFG_DELIMITER, cc.getDelimiter());
        settings.addString(CFG_QUOTE_CHAR, cc.getQuote());
        settings.addString(CFG_QUOTE_ESCAPE_CHAR, cc.getQuoteEscape());
        settings.addBoolean(CFG_USE_LINE_BREAK_ROW_DELIMITER, cc.useLineBreakRowDelimiter());
        settings.addString(CFG_ROW_DELIMITER, cc.getLineSeparator());
        settings.addInt(CFG_AUTODETECT_BUFFER_SIZE, cc.getAutoDetectionBufferSize());
    }

    @SuppressWarnings("deprecation")
    private static void saveAdvancedSettingsTab(final CSVMultiTableReadConfig config, final NodeSettingsWO settings) {

        if (config.getSpecMergeMode() != null) {
            // We have an old SpecMergeMode stored, so we need to carry it along
            // otherwise a node might change its behavior without having been reconfigured
            // Example: Node was stored with SpecMergeMode.INTERSECTION, then stored in 4.3
            // In that case if we don't store the SpecMergeMode, there won't be one in the
            // settings which means that we default to SpecMergeMode.UNION
            settings.addString(CFG_SPEC_MERGE_MODE_NEW, config.getSpecMergeMode().name());
        }

        settings.addBoolean(CFG_FAIL_ON_DIFFERING_SPECS, config.failOnDifferingSpecs());
        settings.addBoolean(CFG_APPEND_PATH_COLUMN, config.appendItemIdentifierColumn());
        settings.addString(CFG_PATH_COLUMN_NAME, config.getItemIdentifierColumnName());

        final TableReadConfig<?> tc = config.getTableReadConfig();
        settings.addBoolean(CFG_LIMIT_DATA_ROWS_SCANNED, tc.limitRowsForSpec());
        settings.addLong(CFG_MAX_DATA_ROWS_SCANNED, tc.getMaxRowsForSpec());

        settings.addBoolean(CFG_SAVE_TABLE_SPEC_CONFIG, config.saveTableSpecConfig());
        settings.addBoolean(CFG_CHECK_TABLE_SPEC, config.checkSavedTableSpec());

        final CSVTableReaderConfig cc = config.getReaderSpecificConfig();
        settings.addBoolean(CFG_LIMIT_MEMORY_PER_COLUMN, cc.isCharsPerColumnLimited());
        settings.addInt(CFG_MAXIMUM_NUMBER_OF_COLUMNS, cc.getMaxColumns());

        settings.addString(CFG_QUOTE_OPTION, cc.getQuoteOption().name());
        settings.addBoolean(CFG_REPLACE_EMPTY_QUOTES_WITH_MISSING, cc.replaceEmptyWithMissing());
        settings.addBoolean(CFG_NO_ROW_DELIMITERS_IN_QUOTES, cc.noRowDelimitersInQuotes());

        settings.addLong(CFG_MIN_CHUNK_SIZE, cc.getMinChunkSizeInBytes());
        settings.addInt(CFG_MAX_NUM_CHUNKS_PER_FILE, cc.getMaxNumChunksPerFile());

        settings.addString(CFG_THOUSANDS_SEPARATOR, cc.getThousandsSeparator());
        settings.addString(CFG_DECIMAL_SEPARATOR, cc.getDecimalSeparator());
    }

    private static void saveLimitRowsTab(final CSVMultiTableReadConfig config, final NodeSettingsWO settings) {

        final CSVTableReaderConfig cc = config.getReaderSpecificConfig();
        settings.addBoolean(CFG_SKIP_LINES, cc.skipLines());
        settings.addLong(CFG_NUMBER_OF_LINES_TO_SKIP, cc.getNumLinesToSkip());

        final TableReadConfig<?> tc = config.getTableReadConfig();
        settings.addBoolean(CFG_SKIP_DATA_ROWS, tc.skipRows());
        settings.addLong(CFG_NUMBER_OF_ROWS_TO_SKIP, tc.getNumRowsToSkip());
        settings.addBoolean(CFG_LIMIT_DATA_ROWS, tc.limitRows());
        settings.addLong(CFG_MAX_ROWS, tc.getMaxRows());
    }

    private static void saveEncodingTab(final CSVMultiTableReadConfig config, final NodeSettingsWO settings) {
        settings.addString(CFG_CHARSET, config.getReaderSpecificConfig().getCharSetName());
    }

    @Override
    public void validate(final CSVMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final NodeSettingsRO advancedSettings = settings.getNodeSettings(CFG_ADVANCED_SETTINGS_TAB);
        loadTableSpecConfig(settings);
        validateSettingsTab(settings.getNodeSettings(CFG_SETTINGS_TAB));
        validateAdvancedSettingsTab(advancedSettings);
        validateLimitRowsTab(settings.getNodeSettings(CFG_LIMIT_ROWS_TAB));
        validateEncodingTab(settings.getNodeSettings(CFG_ENCODING_TAB));
    }

    private static void validateSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_SUPPORT_SHORT_DATA_ROWS);
        settings.getBoolean(CFG_HAS_COLUMN_HEADER);
        settings.getBoolean(CFG_HAS_ROW_ID);
        settings.getBoolean(CFG_SKIP_EMPTY_DATA_ROWS);
        settings.getInt(CFG_AUTODETECT_BUFFER_SIZE);
        settings.getString(CFG_DELIMITER);
        // added with 4.4 AP-140006
        if (settings.containsKey(CFG_USE_LINE_BREAK_ROW_DELIMITER)) {
            settings.getBoolean(CFG_USE_LINE_BREAK_ROW_DELIMITER);
        }
        settings.getString(CFG_ROW_DELIMITER);
        settings.getString(CFG_QUOTE_CHAR);
        settings.getString(CFG_QUOTE_ESCAPE_CHAR);
        settings.getString(CFG_COMMENT_CHAR);

    }

    @SuppressWarnings("deprecation")
    private static void validateAdvancedSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            SpecMergeMode.valueOf(settings.getString(CFG_SPEC_MERGE_MODE_OLD, SpecMergeMode.INTERSECTION.name()));
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException(ex);
        }

        settings.getBoolean(CFG_LIMIT_DATA_ROWS_SCANNED);
        settings.getLong(CFG_MAX_DATA_ROWS_SCANNED);

        // added in 4.3.1
        if (settings.containsKey(CFG_SAVE_TABLE_SPEC_CONFIG)) {
            settings.getBoolean(CFG_SAVE_TABLE_SPEC_CONFIG);
        }

        // added in 5.2.2
        if (settings.containsKey(CFG_CHECK_TABLE_SPEC)) {
            settings.getBoolean(CFG_CHECK_TABLE_SPEC);
        }

        settings.getBoolean(CFG_REPLACE_EMPTY_QUOTES_WITH_MISSING);

        try {
            QuoteOption.valueOf(settings.getString(CFG_QUOTE_OPTION));
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException(ex);
        }

        settings.getBoolean(CFG_LIMIT_MEMORY_PER_COLUMN);
        settings.getInt(CFG_MAXIMUM_NUMBER_OF_COLUMNS);

        // added in 4.4.0
        if (settings.containsKey(CFG_APPEND_PATH_COLUMN)) {
            settings.getBoolean(CFG_APPEND_PATH_COLUMN);
            settings.getString(CFG_PATH_COLUMN_NAME);
        }

    }

    private static void validateLimitRowsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_SKIP_DATA_ROWS);
        settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP);
        settings.getBoolean(CFG_LIMIT_DATA_ROWS);
        settings.getLong(CFG_MAX_ROWS);
        settings.getBoolean(CFG_SKIP_LINES);
        settings.getLong(CFG_NUMBER_OF_LINES_TO_SKIP);
    }

    private static void validateEncodingTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var charset = settings.getString(CFG_CHARSET);
        if (charset != null && charset.isBlank()) {
            throw new InvalidSettingsException("Custom encoding must not be blank.");
        }
    }

    @Override
    public void saveInDialog(final CSVMultiTableReadConfig config, final NodeSettingsWO settings)
        throws InvalidSettingsException {
        saveInModel(config, settings);
    }

    @Override
    public ConfigID createFromConfig(final CSVMultiTableReadConfig config) {
        final NodeSettings settings = new NodeSettings("multi_table_read");
        saveConfigIDSettingsTab(config, settings.addNodeSettings(CFG_SETTINGS_TAB));
        saveConfigIDAdvancedSettingsTab(config, settings.addNodeSettings(CFG_ADVANCED_SETTINGS_TAB));
        saveConfigIDEncodingTab(config, settings.addNodeSettings(CFG_ENCODING_TAB));
        saveConfigIDLimitRowsTab(config, settings.addNodeSettings(CFG_LIMIT_ROWS_TAB));
        return new NodeSettingsConfigID(settings);
    }

    private static void saveConfigIDSettingsTab(final CSVMultiTableReadConfig config, final NodeSettingsWO settings) {
        final TableReadConfig<?> tc = config.getTableReadConfig();
        settings.addBoolean(CFG_HAS_COLUMN_HEADER, tc.useColumnHeaderIdx());
        settings.addBoolean(CFG_HAS_ROW_ID, tc.useRowIDIdx());

        final CSVTableReaderConfig cc = config.getReaderSpecificConfig();
        settings.addString(CFG_COMMENT_CHAR, cc.getComment());
        settings.addString(CFG_DELIMITER, cc.getDelimiter());
        settings.addString(CFG_QUOTE_CHAR, cc.getQuote());
        settings.addString(CFG_QUOTE_ESCAPE_CHAR, cc.getQuoteEscape());
        settings.addString(CFG_ROW_DELIMITER, cc.getLineSeparator());
        settings.addBoolean(CFG_USE_LINE_BREAK_ROW_DELIMITER, cc.useLineBreakRowDelimiter());
    }

    private static void saveConfigIDAdvancedSettingsTab(final CSVMultiTableReadConfig config,
        final NodeSettingsWO settings) {
        final CSVTableReaderConfig cc = config.getReaderSpecificConfig();
        settings.addString(CFG_QUOTE_OPTION, cc.getQuoteOption().name());
        settings.addBoolean(CFG_REPLACE_EMPTY_QUOTES_WITH_MISSING, cc.replaceEmptyWithMissing());

        settings.addBoolean(CFG_LIMIT_DATA_ROWS_SCANNED, config.getTableReadConfig().limitRowsForSpec());
        settings.addLong(CFG_MAX_DATA_ROWS_SCANNED, config.getTableReadConfig().getMaxRowsForSpec());

        settings.addString(CFG_THOUSANDS_SEPARATOR, cc.getThousandsSeparator());
        settings.addString(CFG_DECIMAL_SEPARATOR, cc.getDecimalSeparator());
    }

    private static void saveConfigIDLimitRowsTab(final CSVMultiTableReadConfig config,
        final NodeSettingsWO limitRowsSettings) {
        limitRowsSettings.addBoolean(CFG_SKIP_LINES, config.getReaderSpecificConfig().skipLines());
        limitRowsSettings.addLong(CFG_NUMBER_OF_LINES_TO_SKIP, config.getReaderSpecificConfig().getNumLinesToSkip());
        limitRowsSettings.addBoolean(CFG_SKIP_DATA_ROWS, config.getTableReadConfig().skipRows());
        limitRowsSettings.addLong(CFG_NUMBER_OF_ROWS_TO_SKIP, config.getTableReadConfig().getNumRowsToSkip());
    }

    private static void saveConfigIDEncodingTab(final CSVMultiTableReadConfig config, final NodeSettingsWO settings) {
        settings.addString(CFG_CHARSET, config.getReaderSpecificConfig().getCharSetName());
    }

    @Override
    public ConfigID createFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        return new NodeSettingsConfigID(settings.getNodeSettings("multi_table_read"));
    }

}
