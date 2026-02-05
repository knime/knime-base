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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.meta.feature.selection;

import java.util.List;

import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.AutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;

/**
 * Node parameters for Feature Selection Loop End.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class FeatureSelectionLoopEndNodeParameters implements NodeParameters {

    @Persist(configKey = FeatureSelectionLoopEndSettings.CFG_SCORE_VARIABLE)
    @Widget(title = "Score", description = """
            The flow variable that contains the score for the model.
            """)
    @ChoicesProvider(DoubleFlowVariablesProvider.class)
    @ValueReference(ScorerVariableRef.class)
    @ValueProvider(ScorerVariableNameProvider.class)
    String m_scoreVariableName;

    static final class ScorerVariableRef implements ParameterReference<String> {
    }

    @Persist(configKey = FeatureSelectionLoopEndSettings.CFG_IS_MINIMIZE)
    @Widget(title = "Minimize score", description = """
            Check this option if you want to minimize the score (if you use error as score, for example, and you want
            to minimize error). If the option is left unchecked, the score will be maximized.
            """)
    boolean m_isMinimize;

    static final class DoubleFlowVariablesProvider implements FlowVariableChoicesProvider {

        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            return context.getAvailableInputFlowVariables(DoubleType.INSTANCE).values().stream().toList();
        }

    }

    static final class ScorerVariableNameProvider extends AutoGuessValueProvider<String> {

        protected ScorerVariableNameProvider() {
            super(ScorerVariableRef.class);
        }

        @Override
        protected boolean isEmpty(final String value) {
            return value == null || value.isEmpty();
        }

        @Override
        protected String autoGuessValue(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return parametersInput
                .getAvailableInputFlowVariables(DoubleType.INSTANCE).values()
                .stream().map(FlowVariable::getName).findFirst().orElse(null);
        }

    }

}
