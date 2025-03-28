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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.base.data.filter.row.v2.IndexedRowReadPredicate;
import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion.SelectedColumnRef;
import org.knime.base.node.preproc.filter.row3.predicates.PredicateFactories;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * Shared settings class for the Filter and Splitter nodes.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
abstract class AbstractRowFilterNodeSettings implements DefaultNodeSettings {

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

    static class FilterCriterion implements DefaultNodeSettings {

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

        private static class ColumnsWithTypeMapping implements ColumnChoicesStateProvider {

            @Override
            public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
                return context.getDataTableSpec(0) //
                    .map(DataTableSpec::stream) //
                    .orElseGet(Stream::empty) //
                    .toArray(DataColumnSpec[]::new);
            }

        }

        @Widget(title = "Filter column", description =
                """
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
        @ChoicesWidget(showRowKeysColumn = true, showRowNumbersColumn = true,
            choicesProvider = ColumnsWithTypeMapping.class)
        @Layout(Condition.ColumnOperator.Column.class)
        @ValueReference(SelectedColumnRef.class)
        ColumnSelection m_column = SpecialColumns.ROWID.toColumnSelection();

        static final class SelectedColumnRef implements Reference<ColumnSelection> {
        }

        // We explicitly do not "reset" the current operator to one applicable for the current column data type,
        // in order to allow the user to switch between columns without resetting their operator selection.
        @Widget(title = "Operator", description =
                """
                The operator determines whether a value meets the filter criterion. A value matches the criterion if the
                applied operator returns "true" and does not match if it returns "false."
                Not all operators are compatible with every column data type. Only the applicable operators are shown
                for the selected column.
                <br /><br />

                <b>Missing value handling:</b> The "is missing" and "is not missing" operators are available for all
                data types. Most other operators do not evaluate on missing values and will not match if a missing cell
                is encountered. An exception is the "is not equal" operator, which matches missing cells because they
                cannot equal the reference value. To exclude missing cells in this case, use the "is not equal (nor
                missing)" operator. "RowID" and "Row number" special columns are never missing.
                """)
        @Layout(Condition.ColumnOperator.Operator.class)
        @ValueReference(OperatorRef.class)
        @ChoicesWidget(choicesProvider = TypeBasedOperatorChoices.class)
        FilterOperator m_operator = FilterOperator.EQ;

        static class OperatorRef implements Reference<FilterOperator> {
        }

        FilterCriterion() {
            // serialization
            this((DataColumnSpec)null);
        }

        FilterCriterion(final DefaultNodeSettingsContext ctx) {
            // set last supported column as default column, like old Row Filter did
            this(ctx.getDataTableSpec(0).stream().flatMap(DataTableSpec::stream)
                .reduce((f, s) -> s).orElse(null));
        }

        FilterCriterion(final DataColumnSpec colSpec) {
            if (colSpec == null) {
                m_column = SpecialColumns.ROWID.toColumnSelection();
                // we don't know how RowIDs look in general, since they can be user-defined, hence we just put
                // a placeholder here that is not null
                m_predicateValues = DynamicValuesInput.forRowID();
                return;
            }
            m_column = new ColumnSelection(colSpec);
            if (DynamicValuesInput.supportsDataType(colSpec.getType())) {
                m_predicateValues =
                    DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(colSpec.getType());
            } else {
                m_predicateValues = DynamicValuesInput.emptySingle();
            }
        }

        void validate(final DataTableSpec spec) throws InvalidSettingsException {
            // check table slicing (filter on numeric row number values)
            if (isFilterOnRowNumbers() && RowNumberFilterSpec.supportsOperator(m_operator)) {
                RowNumberFilterSpec.toFilterSpec(this);
                return;
            }

            // validate using filter on row read (i.e. values)
            toPredicate(spec);
        }

        IndexedRowReadPredicate toPredicate(final DataTableSpec spec) throws InvalidSettingsException {
            // Special case for RowID, which is not a DataValue
            if (isFilterOnRowKeys()) {
                return rowKeyPredicate(m_predicateValues);
            }

            // case where row numbers are treated as data, e.g. WILDCARD/REGEX matching
            if (isFilterOnRowNumbers()) {
                return rowNumberPredicate(m_predicateValues);
            }

            final var column = m_column.getSelected();
            final var columnIndex = spec.findColumnIndex(column);
            if (columnIndex < 0) {
                throw new InvalidSettingsException("Column \"%s\" could not be found in input table".formatted(column));
            }

            return m_operator.translateToPredicate(m_predicateValues, columnIndex,
                spec.getColumnSpec(columnIndex).getType());
        }

        private IndexedRowReadPredicate rowKeyPredicate(final DynamicValuesInput predicateValues)
            throws InvalidSettingsException {
            return PredicateFactories //
                .getRowKeyPredicateFactory(m_operator) //
                .orElseThrow(() -> new InvalidSettingsException( //
                    "Unsupported operator \"%s\" for RowID comparison".formatted(m_operator.label()))) //
                .createPredicate(OptionalInt.empty(), predicateValues);
        }

        private IndexedRowReadPredicate rowNumberPredicate(final DynamicValuesInput predicateValues)
            throws InvalidSettingsException {
            return PredicateFactories //
                .getRowNumberPredicateFactory(m_operator) //
                .orElseThrow(() -> new InvalidSettingsException( //
                    "Unsupported operator \"%s\" for row number comparison".formatted(m_operator.label()))) //
                .createPredicate(OptionalInt.empty(), predicateValues);
        }

        boolean isFilterOnRowKeys() {
            return isRowIDSelected(m_column.getSelected());
        }

        boolean isFilterOnRowNumbers() {
            return isRowNumberSelected(m_column.getSelected());
        }

        @Widget(title = "Filter value", description = """
                The value for the filter criterion.
                <br/><br />

