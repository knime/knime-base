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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.workflow.LoopEndNode;

/**
 * This is the model implementation of Recursive Loop End Node (1port).
 *
 *
 * @author Iris Adae, University of Konstanz, Germany
 * @deprecated superseded by {@link RecursiveLoopEndDynamicNodeFactory}
 */
@Deprecated(since = "4.5")
class RecursiveLoopEndNodeModel extends NodeModel implements LoopEndNode {

    private static final int PORT_INDEX_COLLECTING_IN = 0;

    private static final int PORT_INDEX_RESULTING_IN = 1;

    private BufferedDataContainer m_outcontainer;

    private BufferedDataTable m_inData;

    private int m_iterationnr = 0;

    private final SettingsModelIntegerBounded m_maxIterations = createIterationsModel();

    private final SettingsModelInteger m_minNumberOfRows = createNumOfRowsModel();

    private final SettingsModelBoolean m_onlyLastResult = createOnlyLastModel();

    private final SettingsModelString m_endLoopDeprecated = createEndLoop();

    private final SettingsModelString m_endLoopVariableName = createEndLoopVarModel();

    private final SettingsModelBoolean m_useVariable = createUseVariable();

    private final SettingsModelBoolean m_addIterationNr = createAddIterationColumn();

    private final SettingsModelBoolean m_propagateVariables = createPropagateVariableModel();

    /**
     * Constructor for the node model.
     *
     * @param inPorts the number of inports
     * @param outPorts the number of outports
     */
    RecursiveLoopEndNodeModel(final int inPorts, final int outPorts) {
        super(inPorts, outPorts);
    }

    /**
     * Check if the loop end is connected to the correct loop start.
     */
    protected void validateLoopStart() {
        if (!(this.getLoopStartNode() instanceof RecursiveLoopStartNodeModel)) {
            throw new IllegalStateException(
                "Loop End is not connected to matching/corresponding Recursive Loop Start node.");
        }
    }

    /**
     * Check if the datatable size is smaller than the threshold.
     *
     * @param minNrRows the minimal number of rows to continue the loop.
     * @return true when the data table size is smaller than the number of rows.
     */
    protected boolean checkDataTableSize(final int minNrRows) {
        return m_inData.size() < minNrRows;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        validateLoopStart();

        // in port 0: collects the data provided at the output port
        // in port 1: is fed back to loop start node
        final BufferedDataContainer loopData = exec.createDataContainer(inData[PORT_INDEX_RESULTING_IN].getDataTableSpec());
        for (final DataRow row : inData[PORT_INDEX_RESULTING_IN]) {
            exec.checkCanceled();
            exec.setMessage("Copy input table 1");
            loopData.addRowToTable(createNewRow(row, row.getKey()));
        }
        loopData.close();
        m_inData = loopData.getTable();

        final boolean endLoopFromVariable;
        if (m_useVariable.getBooleanValue()) {
            final String value = peekFlowVariableString(m_endLoopVariableName.getStringValue());
            if (value == null) {
                throw new InvalidSettingsException(
                    "The selected flow variable '" + m_endLoopVariableName.getStringValue() + "' does not exist");
            }
            endLoopFromVariable = "true".equalsIgnoreCase(value);
        } else {
            // check if legacy setting is set
            endLoopFromVariable = "true".equalsIgnoreCase(m_endLoopDeprecated.getStringValue());
        }

        final boolean endLoop = checkDataTableSize(m_minNumberOfRows.getIntValue())
            || ((m_iterationnr + 1) >= m_maxIterations.getIntValue()) || endLoopFromVariable;

        if (m_onlyLastResult.getBooleanValue()) {
            if (endLoop) {
                return new BufferedDataTable[]{inData[PORT_INDEX_COLLECTING_IN]};
            }
        } else {
            if (m_outcontainer == null) {
                final DataTableSpec dts = createSpec(inData[PORT_INDEX_COLLECTING_IN].getDataTableSpec());
                m_outcontainer = exec.createDataContainer(dts);
            }
            if (m_addIterationNr.getBooleanValue()) {
                final IntCell currIterCell = new IntCell(m_iterationnr);
                for (final DataRow row : inData[PORT_INDEX_COLLECTING_IN]) {
                    exec.checkCanceled();
                    exec.setMessage("Collect data for output");
                    final RowKey newKey = new RowKey(row.getKey() + "#" + m_iterationnr);
                    final AppendedColumnRow newRow = new AppendedColumnRow(createNewRow(row, newKey), currIterCell);
                    m_outcontainer.addRowToTable(newRow);
                }
            } else {
                for (final DataRow row : inData[PORT_INDEX_COLLECTING_IN]) {
                    exec.checkCanceled();
                    exec.setMessage("Collect data for output");
                    final RowKey newKey = new RowKey(row.getKey() + "#" + m_iterationnr);
                    m_outcontainer.addRowToTable(createNewRow(row, newKey));
                }
            }

            // stop loop if there are less rows than needed.
            // or the max number of iterations is reached
            if (endLoop) {
                m_outcontainer.close();
                return new BufferedDataTable[]{m_outcontainer.getTable()};
            }
        }
        m_iterationnr++;
        // go on with loop
        super.continueLoop();
        return new BufferedDataTable[1];
    }

