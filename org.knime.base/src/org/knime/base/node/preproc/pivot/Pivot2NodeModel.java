/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.pivot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.groupby.GroupByNodeModel;
import org.knime.base.node.preproc.groupby.GroupByTable;
import org.knime.base.node.util.SortKeyItem;
import org.knime.base.node.util.duplicates.DuplicateRowMarker;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.sort.AlphanumericComparator;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.data.sort.RowComparator;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.util.Pair;

/**
 * The {@link NodeModel} implementation of the pivot node which uses the {@link GroupByNodeModel} class implementations
 * to create an intermediate group-by table from which the pivoting table is extracted.
 *
 * @author Thomas Gabriel, KNIME.com AG, Switzerland
 */
public class Pivot2NodeModel extends GroupByNodeModel {

    /**
     * Enum to specify the output order of columns for group-by columns, pivot columns, and .
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    enum OutputColumnOrder implements ButtonGroupEnumInterface {

        ANY("Unspecified order", "Output column order is not specified and is implementation dependent."),
        RETAIN("Input order",
            "Output column order is as encountered in the input. For group columns this means the input column order, "
            + "for pivot columns this is based on the first occurrence of each pivot value."),
        SORTED_LEXICOGRAPHIC("Lexicographic order", "Columns are sorted lexicographically based on their name."),
        SORTED_ALPHANUMERIC("Alphanumeric order", "Columns are sorted alphanumerically based on their name.");

        private String m_text;
        private String m_tooltipText;

        OutputColumnOrder(final String text, final String tooltipText) {
            m_text = text;
            m_tooltipText = tooltipText;
        }

        @Override
        public String getText() {
            return m_text;
        }

        @Override
        public String getActionCommand() {
            return name();
        }

        @Override
        public String getToolTip() {
            return m_tooltipText;
        }

        @Override
        public boolean isDefault() {
            return this == ANY;
        }
    }

    /**
     * Enum providing the different options to name the pivot columns.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     * @since 3.7
     */
    public enum ColNameOption implements BiFunction<String, String, String> {

            /** The pivot name + aggregation name option. */
            PIV_FIRST_AGG_LAST("Pivot name" + PIVOT_AGGREGATION_DELIMITER + "Aggregation name",
                (t, u) -> t + PIVOT_AGGREGATION_DELIMITER + u),

            /** The aggregation name + pivot name option. */
            AGG_FIRST_PIV_LAST("Aggregation name" + PIVOT_AGGREGATION_DELIMITER + "Pivot name",
                (t, u) -> u + PIVOT_AGGREGATION_DELIMITER + t),

            /** The pivot name only option. */
            PIV_ONLY("Pivot name", (t, u) -> t);

        // currently not supported. The problem is that the columns created by
        // append overall totals are already reserve these names
        //            AGG_ONLY("Aggregation name only", (t, u) -> u);

        /** Missing name exception. */
        private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";

        /** IllegalArgumentException prefix. */
        private static final String ARGUMENT_EXCEPTION_PREFIX = "No ColNameOption constant with name: ";

        /** The option name. */
        private final String m_name;

        /** The function. */
        private final BiFunction<String, String, String> m_func;

        /** Constructor. */
        ColNameOption(final String name, final BinaryOperator<String> func) {
            m_name = name;
            m_func = func;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String apply(final String t, final String u) {
            return m_func.apply(t, u);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_name;
        }

        /**
         * Returns the enum for a given name.
         *
         * @param name the enum name
         * @return the enum
         * @throws InvalidSettingsException if the given name is not associated with an {@link ColNameOption} value
         */
        public static ColNameOption getEnum(final String name) throws InvalidSettingsException {
            if (name == null) {
                throw new InvalidSettingsException(NAME_MUST_NOT_BE_NULL);
            }
            return Arrays.stream(values()).filter(t -> t.m_name.equals(name)).findFirst()
                .orElseThrow(() -> new InvalidSettingsException(ARGUMENT_EXCEPTION_PREFIX + name));
        }

    }

    /**
     * Creates a row comparator with a descending sort order with missing values at the top.
     *
     * @param columnNames columns to sort on, all of which must be present in the table
     * @param spec table spec of table that will be sorted
     * @return row comparator
     */
    private static Comparator<DataRow> createComparator(final List<String> columnNames, final DataTableSpec spec) {
        final var cmp = RowComparator.on(spec);
        for (final var col : columnNames) {
            final var idx = spec.findColumnIndex(col);
            if (idx < 0) {
                throw new IllegalArgumentException(String.format("Could not find column name \"%s\" in table.", col));
            }
            cmp.thenComparingColumn(idx,
                b -> b.withDescendingSortOrder() //
                      .withMissingsLast(false) //
                      .withAlphanumericComparison(false));
        }
        return cmp.build();
    }

    /** The column name options config key. */
    private static final String CFG_COL_NAME_OPTION = "column_name_option";

    /** Key for order of groupby & pivot columns in output. */
    private static final String CFG_COLUMN_OUTPUT_ORDER = "columnOutputOrder";

    /** The lexicographical sort config key. */
    // since 5.1 subsumed in CFG_OUTPUT_COLUMN_ORDER
    private static final String LEGACY_CFG_LEXICOGRAPHICAL_SORT = "sort_lexicographical";

