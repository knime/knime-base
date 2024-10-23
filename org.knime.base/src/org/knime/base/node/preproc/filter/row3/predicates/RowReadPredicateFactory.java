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
 *   16 Sept 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.predicates;

import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.core.data.IntValue;
import org.knime.core.data.v2.RowRead;

/**
 * Factory for creating predicates operating on {@link RowRead}s.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
interface RowReadPredicateFactory<T> {

    enum RangeOperator {
            LT, LTE, GT, GTE;
    }

    Predicate<RowRead> createPredicate(Function<RowRead, T> valueAccessor, T referenceValue);

    final class IntIntEquality implements RowReadPredicateFactory<IntValue> {

        private final boolean m_matchEqual;

        IntIntEquality(final boolean matchEqual) {
            m_matchEqual = matchEqual;
        }

        @Override
        public Predicate<RowRead> createPredicate(final Function<RowRead, IntValue> valueAccessor,
            final IntValue referenceValue) {
            return m_matchEqual
                ? (rowRead -> valueAccessor.apply(rowRead).getIntValue() == referenceValue.getIntValue())
                : (rowRead -> valueAccessor.apply(rowRead).getIntValue() != referenceValue.getIntValue());
        }

    }

    final class IntIntComparator implements RowReadPredicateFactory<IntValue> {

        private final RangeOperator m_rangeOperator;

        IntIntComparator(final RangeOperator rangeOperator) {
            m_rangeOperator = rangeOperator;
        }

        @Override
        public Predicate<RowRead> createPredicate(final Function<RowRead, IntValue> acc,
            final IntValue ref) {
            return switch (m_rangeOperator) {
                case LT -> read -> acc.apply(read).getIntValue() < ref.getIntValue();
                case LTE -> read -> acc.apply(read).getIntValue() <= ref.getIntValue();
                case GT -> read -> acc.apply(read).getIntValue() > ref.getIntValue();
                case GTE -> read -> acc.apply(read).getIntValue() >= ref.getIntValue();
            };
        }

    }
}