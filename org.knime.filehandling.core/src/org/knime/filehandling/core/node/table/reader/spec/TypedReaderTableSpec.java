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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.knime.core.node.util.CheckUtils;

/**
 * Representation of a table as a collection of {@link TypedReaderColumnSpec ReaderColumnSpecs}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> type used to identify data types
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class TypedReaderTableSpec<T> extends ReaderTableSpec<TypedReaderColumnSpec<T>> {

    private static final String TYPES = "types";

    private static final String HAS_TYPES = "hasTypes";

    /**
     * Array-based constructor.
     *
     * @param columns the table contains
     */
    @SafeVarargs // because the varargs array is neither modified nor exposed
    public TypedReaderTableSpec(final TypedReaderColumnSpec<T>... columns) {
        super(columns);
    }

    /**
     * Collection-based constructor.
     *
     * @param columns the table contains
     */
    public TypedReaderTableSpec(final Collection<TypedReaderColumnSpec<T>> columns) {
        super(columns);
    }

    private TypedReaderTableSpec(final List<TypedReaderColumnSpec<T>> columns, final boolean copy) {
        super(copy ? new ArrayList<>(columns) : columns);
    }

    /**
     * Creates a {@link TypedReaderTableSpec} from a collection of <b>names</b> and <b>types</b></br>
     * Note that <b>names</b> may contain {@code null} elements while <b>types</b> may not.
     *
     * @param names the column names (may contain {@code null})
     * @param types the column types (must NOT contain {@code null})
     * @param hasTypes whether the columns have types, same order as {@code types} (must NOT contain {@code null})
     * @return a {@link TypedReaderTableSpec} with columns according to the provided arguments
     */
    public static <T> TypedReaderTableSpec<T> create(final Collection<String> names, final Collection<T> types,
        final Collection<Boolean> hasTypes) {
        CheckUtils.checkArgumentNotNull(names, "The names argument must not be null.");
        CheckUtils.checkArgumentNotNull(types, "The types argument must not be null.");
        CheckUtils.checkArgumentNotNull(hasTypes, "The hasTypes argument must not be null.");
        CheckUtils.checkArgument(names.size() == types.size(), "Names and types must have the same size.");
        CheckUtils.checkArgument(types.size() == hasTypes.size(), "Types and hasTypes must have the same size.");
        final Iterator<String> nameIterator = names.iterator();
        final Iterator<T> typesIterator = types.iterator();
        final Iterator<Boolean> hasTypesIterator = hasTypes.iterator();
        final List<TypedReaderColumnSpec<T>> cols = new ArrayList<>(names.size());
        while (nameIterator.hasNext()) {
            final String name = nameIterator.next();
            final T type = CheckUtils.checkArgumentNotNull(typesIterator.next(), "A type must not be null.");
            final Boolean hasType =
                CheckUtils.checkArgumentNotNull(hasTypesIterator.next(), "A hasType Boolean must not be null.");
            cols.add(TypedReaderColumnSpec.createWithName(name, type, hasType));
        }
        return new TypedReaderTableSpec<>(cols, false);
    }

    /**
     * Creates a {@link TypedReaderTableSpec} with nameless columns whose types are given by <b>types</b> and booleans
     * that indicate whether the columns actually have types are given by <b>hasTypes</b>.
     *
     * @param types of the columns (must NOT be or contain {@code null})
     * @param hasTypes whether the columns have types, same order as {@code types} (must NOT contain {@code null})
     * @return a {@link TypedReaderTableSpec} with nameless columns of the types provided types
     */
    public static <T> TypedReaderTableSpec<T> create(final Collection<T> types, final Collection<Boolean> hasTypes) {
        notNull(types, TYPES);
        notNull(hasTypes, HAS_TYPES);
        CheckUtils.checkArgument(types.size() == hasTypes.size(), "Types and hasTypes must have the same size.");
        final List<TypedReaderColumnSpec<T>> colSpecs = new ArrayList<>();
        final Iterator<T> typesIterator = types.iterator();
        final Iterator<Boolean> hasTypesIterator = hasTypes.iterator();
        while (typesIterator.hasNext()) {
            final T type = CheckUtils.checkArgumentNotNull(typesIterator.next(), "A type must not be null.");
            final Boolean hasType =
                CheckUtils.checkArgumentNotNull(hasTypesIterator.next(), "A hasType Boolean must not be null.");
            colSpecs.add(TypedReaderColumnSpec.create(type, hasType));
        }
        return new TypedReaderTableSpec<>(colSpecs, false);
    }

    /**
     * Convenience method to create a {@link TypedReaderTableSpecBuilder}.
     *
     * @param <T> the type used to identify external data types
     * @return a fresh {@link TypedReaderTableSpecBuilder}
     */
    public static <T> TypedReaderTableSpecBuilder<T> builder() {
        return new TypedReaderTableSpecBuilder<>();
    }

    /**
     * Builder for {@link TypedReaderTableSpec}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param <T> the type used identify external data types
     */
    public static final class TypedReaderTableSpecBuilder<T> {

        private final LinkedHashSet<TypedReaderColumnSpec<T>> m_columns = new LinkedHashSet<>();

        private final Set<String> m_names = new HashSet<>();

        /**
         * Adds a named column.
         *
         * @param name of the column
         * @param type of the column
         * @param hasType {@code true} if the type is definitive, {@code false} if the type couldn't be determined
         *            definitively e.g. because only missing values were present
         * @return this builder
         * @throws IllegalArgumentException if the name is already taken
         */
        public TypedReaderTableSpecBuilder<T> addColumn(final String name, final T type, final boolean hasType) {
            checkName(name);
            final TypedReaderColumnSpec<T> column = TypedReaderColumnSpec.createWithName(name, type, hasType);
            m_columns.add(column);
            m_names.add(name);
            return this;
        }

        private void checkName(final String name) {
            CheckUtils.checkArgument(!m_names.contains(name), "The name '%s' is already taken.");
        }

        /**
         * Adds a column without name.
         *
         * @param type of the column
         * @param hasType {@code true} if the type is definitive, {@code false} if the type couldn't be determined
         *            definitively e.g. because only missing values were present
         * @return this builder
         */
        public TypedReaderTableSpecBuilder<T> addColumn(final T type, final boolean hasType) {
            final TypedReaderColumnSpec<T> column = TypedReaderColumnSpec.create(type, hasType);
            m_columns.add(column);
            return this;
        }

        /**
         * Adds the provided column.
         *
         * @param column to add
         * @return this builder
         */
        public TypedReaderTableSpecBuilder<T> addColumn(final TypedReaderColumnSpec<T> column) {
            Optional<String> name = column.getName();
            if (name.isPresent()) {
                checkName(name.get());
                m_names.add(name.get());
            }
            m_columns.add(column);
            return this;
        }

        /**
         * Builds a {@link TypedReaderTableSpec} containing the set of columns added so far.
         *
         * @return a {@link TypedReaderTableSpec} corresponding to the current state of the builder
         */
        public TypedReaderTableSpec<T> build() {
            return new TypedReaderTableSpec<>(m_columns);
        }
    }

}