    /** Configuration key of the selected group by columns. */
    protected static final String CFG_PIVOT_COLUMNS = "pivotColumns";

    private final SettingsModelFilterString m_pivotCols = new SettingsModelFilterString(CFG_PIVOT_COLUMNS);

    private final SettingsModelBoolean m_ignoreMissValues = createSettingsMissingValues();

    private final SettingsModelBoolean m_totalAggregation = createSettingsTotal();

    private final SettingsModelBoolean m_ignoreDomain = createSettingsIgnoreDomain();

    private static final String PIVOT_COLUMN_DELIMITER = "_";

    private static final String PIVOT_AGGREGATION_DELIMITER = "+";

    private final HiLiteHandler m_totalGroupsHilite = new HiLiteHandler();

    private final SettingsModelString m_colAggOption = createSettingsColNameOption();

    private final SettingsModelString m_columnOrder = createSettingsOutputColumnOrder();

    /** Create a new pivot node model. */
    public Pivot2NodeModel() {
        super(1, 3);
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // we have to explicitly set all not pivot columns in the exclude list of the SettingsModelFilterString. The
        // DialogComponentColumnFilter component always uses the exclude/ list to update the component if we don't set
        // the exclude list all columns are added as group by columns.
        final DataTableSpec origSpec = (DataTableSpec)inSpecs[0];
        final Collection<String> exclList = getExcludeList(origSpec, m_pivotCols.getIncludeList());
        m_pivotCols.setExcludeList(exclList);
        final List<String> pivotCols = m_pivotCols.getIncludeList();
        if (pivotCols.isEmpty()) {
            throw new InvalidSettingsException("No pivot columns selected.");
        }
        // call super configure do have everything applied as in the super class
        super.configure(inSpecs);
        final List<String> groupCols = getGroupByColumns();
        for (final String piv : pivotCols) {
            if (groupCols.contains(piv)) {
                throw new InvalidSettingsException(
                    String.format("Ambiguous group/pivot column selection for column \"%s\".", piv));
            }
        }

        final List<String> groupAndPivotCols = createAllColumns();
        final DataTableSpec groupSpec = createGroupBySpec(origSpec, groupAndPivotCols);
        if (groupSpec.getNumColumns() == groupAndPivotCols.size()) {
            throw new InvalidSettingsException("No aggregation columns selected.");
        }


        final var outputColumnOrder = OutputColumnOrder.valueOf(m_columnOrder.getStringValue());
        final var sortColumns = OutputColumnOrder.SORTED_LEXICOGRAPHIC == outputColumnOrder
                || OutputColumnOrder.SORTED_ALPHANUMERIC == outputColumnOrder;

        // sort the columns if necessary
        DataTableSpec groupRowsSpec = createGroupBySpec(origSpec, groupCols);
        if (sortColumns) {
            groupRowsSpec = sortCols(groupRowsSpec, OutputColumnOrder.SORTED_ALPHANUMERIC == outputColumnOrder,
                createRange(0, groupCols.size()),
                createRange(groupCols.size(), groupRowsSpec.getNumColumns())).createSpec();
        }

        final var ignoreMissingValues = m_ignoreMissValues.getBooleanValue();
        if (ignoreMissingValues) {

            if (OutputColumnOrder.RETAIN == outputColumnOrder) {
                // cannot know the spec without touching the whole data in case our output column order must follow
                // the input order of pivot values
                return new DataTableSpec[] {null, groupRowsSpec, null};
            }

            final var pivotColIdx = columnsToIndices(groupSpec,
                colName -> () -> new IllegalArgumentException("Unknown column name \"%s\" ".formatted(colName)),
                pivotCols);
            final var ignoreDomain = m_ignoreDomain.getBooleanValue();
            final LinkedHashSet<String>[] combPivots = pivotValuesFromDomains(groupSpec, pivotColIdx, ignoreDomain,
                ignoreMissingValues);
            for (final Set<String> combPivot : combPivots) {
                if (combPivot == null) {
                    return new DataTableSpec[]{null, groupRowsSpec, null};
                }
            }
            var outSpec = createOutSpec(groupSpec, linkedSetToArray(combPivots), /* ignored */ new HashMap<>(), null);

            // sort the columns if necessary
            if (sortColumns) {
                final int grpSize = getGroupByColumns().size();
                outSpec = sortCols(outSpec, OutputColumnOrder.SORTED_ALPHANUMERIC == outputColumnOrder,
                        createRange(0, grpSize), createRange(grpSize, outSpec.getNumColumns()))
                    .createSpec();
            }
            if (m_totalAggregation.getBooleanValue()) {
                final var totalGroupSpec = createGroupBySpec(origSpec, Collections.emptyList());
                final var pivotRowsSpec =
                    new DataColumnSpec[outSpec.getNumColumns() + totalGroupSpec.getNumColumns()];
                for (var i = 0; i < outSpec.getNumColumns(); i++) {
                    pivotRowsSpec[i] = outSpec.getColumnSpec(i);
                }
                final int totalOffset = outSpec.getNumColumns();
                for (var i = 0; i < totalGroupSpec.getNumColumns(); i++) {
                    pivotRowsSpec[i + totalOffset] = totalGroupSpec.getColumnSpec(i);
                }
                var pivTotalsSpec = new DataTableSpec(pivotRowsSpec);
                if (sortColumns) {
                    pivTotalsSpec =
                        sortCols(pivTotalsSpec, OutputColumnOrder.SORTED_ALPHANUMERIC == outputColumnOrder,
                            createRange(totalOffset, pivTotalsSpec.getNumColumns())).createSpec();
                }
                return new DataTableSpec[]{outSpec, groupRowsSpec, pivTotalsSpec};
            } else {
                return new DataTableSpec[]{outSpec, groupRowsSpec, outSpec};
            }
        } else {
            return new DataTableSpec[]{null, groupRowsSpec, null};
        }
    }

