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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InternalTableAPI;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
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
import org.knime.core.util.Pair;
import org.knime.core.util.valueformat.NumberFormatter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * Implementation of the Row Filter 2 (Labs) node.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // Web UI not API yet
final class RowFilterNodeModel<S extends AbstractRowFilterNodeSettings> extends WebUINodeModel<S> {

    private static final int INPUT = 0;

    private static final int MATCHING_OUTPUT = 0;

    private static final int NON_MATCHING_OUTPUT = 1;

    RowFilterNodeModel(final WebUINodeConfiguration config, final Class<S> settingsClass) {
        super(config, settingsClass);
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final AbstractRowFilterNodeSettings settings)
        throws InvalidSettingsException {
        final var spec = (DataTableSpec)inSpecs[INPUT];
        final var selectedColumn = settings.m_column.getSelected();
        final var selectedType = getDataTypeNameForColumn(selectedColumn, () -> Optional.ofNullable(spec)).orElseThrow(
            () -> new InvalidSettingsException("Cannot get data type for column \"%s\"".formatted(selectedColumn)));

        final var valueClass = settings.m_type;
        if (!valueClass.equals(selectedType)) {
            throw new InvalidSettingsException(
                "Selected column type \"%s\" and value type \"%s\" do not match. Please reconfigure the node."
                    .formatted(selectedType, valueClass));
        }

        if (AbstractRowFilterNodeSettings.isFilterOnRowNumbers(settings)) {
            RowNumberFilter.validateRowNumberOperatorSupported(settings);
            // check if we have number of rows instead of row number
            final var isNumberOfRows =
                FilterOperator.FIRST_N_ROWS == settings.m_operator || FilterOperator.LAST_N_ROWS == settings.m_operator;
            RowNumberFilter.parseInputAsRowNumber(settings.m_value, settings.m_type, isNumberOfRows);
        } else {
            RowReadPredicate.validateSettings(settings, spec);
        }

        return settings.isSecondOutputActive() ? new DataTableSpec[]{spec, spec} : new DataTableSpec[]{spec};
    }

    static Optional<String> getDataTypeNameForColumn(final String selected,
        final Supplier<Optional<DataTableSpec>> dtsSupplier) {
        return getDataTypeForColumn(selected, dtsSupplier).map(DataType::getCellClass).map(Class::getName);
    }

