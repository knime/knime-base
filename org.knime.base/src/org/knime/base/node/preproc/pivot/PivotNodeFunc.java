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
 *   Nov 12, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.pivot;

import org.knime.base.node.preproc.groupby.GroupByNodeFunc;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.func.ArgumentDefinition.PrimitiveArgumentType;
import org.knime.core.node.func.EnumArgumentType;
import org.knime.core.node.func.ListArgumentType;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.func.SimpleNodeFunc;
import org.knime.core.node.func.StructArgumentType;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class PivotNodeFunc implements SimpleNodeFunc {

    private static final String AGGREGATIONS = "aggregations";

    private static final String PIVOT_COLUMNS = "pivot_columns";

    private static final String GROUP_COLUMNS = "group_columns";

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var tableSpec = (DataTableSpec)inputSpecs[0];
        if (tableSpec == null) {
            throw new InvalidSettingsException("Can't configure Pivot node without the input table spec.");
        }

        // the Pivot node is an extension of the GroupBy node
        new GroupByNodeFunc().saveSettings(arguments, inputSpecs, settings);

        var pivotColumns = arguments.getStringArray(PIVOT_COLUMNS);
        for (var column : pivotColumns) {
            CheckUtils.checkSetting(tableSpec.containsName(column),
                "The specified pivot column '%s' does not exist in the input table.", column);
        }
        var pivotColumnsModel = new SettingsModelFilterString(Pivot2NodeModel.CFG_PIVOT_COLUMNS);
        pivotColumnsModel.setIncludeList(pivotColumns);
        pivotColumnsModel.saveSettingsTo(settings);
        Pivot2NodeModel.createSettingsColNameOption().saveSettingsTo(settings);
        Pivot2NodeModel.createSettingsIgnoreDomain().saveSettingsTo(settings);
        Pivot2NodeModel.createSettingsLexicographical().saveSettingsTo(settings);
        Pivot2NodeModel.createSettingsMissingValues().saveSettingsTo(settings);
        Pivot2NodeModel.createSettingsTotal().saveSettingsTo(settings);
    }

    @Override
    public NodeFuncApi getApi() {
        return NodeFuncApi.builder("pivot")//
            .withDescription(
                """
                Extension of the group_by operation that also pivots the table for selected columns.
                This node DOES NOT need a preceding group_by to do the grouping
                because as an extension of group_by it does the grouping and aggregations itself.
                """)
            .withInputTable("table", "The table to pivot.")//
            .withOutputTable("pivot_table", """
                    This table has one row per unique combination of the group column values
                    and one column per unique combination of pivot column values and the specified aggregations.
                    The naming scheme of the columns is pivot1_pivot2+Aggregation(column).
                    Example: For configuration group_columns=["group"], pivot_columns=["pivot1", "pivot2"]
                    with unique values pivot1=["foo", "bar"], pivot2=["baz"]
                    and aggregation {"column": "age", "method": "Mean"}.
                    Output columns: ["group", "foo_baz+Mean(age)", "bar_baz+Mean(age)"]
                     """)//
            .withOutputTable("group_totals", "A table containing the group totals ignoring the pivot columns.")
            .withOutputTable("pivot_totals",
                "A table containing the aggregated values for the pivot columns, ignoring the groups.")
            .withArgument(GROUP_COLUMNS, "The columns to group by.",
                ListArgumentType.create(PrimitiveArgumentType.STRING, false))//
            .withArgument(PIVOT_COLUMNS, "The columns to pivot by.",
                ListArgumentType.create(PrimitiveArgumentType.STRING, false))//
            .withArgument(AGGREGATIONS,
                """
                        A list of aggregations to perform.
                        Each aggregation consists of a column being aggregated and a method of aggregation.
                        Example: {"column": "foo", "method": "First"}
                        Valid aggregation methods are %s."""
                    .formatted(EnumArgumentType.createValuesString(GroupByNodeFunc.Aggregation.class)),
                ListArgumentType.create(StructArgumentType.builder()
                    .withProperty("column", "The column to aggregate.", PrimitiveArgumentType.STRING)
                    .withProperty("method", "The aggregation method to use.",
                        EnumArgumentType.create(GroupByNodeFunc.Aggregation.class))
                    .build(), false))//
            .build();
    }

    @Override
    public Class<? extends NodeFactory<?>> getNodeFactoryClass() {
        return Pivot2NodeFactory.class;
    }

}
