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
 *   27 Aug 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.predicates;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.knime.base.node.preproc.filter.row3.FilterOperator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparatorDelegator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.message.Message;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;

/**
 * Predicate factory for range predicates (<, <=, >, >=). If possible, the predicate is specialized to primitive values.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
abstract class RangePredicateFactory extends AbstractPredicateFactory {

    private enum RangeOperator {
            LT, LTE, GT, GTE;
    }

    protected final RangeOperator m_operator;

    private RangePredicateFactory(final FilterOperator operator) {
        m_operator = switch (operator) {
            case LT -> RangeOperator.LT;
            case LTE -> RangeOperator.LTE;
            case GT -> RangeOperator.GT;
            case GTE -> RangeOperator.GTE;
            default -> throw new IllegalArgumentException("Operator not supported: " + operator);
        };
    }

    static Optional<PredicateFactory> create(final DataType columnDataType, final FilterOperator operator) {
        final var isSupported = switch (operator) {
            case LT, LTE, GT, GTE -> true;
            default -> false;
        };
        if (!isSupported) {
            return Optional.empty();
        }
        final PredicateFactory factory;
        final var preferredValueClass = columnDataType.getPreferredValueClass();
        if (preferredValueClass.equals(IntValue.class)) {
            factory = new RangeIntPredicateFactory(operator);
        } else if (preferredValueClass.equals(LongValue.class)) {
            factory = new RangeLongPredicateFactory(operator);
        } else if (preferredValueClass.equals(DoubleValue.class)) {
            factory = new RangeDoublePredicateFactory(operator);
        } else {
            factory = new RangeDataValuePredicateFactory(operator, columnDataType);
        }
        return Optional.of(factory);
    }

    private static final class RangeIntPredicateFactory extends RangePredicateFactory {

        private RangeIntPredicateFactory(final FilterOperator operator) {
            super(operator);
        }

        @Override
        public Predicate<RowRead> createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            final var refCell = getCellAtOrThrow(inputValues, 0);
            if (refCell instanceof IntCell intCell) {
                // comparing Integer column with int value
                final var ref = intCell.getIntValue();
                final var predicate = IntRangePredicate.create(ref, m_operator);
                return rowRead -> predicate.test(rowRead.<IntValue> getValue(columnIndex).getIntValue());
            }

            if (refCell instanceof LongCell longCell) {
                // comparing Integer column with long value
                final var ref = longCell.getLongValue();
                return comparingWithLongValue(columnIndex, ref);
            }

            if (refCell instanceof DoubleCell doubleCell) {
                // comparing Integer column with double value
                final var ref = doubleCell.getDoubleValue();
                return comparingWithDoubleValue(columnIndex, ref);
            }

            final var refCellType = refCell.getType();
            throw Message.builder() //
                .withSummary("Cannot apply range comparison to column of type \"%s\" and reference value of type \"%s\""
                    .formatted(IntCell.TYPE, refCellType)) //
                .addResolutions( //
                    "Reconfigure the node to use a reference value of type \"%s\", \"%s\", or \"%s\"" //
                        .formatted(IntCell.TYPE, LongCell.TYPE, DoubleCell.TYPE), //
                    "Convert the input column to \"%s\" before applying the filter, e.g. with an expression node."
                        .formatted(refCellType)) //
                .build().orElseThrow().toInvalidSettingsException();
        }

        private Predicate<RowRead> comparingWithLongValue(final int intColumnIndex, final long ref) {
            // TODO(performance): take Integer max/min values into account to return static ALWAYS_TRUE/ALWAYS_FALSE
            //                    and then use int predicate for remaining comparison

            // the column is an IntValue, but the reference is a long, so we need to upcast the column value
            return rowRead -> LongRangePredicate.create(ref, m_operator)
                .test(rowRead.<IntValue> getValue(intColumnIndex).getIntValue());
        }

        private Predicate<RowRead> comparingWithDoubleValue(final int intColumnIndex, final double reference) {
            // TODO(performance): take Integer max/min values into account to return static ALWAYS_TRUE/ALWAYS_FALSE
            //                    We might then be able to round the reference depending on the operator
            //                    and use the int predicate.

            // the column is an IntValue, but the reference is a double, so we need to upcast the column value
            return rowRead -> DoubleRangePredicate.create(reference, m_operator)
                .test(rowRead.<IntValue> getValue(intColumnIndex).getIntValue());
        }

    }

    private static final class RangeLongPredicateFactory extends RangePredicateFactory {

        private RangeLongPredicateFactory(final FilterOperator operator) {
            super(operator);
        }

        @Override
        public Predicate<RowRead> createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            throw new UnsupportedOperationException("Not yet implemented");
        }

    }

    private static final class RangeDoublePredicateFactory extends RangePredicateFactory {

        private RangeDoublePredicateFactory(final FilterOperator operator) {
            super(operator);
        }

        @Override
        public Predicate<RowRead> createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            throw new UnsupportedOperationException("Not yet implemented");
        }

    }

    private static final class RangeDataValuePredicateFactory extends RangePredicateFactory {

        private final Comparator<DataValue> m_comparator;

        private final DataType m_columnType;

        RangeDataValuePredicateFactory(final FilterOperator operator, final DataType columnDataType) {
            super(operator);
            m_columnType = columnDataType;
            m_comparator = new DataValueComparatorDelegator<>(columnDataType.getComparator());
        }

        @Override
        public Predicate<RowRead> createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            final var isRowKey = columnIndex < 0;
            if (isRowKey) {
                throw Message.builder().withSummary("Cannot compare RowIDs by range value")
                    .addResolutions("Convert the RowID into a data column before applying the filter.").build()
                    .orElseThrow().toInvalidSettingsException();
            }
            final var refCell = getCellAtOrThrow(inputValues, 0);
            final var refType = refCell.getType();
            if (!m_columnType.isASuperTypeOf(refType)) {
                throw Message.builder()
                    .withSummary("Reference value type \"%s\" does not match column type \"%s\"".formatted(refType,
                        m_columnType))
                    .addResolutions(
                        "Reconfigure the node to provide a reference value of type \"%s\".".formatted(m_columnType))
                    .build().orElseThrow().toInvalidSettingsException();
            }
            final BiPredicate<DataValue, DataValue> comparator = switch (m_operator) {
                case LT -> (a, b) -> m_comparator.compare(a, b) < 0;
                case LTE -> (a, b) -> m_comparator.compare(a, b) <= 0;
                case GT -> (a, b) -> m_comparator.compare(a, b) > 0;
                case GTE -> (a, b) -> m_comparator.compare(a, b) >= 0;
            };
            return rowRead -> comparator.test(rowRead.getValue(columnIndex), refCell);
        }

    }

    interface IntRangePredicate {
        /**
         * @param value the value to test
         * @return {@code true} if the value satisfies the range predicate, {@code false} otherwise
         */
        boolean test(int value);

        /**
         * Creates a range predicate for the given bound and operator.
         *
         * @param bound the bound to compare against
         * @param operator the range operator
         * @return the range predicate
         */
        static IntRangePredicate create(final int bound, final RangeOperator operator) {
            return switch (operator) {
                case LT -> v -> v < bound;
                case LTE -> v -> v <= bound;
                case GT -> v -> v > bound;
                case GTE -> v -> v >= bound;
            };
        }
    }

    interface LongRangePredicate {
        /**
         * @param value the value to test
         * @return {@code true} if the value satisfies the range predicate, {@code false} otherwise
         */
        boolean test(long value);

        /**
         * Creates a range predicate for the given bound and operator.
         *
         * @param bound the bound to compare against
         * @param operator the range operator
         * @return the range predicate
         */
        static LongRangePredicate create(final long bound, final RangeOperator operator) {
            return switch (operator) {
                case LT -> v -> v < bound;
                case LTE -> v -> v <= bound;
                case GT -> v -> v > bound;
                case GTE -> v -> v >= bound;
            };
        }
    }

    interface DoubleRangePredicate {
        /**
         * @param value the value to test
         * @return {@code true} if the value satisfies the range predicate, {@code false} otherwise
         */
        boolean test(double value);

        /**
         * Creates a range predicate for the given bound and operator.
         *
         * @param bound the bound to compare against
         * @param operator the range operator
         * @return the range predicate
         */
        static DoubleRangePredicate create(final double bound, final RangeOperator operator) {
            return switch (operator) {
                case LT -> v -> v < bound;
                case LTE -> v -> v <= bound;
                case GT -> v -> v > bound;
                case GTE -> v -> v >= bound;
            };
        }
    }

}
