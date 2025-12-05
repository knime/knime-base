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
 *   16 Dec 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.data.filter.row.v2;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.knime.core.data.v2.IndexedRowReadPredicate;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.RowWriteCursor;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.util.valueformat.NumberFormatter;

/**
 * Utilities to slice and filter input tables by predicates.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class RowFilter {

    private RowFilter() {
        // no instantiation
    }

    /**
     * Filter the input with the given predicate and writes matching rows to the {@code included} output cursor
     * and optionally, non-matching rows to the {@code excluded} output cursor.
     *
     * @param exec execution context
     * @param input input data
     * @param tableSize number of input rows for reporting progress
     * @param included output for included rows matching the predicate
     * @param excluded nullable output for excluded rows, not matching the predicate
     * @param predicate predicate to test input rows
     * @param includeMatches whether to include rows that match the predicate or exclude them
     * @throws CanceledExecutionException if execution was canceled by the user
     * @throws InvalidSettingsException in case some settings are invalid
     */
    public static void filterOnPredicate(final ExecutionContext exec, final RowCursor input, final long tableSize,
        final RowWriteCursor included, final RowWriteCursor excluded, final IndexedRowReadPredicate predicate,
        final boolean includeMatches) throws CanceledExecutionException, InvalidSettingsException {
        // top-level progress reports number of processed rows as a fraction of the input table size
        final var readRows = new AtomicLong();
        final var msg = progressFractionBuilder(readRows::get, tableSize);
        exec.setProgress(0, () -> msg.apply(new StringBuilder("Processed row ")).toString());

        // sub-progress for reporting the number of matching rows
        final var matchingRows = new AtomicLong();
        final var outputProgress = exec.createSubProgress(0.0);
        final var numFormat = NumberFormatter.builder().setGroupSeparator(",").build();
        outputProgress.setMessage(() -> numFormat.format(matchingRows.get()) + " rows matching");

        while (input.canForward()) {
            exec.checkCanceled();
            final var read = input.forward();
            final var index = readRows.getAndIncrement();
            if (includeMatches == predicate.test(index, read)) {
                included.commit(read);
                matchingRows.incrementAndGet();
            } else if (excluded != null) {
                excluded.commit(read);
            }
            exec.setProgress(1.0 * readRows.get() / tableSize);
        }
    }

    /**
     * Slices the input table according to the given partition information into one or two output tables.
     *
     * @param exec execution context
     * @param input input data
     * @param partition partition information about matching and non-matching rows
     * @param isSplitting whether the filter is a splitter, i.e. returns the "non-matching" output table
     * @return single-element array with output table containing matching rows or two-element array with matching and
     *         non-matching table output
     * @throws CanceledExecutionException if the user canceled the execution
     */
    public static BufferedDataTable[] slice(final ExecutionContext exec, final BufferedDataTable input,
        final FilterPartition partition, final boolean isSplitting) throws CanceledExecutionException {
        return partition.sliceTable(exec, input, isSplitting);
    }

    /**
     * Filters the streaming input with the given predicate and writes matching rows to the {@code included} output.
     * Non-matching rows are written to the {@code excluded} output, if provided.
     *
     * @param exec execution context
     * @param input streaming input data
     * @param included output for included rows matching the predicate
     * @param excluded nullable output for excluded rows, not matching the predicate
     * @param predicate predicate to test input rows
     * @param includeMatches whether to include rows that match the predicate or exclude them
     * @throws CanceledExecutionException if execution was canceled by the user
     * @throws InvalidSettingsException in case some settings are invalid
     * @throws InterruptedException if execution was interrupted
     */
    public static void filterOnPredicate(final ExecutionContext exec, final RowInput input, final RowOutput included,
        final RowOutput excluded, final IndexedRowReadPredicate predicate, final boolean includeMatches)
        // The method uses InterruptibleRowCursor and InterruptibleRowWriteCursor, which for
        // backwards-compatibility do not annotate that they may throw InterruptedException.
        // However, by re-annotating here, we can signal to callers that we might throw this.
        // See ExceptionUtils#asRuntimeException.
        throws CanceledExecutionException, //
        InvalidSettingsException, //
        InterruptedException { // NOSONAR InterruptedException from cursors
        final var inSpec = input.getDataTableSpec();

        // the only stats to report are read and included rows, we don't know the size of the input
        final var numFormat = NumberFormatter.builder().setGroupSeparator(",").build();
        final var matchedRead = new long[2];
        final Supplier<String> progress = () -> "%s/%s rows included" //
            .formatted(numFormat.format(matchedRead[0]), numFormat.format(matchedRead[1]));

        try (final var in = input.asCursor();
                // the cursors might block and throw InterruptedException
                final var incl = included.asWriteCursor(inSpec);
                @SuppressWarnings("resource")
                final var excl = excluded != null ? excluded.asWriteCursor(inSpec) : null) {
            for (RowRead rowRead; (rowRead = in.forward()) != null;) {
                exec.checkCanceled();
                final var index = matchedRead[1];
                matchedRead[1]++;

                if (includeMatches == predicate.test(index, rowRead)) {
                    incl.commit(rowRead);
                    matchedRead[0]++;
                } else if (excl != null) {
                    excl.commit(rowRead);
                }
                exec.setMessage(progress);
            }
        }
    }

    /**
     * Filters the streaming input according to the given partition information. Matching rows are written to the
     * {@code included} output, non-matching rows to the optional {@code excluded} output.
     *
     * @param exec execution context
     * @param input streaming input data
     * @param included output for included rows according to the {@code matching} partition
     * @param excluded nullable output for excluded rows according to the {@code non-matching} partition
     * @param rowPartition partition information about matching and non-matching rows
     * @throws CanceledExecutionException if execution was canceled by the user
     * @throws InvalidSettingsException in case some settings are invalid
     * @throws InterruptedException if execution was interrupted
     */
    public static void filterRange(final ExecutionContext exec, final RowInput input, // NOSONAR
        final RowOutput included, final RowOutput excluded, final FilterPartition rowPartition)
        throws CanceledExecutionException, //
        InvalidSettingsException, //
        InterruptedException { // NOSONAR cursors from RowInput/RowOutput can throw InterruptedException

        final var includeRanges = rowPartition.matching().asRanges().stream().toList();
        final var numIncludeRanges = includeRanges.size();

        // the only stats to report are read and included rows, we don't know the size of the input
        final var numFormat = NumberFormatter.builder().setGroupSeparator(",").build();
        final var matchedRead = new long[2];
        final Supplier<String> progress = () -> "%s/%s rows included" //
            .formatted(numFormat.format(matchedRead[0]), numFormat.format(matchedRead[1]));

        /*
         * The following loop acts as a state machine, alternating between "include" and "exclude" states:
         *
         *                       ┌─────────┐ input ends or range ends and not a splitter
         *                       │ INCLUDE ├─────────────────────────────────────────┐
         *                    ┌──┤  ROWS   ◄──┐                                      │
         *             include│  └─────────┘  │include                            ┌──▼──┐
         *               range│               │range                              │ END │
         *                ends│  ┌─────────┐  │starts                             └──▲──┘
         *                    └──► EXCLUDE ├──┘                                      │
         *              ─────────►  ROWS   ├─────────────────────────────────────────┘
         *               start   └─────────┘              input ends
         */
        RowRead rowRead;
        try (final var in = input.asCursor();
                // the cursors might block and throw InterruptedException
                final var incl = included.asWriteCursor(input.getDataTableSpec());
                final var excl = excluded != null ? excluded.asWriteCursor(input.getDataTableSpec()) : null) {
            for (var nextRange = 0;; nextRange++) {

                // === EXCLUDE ROWS state ===
                final long lastExclRow;
                if (nextRange < numIncludeRanges) {
                    // there's another include range ahead, everything before is excluded
                    lastExclRow = includeRanges.get(nextRange).lowerEndpoint() - 1;
                } else {
                    // only excluded rows remain
                    if (excluded == null) { // NOSONAR
                        // not a splitter, nothing left to do
                        return;
                    }
                    incl.close(); // NOSONAR closing resource as soon as possible in streaming execution
                    lastExclRow = Long.MAX_VALUE; // effectively makes `while` condition below `true`
                }
                while (matchedRead[1] <= lastExclRow) {
                    exec.checkCanceled();
                    if ((rowRead = in.forward()) == null) { // NOSONAR
                        return;
                    }
                    if (excl != null) { // NOSONAR
                        excl.commit(rowRead);
                    }
                    matchedRead[1]++;
                    exec.setMessage(progress);
                }

                // === INCLUDE ROWS state ===
                final var currentRange = includeRanges.get(nextRange);
                final long lastInclRow;
                if (currentRange.hasUpperBound()) {
                    // current include range ends somewhere
                    lastInclRow = currentRange.upperEndpoint() - 1;
                } else {
                    // only included rows remain
                    if (excl != null) { // NOSONAR
                        excl.close(); // NOSONAR closing resource as soon as possible in streaming execution
                    }
                    lastInclRow = Long.MAX_VALUE; // effectively makes `while` condition below `true`
                }
                while (matchedRead[1] <= lastInclRow) {
                    exec.checkCanceled();
                    if ((rowRead = in.forward()) == null) { // NOSONAR
                        return;
                    }
                    incl.commit(rowRead);
                    matchedRead[1]++;
                    matchedRead[0]++;
                    exec.setMessage(progress);
                }
            }
        }
    }

    /**
     * Creates a function that adds a nicely formatted, padded fraction of the form {@code " 173/2,065"} to a given
     * {@link StringBuilder} that reflects the current value of the given supplier {@code currentValue}. The padding
     * with space characters tries to minimize jumping in the UI.
     *
     * @param currentValue supplier for the current numerator
     * @param total fixed denominator
     * @return function that modified the given {@link StringBuilder} and returns it for convenience
     */
    // TODO The following code is copied from `AbstractTableSorter` and should be moved to a better place
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
