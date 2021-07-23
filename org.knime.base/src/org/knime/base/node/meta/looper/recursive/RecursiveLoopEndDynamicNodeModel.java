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
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.VariableType;

/**
 * This is the model implementation of Recursive Loop End Node (with an arbitrary amount of ports).
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author based on {@link RecursiveLoopEndNodeModel} by Iris Adae, University of Konstanz, Germany
 */
class RecursiveLoopEndDynamicNodeModel extends NodeModel implements LoopEndNode {

    private static final VariableType<?>[] BOOLEAN_COMPATIBLE_TYPES =
        VariableType.BooleanType.INSTANCE.getConvertibleTypes().toArray(VariableType<?>[]::new);

    private final Supplier<Map<String, FlowVariable>> m_variableSupplier =
        getValidVariablesSupplier(this::getAvailableFlowVariables);

    private BufferedDataContainer[] m_outContainers;

    private BufferedDataTable[] m_inData;

    private int m_iterationNr = 0;

    private final SettingsModelString m_endLoopVariableName = createEndLoopVarModel();

    private final RecursiveLoopEndDynamicNodeSettings m_settings;

    private final int m_numberOfRecursionPorts;

    /**
     * Constructor for the node model.
     *
     * @param inPorts the input ports
     * @param outPorts the output ports
     */
    RecursiveLoopEndDynamicNodeModel(final PortType[] inPorts, final PortType[] outPorts) {
        super(inPorts, outPorts);
        m_numberOfRecursionPorts = inPorts.length - outPorts.length;
        m_settings = new RecursiveLoopEndDynamicNodeSettings(m_numberOfRecursionPorts, outPorts.length);
        m_outContainers = new BufferedDataContainer[outPorts.length];
        m_inData = new BufferedDataTable[m_numberOfRecursionPorts];
    }

    /**
     * @return the numberOfRecursionPorts
     */
    int getNrRecursionPorts() {
        return m_numberOfRecursionPorts;
    }

    /**
     * Call to get the in data tables of the last iteration.
     *
     * @param index the index of the port to get
     * @return the indata table of the last iteration.
     */
    BufferedDataTable getInData(final int index) {
        return m_inData[index];
    }

    /**
     * Check if the loop end is connected to the correct loop start.
     */
    private void validateLoopStart() {
        final var lsn = getLoopStartNode();
        if (lsn instanceof RecursiveLoopStartDynamicNodeModel) {
            final var clsn = (RecursiveLoopStartDynamicNodeModel)lsn;
            CheckUtils.checkState(clsn.getNrRecursionPorts() == m_numberOfRecursionPorts,
                "Connected Recursive Loop Start node has not the same amount of recursion ports! End node: %d, Start node: %d",
                m_numberOfRecursionPorts, clsn.getNrRecursionPorts());
        } else {
            throw new IllegalStateException(
                "Loop End is not connected to matching/corresponding Recursive Loop Start node.");
        }
    }

    /**
     * @return true if any of the datatable's row sizes is smaller than their configured threshold.
     */
    private boolean anyDataTableBelowThreshold() {
        for (var i = 0; i < m_inData.length; i++) {
            if (m_inData[i].size() < m_settings.getMinNumberOfRows(i)) {
                return true;
            }
        }
        return false;
    }

    private static BufferedDataTable copyRecursionTable(final int index, final BufferedDataTable toCopy,
        final ExecutionContext exec) throws CanceledExecutionException {
        final var loopData = exec.createDataContainer(toCopy.getDataTableSpec());
        final var message = "Copy input table " + index;
        for (final var row : toCopy) {
            exec.checkCanceled();
            exec.setMessage(message);
            loopData.addRowToTable(createNewRow(row, row.getKey()));
        }
        loopData.close();
        return loopData.getTable();
    }

    private BufferedDataTable collectData(final int index, final boolean endLoop, final BufferedDataTable toCollect,
        final ExecutionContext exec) throws CanceledExecutionException {
        final var message = "Collect data for output " + index;
        if (m_settings.hasOnlyLastData(index)) {
            return endLoop ? toCollect : null;
        }

        if (m_outContainers[index] == null) {
            final var dts = createSpec(index, toCollect.getDataTableSpec()); // this returns never null because only last data was checked above
            m_outContainers[index] = exec.createDataContainer(dts);
        }

        // should the row be iteration column be appended?
        final var currIterCell = new IntCell(m_iterationNr);
        final BiFunction<DataRow, RowKey, DataRow> rowSupplier = m_settings.hasIterationColumn(index)
            ? ((row, key) -> new AppendedColumnRow(createNewRow(row, key), currIterCell))
            : RecursiveLoopEndDynamicNodeModel::createNewRow;

        // append the rows
        for (final var row : toCollect) {
            exec.checkCanceled();
            exec.setMessage(message);
            final var newKey = new RowKey(row.getKey() + "#" + m_iterationNr);
            m_outContainers[index].addRowToTable(rowSupplier.apply(row, newKey));
        }

        // stop loop if there are less rows than needed.
        // or the max number of iterations is reached
        if (endLoop) {
            m_outContainers[index].close();
            return m_outContainers[index].getTable();
        }

        return null;
    }

