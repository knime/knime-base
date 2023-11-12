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

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
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
 * @since 5.2
 */
public final class GroupByNodeFunc implements SimpleNodeFunc {

    private static final String AGGREGATIONS = "aggregations";

    private static final String METHOD = "method";

    private static final String COLUMN = "column";

    private static final String GROUP_COLUMNS = "group_columns";

    /**
     * The supported aggregation methods.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    @SuppressWarnings("javadoc")
    public enum Aggregation {
            Mean("Mean_V4.6"), //
            Count("Count"), //
            First("First"), //
            Last("Last"), //
            List("List"), //
            Median("Median_V3.4"), //
            Sum("Sum_V2.5.2");

        private final String m_methodId;

        Aggregation(final String methodId) {
            m_methodId = methodId;
        }

        public static String valuesString() {
            return Stream.of(values())//
                .map(Enum::name)//
                .collect(joining(", ", "[", "]"));
        }

        public ColumnAggregator createOperator(final DataColumnSpec column) throws InvalidSettingsException {
            return new ColumnAggregator(column, getMethod());
        }

        public AggregationMethod getMethod() {
            return AggregationMethods.getMethod4Id(m_methodId);
        }

        public static Aggregation forName(final String name) throws InvalidSettingsException {
            try {
                return Aggregation.valueOf(name);
            } catch (IllegalArgumentException ex) {
                throw new InvalidSettingsException("The aggregation '%s' does not exist.".formatted(name));
            }
        }

    }

    @Override
    public Class<? extends NodeFactory<?>> getNodeFactoryClass() {
        return GroupByNodeFactory.class;
    }

    @Override
    public NodeFuncApi getApi() {
        return NodeFuncApi.builder("group_by")//
            .withDescription("""
                    Groups the input table by the %s and calculates the specified %s.
                    The naming of the output columns follows the scheme Aggregation(column).
                    Example: The mean of the column 'foo' would result in the column being named 'Mean(foo)'.
                    """.formatted(GROUP_COLUMNS, AGGREGATIONS))//
            .withInputTable("table", "The table to group and aggregate.")//
            .withArgument(GROUP_COLUMNS, "The columns to group by.",
                ListArgumentType.create(PrimitiveArgumentType.STRING, false))//
            .withArgument(AGGREGATIONS, """
                    A list of aggregations to perform.
                    Each aggregation consists of a column being aggregated and a method of aggregation.
                    Example: {"column": "foo", "method": "First"}
                    Valid aggregation methods are %s.
                            """.formatted(Aggregation.valuesString()), ListArgumentType.create(
                                StructArgumentType.builder()//
                .withProperty(COLUMN, "The column to aggregate.", PrimitiveArgumentType.STRING)//
                .withProperty(METHOD, "The method of aggregation.", EnumArgumentType.create(Aggregation.class))//
                .build(), false))
            .withOutputTable("output", "The grouped and aggregated table")//
            .build();
    }

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var nodeModel = new GroupByNodeModel();
        nodeModel.saveSettingsTo(settings);

        var tableSpec = (DataTableSpec)inputSpecs[0];
        if (tableSpec == null) {
            throw new InvalidSettingsException("Can't configure GroupBy without the input table spec.");
        }
        saveGroupSettings(arguments, settings, tableSpec);

        saveAggregationSettings(arguments, settings, tableSpec);
    }

    private static void saveAggregationSettings(final NodeSettingsRO arguments, final NodeSettingsWO settings,
        final DataTableSpec tableSpec) throws InvalidSettingsException {
        var aggregationsArg = arguments.getNodeSettings(AGGREGATIONS);
        var numAggregations = ListArgumentType.size(aggregationsArg);
        var aggregators = new ArrayList<ColumnAggregator>();

        for (int i = 0; i < numAggregations; i++) {
            var aggregationArg = aggregationsArg.getNodeSettings("" + i);
            var column = aggregationArg.getString(COLUMN);
            var columnSpec = tableSpec.getColumnSpec(column);
            CheckUtils.checkSetting(columnSpec != null,
                "The aggregation column '%s' does not exist in the input table.", column);
            var method = Aggregation.forName(aggregationArg.getString(METHOD));
            aggregators.add(method.createOperator(columnSpec));
        }

        ColumnAggregator.saveColumnAggregators(settings, aggregators);
    }

    private static void saveGroupSettings(final NodeSettingsRO arguments, final NodeSettingsWO settings,
        final DataTableSpec tableSpec) throws InvalidSettingsException {
        var groupColumnSettings = GroupByNodeModel.createGroupByColsSettings();
        var groupColumnsArg = arguments.getStringArray(GROUP_COLUMNS);

        Collection<String> includes = Stream.of(groupColumnsArg)//
            .collect(Collectors.toCollection(LinkedHashSet::new));

        for (var groupCol : groupColumnsArg) {
            CheckUtils.checkSetting(tableSpec.containsName(groupCol),
                "The group column '%s' does not exist in the input table.", groupCol);
        }
        var excludes = tableSpec.stream()//
            .map(DataColumnSpec::getName)//
            .filter(Predicate.not(includes::contains))//
            .toList();

        groupColumnSettings.setNewValues(includes, excludes, false);
        groupColumnSettings.saveSettingsTo(settings);
    }

}
