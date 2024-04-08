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
 *   20 Mar 2024 (jasper): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.knime.base.node.preproc.filter.row3.RowFilter3NodeSettings.Anchors.RowNumbersAnchor;
import org.knime.base.node.preproc.filter.row3.RowFilter3NodeSettings.OutputMode;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InternalTableAPI;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.table.row.Selection;
import org.knime.core.util.valueformat.NumberFormatter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;

/**
 * Implementation of the Row Filter 2 (Labs) node.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // Web UI not API yet
final class RowFilter3NodeModel extends WebUINodeModel<RowFilter3NodeSettings> {

    protected RowFilter3NodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, RowFilter3NodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final RowFilter3NodeSettings settings)
        throws InvalidSettingsException {
        settings.m_anchors.m_real.m_bounds.validate();
        settings.m_anchors.m_integer.m_bounds.validate();
        settings.m_anchors.m_rowNumbers.m_bounds.validate();
        if (RowNumberFilter.isFilterOnRowNumbers(settings)) {
            RowNumberFilter.validateRowNumberOperatorSupported(settings);
        }
        return inSpecs;
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final RowFilter3NodeSettings settings) throws Exception {
        final var in = inData[0];
        return new BufferedDataTable[]{filterTable(exec, in, settings)};
    }

    private static BufferedDataTable filterTable(final ExecutionContext exec, final BufferedDataTable in,
        final RowFilter3NodeSettings settings)
        throws CanceledExecutionException, InvalidSettingsException, IOException { // NOSONAR
        if (RowNumberFilter.isFilterOnRowNumbers(settings)) {
            return RowNumberFilter.filterTable(exec, in, settings);
        }

        final var inSpec = in.getSpec();
        final Predicate<RowRead> predicate = DataValuePredicate.forSettings(exec, settings, inSpec);
        final var includeMatches = settings.includeMatches();
        final long size = in.size();
        try (final var input = in.cursor();
                // take domains from input in order to allow downstream visualizations to retain
                // useful bounds, e.g. [0, 10] for an axis
                final var output = exec.createRowContainer(inSpec, true);
                final var writeCursor = output.createCursor()) {
            exec.setProgress(0);
            final var readRows = new AtomicLong();
            final var writtenRows = new AtomicLong();
            final var msg = progressFractionBuilder(readRows::get, size);
            final var outputProgress = exec.createSubProgress(1.0);
            while (input.canForward()) {
                exec.checkCanceled();
                final var read = input.forward();
                readRows.incrementAndGet();
                if (includeMatches == predicate.test(read)) {
                    final var write = writeCursor.forward();
                    write.setFrom(read);
                    writtenRows.incrementAndGet();
                    outputProgress.setMessage(() -> "Output row " + writtenRows.get());
                }
                exec.setProgress(1.0 * readRows.get() / size,
                    () -> msg.apply(new StringBuilder("Processed row ")).toString());
            }
            return output.finish();
        }
    }

    private static final class RowNumberFilter {

        private static final EnumSet<FilterOperator> SUPPORTED_OPERATORS =
            EnumSet.of(FilterOperator.EQ, FilterOperator.NEQ, FilterOperator.LT, FilterOperator.LTE, FilterOperator.GT,
                FilterOperator.GTE, FilterOperator.BETWEEN, FilterOperator.FIRST_N_ROWS, FilterOperator.LAST_N_ROWS);

        private RowNumberFilter() {
            // hidden
        }

        private static boolean isFilterOnRowNumbers(final RowFilter3NodeSettings settings) {
            return SpecialColumns.ROW_NUMBERS.getId().equals(settings.m_column.getSelected());
        }

        private static boolean isTailFilter(final RowFilter3NodeSettings settings) {
            return isFilterOnRowNumbers(settings) && settings.m_operator == FilterOperator.LAST_N_ROWS;
        }

        static BufferedDataTable filterTable(final ExecutionContext exec, final BufferedDataTable in,
            final RowFilter3NodeSettings settings) throws CanceledExecutionException {
            final long tableSize = in.size();
            final var includeRanges = computeIncludedRanges(settings, tableSize);
            if (includeRanges.isEmpty()) {
                // nothing is selected
                return exec.createVoidTable(in.getSpec());
            }

            // if our contained ranges span the whole table, we don't need to slice it
            if (allRowsSelected(includeRanges, tableSize)) {
                return in;
            }

            final var columnSelection = Selection.all();
            final var ranges = includeRanges.asRanges();
            if (ranges.size() == 1) {
                final var range = ranges.iterator().next();
                return InternalTableAPI.slice(exec, in, applyRowRange(columnSelection, range));
            }

            return exec.createConcatenateTable(exec,
                ranges.stream().map(range -> InternalTableAPI.slice(exec, in, applyRowRange(columnSelection, range)))
                    .toArray(BufferedDataTable[]::new));
        }

        private static RangeSet<Long> computeIncludedRanges(final RowFilter3NodeSettings settings,
            final long tableSize) {
            final var operator = settings.m_operator;
            final var bounds = settings.m_anchors.m_rowNumbers;
            return RowNumberFilter.computeRanges(operator, settings.m_outputMode, tableSize, bounds);
        }

        private static Selection applyRowRange(final Selection selection, final Range<Long> range) {
            // we need lower inclusive
            final var lower = range.lowerEndpoint() + (range.lowerBoundType() == BoundType.OPEN ? 1 : 0);
            // ... but upper exclusive
            final var upper = range.upperEndpoint() + (range.upperBoundType() == BoundType.CLOSED ? 1 : 0);
            return selection.retainRows(lower, upper);
        }

        private static boolean allRowsSelected(final RangeSet<Long> includeRanges, final long tableSize) {
            return includeRanges.encloses(Range.closedOpen(0L, tableSize));
        }

        private static void validateRowNumberOperatorSupported(final RowFilter3NodeSettings settings)
            throws InvalidSettingsException {
            final var op = settings.m_operator;
            CheckUtils.checkSetting(SUPPORTED_OPERATORS.contains(settings.m_operator),
                "Cannot use operator \"%s\" on row numbers.", op.m_label);
        }

        /**
         * Computes a range set given the current settings and input table.
         *
         * @param operator
         * @param outputMode
         * @param size
         * @param rowNumbers
         * @return range set derived from settings, or empty range set if the whole input table is covered
         */
        private static RangeSet<Long> computeRanges(final FilterOperator operator, final OutputMode outputMode,
            final long size, final RowNumbersAnchor rowNumbers) {
            // canonicalize operator to inclusive form if it makes sense (i.e. their ranges/filters are the same)
            final var exclude = outputMode == OutputMode.EXCLUDE;
            var canonicalOperator = canonicalize(operator, exclude);
            // A few notes about ranges and indices/offsets:
            // - The dialog accepts 1-based row numbers but we use 0-based row offsets internally.
            // - RowRangeSelection works with [from,to) intervals.
            // - We always use right-open ranges (`closedOpen` or `open`) to ensure adjacent ranges coalesce
            //   automatically.
            final var k = rowNumbers.m_noOfRows;
            final RangeSet<Long> rangeSet = switch (canonicalOperator) { // NOSONAR
                case EQ, NEQ, LT, LTE, GT, GTE -> computeSingleValueRange(canonicalOperator, rowNumbers.m_rowNumber - 1,
                    size);
                case BETWEEN -> computeRangeBetween(rowNumbers.m_bounds.rangeClosed(rowNumber -> rowNumber - 1), size,
                    exclude);
                case FIRST_N_ROWS -> exclude ? ImmutableRangeSet.of(Range.closedOpen(k, size)) // [N, size)
                    : ImmutableRangeSet.of(Range.closedOpen(0L, k)); // [0, N)
                case LAST_N_ROWS -> exclude ? ImmutableRangeSet.of(Range.closedOpen(0L, size - k)) // [0, size - N]
                    : ImmutableRangeSet.of(Range.closedOpen(size - k, size)); // [size - N, size)
                default -> throw new IllegalArgumentException(
                    "Unsupported table filter operator: " + canonicalOperator);
            };
            return rangeSet;
        }

        private static FilterOperator canonicalize(final FilterOperator operator, final boolean exclude) {
            if (!exclude) {
                return operator;
            }
            return switch (operator) {
                case EQ -> FilterOperator.NEQ;
                case NEQ -> FilterOperator.EQ;
                case LT -> FilterOperator.GTE;
                case LTE -> FilterOperator.GT;
                case GT -> FilterOperator.LTE;
                case GTE -> FilterOperator.LT;
                default -> operator;
            };
        }

        private static RangeSet<Long> computeSingleValueRange(final FilterOperator operator, final long v,
            final long size) {
            return switch (operator) {
                case EQ -> ImmutableRangeSet.of(Range.closedOpen(v, v + 1)); // [v, v]
                case NEQ -> ImmutableRangeSet.<Long> builder().add(Range.closedOpen(0L, v)).add(Range.open(v, size))
                    .build(); // [0, v), (v, size)
                case LT -> ImmutableRangeSet.of(Range.closedOpen(0L, v)); // [0, v)
                case LTE -> ImmutableRangeSet.of(Range.closedOpen(0L, v + 1)); // [0, v]
                case GT -> ImmutableRangeSet.of(Range.open(v, size)); // (v, size)
                case GTE -> ImmutableRangeSet.of(Range.closedOpen(v, size)); // [v, size)
                default -> throw new IllegalArgumentException("Unsupported table filter operator: " + operator);
            };
        }

        private static RangeSet<Long> computeRangeBetween(final Range<Long> betweenIncl, final long size,
            final boolean exclude) {
            final var lowerIncl = betweenIncl.lowerEndpoint();
            final var upperIncl = betweenIncl.upperEndpoint();
            if (!exclude) {
                return ImmutableRangeSet.of(Range.closedOpen(lowerIncl, upperIncl + 1));
            }
            final var builder = ImmutableRangeSet.<Long> builder();
            if (lowerIncl > 0) {
                builder.add(Range.closedOpen(0L, lowerIncl));
            }
            if (upperIncl < size - 1) {
                builder.add(Range.open(upperIncl, size));
            }
            return builder.build();
        }

    }

    /**
     * Creates a function that adds a nicely formatted, padded fraction of the form {@code " 173/2065"} to a given
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

    /* === STREAMING Implementation */

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new RowFilterOperator();
    }

    /**
     * Streamable operator implementation for Row Filter.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    private final class RowFilterOperator extends StreamableOperator {

        @Override
        public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
            throws Exception {
            final var optSettings = getSettings();
            if (optSettings.isEmpty()) {
                return;
            }
            final var settings = optSettings.get();
            final RowInput input = (RowInput)inputs[0];
            final RowOutput output = (RowOutput)outputs[0];
            try {
                if (RowNumberFilter.isFilterOnRowNumbers(settings)) {
                    filterRange(exec, input, output, settings);
                } else {
                    filterOnPredicate(exec, input, output, settings);
                }
            } finally {
                input.close();
                output.close();
            }
        }

        private static void filterOnPredicate(final ExecutionContext exec, final RowInput input, final RowOutput output,
            final RowFilter3NodeSettings settings) throws CanceledExecutionException, InvalidSettingsException {
            final var inSpec = input.getDataTableSpec();
            final var rowPredicate = DataValuePredicate.forSettings(exec, settings, inSpec);
            final var includeMatches = settings.includeMatches();

            final var rowRead = new DataRowAdapter();
            DataRow row;
            try {
                while ((row = input.poll()) != null) {
                    exec.checkCanceled();
                    rowRead.setDataRow(row);
                    if (includeMatches == rowPredicate.test(rowRead)) {
                        output.push(row);
                    }
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private static void filterRange(final ExecutionContext exec, final RowInput input, final RowOutput output,
            final RowFilter3NodeSettings settings) throws CanceledExecutionException {
            final var includeRanges = RowNumberFilter.computeIncludedRanges(settings, Long.MAX_VALUE);

            if (includeRanges.isEmpty()) {
                // nothing selected
                return;
            }
            DataRow row;
            try {
                long rowNumber = 0;
                while ((row = input.poll()) != null) {
                    exec.checkCanceled();
                    if (includeRanges.contains(rowNumber)) {
                        output.push(row);
                    }
                    rowNumber++;
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private static final class DataRowAdapter implements RowRead {

            private DataRow m_row;

            void setDataRow(final DataRow row) {
                m_row = row;
            }

            @Override
            public boolean isMissing(final int index) {
                return m_row.getCell(index).isMissing();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <D extends DataValue> D getValue(final int index) {
                return (D)m_row.getCell(index);
            }

            @Override
            public int getNumColumns() {
                return m_row.getNumCells();
            }

            @Override
            public RowKeyValue getRowKey() {
                return m_row.getKey();
            }
        }

    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        final var settings = getSettings().orElseThrow(() -> new IllegalStateException("Node is not yet configured."));
        if (RowNumberFilter.isFilterOnRowNumbers(settings)) {
            if (RowNumberFilter.isTailFilter(settings)) {
                // in case we need the last n rows, we need the whole table
                return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
            }
            return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_STREAMABLE};
        }
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

}
