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

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparatorDelegator;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.util.WildcardToRegexUtil;

/**
 * Predicate for filtering rows by RowID, data values, or missingness.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
final class RowReadPredicate {

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

    private static Predicate<RowRead> createFrom(final FilterCriterion criterion, final DataTableSpec spec)
        throws InvalidSettingsException {

        // Special case for RowID, which is not a DataValue
        if (criterion.isFilterOnRowKeys()) {
            return rowKeyPredicate(criterion);
        }

        final var column = criterion.m_column.getSelected();
        final var columnIndex = spec.findColumnIndex(column);
        final var operator = criterion.m_operator;

        // Special case for "IS MISSING" operator
        if (operator == FilterOperator.IS_MISSING) {
            return row -> row.isMissing(columnIndex);
        }

        final var columnSpec = spec.getColumnSpec(columnIndex);
        final var dataType = columnSpec.getType();

        CheckUtils.check(operator.isApplicableFor(null, dataType), InvalidSettingsException::new,
            () -> "Operator \"%s\" is not applicable for column data type \"%s\"".formatted(operator.label(),
                dataType.getName()));

        final var valuePredicate = getValuePredicate(criterion, dataType, columnIndex);
        // missings never match
        return rowRead -> !rowRead.isMissing(columnIndex) && valuePredicate.test(rowRead);
    }

    private static Predicate<RowRead> rowKeyPredicate(final FilterCriterion criterion) throws InvalidSettingsException {
        final int index = 0; // take from first widget input value
        final var predicate = new StringPredicate(criterion.m_operator, isCaseSensitiveMatch(criterion, index),
            criterion.m_predicateValues.getCellAt(index).map(c -> (StringCell)c).map(StringCell::getStringValue)
                .orElseThrow(() -> new InvalidSettingsException("Missing string value for RowID comparison")));
        return row -> predicate.test(row.getRowKey().getString());
    }

    private static Predicate<RowRead> getValuePredicate(final FilterCriterion criterion, final DataType dataType,
        final int columnIndex) throws InvalidSettingsException {
        final var operator = criterion.m_operator;
        // special case Boolean
        if (dataType.equals(BooleanCell.TYPE)) {
            final var booleanPredicate = new BooleanValuePredicate(operator);
            return rowRead -> booleanPredicate.test(rowRead.<BooleanValue> getValue(columnIndex));
        }

        final int index = 0; // take from first widget input value
        final var comparisonCell = criterion.m_predicateValues.getCellAt(index)
            .orElseThrow(() -> new InvalidSettingsException("Missing comparison value"));

        // special case String
        if (dataType.equals(StringCell.TYPE)) {
            final var stringPredicate = new StringPredicate(operator, isCaseSensitiveMatch(criterion, index),
                ((StringCell)comparisonCell).getStringValue());
            return rowRead -> stringPredicate.test(rowRead.<StringValue> getValue(columnIndex).getStringValue());
        }

        // everything else
        final var predicate = new DataValuePredicate(operator, comparisonCell);
        return rowRead -> predicate.test(rowRead.getValue(columnIndex));
    }

    private static boolean isCaseSensitiveMatch(final FilterCriterion criterion, final int index) {
        return criterion.m_predicateValues.isStringMatchCaseSensitive(index);
    }

    sealed interface FilterPredicate<T> extends Predicate<T>
        permits BooleanValuePredicate, StringPredicate, DataValuePredicate {
        boolean isApplicableFor(FilterOperator operator);
    }

    static final class BooleanValuePredicate implements FilterPredicate<BooleanValue> {

        final boolean m_matchTrue;

        BooleanValuePredicate(final FilterOperator operator) {
            CheckUtils.checkArgument(isApplicableFor(operator), "Unsupported operator \"%s\"", operator.label());
            m_matchTrue = operator == FilterOperator.IS_TRUE;
        }

        @Override
        public boolean test(final BooleanValue b) {
            return m_matchTrue == b.getBooleanValue();
        }

        @Override
        public boolean isApplicableFor(final FilterOperator operator) {
            return switch (operator) {
                case IS_TRUE, IS_FALSE -> true;
                default -> false;
            };
        }
    }

    static final class StringPredicate implements FilterPredicate<String> {

        final Predicate<String> m_predicate;

        StringPredicate(final FilterOperator operator, final boolean isCaseSensitive, final String value) {
            CheckUtils.checkArgument(isApplicableFor(operator), "Unsupported operator \"%s\"", operator.label());

            if (operator == FilterOperator.EQ || operator == FilterOperator.NEQ) {
                final var isNegated = operator == FilterOperator.NEQ;
                m_predicate = cellValue -> {
                    final var equal = isCaseSensitive ? cellValue.equals(value) : cellValue.equalsIgnoreCase(value);
                    return isNegated ^ equal; // XOR, exactly one must be true
                };
                return;
            } else if (operator == FilterOperator.WILDCARD || operator == FilterOperator.REGEX) {
                final var pattern =
                    operator == FilterOperator.WILDCARD ? WildcardToRegexUtil.wildcardToRegex(value) : value;
                var flags = Pattern.DOTALL | Pattern.MULTILINE;
                flags |= isCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
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

    static final class DataValuePredicate implements FilterPredicate<DataValue> {

        final Predicate<DataValue> m_predicate;

        DataValuePredicate(final FilterOperator operator, final DataValue comparisonValue)
            throws InvalidSettingsException {
            CheckUtils.checkArgument(isApplicableFor(operator), "Unsupported operator \"%s\"", operator.label());
            final var compCell = comparisonValue.materializeDataCell();
            final var comparator = new DataValueComparatorDelegator<>(compCell.getType().getComparator());
            m_predicate = switch (operator) {
                case EQ -> v -> v.materializeDataCell().equals(compCell);
                case NEQ -> v -> !v.materializeDataCell().equals(compCell);
                case LT -> v -> comparator.compare(v, comparisonValue) < 0;
                case LTE -> v -> comparator.compare(v, comparisonValue) <= 0;
                case GT -> v -> comparator.compare(v, comparisonValue) > 0;
                case GTE -> v -> comparator.compare(v, comparisonValue) >= 0;
                default -> throw new InvalidSettingsException("Unexpected operator for value comparison: " + operator);
            };
        }

        @Override
        public boolean test(final DataValue value) {
            return m_predicate.test(value);
        }

        @Override
        public boolean isApplicableFor(final FilterOperator operator) {
            return switch (operator) {
                case EQ, NEQ, LT, LTE, GT, GTE -> true;
                default -> false;
            };
        }
    }
}
