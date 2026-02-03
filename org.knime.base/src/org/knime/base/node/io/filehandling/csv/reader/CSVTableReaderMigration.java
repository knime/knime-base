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
 *   Feb 3, 2026 (Thomas Reifenberger): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.NodeParametersMigration;

/**
 * Migrate from legacy settings to the new {@link CSVTableReaderNodeParameters}.
 *
 * @author Thomas Reifenberger
 */
class CSVTableReaderMigration implements NodeParametersMigration<CSVTableReaderNodeParameters> {

    private static final String SETTINGS_KEY = "settings";

    private static final String ADVANCED_SETTINGS_KEY = "advanced_settings";

    private static final String LIMIT_ROWS_KEY = "limit_rows";

    private static final String ENCODING_KEY = "encoding";

    private static final String TABLE_TRANSFORMATION_KEY = "table_spec_config_Internals";

    private static CSVTableReaderNodeParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
        var newSettings = new CSVTableReaderNodeParameters();
        var config = new CSVMultiTableReadConfig();
        CSVMultiTableReadConfigSerializer.INSTANCE.loadInModel(config, settings);

        newSettings.m_csvReaderParameters.m_multiFileSelectionParams.loadFromLegacySettings(settings);
        newSettings.m_csvReaderParameters.m_skipFirstDataRowsParams.loadFromConfig(config.getTableReadConfig());
        newSettings.m_csvReaderParameters.m_maxNumberOfRowsParams.loadFromConfig(config.getTableReadConfig());
        newSettings.m_csvReaderParameters.m_ifSchemaChangesParams.loadFromConfig(config);
        newSettings.m_csvReaderParameters.m_multiFileReaderParams.loadFromConfigAfter4_4(config);
        newSettings.m_csvReaderParameters.m_fileEncodingParams.loadFromConfig(config);
        newSettings.m_csvReaderParameters.m_skipFirstLinesParams.loadFromConfig(config);
        newSettings.m_csvReaderParameters.m_csvFormatParams.loadFromConfig(config);
        newSettings.m_csvReaderParameters.m_autoDetectParams.loadFromConfig(config);
        newSettings.m_csvReaderParameters.m_firstRowContainsColumnNamesParams.loadFromConfig(config);
        newSettings.m_csvReaderParameters.m_firstColumnContainsRowIdsParams.loadFromConfig(config);
        newSettings.m_csvReaderParameters.m_ifRowHasFewerColumnsParams.loadFromConfig(config);
        newSettings.m_csvReaderParameters.m_quotedStringsParams.loadFromConfig(config);
        newSettings.m_csvReaderParameters.m_limitScannedRowsParams.loadFromConfig(config);
        newSettings.m_csvReaderParameters.m_maxColumnsParams.loadFromConfig(config);
        newSettings.m_csvReaderParameters.m_limitMemoryParams.loadFromConfig(config);
        newSettings.m_csvReaderParameters.m_prependFileIndexParams.loadFromConfig(config);
        newSettings.m_transformationParameters.loadFromLegacySettings(settings);

        return newSettings;
    }

    @Override
    public List<ConfigMigration<CSVTableReaderNodeParameters>> getConfigMigrations() {
        return List.of(//
            ConfigMigration.builder(CSVTableReaderMigration::load) //
                .withMatcher(s -> s.containsKey(SETTINGS_KEY))
                .withDeprecatedConfigPath(SETTINGS_KEY)//
                .withDeprecatedConfigPath(ADVANCED_SETTINGS_KEY)//
                .withDeprecatedConfigPath(LIMIT_ROWS_KEY)//
                .withDeprecatedConfigPath(ENCODING_KEY)//
                .withDeprecatedConfigPath(TABLE_TRANSFORMATION_KEY) //
                .build());
    }
}
