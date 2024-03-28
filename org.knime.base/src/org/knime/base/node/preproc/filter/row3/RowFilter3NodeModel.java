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

import java.util.Collection;
import java.util.List;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
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
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

import com.google.common.collect.Range;

/**
 * Implementation of the Row Filter 2 (TODO: name change?) node
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // Web UI not API yet
final class RowFilter3NodeModel extends WebUINodeModel<RowFilter3NodeSettings> {

    /**
     * Holds the row predicate which is created at configure time
     */
    private RowPredicate<?> m_predicate;

    protected RowFilter3NodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, RowFilter3NodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final RowFilter3NodeSettings settings)
        throws InvalidSettingsException {
        CheckUtils.checkSetting(settings.m_compareOn != null || settings.m_operator == FilterOperator.IS_MISSING,
            "Not able to filter with these settings, please reconfigure the node.");
        m_predicate = createPredicate(inSpecs[0], settings);
        return inSpecs;
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final RowFilter3NodeSettings settings) throws Exception {
        final var in = inData[0];
        final var out = exec.createDataContainer(in.getSpec());

        final var rowIndexRanges = computeRowIndexRange(in.size(), settings);

        try (final var inIter = in.iterator()) {
            final FilteredRowIterator filtered;
            if (settings.includeMatches()) {
                filtered = FilteredRowIterator.includeRows(inIter, m_predicate, rowIndexRanges);
            } else {
                filtered = FilteredRowIterator.excludeRows(inIter, m_predicate, rowIndexRanges);
            }

            while (filtered.hasNext()) {
                exec.checkCanceled();
                out.addRowToTable(filtered.next());
            }
        } finally {
            out.close();
        }
        return new BufferedDataTable[]{out.getTable()};
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext ctx)
                throws Exception {
                final var settings = getSettings();
                if (settings.isEmpty()) {
                    return;
                }
                final RowInput inData = (RowInput)inputs[0];
                final RowOutput output = (RowOutput)outputs[0];
                try {
                    final var rowPredicate =
                        RowFilter3NodeModel.createPredicate(inData.getDataTableSpec(), settings.get());
                    RowFilter3NodeModel.filterInput(inData, output, rowPredicate, ctx, settings.get().includeMatches());
                } finally {
                    inData.close();
                    output.close();
                }
            }
        };
    }

    /**
     * Checks every row, if the test is passed, it is pushed to the output rows.
     *
     * @throws CanceledExecutionException
     */
    private static void filterInput(final RowInput inData, final RowOutput outData, final RowPredicate<?> rowPredicate,
        final ExecutionContext exec, final boolean include) throws InterruptedException, CanceledExecutionException {
        DataRow row;
        while ((row = inData.poll()) != null) {
            exec.checkCanceled();
            if (!include ^ rowPredicate.test(row)) {
                outData.push(row);
            }
        }
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        final var settings = getSettings();
        if (settings.isEmpty() || SpecialColumns.ROW_NUMBERS.getId().equals(settings.get().m_column.getSelected())) {
            return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE}; // need the whole table
        }
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        final var settings = getSettings();
        if (settings.isEmpty() || SpecialColumns.ROW_NUMBERS.getId().equals(settings.get().m_column.getSelected())) {
            return new OutputPortRole[]{OutputPortRole.NONDISTRIBUTED}; // need the whole table for row numbers
        }
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /**
     * Create a range or ranges that include the row indices that we want to include in the output
     *
     * @param size the table size
     * @param settings
     * @return a collection of ranges
     * @throws InvalidSettingsException
     */
    private static Collection<Range<Long>> computeRowIndexRange(final long size, final RowFilter3NodeSettings settings)
        throws InvalidSettingsException {
        if (!SpecialColumns.ROW_NUMBERS.getId().equals(settings.m_column.getSelected())) {
            return List.of(Range.all());
        }
        // adjust with "-1" because row numbers are 1-based
        final var lb = settings.m_anchors.m_integer.m_bounds.m_lowerBound - 1;
        final var ub = settings.m_anchors.m_integer.m_bounds.m_upperBound - 1;
        final var val = settings.m_anchors.m_integer.m_value - 1;
        final var nrOfRows = settings.m_anchors.m_integer.m_noOfRows;
        return switch (settings.m_operator) {
            case EQ -> List.of(Range.singleton(val));
            case NEQ -> List.of(Range.lessThan(val), Range.greaterThan(val));
            case LT -> List.of(Range.lessThan(val));
            case LTE -> List.of(Range.atMost(val));
            case GT -> List.of(Range.greaterThan(val));
            case GTE -> List.of(Range.atLeast(val));
            case BETWEEN -> List.of(Range.closed(lb, ub));
            case FIRST_N_ROWS -> List.of(Range.closedOpen(0L, nrOfRows));
            case LAST_N_ROWS -> List.of(Range.closedOpen(size - nrOfRows, size));
            default -> throw new InvalidSettingsException(
                "Unexpected operator for row number range: " + settings.m_operator);
        };
    }

    /**
     * Create a row predicate based on the provided settings and input table spec
     *
     * @param spec
     * @param settings
     * @return
     * @throws InvalidSettingsException
     */
    private static RowPredicate<?> createPredicate(final DataTableSpec spec, final RowFilter3NodeSettings settings)
        throws InvalidSettingsException {
        if (SpecialColumns.ROW_NUMBERS.getId().equals(settings.m_column.getSelected())) {
            return RowPredicate.truePredicate(); // Row numbers are handled elsewhere
        }
        return RowPredicate.forSettings(settings, spec);
    }

}
