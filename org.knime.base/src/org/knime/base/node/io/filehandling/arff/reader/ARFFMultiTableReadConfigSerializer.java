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
 *   02.07.2022. (Dragan Keselj): created
 */
package org.knime.base.node.io.filehandling.arff.reader;

import org.knime.core.data.DataType;
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
import org.knime.filehandling.core.node.table.reader.config.tablespec.DefaultProductionPathSerializer;
import org.knime.filehandling.core.node.table.reader.config.tablespec.NodeSettingsConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.NodeSettingsSerializer;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigSerializer;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * The {@link ConfigSerializer} for the ARFF reader node. This class serializes the settings for the reader node. <br>
 * Abstracting the settings loading in this way allows for:
 * <ul>
 * <li>Flexibility e.g. for backwards compatible loading
 * <li>Storing the settings in such a way that they reflect the tabs and naming of the dialog.
 * </ul>
 *
 * @author Dragan Keselj, Redfield SE
 */
enum ARFFMultiTableReadConfigSerializer
    implements ConfigSerializer<ARFFMultiTableReadConfig>, ConfigIDFactory<ARFFMultiTableReadConfig> {

        /**
         * Singleton instance.
         */
        INSTANCE;

    private static final String KEY = "example_csv_reader";

    private static final String CFG_LIMIT_ROWS_TAB = "limit_rows";

    private static final String CFG_LIMIT_DATA_ROWS = "is_limit_rows";

    private static final String CFG_MAX_ROWS = "limit_rows";

    private static final String CFG_SKIP_DATA_ROWS = "is_skip_rows";

    private static final String CFG_NUMBER_OF_ROWS_TO_SKIP = "skip_rows";

    /*
     * Appending SettingsModel.CFGKEY_INTERNAL hides the setting in the flow variable tab of the dialog.
     */

    private static final String CFG_APPEND_PATH_COLUMN = "append_path_column" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PATH_COLUMN_NAME = "path_column_name" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_TABLE_SPEC_CONFIG = "table_spec_config" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_ROW_HEADER_PREFIX = "row_header_prefix";

    private final TableSpecConfigSerializer<DataType> m_tableSpecConfigSerializer;

    /**
     * Constructor for the singleton instance. Invoked by Java when the class is loaded.
     */
    private ARFFMultiTableReadConfigSerializer() {
        m_tableSpecConfigSerializer = TableSpecConfigSerializer.createStartingV44(//
            // loads and saves the production paths specified by the user
            new DefaultProductionPathSerializer(ARFFReadAdapterFactory.INSTANCE.getProducerRegistry()),
            // loads and saves an identifier of the config that allows to check if the config changed
            this,
            // used to save the type of columns
            new NodeSettingsSerializer<DataType>() {

                @Override
                public void save(final DataType object, final NodeSettingsWO settings) {
                    settings.addDataType("type", object);
                }

                @Override
                public DataType load(final NodeSettingsRO settings) throws InvalidSettingsException {
                    return settings.getDataType("type");
                }
            });
    }

    @Override
    public void loadInDialog(final ARFFMultiTableReadConfig config, final NodeSettingsRO settings,
        final PortObjectSpec[] specs) {
        loadSettingsTabInDialog(config, SettingsUtils.getOrEmpty(settings, SettingsUtils.CFG_SETTINGS_TAB));
        loadLimitRowsTabInDialog(config, SettingsUtils.getOrEmpty(settings, CFG_LIMIT_ROWS_TAB));
        loadTransformationTabInDialog(config, settings);
    }

    private void loadTransformationTabInDialog(final ARFFMultiTableReadConfig config, final NodeSettingsRO settings) {
        try {
            loadTransformationTabInModel(config, settings);
        } catch (InvalidSettingsException ex) { //NOSONAR
            // can be ignored, then the dialog will create a new TableSpecConfig when it parsed the input file.
        }
    }

    private static void loadSettingsTabInDialog(final ARFFMultiTableReadConfig config, final NodeSettingsRO settings) {
        config.setAppendItemIdentifierColumn(
            settings.getBoolean(CFG_APPEND_PATH_COLUMN, config.appendItemIdentifierColumn()));
        config.setItemIdentifierColumnName(
            settings.getString(CFG_PATH_COLUMN_NAME, config.getItemIdentifierColumnName()));
        config.getTableReadConfig().setPrefixForGeneratedRowIds(settings.getString(CFG_ROW_HEADER_PREFIX, "Row"));
    }

    private static void loadLimitRowsTabInDialog(final ARFFMultiTableReadConfig config, final NodeSettingsRO settings) {
        final DefaultTableReadConfig<ARFFReaderConfig> tc = config.getTableReadConfig();
        tc.setSkipRows(settings.getBoolean(CFG_SKIP_DATA_ROWS, false));
        tc.setNumRowsToSkip(settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP, 0));
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS, false));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS, 50L));
    }

    @Override
    public void loadInModel(final ARFFMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        loadSettingsTabInModel(config, settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        loadLimitRowsTabInModel(config, settings.getNodeSettings(CFG_LIMIT_ROWS_TAB));
        loadTransformationTabInModel(config, settings);
    }

    private TableSpecConfig<DataType> loadTableSpecConfig(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            return m_tableSpecConfigSerializer.load(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG));
        } else {
            return null;
        }
    }

    private void loadTransformationTabInModel(final ARFFMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        config.setTableSpecConfig(loadTableSpecConfig(settings));
    }

    private static void loadSettingsTabInModel(final ARFFMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        config.setAppendItemIdentifierColumn(settings.getBoolean(CFG_APPEND_PATH_COLUMN));
        config.setItemIdentifierColumnName(settings.getString(CFG_PATH_COLUMN_NAME));
        config.getTableReadConfig().setPrefixForGeneratedRowIds(settings.getString(CFG_ROW_HEADER_PREFIX));
    }

    private static void loadLimitRowsTabInModel(final ARFFMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final DefaultTableReadConfig<ARFFReaderConfig> tc = config.getTableReadConfig();
        tc.setSkipRows(settings.getBoolean(CFG_SKIP_DATA_ROWS));
        tc.setNumRowsToSkip(settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP));
        tc.setLimitRows(settings.getBoolean(CFG_LIMIT_DATA_ROWS));
        tc.setMaxRows(settings.getLong(CFG_MAX_ROWS));
    }

    @Override
    public void saveInModel(final ARFFMultiTableReadConfig config, final NodeSettingsWO settings) {
        if (config.hasTableSpecConfig()) {
            m_tableSpecConfigSerializer.save(config.getTableSpecConfig(),
                settings.addNodeSettings(CFG_TABLE_SPEC_CONFIG));
        }
        saveSettingsTab(config, SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
        saveLimitRowsTab(config, settings.addNodeSettings(CFG_LIMIT_ROWS_TAB));
    }

    private static void saveSettingsTab(final ARFFMultiTableReadConfig config, final NodeSettingsWO settings) {
        settings.addString(CFG_ROW_HEADER_PREFIX, config.getTableReadConfig().getPrefixForGeneratedRowIDs());
        settings.addBoolean(CFG_APPEND_PATH_COLUMN, config.appendItemIdentifierColumn());
        settings.addString(CFG_PATH_COLUMN_NAME, config.getItemIdentifierColumnName());
    }

    private static void saveLimitRowsTab(final ARFFMultiTableReadConfig config, final NodeSettingsWO settings) {
        final TableReadConfig<ARFFReaderConfig> tc = config.getTableReadConfig();

        settings.addBoolean(CFG_SKIP_DATA_ROWS, tc.skipRows());
        settings.addLong(CFG_NUMBER_OF_ROWS_TO_SKIP, tc.getNumRowsToSkip());
        settings.addBoolean(CFG_LIMIT_DATA_ROWS, tc.limitRows());
        settings.addLong(CFG_MAX_ROWS, tc.getMaxRows());
    }

    @Override
    public void saveInDialog(final ARFFMultiTableReadConfig config, final NodeSettingsWO settings) {
        saveInModel(config, settings);
    }

    @Override
    public void validate(final ARFFMultiTableReadConfig config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        validateSettingsTab(settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        validateLimitRowsTab(settings.getNodeSettings(CFG_LIMIT_ROWS_TAB));
    }

    public static void validateSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_APPEND_PATH_COLUMN);
        settings.getString(CFG_PATH_COLUMN_NAME);
        settings.getString(CFG_ROW_HEADER_PREFIX);
    }

    private static void validateLimitRowsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_SKIP_DATA_ROWS);
        settings.getLong(CFG_NUMBER_OF_ROWS_TO_SKIP);
        settings.getBoolean(CFG_LIMIT_DATA_ROWS);
        settings.getLong(CFG_MAX_ROWS);
    }

    @Override
    public ConfigID createFromConfig(final ARFFMultiTableReadConfig config) {
        final var settings = new NodeSettings(KEY);
        saveConfigIDSettingsTab(config, settings.addNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        saveConfigIDLimitRowsTab(config, settings.addNodeSettings(CFG_LIMIT_ROWS_TAB));
        return new NodeSettingsConfigID(settings);
    }

    @SuppressWarnings("unused")
    private static void saveConfigIDSettingsTab(final ARFFMultiTableReadConfig config, final NodeSettingsWO settings) {
        //
    }

    private static void saveConfigIDLimitRowsTab(final ARFFMultiTableReadConfig config, final NodeSettingsWO settings) {
        final TableReadConfig<ARFFReaderConfig> tc = config.getTableReadConfig();
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
