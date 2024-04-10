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
 *   Mar 12, 2024 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.sorter;

import java.util.Optional;

import org.knime.base.node.preproc.sorter.SorterNodeSettings.SortingCriterionSettings.SortingOrder;
import org.knime.base.node.preproc.sorter.SorterNodeSettings.SortingCriterionSettings.StringComparison;
import org.knime.base.node.preproc.sorter.dialog.DynamicSorterPanel;
import org.knime.base.node.util.SortKeyItem;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DefaultFieldNodeSettingsPersistorFactory;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DeprecatedConfigs;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.IsColumnOfTypeCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.AllColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;

/**
 *
 * @author Paul Bärnreuther
 * @since 5.3
 */
final class SorterNodeSettings implements DefaultNodeSettings {

    SorterNodeSettings() {
    }

    SorterNodeSettings(final DefaultNodeSettingsContext context) {
        m_sortingCriteria = new SortingCriterionSettings[]{new SortingCriterionSettings(context)};
    }

    static final class SortingCriterionSettings implements DefaultNodeSettings {

        private SortingCriterionSettings(final ColumnSelection column, final SortingOrder sortingOrder,
            final StringComparison stringComparison) {
            m_column = column;
            m_sortingOrder = sortingOrder;
            m_stringComparison = stringComparison;

        }

        SortingCriterionSettings() {
            this((DataColumnSpec)null);
        }

        SortingCriterionSettings(final DefaultNodeSettingsContext context) {
            this(context.getDataTableSpec(0).flatMap(Optional::ofNullable)
                .map(spec -> spec.getNumColumns() == 0 ? null : spec.getColumnSpec(0)).flatMap(Optional::ofNullable)
                .orElse(null));
        }

        SortingCriterionSettings(final DataColumnSpec colSpec) {
            m_column = colSpec == null ? SpecialColumns.ROWID.toColumnSelection() : new ColumnSelection(colSpec);
        }

        static final class IsStringColumnCondition extends IsColumnOfTypeCondition {

            @Override
            public Class<? extends DataValue> getDataValueClass() {
                return StringValue.class;
            }

        }

        @Widget(title = "Column",
            description = "Sort rows by the values in this column. "
                + "If you set multiple sorting criteria, the table is sorted by the first criterion. "
                + "The following criteria are only considered, if the comparison by all previous "
                + "criteria results in a tie.")
        @ChoicesWidget(choices = AllColumnChoicesProvider.class, showRowKeysColumn = true)
        @Signal(condition = IsStringColumnCondition.class)
        ColumnSelection m_column;

        enum SortingOrder {
                @Label(value = "Ascending",
                    description = "The smallest or earliest in the order will appear at the top of the list. "
                        + "E.g., for numbers the sort is smallest to largest, "
                        + "for dates the sort will be oldest dates to most recent.")
                ASCENDING,
                @Label(value = "Descending",
                    description = "The largest or latest in the order will appear at the top of the list. "
                        + "E.g., for numbers the sort is largest to smallest, "
                        + "for dates the sort will be most recent dates to oldest.")
                DESCENDING;
        }

        @Widget(title = "Order", description = "Specifies the sorting order:")
        @ValueSwitchWidget
        SortingOrder m_sortingOrder = SortingOrder.ASCENDING;

        enum StringComparison {
                @Label(value = "Natural",
                    description = "Sorts strings by treating the numberic parts of a string as one character. "
                        + "For example, results in sort order “'Row1', 'Row2', 'Row10'”.")
                NATURAL,
                @Label(value = "Lexicographic",
                    description = "Sorts strings so that each digit is treated as a separated character. "
                        + "For example, results in sort order “'Row1', 'Row10', 'Row2'”.")
                LEXICOGRAPHIC;
        }

        @Widget(title = "String comparison", description = "Specifies which type of sorting to apply to the strings:",
            advanced = true)
        @Effect(signals = IsStringColumnCondition.class, type = EffectType.SHOW)
        @ValueSwitchWidget
        StringComparison m_stringComparison = StringComparison.NATURAL;

    }