    /**
     * Creates a column rearranger that reorders the columns of the provided spec within the provided ranges.
     *
     * The order of columns in each range will be alphanumerical if {@code alphaNum} is {@code true} and lexicographical
     * if it is {@code false}.
     *
     * @param spec the spec whose columns are to be reordered
     * @param range the ranges within which the columns are to be reordered
     * @return the column rearranger
     */
    @SafeVarargs
    private static ColumnRearranger sortCols(final DataTableSpec spec, final boolean alphaNum,
            final Pair<Integer, Integer>... range) {
        final var colReArr = new ColumnRearranger(spec);
        final String[] colNames = spec.getColumnNames();
        for (final Pair<Integer, Integer> p : range) {
            Arrays.sort(colNames, p.getFirst(), p.getSecond(),
                alphaNum ? new AlphanumericComparator(Comparator.naturalOrder()) : Comparator.naturalOrder());
        }
        colReArr.permute(colNames);
        return colReArr;
    }

    /**
     * Convenience method to create a range.
     *
     * @param lower the lower value
     * @param upper the upper value
     * @return the range
     */
    private static Pair<Integer, Integer> createRange(final int lower, final int upper) {
        return Pair.create(lower, upper);
    }

    private static LinkedHashSet<String>[] pivotValuesFromDomains(final DataTableSpec spec,
            final int[] pivotCols,
            final boolean ignoreDomain, final boolean ignoreMissingValues) {
        final var numInputPivotColumns = pivotCols.length;
        @SuppressWarnings("unchecked")
        final LinkedHashSet<String>[] combPivots = new LinkedHashSet[numInputPivotColumns]; // NOSONAR need type param
        if (ignoreDomain) {
            return combPivots;
        }
        for (var i = 0; i < pivotCols.length; i++) {
            final DataColumnDomain domain = spec.getColumnSpec(pivotCols[i]).getDomain();
            if (domain.hasValues()) {
                combPivots[i] = new LinkedHashSet<>();
                final Set<DataCell> values = domain.getValues();
                for (final DataCell pivotValue : values) {
                    combPivots[i].add(pivotValue.toString());
                }
                if (!ignoreMissingValues) {
                    combPivots[i].add("?");
                }
            }
        }
        return combPivots;
    }

    private static final String[][] linkedSetToArray(final LinkedHashSet<String>[] combPivots) {
        final var pivotValues = new String[combPivots.length][];
        for (var i = 0; i < combPivots.length; i++) {
            final var pv = combPivots[i];
            final var out = new String[pv.size()];
            var j = 0;
            for (final String v : pv) {
                out[j] = v;
                j++;
            }
            pivotValues[i] = out;
        }
        return pivotValues;
    }

