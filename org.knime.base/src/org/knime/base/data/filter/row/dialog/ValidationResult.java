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

package org.knime.base.data.filter.row.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Result of the operator parameters validation.
 *
 * @author Viktor Buria
 * @since 4.0
 */

public class ValidationResult {
    /**
     * ID for error messages which are common for multiple operands or the operator under a test.
     */
    public static final int COMMON_ERROR_ID = -1;

    private final List<OperandError> m_errors = new ArrayList<>();

    /**
     * Constructs a {@link ValidationResult} object.
     */
    public ValidationResult() {

    }

    /**
     * Constructs a {@link ValidationResult} object with one registered error on board.
     *
     * @param operandIdx an index of the operator parameter.
     * @param message the error message.
     */
    public ValidationResult(final int operandIdx, final String message) {
        addError(operandIdx, message);// NOSONAR can't fix because it's API
    }

    /**
     * Checks an error presents status.
     *
     * @return {@code true} if error(s) presents, otherwise {@code false}
     */
    public boolean hasErrors() {
        return !m_errors.isEmpty();
    }

    /**
     * Adds an error record for the given operator parameter index.
     *
     * @param operandIdx an index of the operator parameter.
     * @param message the error message.
     */
    public void addError(final int operandIdx, final String message) {
        if (operandIdx < COMMON_ERROR_ID) {
            throw new IllegalArgumentException("operandIdx should be greater than or equal to " + COMMON_ERROR_ID);
        }
        m_errors.add(new OperandError(operandIdx, Objects.requireNonNull(message, "message")));
    }

    /**
     * Gets all errors.
     *
     * @return the list of {@link OperandError}.
     */
    public List<OperandError> getErrors() {
        return m_errors;
    }

    /**
     * Operand's error container object.
     *
     * @author Viktor Buria
     */
    public static final class OperandError {
        private final int m_operandIdx;

        private final String m_error;

        private OperandError(final int operandIdx, final String error) {
            m_operandIdx = operandIdx;
            m_error = error;
        }

        /**
         * Gets the index of a parameter with an error.
         *
         * @return the index of an operator parameter
         */
        public int getIdx() {
            return m_operandIdx;
        }

        /**
         * Gets an error message.
         *
         * @return the error message
         */
        public String getError() {
            return m_error;
        }

    }

}
