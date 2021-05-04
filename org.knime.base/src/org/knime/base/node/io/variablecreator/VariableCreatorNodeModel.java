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
 *   06.04.2021 (jl): created
 */
package org.knime.base.node.io.variablecreator;

import java.io.File;
import java.util.Optional;

import org.knime.base.node.io.variablecreator.VariableTable.Type;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.util.Pair;

/**
 * The {@link NodeModel} for the “Create Variables” node
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
final class VariableCreatorNodeModel extends NodeModel {

    /** A table containing the settings of this model, i.e. the variables, their name and value. */
    private final VariableTable m_table = new VariableTable(getAvailableFlowVariables(Type.getAllTypes()));

    /**
     * Create the node model for the "Variable Creator" node
     */
    VariableCreatorNodeModel() {
        super(new PortType[0], new PortType[]{FlowVariablePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        pushVariablesAndSetWarning();
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        pushVariablesAndSetWarning();
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    private void pushVariablesAndSetWarning() {
        if (m_table.getRowCount() == 0) {
            setWarningMessage("No new variables defined");
        } else {
            pushVariables();
            setWarningMessage(getNameWarningMessage());
        }
    }

    /**
     * Generate the warning message for this node. Currently, only name overrides and same names are respected.
     *
     * @return the warning message. <code>null</code> if there is no warning.
     */
    private String getNameWarningMessage() {
        m_table.setExternalVariables(getAvailableInputFlowVariables(Type.getAllTypes()));
        // the name cannot be empty in this case
        boolean foundOverride = false;
        boolean foundConflict = false;
        for (int i = m_table.getRowCount() - 1; i >= 0 && (!foundOverride || !foundConflict); i--) {
            final Pair<Boolean, Optional<String>> result = m_table.checkVariableNameExternal(i, m_table.getName(i));
            final Optional<String> hintMsg = result.getSecond();

            if (!result.getFirst().booleanValue()) {
                throw new IllegalStateException(
                    "Name should be valid!: " + result.getSecond().orElse("(Unknown Error)"));
            }

            if (hintMsg.isPresent()) {
                switch (hintMsg.get()) {
                    case VariableTable.MSG_NAME_EXTERNAL_CONFLICT:
                        foundConflict = true;
                        break;
                    case VariableTable.MSG_NAME_EXTERNAL_OVERRIDES:
                        foundOverride = true;
                        break;
                    default:
                        // we are ignoring other hints
                        break;
                }
            }
        }
        return buildWarningMessage(foundOverride, foundConflict);
    }

    /**
     * Builds a warning message from this node depending on whether errors indicated by the parameters were found.
     *
     * @param foundOverride whether a variable override was found
     * @param foundConflict whether a variable with the same name was found
     * @return the warning message or <code>null</code> if there is no warning.
     */
    private static String buildWarningMessage(final boolean foundOverride, final boolean foundConflict) {

        if (!foundConflict && !foundOverride) {
            return null;
        }

        final StringBuilder result = new StringBuilder("Some defined variables ");

        if (foundOverride) {
            result.append("override");
        }

        if (foundConflict && foundOverride) {
            result.append(" or ");
        }

        if (foundConflict) {
            result.append("have the same name as");
        }

        result.append(" upstream variables.");

        return result.toString();
    }

    /**
     * Push a variable from the <code>m_table</code> table.
     *
     * @param row the row of the variable to push
     */
    private void pushVariables() {
        // it is a stack so we have to push  the variables in opposite order
        for (int row = m_table.getRowCount() - 1; row >= 0; row--) {
            final Type type = m_table.getType(row);
            switch (type) {
                case STRING:
                    pushFlowVariable(m_table.getName(row), VariableType.StringType.INSTANCE,
                        (String)m_table.getValue(row));
                    break;
                case INTEGER:
                    pushFlowVariable(m_table.getName(row), VariableType.IntType.INSTANCE,
                        (Integer)m_table.getValue(row));
                    break;
                case LONG:
                    pushFlowVariable(m_table.getName(row), VariableType.LongType.INSTANCE, (Long)m_table.getValue(row));
                    break;
                case DOUBLE:
                    pushFlowVariable(m_table.getName(row), VariableType.DoubleType.INSTANCE,
                        (Double)m_table.getValue(row));
                    break;
                case BOOLEAN:
                    pushFlowVariable(m_table.getName(row), VariableType.BooleanType.INSTANCE,
                        (Boolean)m_table.getValue(row));
                    break;
                default:
                    throw new IllegalStateException("Unknown variable type: " + type);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_table.saveVariablesToSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        VariableTable.validateVariablesFromSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_table.loadVariablesFromSettings(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do here

    }

    /**
     * Unused.<br>
     * <br>
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do here
    }

    /**
     * Unused.<br>
     * <br>
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do here
    }

}
