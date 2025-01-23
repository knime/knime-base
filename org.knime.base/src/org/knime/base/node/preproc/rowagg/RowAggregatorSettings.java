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

import java.util.stream.Stream;

import org.knime.base.node.preproc.rowagg.RowAggregatorNodeModel.AggregationFunction;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * Settings for the Row Aggregator node model.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class RowAggregatorSettings implements DefaultNodeSettings {

    interface CategoryColumnRef extends Reference<String> {
    }

    static final class IsNoneCategoryColumnSelected implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getString(CategoryColumnRef.class).isNoneString();
        }
    }

    @Widget(title = "Category column", description = "Select the column that defines the category on which rows "
        + "are grouped. If no category column is selected, \"grand total\" values in which all rows belong to the same "
        + "group will be calculated.")
    @ChoicesWidget(choices = CategoryColumns.class, showNoneColumn = true)
    @ValueReference(CategoryColumnRef.class)
    String m_categoryColumn = SpecialColumns.NONE.getId();

    static final class CategoryColumns implements ChoicesProvider {
        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).map(DataTableSpec::getColumnNames).orElse(new String[0]);
        }
    }

    interface AggregationFunctionRef extends Reference<AggregationFunction> {
    }

    static final class AggregationFunctionIsCount implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(AggregationFunctionRef.class).isOneOf(AggregationFunction.COUNT);
        }
    }

    static final class AggregationFunctionIsCountOrMinOrMax implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(AggregationFunctionRef.class).isOneOf(AggregationFunction.COUNT, AggregationFunction.MIN,
                AggregationFunction.MAX);
        }
    }

    @Widget(title = "Aggregation",
        description = "Select the aggregation function to be applied on all rows belonging to the same category."
            + "<ul>"
            // COUNT
            + "<li><i>Occurrence count:</i>" + " Count how many rows occur</li>"
            // SUM
            + "<li><i>Sum:</i>" + " Sum up values, optionally weighted by the value from the weight column</li>"
            // AVERAGE
            + "<li><i>Average:</i>"
            + " Calculate the mean value, optionally weighted by the value from the weight column</li>"
            // MIN
            + "<li><i>Minimum:</i>" + " Calculate the minimum value</li>"
            // MAX
            + "<li><i>Maximum:</i>" + " Calculate the maximum value</li>" + "</ul>")
    @RadioButtonsWidget(horizontal = true)
    @ValueReference(AggregationFunctionRef.class)
    AggregationFunction m_aggregationMethod = AggregationFunction.SUM;

    @Widget(title = "Aggregation columns", description = "Select the columns to apply the aggregation function to.")
    @ChoicesWidget(choices = AggregatableColumns.class)
    @Effect(predicate = AggregationFunctionIsCount.class, type = EffectType.DISABLE)
    ColumnFilter m_frequencyColumns;

    static final class AggregatableColumns implements ColumnChoicesProvider {
        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .filter(RowAggregatorNodeModel::isAggregatableColumn)//
                .toArray(DataColumnSpec[]::new);
        }
    }

    @Widget(title = "Weight column", description = "Select the column that defines the weight with which a "
        + "value is multiplied before aggregation. Note, that only the aggregation functions \"Sum\" and \"Average\" "
        + "support a weight column")
    @ChoicesWidget(choices = WeightColumns.class, showNoneColumn = true)
    @Effect(predicate = AggregationFunctionIsCountOrMinOrMax.class, type = EffectType.DISABLE)
    String m_weightColumn;

    static final class WeightColumns implements ChoicesProvider {
        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .filter(RowAggregatorNodeModel::isWeightColumn)//
                .map(DataColumnSpec::getName).toArray(String[]::new);
        }
    }

    @Widget(title = "Additional \"grand totals\" at second output port",
        description = "If a category column is selected, additionally compute the aggregations <i>without</i> the "
            + "category column (\"grand totals\") and output them in the second output table. "
            + "The second output is inactive if no category " + "column is selected or this setting is not enabled.")
    @Effect(predicate = IsNoneCategoryColumnSelected.class, type = EffectType.DISABLE)
    boolean m_grandTotals;

    @Widget(title = "Enable Hiliting", advanced = true,
        description = "Enable hiliting between the input port and the aggregated output table.")
    @Migrate(loadDefaultIfAbsent = true)
    boolean m_enableHiliting;

    /**
     * Constructor for de/serialization.
     */
    RowAggregatorSettings() {
        // required by interface
    }

    RowAggregatorSettings(final DefaultNodeSettingsContext ctx) {
        m_frequencyColumns = ColumnFilter.createDefault(AggregatableColumns.class, ctx);
    }
}
