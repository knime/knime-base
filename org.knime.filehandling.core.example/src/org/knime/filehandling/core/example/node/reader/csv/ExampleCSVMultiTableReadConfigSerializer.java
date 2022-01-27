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
 *   28 Jun 2021 (Moditha Hewasinghaget): created
 */
package org.knime.filehandling.core.example.node.reader.csv;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.ConfigSerializer;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDFactory;
import org.knime.filehandling.core.node.table.reader.config.tablespec.NodeSettingsConfigID;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * The {@link ConfigSerializer} for the example CSV reader node. This class
 * serializes the settings for the reader node.
 * 
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 */
enum ExampleCSVMultiTableReadConfigSerializer
        implements ConfigSerializer<ExampleCSVMultiTableReadConfig>, ConfigIDFactory<ExampleCSVMultiTableReadConfig> {

    /**
     * Singleton instance.
     */
    INSTANCE;

    private static final String KEY = "example_csv_reader";

    private static final String CFG_COL_HEADER_PREFIX = "col_header_prefix";

    private static final String CFG_LIMIT_ROWS_TAB = "limit_rows";

    private static final String CFG_LIMIT_DATA_ROWS = "is_limit_rows";

    private static final String CFG_MAX_ROWS = "limit_rows";

    private static final String CFG_SKIP_DATA_ROWS = "is_skip_rows";

    private static final String CFG_NUMBER_OF_ROWS_TO_SKIP = "skip_rows";

    private static final String CFG_APPEND_PATH_COLUMN = "append_path_column" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PATH_COLUMN_NAME = "path_column_name" + SettingsModel.CFGKEY_INTERNAL;

    @Override
    public void loadInDialog(final ExampleCSVMultiTableReadConfig config, final NodeSettingsRO settings,
            final PortObjectSpec[] specs) {
        loadSettingsTabInDialog(config, SettingsUtils.getOrEmpty(settings, SettingsUtils.CFG_SETTINGS_TAB));
        loadLimitRowsTabInDialog(config, SettingsUtils.getOrEmpty(settings, CFG_LIMIT_ROWS_TAB));
    }

    private static void loadSettingsTabInDialog(final ExampleCSVMultiTableReadConfig config,
            final NodeSettingsRO settings) {
        final ExampleCSVReaderConfig csvReaderCfg = config.getReaderSpecificConfig();
        csvReaderCfg.setColumnHeaderPrefix(settings.getString(CFG_COL_HEADER_PREFIX, "Column"));

        config.setAppendItemIdentifierColumn(
                settings.getBoolean(CFG_APPEND_PATH_COLUMN, config.appendItemIdentifierColumn()));
        config.setItemIdentifierColumnName(
                settings.getString(CFG_PATH_COLUMN_NAME, config.getItemIdentifierColumnName()));

    }

    private static void loadLimitRowsTabInDialog(final ExampleCSVMultiTableReadConfig config,
            final NodeSettingsRO settings) {
        final DefaultTableReadConfig<ExampleCSVReaderConfig> tc = config.getTableReadConfig();
        tc.setSkipRows(settings.getBoolean(CFG_SKIP_DATA_ROWS, false));
        tc.setNumRowsToSkip(settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP, 0));
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS, false));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS, 50L));
    }

    @Override
    public void loadInModel(final ExampleCSVMultiTableReadConfig config, final NodeSettingsRO settings)
            throws InvalidSettingsException {
        loadSettingsTabInModel(config, settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        loadLimitRowsTabInModel(config, settings.getNodeSettings(CFG_LIMIT_ROWS_TAB));
    }

    private static void loadSettingsTabInModel(final ExampleCSVMultiTableReadConfig config,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        final ExampleCSVReaderConfig csvReaderCfg = config.getReaderSpecificConfig();
        csvReaderCfg.setColumnHeaderPrefix(settings.getString(CFG_COL_HEADER_PREFIX, "Column"));

        config.setAppendItemIdentifierColumn(settings.getBoolean(CFG_APPEND_PATH_COLUMN));
        config.setItemIdentifierColumnName(settings.getString(CFG_PATH_COLUMN_NAME));
    }

    private static void loadLimitRowsTabInModel(final ExampleCSVMultiTableReadConfig config,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        final DefaultTableReadConfig<ExampleCSVReaderConfig> tc = config.getTableReadConfig();
        tc.setSkipRows(settings.getBoolean(CFG_SKIP_DATA_ROWS));
        tc.setNumRowsToSkip(settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP));
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS));
    }

    @Override
    public void saveInModel(final ExampleCSVMultiTableReadConfig config, final NodeSettingsWO settings) {
        saveSettingsTab(config, SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
        saveLimitRowsTab(config, settings.addNodeSettings(CFG_LIMIT_ROWS_TAB));
    }

    private static void saveSettingsTab(final ExampleCSVMultiTableReadConfig config, final NodeSettingsWO settings) {
        final ExampleCSVReaderConfig exampleReaderCfg = config.getReaderSpecificConfig();
        settings.addString(CFG_COL_HEADER_PREFIX, exampleReaderCfg.getColumnHeaderPrefix());

        settings.addBoolean(CFG_APPEND_PATH_COLUMN, config.appendItemIdentifierColumn());
        settings.addString(CFG_PATH_COLUMN_NAME, config.getItemIdentifierColumnName());
    }

    private static void saveLimitRowsTab(final ExampleCSVMultiTableReadConfig config, final NodeSettingsWO settings) {
        final TableReadConfig<ExampleCSVReaderConfig> tc = config.getTableReadConfig();

        settings.addBoolean(CFG_SKIP_DATA_ROWS, tc.skipRows());
        settings.addLong(CFG_NUMBER_OF_ROWS_TO_SKIP, tc.getNumRowsToSkip());
        settings.addBoolean(CFG_LIMIT_DATA_ROWS, tc.limitRows());
        settings.addLong(CFG_MAX_ROWS, tc.getMaxRows());
    }

    @Override
    public void saveInDialog(final ExampleCSVMultiTableReadConfig config, final NodeSettingsWO settings) {
        saveInModel(config, settings);
    }

    @Override
    public void validate(final ExampleCSVMultiTableReadConfig config, final NodeSettingsRO settings)
            throws InvalidSettingsException {
        validateSettingsTab(settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        validateLimitRowsTab(settings.getNodeSettings(CFG_LIMIT_ROWS_TAB));
    }

    public static void validateSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFG_COL_HEADER_PREFIX);
        settings.getBoolean(CFG_APPEND_PATH_COLUMN);
        settings.getString(CFG_PATH_COLUMN_NAME);
    }

    private static void validateLimitRowsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_SKIP_DATA_ROWS);
        settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP);
        settings.getBoolean(CFG_LIMIT_DATA_ROWS);
        settings.getLong(CFG_MAX_ROWS);
    }

    @Override
    public ConfigID createFromConfig(final ExampleCSVMultiTableReadConfig config) {
        final NodeSettings settings = new NodeSettings(KEY);
        saveConfigIDSettingsTab(config, settings.addNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        saveConfigIDLimitRowsTab(config, settings.addNodeSettings(CFG_LIMIT_ROWS_TAB));
        return new NodeSettingsConfigID(settings);
    }

    private static void saveConfigIDSettingsTab(final ExampleCSVMultiTableReadConfig config,
            final NodeSettingsWO settings) {
        settings.addString(CFG_COL_HEADER_PREFIX, config.getReaderSpecificConfig().getColumnHeaderPrefix());
    }

    private static void saveConfigIDLimitRowsTab(final ExampleCSVMultiTableReadConfig config,
            final NodeSettingsWO settings) {
        final TableReadConfig<ExampleCSVReaderConfig> tc = config.getTableReadConfig();
        settings.addBoolean(CFG_LIMIT_DATA_ROWS, tc.limitRows());
        settings.addLong(CFG_MAX_ROWS, tc.getMaxRows());
        settings.addBoolean(CFG_SKIP_DATA_ROWS, tc.skipRows());
        settings.addLong(CFG_NUMBER_OF_ROWS_TO_SKIP, tc.getNumRowsToSkip());
    }

    @Override
    public ConfigID createFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        return new NodeSettingsConfigID(settings.getNodeSettings(KEY));
    }
}
