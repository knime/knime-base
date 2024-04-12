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
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.base.node.preproc.stringreplacer.CaseMatching;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.And;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Expression;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Not;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.IsColumnOfTypeCondition;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.IsSpecificColumnCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.AllColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
final class RowFilter3NodeSettings implements DefaultNodeSettings {

    @Widget(title = "Filter column", description = "The column on which to apply the filter")
    @ChoicesWidget(showRowKeysColumn = true, showRowNumbersColumn = true, choices = AllColumnChoicesProvider.class)
    @Layout(RowFilter3NodeSettings.DialogSections.Filter.Condition.Column.class)
    // signals for specially supported data types
    @Signal(condition = IsRowNumbersColumnCondition.class)
    @Signal(condition = IsLongColumn.class)
    @Signal(condition = IsDoubleColumn.class)
    @Signal(condition = IsStringColumn.class)
    @ValueReference(SelectedColumnRef.class)
    ColumnSelection m_column = SpecialColumns.ROWID.toColumnSelection();

    static final class SelectedColumnRef implements Reference<ColumnSelection> {
    }

    @Widget(title = "Operator", description = "The operator defining the filter criterion.")
    @Signal(condition = IsUnaryOperator.class)
    @Signal(condition = IsBinaryOperator.class)
    @Signal(condition = IsPatternOperator.class)
    @Layout(RowFilter3NodeSettings.DialogSections.Filter.Condition.Operator.class)
    @ValueReference(TypeBasedOperatorChoices.class)
    @ChoicesWidget(choicesProvider = TypeBasedOperatorChoices.class)
    FilterOperator m_operator = FilterOperator.EQ;

    @Widget(title = "Case matching",
        description = "Whether RowIDs and strings should be matched case-sensitive or case-insensitive.")
    @ValueSwitchWidget
    @Effect(signals = {IsStringColumn.class, IsUnaryOperator.class}, operation = AllButTheLast.class,
        type = EffectType.SHOW)
    @Layout(RowFilter3NodeSettings.DialogSections.Filter.Condition.Operator.class)
    CaseMatching m_caseMatching = CaseMatching.DEFAULT;

    boolean caseSensitive() {
        return m_caseMatching == CaseMatching.CASESENSITIVE;
    }

    @Widget(title = "Value",
        description = "The value for the filter criterion in a format suitable for the selected filter column "
            + "data type.")
    @Layout(RowFilter3NodeSettings.DialogSections.Filter.Condition.ValueInput.class)
    @Effect(signals = IsBinaryOperator.class, type = EffectType.SHOW)
    @ValueProvider(ValueFieldCleaning.class)
    @ValueReference(ValueFieldCleaning.class)
    public String m_value = "";

    @Persist(hidden = true)
    @ValueProvider(DataTypeCellClassNameProvider.class)
    //@ValueReference(DataTypeCellClassNameProvider.class)
    public String m_type = StringCell.TYPE.getCellClass().getName();

    enum FilterMode {
            @Label("Include matches")
            INCLUDE, //
            @Label("Exclude matches")
            EXCLUDE
    }

    @Widget(title = "Filter behavior",
        description = "Determines whether a row that matches the filter criterion is included or excluded. "
            + "Included rows are output in the first output table. If a second output table is configured, "
            + "non-matching rows are output there.",
        hideTitle = true)
    @ValueSwitchWidget
    @Layout(DialogSections.Output.class)
    FilterMode m_outputMode = FilterMode.INCLUDE;

    boolean includeMatches() {
        return m_outputMode == FilterMode.INCLUDE;
    }

    // constructor needed for de-/serialisation
    RowFilter3NodeSettings() {
        this((DataColumnSpec)null);
    }

    // auto-configuration
    RowFilter3NodeSettings(final DefaultNodeSettingsContext ctx) {
        // set last column as default column, like old Row Filter did
        this(ctx.getDataTableSpec(0).stream().flatMap(DataTableSpec::stream).reduce((f, s) -> s).orElse(null));
    }

    RowFilter3NodeSettings(final DataColumnSpec colSpec) {
        if (colSpec == null) {
            m_column = SpecialColumns.ROWID.toColumnSelection();
            return;
        }
        m_column = new ColumnSelection(colSpec);
        m_type = colSpec.getType().getCellClass().getName();
        m_value = "";
    }

    // UPDATE HANDLER

    private static final class DataTypeCellClassNameProvider implements StateProvider<String>, Reference<String> {
        private Supplier<ColumnSelection> m_selectedColumn;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_selectedColumn = initializer.computeFromValueSupplier(SelectedColumnRef.class);
        }

        @Override
        public String computeState(final DefaultNodeSettingsContext context) {
            final var selected = m_selectedColumn.get().getSelected();
            return getTypeNameForSelected(context, selected).orElse(UNKNOWN_TYPE);
        }

        private static final String UNKNOWN_TYPE = "?";

