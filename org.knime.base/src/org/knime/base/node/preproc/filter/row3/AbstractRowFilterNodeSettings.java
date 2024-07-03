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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion.SelectedColumnRef;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.HasMultipleItemsCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
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
    @Effect(signals = HasMultipleItemsCondition.class, type = EffectType.SHOW)
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
                    // we disallow non-native types for now
                    .filter(colSpec -> DynamicValuesInput.supportsDataType(colSpec.getType())) //
                    .toArray(DataColumnSpec[]::new);
            }

        }

        @Widget(title = "Filter column", description = "The column on which to apply the filter.")
        @ChoicesWidget(showRowKeysColumn = true, showRowNumbersColumn = true,
            choicesProvider = ColumnsWithTypeMapping.class)
        @Layout(Condition.ColumnOperator.Column.class)
        @ValueReference(SelectedColumnRef.class)
        ColumnSelection m_column = SpecialColumns.ROWID.toColumnSelection();

        static final class SelectedColumnRef implements Reference<ColumnSelection> {
        }

        @Widget(title = "Operator", description = "The operator defining the filter criterion.")
        @Layout(Condition.ColumnOperator.Operator.class)
        @ValueReference(OperatorRef.class)
        @ValueProvider(TypeBasedOperatorChoice.class)
        @ChoicesWidget(choicesProvider = TypeBasedOperatorChoices.class)
        FilterOperator m_operator = FilterOperator.EQ;

        static class OperatorRef implements Reference<FilterOperator> {
        }

        /**
         * Provides the first operator from the given choices if the current one is missing.
         */
        static class TypeBasedOperatorChoice implements StateProvider<FilterOperator> {

            private Supplier<List<FilterOperator>> m_typeBasedOperators;

            private Supplier<FilterOperator> m_currentValue;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_currentValue = initializer.getValueSupplier(OperatorRef.class);
                m_typeBasedOperators = initializer.computeFromProvidedState(TypeBasedOperatorsProvider.class);
            }

            @Override
            public FilterOperator computeState(final DefaultNodeSettingsContext context) {
                final var currentValue = m_currentValue.get();
                final var operators = m_typeBasedOperators.get();
                if (operators.contains(currentValue)) {
                    return currentValue;
                }
                return operators.stream().findFirst().orElseThrow();
            }

        }

        FilterCriterion() {
            // serialization
            this((DataColumnSpec)null);
        }

        FilterCriterion(final DefaultNodeSettingsContext ctx) {
            // set last supported column as default column, like old Row Filter did
            this(ctx.getDataTableSpec(0).stream().flatMap(DataTableSpec::stream)
                // don't auto-configure with non-supported columns
                .filter(colSpec -> DynamicValuesInput.supportsDataType(colSpec.getType()))
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
            m_predicateValues = DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(colSpec.getType());
        }

        void validate(final DataTableSpec spec) throws InvalidSettingsException {
            if (isFilterOnRowNumbers()) {
                RowNumberFilter.getAsFilterSpec(this);
                return;
            }

            final var operator = m_operator;
            if (isFilterOnRowKeys()) {
                CheckUtils.checkSetting(operator != FilterOperator.IS_MISSING, "Cannot filter RowID for presence.");
                CheckUtils.checkSetting(operator.isEnabledFor(SpecialColumns.ROWID, StringCell.TYPE),
                    "Filter operator \"%s\" cannot be applied to RowID.", operator.label());
                return;
            }

            final var columnName = m_column.getSelected();
            final var colSpec = spec.getColumnSpec(columnName);
            CheckUtils.checkSettingNotNull(colSpec, "Unknown column \"%s\".", columnName);
            m_predicateValues.validate(colSpec);
        }

        boolean isFilterOnRowKeys() {
            return isRowIDSelected(m_column.getSelected());
        }

        boolean isFilterOnRowNumbers() {
            return isRowNumberSelected(m_column.getSelected());
        }

        @Widget(title = "Filter value", description = """
                The value for the filter criterion.
                <br/>

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
                // Only as dependency, since TypeBasedOperatorChoice is triggered by this already.
                m_selectedColumn = initializer.getValueSupplier(SelectedColumnRef.class);
                m_currentOperator = initializer.computeFromProvidedState(TypeBasedOperatorChoice.class);
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
                return keepCurrentValueIfPossible(createDynamicValue(inputSpec.orElse(null)));
            }

            private DynamicValuesInput createDynamicValue(final DataTableSpec spec) {
                if (!m_currentOperator.get().m_isBinary) {
                    return DynamicValuesInput.emptySingle();
                }
                final var selected = m_selectedColumn.get().getSelected();
                if (spec == null || isRowIDSelected(selected)) {
                    return DynamicValuesInput.forRowID();
                }
                if (isRowNumberSelected(selected)) {
                    return DynamicValuesInput.forRowNumber();
                }
                return DynamicValuesInput
                    .singleValueWithCaseMatchingForStringWithDefault(spec.getColumnSpec(selected).getType());
            }

            private DynamicValuesInput keepCurrentValueIfPossible(final DynamicValuesInput newValue) {
                final var currentValue = m_currentValue.get();
                if (currentValue.isConfigurableFrom(newValue)) {
                    return currentValue;
                }
                return newValue;
            }
        }
    }

    @Widget(title = "Filter criteria", description = "The list of criteria that should be filtered on.")
    @ArrayWidget(elementTitle = "Criterion", showSortButtons = true, addButtonText = "Add criterion")
    @Layout(DialogSections.Filter.Conditions.class)
    @Signal(condition = HasMultipleItemsCondition.class)
    FilterCriterion[] m_predicates;

    @Widget(title = "Column domains", description = """
            Specify whether to take input column domains as output domains or compute them on output columns.
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

            For more control over column domains, you can use the <a href="
            """ + ExternalLinks.HUB_DOMAIN_CALCULATOR + """
                    "><em>Domain Calculator</em></a>, <a href="
            """ + ExternalLinks.HUB_EDIT_NUMERIC_DOMAIN + """
                    "><em>Edit Numeric Domain</em></a>, or <a href="
            """ + ExternalLinks.HUB_EDIT_NOMINAL_DOMAIN + """
                    "><em>Edit Nominal Domain</em></a> nodes.
            """)
    @ValueSwitchWidget()
    @Layout(DialogSections.Output.class)
    ColumnDomains m_domains = ColumnDomains.RETAIN;

    enum ColumnDomains {
        @Label(value = "Retain", description = "Retain input domains on output columns")
        RETAIN,
        @Label(value = "Compute", description = "Compute column domains on output columns")
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
    AbstractRowFilterNodeSettings(final DefaultNodeSettingsContext ctx) {
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
            m_columnSelection = initializer.computeFromValueSupplier(SelectedColumnRef.class);
        }

        @Override
        public List<FilterOperator> computeState(final DefaultNodeSettingsContext context)
            throws WidgetHandlerException {
            final var column = m_columnSelection.get().getSelected();
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
                return List.of(FilterOperator.IS_MISSING);
            }
            // filter on top-level type
            return Arrays.stream(FilterOperator.values()) //
                .filter(op -> op.isEnabledFor(specialColumn, dataType.get())) //
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
        }
    }

}
