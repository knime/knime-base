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
 *   Mar 1, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Filters the elements to be displayed.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class FilteredList<T extends Positionable> {

    private static final Comparator<Positionable> COMPARATOR =
        (e1, e2) -> Integer.compare(e1.getPosition(), e2.getPosition());

    private final ArrayList<T> m_filtered = new ArrayList<>();

    private final ArrayList<T> m_unfiltered = new ArrayList<>();

    private Predicate<T> m_filter = e -> true;

    void clear() {
        m_filtered.clear();
        m_unfiltered.clear();
    }

    void setFilter(final Predicate<T> filter) {
        m_filter = filter;
        m_filtered.clear();
        for (T element : m_unfiltered) {
            if (m_filter.test(element)) {
                m_filtered.add(element);
            }
        }
    }

    void add(final T element) {
        m_unfiltered.add(element);
        if (m_filter.test(element)) {
            m_filtered.add(element);
        }
    }

    void sort() {
        m_unfiltered.sort(COMPARATOR);
        m_filtered.sort(COMPARATOR);
    }

    /**
     * Reorders one element.
     *
     * @param from the index from which to move the element
     * @param to the index to move the element to
     * @return {@code true} if the reordering resulted in a change (i.e. if from != to)
     */
    boolean reorder(final int from, final int to) {
        if (from == to) {
            return false; // nothing changes
        }
        final T moved = m_filtered.get(from);
        final int unfilteredFromIdx = moved.getPosition();
        final int unfilteredToIdx = m_filtered.get(to).getPosition();
        if (from < to) {
            for (int i = unfilteredFromIdx; i < unfilteredToIdx; i++) {
                m_unfiltered.get(i + 1).setPosition(i);
            }
        } else {
            for (int i = unfilteredFromIdx; i >= unfilteredToIdx; i--) {
                m_unfiltered.get(i).setPosition(i + 1);
            }
        }
        moved.setPosition(unfilteredToIdx);
        sort();
        return true;
    }

    T get(final int idx) {
        return m_filtered.get(idx);
    }

    int filteredSize() {
        return m_filtered.size();
    }

    int unfilteredSize() {
        return m_unfiltered.size();
    }

    Iterable<T> unfilteredIterable() {
        return m_unfiltered;
    }

    Stream<T> unfilteredStream() {
        return m_unfiltered.stream();
    }

}
