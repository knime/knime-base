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
 *   20 Oct 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.groupby;

import java.util.List;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.node.preproc.groupby.OptionalParameters.AggregationOperatorParameters;
import org.knime.base.node.preproc.groupby.OptionalParameters.NoOperatorParameters;
import org.knime.base.node.util.regex.PatternType;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.SubParameters;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

class PatternAggregatorElement implements NodeParameters {
    @Widget(title = "Search pattern", description = "The search pattern to match column names")
    String m_pattern = "";

    static final class PatternTypeChoices implements EnumChoicesProvider<PatternType> {
        @Override
        public List<PatternType> choices(final NodeParametersInput context) {
            return List.of(PatternType.WILDCARD, PatternType.REGEX);
        }
    }

    @Widget(title = "Pattern type", description = "...")
    @ValueSwitchWidget
    @ChoicesProvider(PatternTypeChoices.class)
    PatternType m_isRegex = PatternType.WILDCARD;

    @Widget(title = "Aggregation", description = "The aggregation method to use")
    @SubParameters(subLayoutRoot = PatternOperatorParametersRef.class,
        showSubParametersProvider = HasPatternOperatorParameters.class)
    @ValueReference(PatternAggregationRef.class)
    @ChoicesProvider(PatternAggregationChoices.class)
    String m_aggregationMethod = "First";

    @Widget(title = "Missing values", description = "")
    @ValueSwitchWidget
    MissingValueOption m_includeMissing = MissingValueOption.EXCLUDE;

    // TODO show new parameters via extension point if defined
    @DynamicParameters(value = PatternAggregationOperatorParametersProvider.class,
        widgetAppearingInNodeDescription = @Widget(title = "Operator settings", description = "...", advanced = true))
    @ValueReference(PatternOperatorParametersRef.class)
    @Layout(PatternOperatorParametersRef.class)
    AggregationOperatorParameters m_parameters = new NoOperatorParameters();

    static final class PatternAggregationRef implements ParameterReference<String> {
    }

    static final class PatternOperatorParametersRef implements ParameterReference<AggregationOperatorParameters> {
    }

    static final class HasPatternOperatorParameters extends HasOperatorParameters {
        @Override
        Class<? extends ParameterReference<String>> getAggregationMethodRefClass() {
            return PatternAggregationRef.class;
        }
    }

    static final class PatternAggregationOperatorParametersProvider extends AggregationOperatorParametersProvider {
        @Override
        Class<? extends ParameterReference<AggregationOperatorParameters>> getParameterRefClass() {
            return PatternOperatorParametersRef.class;
        }
    }

    static final class PatternAggregationChoices implements StringChoicesProvider {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
        }

        @Override
        public List<StringChoice> computeState(final NodeParametersInput parametersInput) {
            // TODO filter?
            return AggregationMethods.getAvailableMethods().stream()
                .map(agg -> new StringChoice(agg.getId(), agg.getLabel())).toList();
        }

    }
}