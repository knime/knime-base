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
 *   19 Dec 2022 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.rowagg;

import org.knime.base.node.preproc.rowagg.RowAggregatorNodeModel.AggregationFunction;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.webui.node.dialog.impl.ChoicesProvider;
import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.impl.Schema;

/**
 * Settings for the Row Aggregator node model.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class RowAggregatorSettings implements DefaultNodeSettings {

    @Schema(title = "Aggregation",
            description = "Select the aggregation function to be applied on all rows belonging to the same category."
                + "<ul>"
                // COUNT
                + "<li><i>Occurrence count:</i>"
                + " Count how many rows occur</li>"
                // SUM
                + "<li><i>Sum:</i>"
                + " Sum up values, optionally weighted by the value from the weight column</li>"
                // AVERAGE
                + "<li><i>Average:</i>"
                + " Calculate the mean value, optionally weighted by the value from the weight column</li>"
                // MIN
                + "<li><i>Minimum:</i>"
                + " Calculate the minimum value</li>"
                // MAX
                + "<li><i>Maximum:</i>"
                + " Calculate the maximum value</li>"
                + "</ul>")
    AggregationFunction m_aggregationMethod = AggregationFunction.SUM;

    @Schema(title = "Frequency columns", description = "Select the columns to apply the aggregation function to.",
            choices = FrequencyColumns.class, multiple = true)
    String[] m_frequencyColumns;

    static final class FrequencyColumns implements ChoicesProvider {

        @Override
        public String[] choices(final SettingsCreationContext context) {
            final var spec = context.getDataTableSpecs()[0];
            if (spec == null) {
                return new String[0];
            }
            return spec.stream()
                // In theory this should check the type of either (as configured):
                // - the aggregated column type if weight column is none or
                // - the result of the multiplication with the weight column if a weight column is selected.
                // However, at this point we don't know the selected weight column here (and vice-versa), so we do the
                // next-best thing. Note: This could filter too much, i.e. could remove columns that would be compatible
                // _after_ applying the weight to them, which is currently not the case, since Multiply supports the
                // same (input) types as Sum and Average.
                // If we add numeric types and change the Multiply/Sum/Average, we should revisit this filter here.
                .filter(c -> RowAggregatorNodeModel.isSupportedAsAggregatedColumn(c.getType()))
                .map(DataColumnSpec::getName)
                .toArray(String[]::new);
        }

    }

    @Schema(title = "Weight column", description = "Select the column that defines the weight with which a frequency "
        + "value is multiplied before aggregation. Note, that only some aggregation functions support weighted "
        + "aggregates.",
            choices = WeightColumns .class)
    String m_weightColumn;


    static final class WeightColumns implements ChoicesProvider {

        @Override
        public String[] choices(final SettingsCreationContext context) {
            final var spec = context.getDataTableSpecs()[0];
            if (spec == null) {
                return new String[0];
            }
            return spec.stream()
                .filter(wc -> RowAggregatorNodeModel.isSupportedAsWeightColumn(wc.getType()))
                .map(DataColumnSpec::getName)
                .toArray(String[]::new);
        }

    }

    @Schema(title = "Category column", description = "Select the column that defines the category on which rows "
        + "are grouped. If no category column is selected, \"grand total\" values in which all rows belong to the same "
        + "group will be calculated.",
            choices = CategoryColumns.class)
    String m_categoryColumn;

    static final class CategoryColumns implements ChoicesProvider {
        @Override
        public String[] choices(final SettingsCreationContext context) {
            final var spec = context.getDataTableSpecs()[0];
            return spec != null ? spec.getColumnNames() : new String[0];
        }
    }

    @Schema(title = "Additional \"grand totals\" at second output port",
            description = "If a category column is selected, additionally compute the aggregations <i>without</i> the "
                + "category column (\"grand totals\") and output them in the second output table. "
                + "The second output is inactive if no category "
                + "column is selected or this setting is not enabled.")
    boolean m_grandTotals;

    /**
     * Constructor for deserialization.
     */
    RowAggregatorSettings() {
        // required by interface
    }

    /**
     * Constructor for auto-configuration.
     * @param ctx context for creation
     */
    RowAggregatorSettings(final SettingsCreationContext ctx) {
        final var spec = ctx.getDataTableSpecs()[0];
        if (spec != null) {
            final var numCols = spec.getNumColumns();
            if (numCols > 0) {
                // last column as category automatically? some classification nodes put the class column
                // last, which would be good to autoconfigure with here
                final var last = spec.getColumnSpec(numCols - 1);
                m_categoryColumn = last.getName();
            }
        }
    }
}
