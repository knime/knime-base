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

import java.util.Optional;
import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;

/**
 * Umbrella class for different dictionary implementations that show different behaviours used by the
 * {@link ValueLookupNodeModel}. Contrary to the {@code BinarySearchDict}, all Key-Value pairs must be inserted
 * individually.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
abstract class UnsortedInputDict<K> extends AbstractLookupDict<K> {

    /**
     * Exception that indicates that a key could not be inserted in the dictionary
     */
    static class IllegalLookupKeyException extends Exception {
        private static final long serialVersionUID = -5487470362932276861L;

        IllegalLookupKeyException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Holds the relevant node settings
     */
    protected final ValueLookupNodeSettings m_settings;

    /**
     * Create a new instance by providing the settings of a node instance
     *
     * @param settings the relevant settings instance
     */
    protected UnsortedInputDict(final ValueLookupNodeSettings settings, final Function<DataRow, K> keyExtractor) {
        super(keyExtractor);
        m_settings = settings;
    }

    /**
     * Insert a new search pair to the dictionary
     *
     * @param key The DataCell that contains the key of the new search pair
     * @param dictRowID The row ID of the corresponding row in the dictionary
     * @param values All the replacement cells that are associated with the new key
     * @return {@code true} if the key is already present in the dictionary, {@code false} if it is not or
     *         {@link Optional#empty()} if no knowledge on the already contained keys is available
     * @throws IllegalLookupKeyException If the search pair could not be inserted
     */
    public abstract Optional<Boolean> insertSearchPair(final K key, final RowKey dictRowID,
        final DataCell[] values) throws IllegalLookupKeyException;

}
