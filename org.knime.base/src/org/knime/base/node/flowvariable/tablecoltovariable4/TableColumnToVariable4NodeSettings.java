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
 *   Jan 31, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.flowvariable.tablecoltovariable4;

import java.util.List;

import org.knime.base.node.flowvariable.converter.celltovariable.CellToVariableConverterFactory;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;

/**
 * The settings for the "Table Column to Variable" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class TableColumnToVariable4NodeSettings implements NodeParameters {

    TableColumnToVariable4NodeSettings() {

    }

    TableColumnToVariable4NodeSettings(final NodeParametersInput context) {
        final var spec = context.getInTableSpec(0);
        if (spec.isPresent()) {
            this.m_column = spec.get().stream()//
                .filter(s -> CellToVariableConverterFactory.isSupported(s.getType()))//
                .map(DataColumnSpec::getName).findFirst().orElse(null);
        }
    }

    enum MissingOperation {
            @Label(value = "Ignore", description = "Rows with a missing value in the selected column will be skipped.")
            IGNORE, //
            @Label(value = "Fail",
                description = "The node execution will fail if a row contains a missing value in the selected column.")
            FAIL
    }

    @Widget(title = "Column name", description = "Name of the column for the values.")
    @ChoicesProvider(AllColumnsProvider.class)
    String m_column;

    @Widget(title = "If value in cell is missing", description = "Action to take if the value in cell is missing.")
    @ValueSwitchWidget
    @Migration(MissingOperationMigration.class)
    MissingOperation m_missingOperation = MissingOperation.IGNORE;

    static final class MissingOperationMigration implements NodeParametersMigration<MissingOperation> {

        private static final String KEY = "skip_missing";

        private static MissingOperation load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(KEY) ? MissingOperation.IGNORE : MissingOperation.FAIL;
        }

        @Override
        public List<ConfigMigration<MissingOperation>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(MissingOperationMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }
    }
}
