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
 *   12 Dec 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;

/**
 * Tests for the {@link FilterCriterion} class.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class FilterCriterionTest {

    private static final DataTableSpec SPEC = new DataTableSpecCreator() //
        .addColumns( //
            new DataColumnSpecCreator("Int1", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Double1", DoubleCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Bool1", BooleanCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("String1", StringCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Int2", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Long1", LongCell.TYPE).createSpec()) //
        .createSpec();

    @Nested
    static final class Equality {
        /**
         * Tests that RowID can be compared with the default value of the EQ and NEQ operators.
         */
        @Test
        void testEqualityRowID() {
            // RowID with default values
            final var criterion = new FilterCriterion();
            criterion.m_column = SpecialColumns.ROWID.toColumnSelection();
            criterion.m_predicateValues = DynamicValuesInput.forRowID();

            criterion.m_operator = FilterOperator.EQ;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("RowID with default value via EQ") //
                .doesNotThrowAnyException();

            criterion.m_operator = FilterOperator.NEQ;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("RowID with default value via NEQ") //
                .doesNotThrowAnyException();
        }

        /**
         * Tests that "Long" column can be compared with the default value of the EQ and NEQ operators.
         */
        @Test
        void testEqualityLong() {
            // Long column with default values
            final var criterion = new FilterCriterion();

            criterion.m_column = new ColumnSelection(SPEC.getColumnSpec("Long1"));
            criterion.m_predicateValues =
                DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(LongCell.TYPE);

            criterion.m_operator = FilterOperator.EQ;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Long with default value via EQ") //
                .doesNotThrowAnyException();

            criterion.m_operator = FilterOperator.NEQ;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Long with default value via NEQ") //
                .doesNotThrowAnyException();
        }

        /**
         * Tests that "Int" column can be compared with the default value of the EQ and NEQ operators.
         */
        @Test
        void testEqualityInt() {
            // Int column with default values
            final var criterion = new FilterCriterion();

            criterion.m_column = new ColumnSelection(SPEC.getColumnSpec("Int1"));
            criterion.m_predicateValues =
                DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(IntCell.TYPE);

            criterion.m_operator = FilterOperator.EQ;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Int with default value via EQ") //
                .doesNotThrowAnyException();

            criterion.m_operator = FilterOperator.NEQ;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Int with default value via NEQ") //
                .doesNotThrowAnyException();
        }

        /**
         * Tests that comparing a "Long" column with a "String" reference value for EQ and NEQ throws an exception.
         */
        @Test
        void testEqualityException() {
            // Long column with default values
            final var criterion = new FilterCriterion();

            criterion.m_column = new ColumnSelection(SPEC.getColumnSpec("Long1"));
            criterion.m_predicateValues =
                DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(StringCell.TYPE);

            criterion.m_operator = FilterOperator.EQ;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Long with default value via EQ") //
                .hasMessageContaining(
                    "Cannot compare column of type \"Number (long)\" with a value of type \"String\" for equality");

            criterion.m_operator = FilterOperator.NEQ;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Long with default value via NEQ") //
                .hasMessageContaining(
                    "Cannot compare column of type \"Number (long)\" with a value of type \"String\" for inequality");

        }
    }

    @Nested
    static final class BooleanOperators {
        @Test
        void testBooleanCell() {
            // Boolean cell can be filtered by IS_TRUE and IS_FALSE and nothing else.
            final var criterion = new FilterCriterion();

            criterion.m_column = new ColumnSelection(SPEC.getColumnSpec("Bool1"));
            criterion.m_predicateValues =
                DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(BooleanCell.TYPE);

            criterion.m_operator = FilterOperator.IS_TRUE;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Boolean via IS_TRUE") //
                .doesNotThrowAnyException();

            criterion.m_operator = FilterOperator.IS_FALSE;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Boolean via IS_FALSE") //
                .doesNotThrowAnyException();

            criterion.m_operator = FilterOperator.EQ;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Boolean via EQ") //
                .hasMessage("Unsupported operator for input column type \"Boolean value\"");
        }

        @Test
        void testUnsupportedTruthy() {
            // anything other than BooleanCell are not supported by IS_TRUE and IS_FALSE
            final var criterion = new FilterCriterion();
            criterion.m_column = SpecialColumns.ROWID.toColumnSelection();
            criterion.m_predicateValues = DynamicValuesInput.forRowID();

            criterion.m_operator = FilterOperator.IS_TRUE;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("RowID cannot be compared with IS_TRUE") //
                .hasMessage("Unsupported operator \"Is true\" for RowID comparison");

            criterion.m_operator = FilterOperator.IS_FALSE;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("RowID cannot be compared with IS_FALSE") //
                .hasMessage("Unsupported operator \"Is false\" for RowID comparison");
        }
    }

    @Nested
    static final class Ordering {
        @Test
        void testOrdering() {
            // for backwards compatibility with the initial release, we allow any column value to be ordered
            // we just show the operators only for the ones that implement BoundedValue
            final var criterion = new FilterCriterion();

            criterion.m_operator = FilterOperator.LT;

            // Row numbers are orderable
            criterion.m_column = SpecialColumns.ROW_NUMBERS.toColumnSelection();
            criterion.m_predicateValues = DynamicValuesInput.forRowNumber(LongCell.TYPE);
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Row number via LT") //
                .doesNotThrowAnyException();

            // RowIDs cannot be ordered (only column values)
            criterion.m_column = SpecialColumns.ROWID.toColumnSelection();
            criterion.m_predicateValues = DynamicValuesInput.forRowID();
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("RowID cannot be compared via ordering") //
                .hasMessage("Unsupported operator \"Less than\" for RowID comparison");

            // Other DataCells can be ordered (for backwards-compatibility) but operators are hidden
            criterion.m_column = new ColumnSelection(SPEC.getColumnSpec("String1"));
            criterion.m_predicateValues =
                DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(StringCell.TYPE);
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("DataCell (non-BoundedValue) can be ordered, but operators may be hidden in dialog") //
                .doesNotThrowAnyException();

            // Normal column that implements BoundedValue
            criterion.m_column = new ColumnSelection(SPEC.getColumnSpec("Long1"));
            criterion.m_predicateValues =
                DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(LongCell.TYPE);
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Long (BoundedValue) can be ordered") //
                .doesNotThrowAnyException();
        }
    }

    @Nested
    static final class PatternMatching {

        @Test
        void testPatternMatchingRowID() {
            // RowID with default values
            final var criterion = new FilterCriterion();
            criterion.m_column = SpecialColumns.ROWID.toColumnSelection();
            criterion.m_predicateValues = DynamicValuesInput.forRowID();

            criterion.m_operator = FilterOperator.WILDCARD;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("RowID with default value via wildcard") //
                .doesNotThrowAnyException();

            criterion.m_operator = FilterOperator.REGEX;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("RowID with default value via regex") //
                .doesNotThrowAnyException();
        }

        @Test
        void testPatternMatchingRowNumber() {
            // RowNumber with default values
            final var criterion = new FilterCriterion();
            criterion.m_column = SpecialColumns.ROW_NUMBERS.toColumnSelection();
            criterion.m_predicateValues = DynamicValuesInput.forRowNumber(StringCell.TYPE);

            criterion.m_operator = FilterOperator.WILDCARD;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("RowNumber with default value via wildcard") //
                .doesNotThrowAnyException();

            criterion.m_operator = FilterOperator.REGEX;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("RowNumber with default value via regex") //
                .doesNotThrowAnyException();
        }

        @Test
        void testPatternMatchingLong() {
            // Long column with default values
            final var criterion = new FilterCriterion();

            criterion.m_column = new ColumnSelection(SPEC.getColumnSpec("Long1"));
            criterion.m_predicateValues =
                DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(StringCell.TYPE);

            criterion.m_operator = FilterOperator.WILDCARD;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Long with default value via wildcard") //
                .doesNotThrowAnyException();

            criterion.m_operator = FilterOperator.REGEX;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Long with default value via regex") //
                .doesNotThrowAnyException();
        }

        @Test
        void testPatternMatchingInt() {
            // Long column with default values
            final var criterion = new FilterCriterion();

            criterion.m_column = new ColumnSelection(SPEC.getColumnSpec("Int1"));
            criterion.m_predicateValues =
                DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(StringCell.TYPE);

            criterion.m_operator = FilterOperator.WILDCARD;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Int with default value via wildcard") //
                .doesNotThrowAnyException();

            criterion.m_operator = FilterOperator.REGEX;
            assertThatCode(() -> criterion.toPredicate(SPEC)) //
                .as("Int with default value via regex") //
                .doesNotThrowAnyException();
        }

        @Test
        void testPatternMatchingString() {
            // String column with default values
            final var criterion = new FilterCriterion();

            criterion.m_column = new ColumnSelection(SPEC.getColumnSpec("String1"));
            criterion.m_predicateValues =
                DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(StringCell.TYPE);

            criterion.m_operator = FilterOperator.WILDCARD;
            assertThatCode(() -> criterion.toPredicate(SPEC)).as("String with default value via wildcard") //
                .doesNotThrowAnyException();

            criterion.m_operator = FilterOperator.REGEX;
            assertThatCode(() -> criterion.toPredicate(SPEC)).as("String with default value via regex") //
                .doesNotThrowAnyException();
        }

        @Test
        void testPatternMatchingDoubleUnsupported() {
            // Double column with default values
            final var criterion = new FilterCriterion();

            criterion.m_column = new ColumnSelection(SPEC.getColumnSpec("Double1"));
            criterion.m_predicateValues =
                DynamicValuesInput.singleValueWithCaseMatchingForStringWithDefault(StringCell.TYPE);

            criterion.m_operator = FilterOperator.WILDCARD;
            assertThatCode(() -> criterion.toPredicate(SPEC)).as("Double with default value via wildcard") //
                .hasMessageContaining("Unsupported operator for input column type \"Number (double)\"");

            criterion.m_operator = FilterOperator.REGEX;
            assertThatCode(() -> criterion.toPredicate(SPEC)).as("Double with default value via regex") //
                .hasMessageContaining("Unsupported operator for input column type \"Number (double)\"");
        }
    }
}
