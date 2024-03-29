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
package org.knime.base.node.meta.looper.recursive;

import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentFlowVariableNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * Dialog for the recursive loop end.
 *
 * @author Iris Adae
 * @deprecated superseded by {@link RecursiveLoopEndDynamicNodeFactory}
 */
@Deprecated(since = "4.5")
final class RecursiveLoopEndNodeDialog extends DefaultNodeSettingsPane {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RecursiveLoopEndNodeDialog.class);

    private final SettingsModelString m_endLoopVar = RecursiveLoopEndNodeModel.createEndLoopVarModel();

    private final SettingsModelString m_endLoopDeprecated = RecursiveLoopEndNodeModel.createEndLoop();

    private final SettingsModelBoolean m_useVariable = RecursiveLoopEndNodeModel.createUseVariable();

    private final SettingsModelBoolean m_propagateVariables = RecursiveLoopEndNodeModel.createPropagateVariableModel();

    private final DialogComponentFlowVariableNameSelection m_flowVarSelection;

    private boolean m_varsAvailable = false;

    /**
     * Create new dialog.
     */
    RecursiveLoopEndNodeDialog() {
        createNewGroup("End settings");

        addDialogComponent(new DialogComponentNumber(RecursiveLoopEndNodeModel.createNumOfRowsModel(),
            "Minimal number of rows :", 1, 10));
        addDialogComponent(new DialogComponentNumber(RecursiveLoopEndNodeModel.createIterationsModel(),
            "Maximal number of iterations :", 10, 10));

        m_flowVarSelection = new DialogComponentFlowVariableNameSelection(m_endLoopVar, "",
            getAvailableFlowVariables().values(), false, FlowVariable.Type.STRING);

        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentBoolean(m_useVariable, "End Loop with Variable:"));
        addDialogComponent(m_flowVarSelection);
        setHorizontalPlacement(false);
        closeCurrentGroup();

        createNewGroup("Data settings");
        addDialogComponent(new DialogComponentBoolean(RecursiveLoopEndNodeModel.createOnlyLastModel(),
            "Collect data from last iteration only"));
        addDialogComponent(
            new DialogComponentBoolean(RecursiveLoopEndNodeModel.createAddIterationColumn(), "Add iteration column"));
        closeCurrentGroup();

        createNewGroup("Variable settings");
        addDialogComponent(new DialogComponentBoolean(m_propagateVariables, "Propagate modified loop variables"));
        closeCurrentGroup();

        // listener setup
        m_useVariable
            .addChangeListener(e -> m_endLoopVar.setEnabled(m_varsAvailable && m_useVariable.getBooleanValue()));
    }

    /**
     * List of available string flow variables must be updated since it could have changed.
     *
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        try {
            m_endLoopDeprecated.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException exc) {
            LOGGER.debug("Exception during loadAdditionalSettings:", exc);
            throw new NotConfigurableException(exc.getMessage());
        }

        // get all string flow vars
        final List<FlowVariable> vars = getAvailableFlowVariables().values().stream()
            .filter(v -> (v.getScope() == Scope.Flow) && (v.getType() == Type.STRING)).collect(Collectors.toList());

        if (vars.isEmpty()) {
            m_varsAvailable = false;
        } else {
            try {
                final String flowVar =
                    ((SettingsModelString)m_endLoopVar.createCloneWithValidatedValue(settings)).getStringValue();
                m_flowVarSelection.replaceListItems(vars, flowVar);
                m_varsAvailable = true;
            } catch (final InvalidSettingsException e) {
                LOGGER.warn("Could not clone settings object correctly!", e);
            }
        }
        // disable flow variable selection when no valid vars are available
        m_endLoopVar.setEnabled(m_varsAvailable && m_useVariable.getBooleanValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveAdditionalSettingsTo(settings);
        m_endLoopDeprecated.saveSettingsTo(settings);
    }

}
