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
 *   24 Jan 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.node.preproc.filter.row.RowFilterNodeSettings.DialogLayout.FilterSection;
import org.knime.base.node.preproc.filter.row.RowFilterNodeSettings.DialogLayout.FilterSection.OperatorSelection;
import org.knime.base.node.preproc.filter.row.RowFilterNodeSettings.DialogLayout.FilterSection.Values;
import org.knime.base.node.preproc.filter.row.RowFilterNodeSettings.DialogLayout.OutputSection;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.rule.And;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Expression;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Not;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.rule.TrueCondition;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.IsColumnOfTypeCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesUpdateHandler;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.DeclaringDefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;


/**
 * Settings for the {@link RowFilterNodeModel}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
@Persistor(value = LegacyRowFilterPersistor.class)
@Layout(FilterSection.class)
final class RowFilterNodeSettings implements DefaultNodeSettings {

    interface DialogLayout {
        @Section(title = "Filter")
        interface FilterSection {

            interface ColumnSelection {}

            @After(FilterSection.ColumnSelection.class)
            interface OperatorSelection {
                @HorizontalLayout
                interface StringMatching {}
            }

            @After(OperatorSelection.class)
            interface Values {

                @HorizontalLayout
                @Effect(signals = FilterOperator.IsBinary.class, type = EffectType.SHOW)
                interface Bounds {}
            }
        }

        @Section(title = "Output")
        @After(FilterSection.class)
        interface OutputSection {}
    }


    @Widget(title = "Filter column")
    @Layout(FilterSection.ColumnSelection.class)
    @ChoicesWidget(choices = AllColumns.class, showRowKeysColumn = true)
    @Signal(condition = StringColumnSelected.class)
    @Signal(condition = BooleanColumnSelected.class)
    @Signal(condition = NumberColumnSelected.class)
    @Signal(condition = CollectionColumnSelected.class)
    ColumnSelection m_targetSelection = new ColumnSelection();

    private static final class AllColumns implements ColumnChoicesProvider {

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0)//
                .stream()//
                .flatMap(DataTableSpec::stream)//
                .toArray(DataColumnSpec[]::new);
        }
    }

    static final class StringColumnSelected extends IsColumnOfTypeCondition {
        @Override
        public Class<? extends DataValue> getDataValueClass() {
            return StringValue.class;
        }
    }

    static final class BooleanColumnSelected extends IsColumnOfTypeCondition {
        @Override
        public Class<? extends DataValue> getDataValueClass() {
            return BooleanValue.class;
        }
    }

    static final class NumberColumnSelected extends IsColumnOfTypeCondition {
        @Override
        public Class<? extends DataValue> getDataValueClass() {
            return DoubleValue.class;
        }
    }

    static final class CollectionColumnSelected extends IsColumnOfTypeCondition {
        @Override
        public Class<? extends DataValue> getDataValueClass() {
            return CollectionDataValue.class;
        }
    }

    // just used for the signal ID below
    private interface FilterCollectionElements {}

    @Widget(title = "Filter based on collection elements")
    @Layout(FilterSection.ColumnSelection.class)
    @Effect(signals = CollectionColumnSelected.class, type = EffectType.SHOW)
    @Signal(id = FilterCollectionElements.class, condition = TrueCondition.class)
    boolean m_deepFiltering = true;

    private static final class DataTypeFilterSettings {
        @DeclaringDefaultNodeSettings(RowFilterNodeSettings.class)
        ColumnSelection m_targetSelection;
        @DeclaringDefaultNodeSettings(RowFilterNodeSettings.class)
        boolean m_deepFiltering;
    }

    @Widget(title = "Operator")
    @Layout(OperatorSelection.class)
    @ChoicesWidget(choicesUpdateHandler = TypeBasedOperatorChoices.class)
    @Signal(condition = FilterOperator.IsEquals.class)
    @Signal(condition = FilterOperator.IsUnary.class)
    @Signal(condition = FilterOperator.IsBinary.class)
    FilterOperator m_operator = FilterOperator.EQ;

    private static class TypeBasedOperatorChoices implements ChoicesUpdateHandler<DataTypeFilterSettings> {

        @Override
        public IdAndText[] update(final DataTypeFilterSettings settings, final DefaultNodeSettingsContext context)
            throws WidgetHandlerException {
            final var filterColumn = settings.m_targetSelection;
            if (filterColumn == null) {
                return new IdAndText[0];
            }

            final var types = filterColumn.m_compatibleTypes;
            if (types == null || types.length == 0) {
                return new IdAndText[0];
            }
            final var compatibleClassNames = Arrays.stream(filterColumn.m_compatibleTypes)
                    // // only look at preferred value class of type
                    // .findFirst().map(Set::of).orElse(Set.of());
                    .collect(Collectors.toSet());

            final var elementComp = settings.m_deepFiltering;
            if (elementComp &&
                    compatibleClassNames.contains(ColumnSelection.getTypeClassIdentifier(CollectionDataValue.class))) {
                // we need to filter based on element types
                final var elementClassNames = Arrays.stream(context.getDataTableSpec(0)
                        .map(dts -> dts.getColumnSpec(filterColumn.m_selected))
                        .map(dcs -> dcs.getType().getCollectionElementType())
                        .map(ColumnSelection::getCompatibleTypes).orElse(new String[0])).collect(Collectors.toSet());
                return filterForCompatibility(elementClassNames);
            }
            // filter on top-level type
            return filterForCompatibility(compatibleClassNames);
        }

        private static IdAndText[] filterForCompatibility(final Set<String> compatibleClassNames) {
            return Arrays.stream(FilterOperator.values()).filter(op -> op.isEnabledFor(compatibleClassNames))
                    .map(op -> new IdAndText(op.name(), op.label()))
                    .toArray(IdAndText[]::new);
        }

    }

    // TODO old Row Filter would convert everything with toString
    enum FilterOperator {
        @Label("=") // RowID, RowIndex/Number, Int, Long, Double, String
        EQ("=", Set.of(new IsEq()), Arity.UNARY),
        @Label("≠") // RowID, RowIndex/Number, Int, Long, Double, String
        NEQ("≠", Set.of(new IsEq()), Arity.UNARY),
        @Label("<") // RowIndex/Number, Int, Long, Double
        LT("<", Set.of(new IsOrd()), Arity.UNARY),
        @Label(">") // RowIndex/Number, Int, Long, Double
        GT(">", Set.of(new IsOrd()), Arity.UNARY),
        @Label("≤") // RowIndex/Number, Int, Long, Double
        LTE("≤", Set.of(new IsOrd()), Arity.UNARY),
        @Label("≥") // RowIndex/Number, Int, Long, Double
        GTE("≥", Set.of(new IsOrd()), Arity.UNARY),
        @Label("is between") // RowIndex/Number, Int, Long, Double
        BETWEEN("is between", Set.of(new IsOrd()), Arity.BINARY),
        @Label("is true") // Boolean
        IS_TRUE("is true", Set.of(new IsTruthy()), Arity.NULLARY),
        @Label("is false") // Boolean
        IS_FALSE("is false", Set.of(new IsTruthy()), Arity.NULLARY),
        @Label("is missing") // RowID, RowIndex/Number, Int, Long, Double, String
        IS_MISSING("is missing", Set.of(new IsMissing()), Arity.NULLARY);

        private final String m_label;
        private final Set<Capability> m_filters;
        final Arity m_arity;

        FilterOperator(final String label, final Set<Capability> filters, final Arity arity) {
            m_label = label;
            m_filters = filters;
            m_arity = arity;
        }

        boolean isEnabledFor(final Set<String> fullyQualifiedTypeNames) {
            return m_filters.stream().anyMatch(f -> f.satisfiedByAnyOf(fullyQualifiedTypeNames));
        }

        String label() {
            return m_label;
        }

        /**
         * Arity of the operator to determine whether to show zero, one, or two input fields.
         */
        enum Arity {
            NULLARY,
            UNARY,
            BINARY
        }

        // Classes for Conditions

        private static final class IsEquals extends OneOfEnumCondition<FilterOperator> {
            @Override
            public FilterOperator[] oneOf() {
                return new FilterOperator[] {EQ, NEQ};
            }
        }

        private static final class IsUnary extends OneOfEnumCondition<FilterOperator> {
            @Override
            public FilterOperator[] oneOf() {
                return Arrays.stream(FilterOperator.values()).filter(op -> op.m_arity == Arity.UNARY)
                        .toArray(FilterOperator[]::new);
            }
        }

        private static final class IsBinary extends OneOfEnumCondition<FilterOperator> {
            @Override
            public FilterOperator[] oneOf() {
                return Arrays.stream(FilterOperator.values()).filter(op -> op.m_arity == Arity.BINARY)
                        .toArray(FilterOperator[]::new);
            }
        }

        // Classes for Choices update handler

        sealed interface Capability permits IsOrd, IsEq, IsMissing, IsTruthy {
            boolean satisfiedByAnyOf(Set<String> fullyQualifiedValueClassesNames);
        }

        private static final class IsOrd implements Capability {
            // Row Index, Row Number, and numeric data types are orderable
            @Override
            public boolean satisfiedByAnyOf(final Set<String> fqValueClassesNames) {
                return !new IsTruthy().satisfiedByAnyOf(fqValueClassesNames)
                        && fqValueClassesNames.contains(ColumnSelection.getTypeClassIdentifier(DoubleValue.class));
             }
        }

        private static final class IsEq implements Capability {
            // RowID, Row Index, Row Number, and all data types are equals comparable, except Boolean for which we
            // want separate operators: "is true" and "is false"
            // TODO old Row Filter would convert everything with toString
            @Override
            public boolean satisfiedByAnyOf(final Set<String> fqValueClassesNames) {
                // TODO also not for collection column atm (except for if comparison on collection elements...)
                return !new IsTruthy().satisfiedByAnyOf(fqValueClassesNames);
            }
        }

        private static final class IsTruthy implements Capability {

            @Override
            public boolean satisfiedByAnyOf(final Set<String> fqValueClassesNames) {
                return fqValueClassesNames.contains(ColumnSelection.getTypeClassIdentifier(BooleanValue.class));
            }

        }

        private static final class IsMissing implements Capability {
            // Normal columns are is "missing comparable"
            @Override
            public boolean satisfiedByAnyOf(final Set<String> fqValueClassesNames) {
                // TODO marker missing to disable it for RowID/RowIndex/RowNumber
                return true;
            }
        }
    }


    enum StringMatchingMode {
        LITERAL,
        WILDCARDS,
        REGEX;

        private static final class IsLiteral extends OneOfEnumCondition<StringMatchingMode> {
            @Override
            public StringMatchingMode[] oneOf() {
                return new StringMatchingMode[] {LITERAL};
            }
        }
    }

    @Layout(OperatorSelection.StringMatching.class)
    @Widget(title = "String matching")
    @ValueSwitchWidget
    @Effect(signals = {StringColumnSelected.class, FilterOperator.IsEquals.class}, operation = And.class,
        type = EffectType.SHOW)
    @Signal(condition = StringMatchingMode.IsLiteral.class)
    StringMatchingMode m_stringMatching = StringMatchingMode.LITERAL;

    @Layout(OperatorSelection.StringMatching.class)
    @Widget(title = "Match strings case-sensitive")
    @Effect(signals = {StringColumnSelected.class, FilterOperator.IsEquals.class}, operation = And.class,
        type = EffectType.SHOW)
    boolean m_caseSensitive;

    // if data type is string (or as fallback for string-compatible types)
    @Layout(Values.class)
    @Widget(title = "Value")
    @Effect(signals = { StringColumnSelected.class, FilterOperator.IsUnary.class, StringMatchingMode.IsLiteral.class},
        operation = And.class, type = EffectType.SHOW)
    String m_stringValue;

    // implements: and( and( l, m ), not(r) )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class PatternOp extends And {
        PatternOp(final Expression left, final Expression middle, final Expression right) {
            super(new And(left, middle), new Not(right));
        }
    }

    @Layout(Values.class)
    @Widget(title = "Pattern")
    @Effect(signals = {StringColumnSelected.class, FilterOperator.IsUnary.class, StringMatchingMode.IsLiteral.class},
        operation = PatternOp.class, type = EffectType.SHOW)
    String m_patternValue;


    // if data type is number
    @Layout(Values.class)
    @Widget(title = "Value")
    @Effect(signals = { NumberColumnSelected.class, FilterOperator.IsUnary.class }, operation = And.class,
        type = EffectType.SHOW)
    double m_numericValue;

    // TODO needs ColumnSelection support for RowNumber/RowIndex and associated condition
    // @Layout(Values.class)
    // @Widget(title = "Number of rows")
    // int m_numberOfRows;

    // Bounds (Value and Date)

    @Layout(Values.Bounds.class)
    @Widget(title = "Lower bound")
    double m_lowerBound;
    @Layout(Values.Bounds.class)
    @Widget(title = "Upper bound")
    double m_upperBound;

    // TODO needs DataValues from org.knime.time (or Filter must be extensible via extension point)
    // @Layout(Values.Bounds.class)
    // @Widget(title = "Lower bound")
    // LocalDate m_dateLowerBound;
    //
    // @Layout(Values.Bounds.class)
    // @Widget(title = "Upper bound")
    // LocalDate m_dateUpperBound;


    @Widget(title = "Filter behavior")
    @ValueSwitchWidget
    @Layout(OutputSection.class)
    OutputMode m_outputMode = OutputMode.INCLUDE;

    enum OutputMode {
        @Label("Include matches")
        INCLUDE,
        @Label("Exclude matches")
        EXCLUDE
    }

}
