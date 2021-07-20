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
 *   24.02.2009 (meinl): created
 */
package org.knime.base.node.meta.looper;

import java.io.File;
import java.math.BigDecimal;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.core.node.workflow.VariableType;

/**
 * This is the model for the interval loop start node. It lets the user defined an interval in which a variable is
 * increased by a certain amount in each iteration.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author based on {@link LoopStartIntervalNodeModel} by Thorsten Meinl, University of Konstanz
 */
final class LoopStartIntervalDynamicNodeModel extends NodeModel implements LoopStartNodeTerminator {

    private double m_value;

    private final LoopStartIntervalDynamicSettings m_settings = new LoopStartIntervalDynamicSettings();

    private final SettingsModelString m_variableTypeSettings = createVariableTypeSettings();

    private OutputType m_variableType = OutputType.DOUBLE;

    static SettingsModelString createVariableTypeSettings() {
        return new SettingsModelString("variableType", OutputType.DOUBLE.name());
    }

    /**
     * Creates a new model with one input and one output port.
     *
     * @param inPorts the input ports
     * @param outPorts the output ports
     */
    public LoopStartIntervalDynamicNodeModel(final PortType[] inPorts, final PortType[] outPorts) {
        super(inPorts, outPorts);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if ((m_settings.from() > m_settings.to()) && (m_settings.step() > 0)) {
            throw new InvalidSettingsException("From must be smaller than to if step size is positive.");
        } else if ((m_settings.from() < m_settings.to()) && (m_settings.step() < 0)) {
            throw new InvalidSettingsException("From must be bigger than to if step size is negative.");
        } else if (m_settings.step() == 0) { //NOSONAR: in this case exactness is fine because we fail otherwise in execution
            throw new InvalidSettingsException("Step size is zero, this will lead to an endless loop");
        }

        m_value = m_settings.from();
        pushState();
        return inSpecs;
    }

    private void pushState() {
        final var prefix = m_settings.prefix();
        if (m_variableType == OutputType.INTEGER) {
            try {
                pushFlowVariableInt(prefix + "from", Math.toIntExact(Math.round(m_settings.from())));
                pushFlowVariableInt(prefix + "to", Math.toIntExact(Math.round(m_settings.to())));
                pushFlowVariableInt(prefix + "step", Math.toIntExact(Math.round(m_settings.step())));
                pushFlowVariableInt(prefix + "value", Math.toIntExact(Math.round(m_value)));
            } catch (ArithmeticException e) {
                throw new IllegalStateException(
                    "Could not save current value in an integer. Please choose a different type", e);
            }
        } else if (m_variableType == OutputType.LONG) {
            final var type = VariableType.LongType.INSTANCE;
            pushFlowVariable(prefix + "from", type, Math.round(m_settings.from()));
            pushFlowVariable(prefix + "to", type, Math.round(m_settings.to()));
            pushFlowVariable(prefix + "step", type, Math.round(m_settings.step()));
            pushFlowVariable(prefix + "value", type, Math.round(m_value));
        } else if (m_variableType == OutputType.DOUBLE) {
            pushFlowVariableDouble(prefix + "from", m_settings.from());
            pushFlowVariableDouble(prefix + "to", m_settings.to());
            pushFlowVariableDouble(prefix + "step", m_settings.step());
            pushFlowVariableDouble(prefix + "value", m_value);
        } else {
            throw new IllegalStateException("Unknow variable type!");
        }
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // calculate next step
        final var newValue = BigDecimal.valueOf(m_value).add(BigDecimal.valueOf(m_settings.step())).doubleValue();
        CheckUtils.checkState(newValue != m_value, // NOSONAR: we only want to know if it was changed at all
            "Value did not change after adding step value! This is probably due to rounding errors."
                + " Please change your “from”, “to” or “step” values (current value=%f, step=%f).",
            newValue, m_settings.step());
        // let's see if we have access to the tail: if we do, it's not the
        // first time we are doing this...
        if (getLoopEndNode() == null) {
            // if it's null we know that this is the first time the
            // loop is being executed.
            CheckUtils.checkState(m_value == m_settings.from(), //NOSONAR: only checks if this value wasn't changed
                "Value changed before loop execution!");
        }
        // let's also put the counts on the stack for someone else:
        pushState();

        m_value = newValue;
        return inObjects;
    }

    @Override
    public boolean terminateLoop() {
        if (m_settings.step() > 0) {
            return m_value > m_settings.to();
        } else if (m_settings.step() < 0) {
            return m_value < m_settings.to();
        } else {
            // we never end up here --> step() == 0
            return true;
        }
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // empty
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
        m_variableTypeSettings.loadSettingsFrom(settings);
        try {
            m_variableType = OutputType.valueOf(m_variableTypeSettings.getStringValue());
        } catch (IllegalArgumentException | NullPointerException e) { // NOSONAR: may be user input
            throw new InvalidSettingsException("Output type is not supported!", e);
        }
    }

    @Override
    protected void reset() {
        m_value = m_settings.from();
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // empty
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
        m_variableTypeSettings.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var s = new LoopStartIntervalDynamicSettings();
        s.loadSettings(settings);
        final var vts = createVariableTypeSettings();
        vts.loadSettingsFrom(settings);

        if ((s.from() > s.to()) && (s.step() > 0)) {
            throw new InvalidSettingsException("From must be smaller than to if step size is positive.");
        }
        if ((s.from() < s.to()) && (s.step() < 0)) {
            throw new InvalidSettingsException("From must be bigger than to if step size is negative.");
        }
        if (s.step() == 0) { // NOSONAR: We are only interested in this special case and my fail later
            throw new InvalidSettingsException("Step size is zero, this will lead to an endless loop");
        }
        try {
            OutputType.valueOf(vts.getStringValue());
        } catch (IllegalArgumentException | NullPointerException e) { // NOSONAR: may be user input
            throw new InvalidSettingsException("Output type is not supported!", e);
        }

    }

    enum OutputType implements ButtonGroupEnumInterface {
            DOUBLE("double"), LONG("long"), INTEGER("integer");

        final String m_text;

        OutputType(final String text) {
            m_text = text;
        }

        @Override
        public String getText() {
            return m_text;
        }

        @Override
        public String getActionCommand() {
            return name();
        }

        @Override
        public String getToolTip() {
            return "The variables are of type “" + m_text + "”";
        }

        @Override
        public boolean isDefault() {
            return this == DOUBLE;
        }
    }

}
