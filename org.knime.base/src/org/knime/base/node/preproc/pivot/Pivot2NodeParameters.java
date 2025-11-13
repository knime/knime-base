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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.preproc.pivot;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.base.node.preproc.groupby.Sections;
import org.knime.base.node.preproc.groupby.common.ColumnAggregatorElement;
import org.knime.base.node.preproc.groupby.common.GroupByAdditionalParameters;
import org.knime.base.node.preproc.groupby.common.LegacyColumnAggregatorsMigration;
import org.knime.base.node.preproc.groupby.common.LegacyColumnAggregatorsPersistor;
import org.knime.base.node.preproc.pivot.Pivot2NodeModel.ColNameOption;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin.PersistEmbedded;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Before;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter.ColumnBasedExclListProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesStateProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.TypedStringChoice;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;

/**
 * Node parameters for Pivot.
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class Pivot2NodeParameters implements NodeParameters {

    /**
     * Additional section for the group columns which was not required in the GroupBy node as there we just had the
     * columns in no section on top of the dialog.
     *
     * @author Paul Bärnreuther
     */
    @Section(title = "Groups")
    interface GroupsSection {
    }

    @Section(title = "Pivots")
    @After(GroupsSection.class)
    @Before(Sections.Aggregation.class)
    interface PivotsSection {
    }

    @Persist(configKey = "grouByColumns")
    @Modification(GroupByColumnsModification.class)
    @ValueReference(GroupByColumnsRef.class)
    @Layout(GroupsSection.class)
    LegacyStringFilter m_groupByColumns = new LegacyStringFilter(new String[0], new String[0]);

    interface GroupByColumnsRef extends ParameterReference<LegacyStringFilter> {
    }

    static final class ExclListProvider extends ColumnBasedExclListProvider {

        @Override
        public Class<? extends ChoicesStateProvider<TypedStringChoice>> getChoicesProviderClass() {
            return AllColumnsProvider.class;
        }

    }

    static final class GroupByColumnsModification extends LegacyStringFilter.LegacyStringFilterModification {
        GroupByColumnsModification() {
            super(false, "Group columns", """
                    Select one or more columns according to which the group rows are created.
                    """, "Group columns", "Available columns", AllColumnsProvider.class, ExclListProvider.class);
        }
    }

    @Persist(configKey = Pivot2NodeModel.CFG_PIVOT_COLUMNS)
    @Modification(PivotColumnsModification.class)
    @ValueReference(PivotColumnsRef.class)
    @Layout(PivotsSection.class)
    LegacyStringFilter m_pivotColumns = new LegacyStringFilter(new String[0], new String[0]);

    interface PivotColumnsRef extends ParameterReference<LegacyStringFilter> {
    }

    static final class PivotColumnsModification extends LegacyStringFilter.LegacyStringFilterModification {
        PivotColumnsModification() {
            super(false, "Pivot columns", """
                    Select one or more columns according to which the pivot columns are created.
                    """, "Pivot columns", "Available columns", AllColumnsProvider.class, ExclListProvider.class);
        }
    }

    @Persist(configKey = Pivot2NodeModel.CFG_MISSING_VALUES)
    @Widget(title = "Ignore missing values", description = "Ignore rows containing missing values in pivot column.")
    @Layout(PivotsSection.class)
    boolean m_ignoreMissingValues = true;

    @Persist(configKey = Pivot2NodeModel.CFG_TOTAL_AGGREGATION)
    @Widget(title = "Append overall totals",
        description = "Appends the overall pivot totals with each aggregation"
            + " performed together on all selected pivot columns.")
    @Layout(PivotsSection.class)
    boolean m_appendOverallTotals;

    @Persist(configKey = Pivot2NodeModel.CFG_IGNORE_DOMAIN)
    @Widget(title = "Ignore domain",
        description = "Ignore domain and use only the possible values available in the input data.")
    @Layout(PivotsSection.class)
    boolean m_ignoreDomain = true;

    static final class NonGroupAndNonPivotColumnsProvider implements ColumnChoicesProvider {

        private Supplier<LegacyStringFilter> m_groupByColumnsSupplier;

        private Supplier<LegacyStringFilter> m_pivotColumnsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            ColumnChoicesProvider.super.init(initializer);
            m_groupByColumnsSupplier = initializer.computeFromValueSupplier(GroupByColumnsRef.class);
            m_pivotColumnsSupplier = initializer.computeFromValueSupplier(PivotColumnsRef.class);
        }

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            final var tableSpec = context.getInTableSpec(0);
            if (tableSpec.isEmpty()) {
                return List.of();
            }
            final var exclSet =
                Arrays.stream(m_groupByColumnsSupplier.get().m_twinList.m_inclList).collect(Collectors.toSet());
            exclSet.addAll(Arrays.asList(m_pivotColumnsSupplier.get().m_twinList.m_inclList));
            return tableSpec.get().stream().filter(colSpec -> !exclSet.contains(colSpec.getName())).toList();
        }
    }

    @Layout(Sections.Aggregation.class)
    @Widget(title = "Manual Aggregations", description = """
            Select one or more column(s) for aggregation from the available
            columns list. Change the aggregation method in the Aggregation
            column of the table. You can add the same column multiple
            times.
            """)
    @Persistor(LegacyColumnAggregatorsPersistor.class) // No array persistor...
    @Migration(LegacyColumnAggregatorsMigration.class) // ...because then we could not deprecate keys here
    @Modification(ColumnAggregatorElementModifier.class)
    @ArrayWidget(addButtonText = "Add aggregation",
        // TODO disable "add" button based on input (e.g. no table connected)
        elementDefaultValueProvider = DefaultColumnAggregatorElementProvider.class)
    ColumnAggregatorElement[] m_columnAggregators = new ColumnAggregatorElement[0];

    static final class ColumnAggregatorElementModifier extends ColumnAggregatorElement.ColumnAggregatorElementModifier {

        ColumnAggregatorElementModifier() {
            super(NonGroupAndNonPivotColumnsProvider.class);
        }

    }

    static final class DefaultColumnAggregatorElementProvider
        extends ColumnAggregatorElement.DefaultColumnAggregatorElementProvider {

        DefaultColumnAggregatorElementProvider() {
            super(NonGroupAndNonPivotColumnsProvider.class);
        }

    }

    @Widget(title = "Column name", description = """
            The name of the resulting pivot column(s) depends on the selected naming schema.
            """)
    @Layout(Sections.Output.class)
    @Persistor(ColNameOptionPersistor.class)
    ColNameOption m_colNameOption = ColNameOption.PIV_FIRST_AGG_LAST;

    static final class ColNameOptionPersistor implements NodeParametersPersistor<ColNameOption> {

        private static final String CFG_KEY = Pivot2NodeModel.CFG_COL_NAME_OPTION;

        @Override
        public ColNameOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return ColNameOption.getEnum(settings.getString(CFG_KEY, ColNameOption.PIV_FIRST_AGG_LAST.toString()));
        }

        @Override
        public void save(final ColNameOption param, final NodeSettingsWO settings) {
            settings.addString(CFG_KEY, param.toString());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_KEY}};
        }
    }

    @Modification(ChangeColumnNamingStragegyTitleModification.class)
    @PersistEmbedded
    GroupByAdditionalParameters m_groupByAdditionalParameters = new GroupByAdditionalParameters();

    static final class ChangeColumnNamingStragegyTitleModification
        extends GroupByAdditionalParameters.ChangeColumnNamePolicyTitleModification {

        @Override
        protected String getColumnNamePolicyTitle() {
            return "Aggregation name";
        }
    }

    @Widget(title = "Sort lexicographically",
        description = "Lexicographically sorts all columns belonging to the same logical group, "
            + "i.e., pivots (aggregations), groups, and overall totals.")
    @Persist(configKey = Pivot2NodeModel.CFG_LEXICOGRAPHICAL_SORT)
    @Layout(Sections.Output.class)
    boolean m_sortLexicographically;

}
