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
 *   Dec 16, 2022 (benjamin): created
 */
package org.knime.base.node.preproc.table.splitter;

import java.util.function.LongConsumer;
import java.util.function.Predicate;

import org.knime.base.node.preproc.table.splitter.TableSplitterNodeSettings.FindSplittingRowMode;
import org.knime.base.node.preproc.table.splitter.TableSplitterNodeSettings.MatchingCriteria;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.BufferedTableBackend;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InternalTableAPI;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.WorkflowTableBackendSettings;
import org.knime.core.table.row.Selection;
import org.knime.core.webui.node.dialog.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.dialog.impl.WebUINodeModel;

/**
 * Model of the Table Splitter node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction") // uses the restricted WebUI API
final class TableSplitterNodeModel extends WebUINodeModel<TableSplitterNodeSettings> {

    /** Placeholder string for the RowID column */
    static final String ROWID_PLACEHOLDER = "<row-keys>";

    // Warning and error messages

    private static final String NO_MATCHING_ROW_WARNING = "No matching row found. Bottom output table is empty";

    private static final String ROWID_MISSING_CRITERIA_WARNING =
        "Selected matching criteria \"Missing\" for RowID. This will never match because RowID cannot be missing.";

    private static final String SELECTED_COLUMN_MISSING_ERROR =
        "The selected column \"%s\" is not available. Please re-configure the node.";

    private static final String UNSUPPORTED_COLUMN_TYPE_ERROR =
        "The type \"%s\" of the selected column \"%s\" is not supported. Choose a string, integer, or long column.";

    private static final String NUMBER_NOT_PARSEABLE_ERROR =
        "The search pattern \"%s\" is not valid for the column \"%s\" because it is not a number.";

    private static final String INT_OUT_OF_RANGE_ERROR =
        "The search pattern \"%s\" is not valid for the column \"%s\" because it is too %s for integer values.";

    TableSplitterNodeModel(final WebUINodeConfiguration config) {
        super(config, TableSplitterNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final TableSplitterNodeSettings settings)
        throws InvalidSettingsException {
        final var spec = inSpecs[0];
        final var rowidSelected = ROWID_PLACEHOLDER.equals(settings.m_lookupColumn);
        final var selectedColumn = spec.stream().filter(c -> settings.m_lookupColumn.equals(c.getName())).findAny();

        // Check that the selected column is still available
        if (!rowidSelected && selectedColumn.isEmpty()) {
            throw new InvalidSettingsException(String.format(SELECTED_COLUMN_MISSING_ERROR, settings.m_lookupColumn));
        }

        // Check if the search pattern fits the type of the selected column
        if (!rowidSelected && settings.m_matchingCriteria == MatchingCriteria.EQUALS) {
            // NB: selectedColumn.isPresent() -> true because otherwise we throw the exception earlier
            checkColumnTypeAndSearchPattern(selectedColumn.get(), settings.m_searchPattern);
        }

        // Warning if RowID is selected and the matching criteria is "Missing"
        if (rowidSelected && settings.m_matchingCriteria == MatchingCriteria.MISSING) {
            setWarningMessage(ROWID_MISSING_CRITERIA_WARNING);
        }

        // Output two tables with the same spec as the input table
        return new DataTableSpec[]{spec, spec};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final TableSplitterNodeSettings settings) throws Exception {
        final var table = inData[0];
        final var tableSize = table.size();

        final double progressMatching;
        final double progressSlicing;
        if (WorkflowTableBackendSettings.getTableBackendForCurrentContext() instanceof BufferedTableBackend) {
            // Buffered data table backend: slicing the table is slow
            progressMatching = 0.3;
            progressSlicing = 0.7;
        } else {
            // Columnar backend: slicing the table is much faster
            progressMatching = 0.95;
            progressSlicing = 0.05;
        }

        // Find the matching row
        final var matchingRowIdx = findMatchingRowIdx(table, exec.createSubProgress(progressMatching), settings);
        exec.setProgress(progressMatching, "Splitting table");

        // Get the limits of the top and bottom table
        final long toRowIdx;
        final long fromRowIdx;
        if (matchingRowIdx < tableSize) {
            toRowIdx = matchingRowIdx + (settings.m_includeMatchingRowInTopTable ? 1 : 0);
            fromRowIdx = matchingRowIdx + (settings.m_includeMatchingRowInBottomTable ? 0 : 1);
        } else {
            setWarningMessage(NO_MATCHING_ROW_WARNING);
            toRowIdx = tableSize;
            fromRowIdx = tableSize;
        }

        // Slice the tables and return the result
        return new BufferedDataTable[]{ //
            getTopTable(table, toRowIdx, exec.createSubExecutionContext(progressSlicing / 2), //
                settings.m_updateDomains), //
            getBottomTable(table, fromRowIdx, exec.createSubExecutionContext(progressSlicing / 2), //
                settings.m_updateDomains) //
        };
    }

    // ================================ ITERATING TABLES ================================

    /** Find the index of the matching row (first or last according to the settings) */
    private static long findMatchingRowIdx(final BufferedDataTable table, final ExecutionMonitor exec,
        final TableSplitterNodeSettings settings) {
        final var progressUpdater = createProgressUpdater(exec, table.size());
        final var rowMatcher = createRowMatcher(table.getDataTableSpec(), settings);
        try (final RowCursor cursor = table.cursor(createColumnFilter(table, settings))) {
            if (settings.m_findSplittingRowMode == FindSplittingRowMode.FIRST_MATCH) {
                return findFirstMatchingRowIdx(cursor, progressUpdater, rowMatcher);
            } else {
                return findLastMatchingRowIdx(cursor, progressUpdater, rowMatcher);
            }
        }
    }

    /** Iterate over the cursor and find the first index that matches the predicate */
    private static long findFirstMatchingRowIdx(final RowCursor cursor, final LongConsumer progressUpdater,
        final Predicate<RowRead> matcher) {
        long rowIdx = 0;
        while (cursor.canForward()) {

            // Update the progress
            progressUpdater.accept(rowIdx);

            // Test if the row matches
            if (matcher.test(cursor.forward())) {
                return rowIdx;
            }

            rowIdx++;
        }

        // No row matches. There will be a warning (see #execute)
        return rowIdx;
    }

    /** Iterate over the cursor and find the last index that matches the predicate */
    private static long findLastMatchingRowIdx(final RowCursor cursor, final LongConsumer progressUpdater,
        final Predicate<RowRead> matcher) {
        long splitIdx = -1;
        long rowIdx = 0;
        while (cursor.canForward()) {

            // Update the progress
            progressUpdater.accept(rowIdx);

            // Test if the row matches
            if (matcher.test(cursor.forward())) {
                splitIdx = rowIdx;
            }
            rowIdx++;
        }
        if (splitIdx == -1) {
            // No row matches. There will be a warning (see #execute)
            return rowIdx;
        }
        return splitIdx;
    }

    /** Create a table filter that only materializes the column we check on */
    private static TableFilter createColumnFilter(final BufferedDataTable table,
        final TableSplitterNodeSettings settings) {
        if (ROWID_PLACEHOLDER.equals(settings.m_lookupColumn)) {
            return TableFilter.materializeCols();
        } else {
            return TableFilter.materializeCols(table.getDataTableSpec(), settings.m_lookupColumn);
        }
    }

    /** Create a consumer that updates the progress while we loop over the table */
    private static LongConsumer createProgressUpdater(final ExecutionMonitor exec, final long tableSize) {
        final var tableSizeDouble = (double)tableSize;
        // Update a maximum of around 200 times per table
        // The progress is still very accurate but we save some time
        final var updateInterval = Math.max(tableSize / 200, 1);

        return rowIdx -> {
            if (rowIdx % updateInterval == 0) {
                exec.setProgress(rowIdx / tableSizeDouble,
                    () -> String.format("Checking row %d of %d", rowIdx, tableSize));
            }
        };
    }

    // ================================ SLICING TABLES ================================

    /**
     * Slice of the top table
     *
     * @throws CanceledExecutionException
     */
    private static BufferedDataTable getTopTable(final BufferedDataTable table, final long toRowIdx,
        final ExecutionContext exec, final boolean updateDomains) throws CanceledExecutionException {
        return getTableSlice(table, Selection.all().retainRows(0, toRowIdx), updateDomains, exec);
    }

    /**
     * Slice of the bottom table
     *
     * @throws CanceledExecutionException
     */
    private static BufferedDataTable getBottomTable(final BufferedDataTable table, final long fromRowIdx,
        final ExecutionContext exec, final boolean updateDomains) throws CanceledExecutionException {
        long size = table.size();
        return getTableSlice(table, Selection.all().retainRows(fromRowIdx, size), updateDomains, exec);
    }

    private static BufferedDataTable getTableSlice(final BufferedDataTable table, final Selection selection,
        final boolean updateDomains, final ExecutionContext exec) throws CanceledExecutionException {
        var slicingExec = updateDomains ? exec.createSubExecutionContext(0.5) : exec;
        var resultTable = InternalTableAPI.slice(slicingExec, table, selection);

        if (updateDomains) {
            var specWithNewDomain = recalculateDomain(resultTable, exec.createSubExecutionContext(0.5));
            resultTable = exec.createSpecReplacerTable(resultTable, specWithNewDomain);
        }

        return resultTable;
    }

    // ================================ MATCHING ROWS ================================

    /** Create a predicate that returns true if a given row matches according to the settings */
    private static Predicate<RowRead> createRowMatcher(final DataTableSpec spec,
        final TableSplitterNodeSettings settings) {
        if (ROWID_PLACEHOLDER.equals(settings.m_lookupColumn)) {
            if (settings.m_matchingCriteria == MatchingCriteria.EQUALS) {
                return row -> settings.m_searchPattern.equals(row.getRowKey().getString());
            } else if (settings.m_matchingCriteria == MatchingCriteria.EMPTY) {
                return row -> row.getRowKey().getString().isBlank();
            } else {
                // NB: We show a warning in configure that this will never match
                return row -> false;
            }
        } else {
            final int columnIdx = spec.findColumnIndex(settings.m_lookupColumn);
            if (settings.m_matchingCriteria == MatchingCriteria.EQUALS) {
                return createEqualsColumnRowMatcher(spec, columnIdx, settings.m_searchPattern);
            } else if (settings.m_matchingCriteria == MatchingCriteria.EMPTY) {
                return createEmptyColumnRowMatcher(spec, columnIdx);
            } else {
                return row -> row.isMissing(columnIdx);
            }
        }
    }

    /** Check if the type of the column is supported */
    static boolean isCompatible(final DataColumnSpec columnSpec) {
        final var type = columnSpec.getType();
        return StringCell.TYPE.equals(type) // Strings
            || IntCell.TYPE.equals(type) // Integers
            || LongCell.TYPE.equals(type) // Longs
        ;
    }

    /** Check that the column has a supported type and if the search pattern fits the column type */
    private static void checkColumnTypeAndSearchPattern(final DataColumnSpec column, final String searchPattern)
        throws InvalidSettingsException {
        final var type = column.getType();

        if (IntCell.TYPE.equals(type) || LongCell.TYPE.equals(type)) {
            final long value;
            try {
                value = Long.parseLong(searchPattern);
            } catch (final NumberFormatException e) {
                // Not a parseable number
                throw new InvalidSettingsException(
                    String.format(NUMBER_NOT_PARSEABLE_ERROR, searchPattern, column.getName()), e);
            }
            if (IntCell.TYPE.equals(type)) {
                if (value > Integer.MAX_VALUE) {
                    // Too big for integers
                    throw new InvalidSettingsException(
                        String.format(INT_OUT_OF_RANGE_ERROR, searchPattern, column.getName(), "big"));
                } else if (value < Integer.MIN_VALUE) {
                    // Too small for integers
                    throw new InvalidSettingsException(
                        String.format(INT_OUT_OF_RANGE_ERROR, searchPattern, column.getName(), "small"));
                }
            }
        } else if (!StringCell.TYPE.equals(type)) {
            throw new InvalidSettingsException(
                String.format(UNSUPPORTED_COLUMN_TYPE_ERROR, type.getName(), column.getName()));
        }
    }

    /** Create a predicate that matches rows by comparing the value of one column */
    private static Predicate<RowRead> createEqualsColumnRowMatcher(final DataTableSpec spec, final int columnIdx,
        final String searchPattern) {
        final var type = spec.getColumnSpec(columnIdx).getType();
        final Predicate<RowRead> notMatchMissing = row -> !row.isMissing(columnIdx);

        if (StringCell.TYPE.equals(type)) {
            return notMatchMissing
                .and(row -> ((StringValue)row.getValue(columnIdx)).getStringValue().equals(searchPattern));
        } else if (IntCell.TYPE.equals(type)) {
            final var searchNumber = Integer.parseInt(searchPattern);
            return notMatchMissing.and(row -> ((IntValue)row.getValue(columnIdx)).getIntValue() == searchNumber);
        } else if (LongCell.TYPE.equals(type)) {
            final var searchNumber = Long.parseLong(searchPattern);
            return notMatchMissing.and(row -> ((LongValue)row.getValue(columnIdx)).getLongValue() == searchNumber);
        } else {
            // NB: This cannot happen because we fail in #checkColumnTypeAndSearchPattern
            //     if an unsupported column is selected
            throw new IllegalStateException(
                String.format(UNSUPPORTED_COLUMN_TYPE_ERROR, type.getName(), spec.getColumnNames()[columnIdx]));
        }
    }

    /** Create a predicate that matches rows based on the value in the specified column being either empty or missing */
    private static Predicate<RowRead> createEmptyColumnRowMatcher(final DataTableSpec spec, final int columnIdx) {
        final var type = spec.getColumnSpec(columnIdx).getType();
        final Predicate<RowRead> matchMissing = row -> row.isMissing(columnIdx);

        if (StringCell.TYPE.equals(type)) {
            return matchMissing //
                .or(row -> ((StringValue)row.getValue(columnIdx)).getStringValue().isBlank());
        } else if (IntCell.TYPE.equals(type) || LongCell.TYPE.equals(type)) {
            return matchMissing;
        } else {
            // NB: This cannot happen because we fail in #checkColumnTypeAndSearchPattern
            //     if an unsupported column is selected
            throw new IllegalStateException(
                String.format(UNSUPPORTED_COLUMN_TYPE_ERROR, type.getName(), spec.getColumnNames()[columnIdx]));
        }
    }

    private static DataTableSpec recalculateDomain(final BufferedDataTable table, final ExecutionMonitor exec)
        throws CanceledExecutionException {
        var domainCalculator = new DataTableDomainCreator(table.getDataTableSpec(), false);
        domainCalculator.updateDomain(table, exec);
        return domainCalculator.createSpec();
    }
}
