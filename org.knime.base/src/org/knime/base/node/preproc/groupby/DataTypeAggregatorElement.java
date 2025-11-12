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

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.AggregationOperatorParameters;
import org.knime.base.node.preproc.groupby.AggregationOperatorParametersProvider.AggregationMethodRef;
import org.knime.base.node.preproc.groupby.LegacyDataTypeAggregatorsArrayPersistor.DataTypeAggregatorElementDTO;
import org.knime.base.node.preproc.groupby.LegacyDataTypeAggregatorsArrayPersistor.IndexedElement;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArrayElement;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.SubParameters;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.DataTypeChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Aggregation operators based on data types.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class DataTypeAggregatorElement implements NodeParameters {

    static final class DataTypeSelectedRef implements ParameterReference<DataType> {
    } //

    @Widget(title = "Data Type", description = """
            Select the data type to apply the aggregation on.
            You can add the same data type multiple times.
            The list contains all registered data types the system knows about.
            """)
    // Note: the old dialog listed only "standard types" and all types of input table, but this behavior could be
    // annoying if the input table is temporarily missing some columns. The search makes it easy to find the desired
    // type even if the list is large.
    @ChoicesProvider(RegisteredTypesChoicesProvider.class)
    @ValueReference(DataTypeSelectedRef.class)
    @PersistArrayElement(LegacyDataTypeAggregatorsArrayPersistor.DataTypePersistor.class)
    DataType m_dataType = StringCell.TYPE;

    static final class DataTypeAggregationRef implements AggregationMethodRef {
    } //

    @Widget(title = "Aggregation", description = "The aggregation method to use")
    @SubParameters(subLayoutRoot = DataTypeOperatorParametersRef.class,
        showSubParametersProvider = HasDataTypeOperatorParameters.class)
    @ValueReference(DataTypeAggregationRef.class)
    @ChoicesProvider(DataTypeAggregationChoices.class)
    @ValueProvider(DefaultMethodProvider.class)
    @PersistArrayElement(LegacyDataTypeAggregatorsArrayPersistor.AggregationMethodPersistor.class)
    String m_aggregationMethod = "First";

    static final class NoPersistence
        extends NoPersistenceElementFieldPersistor<Boolean, IndexedElement, DataTypeAggregatorElementDTO> {
        @Override
        Boolean getLoadDefault() {
            return false;
        }
    }

    static final class SupportsMissingValueOptions extends MissingValueOption.SupportsMissingValueOptions {
        @Override
        Class<? extends ParameterReference<String>> getMethodReference() {
            return DataTypeAggregationRef.class;
        }
    }

    @ValueProvider(SupportsMissingValueOptions.class)
    @ValueReference(SupportsMissingValueOptions.class)
    @PersistArrayElement(NoPersistence.class)
    // transient helper flag to show/hide missing value option
    boolean m_supportsMissingValueOption;

    @Widget(title = "Missing values", description = """
            Missing values are considered during aggregation if the missing
            option set to "Included".
            Some aggregation methods do not support the changing of the missing
            option such as "Mean".
            """)
    @ValueSwitchWidget
    @PersistArrayElement(LegacyDataTypeAggregatorsArrayPersistor.MissingValueOptionPersistor.class)
    @Effect(type = EffectType.SHOW, predicate = SupportsMissingValueOptions.class)
    MissingValueOption m_includeMissing = MissingValueOption.EXCLUDE;

    static final class DataTypeOperatorParametersRef implements ParameterReference<AggregationOperatorParameters> {
    } //

    @DynamicParameters(value = DataTypeAggregationOperatorParametersProvider.class,
        widgetAppearingInNodeDescription = @Widget(title = "Operator settings", description = """
                Additional parameters for the selected aggregation method.
                Most aggregation methods do not have additional parameters.
                """))
    @ValueReference(DataTypeOperatorParametersRef.class)
    @Layout(DataTypeOperatorParametersRef.class)
    @PersistArrayElement(LegacyDataTypeAggregatorsArrayPersistor.OperatorParametersPersistor.class)
    AggregationOperatorParameters m_parameters;

    /* ===== Providers ===== */

    static final class RegisteredTypesChoicesProvider implements DataTypeChoicesProvider {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
        }

        @Override
        public List<DataType> choices(final NodeParametersInput context) {
            final var registered = new HashSet<>(DataTypeRegistry.getInstance().availableDataTypes());
            registered.add(DataType.getType(DataCell.class)); // plain DataCell for backwards-compatibility
            final var fromInput = context.getInTableSpec(0).stream() //
                .flatMap(dts -> dts.stream()).map(DataColumnSpec::getType) //
                .distinct() //
                .map(t -> {
                    registered.remove(t);
                    return t;
                }).toList();
            return Stream.concat(
                // first distinct types from input table (this also includes Collection types of present columns)
                fromInput.stream(),
                // fill up with all remaining registered types
                registered.stream().sorted(Comparator.comparing(DataType::getName))) //
                .toList();
        }
    }

    static final class DataTypeAggregationChoices implements StringChoicesProvider {

        private Supplier<DataType> m_type;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_type = initializer.computeFromValueSupplier(DataTypeSelectedRef.class);
        }

        @Override
        public List<StringChoice> computeState(final NodeParametersInput parametersInput) {
            return AggregationMethods.getCompatibleMethods(m_type.get(), true).stream()
                .map(agg -> new StringChoice(agg.getId(), agg.getLabel())).toList();
        }

    }

    static final class DefaultMethodProvider extends DefaultAggregationMethodProvider {
        @Override
        Class<? extends ParameterReference<DataType>> getTypeProvider() {
            return DataTypeSelectedRef.class;
        }

        @Override
        Class<? extends ParameterReference<String>> getMethodSelfProvider() {
            return DataTypeAggregationRef.class;
        }
    }

    static final class HasDataTypeOperatorParameters extends HasOperatorParameters {

        @Override
        Class<? extends AggregationMethodRef> getAggregationMethodRefClass() {
            return DataTypeAggregationRef.class;
        }

    }

    static final class DataTypeAggregationOperatorParametersProvider extends AggregationOperatorParametersProvider {
        @Override
        Class<? extends ParameterReference<AggregationOperatorParameters>> getParameterRefClass() {
            return DataTypeOperatorParametersRef.class;
        }

        @Override
        Class<? extends AggregationMethodRef> getMethodParameterRefClass() {
            return DataTypeAggregationRef.class;
        }
    }
}
