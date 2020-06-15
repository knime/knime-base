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
package org.knime.base.node.io.filehandling.table.csv;

import org.knime.base.node.io.filehandling.table.csv.reader.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.table.csv.reader.QuoteOption;
import org.knime.base.node.io.filehandling.table.csv.reader.StringReadAdapterFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.ConfigSerializer;
import org.knime.filehandling.core.node.table.reader.config.DefaultMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;

/**
 * {@link ConfigSerializer} for CSV reader nodes.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
enum CSVMultiTableReadConfigSerializer implements
    ConfigSerializer<DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>>> {

        /**
         * Singleton instance.
         */
        INSTANCE;

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

    private static final String CFG_SPEC_MERGE_MODE = "spec_merge_mode";

    private static final String CFG_AUTODETECT_BUFFER_SIZE = "autodetect_buffer_size";

    private static final String CFG_SKIP_EMPTY_DATA_ROWS = "skip_empty_data_rows";

    private static final String CFG_HAS_ROW_ID = "has_row_id";

    private static final String CFG_HAS_COLUMN_HEADER = "has_column_header";

    private static final String CFG_SUPPORT_SHORT_DATA_ROWS = "support_short_data_rows";

    private static final String CFG_ENCODING_TAB = "encoding";

    private static final String CFG_LIMIT_ROWS_TAB = "limit_rows";

    private static final String CFG_ADVANCED_SETTINGS_TAB = "advanced_settings";

    private static final String CFG_SETTINGS_TAB = "settings";

    private static final String CFG_TABLE_SPEC_CONFIG = "table_spec_config" + SettingsModel.CFGKEY_INTERNAL;

    /** string key used to save the value of column delimiter used to read csv files */
    private static final String CFG_DELIMITER = "column_delimiter";

    /** string key used to save the value of line separator used to read csv files */
    private static final String CFG_ROW_DELIMITER = "row_delimiter";

    /** string key used to save the value of the character used as qoute */
    private static final String CFG_QUOTE_CHAR = "quote_char";

    /** string key used to save the value of the character used as qoute escape */
    private static final String CFG_QUOTE_ESCAPE_CHAR = "quote_escape_char";

    /** string key used to save the value of the character used as comment start */
    private static final String CFG_COMMENT_CHAR = "comment_char";

    @Override
    public void loadInDialog(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        loadSettingsTabInDialog(config, getOrEmpty(settings, CFG_SETTINGS_TAB));
        loadAdvancedSettingsTabInDialog(config, getOrEmpty(settings, CFG_ADVANCED_SETTINGS_TAB));
        loadLimitRowsTabInDialog(config, getOrEmpty(settings, CFG_LIMIT_ROWS_TAB));
        loadEncodingTabInDialog(config, getOrEmpty(settings, CFG_ENCODING_TAB));
        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            try {
                config.setTableSpecConfig(TableSpecConfig.load(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG),
                    StringReadAdapterFactory.INSTANCE.getProducerRegistry()));
            } catch (InvalidSettingsException ex) {
                /* Can only happen in TableSpecConfig#load, since we checked #NodeSettingsRO#getNodeSettings(String)
                 * before. The framework takes care that #validate is called before load so we can assume that this
                 * exception does not occur.
                 */
            }
        } else {
            config.setTableSpecConfig(null);
        }
    }

    private static NodeSettingsRO getOrEmpty(final NodeSettingsRO settings, final String key) {
        try {
            return settings.getNodeSettings(key);
        } catch (InvalidSettingsException ise) {
            return new NodeSettings(key);
        }
    }

    private static void loadSettingsTabInDialog(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsRO settings) {
        final DefaultTableReadConfig<?> tc = config.getTableReadConfig();
        tc.setAllowShortRows(settings.getBoolean(CFG_SUPPORT_SHORT_DATA_ROWS, true));
        tc.setColumnHeaderIdx(0);
        tc.setUseColumnHeaderIdx(settings.getBoolean(CFG_HAS_COLUMN_HEADER, true));
        tc.setRowIDIdx(0);
        tc.setUseRowIDIdx(settings.getBoolean(CFG_HAS_ROW_ID, true));
        tc.setSkipEmptyRows(settings.getBoolean(CFG_SKIP_EMPTY_DATA_ROWS, true));

        final CSVTableReaderConfig csvConfig = config.getReaderSpecificConfig();
        csvConfig.setAutoDetectionBufferSize(
            settings.getInt(CFG_AUTODETECT_BUFFER_SIZE, CSVTableReaderConfig.DEFAULT_AUTODETECTION_BUFFER_SIZE));
        csvConfig.setDelimiter(settings.getString(CFG_DELIMITER, ","));
        csvConfig.setLineSeparator(settings.getString(CFG_ROW_DELIMITER, "\n"));
        csvConfig.setQuote(settings.getString(CFG_QUOTE_CHAR, "\""));
        csvConfig.setQuoteEscape(settings.getString(CFG_QUOTE_ESCAPE_CHAR, "\""));
        csvConfig.setComment(settings.getString(CFG_COMMENT_CHAR, "\0"));
    }

    private static void loadAdvancedSettingsTabInDialog(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsRO settings) {
        SpecMergeMode specMergeMode;
        try {
            specMergeMode =
                SpecMergeMode.valueOf(settings.getString(CFG_SPEC_MERGE_MODE, SpecMergeMode.INTERSECTION.name()));
        } catch (Exception ex) {
            specMergeMode = SpecMergeMode.INTERSECTION;
        }
        config.setSpecMergeMode(specMergeMode);

        final DefaultTableReadConfig<CSVTableReaderConfig> tc = config.getTableReadConfig();
        tc.setLimitRowsForSpec(settings.getBoolean(CFG_LIMIT_DATA_ROWS_SCANNED, true));
        tc.setMaxRowsForSpec(settings.getLong(CFG_MAX_DATA_ROWS_SCANNED, 50));

        final CSVTableReaderConfig cc = tc.getReaderSpecificConfig();
        cc.setReplaceEmptyWithMissing(settings.getBoolean(CFG_REPLACE_EMPTY_QUOTES_WITH_MISSING, true));

        QuoteOption quoteOption;
        try {
            quoteOption = QuoteOption.valueOf(settings.getString(CFG_QUOTE_OPTION));
        } catch (Exception ex) {
            quoteOption = QuoteOption.KEEP_QUOTES;
        }
        cc.setQuoteOption(quoteOption);

        cc.limitCharsPerColumn(settings.getBoolean(CFG_LIMIT_MEMORY_PER_COLUMN, true));
        cc.setMaxColumns(settings.getInt(CFG_MAXIMUM_NUMBER_OF_COLUMNS, CSVTableReaderConfig.DEFAULT_MAX_COLUMNS));
    }

    private static void loadLimitRowsTabInDialog(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsRO settings) {
        final DefaultTableReadConfig<?> tc = config.getTableReadConfig();
        tc.setSkipRows(settings.getBoolean(CFG_SKIP_DATA_ROWS, false));
        tc.setNumRowsToSkip(settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP, 1L));
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS, false));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS, 50L));

        final CSVTableReaderConfig cc = config.getReaderSpecificConfig();
        cc.setSkipLines(settings.getBoolean(CFG_SKIP_LINES, false));
        cc.setNumLinesToSkip(settings.getLong(CFG_NUMBER_OF_LINES_TO_SKIP, 1));
    }

    private static void loadEncodingTabInDialog(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsRO settings) {
        config.getReaderSpecificConfig().setCharSetName(settings.getString(CFG_CHARSET, null));
    }

    @Override
    public void loadInModel(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsRO settings) throws InvalidSettingsException {
        loadSettingsTabInModel(config, settings.getNodeSettings(CFG_SETTINGS_TAB));
        loadAdvancedSettingsTabInModel(config, settings.getNodeSettings(CFG_ADVANCED_SETTINGS_TAB));
        loadLimitRowsTabInModel(config, settings.getNodeSettings(CFG_LIMIT_ROWS_TAB));
        loadEncodingTabInModel(config, settings.getNodeSettings(CFG_ENCODING_TAB));
        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            config.setTableSpecConfig(TableSpecConfig.load(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG),
                StringReadAdapterFactory.INSTANCE.getProducerRegistry()));
        } else {
            config.setTableSpecConfig(null);
        }
    }

    private static void loadSettingsTabInModel(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsRO settings) throws InvalidSettingsException {
        final DefaultTableReadConfig<?> tc = config.getTableReadConfig();
        tc.setAllowShortRows(settings.getBoolean(CFG_SUPPORT_SHORT_DATA_ROWS));
        tc.setColumnHeaderIdx(0);
        tc.setUseColumnHeaderIdx(settings.getBoolean(CFG_HAS_COLUMN_HEADER));
        tc.setRowIDIdx(0);
        tc.setUseRowIDIdx(settings.getBoolean(CFG_HAS_ROW_ID));
        tc.setSkipEmptyRows(settings.getBoolean(CFG_SKIP_EMPTY_DATA_ROWS));

        final CSVTableReaderConfig csvConfig = config.getReaderSpecificConfig();
        csvConfig.setAutoDetectionBufferSize(settings.getInt(CFG_AUTODETECT_BUFFER_SIZE));
        csvConfig.setDelimiter(settings.getString(CFG_DELIMITER));
        csvConfig.setLineSeparator(settings.getString(CFG_ROW_DELIMITER));
        csvConfig.setQuote(settings.getString(CFG_QUOTE_CHAR));
        csvConfig.setQuoteEscape(settings.getString(CFG_QUOTE_ESCAPE_CHAR));
        csvConfig.setComment(settings.getString(CFG_COMMENT_CHAR));
    }

    private static void loadAdvancedSettingsTabInModel(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsRO settings) throws InvalidSettingsException {
        SpecMergeMode specMergeMode;
        try {
            specMergeMode =
                SpecMergeMode.valueOf(settings.getString(CFG_SPEC_MERGE_MODE, SpecMergeMode.INTERSECTION.name()));
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException(ex);
        }
        config.setSpecMergeMode(specMergeMode);

        final DefaultTableReadConfig<CSVTableReaderConfig> tc = config.getTableReadConfig();
        tc.setLimitRowsForSpec(settings.getBoolean(CFG_LIMIT_DATA_ROWS_SCANNED));
        tc.setMaxRowsForSpec(settings.getLong(CFG_MAX_DATA_ROWS_SCANNED));

        final CSVTableReaderConfig cc = tc.getReaderSpecificConfig();
        cc.setReplaceEmptyWithMissing(settings.getBoolean(CFG_REPLACE_EMPTY_QUOTES_WITH_MISSING));

        QuoteOption quoteOption;
        try {
            quoteOption = QuoteOption.valueOf(settings.getString(CFG_QUOTE_OPTION));
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException(ex);
        }
        cc.setQuoteOption(quoteOption);

        cc.limitCharsPerColumn(settings.getBoolean(CFG_LIMIT_MEMORY_PER_COLUMN));
        cc.setMaxColumns(settings.getInt(CFG_MAXIMUM_NUMBER_OF_COLUMNS));
    }

    private static void loadLimitRowsTabInModel(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsRO settings) throws InvalidSettingsException {
        final DefaultTableReadConfig<?> tc = config.getTableReadConfig();
        tc.setSkipRows(settings.getBoolean(CFG_SKIP_DATA_ROWS));
        tc.setNumRowsToSkip(settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP));
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS));

        final CSVTableReaderConfig cc = config.getReaderSpecificConfig();
        cc.setSkipLines(settings.getBoolean(CFG_SKIP_LINES));
        cc.setNumLinesToSkip(settings.getLong(CFG_NUMBER_OF_LINES_TO_SKIP));
    }

    private static void loadEncodingTabInModel(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsRO settings) throws InvalidSettingsException {
        config.getReaderSpecificConfig().setCharSetName(settings.getString(CFG_CHARSET));
    }

    @Override
    public void saveInModel(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsWO settings) {
        if (config.hasTableSpecConfig()) {
            config.getTableSpecConfig().save(settings.addNodeSettings(CFG_TABLE_SPEC_CONFIG));
        }

        saveSettingsTab(config, settings.addNodeSettings(CFG_SETTINGS_TAB));
        saveAdvancedSettingsTab(config, settings.addNodeSettings(CFG_ADVANCED_SETTINGS_TAB));
        saveLimitRowsTab(config, settings.addNodeSettings(CFG_LIMIT_ROWS_TAB));
        saveEncodingTab(config, settings.addNodeSettings(CFG_ENCODING_TAB));
    }

    private static void saveSettingsTab(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsWO settings) {
        final TableReadConfig<?> tc = config.getTableReadConfig();
        settings.addBoolean(CFG_HAS_COLUMN_HEADER, tc.useColumnHeaderIdx());
        settings.addBoolean(CFG_HAS_ROW_ID, tc.useRowIDIdx());
        settings.addBoolean(CFG_SUPPORT_SHORT_DATA_ROWS, tc.allowShortRows());
        settings.addBoolean(CFG_SKIP_EMPTY_DATA_ROWS, tc.skipEmptyRows());

        final CSVTableReaderConfig cc = config.getReaderSpecificConfig();
        settings.addString(CFG_COMMENT_CHAR, cc.getComment());
        settings.addString(CFG_DELIMITER, cc.getDelimiter());
        settings.addString(CFG_QUOTE_CHAR, cc.getQuote());
        settings.addString(CFG_QUOTE_ESCAPE_CHAR, cc.getQuoteEscape());
        settings.addString(CFG_ROW_DELIMITER, cc.getLineSeparator());
        settings.addInt(CFG_AUTODETECT_BUFFER_SIZE, cc.getAutoDetectionBufferSize());
    }

    private static void saveAdvancedSettingsTab(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsWO settings) {
        settings.addString(CFG_SPEC_MERGE_MODE, config.getSpecMergeMode().name());

        final TableReadConfig<?> tc = config.getTableReadConfig();
        settings.addBoolean(CFG_LIMIT_DATA_ROWS_SCANNED, tc.limitRowsForSpec());
        settings.addLong(CFG_MAX_DATA_ROWS_SCANNED, tc.getMaxRowsForSpec());

        final CSVTableReaderConfig cc = config.getReaderSpecificConfig();
        settings.addBoolean(CFG_LIMIT_MEMORY_PER_COLUMN, cc.isCharsPerColumnLimited());
        settings.addInt(CFG_MAXIMUM_NUMBER_OF_COLUMNS, cc.getMaxColumns());

        settings.addString(CFG_QUOTE_OPTION, cc.getQuoteOption().name());
        settings.addBoolean(CFG_REPLACE_EMPTY_QUOTES_WITH_MISSING, cc.replaceEmptyWithMissing());

    }

    private static void saveLimitRowsTab(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsWO settings) {

        final CSVTableReaderConfig cc = config.getReaderSpecificConfig();
        settings.addBoolean(CFG_SKIP_LINES, cc.skipLines());
        settings.addLong(CFG_NUMBER_OF_LINES_TO_SKIP, cc.getNumLinesToSkip());

        final TableReadConfig<?> tc = config.getTableReadConfig();
        settings.addBoolean(CFG_SKIP_DATA_ROWS, tc.skipRows());
        settings.addLong(CFG_NUMBER_OF_ROWS_TO_SKIP, tc.getNumRowsToSkip());
        settings.addBoolean(CFG_LIMIT_DATA_ROWS, tc.limitRows());
        settings.addLong(CFG_MAX_ROWS, tc.getMaxRows());
    }

    private static void saveEncodingTab(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsWO settings) {
        settings.addString(CFG_CHARSET, config.getReaderSpecificConfig().getCharSetName());
    }

    @Override
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            TableSpecConfig.validate(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG),
                StringReadAdapterFactory.INSTANCE.getProducerRegistry());
        }
        validateSettingsTab(settings.getNodeSettings(CFG_SETTINGS_TAB));
        validateAdvancedSettingsTab(settings.getNodeSettings(CFG_ADVANCED_SETTINGS_TAB));
        validateLimitRowsTab(settings.getNodeSettings(CFG_LIMIT_ROWS_TAB));
        validateEncodingTab(settings.getNodeSettings(CFG_ENCODING_TAB));
    }

    public static void validateSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_SUPPORT_SHORT_DATA_ROWS);
        settings.getBoolean(CFG_HAS_COLUMN_HEADER);
        settings.getBoolean(CFG_HAS_ROW_ID);
        settings.getBoolean(CFG_SKIP_EMPTY_DATA_ROWS);
        settings.getInt(CFG_AUTODETECT_BUFFER_SIZE);
        settings.getString(CFG_DELIMITER);
        settings.getString(CFG_ROW_DELIMITER);
        settings.getString(CFG_QUOTE_CHAR);
        settings.getString(CFG_QUOTE_ESCAPE_CHAR);
        settings.getString(CFG_COMMENT_CHAR);

    }

    public static void validateAdvancedSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            SpecMergeMode.valueOf(settings.getString(CFG_SPEC_MERGE_MODE, SpecMergeMode.INTERSECTION.name()));
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException(ex);
        }

        settings.getBoolean(CFG_LIMIT_DATA_ROWS_SCANNED);
        settings.getLong(CFG_MAX_DATA_ROWS_SCANNED);

        settings.getBoolean(CFG_REPLACE_EMPTY_QUOTES_WITH_MISSING);

        try {
            QuoteOption.valueOf(settings.getString(CFG_QUOTE_OPTION));
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException(ex);
        }

        settings.getBoolean(CFG_LIMIT_MEMORY_PER_COLUMN);
        settings.getInt(CFG_MAXIMUM_NUMBER_OF_COLUMNS);

    }

    public static void validateLimitRowsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_SKIP_DATA_ROWS);
        settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP);
        settings.getBoolean(CFG_LIMIT_DATA_ROWS);
        settings.getLong(CFG_MAX_ROWS);
        settings.getBoolean(CFG_SKIP_LINES);
        settings.getLong(CFG_NUMBER_OF_LINES_TO_SKIP);
    }

    public static void validateEncodingTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFG_CHARSET);
    }

    @Override
    public void saveInDialog(
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        saveInModel(config, settings);
    }

}
