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
 *   Apr 30, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.util.iter;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.knime.core.node.util.CheckUtils;

/**
 * Maps an {@link Iterator} of type S to an {@link Iterator} of type T using the {@link Function mappings} provided by
 * a mapping {@link Iterator}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <S> source type
 * @param <T> target type
 */
final class MappingIterator<S, T> implements Iterator<T> {

    private final Iterator<S> m_sourceIterator;

    private final Iterator<Function<S, T>> m_mappingIterator;

    /**
     * Creates a MappingIterator using <b>sourceIterator</b> and <b>mappingIterator</b>.
     * It is the callers responsibility to ensure that <b>sourceIterator</b> and <b>mappingIterator</b> contain
     * the same number of elements.
     *
     * @param sourceIterator {@link Iterator} of source elements
     * @param mappingIterator {@link Iterator} of mappings
     */
    public MappingIterator(final Iterator<S> sourceIterator, final Iterator<Function<S, T>> mappingIterator) {
        m_sourceIterator = sourceIterator;
        m_mappingIterator = mappingIterator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_sourceIterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        CheckUtils.checkState(m_mappingIterator.hasNext(),
            "There is at least one source element left but no more mapping.");
        final S nextSourceElement = m_sourceIterator.next();
        final Function<S, T> nextMapping = m_mappingIterator.next();
        return nextMapping.apply(nextSourceElement);
    }

}