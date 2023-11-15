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
 *   Nov 10, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.sorter;

import java.util.ArrayList;

import org.knime.base.node.preproc.sorter.dialog.DynamicSorterPanel;
import org.knime.base.node.util.SortKeyItem;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.func.ArgumentDefinition.PrimitiveArgumentType;
import org.knime.core.node.func.EnumArgumentType;
import org.knime.core.node.func.ListArgumentType;
import org.knime.core.node.func.NodeFunc;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.func.StructArgumentType;
import org.knime.core.node.port.PortObjectSpec;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
public final class SorterNodeFunc implements NodeFunc {

    private static final String SORT_CRITERIA = "sort_criteria";
    private static final String ORDER = "order";
    private static final String COLUMN = "column";

    private enum Order {
        ASC,
        DSC;
    }

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var sortCriteriaSettings = arguments.getNodeSettings(SORT_CRITERIA);
        var numCriteria = ListArgumentType.size(sortCriteriaSettings);
        var sortCriteria = new ArrayList<SortKeyItem>(numCriteria);
        for (int i = 0; i < numCriteria; i++) {
            var sortCriterionSettings = sortCriteriaSettings.getNodeSettings("" + i);
            var column = sortCriterionSettings.getString(COLUMN);
            var order = EnumArgumentType.getConstant(Order.class, sortCriterionSettings.getString(ORDER));
            var sortCriterion = new SortKeyItem(column, order == Order.ASC, false);
            sortCriteria.add(sortCriterion);
        }
        SortKeyItem.saveTo(sortCriteria, SorterNodeModel.INCLUDELIST_KEY, SorterNodeModel.SORTORDER_KEY,
            SorterNodeModel.ALPHANUMCOMP_KEY, settings);
        settings.addBoolean(SorterNodeModel.SORTINMEMORY_KEY, false);
        settings.addBoolean(SorterNodeModel.MISSING_TO_END_KEY, false);
    }

    @Override
    public NodeFuncApi getApi() {
        return NodeFuncApi.builder("sort")//
            .withInputTable("table", "The table to sort.")//
            .withOutputTable("sorted_table", "The sorted table.")//
            .withArgument(SORT_CRITERIA, """
                A list of sort criteria to sort by.
                Each criterion consists of the column to sort and the order ('ASC' or 'DSC').
                Example for sorting ascending by column foo and descending by column bar:
                [{"column": "foo", "order": "ASC"}, {"column": "bar", "order": "DSC"}]
                    """,
                ListArgumentType.create(StructArgumentType.builder()//
                    .withProperty(COLUMN, "The column to sort by or '%s' to sort by RowID."
                        .formatted(DynamicSorterPanel.ROWKEY.getName()),
                        PrimitiveArgumentType.STRING)
                    .withProperty(ORDER, "Controls whether to sort ascending or descending.",
                        EnumArgumentType.create(Order.class))
                    .build(), false))
            .build();
    }

    @Override
    public String getNodeFactoryClassName() {
        return SorterNodeFactory.class.getName();
    }

}
