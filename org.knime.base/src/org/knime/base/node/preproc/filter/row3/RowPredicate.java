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
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.knime.base.node.preproc.stringreplacer.CaseMatching;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
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
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.util.WildcardToRegexUtil;

/**
 * Predicate for filtering rows by data values.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class DataValuePredicate implements Predicate<RowRead> {

    private final Predicate<RowRead> m_predicate;

    private final int m_columnIndex;

    private DataValuePredicate(final int columnIndex, final Predicate<RowRead> predicate) {
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

    @SuppressWarnings("restriction")
    public static Predicate<RowRead> forSettings(final ExecutionContext exec, final RowFilter3NodeSettings settings,
        final DataTableSpec spec) throws InvalidSettingsException {
        if (RowFilter3NodeSettings.isFilterOnRowKeys(settings)) {
            final var stringPredicate = buildStringPredicate(settings);
            return row -> stringPredicate.test(row.getRowKey().getString());
        }

        final var column = settings.m_column.m_selected;
        final var columnIndex = spec.findColumnIndex(column);
        if (settings.m_operator == FilterOperator.IS_MISSING) {
            return row -> row.isMissing(columnIndex);
        }

        return switch (settings.m_compareOn) {
            case AS_STRING -> createStringValuePredicate(columnIndex, settings);
            case TYPE_MAPPING, INTEGRAL, DECIMAL, BOOL -> {
                final var columnSpec = spec.getColumnSpec(columnIndex);
                yield createDataValuePredicate(exec, columnIndex, columnSpec, settings);
            }
        };
    }

    private static Predicate<RowRead> createDataValuePredicate(final ExecutionContext exec, final int columnIndex,
        final DataColumnSpec spec, final RowFilter3NodeSettings settings) throws InvalidSettingsException {
        final var dataType = spec.getType();
        // check specially supported data types
        if (BooleanCell.TYPE == dataType) {
            final var matchTrue = switch (settings.m_operator) {
                case IS_TRUE -> true;
                case IS_FALSE -> false;
                default -> throw new InvalidSettingsException(
                    "Unsupported boolean operator \"%s\"".formatted(settings.m_operator));
            };
            return new DataValuePredicate(columnIndex, rowRead -> {
                final var value = ((BooleanValue)rowRead.getValue(columnIndex)).getBooleanValue();
                return matchTrue == value;
            });
        }
        if (LongCell.TYPE == dataType) {
            final var predicate = buildLongPredicate(settings);
            return new DataValuePredicate(columnIndex, rowRead -> {
                final var value = (LongValue)rowRead.getValue(columnIndex);
                return predicate.test(value.getLongValue());
            });
        }
        if (IntCell.TYPE == dataType) {
            final var predicate = buildLongPredicate(settings);
            return new DataValuePredicate(columnIndex, rowRead -> {
                final var value = (IntValue)rowRead.getValue(columnIndex);
                return predicate.test(value.getIntValue());
            });
        }
        if (DoubleCell.TYPE == dataType) {
            final var predicate = buildDoublePredicate(settings);
            return new DataValuePredicate(columnIndex, rowRead -> {
                final var value = (DoubleValue)rowRead.getValue(columnIndex);
                return predicate.test(value.getDoubleValue());
            });
        }

        // use type-mapping as fallback
        final var operator = settings.m_operator;
        if (!(operator == FilterOperator.EQ || operator == FilterOperator.NEQ)) {
            throw new InvalidSettingsException(
                "Unexpected operator \"%s\", expected \"=\" or \"!=\"".formatted(operator.m_label));
        }
        final DataCell comparisonValue = parseString(exec, dataType, settings.m_anchors.m_string.m_value);
        final var isNegated = operator == FilterOperator.NEQ;
        return new DataValuePredicate(columnIndex, row -> {
            final var equal = comparisonValue.equals(row.getValue(columnIndex).materializeDataCell());
            return isNegated ? !equal : equal;
        });
    }

    private static DataCell parseString(final ExecutionContext exec, final DataType dataType, final String value)
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

    private static Predicate<RowRead> createStringValuePredicate(final int colIdx,
        final RowFilter3NodeSettings settings) throws InvalidSettingsException {
        final var predicate = buildStringPredicate(settings);
        return new DataValuePredicate(colIdx, row -> {
            final var value = row.getValue(colIdx);
            return predicate.test(value instanceof StringValue sv ? sv.getStringValue() : value.toString());
        });
    }

    static Predicate<String> buildStringPredicate(final RowFilter3NodeSettings settings)
        throws InvalidSettingsException {
        final UnaryOperator<String> normalize = settings.m_caseMatching == CaseMatching.CASESENSITIVE
            ? UnaryOperator.identity() : (s -> s.toLowerCase(Locale.ROOT));

        final var operator = settings.m_operator;

        if (operator == FilterOperator.EQ || operator == FilterOperator.NEQ) {
            final var comparisonValue = normalize.apply(settings.m_anchors.m_string.m_value);
            final var isNegated = operator == FilterOperator.NEQ;
            return cellValue -> {
                final var equal = normalize.apply(cellValue).equals(comparisonValue);
                return isNegated ? !equal : equal;
            };
        }

        if (operator == FilterOperator.WILDCARD || operator == FilterOperator.REGEX) {
            var pattern = settings.m_anchors.m_string.m_pattern;
            if (settings.m_operator == FilterOperator.WILDCARD) {
                pattern = WildcardToRegexUtil.wildcardToRegex(pattern);
            }
            var flags = Pattern.DOTALL | Pattern.MULTILINE;
            flags |= settings.m_caseMatching == CaseMatching.CASESENSITIVE ? 0 : Pattern.CASE_INSENSITIVE;
            var regex = Pattern.compile(pattern, flags);
            return cellValue -> regex.matcher(cellValue).matches();
        }

        throw new InvalidSettingsException("Unsupported operator for string condition: " + settings.m_operator);
    }

    private static LongPredicate buildLongPredicate(final RowFilter3NodeSettings settings)
        throws InvalidSettingsException {
        final var val = settings.m_anchors.m_integer.m_value;
        return switch (settings.m_operator) {
            case EQ -> l -> Long.compare(l, val) == 0;
            case NEQ -> l -> Long.compare(l, val) != 0;
            case LT -> l -> Long.compare(l, val) < 0;
            case LTE -> l -> Long.compare(l, val) <= 0;
            case GT -> l -> Long.compare(l, val) > 0;
            case GTE -> l -> Long.compare(l, val) >= 0;
            case BETWEEN -> {
                final var lb = settings.m_anchors.m_integer.m_bounds.m_lowerBound;
                final var ub = settings.m_anchors.m_integer.m_bounds.m_upperBound;
                yield l -> Long.compare(l, lb) >= 0 && Long.compare(l, ub) <= 0;
            }
            default -> throw new InvalidSettingsException(
                "Unexpected operator for integer numeric condition: " + settings.m_operator);
        };
    }

    private static DoublePredicate buildDoublePredicate(final RowFilter3NodeSettings settings)
        throws InvalidSettingsException {
        final var val = settings.m_anchors.m_real.m_value;

        return switch (settings.m_operator) {
            case EQ -> d -> Double.compare(d, val) == 0;
            case NEQ -> d -> Double.compare(d, val) != 0;
            case LT -> d -> Double.compare(d, val) < 0;
            case LTE -> d -> Double.compare(d, val) <= 0;
            case GT -> d -> Double.compare(d, val) > 0;
            case GTE -> d -> Double.compare(d, val) >= 0;
            case BETWEEN -> {
                final var lb = settings.m_anchors.m_real.m_bounds.m_lowerBound;
                final var ub = settings.m_anchors.m_real.m_bounds.m_upperBound;
                yield d -> Double.compare(d, lb) >= 0 && Double.compare(d, ub) <= 0;
            }
            default -> throw new InvalidSettingsException(
                "Unexpected operator for real numeric condition: " + settings.m_operator);
        };
    }

}
