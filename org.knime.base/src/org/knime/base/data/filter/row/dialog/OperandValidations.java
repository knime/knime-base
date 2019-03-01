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

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Library of essential operand validations.
 *
 * @author Viktor Buria
 * @since 3.8
 */
public final class OperandValidations {

    /**
     * Error message if empty value is not allowed.
     */
    private static final String ERROR_COLUMN_EMPTY_VALUE = "Value is empty";

    /**
     * Prefix of the "cannot convert to <em>type</em>" error message.
     */
    public static final String ERROR_PREFIX_CANNOT_CONVERT_TO = "Problem with types, cannot convert to ";

    /**
     * Error message if value can not be converter to the boolean type.
     */
    private static final String ERROR_CANNOT_CONVERT_TO_BOOLEAN = ERROR_PREFIX_CANNOT_CONVERT_TO + "boolean";

    /**
     * Error message if value can not be converter to the integer type.
     */
    private static final String ERROR_CANNOT_CONVERT_TO_INTEGER = ERROR_PREFIX_CANNOT_CONVERT_TO + "integer";

    /**
     * Error message if value can not be converter to the long type.
     */
    private static final String ERROR_CANNOT_CONVERT_TO_LONG = ERROR_PREFIX_CANNOT_CONVERT_TO + "long";

    /**
     * Error message if value can not be converter to the double type.
     */
    private static final String ERROR_CANNOT_CONVERT_TO_DOUBLE = ERROR_PREFIX_CANNOT_CONVERT_TO + "double";

    /**
     * Error message if value can not be converter to the {@link LocalDate} type.
     */
    private static final String ERROR_CANNOT_CONVERT_TO_DATE = ERROR_PREFIX_CANNOT_CONVERT_TO + "local date";

    /**
     * Error message if value can not be converter to the {@link LocalDateTime} type.
     */
    private static final String ERROR_CANNOT_CONVERT_TO_DATE_TIME =
        ERROR_PREFIX_CANNOT_CONVERT_TO + "local date & time";

    /**
     * Error message if value can not be converter to the {@link LocalTime} type.
     */
    private static final String ERROR_CANNOT_CONVERT_TO_TIME = ERROR_PREFIX_CANNOT_CONVERT_TO + "local time";

    /**
     * Checks that value is not {@code null} or blank.
     *
     * @param value the given value
     * @return an error message or {@link Optional}.empty()
     */
    public static Optional<String> notBlankValue(final String value) {
        return value == null || StringUtils.isBlank(value) ? Optional.of(ERROR_COLUMN_EMPTY_VALUE) : Optional.empty();
    }

    /**
     * Checks that value could be converted to {@link Boolean}.
     *
     * @param value the given value
     * @return the error message or an {@link Optional}.empty() if no error
     */
    public static Optional<String> canParseBoolean(final String value) {
        if (value != null && (value.trim().equalsIgnoreCase("true") || value.trim().equalsIgnoreCase("false"))) {
            return Optional.empty();
        } else {
            return Optional.of(ERROR_CANNOT_CONVERT_TO_BOOLEAN);
        }
    }

    /**
     * Checks that value could be converted to {@link Integer}.
     *
     * @param value the given value
     * @return the error message or an {@link Optional}.empty() if no error
     */
    public static Optional<String> canParseInteger(final String value) {
        try {
            Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return Optional.of(ERROR_CANNOT_CONVERT_TO_INTEGER);
        }
        return Optional.empty();
    }

    /**
     * Checks that value could be converted to {@link Long}.
     *
     * @param value the given value
     * @return the error message or an {@link Optional}.empty() if no error
     */
    public static Optional<String> canParseLong(final String value) {
        try {
            Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return Optional.of(ERROR_CANNOT_CONVERT_TO_LONG);
        }
        return Optional.empty();
    }

    /**
     * Checks that value could be converted to {@link Double}.
     *
     * @param value the given value
     * @return the error message or an {@link Optional}.empty() if no error
     */
    public static Optional<String> canParseDouble(final String value) {
        try {
            Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return Optional.of(ERROR_CANNOT_CONVERT_TO_DOUBLE);
        }
        return Optional.empty();
    }

    /**
     * Trying to {@link Date} conversion.
     *
     * @param value the given value
     * @return the error message or an {@link Optional}.empty() if no error
     */
    public static Optional<String> canParseDate(final String value) {
        try {
            LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return Optional.of(ERROR_CANNOT_CONVERT_TO_DATE);
        }
        return Optional.empty();
    }

    /**
     * Trying to {@link Date} conversion.
     *
     * @param value the given value
     * @return the error message or an {@link Optional}.empty() if no error
     */
    public static Optional<String> canParseDateTime(final String value) {
        try {
            LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            return Optional.of(ERROR_CANNOT_CONVERT_TO_DATE_TIME);
        }
        return Optional.empty();
    }

    /**
     * Checks that value could be converted to {@link Double}.
     *
     * @param value the given value
     * @return the error message or an {@link Optional}.empty() if no error
     */
    public static Optional<String> canParseTime(final String value) {
        try {
            LocalTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            return Optional.of(ERROR_CANNOT_CONVERT_TO_TIME);
        }
        return Optional.empty();
    }

    private OperandValidations() {
        throw new UnsupportedOperationException();
    }

}
