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
 *   Jun 13, 2019 (Perla Gjoka): created
 */
package org.knime.base.node.preproc.filter.row2.operator;

import java.util.Iterator;
import java.util.Set;
import java.util.function.BiPredicate;

import org.knime.core.data.DataRow;

import com.google.common.collect.Range;

/**
 * A {@link RowPredicate} is used to filter rows in a data table.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface RowPredicate extends BiPredicate<DataRow, Long> {

    /**
     * @return a set of all the column indices in the input table, chosen by the user for different conditions.
     */
    Set<Integer> getRequiredColumns();

    /**
     * @return the index of rows needed when user selects the Row Index option.
     */
    Range<Long> getRowIndexRange();

    /**
     * Negates the RowPredicate created. If the test is passed, then this method fails it or vice-versa.
     *
     * @param rowPredicate holds the created Row Predicate, which we might want to negate.
     * @return the opposite Row Predicate.
     */
    static RowPredicate negate(final RowPredicate rowPredicate) {
        return new NegationPredicate(rowPredicate);
    }

    /**
     * @param predicateIterator is an iterator of row predicates, created by multiple conditions given by the user.
     * @return GroupRowPredicate handled with AND.
     */
    static RowPredicate and(final Iterator<RowPredicate> predicateIterator) {
        return new GroupRowPredicate(predicateIterator, (p, q) -> p.and(q), (p, q) -> p.intersection(q));
    }

    /**
     * @param predicateIterator is an iterator of row predicates, created by multiple conditions given by the user.
     * @return GroupRowPredicate handled with OR.
     */
    static RowPredicate or(final Iterator<RowPredicate> predicateIterator) {
        return new GroupRowPredicate(predicateIterator, (p, q) -> p.or(q), (p, q) -> p.span(q));
    }

    /**
     * Negates a given {@link RowPredicate}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class NegationPredicate implements RowPredicate {

        private final RowPredicate m_rowPredicate;

        private final Range<Long> m_range;

        /**
         * @param rowPredicate the {@link RowPredicate} to negate
         */
        public NegationPredicate(final RowPredicate rowPredicate) {
            m_rowPredicate = rowPredicate;
            m_range = createNegatedRange(rowPredicate.getRowIndexRange());
        }

        private static Range<Long> createNegatedRange(final Range<Long> range) {
            if ((!range.hasLowerBound() && !range.hasUpperBound())
                || (range.hasLowerBound() && range.hasUpperBound())) {
                return Range.all();
            } else if (range.hasLowerBound()) {
                return Range.atMost(range.lowerEndpoint());
            } else {
                return Range.atLeast(range.upperEndpoint());
            }
        }

        @Override
        public boolean test(final DataRow t, final Long index) {
            return !m_rowPredicate.test(t, index);
        }

        @Override
        public Set<Integer> getRequiredColumns() {
            return m_rowPredicate.getRequiredColumns();
        }

        @Override
        public Range<Long> getRowIndexRange() {
            return m_range;
        }
    }

}
