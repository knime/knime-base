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
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.ColumnDomains;
import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
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
import org.knime.core.util.Pair;
import org.knime.core.util.valueformat.NumberFormatter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * Implementation of the Row Filter node based on the webui.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // Web UI not API yet
final class RowFilterNodeModel<S extends AbstractRowFilterNodeSettings> extends WebUINodeModel<S> {

    private static final int INPUT = 0;

    private static final int MATCHING_OUTPUT = 0;

    private static final int NON_MATCHING_OUTPUT = 1;

    static final long UNKNOWN_SIZE = -1;

    RowFilterNodeModel(final WebUINodeConfiguration config, final Class<S> settingsClass) {
        super(config, settingsClass);
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final AbstractRowFilterNodeSettings settings)
        throws InvalidSettingsException {
        if (settings.m_predicates.length == 0) {
            throw new InvalidSettingsException("At least one filter criterion is needed.");
        }

        final var spec = (DataTableSpec)inSpecs[INPUT];
        settings.validate(spec);

        return settings.isSecondOutputActive() ? new DataTableSpec[]{spec, spec} : new DataTableSpec[]{spec};
    }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] inPortObjects, final ExecutionContext exec,
            final AbstractRowFilterNodeSettings settings) throws Exception {
        final var in = (BufferedDataTable)inPortObjects[INPUT];
        final var spec = in.getSpec();
        settings.validate(spec);

        final var isSplitter = settings.isSecondOutputActive();

        final var isAnd = settings.m_matchCriteria.isAnd();
        // separate row numbers from all other criteria
        final var predicatePartition = partitionCriteria(settings.m_predicates);
        final var rowNumberCriteria = predicatePartition.getFirst();
        final var dataCriteria = predicatePartition.getSecond();

        final var tableSize = in.size();
        if (dataCriteria.isEmpty()) {
            // slicing-only is possible since we never look at any column
            final var includedExcludedPartition =
                    RowNumberFilter.computeRowPartition(isAnd, RowNumberFilter.getAsFilterSpecs(rowNumberCriteria),
                        settings.outputMode(), tableSize);
            return RowNumberFilter.sliceTable(exec, in, includedExcludedPartition, isSplitter);
        }
        final var inSpec = in.getSpec();
        final var predicate = createFilterPredicate(isAnd, rowNumberCriteria, dataCriteria, inSpec, tableSize);

        // inherit domain from spec?
        final var initializedDomain = settings.m_domains == ColumnDomains.RETAIN;
        // update while adding?
        final var domainUpdate = settings.m_domains == ColumnDomains.COMPUTE;
        final var dcSettings = DataContainerSettings.builder() //
                .withInitializedDomain(initializedDomain) //
                .withDomainUpdate(domainUpdate) // (note that older versions DO update domains, for historical reasons)
                .withCheckDuplicateRowKeys(false) // only copying data
                .build();
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
            final var msg = progressFractionBuilder(readRows::get, tableSize);
            exec.setProgress(0, () -> msg.apply(new StringBuilder("Processed row ")).toString());

            // sub-progress for reporting the number of matching rows
            final var matchingRows = new AtomicLong();
            final var outputProgress = exec.createSubProgress(0.0);
            final var numFormat = NumberFormatter.builder().setGroupSeparator(",").build();
            outputProgress.setMessage(() -> numFormat.format(matchingRows.get()) + " rows matching");

            final var includeMatches = settings.outputMatches();
            while (input.canForward()) {
                exec.checkCanceled();
                final var read = input.forward();
                final var index = readRows.getAndIncrement();
                if (includeMatches == predicate.test(index, read)) {
                    matchesCursor.forward().setFrom(read);
                    matchingRows.incrementAndGet();
                } else if (nonMatchesCursor != null) {
                    nonMatchesCursor.forward().setFrom(read);
                }
                exec.setProgress(1.0 * readRows.get() / tableSize);
            }

            return nonMatches != null ? new BufferedDataTable[]{matches.finish(), nonMatches.finish()}
                : new BufferedDataTable[]{matches.finish()};
        }
    }

    private static RowFilterPredicate createFilterPredicate(final boolean isAnd,
        final List<FilterCriterion> rowNumberCriteria, final List<FilterCriterion> dataCriteria,
        final DataTableSpec spec, final long tableSize) throws InvalidSettingsException {
        final var rowNumbers = RowNumberPredicate.buildPredicate(isAnd, rowNumberCriteria, tableSize);
        final var data = RowReadPredicate.buildPredicate(isAnd, dataCriteria, spec);
        if (rowNumbers == null) {
            return (index, read) -> data.test(read);
        }
        if (data == null) {
            throw new IllegalStateException("Row number predicate without data predicate, should have used slicing");
        }
        return isAnd ? (index, read) -> rowNumbers.test(index) && data.test(read) // NOSONAR this is not too hard to read
            : (index, read) -> rowNumbers.test(index) || data.test(read); // NOSONAR see above
    }

    /**
     * Partitions the criteria into row number and data (incl. RowID) criteria.
     *
     * @param criteria list of criteria to partition
     * @param colSpecSupplier supplier for columns spec given a column name (incl. {@link SpecialColumns}).
     * @return row number and data criteria
     */
    private static Pair<List<FilterCriterion>, List<FilterCriterion>>
            partitionCriteria(final FilterCriterion[] criteria) {
        final var rowNumberCriteria = new ArrayList<FilterCriterion>();
        final var dataCriteria = new ArrayList<FilterCriterion>();
        for (final var c : criteria) {
            final var selected = c.m_column.getSelected();
            if (AbstractRowFilterNodeSettings.isRowNumberSelected(selected)) {
                rowNumberCriteria.add(c);
            } else {
                dataCriteria.add(c);
            }
        }
        return Pair.create(rowNumberCriteria, dataCriteria);
    }



    @FunctionalInterface
    interface RowFilterPredicate {
        /**
         * Tests the predicate with the supplied row index (0-based) and {@link RowRead row read}.
         * @param index 0-based index of row
         * @param read row data
         * @return {@code true} if the predicate matches the supplied row, {@code false} otherwise
         */
        boolean test(final long index, final RowRead read);
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
        final var rowNumberCriteria = partitionCriteria(assertSettings().m_predicates).getFirst();
        if (!rowNumberCriteria.isEmpty()) {
            if (AbstractRowFilterNodeSettings.hasLastNFilter(rowNumberCriteria)) {
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
        final var rowNumberCriteria = partitionCriteria(assertSettings().m_predicates).getFirst();
        if (AbstractRowFilterNodeSettings.hasLastNFilter(rowNumberCriteria)) {
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
                final var rowNumberAndDataPredicates = partitionCriteria(settings.m_predicates);
                final var rowNumberPredicates = rowNumberAndDataPredicates.getFirst();
                final var dataPredicates = rowNumberAndDataPredicates.getSecond();
                if (!rowNumberPredicates.isEmpty() && dataPredicates.isEmpty()) {
                    // we can only filter based on row numbers
                    filterRange(exec, input, outputs, settings);
                } else {
                    // we have to filter on richer predicates
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

            final var predicates = partitionCriteria(settings.m_predicates);
            final var rowPredicate = createFilterPredicate(settings.m_matchCriteria.isAnd(),
                predicates.getFirst(), predicates.getSecond(), inSpec, UNKNOWN_SIZE);

            final var includeMatches = settings.outputMatches();

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
                final var index = matchedRead[1];
                matchedRead[1]++;

                rowRead.setDataRow(row);
                if (includeMatches == rowPredicate.test(index, rowRead)) {
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

            final var rowNumberCriteria = partitionCriteria(settings.m_predicates).getFirst();
            final var filterSpecs = RowNumberFilter.getAsFilterSpecs(rowNumberCriteria);

            final var rowPartition = RowNumberFilter.computeRowPartition(settings.m_matchCriteria.isAnd(),
                filterSpecs, settings.outputMode(), UNKNOWN_SIZE);
            final var includeRanges = rowPartition.matching().asRanges().stream().toList();
            final var numIncludeRanges = includeRanges.size();

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
                if (nextRange < numIncludeRanges) {
                    // there's another include range ahead, everything before is excluded
                    lastExclRow = includeRanges.get(nextRange).lowerEndpoint() - 1;
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
                final var currentRange = includeRanges.get(nextRange);
                final long lastInclRow;
                if (currentRange.hasUpperBound()) {
                    // current include range ends somewhere
                    lastInclRow = currentRange.upperEndpoint() - 1;
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
