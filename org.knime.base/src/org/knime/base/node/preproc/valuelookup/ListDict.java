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
 *   19 Dec 2022 (jasper): created
 */
package org.knime.base.node.preproc.valuelookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.SearchDirection;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.util.Pair;

/**
 * Sub-Umbrella-Class for dictionaries that perform the lookup by iterating through the list of all key-value pairs and
 * selecting the first (last) match
 *
 * @param <K> The type of the lookup key
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
abstract class ListDict<DK, LK> extends UnsortedInputDict<LK> {

    /**
     * Stores all Key-Value pairs from the dictionary table
     */
    protected final List<Map.Entry<DK, Pair<RowKey, DataCell[]>>> m_dict;

    /**
     * Create a new instance by providing the settings of a node instance
     *
     * @param settings the relevant settings instance
     * @param keyExtractor
     */
    protected ListDict(final ValueLookupNodeSettings settings, final Function<DataRow, LK> keyExtractor) {
        super(settings, keyExtractor);
        m_dict = new ArrayList<>();
    }

    /**
     * Method can be used by inheriting classes to add a search pair to the list of entries (after pre-processing)
     *
     * @param key
     * @param dictRowID
     * @param values
     */
    protected final void insertKVPair(final DK key, final RowKey dictRowID, final DataCell[] values) {
        m_dict.add(Map.entry(key, Pair.create(dictRowID, values)));
    }

    @Override
    public Optional<Pair<RowKey, DataCell[]>> getDictEntry(final LK key) {
        if (m_settings.m_searchDirection == SearchDirection.FORWARD) {
            for (var it = m_dict.listIterator(); it.hasNext();) {
                var elem = it.next();
                if (matches(elem.getKey(), key)) {
                    return Optional.of(elem.getValue());
                }
            }
        } else if (m_settings.m_searchDirection == SearchDirection.BACKWARD) {
            for (var it = m_dict.listIterator(m_dict.size()); it.hasPrevious();) {
                var elem = it.previous();
                if (matches(elem.getKey(), key)) {
                    return Optional.of(elem.getValue());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Indicates whether a key saved in a dictionary entry matches a lookup cell
     *
     * @param entry a key from the dictionary table
     * @param lookup a lookup cell from the data table
     * @return whether they match
     */
    abstract boolean matches(DK entry, LK lookup);

}
