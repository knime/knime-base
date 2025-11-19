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
 *   20 Mar 2024 (jasper): created
 */
package org.knime.base.node.preproc.filter.row3;

import static org.knime.base.node.preproc.filter.row3.RowIdentifiers.ROW_ID;
import static org.knime.base.node.preproc.filter.row3.RowIdentifiers.ROW_NUMBER;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.base.data.filter.row.v2.IndexedRowReadPredicate;
import org.knime.base.node.preproc.filter.row3.operators.FilterOperatorsUtil;
import org.knime.base.node.preproc.filter.row3.operators.defaults.StringWithCaseParameters;
import org.knime.base.node.preproc.filter.row3.operators.legacy.LegacyFilterParameters;
import org.knime.base.node.preproc.filter.row3.operators.legacy.LegacyFilterParametersMigration;
import org.knime.base.node.preproc.filter.row3.operators.rowkey.RowKeyFilterOperator;
import org.knime.base.node.preproc.filter.row3.operators.rownumber.RowNumberFilterOperator;
import org.knime.base.node.preproc.filter.row3.operators.rownumber.RowNumberFilterSpec;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DefaultClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicParametersProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperatorDefinition;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.EqualsOperator;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelectionToStringOrEnumMigration;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;

/**
 * Shared settings class for the Filter and Splitter nodes.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
abstract class AbstractRowFilterNodeSettings implements NodeParameters {

    AbstractRowFilterNodeSettings() {
        m_predicates = new FilterCriterion[]{new FilterCriterion()};
    }

    AbstractRowFilterNodeSettings(final NodeParametersInput context) {
        m_predicates = new FilterCriterion[]{new FilterCriterion(context)};
    }

    enum Criteria {
            @Label("All criteria")
            AND, //
            @Label("Any criterion")
            OR;

        boolean isAnd() {
            return this == AND;
        }
    }

    @Widget(title = "Match row if matched by", description = """
            Match the row if all or any criteria match:
            <ul>
                <li><b>All criteria</b>: a row is matched if <i>all</i> of the criteria match
                (intersection of matches)</li>
                <li><b>Any criterion</b>: a row is matched if <i>at least one</i> of the
                criteria matches (union of matches)</li>
            </ul>
            """)
    @Effect(predicate = HasMultipleFilterConditions.class, type = EffectType.SHOW)
    @ValueSwitchWidget
    @Layout(DialogSections.Filter.AllAny.class)
    Criteria m_matchCriteria = Criteria.AND;

    static class FilterCriterion implements NodeParameters {

        interface Condition {
            @HorizontalLayout
            interface ColumnOperator {
                interface Column {
                }

                interface Operator {
                }
            }

            interface Modifier {
            }

            interface ValueInput {
            }
        }

        @Widget(title = "Filter column", description = """
                The column on which to apply the filter.
                <br />

                The special column "RowID" represents the RowID of the input and is treated as a String column that
                is never missing. The special column "Row number" targets the 1-based row number of the input, is
                treated as a Long column and is never missing. Both special columns are always available, regardless of
                the input table spec or data.
                <br /><br />

                Columns containing data types that are non-native, i.e. contain cells of heterogeneous data types, or
                do not offer a conversion from and to a string representation are not supported and are filtered out
                from the available options.
                <br />

                Collection columns are also not supported by the node.
                """)
        @ChoicesProvider(AllColumnsProvider.class)
        @Layout(Condition.ColumnOperator.Column.class)
        @ValueReference(SelectedColumnRef.class)
        @Migration(FromColumnSelectionMigration.class)
        @Persist(configKey = "columnV2")
        StringOrEnum<RowIdentifiers> m_column = new StringOrEnum<>(ROW_ID);

        static final class FromColumnSelectionMigration extends ColumnSelectionToStringOrEnumMigration<RowIdentifiers> {

            FromColumnSelectionMigration() {
                super("column");
            }

            @Override
            public Optional<RowIdentifiers> loadEnumFromLegacyString(final String legacyString) {
                if (LEGACY_ROW_KEYS_IDENTIFIER.equals(legacyString)) {
                    return Optional.of(ROW_ID);
                }
                if (LEGACY_ROW_NUMBERS_IDENTIFIER.equals(legacyString)) {
                    return Optional.of(ROW_NUMBER);
                }
                return Optional.empty();
            }
        }

        static final class SelectedColumnRef implements ParameterReference<StringOrEnum<RowIdentifiers>> {
        }

        /**
         * The type of the currently selected column when in a dialog, otherwise the type of the selected column the
         * last time the dialog was applied. null if no column is selected (i.e. ROW_ID or ROW_NUMBER is selected) or
         * the selected column is missing and the dialog is opened.
         */
        @ValueProvider(DataTypeProvider.class)
        @Migrate(loadDefaultIfAbsent = true)
        @ValueReference(ColumnDataTypeRef.class) // Only used as a fallback in case of missing columns
        DataType m_columnType;

        static final class ColumnDataTypeRef implements ParameterReference<DataType> {
        }

        static final class DataTypeProvider implements StateProvider<DataType> {

            private Supplier<StringOrEnum<RowIdentifiers>> m_selectedColumn;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeBeforeOpenDialog();
                m_selectedColumn = initializer.computeFromValueSupplier(SelectedColumnRef.class);
            }

            @Override
            public DataType computeState(final NodeParametersInput context) throws StateComputationFailureException {
                final var selectedColumn = m_selectedColumn.get();
                return columnToDataType(context, selectedColumn);
            }

            static DataType columnToDataType(final NodeParametersInput context,
                final StringOrEnum<RowIdentifiers> selectedColumn) throws StateComputationFailureException {
                if (selectedColumn.getEnumChoice().isPresent()) {
                    return null;
                }
                if (context == null) {
                    throw new StateComputationFailureException();
                }
                final var stringChoice = selectedColumn.getStringChoice();
                return context.getInTableSpec(0).flatMap(s -> Optional.ofNullable(s.getColumnSpec(stringChoice)))
                    .map(DataColumnSpec::getType).orElseThrow(() -> new StateComputationFailureException());
            }
        }

        static final class OperatorsProvider implements StringChoicesProvider {

            private Supplier<StringOrEnum<RowIdentifiers>> m_selectedColumn;

            private Supplier<DataType> m_previousDataType;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeBeforeOpenDialog();
                m_selectedColumn = initializer.computeFromValueSupplier(SelectedColumnRef.class);
                m_previousDataType = initializer.getValueSupplier(ColumnDataTypeRef.class);
            }

            @Override
            public List<StringChoice> computeState(final NodeParametersInput context) {
                final var selectedColumn = m_selectedColumn.get();
                return getOperators(selectedColumn, context, m_previousDataType)
                    .map(op -> new StringChoice(op.getId(), op.getLabel())).toList();
            }

            static Stream<FilterOperatorDefinition<?>> getOperators(final StringOrEnum<RowIdentifiers> selectedColumn,
                final NodeParametersInput context, final Supplier<DataType> previousDataType) {
                return getOperatorsWithFallback(selectedColumn, context, () -> {
                    final var dt = previousDataType.get();
                    if (dt != null) {
                        return getOperators(dt);
                    }
                    return Stream.of();
                });
            }

            static Stream<FilterOperatorDefinition<?>> getOperators(final StringOrEnum<RowIdentifiers> selectedColumn,
                final NodeParametersInput context) {
                return getOperatorsWithFallback(selectedColumn, context, () -> Stream.of());
            }

            private static Stream<FilterOperatorDefinition<?>> getOperatorsWithFallback(
                final StringOrEnum<RowIdentifiers> selectedColumn, final NodeParametersInput context,
                final Supplier<Stream<FilterOperatorDefinition<?>>> fallback) {
                try {
                    return getOperatorsOrThrow(selectedColumn, context);
                } catch (final StateComputationFailureException e) {
                    return fallback.get(); // NOSONAR
                }
            }

            private static Stream<FilterOperatorDefinition<?>> getOperatorsOrThrow(
                final StringOrEnum<RowIdentifiers> selectedColumn, final NodeParametersInput context)
                throws StateComputationFailureException {
                final var rowIdentifierChoice = selectedColumn.getEnumChoice();

                if (rowIdentifierChoice.isPresent()) {
                    return switch (rowIdentifierChoice.get()) {
                        case ROW_ID -> FilterOperatorsUtil.getRowKeyOperators().stream()
                            .map(FilterOperatorDefinition.class::cast);
                        case ROW_NUMBER -> FilterOperatorsUtil.getRowNumberOperators().stream()
                            .map(FilterOperatorDefinition.class::cast);
                    };
                }

                final var dataType = DataTypeProvider.columnToDataType(context, selectedColumn);
                if (dataType == null) {
                    return Stream.of();
                }
                return getOperators(dataType);
            }

            static Stream<FilterOperatorDefinition<?>> getOperators(final DataType dataType) {
                return FilterOperatorsUtil.getOperators(dataType).stream().map(FilterOperatorDefinition.class::cast);
            }

        }

        // We explicitly do not "reset" the current operator to one applicable for the current column data type,
        // in order to allow the user to switch between columns without resetting their operator selection.
        @ChoicesProvider(OperatorsProvider.class)
        @Widget(title = "Operator", description = "") // TODO
        @Layout(Condition.ColumnOperator.Operator.class)
        @ValueReference(OperatorIdRef.class)
        String m_operator = EqualsOperator.ID;

        static class OperatorIdRef implements ParameterReference<String> {
        }

        FilterCriterion() {
            // no-arg constructor for persistence
        }

        // for construction by K-AI
        FilterCriterion(final DataColumnSpec colSpec) {
            m_column = new StringOrEnum<>(colSpec.getName());
            m_columnType = colSpec.getType();
        }

        FilterCriterion(final NodeParametersInput ctx) {
            // set last supported column as default column, like old Row Filter did
            final var lastColumn =
                ctx.getInTableSpec(0).stream().flatMap(DataTableSpec::stream).reduce((f, s) -> s).orElse(null);
            if (lastColumn == null) {
                // we don't know how RowIDs look in general, since they can be user-defined, hence we just put
                // a placeholder here that is not null
                final var defaulRowKeyValue = getFirstRowKey(ctx).map(RowKey::getString).orElse("");
                m_filterValueParameters = new StringWithCaseParameters(defaulRowKeyValue);
                return;
            }
            m_column = new StringOrEnum<>(lastColumn.getName());
            m_columnType = lastColumn.getType();

            final var validOperatorsIds =
                OperatorsProvider.getOperators(m_column, ctx).map(FilterOperatorDefinition::getId).toList();
            if (!validOperatorsIds.contains(EqualsOperator.ID)) {
                m_operator = validOperatorsIds.get(0);
            }
            m_filterValueParameters = FilterValueParametersProvider.createNewParameters(ctx, m_column, m_operator);
        }

        private static Optional<RowKey> getFirstRowKey(final NodeParametersInput ctx) {
            return ctx.getInTable(0).flatMap(t -> {
                try (var it = t.iterator()) {
                    return it.hasNext() ? Optional.of(it.next().getKey()) : Optional.empty();
                }
            });

        }

        @Override
        public void validate() throws InvalidSettingsException { // TODO: Use
            if (m_filterValueParameters != null) {
                m_filterValueParameters.validate();
            }
        }

        void validate(final DataTableSpec spec) throws InvalidSettingsException {

            // check table slicing (filter on numeric row number values)
            final var isSlicingOperator = m_column.getEnumChoice().map(ROW_NUMBER::equals).orElse(false)
                && FilterOperatorsUtil.findMatchingRowNumberOperator(m_operator)
                    .map(RowNumberFilterOperator::supportsSlicing).orElse(false);
            if (isSlicingOperator) {
                RowNumberFilterSpec.toPartitionFunction(m_operator, m_filterValueParameters);
                return;
            }

            // validate using filter on row read (i.e. values)
            toPredicate(spec, -1);

        }

        IndexedRowReadPredicate toPredicate(final DataTableSpec spec, final long optionalTableSize)
            throws InvalidSettingsException {

            if (m_filterValueParameters instanceof LegacyFilterParameters legacyParams) {
                return legacyParams.toPredicate(m_column, spec, optionalTableSize);
            }

            final var rowIdentifierChoice = m_column.getEnumChoice();
            if (rowIdentifierChoice.isPresent()) {
                return switch (rowIdentifierChoice.get()) {
                    case ROW_ID -> rowKeyPredicate();
                    case ROW_NUMBER -> rowNumberPredicate(optionalTableSize);
                };
            }

            final var column = m_column.getStringChoice();
            final var columnIndex = spec.findColumnIndex(column);
            if (columnIndex < 0) {
                throw new InvalidSettingsException("Column \"%s\" could not be found in input table".formatted(column));
            }

            return columnPredicate(spec, columnIndex);

        }

        private IndexedRowReadPredicate rowKeyPredicate() throws InvalidSettingsException {
            final var operator = FilterOperatorsUtil.findMatchingRowKeyOperator(m_operator)
                .orElseThrow(() -> new InvalidSettingsException("No row key operator found for ID \"%s\""));
            return toRowKeyBasedPredicate(operator);

        }

        private <P extends FilterValueParameters> IndexedRowReadPredicate
            toRowKeyBasedPredicate(final RowKeyFilterOperator<P> rowKeyFilterOperator) throws InvalidSettingsException {
            @SuppressWarnings("unchecked")
            final var predicate = rowKeyFilterOperator.createPredicate((P)m_filterValueParameters);
            return (index, read) -> predicate.test(read.getRowKey());

        }

        private IndexedRowReadPredicate rowNumberPredicate(final long optionalTableSize)
            throws InvalidSettingsException {
            final var operator = FilterOperatorsUtil.findMatchingRowNumberOperator(m_operator)
                .orElseThrow(() -> new InvalidSettingsException("No row number operator found for ID \"%s\""));
            return toRowNumberBasedPredicate(operator, optionalTableSize);

        }

        private <P extends FilterValueParameters> IndexedRowReadPredicate toRowNumberBasedPredicate(
            final RowNumberFilterOperator<P> operator, final long optionalTableSize) throws InvalidSettingsException {
            @SuppressWarnings("unchecked")
            final var predicate = operator.createPredicate((P)m_filterValueParameters, optionalTableSize);
            return (index, read) -> predicate.test(index + 1);
        }

        private IndexedRowReadPredicate columnPredicate(final DataTableSpec spec, final int columnIndex)
            throws InvalidSettingsException {
            final var selectedOperator = FilterOperatorsUtil
                .findMatchingColumnOperator(m_columnType, m_operator, m_filterValueParameters)
                .orElseThrow(() -> new InvalidSettingsException("No operator found for ID \"%s\" with%s.".formatted(
                    m_operator, //
                    m_filterValueParameters == null //
                        ? "out any parameters"
                        : String.format("  parameter class \"%s\"", m_filterValueParameters.getClass().getName()))));
            return toColumnBasedPredicate(selectedOperator, spec, columnIndex);
        }

        private <P extends FilterValueParameters> IndexedRowReadPredicate toColumnBasedPredicate(
            final FilterOperator<P> selectedOperator, final DataTableSpec spec, final int columnIndex)
            throws InvalidSettingsException {
            @SuppressWarnings("unchecked")
            final var predicate = CheckUtils.checkSettingNotNull(
                selectedOperator.createPredicate(spec.getColumnSpec(columnIndex), m_columnType,
                    (P)m_filterValueParameters),
                "Operator \"%s\" returned 'null' predicate", selectedOperator.getLabel());
            final var returnTrueForMissingCells = selectedOperator.returnTrueForMissingCells();
            return (index, read) -> {
                if (read.isMissing(columnIndex)) {
                    return returnTrueForMissingCells;
                }
                final var dataValue = read.getValue(columnIndex);
                return predicate.test(dataValue);
            };
        }

        @Layout(Condition.ValueInput.class)
        @DynamicParameters(value = FilterValueParametersProvider.class,
            widgetAppearingInNodeDescription = @Widget(title = "Filter value",
                description = """
                        The value for the filter criterion.
                        <br/><br />

                        <i>Note:</i> Currently, comparison values for non-numeric and non-string data types, e.g.
                        date&amp;time-based types, must be entered as its string representation like in the <a href="
                        """ + ExternalLinks.HUB_TABLE_CREATOR
                    + """
                            "><i>Table Creator</i></a> node.
                            <br/>

                            The format for date&amp;time-based values is "ISO-8601 extended". For example, a "Local Date" must be
                            entered in the format "2006-07-28". More information can be obtained from the ISO patterns in the
                            "Predefined Formatters" table of the <a href="
                            """
                    + ExternalLinks.ISO_DATETIME_PATTERNS + """
                            ">Java SE 17 documentation</a>.
                                    """))
        @ValueReference(CurrentFilterValueParametersRef.class)
        @Migration(LegacyFilterParametersMigration.class)
        FilterValueParameters m_filterValueParameters;// = new EqualsStringParameters("");

        static final class CurrentFilterValueParametersRef implements ParameterReference<FilterValueParameters> {
        }

        static class FilterValueParametersProvider implements DynamicParametersProvider<FilterValueParameters> {

            private Supplier<String> m_currentOperatorId;

            private Supplier<FilterValueParameters> m_currentValue;

            private Supplier<StringOrEnum<RowIdentifiers>> m_selectedColumn;

            private Supplier<DataType> m_previousDataType;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeBeforeOpenDialog();
                m_currentOperatorId = initializer.computeFromValueSupplier(OperatorIdRef.class);
                m_currentValue = initializer.getValueSupplier(CurrentFilterValueParametersRef.class);
                m_selectedColumn = initializer.computeFromValueSupplier(SelectedColumnRef.class);
                m_previousDataType = initializer.getValueSupplier(ColumnDataTypeRef.class);
            }

            @Override
            public ClassIdStrategy<FilterValueParameters> getClassIdStrategy() {
                return new DefaultClassIdStrategy<>(FilterOperatorsUtil.getAllParameterClasses());
            }

            @Override
            public FilterValueParameters computeParameters(final NodeParametersInput input)
                throws StateComputationFailureException {
                final var selectedColumn = m_selectedColumn.get();
                final var currentOperatorId = m_currentOperatorId.get();
                final var currentValue = m_currentValue.get();
                return toNewParameters(input, selectedColumn, currentOperatorId, currentValue, m_previousDataType);

            }

            static FilterValueParameters createNewParameters(final NodeParametersInput input,
                final StringOrEnum<RowIdentifiers> selectedColumn, final String operatorId) {
                return toNewParameters(input, selectedColumn, operatorId, null, () -> null);
            }

            static FilterValueParameters createNewParameters(final DataType colType, final String operatorId) {
                return toNewParameters(operatorId, null, OperatorsProvider.getOperators(colType));
            }

            static FilterValueParameters toNewParameters(final NodeParametersInput input,
                final StringOrEnum<RowIdentifiers> selectedColumn, final String currentOperatorId,
                final FilterValueParameters currentValue, final Supplier<DataType> previousDataType) {
                final var operators = OperatorsProvider.getOperators(selectedColumn, input, previousDataType);
                return toNewParameters(currentOperatorId, currentValue, operators);
            }

            private static FilterValueParameters toNewParameters(final String currentOperatorId,
                final FilterValueParameters currentValue, final Stream<FilterOperatorDefinition<?>> operators) {
                final var operator = operators.filter(op -> op.getId().equals(currentOperatorId)).findFirst();
                if (operator.isEmpty()) {
                    return null;
                }
                final var targetClass = operator.get().getNodeParametersClass();
                if (targetClass == null) {
                    // No class means no parameters
                    return null;
                }
                if (currentValue != null && targetClass.equals(currentValue.getClass())) {
                    return currentValue;
                }
                try {
                    final var constructor = targetClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    final var newInstance = constructor.newInstance();
                    if (currentValue != null) {
                        newInstance.applyStash(currentValue.stash());
                    }
                    return newInstance;
                } catch (Exception e) {
                    throw new IllegalStateException(
                        "Could not instantiate FilterValueParameters of type " + targetClass, e);
                }
            }

        }

    }

    interface PredicatesRef extends ParameterReference<FilterCriterion[]> {
    }

    static final class HasMultipleFilterConditions implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getArray(PredicatesRef.class).hasMultipleItems();
        }
    }

    @Widget(title = "Filter criteria", description = "The list of criteria that should be filtered on.")
    @ArrayWidget(elementTitle = "Criterion", showSortButtons = true, addButtonText = "Add filter criterion",
        elementDefaultValueProvider = DefaultFilterCriterionProvider.class)
    @Layout(DialogSections.Filter.Conditions.class)
    @ValueReference(PredicatesRef.class)
    FilterCriterion[] m_predicates = new FilterCriterion[0];

    @Widget(title = "Column domains", description = """
            Specify whether to take domains of all input columns as output domains as-is or compute them on the output
            rows.
            <br />

            Depending on the use case, one or the other setting may be preferable:
            <ul>
                <li><em>Retaining</em> input columns can be useful, if the axis limits of a view should be derived from
                domain bounds, and that bounds should stay stable even when the displayed data is filtered.
                </li>
                <li><em>Computing</em> domains can be useful when a selection widget consumes the output and should only
                display actually present options to users.</li>
            </ul>

            If column domains are irrelevant for a particular use case, the &quot;Retain&quot; option should be used
            since it does not incur computation costs.
            <br />

            For more control over individual column domains, you can use the <a href="
            """ + ExternalLinks.HUB_DOMAIN_CALCULATOR + """
                    "><em>Domain Calculator</em></a>, <a href="
            """ + ExternalLinks.HUB_EDIT_NUMERIC_DOMAIN + """
                    "><em>Edit Numeric Domain</em></a>, or <a href="
            """ + ExternalLinks.HUB_EDIT_NOMINAL_DOMAIN + """
                    "><em>Edit Nominal Domain</em></a> nodes.
            """)
    @ValueSwitchWidget()
    @Layout(DialogSections.Output.Domain.class)
    ColumnDomains m_domains = ColumnDomains.RETAIN;

    enum ColumnDomains {
            @Label(value = "Retain",
                description = """
                        Retain input domains on output columns, i.e. the upper and lower bounds or possible values in the table
                        spec are not changed, even if one of the bounds or one value is fully filtered out from the output
                        table. If the input does not contain domain information, so will the output.
                            """)
            RETAIN, @Label(value = "Compute",
                description = """
                        Compute column domains on output columns, i.e. upper and lower bounds and possible values are computed
                        only on the rows output by the node.
                            """)
            COMPUTE;
    }

    /**
     * Get the output mode, i.e. output only matching rows or only non-matching rows in the first output. The second
     * output, if present, will receive the complementary set of rows.
     *
     * @return {@link FilterMode#MATCHING} if only matching rows should be output (in the first output) or
     *         {@link FilterMode#NON_MATCHING} for only non-matching.
     */
    abstract FilterMode outputMode();

    boolean outputMatches() {
        return outputMode() == FilterMode.MATCHING;
    }

    void validate(final DataTableSpec spec) throws InvalidSettingsException {
        for (final var p : m_predicates) {
            p.validate(spec);
        }
    }

    abstract boolean isSecondOutputActive();

    static class DefaultFilterCriterionProvider implements StateProvider<FilterCriterion> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public FilterCriterion computeState(final NodeParametersInput context) {
            final var filterCriterion = new FilterCriterion(context);

            return filterCriterion;
        }
    }

    // UTILITIES

    static boolean hasLastNFilter(final List<FilterCriterion> criteria) {
        return criteria.stream().anyMatch(c -> "LAST_N_ROWS".equals(c.m_operator));
    }

    // SECTIONS

    interface DialogSections {
        @Section(title = "Filter")
        interface Filter {
            interface AllAny {
            }

            interface Conditions {
            }
        }

        @Section(title = "Output")
        @After(Filter.class)
        interface Output {
            interface OutputMode {
            }

            interface Domain {
            }
        }
    }

    static IndexedRowReadPredicate createFilterPredicate(final boolean isAnd,
        final List<FilterCriterion> rowNumberCriteria, final List<FilterCriterion> dataCriteria,
        final DataTableSpec spec, final long tableSize) throws InvalidSettingsException {
        // TODO(performance): use domain bounds to derive whether predicates are always true or always false
        // TODO(performance): propagate ALWAYS_TRUE and ALWAYS_FALSE predicates
        final var optRowNumbers = mergeRowNumberPredicates(isAnd, mapToPredicates(rowNumberCriteria, spec, tableSize));
        final var data = mergeValuePredicates(isAnd, mapToPredicates(dataCriteria, spec, tableSize)).orElseThrow(
            () -> new IllegalStateException("Row number predicate without data predicate, should have used slicing"));
        if (optRowNumbers.isEmpty()) {
            return data;
        }
        final var rowNumbers = optRowNumbers.get();
        return isAnd ? (index, read) -> rowNumbers.test(index, read) && data.test(index, read) // NOSONAR this is not too hard to read
            : (index, read) -> rowNumbers.test(index, read) || data.test(index, read); // NOSONAR see above
    }

    private static List<IndexedRowReadPredicate> mapToPredicates(final List<FilterCriterion> criteria,
        final DataTableSpec spec, final long tableSize) throws InvalidSettingsException {
        final var predicates = new ArrayList<IndexedRowReadPredicate>();
        for (final var filterCriterion : criteria) {
            predicates.add(filterCriterion.toPredicate(spec, tableSize));
        }
        return predicates;
    }

    /* === Private methods operating on "core" classes  === */

    /**
     * Merges the given predicates using AND or OR, possibly short-circuiting.
     *
     * @param isAnd if {@code true}, combine predicates with AND, otherwise with OR
     * @param rowNumberCriteria list of predicates to merge
     * @return merged predicate
     */
    private static Optional<IndexedRowReadPredicate> mergeRowNumberPredicates(final boolean isAnd,
        final List<IndexedRowReadPredicate> rowNumberCriteria) {
        return rowNumberCriteria.stream().reduce((l, r) -> isAnd ? l.and(r) : l.or(r));
    }

    /**
     * Merges the given predicates using AND or OR.
     *
     * @param isAnd if {@code true}, combine predicates with AND, otherwise with OR
     * @param predicates list of predicates to merge
     * @return merged predicate, possibly short-circuited
     */
    private static Optional<IndexedRowReadPredicate> mergeValuePredicates(final boolean isAnd,
        final List<IndexedRowReadPredicate> predicates) {
        return predicates.stream().reduce((l, r) -> merge(isAnd, l, r));
    }

    /**
     * Merges the given predicates, short-circuiting if possible.
     *
     * @param isAnd if {@code true}, combine predicates with AND, otherwise with OR
     * @param l left-hand-side predicate
     * @param r right-hand-side predicate
     * @return combined predicate
     */
    private static final IndexedRowReadPredicate merge(final boolean isAnd, final IndexedRowReadPredicate l,
        final IndexedRowReadPredicate r) {
        return isAnd ? // NOSONAR
        // AND case
        // l AND false -> false
            (r == IndexedRowReadPredicate.FALSE ? IndexedRowReadPredicate.FALSE // NOSONAR
                // l AND true -> l
                : (r == IndexedRowReadPredicate.TRUE ? l // NOSONAR
                    // else simply combine
                    : l.and(r)))
            : // OR case
                           // l OR false -> l
            (r == IndexedRowReadPredicate.FALSE ? l // NOSONAR
                // x OR true -> true
                : (r == IndexedRowReadPredicate.TRUE ? IndexedRowReadPredicate.TRUE // NOSONAR
                    : // else simply combine
                    l.or(r)));
    }

}
