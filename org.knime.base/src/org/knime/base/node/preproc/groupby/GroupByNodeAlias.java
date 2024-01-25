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
 *   Jan 24, 2024 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.groupby;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.node.preproc.groupby.GroupByAliasSettings.Aggregation;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.alias.NodeAlias;
import org.knime.core.webui.node.alias.NodeAliasSpec;
import org.knime.core.webui.node.impl.PortDescription;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
@SuppressWarnings("restriction")
public final class GroupByNodeAlias implements NodeAlias<GroupByAliasSettings> {

    @Override
    public NodeAliasSpec getSpec() {
        return NodeAliasSpec.builder()//
            .withName("group_by")//
            .withDescription("""
                    Groups the input table by the groups and calculates the specified aggregations.
                    The naming of the output columns follows the scheme Aggregation(column).
                    Example: The mean of the column 'foo' would result in the column being named 'Mean(foo)'.
                    """)//
            .withInputPort(new PortDescription("Table", BufferedDataTable.TYPE, "The table to group and aggregate."))
            .withOutputPort(new PortDescription("output", BufferedDataTable.TYPE, "The grouped and aggregated table"))
            .build();
    }

    @Override
    public Class<?> getSettingsClass() {
        return GroupByAliasSettings.class;
    }

    @Override
    public Class<? extends NodeFactory<?>> getNodeFactoryClass() {
        return GroupByNodeFactory.class;
    }

    @Override
    public void saveSettings(final GroupByAliasSettings obj, final PortObjectSpec[] specs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var nodeModel = new GroupByNodeModel();
        nodeModel.saveSettingsTo(settings);

        var tableSpec = (DataTableSpec)specs[0];
        if (tableSpec == null) {
            throw new InvalidSettingsException("Can't configure GroupBy without the input table spec.");
        }
        saveGroupSettings(obj, settings, tableSpec);

        saveAggregationSettings(obj, settings, tableSpec);
    }

    private static void saveAggregationSettings(final GroupByAliasSettings arguments, final NodeSettingsWO settings,
        final DataTableSpec tableSpec) throws InvalidSettingsException {
        var aggregators = new ArrayList<ColumnAggregator>();
        for (var aggregation : arguments.m_aggregations) {
            aggregators.add(createAggregator(aggregation, tableSpec));
        }
        ColumnAggregator.saveColumnAggregators(settings, aggregators);
    }

    private static ColumnAggregator createAggregator(final Aggregation aggregation, final DataTableSpec tableSpec)
        throws InvalidSettingsException {
        var column = aggregation.m_column.getSelected();
        var columnSpec = tableSpec.getColumnSpec(column);
        CheckUtils.checkSetting(columnSpec != null, "The aggregation column '%s' does not exist in the input table.",
            column);
        var method = aggregation.m_aggregationMethod;
        return method.createOperator(columnSpec);
    }

    private static void saveGroupSettings(final GroupByAliasSettings arguments, final NodeSettingsWO settings,
        final DataTableSpec tableSpec) throws InvalidSettingsException {
        var groupColumnSettings = GroupByNodeModel.createGroupByColsSettings();
        var groupColumnFilter = arguments.m_groupColumns;
        var includes = Stream.of(groupColumnFilter.getSelected(tableSpec.getColumnNames(), tableSpec))//
            .collect(Collectors.toCollection(LinkedHashSet::new));

        for (var groupCol : groupColumnFilter.getSelected(tableSpec.getColumnNames(), tableSpec)) {
            CheckUtils.checkSetting(tableSpec.containsName(groupCol),
                "The group column '%s' does not exist in the input table.", groupCol);
        }
        var excludes = tableSpec.stream()//
            .map(DataColumnSpec::getName)//
            .filter(not(includes::contains))//
            .toList();

        groupColumnSettings.setNewValues(includes, excludes, false);
        groupColumnSettings.saveSettingsTo(settings);
    }

}
