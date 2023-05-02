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
 *   21 Apr 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.util.duplicates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.knime.base.node.preproc.pivot.Pivot2NodeModel;
import org.knime.base.node.util.SortKeyItem;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.data.sort.RowComparator;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;

/**
 * Identify duplicate rows with regards to a set of columns.
 *
 * Mostly extracted from {@code DuplicateRowFilterNodeModel} for use in {@link Pivot2NodeModel}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class DuplicateRowMarker {

    /** The unique row identifier. */
    static final StringCell UNIQUE_IDENTIFIER = new StringCell("unique");

    /** The chosen row identifier. */
    static final StringCell CHOSEN_IDENTIFIER = new StringCell("chosen");

    /** The duplicate row identifier. */
    static final StringCell DUPLICATE_IDENTIFIER = new StringCell("duplicate");

    /** The suggested name of the columns used to retain the row order. */
    private static final String ORDER_COL_NAME_SUGGESTION = "orig-order";

    private final boolean m_retainOrder;
    private final boolean m_dropDuplicates;
    private final boolean m_chooseFirst;
    private final boolean m_updateDomains;
    private final boolean m_addUniqueLabel;
    private final boolean m_addRowLabel;

    /**
     * Creates a duplicate row marker.
     *
     * @param retainOrder {@code true} if the order of rows should be retained, where the first occurrence of a
     *            duplicate in the input determines the order to be retained, {@code false} otherwise
     * @param dropDuplicates {@code true} if duplicates should be dropped, {@code false} if they should be flagged
     * @param chooseFirst {@code true} if the first of each list of duplicate rows should be chosen,
     *            {@code false} if the last should be chosen
     * @param updateDomains {@code true} if domains should be updated, {@code false} otherwise
     * @param addUniqueLabel {@code true} if a "unique" label should be put on rows which do not have a duplicate in
     *            the input, {@code false} otherwise
     * @param addRowLabel {@code true} if each row should get a unique row label, {@code false} otherwise
     */
    public DuplicateRowMarker(final boolean retainOrder,
            final boolean dropDuplicates, final boolean chooseFirst, final boolean updateDomains,
            final boolean addUniqueLabel, final boolean addRowLabel) {
        m_retainOrder = retainOrder;
        m_dropDuplicates = dropDuplicates;
        m_chooseFirst = chooseFirst;
        m_updateDomains = updateDomains;
        m_addUniqueLabel = addUniqueLabel;
        m_addRowLabel = addRowLabel;
    }

    /**
     * Runs the duplicate marker on the given table and returns a table as configured by the constructor.
     *
     * The marker will always choose the first row among a list of duplicate rows.
     * The given sort key can influence which duplicate is first in its list and will subsequently be chosen.
     * For example, the sort key can be used to select a chosen duplicate based on some maximum/minimum value of
     * a column.
     *
     * @param inData table to mark duplicates on
     * @param dupColumns columns used for detecting whether two rows are duplicates of each other
     * @param sortKey sort key to influence which of the rows among duplicates are chosen and which are flagged as
     *            duplicates or removed
     * @param inExec execution context
     * @return table with duplicates marked or removed
     * @throws CanceledExecutionException if the execution was canceled
     */
    public BufferedDataTable run(final BufferedDataTable inData, final String[] dupColumns, final SortKeyItem[] sortKey,
            final ExecutionContext inExec) throws CanceledExecutionException {
        final String inputOrderColName;

        final BufferedDataTable inputData;
        ExecutionContext mainExec;
        if (m_retainOrder) {
            inputOrderColName = new UniqueNameGenerator(inData.getSpec()).newName(ORDER_COL_NAME_SUGGESTION);
            mainExec = inExec.createSubExecutionContext(0.95);
            inputData = addInputOrderColumn(inExec.createSubExecutionContext(0.05), inData, inputOrderColName);
        } else {
            inputOrderColName = null;
            mainExec = inExec;
            inputData = inData;
        }
        ExecutionContext domainsExec = null;
        if (m_updateDomains) {
            domainsExec = mainExec.createSubExecutionContext(0.3);
            mainExec = mainExec.createSubExecutionContext(0.7);
        }

        final var retainInputOrder = inputOrderColName != null;

        final var sortExec = mainExec.createSubExecutionContext(0.9);
        final var dedupExec = mainExec.createSubExecutionContext(0.1);

        var dedup = sortMarkDuplicates(inputData, DuplicateEliminationComparator.of(inputData.getSpec(), dupColumns,
            sortKey, m_chooseFirst), retainInputOrder ? sortExec.createSubExecutionContext(0.5) : sortExec, dedupExec);

        // recreate input row order
        if (retainInputOrder) {
            final var spec = dedup.getSpec();
            final var orderIdx = spec.lookupColumnIndex(inputOrderColName).orElseThrow();
            final var cmp = RowComparator.on(spec).thenComparingColumn(orderIdx, UnaryOperator.identity()).build();
            final var sorter = new BufferedDataTableSorter(dedup, cmp);
            dedup = sorter.sort(mainExec.createSubExecutionContext(0.5));
            // remove ordering column
            final var cR = new ColumnRearranger(dedup.getSpec());
            cR.remove(inputOrderColName);
            // Note: deleting a column does not set any progress
            dedup = mainExec.createColumnRearrangeTable(dedup, cR, mainExec);
        }
        if (m_updateDomains) {
            dedup = updateDomain(dedup, domainsExec);
        }
        return dedup;
    }

    /**
     * Comparator used for duplicate elimination.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    private static final class DuplicateEliminationComparator {

        final String[] m_dupColumns;
        final Comparator<DataRow> m_comparator;

        private DuplicateEliminationComparator(final String[] dupColumns, final Comparator<DataRow> comp) {
            m_dupColumns = dupColumns;
            m_comparator = comp;
        }

        /**
         * Creates a new duplicate elimination comparator.
         *
         * The sort key can contain columns which are not among the columns used for duplicate detection,
         * in order to influence the order among duplicate rows.
         *
         * @param spec data table spec
         * @param dupColumns columns to consider for duplicate elimination
         * @param sortKey sort key to change order of each set of duplicate rows
         * @return comparator for duplicate elimination
         */
        static DuplicateEliminationComparator of(final DataTableSpec spec, final String[] dupColumns,
                final SortKeyItem[] sortKey, final boolean chooseFirst) {
            final String[] columns;
            if (dupColumns != null) {
                columns = dupColumns;
            } else {
                columns = spec.getColumnNames();
            }
            final Comparator<DataRow> comp;
            if (sortKey != null) {
                checkIsPrefixOfSortKey(columns, sortKey);
                comp = SortKeyItem.toRowComparator(spec, Arrays.asList(sortKey), true, colName -> false);
            } else {
                final var rcb = RowComparator.on(spec);
                final var colIndices = spec.columnsToIndices(columns);
                for (final var idx : colIndices) {
                    rcb.thenComparingColumn(idx, c -> c.withDescendingSortOrder(false).withMissingsLast());
                }
                comp = rcb.build();
            }

            return new DuplicateEliminationComparator(columns, chooseFirst ? comp : comp.reversed());
        }

        private static void checkIsPrefixOfSortKey(final String[] columns, final SortKeyItem[] sortKey)
                throws IllegalArgumentException {
            // sortKey must be non-null; otherwise we would not need to check, since we built the sort key ourselves
            // columns must be non-null, since we fall back to the whole list of columns in the spec in case the user
            // does not pass columns that are used for checking duplicates
            final Supplier<String> msg = () -> {
                final var sb = new StringBuilder();
                sb.append("List of columns used for duplicate detection contains more "
                    + "entries than sort key. Extra columns: ");
                return appendRangeAbbrev(sb, columns, sortKey.length, 3).toString();
            };
            CheckUtils.check(columns.length <= sortKey.length, IllegalArgumentException::new, msg);

            for (var i = 0; i < columns.length; i++) {
                final var col = columns[i];
                final var keyCol = sortKey[i];
                CheckUtils.checkArgument(col.equals(keyCol.getIdentifier()),
                    "Duplicate columns are not a prefix of the sort key. They differ at position "
                    + "%d: duplicate column=\"%s\", key column=\"%s\"".formatted(i + 1, col, keyCol));
            }
        }

        /**
         * Append the entries of the given array starting at the given offset, appending at most the given number of
         * entries.
         *
         * @param sb string builder to append to
         * @param entries entries to append from
         * @param fromIncl
         * @param abbr
         * @return
         */
        private static <T> StringBuilder appendRangeAbbrev(final StringBuilder sb, final T[] entries, final int offset,
                final int abbr) {
            if (offset >= entries.length) {
                throw new IllegalArgumentException("Offset \"%d\" outside given array".formatted(offset));
            }
            final int toExcl;
            final String right;
            final var diff = entries.length - offset;
            if (diff <= abbr) {
                toExcl = entries.length;
                right = "]";
            } else {
                toExcl = offset + diff;
                right = ", ...]";
            }
            return appendRange(sb, entries, offset, toExcl, "[", right, ", ");
        }

        /**
         * Appends the entries in the given range to the string builder, enclosed in left and right strings and
         * delimited by the given delimiter.
         *
         * The list of entries is possibly abbreviated as early as specified by {@code abbreviateAfter}.
         * The output is not abbreviated, for example, if the abbreviation would make the resulting string longer
         * than including the last missing column.
         *
         * @param sb string builder to append to
         * @param entries entries to append
         * @param fromIncl inclusive starting index
         * @param toExcl exclusive end index
         * @param left left enclosing string
         * @param right right enclosing string
         * @param delim entry delimiter
         * @return given string builder
         */
        private static <T> StringBuilder appendRange(final StringBuilder sb, final T[] entries, final int fromIncl,
                final int toExcl, final String left, final String right, final String delim) {
            Objects.requireNonNull(entries);
            CheckUtils.checkArgument(fromIncl < toExcl, "Start index must be before end index.");
            CheckUtils.checkArgument(fromIncl >= 0, "Start index must be non-negative.");
            CheckUtils.checkArgument(toExcl <= entries.length, "End index must not exceed length of array.");
            sb.append(left);
            for (int i = fromIncl; i < toExcl; i++) {
                sb.append(entries[i]);
                if (i != (toExcl - 1)) {
                    sb.append(delim);
                }
            }
            return sb.append(right);
        }
    }

    /**
     * Sorts rows in the given table such that duplicates are next to each other in the output table, possibly by
     * considering only the given column names for duplicate detection.
     * The chosen row will be the first row in each list of duplicate rows.
     *
     * The optional sort key can be used to influence which row of a list of duplicates will be first.
     * <b>Note:</b> If a non-null sort key is given, the list of columns to identify duplicates with must be a prefix
     * of the columns comprising the sort key. In particular, if the column list is null but the sort key is non-null,
     * the list of columns in the data table spec must be a prefix of the columns comprising the sort key.
     *
     * @param data data table to mark duplicates in
     * @param dupColumns optional list of columns to consider for duplicate detection
     * @param sortKey optional sort key to influence which row among a list of duplicate rows is the first row
     * @param sortExec execution context to report progress on for sorting
     * @param dedupExec execution context to report progress on for deduplicating/flagging duplicates
     * @return data table with duplicates identified
     * @throws CanceledExecutionException when execution was canceled
     */
    private BufferedDataTable sortMarkDuplicates(final BufferedDataTable data, final DuplicateEliminationComparator dec,
            final ExecutionContext sortExec, final ExecutionContext dedupExec)
            throws CanceledExecutionException {
        final var sorter = new BufferedDataTableSorter(data, dec.m_comparator);
        final var sortedTable = sorter.sort(sortExec);
        if (m_dropDuplicates) {
            return removeDuplicates(dedupExec, dec.m_dupColumns, sortedTable);
        }
        return appendColumns(dedupExec, dec.m_dupColumns, sortedTable);
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
    private static BufferedDataTable addInputOrderColumn(final ExecutionContext exec,
            final BufferedDataTable data, final String orderColName)
            throws CanceledExecutionException {
        final var cR = new ColumnRearranger(data.getSpec());
        cR.append(new SingleCellFactory(false, new DataColumnSpecCreator(orderColName, LongCell.TYPE).createSpec()) {
            @Override
            public DataCell getCell(final DataRow row, final long rowIndex) {
                return new LongCell(rowIndex - Long.MIN_VALUE);
            }
        });
        return exec.createColumnRearrangeTable(data, cR, exec);
    }

    private static BufferedDataTable removeDuplicates(final ExecutionContext exec, final String[] grpCols,
            final BufferedDataTable sortedTbl) throws CanceledExecutionException {
        final var rowCount = sortedTbl.size();
        final BufferedDataContainer cont = exec.createDataContainer(sortedTbl.getDataTableSpec());
        final int[] grpIndices = sortedTbl.getSpec().columnsToIndices(grpCols);
        double rowCnt = 0;
        try (final CloseableRowIterator iterator = sortedTbl.iterator()) {
            exec.checkCanceled();
            // maintain pointer to the first row within each group
            DataRow firstRowInGrp = iterator.next();
            ++rowCnt;
            exec.setProgress(rowCnt / rowCount);
            while (iterator.hasNext()) {
                final DataRow curRow = iterator.next();
                // if we witness a new group write the firstRowInGrp and reset it
                if (isDifferentGroup(grpIndices, firstRowInGrp, curRow)) {
                    cont.addRowToTable(firstRowInGrp);
                    firstRowInGrp = curRow;
                }
                ++rowCnt;
                exec.setProgress(rowCnt / rowCount);
            }
            cont.addRowToTable(firstRowInGrp);
        } finally {
            cont.close();
        }
        return cont.getTable();
    }

    private static boolean isDifferentGroup(final int[] grpIndices, final DataRow prevRow, final DataRow curRow) {
        return Arrays.stream(grpIndices).anyMatch(i -> !prevRow.getCell(i).equals(curRow.getCell(i)));
    }

    private BufferedDataTable appendColumns(final ExecutionContext exec, final String[] grpCols,
        final BufferedDataTable sortedTbl) throws CanceledExecutionException {
        final int[] grpIndices = sortedTbl.getSpec().columnsToIndices(grpCols);
        final BufferedDataContainer cont = exec.createDataContainer(createAdditionalColsSpec(sortedTbl.getSpec()));
        final var rowCount = sortedTbl.size();
        double rowCnt = 0;
        DataCell referenceKey = DataType.getMissingCell();
        try (final CloseableRowIterator iterator = sortedTbl.iterator()) {
            var isFirstRowInGrp = true;
            exec.checkCanceled();
            DataRow curRow = iterator.next();
            ++rowCnt;
            exec.setProgress(rowCnt / rowCount);
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
                ++rowCnt;
                exec.setProgress(rowCnt / rowCount);
                curRow = nextRow;
            } while (nextRow != null);
            // add the last row
            if (isFirstRowInGrp) {
                cont.addRowToTable(createRow(curRow, UNIQUE_IDENTIFIER, DataType.getMissingCell()));
            } else {
                cont.addRowToTable(createRow(curRow, DUPLICATE_IDENTIFIER, referenceKey));
            }
        } finally {
            cont.close();
        }
        return exec.createJoinedTable(sortedTbl, cont.getTable(), exec);
    }

    private DataRow createRow(final DataRow curRow, final StringCell label, final DataCell referenceKey) {
        if (m_addUniqueLabel && m_addRowLabel) {
            return new DefaultRow(curRow.getKey(), label, referenceKey);
        } else if (m_addUniqueLabel) {
            return new DefaultRow(curRow.getKey(), label);
        } else {
            return new DefaultRow(curRow.getKey(), referenceKey);
        }
    }

    /**
     * Create the output spec given the input spec.
     * @param inSpec input data table spec
     * @return output data table spec after running the deduplication
     */
    public DataTableSpec createOutputSpec(final DataTableSpec inSpec) {
        if (m_dropDuplicates) {
            return inSpec;
        }
        return new DataTableSpec(inSpec, createAdditionalColsSpec(inSpec));
    }

    private DataTableSpec createAdditionalColsSpec(final DataTableSpec inSpec) {
        final var uniqueNameGen = new UniqueNameGenerator(inSpec);
        final var addCols = new ArrayList<>();
        if (m_addUniqueLabel) {
            addCols.add(uniqueNameGen.newColumn("duplicate-type-classifier", StringCell.TYPE));
        }
        if (m_addRowLabel) {
            addCols.add(uniqueNameGen.newColumn("duplicate-row-identifier", StringCell.TYPE));
        }
        return new DataTableSpec(addCols.stream().toArray(DataColumnSpec[]::new));
    }

    private static BufferedDataTable updateDomain(final BufferedDataTable data, final ExecutionContext exec)
            throws CanceledExecutionException {
        final var dc = new DataTableDomainCreator(data.getSpec(), false);
        dc.updateDomain(data, exec);
        final var specWithNewDomain = dc.createSpec();
        return exec.createSpecReplacerTable(data, specWithNewDomain);
    }

}
