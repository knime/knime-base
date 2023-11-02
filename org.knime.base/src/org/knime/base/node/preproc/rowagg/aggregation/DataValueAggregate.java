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
 *   22 Dec 2022 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.rowagg.aggregation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.base.node.preproc.groupby.GroupByTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;

/**
 * Aggregate of {@link DataValue data values} for use in the {@link GroupByTable} operator framework.
 *
 * @param <T> weight column (if any)
 * @param <U> data column
 * @param <O> result of aggregate of {@code T x U}s
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class DataValueAggregate // NOSONAR
<T extends DataValue, U extends DataValue, O extends DataValue> extends AggregationOperator {

    private String m_weightColumnName;

    private String m_description;

    private BiFunction<DataType, DataType, BinaryAccumulator<U, T, O>> m_aggConstructor;

    private DataValueAggregate(final BiFunction<DataType, DataType, BinaryAccumulator<U, T, O>> aggConstructor,
        final String weightColumnName, final String description, final OperatorData operatorData,
        final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
        m_aggConstructor = aggConstructor;
        m_weightColumnName = weightColumnName;
        m_description = description;
    }

    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        // only null during registration, in which #createInstance is not invoked
        final var inType = Objects.requireNonNull(opColSettings.getOriginalColSpec()).getType();

        // no weighting
        if (m_weightColumnName == null) {
            return new DataValueAggregateImpl(-1, msg -> m_aggConstructor.apply(null, inType), getOperatorData(),
                globalSettings, opColSettings);
        }

        final var weightIndex = globalSettings.findColumnIndex(m_weightColumnName);
        final var weightType = globalSettings.getOriginalColumnSpec(m_weightColumnName).getType();

        return new DataValueAggregateImpl(weightIndex, msg -> m_aggConstructor.apply(weightType, inType),
            getOperatorData(), globalSettings, opColSettings);
    }

    @Override
    protected DataType getDataType(final DataType inType) {
        if (m_weightColumnName == null) {
            return m_aggConstructor.apply(null, inType).getResultType();
        }
        final var gs = getGlobalSettings();
        final var weightType = gs.getOriginalColumnSpec(m_weightColumnName).getType();
        return m_aggConstructor.apply(weightType, inType).getResultType();
    }

    @Override
    public String getDescription() {
        return m_description;
    }

    @Override
    public Collection<String> getAdditionalColumnNames() {
        return m_weightColumnName != null ? List.of(m_weightColumnName) : Collections.emptyList();
    }

    @Override
    protected boolean computeInternal(final DataCell cell) {
        throw new UnsupportedOperationException("Outer class instance cannot compute aggregates.");
    }

    @Override
    protected DataCell getResultInternal() {
        throw new UnsupportedOperationException("Outer class instance cannot compute result.");
    }

    @Override
    protected void resetInternal() {
        throw new UnsupportedOperationException("Outer class instance cannot be reset.");
    }

    private static final class SimpleBinaryAccumulator
        <U extends DataValue, T extends DataValue, R extends DataValue, O extends DataValue>
        implements BinaryAccumulator<U, T, O> {

        private final Combiner<T, U, R> m_combiner;

        private final Accumulator<R, O> m_agg;

        private SimpleBinaryAccumulator(final Combiner<T, U, R> combiner, final Accumulator<R, O> agg) {
            m_combiner = combiner;
            m_agg = agg;
        }

        @Override
        public boolean apply(final U value, final T weight) throws ArithmeticException {
            // as requested by documentation of `computeInternal(cell)`,
            // do something sensible in case the old method without weight is still invoked:
            // compute aggregate if unweighted, else fail
            if (m_combiner == null) {
                // U == R
                @SuppressWarnings("unchecked")
                final var res = m_agg.apply((R)value);

                return res;
            }
            if (weight == null) {
                throw new IllegalStateException("Cannot compute weighted aggregate without weight column.");
            }

            final var v = m_combiner.apply(weight, value);
            if (v.isEmpty()) {
                return true;
            }
            return m_agg.apply(v.get());
        }

        @Override
        public DataType getResultType() {
            return m_agg.getResultType();
        }

        @Override
        public Optional<O> getResult() {
            return m_agg.getResult();
        }

        @Override
        public void reset() {
            // no need to reset combiner
            m_agg.reset();
        }

    }

    /**
     * Nested class that represents the operator doing the actual aggregation work. This operator ignores missing cells
     * and reports a missing cell only if the input was effectively empty (i.e. ignoring a row if either of the two
     * columns considered contains a missing cell).
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    // nested class so we can separate the behavior which is used when registering the operator in the group-by/pivot
    // framework (e.g. spec is null) from the behavior which is used to actually aggregate values
    final class DataValueAggregateImpl extends AggregationOperator { // NOSONAR

        /**
         * This flag is set to true iff the operator has seen at least one non-missing input (i.e. where both considered
         * cells are non-missing cells). This is used so that operators that (effectively) did not see any input report
         * a missing cell.
         */
        private boolean m_init;

        private BinaryAccumulator<U, T, O> m_binAgg;

        private int m_weightColumnIndex;

        /**
         * Creates a new operator. For the weighted/unweighted cases it should hold:
         * {@code (weightColumnIndex < 0) <=> (combiner == null)}.
         *
         * @param weightColumnIndex column index to use for weighted aggregate, or {@code -1} if unweighted
         * @param combiner combiner to use for calculating weighted value, or {@code null} if unweighted
         * @param agg aggregate for data cells
         * @param operatorData operator data
         * @param globalSettings global settings
         * @param opColSettings operator column settings
         */
        private DataValueAggregateImpl(final int weightColumnIndex,
            final Function<Consumer<String>, BinaryAccumulator<U, T, O>> binAgg, final OperatorData operatorData,
            final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings) {
            super(operatorData, globalSettings, opColSettings);
            m_weightColumnIndex = weightColumnIndex;
            m_binAgg = binAgg.apply(this::setSkippedWithMessage);
        }

        @Override
        public Collection<String> getAdditionalColumnNames() {
            return DataValueAggregate.this.getAdditionalColumnNames();
        }

        @Override
        protected boolean computeInternal(final DataCell cell) {
            try {
                if (cell.isMissing()) {
                    // skip missing cells and proceed with column
                    return false;
                }
                m_init = true;
                @SuppressWarnings("unchecked")
                final var res = m_binAgg.apply((U)cell, null);
                return res;
            } catch (ArithmeticException e) {
                return handleOverflow(e);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected boolean computeInternal(final DataRow row, final DataCell cell) {
            try {
                if (cell.isMissing()) {
                    // skip missing cells and proceed with column
                    return false;
                }
                if (m_weightColumnIndex < 0) {
                    m_init = true;
                    return m_binAgg.apply((U)cell, null);
                }

                final var weight = row.getCell(m_weightColumnIndex);
                if (weight.isMissing()) {
                    // skip missing cells and proceed with column
                    return false;
                }
                m_init = true;
                return m_binAgg.apply((U)cell, (T)weight);
            } catch (final ArithmeticException e) { // NOSONAR
                return handleOverflow(e);
            }
        }

        private boolean handleOverflow(final ArithmeticException e) {
            final var msg = Optional.ofNullable(e.getMessage())
                    .or(() -> Optional.ofNullable(e.getCause()).map(Throwable::getMessage)).orElse(null);

            if (msg != null) {
                setSkipMessage(msg);
            } else {
                setSkipMessage("Could not aggregate values due to arithmetic exception");
            }
            // true indicates that the computation is skipped
            return true;
        }

        void setSkippedWithMessage(final String msg) {
            setSkipped(true);
            setSkipMessage(msg);
        }

        private static String getAggregateOverflowMessage(final DataType colType) {
            var msg = String.format("Numeric overflow of aggregation result for input type \"%s\".", colType);
            if (colType.equals(IntCell.TYPE)) {
                msg += String.format(" Consider converting the input column to \"%s\".", LongCell.TYPE);
            }
            return msg;
        }

        @Override
        protected DataType getDataType(final DataType origType) {
            return m_binAgg.getResultType();
        }

        @Override
        protected DataCell getResultInternal() {
            // check for empty input
            if (!m_init) {
                // agg of empty input is missing cell
                return DataType.getMissingCell();
            }
            // check if the aggregate did not overflow the accumulator type but _does_ overflow the result type
            // (if the accumulator type already overflowed, the individual call to #computeInternal already indicated
            // that the column can be skipped and this function would not get called)
            final var res = m_binAgg.getResult();
            if (res.isEmpty()) {
                setSkippedWithMessage(
                    getAggregateOverflowMessage(getOperatorColumnSettings().getOriginalColSpec().getType()));
                return DataType.getMissingCell();
            }
            return (DataCell)res.get();
        }

        @Override
        protected void resetInternal() {
            m_init = false;
            m_binAgg.reset();
        }

        @Override
        public String getDescription() {
            throw new UnsupportedOperationException("Internal operator has no description.");
        }

        @Override
        public AggregationOperator createInstance(final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
            throw new UnsupportedOperationException(
                "Internal operator does not support creation of an instance. Use" + " the outer class instance.");
        }

    }

    /**
     * Builder for {@link DataValueAggregate} instances.
     *
     * @param <T> weight column (if any)
     * @param <U> data column
     * @param <R> result of multiplication {@code T x U}
     * @param <O> result of aggregate of {@code R}s
     */
    public static final class Builder
        <T extends DataValue, U extends DataValue, R extends DataValue, O extends DataValue>
        implements WithOperatorInfo<T, U, R, O>, WithSupportedClass<T, U, R, O>, WithAggregate<T, U, R, O> {

        private String m_id;

        private String m_label;

        private String m_description;

        /** Supported type, needed to register operator in the aggregation framework. */
        private Class<? extends DataValue> m_suppClass;

        private String m_weightColumnName;

        private BiFunction<DataType, DataType, BinaryAccumulator<U, T, O>> m_binAggCreator;

        /** Should only be called from {@link #create()}. */
        private Builder() {
            // nothing to do
        }

        /**
         * Create the aggregate based on the configured options.
         *
         * @param gs global operator settings
         * @param ocs operator column settings
         * @return configured aggregate
         */
        public DataValueAggregate<T, U, O> build(final GlobalSettings gs, final OperatorColumnSettings ocs) {
            return new DataValueAggregate<>(m_binAggCreator, m_weightColumnName, m_description,
                new OperatorData(m_id, m_label, m_label, false, false, m_suppClass, false), gs, ocs);
        }

        @Override
        public WithSupportedClass<T, U, R, O> withOperatorInfo(final String id, final String label,
            final String description) {
            m_id = Objects.requireNonNull(id);
            m_label = Objects.requireNonNull(label);
            m_description = Objects.requireNonNull(description);
            return this;
        }

        @Override
        public Builder<T, U, R, O> withAggregate(final Function<DataType, Accumulator<R, O>> aggCreator) {
            return withWeightedAggregate(null, null, aggCreator);
        }

        @Override
        public Builder<T, U, R, O> withWeightedAggregate(final String weightColumnName,
            final BiFunction<DataType, DataType, Combiner<T, U, R>> combCreator,
            final Function<DataType, Accumulator<R, O>> aggCreator) {
            if (weightColumnName == null) {
                return withWeightedAggregate(weightColumnName,
                    (weightType, inType) -> new SimpleBinaryAccumulator<U, T, R, O>(null,
                        Objects.requireNonNull(aggCreator).apply(inType)));
            }
            return withWeightedAggregate(weightColumnName, (weightType, inType) -> {
                final var comb = combCreator.apply(weightType, inType);
                return new SimpleBinaryAccumulator<>(comb,
                    Objects.requireNonNull(aggCreator).apply(comb.getResultDataType()));
            });
        }

        @Override
        public Builder<T, U, R, O> withWeightedAggregate(final String weightColumnName,
            final BiFunction<DataType, DataType, BinaryAccumulator<U, T, O>> aggCreator) {
            m_weightColumnName = weightColumnName;
            m_binAggCreator = Objects.requireNonNull(aggCreator);
            return this;
        }

        @Override
        public WithAggregate<T, U, R, O> withSupportedClass(final Class<? extends DataValue> clazz) {
            m_suppClass = Objects.requireNonNull(clazz);
            return this;
        }
    }

    /**
     * Configuration options for operator data.
     *
     * @param <T> weight column (if any)
     * @param <U> data column
     * @param <R> result of multiplication {@code T x U}
     * @param <O> result of aggregate of {@code R}s
     */
    public interface WithOperatorInfo
        <T extends DataValue, U extends DataValue, R extends DataValue, O extends DataValue> {
        /**
         * Configure info of the operator.
         *
         * @param id operator id
         * @param label operator label
         * @param description operator description
         * @return builder instance
         */
        WithSupportedClass<T, U, R, O> withOperatorInfo(final String id, final String label, final String description);
    }

    /**
     * Configuration option for supported class.
     *
     * @param <T> weight column (if any)
     * @param <U> data column
     * @param <R> result of intermediate {@code T x U}
     * @param <O> result of aggregate of {@code R}s
     */
    public interface WithSupportedClass
        <T extends DataValue, U extends DataValue, R extends DataValue, O extends DataValue> {
        /**
         * Configure supported class. If your operator should support more than one data type, then configure it with a
         * supported class that all types are compatible with.
         *
         * @param clazz class of supported data type
         * @return builder instance
         */
        WithAggregate<T, U, R, O> withSupportedClass(final Class<? extends DataValue> clazz);
    }

    /**
     * Configuration option for aggregate.
     *
     * @param <T> weight column (if any)
     * @param <U> data column
     * @param <R> result of intermediate {@code T x U}
     * @param <O> result of aggregate of {@code R}s
     */
    public interface WithAggregate<T extends DataValue, U extends DataValue, R extends DataValue, O extends DataValue> {

        /**
         * Configure the aggregate operator using only an accumulator.
         *
         * @param aggCreator creator for accumulator given the input data type
         * @return builder instance
         */
        Builder<T, U, R, O> withAggregate(final Function<DataType, Accumulator<R, O>> aggCreator);

        /**
         * Configure the aggregate operator using a combiner and accumulator.
         *
         * @param weightColumnName column name to get data input for combiner
         * @param combCreator creator for combiner
         * @param aggCreator creator for accumulator
         * @return builder instance
         */
        Builder<T, U, R, O> withWeightedAggregate(final String weightColumnName,
            final BiFunction<DataType, DataType, Combiner<T, U, R>> combCreator,
            final Function<DataType, Accumulator<R, O>> aggCreator);

        /**
         * Configure binary accumulator for the aggregate operator.
         *
         * @param weightColumnName column name to get data input for
         * @param aggCreator creator for binary accumulator
         * @return builder instance
         */
        Builder<T, U, R, O> withWeightedAggregate(final String weightColumnName,
            final BiFunction<DataType, DataType, BinaryAccumulator<U, T, O>> aggCreator);
    }

    /**
     * Creates a builder for a {@link DataValueAggregate} instance.
     *
     * @param <T> weight column (if any)
     * @param <U> data column
     * @param <R> result of multiplication {@code T x U}
     * @param <O> result of aggregate of {@code R}s
     * @return the new builder instance
     */
    public static <T extends DataValue, U extends DataValue, R extends DataValue, O extends DataValue>
        WithOperatorInfo<T, U, R, O> create() {
        return new Builder<>();
    }
}
