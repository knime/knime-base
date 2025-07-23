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

import static org.knime.base.node.preproc.rowagg.RowAggregatorNodeModel.isAggregatableColumn;
import static org.knime.base.node.preproc.rowagg.RowAggregatorNodeModel.isWeightColumn;
import static org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.NoneChoice.NONE;

import java.util.Optional;

import org.knime.base.node.preproc.rowagg.RowAggregatorNodeModel.AggregationFunction;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.StringToStringWithNoneChoiceMigration;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.NoneChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.FilteredInputTableColumnsProvider;

/**
 * Settings for the Row Aggregator node model.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class RowAggregatorSettings implements NodeParameters {

    interface CategoryColumnRef extends ParameterReference<StringOrEnum<NoneChoice>> {
    }

    static final class IsNoneCategoryColumnSelected implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getStringOrEnum(CategoryColumnRef.class).isEnumChoice(NONE);
        }
    }

    @Widget(title = "Category column", description = "Select the column that defines the category on which rows "
        + "are grouped. If no category column is selected, \"grand total\" values in which all rows belong to the same "
        + "group will be calculated.")
    @ChoicesProvider(AllColumnsProvider.class)
    @ValueReference(CategoryColumnRef.class)
    @Migration(CategoryColumnMigration.class)
    @Persist(configKey = "categoryColumnV2")
    StringOrEnum<NoneChoice> m_categoryColumn = new StringOrEnum<>(NONE);

    static final class CategoryColumnMigration extends StringToStringWithNoneChoiceMigration {
        protected CategoryColumnMigration() {
            super("categoryColumn");
        }

        @Override
        public Optional<NoneChoice> loadEnumFromLegacyString(final String legacyCategoryColumnString) {
            if (legacyCategoryColumnString == null || legacyCategoryColumnString.isEmpty()) {
                return Optional.of(NONE);
            }
            return super.loadEnumFromLegacyString(legacyCategoryColumnString);
        }

    }

    interface AggregationFunctionRef extends ParameterReference<AggregationFunction> {
    }

    static final class AggregationFunctionIsCount implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(AggregationFunctionRef.class).isOneOf(AggregationFunction.COUNT);
        }
    }

    static final class AggregationFunctionIsCountOrMinOrMax implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(AggregationFunctionRef.class).isOneOf(AggregationFunction.COUNT, AggregationFunction.MIN,
                AggregationFunction.MAX);
        }
    }

    @Widget(title = "Aggregation",
        description = "Select the aggregation function to be applied on all rows belonging to the same category.")
    @RadioButtonsWidget(horizontal = true)
    @ValueReference(AggregationFunctionRef.class)
    AggregationFunction m_aggregationMethod = AggregationFunction.SUM;

    @Widget(title = "Aggregation columns", description = "Select the columns to apply the aggregation function to.")
    @ChoicesProvider(AggregatableColumns.class)
    @Effect(predicate = AggregationFunctionIsCount.class, type = EffectType.DISABLE)
    ColumnFilter m_frequencyColumns;

    static final class AggregatableColumns implements FilteredInputTableColumnsProvider {

        @Override
        public boolean isIncluded(final DataColumnSpec col) {
            return isAggregatableColumn(col);
        }
    }

    @Widget(title = "Weight column", description = "Select the column that defines the weight with which a "
        + "value is multiplied before aggregation. Note, that only the aggregation functions \"Sum\" and \"Average\" "
        + "support a weight column")
    @ChoicesProvider(WeightColumns.class)
    @Effect(predicate = AggregationFunctionIsCountOrMinOrMax.class, type = EffectType.DISABLE)
    @Persist(configKey = "weightColumnV2")
    @Migration(WeightColumnMigration.class)
    StringOrEnum<NoneChoice> m_weightColumn = new StringOrEnum<>(NONE);

    static final class WeightColumnMigration extends StringToStringWithNoneChoiceMigration {
        protected WeightColumnMigration() {
            super("weightColumn");
        }

        @Override
        public Optional<NoneChoice> loadEnumFromLegacyString(final String legacyWeightColumnString) {
            if (legacyWeightColumnString == null) {
                return Optional.of(NONE);
            }
            return super.loadEnumFromLegacyString(legacyWeightColumnString);
        }

    }

    static final class WeightColumns implements FilteredInputTableColumnsProvider {

        @Override
        public boolean isIncluded(final DataColumnSpec col) {
            return isWeightColumn(col);
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

    RowAggregatorSettings() {
        m_frequencyColumns = new ColumnFilter().withIncludeUnknownColumns();
    }

    RowAggregatorSettings(final NodeParametersInput ctx) {
        m_frequencyColumns = new ColumnFilter(
            ColumnSelectionUtil.getFilteredColumns(ctx, 0, RowAggregatorNodeModel::isAggregatableColumn))
                .withIncludeUnknownColumns();
    }

}
