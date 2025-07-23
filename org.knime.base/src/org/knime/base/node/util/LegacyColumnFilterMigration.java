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
 *   Jan 13, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.util;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.LegacyColumnFilterPersistor;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;

/**
 * Loads from legacy column filter settings. If the settings have to be saved to this legacy format as well, use a
 * {@link LegacyColumnFilterPersistor} instead.
 *
 * @noreference using this migration changes a behavior which users might rely on.
 * @deprecated
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.6
 */
@SuppressWarnings("restriction")
@Deprecated
public abstract class LegacyColumnFilterMigration implements NodeParametersMigration<ColumnFilter> {

    private final String m_configKey;

    /**
     * @param configKey the root config key to load from.
     */
    protected LegacyColumnFilterMigration(final String configKey) {
        m_configKey = configKey;
    }

    private ColumnFilter loadLegacy(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
        return LegacyColumnFilterPersistor.load(nodeSettings, m_configKey);
    }

    private boolean matchesLegacy(final NodeSettingsRO settings) {
        return settings.containsKey(m_configKey);
    }

    @Override
    public List<ConfigMigration<ColumnFilter>> getConfigMigrations() {
        final var configsMigrationBuilder = ConfigMigration.builder(this::loadLegacy).withMatcher(this::matchesLegacy);
        for (var configPath : LegacyColumnFilterPersistor.getConfigPaths(m_configKey)) {
            configsMigrationBuilder.withDeprecatedConfigPath(configPath);
        }
        return List.of(configsMigrationBuilder.build());
    }
}
