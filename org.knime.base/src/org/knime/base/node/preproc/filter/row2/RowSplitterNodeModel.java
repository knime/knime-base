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
 *   Jan 23, 2020 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row2;

import org.knime.base.node.preproc.filter.row2.operator.RowPredicate;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
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
 * This class is the model implementation of the "RowSplitter" node. It splits the rows into two tables: one holding the
 * matches (all the rows that fulfill the filtering criteria) and one holding all the misses (all the rows that do not
 * fulfill the matching criteria).
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
final class RowSplitterNodeModel extends AbstractRowFilterNodeModel {


    /**
     * Creates a new Row Splitter Node Model.
     */
    protected RowSplitterNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec dataTableSpec = inSpecs[0];
        configureInputSpec(dataTableSpec);
        return new DataTableSpec[]{dataTableSpec, dataTableSpec};
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable inTable = inData[0];
        final DataTableRowInput rowInput = new DataTableRowInput(inTable);
        final DataTableSpec inSpec = inTable.getDataTableSpec();
        final BufferedDataContainer match = exec.createDataContainer(inSpec);
        final BufferedDataTableRowOutput matchOutput = new BufferedDataTableRowOutput(match);
        final BufferedDataContainer miss = exec.createDataContainer(inSpec);
        final BufferedDataTableRowOutput missOutput = new BufferedDataTableRowOutput(miss);
        final RowPredicate rowPredicate = createRowPredicate(inSpec);
        RowSplitterNodeModel.filterInput(rowInput, matchOutput, missOutput, rowPredicate, exec, inTable.size());
        return new BufferedDataTable[]{matchOutput.getDataTable(), missOutput.getDataTable()};
    }

    /**
     * Checks the rows one by one and pushes it in the match output if the criteria are fulfilled, otherwise in the miss
     * output rows.
     *
     * @param inData holds the input row.
     * @param matchData holds the rows fulfilling the filtering criteria.
     * @param missData holds the rows not fulfilling the filtering criteria.
     * @param rowPredicate holds the {@link RowPredicate} to test the rows one by one.
     * @param exec holds the {@link ExecutionContext}.
     * @throws InterruptedException thrown if the execution is interrupted.
     * @throws CanceledExecutionException thrown if the execution is canceled.
     */
    private static void filterInput(final RowInput inData, final RowOutput matchData, final RowOutput missData,
        final RowPredicate rowPredicate, final ExecutionContext exec, final long tableLength)
        throws InterruptedException, CanceledExecutionException {
        DataRow row;
        for (long i = 0; (row = inData.poll()) != null; i++) {
            exec.checkCanceled();
            if (rowPredicate.test(row, i)) {
                matchData.push(row);
            } else {
                missData.push(row);
            }
            exec.getProgressMonitor().setProgress((double) i/tableLength);
        }
        matchData.close();
        missData.close();
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED, OutputPortRole.DISTRIBUTED};
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
                final RowOutput matchData = (RowOutput)outputs[0];
                final RowOutput missData = (RowOutput)outputs[1];
                final RowPredicate rowPredicate = createRowPredicate(inData.getDataTableSpec());

                RowSplitterNodeModel.filterInput(inData, matchData, missData, rowPredicate, ctx, -1);
            }
        };
    }
}
