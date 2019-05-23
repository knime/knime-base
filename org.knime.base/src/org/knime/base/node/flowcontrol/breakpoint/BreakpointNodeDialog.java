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
 */
package org.knime.base.node.flowcontrol.breakpoint;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * @author M. Berthold, University of Konstanz
 */
public class BreakpointNodeDialog extends DefaultNodeSettingsPane {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(BreakpointNodeDialog.class);

    /** break on table with zero rows. */
    static final String EMTPYTABLE = "empty table";

    /** break on active branch. */
    static final String ACTIVEBRANCH = "active branch";

    /** break on inactive branch. */
    static final String INACTIVEBRANCH = "inactive branch";

    /** break on variable having given value. */
    static final String VARIABLEMATCH = "variable matches value";

    private final AtomicBoolean m_varsAvailable = new AtomicBoolean(false);

    private final SettingsModelString m_varNameModel = createVarNameModel();

    private final SettingsModelString m_choicesModel = createChoiceModel(m_varNameModel, m_varsAvailable);

    private final SettingsModelString m_varValueModel = createVarValueModel();

    private final SettingsModelString m_customMessageModel = createCustomMessageModel();

    private final SettingsModelBoolean m_useCustomMessageModel = createUseCustomMessageModel(m_customMessageModel);

    private final SettingsModelBoolean m_enableModel = createEnableModel(m_choicesModel, m_varNameModel,
        m_varValueModel, m_useCustomMessageModel, m_customMessageModel, m_varsAvailable);

    private final DialogComponentStringSelection m_variableName;

    /**
     * Creates the dialog of the Breakpoint node.
     */
    public BreakpointNodeDialog() {
        final DialogComponentBoolean enable = new DialogComponentBoolean(m_enableModel, "Breakpoint Enabled");
        addDialogComponent(enable);
        final DialogComponentButtonGroup choices = new DialogComponentButtonGroup(m_choicesModel, false,
            "Breakpoint active for:", EMTPYTABLE, ACTIVEBRANCH, INACTIVEBRANCH, VARIABLEMATCH);
        addDialogComponent(choices);
        m_variableName =
            new DialogComponentStringSelection(m_varNameModel, "Select Variable: ", "no variables available");
        m_varNameModel.setEnabled(false);
        final DialogComponentString varvalue = new DialogComponentString(m_varValueModel, "Enter Variable Value: ");
        // the choice control enable-status of the variable entry fields.

        addDialogComponent(m_variableName);
        addDialogComponent(varvalue);

        setHorizontalPlacement(true);

        final DialogComponentBoolean useCustomMessage =
            new DialogComponentBoolean(m_useCustomMessageModel, "Custom message");

        final DialogComponentString customMessage = new DialogComponentString(m_customMessageModel, "");

        addDialogComponent(useCustomMessage);
        addDialogComponent(customMessage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        final Set<String> availableVars = this.getAvailableFlowVariables().keySet();
        if (availableVars.isEmpty()) {
            m_varsAvailable.set(false);
            m_varNameModel.setEnabled(false);
            m_varValueModel.setEnabled(false);
        } else {
            m_varsAvailable.set(true);
            try {
                final String var =
                    ((SettingsModelString)m_varNameModel.createCloneWithValidatedValue(settings)).getStringValue();
                m_variableName.replaceListItems(availableVars, var);
            } catch (final InvalidSettingsException e) {
                LOGGER.warn("Could not clone settings!", e);
            }
            m_choicesModel.setEnabled(m_enableModel.getBooleanValue());
            final boolean varsEnabled =
                m_enableModel.getBooleanValue() && VARIABLEMATCH.equals(m_choicesModel.getStringValue());
            m_varNameModel.setEnabled(varsEnabled);
            m_varValueModel.setEnabled(varsEnabled);
        }

        m_useCustomMessageModel.setEnabled(m_enableModel.getBooleanValue());
        m_customMessageModel.setEnabled(m_enableModel.getBooleanValue() && m_useCustomMessageModel.getBooleanValue());
    }

    /**
     * @return settingsmodel for custom error message
     */
    static SettingsModelString createCustomMessageModel() {
        return new SettingsModelString("Custom error message", "Breakpoint halted execution");
    }

    /**
     * @param customMessageModel
     * @return settingsmodel for whether to use a custom error message
     */
    static SettingsModelBoolean createUseCustomMessageModel(final SettingsModelString customMessageModel) {
        final SettingsModelBoolean useCustomMessageModel = new SettingsModelBoolean("Use custom error message", false);
        useCustomMessageModel
            .addChangeListener(c -> customMessageModel.setEnabled(useCustomMessageModel.getBooleanValue()));
        return useCustomMessageModel;
    }

    /**
     * @param varNameModel
     * @param e
     * @return settings model (choice) for node and dialog.
     */
    static SettingsModelString createChoiceModel(final SettingsModelString varNameModel,
        final AtomicBoolean varsAvailable) {
        final SettingsModelString choiceModel = new SettingsModelString("BreakPoint", EMTPYTABLE);

        choiceModel.addChangeListener(e -> {
            final boolean useVar = VARIABLEMATCH.equals(choiceModel.getStringValue());
            varNameModel.setEnabled(useVar && varsAvailable.get());
            varNameModel.setEnabled(useVar && varsAvailable.get());
        });

        return choiceModel;
    }

    /**
     * @return settings model (enable) for node and dialog.
     */
    static SettingsModelBoolean createEnableModel(final SettingsModelString choicesModel,
        final SettingsModelString varNameModel, final SettingsModelString varValueModel,
        final SettingsModelBoolean useCustomMessageModel, final SettingsModelString customMessageModel,
        final AtomicBoolean varsAvailable) {
        final SettingsModelBoolean enableModel = new SettingsModelBoolean("Enabled", false);

        // the enable button controls enable status of everything!
        // (but needs to keep in mind the variable choice settings)
        enableModel.addChangeListener(e -> {
            if (enableModel.getBooleanValue()) {
                choicesModel.setEnabled(true);
                final boolean useVar = varsAvailable.get() && VARIABLEMATCH.equals(choicesModel.getStringValue());
                varNameModel.setEnabled(useVar);
                varValueModel.setEnabled(useVar);
                useCustomMessageModel.setEnabled(true);
                customMessageModel.setEnabled(useCustomMessageModel.getBooleanValue());
            } else {
                choicesModel.setEnabled(false);
                varNameModel.setEnabled(false);
                varValueModel.setEnabled(false);
                useCustomMessageModel.setEnabled(false);
                customMessageModel.setEnabled(false);
            }
        });

        return enableModel;
    }

    /**
     * @return settings model (choice) for node and dialog.
     */
    static SettingsModelString createVarNameModel() {
        return new SettingsModelString("Variable Name", "");
    }

    /**
     * @return settings model (choice) for node and dialog.
     */
    static SettingsModelString createVarValueModel() {
        return new SettingsModelString("Variable Value", "0");
    }
}
