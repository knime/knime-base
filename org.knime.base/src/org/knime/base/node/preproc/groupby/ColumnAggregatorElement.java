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
import java.util.function.Supplier;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.node.preproc.groupby.OptionalParameters.AggregationOperatorParameters;
import org.knime.base.node.preproc.groupby.OptionalParameters.NoOperatorParameters;
import org.knime.core.data.DataType;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.SubParameters;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;

class ColumnAggregatorElement implements NodeParameters {
    @Widget(title = "Column", description = "The column to aggregate")
    @ChoicesProvider(AggregationColumnsProvider.class)
    @ValueReference(SelectedColumnRef.class)
    String m_column;

    // not exposed in UI, but we need this for saving aggregation operators to the settings in the legacy persistor
    @ValueProvider(SelectedColumnTypeProvider.class)
    DataType m_dataType;

    @Widget(title = "Aggregation", description = "The aggregation method to use")
    @SubParameters(subLayoutRoot = ColumnAggregationOperatorParametersRef.class,
        showSubParametersProvider = HasColumnOperatorParameters.class)
    @ValueReference(AggregationMethodRef.class)
    @ChoicesProvider(AggregationMethodChoices.class)
    String m_aggregationMethod;

    @Widget(title = "Missing values", description = "")
    @ValueSwitchWidget
    MissingValueOption m_includeMissing = MissingValueOption.EXCLUDE;

    // TODO show new parameters via extension point if defined
    @DynamicParameters(value = ColumnAggregationOperatorParametersProvider.class,
        widgetAppearingInNodeDescription = @Widget(title = "Operator settings", description = "...", advanced = true))
    @ValueReference(ColumnAggregationOperatorParametersRef.class)
    @Layout(ColumnAggregationOperatorParametersRef.class)
    AggregationOperatorParameters m_parameters = new NoOperatorParameters();

    static class AggregationMethodRef implements ParameterReference<String> {
    }

    static class AggregationColumnsProvider extends AllColumnsProvider {
    }

    static final class SelectedColumnRef implements ParameterReference<String> {
    }

    static final class SelectedColumnTypeProvider implements StateProvider<DataType> {

        private Supplier<String> m_columnNameSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_columnNameSupplier = initializer.computeFromValueSupplier(SelectedColumnRef.class);
        }

        @Override
        public DataType computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var dataTableSpec = parametersInput.getInTableSpecs();
            if (dataTableSpec.length <= 0) {
                return null;
            }
            final var dcs = dataTableSpec[0].getColumnSpec(m_columnNameSupplier.get());
            if (dcs == null) {
                return null;
            }
            return dcs.getType();
        }

    }

    static class AggregationMethodChoices implements StringChoicesProvider {

        private Supplier<String> m_column;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_column = initializer.computeFromValueSupplier(SelectedColumnRef.class);
        }

        @Override
        public List<StringChoice> computeState(final NodeParametersInput parametersInput) {
            final var dts = parametersInput.getInTableSpec(0).orElseThrow();
            final var column = dts.getColumnSpec(m_column.get());
            if (column == null) {
                return List.of();
            }
            final var type = column.getType();
            return AggregationMethods.getCompatibleMethods(type, true).stream()
                .map(agg -> new StringChoice(agg.getId(), agg.getLabel())).toList();
        }

    }

    static final class HasColumnOperatorParameters extends HasOperatorParameters {
        @Override
        Class<? extends ParameterReference<String>> getAggregationMethodRefClass() {
            return AggregationMethodRef.class;
        }
    }

    static final class ColumnAggregationOperatorParametersRef
        implements ParameterReference<AggregationOperatorParameters> {
    }

    static final class ColumnAggregationOperatorParametersProvider extends AggregationOperatorParametersProvider {
        @Override
        Class<? extends ParameterReference<AggregationOperatorParameters>> getParameterRefClass() {
            return ColumnAggregationOperatorParametersRef.class;
        }
    }
}