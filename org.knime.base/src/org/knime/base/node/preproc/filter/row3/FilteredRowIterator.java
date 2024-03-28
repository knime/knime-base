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
 *   28 Mar 2024 (jasper): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

/**
 * Row iterator that filters another row iterator based on a predicate and a range of row indices.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
final class FilteredRowIterator extends RowIterator {

    private final RowIterator m_iter;

    private final RangeSet<Long> m_includeRange;

    private final RowPredicate<?> m_predicate;

    private DataRow m_next;

    private long m_index = -1;

    private boolean m_includeMatches;

    private FilteredRowIterator(final RowIterator iter, final RowPredicate<?> predicate,
        final RangeSet<Long> rowIndices, final boolean include) {
        m_iter = iter;
        m_predicate = predicate;
        m_includeRange = rowIndices;
        m_includeMatches = include;
        advance();
    }

    public static FilteredRowIterator includeRows(final RowIterator iter, final RowPredicate<?> predicate,
        final Collection<Range<Long>> rowIndices) {
        var rangeSet = TreeRangeSet.<Long> create();
        for (var range : rowIndices) {
            rangeSet.add(range);
        }
        return new FilteredRowIterator(iter, predicate, rangeSet, true);
    }

    public static FilteredRowIterator includeRows(final RowIterator iter, final RowPredicate<?> predicate) {
        return includeRows(iter, predicate, List.of(Range.all()));
    }

    public static FilteredRowIterator excludeRows(final RowIterator iter, final RowPredicate<?> predicate,
        final Collection<Range<Long>> rowIndices) {
        var rangeSet = TreeRangeSet.<Long> create();
        for (var range : rowIndices) {
            rangeSet.add(range);
        }
        return new FilteredRowIterator(iter, predicate, rangeSet, false);
    }

    public static FilteredRowIterator excludeRows(final RowIterator iter, final RowPredicate<?> predicate) {
        return excludeRows(iter, predicate, List.of(Range.all()));
    }

    @Override
    public boolean hasNext() {
        return (m_next != null);
    }

    @Override
    public DataRow next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No next element");
        }
        var toReturn = m_next;
        advance();
        return toReturn;
    }

    private void advance() {
        m_next = null;
        while (m_iter.hasNext()) {
            var next = m_iter.next();
            m_index++; // increment for every element we check, since we refer to the index in the original iterator
            if (!m_includeMatches ^ (m_includeRange.contains(m_index) && m_predicate.test(next))) {
                // TODO: make bound-checking more efficient (don't check every time, skip n rows / abort if possible)
                m_next = next;
                break;
            }
        }
    }
}
