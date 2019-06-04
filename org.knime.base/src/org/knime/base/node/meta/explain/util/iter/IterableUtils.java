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

import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import com.google.common.collect.Iterables;

/**
 * Provides different utility functions for {@link Iterable Iterables} that are not provided by {@link Iterables}. Also
 * contains utility functions for {@link DoubleIterable}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class IterableUtils {

    private IterableUtils() {
        // static utility class
    }

    /**
     * @param value singleton value
     * @return a {@link DoubleIterable} that contains only <b>value</b>
     */
    public static DoubleIterable singletonDoubleIterable(final double value) {
        return () -> new SingletonDoubleIterator(value);
    }

    /**
     * @param iterables an Iterable of {@link DoubleIterable}
     * @return a {@link DoubleIterable} that iterates over all the elements contained in <b>iterables</b>
     */
    public static DoubleIterable concatenatedDoubleIterable(final Iterable<? extends DoubleIterable> iterables) {
        return () -> new ConcatenatedDoubleIterator(iterables.iterator());
    }

    /**
     * @param value constant value to return
     * @param size number of times to return <b>value</b>
     * @return a {@link DoubleIterable} that returns <b>value</b> <b>size</b> times
     */
    public static DoubleIterable constantDoubleIterable(final double value, final long size) {
        return () -> new ConstantDoubleIterator(value, size);
    }

    /**
     * @param values the values to iterator over
     * @param copy true if the <b>values</b> should be cloned
     * @return a {@link DoubleIterable} that can iterates over <b>values</b>
     */
    public static DoubleIterable arrayDoubleIterable(final double[] values, final boolean copy) {
        final double[] vals = copy ? values.clone() : values;
        return () -> new ArrayDoubleIterator(vals);
    }

    /**
     * @param source provides the elements that are mapped to double
     * @param mapping maps a single element to double
     * @return a DoubleIterable which applies <b>mapping</b> to the elements of <b>source</b>
     */
    public static <T> DoubleIterable toDoubleIterable(final Iterable<T> source, final ToDoubleFunction<T> mapping) {
        return () -> new MappingDoubleIterator<>(source.iterator(), mapping);
    }

    /**
     * Similar ot {@link Iterables#transform(Iterable, com.google.common.base.Function)} but requires a mapping for each
     * element of <b>source</b>.
     *
     * @param source {@link Iterable} of source elements
     * @param mappings {@link Iterable} of mapping functions
     * @return an {@link Iterable} where all elements of <b>source</b> are mapped using the functions in <b>mappings</b>
     */
    public static <S, T> Iterable<T> mappingIterable(final Iterable<S> source,
        final Iterable<Function<S, T>> mappings) {
        return () -> new MappingIterator<>(source.iterator(), mappings.iterator());
    }

}
