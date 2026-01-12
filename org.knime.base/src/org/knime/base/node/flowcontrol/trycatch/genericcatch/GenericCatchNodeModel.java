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
 *   Sept 17 2008 (mb): created (from wiswedel's TableToVariableNode)
 */
package org.knime.base.node.flowcontrol.trycatch.genericcatch;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchConsumer;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.workflow.FlowTryCatchContext;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.ScopeEndNode;
import org.knime.core.node.workflow.VariableType;

/**
 * End of an Try-Catch Enclosure. Takes the first input if active (i.e. did
 * not fail during execution) or the second one if not. Hence the second port
 * can be fed with the default object used downstream from here when the original
 * branch failed.
 *
 * @author M. Berthold, University of Konstanz
 */
final class GenericCatchNodeModel extends NodeModel
    implements ScopeEndNode<FlowTryCatchContext>, InactiveBranchConsumer {

    static final String VAR_FAILING_NAME = "FailingNode";
    static final String VAR_FAILING_MESSAGE = "FailingNodeMessage";
    static final String VAR_FAILING_DETAILS = "FailingNodeDetails";
    static final String VAR_FAILING_STACKTRACE = "FailingNodeStackTrace";

    static final String CFG_FAILING_NAME = "CFG_DEFAULT_TEXT_VARIABLE";
    static final String CFG_FAILING_MESSAGE = "CFG_DEFAULT_TEXT_MESSAGE";
    static final String CFG_FAILING_DETAILS = "CFG_DEFAULT_TEXT_DETAILS";
    static final String CFG_FAILING_STACKTRACE = "CFG_DEFAULT_STACK_TRACE";
    static final String CFG_ALWAYS_POPULATE = "CFG_ALWAYS_POPULATE";
    static final String CFG_PROPAGATE_VARIABLES = "CFG_PROPAGATE_VARIABLES";

    // new since 2.11
    private final SettingsModelBoolean m_alwaysPopulate = getAlwaysPopulate();

    private final SettingsModelString m_defaultVariable = getDefaultVariable(m_alwaysPopulate);
    private final SettingsModelString m_defaultMessage = getDefaultMessage(m_alwaysPopulate);
    private final SettingsModelString m_defaultDetails = getDefaultDetails(m_alwaysPopulate);
    private final SettingsModelString m_defaultStackTrace = getDefaultStackTrace(m_alwaysPopulate);

    // added in 4.4
    private final SettingsModelBoolean m_propagateVariables = createPropagateVariablesModel();

    /**
     * Multiple inputs, multiple outputs.
     *
     * @param pTypes types of ports.
     */
    protected GenericCatchNodeModel(final PortType... pTypes) {
        super(ArrayUtils.addAll(pTypes, pTypes), ArrayUtils.add(pTypes, FlowVariablePortObject.TYPE));
    }

    /** Generic constructor.
     */
    protected GenericCatchNodeModel() {
        this(PortObject.TYPE);
    }

    @Override
    public Class<FlowTryCatchContext> getFlowScopeContextClass() {
        return FlowTryCatchContext.class;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (m_alwaysPopulate.getBooleanValue()) {
            pushFlowVariableString(VAR_FAILING_NAME, m_defaultVariable.getStringValue());
            pushFlowVariableString(VAR_FAILING_MESSAGE, m_defaultMessage.getStringValue());
            pushFlowVariableString(VAR_FAILING_DETAILS, m_defaultDetails.getStringValue());
            pushFlowVariableString(VAR_FAILING_STACKTRACE, m_defaultStackTrace.getStringValue());
        }

        final var numInputs = inSpecs.length / 2;
        final var outputSpecs = new PortObjectSpec[numInputs + 1];
        outputSpecs[numInputs] = FlowVariablePortObjectSpec.INSTANCE;

        for (int i = 0; i < numInputs; i++) {
            if (inSpecs[i] instanceof InactiveBranchPortObjectSpec) {
             // main branch inactive, grab specs from alternative (default) input
                System.arraycopy(inSpecs, numInputs, outputSpecs, 0, numInputs);
                break;
            }

            // main branch is active - no failure so far...
            outputSpecs[i] = inSpecs[i];
        }

        return outputSpecs;
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final var numInputs = inData.length / 2;
        final var outputs = new PortObject[numInputs + 1];
        Arrays.fill(outputs, PortObject.TYPE);
        outputs[numInputs] = FlowVariablePortObject.INSTANCE;

        for (int i = 0; i < numInputs; i++) {
            if (inData[i] instanceof InactiveBranchPortObject) {
                // main branch inactive, grab spec from alternative (default) input
                // and push error reasons on stack (they come from the ScopeObject
                // which will we removed after this node, closing the scope).
                pushErrorFlowVars();
                System.arraycopy(inData, numInputs, outputs, 0, numInputs);
                break;
            }

            // main branch is active - no failure so far...
            outputs[i] = inData[i];
        }

        propagateVariables();
        return outputs;
    }

    private void pushErrorFlowVars() {
        FlowTryCatchContext ftcc = getFlowContext();
        if ((ftcc != null) && (ftcc.hasErrorCaught())) {
            pushFlowVariableString(VAR_FAILING_NAME, ftcc.getNode());
            pushFlowVariableString(VAR_FAILING_MESSAGE, ftcc.getReason());
            pushFlowVariableString(VAR_FAILING_DETAILS, ftcc.getDetails());
            pushFlowVariableString(VAR_FAILING_STACKTRACE, ftcc.getStacktrace());
        } else if (m_alwaysPopulate.getBooleanValue()) {
            pushFlowVariableString(VAR_FAILING_NAME, m_defaultVariable.getStringValue());
            pushFlowVariableString(VAR_FAILING_MESSAGE, m_defaultMessage.getStringValue());
            pushFlowVariableString(VAR_FAILING_DETAILS, m_defaultDetails.getStringValue());
            pushFlowVariableString(VAR_FAILING_STACKTRACE, m_defaultStackTrace.getStringValue());
        }
    }

    /**
     * Calls {@link #pushFlowVariable(String, VariableType, Object)} for variables defined within
     * the scope IFF the corresponding configuration is set.
     */
    @SuppressWarnings({"rawtypes", "unchecked", "java:S3740"})
    private void propagateVariables() {
        if (m_propagateVariables.getBooleanValue()) {
            for (FlowVariable v : getVariablesDefinedInScope()) {
                VariableType t = v.getVariableType();
                pushFlowVariable(v.getName(), t, v.getValue(t));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_alwaysPopulate.saveSettingsTo(settings);
        m_propagateVariables.saveSettingsTo(settings);

        m_defaultVariable.saveSettingsTo(settings);
        m_defaultMessage.saveSettingsTo(settings);
        m_defaultDetails.saveSettingsTo(settings);
        m_defaultStackTrace.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(m_defaultMessage.getKey())) {
            m_alwaysPopulate.validateSettings(settings);
            m_defaultMessage.validateSettings(settings);
            m_defaultVariable.validateSettings(settings);
            m_defaultStackTrace.validateSettings(settings);
        }
        // added in 4.4 (but if present it needs to be "valid")
        if (settings.containsKey(m_propagateVariables.getConfigName())) {
            m_propagateVariables.validateSettings(settings);
        }
        // added in 5.4 (but if present it needs to be "valid")
        if (settings.containsKey(CFG_FAILING_DETAILS)) {
            m_defaultDetails.validateSettings(settings);
        }
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(m_defaultMessage.getKey())) {
            m_alwaysPopulate.loadSettingsFrom(settings);

            m_defaultVariable.loadSettingsFrom(settings);
            m_defaultMessage.loadSettingsFrom(settings);
            m_defaultStackTrace.loadSettingsFrom(settings);
        }
        // setting was added in 4.4 (AP-16447)
        if (settings.containsKey(m_propagateVariables.getConfigName())) {
            m_propagateVariables.loadSettingsFrom(settings);
        } else {
            // was 'false' in prior versions
            m_propagateVariables.setBooleanValue(false);
        }
        // setting was added in 5.4 (AP-23343)
        if (settings.containsKey(CFG_FAILING_DETAILS)) {
            m_defaultDetails.loadSettingsFrom(settings);
        } else {
            // set to error message default for prior versions
            m_defaultDetails.setStringValue(m_defaultMessage.getStringValue());
        }
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * @return the SM for always populating the model. (if true, the flow variables will always be shown)
     */
    static SettingsModelBoolean getAlwaysPopulate() {
        return new SettingsModelBoolean(CFG_ALWAYS_POPULATE, false);
    }

    /**
     * @param alwaysPopulateModel For enable/disable-ment
     * @return the SM for the default variable if the is failing
     */
    static SettingsModelString getDefaultVariable(final SettingsModelBoolean alwaysPopulateModel) {
        return registerEnablementListener(new SettingsModelString(CFG_FAILING_NAME, "none"),
            alwaysPopulateModel);
    }

    /**
     * @param alwaysPopulateModel For enable/disable-ment
     * @return the SM for the default text message if the node is failing.
     */
    static SettingsModelString getDefaultMessage(final SettingsModelBoolean alwaysPopulateModel) {
        return registerEnablementListener(new SettingsModelString(CFG_FAILING_MESSAGE, "none"),
            alwaysPopulateModel);
    }

    /**
     * @param alwaysPopulateModel For enable/disable-ment
     * @return the SM for the default text details if the node is failing.
     */
    static SettingsModelString getDefaultDetails(final SettingsModelBoolean alwaysPopulateModel) {
        return registerEnablementListener(new SettingsModelString(CFG_FAILING_DETAILS, "none"),
            alwaysPopulateModel);
    }

    /**
     * @param alwaysPopulateModel For enable/disable-ment
     * @return the SM for the default variable if the is failing
     */
    static SettingsModelString getDefaultStackTrace(final SettingsModelBoolean alwaysPopulateModel) {
        return registerEnablementListener(new SettingsModelString(CFG_FAILING_STACKTRACE, "none"),
            alwaysPopulateModel);
    }

    private static SettingsModelString registerEnablementListener(final SettingsModelString stringModel,
        final SettingsModelBoolean alwaysPopulateModel) {
        ChangeListener changeListener = e -> stringModel.setEnabled(alwaysPopulateModel.getBooleanValue());
        alwaysPopulateModel.addChangeListener(changeListener);
        changeListener.stateChanged(null);
        return stringModel;
    }

    static SettingsModelBoolean createPropagateVariablesModel() {
        return new SettingsModelBoolean(CFG_PROPAGATE_VARIABLES, true);
    }
}
