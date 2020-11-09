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
 */
package org.knime.base.node.flowvariable.variabletocredentials;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.CredentialsStore.CredentialsNode;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.WorkflowLoadHelper;

/**
 * Node model implementation.
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
final class VariableToCredentialsNodeModel extends NodeModel implements CredentialsNode {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(VariableToCredentialsNodeModel.class);

    private static final String CREDENTIAL_ID_LABEL = "credentials identifier";
    private static final String USERNAME_LABEL = "username";
    private static final String PASSWORD_LABEL = "password";
    private final SettingsModelString m_credentialsNameModel = createCredentialsNameModel();
    private final SettingsModelString m_usernameModel = createUserModel();
    private final SettingsModelString m_passwordModel = createPwdModel();
    static final String EMPTY_ELEMENT = "<none available>";

    /**
     * @return the credentials name model
     */
    static SettingsModelString createCredentialsNameModel() {
        return new SettingsModelString("credentialsName", "");
    }

    /**
     * @return the password model
     */
    static SettingsModelString createPwdModel() {
        return new SettingsModelString("passwordVarName", ""); // NOSONAR
    }

    /**
     * @return the user model.
     */
    static SettingsModelString createUserModel() {
        return new SettingsModelString("usernameVarName", ""); // NOSONAR
    }

    VariableToCredentialsNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE}, new PortType[]{FlowVariablePortObject.TYPE});
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        pushCredentials();
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        // must not call pushCredentials() as it has been done in configure()
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }


    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_credentialsNameModel.saveSettingsTo(settings);
        m_usernameModel.saveSettingsTo(settings);
        m_passwordModel.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        checkNotEmpty(m_credentialsNameModel, settings, CREDENTIAL_ID_LABEL);
        checkNotEmpty(m_usernameModel, settings, USERNAME_LABEL);
        checkNotEmpty(m_passwordModel, settings, PASSWORD_LABEL);
    }

    private static void checkNotEmpty(final SettingsModelString model, final NodeSettingsRO settings,
        final String name) throws InvalidSettingsException {
        final String val = model.<SettingsModelString>createCloneWithValidatedValue(settings).getStringValue();
        if (StringUtils.isEmpty(val) || EMPTY_ELEMENT.equals(val)) {
            throw new InvalidSettingsException("Please provide the " + name);
        }
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_credentialsNameModel.loadSettingsFrom(settings);
        m_usernameModel.loadSettingsFrom(settings);
        m_passwordModel.loadSettingsFrom(settings);
    }

    private void pushCredentials() throws InvalidSettingsException {
        final Map<String, FlowVariable> variables = getAvailableFlowVariables(VariableType.StringType.INSTANCE);
        final String credentialsName = m_credentialsNameModel.getStringValue();
        final FlowVariable userVar =
                getVariableOrThrow(USERNAME_LABEL, m_usernameModel.getStringValue(), variables);
        final FlowVariable passwordVar =
                getVariableOrThrow(PASSWORD_LABEL, m_passwordModel.getStringValue(), variables);
        pushCredentialsFlowVariable(credentialsName, userVar.getStringValue(), passwordVar.getStringValue());
    }

    private static FlowVariable getVariableOrThrow(final String name, final String val, final Map<String, FlowVariable> variables)
        throws InvalidSettingsException {
        if (StringUtils.isEmpty(val)) {
            throw new InvalidSettingsException("Please select the " + name + " variable");
        }
        final FlowVariable variable = variables.get(val);
        if (variable == null) {
            throw new InvalidSettingsException("Selected " + name + " variable (" + val + ") no longer available");
        }
        return variable;
    }

    @Override
    public void doAfterLoadFromDisc(final WorkflowLoadHelper loadHelper, final CredentialsProvider credProvider,
        final boolean isExecuted, final boolean isInactive) {
        try {
            pushCredentials();
        } catch (InvalidSettingsException e) {
            //this happens if the node isn't properly configured and thus can not register the credentials
            LOGGER.debug("Couldn't restore credentials on load due to invalid configuration", e);
        }
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //nothing to do
    }

    @Override
    protected void reset() {
        //nothing to do
    }
}
