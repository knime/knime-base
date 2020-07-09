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
 *   Jan 16, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.type.hierarchy;

import java.util.function.Consumer;

/**
 * Defines how different types relate to each other.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> type that is used to identify types (e.g. Class)
 * @param <V> type of values that are tested for being of a certain type
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface TypeHierarchy<T, V> {

    /**
     * Creates a {@link TypeResolver resolver} that is based on this {@link TypeHierarchy hierarchy}.
     *
     * @return a resolver that is based on this hierarchy
     */
    TypeResolver<T, V> createResolver();

    /**
     * Allows to find the common supertype of values by following a type hierarchy.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param <T> type that is used to identify types (e.g. Class)
     * @param <V> type of values that are tested for being of a certain type
     */
    public interface TypeResolver<T, V> extends Consumer<V> {

        /**
         * @return the preferred type of this hierarchy
         */
        T getMostSpecificType();

        /**
         * Observes the given value.
         * If <b>value</b> is compatible with the currently most specific type nothing changes,
         * otherwise the hierarchy is traversed until a type is found that is compatible with <b>value</b>
         *
         * @param value to observe
         */
        @Override
        void accept(V value);

        /**
         * Indicates whether the top of the hierarchy i.e. the most generic type has been reached.
         * Once the top of the hierarchy is reached, there is no point in further
         * search since there is no more generic type.
         *
         * @return true if the top of the hierarchy is reached
         */
        boolean reachedTop();

        /**
         * Indicates whether a type is available, i.e., if at least one value has ever been observed.
         *
         * @return {@code true} if a type is available
         */
        boolean hasType();
    }

}
