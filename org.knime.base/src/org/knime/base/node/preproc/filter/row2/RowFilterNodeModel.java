/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   29.06.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row2;

import org.knime.base.node.preproc.filter.row2.operator.RowPredicate;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataContainer;
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
import org.knime.core.node.streamable.StreamableOperatorInternals;

/**
 * Model of a node filtering rows. It keeps an instance of a row filter, which tells whether or not to include a row
 * into the result, and a range to allow for index based filtering. The reason the range is kept separately and not in a
 * normal filter instance is performance. If we are leaving the row number range we can immediately flag the end of the
 * table, while if we would use a filter instance we would have to run to the end of the input table (always getting a
 * mismatch because the row number is out of the valid range).
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class RowFilterNodeModel extends AbstractRowFilterNodeModel {

    /**
     * Creates a new Row Filter Node Model.
     */
    protected RowFilterNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        configureInputSpec(inSpecs[0]);
        return inSpecs;
    }

    /**
     * Checks every row if the test is passed, it is pushed in the output rows.
     *
     * @throws CanceledExecutionException
     */
    private static void filterInput(final RowInput inData, final RowOutput outData, final RowPredicate rowPredicate,
        final ExecutionContext exec) throws InterruptedException, CanceledExecutionException {
        DataRow row;
        for (long i = 0; (row = inData.poll()) != null; i++) {
            exec.checkCanceled();
            if (rowPredicate.test(row, i)) {
                outData.push(row);
            }
        }
        inData.close();
        outData.close();
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable in = inData[0];
        final var dcSettings = DataContainerSettings.builder() //
                .withInitializedDomain(true) //
                .withDomainUpdate(true) // columns without domain need a domain (historical reasons, backw. compat.)
                .withCheckDuplicateRowKeys(false) // only copying data
                .build();
        final BufferedDataContainer container = exec.createDataContainer(in.getDataTableSpec(), dcSettings);
        exec.setMessage("Searching first matching row...");
        // Create RowPredicate
        final RowPredicate rowPredicate = createRowPredicate(in.getDataTableSpec());
        final long totalRowCount = in.size();
        final double totalRowCountDouble = totalRowCount;
        // progress is handled here, so the subprogress for the iterator should have no contribution to the progress
        try (CloseableRowIterator rowIterator =
            in.filter(createTableFilter(rowPredicate), exec.createSilentSubProgress(0)).iterator()) {
            for (long i = getStartIdx(rowPredicate); rowIterator.hasNext(); i++) {
                exec.checkCanceled();
                final DataRow row = rowIterator.next();
                final long finalI = i;
                exec.setProgress(i / totalRowCountDouble,
                    () -> String.format("Testing row %s/%s, (%s)", finalI, totalRowCount, row.getKey()));
                if (rowPredicate.test(row, i)) {
                    container.addRowToTable(row);
                }
            }
        }
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    private static long getStartIdx(final RowPredicate rowPredicate) {
        if (rowPredicate.getRowIndexRange().hasLowerBound()) {
            return rowPredicate.getRowIndexRange().lowerEndpoint();
        } else {
            return 0;
        }
    }

    private static TableFilter createTableFilter(final RowPredicate rowPredicate) {
        TableFilter.Builder tableFilterBuilder = new TableFilter.Builder();
        if (rowPredicate.getRowIndexRange().hasLowerBound()) {
            tableFilterBuilder = tableFilterBuilder.withFromRowIndex(rowPredicate.getRowIndexRange().lowerEndpoint());
        }
        if (rowPredicate.getRowIndexRange().hasUpperBound()) {
            tableFilterBuilder = tableFilterBuilder.withToRowIndex(rowPredicate.getRowIndexRange().upperEndpoint());
        }
        return tableFilterBuilder.build();
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public StreamableOperatorInternals saveInternals() {
                return null;
            }

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext ctx)
                throws Exception {
                final RowInput inData = (RowInput)inputs[0];
                final RowOutput output = (RowOutput)outputs[0];
                final RowPredicate rowPredicate = createRowPredicate(inData.getDataTableSpec());

                RowFilterNodeModel.filterInput(inData, output, rowPredicate, ctx);
            }
        };
    }

}
