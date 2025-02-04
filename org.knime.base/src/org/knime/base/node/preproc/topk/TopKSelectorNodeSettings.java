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
 *   Feb 3, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.topk;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.knime.base.util.preproc.SortingUtils.SortingOrder;
import org.knime.base.util.preproc.SortingUtils.StringComparison;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.AllColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;

/**
 * The settings for the "Top k Row Filter" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class TopKSelectorNodeSettings implements DefaultNodeSettings {

    enum FilterMode {
            @Label(value = "Rows", description = """
                    Returns the top k rows based on their first occurrence in the input table.
                    """)
            ROWS(TopKMode.TOP_K_ROWS), //
            @Label(value = "Unique values", description = """
                    Returns all rows associated with the top k unique values.
                    """)
            UNIQUE_VALUES(TopKMode.TOP_K_ALL_ROWS_W_UNIQUE);

        TopKMode m_topkMode;

        FilterMode(final TopKMode topkMode) {
            this.m_topkMode = topkMode;
        }

        static FilterMode getFromTopkMode(final TopKMode topKMode) {
            return switch (topKMode) {
                case TOP_K_ROWS -> ROWS;
                case TOP_K_ALL_ROWS_W_UNIQUE -> UNIQUE_VALUES;
            };
        }
    }

    enum RowOrder {
            @Label(value = "Arbitrary", description = """
                    The rows are directly output in the order they are returned in by the algorithm. This option \
                    doesn't incur any additional runtime costs. Note: It is possible but not guaranteed that the \
                    output order is the same as the input order
                    """)
            ARBITRARY(OutputOrder.NO_ORDER), //
            @Label(value = "Input order", description = """
                    The input order is reestablished for the rows returned by the algorithm. This requires \
                    sorting the output table.
                    """)
            INPUT_ORDER(OutputOrder.RETAIN), //
            @Label(value = "Sorted", description = """
                    The rows in the output are sorted according to the specified selection criteria.
                    """)
            SORTED(OutputOrder.SORT); //

        OutputOrder m_outputOrder;

        RowOrder(final OutputOrder outputOrder) {
            this.m_outputOrder = outputOrder;
        }

        static RowOrder getFromOutputOrder(final OutputOrder outputOrder) {
            return switch (outputOrder) {
                case NO_ORDER -> ARBITRARY;
                case RETAIN -> INPUT_ORDER;
                case SORT -> SORTED;
            };
        }
    }

    @Section(title = "Sorting")
    interface SortingSection {
    }

    @Section(title = "Filter")
    @After(SortingSection.class)
    interface FilterSection {
    }

    @Section(title = "Special Values and Performance", advanced = true)
    @After(FilterSection.class)
    interface AdvancedSection {
    }

    @Layout(SortingSection.class)
    @Widget(title = "Sorting", description = "A list of sorting critera.")
    @Migration(LegacySortingSettingsMigration.class)
    @ArrayWidget(elementTitle = "Criterion", addButtonText = "Add sorting criterion", showSortButtons = true)
    SortingCriterionSettings[] m_sortingCriteria = new SortingCriterionSettings[]{new SortingCriterionSettings()};

    @Layout(FilterSection.class)
    @Widget(title = "Filter mode", description = "Specifies the mode for the top k selection of the output")
    @ValueSwitchWidget
    @Migration(FilterModeSettingsMigration.class)
    FilterMode m_filterMode = FilterMode.ROWS;

    @Widget(title = "Number of rows / unique values", description = """
            The number of rows to select from the input table based on the specified mode.
            """)
    @NumberInputWidget(min = 1)
    @Persist(configKey = "k")
    @Layout(FilterSection.class)
    int m_amount = 5;

    @Widget(title = "Sort missing values to end of table", description = """
            If selected, missing values are always considered to be inferior to present cells.
            """, advanced = true)
    @Layout(AdvancedSection.class)
    @Persist(configKey = "missingsToEnd")
    boolean m_missingToEnd;

    @Widget(title = "Row order", description = """
            Depending on the settings of the algorithm the order might change in the output and this option \
            allows you to specify constraints on the order.
            """, advanced = true)
    @Layout(AdvancedSection.class)
    @ValueSwitchWidget
    @Migration(RowOrderSettingsMigration.class)
    RowOrder m_rowOrder = RowOrder.ARBITRARY;

    static final class SortingCriterionSettings implements DefaultNodeSettings {

        /**
         * @param column
         * @param sortingOrder
         * @param stringComparison
         */
        public SortingCriterionSettings(final String column, final SortingOrder sortingOrder,
            final StringComparison stringComparison) {
            m_column = column;
            m_sortingOrder = sortingOrder;
            m_stringComparison = stringComparison;
        }

        /**
         *
         */
        public SortingCriterionSettings() {
            this((DataColumnSpec)null);
        }

        /**
         * @param context
         */
        public SortingCriterionSettings(final DefaultNodeSettingsContext context) {
            this(context.getDataTableSpec(0).flatMap(Optional::ofNullable)
                .map(spec -> spec.getNumColumns() == 0 ? null : spec.getColumnSpec(0)).flatMap(Optional::ofNullable)
                .orElse(null));
        }

        SortingCriterionSettings(final DataColumnSpec colSpec) {
            m_column = colSpec == null ? SpecialColumns.ROWID.getId() : colSpec.getName();
        }

        @Widget(title = "Column",
            description = "Sort rows by the values in this column. "
                + "If you set multiple sorting criteria, the table is sorted by the first criterion. "
                + "The following criteria are only considered, if the comparison by all previous "
                + "criteria results in a tie.")
        @ChoicesWidget(choices = AllColumnChoicesProvider.class, showRowKeysColumn = true)
        String m_column;

        @Widget(title = "Order", description = "Specifies the sorting order:")
        @ValueSwitchWidget
        SortingOrder m_sortingOrder = SortingOrder.ASCENDING;

        @Widget(title = "String comparison", description = "Specifies which type of sorting to apply to the strings:",
            advanced = true)
        @ValueSwitchWidget
        StringComparison m_stringComparison = StringComparison.NATURAL;

        /**
         * @return the column
         */
        public String getColumn() {
            return m_column;
        }

        /**
         * @return the sortingOrder
         */
        public SortingOrder getSortingOrder() {
            return m_sortingOrder;
        }

        /**
         * @return the stringComparison
         */
        public StringComparison getStringComparison() {
            return m_stringComparison;
        }

    }

    static final class LegacySortingSettingsMigration implements NodeSettingsMigration<SortingCriterionSettings[]> {

        private static SortingCriterionSettings[] load(final NodeSettingsRO settings) {
            final var columns = settings.getStringArray(TopKSelectorNodeModel.INCLUDELIST_KEY, new String[0]);
            final var sortKeys =
                settings.getBooleanArray(TopKSelectorNodeModel.SORTORDER_KEY, new boolean[columns.length]);
            final var compKeys =
                settings.getBooleanArray(TopKSelectorNodeModel.ALPHANUMCOMP_KEY, new boolean[columns.length]);

            return IntStream.range(0, columns.length)
                .mapToObj(i -> new SortingCriterionSettings(columns[i],
                    sortKeys[i] ? SortingOrder.ASCENDING : SortingOrder.DESCENDING,
                    compKeys[i] ? StringComparison.NATURAL : StringComparison.LEXICOGRAPHIC))
                .toArray(SortingCriterionSettings[]::new);
        }

        @Override
        public List<ConfigMigration<SortingCriterionSettings[]>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(LegacySortingSettingsMigration::load) //
                    // we cannot use the default matcher here, since LEGACY_ALPHANUMCOMP_KEY was added with 4.7
                    .withMatcher(settings -> settings.containsKey(TopKSelectorNodeModel.INCLUDELIST_KEY)) //)
                    .withDeprecatedConfigPath(TopKSelectorNodeModel.INCLUDELIST_KEY)//
                    .withDeprecatedConfigPath(TopKSelectorNodeModel.SORTORDER_KEY) //
                    .withDeprecatedConfigPath(TopKSelectorNodeModel.ALPHANUMCOMP_KEY) //
                    .build());
        }

    }

    static final class FilterModeSettingsMigration implements NodeSettingsMigration<FilterMode> {

        private static final String KEY = "selectionMode";

        private static FilterMode load(final NodeSettingsRO settings) {
            return FilterMode
                .getFromTopkMode(TopKMode.getTopKModeByText(settings.getString(KEY, TopKMode.TOP_K_ROWS.getText())));
        }

        @Override
        public List<ConfigMigration<FilterMode>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(FilterModeSettingsMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }

    }

    static final class RowOrderSettingsMigration implements NodeSettingsMigration<RowOrder> {

        private static final String KEY = "outputOrder";

        private static RowOrder load(final NodeSettingsRO settings) {
            return RowOrder.getFromOutputOrder(OutputOrder.valueOf(settings.getString(KEY, OutputOrder.SORT.name())));
        }

        @Override
        public List<ConfigMigration<RowOrder>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(RowOrderSettingsMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }

    }

}
