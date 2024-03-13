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

import java.util.Map;
import java.util.Optional;

import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.SearchDirection;
import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.core.util.Pair;

/**
 * Sub-Umbrella-Class for dictionaries that perform a lookup via a map instance that provides faster-than-linear access
 * to a queried key
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
abstract class MapDict extends UnsortedInputDict {

    /**
     * Create a new instance by providing the settings of a node instance
     *
     * @param settings the relevant settings instance
     */
    protected MapDict(final ValueLookupNodeSettings settings) {
        super(settings);
        switch (m_settings.m_searchDirection) { //NOSONAR: switch is nicer to read here
            case FORWARD, BACKWARD:
                break;
            default:
                throw new UnsupportedOperationException(
                    "Unsupported search direction " + m_settings.m_searchDirection.toString());
        }
    }

    /**
     * Can be used by inheriting classes to add a search pair to the map. Respects the specified search direction by
     * only replacing an existing entry if the last match shall be found later.
     *
     * @param dict the dictionary in which to insert the key-value pair
     * @param key
     * @param dictRowID
     * @param values
     * @param <K> the type of the key (e.g. DataCell, String, Pattern, etc...)
     * @param <M> the dictionary implementation
     * @return {@code true} if the key is already present in the dictionary, {@code false} otherwise
     */
    protected <K, M extends Map<K, Pair<RowKey, DataCell[]>>> Optional<Boolean> insertKVPair(final M dict, final K key,
        final RowKey dictRowID, final DataCell[] values) {
        // deduplicate input pairs based on search direction: FORWARD -> first key wins, BACKWARD -> last key wins
        var dup = (m_settings.m_searchDirection == SearchDirection.FORWARD) //
            ? dict.putIfAbsent(key, Pair.create(dictRowID, values)) : dict.put(key, Pair.create(dictRowID, values));
        return Optional.of(dup != null);
    }

}
