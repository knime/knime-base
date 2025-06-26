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
 *   28 Oct 2024 (Alexander Jauch-Walser): created
 */
package org.knime.base.node.preproc.filter.row3;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.func.NodeFuncApi.Builder;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicValuesInput;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;

/**
 *
 * Filters rows based on the row index and a operator e.g. greater than
 *
 * @author Alexander Jauch-Walser, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class RowFilterByRowNumberNodeFunc extends AbstractRowFilterNodeFunc {

    private static final String OPERATOR = "operator";

    private static final String ROW_NUMBER = "row_number";

    private static final String EQUALS = "equals";

    private static final String NEQUALS = "notEquals";

    private static final String LESSTHAN = "lessThan";

    private static final String LESSTHANEQUALS = "lessThanEquals";

    private static final String GREATERTHAN = "greaterThan";

    private static final String GREATERTHANEQUALS = "greaterThanEquals";

    private static final String FIRSTN = "firstN";

    private static final String LASTN = "lastN";

    @Override
    FilterCriterion[] getFilterCriteria(final NodeSettingsRO arguments, final DataTableSpec tableSpec)
        throws InvalidSettingsException {

        var operatorName = arguments.getString(OPERATOR);
        var index = arguments.getLong(ROW_NUMBER);

        var criterion = new FilterCriterion();
        criterion.m_column = new StringOrEnum<>(RowIdentifiers.ROW_NUMBER);
        criterion.m_operator = getOperator(operatorName);
        var longCell = new LongCell.LongCellFactory().createCell(Long.toString(index));
        criterion.m_predicateValues = DynamicValuesInput.singleValueWithInitialValue(LongCell.TYPE, longCell);

        return new FilterCriterion[]{criterion};
    }

    private static FilterOperator getOperator(final String operatorName) {
        return switch (operatorName) {
            case EQUALS -> FilterOperator.EQ;
            case NEQUALS -> FilterOperator.NEQ;
            case LESSTHAN -> FilterOperator.LT;
            case LESSTHANEQUALS -> FilterOperator.LTE;
            case GREATERTHAN -> FilterOperator.GT;
            case GREATERTHANEQUALS -> FilterOperator.GTE;
            case FIRSTN -> FilterOperator.FIRST_N_ROWS;
            case LASTN -> FilterOperator.LAST_N_ROWS;
            default -> null;
        };
    }

    @Override
    void extendApi(final Builder builder) {
        builder
            .withDescription(
                "Creates a new table by filtering the range of rows by row number. The first row has row number 1.")//
            .withStringArgument(OPERATOR,
                String.format(
                    "The operator which will be used to filter the row numbers on:\n"
                        + "%s: Returns the row number which equals the specified number.\n"
                        + "%s: Returns the rows with row number not matching the specified number.\n"
                        + "%s: Returns the rows with row number which are strictly smaller than specified number\n"
                        + "%s: Returns the rows with row number which are smaller than or equal to specified number\n"
                        + "%s: Returns the rows with row number which are strictly larger than specified number\n"
                        + "%s: Returns the rows with row number which are larger than or equal than specified number\n"
                        + "%s: Returns the first n rows from the start of the table.\n"
                        + "%s: Returns the last n rows from the end of the table.",
                    EQUALS, NEQUALS, LESSTHAN, LESSTHANEQUALS, GREATERTHAN, GREATERTHANEQUALS, FIRSTN, LASTN))
            .withOptionalLongArgument(ROW_NUMBER,
                "The row number which filters rows of the table in combination with an operator.");

    }

    @Override
    String getName() {
        return "filter_row_by_row_number";
    }

}
