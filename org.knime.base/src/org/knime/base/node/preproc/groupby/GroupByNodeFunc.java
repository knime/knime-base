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
 *   Oct 17, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.groupby;

import java.util.List;
import java.util.Map;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.func.AbstractNodeFunc;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.port.PortObjectSpec;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
public final class GroupByNodeFunc extends AbstractNodeFunc {

    private static final Map<String, String> METHOD_IDS = Map.of(//
        "mean", "Mean_V4.6", //
        "count", "Count", //
        "first", "First", //
        "last", "Last", //
        "list", "List", //
        "median", "Median_V3.4"//
    );

    private static final NodeFuncApi API = NodeFuncApi.builder("group_by")//
        .withDescription("""
                Groups df by the group_column and appends a new column with the aggregation function
                applied to the aggregation_column.
                Valid values for aggregation are ['first', 'last', 'mean', 'median', 'list', 'count'].
                """)//
        .withInputTable("df", "The table to aggregate")//
        .withStringArgument("group_column", "The column to group by")//
        .withStringArgument("aggregation_column", "The column to aggregate")//
        .withStringArgument("aggregation",
            "The aggregation method. Valid values are ['first', 'last', 'mean', 'median', 'list', 'count'].")//
        .withOutputTable("output", "The grouped and aggregated table")//
        .build();

    /**
     * Constructor used by the framework to instantiate the NodeFunc.
     */
    public GroupByNodeFunc() {
        super(API, GroupByNodeFactory.class.getName());
    }

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs, final NodeSettingsWO settings)
        throws InvalidSettingsException {
        var nodeModel = new GroupByNodeModel();
        nodeModel.saveSettingsTo(settings);
        var groupColumn = arguments.getString("group_column");
        var aggregationColumn = arguments.getString("aggregation_column");
        var aggregationMethod = arguments.getString("aggregation");

        var tableSpec = (DataTableSpec)inputSpecs[0];
        var groupColumnSettings = GroupByNodeModel.createGroupByColsSettings();
        groupColumnSettings.setNewValues(//
            List.of(groupColumn), //
            tableSpec.stream()//
                .map(DataColumnSpec::getName)//
                .filter(n -> !n.equals(groupColumn))//
                .toList(), //
            false);
        groupColumnSettings.saveSettingsTo(settings);

        var aggregator = getColumnAggregator(tableSpec.getColumnSpec(aggregationColumn), aggregationMethod);
        ColumnAggregator.saveColumnAggregators(settings, List.of(aggregator));
    }

    private static String methodToId(final String method) throws InvalidSettingsException {
        var id = METHOD_IDS.get(method);
        if (id == null) {
            throw new InvalidSettingsException("The method %s is unknown.".formatted(method));
        }
        return id;
    }

    private static ColumnAggregator getColumnAggregator(final DataColumnSpec aggregationColumn,
        final String aggregationMethod) throws InvalidSettingsException {
        var id = methodToId(aggregationMethod);
        var aggregationOperator = AggregationMethods.getMethod4Id(id);
        return new ColumnAggregator(aggregationColumn, aggregationOperator);
    }

}
