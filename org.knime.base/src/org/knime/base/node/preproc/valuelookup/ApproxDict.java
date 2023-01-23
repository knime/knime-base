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

import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.MatchBehaviour;
import org.knime.core.data.DataCell;

/**
 * Dictionary implementation that support fast queries for e.g. the next-highest element in the key set. The Matching
 * behaviours {@link MatchBehaviour.EQUALORSMALLER} and {@link MatchBehaviour.EQUALORLARGER} are supported by this
 * dictionary, any other behaviour will throw a {@link UnsupportedOperationException} on instantiation.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
class ApproxDict extends MapDict {

    private final NavigableMap<DataCell, DataCell[]> m_dict;

    /**
     * This function will be used to extract an entry from m_dict and is defined by the match behaviour
     */
    private final Function<DataCell, Map.Entry<DataCell, DataCell[]>> m_lookupFunction;

    /**
     * Create a new instance by providing the settings of a node instance and a suitable comparator
     *
     * @param settings the relevant settings instance
     * @param comp a comparator that is used to compare the key cells
     */
    ApproxDict(final ValueLookupNodeSettings settings, final Comparator<DataCell> comp) {
        super(settings);
        m_dict = new TreeMap<>(comp);
        switch (m_settings.m_matchBehaviour) {
            case EQUALORLARGER:
                m_lookupFunction = m_dict::ceilingEntry;
                break;
            case EQUALORSMALLER:
                m_lookupFunction = m_dict::floorEntry;
                break;
            default:
                throw new UnsupportedOperationException("This dictionary only supports approximate matching.");
        }
    }

    @Override
    public Optional<Boolean> insertSearchPair(final DataCell key, final DataCell[] values)
        throws IllegalLookupKeyException {
        return insertKVPair(m_dict, key, values);
    }

    @Override
    public Optional<DataCell[]> getCells(final DataCell key) {
        var entry = m_lookupFunction.apply(key);
        return Optional.ofNullable(entry).map(Map.Entry::getValue);
    }
}
