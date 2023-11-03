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

import static org.knime.base.node.flowvariable.variabletocredentials.VariableToCredentialsNodeModel.EMPTY_ELEMENT;
import static org.knime.base.node.flowvariable.variabletocredentials.VariableToCredentialsNodeModel.NONE_ELEMENT;
import static org.knime.base.node.flowvariable.variabletocredentials.VariableToCredentialsNodeModel.createCredentialsNameModel;
import static org.knime.base.node.flowvariable.variabletocredentials.VariableToCredentialsNodeModel.createPwdModel;
import static org.knime.base.node.flowvariable.variabletocredentials.VariableToCredentialsNodeModel.createSecondFactorModel;
import static org.knime.base.node.flowvariable.variabletocredentials.VariableToCredentialsNodeModel.createUserModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.VariableType;

/**
 * Node dialog implementation.
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
final class VariableToCredentialsNodeDialog extends DefaultNodeSettingsPane {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(VariableToCredentialsNodeDialog.class);

    private static final String[] DEFAULT_LIST = new String[] {EMPTY_ELEMENT};

    private final DialogComponentStringSelection m_usernameComponent  =
            new DialogComponentStringSelection(createUserModel(), "Username variable: ", DEFAULT_LIST);

    private final DialogComponentStringSelection m_passwordComponent =
            new DialogComponentStringSelection(createPwdModel(), "Password variable: ", DEFAULT_LIST);

    private final DialogComponentStringSelection m_secondFactorComponent =
            new DialogComponentStringSelection(createSecondFactorModel(), "Second factor variable: ", DEFAULT_LIST);

    private final DialogComponentString m_credentialsNameComponent;

    VariableToCredentialsNodeDialog() {
        final FlowVariableModel flowVar =
            createFlowVariableModel(createCredentialsNameModel().getKey(), VariableType.StringType.INSTANCE);
        m_credentialsNameComponent =
            new DialogComponentString(createCredentialsNameModel(), "Credentials name: ", true, 15, flowVar);
        addDialogComponent(m_credentialsNameComponent);
        flowVar.addChangeListener(evt -> {
            final FlowVariableModel vm = (FlowVariableModel)(evt.getSource());
            if (vm.isVariableReplacementEnabled()) {
                ((SettingsModelString)m_credentialsNameComponent.getModel())
                .setStringValue(vm.getVariableValue().get().getValueAsString());
            }
        });
        addDialogComponent(m_usernameComponent);
        addDialogComponent(m_passwordComponent);
        addDialogComponent(m_secondFactorComponent);
    }

    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadAdditionalSettingsFrom(settings, specs);
        final Set<String> variableNames =
                getAvailableFlowVariables(VariableType.StringType.INSTANCE).keySet();
        final List<String> sortedNames = new ArrayList<>(variableNames);
        sortedNames.remove("knime.workspace");
        Collections.sort(sortedNames);
        if (sortedNames.isEmpty()) {
            sortedNames.add(EMPTY_ELEMENT);
        }
        final List<String> namesWithNone = new LinkedList<>(sortedNames);
        namesWithNone.add(0, NONE_ELEMENT);
        replaceList(settings, sortedNames, m_usernameComponent);
        replaceList(settings, sortedNames, m_passwordComponent);
        replaceList(settings, namesWithNone, m_secondFactorComponent);
    }

    private static void replaceList(final NodeSettingsRO settings, final List<String> sortedNames,
        final DialogComponentStringSelection component) {
        try {
            final String var = ((SettingsModelString)component.getModel().createCloneWithValidatedValue(settings))
                    .getStringValue();
            component.replaceListItems(sortedNames, var);
        } catch (final InvalidSettingsException e) {
            LOGGER.warn("Could not clone settings!", e);
        }
    }
}