    private boolean checkLoopEnd() throws InvalidSettingsException {
        // check flow variable
        final var flowVars = m_variableSupplier.get();
        if (m_settings.hasFlowVariable() && !flowVars.isEmpty()) {
            final var value = Optional.ofNullable(flowVars.get(m_endLoopVariableName.getStringValue()))//
                .map(f -> f.getValue(VariableType.BooleanType.INSTANCE));
            if (value.isEmpty()) {
                throw new InvalidSettingsException(
                    "The selected flow variable '" + m_endLoopVariableName.getStringValue() + "' does not exist");
            } else if (value.get().booleanValue()) {
                return true;
            }
        }
        // check deprecated setting
        if (m_settings.isEndLoopDeprecated()) {
            return true;
        }
        // check iteration number
        if ((m_iterationNr + 1) >= m_settings.getMaxIterations()) {
            return true;
        }
        // check minimum row thresholds
        return anyDataTableBelowThreshold();
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        validateLoopStart();

        // copy recursion data
        for (var i = 0; i < m_inData.length; i++) {
            m_inData[i] = copyRecursionTable(i, inData[getNrOutPorts() + i], exec);
        }

        final var endLoop = checkLoopEnd();

        // collect data
        final var result = new BufferedDataTable[getNrOutPorts()];
        for (var i = 0; i < result.length; i++) {
            result[i] = collectData(i, endLoop, inData[i], exec);
        }

        if (!endLoop) {
            m_iterationNr++;
            // go on with loop
            super.continueLoop();
        }
        return result;
    }

    /**
     * Creates a new row, with the cells as in row and the rowkey newkey.
     *
     * @param row previous data cells
     * @param newKey the new rowkey
     * @return a new row with the given key.
     */
    private static DataRow createNewRow(final DataRow row, final RowKey newKey) {
        final var cells = new DataCell[row.getNumCells()];
        for (var i = 0; i < row.getNumCells(); i++) {
            cells[i] = row.getCell(i);
        }
        return new DefaultRow(newKey, cells);
    }

    @Override
    protected void reset() {
        m_iterationNr = 0;
        Arrays.fill(m_inData, null);
        Arrays.fill(m_outContainers, null);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final var result = IntStream.range(0, getNrOutPorts())//
            .mapToObj(i -> createSpec(i, inSpecs[i]))//
            .toArray(DataTableSpec[]::new);

        // check if variable exists
        final var flowVars = m_variableSupplier.get();
        if (m_settings.hasFlowVariable() && !flowVars.isEmpty()
            && !flowVars.containsKey(m_endLoopVariableName.getStringValue())) {
            throw new InvalidSettingsException(
                "The selected flow variable '" + m_endLoopVariableName.getStringValue() + "' does not exist");
        }

        return result;
    }

    private DataTableSpec createSpec(final int index, final DataTableSpec inSpec) {
        if (m_settings.hasOnlyLastData(index)) {
            // the output may change over the loops
            return null;
        }

        if (m_settings.hasIterationColumn(index)) {
            final var builder =
                new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(inSpec, "Iteration"), IntCell.TYPE);
            return new DataTableSpec(inSpec, new DataTableSpec(builder.createSpec()));
        } else {
            return inSpec;
        }
    }

    @Override
    public boolean shouldPropagateModifiedVariables() {
        return m_settings.isPropagateVariables();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_endLoopVariableName.saveSettingsTo(settings);
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsForModel(settings);
        m_endLoopVariableName.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new RecursiveLoopEndDynamicNodeSettings(m_numberOfRecursionPorts, getNrOutPorts())
            .loadSettingsForModel(settings);
        m_endLoopVariableName.validateSettings(settings);
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

    static Supplier<Map<String, FlowVariable>>
        getValidVariablesSupplier(final Function<VariableType<?>[], Map<String, FlowVariable>> variableSuplier) {
        return () -> variableSuplier.apply(BOOLEAN_COMPATIBLE_TYPES).entrySet().stream()//
            .filter(e -> e.getValue().getScope() == Scope.Flow)//
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * @return the SM for the name of the variable that can ending loop
     */
    static SettingsModelString createEndLoopVarModel() {
        return new SettingsModelString("flowVariableName", "");
    }
}
