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
 *   Feb 4, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.randomaccess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents a collection of values that can be accessed via index in constant time.</br>
 * NOTE: It is recommended to extend {@link AbstractRandomAccessible} instead of implementing RandomAccessible directly.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <V> the type of values stored in this RandomAccessible
 */
public interface RandomAccessible<V> extends Iterable<V> {

    /**
     * Returns the number of values stored by this {@link RandomAccessible}.
     *
     * @return the number of values stored
     */
    int size();

    /**
     * Returns the element at the specified position in this {@link RandomAccessible}. </br>
     * Indexing is zero based and the last valid index is <code>{@link #size()}-1</code>.
     *
     * @param idx to retrieve the value from
     * @return the value stored at position <b>idx</b>
     */
    V get(int idx);

    /**
     * {@inheritDoc}
     *
     * The values returned by the iterator are ordered according to their index.
     */
    @Override
    default Iterator<V> iterator() {
        return new DefaultRandomAccessibleIterator<>(this);
    }

    /**
     * Creates a shallow copy of this {@link RandomAccessible} i.e. it does not copy the underlying values.
     *
     * @return a shallow copy of this {@link RandomAccessible}
     */
    default RandomAccessible<V> copy() {
        final ArrayList<V> list = new ArrayList<>(size());
        for (V element : this) {
            list.add(element);
        }
        return new ArrayListRandomAccessible<>(list);
    }


    /**
     * Returns a sequential {@link Stream} with this RandomAccessible as its source.<br/>
     * <p>
     * This method should be overridden when the {@link #spliterator()} method cannot return a spliterator that is
     * {@code IMMUTABLE},
     * </p>
     *
     * @return a sequential {@link Stream} over the elements in this RandomAccessible
     */
    default Stream<V> stream() {
        return StreamSupport.stream(Spliterators.spliterator(iterator(), size(), Spliterator.IMMUTABLE), false);
    }

}
