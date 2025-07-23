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
 *   Jan 29, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.createtablestructure;

import static org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.validateColumnName;

import java.util.Arrays;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationMessageBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationMessageBuilder.ColumnNameSettingContext;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * The model for the "Table Structure Creator" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class CreateTableStructureNodeModel extends WebUINodeModel<CreateTableStructureNodeSettings> {

    protected CreateTableStructureNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, CreateTableStructureNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final CreateTableStructureNodeSettings modelSettings) throws InvalidSettingsException {

        return new DataTableSpec[]{createSpec(modelSettings)};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final CreateTableStructureNodeSettings modelSettings) throws Exception {
        BufferedDataContainer cont = exec.createDataContainer(createSpec(modelSettings));
        cont.close();
        return new BufferedDataTable[]{cont.getTable()};
    }

    private static DataTableSpec createSpec(final CreateTableStructureNodeSettings modelSettings)
        throws InvalidSettingsException {
        final long distinct =
            Arrays.stream(modelSettings.m_columnSettings).map(setting -> setting.m_columnName).distinct().count();
        if (distinct != modelSettings.m_columnSettings.length) {
            throw new InvalidSettingsException("Duplicate column name found");
        }
        final var specs = Arrays.stream(modelSettings.m_columnSettings)
            .map(setting -> (new DataColumnSpecCreator(setting.m_columnName, setting.m_colType)).createSpec())
            .toArray(DataColumnSpec[]::new);
        return new DataTableSpec("Table Structure", specs);
    }

    @Override
    protected void validateSettings(final CreateTableStructureNodeSettings settings) throws InvalidSettingsException {
        if (settings.m_validateColumnNames) {
            for (var i = 0; i < settings.m_columnSettings.length; i++) {
                final var columnSetting = settings.m_columnSettings[i];
                final var invalidColNameToErrorMessage = new ColumnNameValidationMessageBuilder("column name") //
                    .withSpecificSettingContext(ColumnNameSettingContext.INSIDE_NON_COMPACT_ARRAY_LAYOUT) //
                    .withArrayItemIdentifier(String.format("Column %d", i + 1)).build();
                validateColumnName(columnSetting.m_columnName, invalidColNameToErrorMessage);
            }
        }
    }
}
