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

import org.knime.base.data.filter.row.v2.RowFilter;
import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.ColumnDomains;
import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.node.BufferedDataTable;
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

    private static final long UNKNOWN_SIZE = -1;

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
            final var includedExcludedPartition = RowNumberFilterSpec.computeRowPartition(isAnd,
                RowNumberFilterSpec.toFilterSpec(rowNumberCriteria), settings.outputMode(), tableSize);
            return RowFilter.slice(exec, in, includedExcludedPartition, isSplitter);
        }
        final var inSpec = in.getSpec();
        // TODO (performance): use ALWAYS_TRUE and ALWAYS_FALSE predicates to return input table or empty table
        final var predicate = AbstractRowFilterNodeSettings.createFilterPredicate(isAnd, rowNumberCriteria,
            dataCriteria, inSpec, tableSize);

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
            RowFilter.filterOnPredicate(exec, input, tableSize, matchesCursor, nonMatchesCursor, predicate,
                settings.outputMatches());
            return nonMatches != null ? new BufferedDataTable[]{matches.finish(), nonMatches.finish()}
                : new BufferedDataTable[]{matches.finish()};
        }
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
            // in case of REGEX and WILDCARD operators, we treat the row number column as a data column
            if (c.m_column.getEnumChoice().filter(RowIdentifiers.ROW_NUMBER::equals).isPresent()
                && RowNumberFilterSpec.supportsOperator(c.m_operator)) {
                rowNumberCriteria.add(c);
            } else {
                dataCriteria.add(c);
            }
        }
        return Pair.create(rowNumberCriteria, dataCriteria);
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
        final PortObjectSpec[] inSpecs, final S settings) throws InvalidSettingsException {
        final var rowNumberCriteria = partitionCriteria(settings.m_predicates).getFirst();
        if (AbstractRowFilterNodeSettings.hasLastNFilter(rowNumberCriteria)) {
            return super.createStreamableOperator(partitionInfo, inSpecs, settings);
        }
        return new RowFilterOperator();
    }

    private static final int MATCHING_OUTPUT = 0;

    private static final int NON_MATCHING_OUTPUT = 1;

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
                    final var filterSpecs = RowNumberFilterSpec.toFilterSpec(rowNumberPredicates);
                    final var rowPartition = RowNumberFilterSpec.computeRowPartition(settings.m_matchCriteria.isAnd(),
                        filterSpecs, settings.outputMode(), UNKNOWN_SIZE);
                    final var included = (RowOutput)outputs[MATCHING_OUTPUT];
                    final var excluded = outputs.length > 1 ? (RowOutput)outputs[NON_MATCHING_OUTPUT] : null;
                    RowFilter.filterRange(exec, input, included, excluded, rowPartition);
                } else {
                    // we have to filter on richer predicates
                    final var predicates = partitionCriteria(settings.m_predicates);
                    // TODO(performance): use TRUE and FALSE static predicates to return whole or empty table
                    final var rowPredicate =
                        AbstractRowFilterNodeSettings.createFilterPredicate(settings.m_matchCriteria.isAnd(),
                            predicates.getFirst(), predicates.getSecond(), input.getDataTableSpec(), UNKNOWN_SIZE);
                    final var includeMatches = settings.outputMatches();
                    final var included = (RowOutput)outputs[MATCHING_OUTPUT];
                    final var excluded = outputs.length > 1 ? (RowOutput)outputs[NON_MATCHING_OUTPUT] : null;
                    RowFilter.filterOnPredicate(exec, input, included, excluded, rowPredicate, includeMatches);
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

    }
}
