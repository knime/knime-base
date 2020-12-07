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
package org.knime.base.node.preproc.manipulator;

import static org.knime.base.node.preproc.manipulator.mapping.DataTypeProducerRegistry.PATH_LOADER;
import static org.knime.filehandling.core.util.SettingsUtils.getOrEmpty;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.reader.config.ConfigSerializer;
import org.knime.filehandling.core.node.table.reader.config.DefaultMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfigSerializer;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * {@link ConfigSerializer} for CSV reader nodes.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
enum TableManipulatorConfigSerializer implements
    ConfigSerializer<DefaultMultiTableReadConfig<TableManipulatorConfig, DefaultTableReadConfig<TableManipulatorConfig>>> {

        /**
         * Singleton instance.
         */
        INSTANCE;

    private static final Class<String> MOST_GENERIC_EXTERNAL_TYPE = String.class;

    private static final String CFG_HAS_ROW_ID = "has_row_id";

    private static final String CFG_PREPEND_TABLE_IDX_TO_ROWID = "prepend_table_index_to_row_id";

    private static final String CFG_SETTINGS_TAB = "settings";

    private static final String CFG_TABLE_SPEC_CONFIG = "table_spec_config" + SettingsModel.CFGKEY_INTERNAL;

    private static final DefaultTableSpecConfigSerializer TABLE_SPEC_SERIALIZER =
        new DefaultTableSpecConfigSerializer(PATH_LOADER, MOST_GENERIC_EXTERNAL_TYPE);

    @Override
    public void saveInDialog(
        final DefaultMultiTableReadConfig<TableManipulatorConfig, DefaultTableReadConfig<TableManipulatorConfig>> config,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        saveInModel(config, settings);
    }

    @Override
    public void loadInDialog(
        final DefaultMultiTableReadConfig<TableManipulatorConfig, DefaultTableReadConfig<TableManipulatorConfig>> config,
        final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        loadSettingsTabInDialog(config, getOrEmpty(settings, CFG_SETTINGS_TAB));
        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            try {
                config.setTableSpecConfig(
                    TABLE_SPEC_SERIALIZER.load(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG), null));
            } catch (InvalidSettingsException ex) { // NOSONAR, see below
                /* Can only happen in TableSpecConfig#load, since we checked #NodeSettingsRO#getNodeSettings(String)
                 * before. The framework takes care that #validate is called before load so we can assume that this
                 * exception does not occur.
                 */
            }
        } else {
            config.setTableSpecConfig(null);
        }
    }

    @Override
    public void saveInModel(
        final DefaultMultiTableReadConfig<TableManipulatorConfig, DefaultTableReadConfig<TableManipulatorConfig>> config,
        final NodeSettingsWO settings) {
        if (config.hasTableSpecConfig()) {
            config.getTableSpecConfig().save(settings.addNodeSettings(CFG_TABLE_SPEC_CONFIG));
        }
        saveSettingsTab(config, SettingsUtils.getOrAdd(settings, CFG_SETTINGS_TAB));
    }

    @Override
    public void loadInModel(
        final DefaultMultiTableReadConfig<TableManipulatorConfig, DefaultTableReadConfig<TableManipulatorConfig>> config,
        final NodeSettingsRO settings) throws InvalidSettingsException {
        loadSettingsTabInModel(config, settings.getNodeSettings(CFG_SETTINGS_TAB));
        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            config
                .setTableSpecConfig(TABLE_SPEC_SERIALIZER.load(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG), null));
        } else {
            config.setTableSpecConfig(null);
        }
    }

    @Override
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            TABLE_SPEC_SERIALIZER.validate(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG));
        }
        validateSettingsTab(settings.getNodeSettings(CFG_SETTINGS_TAB));
    }

    private static void loadSettingsTabInDialog(
        final DefaultMultiTableReadConfig<TableManipulatorConfig, DefaultTableReadConfig<TableManipulatorConfig>> config,
        final NodeSettingsRO settings) {
        final DefaultTableReadConfig<TableManipulatorConfig> tc = config.getTableReadConfig();
        tc.setUseRowIDIdx(settings.getBoolean(CFG_HAS_ROW_ID, true));
        tc.setPrependSourceIdxToRowId(settings.getBoolean(CFG_PREPEND_TABLE_IDX_TO_ROWID, false));
    }

    private static void loadSettingsTabInModel(
        final DefaultMultiTableReadConfig<TableManipulatorConfig, DefaultTableReadConfig<TableManipulatorConfig>> config,
        final NodeSettingsRO settings) throws InvalidSettingsException {
        final DefaultTableReadConfig<TableManipulatorConfig> tc = config.getTableReadConfig();
        tc.setUseRowIDIdx(settings.getBoolean(CFG_HAS_ROW_ID));
        tc.setPrependSourceIdxToRowId(settings.getBoolean(CFG_PREPEND_TABLE_IDX_TO_ROWID));
    }

    private static void saveSettingsTab(
        final DefaultMultiTableReadConfig<TableManipulatorConfig, DefaultTableReadConfig<TableManipulatorConfig>> config,
        final NodeSettingsWO settings) {
        final TableReadConfig<?> tc = config.getTableReadConfig();
        settings.addBoolean(CFG_HAS_ROW_ID, tc.useRowIDIdx());
        settings.addBoolean(CFG_PREPEND_TABLE_IDX_TO_ROWID, tc.prependSourceIdxToRowID());
    }

    static void validateSettingsTab(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_HAS_ROW_ID);
        settings.getBoolean(CFG_PREPEND_TABLE_IDX_TO_ROWID);

    }
}