    private List<String> createAllColumns() {
        final List<String> all = new ArrayList<>(getGroupByColumns());
        final List<String> pivotCols = m_pivotCols.getIncludeList();
        all.removeAll(pivotCols);
        all.addAll(pivotCols);
        return all;
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable table = (BufferedDataTable)inData[0];
        final List<String> groupAndPivotCols = createAllColumns();
        final BufferedDataTable groupTable;
        final String orderPivotColumnName;

        final var groupAndPivotExec = exec.createSubExecutionContext(0.5);
        final var groupExec = exec.createSubExecutionContext(0.25); // NOSONAR
        final var pivotExec = exec.createSubExecutionContext(0.25);

        final var processInMemory = isProcessInMemory();
        final var retainRowOrderForGroups = isRetainOrder();
        final var outputColumnOrder = OutputColumnOrder.valueOf(m_columnOrder.getStringValue());
        final var retainOrderForOutputOrder = OutputColumnOrder.RETAIN == outputColumnOrder;

        var progMainTotal = 0.0;
        double progMainTableAppendIndexForSort = processInMemory || retainRowOrderForGroups ? 1.0 : 0.0;
        progMainTotal += progMainTableAppendIndexForSort;
        double progMainDeterminePivotOrderToRetain = retainOrderForOutputOrder ? 2.0 : 0.0;
        progMainTotal += progMainDeterminePivotOrderToRetain;
        var progMainTableGroup = 5.0;
        progMainTotal += progMainTableGroup;
        double progMainTableInMemSort = processInMemory ? 3.0 : 0.0;
        progMainTotal += progMainTableInMemSort;
        var progMainTableGetPivots = 1.0;
        progMainTotal += progMainTableGetPivots;
        var progMainTableFillPivots = 1.0;
        progMainTotal += progMainTableFillPivots;
        double progMainTableRestoreSort = processInMemory || retainRowOrderForGroups ? 1.0 : 0.0;
        progMainTotal += progMainTableRestoreSort;
        double progMainTableReplaceRowKey = processInMemory ? 1.0 : 0.0;
        progMainTotal += progMainTableReplaceRowKey;


        BufferedDataTable distinctPivotTuples = null;
        if (retainOrderForOutputOrder) {
            exec.setProgress("Determining pivot input order...");
            final var pivotCols =  m_pivotCols.getIncludeList().toArray(String[]::new);
            final var cR = new ColumnRearranger(table.getSpec());
            cR.keepOnly(pivotCols);
            final var compPivotOrderExec = groupAndPivotExec.createSubExecutionContext(
                progMainDeterminePivotOrderToRetain / progMainTotal);
            final var pivotValues = compPivotOrderExec.createColumnRearrangeTable(table, cR, compPivotOrderExec);
            final var marker = new DuplicateRowMarker(true, true, true, false, false, false);
            // we are only interested in the first occurrence of each duplicate row
            final SortKeyItem[] sortKey = Arrays.stream(pivotCols).map(col -> new SortKeyItem(col, true, false))
                    .toArray(SortKeyItem[]::new);
            distinctPivotTuples = marker.run(pivotValues, pivotCols, sortKey, exec);
        }

        if (processInMemory || retainRowOrderForGroups) {
            exec.setMessage("Keeping row order");
            final String retainOrderCol = DataTableSpec.getUniqueColumnName(table.getDataTableSpec(), "#pivot_order#");
            // append temp. id column with minimum-aggregation method
            final ColumnAggregator[] colAggregators = getColumnAggregators().toArray(new ColumnAggregator[0]);
            final Set<String> workingCols = new LinkedHashSet<>();
            workingCols.addAll(groupAndPivotCols);
            for (final ColumnAggregator ca : colAggregators) {
                workingCols.add(ca.getOriginalColName());
            }
            workingCols.add(retainOrderCol);
            final BufferedDataTable appTable = GroupByTable.appendOrderColumn(
                groupAndPivotExec.createSubExecutionContext(progMainTableAppendIndexForSort / progMainTotal), table,
                workingCols, retainOrderCol);
            final DataColumnSpec retainOrderColSpec = appTable.getSpec().getColumnSpec(retainOrderCol);
            final var aggrs = new ColumnAggregator[colAggregators.length + 1];
            System.arraycopy(colAggregators, 0, aggrs, 0, colAggregators.length);
            aggrs[colAggregators.length] =
                new ColumnAggregator(retainOrderColSpec, AggregationMethods.getRowOrderMethod(), true);
            orderPivotColumnName = getColumnNamePolicy().createColumName(aggrs[colAggregators.length]);
            exec.setMessage("Grouping main table");
            final var groupByTable =
                createGroupByTable(groupAndPivotExec.createSubExecutionContext(progMainTableGroup / progMainTotal),
                    appTable, groupAndPivotCols, isProcessInMemory(),
                    false /* retain order always false; handled by pivoting */, Arrays.asList(aggrs));
            // table is not sorted by group&pivot columns; if process in memory
            // true then sort table by group&pivot columns
            final BufferedDataTable origGroupByTable = groupByTable.getBufferedTable();
            if (processInMemory) {
                exec.setMessage("Sorting group table");
                final var comp = createComparator(groupAndPivotCols, origGroupByTable.getSpec());
                groupTable = new BufferedDataTableSorter(origGroupByTable, comp)
                    .sort(groupAndPivotExec.createSubExecutionContext(progMainTableInMemSort / progMainTotal));
            } else {
                groupTable = origGroupByTable;
            }
        } else {
            exec.setMessage("Grouping main table");
            final var groupByTable =
                createGroupByTable(groupAndPivotExec.createSubExecutionContext(progMainTableGroup / progMainTotal),
                    table, groupAndPivotCols, isProcessInMemory(), false, getColumnAggregators());
            groupTable = groupByTable.getBufferedTable();
            orderPivotColumnName = null;
        }
        final List<String> pivotCols = m_pivotCols.getIncludeList();
        final DataTableSpec groupSpec = groupTable.getSpec();
        final var pivotIdx = columnsToIndices(groupSpec,
            colName -> () -> new IllegalArgumentException("Unknown column name \"%s\" ".formatted(colName)),
            pivotCols);
        final var ignoreDomain = m_ignoreDomain.getBooleanValue();
        final var ignoreMissingValues = m_ignoreMissValues.getBooleanValue();

        final String[][] pivotValues;
        final var fillExec = groupAndPivotExec.createSubExecutionContext(progMainTableGetPivots / progMainTotal);
        if (distinctPivotTuples == null) {
            // pivot values are derived in arbitrary order from domain or from group by table
            final var combPivots = pivotValuesFromDomains(groupSpec, pivotIdx, ignoreDomain, ignoreMissingValues);
            exec.setProgress("Determining pivots...");
            final long groupTableSize = groupTable.size();
            long groupIndex = 0;
            for (final DataRow row : groupTable) {
                for (var i = 0; i < pivotIdx.length; i++) {
                    if (combPivots[i] == null) {
                        combPivots[i] = new LinkedHashSet<>();
                    }
                    final DataCell cell = row.getCell(pivotIdx[i]);
                    if (cell.isMissing()) {
                        if (!ignoreMissingValues) {
                            combPivots[i].add(cell.toString());
                        }
                    } else {
                        combPivots[i].add(cell.toString());
                    }
                }
                fillExec.setProgress(groupIndex++ / (double)groupTableSize,
                    String.format("Group \"%s\" (%d/%d)", row.getKey(), groupIndex, groupTableSize));
                fillExec.checkCanceled();
            }
            // copy array of linked hash sets to output array
            pivotValues = linkedSetToArray(combPivots);
        } else {
            exec.setProgress("Determining pivots from distinct pivot values...");
            if (distinctPivotTuples.size() > Integer.MAX_VALUE) {
                createMessageBuilder().addTextIssue("Cannot pivot with more than %d distinct pivots."
                    .formatted(distinctPivotTuples.size()));
            }
            final var numDistinctPivotTuples = (int) distinctPivotTuples.size();
            pivotValues = new String[distinctPivotTuples.getSpec().getNumColumns()][numDistinctPivotTuples];
            var pivotIndex = 0;
            for (final DataRow row : distinctPivotTuples) {
                rowToArray(pivotValues, pivotIndex, row, ignoreMissingValues);
                fillExec.setProgress(pivotIndex / (double) numDistinctPivotTuples, String.format("Pivot \"%s\" (%d/%d)",
                    row.getKey(), pivotIndex, numDistinctPivotTuples));
                fillExec.checkCanceled();
                pivotIndex++;
            }
        }


        final Map<String, Integer> pivotStarts = new LinkedHashMap<>();
        final DataTableSpec outSpec = createOutSpec(groupSpec, pivotValues, pivotStarts, orderPivotColumnName);
        exec.setProgress("Filling pivot table");
        BufferedDataTable pivotTable = fillPivotTable(groupTable, outSpec, pivotStarts,
            groupAndPivotExec.createSubExecutionContext(progMainTableFillPivots / progMainTotal), orderPivotColumnName);

        if (orderPivotColumnName != null) {
            exec.setMessage("Restoring row order");
            final var sortedPivotTable =
                new SortedTable(pivotTable, Arrays.asList(orderPivotColumnName), new boolean[]{true},
                    groupAndPivotExec.createSubExecutionContext(progMainTableRestoreSort / progMainTotal));
            pivotTable = sortedPivotTable.getBufferedDataTable();
            final var colre = new ColumnRearranger(pivotTable.getSpec());
            colre.remove(orderPivotColumnName);
            pivotTable = exec.createColumnRearrangeTable(pivotTable, colre, exec.createSilentSubProgress(0.0));
        }
        // temp fix for bug 3286
        if (isProcessInMemory()) {
            // if process in memory is true, RowKey's needs to be re-computed
            final BufferedDataContainer rowkeyBuf =
                groupAndPivotExec.createSubExecutionContext(progMainTableReplaceRowKey / progMainTotal)
                    .createDataContainer(pivotTable.getSpec());
            long rowIndex = 0;
            for (DataRow row : pivotTable) {
                rowkeyBuf.addRowToTable(new DefaultRow(RowKey.createRowKey(rowIndex++), row));
            }
            rowkeyBuf.close();
            pivotTable = rowkeyBuf.getTable();
        }
        groupAndPivotExec.setProgress(1.0);

        /* Fill the 3rd port */
        exec.setMessage("Determining pivot totals");
        var progPivotTotal = 0.0d;
        final var progPivotGroup = 5.0d;
        progPivotTotal += progPivotGroup;
        final var progPivotFillMissing = 1.0d;
        progPivotTotal += progPivotFillMissing;
        final var progPivotFillPivots = 1.0d;
        progPivotTotal += progPivotFillPivots;
        final double progPivotOverallTotals = m_totalAggregation.getBooleanValue() ? 5.0 : 0.0;
        progPivotTotal += progPivotOverallTotals;

        // create pivot table only on pivot columns (for grouping)
        // perform pivoting: result in single line
        final var rowGroup =
            createGroupByTable(pivotExec.createSubExecutionContext(progPivotGroup / progPivotTotal), table,
                m_pivotCols.getIncludeList(), processInMemory, retainRowOrderForGroups, getColumnAggregators());
        final BufferedDataTable rowGroupTable = rowGroup.getBufferedTable();
        // fill group columns with missing cells
        final var colre = new ColumnRearranger(rowGroupTable.getDataTableSpec());
        for (var i = 0; i < getGroupByColumns().size(); i++) {
            final DataColumnSpec cspec = outSpec.getColumnSpec(i);
            final CellFactory factory = new SingleCellFactory(cspec) {
                @Override
                public DataCell getCell(final DataRow row) {
                    return DataType.getMissingCell();
                }
            };
            colre.insertAt(i, factory);
        }
        final BufferedDataTable groupedRowTable = exec.createColumnRearrangeTable(rowGroupTable, colre,
            pivotExec.createSubExecutionContext(progPivotFillMissing / progPivotTotal));
        BufferedDataTable pivotRowsTable = fillPivotTable(groupedRowTable, outSpec, pivotStarts,
            pivotExec.createSubExecutionContext(progPivotFillPivots / progPivotTotal), null);
        if (orderPivotColumnName != null) {
            final var colre2 = new ColumnRearranger(pivotRowsTable.getSpec());
            colre2.remove(orderPivotColumnName);
            pivotRowsTable = exec.createColumnRearrangeTable(pivotRowsTable, colre2, exec.createSilentSubProgress(0.0));
        }

        // total aggregation without grouping
        if (m_totalAggregation.getBooleanValue()) {
            final var totalGroup =
                createGroupByTable(pivotExec.createSubExecutionContext(progPivotOverallTotals / progPivotTotal), table,
                    Collections.emptyList(), processInMemory, retainRowOrderForGroups, getColumnAggregators());
            final var totalGroupTable = totalGroup.getBufferedTable();

            final var pivotsRowsSpec = pivotRowsTable.getSpec();
            final var totalGroupSpec = totalGroupTable.getSpec();
            final var overallTotalSpec = new DataTableSpec(pivotsRowsSpec, totalGroupSpec);
            final var buf = exec.createDataContainer(overallTotalSpec);
            if (pivotRowsTable.size() > 0) {
                final List<DataCell> pivotTotalsCells = new ArrayList<>();
                try (final var it = pivotRowsTable.iterator()) {
                    final DataRow pivotsRow = it.next();
                    pivotsRow.forEach(pivotTotalsCells::add);
                }
                try (final var it = totalGroupTable.iterator()) {
                    final DataRow totalGroupRow = it.next();
                    totalGroupRow.forEach(pivotTotalsCells::add);
                }
                buf.addRowToTable(new DefaultRow(new RowKey("Totals"), pivotTotalsCells));
            }
            buf.close();
            pivotRowsTable = buf.getTable();
        }
        pivotExec.setProgress(1.0);

        /* Fill the 2nd port: important to create this last since it will create
         * the final hilite handler (mapping) for port #1 AND #2 (bug 3270) */
        exec.setMessage("Creating group totals");
        // create group table only on group columns; no pivoting
        BufferedDataTable columnGroupTable =
            createGroupByTable(groupExec, table, getGroupByColumns()).getBufferedTable();

        // Change output column orders in each "column group" (group columns, pivot columns)
        // based on column name sorting
        final var sortColumns = OutputColumnOrder.SORTED_LEXICOGRAPHIC == outputColumnOrder
                || OutputColumnOrder.SORTED_ALPHANUMERIC == outputColumnOrder;
        if (sortColumns) {
            final var pivotTableSpec = pivotTable.getDataTableSpec();
            // the group range
            final Pair<Integer, Integer> grpRange = createRange(0, getGroupByColumns().size());
            // the pivot range
            final Pair<Integer, Integer> pivRange = createRange(grpRange.getSecond(), pivotTableSpec.getNumColumns());

            final var alphaNum = OutputColumnOrder.SORTED_ALPHANUMERIC == outputColumnOrder;
            // rearrange the tables
            pivotTable = exec.createColumnRearrangeTable(pivotTable, sortCols(pivotTableSpec, alphaNum,
                grpRange, pivRange),
                exec.createSilentSubProgress(0));

            final var columnGroupTableSpec = columnGroupTable.getDataTableSpec();
            columnGroupTable = exec.createColumnRearrangeTable(columnGroupTable,
                sortCols(columnGroupTableSpec, alphaNum, grpRange,
                    createRange(grpRange.getSecond(), columnGroupTableSpec.getNumColumns())),
                exec.createSilentSubProgress(0));

            final var pivotRowsTableSpec = pivotRowsTable.getDataTableSpec();
            pivotRowsTable = exec.createColumnRearrangeTable(pivotRowsTable,
                sortCols(pivotRowsTableSpec, alphaNum, grpRange, pivRange,
                    createRange(pivRange.getSecond(), pivotRowsTableSpec.getNumColumns())),
                exec.createSilentSubProgress(0));
        }

        return new PortObject[]{
            // pivot table
            pivotTable,
            // group totals
            columnGroupTable,
            // pivot and overall totals
            pivotRowsTable};
    }

