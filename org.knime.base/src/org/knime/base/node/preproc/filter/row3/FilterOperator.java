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

import java.util.Objects;
import java.util.function.BiPredicate;

import org.knime.base.node.preproc.filter.row3.RowReadPredicate.BooleanValuePredicate;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.message.Message;
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
enum FilterOperator {

        @Label(value = "Equals", description = "Value in column must be <b>equal</b> to specified value")
        EQ("Equals", new IsEq(), null, true),
        @Label(value = "Does not equal", description = "Value in column must be <b>not equal</b> to specified value")
        NEQ("Does not equal", new IsEq(), null, true),

        @Label(value = "Less than", //
               description = "Value in column must be <b>strictly smaller</b> than specified value") //
        LT("Less than", new IsOrd(), new BoundedNumeric(), true), //
        @Label(value = "Less than or equal", //
               description = "Value in column must be <b>smaller than or equal</b> to specified value") //
        LTE("Less than or equal", new IsOrd(), new BoundedNumeric(), true), //
        @Label(value = "Greater than", //
               description = "Value in column must be <b>strictly larger</b> than specified value") //
        GT("Greater than", new IsOrd(), new BoundedNumeric(), true), //
        @Label(value = "Greater than or equal", //
            description = "Value in column must be <b>larger than or equal</b> than specified value") //
        GTE("Greater than or equal", new IsOrd(), new BoundedNumeric(), true), //

        @Label(value = "First <i>n</i> rows",
            description = "Matches the specified number of rows at the start of the input")
        FIRST_N_ROWS("First n rows", new IsRowNumber(), null, true), //
        @Label(value = "Last <i>n</i> rows",
            description = "Matches the specified number of rows at the end of the input")
        LAST_N_ROWS("Last n rows", new IsRowNumber(), null, true), //

        @Label(value = "Matches regex", description = "Value in column must match the specified regular expression")
        REGEX("Matches regex", new IsPatternMatchable(), null, true),
        @Label(value = "Matches wildcard", description = "Value in column must match the specified pattern, "
                + "which may contain wildcards <tt>*</tt> and <tt>?</tt>")
        WILDCARD("Matches wildcard", new IsPatternMatchable(), null, true),

        @Label(value = "Is true", description = "Boolean value in column must be <tt>true</tt>")
        IS_TRUE("Is true", new IsTruthy(), null, false),
        @Label(value = "Is false", description = "Boolean value in column must be <tt>false</tt>")
        IS_FALSE("Is false", new IsTruthy(), null, false),

        @Label(value = "Is missing", description = "Value in column must be <i>missing</i>")
        IS_MISSING("Is missing", new IsMissing(), null, false),
        @Label(value = "Is not missing", description = "Value in column must <em>not</em> be <i>missing</i>")
        IS_NOT_MISSING("Is not missing", new IsMissing(), null, false);

    final String m_label;

    private final InputSpecValidator m_validator;

    private final BiPredicate<SpecialColumns, DataType> m_isOffered;

    final boolean m_isBinary;

    FilterOperator(final String label, final InputSpecValidator validator,
        final BiPredicate<SpecialColumns, DataType> isOffered, final boolean isBinary) {
        m_label = label;
        m_validator = validator;
        m_isOffered = isOffered != null ? isOffered : validator;
        m_isBinary = isBinary;
    }

    private interface InputSpecValidator extends BiPredicate<SpecialColumns, DataType> {

        default void validate(final DataColumnSpec colSpec, final DynamicValuesInput predicateValues)
            throws InvalidSettingsException {
            predicateValues.checkParseError();
            validateImpl(colSpec, predicateValues);
        }

        void validateImpl(DataColumnSpec colSpec, DynamicValuesInput predicateValues) throws InvalidSettingsException;
    }

    boolean isApplicableFor(final SpecialColumns specialColumn, final DataType dataType) {
        return m_validator.test(specialColumn, dataType);
    }

    boolean isOfferedFor(final SpecialColumns specialColumn, final DataType dataType) {
        return m_isOffered.test(specialColumn, dataType);
    }

    void validate(final DataColumnSpec colSpec, final DynamicValuesInput predicateValues)
        throws InvalidSettingsException {
        m_validator.validate(colSpec, predicateValues);
    }

    String label() {
        return m_label;
    }

    static final class IsMissing implements InputSpecValidator {
        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            // All non-special columns can be checked for missing values
            return specialColumn == null;
        }