                <i>Note:</i> Currently, comparison values for non-numeric and non-string data types, e.g.
                date&amp;time-based types, must be entered as its string representation like in the <a href="
                """ + ExternalLinks.HUB_TABLE_CREATOR + """
                "><i>Table Creator</i></a> node.
                <br/>

                The format for date&amp;time-based values is "ISO-8601 extended". For example, a "Local Date" must be
                entered in the format "2006-07-28". More information can be obtained from the ISO patterns in the
                "Predefined Formatters" table of the <a href="
                """ + ExternalLinks.ISO_DATETIME_PATTERNS + """
                ">Java SE 17 documentation</a>.
                        """)
        @Layout(Condition.ValueInput.class)
        @ValueProvider(TypeAndOperatorBasedInput.class)
        @ValueReference(DynamicValuesInputRef.class)
        DynamicValuesInput m_predicateValues = DynamicValuesInput.emptySingle();

        static class DynamicValuesInputRef implements Reference<DynamicValuesInput> {

        }

        static class TypeAndOperatorBasedInput implements StateProvider<DynamicValuesInput> {

            private Supplier<ColumnSelection> m_selectedColumn;

            private Supplier<DynamicValuesInput> m_currentValue;

            private Supplier<FilterOperator> m_currentOperator;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_selectedColumn = initializer.computeFromValueSupplier(SelectedColumnRef.class);
                m_currentOperator = initializer.computeFromValueSupplier(OperatorRef.class);
                /**
                 * Necessary, since the TypeBasedOperatorChoice does not have OperatorRef as trigger, only as a
                 * dependency
                 */
                initializer.computeOnValueChange(OperatorRef.class);
                m_currentValue = initializer.getValueSupplier(DynamicValuesInputRef.class);
            }

            @Override
            public DynamicValuesInput computeState(final DefaultNodeSettingsContext context) {
                final var inputSpec = context.getDataTableSpec(0);
                // spec empty, e.g. when
                // - nothing connected
                // - consuming a component input that does not have executed predecessors
                if (inputSpec.isEmpty()) {
                    // show any existing value
                    return m_currentValue.get();
                }
                if (!m_currentOperator.get().isBinary()) {
                    // we don't need an input field
                    return DynamicValuesInput.emptySingle();
                }

                final var selected = m_selectedColumn.get().getSelected();
                final var dts = inputSpec.get();
                final var columnSpec = dts.getColumnSpec(selected);
                final var operatorRequiredType = m_currentOperator.get().getRequiredInputType();
                if (columnSpec == null) {
                    // column went missing or we selected a "special column"
                    if (isRowIDSelected(selected)) {
                        return keepCurrentValueIfPossible(DynamicValuesInput.forRowID());
                    }
                    if (isRowNumberSelected(selected)) {
                        return keepCurrentValueIfPossible(
                            DynamicValuesInput.forRowNumber(operatorRequiredType.orElse(LongCell.TYPE)));
                    }
                    // we don't know the column type, but we still have the (user-supplied) comparison value,
                    // which we don't want to clear
                    return m_currentValue.get();
                }
                // provide an input field for the given type, if we can typemap it, or fall back to the column type
                // if the operator does not require a specific type
                final var columnType = columnSpec.getType();
                final var type = operatorRequiredType.orElse(columnType);
                if (DynamicValuesInput.supportsDataType(type)) {
                    final var defaultValue = DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(type);
                    return keepCurrentValueIfPossible(defaultValue);
                }
                // cannot provide an input field
                return DynamicValuesInput.emptySingle();
            }

            private DynamicValuesInput keepCurrentValueIfPossible(final DynamicValuesInput newValue) {
                final var currentValue = m_currentValue.get();
                // types match exactly
                if (currentValue.isConfigurableFrom(newValue)) {
                    return currentValue;
                }
                // try to convert via String
                return currentValue.convertToType(newValue).orElse(newValue);
            }
        }
    }

    interface PredicatesRef extends Reference<FilterCriterion[]> {
    }

    static final class HasMultipleFilterConditions implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getArray(PredicatesRef.class).hasMultipleItems();
        }
    }

    @Widget(title = "Filter criteria", description = "The list of criteria that should be filtered on.")
    @ArrayWidget(elementTitle = "Criterion", showSortButtons = true, addButtonText = "Add criterion",
        elementDefaultValueProvider = DefaultFitlerCriterionProvider.class)
    @Layout(DialogSections.Filter.Conditions.class)
    @ValueReference(PredicatesRef.class)
    FilterCriterion[] m_predicates;

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
     * Mode to determine which set of rows is output at the first output port (and second in case of a splitter).
     */
    enum FilterMode {
            /**
             * Include matching rows at the first port.
             */
            @Label("Output matching rows")
            MATCHING, //
            /**
             * Exclude matching rows from the first port.
             */
            @Label("Output non-matching rows")
            NON_MATCHING
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

    // constructor needed for de-/serialisation

    AbstractRowFilterNodeSettings() {
        this(null);
    }

    // auto-configuration
    AbstractRowFilterNodeSettings(@SuppressWarnings("unused") final DefaultNodeSettingsContext ctx) { // NOSONAR
        // we don't add a filter criterion automatically in order to avoid setting a default value without
        // the user noticing (and we need to set some default value in the filter criterion, s.t. flow variables work
        // correctly)
        m_predicates = new FilterCriterion[0];
    }

    void validate(final DataTableSpec spec) throws InvalidSettingsException {
        for (final var p : m_predicates) {
            p.validate(spec);
        }
    }

    abstract boolean isSecondOutputActive();

    // UPDATE HANDLER

    /**
     * Compute possible enum values for filter operator based on the selected column.
     */
    static class TypeBasedOperatorsProvider implements StateProvider<List<FilterOperator>> {

        private Supplier<ColumnSelection> m_columnSelection;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_columnSelection = initializer.computeFromValueSupplier(SelectedColumnRef.class);
        }

        @Override
        public List<FilterOperator> computeState(final DefaultNodeSettingsContext context)
            throws WidgetHandlerException {
            final var column = m_columnSelection.get().getSelected();
            return getFilterOperators(context, column);
        }

        private static List<FilterOperator> getFilterOperators(final DefaultNodeSettingsContext context,
            final String column) {
            if (column == null) {
                return List.of();
            }
            final var specialColumn =
                Arrays.stream(SpecialColumns.values()).filter(t -> t.getId().equals(column)).findFirst().orElse(null);
            if (SpecialColumns.NONE == specialColumn) {
                return List.of();
            }
            final var dataType = getColumnType(specialColumn,
                () -> context.getDataTableSpec(0).map(dts -> dts.getColumnSpec(column)).map(DataColumnSpec::getType));
            if (dataType.isEmpty()) {
                // we don't know the column, but we know that columns always can contain missing cells
                return List.of(FilterOperator.IS_MISSING, FilterOperator.IS_NOT_MISSING);
            }
            // filter on top-level type
            return Arrays.stream(FilterOperator.values()) //
                .filter(op -> !op.isHidden(specialColumn, dataType.get())) //
                .toList();
        }

        private static Optional<DataType> getColumnType(final SpecialColumns optionalSpecialColumn,
            final Supplier<Optional<DataType>> columnDataTypeSupplier) {
            if (optionalSpecialColumn == null) {
                return columnDataTypeSupplier.get();
            }
            return Optional.ofNullable(switch (optionalSpecialColumn) {
                case ROWID -> StringCell.TYPE;
                case ROW_NUMBERS -> LongCell.TYPE;
                case NONE -> throw new IllegalArgumentException(
                    "Unsupported special column type \"%s\"".formatted(optionalSpecialColumn.name()));
            });
        }
    }

    static class DefaultFitlerCriterionProvider implements StateProvider<FilterCriterion> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public FilterCriterion computeState(final DefaultNodeSettingsContext context) {
            final var filterCriterion = new FilterCriterion(context);
            final var validOperators =
                TypeBasedOperatorsProvider.getFilterOperators(context, filterCriterion.m_column.getSelected());
            if (!validOperators.contains(FilterOperator.EQ)) {
                filterCriterion.m_operator = validOperators.get(0);
            }
            return filterCriterion;
        }
    }

    /**
     * Compute choices for the filter operator based on the selected column and compare mode
     */
    static class TypeBasedOperatorChoices implements StringChoicesStateProvider {

        private Supplier<List<FilterOperator>> m_typeBasedOperators;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_typeBasedOperators = initializer.computeFromProvidedState(TypeBasedOperatorsProvider.class);
        }

        @Override
        public IdAndText[] computeState(final DefaultNodeSettingsContext context) throws WidgetHandlerException {
            return m_typeBasedOperators.get().stream().map(op -> new IdAndText(op.name(), op.label())) //
                .toArray(IdAndText[]::new);
        }
    }

    // UTILITIES

    static boolean isRowIDSelected(final String selected) {
        return SpecialColumns.ROWID.getId().equals(selected);
    }

    static boolean isRowNumberSelected(final String selected) {
        return SpecialColumns.ROW_NUMBERS.getId().equals(selected);
    }

    static boolean hasLastNFilter(final List<FilterCriterion> criteria) {
        return criteria.stream().anyMatch(c -> c.m_operator == FilterOperator.LAST_N_ROWS);
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
        final var optRowNumbers = mergeRowNumberPredicates(isAnd, mapToPredicates(rowNumberCriteria, tableSize));
        final var data = mergeValuePredicates(isAnd, mapToPredicates(dataCriteria, spec)).orElseThrow(
            () -> new IllegalStateException("Row number predicate without data predicate, should have used slicing"));
        if (optRowNumbers.isEmpty()) {
            return data;
        }
        final var rowNumbers = optRowNumbers.get();
        return isAnd ? (index, read) -> rowNumbers.test(index, read) && data.test(index, read) // NOSONAR this is not too hard to read
            : (index, read) -> rowNumbers.test(index, read) || data.test(index, read); // NOSONAR see above
    }

    private static List<IndexedRowReadPredicate> mapToPredicates(final List<FilterCriterion> criteria,
        final DataTableSpec spec) throws InvalidSettingsException {
        final var predicates = new ArrayList<IndexedRowReadPredicate>();
        for (final var filterCriterion : criteria) {
            predicates.add(filterCriterion.toPredicate(spec));
        }
        return predicates;
    }

    private static List<IndexedRowReadPredicate> mapToPredicates(final List<FilterCriterion> criteria,
        final long optionalTableSize) throws InvalidSettingsException {
        final var predicates = new ArrayList<IndexedRowReadPredicate>();
        for (final var filterCriterion : criteria) {
            final var filterSpec = RowNumberFilterSpec.toFilterSpec(filterCriterion);
            final var offsetFilter = filterSpec.toOffsetFilter(optionalTableSize);
            predicates.add(offsetFilter.asPredicate());
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
