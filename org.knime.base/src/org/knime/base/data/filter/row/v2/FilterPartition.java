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

import org.knime.base.data.filter.row.v2.OffsetFilter.Operator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InternalTableAPI;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.table.row.Selection;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

/**
 * A partition of row numbers.
 *
 * @param matching range set specifying which row numbers from a table match the filter
 * @param nonMatching range set specifying which row numbers from a table <em>do not</em> match the filter
 *
 */
public record FilterPartition(ImmutableRangeSet<Long> matching, ImmutableRangeSet<Long> nonMatching) {

    /**
     * Combines this partition's ranges with the given partition's ranges via pair-wise union or intersection.
     *
     * @param isAnd {@code true} for union, {@code false} for intersection
     * @param other other partition
     * @return combined filter partition
     */
    public FilterPartition combine(final boolean isAnd, final FilterPartition other) {
        final var includedRange =
            isAnd ? matching.intersection(other.matching) : matching.union(other.matching);
        final var excludedRange =
            isAnd ? nonMatching.union(other.nonMatching) : nonMatching.intersection(other.nonMatching);
        return new FilterPartition(includedRange, excludedRange);
    }

    /**
     * Returns a new partition with matching and non-matching swapped.
     * @return swapped partition
     */
    public FilterPartition swapped() {
        return new FilterPartition(nonMatching, matching);
    }

    /**
     * Computes the sets of rows which are matched/not matched by the given filter as a pair of
     * non-overlapping sets.
     * <ul>
     * <li>The lower bounds of all ranges in the sets are always inclusive.</li>
     * <li>The upper bound is absent iff it is the end of the input and the table size is unknown. Otherwise the
     * upper bound is guaranteed to be exclusive.</li>
     * </ul>
     *
     * @param filter filter to apply
     * @param optionalTableSize table size if known, negative otherwise
     * @return pair where the first/second element represents the matched/non-matched row indices, respectively
     */
    public static FilterPartition computePartition(final OffsetFilter filter,
            final long optionalTableSize) {
        final var indexOperator = filter.operator();
        final var complementIndexOperator = switch (indexOperator) {
            case EQ -> Operator.NEQ;
            case NEQ -> Operator.EQ;
            case LT -> Operator.GTE;
            case LTE -> Operator.GT;
            case GT -> Operator.LTE;
            case GTE -> Operator.LT;
        };
        final var offsetNonNeg = filter.offset();
        final var matching = rowIndexRanges(indexOperator, offsetNonNeg, optionalTableSize);
        final var nonMatching = rowIndexRanges(complementIndexOperator, offsetNonNeg, optionalTableSize);

        return new FilterPartition(matching, nonMatching);
    }

    private static ImmutableRangeSet<Long> rowIndexRanges(final Operator operator,
        final long indexNonNeg, final long optSize) {
        final TreeRangeSet<Long> rangesOut = TreeRangeSet.create();
        switch (operator) {
            case EQ  -> addRangeIfNonEmpty(rangesOut, indexNonNeg, indexNonNeg + 1, optSize);
            case NEQ -> {
                addRangeIfNonEmpty(rangesOut, 0, indexNonNeg, optSize);
                addRangeIfNonEmpty(rangesOut, indexNonNeg + 1, optSize, optSize);
            }
            case LT  -> addRangeIfNonEmpty(rangesOut, 0, indexNonNeg, optSize);
            case LTE -> addRangeIfNonEmpty(rangesOut, 0, indexNonNeg + 1, optSize);
            case GT  -> addRangeIfNonEmpty(rangesOut, indexNonNeg + 1, optSize, optSize);
            case GTE -> addRangeIfNonEmpty(rangesOut, indexNonNeg, optSize, optSize);
        }
        return ImmutableRangeSet.copyOf(rangesOut);
    }

