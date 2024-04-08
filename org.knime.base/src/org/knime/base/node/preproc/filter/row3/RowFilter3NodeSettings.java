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

import org.knime.base.node.preproc.filter.row3.FilterBounds.DecimalBounds;
import org.knime.base.node.preproc.filter.row3.FilterBounds.IntegralBounds;
import org.knime.base.node.preproc.filter.row3.FilterBounds.RowNumberBounds;
import org.knime.base.node.preproc.filter.row3.FilterOperator.Arity;
import org.knime.base.node.preproc.filter.row3.FilterOperator.CompareMode;
import org.knime.base.node.preproc.filter.row3.FilterOperator.TypeBasedCompareModeChoices;
import org.knime.base.node.preproc.filter.row3.FilterOperator.TypeBasedOperatorChoices;
import org.knime.base.node.preproc.stringreplacer.CaseMatching;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.rule.And;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Expression;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Not;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Or;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.IsColumnOfTypeCondition;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.IsSpecificColumnCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.AllColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;

/**
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // Web UI not API yet
final class RowFilter3NodeSettings implements DefaultNodeSettings {

    @Widget(title = "Filter column", description = "Choose the column in which to filter for values")
    @ChoicesWidget(showRowKeysColumn = true, showRowNumbersColumn = true, choices = AllColumnChoicesProvider.class)
    @Layout(DialogSections.MatchingCriterion.ColumnAndMode.class)
    // signals for specially supported data types
    @Signal(condition = IsRowNumbersColumnCondition.class)
    @Signal(condition = IsLongCompatible.class)
    @Signal(condition = IsDoubleCompatible.class)
    ColumnSelection m_column = SpecialColumns.ROWID.toColumnSelection(); // choice is probably overridden in constructor

    @Widget(title = "Compare as", description = "TODO", advanced = true)
    @Signal(condition = IsTypeMappingComparison.class)
    @Signal(condition = IsStringComparison.class)
    @Signal(condition = IsIntegralComparison.class)
    @Signal(condition = IsDecimalComparison.class)
    @Signal(condition = IsBoolComparison.class)
    @ChoicesWidget(choicesUpdateHandler = TypeBasedCompareModeChoices.class)
    @Layout(DialogSections.MatchingCriterion.ColumnAndMode.class)
    CompareMode m_compareOn = CompareMode.TYPE_MAPPING;

    @Widget(title = "Operator", description = "TODO")
    @ChoicesWidget(choicesUpdateHandler = TypeBasedOperatorChoices.class) // depends on m_column
    @Signal(condition = IsNullaryOperator.class)
    @Signal(condition = IsUnaryOperator.class)
    @Signal(condition = IsBinaryOperator.class)
    @Signal(condition = IsPatternOperator.class)
    @Signal(condition = IsNumberOfRowsOperator.class)
    @Layout(DialogSections.MatchingCriterion.Operator.class)
    FilterOperator m_operator = FilterOperator.EQ;

    @Widget(title = "Case matching", description = "TODO")
    @ValueSwitchWidget
    @Effect(signals = {IsStringComparison.class, IsNullaryOperator.class}, operation = AllButTheLast.class,
        type = EffectType.SHOW)
    @Layout(DialogSections.MatchingCriterion.Operator.class)
    CaseMatching m_caseMatching = CaseMatching.DEFAULT;

    boolean caseSensitive() {
        return m_caseMatching == CaseMatching.CASESENSITIVE;
    }

    static class Anchors implements DefaultNodeSettings {

        static class RowNumbersAnchor implements DefaultNodeSettings {

            /**
             * Row Number bounds.
             */
            @Effect(signals = {IsRowNumbersColumnCondition.class, IsBinaryOperator.class, IsNumberOfRowsOperator.class},
                operation = AllButTheLast.class, type = EffectType.SHOW)
            RowNumberBounds m_bounds = new RowNumberBounds();

            /**
             * A specific Row Number.
             */
            @Widget(title = "Row number", description = "TODO")
            @Effect(signals = {IsRowNumbersColumnCondition.class, IsUnaryOperator.class, IsNumberOfRowsOperator.class},
                operation = AllButTheLast.class, type = EffectType.SHOW)
            @NumberInputWidget(min = 1)
            long m_rowNumber = 1;

            /**
             * n rows from start or end depending on operator (last/first n)
             */
            // we allow 0 for convenience (i.e. disabled filter)
            @Widget(title = "Number of rows", description = "TODO")
            @Effect(signals = {IsRowNumbersColumnCondition.class, IsNumberOfRowsOperator.class}, operation = And.class,
                type = EffectType.SHOW)
            @NumberInputWidget(min = 0)
            long m_noOfRows = 10;
        }

        RowNumbersAnchor m_rowNumbers = new RowNumbersAnchor();

        static class DecimalAnchor implements DefaultNodeSettings {

            @Effect(
                signals = {IsDecimalComparison.class, IsDoubleCompatible.class, IsBinaryOperator.class,
                    IsStringComparison.class},
                operation = AllButTheLast.class, type = EffectType.SHOW)
            DecimalBounds m_bounds = new DecimalBounds();

            @Widget(title = "Double value", description = "TODO")
            @Effect(
                signals = {IsDecimalComparison.class, IsDoubleCompatible.class, IsUnaryOperator.class,
                    IsStringComparison.class},
                operation = AllButTheLast.class, type = EffectType.SHOW)
            double m_value;
        }

        DecimalAnchor m_real = new DecimalAnchor();

        static class IntegralAnchor implements DefaultNodeSettings {

            @Effect(
                signals = {IsIntegralComparison.class, IsLongCompatible.class, IsBinaryOperator.class,
                    IsRowNumbersColumnCondition.class, IsStringComparison.class},
                operation = AllButTheLastTwo.class, type = EffectType.SHOW)
            IntegralBounds m_bounds = new IntegralBounds();

            @Widget(title = "Integral value", description = "TODO")
            @Effect(
                signals = {IsIntegralComparison.class, IsLongCompatible.class, IsUnaryOperator.class,
                    IsRowNumbersColumnCondition.class, IsStringComparison.class},
                operation = AllButTheLastTwo.class, type = EffectType.SHOW)
            long m_value;
        }

        IntegralAnchor m_integer = new IntegralAnchor();

        static class StringAnchor implements DefaultNodeSettings {
            @Widget(title = "String value", description = "TODO")
            @Effect(signals = {IsUnaryOperator.class, IsStringComparison.class, IsTypeMappingComparison.class,
                IsPatternOperator.class}, operation = ShowValueInput.class, type = EffectType.SHOW)
            String m_value;

            @Widget(title = "Pattern", description = "TODO")
            @Effect(signals = {IsUnaryOperator.class, IsStringComparison.class, IsPatternOperator.class},
                operation = And.class, type = EffectType.SHOW)
            String m_pattern;

            @SuppressWarnings("rawtypes")
            static final class ShowValueInput extends And {

                @SuppressWarnings("unchecked")
                public ShowValueInput(final Expression... expressions) {
                    super(buildExpression(expressions));
                }

                // IsUnaryOperator
                //   AND (
                //     IsStringComparison
                //       OR IsTypeMapping
                //   )
                //   AND !IsPatternOperator
                @SuppressWarnings("unchecked")
                private static Expression[] buildExpression(final Expression[] expressions) {
                    if (expressions.length != 4) {
                        throw new IllegalArgumentException();
                    }
                    final var expr = new Expression[3];
                    expr[0] = expressions[0]; // IsUnaryOperator
                    expr[1] = new Or<>(expressions[1], expressions[2]);
                    expr[2] = new Not(expressions[3]); // !IsPatternOperator
                    return expr;
                }

            }
        }

        StringAnchor m_string = new StringAnchor();
    }

    @Layout(DialogSections.MatchingCriterion.ValueInput.class)
    Anchors m_anchors = new Anchors();

    enum OutputMode {
            @Label("Include matches")
            INCLUDE, //
            @Label("Exclude matches")
            EXCLUDE
    }

    @Widget(title = "Filter behavior", description = "TODO")
    @ValueSwitchWidget
    @Layout(DialogSections.Output.class)
    OutputMode m_outputMode = OutputMode.INCLUDE;

    boolean includeMatches() {
        return m_outputMode == OutputMode.INCLUDE;
    }

    // ==================
    // === META STUFF ===
    // ==================

    // CONSTRUCTORS

    RowFilter3NodeSettings() {
        // needed for de-/serialisation
    }

    RowFilter3NodeSettings(final DefaultNodeSettingsContext ctx) {
        // set last column as default column
        final var spec = ctx.getDataTableSpec(0);
        if (spec.isEmpty() || spec.get().getNumColumns() == 0) {
            return;
        }
        final var col = spec.get().getColumnSpec(spec.get().getNumColumns() - 1);
        m_column = new ColumnSelection(col.getName(), col.getType());
    }

    // SECTIONS

    interface DialogSections {
        @Section(title = "Matching Criterion")
        interface MatchingCriterion {
            @HorizontalLayout
            interface ColumnAndMode {
            }

            @HorizontalLayout
            @After(DialogSections.MatchingCriterion.ColumnAndMode.class)
            interface Operator {
            }

            @HorizontalLayout
            @After(DialogSections.MatchingCriterion.Operator.class)
            interface ValueInput {
            }
        }

        @Section(title = "Output")
        @After(DialogSections.MatchingCriterion.class)
        interface Output {
        }
    }

    // SIGNALS

    static class IsLongCompatible extends IsColumnOfTypeCondition {
        @Override
        public Class<LongValue> getDataValueClass() {
            return LongValue.class;
        }
    }

    static class IsDoubleCompatible extends IsColumnOfTypeCondition {
        @Override
        public Class<DoubleValue> getDataValueClass() {
            return DoubleValue.class;
        }
    }

    static class IsStringCompatible extends IsColumnOfTypeCondition {
        @Override
        public Class<StringValue> getDataValueClass() {
            return StringValue.class;
        }
    }

    /* === COMPARISON MODE signals */

    static class IsStringComparison extends OneOfEnumCondition<CompareMode> {
        @Override
        public CompareMode[] oneOf() {
            return new CompareMode[]{CompareMode.AS_STRING};
        }
    }

    static class IsTypeMappingComparison extends OneOfEnumCondition<CompareMode> {
        @Override
        public CompareMode[] oneOf() {
            return new CompareMode[]{CompareMode.TYPE_MAPPING};
        }
    }

    static class IsIntegralComparison extends OneOfEnumCondition<CompareMode> {
        @Override
        public CompareMode[] oneOf() {
            return new CompareMode[]{CompareMode.INTEGRAL};
        }
    }

    static class IsDecimalComparison extends OneOfEnumCondition<CompareMode> {
        @Override
        public CompareMode[] oneOf() {
            return new CompareMode[]{CompareMode.DECIMAL};
        }
    }

    static class IsBoolComparison extends OneOfEnumCondition<CompareMode> {
        @Override
        public CompareMode[] oneOf() {
            return new CompareMode[]{CompareMode.BOOL};
        }
    }

    /* === OPERATOR ARITY signals */

    static final class IsNullaryOperator extends OneOfEnumCondition<FilterOperator> {
        @Override
        public FilterOperator[] oneOf() {
            return Arrays.stream(FilterOperator.values()).filter(op -> op.m_arity == Arity.NULLARY)
                .toArray(FilterOperator[]::new);
        }
    }

    static final class IsUnaryOperator extends OneOfEnumCondition<FilterOperator> {
        @Override
        public FilterOperator[] oneOf() {
            return Arrays.stream(FilterOperator.values()).filter(op -> op.m_arity == Arity.UNARY)
                .toArray(FilterOperator[]::new);
        }
    }

    static final class IsBinaryOperator extends OneOfEnumCondition<FilterOperator> {
        @Override
        public FilterOperator[] oneOf() {
            return Arrays.stream(FilterOperator.values()).filter(op -> op.m_arity == Arity.BINARY)
                .toArray(FilterOperator[]::new);
        }
    }

    static final class IsPatternOperator extends OneOfEnumCondition<FilterOperator> {
        @Override
        public FilterOperator[] oneOf() {
            return new FilterOperator[]{FilterOperator.REGEX, FilterOperator.WILDCARD};
        }
    }

    static final class IsNumberOfRowsOperator extends OneOfEnumCondition<FilterOperator> {
        @Override
        public FilterOperator[] oneOf() {
            return new FilterOperator[]{FilterOperator.FIRST_N_ROWS, FilterOperator.LAST_N_ROWS};
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
            super(swapLast(expressions));
        }

        private static Expression[] swapLast(final Expression[] expressions) {
            if (expressions == null || expressions.length == 0) {
                throw new IllegalArgumentException("Operator can only be used on at least one expression");
            }
            var result = new Expression[expressions.length];
            System.arraycopy(expressions, 0, result, 0, expressions.length - 1);
            result[expressions.length - 1] = new Not(expressions[expressions.length - 1]);
            return result;

        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class AllButTheLastTwo extends And {
        public AllButTheLastTwo(final Expression... expressions) {
            super(swapLast(expressions));
        }

        private static Expression[] swapLast(final Expression[] expressions) {
            if (expressions == null || expressions.length <= 1) {
                throw new IllegalArgumentException("Operator can only be used on at least two expressions");
            }
            var result = new Expression[expressions.length];
            System.arraycopy(expressions, 0, result, 0, expressions.length - 2);
            result[expressions.length - 2] = new Not(expressions[expressions.length - 2]);
            result[expressions.length - 1] = new Not(expressions[expressions.length - 1]);
            return result;

        }
    }

    // UTILITIES

    static boolean isFilterOnRowKeys(final RowFilter3NodeSettings settings) {
        return SpecialColumns.ROWID.getId().equals(settings.m_column.getSelected());
    }
}
