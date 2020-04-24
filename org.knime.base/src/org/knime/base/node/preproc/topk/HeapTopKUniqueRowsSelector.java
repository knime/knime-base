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
 *   April 25, 2020 (Lars Schweikardt, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.topk;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

import org.knime.core.data.DataRow;

/**
 * Top k Selector for unique rows.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class HeapTopKUniqueRowsSelector implements TopKSelector {

    private final Comparator<DataRow> m_comparator;

    private final PriorityQueue<UniqueRowCollection> m_topRows;

    private final int m_nrRows;

    HeapTopKUniqueRowsSelector(final Comparator<DataRow> comparator, final int nrElements) {
        m_comparator = comparator;
        m_nrRows = nrElements;
        m_topRows = new PriorityQueue<>(m_nrRows + 1,
            (h1, h2) -> m_comparator.compare(h1.m_representative, h2.m_representative));
    }

    @Override
    public void consume(final DataRow row) {
        final int comparisonResult =
            m_topRows.isEmpty() ? 1 : m_comparator.compare(m_topRows.element().m_representative, row);

        if (comparisonResult == 0) {
            m_topRows.element().m_rows.add(row);
        } else if (comparisonResult > 0 && m_topRows.size() < m_nrRows) {
            m_topRows.add(new UniqueRowCollection(row));
        } else if (comparisonResult < 0) {
            final Optional<UniqueRowCollection> uniqueRow = searchUniqueRowHelper(row);
            if (uniqueRow.isPresent()) {
                uniqueRow.get().m_rows.add(row);
            } else if (m_topRows.size() < m_nrRows) {
                m_topRows.add(new UniqueRowCollection(row));
            } else {
                m_topRows.poll();
                m_topRows.add(new UniqueRowCollection(row));
            }
        }
    }

    /**
     * Check if a row is already in the queue and return the corresponding {@code UniqueRowHelper}.
     *
     * @param row the row to check for
     * @return Optional<UniqueRowHelper>
     */
    private Optional<UniqueRowCollection> searchUniqueRowHelper(final DataRow row) {
        return m_topRows.stream().filter(r -> m_comparator.compare(r.m_representative, row) == 0).findFirst();
    }

    @Override
    public Collection<DataRow> getTopK() {
        return Collections.unmodifiableCollection(
            m_topRows.stream().map(x -> x.m_rows).collect(LinkedList<DataRow>::new, List::addAll, List::addAll));
    }

    /**
     * Helper class to save a representative of a row and all equal rows into it.
     */
    private static class UniqueRowCollection {
        private final DataRow m_representative;

        private final List<DataRow> m_rows = new LinkedList<>();

        UniqueRowCollection(final DataRow representative) {
            m_representative = representative;
            m_rows.add(representative);
        }
    }
}
