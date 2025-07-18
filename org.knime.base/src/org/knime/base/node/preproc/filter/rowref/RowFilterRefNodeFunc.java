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
 *   Nov 11, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.rowref;

import static org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice.ROW_ID;

import org.knime.base.node.preproc.filter.rowref.RowFilterRefNodeSettings.IncludeOrExcludeRows;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.func.ArgumentDefinition.PrimitiveArgumentType;
import org.knime.core.node.func.NodeFunc;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;

/**
 * NodeFunc that makes the Reference Row Filter available to K-AI's build mode.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
@SuppressWarnings("restriction")
public final class RowFilterRefNodeFunc implements NodeFunc {

    private static final String INCLUDE = "include";

    private static final String REF_COLUMN = "ref_column";

    private static final String USE_ROW_ID_REF_COLUMN = "use_row_id_ref_column";

    private static final String DATA_COLUMN = "data_column";

    private static final String USE_ROW_ID_DATA_COLUMN = "use_row_id_data_column";

    private static final String REF_TABLE = "ref_table";

    private static final String DATA_TABLE = "data_table";

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var refRowFilterSettings = new RowFilterRefNodeSettings();
        var dataTable = (DataTableSpec)inputSpecs[0];
        var refTable = (DataTableSpec)inputSpecs[1];
        refRowFilterSettings.m_dataColumn = checkContained(//
            arguments.getBoolean(USE_ROW_ID_DATA_COLUMN), //
            arguments.getString(DATA_COLUMN), //
            dataTable, //
            DATA_TABLE//
        );
        refRowFilterSettings.m_referenceColumn = checkContained(//
            arguments.getBoolean(USE_ROW_ID_REF_COLUMN), //
            arguments.getString(REF_COLUMN), //
            refTable, //
            REF_TABLE //
        );
        refRowFilterSettings.m_inexclude =
            arguments.getBoolean(INCLUDE) ? IncludeOrExcludeRows.INCLUDE : IncludeOrExcludeRows.EXCLUDE;
        NodeParametersUtil.saveSettings(RowFilterRefNodeSettings.class, refRowFilterSettings, settings);
    }

    private static StringOrEnum<RowIDChoice> checkContained(final boolean useRowId, final String column,
        final DataTableSpec tableSpec, final String tableName) throws InvalidSettingsException {
        if (useRowId) {
            return new StringOrEnum<>(ROW_ID);
        }
        if (tableSpec == null || tableSpec.containsName(column)) {
            return new StringOrEnum<>(column);
        }
        throw new InvalidSettingsException("The column '%s' does not exist in the %s.".formatted(column, tableName));
    }

    @Override
    public NodeFuncApi getApi() {
        return NodeFuncApi.builder("reference_row_filter")//
            .withDescription(
                "Filters rows from the data table by matching values in a column with values from a reference table.")//
            .withInputTable(DATA_TABLE, "The table to filter.")//
            .withInputTable(REF_TABLE,
                "The reference table that contains the rows to in- or exclude in the output table.")//
            .withArgument(INCLUDE, "Whether matched rows should be in- or excluded.", PrimitiveArgumentType.BOOLEAN)//
            .withArgument(USE_ROW_ID_DATA_COLUMN,
                "Whether RowID should be as the column in the data_table. If true, data_column is ignored.",
                PrimitiveArgumentType.BOOLEAN)//
            .withArgument(DATA_COLUMN, "The column in the data_table to filter by.", PrimitiveArgumentType.STRING)//
            .withArgument(USE_ROW_ID_REF_COLUMN,
                "Whether RowID should be as the column in the ref_table. If true, ref_column is ignored.",
                PrimitiveArgumentType.BOOLEAN)//
            .withArgument(REF_COLUMN, "The column in the ref_table to match.", PrimitiveArgumentType.STRING)
            .withOutputTable("filtered_table", "The filtered table.")//
            .build();
    }

    @Override
    public String getNodeFactoryClassName() {
        return RowFilterRefNodeFactory.class.getName();
    }

}
