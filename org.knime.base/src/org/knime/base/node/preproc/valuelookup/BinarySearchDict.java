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
 *   20 Dec 2022 (jasper): created
 */
package org.knime.base.node.preproc.valuelookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.MatchBehaviour;
import org.knime.core.data.DataCell;
import org.knime.core.node.util.CheckUtils;

/**
 * Dictionary implementation that performs binary search in the dictionary table for every lookup. The Input must be a
 * dictionary table that is sorted by key.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
class BinarySearchDict implements LookupDict {

    private final ValueLookupNodeSettings m_settings;

    /**
     * The key cells from the dictionary table
     */
    private final ArrayList<DataCell> m_keys;

    /**
     * The output cells from the dictionary table. m_values[i] corresponds to m_keys[i]
     */
    private final ArrayList<DataCell[]> m_values;

    /**
     * A comparator that will be used to compare cells in m_keys.
     */
    private final Comparator<DataCell> m_comparator;

    /**
     * This comparator is used for the binary search. It might induce the same ordering as m_comparator, or the reversed
     * one, depending on the sorting order.
     */
    private final Comparator<DataCell> m_comparatorForBinsearch;

    /** To be more verbose, the order of the keys is indicated with this enum. */
    enum SortingOrder {
            ASC, DESC;
    }

    /**
     * Defines in what order the keys are stored in m_keys
     */
    private final SortingOrder m_sortingOrder;

    /**
     * Create a new Binary Search dictionary. The constructor already provides the data on which the search will be run
     *
     * @param settings The settings that define the matching behaviour of the Value Lookup instance
     * @param comp A comparator on {@link DataCell}s. This comparator shall compare cells ascendingly, regardless of how
     *            the keys are sorted.
     * @param keys The vector of keys, on which the binary search will be performed. The keys must be sorted, consistent
     *            with sortingOrder
     * @param values The vector of values that will be returned once the corresponding key has been found. It must hold
     *            m_values.size() == m_keys.size(), although meaningful results will only be achieved with equality.
     * @param sortingOrder Whether the provided key vector is sorted ascendingly or descendingly w.r.t. the provided
     *            comparator
     */
    BinarySearchDict(final ValueLookupNodeSettings settings, final Comparator<DataCell> comp,
        final ArrayList<DataCell> keys, final ArrayList<DataCell[]> values, final SortingOrder sortingOrder) {
        CheckUtils.checkArgument(keys.size() == values.size(), "The number of keys and values must be equal.");
        m_settings = settings;
        m_keys = keys;
        m_values = values;
        m_comparator = comp;
        m_sortingOrder = sortingOrder;
        m_comparatorForBinsearch = m_sortingOrder == SortingOrder.ASC ? m_comparator : m_comparator.reversed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<DataCell[]> getCells(final DataCell key) {
        var index = Collections.binarySearch(m_keys, key, m_comparatorForBinsearch);
        // `index` is either the index of an exact match, or (-insertionpoint - 1)

        if (index >= 0) { // Found an exact match at index
            return Optional.of(m_values.get(index));
        } else {
            if (chooseLeft()) {
                // We want to get the element LEFT of where the exact match would be
                var indexLeftOfInsertionPoint = -index - 2; // One left of the insertionpoint
                if (indexLeftOfInsertionPoint >= 0) { // could be out of bounds (= -1)
                    return Optional.of(m_values.get(indexLeftOfInsertionPoint));
                }
            } else if (chooseRight()) {
                // We want to get the element RIGHT of where the exact match would be
                var indexAtInsertionPoint = -index - 1; // Exactly at the insertionpoint
                if (indexAtInsertionPoint < m_values.size()) { // could be out of bounds (= m_values.size())
                    return Optional.of(m_values.get(indexAtInsertionPoint));
                }
            }
        }
        return Optional.empty(); // Fallback
    }

    private boolean chooseLeft() {
        return (m_settings.m_matchBehaviour == MatchBehaviour.EQUALORSMALLER && m_sortingOrder == SortingOrder.ASC)
            || (m_settings.m_matchBehaviour == MatchBehaviour.EQUALORLARGER && m_sortingOrder == SortingOrder.DESC);
    }

    private boolean chooseRight() {
        return (m_settings.m_matchBehaviour == MatchBehaviour.EQUALORLARGER && m_sortingOrder == SortingOrder.ASC)
            || (m_settings.m_matchBehaviour == MatchBehaviour.EQUALORSMALLER && m_sortingOrder == SortingOrder.DESC);
    }

}
