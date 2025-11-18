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
package org.knime.base.node.preproc.filter.row3.operators.legacy;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;

import org.knime.base.data.filter.row.v2.IndexedRowReadPredicate;
import org.knime.base.data.filter.row.v2.OffsetFilter;
import org.knime.base.node.preproc.filter.row3.operators.rownumber.RowNumberFilterSpec;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

/**
 * Factory for equality predicates ({@code EQ}, {@code NEQ}). The actual implementation is chosen based on the passsed
 * data type.
 *
 * For comparison of row numbers on numeric values, see {@link RowNumberFilterSpec} and {@link OffsetFilter}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("deprecation") // legacy DynamicValuesInput
abstract class EqualityPredicateFactory extends AbstractPredicateFactory {

    /**
     * {@code true} if the predicate should match "equals", {@code false} if it should match "not equals".
     */
    protected final boolean m_matchEqual;

    private EqualityPredicateFactory(final boolean matchEqual) {
        m_matchEqual = matchEqual;
    }

    protected static InvalidSettingsException createInvalidSettingsException(final String rowNumberOrColumn,
        final DataType type, final DataType refCellType, final boolean isEquals, final DataType... supportedTypes) {
        return createInvalidSettingsException(builder -> builder //
            .withSummary("Cannot compare %s of type \"%s\" with a value of type \"%s\" for %sequality"
                .formatted(rowNumberOrColumn, type, refCellType, isEquals ? "" : "in"))
            .addResolutions(
                appendElements(new StringBuilder("Reconfigure the node to use a reference value of type "),
                    supportedTypes).toString(),
                "Convert the %s to \"%s\" before applying the filter using a converter node, e.g. an expression node"
                    .formatted(rowNumberOrColumn, refCellType))
        );
    }

    /**
     * Creates an equality predicate for row keys.
     *
     * @param matchEqual {@code true} if the equality predicate should match "equal", {@code false} if it should match
     *            "not equal"
     * @return factory for row key equality predicates
     */
    static Optional<PredicateFactory> forRowKey(final boolean matchEqual) {
        return Optional.of(new EqualityRowKeyPredicateFactory(matchEqual));
    }

    /**
     * Creates an equality predicate factory for the given column data type.
     *
     * @param columnDataType data type of the column to compare
     * @param matchEqual {@code true} if the equality predicate should match "equals", {@code false} if it should match
     *            "not equals"
     * @return factory for the given data type, or empty optional if the data type is not supported
     */
    static Optional<PredicateFactory> create(final DataType columnDataType, final boolean matchEqual) {
        final var preferredValueClass = columnDataType.getPreferredValueClass();

        if (BooleanValue.class.equals(preferredValueClass)) {
            // Boolean has IS_TRUE and IS_FALSE operators and does not use these predicate factories
            return Optional.empty();
        }

        final PredicateFactory predicateFactory;
        if (LongValue.class.equals(preferredValueClass)) {
            predicateFactory = new EqualityLongPredicateFactory(matchEqual);
        } else if (IntValue.class.equals(preferredValueClass)) {
            predicateFactory = new EqualityIntPredicateFactory(matchEqual);
        } else if (DoubleValue.class.equals(preferredValueClass)) {
            predicateFactory = new EqualityDoublePredicateFactory(matchEqual);
        } else if (preferredValueClass.equals(StringValue.class)) {
            predicateFactory = new EqualityStringPredicateFactory(matchEqual);
        } else {
            predicateFactory = new EqualityDataCellPredicateFactory(matchEqual);
        }
        return Optional.of(predicateFactory);
    }

    protected abstract IndexedRowReadPredicate createPredicate(int columnIndex, DynamicValuesInput inputValues)
        throws InvalidSettingsException;

    @Override
    public final IndexedRowReadPredicate createPredicate(final OptionalInt columnIndex,
        final DynamicValuesInput inputValues) throws InvalidSettingsException {
        final var columnIdx = columnIndex.orElseThrow(
            () -> new IllegalStateException("Equality predicate operates on column but did not get a column index"));
        CheckUtils.checkArgument(columnIdx >= 0,
            "Expected non-negative column index for equality predicate, but got %d", columnIndex);
        return createPredicate(columnIdx, inputValues);
    }

    private static final class EqualityDataCellPredicateFactory extends EqualityPredicateFactory {

        private EqualityDataCellPredicateFactory(final boolean matchEqual) {
            super(matchEqual);
        }

        @Override
        public IndexedRowReadPredicate createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            final var refCell = getCellAtOrThrow(inputValues, 0);
            return (idx, rowRead) -> //
                rowRead.getValue(columnIndex).materializeDataCell().equals(refCell) == m_matchEqual; //
        }

    }

    private static final class EqualityIntPredicateFactory extends EqualityPredicateFactory {

        private EqualityIntPredicateFactory(final boolean matchEqual) {
            super(matchEqual);
        }

        @Override
        public IndexedRowReadPredicate createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            final var refCell = getCellAtOrThrow(inputValues, 0);
            if (refCell instanceof IntCell intCell) {
                // comparing Integer column with int value
                final var ref = intCell.getIntValue();
                final var predicate = new IntEquality(ref, m_matchEqual);
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
            throw createInvalidSettingsException("input column", IntCell.TYPE, refCellType, m_matchEqual, IntCell.TYPE,
                LongCell.TYPE, DoubleCell.TYPE);
        }

        private IndexedRowReadPredicate comparingWithLongValue(final int columnIndex, final long ref) {
            // reference outside of int column domain, so it can never (or always) match
            if (ref < Integer.MIN_VALUE || ref > Integer.MAX_VALUE) {
                return m_matchEqual ? IndexedRowReadPredicate.FALSE : IndexedRowReadPredicate.TRUE;
            }

            // long value is inside of the int domain, so matches are possible
            final var predicate = new IntEquality((int)ref, m_matchEqual);
            return (i, rowRead) -> predicate.test(rowRead.<IntValue> getValue(columnIndex).getIntValue());
        }

        private IndexedRowReadPredicate comparingWithDoubleValue(final int columnIndex, final double ref) {
            // reference outside of int column domain, so it can never (or always) match
            if (ref < Integer.MIN_VALUE || ref > Integer.MAX_VALUE) {
                return m_matchEqual ? IndexedRowReadPredicate.FALSE : IndexedRowReadPredicate.TRUE;
            }

            // double value is inside of the int domain, so matches are possible
            final var predicate = new DoubleEquality(ref, m_matchEqual);
            return (i, rowRead) -> predicate.test(rowRead.<IntValue> getValue(columnIndex).getIntValue());
        }

    }

    private static final class EqualityLongPredicateFactory extends EqualityPredicateFactory {

        private EqualityLongPredicateFactory(final boolean matchEqual) {
            super(matchEqual);
        }

        @Override
        public IndexedRowReadPredicate createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            final var refCell = getCellAtOrThrow(inputValues, 0);
            long ref;
            if (refCell instanceof IntCell intCell) {
                ref = intCell.getIntValue();
            } else if (refCell instanceof LongCell longCell) {
                ref = longCell.getLongValue();
            } else {
                final var refCellType = refCell.getType();
                throw createInvalidSettingsException("input column", LongCell.TYPE, refCellType, m_matchEqual,
                    IntCell.TYPE, LongCell.TYPE);
            }
            final var predicate = new LongEquality(ref, m_matchEqual);
            return (i, rowRead) -> predicate.test(rowRead.<LongValue> getValue(columnIndex).getLongValue());
        }
    }

    private static final class EqualityDoublePredicateFactory extends EqualityPredicateFactory {

        private EqualityDoublePredicateFactory(final boolean matchEqual) {
            super(matchEqual);
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
                final var refCellType = refCell.getType();
                throw createInvalidSettingsException("input column", DoubleCell.TYPE, refCellType, m_matchEqual,
                    IntCell.TYPE, DoubleCell.TYPE);
            }
            final var predicate = new DoubleEquality(ref, m_matchEqual);
            return (i, rowRead) -> predicate.test(rowRead.<DoubleValue> getValue(columnIndex).getDoubleValue());
        }

    }

    private static final class EqualityStringPredicateFactory extends EqualityPredicateFactory {

        private EqualityStringPredicateFactory(final boolean matchEqual) {
            super(matchEqual);
        }

        @Override
        public IndexedRowReadPredicate createPredicate(final int columnIndex, final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            return getPredicate(m_matchEqual, inputValues, 0, "input column",
                r -> r.<StringValue> getValue(columnIndex).getStringValue());
        }

        private static IndexedRowReadPredicate getPredicate(final boolean matchEqual,
            final DynamicValuesInput inputValues, final int referenceValueIndex, final String rowKeyOrColumn,
            final Function<RowRead, String> valueFn) throws InvalidSettingsException {
            final var refCell = getCellAtOrThrow(inputValues, referenceValueIndex);
            final var refCellType = refCell.getType();
            if (!refCellType.isCompatible(StringValue.class)) {
                throw createInvalidSettingsException(rowKeyOrColumn, StringCell.TYPE, refCellType, matchEqual,
                    StringCell.TYPE);
            }
            final var refValue = ((StringValue)refCell).getStringValue();
            final var isCaseSensitive = inputValues.isStringMatchCaseSensitive(referenceValueIndex);
            final var pred = StringPredicate.equality(refValue, isCaseSensitive);

            return (i, rowRead) -> matchEqual == pred.test(valueFn.apply(rowRead));
        }

    }

    private static final class EqualityRowKeyPredicateFactory extends RowKeyPredicateFactory {

        private final boolean m_matchEqual;

        private EqualityRowKeyPredicateFactory(final boolean matchEqual) {
            m_matchEqual = matchEqual;
        }

        @Override
        protected IndexedRowReadPredicate createPredicate(final DynamicValuesInput inputValues)
            throws InvalidSettingsException {
            return EqualityStringPredicateFactory.getPredicate(m_matchEqual, inputValues, 0,
                "RowID", r -> r.getRowKey().getString());
        }

    }

    private static final class IntEquality implements IntPredicate {

        private final IntPredicate m_predicate;

        private IntEquality(final int ref, final boolean matchEqual) {
            m_predicate = matchEqual ? (i -> i == ref) : (i -> i != ref);
        }

        @Override
        public boolean test(final int value) {
            return m_predicate.test(value);
        }

    }

    private static final class LongEquality implements LongPredicate {

        private final LongPredicate m_predicate;

        private LongEquality(final long ref, final boolean matchEqual) {
            m_predicate = matchEqual ? (l -> l == ref) : (l -> l != ref);
        }

        @Override
        public boolean test(final long value) {
            return m_predicate.test(value);
        }

    }

    private static final class DoubleEquality implements DoublePredicate {

        private DoublePredicate m_predicate;

        private DoubleEquality(final double ref, final boolean matchEqual) {
            m_predicate = matchEqual ? (d -> Double.doubleToLongBits(d) == Double.doubleToLongBits(ref))
                : (d -> Double.doubleToLongBits(d) != Double.doubleToLongBits(ref));
        }

        @Override
        public boolean test(final double value) {
            return m_predicate.test(value);
        }

    }

}
