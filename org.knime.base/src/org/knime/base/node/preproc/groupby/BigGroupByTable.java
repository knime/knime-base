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
 */

package org.knime.base.node.preproc.groupby;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.UnaryOperator;

import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.util.valueformat.NumberFormatter;


/**
 * A data table that groups a given input table by the given columns and calculates the aggregation values of the
 * remaining rows. Call the {@link #getBufferedTable()} method after instance creation to get the grouped table. If the
 * enableHilite flag was set to <code>true</code> call the {@link #getHiliteMapping()} method to get the row key
 * translation <code>Map</code>. Call the {@link #getSkippedGroupsByColName()} method to get a <code>Map</code> with all
 * skipped groups or the {@link #getSkippedGroupsMessage(int, int)} for a appropriate warning message.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class BigGroupByTable extends GroupByTable {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(BigGroupByTable.class);

    /**
     * Constructor for class BigGroupByTable.
     *
     * @param exec the <code>ExecutionContext</code>
     * @param inDataTable the table to aggregate
     * @param groupByCols the name of all columns to group by
     * @param colAggregators the aggregation columns with the aggregation method to use in the order the columns should
     *        appear in the result table
     * @param countColumnName name of the group row count column or {@code null} if counts should not be added
     * @param globalSettings the global settings
     * @param enableHilite <code>true</code> if a row key map should be maintained to enable hiliting
     * @param colNamePolicy the {@link ColumnNamePolicy} for the aggregation columns
     * @param retainOrder returns the row of the table in the same order as the input table if set to <code>true</code>
     * @throws CanceledExecutionException if the user has canceled the execution
     * @since 5.0
     */
    public BigGroupByTable(final ExecutionContext exec,
            final BufferedDataTable inDataTable,
            final List<String> groupByCols,
            final ColumnAggregator[] colAggregators,
            final String countColumnName,
            final GlobalSettings globalSettings, final boolean enableHilite,
            final ColumnNamePolicy colNamePolicy, final boolean retainOrder)
    throws CanceledExecutionException {
        super(exec, inDataTable, groupByCols, colAggregators, countColumnName, globalSettings,
                enableHilite, colNamePolicy, retainOrder);
    }

    /**Constructor for class BigGroupByTable.
     *
     * @param exec the <code>ExecutionContext</code>
     * @param inDataTable the table to aggregate
     * @param groupByCols the name of all columns to group by
     * @param colAggregators the aggregation columns with the aggregation method to use in the order the columns should
     *        appear in the result table numerical columns
     * @param globalSettings the global settings
     * @param enableHilite <code>true</code> if a row key map should be maintained to enable hiliting
     * @param colNamePolicy the {@link ColumnNamePolicy} for the aggregation columns
     * @param retainOrder returns the row of the table in the same order as the input table if set to <code>true</code>
     * @throws CanceledExecutionException if the user has canceled the execution
     * @since 2.6
     * @deprecated added option for "COUNT-*"-style aggregation with {@code countColumnName} constructor
     * @see #BigGroupByTable(ExecutionContext, BufferedDataTable, List, ColumnAggregator[], String, GlobalSettings,
     *        boolean, ColumnNamePolicy, boolean)
     */
    @Deprecated
    public BigGroupByTable(final ExecutionContext exec,
            final BufferedDataTable inDataTable,
            final List<String> groupByCols,
            final ColumnAggregator[] colAggregators,
            final GlobalSettings globalSettings, final boolean enableHilite,
            final ColumnNamePolicy colNamePolicy, final boolean retainOrder)
    throws CanceledExecutionException {
        super(exec, inDataTable, groupByCols, colAggregators, null, globalSettings,
                enableHilite, colNamePolicy, retainOrder);
    }

    /**
     * Constructor for class BigGroupByTable.
     *
     * @param exec the <code>ExecutionContext</code>
     * @param inDataTable the table to aggregate
     * @param groupByCols the name of all columns to group by
     * @param colAggregators the aggregation columns with the aggregation method to use in the order the columns should
     *        appear in the result table numerical columns
     * @param countColumnName name of the group row count column or {@code null} if counts should not be added
     * @param globalSettings the global settings
     * @param sortInMemory <code>true</code> if the table should be sorted in the memory
     * @param enableHilite <code>true</code> if a row key map should be maintained to enable hiliting
     * @param colNamePolicy the {@link ColumnNamePolicy} for the aggregation columns
     * @param retainOrder returns the row of the table in the same order as the input table if set to <code>true</code>
     * @throws CanceledExecutionException if the user has canceled the execution
     * @deprecated sortInMemory option is no longer required
     * @see #BigGroupByTable(ExecutionContext, BufferedDataTable, List, ColumnAggregator[], GlobalSettings, boolean,
     *                       ColumnNamePolicy, boolean)
     *
     * @since 5.0
     */
    @Deprecated
    public BigGroupByTable(final ExecutionContext exec,
            final BufferedDataTable inDataTable,
            final List<String> groupByCols,
            final ColumnAggregator[] colAggregators,
            final String countColumnName,
            final GlobalSettings globalSettings, final boolean sortInMemory,
            final boolean enableHilite, final ColumnNamePolicy colNamePolicy,
            final boolean retainOrder)
    throws CanceledExecutionException {
        super(exec, inDataTable, groupByCols, colAggregators, countColumnName, globalSettings,
                sortInMemory, enableHilite, colNamePolicy, retainOrder);
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.0
     */
    @SuppressWarnings("resource")
    @Override
    protected void createGroupByTable(final ExecutionContext exec, final BufferedDataTable table,
            final int[] groupColIdx, final boolean appendRowCountColumn, final BufferedDataContainer dc)
            throws CanceledExecutionException {
        LOGGER.debug("Entering createGroupByTable(exec, table) of class BigGroupByTable.");
        final var origSpec = table.getDataTableSpec();
        //sort the data table in order to process the input table chunk wise
        final CloseableRowIterator sortedTableIter;
        final ExecutionContext groupExec;
        final DataValueComparator[] comparators;
        if (groupColIdx.length < 1) {
            sortedTableIter = table.iterator();
            groupExec = exec;
            comparators = new DataValueComparator[0];
        } else {
            exec.setMessage("Sorting input table");
            final var sortCols = getGroupCols();
            final var sortOrder = new boolean[sortCols.size()];
            Arrays.fill(sortOrder, true);
            final var sortExec = exec.createSubExecutionContext(2 / 3.0);
            sortedTableIter = new BufferedDataTableSorter(table, sortCols, sortOrder).sortedIterator(sortExec);
            sortExec.setProgress(1.0);

            groupExec = exec.createSubExecutionContext(1 / 3.0);
            comparators = new DataValueComparator[groupColIdx.length];
            for (int i = 0, length = groupColIdx.length; i < length; i++) {
                final DataColumnSpec colSpec = origSpec.getColumnSpec(groupColIdx[i]);
                comparators[i] = colSpec.getType().getComparator();
            }
        }

        final var processedRows = new AtomicLong();
        final long tableSize = table.size();
        final var fractionBuilder = progressFractionBuilder(processedRows::get, tableSize);
        exec.setMessage(() -> fractionBuilder.apply(new StringBuilder("Creating groups (row ")).append(")").toString());

        final var groupIDCounter = new AtomicLong();
        var firstRow = true;
        // In the rare case that the DataCell comparator return 0 for two data cells that are not equal we have to
        // maintain a map with all rows with equal cells in the group columns per chunk. This variable stores for each
        // chunk these members. A chunk consists of rows which return 0 for the pairwise group value comparison.
        // Usually only equal data cells return 0 when compared with each other but in rare occasions also data cells
        // that are NOT equal return 0 when compared to each other (such as cells that contain chemical structures).
        // In this rare case this map will contain for each group of data cells that are pairwise equal in the chunk a
        // separate entry.
        final Map<GroupKey, GroupAggregate> chunkMembers = new LinkedHashMap<>(3);
        var logUnusualCells = true;
        var groupLabel = "";
        // cannot put init to the constructor, as the super() constructor directly calls the current function
        initMissingValuesMap();

        final var previousGroup = new DataCell[groupColIdx.length];
        final var currentGroup = new DataCell[groupColIdx.length];

        // map aggregated columns to aggregators
        final var aggToColIdx = Arrays.stream(getColAggregators()) //
                .mapToInt(agg -> origSpec.findColumnIndex(agg.getOriginalColName())) //
                .toArray();

        try (sortedTableIter) {
            while (sortedTableIter.hasNext()) {
                final DataRow row = sortedTableIter.next();
                //fetch the current group column values
                for (int i = 0, length = groupColIdx.length; i < length; i++) {
                    currentGroup[i] = row.getCell(groupColIdx[i]);
                }
                if (firstRow) {
                    groupLabel = createGroupLabelForProgress(currentGroup);
                    System.arraycopy(currentGroup, 0, previousGroup, 0, currentGroup.length);
                    firstRow = false;
                }
                // check if we are still in the same data chunk which contains rows that return 0 for all pairwise
                // comparisons of their group column data cells
                if (!sameChunk(comparators, previousGroup, currentGroup)) {
                    groupLabel = createGroupLabelForProgress(currentGroup);
                    addAggregateRows(dc, groupIDCounter::getAndIncrement, chunkMembers, appendRowCountColumn);
                    //set the current group as previous group
                    System.arraycopy(currentGroup, 0, previousGroup, 0, currentGroup.length);
                    if (logUnusualCells && chunkMembers.size() > 1) {
                        // log unusual number of chunk members with the classes that cause the problem
                        if (LOGGER.isEnabledFor(LEVEL.INFO)) {
                            final var sb = new StringBuilder();
                            sb.append("Data chunk with ");
                            sb.append(chunkMembers.size());
                            sb.append(" members occured in groupby node. Involved classes are: ");
                            final GroupKey key = chunkMembers.keySet().iterator().next();
                            for (final DataCell cell : key.getGroupVals()) {
                                sb.append(cell.getClass().getCanonicalName());
                                sb.append(", ");
                            }
                            LOGGER.info(sb.toString());
                        }
                        logUnusualCells = false;
                    }
                    chunkMembers.clear();
                }
                //process the row as one of the members of the current chunk
                GroupAggregate group = chunkMembers.get(new GroupKey(currentGroup));
                if (group == null) {
                    // first row of group encountered
                    group = new GroupAggregate(aggToColIdx, cloneColumnAggregators(), isEnableHilite(),
                        getGlobalSettings());
                    final var groupKeyValues = new DataCell[currentGroup.length];
                    System.arraycopy(currentGroup, 0, groupKeyValues, 0, currentGroup.length);
                    chunkMembers.put(new GroupKey(groupKeyValues), group);
                }
                // compute aggregates, group size, and hilite
                group.updateAggregates(row);

                groupExec.checkCanceled();
                final var processed = processedRows.incrementAndGet();
                groupExec.setProgress(1d * processed / tableSize, groupLabel);
            }
        }

        //create the final row for the last chunk after processing the last table row
        addAggregateRows(dc, groupIDCounter::getAndIncrement, chunkMembers, appendRowCountColumn);
        groupExec.setProgress(1.0, (String)null);
    }

    /**
     * Get a string describing the current group. Used in progress message.
     *
     * @param currentGroup The current group
     * @return That string.
     */
    private static String createGroupLabelForProgress(final DataCell[] currentGroup) {
        final var sb = new StringBuilder("(");
        for (var i = 0; i < currentGroup.length; i++) {
            sb.append(i > 0 ? "; " : "");
            if (i > 3) {
                sb.append("...");
            } else {
                sb.append('\"');
                var cellStr = currentGroup[i].toString();
                if (cellStr.length() > 31) {
                    cellStr = cellStr.substring(0, 30).concat("...");
                }
                sb.append(cellStr.replace('\n', '_')).append('\"');
            }
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * Creates and adds the result rows for the members of a data chunk to the given data container. It also handles the
     * row key mapping if hilite translation is enabled. Appends the size of the group as a row count column to the data
     * container if enabled.
     *
     * @param dc the {@link DataContainer} to put rows into
     * @param groupRowIDSupplier supplier for {@link RowKey RowIDs} for groups of the current data chunk
     * @param chunkMembers the members of the current data chunk
     * @param appendRowCountColumn the index
     */
    private void addAggregateRows(final BufferedDataContainer dc, final LongSupplier groupRowIDSupplier,
            final Map<GroupKey, GroupAggregate> chunkMembers, final boolean appendRowCountColumn) {
        if (chunkMembers == null || chunkMembers.isEmpty()) {
            return;
        }

        chunkMembers.forEach((groupKey, aggregates) -> {
            final var rowKey = RowKey.createRowKey(groupRowIDSupplier.getAsLong());
            dc.addRowToTable(createOutputRow(rowKey, groupKey, aggregates, appendRowCountColumn));
        });
    }

    /**
     * Returns <code>true</code> if both {@link DataCell} groups return 0 for each pairwise comparison of their elements
     * at the same position.
     *
     * @param comparators the {@link DataValueComparator}s to use
     * @param previousGroup the values of the previous group
     * @param currentGroup the values of the current group
     * @return <code>true</code> if both groups return 0 when all pairs are
     *        compared using the given {@link DataValueComparator}s
     */
    private static boolean sameChunk(final DataValueComparator[] comparators, final DataCell[] previousGroup,
            final DataCell[] currentGroup) {
        if (comparators == null || comparators.length < 1) {
            return true;
        }
        for (int i = 0, length = comparators.length; i < length; i++) {
            if (comparators[i].compare(previousGroup[i], currentGroup[i]) != 0) {
                return false;
            }
        }
        return true;
    }


    // TODO The following code is copied from `AbstractTableSorter` and should be moved to a better place

    /**
     * Creates a function that adds a nicely formatted, padded fraction of the form {@code " 173/2065"} to a given
     * {@link StringBuilder} that reflects the current value of the given supplier {@code currentValue}. The padding
     * with space characters tries to minimize jumping in the UI.
     *
     * @param currentValue supplier for the current numerator
     * @param total fixed denominator
     * @return function that modified the given {@link StringBuilder} and returns it for convenience
     */
    private static UnaryOperator<StringBuilder> progressFractionBuilder(final LongSupplier currentValue,
            final long total) {
        try {
            // only computed once
            final var numFormat = NumberFormatter.builder().setGroupSeparator(",").build();
            final var totalStr = numFormat.format(total);
            final var paddingStr = totalStr.replaceAll("\\d", "\u2007").replace(',', ' '); // NOSONAR

            return sb -> {
                // computed every time a progress message is requested
                final var currentStr = numFormat.format(currentValue.getAsLong());
                final var padding = paddingStr.substring(0, Math.max(totalStr.length() - currentStr.length(), 0));
                return sb.append(padding).append(currentStr).append("/").append(totalStr);
            };
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