    static final class LoadDeprecatedSortingCriterionArraySettings
        extends NodeSettingsPersistorWithConfigKey<SortingCriterionSettings[]> {

        /**
         * The key for the IncludeList in the NodeSettings.
         */
        private static final String LEGACY_INCLUDELIST_KEY = "incllist";

        /**
         * The key for the Sort Order Array in the NodeSettings.
         */
        private static final String LEGACY_SORTORDER_KEY = "sortOrder";

        /**
         * The key for the Alphanumeric Comparison in the node settings.
         *
         * @since 4.7
         */
        private static final String LEGACY_ALPHANUMCOMP_KEY = "alphaNumStringComp";

        private static final String LEGACY_ROW_ID = DynamicSorterPanel.ROWKEY.getName();

        private NodeSettingsPersistor<SortingCriterionSettings[]> m_defaultPersistor;

        @Override
        public void setConfigKey(final String configKey) {
            super.setConfigKey(configKey);
            m_defaultPersistor = DefaultFieldNodeSettingsPersistorFactory
                .createDefaultPersistor(SortingCriterionSettings[].class, configKey);

        }

        @Override
        public SortingCriterionSettings[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(LEGACY_INCLUDELIST_KEY)) {
                return loadFromLegacySettings(settings);
            }
            return m_defaultPersistor.load(settings);

        }

        private static SortingCriterionSettings[] loadFromLegacySettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            SortKeyItem.validate(LEGACY_INCLUDELIST_KEY, LEGACY_SORTORDER_KEY, LEGACY_ALPHANUMCOMP_KEY, settings);
            final var sortKeyItems =
                SortKeyItem.loadFrom(LEGACY_INCLUDELIST_KEY, LEGACY_SORTORDER_KEY, LEGACY_ALPHANUMCOMP_KEY, settings);
            return sortKeyItems.stream().map(LoadDeprecatedSortingCriterionArraySettings::toCriterion)
                .toArray(SortingCriterionSettings[]::new);
        }

        private static SortingCriterionSettings toCriterion(final SortKeyItem item) {
            final var column = getColumnSelection(item);
            final var sortingOrder = item.isAscendingOrder() ? SortingOrder.ASCENDING : SortingOrder.DESCENDING;
            final var stringComparison =
                item.isAlphaNumComp() ? StringComparison.NATURAL : StringComparison.LEXICOGRAPHIC;
            return new SortingCriterionSettings(column, sortingOrder, stringComparison);

        }

        private static ColumnSelection getColumnSelection(final SortKeyItem item) {
            final var identifier = item.getIdentifier();
            if (LEGACY_ROW_ID.equals(identifier)) {
                return SpecialColumns.ROWID.toColumnSelection();
            }
            return new ColumnSelection(item.getIdentifier(), null);
        }

        @Override
        public void save(final SortingCriterionSettings[] criteria, final NodeSettingsWO settings) {
            m_defaultPersistor.save(criteria, settings);
        }

        @Override
        public DeprecatedConfigs[] getDeprecatedConfigs() {
            return new DeprecatedConfigs[]{new DeprecatedConfigs.DeprecatedConfigsBuilder() //
                .forDeprecatedConfigPath(LEGACY_INCLUDELIST_KEY)//
                .forDeprecatedConfigPath(LEGACY_ALPHANUMCOMP_KEY) //
                .forDeprecatedConfigPath(LEGACY_SORTORDER_KEY) //
                .forNewConfigPath(getConfigKey()).build()};
        }

    }

    @Section(title = "Sorting")
    interface Criteria {
    }

    @Layout(Criteria.class)
    @Widget(title = "Sorting", description = "A list of sorting critera.")
    @Persist(customPersistor = LoadDeprecatedSortingCriterionArraySettings.class)
    @ArrayWidget(elementTitle = "Criterion", addButtonText = "Add sorting criterion", showSortButtons = true)
    SortingCriterionSettings[] m_sortingCriteria = new SortingCriterionSettings[]{new SortingCriterionSettings()};

    @Section(title = "Special Values and Performance", advanced = true)
    @After(Criteria.class)
    interface Options {
    }

    @Persist(configKey = "missingToEnd", optional = true)
    @Widget(title = "Sort missing values to end of table",
        description = "If selected missing values are always placed at the end of the sorted output. This is"
            + " independent of the sort order, i.e. if sorted ascendingly they are"
            + " considered to be larger than a non-missing value and if sorted descendingly"
            + " they are smaller than any non-missing value.")
    @Layout(Options.class)
    boolean m_sortMissingCellsToEndOfList;

    @Persist(configKey = "sortinmemory", optional = true)
    @Widget(title = "Sort in memory",
        description = "If selected the table is sorted in memory which requires more memory, but is faster. "
            + "In case the input table is large and memory is scarce it is recommended not to check this option.")
    @Layout(Options.class)
    boolean m_sortInMemory;

}
