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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.base.node.util.preproc.SortingUtils.SortingCriterionSettings;
import org.knime.base.node.util.preproc.SortingUtils.SortingCriterionSettings.SortingModification;
import org.knime.base.node.util.preproc.SortingUtils.SortingOrder;
import org.knime.base.node.util.preproc.SortingUtils.StringComparison;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.TextInputWidgetValidation.MinLengthValidation;

/**
 * The settings for the "Rank" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class RankNodeSettings implements DefaultNodeSettings {

    RankNodeSettings() {

    }

    RankNodeSettings(final DefaultNodeSettingsContext context) {
        this.m_sortingCriteria = new RankingCriterionSettings[]{new RankingCriterionSettings(context)};
    }

    enum RankMode {
            @Label(value = "Same rank, then gap (e.g., 1, 1, 3, 4…)", description = """
                    Rows with the same value receive the same rank, and the next distinct value receives a rank \
                    incremented by the count of tied rows (i.e., ranking has gaps).
                    """)
            STANDARD(StandardRankAssigner::new, "Standard"), //
            @Label(value = "Same rank, no gap (e.g., 1, 1, 2, 3…)", description = """
                    Rows with the same value receive the same rank, but the next distinct value receives a rank \
                    incremented by only one (i.e., ranking has no gaps).
                    """)
            DENSE(DenseRankAssigner::new, "Dense"), //
            @Label(value = "Unique rank, no gap (e.g., 1, 2, 3, 4…)", description = """
                    Each row receives a unique rank, even if values are tied. This means ranking follows the row \
                    order, ensuring no duplicate ranks.
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

        static RankMode fromLegacyRankMode(final String legacyName) throws InvalidSettingsException {
            return Stream.of(RankMode.values())//
                .filter(m -> m.m_legacyName.equals(legacyName))//
                .findFirst()//
                .orElseThrow(() -> new InvalidSettingsException(
                    String.format("Unknown RankMode '%s' encountered.", legacyName)));
        }
    }

    enum RankDataType {
            @Label(value = "Long integer", description = """
                    Recommended for most use cases, especially for large datasets. The long integer type ensures \
                    that very large tables can be ranked without exceeding data type limitations.
                    """)
            LONG, //
            @Label(value = "Integer", description = """
                     Can be used if the dataset is small and rank values will not exceed the maximum allowed \
                     integer size. However, long integer is recommended as the default to prevent potential \
                     overflow issues in large datasets.
                    """)
            INTEGER

    }

    enum RowOrder {
            @Label(value = "Rank", description = "The table is sorted by the computed ranks.")
            RANK, //
            @Label(value = "Input order", description = """
                    This option should only be selected if necessary, as restoring the original order can be \
                    computationally expensive.
                    """)
            INPUT_ORDER
    }

    @Section(title = "Rank Ordering")
    interface SortingSection {
    }

    @Section(title = "Ranking")
    @After(SortingSection.class)
    interface RankingSection {
    }

    @Section(title = "Special Values and Performance", advanced = true)
    @After(RankingSection.class)
    interface AdvancedSection {
    }

    interface SortingCriteriaRef extends Reference<RankingCriterionSettings[]> {
    }

    interface CategoryColumnsRef extends Reference<ColumnFilter> {
    }

    @Layout(SortingSection.class)
    @Widget(title = "Rank Ordering", description = "A list of ordering criteria to assign ranks.")
    @Migration(LegacySortingSettingsMigration.class)
    @ArrayWidget(elementTitle = "Criterion", addButtonText = "Add ordering criterion", showSortButtons = true,
        elementDefaultValueProvider = DefaultCriterionProvider.class)
    @ValueReference(SortingCriteriaRef.class)
    RankingCriterionSettings[] m_sortingCriteria = new RankingCriterionSettings[]{new RankingCriterionSettings()};

    @Widget(title = "Category columns", description = """
            Defines how ranking should be grouped. If one or more columns are selected, ranking is computed \
            separately within each unique group defined by the selected columns. If no category columns are \
            specified, ranking is applied to the entire dataset.""")
    @Layout(RankingSection.class)
    @ChoicesProvider(CategoryColumnChoiceProvider.class)
    @Migration(CategoryColumnSettingsMigration.class)
    @ValueReference(CategoryColumnsRef.class)
    ColumnFilter m_categoryColumns = new ColumnFilter();

    @Widget(title = "If there are ties", description = "Defines how tied values are handled in the ranking:")
    @RadioButtonsWidget
    @Layout(RankingSection.class)
    @Migration(RankModeSettingsMigration.class)
    RankMode m_rankMode = RankMode.STANDARD;

    private static final class MinLenValidation extends MinLengthValidation {
        @Override
        public int getMinLength() {
            return 1;
        }
    }

    @Widget(title = "Rank column name",
        description = "Defines the name of the appended ranking column. This field cannot be left empty.")
    @TextInputWidget(validation = MinLenValidation.class)
    @Layout(RankingSection.class)
    @Persist(configKey = "RankOutFieldName")
    String m_rankOutFieldName = "Rank";

    @Widget(title = "Rank data type", description = "Specifies the data type for the ranking column", advanced = true)
    @ValueSwitchWidget
    @Layout(RankingSection.class)
    @Migration(RankDataTypeSettingsMigration.class)
    RankDataType m_rankDataType = RankDataType.LONG;

    @Widget(title = "Sort missing values to end of table", description = """
            If selected, missing values are always placed at the end of the ranked output, regardless of the ranking \
            order. This means that in ascending order, missing values are considered larger than any non-missing \
            value, while in descending order, they are considered smaller than any non-missing value. If left \
            unchecked (default), missing values follow the defined ranking behavior and are treated as the smallest \
            possible value.
            """, advanced = true)
    @Layout(AdvancedSection.class)
    @Migrate(loadDefaultIfAbsent = true)
    boolean m_missingToEnd;

    @Widget(title = "Row order", description = "Defines how the output table is ordered after ranking:",
        advanced = true)
    @Layout(AdvancedSection.class)
    @ValueSwitchWidget
    @Migration(RowOrderSettingsMigration.class)
    RowOrder m_rowOrder = RowOrder.RANK;

    static final class RankingModification extends SortingModification {

        @Override
        public void modify(final WidgetGroupModifier group) {
            this.getColumnModifier(group).modifyAnnotation(Widget.class).withProperty("description", """
                    Specifies the column by which rows should be ranked. If multiple criteria are defined, ranking is \
                    first applied to the primary column. Additional criteria are only considered in the event of a tie \
                    in the previous criteria.""").modify();
            this.getSortingOrderModifier(group).modifyAnnotation(Widget.class)
                .withProperty("description", "Determines whether ranking is done in ascending or descending order:")
                .modify();
            this.getColumnModifier(group).modifyAnnotation(ChoicesProvider.class)
                .withValue(CriterionColumnChoicesProvider.class).modify();
        }

    }

    //TODO REMOVE: Temporary class until UIEXT-2626 is fixed
    @Modification(RankingModification.class)
    static final class RankingCriterionSettings extends SortingCriterionSettings {

        RankingCriterionSettings() {
            super();
        }

        RankingCriterionSettings(final StringOrEnum<RowIDChoice> column, final SortingOrder sortingOrder,
            final StringComparison stringComparison) {
            super(column, sortingOrder, stringComparison);
        }

        RankingCriterionSettings(final DataColumnSpec colSpec) {
            super(colSpec);
        }

        RankingCriterionSettings(final DefaultNodeSettingsContext context) {
            super(context);
        }

    }

    static final class CategoryColumnChoiceProvider implements ColumnChoicesProvider {

        private Supplier<RankingCriterionSettings[]> m_criterion;

        @Override
        public void init(final StateProviderInitializer initializer) {
            this.m_criterion = initializer.computeFromValueSupplier(SortingCriteriaRef.class);
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public List<DataColumnSpec> columnChoices(final DefaultNodeSettingsContext context) {
            final var criterion = Arrays.stream(m_criterion.get()).map(RankingCriterionSettings::getColumn)
                .filter(column -> column.getEnumChoice().isEmpty()).map(StringOrEnum::getStringChoice)
                .collect(Collectors.toSet());
            return context.getDataTableSpec(0).map(DataTableSpec::stream) //
                .orElseGet(Stream::empty) //
                .filter(spec -> !criterion.contains(spec.getName())).toList();
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

        private static RankDataType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var rankAsLong = settings.getBoolean(KEY);
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

        private static RowOrder load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var retainOrder = settings.getBoolean(KEY);
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

    static final class LegacySortingSettingsMigration implements NodeSettingsMigration<RankingCriterionSettings[]> {

        private static final String COL_KEY = "RankingColumns";

        private static final String COL_ORDER_KEY = "RankOrder";

        private static RankingCriterionSettings[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var columns = settings.getStringArray(COL_KEY);
            final var sortKeys = settings.getStringArray(COL_ORDER_KEY);
            return IntStream.range(0, columns.length)
                .mapToObj(i -> new RankingCriterionSettings(new StringOrEnum<>(columns[i]),
                    "Ascending".equals(sortKeys[i]) ? SortingOrder.ASCENDING : SortingOrder.DESCENDING,
                    StringComparison.LEXICOGRAPHIC))
                .toArray(RankingCriterionSettings[]::new);
        }

        @Override
        public List<ConfigMigration<RankingCriterionSettings[]>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(LegacySortingSettingsMigration::load) //
                    .withDeprecatedConfigPath(COL_KEY)//
                    .withDeprecatedConfigPath(COL_ORDER_KEY) //
                    .build());
        }
    }

    static final class DefaultCriterionProvider implements StateProvider<RankingCriterionSettings> {

        private Supplier<ColumnFilter> m_categoryColumns;

        private Supplier<RankingCriterionSettings[]> m_criterion;

        @Override
        public void init(final StateProviderInitializer initializer) {
            this.m_categoryColumns = initializer.computeFromValueSupplier(CategoryColumnsRef.class);
            this.m_criterion = initializer.computeFromValueSupplier(SortingCriteriaRef.class);
        }

        @Override
        public RankingCriterionSettings computeState(final DefaultNodeSettingsContext context) {
            final var spec = context.getDataTableSpec(0);
            if (spec.isEmpty()) {
                return new RankingCriterionSettings();
            }
            final var usedColumns = Stream
                .concat(Arrays.stream(this.m_categoryColumns.get().filterFromFullSpec(spec.get())),
                    Arrays.stream(this.m_criterion.get()).map(RankingCriterionSettings::getColumn)
                        .filter(column -> column.getEnumChoice().isEmpty()).map(StringOrEnum::getStringChoice))
                .collect(Collectors.toSet());
            final var firstAvailableColumn =
                spec.get().stream().filter(colSpec -> !usedColumns.contains(colSpec.getName())).findFirst();
            return firstAvailableColumn.map(RankingCriterionSettings::new).orElse(new RankingCriterionSettings());
        }

    }

    static final class CriterionColumnChoicesProvider implements ColumnChoicesProvider {
        private Supplier<ColumnFilter> m_categoryColumns;

        private Supplier<RankingCriterionSettings[]> m_criterions;

        private Supplier<StringOrEnum<RowIDChoice>> m_columSelection;

        @Override
        public void init(final StateProviderInitializer initializer) {
            this.m_categoryColumns = initializer.computeFromValueSupplier(CategoryColumnsRef.class);
            this.m_criterions = initializer.computeFromValueSupplier(SortingCriteriaRef.class);
            this.m_columSelection = initializer.getValueSupplier(SortingCriterionSettings.getColumnRef());
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public List<DataColumnSpec> columnChoices(final DefaultNodeSettingsContext context) {
            final var spec = context.getDataTableSpec(0);
            if (spec.isEmpty()) {
                return Collections.emptyList();
            }
            final var columns = Stream
                .concat(Arrays.stream(m_categoryColumns.get().filterFromFullSpec(spec.get())),
                    Arrays.stream(m_criterions.get()).map(RankingCriterionSettings::getColumn)
                        .filter(column -> column.getEnumChoice().isEmpty()).map(StringOrEnum::getStringChoice))
                .filter(stringChoice -> !stringChoice.equals(this.m_columSelection.get().getStringChoice()))
                .collect(Collectors.toSet());
            return spec.get().stream().filter(colSpec -> !columns.contains(colSpec.getName())).toList();
        }
    }
}
