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
 *   Nov 14, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.columnaggregator;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.AggregationOperatorParameters;
import org.knime.base.node.preproc.columnaggregator.ColumnAggregatorNodeParameters.AggregationColumnsRef;
import org.knime.base.node.preproc.groupby.common.AggregationOperatorParametersProvider;
import org.knime.base.node.preproc.groupby.common.AggregationOperatorParametersProvider.AggregationMethodRef;
import org.knime.base.node.preproc.groupby.common.HasOperatorParameters;
import org.knime.base.node.preproc.groupby.common.MissingValueOption;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.SubParameters;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;

final class AggregationMethodElement implements NodeParameters {

    AggregationMethodElement() {
        // default constructor
    }

    AggregationMethodElement(final AggregationMethod method) {
        m_aggregationMethod = method.getId();
        m_resultColName = method.getColumnLabel();
    }

    static final class HasColumnOperatorParameters extends HasOperatorParameters {
        @Override
        protected Class<? extends AggregationMethodRef> getAggregationMethodRefClass() {
            return ColumnAggregationMethodRef.class;
        }
    }

    @Widget(title = "Aggregation method",
        description = "Choose an aggregation method to be applied to the selected aggregation columns. "
            + "Only methods compatible with those selected columns are available.")
    @ChoicesProvider(AggregationMethodAvailableForAllSelectedColumns.class)
    @ValueReference(ColumnAggregationMethodRef.class)
    @SubParameters(subLayoutRoot = ColumnAggregationOperatorParametersRef.class,
        showSubParametersProvider = HasColumnOperatorParameters.class)
    String m_aggregationMethod;

    interface ColumnAggregationMethodRef extends AggregationMethodRef {
    }

    static final class AggregationMethodAvailableForAllSelectedColumns implements StringChoicesProvider {

        private Supplier<ColumnFilter> m_aggregationColumnsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_aggregationColumnsSupplier = initializer.computeFromValueSupplier(AggregationColumnsRef.class);
        }

        @Override
        public List<StringChoice> computeState(final NodeParametersInput context) {
            final var isAggregationColumn = m_aggregationColumnsSupplier.get().getFilterPredicate();
            final var dataTypes = context.getInTableSpec(0).stream().flatMap(DataTableSpec::stream)
                .filter(isAggregationColumn).map(DataColumnSpec::getType).collect(Collectors.toSet());
            final var allMethods = AggregationMethods.getAvailableMethods();
            return allMethods.stream().filter(method -> dataTypes.stream().allMatch(method::isCompatible))
                .map(method -> new StringChoice(method.getId(), method.getLabel())).collect(Collectors.toList());
        }

    }

    static final class SetFromAggregationMethod implements StateProvider<String> {

        private Supplier<String> m_aggregationMethod;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_aggregationMethod = initializer.computeFromValueSupplier(ColumnAggregationMethodRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            final var methodId = m_aggregationMethod.get();
            if (methodId == null) {
                throw new StateComputationFailureException();
            }
            final var method = AggregationMethods.getMethod4Id(methodId);
            return method.getColumnLabel();
        }

    }

    @Widget(title = "Column name", description = """
            Name of the resulting aggregation column.
            By default, the name suggested by the selected aggregation method is set.
            """)
    @ValueProvider(SetFromAggregationMethod.class)
    String m_resultColName;

    public static final class SupportsMissingValueOptions extends MissingValueOption.SupportsMissingValueOptions {
        @Override
        protected Class<? extends ParameterReference<String>> getMethodReference() {
            return ColumnAggregationMethodRef.class;
        }
    }

    @ValueProvider(SupportsMissingValueOptions.class)
    @ValueReference(SupportsMissingValueOptions.class)
    // transient helper flag to show/hide missing value option
    boolean m_supportsMissingValueOption;

    @Widget(title = "Missing values", description = """
            Missing values are considered during aggregation if the missing
            option set to "Included".
            Some aggregation methods do not support the changing of the missing
            option such as "Mean".
            """)
    @ValueSwitchWidget
    @Effect(type = EffectType.SHOW, predicate = SupportsMissingValueOptions.class)
    MissingValueOption m_includeMissing = MissingValueOption.EXCLUDE;

    static final class ColumnAggregationOperatorParametersRef //
        implements ParameterReference<AggregationOperatorParameters> {
    } //

    @DynamicParameters(value = ColumnAggregationOperatorParametersProvider.class,
        widgetAppearingInNodeDescription = @Widget(title = "Operator settings", description = """
                Additional parameters for the selected aggregation method.
                Most aggregation methods do not have additional parameters.
                """))
    @ValueReference(ColumnAggregationOperatorParametersRef.class)
    @Layout(ColumnAggregationOperatorParametersRef.class)
    AggregationOperatorParameters m_parameters;

    /**
     * Provider that ties the aggregation method and the optional parameters field together.
     */
    static final class ColumnAggregationOperatorParametersProvider extends AggregationOperatorParametersProvider {
        @Override
        protected Class<? extends ParameterReference<AggregationOperatorParameters>> getParameterRefClass() {
            return ColumnAggregationOperatorParametersRef.class;
        }

        @Override
        protected Class<? extends AggregationMethodRef> getMethodParameterRefClass() {
            return ColumnAggregationMethodRef.class;
        }
    }

}