        @Override
        public void validateImpl(final DataColumnSpec colSpec, final DynamicValuesInput predicateValues) {
            // no-op
        }
    }

    /**
     * Defines for which data types we want to offer ordering-based operators in the UI. If you want to
     * test whether the type supports ordering -- according to our custom definition -- use {@link IsOrd}.
     */
    static final class BoundedNumeric implements BiPredicate<SpecialColumns, DataType> {
        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            return dataType != BooleanCell.TYPE && dataType != StringCell.TYPE // explicitly disabled cell types
                && dataType.isCompatible(BoundedValue.class); // explicitly enabled value classes
        }
    }

    static final class IsOrd implements InputSpecValidator {
        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            // In practice all cells bring a comparator, but not all implementations offer something more "meaningful"
            // than a lexicographic ordering. Still, we want to disallow ordering for boolean and string cells
            // explicitly for now.
            return dataType != BooleanCell.TYPE && dataType != StringCell.TYPE;
        }

        @Override
        public void validateImpl(final DataColumnSpec colSpec, final DynamicValuesInput predicateValues)
            throws InvalidSettingsException {
            predicateValues.validate(colSpec);
        }
    }

    static final class IsEq implements InputSpecValidator {

        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            // our filtering can always use equality
            // but booleans are handled with "is true" and "is false" operators
            return !BooleanValuePredicate.isApplicableFor(dataType);
        }

        @Override
        public void validateImpl(final DataColumnSpec colSpec, final DynamicValuesInput predicateValues)
            throws InvalidSettingsException {
            DataType refCellType = predicateValues.getCellAt(0).orElseThrow().getType();
            DataType inputType = colSpec.getType();
            if (!Objects.equals(refCellType.getPreferredValueClass(), inputType.getPreferredValueClass())) {
                final var inputName = colSpec.getName();
                throw Message.builder()
                    .withSummary("Cannot compare column \"%s\" for (in)equality.".formatted(inputName))
                    .addTextIssue(
                        "Table column \"%s\" is of type \"%s\", but reference value is of incompatible type \"%s\"."
                            .formatted(inputName, inputType.toPrettyString(), refCellType.toPrettyString()))
                    .addResolutions("Review configuration if the selected column is correct",
                        "Convert the input column to \"%s\" using a converter node"
                            .formatted(refCellType.toPrettyString()))
                    .build().orElseThrow().toInvalidSettingsException();
            }
        }
    }

    static final class IsTruthy implements InputSpecValidator {
        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            return specialColumn == null && BooleanValuePredicate.isApplicableFor(dataType);
        }

        @Override
        public void validateImpl(final DataColumnSpec colSpec, final DynamicValuesInput predicateValues)
            throws InvalidSettingsException {
            if (!colSpec.getType().isCompatible(BooleanValue.class)) {
                throw Message.builder()
                    .withSummary("Cannot apply boolean operators to "
                        + "column \"%s\" of type \"%s\" - it is not boolean-compatible.".formatted(colSpec.getName(),
                            colSpec.getType().toPrettyString()))
                    .addResolutions("Review configuration if the selected column is correct",
                        "Convert the input column to boolean using a converter node, e.g. an expression node")
                    .build().orElseThrow().toInvalidSettingsException();
            }
        }
    }

    static final class IsPatternMatchable implements InputSpecValidator {
        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            return dataType == StringCell.TYPE;
        }

        @Override
        public void validateImpl(final DataColumnSpec colSpec, final DynamicValuesInput predicateValues)
            throws InvalidSettingsException {
            if (!colSpec.getType().isCompatible(StringValue.class)) {
                throw Message.builder()
                    .withSummary("Cannot apply string pattern matching to "
                        + "column \"%s\" of type \"%s\" - it is not string-compatible.".formatted(colSpec.getName(),
                            colSpec.getType().toPrettyString()))
                    .addResolutions("Review configuration if the selected column is correct",
                        "Convert the input column to string using a converter node")
                    .build().orElseThrow().toInvalidSettingsException();
            }
        }
    }

    static final class IsRowNumber implements InputSpecValidator {
        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            return specialColumn == SpecialColumns.ROW_NUMBERS;
        }

        @Override
        public void validateImpl(final DataColumnSpec colSpec, final DynamicValuesInput predicateValues)
            throws InvalidSettingsException {
            LongValue count = (LongValue)predicateValues.getCellAt(0).orElseThrow();
            if (count.getLongValue() < 0) {
                throw new InvalidSettingsException("Row number must be non-negative: " + count.getLongValue());
            }
        }
    }

}
