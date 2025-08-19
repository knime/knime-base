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
 *   Oct 23, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.meta.looper.variable.end;

import java.util.List;

import org.knime.base.node.flowvariable.converter.variabletocell.VariableToCellConverterFactory;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilter;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilterWidget;

/**
 * Node settings for the Variable Loop End node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class LoopEndVariableNodeSettings implements NodeParameters {

    /**
     * Layout interface for organizing dialog sections.
     */
    interface DialogLayout {
        @Section(title = "Variable Selection")
        interface VariableSelection {
        }

        @Section(title = "Loop Variables")
        @After(VariableSelection.class)
        interface LoopVariables {
        }
    }

    /**
     * Flow variable choices provider that filters to supported variable types for conversion to data cells.
     */
    static final class SupportedVariableTypesProvider implements FlowVariableChoicesProvider {
        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            final VariableType<?>[] supportedTypes = VariableToCellConverterFactory.getSupportedTypes();
            return context.getFlowVariables()
                .stream()
                .filter(var -> {
                    for (VariableType<?> type : supportedTypes) {
                        if (type.equals(var.getVariableType())) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();
        }
    }

    @Widget(title = "Variable selection", description = """
            Select which variables will be collected and put into the output table. \
            Only variables with supported data types can be converted to table columns.
            """)
    @FlowVariableFilterWidget(choicesProvider = SupportedVariableTypesProvider.class)
    @Layout(DialogLayout.VariableSelection.class)
    @Persist(configKey = "variable_filter")
    FlowVariableFilter m_variableFilter = new FlowVariableFilter();

    @Widget(title = "Propagate modified loop variables", description = """
            If checked, variables whose values are modified within the loop are exported by this node. \
            These variables must be declared outside the loop, i.e. injected into the loop from a side-branch \
            or be available upstream of the corresponding loop start node. For the latter, any modification \
            of a variable is passed back to the start node in subsequent iterations (e.g. moving sum calculation). \
            Note that variables defined by the loop start node itself are excluded as these usually represent \
            loop controls (e.g. "currentIteration").
            """)
    @Layout(DialogLayout.LoopVariables.class)
    @Persist(configKey = "propagateLoopVariables")
    boolean m_propagateLoopVariables = false;
}
