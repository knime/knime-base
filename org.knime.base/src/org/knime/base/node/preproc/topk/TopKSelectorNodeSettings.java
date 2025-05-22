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

import static org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice.ROW_ID;

import java.util.List;
import java.util.stream.IntStream;

import org.knime.base.node.util.preproc.SortingUtils;
import org.knime.base.node.util.preproc.SortingUtils.SortingCriterionSettings;
import org.knime.base.node.util.preproc.SortingUtils.SortingOrder;
import org.knime.base.node.util.preproc.SortingUtils.StringComparison;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * The settings for the "Top k Row Filter" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class TopKSelectorNodeSettings implements DefaultNodeSettings {
    TopKSelectorNodeSettings() {

    }

    TopKSelectorNodeSettings(final DefaultNodeSettingsContext context) {
        this.m_sortingCriteria = new SortingCriterionSettings[]{new SortingCriterionSettings(context)};
    }

    enum FilterMode {
            @Label(value = "Rows",
                description = "Returns the top k rows based on their first occurrence in the input table.")
            ROWS(TopKMode.TOP_K_ROWS), //
            @Label(value = "Unique values", description = "Returns all rows associated with the top k unique values.")
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
            @Label(value = "Sorted",
                description = "The rows in the output are sorted according to the specified selection criteria.")
            SORTED(OutputOrder.SORT), //
            @Label(value = "Input order", description = """
                    The input order is reestablished for the rows returned by the algorithm. This requires \
                    sorting the output table.
                    """)
            INPUT_ORDER(OutputOrder.RETAIN), //
            @Label(value = "Arbitrary", description = """
                    The rows are directly output in the order they are returned in by the algorithm. This option \
                    doesn't incur any additional runtime costs. Note: It is possible but not guaranteed that the \
                    output order is the same as the input order
                    """)
            ARBITRARY(OutputOrder.NO_ORDER); //

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

    @Section(title = "Output Sorting")
    @After(FilterSection.class)
    interface OutputSortingSection {
    }

    interface SortingCriteriaRef extends Reference<SortingCriterionSettings[]> {
    }

    @Layout(SortingSection.class)
    @Widget(title = "Sorting", description = "A list of sorting critera.")
    @Migration(LegacySortingSettingsMigration.class)
    @ArrayWidget(elementTitle = "Criterion", addButtonText = "Add sorting criterion", showSortButtons = true,
        elementDefaultValueProvider = TopKDefaultValueProvider.class)
    @ValueReference(SortingCriteriaRef.class)
    @Modification(TopKModification.class)
    SortingCriterionSettings[] m_sortingCriteria = new SortingCriterionSettings[]{new SortingCriterionSettings()};

    static final class TopKDefaultValueProvider extends SortingUtils.SortingCriterionDefaultValueProvider {

        TopKDefaultValueProvider() {
            super(SortingCriteriaRef.class);
        }

    }

    static final class TopKCriterionColumnChoicesProvider
        extends SortingCriterionSettings.CriterionColumnChoicesProvider<SortingCriterionSettings> {

        protected TopKCriterionColumnChoicesProvider() {
            super(SortingCriteriaRef.class);
        }
    }

    static final class TopKModification extends SortingCriterionSettings.CriterionColumnChoicesModification {

        protected TopKModification() {
            super(TopKCriterionColumnChoicesProvider.class);
        }

    }

    @Layout(FilterSection.class)
    @Widget(title = "Filter mode", description = "Specifies the mode for the top k selection of the output")
    @ValueSwitchWidget
    @Migration(FilterModeSettingsMigration.class)
    FilterMode m_filterMode = FilterMode.ROWS;

    @Widget(title = "Number of rows / unique values",
        description = "The number of rows to select from the input table based on the specified mode.")
    @NumberInputWidget(validation = IsPositiveIntegerValidation.class)
    @Layout(FilterSection.class)
    @Migration(KSettingsMigration.class)
    long m_amount = 5;

    @Widget(title = "Sort missing values to end",
        description = "If selected, missing values are always considered to be smaller to present cells.")
    @Layout(SortingSection.class)
    boolean m_missingsToEnd = true;

    @Widget(title = "Output order", description = """
            Depending on the settings of the algorithm the order might change in the output and this option \
            allows you to specify constraints on the order.
            """)
    @Layout(OutputSortingSection.class)
    @ValueSwitchWidget
    @Migration(RowOrderSettingsMigration.class)
    RowOrder m_rowOrder = RowOrder.SORTED;

    static final class LegacySortingSettingsMigration implements NodeSettingsMigration<SortingCriterionSettings[]> {

        /**
         * The key for the IncludeList in the NodeSettings.
         */
        static final String INCLUDELIST_KEY = "columns";

        /**
         * The key for the Sort Order Array in the NodeSettings.
         */
        static final String SORTORDER_KEY = "order";

        /**
         * The key for the Alphanumeric Comparison in the node settings.
         *
         * @since 4.7
         */
        static final String ALPHANUMCOMP_KEY = "alphaNumStringComp";

        private static SortingCriterionSettings[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return loadSettings(settings, null);
        }

        private static SortingCriterionSettings[] loadWithAlphanumeric(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            return loadSettings(settings, settings.getBooleanArray(ALPHANUMCOMP_KEY));
        }

        private static SortingCriterionSettings[] loadSettings(final NodeSettingsRO settings, final boolean[] comps)
            throws InvalidSettingsException {
            final var columns = settings.getStringArray(INCLUDELIST_KEY);
            final var sortKeys = settings.getBooleanArray(SORTORDER_KEY);
            final var compKeys = comps != null ? comps : new boolean[columns.length];
            return IntStream.range(0, columns.length)
                .mapToObj(i -> new SortingCriterionSettings(
                    columns[i].equals("-ROWKEY -") ? new StringOrEnum<>(ROW_ID) : new StringOrEnum<>(columns[i]),
                    sortKeys[i] ? SortingOrder.ASCENDING : SortingOrder.DESCENDING,
                    compKeys[i] ? StringComparison.NATURAL : StringComparison.LEXICOGRAPHIC))
                .toArray(SortingCriterionSettings[]::new);
        }

        @Override
        public List<ConfigMigration<SortingCriterionSettings[]>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(LegacySortingSettingsMigration::loadWithAlphanumeric)//
                    .withDeprecatedConfigPath(INCLUDELIST_KEY)//
                    .withDeprecatedConfigPath(SORTORDER_KEY) //
                    .withDeprecatedConfigPath(ALPHANUMCOMP_KEY) //
                    .build(),
                ConfigMigration.builder(LegacySortingSettingsMigration::load)//
                    .withDeprecatedConfigPath(INCLUDELIST_KEY)//
                    .withDeprecatedConfigPath(SORTORDER_KEY).build());
        }

    }

    static final class FilterModeSettingsMigration implements NodeSettingsMigration<FilterMode> {

        private static final String KEY = "selectionMode";

        private static FilterMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var mode = settings.getString(KEY);
            try {
                return FilterMode.getFromTopkMode(TopKMode.getTopKModeByText(mode));
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingsException(String.format(
                    "No selection mode was found for the input \"%s\". Change it in the configuration.", mode), e);
            }
        }

        @Override
        public List<ConfigMigration<FilterMode>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(FilterModeSettingsMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build(),
                ConfigMigration.builder(settings -> FilterMode.ROWS).build());
        }

    }

    static final class RowOrderSettingsMigration implements NodeSettingsMigration<RowOrder> {

        private static final String KEY = "outputOrder";

        private static RowOrder load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return RowOrder.getFromOutputOrder(OutputOrder.valueOf(settings.getString(KEY)));
        }

        @Override
        public List<ConfigMigration<RowOrder>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(RowOrderSettingsMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }

    }

    static final class KSettingsMigration implements NodeSettingsMigration<Long> {
        private static final String KEY = "k";

        private static Long load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return Long.valueOf(settings.getInt(KEY));
        }

        @Override
        public List<ConfigMigration<Long>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(KSettingsMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }
    }

}
