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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;

import org.knime.base.node.preproc.sorter.SorterNodeSettings.SortingCriterionSettings.SortingOrder;
import org.knime.base.node.preproc.sorter.SorterNodeSettings.SortingCriterionSettings.StringComparison;
import org.knime.base.node.preproc.sorter.dialog.DynamicSorterPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.data.sort.RowComparator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * This class implements the {@link NodeModel} for the sorter node. The input table is segmented into containers that
 * are sorted with guaranteed n*log(n) performance, based on a selection of columns and a corresponding order
 * (ascending/descending) and string comparator. In the end, all sorted containers are merged together and transformed
 * in a output datatable. To compare two datarows, the Comparator compares the {@link org.knime.core.data.DataCell}s
 * with their <code>compareTo</code>-method on each position.
 *
 * @see org.knime.core.data.container.DataContainer
 * @see java.util.Arrays#sort(java.lang.Object[], int, int, java.util.Comparator)
 * @author Nicolas Cebron, University of Konstanz
 */
public class SorterNodeModel extends WebUINodeModel<SorterNodeSettings> {
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
     *
     * @since 4.7
     */
    static final String ALPHANUMCOMP_KEY = "alphaNumStringComp";

    /**
     * The key for the memory-sort flag in the NodeSettings.
     */
    static final String SORTINMEMORY_KEY = "sortinmemory";

    /**
     * Settings key: Sort missings always to end.
     *
     * @since 2.6
     */
    static final String MISSING_TO_END_KEY = "missingToEnd";

    /**
     * Inits a new <code>SorterNodeModel</code> with one in- and one output.
     *
     */
    SorterNodeModel(final WebUINodeConfiguration config, final Class<SorterNodeSettings> modelSettingsClass) {
        super(config, modelSettingsClass);
    }

    /**
     * When the model gets executed, the {@link org.knime.core.data.DataTable} is split in several
     * {@link org.knime.core.data.container.DataContainer}s. Each one is first removed, then swapped back into memory,
     * gets sorted and is then removed again. At the end, all containers are merged together in one Result-Container.
     * The list of columns that shall be sorted and their corresponding sort order in a boolean array should be set,
     * before executing the model.
     *
     * @param inData the data table at the input port
     * @param exec the execution monitor
     * @return the sorted data table
     * @throws Exception when node is canceled or settings are invalid
     *
     * @see java.util.Arrays sort(java.lang.Object[], int, int, java.util.Comparator)
     * @see org.knime.core.node.NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final SorterNodeSettings modelSettings) throws Exception {
        CheckUtils.checkNotNull(modelSettings.m_sortingCriterions, "Sort key columns should not be null.");
        final var dataTable = inData[INPORT];
        // If no columns are set, we do not start the sorting process
        if (modelSettings.m_sortingCriterions.length == 0) {
            setWarningMessage("No columns were selected - returning original table");
            return new BufferedDataTable[]{dataTable};
        }
        final var dts = dataTable.getDataTableSpec();
        final var sorter = new BufferedDataTableSorter(dataTable, toRowComparator(dts, modelSettings));
        sorter.setSortInMemory(modelSettings.m_sortInMemory);
        final var sortedTable = sorter.sort(exec);
        return new BufferedDataTable[]{sortedTable};
    }

    private static RowComparator toRowComparator(final DataTableSpec spec, final SorterNodeSettings modelSettings) {
        final var rc = RowComparator.on(spec);
        Arrays.stream(modelSettings.m_sortingCriterions).forEach(criterion -> {
            final var ascending = criterion.m_sortingOrder == SortingOrder.ASCENDING;
            final var alphaNum = criterion.m_stringComparison == StringComparison.ALPHANUMERIC;
            resolveColumnName(spec, criterion.m_column.getSelected(), SorterNodeModel::isRowKey).ifPresentOrElse(
                col -> rc.thenComparingColumn(col,
                    c -> c.withDescendingSortOrder(!ascending).withAlphanumericComparison(alphaNum)
                        .withMissingsLast(modelSettings.m_sortMissingCellsToEndOfList)),
                () -> rc.thenComparingRowKey(
                    k -> k.withDescendingSortOrder(!ascending).withAlphanumericComparison(alphaNum)));
        });
        return rc.build();
    }

    private static OptionalInt resolveColumnName(final DataTableSpec dts, final String colName,
        final Predicate<String> isRowKey) {
        final var idx = dts.findColumnIndex(colName);
        if (idx == -1) {
            if (!isRowKey.test(colName)) {
                throw new IllegalArgumentException(
                    "The column identifier \"" + colName + "\" does not refer to a known column.");
            }
            return OptionalInt.empty();
        }
        return OptionalInt.of(idx);
    }

    private static boolean isRowKey(final String colName) {
        return DynamicSorterPanel.ROWKEY.getName().equals(colName);
    }

    /**
     * Check if the values of the include list also exist in the {@link DataTableSpec} at the inport. If everything is
     * ok, the v from the inport is translated without modification to the outport.
     *
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final SorterNodeSettings modelSettings)
        throws InvalidSettingsException {
        if (modelSettings.m_sortingCriterions.length == 0) {
            throw new InvalidSettingsException(
                "No selected columns to sort by. Add a sorting criterion in the node’s settings");
        }
        // check if the values of the include List
        // exist in the DataTableSpec
        final List<String> notAvailableCols = new ArrayList<>();
        final var spec = inSpecs[INPORT];
        for (var ic : modelSettings.m_sortingCriterions) {
            final var id = ic.m_column.getSelected();
            if (!isRowKey(id)) {
                final var idx = spec.findColumnIndex(id);
                if (idx == -1) {
                    notAvailableCols.add(id);
                } else if (ic.m_stringComparison == StringComparison.ALPHANUMERIC
                    && !spec.getColumnSpec(idx).getType().isCompatible(StringValue.class)) {
                    throw new InvalidSettingsException("Alphanumeric sorting is not available for '" + id
                        + "' since it does not have a string-compatible type.");
                }
            }
        }

        if (!notAvailableCols.isEmpty()) {
            throw new InvalidSettingsException("The input table has changed. Some columns are missing: "
                + ConvenienceMethods.getShortStringFrom(notAvailableCols, 3));
        }
        return new DataTableSpec[]{inSpecs[INPORT]};
    }

    /**
     * Exposes super method for tests
     */
    void performValidateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
    }

    /**
     * Exposes super method for tests
     */
    void performLoadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
    }

    /**
     * Exposes super method for tests
     */
    DataTableSpec[] performConfigure(final DataTableSpec[] specs) throws InvalidSettingsException {
        return super.configure(specs);
    }

    /**
     * Exposes super method for tests
     *
     * @throws Exception
     */
    BufferedDataTable[] performExecute(final BufferedDataTable[] inObjects, final ExecutionContext exec)
        throws Exception {
        return super.execute(inObjects, exec);
    }

    /**
     * Exposes super method for tests
     */
    void performReset() {
        super.reset();
    }

    /**
     * Exposes super method for tests
     */
    void performSaveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
    }

}
