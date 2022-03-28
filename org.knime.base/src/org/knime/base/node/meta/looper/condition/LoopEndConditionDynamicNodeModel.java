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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.09.2008 (thor): created
 */
package org.knime.base.node.meta.looper.condition;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.core.node.workflow.VariableType;

/**
 * This class is the model for the condition loop tail node. It checks the user condition in each iteration and decides
 * if the loop should be finished. Meanwhile it collects all rows from each iteration.s
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author based on {@link LoopEndConditionNodeModel} by Thorsten Meinl, University of Konstanz
 * @since 4.5
 */
final class LoopEndConditionDynamicNodeModel extends NodeModel implements LoopEndNode {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(LoopEndConditionDynamicNodeModel.class);

    private long m_startTime;

    private final LoopEndConditionDynamicSettings m_settings;

    private BufferedDataContainer[] m_collectContainers;

    private BufferedDataContainer m_variableContainer;

    private DataTableSpec createDataSpec(final int index, final DataTableSpec inSpec) {
        if (m_settings.addIterationColumn(index)) {
            final var builder =
                new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(inSpec, "Iteration"), IntCell.TYPE);
            final var appended = new DataTableSpec(builder.createSpec());
            return new DataTableSpec(inSpec, appended);
        } else {
            return inSpec;
        }
    }

    private DataTableSpec createVariableValueSpec() throws InvalidSettingsException {
        final DataType type;
        if (m_settings.variableType() == VariableType.DoubleType.INSTANCE) {
            type = DoubleCell.TYPE;
        } else if (m_settings.variableType() == VariableType.IntType.INSTANCE) {
            type = IntCell.TYPE;
        } else if (m_settings.variableType() == VariableType.LongType.INSTANCE) {
            type = LongCell.TYPE;
        } else if (m_settings.variableType() == VariableType.StringType.INSTANCE) {
            type = StringCell.TYPE;
        } else if (m_settings.variableType() == VariableType.BooleanType.INSTANCE) {
            type = BooleanCell.TYPE;
        } else {
            throw new InvalidSettingsException(
                "Cannot work with variables of type " + m_settings.variableType().getSimpleType().getSimpleName());
        }

        final var builder = new DataColumnSpecCreator("Variable value", type);
        return new DataTableSpec(builder.createSpec());
    }

    /**
     * Creates a new node model.
     *
     * @param inputPorts the input ports
     * @param outputPorts the output ports
     */
    public LoopEndConditionDynamicNodeModel(final PortType[] inputPorts, final PortType[] outputPorts) {
        super(inputPorts, outputPorts);
        m_settings = new LoopEndConditionDynamicSettings(outputPorts.length);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_settings.variableName() == null) {
            throw new InvalidSettingsException("No variable selected");
        }
        if (m_settings.value() == null) {
            throw new InvalidSettingsException("No value for the condition entered");
        }

        checkComparison();

        return Stream.concat(//
            IntStream.range(0, inSpecs.length)//
                .mapToObj(i -> createDataSpec(i, inSpecs[i])), //
            Stream.of(createVariableValueSpec()))//
            .toArray(DataTableSpec[]::new);
    }

    final void checkComparison() throws InvalidSettingsException {
        try {
            peekFlowVariable(m_settings.variableName(), m_settings.variableType());
        } catch (NoSuchElementException ex) {
            throw new InvalidSettingsException("No variable named '" + m_settings.variableName() + "' of type "
                + m_settings.variableType().getIdentifier() + " found", ex);

        }

        try {
            parseComparison(m_settings);
        } catch (NumberFormatException ex) {
            throw new InvalidSettingsException("Could not parse numerical value " + m_settings.value() + " as "
                + m_settings.variableType().getSimpleType().getSimpleName(), ex);
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException("Could not parse value " + m_settings.value() + " as "
                + m_settings.variableType().getSimpleType().getSimpleName() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final var iteration = peekFlowVariableInt("currentIteration");
        exec.setMessage("Iteration " + iteration);
        final var specs = IntStream.range(0, inData.length)
            .mapToObj(i -> createDataSpec(i, inData[i].getDataTableSpec())).toArray(DataTableSpec[]::new);
        if (m_collectContainers == null) {
            assert m_variableContainer == null;
            m_startTime = System.currentTimeMillis();
            // first time we are getting to this: open container
            m_collectContainers =
                Arrays.stream(specs).map(exec::createDataContainer).toArray(BufferedDataContainer[]::new);
            m_variableContainer = exec.createDataContainer(createVariableValueSpec());
        } else {
            // check compatibility
            for (var j = 0; j < inData.length; j++) {
                final var oldStruc = m_collectContainers[j].getTableSpec();
                final var newStruc = specs[j];
                if (!newStruc.equalStructure(oldStruc)) {
                    throwStructureError(newStruc, oldStruc, j);
                }
            }
        }

        recordVariable(iteration);

        final var startNode = getLoopStartNode();

        boolean inLastIteration = checkCondition()
            || ((startNode instanceof LoopStartNodeTerminator) && ((LoopStartNodeTerminator)startNode).terminateLoop());

        appendRows(inData, exec, iteration, inLastIteration);

        if (inLastIteration) {
            Arrays.stream(m_collectContainers).forEach(BufferedDataContainer::close);
            m_variableContainer.close();

            final var result = Stream.concat(Arrays.stream(m_collectContainers), Stream.of(m_variableContainer))//
                .map(BufferedDataContainer::getTable)//
                .toArray(BufferedDataTable[]::new);

            LOGGER.debug("Total loop execution time: " + (System.currentTimeMillis() - m_startTime) + "ms");
            m_startTime = 0;
            return result;
        } else {
            continueLoop();
            return new BufferedDataTable[inData.length + 1];
        }
    }

    private static void throwStructureError(final DataTableSpec newStruc, final DataTableSpec oldStruc,
        final int index) {
        final var error = new StringBuilder(
            "Input table #" + index + "'s structure differs from reference (first iteration) table: ");
        if (newStruc.getNumColumns() != oldStruc.getNumColumns()) {
            error.append("different column counts ");
            error.append(newStruc.getNumColumns());
            error.append(" vs. ").append(oldStruc.getNumColumns());
        } else {
            for (var i = 0; i < newStruc.getNumColumns(); i++) {
                final var newCol = newStruc.getColumnSpec(i);
                final var oldCol = oldStruc.getColumnSpec(i);
                if (!newCol.equalStructure(oldCol)) {
                    error.append("Column ").append(i).append(" [");
                    error.append(newCol).append("] vs. [");
                    error.append(oldCol).append("]");
                }
            }
        }
        throw new IllegalArgumentException(error.toString());

    }

    private void recordVariable(final int iteration) throws InvalidSettingsException {
        final var rk = new RowKey("Iteration " + iteration);
        final var type = m_settings.variableType();
        if (type == VariableType.IntType.INSTANCE) {
            m_variableContainer
                .addRowToTable(new DefaultRow(rk, new IntCell(peekFlowVariableInt(m_settings.variableName()))));
        } else if (type == VariableType.DoubleType.INSTANCE) {
            m_variableContainer
                .addRowToTable(new DefaultRow(rk, new DoubleCell(peekFlowVariableDouble(m_settings.variableName()))));
        } else if (type == VariableType.LongType.INSTANCE) {
            m_variableContainer.addRowToTable(new DefaultRow(rk,
                new LongCell(peekFlowVariable(m_settings.variableName(), VariableType.LongType.INSTANCE))));
        } else if (type == VariableType.StringType.INSTANCE) {
            m_variableContainer
                .addRowToTable(new DefaultRow(rk, new StringCell(peekFlowVariableString(m_settings.variableName()))));
        } else if (type == VariableType.BooleanType.INSTANCE) {
            m_variableContainer.addRowToTable(new DefaultRow(rk,
                BooleanCellFactory.create(peekFlowVariable(m_settings.variableName(), VariableType.BooleanType.INSTANCE))));
        } else {
            throw new InvalidSettingsException(
                "Cannot work with variables of type " + type.getSimpleType().getSimpleName());
        }
    }

    private void appendRows(final BufferedDataTable[] inData, final ExecutionContext exec, final int iteration,
        final boolean inLastIteration) throws CanceledExecutionException {
        for (var index = 0; index < inData.length; index++) {
            if ((!m_settings.addLastRows(index) || m_settings.addLastRowsOnly(index))
                && ((inLastIteration != m_settings.addLastRows(index))
                    || (inLastIteration != m_settings.addLastRowsOnly(index)))) {
                continue;
            }
            exec.setMessage("Collecting rows from current iteration");

            var k = 0L;
            final double max = inData[index].size();
            for (final var row : inData[index]) {
                exec.checkCanceled();
                k++;
                if (k % 10 == 0) {
                    exec.setProgress(k / max);
                }
                DataRow newRow = new DefaultRow(new RowKey(row.getKey() + "#" + iteration), row);
                if (m_settings.addIterationColumn(index)) {
                    final var currIterCell = new IntCell(iteration);
                    newRow = new AppendedColumnRow(newRow, currIterCell);
                }
                m_collectContainers[index].addRowToTable(newRow);
            }
        }

    }

    @Override
    public boolean shouldPropagateModifiedVariables() {
        return m_settings.propagateLoopVariables();
    }

    private boolean checkCondition() throws InvalidSettingsException {
        final var type = m_settings.variableType();
        final var name = m_settings.variableName();
        if (type == VariableType.IntType.INSTANCE) {
            final var compVal = Integer.valueOf(m_settings.value());
            final var varVal = peekFlowVariable(name, VariableType.IntType.INSTANCE);
            return m_settings.operator().test(varVal, compVal);
        } else if (type == VariableType.LongType.INSTANCE) {
            final var compVal = Long.valueOf(m_settings.value());
            final var varVal = peekFlowVariable(name, VariableType.LongType.INSTANCE);
            return m_settings.operator().test(varVal, compVal);
        } else if (type == VariableType.DoubleType.INSTANCE) {
            final var compVal = Double.valueOf(m_settings.value());
            final var varVal = peekFlowVariable(name, VariableType.DoubleType.INSTANCE);
            return m_settings.operator().test(varVal, compVal);
        } else if (type == VariableType.StringType.INSTANCE) {
            final var compVal = m_settings.value();
            final var varVal = peekFlowVariable(name, VariableType.StringType.INSTANCE);
            return m_settings.operator().test(varVal, compVal);
        } else if (type == VariableType.BooleanType.INSTANCE) {
            final var compVal = Boolean.valueOf(m_settings.value());
            final var varVal = peekFlowVariable(name, VariableType.BooleanType.INSTANCE);
            return m_settings.operator().test(varVal, compVal);
        } else {
            throw new InvalidSettingsException(
                "Cannot work with variables of type " + type.getSimpleType().getSimpleName());
        }
    }

    private static final void parseComparison(final LoopEndConditionDynamicSettings settings) throws InvalidSettingsException {
        final var type = settings.variableType();
        if (type == VariableType.IntType.INSTANCE) {
            Integer.valueOf(settings.value()); //NOSONAR: only check format here
        } else if (type == VariableType.LongType.INSTANCE) {
            Long.valueOf(settings.value()); //NOSONAR: only check format here
        } else if (type == VariableType.DoubleType.INSTANCE) {
            Double.valueOf(settings.value()); //NOSONAR: only check format here
        } else if (type == VariableType.BooleanType.INSTANCE) {
            final var val = settings.value();
            if (!val.equalsIgnoreCase("true") && !val.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException("Boolean has to be “true” or “false”");
            }
        } else if (type != VariableType.StringType.INSTANCE) {
            throw new InvalidSettingsException(
                "Cannot work with variables of type " + type.getSimpleType().getSimpleName());
        }
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // empty
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    @Override
    protected void reset() {
        m_variableContainer = null;
        m_collectContainers = null;
        m_startTime = 0;
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // empty
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var s = new LoopEndConditionDynamicSettings(getNrInPorts());
        s.loadSettings(settings);

        if (s.operator() == null) {
            throw new InvalidSettingsException("No comparison operator selected");
        }
        if (s.value() == null || (s.variableType() != VariableType.StringType.INSTANCE && s.value().isEmpty())) {
            throw new InvalidSettingsException("No comparison value given");
        }
        try {
            parseComparison(s);
        } catch (NumberFormatException ex) {
            throw new InvalidSettingsException("Comparison value “" + s.value() + "” is not a valid "
                + s.variableType().getSimpleType().getSimpleName(), ex);
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException("Comparison value “" + s.value() + "” is not a valid "
                + s.variableType().getSimpleType().getSimpleName() + ": " + ex.getMessage(), ex);
        }
    }
}