    static Optional<DataType> getDataTypeForColumn(final String selected,
        final Supplier<Optional<DataTableSpec>> dtsSupplier) {
        if (SpecialColumns.ROWID.getId().equals(selected)) {
            return Optional.of(StringCell.TYPE);
        }
        if (SpecialColumns.ROW_NUMBERS.getId().equals(selected)) {
            return Optional.of(LongCell.TYPE);
        }
        if (SpecialColumns.NONE.getId().equals(selected)) {
            throw new IllegalStateException("Column selection required");
        }
        return dtsSupplier.get().map(dts -> dts.getColumnSpec(selected)).map(DataColumnSpec::getType);
    }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] inPortObjects, final ExecutionContext exec,
            final AbstractRowFilterNodeSettings settings) throws Exception {
        final var in = (BufferedDataTable)inPortObjects[INPUT];

        final var isSplitter = settings.isSecondOutputActive();
        if (AbstractRowFilterNodeSettings.isFilterOnRowNumbers(settings)) {
            return RowNumberFilter.sliceTable(exec, in, settings, isSplitter);
        }

        final var inSpec = in.getSpec();
        final Predicate<RowRead> predicate = RowReadPredicate.createFrom(exec, settings, inSpec);
        final var includeMatches = settings.includeMatches();
        final long size = in.size();
        final DataContainerSettings dcSettings = DataContainerSettings.builder() //
                .withInitializedDomain(true) //
                .withCheckDuplicateRowKeys(false) // only copying data
                .withDomainUpdate(false).build();
        try (final var input = in.cursor();
                // take domains from input in order to allow downstream visualizations to retain
                // useful bounds, e.g. [0, 10] for an axis
                final var matches = exec.createRowContainer(inSpec, dcSettings);
                final var matchesCursor = matches.createCursor();
                final var nonMatches = isSplitter ? exec.createRowContainer(inSpec, dcSettings) : null;
                final var nonMatchesCursor = nonMatches != null ? nonMatches.createCursor() : null //
        ) {
            // top-level progress reports number of processed rows as a fraction of the input table size
            final var readRows = new AtomicLong();
            final var msg = progressFractionBuilder(readRows::get, size);
            exec.setProgress(0, () -> msg.apply(new StringBuilder("Processed row ")).toString());

            // sub-progress for reporting the number of matching rows
            final var matchingRows = new AtomicLong();
            final var outputProgress = exec.createSubProgress(0.0);
            final var numFormat = NumberFormatter.builder().setGroupSeparator(",").build();
            outputProgress.setMessage(() -> numFormat.format(matchingRows.get()) + " rows matching");

            while (input.canForward()) {
                exec.checkCanceled();
                final var read = input.forward();
                readRows.incrementAndGet();
                if (includeMatches == predicate.test(read)) {
                    matchesCursor.forward().setFrom(read);
                    matchingRows.incrementAndGet();
                } else if (nonMatchesCursor != null) {
                    nonMatchesCursor.forward().setFrom(read);
                }
                exec.setProgress(1.0 * readRows.get() / size);
            }

            return nonMatches != null ? new BufferedDataTable[]{matches.finish(), nonMatches.finish()}
                : new BufferedDataTable[]{matches.finish()};
        }
    }

    private static final long UNKNOWN_SIZE = -1;

    record RowRange(long fromIncl, long toExcl) {
        RowRange {
            CheckUtils.checkArgument(fromIncl >= 0, "Lower bound must be non-negative, found %d", fromIncl);
            if (toExcl >= 0) {
                CheckUtils.checkArgument(fromIncl < toExcl,
                    "Lower bound must be smaller than upper bound, found %d (lower) >= %d (upper)", fromIncl, toExcl);
            } else {
                CheckUtils.checkArgument(toExcl == UNKNOWN_SIZE,
                    "Expected non-negative upper bound or %d, found %d", UNKNOWN_SIZE, toExcl);
            }
        }

        boolean hasUpperBound() {
            return toExcl != UNKNOWN_SIZE;
        }
    }

    static final class RowNumberFilter {

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

        private static void validateRowNumberOperatorSupported(final AbstractRowFilterNodeSettings settings)
            throws InvalidSettingsException {
            final var op = settings.m_operator;
            CheckUtils.checkSetting(SUPPORTED_OPERATORS.contains(settings.m_operator),
                "Cannot use operator \"%s\" on row numbers.", op.m_label);
        }

        private static long parseInputAsRowNumber(final String value, final String type, final boolean isNumberOfRows)
            throws InvalidSettingsException {
            // row numbers are of long type
            if (!LongCell.TYPE.getCellClass().getName().equals(type)) {
                throw new InvalidSettingsException(
                    "Unexpected row number value input \"%s\" of type \"%s\"".formatted(value, type));
            }
            try {
                final var rowNumber = Long.parseLong(value);
                if (!isNumberOfRows) {
                    CheckUtils.checkSetting(rowNumber > 0, "Row number must be larger than zero: %d", rowNumber);
                } else {
                    CheckUtils.checkSetting(rowNumber >= 0, "Number of rows must not be negative: %d", rowNumber);
                }
                return rowNumber;
            } catch (NumberFormatException e) {
                throw new InvalidSettingsException("Cannot interpret value \"%s\" as row number.".formatted(value), e);
            }
        }

        static BufferedDataTable[] sliceTable(final ExecutionContext exec, final BufferedDataTable in,
                final AbstractRowFilterNodeSettings settings, final boolean isSplitter)
                throws CanceledExecutionException, InvalidSettingsException {
            final var includedExcludedPartition = computeRowPartition(settings, in.size());

            final var includedRanges = includedExcludedPartition.getFirst();
            if (includedRanges.length == 0) {
                // no rows are included
                final var empty = exec.createVoidTable(in.getSpec());
                return isSplitter ? new BufferedDataTable[]{empty, in} : new BufferedDataTable[]{empty};
            }

            final var excludedRanges = includedExcludedPartition.getSecond();
            if (excludedRanges.length == 0) {
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

        private static Pair<RowRange[], RowRange[]> computeRowPartition(final AbstractRowFilterNodeSettings settings,
                final long optionalTableSize) throws InvalidSettingsException {
            final var operator = settings.m_operator;
            final var value = parseInputAsRowNumber(settings.m_value, settings.m_type,
                operator == FilterOperator.FIRST_N_ROWS || operator == FilterOperator.LAST_N_ROWS);
            final var matchedNonMatchedPartition = computePartition(operator, value, optionalTableSize);

            // determine whether matched or non-matched rows are included in the first output, flip pair as needed
            return settings.m_outputMode == FilterMode.INCLUDE ? matchedNonMatchedPartition
                : Pair.create(matchedNonMatchedPartition.getSecond(), matchedNonMatchedPartition.getFirst());
        }

        private static BufferedDataTable slicedFromRanges(final ExecutionContext exec, final BufferedDataTable in,
                final RowRange[] includeRanges) throws CanceledExecutionException {

            if (includeRanges.length < 2) {
                return includeRanges.length == 0 ? exec.createVoidTable(in.getDataTableSpec())
                    : InternalTableAPI.slice(exec, in, toSelection(includeRanges[0]));
            }

            final var subTables = new BufferedDataTable[includeRanges.length];
            for (var i = 0; i < includeRanges.length; i++) {
                final var sliceExec = exec.createSubExecutionContext(0.5 / includeRanges.length);
                subTables[i] = InternalTableAPI.slice(sliceExec, in, toSelection(includeRanges[i]));
                sliceExec.setProgress(1.0, (String)null);
            }

            final var concatProgress = exec.createSubProgress(0.5);
            try {
                return exec.createConcatenateTable(concatProgress, subTables);
            } finally {
                concatProgress.setProgress(1.0, (String)null);
            }
        }

        private static Selection toSelection(final RowRange range) {
            return Selection.all().retainRows(range.fromIncl, range.toExcl);
        }

        /**
         * Computes the sets of rows which are matched/not matched by the given operator and value as a pair of
         * sorted arrays of non-overlapping {@link RowRange}.
         * <ul>
         *   <li>The lower bounds of all ranges in the sets are always inclusive.</li>
         *   <li>The upper bound is absent iff it is the end of the input and the table size is unknown.
         *       Otherwise the upper bound is guaranteed to be exclusive.</li>
         * </ul>
         *
         * @param operator operator as selected in the dialog
         * @param value comparison value as specified in the dialog
         * @param optionalTableSize table size if known, {@link #UNKNOWN_SIZE} otherwise
         * @return pair where the first/second element represents the matched/non-matched row indices, respectively
         */
        static Pair<RowRange[], RowRange[]> computePartition(final FilterOperator operator,
                final long value, final long optionalTableSize) {
            final FilterOperator indexOperator;
            final long offsetNonNeg;
            if (operator == FilterOperator.FIRST_N_ROWS) {
                indexOperator = FilterOperator.LT;
                offsetNonNeg = value;
            } else if (operator == FilterOperator.LAST_N_ROWS) {
                CheckUtils.check(optionalTableSize != UNKNOWN_SIZE, IllegalStateException::new,
                        () -> "Expected table size for filter operator \"%s\"".formatted(operator.label()));
                indexOperator = FilterOperator.GTE;
                // if the table has fewer than `n` rows, return the whole table
                offsetNonNeg = Math.max(0, optionalTableSize - value);
            } else {
                // the dialog accepts 1-based row numbers but we use 0-based row offsets internally
                indexOperator = operator;
                offsetNonNeg = value - 1;
            }

            final var complementIndexOperator = switch (indexOperator) {
                case EQ -> FilterOperator.NEQ;
                case NEQ -> FilterOperator.EQ;
                case LT -> FilterOperator.GTE;
                case LTE -> FilterOperator.GT;
                case GT -> FilterOperator.LTE;
                case GTE -> FilterOperator.LT;
                default -> throw new IllegalArgumentException("Unsupported table filter operator: " + indexOperator);
            };

            return Pair.create(rowIndexRanges(indexOperator, offsetNonNeg, optionalTableSize),
                    rowIndexRanges(complementIndexOperator, offsetNonNeg, optionalTableSize));
        }

        private static RowRange[] rowIndexRanges(final FilterOperator operator, final long indexNonNeg,
                final long optSize) {
            final var rangesOut = new ArrayList<RowRange>();
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
            return rangesOut.toArray(RowRange[]::new);
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
        private static void addRangeIfNonEmpty(final List<RowRange> out, final long lowerIncl, final long upperExcl,
                final long optSize) {
            if (lowerIncl < 0) {
                // possible if `operator == GT` and `indexNonNeg == Long.MAX_VALUE`, empty selection
                return;
            }

            final var sizeUpperBound = optSize < 0 ? Long.MAX_VALUE : optSize;
            final var lowerInclClamped = Math.min(lowerIncl, sizeUpperBound);
            if (upperExcl < 0) {
                // may be from overflow (if `lowerIncl == Long.MAX_VALUE`) or unknown size
                out.add(new RowRange(lowerInclClamped, UNKNOWN_SIZE));
            }

            final var upperExclClamped = Math.min(upperExcl, sizeUpperBound);
            if (lowerInclClamped < upperExclClamped) {
                out.add(new RowRange(lowerInclClamped, upperExclClamped));
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

    /* ========================================== STREAMING Implementation ========================================== */

    @Override
    public InputPortRole[] getInputPortRoles() {
        final var settings = assertSettings();
        if (AbstractRowFilterNodeSettings.isFilterOnRowNumbers(settings)) {
            if (AbstractRowFilterNodeSettings.isLastNFilter(settings)) {
                return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
            }
            return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_STREAMABLE};
        }
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        final var out = new OutputPortRole[getNrOutPorts()];
        Arrays.fill(out, OutputPortRole.DISTRIBUTED);
        return out;
    }

    private AbstractRowFilterNodeSettings assertSettings() {
        return getSettings().orElseThrow(() -> new IllegalStateException("Node is not yet configured."));
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final var settings = assertSettings();
        if (AbstractRowFilterNodeSettings.isFilterOnRowNumbers(settings) && AbstractRowFilterNodeSettings.isLastNFilter(settings)) {
            return super.createStreamableOperator(partitionInfo, inSpecs);
        }
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
            final var settings = assertSettings();
            final RowInput input = (RowInput)inputs[INPUT];
            try {
                if (AbstractRowFilterNodeSettings.isFilterOnRowNumbers(settings)) {
                    filterRange(exec, input, outputs, settings);
                } else {
                    filterOnPredicate(exec, input, outputs, settings);
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                input.close();
                for (final var out : outputs) {
                    ((RowOutput)out).close();
                }
            }
        }

        private static void filterOnPredicate(final ExecutionContext exec, final RowInput input,
                final PortOutput[] outputs, final AbstractRowFilterNodeSettings settings)
                throws CanceledExecutionException, InvalidSettingsException, InterruptedException {
            final var inSpec = input.getDataTableSpec();
            final var rowPredicate = RowReadPredicate.createFrom(exec, settings, inSpec);
            final var includeMatches = settings.includeMatches();

            // the only stats to report are read and included rows, we don't know the size of the input
            final var numFormat = NumberFormatter.builder().setGroupSeparator(",").build();
            final var matchedRead = new long[2];
            final Supplier<String> progress = () -> "%s/%s rows included" //
                .formatted(numFormat.format(matchedRead[0]), numFormat.format(matchedRead[1]));

            final var rowRead = new DataRowAdapter();
            final var included = (RowOutput)outputs[MATCHING_OUTPUT];
            final var excluded = outputs.length > 1 ? (RowOutput)outputs[NON_MATCHING_OUTPUT] : null;
            for (DataRow row; (row = input.poll()) != null;) {
                exec.checkCanceled();
                matchedRead[1]++;

                rowRead.setDataRow(row);
                if (includeMatches == rowPredicate.test(rowRead)) {
                    included.push(row);
                    matchedRead[0]++;
                } else if (excluded != null) {
                    excluded.push(row);
                }
                exec.setMessage(progress);
            }
        }

        private static void filterRange(final ExecutionContext exec, final RowInput input, // NOSONAR
                final PortOutput[] outputs, final AbstractRowFilterNodeSettings settings)
                throws CanceledExecutionException, InterruptedException, InvalidSettingsException {
            final var isSplitter = outputs.length > 1;
            final var rowPartition = RowNumberFilter.computeRowPartition(settings, UNKNOWN_SIZE);
            final var includeRanges = rowPartition.getFirst();

            // the only stats to report are read and included rows, we don't know the size of the input
            final var numFormat = NumberFormatter.builder().setGroupSeparator(",").build();
            final var matchedRead = new long[2];
            final Supplier<String> progress = () -> "%s/%s rows included" //
                .formatted(numFormat.format(matchedRead[0]), numFormat.format(matchedRead[1]));

            final var included = (RowOutput)outputs[MATCHING_OUTPUT];
            final var excluded = isSplitter ? (RowOutput)outputs[NON_MATCHING_OUTPUT] : null;

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
            DataRow row;
            for (var nextRange = 0;; nextRange++) {

                // === EXCLUDE ROWS state ===
                final long lastExclRow;
                if (nextRange < includeRanges.length) {
                    // there's another include range ahead, everything before is excluded
                    lastExclRow = includeRanges[nextRange].fromIncl() - 1;
                } else {
                    // only excluded rows remain
                    if (excluded == null) {
                        // not a splitter, nothing left to do
                        return;
                    }
                    included.close();
                    lastExclRow = Long.MAX_VALUE; // effectively makes `while` condition below `true`
                }
                while (matchedRead[1] <= lastExclRow) {
                    exec.checkCanceled();
                    if ((row = input.poll()) == null) {
                        return;
                    }
                    if (excluded != null) {
                        excluded.push(row);
                    }
                    matchedRead[1]++;
                    exec.setMessage(progress);
                }

                // === INCLUDE ROWS state ===
                final var currentRange = includeRanges[nextRange];
                final long lastInclRow;
                if (currentRange.hasUpperBound()) {
                    // current include range ends somewhere
                    lastInclRow = currentRange.toExcl() - 1;
                } else {
                    // only included rows remain
                    if (excluded != null) {
                        excluded.close();
                    }
                    lastInclRow = Long.MAX_VALUE; // effectively makes `while` condition below `true`
                }
                while (matchedRead[1] <= lastInclRow) {
                    exec.checkCanceled();
                    if ((row = input.poll()) == null) {
                        return;
                    }
                    included.push(row);
                    matchedRead[1]++;
                    matchedRead[0]++;
                    exec.setMessage(progress);
                }
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
}
