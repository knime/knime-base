/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */

package org.knime.base.data.filter.row.dialog.registry;

import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.knime.base.data.filter.row.dialog.model.Operator;
import org.knime.core.data.DataType;

/**
 * A "key" entity of the {@link OperatorRegistry}.
 *
 * @author Viktor Buria
 * @since 4.0
 */
public final class OperatorKey {

    private final DataType m_dataType;

    private final Operator m_operator;

    /**
     * Creates a operator key for chosen operator and any {@link DataType}.
     *
     * @param operator the {@link Operator}
     * @return the {@link OperatorKey}
     */
    public static OperatorKey defaultKey(final Operator operator) {
        return key(null, operator);
    }

    /**
     * Creates a operator key for chosen operator and {@link DataType}.
     *
     * @param dataType the {@link DataType}
     * @param operator the {@link Operator}, not {@code null}.
     * @return the {@link OperatorKey}
     */
    public static OperatorKey key(final DataType dataType, final Operator operator) {
        return new OperatorKey(dataType, operator);
    }

    /**
     * Gets the operator.
     *
     * @return the {@link Operator}
     */
    public Operator getOperator() {
        return m_operator;
    }

    /**
     * Gets the {@link DataType}.
     *
     * @return the {@link DataType}
     */
    public DataType getDataType() {
        return m_dataType;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OperatorKey)) {
            return false;
        }
        final OperatorKey castOther = (OperatorKey)other;
        return new EqualsBuilder().append(m_dataType, castOther.m_dataType).append(m_operator, castOther.m_operator)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(m_dataType).append(m_operator).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("dataType", m_dataType).append("operator", m_operator).toString();
    }

    private OperatorKey(final DataType dataType, final Operator operator) {
        m_dataType = dataType;
        m_operator = Objects.requireNonNull(operator, "operator");
    }
}
