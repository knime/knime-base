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
 *   May 26, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.selector;

import java.util.Objects;

import org.knime.core.data.DataType;

/**
 * Immutable implementation of {@link UnknownColumnsTransformation}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class ImmutableUnknownColumnsTransformation implements UnknownColumnsTransformation {

    private final DataType m_forcedType;

    private final int m_position;

    private final boolean m_keep;

    private final boolean m_forceType;

    /**
     * Constructor.
     *
     * @param position at which unknown columns are to be inserted
     * @param keep whether unknown columns should be included
     * @param forceType whether unknown columns are converted to a user specified type
     * @param forcedType the type unknown columns are converted to (can be {@code null} if {@code forceType} is
     *            {@code false}
     */
    public ImmutableUnknownColumnsTransformation(final int position, final boolean keep, final boolean forceType,
        final DataType forcedType) {
        m_position = position;
        m_keep = keep;
        m_forceType = forceType;
        m_forcedType = forcedType;
    }

    /**
     * Copy constructor that extracts all values from the provided transformation.
     *
     * @param toCopy transformation to extract values from
     */
    public ImmutableUnknownColumnsTransformation(final UnknownColumnsTransformation toCopy) {
        m_position = toCopy.getPosition();
        m_keep = toCopy.keep();
        m_forceType = toCopy.forceType();
        m_forcedType = toCopy.getForcedType();
    }

    @Override
    public int getPosition() {
        return m_position;
    }

    @Override
    public boolean keep() {
        return m_keep;
    }

    @Override
    public boolean forceType() {
        return m_forceType;
    }

    @Override
    public DataType getForcedType() {
        return m_forcedType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (m_forceType ? 1231 : 1237);
        result = prime * result + ((m_forcedType == null) ? 0 : m_forcedType.hashCode());
        result = prime * result + (m_keep ? 1231 : 1237);
        result = prime * result + m_position;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() == obj.getClass()) {
            final ImmutableUnknownColumnsTransformation other = (ImmutableUnknownColumnsTransformation)obj;
            return m_forceType == other.m_forceType//
                && m_keep == other.m_keep//
                && m_position == other.m_position//
                && Objects.equals(m_forcedType, other.m_forcedType);
        }
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder("{position: ")//
            .append(m_position)//
            .append(", keep: ")//
            .append(m_keep)//
            .append(", forceType: ")//
            .append(m_forceType)//
            .append(", forcedType: ")//
            .append(m_forcedType)//
            .append("}").toString();
    }

}
