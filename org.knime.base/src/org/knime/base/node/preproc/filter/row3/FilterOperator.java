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
 *   25 Mar 2024 (jasper): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.util.Optional;
import java.util.OptionalInt;

import org.knime.base.data.filter.row.v2.IndexedRowReadPredicate;
import org.knime.base.node.preproc.filter.row3.predicates.PredicateFactories;
import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicValuesInput;
import org.knime.node.parameters.widget.choices.Label;

/**
 * Enumeration of filter operators for the row filter node. Additionally, encoded in this class is the selection logic
 * for which operators are applicable to which columns and comparison modes.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // new ui
public enum FilterOperator {

        /** Operator checking equality between values. */
        @Label(value = "Equals", description =
                """
                Value in column must be <b>equal</b> to the specified reference value.
                Equality is define by the particular data type(s) involved and may be on the value's string
                representation.
                """)
        EQ,

        /**
         * Operator checking inequality between values. In particular, two missing cells are considered neither equal
         * nor non-equal to each other (following the SQL semantic of nullable comparison with {@code !=}/{@code <>}).
         * See also {@link #NEQ_MISS}.
         */
        @Label(value = "Is not equal (nor missing)", description =
                """
                Value in column must be <b>not equal</b> to specified reference and also not missing.
                """)
        NEQ,

        /**
         * Operator checking inequality between values, but allowing for missing values. See also {@link #NEQ}.
         */
        @Label(value = "Is not equal", description =
                """
                Value in column must be <b>not equal</b> to specified reference value but can be missing.
                """)
        NEQ_MISS(true),

        /** Operator checking that the left-hand-side value is strictly less than the right-hand-side value. */
        @Label(value = "Less than", description =
        """
        Value in column must be <b>strictly smaller</b> than specified value.
        <br />

        This operator is applicable for all data types that offer a more meaningful ordering than just
        lexicographic ordering. In particular, this includes by default numeric types and Date &amp; Time types.
        String and Boolean types are not supported.
        The same requirements apply to the other ordering-based operators: "Less than", "Less than or equal",
        "Greather than", and "Greater than or equal".
        """) //
        LT, //

        /** Operator checking that the lhs value is less than or equal to the rhs value. */
        @Label(value = "Less than or equal", //
            description = "Value in column must be <b>smaller than or equal</b> to specified value") //
        LTE, //

        /** Operator checking that the lhs value is strictly greater than the rhs value. */
        @Label(value = "Greater than", //
            description = "Value in column must be <b>strictly larger</b> than specified value") //
        GT, //

        /** Operator checking that the lhs value is strictly greater than or equal to the rhs value. */
        @Label(value = "Greater than or equal", //
            description = "Value in column must be <b>larger than or equal</b> than specified value") //
        GTE, //

        /** Operator matching the first {@code n} rows. */
        @Label(value = "First n rows", description =
                """
                Matches the specified number of rows counted from the start of the input.
                """)
        FIRST_N_ROWS, //

        /** Operator matching the last {@code n} rows. */
        @Label(value = "Last n rows", description =
                """
                Matches the specified number of rows counted from the end of the input.
                """)
        LAST_N_ROWS, //

        /** Operator matching the lhs value with the given regular expression. */
        @Label(value = "Matches regex", description =
                """
                Value in column must match the specified regular expression.
                <br />

                This operator is applicable to all data types that are string-compatible,
                i.e. offer a meaningful string representation of themselves, or integral numbers.
                In particular, this includes Date &amp; Time types.
                The same requirements apply to the "Matches wildcard" operator.
                <br /><br />

                <b>Regex matching behavior:</b> By default, the regex pattern must match the whole cell value,
                not just parts of it, since
                the regex pattern is configured with the <tt>DOTALL</tt> and <tt>MULTILINE</tt> flags
                <i>enabled</i>. To disable the <tt>DOTALL</tt> flag, prefix the pattern with <tt>(?-s)</tt>, to disable
                <tt>MULTILINE</tt> use prefix <tt>(?-m)</tt>. To disable both, use <tt>(?-sm)</tt>.
                """)
        REGEX,
        /** Operator matching the lhs value with the given wildcard pattern. */
        @Label(value = "Matches wildcard", description = "Value in column must match the specified pattern, "
                + "which may contain wildcards <tt>*</tt> and <tt>?</tt>.")
        WILDCARD,

        /** Operator checking that the lhs boolean cell value is true. */
        @Label(value = "Is true", description = "Boolean value in column must be <tt>true</tt>")
        IS_TRUE,

        /** Operator checking that the lhs boolean cell value is false. */
        @Label(value = "Is false", description = "Boolean value in column must be <tt>false</tt>")
        IS_FALSE,

        /** Operator checking that the lhs cell is missing. */
        @Label(value = "Is missing", description = "Value in column must be <i>missing</i>")
        IS_MISSING,

        /** Operator checking that the lhs cell is not missing. */
        @Label(value = "Is not missing", description = "Value in column must <em>not</em> be <i>missing</i>")
        IS_NOT_MISSING;

    final boolean m_allowMissing;

    FilterOperator() {
        this(false);
    }

    FilterOperator(final boolean allowMissing) {
        m_allowMissing = allowMissing;
    }

    Optional<DataType> getRequiredInputType() {
        // only Pattern matching needs a special input type
        return Optional.ofNullable(switch (this) {
            case REGEX, WILDCARD -> StringCell.TYPE;
            default -> null;
        });
    }

    boolean isBinary() {
        return switch (this) {
            case EQ, NEQ, NEQ_MISS, LT, LTE, GT, GTE, REGEX, WILDCARD, FIRST_N_ROWS, LAST_N_ROWS -> true;
            case IS_TRUE, IS_FALSE, IS_MISSING, IS_NOT_MISSING -> false;
        };
    }

    /**
     * Checks if the operator is applicable for the given columm.
     *
     * @param specialColumn special column flag
     * @param dataType data type of the column
     * @return {@code true} if the operator is applicable, {@code false} otherwise
     */
    boolean isApplicableFor(final RowIdentifiers specialColumn, final DataType dataType) { // NOSONAR single switch
        // we only need to check if the input supports the data type in case of a binary operator
        final var inputSupportsDataType = !isBinary() || DynamicValuesInput.supportsDataType(dataType);
        return inputSupportsDataType && switch (this) {
            // special columns are never missing, so that would be invalid
            case IS_MISSING, IS_NOT_MISSING -> specialColumn == null;
            // a factory is not required for the Row Numbers special column, since we don't actually access anything
            // from the RowRead
            case FIRST_N_ROWS, LAST_N_ROWS -> specialColumn == RowIdentifiers.ROW_NUMBER;
            // booleans are handled with these two operators
            case IS_TRUE, IS_FALSE -> dataType.equals(BooleanCell.TYPE);
            case NEQ_MISS -> specialColumn == null
                && PredicateFactories.getValuePredicateFactory(this, dataType).isPresent();
            case LT, LTE, GT, GTE, EQ, NEQ, REGEX, WILDCARD ->
                PredicateFactories.getValuePredicateFactory(this, dataType).isPresent();
        };
    }

    /**
     * Checks if the operator should be hidden for the given column.
     *
     * @param specialColumn special column flag
     * @param dataType data type of the column
     * @return {@code true} if the operator should be hidden, {@code false} otherwise
     */
    boolean isHidden(final RowIdentifiers specialColumn, final DataType dataType) {
        final var hide = switch (this) {
            // we hide ordering operators for non-bounded values, but they can still be used to filter if
            // if configured via flow variable or 5.3.0 instance of the node
            case LT, LTE, GT, GTE -> !dataType.isCompatible(BoundedValue.class);
            default -> false;
        };
        return hide || !isApplicableFor(specialColumn, dataType);
    }

    /**
     * Translates the operator and predicate values into a predicate on a {@link RowRead}.
     *
     * @param operator filter operator
     * @param predicateValues values for predicate
     * @param columnIndex index of column to filter on, or negative for row key
     * @param dataType data type of column (or {@link StringCell#TYPE} for row key)
     * @return predicate on a row read
     *
     * @throws InvalidSettingsException in case the arguments are inconsistent, e.g. filtering a row key for missingness
     */
    IndexedRowReadPredicate translateToPredicate(final DynamicValuesInput predicateValues, final int columnIndex,
        final DataType dataType) throws InvalidSettingsException {
        // handle pure missingness tests early
        if (this == FilterOperator.IS_MISSING || this == FilterOperator.IS_NOT_MISSING) {
            // only missing value filter, no value predicate present
            final var isMissing = this == FilterOperator.IS_MISSING;
            return isMissing ? PredicateFactories.IS_MISSING_FACTORY.apply(columnIndex)
                : PredicateFactories.IS_NOT_MISSING_FACTORY.apply(columnIndex);
        }

        // get an actual (non-missing) value predicate
        return translateToValuePredicate(predicateValues, columnIndex, dataType);
    }

    private IndexedRowReadPredicate translateToValuePredicate(final DynamicValuesInput predicateValues,
        final int columnIndex, final DataType dataType) throws InvalidSettingsException {

        final var valuePredicate = PredicateFactories //
            .getValuePredicateFactory(this, dataType) //
            .orElseThrow(() -> new InvalidSettingsException(
                "Unsupported operator for input column type \"%s\"".formatted(dataType.getName())))
            .createPredicate(OptionalInt.of(columnIndex), predicateValues);

        /*
         * Missing value handling:
         *
         * Missing values never match the RowRead predicate.
         * This means the value predicate can (and should since it operates on values, not cells) only be evaluated
         * if it is not missing.
         * The row key cannot be missing, so we have to always evaluate the predicate against it.
         *
         * This results in the following test to determine if the row should pass the filter or not:
         *
         * If the predicate allows for missing values:
         *
         *   (!isRowKey AND isMissing) OR valuePredicate
         *
         * Otherwise, i.e. if the predicate does not allow for missing values:
         *
         *   (isRowKey OR !isMissing) AND valuePredicate
         */

        final var isRowKey = columnIndex < 0;
        if (isRowKey) {
            // The row key cannot be missing, so we just evaluate the value predicate
            return valuePredicate;
        }

        if (m_allowMissing) {
            if (valuePredicate == IndexedRowReadPredicate.TRUE) {
                // if the value predicate is always true, the check will always pass
                return IndexedRowReadPredicate.TRUE;
            }
            // Check for missingness first to ensure the value predicate is only evaluated if the value is present
            return (idx, rowRead) -> rowRead.isMissing(columnIndex) || valuePredicate.test(idx, rowRead);
        } else {
            if (valuePredicate == IndexedRowReadPredicate.FALSE) {
                // if the value predicate is always false, the check will always fail
                return IndexedRowReadPredicate.FALSE;
            }
            // Check for missingness first to ensure the value predicate is only evaluated if the value is present
            return (idx, rowRead) -> !rowRead.isMissing(columnIndex) && valuePredicate.test(idx, rowRead);
        }
    }

}
