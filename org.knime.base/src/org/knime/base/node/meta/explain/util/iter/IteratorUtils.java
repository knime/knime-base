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
 *   May 8, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.util.iter;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Provides a number of utility functions for {@link Iterator} and {@link DoubleIterator} objects.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class IteratorUtils {

    private IteratorUtils() {
        // static utility class
    }

    /**
     * @param value the singleton value
     * @return a DoubleIterator that only contains <b>value</b>
     */
    public static DoubleIterator singletonDoubleIterator(final double value) {
        return new SingletonDoubleIterator(value);
    }

    /**
     * @param iterableIterator an {@link Iterator} of {@link DoubleIterable DoubleIterables} that should be concatenated
     * @return a {@link DoubleIterator} that iterates through the elements provided by all the {@link DoubleIterable
     *         DoubleIterables} in <b>iterableIterator</b>
     */
    public static DoubleIterator concatenatedDoubleIterator(final Iterator<? extends DoubleIterable> iterableIterator) {
        return new ConcatenatedDoubleIterator(iterableIterator);
    }

    /**
     * @param source array of int values
     * @return an {@link IntIterator} that iterates over the values in source
     */
    public static IntIterator arrayIntIterator(final int[] source) {
        return new ArrayIntIterator(source);
    }

    /**
     * Maps the elements in <b>source</b> using the functions provided by <b>mappings</b>.
     * The two inputs <b>source</b> and <b>mappings</b> must have the same number of elements.
     *
     * @param source provides the elements to map
     * @param mappings provides the mapping functions
     * @return an {@link Iterator} whose elements are the results of the <b>mappings</b> applied to the elements of
     *         <b>source</b>
     */
    public static <S, T> Iterator<T> createMappingIterator(final Iterator<S> source,
        final Iterator<Function<S, T>> mappings) {
        return new MappingIterator<>(source, mappings);
    }

}
