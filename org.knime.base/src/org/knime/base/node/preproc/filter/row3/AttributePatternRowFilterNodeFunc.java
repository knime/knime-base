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
 *   30 Oct 2024 (Alexander Jauch-Walser): created
 */
package org.knime.base.node.preproc.filter.row3;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.func.NodeFuncApi.Builder;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicValuesInput;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;

/**
 *
 * Filters rows based on a regex on a specified column
 *
 * @author Alexander Jauch-Walser, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class AttributePatternRowFilterNodeFunc extends AbstractRowFilterNodeFunc {

    private static final String REGEX = "regex";

    @Override
    FilterCriterion[] getFilterCriteria(final NodeSettingsRO arguments, final DataTableSpec tableSpec)
        throws InvalidSettingsException {
        var column = arguments.getString(COLUMN);
        var regex = arguments.getString(REGEX);

        var criterion = new FilterCriterion();
        criterion.m_column = new StringOrEnum<>(column);
        criterion.m_operator = FilterOperator.REGEX;

        var stringCell = new StringCell.StringCellFactory().createCell(regex);
        criterion.m_predicateValues = DynamicValuesInput.singleValueWithInitialValue(StringCell.TYPE, stringCell);

        return new FilterCriterion[]{criterion};
    }

    @Override
    void extendApi(final Builder builder) {
        builder
            .withDescription(
                "Creates a new table that contains only rows that match the given regex for a given column.")
            .withStringArgument(COLUMN, "The column on which to filter.")
            .withStringArgument(REGEX, "Regular expression that is used to match the RowIDs of the input table.");
    }

    @Override
    String getName() {
        return "filter_rows_by_regex";
    }

}
