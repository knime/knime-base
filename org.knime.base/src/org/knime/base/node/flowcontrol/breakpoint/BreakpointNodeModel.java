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
package org.knime.base.node.flowcontrol.breakpoint;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.BufferedDataTable;
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
import org.knime.core.node.port.inactive.InactiveBranchConsumer;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * A simple breakpoint node which allows to halt execution when a certain condition on the input table is fulfilled
 * (such as is-empty, is-inactive, is-active, ...).
 *
 * @author M. Berthold, University of Konstanz
 */
public class BreakpointNodeModel extends NodeModel implements InactiveBranchConsumer {

    private final SettingsModelString m_varname = BreakpointNodeDialog.createVarNameModel();

    private final SettingsModelString m_varvalue = BreakpointNodeDialog.createVarValueModel();

    private final SettingsModelString m_choice =
        BreakpointNodeDialog.createChoiceModel(m_varname, new AtomicBoolean(true));

    // since 3.8

    private final SettingsModelString m_customMessageModel = BreakpointNodeDialog.createCustomMessageModel();

    private final SettingsModelBoolean m_useCustomMessageModel =
        BreakpointNodeDialog.createUseCustomMessageModel(m_customMessageModel);

    private final SettingsModelBoolean m_enabled = BreakpointNodeDialog.createEnableModel(m_choice, m_varname,
        m_varvalue, m_useCustomMessageModel, m_customMessageModel, new AtomicBoolean(true));

    /**
     * One input, one output.
     */
    protected BreakpointNodeModel() {
        super(1, 1);
        m_choice.setEnabled(m_enabled.getBooleanValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (m_choice.getStringValue().equals(BreakpointNodeDialog.VARIABLEMATCH)
            && (getAvailableFlowVariables().get(m_varname.getStringValue()) == null)
            && (!ArrayUtils.contains(inSpecs, InactiveBranchPortObjectSpec.INSTANCE))) { // see AP-11667
            throw new InvalidSettingsException(
                "Selected flow variable: '" + m_varname.getStringValue() + "' not available!");

        }
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        if (!m_enabled.getBooleanValue()) {
            return inData;
        }

        String message = null;
        boolean throwException = false;

        if (m_choice.getStringValue().equals(BreakpointNodeDialog.EMTPYTABLE)
            && (inData[0] instanceof BufferedDataTable) && (((BufferedDataTable)inData[0]).size() == 0)) {
            message = "Breakpoint halted execution (table is empty)";
            throwException = true;
        }
        if (m_choice.getStringValue().equals(BreakpointNodeDialog.ACTIVEBRANCH)
            && !(inData[0] instanceof InactiveBranchPortObject)) {
            message = "Breakpoint halted execution (branch is active)";
            throwException = true;
        }
        if (m_choice.getStringValue().equals(BreakpointNodeDialog.INACTIVEBRANCH)
            && (inData[0] instanceof InactiveBranchPortObject)) {
            message = "Breakpoint halted execution (branch is inactive)";
            throwException = true;
        }
        if (m_choice.getStringValue().equals(BreakpointNodeDialog.VARIABLEMATCH)) {
            final FlowVariable fv = getAvailableFlowVariables().get(m_varname.getStringValue());
            if ((fv != null) && fv.getValueAsString().equals(m_varvalue.getStringValue())) {
                message = "Breakpoint halted execution (" + m_varname.getStringValue() + "="
                    + m_varvalue.getStringValue() + ")";
                throwException = true;
            }
        }

        if (throwException) {
            if (m_useCustomMessageModel.getBooleanValue()) {
                message = m_customMessageModel.getStringValue();
            }
            throw new Exception(message);
        }

        // unrecognized option: assume disabled.
        return inData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_enabled.validateSettings(settings);
        m_choice.validateSettings(settings);
        m_varname.validateSettings(settings);
        m_varvalue.validateSettings(settings);
        if (settings.containsKey(m_useCustomMessageModel.getConfigName())) {
            // maintaining backwards comp.
            m_useCustomMessageModel.validateSettings(settings);
            m_customMessageModel.validateSettings(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_enabled.loadSettingsFrom(settings);
        m_choice.loadSettingsFrom(settings);
        m_varname.loadSettingsFrom(settings);
        m_varvalue.loadSettingsFrom(settings);

        if (settings.containsKey(m_useCustomMessageModel.getConfigName())) {
            // maintaining backwards comp.
            m_useCustomMessageModel.loadSettingsFrom(settings);
            m_customMessageModel.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // ignore -> no view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_enabled.saveSettingsTo(settings);
        m_choice.saveSettingsTo(settings);
        m_varname.saveSettingsTo(settings);
        m_varvalue.saveSettingsTo(settings);
        m_useCustomMessageModel.saveSettingsTo(settings);
        m_customMessageModel.saveSettingsTo(settings);
    }

}
