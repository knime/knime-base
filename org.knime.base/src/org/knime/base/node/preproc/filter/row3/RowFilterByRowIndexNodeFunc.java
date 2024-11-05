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
import org.knime.core.data.def.LongCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.func.NodeFuncApi.Builder;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;

/**
 *
 * Filters rows based on the row index and a operator e.g. greater than
 *
 * @author Alexander Jauch-Walser, KNIME GmbH, Konstanz, Germany
 */

public class RowFilterByRowIndexNodeFunc extends AbstractRowFilterNodeFunc{

    private static final String OPERATOR = "operator";
    private static final String INDEX = "index";

    private static final String EQUALS = "equals";
    private static final String NEQUALS = "notEquals";
    private static final String LESSTHAN = "lessThan";
    private static final String LESSTHANEQUALS = "lessThanEquals";
    private static final String GREATERTHAN = "greaterThan";
    private static final String GREATERTHANEQUALS = "greaterThanEquals";
    private static final String FIRSTN = "firstN";
    private static final String LASTN = "lastN";


    @Override
    FilterCriterion getSpecificCriterion(final NodeSettingsRO arguments)
        throws InvalidSettingsException {

        var operatorName = arguments.getString(OPERATOR);
        var index = arguments.getLong(INDEX);

        var criterion = new FilterCriterion();
        criterion.m_column.m_selected = SpecialColumns.ROW_NUMBERS.getId();
        criterion.m_operator = getOperator(operatorName);
        var longCell = new LongCell.LongCellFactory().createCell(Long.toString(index));
        criterion.m_predicateValues = DynamicValuesInput.singleValueWithInitialValue(LongCell.TYPE, longCell);

        return criterion;
    }

    private FilterOperator getOperator(final String operatorName) {
        switch(operatorName) {
            case EQUALS: return FilterOperator.EQ;
            case NEQUALS: return FilterOperator.NEQ;
            case LESSTHAN: return FilterOperator.LT;
            case LESSTHANEQUALS: return FilterOperator.LTE;
            case GREATERTHAN: return FilterOperator.GT;
            case GREATERTHANEQUALS: return FilterOperator.GTE;
            case FIRSTN: return FilterOperator.FIRST_N_ROWS;
            case LASTN: return FilterOperator.LAST_N_ROWS;
            default: return null;
        }
    }

    @Override
    void extendApi(final Builder builder) {
        builder.withDescription("Matches rows whose value of the specified column are missing.")//
        .withDescription("Creates a new table by filtering the range of rows by index."
                        +"The first row has index 0.")//
        .withStringArgument(OPERATOR, String.format("The operator which will be used to filter the row Indexes on:\n"
            + "%s: Returns the row with index which equals the specified number.\n"
            + "%s: Returns the rows with indexes not matching the specified number.\n"
            + "%s: Returns the rows with indexes which are strictly smaller than specified number\n"
            + "%s: Returns the rows with indexes which are smaller than or equal to specified number\n"
            + "%s: Returns the rows with indexes which are strictly larger than specified number\n"
            + "%s: Returns the rows with indexes which are larger than or equal than specified number\n"
            + "%s: Returns the first n rows from the start of the table.\n"
            + "%s: Returns the last n rows from the end of the table.",
            EQUALS, NEQUALS, LESSTHAN, LESSTHANEQUALS, GREATERTHAN, GREATERTHANEQUALS, FIRSTN, LASTN))
        .withOptionalLongArgument(INDEX, "The index which filters rows of the table in combination with an operator.");

    }

    @Override
    String getName() {
        return "filter_row_by_index";
    }

}
