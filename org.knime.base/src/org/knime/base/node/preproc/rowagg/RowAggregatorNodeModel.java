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
import java.util.function.Predicate;
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
import org.knime.base.node.preproc.rowagg.aggregation.AverageNumeric;
import org.knime.base.node.preproc.rowagg.aggregation.DataValueAggregate;
import org.knime.base.node.preproc.rowagg.aggregation.MultiplyNumeric;
import org.knime.base.node.preproc.rowagg.aggregation.SumNumeric;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
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
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * A simplified {@link GroupByNodeModel} to aggregate numeric columns of an input table,
 * optionally grouped by a single column ("category attribute"), with a single aggregation method.
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


    /** Settings error message that all aggregation functions except "count" need at least one aggregation column. */
    private static final String MISSING_AGGREGATION_COLUMNS = "Choose at least one aggregation column to aggregate or "
        + "choose the \"Occurrence count\" aggregation function.";
    /** Settings error message that input is missing. */
    private static final String INPUT_MISSING = "No input specification available.";

    /** Warning message that input table has no columns. */
    private static final String INPUT_NO_COLUMNS = "Input table should contain at least one column.";

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

    /**
     * Determine whether the given column is an aggregatable column,
     * i.e. a column compatible with {@link DoubleValue}, {@link IntValue}, or {@link LongValue}.
     * @param column data column spec
     * @return {@code true} if the column can be aggregated, {@code false} otherwise
     */
    static boolean isAggregatableColumn(final DataColumnSpec column) {
        // In theory this should check the type of either (as configured):
        // - the aggregated column type if weight column is none or
        // - the result of the multiplication with the weight column if a weight column is selected.
        // However, at this point we don't know the selected weight column here (and vice-versa), so we do the
        // next-best thing. Note: This could filter too much, i.e. could remove columns that would be compatible
        // _after_ applying the weight to them, which is currently not the case, since Multiply supports the
        // same (input) types as Sum and Average.
        // If we add numeric types and change the Multiply/Sum/Average, we should revisit this filter here.
        final var t = column.getType();
        // COUNT, MIN, MAX trivially support all data types
        return SumNumeric.supportsDataType(t) && AverageNumeric.supportsDataType(t);
    }


    /**
     * Determine whether the given column can be a weight column,
     * i.e. a column compatible with {@link DoubleValue}, {@link IntValue}, or {@link LongValue}.
     * @param column data column spec
     * @return {@code true} if the column can be a weight column, {@code false} otherwise
     */
    static boolean isWeightColumn(final DataColumnSpec column) {
        // COUNT, MIN, MAX do not provide a weighted version
        final var w = column.getType();
        // Since this function is called by a ChoicesProvider which only checks one column,
        // we effectively have a hole in our types we need to check. But if this weight column alone is not
        // supported, it cannot be supported in conjunction with another column.
        final var l = DataType.getMissingCell().getType();
        return MultiplyNumeric.supportsDataTypes(l, w);
    }


    enum AggregationFunction {
        @Label("Occurrence count")
        COUNT(null, null),
        @Label("Sum")
        SUM(
            (gs, os) -> DataValueAggregate.create()
                .withOperatorInfo("SumNumeric_1.0", "Sum",
                    "Calculates the sum per group, ignoring missing cells in the input. "
                    + "The operator returns a missing cell if the result value exceeds the "
                    + "limits of the column's data type (numeric overflow).")
                // Use double since int and long are both compatible with it
                .withSupportedClass(DoubleValue.class)
                .withAggregate(SumNumeric::new)
                .build(gs, os),
            // weighted aggregate
            (weight, gs, os) -> DataValueAggregate.create()
                .withOperatorInfo("WeightedSumNumeric_1.0", "Weighted Sum",
                    "Calculates the weighted sum per group. "
                    + "The operator returns a missing cell if the result value exceeds the "
                    + "limits of the column's data type (numeric overflow) or any of the data or weight cells are "
                    + "missing.")
                .withSupportedClass(DoubleValue.class)
                .withAggregate(SumNumeric::new)
                .withWeighting(weight, MultiplyNumeric::new)
                .build(gs, os)
        ),
        @Label("Average")
        AVERAGE(
            (gs, os) -> DataValueAggregate.create()
                .withOperatorInfo("AverageNumeric_1.0", "Average",
                    "Calculates the average (arithmetic mean) per group, ignoring missing cells in the input.")
                // Use double since int and long are both compatible with it
                .withSupportedClass(DoubleValue.class)
                .withAggregate(AverageNumeric::new)
                .build(gs, os),
            // weighted aggregate
            (weight, gs, os) -> DataValueAggregate.create()
                .withOperatorInfo("WeightedAverageNumeric_1.0", "Weighted Average",
                    "Calculates the weighted average per group. If any of the data or weight cells are missing, "
                    + "the result is not added to the aggregate.")
                .withSupportedClass(DoubleValue.class)
                .withAggregate(AverageNumeric::new)
                .withWeighting(weight, MultiplyNumeric::new)
                .build(gs, os)
        ),
        @Label("Minimum")
        MIN(MinOperator::new, null),
        @Label("Maximum")
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

    /** Create a builder with common config of #configure and #execute. */
    private static GlobalSettingsBuilder createGlobalSettingsBuilder() {
        return GlobalSettings.builder()
                .setValueDelimiter(GlobalSettings.STANDARD_DELIMITER)
                .setAggregationContext(AggregationContext.ROW_AGGREGATION);
    }

    /** Get the aggregated columns value to pass to the row aggregator. */
    private static Optional<String[]> getEffectiveAggregatedColumns(final DataTableSpec dts,
            final RowAggregatorSettings settings) {
        final var agg = settings.m_aggregationMethod;
        final var aggCols = settings.m_frequencyColumns.getSelected(
            dts.stream()//
                .filter(RowAggregatorNodeModel::isAggregatableColumn)//
                .map(DataColumnSpec::getName)//
                .toArray(String[]::new), dts);
        if (agg == AggregationFunction.COUNT || aggCols.length == 0) {
            // UI could still report an old value that is not used by COUNT (and is disabled/greyed-out in the UI)
            return Optional.empty();
        } else {
            return Optional.of(aggCols);
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

        // check aggregated columns and aggregation function together
        final var agg = settings.m_aggregationMethod;
        final var aggregatedColumns = getEffectiveAggregatedColumns(origSpec, settings);
        if (agg != AggregationFunction.COUNT) {
            // all other aggregations than COUNT currently need at least one aggregation column
            if (aggregatedColumns.isEmpty()) {
                throw new InvalidSettingsException(MISSING_AGGREGATION_COLUMNS);
            }
            final var aggCols = aggregatedColumns.get();
            // only placeholder set, behave as if missing columns
            final var onlyPlaceholder = aggCols.length == 1 && NOTHING_SELECTION.contains(aggCols[0]);
            CheckUtils.checkSetting(!onlyPlaceholder, MISSING_AGGREGATION_COLUMNS);
        }
        if (aggregatedColumns.isPresent()) {
            checkSettingMissingAggregatedColumns(origSpec, aggregatedColumns.get());
        }

        final var groupByColumn = checkSetting(getEffectiveGroupByColumn(settings), col -> origSpec.containsName(col),
            col -> String.format("Missing category column: \"%s\".", col));

        final var weightColumn = checkSetting(getEffectiveWeightColumn(settings), col -> origSpec.containsName(col),
                    col -> String.format("Missing weight column: \"%s\".", col));

        final var rowAgg = new RowAggregator(agg, groupByColumn.orElse(null), aggregatedColumns.orElse(null),
            weightColumn.orElse(null));

        final var groupByColAsList = rowAgg.getGroupByColumn().stream().collect(Collectors.toList());
        final var groupByGlobalSettings = createGlobalSettingsBuilder()
                .setDataTableSpec(origSpec)
                .setGroupColNames(groupByColAsList).build();
        // we have at least one column to aggregate, except for COUNT where we just count rows
        final var aggregators = rowAgg.getAggregators(groupByGlobalSettings, origSpec::getColumnSpec);

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

    /**
     * Generic checkSetting method that checks the given function to return {@code true} when applied to the given
     * instance to check, if the instance is non-null. If the instance is {@code null}, no check is done.
     *
     * @param <T> type of the instance to check
     * @param toCheck instance to check if non-null, otherwise no check is done
     * @param checkFn function that should be checked {@code true} (if {@code toCheck} non-null)
     * @param errorMsg function to construct error message if the check fails
     * @throws InvalidSettingsException
     */
    private static <T> Optional<T> checkSetting(final Optional<T> toCheck, final Predicate<T> checkFn,
            final Function<T, String> errorMsg) throws InvalidSettingsException {
        if (Objects.requireNonNull(toCheck).isEmpty()) {
            return toCheck;
        }
        final var e = toCheck.get();
        if (!checkFn.test(e)) {
            throw new InvalidSettingsException(errorMsg.apply(e));
        }
        return toCheck;
    }

    private static void checkSettingMissingAggregatedColumns(final DataTableSpec dts, final String[] columns)
            throws InvalidSettingsException {
        final var missing = Arrays.stream(columns)
                .filter(Predicate.not(dts::containsName))
                .collect(Collectors.toList());
        if (missing.isEmpty()) {
            return;
        }
        throw new InvalidSettingsException(
            missing.stream()
                .map(col -> "\"" + col + "\"")
                .collect(Collectors.joining(", ",
                    "Missing aggregation column" + (missing.size() > 1 ? "s" : "") + ": ", ".")));
    }

    @Override
    protected PortObject[] execute(final PortObject[] inPortObjects, final ExecutionContext exec,
        final RowAggregatorSettings settings) throws Exception {
        if (inPortObjects == null || inPortObjects[0] == null) {
            throw new Exception("No input available!");
        }

        final var table = (BufferedDataTable) inPortObjects[0];
        final var inSpec = table.getDataTableSpec();
        final var gsb = createGlobalSettingsBuilder()
                .setFileStoreFactory(FileStoreFactory.createWorkflowFileStoreFactory(exec))
                .setDataTableSpec(inSpec)
                .setNoOfRows(table.size());

        final var aggregatedColumns = getEffectiveAggregatedColumns(inSpec, settings);
        final var weightColumn = getEffectiveWeightColumn(settings);
        final var groupByColumn = getEffectiveGroupByColumn(settings);
        final var agg = settings.m_aggregationMethod;

        final var rowAgg = new RowAggregator(agg, groupByColumn.orElse(null), aggregatedColumns.orElse(null),
            weightColumn.orElse(null));

        final var groupByColAsList = rowAgg.getGroupByColumn().stream().collect(Collectors.toList());
        final var groupByGlobalSettings = gsb.setGroupColNames(groupByColAsList).build();
        final var aggregators = rowAgg.getAggregators(groupByGlobalSettings, inSpec::getColumnSpec);
        final var countColumnName = agg == AggregationFunction.COUNT ? COUNT_COLUMN_NAME : null;

        final BufferedDataTable groupedAggregates;
        if (!groupByColumn.isEmpty()) {
            exec.setMessage("Calculating group-by aggregate");
            final var groupedResult = new BigGroupByTable(exec, table,
                groupByColAsList,
                aggregators,
                countColumnName,
                groupByGlobalSettings,
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

        exec.setMessage("Calculating \"grand total\" aggregate");
        final var totalsGlobalSettings = gsb.setGroupColNames(Collections.emptyList()).build();
        final var totalAggregates = new MemoryGroupByTable(exec, table,
            new ArrayList<>(),
            aggregators,
            countColumnName,
            totalsGlobalSettings,
            ENABLE_HILITE,
            COL_NAME_POLICY,
            RETAIN_ORDER);
        warnSkippedGroups(totalAggregates);
        final var bufTotals = totalAggregates.getBufferedTable();

        exec.setMessage("Producing output table");
        final var totalResult = getSingleRowTotalsResult(exec, countColumnName, bufTotals);
        // grand totals get put into first output table, when no class column is selected
        final var first = groupedAggregates != null ? groupedAggregates : totalResult;
        final var second = groupedAggregates != null ? totalResult : InactiveBranchPortObject.INSTANCE;
        return new PortObject[] {
            first, second
        };
    }

    /**
     * Gets a single-row totals result according to our expected behavior for empty GroupBy results:
     * <ul>
     *  <li>COUNT should never return an empty table, nor a missing cell. It should always return a count.</li>
     *  <li>Other aggregates should return missing cells for the expected table columns if the input is empty.</li>
     * </ul>
     * @param exec context
     * @param countColumnName column name for count result column or {@code null} if aggregation functio is not COUNT
     * @param totalsResult data from the grand total aggregation
     * @return single-row grand-total result
     */
    private static BufferedDataTable getSingleRowTotalsResult(final ExecutionContext exec, final String countColumnName,
        final BufferedDataTable totalsResult) {

        final var totalSize = totalsResult.size();
        if (totalSize > 1) {
            throw new IllegalStateException("More than one row in totals table.");
        }

        final var resultSpec = totalsResult.getSpec();
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
            try (final var it = totalsResult.iterator()) {
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
