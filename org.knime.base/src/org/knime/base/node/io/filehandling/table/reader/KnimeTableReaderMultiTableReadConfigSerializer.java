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
 *   27 May 2021 (moditha.hewasinghge): created
 */
package org.knime.base.node.io.filehandling.table.reader;

import static org.knime.base.node.preproc.manipulator.mapping.DataTypeProducerRegistry.PATH_SERIALIZER;
import static org.knime.filehandling.core.util.SettingsUtils.getOrEmpty;

import org.knime.base.node.preproc.manipulator.TableManipulatorConfig;
import org.knime.base.node.preproc.manipulator.TableManipulatorConfigSerializer.DataTypeSerializer;
import org.knime.core.data.DataType;
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
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigSerializer;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * The {@link ConfigSerializer} for the table reader node.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 */
enum KnimeTableMultiTableReadConfigSerializer
    implements ConfigSerializer<KnimeTableMultiTableReadConfig>, ConfigIDFactory<KnimeTableMultiTableReadConfig> {

        /**
         * Singleton instance.
         */
        INSTANCE;

    private KnimeTableMultiTableReadConfigSerializer() {
        m_tableSpecSerializer =
            TableSpecConfigSerializer.createStartingV43(PATH_SERIALIZER, this, DataTypeSerializer.SERIALIZER_INSTANCE);
    }

    private static final String CFG_HAS_ROW_ID = "has_row_id";

    private static final String CFG_PREPEND_TABLE_IDX_TO_ROWID = "prepend_table_index_to_row_id";

    private static final String CFG_TABLE_SPEC_CONFIG = "table_spec_config" + SettingsModel.CFGKEY_INTERNAL;

    private final TableSpecConfigSerializer<DataType> m_tableSpecSerializer;

    private static final String CFG_SETTINGS_TAB = "settings";

    private static final String CFG_LIMIT_DATA_ROWS = "limit_data_rows";

    private static final String CFG_MAX_ROWS = "max_rows";

    private static final String CFG_ADVANCED_SETTINGS_TAB = "advanced_settings";

    private static final String CFG_NUMBER_OF_ROWS_TO_SKIP = "number_of_rows_to_skip";

    private static final String CFG_SKIP_DATA_ROWS = "skip_data_rows";

    private static final String CFG_SAVE_TABLE_SPEC_CONFIG = "save_table_spec_config" + SettingsModel.CFGKEY_INTERNAL;

    private static final boolean DEFAULT_FAIL_ON_DIFFERING_SPECS = true;

    private static final String CFG_FAIL_ON_DIFFERING_SPECS = "fail_on_differing_specs";

    private static final String CFG_APPEND_PATH_COLUMN = "append_path_column" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PATH_COLUMN_NAME = "path_column_name" + SettingsModel.CFGKEY_INTERNAL;

    @Override
    public void loadInDialog(final KnimeTableMultiTableReadConfig config, final NodeSettingsRO settings,
        final PortObjectSpec[] specs) {
        loadSettingsTabInDialog(config, getOrEmpty(settings, CFG_SETTINGS_TAB));
        loadAdvancedSettingsTabInDialog(config, SettingsUtils.getOrEmpty(settings, CFG_ADVANCED_SETTINGS_TAB));
        try {
            config.setTableSpecConfig(loadTableSpecConfig(settings));
        } catch (InvalidSettingsException ex) { // NOSONAR, see below
            /* Can only happen in TableSpecConfig#load, since we checked #NodeSettingsRO#getNodeSettings(String)
             * before. The framework takes care that #validate is called before load so we can assume that this
             * exception does not occur.
             */
        }
    }

    private TableSpecConfig<DataType> loadTableSpecConfig(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            return m_tableSpecSerializer.load(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG));
        } else {
            return null;
        }

    }

    private static void loadSettingsTabInDialog(final KnimeTableMultiTableReadConfig config,
        final NodeSettingsRO settings) {
        final DefaultTableReadConfig<TableManipulatorConfig> tc = config.getTableReadConfig();
        tc.setUseRowIDIdx(settings.getBoolean(CFG_HAS_ROW_ID, false));
        tc.setPrependSourceIdxToRowId(settings.getBoolean(CFG_PREPEND_TABLE_IDX_TO_ROWID, false));

    }

    private static void loadAdvancedSettingsTabInDialog(final KnimeTableMultiTableReadConfig config,
        final NodeSettingsRO settings) {

        config.setFailOnDifferingSpecs(settings.getBoolean(CFG_FAIL_ON_DIFFERING_SPECS, DEFAULT_FAIL_ON_DIFFERING_SPECS));
        config.setSaveTableSpecConfig(settings.getBoolean(CFG_SAVE_TABLE_SPEC_CONFIG, true));
        config.setAppendItemIdentifierColumn(settings.getBoolean(CFG_APPEND_PATH_COLUMN, false));
        config.setItemIdentifierColumnName(
            settings.getString(CFG_PATH_COLUMN_NAME, config.getItemIdentifierColumnName()));

        final DefaultTableReadConfig<TableManipulatorConfig> tc = config.getTableReadConfig();
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS, false));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS, 1000L));

        tc.setSkipRows(settings.getBoolean(CFG_SKIP_DATA_ROWS, false));
        tc.setNumRowsToSkip(settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP, 1L));
    }

    @Override
    public void loadInModel(final KnimeTableMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        loadSettingsTabInModel(config, settings.getNodeSettings(CFG_SETTINGS_TAB));
        loadAdvancedSettingsTabInModel(config, settings.getNodeSettings(CFG_ADVANCED_SETTINGS_TAB));
        config.setTableSpecConfig(loadTableSpecConfig(settings));
    }

    private static void loadSettingsTabInModel(final KnimeTableMultiTableReadConfig config,
        final NodeSettingsRO settings) throws InvalidSettingsException {
        final DefaultTableReadConfig<TableManipulatorConfig> tc = config.getTableReadConfig();
        tc.setUseRowIDIdx(settings.getBoolean(CFG_HAS_ROW_ID));
        tc.setPrependSourceIdxToRowId(settings.getBoolean(CFG_PREPEND_TABLE_IDX_TO_ROWID));
    }

    private static void loadAdvancedSettingsTabInModel(final KnimeTableMultiTableReadConfig config,
        final NodeSettingsRO settings) throws InvalidSettingsException {
        config.setFailOnDifferingSpecs(settings.getBoolean(CFG_FAIL_ON_DIFFERING_SPECS));
        config.setSaveTableSpecConfig(settings.getBoolean(CFG_SAVE_TABLE_SPEC_CONFIG));
        config.setAppendItemIdentifierColumn(settings.getBoolean(CFG_APPEND_PATH_COLUMN));
        config.setItemIdentifierColumnName(settings.getString(CFG_PATH_COLUMN_NAME));

        final DefaultTableReadConfig<TableManipulatorConfig> tc = config.getTableReadConfig();

        tc.setSkipRows(settings.getBoolean(CFG_SKIP_DATA_ROWS));
        tc.setNumRowsToSkip(settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP));
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS));

    }

    @Override
    public void saveInModel(final KnimeTableMultiTableReadConfig config, final NodeSettingsWO settings) {
        if (config.hasTableSpecConfig()) {
            m_tableSpecSerializer.save(config.getTableSpecConfig(), settings.addNodeSettings(CFG_TABLE_SPEC_CONFIG));
        }
        saveSettingsTab(config, SettingsUtils.getOrAdd(settings, CFG_SETTINGS_TAB));
        saveAdvancedSettingsTab(config, settings.addNodeSettings(CFG_ADVANCED_SETTINGS_TAB));
    }

    private static void saveAdvancedSettingsTab(final KnimeTableMultiTableReadConfig config,
        final NodeSettingsWO settings) {
        final TableReadConfig<TableManipulatorConfig> tc = config.getTableReadConfig();
        settings.addBoolean(CFG_SKIP_DATA_ROWS, tc.skipRows());
        settings.addLong(CFG_NUMBER_OF_ROWS_TO_SKIP, tc.getNumRowsToSkip());
        settings.addBoolean(CFG_LIMIT_DATA_ROWS, tc.limitRows());
        settings.addLong(CFG_MAX_ROWS, tc.getMaxRows());
        settings.addBoolean(CFG_FAIL_ON_DIFFERING_SPECS, config.failOnDifferingSpecs());
        settings.addBoolean(CFG_SAVE_TABLE_SPEC_CONFIG, config.saveTableSpecConfig());
        settings.addBoolean(CFG_APPEND_PATH_COLUMN, config.appendItemIdentifierColumn());
        settings.addString(CFG_PATH_COLUMN_NAME, config.getItemIdentifierColumnName());
    }

    @Override
    public void saveInDialog(final KnimeTableMultiTableReadConfig config, final NodeSettingsWO settings) {
        saveInModel(config, settings);
    }

    public static void validateAdvancedSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_LIMIT_DATA_ROWS);
        settings.getLong(CFG_MAX_ROWS);
        settings.getBoolean(CFG_SKIP_DATA_ROWS);
        settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP);
        settings.getBoolean(CFG_SAVE_TABLE_SPEC_CONFIG);
        settings.getBoolean(CFG_APPEND_PATH_COLUMN);
        settings.getString(CFG_PATH_COLUMN_NAME);
    }

    private static void saveSettingsTab(final KnimeTableMultiTableReadConfig config, final NodeSettingsWO settings) {
        final TableReadConfig<?> tc = config.getTableReadConfig();
        settings.addBoolean(CFG_HAS_ROW_ID, tc.useRowIDIdx());
        settings.addBoolean(CFG_PREPEND_TABLE_IDX_TO_ROWID, tc.prependSourceIdxToRowID());
    }

    static void validateSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_HAS_ROW_ID);
        settings.getBoolean(CFG_PREPEND_TABLE_IDX_TO_ROWID);
    }

    @Override
    public void validate(final KnimeTableMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        loadTableSpecConfig(settings);
        validateSettingsTab(settings.getNodeSettings(CFG_SETTINGS_TAB));
        validateAdvancedSettingsTab(settings.getNodeSettings(CFG_ADVANCED_SETTINGS_TAB));
    }

    @Override
    public ConfigID createFromConfig(final KnimeTableMultiTableReadConfig config) {
        final NodeSettings settings = new NodeSettings("table_reader");
        return new NodeSettingsConfigID(settings);
    }

    @Override
    public ConfigID createFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        return new NodeSettingsConfigID(settings.getNodeSettings("table_reader"));
    }
}
