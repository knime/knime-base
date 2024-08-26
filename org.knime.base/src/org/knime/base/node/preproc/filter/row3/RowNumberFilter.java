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
 *   24 May 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterMode;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InternalTableAPI;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.table.row.Selection;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

/**
 * Utility to filter on row numbers.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class RowNumberFilter {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RowNumberFilter.class);

    private static final long UNKNOWN_SIZE = RowFilterNodeModel.UNKNOWN_SIZE;

    private static final EnumSet<FilterOperator> SUPPORTED_OPERATORS = EnumSet.of( //
        FilterOperator.EQ, //
        FilterOperator.NEQ, //
        FilterOperator.LT, //
        FilterOperator.LTE, //
        FilterOperator.GT, //
        FilterOperator.GTE, //
        FilterOperator.FIRST_N_ROWS, //
        FilterOperator.LAST_N_ROWS //
    );

    private RowNumberFilter() {
        // hidden
    }

    private static void validateRowNumberOperatorSupported(final FilterOperator op)
            throws InvalidSettingsException {
        CheckUtils.checkSetting(SUPPORTED_OPERATORS.contains(op),
            "Cannot use operator \"%s\" on row numbers.", op.m_label);
    }

    static BufferedDataTable[] sliceTable(final ExecutionContext exec, final BufferedDataTable in,
        final FilterPartition includedExcludedPartition, final boolean isSplitter) throws CanceledExecutionException {
        LOGGER.debug("Slicing input table");

        final var includedRanges = includedExcludedPartition.matching;
        final var excludedRanges = includedExcludedPartition.nonMatching;
        if (includedRanges.isEmpty()) {
            final var inputSize = in.size();
            if (inputSize > 0 && !Range.closedOpen(0L, inputSize).equals(excludedRanges.span())) {
                throw new IllegalStateException(
                    "Inclusion is empty but exclusion does not span whole table: %s"
                        .formatted(excludedRanges));
            }
            // no rows are included
            final var empty = exec.createVoidTable(in.getSpec());
            var matching = empty;
            // handle empty input table -> includeRanges empty and excludeRanges empty
            var nonMatching = inputSize == 0 ? empty : in;
            return isSplitter ? new BufferedDataTable[]{matching, nonMatching} : new BufferedDataTable[]{matching};
        }

        if (excludedRanges.isEmpty()) {
            if (!Range.closedOpen(0L, in.size()).equals(includedRanges.span())) {
                throw new IllegalStateException(
                    "Exclusion is empty but inclusion does not span whole table: %s"
                        .formatted(includedRanges));
            }
            // all rows are included
            final var empty = exec.createVoidTable(in.getSpec());
            return isSplitter ? new BufferedDataTable[]{in, empty} : new BufferedDataTable[]{in};
        }

        if (!isSplitter) {
            return new BufferedDataTable[]{slicedFromRanges(exec, in, includedRanges)};
        }

        LOGGER.debug("Splitting table");
        // split the progress between the two output tables
        final var included = slicedFromRanges(exec.createSubExecutionContext(0.5), in, includedRanges);
        final var excluded = slicedFromRanges(exec.createSubExecutionContext(0.5), in, excludedRanges);
        return new BufferedDataTable[] { included, excluded };
    }

    static FilterPartition computeRowPartition(final boolean isAnd,
        final List<RowNumberFilterSpec> rowNumberFilters, final FilterMode outputMode,
        final long optionalTableSize) {
        LOGGER.debug(() -> "Computing row partitions from %d %s-ed predicates".formatted(rowNumberFilters.size(),
            isAnd ? "AND" : "OR"));
        final var matchedNonMatchedPartition = rowNumberFilters
            .stream() //
            .map(filterSpec -> FilterPartition
                .computePartition(filterSpec.toOffsetFilter(optionalTableSize), optionalTableSize))
            .reduce((a, b) -> a.combine(isAnd, b)) //
            .orElseThrow(() -> new IllegalArgumentException("Need at least one filter criterion"));
        // determine whether matched or non-matched rows are included in the first output, flip pair as needed
        return outputMode == FilterMode.MATCHING ? matchedNonMatchedPartition : matchedNonMatchedPartition.swapped();
    }

    private static BufferedDataTable slicedFromRanges(final ExecutionContext exec, final BufferedDataTable in,
            final RangeSet<Long> includeRanges) throws CanceledExecutionException {
        LOGGER.debug(() -> "Slicing with included range: %s".formatted(includeRanges.toString()));
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
            LOGGER.debug(() -> "Concatenating %d sliced tables".formatted(subTables.length));
            return exec.createConcatenateTable(concatProgress, subTables);
        } finally {
            concatProgress.setProgress(1.0, (String)null);
        }
    }

    private static Selection toSelection(final Range<Long> range) {
        return Selection.all().retainRows(range.lowerEndpoint(), range.upperEndpoint());
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
    private static void addRangeIfNonEmpty(final RangeSet<Long> out, final long lowerIncl, final long upperExcl,
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

    /* ==========================================         RECORDS          ========================================== */

    record RowNumberFilterSpec(FilterOperator operator, long value) {

        /**
         * Computes the index operator and offset given the filter operator and value.
         *
         * @param operator filter operator from the node settings
         * @param value value from the node settings
         * @param optionalTableSize table size if known
         * @return index operator and index value (non-negative offset)
         */
        OffsetFilter toOffsetFilter(final long optionalTableSize) {
            if (operator == FilterOperator.FIRST_N_ROWS) {
                return new OffsetFilter(FilterOperator.LT, value);
            }
            if (operator == FilterOperator.LAST_N_ROWS) {
                CheckUtils.checkState(optionalTableSize != UNKNOWN_SIZE,
                    "Expected table size for filter operator \"%s\"", operator.label());
                // if the table has fewer than `n` rows, return the whole table
                return new OffsetFilter(FilterOperator.GTE, Math.max(0, optionalTableSize - value));
            }
            // the dialog accepts 1-based row numbers but we use 0-based row offsets internally
            return new OffsetFilter(operator, value - 1);
        }
    }

    /**
     * If supported, get the criterion as a row number filter specification.
     *
     * @param criterion filter criterion
     * @return row number filter specification
     * @throws InvalidSettingsException if the filter criterion contains an unsupported operator or the value is
     *             missing
     */
    static RowNumberFilterSpec getAsFilterSpec(final FilterCriterion criterion)
        throws InvalidSettingsException {
        validateRowNumberOperatorSupported(criterion.m_operator);
        final var value = ((LongCell)criterion.m_predicateValues.getCellAt(0).filter(cell -> !cell.isMissing())
            .orElseThrow(() -> new InvalidSettingsException("Row number value is missing"))).getLongValue();
        final var op = criterion.m_operator;
        final var isNumberOfRows = op == FilterOperator.FIRST_N_ROWS || op == FilterOperator.LAST_N_ROWS;
        if (!isNumberOfRows) {
            CheckUtils.checkSetting(value > 0, "Row number must be larger than zero: %d", value);
        } else {
            CheckUtils.checkSetting(value >= 0, "Number of rows must not be negative: %d", value);
        }
        return new RowNumberFilterSpec(op, value);
    }

    /**
     * Gets each of the passed criteria as a filter spec, if supported.
     * @param rowNumberCriteria criteria to map to filter specs
     * @return
     * @throws InvalidSettingsException if not supported
     * @see {@link #getAsFilterSpec(FilterCriterion)}
     */
    static List<RowNumberFilterSpec> getAsFilterSpecs(final List<FilterCriterion> rowNumberCriteria)
            throws InvalidSettingsException {
        final List<RowNumberFilterSpec> rowNumberFilterSpecs = new ArrayList<>();
        for (final var criterion : rowNumberCriteria) {
            rowNumberFilterSpecs.add(RowNumberFilter.getAsFilterSpec(criterion));
        }
        return rowNumberFilterSpecs;
    }

    /**
     * A filter using a row offset from the start of the table.
     *
     * @param operator filter operator taking an offset value
     * @param offset offset value
     */
    record OffsetFilter(FilterOperator operator, long offset) {
        OffsetFilter {
            CheckUtils.checkArgument(offset >= 0, "Offset must not be negative: %d", offset);
            CheckUtils.checkArgument(
                operator != FilterOperator.FIRST_N_ROWS && operator != FilterOperator.LAST_N_ROWS,
                "Offset filter must use \"absolute\" operator, %s was given", operator.name());
        }

        RowNumberPredicate toPredicate() {
            return switch (operator) {
                case EQ  -> rowNumber -> rowNumber == offset;
                case NEQ -> rowNumber -> rowNumber != offset;
                case LT -> rowNumber -> rowNumber < offset;
                case LTE -> rowNumber -> rowNumber <= offset;
                case GT -> rowNumber -> rowNumber > offset;
                case GTE -> rowNumber -> rowNumber >= offset;
                default  -> throw new IllegalStateException("Unsupported offset filter operator: " + operator);
            };
        }
    }

    /**
     * A partition of row numbers.
     *
     * @param matching range set specifying which row numbers from a table match the filter
     * @param nonMatching range set specifying which row numbers from a table <em>do not</em> match the filter
     *
     */
    record FilterPartition(ImmutableRangeSet<Long> matching, ImmutableRangeSet<Long> nonMatching) {

        /**
         * Combines this partition's ranges with the given partition's ranges via pair-wise union or intersection.
         *
         * @param isAnd {@code true} for union, {@code false} for intersection
         * @param other other partition
         * @return combined filter partition
         */
        FilterPartition combine(final boolean isAnd, final FilterPartition other) {
            LOGGER.debug(() -> "Pairwise merge of [%s, %s] with [%s, %s] under %s".formatted(matching, nonMatching,
                other.matching, other.nonMatching, isAnd ? "intersection" : "union"));
            final var includedRange =
                isAnd ? matching.intersection(other.matching) : matching.union(other.matching);
            final var excludedRange =
                isAnd ? nonMatching.union(other.nonMatching) : nonMatching.intersection(other.nonMatching);
            LOGGER.debug(() -> "Combined: [%s, %s]".formatted(includedRange, excludedRange));
            return new FilterPartition(includedRange, excludedRange);
        }

        /**
         * Returns a new partition with matching and non-matching swapped.
         * @return swapped partition
         */
        FilterPartition swapped() {
            return new FilterPartition(nonMatching, matching);
        }

        /**
         * Computes the sets of rows which are matched/not matched by the given operator and value as a pair of
         * sorted arrays of non-overlapping {@link RowRange}.
         * <ul>
         * <li>The lower bounds of all ranges in the sets are always inclusive.</li>
         * <li>The upper bound is absent iff it is the end of the input and the table size is unknown. Otherwise the
         * upper bound is guaranteed to be exclusive.</li>
         * </ul>
         *
         * @param operator operator as selected in the dialog
         * @param value comparison value as specified in the dialog
         * @param optionalTableSize table size if known, {@link #UNKNOWN_SIZE} otherwise
         * @return pair where the first/second element represents the matched/non-matched row indices, respectively
         */
        static FilterPartition computePartition(final OffsetFilter filter,
                final long optionalTableSize) {
            final var indexOperator = filter.operator;
            final var complementIndexOperator = switch (indexOperator) {
                case EQ -> FilterOperator.NEQ;
                case NEQ -> FilterOperator.EQ;
                case LT -> FilterOperator.GTE;
                case LTE -> FilterOperator.GT;
                case GT -> FilterOperator.LTE;
                case GTE -> FilterOperator.LT;
                default -> throw new IllegalArgumentException(
                    "Unsupported table filter operator: " + indexOperator);
            };
            final var offsetNonNeg = filter.offset;
            final var matching = rowIndexRanges(indexOperator, offsetNonNeg, optionalTableSize);
            final var nonMatching = rowIndexRanges(complementIndexOperator, offsetNonNeg, optionalTableSize);

            LOGGER.debug(() -> "Computed partition [%s, %s] from operator %s and value %d".formatted(
                matching, nonMatching, indexOperator, offsetNonNeg));
            return new FilterPartition(matching, nonMatching);
        }

        private static ImmutableRangeSet<Long> rowIndexRanges(final FilterOperator operator, final long indexNonNeg,
                final long optSize) {
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
                default  -> throw new IllegalArgumentException("Unsupported table filter operator: " + operator);
            }
            return ImmutableRangeSet.copyOf(rangesOut);
        }
    }
}
