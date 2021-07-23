package org.knime.base.node.meta.looper.recursive;
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
 * ------------------------------------------------------------------------
 *
 */

import java.io.File;
import java.io.IOException;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.LoopStartNode;

/**
 * This is the model implementation of Recursive Loop Start node with an arbitrary amount of ports.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author based on {@link RecursiveLoopStartNodeModel} by Iris Adae, University of Konstanz, Germany
 */
public class RecursiveLoopStartDynamicNodeModel extends NodeModel implements LoopStartNode {

    private int m_currentiteration;

    /**
     * Constructor for the node model.
     *
     * @param inPorts the input ports
     * @param outPorts the output ports
     */
    protected RecursiveLoopStartDynamicNodeModel(final PortType[] inPorts, final PortType[] outPorts) {
        super(inPorts, outPorts);
    }

    /**
     * Check if the loop end is connected to the correct loop start.
     *
     * @return a function that gets the table with the specified port
     */
    private IntFunction<BufferedDataTable> getTableSupplier() {
        final var len = getLoopEndNode();
        if (len instanceof RecursiveLoopEndDynamicNodeModel) {
            final var clen = (RecursiveLoopEndDynamicNodeModel)len;
            CheckUtils.checkState(clen.getNrRecursionPorts() == getNrRecursionPorts(),
                "Connected Recursive Loop End node has not the same amount of recursion ports! Start node: %d, End node: %d",
                getNrRecursionPorts(), clen.getNrRecursionPorts());
            return clen::getInData;
        } else {
            throw new IllegalStateException(
                "Loop Start is not connected to matching/corresponding Recursive Loop End node.");
        }
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        final BufferedDataTable[] result;
        if (m_currentiteration == 0) {
            // just output the complete data table.
            result = inData;
        } else {
            final var supplier = getTableSupplier();
            //otherwise we get the data from the loop end node
            final var ports = getNrRecursionPorts();
            final var fraction = 1.0 / ports;
            result = IntStream.range(0, ports)//
                .mapToObj(supplier::apply)//
                .map(table -> cloneTableFromLoopEndNode(table, exec, exec.createSubProgress(fraction)))//
                .toArray(BufferedDataTable[]::new);
        }
        pushFlowVariableInt("currentIteration", m_currentiteration);

        m_currentiteration++;

        return result;
    }

    int getNrRecursionPorts() {
        return getNrInPorts();
    }

    /**
     * Added as part of AP-13748 ("Error when saving workflow with failure in Recursive Loop End") -- it copies the
     * table retrieved from the end node. Necessary so that the loop start node can claim ownership of the data. This
     * code may become obsolete when AP-8712 is implemented (data created in the loop is then automatically associated
     * with the outmost loop start node).
     */
    static BufferedDataTable cloneTableFromLoopEndNode(final BufferedDataTable table, final ExecutionContext exec,
        final ExecutionMonitor progress) {
        final var cloneContainer = exec.createDataContainer(table.getSpec(), true);
        try (final var iterator = table.iterator()) {
            for (long rowIndex = 0L, rowCount = table.size(); iterator.hasNext(); rowIndex++) {
                final var row = iterator.next();
                // this does not "copy" file stores since they are already associated with the correct loop start node
                cloneContainer.addRowToTable(row);
                final var rowIndexFinal = rowIndex + 1; // only for progress message
                progress.setProgress((double)rowIndex / rowCount,
                    () -> String.format("Row %d/%d (\"%s\")", rowIndexFinal, rowCount, row.getKey()));
            }
            cloneContainer.close();
        }
        return cloneContainer.getTable();
    }

    @Override
    protected void reset() {
        m_currentiteration = 0;
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        pushFlowVariableInt("currentIteration", m_currentiteration);

        return inSpecs;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // no settings
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no settings
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no settings
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to load
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to save
    }

}
