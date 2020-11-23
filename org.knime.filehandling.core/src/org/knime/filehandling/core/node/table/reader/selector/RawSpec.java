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
 *   Oct 14, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.selector;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * Represents the tuple of the union and intersection of all individual specs.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> type used to identify external data types
 */
public final class RawSpec<T> {

    private final TypedReaderTableSpec<T> m_union;

    private final TypedReaderTableSpec<T> m_intersection;

    private final int m_hashCode;

    /**
     * Constructor.
     *
     * @param union of all {@link TypedReaderTableSpec individual specs}
     * @param intersection of all {@link TypedReaderTableSpec individual specs}
     */
    public RawSpec(final TypedReaderTableSpec<T> union, final TypedReaderTableSpec<T> intersection) {
        m_union = union;
        m_intersection = intersection;
        m_hashCode = new HashCodeBuilder().append(union).append(intersection).build();
    }

    /**
     * Returns the union {@link TypedReaderTableSpec}.
     *
     * @return the union {@link TypedReaderTableSpec}
     */
    public TypedReaderTableSpec<T> getUnion() {
        return m_union;
    }

    /**
     * Returns the intersection {@link TypedReaderTableSpec}.
     *
     * @return the intersection {@link TypedReaderTableSpec}
     */
    public TypedReaderTableSpec<T> getIntersection() {
        return m_intersection;
    }

    @Override
    public String toString() {
        return String.format("<Union: %s Intersection: %s>", m_union, m_intersection);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object obj) { // NOSONAR
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() == getClass()) {
            final RawSpec<T> rawSpec = (RawSpec<T>)obj;
            return m_union.equals(rawSpec.getUnion()) && m_intersection.equals(rawSpec.getIntersection());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return m_hashCode;
    }

}
