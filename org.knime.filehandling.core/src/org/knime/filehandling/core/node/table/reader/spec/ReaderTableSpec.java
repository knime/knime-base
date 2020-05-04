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
 *   Jan 22, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.spec;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.knime.core.node.util.CheckUtils;

/**
 * Representation of a table as a collection of {@link ReaderColumnSpec ReaderColumnSpecs}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> type used to identify data types
 */
public final class ReaderTableSpec<T> implements Iterable<ReaderColumnSpec<T>> {

    private static final String TYPES = "types";

    private final List<ReaderColumnSpec<T>> m_columns;

    /**
     * Array-based constructor.
     *
     * @param columns the table contains
     */
    @SafeVarargs // because the varargs array is neither modified nor exposed
    public ReaderTableSpec(final ReaderColumnSpec<T>... columns) {
        this(Arrays.asList(notNull(columns, "columns")), false);
    }

    /**
     * Collection-based constructor.
     *
     * @param columns the table contains
     */
    public ReaderTableSpec(final Collection<ReaderColumnSpec<T>> columns) {
        this(new ArrayList<>(notNull(columns, "columns")), false);
    }


    private ReaderTableSpec(final List<ReaderColumnSpec<T>> columns, final boolean copy) {
        if (copy) {
            m_columns = new ArrayList<>(columns);
        } else {
            m_columns = columns;
        }
    }

    private static <T> T notNull(final T argument, final String name) {
        return CheckUtils.checkArgumentNotNull(argument, "The %s argument must not be null.", name);
    }

    /**
     * Creates a {@link ReaderTableSpec} from a collection of <b>names</b> and <b>types</b></br>
     * Note that <b>names</b> may contain {@code null} elements while <b>types</b> may not.
     *
     * @param names the column names (may contain {@code null})
     * @param types the column types (must NOT contain {@code null}
     * @return a {@link ReaderTableSpec} with columns according to the provided arguments
     */
    public static <T> ReaderTableSpec<T> create(final Collection<String> names, final Collection<T> types) {
        CheckUtils.checkArgumentNotNull(names, "The names argument must not be null.");
        CheckUtils.checkArgumentNotNull(types, "The types argument must not be null.");

        CheckUtils.checkArgument(names.size() == types.size(), "Names and types must have the same size.");
        final Iterator<String> nameIterator = names.iterator();
        final Iterator<T> typesIterator = types.iterator();
        final List<ReaderColumnSpec<T>> cols = new ArrayList<>(names.size());
        while (nameIterator.hasNext()) {
            final String name = nameIterator.next();
            final T type = CheckUtils.checkArgumentNotNull(typesIterator.next(), "A type must not be null.");
            cols.add(ReaderColumnSpec.createWithName(name, type));
        }
        return new ReaderTableSpec<>(cols, false);
    }

    /**
     * Creates a {@link ReaderTableSpec} with nameless columns whose types are given by <b>types</b>.
     *
     * @param types of the columns (must NOT be or contain {@code null})
     * @return a {@link ReaderTableSpec} with nameless columns of the types provided
     */
    public static <T> ReaderTableSpec<T> create(@SuppressWarnings("unchecked") final T... types) {
        return create(Arrays.asList(notNull(types, TYPES)));
    }

    /**
     * Creates a {@link ReaderTableSpec} with nameless columns whose types are given by <b>types</b>.
     *
     * @param types of the columns (must NOT be or contain {@code null})
     * @return a {@link ReaderTableSpec} with nameless columns of the types provided types
     */
    public static <T> ReaderTableSpec<T> create(final Collection<T> types) {
        notNull(types, TYPES);
        CheckUtils.checkArgument(!types.isEmpty(), "The list of types must contain at least one element.");
        return new ReaderTableSpec<>(types.stream()//
            .map(ReaderColumnSpec::create)//
            .collect(toList()), false);
    }

    @Override
    public Iterator<ReaderColumnSpec<T>> iterator() {
        return m_columns.iterator();
    }

    /**
     * Returns the number of columns of the table.
     *
     * @return the number of columns of the table
     */
    public int size() {
        return m_columns.size();
    }

    /**
     * Returns a sequential {@link Stream} of the contained {@link ReaderColumnSpec column specs}.
     *
     * @return a stream of the contained column specs
     */
    public Stream<ReaderColumnSpec<T>> stream() {
        return m_columns.stream();
    }

    /**
     * Retrieves the {@link ReaderColumnSpec} at index <b>idx</b>.
     *
     * @param idx the column index which has to lie in [0, {@link ReaderTableSpec#size()})
     * @return the {@link ReaderColumnSpec} at the specified index
     */
    public ReaderColumnSpec<T> getColumnSpec(final int idx) {
        return m_columns.get(idx);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof ReaderTableSpec) {
            // if the types not match m_columns.equals(other.m_columns) will return false anyway
            @SuppressWarnings("rawtypes")
            ReaderTableSpec other = (ReaderTableSpec)obj;
            return m_columns.equals(other.m_columns);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return m_columns.hashCode();
    }

    @Override
    public String toString() {
        return m_columns.toString();
    }

}
