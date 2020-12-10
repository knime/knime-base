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

import static org.knime.core.node.util.CheckUtils.checkArgumentNotNull;

import java.util.Objects;

/**
 * Representation of a column as a type and an optional name.</br>
 * The name is optional because it should only be set if it is read from the data.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> type used to identify types
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class TypedReaderColumnSpec<T> extends DefaultReaderColumnSpec {

    @SuppressWarnings("rawtypes")
    private static final TypedReaderColumnSpec NULL = new TypedReaderColumnSpec<>(null, null, false);

    /**
     * Returns a null-object.
     *
     * @param <T> the type used to represent external data types
     * @return the null-object
     */
    @SuppressWarnings("unchecked")
    public static <T> TypedReaderColumnSpec<T> getNull() {
        return NULL;
    }

    private final T m_type;

    private final int m_hashCode;

    private final boolean m_hasType;

    /**
     * Constructor to be used if the column has a name read from the data.
     *
     * @param name the name of the column read from the data
     * @param type the most specific type all observed values in the column share
     * @param hasType if the column has a type
     */
    private TypedReaderColumnSpec(final String name, final T type, final boolean hasType) {
        super(name);
        m_type = type;
        m_hasType = hasType;
        m_hashCode = super.hashCode() * Objects.hash(m_type) * Objects.hash(m_hasType);
    }

    /**
     * Creates a new {@link TypedReaderColumnSpec} with the provided <b>name</b> and <b>type</b>.
     *
     * @param name of the column
     * @param hasType if the column has a type
     * @param type of the column
     * @return the new {@link TypedReaderColumnSpec}
     */
    public static <T> TypedReaderColumnSpec<T> createWithName(final String name, final T type, final boolean hasType) {
        return new TypedReaderColumnSpec<>(name, checkArgumentNotNull(type, "The 'type' argument must not be null"),
            hasType);
    }

    /**
     * Creates a new {@link TypedReaderColumnSpec} with the provided <b>type</b> and <b>hasType</b>.
     *
     * @param type of the column
     * @param hasType if the column has a type
     * @return the new {@link TypedReaderColumnSpec}
     */
    public static <T> TypedReaderColumnSpec<T> create(final T type, final boolean hasType) {
        return new TypedReaderColumnSpec<>(null, checkArgumentNotNull(type, "The 'type' argument must not be null."),
            hasType);
    }

    /**
     * @return the most specific type all observed values in the column share
     */
    public T getType() {
        return m_type;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (super.equals(obj)) {
            // if the T doesn't match, m_type.equals(other.m_type) will return false anyway
            @SuppressWarnings("rawtypes")
            final TypedReaderColumnSpec<?> other = (TypedReaderColumnSpec)obj;
            return m_type.equals(other.m_type) && m_hasType == other.m_hasType;
        }
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder("[")//
            .append(super.toString())//
            .append(", ")//
            .append(m_type).append("]")//
            .append(", hasType: ")//
            .append(m_hasType)//
            .toString();
    }

    @Override
    public int hashCode() {
        return m_hashCode;
    }

    /**
     * Returns {@code true} if the column spec has a type. For example, a column that was empty or filled with only
     * missing values may not have a type.
     *
     * @return {@code true} if the column spec has a type
     */
    public boolean hasType() {
        return m_hasType;
    }

}
