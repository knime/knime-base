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

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiPredicate;

import org.knime.base.data.filter.row.v2.IndexedRowReadPredicate;
import org.knime.base.data.filter.row.v2.OffsetFilter;
import org.knime.base.node.preproc.filter.row3.FilterOperator;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparatorDelegator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicValuesInput;

/**
 * Predicate factory for ordering predicates (<, <=, >, >=). If possible, the predicate is specialized to primitive
 * values.
 *
 * For comparison of row numbers on numeric values, see {@link RowNumberFilterSpec} and {@link OffsetFilter}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
abstract class OrderingPredicateFactory extends AbstractPredicateFactory {

    enum Ordering {
            LT, LTE, GT, GTE;

        static Optional<Ordering> fromOperator(final FilterOperator operator) {
            return Optional.ofNullable(switch (operator) {
                case LT -> Ordering.LT;
                case LTE -> Ordering.LTE;
                case GT -> Ordering.GT;
                case GTE -> Ordering.GTE;
                default -> null;
            });
        }
    }

    protected final Ordering m_ordering;

    private OrderingPredicateFactory(final Ordering ordering) {
        m_ordering = ordering;
    }

    /**
     * Utility method to create an exception with a nice message mentioning the problem and providing potential
     * resolutions based on the passed types.
     *
     * @param rowNumberOrColumn "row number" or "input column" to put into error message summary
     * @param type the actual type
     * @param refCellType the reference cell type
     * @param supportedTypes supported types
     * @return the exception
     */
    protected static InvalidSettingsException createInvalidSettingsException(final String rowNumberOrColumn,
        final DataType type, final DataType refCellType, final DataType[] supportedTypes) {
        return createInvalidSettingsException(builder -> builder //
            .withSummary("Cannot apply ordering comparison to %s of type \"%s\" and reference value of type \"%s\""
                .formatted(rowNumberOrColumn, type, refCellType)) //
            .addResolutions( //
                appendElements(new StringBuilder("Reconfigure the node to use a reference value of type "),
                    supportedTypes).toString(),
                "Convert the %s to \"%s\" before applying the filter, e.g. with an expression node."
                    .formatted(rowNumberOrColumn, refCellType)) //
        );
    }

    /**
     * Factory method for column predicate factories.
     * @param columnDataType column data type
     * @param operator filter operator to use
     * @return factory for predicates applying the given operator
     */
    static Optional<PredicateFactory> create(final DataType columnDataType, final FilterOperator operator) {
        return Ordering.fromOperator(operator).flatMap(ordering -> mapToFactory(columnDataType, ordering));
    }

    private static Optional<PredicateFactory> mapToFactory(final DataType columnDataType, final Ordering ordering) {
        final var preferredValueClass = columnDataType.getPreferredValueClass();
        if (BooleanValue.class.equals(preferredValueClass)) {
            // Booleans have only IS_TRUE and IS_FALSE as operators
            return Optional.empty();
        }
        // Strings were orderable in the intial release, but since it's not bounded it's hidden

        if (IntValue.class.equals(preferredValueClass)) {
            return Optional.of(new OrderingIntPredicateFactory(ordering));
        }
        if (LongValue.class.equals(preferredValueClass)) {
            return Optional.of(new OrderingLongPredicateFactory(ordering));
        }
        if (DoubleValue.class.equals(preferredValueClass)) {
            return Optional.of(new OrderingDoublePredicateFactory(ordering));
        }
        return Optional.of(new OrderingDataValuePredicateFactory(ordering, columnDataType));
    }

    protected abstract IndexedRowReadPredicate createPredicate(final int columnIndex,
        final DynamicValuesInput inputValues) throws InvalidSettingsException;

    @Override
    public IndexedRowReadPredicate createPredicate(final OptionalInt columnIndex, final DynamicValuesInput inputValues)
        throws InvalidSettingsException {
        return createPredicate(columnIndex.orElseThrow(
            () -> new IllegalStateException("Ordering predicate operates on column but did not get a column index")),
            inputValues);
    }

    /**
     * Predicates on {@link IntValue} colums with compatible reference values.
     */
    private static final class OrderingIntPredicateFactory extends OrderingPredicateFactory {

        private OrderingIntPredicateFactory(final Ordering ordering) {
            super(ordering);
        }

        @Override
        public IndexedRowReadPredicate createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {

            final var refCell = getCellAtOrThrow(inputValues, 0);
            if (refCell instanceof IntCell intCell) {
                final var ref = intCell.getIntValue();
                final var predicate = IntOrderingPredicate.create(ref, m_ordering);
                return (i, rowRead) -> predicate.test(rowRead.<IntValue> getValue(columnIndex).getIntValue());
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
            throw createInvalidSettingsException("input column", IntCell.TYPE, refCellType,
                new DataType[]{IntCell.TYPE, LongCell.TYPE, DoubleCell.TYPE});
        }

        private IndexedRowReadPredicate comparingWithLongValue(final int intColumnIndex, final long ref) {
            // [performance optimization]: take Integer max/min values into account to return static TRUE/FALSE
            //                             and then use int predicate for remaining comparison

            // the column is an IntValue, but the reference is a long, so we need to upcast the column value
            final var predicate = LongOrderingPredicate.create(ref, m_ordering);
            return (idx, rowRead) -> predicate.test(rowRead.<IntValue> getValue(intColumnIndex).getIntValue());
        }

        private IndexedRowReadPredicate comparingWithDoubleValue(final int intColumnIndex, final double reference) {
            // [performance optimization]: take Integer max/min values into account to return static TRUE/FALSE
            //                             We might then be able to round the reference depending on the operator
            //                             and use the int predicate.

            // the column is an IntValue, but the reference is a double, so we need to upcast the column value
            final var predicate = DoubleOrderingPredicate.create(reference, m_ordering);
            return (idx, rowRead) -> predicate.test(rowRead.<IntValue> getValue(intColumnIndex).getIntValue());
        }

    }

    /**
     * Predicates on {@link LongValue} colums with compatible reference values.
     */
    private static final class OrderingLongPredicateFactory extends OrderingPredicateFactory {

        private OrderingLongPredicateFactory(final Ordering ordering) {
            super(ordering);
        }

        @Override
        public IndexedRowReadPredicate createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            final var refCell = getCellAtOrThrow(inputValues, 0);
            if (refCell instanceof IntCell intRef) {
                final var ref = intRef.getIntValue();
                return comparingWithIntValue(columnIndex, ref);
            }

            if (refCell instanceof LongCell longRef) {
                final var ref = longRef.getLongValue();
                return comparingWithLongValue(columnIndex, ref);
            }

            // backwards-compatibility: allow this comparison, even though it is possibly lossy
            if (refCell instanceof DoubleCell doubleRef) {
                // we should log a warning at the node here, but we currently don't have access to the node model
                final var ref = doubleRef.getDoubleValue();
                return comparingWithDoubleValue(columnIndex, ref);
            }

            final var refCellType = refCell.getType();
            throw createInvalidSettingsException("input column", LongCell.TYPE, refCellType,
                new DataType[]{IntCell.TYPE, LongCell.TYPE, DoubleCell.TYPE});
        }

        private IndexedRowReadPredicate comparingWithIntValue(final int columnIndex, final int ref) {
            // [performance optimization]: take domain into account
            // need upcast int to long
            final var predicate = LongOrderingPredicate.create(ref, m_ordering);
            return (idx, rowRead) -> predicate.test(rowRead.<LongValue> getValue(columnIndex).getLongValue());
        }

        private IndexedRowReadPredicate comparingWithLongValue(final int columnIndex, final long ref) {
            final var predicate = LongOrderingPredicate.create(ref, m_ordering);
            return (idx, rowRead) -> predicate.test(rowRead.<LongValue> getValue(columnIndex).getLongValue());
        }

        private IndexedRowReadPredicate comparingWithDoubleValue(final int columnIndex, final double ref) {
            // backwards-compatibility: previous versions allowed to compare Long columns with double reference values
            final var predicate = DoubleOrderingPredicate.create(ref, m_ordering);
            return (idx, rowRead) -> predicate.test(rowRead.<DoubleValue> getValue(columnIndex).getDoubleValue());
        }
    }

    /**
     * Predicates on {@link DoubleValue} colums with compatible reference values.
     */
    private static final class OrderingDoublePredicateFactory extends OrderingPredicateFactory {

        private OrderingDoublePredicateFactory(final Ordering ordering) {
            super(ordering);
        }

        @Override
        public IndexedRowReadPredicate createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            final var refCell = getCellAtOrThrow(inputValues, 0);
            double ref;
            if (refCell instanceof IntCell intCell) {
                ref = intCell.getIntValue();
            } else if (refCell instanceof DoubleCell doubleCell) {
                ref = doubleCell.getDoubleValue();
            } else {
                // Note: disallowing "double column x long ref" is inconsistent with allowing "long column x double ref"
                // but we allow the latter only because of backwards compatibility with previous versions.
                final var refCellType = refCell.getType();
                throw createInvalidSettingsException("input column", DoubleCell.TYPE, refCellType,
                    new DataType[]{IntCell.TYPE, DoubleCell.TYPE});
            }
            final var predicate = DoubleOrderingPredicate.create(ref, m_ordering);
            return (idx, rowRead) -> predicate.test(rowRead.<DoubleValue> getValue(columnIndex).getDoubleValue());
        }

    }

    /**
     * Predicates on {@link DataValue} using the data value type's comparator (then the value types of the column and
     * reference must be the same).
     */
    private static final class OrderingDataValuePredicateFactory extends OrderingPredicateFactory {

        private final Comparator<DataValue> m_comparator;

        private final DataType m_columnType;

        OrderingDataValuePredicateFactory(final Ordering ordering, final DataType columnDataType) {
            super(ordering);
            m_columnType = columnDataType;
            m_comparator = new DataValueComparatorDelegator<>(columnDataType.getComparator());
        }

        @Override
        public IndexedRowReadPredicate createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            final var refCell = getCellAtOrThrow(inputValues, 0);
            final var refType = refCell.getType();
            if (!m_columnType.isASuperTypeOf(refType)) {
                throw createInvalidSettingsException(builder -> builder //
                    .withSummary("Column type \"%s\" is not a super type of reference type \"%s\""
                        .formatted(m_columnType, refType))
                    .addResolutions(
                        "Reconfigure the node to provide a reference value of type \"%s\".".formatted(m_columnType))
                    );
            }
            final BiPredicate<DataValue, DataValue> comparator = switch (m_ordering) {
                case LT -> (a, b) -> m_comparator.compare(a, b) < 0;
                case LTE -> (a, b) -> m_comparator.compare(a, b) <= 0;
                case GT -> (a, b) -> m_comparator.compare(a, b) > 0;
                case GTE -> (a, b) -> m_comparator.compare(a, b) >= 0;
            };
            return (idx, rowRead) -> comparator.test(rowRead.getValue(columnIndex), refCell);
        }

    }

    interface IntOrderingPredicate {
        /**
         * @param value the value to test
         * @return {@code true} if the value satisfies the ordering predicate, {@code false} otherwise
         */
        boolean test(int value);

        /**
         * Creates a ordering predicate for the given bound and operator.
         *
         * @param bound the bound to compare against
         * @param operator the ordering operator
         * @return the ordering predicate
         */
        static IntOrderingPredicate create(final int bound, final Ordering operator) {
            return switch (operator) {
                case LT -> v -> v < bound;
                case LTE -> v -> v <= bound;
                case GT -> v -> v > bound;
                case GTE -> v -> v >= bound;
            };
        }
    }

    interface LongOrderingPredicate {
        /**
         * @param value the value to test
         * @return {@code true} if the value satisfies the ordering predicate, {@code false} otherwise
         */
        boolean test(long value);

        /**
         * Creates a ordering predicate for the given bound and operator.
         *
         * @param bound the bound to compare against
         * @param operator the ordering operator
         * @return the ordering predicate
         */
        static LongOrderingPredicate create(final long bound, final Ordering operator) {
            return switch (operator) {
                case LT -> v -> v < bound;
                case LTE -> v -> v <= bound;
                case GT -> v -> v > bound;
                case GTE -> v -> v >= bound;
            };
        }
    }

    interface DoubleOrderingPredicate {
        /**
         * @param value the value to test
         * @return {@code true} if the value satisfies the ordering predicate, {@code false} otherwise
         */
        boolean test(double value);

        /**
         * Creates a ordering predicate for the given bound and operator.
         *
         * @param bound the bound to compare against
         * @param operator the ordering operator
         * @return the ordering predicate
         */
        static DoubleOrderingPredicate create(final double bound, final Ordering operator) {
            // use Double.compare for NaN handling
            return switch (operator) {
                case LT -> v -> Double.compare(v, bound) < 0;
                case LTE -> v -> Double.compare(v, bound) <= 0;
                case GT -> v -> Double.compare(v, bound) > 0;
                case GTE -> v -> Double.compare(v, bound) >= 0;
            };
        }
    }

}