        private static Optional<String> getTypeNameForSelected(final DefaultNodeSettingsContext context,
            final String selected) {
            final Supplier<Optional<DataTableSpec>> dtsSupplier = () -> context.getDataTableSpec(0);
            return RowFilter3NodeModel.getDataTypeNameForColumn(selected, dtsSupplier);
        }
    }

    /**
     * Compute choices for the filter operator based on the selected column and compare mode
     */
    static class TypeBasedOperatorChoices implements StringChoicesStateProvider, Reference<FilterOperator> {

        private static final IdAndText[] EMPTY = new IdAndText[0];

        private Supplier<ColumnSelection> m_columnSelection;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog(); // otherwise it will not load when first opening the dialog!
            m_columnSelection = initializer.computeFromValueSupplier(SelectedColumnRef.class);
        }

        @Override
        public IdAndText[] computeState(final DefaultNodeSettingsContext context) throws WidgetHandlerException {
            final var column = m_columnSelection.get().getSelected();
            if (column == null) {
                return EMPTY;
            }
            final var specialColumn =
                Arrays.stream(SpecialColumns.values()).filter(t -> t.getId().equals(column)).findFirst().orElse(null);
            if (SpecialColumns.NONE == specialColumn) {
                return EMPTY;
            }
            final var dataType = getColumnType(specialColumn,
                () -> context.getDataTableSpec(0).map(dts -> dts.getColumnSpec(column)).map(DataColumnSpec::getType));
            if (dataType.isEmpty()) {
                // we don't know the column, but we know that columns always can contain missing cells
                return new IdAndText[]{
                    new IdAndText(FilterOperator.IS_MISSING.name(), FilterOperator.IS_MISSING.label())};
            }
            // filter on top-level type
            return Arrays.stream(FilterOperator.values()) //
                .filter(op -> op.isEnabledFor(specialColumn, dataType.get())) //
                .map(op -> new IdAndText(op.name(), op.label())) //
                .toArray(IdAndText[]::new);
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
     * A state provider for the value field. Clears the value field if the operator is unary, i.e. does not require that
     * field.
     */
    private static final class ValueFieldCleaning implements StateProvider<String>, Reference<String> {

        private Supplier<String> m_currentValue;

        private Supplier<FilterOperator> m_operator;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnValueChange(SelectedColumnRef.class);
            m_operator = initializer.computeFromValueSupplier(TypeBasedOperatorChoices.class);
            m_currentValue = initializer.getValueSupplier(ValueFieldCleaning.class);
        }

        @Override
        public String computeState(final DefaultNodeSettingsContext context) {
            final var operator = m_operator.get();
            if (operator == null || !operator.m_isBinary) {
                return "";
            }
            return m_currentValue.get();
        }
    }

    // SIGNALS

    static class IsLongColumn extends IsColumnOfTypeCondition {
        @Override
        public Class<LongValue> getDataValueClass() {
            return LongValue.class;
        }
    }

    static class IsDoubleColumn extends IsColumnOfTypeCondition {
        @Override
        public Class<DoubleValue> getDataValueClass() {
            return DoubleValue.class;
        }
    }

    static class IsStringColumn extends IsColumnOfTypeCondition {
        @Override
        public Class<StringValue> getDataValueClass() {
            return StringValue.class;
        }
    }

    /* === OPERATOR ARITY signals */

    static final class IsUnaryOperator extends OneOfEnumCondition<FilterOperator> {
        @Override
        public FilterOperator[] oneOf() {
            return Arrays.stream(FilterOperator.values()).filter(op -> !op.m_isBinary).toArray(FilterOperator[]::new);
        }
    }

    static final class IsBinaryOperator extends OneOfEnumCondition<FilterOperator> {
        @Override
        public FilterOperator[] oneOf() {
            return Arrays.stream(FilterOperator.values()).filter(op -> op.m_isBinary).toArray(FilterOperator[]::new);
        }
    }

    static final class IsPatternOperator extends OneOfEnumCondition<FilterOperator> {
        @Override
        public FilterOperator[] oneOf() {
            return new FilterOperator[]{FilterOperator.REGEX, FilterOperator.WILDCARD};
        }
    }

    static final class IsRowNumbersColumnCondition extends IsSpecificColumnCondition {
        @Override
        public String getColumnName() {
            return SpecialColumns.ROW_NUMBERS.getId();
        }
    }

    // OPERATIONS

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class AllButTheLast extends And {
        public AllButTheLast(final Expression... expressions) {
            super(negateLast(expressions));
        }

        private static Expression[] negateLast(final Expression[] expressions) {
            if (expressions == null || expressions.length == 0) {
                throw new IllegalArgumentException("Operator can only be used on at least one expression");
            }
            final var result = Arrays.copyOf(expressions, expressions.length, Expression[].class);
            result[expressions.length - 1] = new Not(expressions[expressions.length - 1]);
            return result;
        }
    }

    // UTILITIES

    static boolean isFilterOnRowKeys(final RowFilter3NodeSettings settings) {
        return SpecialColumns.ROWID.getId().equals(settings.m_column.getSelected());
    }

    static boolean isFilterOnRowNumbers(final RowFilter3NodeSettings settings) {
        return SpecialColumns.ROW_NUMBERS.getId().equals(settings.m_column.getSelected());
    }

    static boolean isLastNFilter(final RowFilter3NodeSettings settings) {
        return settings.m_operator == FilterOperator.LAST_N_ROWS;
    }

    // SECTIONS

    interface DialogSections {
        @Section(title = "Filter")
        interface Filter {
            interface Condition {
                @HorizontalLayout
                interface Column {
                }

                @HorizontalLayout
                @After(Condition.Column.class)
                interface Operator {
                }

                @HorizontalLayout
                @After(Condition.Operator.class)
                interface ValueInput {
                }
            }
        }

        @Section(title = "Filter behavior")
        @After(DialogSections.Filter.class)
        interface Output {
        }
    }

}
