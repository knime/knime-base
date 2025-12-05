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
 *   16 Dec 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.rownumber;

import java.util.Collection;
import java.util.function.LongFunction;

import org.knime.base.data.filter.row.v2.FilterPartition;
import org.knime.base.data.filter.row.v2.OffsetFilter;
import org.knime.base.node.preproc.filter.row3.FilterMode;
import org.knime.base.node.preproc.filter.row3.operators.FilterOperatorsUtil;
import org.knime.base.node.preproc.filter.row3.operators.legacy.LegacyFilterOperator;
import org.knime.base.node.preproc.filter.row3.operators.legacy.LegacyFilterParameters;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;

/**
 * Subset of filter operators that represent a numeric filter on the row number.
 */
@SuppressWarnings("restriction") // webui
public final class RowNumberFilterSpec {

    /**
     * Operators that produce an {@link OffsetFilter}, i.e. work with a numeric row number.
     */
    enum Operator {
            EQ, NEQ, LT, LTE, GT, GTE, FIRST_N_ROWS, LAST_N_ROWS
    }

    static final long UNKNOWN_SIZE = -1;

    // visible for testing
    final Operator m_operator;

    private final long m_value;

    /**
     * Creates a row number filter specification. Must be a positive 1-based row number for all but first/last n rows.
     *
     * @param operator row number filter operator
     * @param value 1-based row number or number of last/first rows
     * @throws InvalidSettingsException in case the value is out of range for the given operator
     */
    RowNumberFilterSpec(final Operator operator, final long value) throws InvalidSettingsException {
        m_operator = operator;
        switch (operator) {
            case LAST_N_ROWS, FIRST_N_ROWS -> CheckUtils.checkSetting(value >= 0,
                "Number of first/last rows must be non-negative: %d", value);
            case EQ, NEQ, LT, LTE, GT, GTE -> CheckUtils.checkSetting(value > 0,
                "Row number value must be positive: %d", value);
        }
        m_value = value;
    }

    /**
     * Creates a row number filter specification from legacy filter parameters using a 1-based row number for all but
     * first/last n rows.
     *
     * @param operator legacy filter operator
     * @param value 1-based row number value or number of last/first rows
     * @throws InvalidSettingsException in case the legacy operator is not supported for row number filtering
     */
    @SuppressWarnings("deprecation") // for now we still use the legacy class
    public RowNumberFilterSpec(final LegacyFilterOperator operator, final long value) throws InvalidSettingsException {
        // map subset of supported legacy operators to offset-based filter
        this(fromLegacy(operator), value);
    }

    @SuppressWarnings("deprecation") // for now we still use the legacy class
    private static Operator fromLegacy(final LegacyFilterOperator operator) throws InvalidSettingsException {
        return switch (operator) {
            case EQ -> Operator.EQ;
            // row number cannot be missing, but we allow it for backwards-compatibility
            case NEQ, NEQ_MISS -> Operator.NEQ;
            case LT -> Operator.LT;
            case LTE -> Operator.LTE;
            case GT -> Operator.GT;
            case GTE -> Operator.GTE;
            case FIRST_N_ROWS -> Operator.FIRST_N_ROWS;
            case LAST_N_ROWS -> Operator.LAST_N_ROWS;
            // not supported as offset-based row number filter
            case IS_FALSE, IS_TRUE, IS_MISSING, IS_NOT_MISSING, REGEX, WILDCARD -> throw new InvalidSettingsException(
                "Cannot use operator \"%s\" to filter by row number".formatted(operator));
        };
    }

    /**
     * Computes the index operator and offset given the filter operator and value.
     *
     * @param optionalTableSize table size if known
     * @return index operator and index value (non-negative offset)
     */
    public OffsetFilter toOffsetFilter(final long optionalTableSize) {
        // the dialog accepts 1-based row numbers but we use 0-based row offsets internally
        return switch (m_operator) {
            case EQ -> new OffsetFilter(OffsetFilter.Operator.EQ, m_value - 1);
            case NEQ -> new OffsetFilter(OffsetFilter.Operator.NEQ, m_value - 1);
            case LT -> new OffsetFilter(OffsetFilter.Operator.LT, m_value - 1);
            case LTE -> new OffsetFilter(OffsetFilter.Operator.LTE, m_value - 1);
            case GT -> new OffsetFilter(OffsetFilter.Operator.GT, m_value - 1);
            case GTE -> new OffsetFilter(OffsetFilter.Operator.GTE, m_value - 1);
            case FIRST_N_ROWS -> new OffsetFilter(OffsetFilter.Operator.LT, m_value);
            case LAST_N_ROWS -> {
                CheckUtils.checkState(optionalTableSize != UNKNOWN_SIZE, //
                    "Expected table size for filter operator \"%s\"", m_operator);
                // if the table has fewer than `n` rows, return the whole table
                yield new OffsetFilter(OffsetFilter.Operator.GTE, Math.max(0, optionalTableSize - m_value));
            }
        };
    }

    /**
     * Creates a function that computes the filter partition for a given table size.
     *
     * @return function from table size to filter partition
     */
    public LongFunction<FilterPartition> toFilterPartition() {
        return tableSize -> FilterPartition.computePartition(toOffsetFilter(tableSize), tableSize);
    }

    /**
     * Combines multiple row number filters into a single partition, either by AND or OR.
     *
     * @param isAnd whether to combine by AND (true) or OR (false)
     * @param rowNumberFilters list of row number filter functions
     * @param outputMode whether the output should contain matching or non-matching rows
     * @param optionalTableSize the table size or -1 if unknown
     * @return combined filter partition
     */
    public static FilterPartition computeRowPartition(final boolean isAnd,
        final Collection<LongFunction<FilterPartition>> rowNumberFilters, final FilterMode outputMode,
        final long optionalTableSize) {
        final var matchedNonMatchedPartition = rowNumberFilters.stream() //
            .map(createPartition -> createPartition.apply(optionalTableSize)) //
            .reduce((a, b) -> a.combine(isAnd, b)) //
            .orElseThrow(() -> new IllegalArgumentException("Need at least one filter criterion"));
        // determine whether matched or non-matched rows are included in the first output, flip pair as needed
        return outputMode == FilterMode.MATCHING ? matchedNonMatchedPartition : matchedNonMatchedPartition.swapped();
    }

    /**
     * Transforms the parameters into a partition function.
     *
     * @param operatorId the operator id
     * @param filterValueParameters suitable parameters
     *
     * @return row number filter specification
     * @throws InvalidSettingsException if the filter criterion contains an unsupported operator or the value is missing
     */
    public static LongFunction<FilterPartition> toPartitionFunction(final String operatorId,
        final FilterValueParameters filterValueParameters) throws InvalidSettingsException {
        if (filterValueParameters instanceof LegacyFilterParameters legacyParameters) {
            return legacyParameters.toFilterSpec().toFilterPartition();
        }
        final var matchingOperator =
            FilterOperatorsUtil.findMatchingRowNumberOperator(operatorId).orElseThrow(IllegalStateException::new);
        return toSliceFilter(matchingOperator, filterValueParameters);
    }

    private static <P extends FilterValueParameters> LongFunction<FilterPartition> toSliceFilter(
        final RowNumberFilterOperator<P> matchingOperator, final FilterValueParameters filterValueParameters)
        throws InvalidSettingsException {
        return matchingOperator.createSliceFilter((P)filterValueParameters);
    }

}
