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
 *   Aug 19, 2025 (AI Migration): created
 */
package org.knime.base.node.meta.looper.variable.end;

import org.knime.base.node.flowvariable.converter.variabletocell.VariableToCellConverterFactory.ConvertibleFlowVariablesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.LegacyFlowVariableFilterPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilter;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilterWidget;

/**
 * Settings for the "Variable Loop End" node (modern Web UI dialog).
 * <p>
 * The node collects (per loop iteration) the selected flow variables and outputs a table containing one row per
 * iteration with a column for each chosen variable. Optionally, modified variables inside the loop body can be
 * propagated back to the flow variable stack after loop termination.
 * </p>
 *
 * @author AI (migration)
 * @since 5.7
 */
@SuppressWarnings("restriction")
final class LoopEndVariableNodeSettings implements NodeParameters {

    /** Custom persistor for flow variable filter using legacy 'variable_filter' config key. */
    static final class VariableFilterPersistor extends LegacyFlowVariableFilterPersistor {
        VariableFilterPersistor() {
            super("variable_filter");
        }
    }

    /**
     * Filter specifying which flow variables are converted into columns of the result table. The include list defines
     * the variables to output; excluded variables are ignored. Pattern, type, and manual selection modes are supported.
     */
    /**
     * Filter specifying which flow variables are converted into columns of the result table. The include list defines
     * the variables to output; excluded variables are ignored. Pattern, type, and manual selection modes are supported.
     */
    @Widget(title = "Variables", description = "Flow variables to add as columns to the output table. "
        + "Use the filter options to control which variables are available for selection.")
    @FlowVariableFilterWidget(choicesProvider = ConvertibleFlowVariablesProvider.class)
    @Persistor(VariableFilterPersistor.class)
    FlowVariableFilter m_filter = new FlowVariableFilter();

    /** Persistor for the propagate-loop-variables boolean using a SettingsModel to stay backwards compatible. */
    static final class PropagateLoopVariablesPersistor extends SettingsModelBooleanPersistor {
        PropagateLoopVariablesPersistor() { super("propagateLoopVariables"); }
    }

    /**
     * Whether modified loop variables (whose values changed inside the loop body) should be written back onto the flow
     * variable stack after the loop finishes. Disable to keep outer-scope values unchanged.
     */
    @Widget(title = "Propagate modified loop variables", description = "If enabled, any flow variable whose value was "
        + "changed inside the loop is propagated (its final value after the last iteration). Disable to leave the "
        + "values from before entering the loop untouched.")
    @Persistor(PropagateLoopVariablesPersistor.class)
    boolean m_propagateLoopVariables = false; // default matches old SettingsModelBoolean default
}
