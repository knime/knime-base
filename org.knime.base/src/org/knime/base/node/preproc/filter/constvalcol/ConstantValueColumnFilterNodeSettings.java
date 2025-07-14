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
 *   Jul 9, 2025 (marcbux): created
 */
package org.knime.base.node.preproc.filter.constvalcol;

import java.util.List;
import java.util.Optional;

import org.knime.base.node.util.LegacyColumnFilterMigration;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.util.column.ColumnSelectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.AllColumnsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.ColumnFilterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation;

@SuppressWarnings("restriction")
class ConstantValueColumnFilterNodeSettings implements DefaultNodeSettings {

    enum FilterMode {
            @Label(value = "all constant value columns", description = """
                    Remove all constant value columns from the columns considered for filtering,
                    independent of the specific value they contain in duplicates.
                    """)
            ALL, @Label(value = "constant value columns that only contain", description = """
                    Remove only those constant value columns from the columns considered for filtering
                    that only contain a certain specific value.
                    """)
            BY_VALUE,
    }

    interface FilterNumericRef extends Reference<FilterMode> {
    }

    static final class IsFilterByValueProvider implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(FilterNumericRef.class).isOneOf(FilterMode.BY_VALUE);
        }
    }

    static final class FilterModeMigration implements NodeSettingsMigration<FilterMode> {

        private static final String LEGACY_CFG_KEY_FILTER_ALL = "filter-all";

        private static final String LEGACY_LABEL_FILTER_ALL = "all constant value columns";

        @Override
        public List<ConfigMigration<FilterMode>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(FilterModeMigration::load)
                .withDeprecatedConfigPath(LEGACY_CFG_KEY_FILTER_ALL).build());
        }

        private static FilterMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getString(LEGACY_CFG_KEY_FILTER_ALL).equals(LEGACY_LABEL_FILTER_ALL) ? FilterMode.ALL
                : FilterMode.BY_VALUE;
        }
    }

    @Widget(title = "Remove from the selected columns",
        description = "Choose whether to remove all constant value columns or only those containing a specific value.")
    @ValueReference(FilterNumericRef.class)
    @Migration(FilterModeMigration.class)
    FilterMode m_filterMode = FilterMode.ALL;

    static final class FilterNumericMigration implements NodeSettingsMigration<Optional<Double>> {

        private static final String LEGACY_CFG_KEY_FILTER_NUMERIC = "filter-numeric";

        private static final String LEGACY_CFG_KEY_FILTER_NUMERIC_VALUE = "filter-numeric-value";

        @Override
        public List<ConfigMigration<Optional<Double>>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(FilterNumericMigration::load)
                .withDeprecatedConfigPath(LEGACY_CFG_KEY_FILTER_NUMERIC)
                .withDeprecatedConfigPath(LEGACY_CFG_KEY_FILTER_NUMERIC_VALUE).build());
        }

        private static Optional<Double> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(LEGACY_CFG_KEY_FILTER_NUMERIC)
                ? Optional.of(settings.getDouble(LEGACY_CFG_KEY_FILTER_NUMERIC_VALUE)) : Optional.empty();
        }
    }

    @Widget(title = "numeric values of",
        description = "Remove only those constant value columns that only contain a certain specific numeric value.")
    @Effect(type = EffectType.SHOW, predicate = IsFilterByValueProvider.class)
    @Migration(FilterNumericMigration.class)
    Optional<Double> m_filterNumeric = Optional.empty();

    static final class FilterStringMigration implements NodeSettingsMigration<Optional<String>> {

        private static final String LEGACY_CFG_KEY_FILTER_STRING = "filter-string";

        private static final String LEGACY_CFG_KEY_FILTER_STRING_VALUE = "filter-string-value";

        @Override
        public List<ConfigMigration<Optional<String>>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(FilterStringMigration::load)
                .withDeprecatedConfigPath(LEGACY_CFG_KEY_FILTER_STRING)
                .withDeprecatedConfigPath(LEGACY_CFG_KEY_FILTER_STRING_VALUE).build());
        }

        private static Optional<String> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(LEGACY_CFG_KEY_FILTER_STRING)
                ? Optional.of(settings.getString(LEGACY_CFG_KEY_FILTER_STRING_VALUE)) : Optional.empty();
        }
    }

    @Widget(title = "String values of",
        description = "Remove only those constant value columns that only contain a certain specific textual value.")
    @Effect(type = EffectType.SHOW, predicate = IsFilterByValueProvider.class)
    @Migration(FilterStringMigration.class)
    Optional<String> m_filterString = Optional.empty();

    static final class FilterMissingMigration implements NodeSettingsMigration<Boolean> {

        private static final String LEGACY_CFG_KEY_FILTER_MISSING = "filter-missing";

        @Override
        public List<ConfigMigration<Boolean>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(FilterMissingMigration::load)
                .withDeprecatedConfigPath(LEGACY_CFG_KEY_FILTER_MISSING).build());
        }

        private static Boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(LEGACY_CFG_KEY_FILTER_MISSING);
        }
    }

    @Widget(title = "missing values",
        description = "Remove only those constant value columns that only contain empty cells / missing values.")
    @Effect(type = EffectType.SHOW, predicate = IsFilterByValueProvider.class)
    @Migration(FilterMissingMigration.class)
    boolean m_filterMissing;

    static final class ConsideredColumnsMigration extends LegacyColumnFilterMigration {
        ConsideredColumnsMigration() {
            super("filter-list");
        }
    }

    @Widget(title = "Considered columns",
        description = "This list contains the column names of the input table that are to be considered for filtering.")
    @ColumnFilterWidget(choicesProvider = AllColumnsProvider.class)
    @Migration(ConsideredColumnsMigration.class)
    ColumnFilter m_consideredColumns;

    static final class IsPositiveValidation extends MinValidation {
        @Override
        public double getMin() {
            return 1;
        }
    }

    static final class MinRowsMigration implements NodeSettingsMigration<Long> {

        private static final String LEGACY_CFG_KEY_ROW_THRESHOLD = "row-threshold";

        @Override
        public List<ConfigMigration<Long>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(MinRowsMigration::load)
                .withDeprecatedConfigPath(LEGACY_CFG_KEY_ROW_THRESHOLD).build());
        }

        private static Long load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getLong(LEGACY_CFG_KEY_ROW_THRESHOLD);
        }
    }

    @Widget(title = "Minimum number of rows", description = """
            The minimum number of rows a table must have to be considered for filtering. If the table size is below the
            specified value, the table will not be filtered / altered.
            """, advanced = true)
    @NumberInputWidget(minValidation = IsPositiveValidation.class)
    @Migration(MinRowsMigration.class)
    long m_minRows = 1;

    ConstantValueColumnFilterNodeSettings() {
    }

    ConstantValueColumnFilterNodeSettings(final DefaultNodeSettingsContext context) {
        final var inputSpec = context.getDataTableSpec(0);
        if (inputSpec.isPresent()) {
            m_consideredColumns = new ColumnFilter(ColumnSelectionUtil.getAllColumnsOfFirstPort(context));
        }
    }
}
