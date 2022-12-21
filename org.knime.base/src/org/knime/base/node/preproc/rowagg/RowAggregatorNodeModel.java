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
 *   19 Dec 2022 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.rowagg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.GlobalSettings.AggregationContext;
import org.knime.base.data.aggregation.GlobalSettings.GlobalSettingsBuilder;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.general.MaxOperator;
import org.knime.base.data.aggregation.general.MinOperator;
import org.knime.base.node.preproc.groupby.BigGroupByTable;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.base.node.preproc.groupby.GroupByNodeModel;
import org.knime.base.node.preproc.groupby.GroupByTable;
import org.knime.base.node.preproc.groupby.MemoryGroupByTable;
import org.knime.base.node.preproc.rowagg.aggregation.Average;
import org.knime.base.node.preproc.rowagg.aggregation.DataValueAggregate;
import org.knime.base.node.preproc.rowagg.aggregation.Multiply;
import org.knime.base.node.preproc.rowagg.aggregation.Sum;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.impl.Schema;
import org.knime.core.webui.node.dialog.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.dialog.impl.WebUINodeModel;

/**
 * A simplified {@link GroupByNodeModel} to aggregate numeric columns ("frequency attributes") of an input table,
 * optionally grouped by a single column ("class attribute"), with a single aggregation method.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class RowAggregatorNodeModel extends WebUINodeModel<RowAggregatorSettings> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RowAggregatorNodeModel.class);

    // The JSON Forms represent the "None" column (i.e. no column selected) with "<none>"
    private static final String NONE_COLUMN_PLACEHOLDER = "<none>";
    // The "empty string" column name results from our choices provider when the data table spec is null
    // (we return an empty string array, since the API does not like a "null" array)
    /** Values meaning "no column selected" which are potentially returned by the settings
     * (through the JSON Forms API) */
    private static final Set<String> NOTHING_SELECTION = Set.of(NONE_COLUMN_PLACEHOLDER, "");


    /** Settings error message that all aggregation functions except "count" need at least one frequency column. */
    private static final String MISSING_FREQUENCY_COLUMNS = "Choose at least one frequency column to aggregate or "
        + "choose the \"Occurrence count\" aggregation.";
    /** Settings error message that input is missing. */
    private static final String INPUT_MISSING = "No input specification available.";

    /** Warning message that input table has no columns. */
    private static final String INPUT_NO_COLUMNS = "Input table should contain at least one column.";
    /** Warning message that no category column has been selected. */
    private static final String NO_CATEGORY_COLUMN_SELECTED = "No category column selected. Aggregate complete table.";

    /** Instructs *GroupByTables to enable or disable hiliting. */
    private static final boolean ENABLE_HILITE = false;
    /** Instructs *GroupByTables to retain the original row order. */
    private static final boolean RETAIN_ORDER = false;

    /** The original column name can be retained since the node only supports using each column once. */
    private static final ColumnNamePolicy COL_NAME_POLICY = ColumnNamePolicy.KEEP_ORIGINAL_NAME;
    /** The column name for the COUNT aggregation method which does not use an input column to derive a name from. */
    private static final String COUNT_COLUMN_NAME = "OCCURRENCE_COUNT";

    /** How operators other than {@code count} handle missing cells. */
    private static final OperatorColumnSettings OPERATOR_MISSING_HANDLING = OperatorColumnSettings.DEFAULT_EXCL_MISSING;

    @FunctionalInterface
    private interface TriFunction<T, U, V, R> {
        R apply(final T t, final U u, final V v);
    }

    enum AggregationFunction {
        @Schema(title = "Occurrence count")
        COUNT(null, null),
        @Schema(title = "Sum")
        SUM(
            (gs, os) -> DataValueAggregate.create()
                .withOperatorInfo("SumNumeric_1.0", "Sum",
                    "Calculates the sum per group, ignoring missing cells in the input. "
                    + "The operator returns a missing cell if the result value exceeds the "
                    + "limits of the column's data type (numeric overflow).")
                // Use double since int and long are both compatible with it
                .withSupportedClass(DoubleValue.class)
                .withAggregate(Sum::new)
                .build(gs, os),
            // weighted aggregate
            (weight, gs, os) -> DataValueAggregate.create()
                .withOperatorInfo("WeightedSumNumeric_1.0", "Weighted Sum",
                    "Calculates the weighted sum per group. "
                    + "The operator returns a missing cell if the result value exceeds the "
                    + "limits of the column's data type (numeric overflow) or any of the data or weight cells are "
                    + "missing.")
                .withSupportedClass(DoubleValue.class)
                .withAggregate(Sum::new)
                .withWeighting(weight, Multiply::new)
                .build(gs, os)
        ),
        @Schema(title = "Average")
        AVERAGE(
            (gs, os) -> DataValueAggregate.create()
                .withOperatorInfo("AverageNumeric_1.0", "Average",
                    "Calculates the average (arithmetic mean) per group, ignoring missing cells in the input.")
                // Use double since int and long are both compatible with it
                .withSupportedClass(DoubleValue.class)
                .withAggregate(Average::new)
                .build(gs, os),
            // weighted aggregate
            (weight, gs, os) -> DataValueAggregate.create()
                .withOperatorInfo("WeightedAverageNumeric_1.0", "Weighted Average",
                    "Calculates the weighted average per group. If any of the data or weight cells are missing, "
                    + "the result is not added to the aggregate.")
                .withSupportedClass(DoubleValue.class)
                .withAggregate(Average::new)
                .withWeighting(weight, Multiply::new)
                .build(gs, os)
        ),
        @Schema(title = "Minimum")
        MIN(MinOperator::new, null),
        @Schema(title = "Maximum")
        MAX(MaxOperator::new, null);

        private BiFunction<GlobalSettings, OperatorColumnSettings, AggregationOperator> m_unweighted;
        private TriFunction<String, GlobalSettings, OperatorColumnSettings, AggregationOperator>  m_weighted;

        /**
         * Constructor.
         * @param unweighted function for an unweighted aggregation operator
         * @param weighted function for a weighted aggregation operator using the given column name
         */
        AggregationFunction(final BiFunction<GlobalSettings, OperatorColumnSettings, AggregationOperator> unweighted,
            final TriFunction<String, GlobalSettings, OperatorColumnSettings, AggregationOperator> weighted) {
            m_unweighted = unweighted;
            m_weighted = weighted;
        }

        AggregationOperator getOperator(final String weightColumnName, final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
            if (m_weighted == null && m_unweighted == null) {
                // e.g. COUNT
                throw new IllegalStateException(String.format("Aggregation method \"%s\" does not support getting an "
                    + "operator.", this));
            }
            if (weightColumnName != null) {
                if (m_weighted == null) {
                    throw new UnsupportedOperationException(
                        String.format("Aggregation method \"%s\" does not support weight column.", this));
                }
                return m_weighted.apply(weightColumnName, globalSettings, opColSettings);
            }
            if (m_unweighted == null) {
                throw new UnsupportedOperationException(
                    String.format("Aggregation method \"%s\" does not support unweighted aggregation and no weight "
                        + "column was given.", this));
            }
            return m_unweighted.apply(globalSettings, opColSettings);
        }

        boolean supportsWeightColumn() {
            return m_weighted != null;
        }
    }

    static final class RowAggregator {

        /** The function to use for all aggregated columns. */
        private AggregationFunction m_agg;
        /** The (optional) column to group by. */
        private String m_groupByColumn;
        /** The aggregated columns, null if counting rows. */
        private String[] m_aggregatedColumns;
        /** The optional weight column if supported by the aggregation function. */
        private String m_weightColumn;

        RowAggregator(final AggregationFunction agg, final String groupByColumn, final String[] aggregatedColumns,
            final String weightColumn) {
            m_agg = Objects.requireNonNull(agg);
            m_groupByColumn = groupByColumn;
            // agg cols null <=> COUNT
            if (m_agg == AggregationFunction.COUNT) {
                CheckUtils.checkArgument(aggregatedColumns == null,
                        "Cannot aggregate columns with \"COUNT\" aggregation function.");
            } else {
                CheckUtils.checkArgument(aggregatedColumns != null && aggregatedColumns.length > 0,
                        "All non-count aggregation functions need at least one column to aggregate.");
            }
            m_aggregatedColumns = aggregatedColumns;
            CheckUtils.checkArgument(weightColumn == null || agg.supportsWeightColumn(), "Aggregation function does not"
                + " support weighted aggregation, but weight column \"%s\" was given.", weightColumn);
            m_weightColumn = weightColumn;
        }

        Optional<String> getGroupByColumn() {
            final var col = m_groupByColumn;
            if (col == null) {
                return Optional.empty();
            }
            return Optional.of(col);
        }

        ColumnAggregator[] getAggregators(final GlobalSettings globalSettings,
            final Function<String, DataColumnSpec> colSpecs) {
            if (m_aggregatedColumns == null) {
                // e.g. for COUNT
                return new ColumnAggregator[0];
            }
            final var op = m_agg.getOperator(m_weightColumn, globalSettings, OPERATOR_MISSING_HANDLING);
            return Arrays.stream(m_aggregatedColumns)
                    .map(colSpecs)
                    .map(cs -> new ColumnAggregator(cs, op))
                    .collect(Collectors.toList()).toArray(ColumnAggregator[]::new);
        }
    }

    RowAggregatorNodeModel(final WebUINodeConfiguration config) {
        super(config, RowAggregatorSettings.class);
    }

    @Override
    protected void validateSettings(final RowAggregatorSettings settings) throws InvalidSettingsException {
        if (settings.m_aggregationMethod != AggregationFunction.COUNT && settings.m_frequencyColumns != null) {
            // check that we did not accidentally end up with the placeholder value(s) in our frequency column list
            // e.g. when no input is connected and a placeholder column is included as a choice and the node is saved
            CheckUtils.checkSetting(Arrays.stream(settings.m_frequencyColumns)
                .filter(NOTHING_SELECTION::contains)
                .findAny()
                .isEmpty(), "None/empty placeholder column present in frequency columns list.");
        }
    }

    /** Create a builder with common config of #configure and #execute. */
    private static GlobalSettingsBuilder createGlobalSettingsBuilder() {
        return GlobalSettings.builder()
                .setValueDelimiter(GlobalSettings.STANDARD_DELIMITER)
                .setAggregationContext(AggregationContext.ROW_AGGREGATION);
    }

    /** Get the aggregated columns value to pass to the row aggregator. */
    private static Optional<String[]> getEffectiveAggregatedColumns(final RowAggregatorSettings settings) {
        final var agg = settings.m_aggregationMethod;
        if (agg == AggregationFunction.COUNT || settings.m_frequencyColumns.length == 0) {
            // UI could still report an old value that is not used by COUNT (and is disabled/greyed-out in the UI)
            return Optional.empty();
        } else {
            return Optional.of(settings.m_frequencyColumns);
        }
    }

    /** Get the group by column value to pass to the row aggregator. */
    private static Optional<String> getEffectiveGroupByColumn(final RowAggregatorSettings settings) {
        // the field might contain a placeholder value from the forms
        return settings.m_categoryColumn == null
                || NOTHING_SELECTION.contains(settings.m_categoryColumn) ? Optional.empty()
                    : Optional.of(settings.m_categoryColumn);
    }

    /** Get the weight column value to pass to the row aggregator. */
    private static Optional<String> getEffectiveWeightColumn(final RowAggregatorSettings settings) {
        final var weighted = settings.m_weightColumn != null
                && !NONE_COLUMN_PLACEHOLDER.equals(settings.m_weightColumn);
        // when the user previously selected a weight column and then changes to an aggregation method that does
        // not support a weight column (min, max, ...), the input is disabled but still reports a value...
        return weighted && settings.m_aggregationMethod.supportsWeightColumn() ?
            Optional.of(settings.m_weightColumn) : Optional.empty();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final RowAggregatorSettings settings)
        throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(inSpecs, INPUT_MISSING);
        CheckUtils.checkSettingNotNull(inSpecs[0], INPUT_MISSING);

        final var origSpec = (DataTableSpec) inSpecs[0];
        if (origSpec.getNumColumns() < 1) {
            setWarningMessage(INPUT_NO_COLUMNS);
        }

        final var agg = settings.m_aggregationMethod;
        if (agg != AggregationFunction.COUNT) {
            // all other aggregations than COUNT currently need at least one frequency column

            // no column set
            CheckUtils.checkSettingNotNull(settings.m_frequencyColumns, MISSING_FREQUENCY_COLUMNS);
            CheckUtils.checkSetting(settings.m_frequencyColumns.length > 0, MISSING_FREQUENCY_COLUMNS);
            // only placeholder set, behave as if missing columns
            CheckUtils.checkSetting(!(settings.m_frequencyColumns.length == 1
                    && NOTHING_SELECTION.contains(settings.m_frequencyColumns[0])), MISSING_FREQUENCY_COLUMNS);
        }

        final var aggregatedColumns = getEffectiveAggregatedColumns(settings);

        final var groupByColumn = getEffectiveGroupByColumn(settings);

        if (groupByColumn.isEmpty() && !settings.m_grandTotals) {
            // warn if user has not selected the checkbox to append totals, but is missing category column
            // which leads to same outcome (a totals row being appended)
            setWarningMessage(NO_CATEGORY_COLUMN_SELECTED);
        }

        final var weightColumn = getEffectiveWeightColumn(settings);
        final var rowAgg = new RowAggregator(agg, groupByColumn.orElse(null), aggregatedColumns.orElse(null),
            weightColumn.orElse(null));

        final var groupByColAsList = rowAgg.getGroupByColumn().stream().collect(Collectors.toList());
        final var gs = createGlobalSettingsBuilder()
                .setGroupColNames(groupByColAsList)
                .setDataTableSpec(origSpec).build();
        // we have at least one column to aggregate, except for COUNT where we just count rows
        final var aggregators = rowAgg.getAggregators(gs, origSpec::getColumnSpec);

        final var countColumnName = settings.m_aggregationMethod == AggregationFunction.COUNT
                ? COUNT_COLUMN_NAME : null;

        final var groupedSpec = GroupByTable.createGroupByTableSpec(origSpec, groupByColAsList, aggregators,
            countColumnName, COL_NAME_POLICY);

        final var noTotals = (groupByColumn.isEmpty() || !settings.m_grandTotals);

        return new PortObjectSpec[] {
            groupedSpec,
            noTotals ? InactiveBranchPortObjectSpec.INSTANCE
                : GroupByTable.createGroupByTableSpec(origSpec, Collections.emptyList(), aggregators, countColumnName,
                    COL_NAME_POLICY)
        };
    }

    @Override
    protected PortObject[] execute(final PortObject[] inPortObjects, final ExecutionContext exec,
        final RowAggregatorSettings settings) throws Exception {
        if (inPortObjects == null || inPortObjects[0] == null) {
            throw new Exception("No input available!");
        }

        final var table = (BufferedDataTable) inPortObjects[0];
        if (table.size() < 1) {
            setWarningMessage("Input table empty.");
        }

        final var inSpec = table.getDataTableSpec();
        final var gsb = createGlobalSettingsBuilder()
                .setFileStoreFactory(FileStoreFactory.createWorkflowFileStoreFactory(exec))
                .setDataTableSpec(inSpec)
                .setNoOfRows(table.size());

        final var aggregatedColumns = getEffectiveAggregatedColumns(settings);
        final var weightColumn = getEffectiveWeightColumn(settings);
        final var groupByColumn = getEffectiveGroupByColumn(settings);
        final var agg = settings.m_aggregationMethod;

        final var rowAgg = new RowAggregator(agg, groupByColumn.orElse(null), aggregatedColumns.orElse(null),
            weightColumn.orElse(null));

        final var groupByColAsList = rowAgg.getGroupByColumn().stream().collect(Collectors.toList());
        final var gs = gsb.setGroupColNames(groupByColAsList).build();
        final var aggregators = rowAgg.getAggregators(gs, inSpec::getColumnSpec);
        final var countColumnName = agg == AggregationFunction.COUNT ? COUNT_COLUMN_NAME : null;

        final BufferedDataTable groupedAggregates;
        if (!groupByColumn.isEmpty()) {
            exec.setMessage("Calculating group-by aggregate");
            final var groupedResult = new BigGroupByTable(exec, table,
                groupByColAsList,
                aggregators,
                countColumnName,
                gs,
                ENABLE_HILITE,
                COL_NAME_POLICY,
                RETAIN_ORDER);
            warnSkippedGroups(groupedResult);

            groupedAggregates = groupedResult.getBufferedTable();
            if (!settings.m_grandTotals) {
                return new PortObject[] { groupedAggregates, InactiveBranchPortObject.INSTANCE };
            }
        } else {
            // let in-memory aggregate handle the grand total if no class column is selected
            groupedAggregates = null;
        }

        final var totalsResultSpec = GroupByTable.createGroupByTableSpec(inSpec, Collections.emptyList(), aggregators,
            countColumnName, COL_NAME_POLICY);

        final var totalGs = gsb
                .setGroupColNames(new ArrayList<>())
                .setDataTableSpec(totalsResultSpec)
                .build();

        exec.setMessage("Calculating \"grand total\" aggregate");
        final var totalAggregates = new MemoryGroupByTable(exec, table,
            new ArrayList<>(),
            aggregators,
            countColumnName,
            totalGs,
            ENABLE_HILITE,
            COL_NAME_POLICY,
            RETAIN_ORDER);
        warnSkippedGroups(totalAggregates);

        final var bufTotals = totalAggregates.getBufferedTable();

        exec.setMessage("Producing output table");
        final var totalResult = getSingleRowTotalsResult(exec, totalsResultSpec, countColumnName, bufTotals);

        // grand totals get put into first output table, when no class column is selected
        final var first = groupedAggregates != null ? groupedAggregates : totalResult;
        final var second = groupedAggregates != null ? totalResult : InactiveBranchPortObject.INSTANCE;
        return new PortObject[] {
            first, second
        };
    }

    private static BufferedDataTable getSingleRowTotalsResult(final ExecutionContext exec,
        final DataTableSpec resultSpec, final String countColumnName,
        final BufferedDataTable bufTotals) {

        final var totalSize = bufTotals.size();
        if (totalSize > 1) {
            throw new IllegalStateException("More than one row in totals table.");
        }

        final var totalOut = exec.createDataContainer(resultSpec);

        final List<DataCell> cells = new ArrayList<>();
        if (totalSize == 0 && countColumnName != null) {
            // empty input: result of COUNT* is somewhat special
            cells.add(new LongCell(0));
        } else if (totalSize == 0) {
            // empty input: fill agg columns with missing cells
            totalOut.addRowToTable(new DefaultRow(RowKey.createRowKey(0L),
                Arrays.stream(resultSpec.getColumnNames())
                        .map(c -> DataType.getMissingCell()).collect(Collectors.toList())));
            totalOut.close();
            return totalOut.getTable();
        } else {
            try (final var it = bufTotals.iterator()) {
                final var totalRow = it.next();
                totalRow.forEach(cells::add);
            }
        }
        totalOut.addRowToTable(new DefaultRow(RowKey.createRowKey(0L), cells));
        totalOut.close();
        return totalOut.getTable();
    }

    private void warnSkippedGroups(final GroupByTable resultTable) {
        final String warningMsg = resultTable.getSkippedGroupsMessage(3, 3);
        if (warningMsg != null) {
            setWarningMessage(warningMsg);
            LOGGER.info(resultTable.getSkippedGroupsMessage(Integer.MAX_VALUE, Integer.MAX_VALUE));
        }
    }

}
