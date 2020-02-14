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
import java.util.Optional;

/**
 * Representation of a column as a type and an optional name.</br>
 * The name is optional because it should only be set if it is read from the data.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> type used to identify types
 */
public final class ReaderColumnSpec<T> {

    private final String m_name;

    private final T m_type;

    private final int m_hashCode;

    /**
     * Constructor to be used if the column has a name read from the data.
     *
     * @param name the name of the column read from the data
     * @param type the most specific type all observed values in the column share
     */
    private ReaderColumnSpec(final String name, final T type) {
        m_name = name;
        m_type = type;
        m_hashCode = Objects.hash(m_name, m_type);
    }

    /**
     * Creates a new {@link ReaderColumnSpec} with the provided <b>name</b> and <b>type</b>.
     *
     * @param name of the column
     * @param type of the column
     * @return the new {@link ReaderColumnSpec}
     */
    public static <T> ReaderColumnSpec<T> createWithName(final String name, final T type) {
        return new ReaderColumnSpec<>(name, checkArgumentNotNull(type, "The 'type' argument must not be null"));
    }

    /**
     * Creates a new {@link ReaderColumnSpec} with the provided <b>type</b>.
     *
     * @param type of the column
     * @return the new {@link ReaderColumnSpec}
     */
    public static <T> ReaderColumnSpec<T> create(final T type) {
        return new ReaderColumnSpec<>(null, checkArgumentNotNull(type, "The 'type' argument must not be null."));
    }

    /**
     * @return the name of the column as read from the data or {@link Optional#empty()} if the data doesn't provide a
     *         column name
     */
    public Optional<String> getName() {
        return Optional.ofNullable(m_name);
    }

    /**
     * @return the most specific type all observed values in the column share
     */
    public T getType() {
        return m_type;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof ReaderColumnSpec) {
            // if the T doesn't match, m_type.equals(other.m_type) will return false anyway
            @SuppressWarnings("rawtypes")
            final ReaderColumnSpec other = (ReaderColumnSpec)obj;
            return m_type.equals(other.m_type) && Objects.equals(m_name, other.m_name);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        if (m_name == null) {
            sb.append("<no name>");
        } else {
            sb.append(m_name);
        }
        sb.append(", ").append(m_type).append("]");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return m_hashCode;
    }

}
