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
 *   Feb 11, 2026 (Thomas Reifenberger, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.io.filehandling.table.reader;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.NodeParametersMigration;

/**
 * Migrate from legacy settings to the new {@link KnimeTableReaderNodeParameters}.
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH, Germany
 */
class KnimeTableReaderMigration implements NodeParametersMigration<KnimeTableReaderNodeParameters> {

    private static final String SETTINGS_KEY = "settings";

    private static final String ADVANCED_SETTINGS_KEY = "advanced_settings";

    private static final String TABLE_SPEC_CONFIG_KEY = "table_spec_config_Internals";

    private static KnimeTableReaderNodeParameters load(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final var newSettings = new KnimeTableReaderNodeParameters();
        final var config = new KnimeTableMultiTableReadConfig();
        KnimeTableMultiTableReadConfigSerializer.INSTANCE.loadInModel(config, settings);
        final var tableReadConfig = config.getTableReadConfig();

        newSettings.m_knimeTableReaderParameters.m_multiFileSelectionParams.loadFromLegacySettings(settings);
        newSettings.m_knimeTableReaderParameters.m_skipFirstDataRowsParams.loadFromConfig(tableReadConfig);
        newSettings.m_knimeTableReaderParameters.m_maxNumberOfRowsParams.loadFromConfig(tableReadConfig);
        newSettings.m_knimeTableReaderParameters.m_ifSchemaChangesParams.loadFromConfig(config);
        newSettings.m_knimeTableReaderParameters.m_multiFileReaderParams.loadFromConfigAfter4_4(config);
        newSettings.m_knimeTableReaderParameters.m_useExistingRowIdParams.loadFromConfig(tableReadConfig);
        newSettings.m_knimeTableReaderParameters.m_prependTableIndexParams.loadFromConfig(tableReadConfig);
        newSettings.m_transformationParameters.loadFromTableSpecConfig(config.getTableSpecConfig());

        return newSettings;
    }

    @Override
    public List<ConfigMigration<KnimeTableReaderNodeParameters>> getConfigMigrations() {
        return List.of(//
            ConfigMigration.builder(KnimeTableReaderMigration::load) //
                .withMatcher(s -> s.containsKey(SETTINGS_KEY))
                .withDeprecatedConfigPath(SETTINGS_KEY)//
                .withDeprecatedConfigPath(ADVANCED_SETTINGS_KEY)//
                .withDeprecatedConfigPath(TABLE_SPEC_CONFIG_KEY) //
                .build());
    }
}