    private static void rowToArray(final String[][] pivotValues, final int pivotIndex, final DataRow row,
            final boolean ignoreMissingValues) {
        final var pivotCells = row.getNumCells();
        for (var i = 0; i < pivotCells; i++) {
            final DataCell cell = row.getCell(i);
            if (!cell.isMissing() || !ignoreMissingValues) {
                pivotValues[i][pivotIndex] = cell.toString();
            }
        }
    }

    private DataTableSpec createOutSpec(final DataTableSpec groupSpec, final String[][] combPivots,
        final Map<String, Integer> pivotStarts, final String orderPivotColumnName) throws InvalidSettingsException {
        final List<String> groupCols = getGroupByColumns();
        final List<String> groupAndPivotCols = createAllColumns();
        final List<String> pivots = new ArrayList<>();
        createPivotColumns(combPivots, pivots, 0);
        final List<DataColumnSpec> cspecs = new ArrayList<>();
        for (final DataColumnSpec cspec : groupSpec) {
            if (groupCols.contains(cspec.getName())) {
                cspecs.add(cspec);
            }
        }

        final var opt = ColNameOption.getEnum(m_colAggOption.getStringValue());

        // all pivots combined with agg. methods
        for (final String p : pivots) {
            pivotStarts.put(p, cspecs.size());
            for (final DataColumnSpec cspec : groupSpec) {
                if (cspec.getName().equals(orderPivotColumnName)) {
                    continue;
                }
                final String name = cspec.getName();
                if (!groupAndPivotCols.contains(name)) {
                    final DataColumnSpec pivotCSpec =
                        new DataColumnSpecCreator(opt.apply(p, name), cspec.getType()).createSpec();
                    cspecs.add(pivotCSpec);
                }
            }
        }

        // append pivot order column
        if (orderPivotColumnName != null) {
            cspecs.add(groupSpec.getColumnSpec(orderPivotColumnName));
        }
        return new DataTableSpec(cspecs.toArray(new DataColumnSpec[0]));
    }

