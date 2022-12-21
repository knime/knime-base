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
import java.util.Map.Entry;

import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class MemoryGroupByTable extends GroupByTable {

    private Map<GroupKey, GroupAggregate> m_groups;

    /**Constructor for class MemoryGroupByTable.
     * @param exec the <code>ExecutionContext</code>
     * @param inDataTable the table to aggregate
     * @param groupByCols the name of all columns to group by
     * @param colAggregators the aggregation columns with the aggregation method
     * to use in the order the columns should be appear in the result table
     * numerical columns
     * @param countColumnName name of the group row count column or {@code null} if counts should not be added
     * @param globalSettings the global settings
     * @param enableHilite <code>true</code> if a row key map should be
     * maintained to enable hiliting
     * @param colNamePolicy the {@link ColumnNamePolicy} for the
     * aggregation columns
     * input table if set to <code>true</code>
     * @param retainOrder <code>true</code> if the original row order should be
     * retained
     * @throws CanceledExecutionException if the user has canceled the execution
     * @since 5.0
     */
    public MemoryGroupByTable(final ExecutionContext exec,
            final BufferedDataTable inDataTable, final List<String> groupByCols,
            final ColumnAggregator[] colAggregators,
            final String countColumnName,
            final GlobalSettings globalSettings,
            final boolean enableHilite, final ColumnNamePolicy colNamePolicy,
            final boolean retainOrder)
            throws CanceledExecutionException {
        super(exec, inDataTable, groupByCols, colAggregators, countColumnName, globalSettings, enableHilite,
            colNamePolicy, false);
    }

    /**Constructor for class MemoryGroupByTable.
     * @param exec the <code>ExecutionContext</code>
     * @param inDataTable the table to aggregate
     * @param groupByCols the name of all columns to group by
     * @param colAggregators the aggregation columns with the aggregation method
     * to use in the order the columns should be appear in the result table
     * numerical columns
     * @param globalSettings the global settings
     * @param enableHilite <code>true</code> if a row key map should be
     * maintained to enable hiliting
     * @param colNamePolicy the {@link ColumnNamePolicy} for the
     * aggregation columns
     * input table if set to <code>true</code>
     * @param retainOrder <code>true</code> if the original row order should be
     * retained
     * @throws CanceledExecutionException if the user has canceled the execution
     * @since 3.2
     * @deprecated added option for "COUNT-*"-style aggregation with {@code countColumnName} constructor
     * @see #MemoryGroupByTable(ExecutionContext, BufferedDataTable, List, ColumnAggregator[], String, GlobalSettings,
     *        boolean, ColumnNamePolicy, boolean)
     */
    @Deprecated
    public MemoryGroupByTable(final ExecutionContext exec,
            final BufferedDataTable inDataTable, final List<String> groupByCols,
            final ColumnAggregator[] colAggregators,
            final GlobalSettings globalSettings,
            final boolean enableHilite, final ColumnNamePolicy colNamePolicy,
            final boolean retainOrder)
            throws CanceledExecutionException {
        //retainOrder is always false since it is automatically maintained
        //in this class by the chosen Map implementation
        super(exec, inDataTable, groupByCols, colAggregators, null, globalSettings,
                enableHilite, colNamePolicy, false);
    }

    /**Constructor for class MemoryGroupByTable.
     * @param exec the <code>ExecutionContext</code>
     * @param inDataTable the table to aggregate
     * @param groupByCols the name of all columns to group by
     * @param colAggregators the aggregation columns with the aggregation method
     * to use in the order the columns should be appear in the result table
     * numerical columns
     * @param countColumnName name of the group row count column or {@code null} if counts should not be added
     * @param globalSettings the global settings
     * @param sortInMemory <code>true</code> if the table should be sorted in
     * the memory
     * @param enableHilite <code>true</code> if a row key map should be
     * maintained to enable hiliting
     * @param colNamePolicy the {@link ColumnNamePolicy} for the
     * aggregation columns
     * input table if set to <code>true</code>
     * @param retainOrder <code>true</code> if the original row order should be
     * retained
     * @throws CanceledExecutionException if the user has canceled the execution
     * @since 5.0
     * @deprecated sortInMemory object is no longer required
     * @see #MemoryGroupByTable(ExecutionContext, BufferedDataTable, List,
     * ColumnAggregator[], GlobalSettings, boolean, ColumnNamePolicy, boolean)
     */
    @Deprecated
    protected MemoryGroupByTable(final ExecutionContext exec,
            final BufferedDataTable inDataTable, final List<String> groupByCols,
            final ColumnAggregator[] colAggregators,
            final String countColumnName,
            final GlobalSettings globalSettings, final boolean sortInMemory,
            final boolean enableHilite, final ColumnNamePolicy colNamePolicy,
            final boolean retainOrder)
            throws CanceledExecutionException {
        //retainOrder is always false since it is automatically maintained
        //in this class by the chosen Map implementation
        super(exec, inDataTable, groupByCols, colAggregators, countColumnName, globalSettings,
                sortInMemory, enableHilite, colNamePolicy, false);
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.0
     */
    @Override
    protected void createGroupByTable(final ExecutionContext exec,
            final BufferedDataTable dataTable, final int[] groupColIdx, final boolean appendRowCountColumn,
            final BufferedDataContainer dc) throws CanceledExecutionException {
        m_groups = new LinkedHashMap<>();
        final var groupMaxProg = 0.7;
        final var outputMaxProg = 1.0 - groupMaxProg;

        final ExecutionMonitor groupExec = exec.createSubProgress(groupMaxProg);
        final var spec = dataTable.getDataTableSpec();
        final long rowCount = dataTable.size();
        long processedRows = 0;
        initMissingValuesMap();

        // map aggregated columns to aggregators
        final var aggToColIdx = Arrays.stream(getColAggregators())
                .mapToInt(agg -> spec.findColumnIndex(agg.getOriginalColName()))
                .toArray();

        for (final DataRow row : dataTable) {
            groupExec.checkCanceled();
            processedRows++;
            groupExec.setProgress(processedRows / (double) rowCount, String.format("Analyzing row %d of %d",
                processedRows, rowCount));
            final var currentGroup = new DataCell[groupColIdx.length];
            //fetch the current group column values
            for (int i = 0, length = groupColIdx.length; i < length; i++) {
                currentGroup[i] = row.getCell(groupColIdx[i]);
            }
            final var groupKey = new GroupKey(currentGroup);

            final var group = m_groups.computeIfAbsent(groupKey, k -> new GroupAggregate(aggToColIdx,
                cloneColumnAggregators(), isEnableHilite(), getGlobalSettings()));

            // compute aggregates, group size, and hilite
            group.updateAggregates(row);
        }

        addAllAggregateRows(exec.createSubExecutionContext(outputMaxProg), appendRowCountColumn, dc);
    }

    private void addAllAggregateRows(final ExecutionContext exec, final boolean appendRowCountColumn,
        final BufferedDataContainer dc) throws CanceledExecutionException {
        var groupCounter = 0L;
        final int size = m_groups.size();
        for (final Entry<GroupKey, GroupAggregate> entry : m_groups.entrySet()) {
            final var groupByKey = entry.getKey();
            final var groupMembers = entry.getValue();
            exec.checkCanceled();
            exec.setProgress(groupCounter / (double)size, "Writing group " + groupCounter + " of " + size);
            final var chunkKey = RowKey.createRowKey(groupCounter);
            final var row = createOutputRow(chunkKey, groupByKey, groupMembers, appendRowCountColumn);
            dc.addRowToTable(row);
            groupCounter++;
        }
    }
}
