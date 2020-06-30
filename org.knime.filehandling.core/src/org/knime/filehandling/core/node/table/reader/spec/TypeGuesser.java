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
 *   Mar 23, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.spec;

import static java.util.stream.Collectors.toList;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy.TypeResolver;

/**
 * Guesses the types of columns by maintaining a list of {@link TypeResolver TypeResolvers} that are incrementally
 * updated.</br>
 * Also provides an early stopping mechanism since no more updates are necessary if all TypeResolvers reached their most
 * general type.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TypeGuesser<T, V> {

    private final List<TypeResolver<T, V>> m_resolvers = new LinkedList<>();

    private final TypeHierarchy<T, V> m_typeHierarchy;

    private final boolean m_enableEarlyStopping;

    private boolean m_canStop = false;

    TypeGuesser(final TypeHierarchy<T, V> typeHierarchy,
        final boolean enableEarlyStopping) {
        m_typeHierarchy = typeHierarchy;
        m_enableEarlyStopping = enableEarlyStopping;
    }

    void update(final RandomAccessible<V> row) {
        ensureEnoughResolvers(row.size());
        boolean canStop = true;
        final Iterator<TypeResolver<T, V>> resolverIterator = m_resolvers.iterator();
        for (int i = 0; i < row.size(); i++) {
            final TypeResolver<T, V> resolver = resolverIterator.next();
            resolver.accept(row.get(i));
            canStop &= resolver.reachedTop();
        }
        m_canStop = canStop;
    }

    private void ensureEnoughResolvers(final long neededResolvers) {
        if (m_resolvers.size() < neededResolvers) {
            Stream.generate(m_typeHierarchy::createResolver)//
                .limit(neededResolvers - m_resolvers.size())//
                .forEach(m_resolvers::add);
        }
    }

    boolean canStop() {
        return m_enableEarlyStopping && m_canStop;
    }

    /**
     * Returns the list of most specific types whose contains at least <b>minimumExpected</b> types
     * but may contain more if more columns were observed by this TypeGuesser.
     *
     * @param minimumExpected the minimum number of types expected to be returned
     * @return the most specific types observed in the table (filled up to minimum expected if necessary)
     */
    List<T> getMostSpecificTypes(final int minimumExpected) {
        ensureEnoughResolvers(minimumExpected);
        return m_resolvers.stream().map(TypeResolver::getMostSpecificType).collect(toList());
    }

    /**
     * Returns the list of Booleans that indicate whether a {@link TypeResolver} has a type or not. The list contains at
     * least <b>minimumExpected</b> Booleans but may contain more if more columns were observed by this TypeGuesser.
     *
     * @param minimumExpected the minimum number of Booleans expected to be returned
     * @return the Booleans that indicate whether a {@link TypeResolver} has a type or not (filled up to minimum
     *         expected if necessary)
     */
    List<Boolean> getHasTypes(final int minimumExpected) {
        ensureEnoughResolvers(minimumExpected);
        return m_resolvers.stream().map(TypeResolver::hasType).collect(toList());
    }

}