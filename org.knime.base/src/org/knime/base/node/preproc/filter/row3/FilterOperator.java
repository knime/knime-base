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

import org.knime.base.node.preproc.filter.row3.predicates.PredicateFactories;
import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;

/**
 * Enumeration of filter operators for the row filter node. Additionally, encoded in this class is the selection logic
 * for which operators are applicable to which columns and comparison modes.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // new ui
public enum FilterOperator {

        /** Equals. */
        @Label(value = "Equals", description = "Value in column must be <b>equal</b> to specified value")
        EQ("Equals"),

        /** Does not equal. */
        @Label(value = "Does not equal", description = "Value in column must be <b>not equal</b> to specified value")
        NEQ("Does not equal"),

        /** Less than. */
        @Label(value = "Less than", //
            description = "Value in column must be <b>strictly smaller</b> than specified value") //
        LT("Less than"), //

        /** Less than or equal. */
        @Label(value = "Less than or equal", //
            description = "Value in column must be <b>smaller than or equal</b> to specified value") //
        LTE("Less than or equal"), //

        /** Greater than. */
        @Label(value = "Greater than", //
            description = "Value in column must be <b>strictly larger</b> than specified value") //
        GT("Greater than"), //

        /** Greater than or equal. */
        @Label(value = "Greater than or equal", //
            description = "Value in column must be <b>larger than or equal</b> than specified value") //
        GTE("Greater than or equal"), //

        /** First n rows. */
        @Label(value = "First <i>n</i> rows",
            description = "Matches the specified number of rows at the start of the input")
        FIRST_N_ROWS("First n rows"), //

        /** Last n rows. */
        @Label(value = "Last <i>n</i> rows",
            description = "Matches the specified number of rows at the end of the input")
        LAST_N_ROWS("Last n rows"), //

        /** Matches regex. */
        @Label(value = "Matches regex", description = "Value in column must match the specified regular expression")
        REGEX("Matches regex"),

        /** Matches wildcard. */
        @Label(value = "Matches wildcard", description = "Value in column must match the specified pattern, "
            + "which may contain wildcards <tt>*</tt> and <tt>?</tt>")
        WILDCARD("Matches wildcard"),

        /** Is true. */
        @Label(value = "Is true", description = "Boolean value in column must be <tt>true</tt>")
        IS_TRUE("Is true"),

        /** Is false. */
        @Label(value = "Is false", description = "Boolean value in column must be <tt>false</tt>")
        IS_FALSE("Is false"),

        /** Is missing. */
        @Label(value = "Is missing", description = "Value in column must be <i>missing</i>")
        IS_MISSING("Is missing"),

        /** Is not missing. */
        @Label(value = "Is not missing", description = "Value in column must <em>not</em> be <i>missing</i>")
        IS_NOT_MISSING("Is not missing");

    final String m_label;

    FilterOperator(final String label) {
        m_label = label;
    }

    /**
     * Gets the user-visible label of the operator.
     *
     * @return label of the operator
     */
    public String label() {
        return m_label;
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
            case EQ, NEQ, LT, LTE, GT, GTE, REGEX, WILDCARD, FIRST_N_ROWS, LAST_N_ROWS -> true;
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
    boolean isApplicableFor(final SpecialColumns specialColumn, final DataType dataType) {
        // we only need to check if the input supports the data type in case of a binary operator
        final var inputSupportsDataType = !isBinary() || DynamicValuesInput.supportsDataType(dataType);
        return inputSupportsDataType && switch (this) {
            // special columns are never missing, so that would be invalid
            case IS_MISSING, IS_NOT_MISSING -> specialColumn == null;
            // a factory is not required for the Row Numbers special column, since we don't actually access anything
            // from the RowRead
            case FIRST_N_ROWS, LAST_N_ROWS -> specialColumn == SpecialColumns.ROW_NUMBERS;
            // booleans are handled with these two operators
            case IS_TRUE, IS_FALSE -> dataType.equals(BooleanCell.TYPE);
            // we need to exclude booleans from the remaining test
            case LT, LTE, GT, GTE, EQ, NEQ, REGEX, WILDCARD -> !dataType.equals(BooleanCell.TYPE)
                && PredicateFactories.getValuePredicateFactory(this, dataType).isPresent();
        };
    }

    /**
     * Checks if the operator should be hidden for the given column.
     *
     * @param specialColumn special column flag
     * @param dataType data type of the column
     * @return {@code true} if the operator should be hidden, {@code false} otherwise
     */
    boolean isHidden(final SpecialColumns specialColumn, final DataType dataType) {
        final var hide = switch (this) {
            // we hide ordering operators for non-bounded values, but they can still be used to filter if
            // if configured via flow variable or 5.3.0 instance of the node
            case LT, LTE, GT, GTE -> !dataType.isCompatible(BoundedValue.class);
            default -> false;
        };
        return hide || !isApplicableFor(specialColumn, dataType);
    }

    void validate(final DataColumnSpec colSpec, final int columnIndex, final DynamicValuesInput predicateValues)
        throws InvalidSettingsException {
        // validation is just to try building a predicate with the operator
        RowReadPredicate.translateToPredicate(this, predicateValues, columnIndex,
            colSpec == null ? StringCell.TYPE : colSpec.getType());
    }

}
