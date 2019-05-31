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

package org.knime.base.data.filter.row.dialog.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * An operation container object.
 *
 * @author Viktor Buria
 * @since 4.0
 */
public class Operation {

    private Operator m_operator;

    private String[] m_values;

    /**
     * Constructs an {@link Operation}.
     */
    public Operation() {
    }

    /**
     * Constructs an {@link Operation} with operator and operands.
     *
     * @param operator the {@link Operator}
     * @param values the operands
     */
    public Operation(final Operator operator, final String[] values) {
        setOperator(operator);
        setValues(values);
    }

    /**
     * Gets an operator.
     *
     * @return the {@link Operator}
     */
    public Operator getOperator() {
        return m_operator;
    }

    /**
     * Sets an operator object.
     *
     * @param operator the {@link Operator}
     */
    public void setOperator(final Operator operator) {
        m_operator = operator;
    }

    /**
     * Gets operands for an operator.
     *
     * @return the operands
     */
    public String[] getValues() {
        return m_values;
    }

    /**
     * Sets operands for an operator.
     *
     * @param values the operands
     */
    public void setValues(final String[] values) {
        m_values = values;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Operation)) {
            return false;
        }
        final Operation castOther = (Operation)other;
        return Objects.equals(m_operator, castOther.m_operator) && Arrays.equals(m_values, castOther.m_values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_operator, m_values);
    }

    @Override
    public String toString() {
        return "Operation [m_operator=" + m_operator + ", m_values=" + Arrays.toString(m_values) + "]";
    }

}
