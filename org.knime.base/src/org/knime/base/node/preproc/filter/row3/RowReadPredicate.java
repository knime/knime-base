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
 *   22 Mar 2024 (jasper): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.util.Optional;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.base.node.preproc.stringreplacer.CaseMatching;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput.DynamicValue.StringValueModifiers;
import org.knime.filehandling.core.util.WildcardToRegexUtil;

/**
 * Predicate for filtering rows by data values.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
final class RowReadPredicate implements Predicate<RowRead> {

    private final Predicate<RowRead> m_predicate;

    private final int m_columnIndex;

    private RowReadPredicate(final int columnIndex, final Predicate<RowRead> predicate) {
        m_columnIndex = columnIndex;
        m_predicate = predicate;
    }

    @Override
    public boolean test(final RowRead row) {
        return !row.isMissing(m_columnIndex) && m_predicate.test(row);
    }

    static <T> Predicate<Optional<T>> wrapOptional(final Predicate<T> predicate) {
        return o -> o.isPresent() && predicate.test(o.get());
    }

    static Predicate<RowRead> buildPredicate(final boolean isAnd, final Iterable<FilterCriterion> filterCriteria,
        final DataTableSpec inSpec) throws InvalidSettingsException {
        final var iter = filterCriteria.iterator();
        if (!iter.hasNext()) {
            return null;
        }
        var filterPredicate = createFrom(iter.next(), inSpec);
        while (iter.hasNext()) {
            final var predicate = createFrom(iter.next(), inSpec);
            filterPredicate = isAnd ? filterPredicate.and(predicate) : filterPredicate.or(predicate);
        }
        return filterPredicate;
    }

    private static CaseMatching getCaseMatching(final FilterCriterion criterion) {
        return ((StringValueModifiers)criterion.m_predicateValues.getModifiersAt(0)).isCaseSensitive()
            ? CaseMatching.CASESENSITIVE : CaseMatching.CASEINSENSITIVE;
    }

    private static Predicate<RowRead> createFrom(final FilterCriterion criterion, final DataTableSpec spec)
        throws InvalidSettingsException {
        final var operator = criterion.m_operator;
        if (criterion.isFilterOnRowKeys()) {
            final var predicate = new StringPredicate(operator, getCaseMatching(criterion),
                criterion.m_predicateValues.getCellAt(0).map(c -> (StringCell)c).map(StringCell::getStringValue)
                    .orElseThrow(() -> new InvalidSettingsException("Missing string value for RowID comparison")));
            return row -> predicate.test(row.getRowKey().getString());
        }

        final var column = criterion.m_column.getSelected();
        final var columnIndex = spec.findColumnIndex(column);
        if (criterion.m_operator == FilterOperator.IS_MISSING) {
            return row -> row.isMissing(columnIndex);
        }

        final var columnSpec = spec.getColumnSpec(columnIndex);
        return new ValuePredicateCreator().fromDataType(criterion, columnIndex, columnSpec.getType(),
            InvalidSettingsException::new);
    }

    /**
     * Handler for a given input value with respect to a target data type. Used for checking format (as much as
     * possible) or creating a filter predicate from the value.
     */
    interface DataTypeHandler<O, X extends Throwable> {

        default O fromDataType(final FilterCriterion criterion, final int columnIndex, // NOSONAR
            final DataType dataType, final Function<String, X> exceptionFn) throws X {
            final var operator = criterion.m_operator;
            CheckUtils.check(operator.isEnabledFor(null, dataType), exceptionFn,
                () -> "Operator \"%s\" is not applicable for column data type \"%s\"".formatted(operator.label(),
                    dataType.getName()));

            if (BooleanCell.TYPE == dataType) {
                return handleBoolean(columnIndex, operator);
            }
            final var value = criterion.m_predicateValues.getCellAt(0)
                    .orElseThrow(() -> exceptionFn.apply("Comparison value missing"));
            if (StringCell.TYPE == dataType) {
                return handleString(columnIndex, operator, getCaseMatching(criterion),
                    ((StringCell)value).getStringValue());
            }
            if (LongCell.TYPE == dataType) {
                return handleLong(columnIndex, operator, ((LongCell)value).getLongValue());
            }
            if (IntCell.TYPE == dataType) {
                return handleInt(columnIndex, operator, ((IntCell)value).getIntValue());
            }
            if (DoubleCell.TYPE == dataType) {
                return handleDouble(columnIndex, operator, ((DoubleCell)value).getDoubleValue());
            }
            return handleGeneric(columnIndex, operator, value);
        }

        O handleString(int columnIndex, FilterOperator operator, CaseMatching caseMatching, String value) throws X;

        O handleBoolean(int columnIndex, FilterOperator operator) throws X;

        O handleLong(int columnIndex, FilterOperator operator, long value) throws X;

        O handleInt(int columnIndex, FilterOperator operator, int value) throws X;

        O handleDouble(int columnIndex, FilterOperator operator, double value) throws X;

        O handleGeneric(int columnIndex, FilterOperator operator, DataCell value) throws X;
    }

    static final class ValuePredicateCreator implements DataTypeHandler<Predicate<RowRead>, InvalidSettingsException> {

        @Override
        public Predicate<RowRead> handleString(final int columnIndex, final FilterOperator operator,
            final CaseMatching caseMatching, final String value) throws InvalidSettingsException {
            final var predicate = new StringPredicate(operator, caseMatching, value);
            return new RowReadPredicate(columnIndex,
                rowRead -> predicate.test(rowRead.<StringValue> getValue(columnIndex).getStringValue()));
        }

        @Override
        public Predicate<RowRead> handleBoolean(final int columnIndex, final FilterOperator operator)
            throws InvalidSettingsException {
            final var matchTrue = switch (operator) {
                case IS_TRUE -> true;
                case IS_FALSE -> false;
                // in practice this default case was already checked before and this method never called
                default -> throw new InvalidSettingsException(
                    "Unsupported boolean operator \"%s\"".formatted(operator));
            };
            return new RowReadPredicate(columnIndex,
                rowRead -> matchTrue == rowRead.<BooleanValue> getValue(columnIndex).getBooleanValue());
        }

        @Override
        public Predicate<RowRead> handleLong(final int columnIndex, final FilterOperator operator, final long value)
            throws InvalidSettingsException {
            final var predicate = new LongValuePredicate(operator, value);
            return new RowReadPredicate(columnIndex,
                rowRead -> predicate.test(rowRead.<LongValue> getValue(columnIndex)));
        }

        @Override
        public Predicate<RowRead> handleInt(final int columnIndex, final FilterOperator operator, final int value)
            throws InvalidSettingsException {
            final var predicate = new IntValuePredicate(operator, value);
            return new RowReadPredicate(columnIndex,
                rowRead -> predicate.test(rowRead.<IntValue> getValue(columnIndex)));
        }

        @Override
        public Predicate<RowRead> handleDouble(final int columnIndex, final FilterOperator operator, final double value)
            throws InvalidSettingsException {
            final var predicate = new DoubleValuePredicate(operator, value);
            return new RowReadPredicate(columnIndex,
                rowRead -> predicate.test(rowRead.<DoubleValue> getValue(columnIndex)));
        }

        @Override
        public Predicate<RowRead> handleGeneric(final int columnIndex, final FilterOperator operator,
            final DataCell value) throws InvalidSettingsException {
            final var predicate = new DataCellPredicate(operator, value);
            return new RowReadPredicate(columnIndex,
                rowRead -> predicate.test(rowRead.getValue(columnIndex).materializeDataCell()));
        }

    }

    /* === Value Predicates */

    sealed interface FilterPredicate<T> extends Predicate<T> permits StringPredicate, ComparableFilterPredicate {
        boolean isApplicableFor(FilterOperator operator);
    }

    static final class StringPredicate implements FilterPredicate<String> {

        final Predicate<String> m_predicate;

        StringPredicate(final FilterOperator operator, final CaseMatching caseMatching, final String value) {
            CheckUtils.checkArgument(isApplicableFor(operator), "Unsupported operator \"%s\"", operator.label());

            if (operator == FilterOperator.EQ || operator == FilterOperator.NEQ) {
                final var caseSensitive = caseMatching == CaseMatching.CASESENSITIVE;
                final var isNegated = operator == FilterOperator.NEQ;
                m_predicate = cellValue -> {
                    final var equal = caseSensitive ? cellValue.equals(value) : cellValue.equalsIgnoreCase(value);
                    return isNegated ^ equal; // XOR, exactly one must be true
                };
                return;
            } else if (operator == FilterOperator.WILDCARD || operator == FilterOperator.REGEX) {
                final var pattern =
                    operator == FilterOperator.WILDCARD ? WildcardToRegexUtil.wildcardToRegex(value) : value;
                var flags = Pattern.DOTALL | Pattern.MULTILINE;
                flags |= caseMatching == CaseMatching.CASESENSITIVE ? 0 : Pattern.CASE_INSENSITIVE;
                var regex = Pattern.compile(pattern, flags);
                m_predicate = cellValue -> regex.matcher(cellValue).matches();
                return;
            }
            throw new IllegalStateException("Unsupported operator for string condition: " + operator);
        }

        @Override
        public boolean test(final String stringValue) {
            return m_predicate.test(stringValue);
        }

        @Override
        public boolean isApplicableFor(final FilterOperator operator) {
            return switch (operator) {
                case EQ, NEQ, WILDCARD, REGEX -> true;
                default -> false;
            };
        }

    }

    sealed interface ComparableFilterPredicate<T> extends FilterPredicate<T>
        permits LongValuePredicate, IntValuePredicate, DoubleValuePredicate, DataCellPredicate {

        @Override
        default boolean isApplicableFor(final FilterOperator operator) {
            return switch (operator) {
                case EQ, NEQ, LT, LTE, GT, GTE -> true;
                default -> false;
            };
        }

    }

    static final class DataCellPredicate implements ComparableFilterPredicate<DataCell> {

        final Predicate<DataCell> m_predicate;

        DataCellPredicate(final FilterOperator operator, final DataCell comparisonValue)
            throws InvalidSettingsException {
            final var comp = comparisonValue.getType().getComparator();
            m_predicate = switch (operator) {
                case EQ -> cell -> cell.equals(comparisonValue);
                case NEQ -> cell -> !cell.equals(comparisonValue);
                case LT -> cell -> comp.compare(cell, comparisonValue) < 0;
                case LTE -> cell -> comp.compare(cell, comparisonValue) <= 0;
                case GT -> cell -> comp.compare(cell, comparisonValue) > 0;
                case GTE -> cell -> comp.compare(cell, comparisonValue) >= 0;
                default -> throw new InvalidSettingsException("Unexpected operator for value comparison: " + operator);
            };
        }

        @Override
        public boolean test(final DataCell value) {
            return m_predicate.test(value);
        }
    }

    static final class LongValuePredicate implements ComparableFilterPredicate<LongValue> {

        final LongPredicate m_predicate;

        LongValuePredicate(final FilterOperator operator, final long comparisonValue) throws InvalidSettingsException {
            m_predicate = switch (operator) {
                case EQ -> value -> value == comparisonValue;
                case NEQ -> value -> value != comparisonValue;
                case LT -> value -> value < comparisonValue;
                case LTE -> value -> value <= comparisonValue;
                case GT -> value -> value > comparisonValue;
                case GTE -> value -> value >= comparisonValue;
                default -> throw new InvalidSettingsException(
                    "Unexpected operator for integral numeric condition: " + operator);
            };
        }

        @Override
        public boolean test(final LongValue value) {
            return m_predicate.test(value.getLongValue());
        }
    }

    static final class IntValuePredicate implements ComparableFilterPredicate<IntValue> {

        final IntPredicate m_predicate;

        IntValuePredicate(final FilterOperator operator, final int comparisonValue) throws InvalidSettingsException {
            m_predicate = switch (operator) {
                case EQ -> value -> value == comparisonValue;
                case NEQ -> value -> value != comparisonValue;
                case LT -> value -> value < comparisonValue;
                case LTE -> value -> value <= comparisonValue;
                case GT -> value -> value > comparisonValue;
                case GTE -> value -> value >= comparisonValue;
                default -> throw new InvalidSettingsException(
                    "Unexpected operator for integral numeric condition: " + operator);
            };
        }

        @Override
        public boolean test(final IntValue value) {
            return m_predicate.test(value.getIntValue());
        }
    }

    static final class DoubleValuePredicate implements ComparableFilterPredicate<DoubleValue> {

        final DoublePredicate m_predicate;

        DoubleValuePredicate(final FilterOperator operator, final double comparisonValue)
                throws InvalidSettingsException {
            /*
             * Why not (only) primitive comparisons for doubles?
             *
             * We need to compare all NaNs equal and larger/smaller than other values
             * (as the DoubleCell and DoubleValueComparator classes do it)
             */
            m_predicate = switch (operator) {
                case EQ -> value -> Double.doubleToLongBits(value) == Double.doubleToLongBits(comparisonValue);
                case NEQ -> value -> Double.doubleToLongBits(value) != Double.doubleToLongBits(comparisonValue);
                case LT -> value -> Double.compare(value, comparisonValue) < 0;
                case LTE -> value -> Double.compare(value, comparisonValue) <= 0;
                case GT -> value -> Double.compare(value, comparisonValue) > 0;
                case GTE -> value -> Double.compare(value, comparisonValue) >= 0;
                default -> throw new InvalidSettingsException(
                    "Unexpected operator for double numeric condition: " + operator);
            };
        }

        @Override
        public boolean test(final DoubleValue value) {
            return m_predicate.test(value.getDoubleValue());
        }
    }
}
