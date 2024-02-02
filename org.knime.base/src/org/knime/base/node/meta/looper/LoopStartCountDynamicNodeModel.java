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
 *   13.02.2008 (thor): created
 */
package org.knime.base.node.meta.looper;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * This model is the head node of a for loop.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author based on {@link LoopStartCountNodeModel} by Thorsten Meinl, University of Konstanz
 * @since 4.5
 */
@SuppressWarnings("restriction")
final class LoopStartCountDynamicNodeModel extends WebUINodeModel<LoopStartCountDynamicSettings>
        implements LoopStartNodeTerminator {

    private int m_iteration;

    private boolean m_lastIteration;

    LoopStartCountDynamicNodeModel(final PortType[] inputPorts, final PortType[] outputPorts) {
        super(inputPorts, outputPorts, LoopStartCountDynamicSettings.class);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs,
        final LoopStartCountDynamicSettings modelSettings) throws InvalidSettingsException {
        if (modelSettings.m_loops < 1) {
            throw new InvalidSettingsException("Cannot loop fewer than once");
        }
        assert m_iteration == 0;
        pushFlowVariableInt("currentIteration", m_iteration);
        pushFlowVariableInt("maxIterations", modelSettings.m_loops);
        m_lastIteration = m_iteration >= modelSettings.m_loops;
        return inSpecs;
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec,
        final LoopStartCountDynamicSettings modelSettings) throws Exception {
        // let's see if we have access to the tail: if we do, it's not the
        // first time we are doing this...
        if (getLoopEndNode() == null) {
            // if it's null we know that this is the first time the
            // loop is being executed.
            assert m_iteration == 0;
        } else {
            assert m_iteration > 0;
            // otherwise we do this again.
        }
        // let's also put the counts on the stack for someone else:
        pushFlowVariableInt("currentIteration", m_iteration);
        pushFlowVariableInt("maxIterations", modelSettings.m_loops);
        // increment counter for next iteration
        m_iteration++;
        m_lastIteration = m_iteration >= modelSettings.m_loops;
        return inObjects;
    }

    @Override
    public boolean terminateLoop() {
        return m_lastIteration;
    }

    @Override
    protected void reset() {
        m_iteration = 0;
        m_lastIteration = false;
    }
}