    private BufferedDataTable fillPivotTable(final BufferedDataTable groupTable, final DataTableSpec pivotSpec,
        final Map<String, Integer> pivotStarts, final ExecutionContext exec, final String orderPivotColumnName)
        throws CanceledExecutionException {
        final var buf = exec.createDataContainer(pivotSpec);
        final var pivotCols = m_pivotCols.getIncludeList();
        final int pivotCount = pivotCols.size();
        final var groupCols = new ArrayList<>(getGroupByColumns());
        groupCols.removeAll(pivotCols);
        final int groupCount = groupCols.size();
        final DataTableSpec groupSpec = groupTable.getSpec();
        final int colCount = groupSpec.getNumColumns();
        final var outcells = new DataCell[pivotSpec.getNumColumns()];
        final long totalRowCount = groupTable.size();
        long rowIndex = 0;
        for (final DataRow row : groupTable) {
            final var origRowKey = row.getKey();
            String pivotColumn = null;
            for (var i = 0; i < colCount; i++) {
                final DataCell cell = row.getCell(i);
                // is a group column
                if (i < groupCount) {
                    // diff group found: write out current group and cont.
                    if (outcells[i] != null && !cell.equals(outcells[i])) {
                        // write row to out table
                        write(buf, outcells);
                        // reset pivot column name and out data row
                        pivotColumn = null;
                        for (int j = i + 1; j < outcells.length; j++) {
                            outcells[j] = null;
                        }
                    }
                    outcells[i] = cell;
                    // is pivot column
                } else if (i < (groupCount + pivotCount)) {
                    // check for missing pivots
                    if (m_ignoreMissValues.getBooleanValue() && cell.isMissing()) {
                        for (var j = 0; j < outcells.length; j++) {
                            outcells[j] = null;
                        }
                        break;
                    }
                    // create pivot column
                    if (pivotColumn == null) {
                        pivotColumn = cell.toString();
                    } else {
                        pivotColumn += PIVOT_COLUMN_DELIMITER + cell.toString();
                    }
                    // is a aggregation column
                } else {
                    final int idx = pivotStarts.get(pivotColumn);
                    final int pivotIndex = i - pivotCount - groupCount;
                    final int pivotCellIndex = idx + pivotIndex;
                    if (orderPivotColumnName == null // if retain order is off
                        || !groupSpec.getColumnSpec(i).getName().equals(orderPivotColumnName)) {
                        outcells[pivotCellIndex] = cell;
                    } else { // temp retain column (type:IntCell)
                        final int retainIndex = outcells.length - 1;
                        if (outcells[retainIndex] == null) {
                            outcells[retainIndex] = cell;
                        } else {
                            final DataValueComparator comp =
                                pivotSpec.getColumnSpec(retainIndex).getType().getComparator();
                            if (comp.compare(outcells[retainIndex], cell) > 0) {
                                outcells[retainIndex] = cell;
                            }
                        }
                    }
                }
            }
            exec.setProgress(rowIndex++ / (double)totalRowCount,
                String.format("Group \"%s\" (%d/%d)", origRowKey, rowIndex, totalRowCount));
            exec.checkCanceled();
        }
        // write last group - if any.
        if (outcells[0] != null) {
            write(buf, outcells);
        }
        buf.close();
        return buf.getTable();
    }

