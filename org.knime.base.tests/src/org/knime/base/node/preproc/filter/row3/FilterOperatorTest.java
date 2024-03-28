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
 *   31 Mar 2024 (jasper): created
 */
package org.knime.base.node.preproc.filter.row3;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.filter.row3.FilterOperator.ColumnSelectionAndCompareModeDependency;
import org.knime.base.node.preproc.filter.row3.FilterOperator.CompareMode;
import org.knime.base.node.preproc.filter.row3.FilterOperator.TypeBasedOperatorChoices;
import org.knime.base.node.preproc.filter.row3.RowFilter3NodeSettings.IsBinaryOperator;
import org.knime.base.node.preproc.filter.row3.RowFilter3NodeSettings.IsNullaryOperator;
import org.knime.base.node.preproc.filter.row3.RowFilter3NodeSettings.IsNumberOfRowsOperator;
import org.knime.base.node.preproc.filter.row3.RowFilter3NodeSettings.IsPatternOperator;
import org.knime.base.node.preproc.filter.row3.RowFilter3NodeSettings.IsUnaryOperator;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;

/**
 * Tests for FilterOperator choices.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"restriction", "static-method"})
final class FilterOperatorTest {

    private static final DataTableSpec SPEC = new DataTableSpecCreator() //
        .addColumns( //
            new DataColumnSpecCreator("Int1", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Double1", DoubleCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Bool1", BooleanCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("String1", StringCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Int2", IntCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("Long1", LongCell.TYPE).createSpec()) //
        .createSpec();

    @Test
    void testConditions() {
        assertThat(new IsNullaryOperator().oneOf()).as("The list of nullary operators is what is expected")
            .containsExactlyInAnyOrder( //
                FilterOperator.IS_MISSING, //
                FilterOperator.IS_TRUE, //
                FilterOperator.IS_FALSE //
            );
        assertThat(new IsUnaryOperator().oneOf()).as("The list of unary operators is what is expected")
            .containsExactlyInAnyOrder( //
                FilterOperator.EQ, //
                FilterOperator.NEQ, //
                FilterOperator.GT, //
                FilterOperator.GTE, //
                FilterOperator.LT, //
                FilterOperator.LTE, //
                FilterOperator.LAST_N_ROWS, //
                FilterOperator.FIRST_N_ROWS, //
                FilterOperator.REGEX, //
                FilterOperator.WILDCARD //
            );
        assertThat(new IsBinaryOperator().oneOf()).as("The list of binary operators is what is expected")
            .containsExactlyInAnyOrder( //
                FilterOperator.BETWEEN //
            );
        assertThat(new IsPatternOperator().oneOf()).as("The list of binary operators is what is expected")
            .containsExactlyInAnyOrder( //
                FilterOperator.REGEX, //
                FilterOperator.WILDCARD //
            );
        assertThat(new IsNumberOfRowsOperator().oneOf()).as("The list of binary operators is what is expected")
            .containsExactlyInAnyOrder( //
                FilterOperator.LAST_N_ROWS, //
                FilterOperator.FIRST_N_ROWS //
            );
    }

    @Test
    void testOperatorChoices() {
        assertThat(operatorChoicesFor("String1", StringCell.TYPE, CompareMode.AS_STRING))
            .as("The list of operators for a string column is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.IS_MISSING, //
                FilterOperator.EQ, //
                FilterOperator.NEQ, //
                FilterOperator.REGEX, //
                FilterOperator.WILDCARD //
            );
        assertThat(operatorChoicesFor("Int1", IntCell.TYPE, CompareMode.INTEGRAL))
            .as("The list of operators for an integer column is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.IS_MISSING, //
                FilterOperator.EQ, //
                FilterOperator.NEQ, //
                FilterOperator.GT, //
                FilterOperator.GTE, //
                FilterOperator.LT, //
                FilterOperator.LTE, //
                FilterOperator.BETWEEN //
            );
        assertThat(operatorChoicesFor("Long1", LongCell.TYPE, CompareMode.INTEGRAL))
            .as("The list of operators for int cells is the same as for long cells")
            .isEqualTo(operatorChoicesFor("Int1", IntCell.TYPE, CompareMode.INTEGRAL));
        assertThat(operatorChoicesFor("Double1", DoubleCell.TYPE, CompareMode.DECIMAL))
            .as("The list of operators for int cells is the same as for double cells")
            .isEqualTo(operatorChoicesFor("Int1", IntCell.TYPE, CompareMode.INTEGRAL));
        assertThat(operatorChoicesFor("Bool1", BooleanCell.TYPE, CompareMode.BOOL))
            .as("The list of operators for a boolean column is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.IS_MISSING, //
                FilterOperator.IS_TRUE, //
                FilterOperator.IS_FALSE //
            );
        assertThat(operatorChoicesFor("Unknown Column", DataType.getType(DataType.getMissingCell().getClass()), null))
            .as("The list of operators for an unknown column type is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.IS_MISSING //
            );
    }

    @Test
    void testOperatorChoicesForSpecialColumns() {
        assertThat(operatorChoicesFor(SpecialColumns.ROW_NUMBERS, null))
            .as("The list of operators for the row numbers is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.EQ, //
                FilterOperator.NEQ, //
                FilterOperator.GT, //
                FilterOperator.GTE, //
                FilterOperator.LT, //
                FilterOperator.LTE, //
                FilterOperator.BETWEEN, //
                FilterOperator.LAST_N_ROWS, //
                FilterOperator.FIRST_N_ROWS //
            );
        assertThat(operatorChoicesFor(SpecialColumns.ROWID, CompareMode.AS_STRING))
            .as("The list of operators for the row id column is what is expected").containsExactlyInAnyOrder( //
                FilterOperator.EQ, //
                FilterOperator.NEQ, //
                FilterOperator.REGEX, //
                FilterOperator.WILDCARD //
            );
    }

    static FilterOperator[] operatorChoicesFor(final String col, final DataType type, final CompareMode mode) {
        return operatorChoicesFor(new ColumnSelection(col, type), mode);
    }

    static FilterOperator[] operatorChoicesFor(final SpecialColumns col, final CompareMode mode) {
        return operatorChoicesFor(col.toColumnSelection(), mode);
    }

    static FilterOperator[] operatorChoicesFor(final ColumnSelection columnSelection, final CompareMode mode) {
        var settings = new ColumnSelectionAndCompareModeDependency();
        settings.m_column = columnSelection;
        settings.m_compareOn = mode;
        final var ctx = DefaultNodeSettings.createDefaultNodeSettingsContext(new DataTableSpec[]{SPEC});
        return Arrays.stream(new TypeBasedOperatorChoices().update(settings, ctx))
            .map(idAndText -> FilterOperator.valueOf(idAndText.id())).toArray(FilterOperator[]::new);
    }
}
