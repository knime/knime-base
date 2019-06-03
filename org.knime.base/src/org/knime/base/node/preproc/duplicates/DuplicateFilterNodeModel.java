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
 *   May 21, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.duplicates;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.knime.base.node.preproc.duplicates.DuplicateFilterSettings.RowSelectionType;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.util.UniqueNameGenerator;

/**
 * This node allows to identify duplicates respective a given set of columns and selecting whether duplicated rows shall
 * be kept, or annotated by either their class \{unique, chosen, duplicate\} / the representative's RowID.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class DuplicateFilterNodeModel extends NodeModel {

    /** The data inport index. */
    static final int DATA_IN_PORT = 0;

    /** The unique row identifier. */
    static final StringCell UNIQUE_IDENTIFIER = new StringCell("unique");

    /** The chosen row identifier. */
    static final StringCell CHOSEN_IDENTIFIER = new StringCell("chosen");

    /** The duplicate row identifier. */
    static final StringCell DUPLICATE_IDENTIFIER = new StringCell("duplicate");

    /** The suggested name of the columns used to retain the row order. */
    private static final String ORDER_COL_NAME_SUGGESTION = "orig-order";

    /** The settings. */
    private final DuplicateFilterSettings m_settings = new DuplicateFilterSettings();

    /**
     * Constructor.
     */
    DuplicateFilterNodeModel() {
        super(1, 1);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable data = inData[DATA_IN_PORT];

        if (data.size() == 0) {
            final BufferedDataContainer cont = exec.createDataContainer(createOutSpec(data.getDataTableSpec()));
            cont.close();
            return new BufferedDataTable[]{cont.getTable()};
        }

        // create a unique name for the columns used to retain the row order
        final String orderColName =
            DataTableSpec.getUniqueColumnName(data.getDataTableSpec(), ORDER_COL_NAME_SUGGESTION);
        // sort the table according to the selected group columns
        final String[] grpCols = m_settings.getGroupCols(data.getDataTableSpec()).getIncludes();

        // append the row order column if required
        final boolean hasOrderCol;
        final ExecutionContext mainContext;
        if (m_settings.retainOrder() || m_settings.getRowSelectionType() == RowSelectionType.LAST) {
            hasOrderCol = true;
            mainContext = exec.createSubExecutionContext(0.95);
            data = addOrderColumn(exec.createSubExecutionContext(0.05), data, orderColName);
        } else {
            hasOrderCol = false;
            mainContext = exec;
        }

        final ExecutionContext sortContext = mainContext.createSubExecutionContext(0.9);
        final ExecutionContext duplicatesContext = mainContext.createSubExecutionContext(0.1);

        final BufferedDataTable sortedTbl = sortTable(data,
            m_settings.retainOrder() ? sortContext.createSubExecutionContext(0.5) : sortContext, grpCols, orderColName);

        // removed / flag duplicates
        if (m_settings.removeDuplicates()) {
            data = removeDuplicates(duplicatesContext, grpCols, sortedTbl);
        } else {
            data = appendColumns(duplicatesContext, grpCols, sortedTbl);
        }

        // retain the row order
        if (m_settings.retainOrder()) {
            final BufferedDataTableSorter sorter =
                new BufferedDataTableSorter(data, Collections.singletonList(orderColName), new boolean[]{true}, false);
            sorter.setSortInMemory(m_settings.inMemory());
            data = sorter.sort(sortContext.createSubExecutionContext(0.5));
        }

        // remove the ordering column if required
        if (hasOrderCol) {
            final ColumnRearranger cR = new ColumnRearranger(data.getDataTableSpec());
            cR.remove(orderColName);
            // Note: deleting a columns does not set any progress
            data = exec.createColumnRearrangeTable(data, cR, exec);
        }

        return new BufferedDataTable[]{data};
    }

    /**
     * Adds a column used to later on retain the original row order.
     *
     * @param exec the execution context
     * @param data the data
     * @param orderColName the name of the column to append
     * @return the table containing the additional ordering column
     * @throws CanceledExecutionException - If the execution has been canceled
     */
    private static BufferedDataTable addOrderColumn(final ExecutionContext exec, BufferedDataTable data,
        final String orderColName) throws CanceledExecutionException {
        final ColumnRearranger cR = new ColumnRearranger(data.getDataTableSpec());
        cR.append(new SingleCellFactory(false, new DataColumnSpecCreator(orderColName, LongCell.TYPE).createSpec()) {

            long idx = Long.MIN_VALUE;

            @Override
            public DataCell getCell(final DataRow row) {
                return new LongCell(idx++);
            }
        });

        data = exec.createColumnRearrangeTable(data, cR, exec);
        return data;
    }

    private BufferedDataTable sortTable(final BufferedDataTable inData, final ExecutionContext exec,
        final String[] grpCols, final String orderColName) throws CanceledExecutionException {
        // append additional sorting requirements if required
        final RowSelectionType rowSelectionType = m_settings.getRowSelectionType();
        final String[] sortCols;
        if (rowSelectionType.supportsRefCol()) {
            sortCols = Arrays.copyOf(grpCols, grpCols.length + 1);
            sortCols[grpCols.length] = m_settings.getReferenceCol();
        } else if (rowSelectionType == RowSelectionType.LAST) {
            sortCols = Arrays.copyOf(grpCols, grpCols.length + 1);
            sortCols[grpCols.length] = orderColName;
        } else {
            sortCols = grpCols;
        }

        // set the sorting ordering according
        final boolean[] sortAsc = new boolean[sortCols.length];
        Arrays.fill(sortAsc, true);
        if (rowSelectionType == RowSelectionType.MAXIMUM || rowSelectionType == RowSelectionType.LAST) {
            sortAsc[grpCols.length] = false;
        }

        // return the sorted table
        BufferedDataTableSorter sorter =
            new BufferedDataTableSorter(inData, Arrays.asList(sortCols), sortAsc, m_settings.skipMissings());
        sorter.setSortInMemory(m_settings.inMemory());
        final BufferedDataTable sortedTbl = sorter.sort(exec);
        return sortedTbl;
    }

    private static BufferedDataTable removeDuplicates(final ExecutionContext exec, final String[] grpCols,
        final BufferedDataTable sortedTbl) throws CanceledExecutionException {
        final BufferedDataContainer cont = exec.createDataContainer(sortedTbl.getDataTableSpec());
        final int[] grpIndices = sortedTbl.getDataTableSpec().columnsToIndices(grpCols);
        double rowCnt = 0;
        final long nRows = sortedTbl.size();
        try (final CloseableRowIterator iterator = sortedTbl.iterator()) {
            exec.checkCanceled();
            // maintain pointer to the first row within each group
            DataRow firstRowInGrp = iterator.next();
            exec.setProgress(++rowCnt / nRows);
            while (iterator.hasNext()) {
                final DataRow curRow = iterator.next();
                // if we witness a new group write the firstRowInGrp and reset it
                if (isDifferentGroup(grpIndices, firstRowInGrp, curRow)) {
                    cont.addRowToTable(firstRowInGrp);
                    firstRowInGrp = curRow;
                }
                exec.setProgress(++rowCnt / nRows);
            }
            cont.addRowToTable(firstRowInGrp);
        }
        cont.close();
        return cont.getTable();
    }

    private static boolean isDifferentGroup(final int[] grpIndices, final DataRow prevRow, final DataRow curRow) {
        return Arrays.stream(grpIndices).anyMatch(i -> !prevRow.getCell(i).equals(curRow.getCell(i)));
    }

    private BufferedDataTable appendColumns(final ExecutionContext exec, final String[] grpCols,
        final BufferedDataTable sortedTbl) throws CanceledExecutionException {
        final int[] grpIndices = sortedTbl.getDataTableSpec().columnsToIndices(grpCols);
        final BufferedDataContainer cont =
            exec.createDataContainer(createAdditionalColsSpec(sortedTbl.getDataTableSpec()));
        double rowCnt = 0;
        final long nRows = sortedTbl.size();
        DataCell referenceKey = DataType.getMissingCell();
        try (final CloseableRowIterator iterator = sortedTbl.iterator()) {
            boolean isFirstRowInGrp = true;
            exec.checkCanceled();
            DataRow curRow = iterator.next();
            exec.setProgress(++rowCnt / nRows);
            DataRow nextRow;
            do {
                // break the loop
                if (!iterator.hasNext()) {
                    nextRow = null;
                    continue;
                }
                // get the next row and tests if it belongs to the same group as the current one
                nextRow = iterator.next();
                final boolean diffGroup = isDifferentGroup(grpIndices, curRow, nextRow);
                final DataRow row;
                // if cur row is not the first in the group it must be a duplicate
                if (!isFirstRowInGrp) {
                    row = createRow(curRow, DUPLICATE_IDENTIFIER, referenceKey);
                    if (diffGroup) {
                        isFirstRowInGrp = true;
                    }
                } else {
                    // if cur row isFirstRowInGrp and nextEntry belongs to a different group curRow is unique
                    referenceKey = DataType.getMissingCell();
                    if (diffGroup) {
                        row = createRow(curRow, UNIQUE_IDENTIFIER, referenceKey);
                    } else {
                        row = createRow(curRow, CHOSEN_IDENTIFIER, referenceKey);
                        referenceKey = new StringCell(curRow.getKey().getString());
                        isFirstRowInGrp = false;
                    }
                }
                cont.addRowToTable(row);
                exec.setProgress(++rowCnt / nRows);
                curRow = nextRow;
            } while (nextRow != null);
            // add the last row
            if (isFirstRowInGrp) {
                cont.addRowToTable(createRow(curRow, UNIQUE_IDENTIFIER, DataType.getMissingCell()));
            } else {
                cont.addRowToTable(createRow(curRow, DUPLICATE_IDENTIFIER, referenceKey));
            }
        }
        cont.close();
        return exec.createJoinedTable(sortedTbl, cont.getTable(), exec);
    }

    private DataRow createRow(final DataRow curRow, final StringCell label, final DataCell referenceKey) {
        if (m_settings.addUniqueLbl() && m_settings.addRowLbl()) {
            return new DefaultRow(curRow.getKey(), label, referenceKey);
        } else if (m_settings.addUniqueLbl()) {
            return new DefaultRow(curRow.getKey(), label);
        } else {
            return new DefaultRow(curRow.getKey(), referenceKey);
        }
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = inSpecs[DATA_IN_PORT];
        final FilterResult groupCols = m_settings.getGroupCols(inSpec);
        CheckUtils.checkSetting(groupCols.getIncludes().length != 0,
            "At least one column has to be selected for duplicate detection.");
        if (m_settings.getRowSelectionType().supportsRefCol()) {
            final String refCol = m_settings.getReferenceCol();
            CheckUtils.checkSetting(refCol != null && !refCol.isEmpty(), "The selected tie-break ("
                + m_settings.getRowSelectionType() + ") requires that a reference column is selected.");
            CheckUtils.checkSetting(inSpec.findColumnIndex(refCol) != -1,
                "The input table does not contain the selected reference column \"" + refCol + "\".");
            CheckUtils.checkSetting(Arrays.stream(groupCols.getIncludes()).noneMatch(grpCol -> refCol.equals(grpCol)),
                "The selected reference column is also used for duplicate detection.");
        }
        return new DataTableSpec[]{createOutSpec(inSpec)};
    }

    DataTableSpec createOutSpec(final DataTableSpec inSpec) {
        if (m_settings.removeDuplicates()) {
            return inSpec;
        }
        return new DataTableSpec(inSpec, createAdditionalColsSpec(inSpec));
    }

    private DataTableSpec createAdditionalColsSpec(final DataTableSpec inSpec) {
        final UniqueNameGenerator uniqueNameGen = new UniqueNameGenerator(inSpec);
        final List<DataColumnSpec> addCols = new ArrayList<>();
        if (m_settings.addUniqueLbl()) {
            addCols.add(uniqueNameGen.newColumn("duplicate-type-classifier", StringCell.TYPE));
        }
        if (m_settings.addRowLbl()) {
            addCols.add(uniqueNameGen.newColumn("duplicate-row-identifier", StringCell.TYPE));
        }
        return new DataTableSpec(addCols.stream().toArray(DataColumnSpec[]::new));
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadValidatedSettingsFrom(settings);
    }

    @Override
    protected void reset() {
    }

}
