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
 *  you the additional permission to use and propagate KNIME with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided this
 *  license scheme is properly communicated. Any other combination of
 *  this software or parts thereof with other software is allowed only
 *  under the same license conditions as applicable to the other software.
 *  KNIME is a trademark of KNIME AG.
 *
 * History
 *   Created on Aug 19, 2025 by Paul Bärnreuther
 */
package org.knime.base.node.meta.looper.variable.end;

import java.util.List;

import org.knime.base.node.flowvariable.converter.variabletocell.VariableToCellConverterFactory;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilter;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilterWidget;

/**
 * Settings for the Loop End (Variable) node.
 */
public final class LoopEndVariableSettings implements NodeParameters {

    @Widget(title = "Flow Variables", 
            description = "Select flow variables to convert. Only variables with supported types are available for selection.")
    @FlowVariableFilterWidget(choicesProvider = SupportedVariablesProvider.class)
    @Persist(configKey = "variable_filter")
    FlowVariableFilter m_variableFilter = new FlowVariableFilter();

    @Widget(title = "Propagate modified loop variables", 
            description = "Check to propagate any loop variables that have been modified by nodes inside the loop.")
    @Persist(configKey = "propagateLoopVariables")
    boolean m_propagateLoopVariables = false;

    /**
     * A {@link FlowVariableChoicesProvider} that provides a list of flow variables 
     * that are convertible using VariableToCellConverterFactory.
     */
    public static final class SupportedVariablesProvider implements FlowVariableChoicesProvider {
        
        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            return ((NodeParametersInputImpl) context)
                .getAvailableInputFlowVariables(VariableToCellConverterFactory.getSupportedTypes())
                .values().stream().toList();
        }
    }
}