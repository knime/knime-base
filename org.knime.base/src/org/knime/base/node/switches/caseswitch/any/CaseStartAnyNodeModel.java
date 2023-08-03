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
package org.knime.base.node.switches.caseswitch.any;

import java.io.File;
import java.util.Arrays;

import org.knime.base.node.switches.startcase.StartcaseNodeModel;
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
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.util.CheckUtils;

/**
 * Start of a CASE Statement. Takes the table from one branch and outputs it to exactly one outport.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author M. Berthold, University of Konstanz (original {@link StartcaseNodeModel} as a base)
 */
final class CaseStartAnyNodeModel extends NodeModel {

    private static final String ACTIVATE_OUTPUT_CFG = "activate_all_outputs_during_configure";

    private static final String ERROR_MESSAGE_TEMPLATE = "Invalid output port \"%s\" specified.";

    private SettingsModelString m_selectedPort = createChoiceModel();

    private final SettingsModelBoolean m_activateAllOutputsDuringConfigureModel =
        createActivateAllOutputsDuringConfigureModel();

    /**
     * @param inPorts the input ports
     * @param outPorts the output ports
     */
    CaseStartAnyNodeModel(final PortType[] inPorts, final PortType[] outPorts) {
        super(inPorts, outPorts);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (getNrInPorts() == 0) {
            throw new InvalidSettingsException("Please select an input type!");
        }
        final var outspecs = new PortObjectSpec[getNrOutPorts()];
        final var defSpec = m_activateAllOutputsDuringConfigureModel.getBooleanValue()//
            ? inSpecs[0]//
            : InactiveBranchPortObjectSpec.INSTANCE;
        Arrays.fill(outspecs, defSpec);
        try {
            final int index = getSelectedOutputPort();
            outspecs[index] = inSpecs[0];
        } catch (InvalidSettingsException ise) {
            if (m_activateAllOutputsDuringConfigureModel.getBooleanValue()) {
                // Warn the user, but configure the node so downstream nodes can be configured
                setWarningMessage(ise.getMessage());
            } else {
                throw ise;
            }
        }
        return outspecs;
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final int index = getSelectedOutputPort();
        final var outs = new PortObject[getNrOutPorts()];
        Arrays.fill(outs, InactiveBranchPortObject.INSTANCE);
        outs[index] = inData[0];
        return outs;
    }

    /**
     * Parse the index of the selected output port and verify that it's valid
     *
     * @return the output port index
     * @throws InvalidSettingsException If the output port is either non-numeric or out of bounds
     */
    private int getSelectedOutputPort() throws InvalidSettingsException {
        final int index;
        try {
            index = Integer.parseInt(m_selectedPort.getStringValue());
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException(String.format(ERROR_MESSAGE_TEMPLATE, m_selectedPort.getStringValue()),
                nfe);
        }
        CheckUtils.checkSetting(index >= 0 && index < getNrOutPorts(), ERROR_MESSAGE_TEMPLATE,
            m_selectedPort.getStringValue());
        return index;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_selectedPort.saveSettingsTo(settings);
        m_activateAllOutputsDuringConfigureModel.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selectedPort.validateSettings(settings);
        if (settings.containsKey(ACTIVATE_OUTPUT_CFG)) {
            m_activateAllOutputsDuringConfigureModel.validateSettings(settings);
        }
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(ACTIVATE_OUTPUT_CFG)) {
            m_activateAllOutputsDuringConfigureModel.loadSettingsFrom(settings);
        } else {
            m_activateAllOutputsDuringConfigureModel.setBooleanValue(false);
        }
        m_selectedPort.loadSettingsFrom(settings);
    }

    @Override
    protected void reset() {
        // empty
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // empty
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // empty
    }

    /**
     * @return name of PMML file model
     */
    static SettingsModelString createChoiceModel() {
        return new SettingsModelString("PortIndex", "0");
    }

    static SettingsModelBoolean createActivateAllOutputsDuringConfigureModel() {
        return new SettingsModelBoolean(ACTIVATE_OUTPUT_CFG, true);
    }

}
