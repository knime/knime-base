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
 *   Oct 22, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.meta.looper.variable.start;

import org.knime.base.node.flowvariable.tablerowtovariable3.TableToVariable3NodeModel;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.core.node.workflow.VariableType.LongType;

/**
 * A loop that pushes in every single iteration the selected columns onto the flow variable stack.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class LoopStartVariable2NodeModel extends TableToVariable3NodeModel implements LoopStartNodeTerminator {

    /** The current iteration. */
    private long m_currentIteration = -1;

    /** The maximum number of allowed iterations. */
    private long m_maxNrIterations = -1;

    /** last seen table in #execute -- used for assertions */
    private DataTable m_lastTable;

    /** The row iterator w.r.t. #m_lastTable. */
    private RowIterator m_iterator;

    LoopStartVariable2NodeModel() {
        super();
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataTable data = (BufferedDataTable)inData[0];
        if (m_currentIteration == -1) {
            assert m_iterator == null : "Iterator has not been closed in previous run";
            m_currentIteration = 0;
            m_maxNrIterations = data.size();
            m_lastTable = data;
            m_iterator = m_lastTable.iterator();
        }
        assert m_lastTable == data : "Input tables differ between iterations";
        if (m_currentIteration > m_maxNrIterations) {
            throw new IllegalStateException("Loop did not terminate correctly.");
        }
        final DataRow row;
        if (m_maxNrIterations > 0) {
            assert m_iterator.hasNext() : "The table contains less rows than expected";
            row = m_iterator.next();
        } else {
            row = null;
        }

        pushIterationVariables(m_maxNrIterations, m_currentIteration);
        pushVariables(m_lastTable.getDataTableSpec(), row);

        ++m_currentIteration;
        if (m_currentIteration == m_maxNrIterations) {
            clearTableReferences();
        }
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    private void pushIterationVariables(final long maxIter, final long curIter) {
        pushVariable(createLongVariable("maxIterations", maxIter));
        pushVariable(createLongVariable("currentIteration", curIter));
    }

    private static FlowVariable createLongVariable(final String string, final long maxNrIterations) {
        return new FlowVariable(string, LongType.INSTANCE, maxNrIterations);
    }

    void clearTableReferences() {
        if (m_iterator instanceof CloseableRowIterator) {
            ((CloseableRowIterator)m_iterator).close();
        }
        m_lastTable = null;
        m_iterator = null;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        pushIterationVariables(0, 0);
        return super.configure(inSpecs);
    }

    @Override
    public boolean terminateLoop() {
        return m_currentIteration == m_maxNrIterations;
    }

    @Override
    protected void reset() {
        clearTableReferences();
        m_currentIteration = -1;
        m_maxNrIterations = -1;
        super.reset();
    }

    @Override
    protected void onDispose() {
        clearTableReferences();
        super.onDispose();
    }

}
