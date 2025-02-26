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
 *   13 Oct 2022 (jasper): created
 */
package org.knime.base.node.preproc.valuelookup;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.util.Pair;

/**
 * Dictionary implementation that matches strings to output cells, whilst considering case-sensitivity
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
class StringDict extends MapDict<String> {

    private final Map<String, Pair<RowKey, DataCell[]>> m_dict;

    /**
     * Function that extracts the string from a cell
     */
    protected Function<String, String> m_stringNormaliser = Function.identity();

    /**
     * Create a new instance by providing the settings of a node instance
     *
     * @param settings the relevant settings instance
     */
    StringDict(final ValueLookupNodeSettings settings, final Function<DataRow, String> keyExtractor) {
        super(settings, keyExtractor);
        m_dict = new HashMap<>();
        if (!m_settings.m_caseSensitive) {
            m_stringNormaliser = m_stringNormaliser.andThen(s -> s.toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public Optional<Boolean> insertSearchPair(final String key, final RowKey dictRowID, final DataCell[] values)
        throws IllegalLookupKeyException {
        var str = m_stringNormaliser.apply(key); // might lower-case the string
        return insertKVPair(m_dict, str, dictRowID, values);
    }

    @Override
    public Optional<Pair<RowKey, DataCell[]>> getDictEntry(final String key) {
        return Optional.ofNullable(m_dict.get(m_stringNormaliser.apply(key)));
    }

}
