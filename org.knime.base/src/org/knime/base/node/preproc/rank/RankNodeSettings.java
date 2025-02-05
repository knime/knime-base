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
 *   Feb 5, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.rank;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.base.node.util.preproc.SortingUtils.SortingOrder;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.AllColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;

/**
 * The settings for the "Rank" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class RankNodeSettings implements DefaultNodeSettings {

    enum RankMode {
            @Label(value = "Standard", description = """
                    Rows with the same values in the ranking attributes receive the same rank and the next row with a \
                    different value receives a rank that is increased by the number of rows that had the same rank. \
                    Therefore the ranking has gaps.
                    """)
            STANDARD(StandardRankAssigner::new, "Standard"), //
            @Label(value = "Dense", description = """
                    Rows with the same values in the ranking attributes receive the same rank but the row with \
                    the next different value receives a rank that is only incremented by one.
                    """)
            DENSE(DenseRankAssigner::new, "Dense"), //
            @Label(value = "Ordinal", description = """
                    The ranking is consecutive, even rows with the same values in the ranking attributes receive \
                    unique ranks.
                    """)
            ORDINAL(i -> new OrdinalRankAssigner(), "Ordinal");

        private final Function<int[], RankAssigner> m_rankAssignerFactory;

        private final String m_legacyName;

        RankMode(final Function<int[], RankAssigner> rankAssignerFactory, final String legacyName) {
            m_rankAssignerFactory = rankAssignerFactory;
            m_legacyName = legacyName;
        }

        RankAssigner createRankAssigner(final int[] rankColumnIndices) {
            return m_rankAssignerFactory.apply(rankColumnIndices);
        }

        static RankMode fromString(final String enumName) throws InvalidSettingsException {
            return Stream.of(RankMode.values())//
                .filter(m -> m.name().equals(enumName))//
                .findFirst()//
                .orElseThrow(
                    () -> new InvalidSettingsException(String.format("Unknown RankMode '%s' encountered.", enumName)));
        }

        static RankMode fromLegacyRankMode(final String legacyName) throws InvalidSettingsException {
            return Stream.of(RankMode.values())//
                .filter(m -> m.m_legacyName.equals(legacyName))//
                .findFirst()//
                .orElseThrow(() -> new InvalidSettingsException(
                    String.format("Unknown RankMode '%s' encountered.", legacyName)));
        }
    }

    enum RankDataType {
            @Label("Integer")
            INTEGER, //
            @Label(value = "Long",
                description = "It is recommended to use this option only if the input table is very large.")
            LONG
    }

    enum RowOrder {
            @Label("Rank")
            RANK, //
            @Label(value = "Input order", description = """
                    The original row order is retained. This option should only be checked if really necessary \
                    because the restoration of the row order is runtime intensive.
                    """)
            INPUT_ORDER
    }

    @Section(title = "Sorting")
    interface SortingSection {
    }

    @Section(title = "Ranking")
    @After(SortingSection.class)
    interface RankingSection {
    }

    @Section(title = "Ranking", advanced = true)
    @After(RankingSection.class)
    interface AdvancedSection {
    }

    @Layout(SortingSection.class)
    @Widget(title = "Sorting", description = "A list of sorting critera.")
    @Migration(LegacySortingSettingsMigration.class)
    @ArrayWidget(elementTitle = "Criterion", addButtonText = "Add sorting criterion", showSortButtons = true)
    SortingCriterionSettings[] m_sortingCriteria = new SortingCriterionSettings[]{new SortingCriterionSettings()};

    @Widget(title = "Category columns", description = """

            """)
    @Layout(RankingSection.class)
    @ChoicesWidget(choices = AllColumnChoicesProvider.class)
    @Migration(CategoryColumnSettingsMigration.class)
    ColumnFilter m_categoryColumns = new ColumnFilter();

    @Widget(title = "Rank mode", description = "There are three possible ranking modes:")
    @ValueSwitchWidget
    @Layout(RankingSection.class)
    @Migration(RankModeSettingsMigration.class)
    RankMode m_rankMode = RankMode.STANDARD;

    @Widget(title = "Rank column name", description = """
            The name that the appended ranking column should have. An empty name is not permitted.
            """)
    @TextInputWidget(minLength = 1)
    @Layout(RankingSection.class)
    @Persist(configKey = "RankOutFieldName")
    String m_rankOutFieldName = "Rank";

    @Widget(title = "Rank data type",
        description = "Decides if the appended rank attribute should be of type Long or Integer")
    @ValueSwitchWidget
    @Layout(RankingSection.class)
    @Migration(RankDataTypeSettingsMigration.class)
    RankDataType m_rankDataType = RankDataType.INTEGER;

    @Widget(title = "Sort missing values to end of table", description = """
            If selected, missing values are always considered to be inferior to present cells.
            """, advanced = true)
    @Layout(AdvancedSection.class)
    @Migrate(loadDefaultIfAbsent = true)
    boolean m_missingtoEnd;

    @Widget(title = "Row order", description = "Order of the output", advanced = true)
    @Layout(AdvancedSection.class)
    @ValueSwitchWidget
    @Migration(RowOrderSettingsMigration.class)
    RowOrder m_rowOrder = RowOrder.RANK;

    static final class SortingCriterionSettings implements DefaultNodeSettings {

        /**
         * @param column
         * @param sortingOrder
         */
        public SortingCriterionSettings(final String column, final SortingOrder sortingOrder) {
            m_column = column;
            m_sortingOrder = sortingOrder;
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

    }

    static final class CategoryColumnSettingsMigration implements NodeSettingsMigration<ColumnFilter> {

        private static final String KEY = "GroupColumns";

        private static ColumnFilter load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var columns = settings.getStringArray(KEY);
            return new ColumnFilter(columns);
        }

        @Override
        public List<ConfigMigration<ColumnFilter>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(CategoryColumnSettingsMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }
    }

    static final class RankModeSettingsMigration implements NodeSettingsMigration<RankMode> {

        private static final String KEY = "RankMode";

        private static RankMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var rankMode = settings.getString(KEY);
            return RankMode.fromLegacyRankMode(rankMode);
        }

        @Override
        public List<ConfigMigration<RankMode>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(RankModeSettingsMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }
    }

    static final class RankDataTypeSettingsMigration implements NodeSettingsMigration<RankDataType> {

        private static final String KEY = "RankAsLong";

        private static RankDataType load(final NodeSettingsRO settings) {
            final var rankAsLong = settings.getBoolean(KEY, false);
            return rankAsLong ? RankDataType.LONG : RankDataType.INTEGER;
        }

        @Override
        public List<ConfigMigration<RankDataType>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(RankDataTypeSettingsMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }
    }

    static final class RowOrderSettingsMigration implements NodeSettingsMigration<RowOrder> {

        private static final String KEY = "RetainRowOrder";

        private static RowOrder load(final NodeSettingsRO settings) {
            final var retainOrder = settings.getBoolean(KEY, false);
            return retainOrder ? RowOrder.INPUT_ORDER : RowOrder.RANK;
        }

        @Override
        public List<ConfigMigration<RowOrder>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(RowOrderSettingsMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }
    }

    static final class LegacySortingSettingsMigration implements NodeSettingsMigration<SortingCriterionSettings[]> {

        private static final String COL_KEY = "RankingColumns";

        private static final String COL_ORDER_KEY = "RankOrder";

        private static SortingCriterionSettings[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var columns = settings.getStringArray(COL_KEY);
            final var sortKeys = settings.getStringArray(COL_ORDER_KEY, new String[columns.length]);
            return IntStream.range(0, columns.length)
                .mapToObj(i -> new SortingCriterionSettings(columns[i],
                    "Ascending".equals(sortKeys[i]) ? SortingOrder.ASCENDING : SortingOrder.DESCENDING))
                .toArray(SortingCriterionSettings[]::new);
        }

        @Override
        public List<ConfigMigration<SortingCriterionSettings[]>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(LegacySortingSettingsMigration::load) //
                    .withDeprecatedConfigPath(COL_KEY)//
                    .withDeprecatedConfigPath(COL_ORDER_KEY) //
                    .build());
        }
    }
}
