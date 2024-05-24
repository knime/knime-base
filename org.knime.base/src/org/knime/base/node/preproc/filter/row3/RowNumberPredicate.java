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
 *   24 May 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.util.function.LongPredicate;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.core.node.InvalidSettingsException;

/**
 * Predicate for filtering rows by row number.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
interface RowNumberPredicate extends LongPredicate {

    static RowNumberPredicate buildPredicate(final boolean isAnd, final Iterable<FilterCriterion> rowNumberCriteria,
            final long optionalTableSize) throws InvalidSettingsException {
        final var iter = rowNumberCriteria.iterator();
        if (!iter.hasNext()) {
            return null;
        }
        var filterPredicate = createFrom(iter.next(), optionalTableSize);
        while (iter.hasNext()) {
            final var predicate = createFrom(iter.next(), optionalTableSize);
            filterPredicate = isAnd ? filterPredicate.and(predicate) : filterPredicate.or(predicate);
        }
        return filterPredicate;
    }

    static void validateSettings(final Iterable<FilterCriterion> criteria) throws InvalidSettingsException {
        for (final var c : criteria) {
            RowNumberFilter.getAsFilterSpec(c);
        }
    }

    private static RowNumberPredicate createFrom(final FilterCriterion criterion, final long optionalTableSize)
            throws InvalidSettingsException {
        final var filterSpec = RowNumberFilter.getAsFilterSpec(criterion);
        final var offsetFilter = filterSpec.toOffsetFilter(optionalTableSize);
        return offsetFilter.toPredicate();
    }

    /**
     * Short-circuiting AND.
     * @param other other predicate
     * @return combined predicate
     * @see LongPredicate#and(LongPredicate)
     */
    @Override
    default RowNumberPredicate and(final LongPredicate other) {
        return value -> test(value) && other.test(value);
    }


    /**
     * Short-circuiting OR.
     * @param other other predicate
     * @return combined predicate
     * @see LongPredicate#or(LongPredicate)
     */
    @Override
    default RowNumberPredicate or(final LongPredicate other) {
        return value -> test(value) || other.test(value);
    }
}
