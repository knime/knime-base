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
 *   Jan 17, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.typehierarchy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.knime.core.node.util.CheckUtils;

/**
 * Represents a {@link TypeHierarchy} that can be seen as simple path from the most specific to the most general type.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify data types
 * @param <V> the type of values
 */
public final class PathTypeHierarchy<T, V> implements TypeHierarchy<T, V> {

    private final List<TypeTester<T, V>> m_hierarchy;

    /**
     * Constructor for a PathTypeHierarchy.
     *
     * @param tester ordered from most specific to most general
     */
    public PathTypeHierarchy(final List<TypeTester<T, V>> tester) {
        CheckUtils.checkArgument(!tester.isEmpty(), "The list of testers must not be empty.");
        m_hierarchy = new ArrayList<>(tester);
    }

    @Override
    public TypeResolver<T, V> createResolver() {
        return new LinearTypeResolver();
    }

    private final class LinearTypeResolver implements TypeResolver<T, V> {
        private final Iterator<TypeTester<T, V>> m_iterator = m_hierarchy.iterator();

        private TypeTester<T, V> m_current = null;

        @Override
        public T getMostSpecificType() {
            if (m_current == null) {
                // we haven't seen anything therefore we should assume
                // the most generic type i.e. the top of the hierarchy
                return m_hierarchy.get(m_hierarchy.size() - 1).getType();
            } else {
                return m_current.getType();
            }
        }

        @Override
        public void accept(final V value) {
            if (m_current == null) {
                // this is the first value we observe, so we start evaluating at the most specific type
                m_current = m_iterator.next();
            }
            while (!m_current.test(value)) {
                CheckUtils.checkState(m_iterator.hasNext(),
                    "The top most type in the type hierarchy (%s) has to accept all incoming values but rejected %s.",
                    m_current.getType(), value);
                nextTester();
            }
        }

        private void nextTester() {
            m_current = m_iterator.next();
        }

        @Override
        public boolean reachedTop() {
            return !m_iterator.hasNext();
        }

    }

}
