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
 *   4 Nov 2020 (lars.schweikardt): created
 */
package org.knime.base.node.io.filehandling.linereader;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.reader.config.ConfigSerializer;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDFactory;
import org.knime.filehandling.core.node.table.reader.config.tablespec.NodeSettingsConfigID;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * The {@link ConfigSerializer} for the line reader node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
enum LineReaderMultiTableReadConfigSerializer
    implements ConfigSerializer<LineMultiTableReadConfig>, ConfigIDFactory<LineMultiTableReadConfig> {

        /**
         * Singleton instance.
         */
        INSTANCE;

    private static final String CFG_ROW_HEADER_PREFIX = "row_header_prefix";

    private static final String CFG_CHARSET = "charset";

    private static final String CFG_HAS_COL_HEADER = "use_column_header";

    private static final String CFG_CUSTOM_COL_HEADER = "custom_col_header";

    private static final String CFG_LIMIT_DATA_ROWS = "limit_data_rows";

    private static final String CFG_MAX_ROWS = "max_rows";

    private static final String CFG_REGEX = "regex";

    private static final String CFG_USE_REGEX = "use_regex";

    private static final String CFG_EMPTY_LINE_MODE = "empty_line_mode";

    private static final String CFG_EMPTY_REPLACEMENT = "empty_line_replacement";

    private static final String CFG_FAIL_DIFFERING_SPECS = "fail_on_different_specs";

    private static final String CFG_ADVANCED_SETTINGS_TAB = "advanced_settings";

    private static final String CFG_ENCODING_TAB = "encoding";

    private static final String CFG_PREPEND_PATH_COLUMN = "prepend_path_column" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PATH_COLUMN_NAME = "path_column_name" + SettingsModel.CFGKEY_INTERNAL;

    @Override
    public void loadInDialog(final LineMultiTableReadConfig config, final NodeSettingsRO settings,
        final PortObjectSpec[] specs) {
        loadSettingsTabInDialog(config, SettingsUtils.getOrEmpty(settings, SettingsUtils.CFG_SETTINGS_TAB));
        loadAdvancedSettingsTabInDialog(config, SettingsUtils.getOrEmpty(settings, CFG_ADVANCED_SETTINGS_TAB));
        loadEncodingTabInDialog(config, SettingsUtils.getOrEmpty(settings, CFG_ENCODING_TAB));
    }

    private static void loadSettingsTabInDialog(final LineMultiTableReadConfig config, final NodeSettingsRO settings) {

        config.setFailOnDifferingSpecs(settings.getBoolean(CFG_FAIL_DIFFERING_SPECS, true));

        final DefaultTableReadConfig<LineReaderConfig2> tc = config.getTableReadConfig();
        tc.setPrefixForGeneratedRowIds(settings.getString(CFG_ROW_HEADER_PREFIX, "Row"));
        tc.setRowIDIdx(-1);
        tc.setUseColumnHeaderIdx(settings.getBoolean(CFG_HAS_COL_HEADER, false));
        tc.setLimitRowsForSpec(false);

        final LineReaderConfig2 lineReaderCfg = config.getReaderSpecificConfig();
        lineReaderCfg.setColumnHeaderName(settings.getString(CFG_CUSTOM_COL_HEADER, "Column"));
    }

    private static void loadAdvancedSettingsTabInDialog(final LineMultiTableReadConfig config,
        final NodeSettingsRO settings) {

        // added in 4.4.0
        config.setPrependItemIdentifierColumn(
            settings.getBoolean(CFG_PREPEND_PATH_COLUMN, config.prependItemIdentifierColumn()));
        config.setItemIdentifierColumnName(
            settings.getString(CFG_PATH_COLUMN_NAME, config.getItemIdentifierColumnName()));

        final DefaultTableReadConfig<LineReaderConfig2> tc = config.getTableReadConfig();
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS, false));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS, 1000L));

        final LineReaderConfig2 lineReaderCfg = config.getReaderSpecificConfig();
        lineReaderCfg.setRegex(settings.getString(CFG_REGEX, ".*"));
        lineReaderCfg.setUseRegex(settings.getBoolean(CFG_USE_REGEX, false));
        lineReaderCfg.setEmptyLineReplacement(settings.getString(CFG_EMPTY_REPLACEMENT, ""));
        lineReaderCfg.setEmptyLineMode(
            EmptyLineMode.valueOf(settings.getString(CFG_EMPTY_LINE_MODE, EmptyLineMode.REPLACE_BY_MISSING.name())));
    }

    @Override
    public void loadInModel(final LineMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        loadSettingsTabInModel(config, settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        loadAdvancedSettingsTabInModel(config, settings.getNodeSettings(CFG_ADVANCED_SETTINGS_TAB));
        loadEncodingTabInModel(config, settings.getNodeSettings(CFG_ENCODING_TAB));
    }

    private static void loadSettingsTabInModel(final LineMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {

        config.setFailOnDifferingSpecs(settings.getBoolean(CFG_FAIL_DIFFERING_SPECS));

        final DefaultTableReadConfig<LineReaderConfig2> tc = config.getTableReadConfig();
        tc.setUseColumnHeaderIdx(settings.getBoolean(CFG_HAS_COL_HEADER));
        tc.setUseRowIDIdx(false);
        tc.setRowIDIdx(-1);
        tc.setColumnHeaderIdx(0);
        tc.setLimitRowsForSpec(false);
        tc.setPrefixForGeneratedRowIds(settings.getString(CFG_ROW_HEADER_PREFIX));

        final LineReaderConfig2 lineReaderCfg = config.getReaderSpecificConfig();
        lineReaderCfg.setColumnHeaderName(settings.getString(CFG_CUSTOM_COL_HEADER));
    }

    private static void loadAdvancedSettingsTabInModel(final LineMultiTableReadConfig config,
        final NodeSettingsRO settings) throws InvalidSettingsException {

        // added in 4.4.0
        if (settings.containsKey(CFG_PREPEND_PATH_COLUMN)) {
            config.setPrependItemIdentifierColumn(settings.getBoolean(CFG_PREPEND_PATH_COLUMN));
            config.setItemIdentifierColumnName(settings.getString(CFG_PATH_COLUMN_NAME));
        }

        final DefaultTableReadConfig<LineReaderConfig2> tc = config.getTableReadConfig();

        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS));

        final LineReaderConfig2 lineReaderCfg = config.getReaderSpecificConfig();

        lineReaderCfg.setRegex(settings.getString(CFG_REGEX));
        lineReaderCfg.setUseRegex(settings.getBoolean(CFG_USE_REGEX));
        lineReaderCfg.setEmptyLineReplacement(settings.getString(CFG_EMPTY_REPLACEMENT));
        lineReaderCfg.setEmptyLineMode(EmptyLineMode.valueOf(settings.getString(CFG_EMPTY_LINE_MODE)));

        tc.setSkipEmptyRows(lineReaderCfg.getReplaceEmptyMode() == EmptyLineMode.SKIP_EMPTY);
    }

    @Override
    public void saveInModel(final LineMultiTableReadConfig config, final NodeSettingsWO settings) {
        saveSettingsTab(config, SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
        saveAdvancedSettingsTab(config, settings.addNodeSettings(CFG_ADVANCED_SETTINGS_TAB));
        saveEncodingTab(config, settings.addNodeSettings(CFG_ENCODING_TAB));
    }

    private static void saveSettingsTab(final LineMultiTableReadConfig config, final NodeSettingsWO settings) {
        final TableReadConfig<LineReaderConfig2> tc = config.getTableReadConfig();
        final LineReaderConfig2 lineReaderCfg = config.getReaderSpecificConfig();

        settings.addBoolean(CFG_FAIL_DIFFERING_SPECS, config.failOnDifferingSpecs());
        settings.addString(CFG_ROW_HEADER_PREFIX, tc.getPrefixForGeneratedRowIDs());
        settings.addBoolean(CFG_HAS_COL_HEADER, tc.useColumnHeaderIdx());
        settings.addString(CFG_CUSTOM_COL_HEADER, lineReaderCfg.getColumnHeaderName());
    }

    private static void saveAdvancedSettingsTab(final LineMultiTableReadConfig config, final NodeSettingsWO settings) {

        settings.addBoolean(CFG_PREPEND_PATH_COLUMN, config.prependItemIdentifierColumn());
        settings.addString(CFG_PATH_COLUMN_NAME, config.getItemIdentifierColumnName());

        final TableReadConfig<LineReaderConfig2> tc = config.getTableReadConfig();
        final LineReaderConfig2 lineReaderCfg = config.getReaderSpecificConfig();

        settings.addString(CFG_EMPTY_LINE_MODE, lineReaderCfg.getReplaceEmptyMode().name());
        settings.addString(CFG_EMPTY_REPLACEMENT, lineReaderCfg.getEmptyLineReplacement());
        settings.addBoolean(CFG_USE_REGEX, lineReaderCfg.useRegex());
        settings.addString(CFG_REGEX, lineReaderCfg.getRegex());
        settings.addBoolean(CFG_LIMIT_DATA_ROWS, tc.limitRows());
        settings.addLong(CFG_MAX_ROWS, tc.getMaxRows());
    }

    @Override
    public void saveInDialog(final LineMultiTableReadConfig config, final NodeSettingsWO settings) {
        saveInModel(config, settings);
    }

    private static void loadEncodingTabInDialog(final LineMultiTableReadConfig config, final NodeSettingsRO settings) {
        config.getReaderSpecificConfig().setCharSetName(settings.getString(CFG_CHARSET, null));
    }

    private static void loadEncodingTabInModel(final LineMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        config.getReaderSpecificConfig().setCharSetName(settings.getString(CFG_CHARSET));
    }

    private static void saveEncodingTab(final LineMultiTableReadConfig config, final NodeSettingsWO settings) {
        settings.addString(CFG_CHARSET, config.getReaderSpecificConfig().getCharSetName());
    }

    private static void validateEncodingTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFG_CHARSET);
    }

    public static void validateSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_FAIL_DIFFERING_SPECS);
        settings.getBoolean(CFG_HAS_COL_HEADER);
        settings.getString(CFG_CUSTOM_COL_HEADER);
        settings.getString(CFG_ROW_HEADER_PREFIX);
    }

    public static void validateAdvancedSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_LIMIT_DATA_ROWS);
        settings.getLong(CFG_MAX_ROWS);
        settings.getString(CFG_REGEX);
        settings.getBoolean(CFG_USE_REGEX);
        settings.getString(CFG_EMPTY_LINE_MODE);
        settings.getString(CFG_EMPTY_REPLACEMENT);
        // added in 4.4.0
        if (settings.containsKey(CFG_PREPEND_PATH_COLUMN)) {
            settings.getBoolean(CFG_PREPEND_PATH_COLUMN);
            settings.getString(CFG_PATH_COLUMN_NAME);
        }
    }

    @Override
    public void validate(final LineMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        validateSettingsTab(settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        validateAdvancedSettingsTab(settings.getNodeSettings(CFG_ADVANCED_SETTINGS_TAB));
        validateEncodingTab(settings.getNodeSettings(CFG_ENCODING_TAB));
    }

    @Override
    public ConfigID createFromConfig(final LineMultiTableReadConfig config) {
        final NodeSettings settings = new NodeSettings("line_reader");
        saveConfigIDSettingsTab(config, settings.addNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        return new NodeSettingsConfigID(settings);
    }

    private static void saveConfigIDSettingsTab(final LineMultiTableReadConfig config, final NodeSettingsWO settings) {
        settings.addBoolean(CFG_HAS_COL_HEADER, config.getTableReadConfig().useColumnHeaderIdx());
        settings.addString(CFG_CUSTOM_COL_HEADER, config.getReaderSpecificConfig().getColumnHeaderName());
    }

    @Override
    public ConfigID createFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        return new NodeSettingsConfigID(settings.getNodeSettings("line_reader"));
    }
}
