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
 *   26 Aug 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.legacy;

import java.util.Optional;
import java.util.function.IntFunction;

import org.knime.base.data.filter.row.v2.IndexedRowReadPredicate;
import org.knime.base.data.filter.row.v2.OffsetFilter;
import org.knime.core.data.DataType;
import org.knime.core.data.v2.RowRead;

/**
 * Utility to create value predicate factories for indexed {@link RowRead row read} predicates. Notable factories are
 * {@link PredicateFactories#IS_MISSING_FACTORY} and {@link PredicateFactories#IS_NOT_MISSING_FACTORY}, which can be
 * used to test for missingness.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("deprecation")
public final class PredicateFactories {

    /**
     * Factory instance to create "is missing" predicates for a given column index.
     */
    public static final IntFunction<IndexedRowReadPredicate> IS_MISSING_FACTORY =
        columnIndex -> (idx, rowRead) -> rowRead.isMissing(columnIndex);

    /**
     * Factory instance to create "is not missing" predicates for a given column index.
     */
    public static final IntFunction<IndexedRowReadPredicate> IS_NOT_MISSING_FACTORY =
        columnIndex -> (idx, rowRead) -> !rowRead.isMissing(columnIndex);

    private PredicateFactories() {
        // hidden
    }

    /**
     * Gets a factory for the given operator and column data type. If the operator does not support the given data type,
     * the returned optional is empty.
     *
     * @param operator filter operator
     * @param dataType data type to apply operator on
     * @return factory for the given operator and column, or empty if the operator is not supported for the given data
     *         type
     */
    public static Optional<PredicateFactory> getValuePredicateFactory(final LegacyFilterOperator operator, // NOSONAR
        final DataType dataType) {
        return switch (operator) {
            case EQ:
                yield EqualityPredicateFactory.create(dataType, true);
            case NEQ, NEQ_MISS:
                yield EqualityPredicateFactory.create(dataType, false);
            case GT:
                yield OrderingPredicateFactory.create(dataType, LegacyFilterOperator.GT);
            case GTE:
                yield OrderingPredicateFactory.create(dataType, LegacyFilterOperator.GTE);
            case LT:
                yield OrderingPredicateFactory.create(dataType, LegacyFilterOperator.LT);
            case LTE:
                yield OrderingPredicateFactory.create(dataType, LegacyFilterOperator.LTE);
            case IS_TRUE:
                yield BooleanPredicateFactory.create(dataType, true);
            case IS_FALSE:
                yield BooleanPredicateFactory.create(dataType, false);
            case REGEX:
                yield PatternMatchingPredicateFactory.forColumn(dataType, true);
            case WILDCARD:
                yield PatternMatchingPredicateFactory.forColumn(dataType, false);
            case IS_MISSING, IS_NOT_MISSING, FIRST_N_ROWS, LAST_N_ROWS:
                yield Optional.empty();
        };
    }

    /**
     * Creates a factory for the given operator for row key predicates. If the operator does not support row keys,
     * the returned optional is empty.
     * @param operator filter operator
     * @return factory for the given operator, or empty if the operator is not supported for row keys
     */
    public static Optional<PredicateFactory> getRowKeyPredicateFactory(final LegacyFilterOperator operator) {
        return switch (operator) {
            case EQ:
                yield EqualityPredicateFactory.forRowKey(true);
            case NEQ, NEQ_MISS:
                yield EqualityPredicateFactory.forRowKey(false);
            case REGEX:
                yield Optional.of(PatternMatchingPredicateFactory.forRowKey(true));
            case WILDCARD:
                yield Optional.of(PatternMatchingPredicateFactory.forRowKey(false));
            case LT, LTE, GT, GTE, IS_MISSING, IS_NOT_MISSING, FIRST_N_ROWS, LAST_N_ROWS, IS_TRUE, IS_FALSE:
                yield Optional.empty();
        };
    }

    /**
     * Creates a factory for the given operator for row number predicates. If the operator does not support row numbers,
     * the returned optional is empty.
     * For comparison of row numbers on numeric values, see {@link OffsetFilter}.
     * @param operator filter operator
     * @return factory for the given operator, or empty if the operator is not supported for row numbers
     */
    public static Optional<PredicateFactory> getRowNumberPredicateFactory(final LegacyFilterOperator operator) {
        return switch (operator) {
            case REGEX:
                yield Optional.of(PatternMatchingPredicateFactory.forRowNumber(true));
            case WILDCARD:
                yield Optional.of(PatternMatchingPredicateFactory.forRowNumber(false));
            case //
                EQ, NEQ, NEQ_MISS, GT, GTE, LT, LTE, // these are handled via `OffsetFilter`
                IS_MISSING, IS_NOT_MISSING, FIRST_N_ROWS, LAST_N_ROWS, IS_TRUE, IS_FALSE:
                yield Optional.empty();
        };
    }
}
