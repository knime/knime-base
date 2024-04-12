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

import java.util.Locale;
import java.util.Optional;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.knime.base.node.preproc.filter.row3.FilterOperator.Arity;
import org.knime.base.node.preproc.stringreplacer.CaseMatching;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.filehandling.core.util.WildcardToRegexUtil;

/**
 * Predicate for filtering rows by data values.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
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
        final var isMissing = row.isMissing(m_columnIndex);
        return !isMissing && m_predicate.test(row);
    }

    static <T> Predicate<Optional<T>> wrapOptional(final Predicate<T> predicate) {
        return o -> o.isPresent() && predicate.test(o.get());
    }

    static void validateSettings(final RowFilter3NodeSettings settings, final DataTableSpec spec)
        throws InvalidSettingsException {
        final var operator = settings.m_operator;
        if (RowFilter3NodeSettings.isFilterOnRowKeys(settings)) {
            CheckUtils.checkSetting(operator != FilterOperator.IS_MISSING, "Cannot filter RowID for presence.");
            CheckUtils.checkSetting(operator.isEnabledFor(SpecialColumns.ROWID, StringCell.TYPE),
                "Filter operator \"%s\" cannot be applied to RowID.", operator.label());
            return;
        }

        final var columnName = settings.m_column.getSelected();
        final var columnSpec =
            CheckUtils.checkNotNull(spec.getColumnSpec(columnName), "Unknown column \"%s\".", columnName);
        final var columnType = columnSpec.getType();
        final var columnIndex = spec.findColumnIndex(columnName);

        if (operator.m_arity == Arity.BINARY) {
            CheckUtils.checkSetting(StringUtils.isNotEmpty(settings.m_value), "The comparison value is missing.");
            new FormatValidator().fromDataType(settings, columnIndex, columnType, InvalidSettingsException::new);
        } else {
            // check that our cleaning hook correctly cleaned any formerly input value
            CheckUtils.checkSetting(StringUtils.isEmpty(settings.m_value), "Unexpected comparison value \"%s\".",
                settings.m_value);
        }
    }

    static Predicate<RowRead> createFrom(final ExecutionContext exec, final RowFilter3NodeSettings settings,
        final DataTableSpec spec) throws InvalidSettingsException {
        final var operator = settings.m_operator;
        if (RowFilter3NodeSettings.isFilterOnRowKeys(settings)) {
            final var predicate = new StringPredicate(operator, settings.m_caseMatching, settings.m_value);
            return row -> predicate.test(row.getRowKey().getString());
        }

        final var column = settings.m_column.m_selected;
        final var columnIndex = spec.findColumnIndex(column);
        if (settings.m_operator == FilterOperator.IS_MISSING) {
            return row -> row.isMissing(columnIndex);
        }

        final var columnSpec = spec.getColumnSpec(columnIndex);
        return new ValuePredicateCreator(exec).fromDataType(settings, columnIndex, columnSpec.getType(),
            InvalidSettingsException::new);
    }

    /**
     * Handler for a given input value with respect to a target data type. Used for checking format (as much as
     * possible) or creating a filter predicate from the value.
     */
    interface DataTypeHandler<O, X extends Throwable> {

        default O fromDataType(final RowFilter3NodeSettings settings, final int columnIndex, final DataType dataType, // NOSONAR
            final Function<String, X> exceptionFn) throws X {
            final var operator = settings.m_operator;
            final var value = settings.m_value;
            CheckUtils.check(operator.isEnabledFor(null, dataType), exceptionFn,
                () -> "Operator \"%s\" is not applicable for column data type \"%s\"".formatted(operator.label(),
                    dataType.getName()));

            if (StringCell.TYPE == dataType) {
                return handleString(columnIndex, operator, settings.m_caseMatching, value);
            }
            // check specially supported data types
            if (BooleanCell.TYPE == dataType) {
                return handleBoolean(columnIndex, operator, value);
            }
            if (LongCell.TYPE == dataType) {
                return handleLong(columnIndex, operator, value);
            }
            if (IntCell.TYPE == dataType) {
                return handleInt(columnIndex, operator, value);
            }
            if (DoubleCell.TYPE == dataType) {
                return handleDouble(columnIndex, operator, value);
            }
            // fallback to typemapping
            return handleGeneric(columnIndex, operator, value, dataType);
        }

        O handleString(int columnIndex, FilterOperator operator, CaseMatching caseMatching, String value) throws X;

        O handleBoolean(int columnIndex, FilterOperator operator, String value) throws X;

        O handleLong(int columnIndex, FilterOperator operator, String value) throws X;

        O handleInt(int columnIndex, FilterOperator operator, String value) throws X;

        O handleDouble(int columnIndex, FilterOperator operator, String value) throws X;

        O handleGeneric(int columnIndex, FilterOperator operator, String value, DataType targetType) throws X;
    }

    static final class FormatValidator implements DataTypeHandler<Boolean, InvalidSettingsException> {

        @Override
        public Boolean handleString(final int columnIndex, final FilterOperator operator,
            final CaseMatching caseMatching, final String value) throws InvalidSettingsException {
            return true;
        }

        @Override
        public Boolean handleBoolean(final int columnIndex, final FilterOperator operator, final String value)
            throws InvalidSettingsException {
            CheckUtils.checkSetting(StringUtils.isEmpty(value),
                "Value input must be empty for boolean comparison operator");
            return true;
        }

        @Override
        public Boolean handleLong(final int columnIndex, final FilterOperator operator, final String value)
            throws InvalidSettingsException {
            try {
                Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new InvalidSettingsException(
                    "Cannot parse \"%s\" as long value: %s".formatted(value, e.getMessage()), e);
            }
            return true;
        }

        @Override
        public Boolean handleInt(final int columnIndex, final FilterOperator operator, final String value)
            throws InvalidSettingsException {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new InvalidSettingsException(
                    "Cannot parse \"%s\" as int value: %s".formatted(value, e.getMessage()), e);
            }
            return true;
        }

        @Override
        public Boolean handleDouble(final int columnIndex, final FilterOperator operator, final String value)
            throws InvalidSettingsException {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new InvalidSettingsException(
                    "Cannot parse \"%s\" as double value: %s".formatted(value, e.getMessage()), e);
            }
            return true;
        }

        @Override
        public Boolean handleGeneric(final int columnIndex, final FilterOperator operator, final String value,
            final DataType targetType) throws InvalidSettingsException {
            // If we have a factory, we could parse the value.
            // To determine whether we actually can parse the value, we need an execution context, which we lack
            // during configure (where this Validator is used)
            return !JavaToDataCellConverterRegistry.getInstance().getConverterFactories(String.class, targetType)
                .isEmpty();
        }

    }

    static final class ValuePredicateCreator implements DataTypeHandler<Predicate<RowRead>, InvalidSettingsException> {

        private ExecutionContext m_exec;

        public ValuePredicateCreator(final ExecutionContext exec) {
            m_exec = exec;
        }

        @Override
        public Predicate<RowRead> handleString(final int columnIndex, final FilterOperator operator,
            final CaseMatching caseMatching, final String value) throws InvalidSettingsException {
            return createStringValuePredicate(columnIndex, operator, caseMatching, value);
        }

        private static Predicate<RowRead> createStringValuePredicate(final int colIdx, final FilterOperator operator,
            final CaseMatching caseMatching, final String value) {
            final var predicate = new StringPredicate(operator, caseMatching, value);
            return new RowReadPredicate(colIdx, row -> {
                final var cellValue = row.getValue(colIdx);
                return predicate.test(cellValue instanceof StringValue sv ? sv.getStringValue() : cellValue.toString());
            });
        }

        @Override
        public Predicate<RowRead> handleBoolean(final int columnIndex, final FilterOperator operator,
            final String value) throws InvalidSettingsException {
            final var matchTrue = switch (operator) {
                case IS_TRUE -> true;
                case IS_FALSE -> false;
                // in practice this default case was already checked before and this method never called
                default -> throw new InvalidSettingsException(
                    "Unsupported boolean operator \"%s\"".formatted(operator));
            };
            return new RowReadPredicate(columnIndex,
                rowRead -> matchTrue == ((BooleanValue)rowRead.getValue(columnIndex)).getBooleanValue());
        }

        @Override
        public Predicate<RowRead> handleLong(final int columnIndex, final FilterOperator operator, final String value)
            throws InvalidSettingsException {
            final var longValue = Long.parseLong(value);
            final var predicate = new LongValuePredicate(operator, longValue);
            return new RowReadPredicate(columnIndex,
                rowRead -> predicate.test(((LongValue)rowRead.getValue(columnIndex))));
        }

        @Override
        public Predicate<RowRead> handleInt(final int columnIndex, final FilterOperator operator, final String value)
            throws InvalidSettingsException {
            final var intValue = Integer.parseInt(value);
            final var predicate = new IntValuePredicate(operator, intValue);
            return new RowReadPredicate(columnIndex,
                rowRead -> predicate.test(((IntValue)rowRead.getValue(columnIndex))));
        }

        @Override
        public Predicate<RowRead> handleDouble(final int columnIndex, final FilterOperator operator, final String value)
            throws InvalidSettingsException {
            final var doubleValue = Double.parseDouble(value);
            final var predicate = new DoubleValuePredicate(operator, doubleValue);
            return new RowReadPredicate(columnIndex,
                rowRead -> predicate.test(((DoubleValue)rowRead.getValue(columnIndex))));
        }

        @Override
        public Predicate<RowRead> handleGeneric(final int columnIndex, final FilterOperator operator,
            final String value, final DataType targetType) throws InvalidSettingsException {
            final DataCell comparisonValue = fromString(m_exec, targetType, value);
            final var predicate = new DataCellPredicate(operator, comparisonValue);
            return new RowReadPredicate(columnIndex,
                rowRead -> predicate.test(rowRead.getValue(columnIndex).materializeDataCell()));
        }

        private static DataCell fromString(final ExecutionContext exec, final DataType dataType, final String value)
            throws InvalidSettingsException {
            final var registry = JavaToDataCellConverterRegistry.getInstance();
            final var converter =
                registry.getConverterFactories(String.class, dataType).stream().findFirst().orElseThrow().create(exec);
            try {
                return converter.convert(value);
            } catch (final Exception e) {
                throw new InvalidSettingsException(
                    "Value \"%s\" cannot be converted to column type \"%s\"".formatted(value, dataType.getName()), e);
            }
        }

    }

    /* === Value Predicates */

    sealed interface FilterPredicate<T> extends Predicate<T> permits StringPredicate, ComparableFilterPredicate<T> {
        boolean isApplicableFor(FilterOperator operator);
    }

    static final class StringPredicate implements FilterPredicate<String> {

        final Predicate<String> m_predicate;

        StringPredicate(final FilterOperator operator, final CaseMatching caseMatching, final String value) {
            CheckUtils.checkArgument(isApplicableFor(operator), "Unsupported operator \"%s\"", operator.label());

            final UnaryOperator<String> normalize = caseMatching == CaseMatching.CASESENSITIVE
                ? UnaryOperator.identity() : (s -> s.toLowerCase(Locale.ROOT));
            if (operator == FilterOperator.EQ || operator == FilterOperator.NEQ) {
                final var comparisonValue = normalize.apply(value);
                final var isNegated = operator == FilterOperator.NEQ;
                m_predicate = cellValue -> {
                    final var equal = normalize.apply(cellValue).equals(comparisonValue);
                    return isNegated ? !equal : equal;
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
                case EQ -> cell -> comp.compare(cell, comparisonValue) == 0;
                case NEQ -> cell -> comp.compare(cell, comparisonValue) != 0;
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

        LongValuePredicate(final FilterOperator operator, final long val) throws InvalidSettingsException {
            m_predicate = switch (operator) {
                case EQ -> l -> Long.compare(l, val) == 0;
                case NEQ -> l -> Long.compare(l, val) != 0;
                case LT -> l -> Long.compare(l, val) < 0;
                case LTE -> l -> Long.compare(l, val) <= 0;
                case GT -> l -> Long.compare(l, val) > 0;
                case GTE -> l -> Long.compare(l, val) >= 0;
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

        IntValuePredicate(final FilterOperator operator, final int val) throws InvalidSettingsException {
            m_predicate = switch (operator) {
                case EQ -> l -> Integer.compare(l, val) == 0;
                case NEQ -> l -> Integer.compare(l, val) != 0;
                case LT -> l -> Integer.compare(l, val) < 0;
                case LTE -> l -> Integer.compare(l, val) <= 0;
                case GT -> l -> Integer.compare(l, val) > 0;
                case GTE -> l -> Integer.compare(l, val) >= 0;
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

        DoubleValuePredicate(final FilterOperator operator, final double val) throws InvalidSettingsException {
            m_predicate = switch (operator) {
                case EQ -> d -> Double.compare(d, val) == 0;
                case NEQ -> d -> Double.compare(d, val) != 0;
                case LT -> d -> Double.compare(d, val) < 0;
                case LTE -> d -> Double.compare(d, val) <= 0;
                case GT -> d -> Double.compare(d, val) > 0;
                case GTE -> d -> Double.compare(d, val) >= 0;
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