    private static void write(final BufferedDataContainer buf, final DataCell[] outcells) {
        for (var j = 0; j < outcells.length; j++) {
            if (outcells[j] == null) {
                outcells[j] = DataType.getMissingCell();
            }
        }
        final var key = RowKey.createRowKey(buf.size());
        final var outrow = new DefaultRow(key, outcells);
        buf.addRowToTable(outrow);
    }

    private static void createPivotColumns(final String[][] combs, final List<String> pivots, final int index) {
        if (index == combs.length || combs[index] == null) {
            return;
        }
        if (pivots.isEmpty()) {
            pivots.addAll(List.of(combs[index]));
        } else {
            final List<String> copy = new ArrayList<>(pivots);
            pivots.clear();
            for (final String s : combs[index]) {
                for (final String p : copy) {
                    pivots.add(p + PIVOT_COLUMN_DELIMITER + s);
                }
            }
        }
        createPivotColumns(combs, pivots, index + 1);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_pivotCols.saveSettingsTo(settings);
        m_ignoreMissValues.saveSettingsTo(settings);
        m_totalAggregation.saveSettingsTo(settings);
        m_ignoreDomain.saveSettingsTo(settings);
        m_colAggOption.saveSettingsTo(settings);
        // sortLexigraphcial removed in KNIME 5.1
        m_columnOrder.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_pivotCols.validateSettings(settings);
        m_ignoreMissValues.validateSettings(settings);
        m_totalAggregation.validateSettings(settings);
        m_ignoreDomain.validateSettings(settings);

        // ensure backwards compatibility (since KNIME 3.7)
        if (settings.containsKey(CFG_COL_NAME_OPTION)) {
            // check if the column naming option string refers to a proper ColNameOption
            m_colAggOption.validateSettings(settings);
            final SettingsModelString tmpColAggOption = createSettingsColNameOption();
            tmpColAggOption.loadSettingsFrom(settings);
            final var colAggOption = ColNameOption.getEnum(tmpColAggOption.getStringValue());
            if (colAggOption == ColNameOption.PIV_ONLY && ColumnAggregator.loadColumnAggregators(settings).size() > 1) {
                throw new InvalidSettingsException("The \'" + ColNameOption.PIV_ONLY.toString()
                    + "\' column naming option solely supports the selection of a single aggregation method.");
            }
        }
        if (settings.containsKey(LEGACY_CFG_LEXICOGRAPHICAL_SORT)) {
            // option is merged into the column order below since KNIME 5.1
            // it was a Boolean before, so there is not much to validate
            createSettingsLexicographical().validateSettings(settings);
        } else if (settings.containsKey(CFG_COLUMN_OUTPUT_ORDER)) {
            m_columnOrder.validateSettings(settings);
        }

        // has to be done after validating the column aggregation option
        // Otherwise it is likely that one of the naming policies throws
        // an exception even though its selection is disabled via the UI
        // (this is the case when PIV_ONLY is selected.
        // Important note! PIV_ONLY valid implies that the selected
        // naming policy is valid too.
        super.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_pivotCols.loadSettingsFrom(settings);
        m_ignoreMissValues.loadSettingsFrom(settings);
        m_totalAggregation.loadSettingsFrom(settings);
        m_ignoreDomain.loadSettingsFrom(settings);

        // ensure backwards compatibility (since KNIME 3.7)
        if (settings.containsKey(CFG_COL_NAME_OPTION)) {
            m_colAggOption.loadSettingsFrom(settings);
        }
        if (settings.containsKey(LEGACY_CFG_LEXICOGRAPHICAL_SORT)) {
            m_columnOrder.setStringValue(OutputColumnOrder.SORTED_LEXICOGRAPHIC.name());
        } else if (settings.containsKey(CFG_COLUMN_OUTPUT_ORDER)) {
            // since KNIME 5.1 (also this includes the lexicographical sort order option)
            m_columnOrder.loadSettingsFrom(settings);
        } else {
            // for backwards compatibility with versions created before even the lexicographical option was introduced
            m_columnOrder.setStringValue(OutputColumnOrder.ANY.name());
        }
    }

    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        // bugfix 3055: wrong hilite handler passed to #2/#3 out-port
        if (outIndex == 0 || outIndex == 1) {
            return super.getOutHiLiteHandler(0);
        } else {
            return m_totalGroupsHilite;
        }
    }

