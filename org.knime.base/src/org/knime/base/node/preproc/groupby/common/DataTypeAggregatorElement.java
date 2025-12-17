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
package org.knime.base.node.preproc.groupby.common;

import java.util.Optional;
import java.util.stream.Stream;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationOperatorParameters;
import org.knime.base.data.aggregation.parameters.AggregationSpec;
import org.knime.base.data.aggregation.parameters.DefaultAggregationMethodProvider;
import org.knime.base.data.aggregation.parameters.HasOperatorParameters;
import org.knime.base.data.aggregation.parameters.AggregationFunctionParametersProvider.AggregationMethodRef;
import org.knime.base.data.aggregation.parameters.StateAndChoicesProviders.AggregationChoicesByTypeRef;
import org.knime.base.data.aggregation.parameters.StateAndChoicesProviders.RegisteredTypesChoicesProvider;
import org.knime.base.node.preproc.groupby.common.LegacyDataTypeAggregatorsArrayPersistor.DataTypeAggregatorElementDTO;
import org.knime.base.node.preproc.groupby.common.LegacyDataTypeAggregatorsArrayPersistor.IndexedElement;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.PortObjectSpec;
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

    static final class DataTypesProvider extends RegisteredTypesChoicesProvider {
        @Override
        protected Stream<DataType> getInputTypes(final NodeParametersInput context) {
            return context.getInTableSpec(0).stream() //
                    .flatMap(dts -> dts.stream()) //
                    .map(DataColumnSpec::getType);
        }
    }

    @Widget(title = "Data Type", description = """
            Select the data type to apply the aggregation on.
            You can add the same data type multiple times.
            The list contains all registered data types the system knows about.
            """)
    // Note: the old dialog listed only "standard types" and all types of input table, but this behavior could be
    // annoying if the input table is temporarily missing some columns. The search makes it easy to find the desired
    // type even if the list is large.
    @ChoicesProvider(DataTypesProvider.class)
    @ValueReference(DataTypeSelectedRef.class)
    @PersistArrayElement(LegacyDataTypeAggregatorsArrayPersistor.DataTypePersistor.class)
    DataType m_dataType = StringCell.TYPE;

    static final class DataTypeAggregationRef implements AggregationMethodRef {
    } //

    static final class DataTypeAggregationChoices extends AggregationChoicesByTypeRef<AggregationMethod,
        AggregationMethodsUtility> {

        @Override
        protected Class<? extends ParameterReference<DataType>> getTypeProvider() {
            return DataTypeSelectedRef.class;
        }

        @Override
        protected Optional<AggregationMethodsUtility> getUtility(final PortObjectSpec spec) {
            return Optional.of(AggregationMethodsUtility.getInstance());
        }

    }

    @Widget(title = "Aggregation", description = "The aggregation method to use")
    @SubParameters(subLayoutRoot = DataTypeOperatorParametersRef.class,
        showSubParametersProvider = HasDataTypeOperatorParameters.class)
    @ValueReference(DataTypeAggregationRef.class)
    @ChoicesProvider(DataTypeAggregationChoices.class)
    @ValueProvider(DefaultMethodProvider.class)
    @PersistArrayElement(LegacyDataTypeAggregatorsArrayPersistor.AggregationMethodPersistor.class)
    String m_aggregationMethod;

    static final class NoPersistence
        extends NoPersistenceElementFieldPersistor<Boolean, IndexedElement, DataTypeAggregatorElementDTO> {
        @Override
        protected Boolean getLoadDefault() {
            return false;
        }
    }

    static final class SupportsMissingValueOptions extends MissingValueOption.SupportsMissingValueOptions {
        @Override
        protected Class<? extends ParameterReference<String>> getMethodReference() {
            return DataTypeAggregationRef.class;
        }

        @Override
        protected Optional<AggregationMethod> lookupMethodById(final String id) {
            return AggregationMethodsUtility.getInstance().lookupFunctionById(id);
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

    /**
     * Default provider if no method already set.
     */
    private static final class DefaultMethodProvider extends DefaultAggregationMethodProvider<AggregationMethod,
            AggregationMethodsUtility> {

        @Override
        protected Class<? extends ParameterReference<DataType>> getTypeProvider() {
            return DataTypeSelectedRef.class;
        }

        @Override
        protected Class<? extends ParameterReference<String>> getMethodSelfProvider() {
            return DataTypeAggregationRef.class;
        }

        @Override
        protected Optional<AggregationMethodsUtility> getUtility(final PortObjectSpec spec) {
            return Optional.of(AggregationMethodsUtility.getInstance());
        }

    }

    static final class HasDataTypeOperatorParameters extends HasOperatorParameters {

        @Override
        protected Class<? extends AggregationMethodRef> getAggregationMethodRefClass() {
            return DataTypeAggregationRef.class;
        }

        @Override
        protected Optional<AggregationSpec> lookupFunctionById(final PortObjectSpec spec, final String id) {
            final var util = AggregationMethodsUtility.getInstance();
            return util.lookupFunctionById(id).map(util::mapToSpec);
        }

    }

    static final class DataTypeAggregationOperatorParametersProvider extends AggregationMethodParametersProvider {
        @Override
        protected Class<? extends ParameterReference<AggregationOperatorParameters>> getParameterRefClass() {
            return DataTypeOperatorParametersRef.class;
        }

        @Override
        protected Class<? extends AggregationMethodRef> getMethodParameterRefClass() {
            return DataTypeAggregationRef.class;
        }
    }
}