    /**
     * Creates a new row, with the cells as in row and the rowkey newkey.
     *
     * @param row previous data cells
     * @param newKey the new rowkey
     * @return a new row with the given key.
     */
    protected DataRow createNewRow(final DataRow row, final RowKey newKey) {
        final DataCell[] cells = new DataCell[row.getNumCells()];
        for (int i = 0; i < row.getNumCells(); i++) {
            cells[i] = row.getCell(i);
        }
        return new DefaultRow(newKey, cells);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_iterationnr = 0;
        m_outcontainer = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_onlyLastResult.getBooleanValue()) {
            // the output may change over the loops
            return new DataTableSpec[]{null};
        }
        if (m_useVariable.getBooleanValue()
            && getAvailableFlowVariables().get(m_endLoopVariableName.getStringValue()) == null) {
            throw new InvalidSettingsException(
                "Selected flow variable: '" + m_endLoopVariableName.getStringValue() + "' not available!");
        }

        return new DataTableSpec[]{createSpec(inSpecs[PORT_INDEX_COLLECTING_IN])};
    }

    private DataTableSpec createSpec(final DataTableSpec inSpec) {
        if (m_addIterationNr.getBooleanValue()) {
            final DataColumnSpecCreator crea =
                new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(inSpec, "Iteration"), IntCell.TYPE);
            return new DataTableSpec(inSpec, new DataTableSpec(crea.createSpec()));
        } else {
            return inSpec;
        }
    }

    @Override
    public boolean shouldPropagateModifiedVariables() {
        return m_propagateVariables.getBooleanValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_maxIterations.saveSettingsTo(settings);
        m_minNumberOfRows.saveSettingsTo(settings);
        m_onlyLastResult.saveSettingsTo(settings);
        m_endLoopDeprecated.saveSettingsTo(settings);
        m_endLoopVariableName.saveSettingsTo(settings);
        m_useVariable.saveSettingsTo(settings);
        m_addIterationNr.saveSettingsTo(settings);
        m_propagateVariables.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_maxIterations.loadSettingsFrom(settings);
        m_minNumberOfRows.loadSettingsFrom(settings);
        m_onlyLastResult.loadSettingsFrom(settings);
        m_endLoopDeprecated.loadSettingsFrom(settings);
        m_addIterationNr.loadSettingsFrom(settings);
        // since 3.6.0
        if (settings.containsKey(m_endLoopVariableName.getKey())) {
            m_endLoopVariableName.loadSettingsFrom(settings);
        }
        if (settings.containsKey(m_useVariable.getConfigName())) {
            m_useVariable.loadSettingsFrom(settings);
        }
        // since 4.4.0
        if (settings.containsKey(m_propagateVariables.getConfigName())) {
            m_propagateVariables.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_maxIterations.validateSettings(settings);
        m_minNumberOfRows.validateSettings(settings);
        m_onlyLastResult.validateSettings(settings);
        m_addIterationNr.validateSettings(settings);
        m_endLoopDeprecated.validateSettings(settings);
        // since 3.6.0
        if (settings.containsKey(m_endLoopVariableName.getKey())) {
            m_endLoopVariableName.validateSettings(settings);
        }
        if (settings.containsKey(m_useVariable.getConfigName())) {
            m_useVariable.validateSettings(settings);
        }
        // since 4.4.0
        if (settings.containsKey(m_propagateVariables.getConfigName())) {
            m_propagateVariables.validateSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to save
    }

    /**
     * Call to get the in data table of the last iteration.
     *
     * @return the indata table of the last iteration.
     */
    public BufferedDataTable getInData() {
        return m_inData;
    }

    /**
     * @return
     */
    static SettingsModelBoolean createUseVariable() {
        return new SettingsModelBoolean("Use Flow Variable", false);
    }

    /**
     * @return the SM for adding the iteration column
     */
    static SettingsModelBoolean createAddIterationColumn() {
        return new SettingsModelBoolean("CFG_AddIterationColumn", false);
    }

    /**
     * @return the SM for ending the loop
     */
    @Deprecated
    static SettingsModelString createEndLoop() {
        return new SettingsModelString("CFG_End_Loop", "false");
    }

    /**
     * @return the SM for the name of the variable that can ending loop
     */
    static SettingsModelString createEndLoopVarModel() {
        return new SettingsModelString("End Loop Variable Name", "");
    }

    /**
     *
     * @return the settings model for the maximal number of iterations.
     */
    static SettingsModelIntegerBounded createIterationsModel() {
        return new SettingsModelIntegerBounded("CFG_MaxNrIterations", 100, 1, Integer.MAX_VALUE);
    }

    /**
     *
     * @return the settings model for the minimal number of rows.
     */
    static SettingsModelInteger createNumOfRowsModel() {
        return new SettingsModelInteger("CFG_MinNrOfRows", 1);
    }

    /**
     * @return the settings model that contains the only last result flag
     */
    static SettingsModelBoolean createOnlyLastModel() {
        return new SettingsModelBoolean("CFG_OnlyLastData", false);
    }

    /**
     * @return the settings model that the "propagate modified vars" flag
     */
    static SettingsModelBoolean createPropagateVariableModel() {
        return new SettingsModelBoolean("CFG_PropagateVariables", false);
    }

}
