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

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.AggregationOperatorParameters;
import org.knime.base.node.preproc.groupby.common.AggregationOperatorParametersProvider.AggregationMethodRef;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.SubParameters;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.TypedStringChoice;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Manual per-column aggregation parameters.
 *
 * Use this as element type in an array layout. Use the {@link ColumnAggregatorElementModifier} to change the column
 * choices provider. Use the {@link LegacyColumnAggregatorsMigration} and the {@link LegacyColumnAggregatorsPersistor}
 * also use a {@link ArrayWidget#elementDefaultValueProvider()} sutiable for these column choices.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"restriction", "javadoc"})
public final class ColumnAggregatorElement implements NodeParameters {

    static final class SelectedColumnRef implements ParameterReference<String>, Modification.Reference {
    } //

    @Widget(title = "Column", description = "The column to aggregate")
    @ValueReference(SelectedColumnRef.class)
    @Modification.WidgetReference(SelectedColumnRef.class)
    String m_column;

    static final class SelectedColumnTypeRef implements ParameterReference<DataType> {
    } //

    /**
     * Not exposed in UI, but we need this for saving aggregation operators to the settings in the legacy persistor
     */
    @ValueProvider(SelectedColumnTypeProvider.class)
    @ValueReference(SelectedColumnTypeRef.class)
    DataType m_dataType;

    static class ColumnAggregationMethodRef implements AggregationMethodRef {
    } //

    @Widget(title = "Aggregation", description = "The aggregation method to use")
    @SubParameters(subLayoutRoot = ColumnAggregationOperatorParametersRef.class,
        showSubParametersProvider = HasColumnOperatorParameters.class)
    @ValueReference(ColumnAggregationMethodRef.class)
    @ChoicesProvider(AggregationMethodChoices.class)
    @ValueProvider(DefaultMethodProvider.class)
    String m_aggregationMethod;

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

    ColumnAggregatorElement() {
        // needed by framework
    }

    ColumnAggregatorElement(final NodeParametersInput input) {
        // auto-select first column if available (non-empty table connected)
        input.getInTableSpec(0).stream() //
            .flatMap(DataTableSpec::stream) //
            .findFirst().map(DataColumnSpec::getName).ifPresent(col -> m_column = col);
    }

    ColumnAggregatorElement(final String initialColumn) {
        m_column = initialColumn;
    }

    /* ===== Providers ===== */

    static final class OptionalSelectedColumnTypeProvider implements StateProvider<Optional<DataType>> {

        private Supplier<String> m_columnNameSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_columnNameSupplier = initializer.computeFromValueSupplier(SelectedColumnRef.class);
        }

        @Override
        public Optional<DataType> computeState(final NodeParametersInput input)
            throws StateComputationFailureException {
            final var selectedName = m_columnNameSupplier.get();
            return input.getInTableSpec(0) //
                .map(dts -> dts.getColumnSpec(selectedName)) // handles `null` selectedName
                .map(DataColumnSpec::getType);
        }

    }

    /**
     * Obtains the data type of the selected column.
     */
    static final class SelectedColumnTypeProvider implements StateProvider<DataType> {

        private Supplier<Optional<DataType>> m_optionalColumnTypeProvider;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_optionalColumnTypeProvider =
                initializer.computeFromProvidedState(OptionalSelectedColumnTypeProvider.class);
        }

        @Override
        public DataType computeState(final NodeParametersInput input) throws StateComputationFailureException {
            return m_optionalColumnTypeProvider.get().orElseThrow(StateComputationFailureException::new);
        }

    }

    /**
     * Obtains the list of aggregation methods compatible with the selected column's data type.
     */
    static class AggregationMethodChoices implements StringChoicesProvider {

        private Supplier<Optional<DataType>> m_type;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_type = initializer.computeFromProvidedState(OptionalSelectedColumnTypeProvider.class);
        }

        @Override
        public List<StringChoice> computeState(final NodeParametersInput parametersInput) {
            final var type = m_type.get();
            if (type.isEmpty()) {
                return List.of();
            }
            return AggregationMethods.getCompatibleMethods(type.get(), true).stream() //
                .map(agg -> new StringChoice(agg.getId(), agg.getLabel())) //
                .toList();
        }

    }

    /**
     * Default provider if no method already set.
     */
    static final class DefaultMethodProvider extends DefaultAggregationMethodProvider {

        @Override
        protected Class<? extends ParameterReference<DataType>> getTypeProvider() {
            return SelectedColumnTypeRef.class;
        }

        @Override
        protected Class<? extends ParameterReference<String>> getMethodSelfProvider() {
            return ColumnAggregationMethodRef.class;
        }

    }

    static final class HasColumnOperatorParameters extends HasOperatorParameters {
        @Override
        protected Class<? extends AggregationMethodRef> getAggregationMethodRefClass() {
            return ColumnAggregationMethodRef.class;
        }
    }

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

    /**
     * To be attached to the array layout of the aggregation columns to provide a default element from the available
     * choices.
     */
    public abstract static class DefaultColumnAggregatorElementProvider
        implements StateProvider<ColumnAggregatorElement> {

        private Class<? extends ColumnChoicesProvider> m_aggregationColumnChoicesProviderClass;

        /**
         * Constructor.
         *
         * @param aggregationColumnChoicesProviderClass the same class that is used for the column choices one the
         *            {@link ColumnAggregatorElement#m_column} field (note that this can be changed via a the
         *            {@link ColumnAggregatorElementModifier}.
         */
        protected DefaultColumnAggregatorElementProvider(
            final Class<? extends ColumnChoicesProvider> aggregationColumnChoicesProviderClass) {
            m_aggregationColumnChoicesProviderClass = aggregationColumnChoicesProviderClass;
        }

        private Supplier<List<TypedStringChoice>> m_aggregationColumnChoicesSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_aggregationColumnChoicesSupplier =
                initializer.computeFromProvidedState(m_aggregationColumnChoicesProviderClass);
        }

        @Override
        public ColumnAggregatorElement computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var choices = m_aggregationColumnChoicesSupplier.get();
            if (choices.isEmpty()) {
                return new ColumnAggregatorElement();
            }
            return new ColumnAggregatorElement(choices.get(0).id());
        }

    }

    /**
     * Use this modifier on the column aggregator element field to change the column choices provider used for the
     * {@link ColumnAggregatorElement#m_column} field.
     *
     * @author Paul Bärnreuther
     */
    public abstract static class ColumnAggregatorElementModifier implements Modification.Modifier {

        private Class<? extends ColumnChoicesProvider> m_aggregationColumnChoicesProviderClass;

        /**
         * Constructor.
         *
         * @param aggregationColumnChoicesProviderClass the class of the column choices provider to use.
         */
        protected ColumnAggregatorElementModifier(
            final Class<? extends ColumnChoicesProvider> aggregationColumnChoicesProviderClass) {
            m_aggregationColumnChoicesProviderClass = aggregationColumnChoicesProviderClass;
        }

        @Override
        public void modify(final WidgetGroupModifier group) {
            group.find(SelectedColumnRef.class).addAnnotation(ChoicesProvider.class)
                .withValue(m_aggregationColumnChoicesProviderClass).modify();

        }

    }
}