    /**
     * Slice the input table according to this filter partition.
     *
     * @param exec execution context to use
     * @param in input table
     * @param isSplitter whether to split the table according to the filter into a "matching" and "non-matching" table
     *            or only return the matching table
     * @return sliced table(s)
     * @throws CanceledExecutionException
     */
    BufferedDataTable[] sliceTable(final ExecutionContext exec, final BufferedDataTable in,
        final boolean isSplitter) throws CanceledExecutionException {

        final var includedRanges = matching();
        final var excludedRanges = nonMatching();
        if (includedRanges.isEmpty()) {
            final var inputSize = in.size();
            CheckUtils.checkState(inputSize == 0 || Range.closedOpen(0L, inputSize).equals(excludedRanges.span()),
                "Inclusion is empty but exclusion does not span whole table: %s", excludedRanges);
            // no rows are included
            final var empty = exec.createVoidTable(in.getSpec());
            var matchingTable = empty;
            // handle empty input table -> includeRanges empty and excludeRanges empty
            var nonMatchingTable = inputSize == 0 ? empty : in;
            return isSplitter ? new BufferedDataTable[]{matchingTable, nonMatchingTable}
                : new BufferedDataTable[]{matchingTable};
        }

        if (excludedRanges.isEmpty()) {
            CheckUtils.checkState(Range.closedOpen(0L, in.size()).equals(includedRanges.span()),
                "Exclusion is empty but inclusion does not span whole table: %s", includedRanges);
            // all rows are included
            final var empty = exec.createVoidTable(in.getSpec());
            return isSplitter ? new BufferedDataTable[]{in, empty} : new BufferedDataTable[]{in};
        }

        if (!isSplitter) {
            return new BufferedDataTable[]{slicedFromRanges(exec, in, includedRanges)};
        }

        // split the progress between the two output tables
        final var included = slicedFromRanges(exec.createSubExecutionContext(0.5), in, includedRanges);
        final var excluded = slicedFromRanges(exec.createSubExecutionContext(0.5), in, excludedRanges);
        return new BufferedDataTable[] { included, excluded };
    }

    /**
     * Adds a range of row indexes to the output list {@code out} if it is not empty. If the table size is known,
     * the range is limited to that size.
     *
     * @param out output list to append to
     * @param lowerIncl first row index in the range
     * @param upperExcl first row index after the range, or {@code -1} if the range doesn't have an upper limit
     * @param optSize size of the input table if known, {@link RowFilterNodeModel#UNKNOWN_SIZE} otherwise
     */
    static void addRangeIfNonEmpty(final RangeSet<Long> out, final long lowerIncl, final long upperExcl,
            final long optSize) {
        if (lowerIncl < 0) {
            // possible if `operator == GT` and `indexNonNeg == Long.MAX_VALUE`, empty selection
            return;
        }

        final var sizeUpperBound = optSize < 0 ? Long.MAX_VALUE : optSize;
        final var lowerInclClamped = Math.min(lowerIncl, sizeUpperBound);
        if (upperExcl < 0) {
            // may be from overflow (if `lowerIncl == Long.MAX_VALUE`) or unknown size
            out.add(Range.atLeast(lowerInclClamped));
        }

        final var upperExclClamped = Math.min(upperExcl, sizeUpperBound);
        if (lowerInclClamped < upperExclClamped) {
            out.add(Range.closedOpen(lowerInclClamped, upperExclClamped));
        }
    }

    /* === UTILITY === */

    private static BufferedDataTable slicedFromRanges(final ExecutionContext exec, final BufferedDataTable in,
            final RangeSet<Long> includeRanges) throws CanceledExecutionException {
        final var ranges = includeRanges.asRanges().stream().toList();
        final var numDisconnectedRanges = ranges.size();
        if (numDisconnectedRanges == 0) {
            return exec.createVoidTable(in.getDataTableSpec());
        }
        if (numDisconnectedRanges == 1) {
            return InternalTableAPI.slice(exec, in, toSelection(ranges.get(0)));
        }

        final var subTables =
            InternalTableAPI.multiSlice(exec, in, ranges.stream().map(r -> toSelection(r)).toArray(Selection[]::new));

        final var concatProgress = exec.createSubProgress(0.5);
        try {
            return exec.createConcatenateTable(concatProgress, subTables);
        } finally {
            concatProgress.setProgress(1.0, (String)null);
        }
    }

    private static Selection toSelection(final Range<Long> range) {
        return Selection.all().retainRows(range.lowerEndpoint(), range.upperEndpoint());
    }

}