    /**
     * Creates the settings model storing the column name option.
     *
     * @return the settings model storing the column name option
     */
    static final SettingsModelString createSettingsColNameOption() {
        // for backwards compatibility reason the default value cannot be changed!
        return new SettingsModelString(CFG_COL_NAME_OPTION, ColNameOption.PIV_FIRST_AGG_LAST.toString());
    }

    /**
     * Creates the settings model storing the sort lexicographical flag.
     *
     * @return the settings model storing the sort lexicographical flag
     */
    static final SettingsModelBoolean createSettingsLexicographical() {
        // for backwards compatibility reason the default value cannot be changed!
        return new SettingsModelBoolean(LEGACY_CFG_LEXICOGRAPHICAL_SORT, false);
    }

    static SettingsModelString createSettingsOutputColumnOrder() {
        return new SettingsModelString(CFG_COLUMN_OUTPUT_ORDER, OutputColumnOrder.ANY.name());
    }

    /** @return settings model boolean for ignoring missing values */
    static final SettingsModelBoolean createSettingsMissingValues() {
        return new SettingsModelBoolean("missing_values", true);
    }

    /** @return settings model boolean for total aggregation */
    static final SettingsModelBoolean createSettingsTotal() {
        return new SettingsModelBoolean("total_aggregation", false);
    }

    /** @return settings model boolean for ignoring domain */
    static final SettingsModelBoolean createSettingsIgnoreDomain() {
        return new SettingsModelBoolean("ignore_domain", true);
    }


    /**
     * Helper method for converting an array of possible column names to an array of column indices.
     * If a name does not represent a column in this spec, the given exception supplier is used to throw the
     * exception.
     *
     * @param <T> type of the exception that is thrown
     * @param excSup function to retrieve a supplier for the exception in case the given name is not among the columns
     *  in the spec
     * @param names column names to look up
     * @return list of column indices in the order of the given names
     * @throws T in case a name does not represent a column in the spec
     * @since 5.1
     */
    private static <T extends Throwable> int[] columnsToIndices(final DataTableSpec spec,
            final Function<String, Supplier<T>> excSup, final List<String> names) throws T {
        final var out = new int[names.size()];
        int i = 0;
        for (final var name : names) {
            out[i] = spec.lookupColumnIndex(name).orElseThrow(excSup.apply(name));
            i++;
        }
        return out;
    }
}
