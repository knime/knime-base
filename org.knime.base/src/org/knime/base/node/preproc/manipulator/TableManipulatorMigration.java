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
 *   Mar 4, 2026 (Thomas Reifenberger, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.manipulator;

import java.util.List;

import org.knime.base.node.preproc.manipulator.TableManipulatorParameters.ColumnFilterModeOption;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.NodeParametersMigration;

import static org.knime.base.node.preproc.manipulator.TableManipulatorConfigSerializer.CFG_TABLE_SPEC_CONFIG;
import static org.knime.base.node.preproc.manipulator.TableManipulatorParameters.CFG_COLUMN_FILTER_MODE;

/**
 * Migrate from legacy settings to the new {@link TableManipulatorNodeParameters}.
 * <p>
 * The {@link TableManipulatorParameters} (RowID handling etc.) are designed in a backwards-compatible way and do not
 * need special migration. Only the {@link TableManipulatorTransformationParameters} are migrated here by loading the
 * legacy {@code table_spec_config_Internals} via the existing {@link TableManipulatorConfigSerializer}.
 * </p>
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 */
final class TableManipulatorMigration {

    private TableManipulatorMigration() {
        // utility class
    }

    static final String CFG_TRANSFORMATION_PARAMETERS = "transformationParameters";

    static class ColumnFilterModeMigration implements NodeParametersMigration<ColumnFilterModeOption> {

        private static ColumnFilterModeOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var config = new TableManipulatorMultiTableReadConfig();
            TableManipulatorConfigSerializer.INSTANCE.loadInModel(config, settings);
            var tableSpecConfig = config.getTableSpecConfig();
            if (tableSpecConfig == null) {
                return ColumnFilterModeOption.UNION;
            }
            var columnFilterMode = config.getTableSpecConfig().getTableTransformation().getColumnFilterMode();
            return ColumnFilterModeOption.fromColumnFilterMode(columnFilterMode);
        }

        @Override
        public List<ConfigMigration<ColumnFilterModeOption>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(ColumnFilterModeMigration::load) //
                    .withMatcher(s -> !s.containsKey(CFG_COLUMN_FILTER_MODE))
                    .withDeprecatedConfigPath(CFG_TABLE_SPEC_CONFIG) //
                    .build());
        }

    }

    static class TransformationParametersMigration
        implements NodeParametersMigration<TableManipulatorTransformationParameters> {

        private static TableManipulatorTransformationParameters load(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            final var newParameters = new TableManipulatorTransformationParameters();
            final var config = new TableManipulatorMultiTableReadConfig();
            TableManipulatorConfigSerializer.INSTANCE.loadInModel(config, settings);

            newParameters.loadFromTableSpecConfig(config.getTableSpecConfig());
            return newParameters;
        }

        @Override
        public List<ConfigMigration<TableManipulatorTransformationParameters>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(TransformationParametersMigration::load) //
                    .withMatcher(s -> !s.containsKey(CFG_TRANSFORMATION_PARAMETERS))
                    .withDeprecatedConfigPath(CFG_TABLE_SPEC_CONFIG) //
                    .build());
        }
    }

}
