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
 * History
 *   02.02.2005 (cebron): created
 */
package org.knime.base.node.preproc.sorter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.preproc.sorter.dialog.DynamicSorterPanel;
import org.knime.base.node.util.SortKeyItem;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * This class implements the {@link NodeModel} for the sorter node. The input
 * table is segmented into containers that are sorted with guaranteed n*log(n)
 * performance, based on a selection of columns and a corresponding order
 * (ascending/descending) and string comparator. In the end, all sorted containers are merged together
 * and transformed in a output datatable. To compare two datarows, the
 * Comparator compares the {@link org.knime.core.data.DataCell}s with their
 * <code>compareTo</code>-method on each position.
 *
 * @see org.knime.core.data.container.DataContainer
 * @see java.util.Arrays#sort(java.lang.Object[], int, int,
 *      java.util.Comparator)
 * @author Nicolas Cebron, University of Konstanz
 */
public class SorterNodeModel extends NodeModel {
    /**
     * The input port used here.
     */
    static final int INPORT = 0;

    /**
     * The output port used here.
     */
    static final int OUTPORT = 0;

    /**
     * The key for the IncludeList in the NodeSettings.
     */
    static final String INCLUDELIST_KEY = "incllist";

    /**
     * The key for the Sort Order Array in the NodeSettings.
     */
    static final String SORTORDER_KEY = "sortOrder";

    /**
     * The key for the Alphanumeric Comparison in the node settings.
     * @since 4.7
     */
    static final String ALPHANUMCOMP_KEY = "alphaNumStringComp";

    /**
     * The key for the memory-sort flag in the NodeSettings.
     */
    static final String SORTINMEMORY_KEY = "sortinmemory";

    /**
     * Settings key: Sort missings always to end.
     * @since 2.6
     */
    static final String MISSING_TO_END_KEY = "missingToEnd";

    /**
     * Sort key items to sort by. Includes with each column/row key the sort order (and comparison types for
     * string-compatible values).
     */
    private List<SortKeyItem> m_sortKeyColumns;

    /**
     * Flag indicating whether to perform the sorting in memory or not.
     */
    private boolean m_sortInMemory;

    /** Move missing values always to end (overwrites natural ordering according
     * to which they are the smallest item).
     * @since 2.6
     */
    private boolean m_missingToEnd;

    /**
     * Inits a new <code>SorterNodeModel</code> with one in- and one output.
     *
     */
    SorterNodeModel() {
        super(1, 1);
    }

    /**
     * When the model gets executed, the {@link org.knime.core.data.DataTable}
     * is split in several {@link org.knime.core.data.container.DataContainer}s.
     * Each one is first removed, then swapped back into memory, gets sorted and
     * is then removed again. At the end, all containers are merged together in
     * one Result-Container. The list of columns that shall be sorted and their
     * corresponding sort order in a boolean array should be set, before
     * executing the model.
     *
     * @param inData the data table at the input port
     * @param exec the execution monitor
     * @return the sorted data table
     * @throws Exception when node is canceled or settings are invalid
     *
     * @see java.util.Arrays sort(java.lang.Object[], int, int,
     *      java.util.Comparator)
     * @see org.knime.core.node.NodeModel#execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        CheckUtils.checkNotNull(m_sortKeyColumns, "Sort key columns should not be null.");
        final var dataTable = inData[INPORT];
        // If no columns are set, we do not start the sorting process
        if (m_sortKeyColumns.isEmpty()) {
            setWarningMessage("No columns were selected - returning original table");
            return new BufferedDataTable[]{dataTable};
        }
        final var dts = dataTable.getDataTableSpec();
        final var sorter = new BufferedDataTableSorter(dataTable,
            SortKeyItem.toRowComparator(dts, m_sortKeyColumns, m_missingToEnd, SorterNodeModel::isRowKey));
        sorter.setSortInMemory(m_sortInMemory);
        final var sortedTable = sorter.sort(exec);
        return new BufferedDataTable[]{sortedTable};
    }

    private static boolean isRowKey(final String colName) {
        return DynamicSorterPanel.ROWKEY.getName().equals(colName);
    }

    @Override
    public void reset() {

        // do nothing yet
    }

    /**
     * Check if the values of the include list also exist in the
     * {@link DataTableSpec} at the inport. If everything is ok, the v from the
     * inport is translated without modification to the outport.
     *
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_sortKeyColumns == null) {
            throw new InvalidSettingsException("No selected columns to sort");
        }
        // check if the values of the include List
        // exist in the DataTableSpec
        final List<String> notAvailableCols = new ArrayList<>();
        final var spec = inSpecs[INPORT];
        for (var ic : m_sortKeyColumns) {
            final var id = ic.getIdentifier();
            if (!isRowKey(id)) {
                final var idx = spec.findColumnIndex(id);
                if (idx == -1) {
                    notAvailableCols.add(id);
                } else if (ic.isAlphaNumComp() && !spec.getColumnSpec(idx).getType().isCompatible(StringValue.class)) {
                    throw new InvalidSettingsException(
                        "Alphanumeric sorting is not available for '" + id + "' since it does not have a "
                            + "string-compatible type.");
                }
            }
        }

        if (!notAvailableCols.isEmpty()) {
            throw new InvalidSettingsException("The input table has "
               + "changed. Some columns are missing: "
               + ConvenienceMethods.getShortStringFrom(notAvailableCols, 3));
        }
        return new DataTableSpec[]{inSpecs[INPORT]};
    }

    /**
     * The list of included columns and their sort order are stored in the
     * settings.
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_sortKeyColumns != null) {
            SortKeyItem.saveTo(m_sortKeyColumns, INCLUDELIST_KEY, SORTORDER_KEY, ALPHANUMCOMP_KEY, settings);
        }
        settings.addBoolean(SORTINMEMORY_KEY, m_sortInMemory);
        // added in 2.6
        settings.addBoolean(MISSING_TO_END_KEY, m_missingToEnd);
    }

    /**
     * Valid settings should contain the list of columns and a corresponding
     * sort order array of same size.
     *
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SortKeyItem.validate(INCLUDELIST_KEY, SORTORDER_KEY, ALPHANUMCOMP_KEY, settings);
        // no "missingToBottom" prior 2.6
    }

    /**
     * Load the settings (includelist and sort order) in the SorterNodeModel.
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        m_sortKeyColumns = SortKeyItem.loadFrom(INCLUDELIST_KEY, SORTORDER_KEY, ALPHANUMCOMP_KEY, settings);

        if (settings.containsKey(SORTINMEMORY_KEY)) {
            m_sortInMemory = settings.getBoolean(SORTINMEMORY_KEY);
        }
        // added in 2.6, catch missing setting
        m_missingToEnd = settings.getBoolean(MISSING_TO_END_KEY, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty.
    }
}
