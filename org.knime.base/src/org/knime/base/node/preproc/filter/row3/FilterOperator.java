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

import java.util.function.BiPredicate;

import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;

/**
 * Enumeration of filter operators for the row filter node. Additionally, encoded in this class is the selection logic
 * for which operators are applicable to which columns and comparison modes.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // new ui
enum FilterOperator {

        @Label(value = "=", description = "Value in column must be <b>equal</b> to specified value")
        EQ("=", new IsEq(), true),
        @Label(value = "≠", description = "Value in column must be <b>not equal</b> to specified value")
        NEQ("≠", new IsEq(), true),
        @Label(value = "&lt;", description = "Value in column must be <b>strictly smaller</b> than specified value")
        LT("<", new IsOrd(), true),
        @Label(value = "≤", description = "Value in column must be <b>smaller than or equal</b> to specified value")
        LTE("≤", new IsOrd(), true),
        @Label(value = "&gt;", description = "Value in column must be <b>strictly larger</b> than specified value")
        GT(">", new IsOrd(), true),
        @Label(value = "≥",
            description = "Value in column must be <b>larger than or equal</b> than specified value")
        GTE("≥", new IsOrd(), true),

        @Label(value = "First <i>n</i> rows",
            description = "Matches the specified number of rows at the start of the input")
        FIRST_N_ROWS("First n rows", new IsRowNumber(), true), //
        @Label(value = "Last <i>n</i> rows",
            description = "Matches the specified number of rows at the end of the input")
        LAST_N_ROWS("Last n rows", new IsRowNumber(), true), //

        @Label(value = "Matches regex", description = "Value in column must match the specified regular expression")
        REGEX("Matches regex", new IsPatternMatchable(), true),
        @Label(value = "Matches wildcard", description = "Value in column must match the specified pattern, "
                + "which may contain wildcards <tt>*</tt> and <tt>?</tt>")
        WILDCARD("Matches wildcard", new IsPatternMatchable(), true),

        @Label(value = "Is true", description = "Boolean value in column must be <tt>true</tt>")
        IS_TRUE("Is true", new IsTruthy(), false),
        @Label(value = "Is false", description = "Boolean value in column must be <tt>false</tt>")
        IS_FALSE("Is false", new IsTruthy(), false),

        @Label(value = "Is missing", description = "Value in column must be <i>missing</i>")
        IS_MISSING("Is missing", new IsMissing(), false);

    final String m_label;

    final BiPredicate<SpecialColumns, DataType> m_capability;

    boolean m_isBinary;

    FilterOperator(final String label, final BiPredicate<SpecialColumns, DataType> capability, final boolean isBinary) {
        m_label = label;
        m_capability = capability;
        m_isBinary = isBinary;
    }

    boolean isEnabledFor(final SpecialColumns specialColumn, final DataType dataType) {
        return m_capability.test(specialColumn, dataType);
    }

    String label() {
        return m_label;
    }

    // order for any datatype except for `boolean` and `String`
    static final class IsOrd implements BiPredicate<SpecialColumns, DataType> {
        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            return !(BooleanCell.TYPE.equals(dataType) || StringCell.TYPE.equals(dataType));
        }
    }

    static final class IsEq implements BiPredicate<SpecialColumns, DataType> {
        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            // string-based filtering can always use equality
            return dataType.isCompatible(StringValue.class)
                // booleans are handled with "is true" and "is false" operators
                || dataType != BooleanCell.TYPE;
        }
    }

    static final class IsTruthy implements BiPredicate<SpecialColumns, DataType> {
        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            return specialColumn == null && dataType == BooleanCell.TYPE;
        }
    }

    static final class IsMissing implements BiPredicate<SpecialColumns, DataType> {
        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            // All non-special columns can be checked for missing values
            return specialColumn == null;
        }
    }

    static final class IsPatternMatchable implements BiPredicate<SpecialColumns, DataType> {
        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            return StringCell.TYPE.equals(dataType);
        }
    }

    static final class IsRowNumber implements BiPredicate<SpecialColumns, DataType> {
        @Override
        public boolean test(final SpecialColumns specialColumn, final DataType dataType) {
            return specialColumn == SpecialColumns.ROW_NUMBERS;
        }
    }

}
