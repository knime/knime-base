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
 *   Oct 23, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.meta.looper.variable.end;

import org.knime.base.node.flowvariable.converter.variabletocell.VariableToDataColumnConverter;
import org.knime.base.node.flowvariable.variabletotablerow4.AbstractVariableToTableNodeModel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * Node model of the variable loop end node. The node creates for each loop iteration one row with one column for each
 * selected {@link FlowVariable}. In each iteration the variable values will be set to the according columns. It is
 * possible to select no variables at all, than a data table with only row keys is be created.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class LoopEndVariableNodeModel extends AbstractVariableToTableNodeModel implements LoopEndNode {

    @SuppressWarnings("hiding")
    static final String CFG_KEY_FILTER = AbstractVariableToTableNodeModel.CFG_KEY_FILTER;

    private VariableToDataColumnConverter m_converter;

    private BufferedDataContainer m_container;

    private long m_rowCount = 0;

    private SettingsModelBoolean m_propagateLoopVariablesModel = createPropagateLoopVariablesModel();

    LoopEndVariableNodeModel() {
        super(FlowVariablePortObject.TYPE);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        CheckUtils.checkSetting(this.getLoopStartNode() instanceof LoopStartNodeTerminator, "Loop End is not connected"
            + " with a matching/compatible Loop Start node. You are trying to create an infinite loop!");
        return super.configure(inSpecs);
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final DataTableSpec curSpec = createOutSpec(false);
        if (m_container == null) {
            m_container = exec.createDataContainer(curSpec);
            m_converter = new VariableToDataColumnConverter();
        } else {
            validateSpecs(curSpec);
        }
        m_container.addRowToTable(createTableRow(exec, m_converter, "Row" + m_rowCount));
        final BufferedDataTable out;
        if (terminateLoop()) {
            m_container.close();
            out = m_container.getTable();
            clear(false);
        } else {
            m_rowCount++;
            continueLoop();
            out = null;
        }
        return new BufferedDataTable[]{out};
    }

    private void validateSpecs(final DataTableSpec curSpec) {
        if (!curSpec.equalStructure(m_container.getTableSpec())) {
            DataTableSpec predSpec = m_container.getTableSpec();
            StringBuilder error =
                new StringBuilder("Output table's structure differs from reference " + "(first iteration) table: ");
            if (curSpec.getNumColumns() != predSpec.getNumColumns()) {
                error.append("different column counts ");
                error.append(curSpec.getNumColumns());
                error.append(" vs. ").append(predSpec.getNumColumns());
            } else {
                buildDiffColStructErrors(curSpec, predSpec, error);
            }
            error.append(". Have the input variables changed in number, name or type?");
            clear(true);
            throw new IllegalArgumentException(error.toString());
        }
    }

    private static void buildDiffColStructErrors(final DataTableSpec curSpec, final DataTableSpec predSpec, final StringBuilder error) {
        for (int i = 0; i < curSpec.getNumColumns(); i++) {
            final DataColumnSpec inCol = curSpec.getColumnSpec(i);
            final DataColumnSpec predCol = predSpec.getColumnSpec(i);
            if (!inCol.equalStructure(predCol)) {
                error.append("Column ").append(i).append(" [");
                error.append(inCol).append("] vs. [");
                error.append(predCol).append(']');
            }
        }
    }

    boolean terminateLoop() {
        return ((LoopStartNodeTerminator)this.getLoopStartNode()).terminateLoop();
    }

    @Override
    public boolean shouldPropagateModifiedVariables() {
        return m_propagateLoopVariablesModel.getBooleanValue();
    }

    @Override
    protected void onDispose() {
        clear(true);
        super.onDispose();
    }

    @Override
    protected void reset() {
        clear(true);
    }

    private void clear(final boolean deleteData) {
        m_rowCount = 0;
        if (m_converter != null) {
            m_converter.close();
        }
        m_converter = null;
        if (m_container != null) {
            m_container.close();
            if (deleteData) {
                m_container.getCloseableTable().close();
            }
        }
        m_container = null;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_propagateLoopVariablesModel.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        if (settings.containsKey(m_propagateLoopVariablesModel.getConfigName())) { // added in 4.4
            m_propagateLoopVariablesModel.loadSettingsFrom(settings);
        }
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        if (settings.containsKey(m_propagateLoopVariablesModel.getConfigName())) { // added in 4.4
            m_propagateLoopVariablesModel.validateSettings(settings);
        }
    }

    /**
     * Factory method to create model storing m_propagateLoopVariables property.
     *
     * @return new model
     */
    static SettingsModelBoolean createPropagateLoopVariablesModel() {
        return new SettingsModelBoolean("propagateLoopVariables", false);
    }

}
