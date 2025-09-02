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
 *   Mar 10, 2025 (david): created
 */
package org.knime.base.node.preproc.filter.nominal;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.base.node.preproc.filter.nominal.NominalValueRowCommonSettings.SettingsUtils.LegacyNameFilterPersistorForNominalValueRowFilter;
import org.knime.base.node.preproc.filter.nominal.NominalValueRowCommonSettings.SettingsUtils.MigrationFromSelectedAttributes;
import org.knime.base.node.preproc.filter.nominal.NominalValueRowCommonSettings.SettingsUtils.NominalColumnWithDomainChoicesProider;
import org.knime.base.node.preproc.filter.nominal.NominalValueRowCommonSettings.SettingsUtils.NominalValueSelectionDependency;
import org.knime.base.node.preproc.filter.nominal.NominalValueRowCommonSettings.SettingsUtils.SelectedColumnDependency;
import org.knime.base.node.preproc.filter.nominal.NominalValueRowCommonSettings.SettingsUtils.SelectedColumnDomainChoicesStateProviderOnInitAndDepChange;
import org.knime.base.node.preproc.filter.nominal.NominalValueRowCommonSettings.SettingsUtils.SelectedDomainValuesStateProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DomainValuesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.DomainChoicesUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyNameFilterPersistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.StringFilter;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;

/**
 * Common settings superclass for the Nominal Value Row Filter and Nominal Value Row Splitter nodes. Since the settings
 * are the same for both nodes, they are shared here.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
abstract sealed class NominalValueRowCommonSettings implements NodeParameters {

    NominalValueRowCommonSettings() {
    }

    NominalValueRowCommonSettings(final NodeParametersInput context) {
        var spec = context.getInTableSpec(0);
        if (spec.isPresent()) {
            m_selectedColumn = spec.flatMap(SettingsUtils::getFirstCompatibleColumn).orElse(null);
        }

        // select all values by default
        m_nominalValueSelection =
            new StringFilter(DomainChoicesUtil.getChoicesByContextAndColumn(context, m_selectedColumn));
    }

    @Widget(title = SettingsUtils.TITLE_FILTER_COLUMN, description = SettingsUtils.DESC_FILTER_COLUMN)
    @ValueReference(SelectedColumnDependency.class)
    @ChoicesProvider(NominalColumnWithDomainChoicesProider.class)
    @Persist(configKey = "selected_column")
    String m_selectedColumn;

    @Widget(title = SettingsUtils.TITLE_VALUES, description = "to be replaced by widget modification")
    @ValueReference(NominalValueSelectionDependency.class)
    @ChoicesProvider(SelectedColumnDomainChoicesStateProviderOnInitAndDepChange.class)
    @ValueProvider(SelectedDomainValuesStateProvider.class)
    @Persistor(LegacyNameFilterPersistorForNominalValueRowFilter.class)
    @Migration(MigrationFromSelectedAttributes.class)
    @Modification.WidgetReference(NominalValueSelectionWidgetReference.class)
    StringFilter m_nominalValueSelection = new StringFilter();

    static final class NominalValueSelectionWidgetReference implements Modification.Reference {
    }

    /**
     * WebUI settings class for the "Nominal Value Row Filter" node.
     *
     * @author Jakob Sanowski, KNIME GmbH, Konstanz
     * @since 5.3
     */
    @Modification(NominalValueRowFilterNodeSettings.NominalValueSelectionWidgetModifier.class)
    static final class NominalValueRowFilterNodeSettings extends NominalValueRowCommonSettings {

        @Widget(title = SettingsUtils.TITLE_MISSING_VALUES, description = SettingsUtils.DESC_MISSING_VALUES)
        @ValueSwitchWidget
        @Migration(MissingValueHandling.Migration.class)
        MissingValueHandling m_missingValueHandling = MissingValueHandling.EXCLUDE;

        NominalValueRowFilterNodeSettings() {
        }

        NominalValueRowFilterNodeSettings(final NodeParametersInput context) {
            super(context);
        }

        static final class NominalValueSelectionWidgetModifier implements Modification.Modifier {

            @Override
            public void modify(final Modification.WidgetGroupModifier group) {
                group.find(NominalValueSelectionWidgetReference.class) //
                    .modifyAnnotation(Widget.class) //
                    .withProperty("description", SettingsUtils.DESC_VALUES_FILTER) //
                    .modify();
            }
        }

        /**
         * Almost duplicate of {@link NominalValueRowSplitterNodeSettings.MissingValueHandling} but kept separate
         * because we need a slightly different enum label in each case.
         */
        enum MissingValueHandling {

                @Label(value = "Exclude", description = "Missing values are excluded from the output table.")
                EXCLUDE, //
                @Label(value = "Include", description = "Missing values are included in the output table.")
                INCLUDE; //

            static class Migration implements NodeParametersMigration<MissingValueHandling> {

                private static final String KEY_INCLUDE_MISSING = "include_missing";

                @Override
                public List<ConfigMigration<MissingValueHandling>> getConfigMigrations() {
                    return List.of(ConfigMigration.builder(Migration::loadFromNominalValueRowFilterIncludeMissing)
                        .withDeprecatedConfigPath(LegacyNameFilterPersistorForNominalValueRowFilter.CFG_FILTER_CONFIG,
                            KEY_INCLUDE_MISSING)
                        .build(), //
                        /**
                         * For backwards compatibility with initial version consisting of selected_column and
                         * selected_attr. The default is "EXCLUDE" based on the default in
                         * {@link NominalValueFilterConfiguration}.
                         */
                        ConfigMigration.builder(settings -> MissingValueHandling.EXCLUDE) //
                            .build() //

                    );
                }

                private static MissingValueHandling loadFromNominalValueRowFilterIncludeMissing(
                    final NodeSettingsRO settings) throws InvalidSettingsException {
                    final var nameFilterConfig =
                        settings.getNodeSettings(LegacyNameFilterPersistorForNominalValueRowFilter.CFG_FILTER_CONFIG);
                    return nameFilterConfig.getBoolean(KEY_INCLUDE_MISSING) //
                        ? MissingValueHandling.INCLUDE //
                        : MissingValueHandling.EXCLUDE;
                }
            }
        }
    }

    /**
     * WebUI settings class for the "Nominal Value Row Splitter" node.
     *
     * @author David Hickey, TNG Technology Consulting GmbH
     */
    @Modification(NominalValueRowSplitterNodeSettings.NominalValueSelectionWidgetModifier.class)
    static final class NominalValueRowSplitterNodeSettings extends NominalValueRowCommonSettings {

        @Widget(title = SettingsUtils.TITLE_MISSING_VALUES, description = SettingsUtils.DESC_MISSING_VALUES)
        @ValueSwitchWidget
        @Migration(MissingValueHandling.Migration.class)
        MissingValueHandling m_missingValueHandling = MissingValueHandling.LOWER;

        NominalValueRowSplitterNodeSettings() {
        }

        NominalValueRowSplitterNodeSettings(final NodeParametersInput context) {
            super(context);
        }

        static final class NominalValueSelectionWidgetModifier implements Modification.Modifier {

            @Override
            public void modify(final Modification.WidgetGroupModifier group) {
                group.find(NominalValueSelectionWidgetReference.class) //
                    .modifyAnnotation(Widget.class) //
                    .withProperty("description", SettingsUtils.DESC_VALUES_SPLITTER) //
                    .modify();

                group.find(NominalValueSelectionWidgetReference.class) //
                    .addAnnotation(TwinlistWidget.class) //
                    .withProperty("includedLabel", "First table") //
                    .withProperty("excludedLabel", "Second table") //
                    .modify();
            }
        }

        /**
         * Almost duplicate of {@link NominalValueRowFilterNodeSettings.MissingValueHandling} but kept separate because
         * we need a slightly different enum label description in each case.
         */
        enum MissingValueHandling {

                @Label(value = "Second", description = """
                        Missing values are excluded from the first output table. \
                        They will be included in the second table instead.
                        """)
                LOWER, // equivalent to "EXCLUDE" in NominalValueRowFilterNodeSettings
                @Label(value = "First", description = """
                        Missing values will be included in the first output \
                        table.
                        """)
                UPPER; // equivalent to "INCLUDE" in NominalValueRowFilterNodeSettings

            static class Migration implements NodeParametersMigration<MissingValueHandling> {

                private static final String KEY_INCLUDE_MISSING = "include_missing";

                @Override
                public List<ConfigMigration<MissingValueHandling>> getConfigMigrations() {
                    return List.of(ConfigMigration.builder(Migration::loadFromNominalValueRowSplitterIncludeMissing)
                        .withDeprecatedConfigPath(LegacyNameFilterPersistorForNominalValueRowFilter.CFG_FILTER_CONFIG,
                            KEY_INCLUDE_MISSING)
                        .build(), //
                        /**
                         * For backwards compatibility with initial version consisting of selected_column and
                         * selected_attr. The default is "LOWER" based on the default in
                         * {@link NominalValueFilterConfiguration}.
                         */
                        ConfigMigration.builder(settings -> MissingValueHandling.LOWER) //
                            .build() //

                    );
                }

                private static MissingValueHandling loadFromNominalValueRowSplitterIncludeMissing(
                    final NodeSettingsRO settings) throws InvalidSettingsException {
                    final var nameFilterConfig =
                        settings.getNodeSettings(LegacyNameFilterPersistorForNominalValueRowFilter.CFG_FILTER_CONFIG);
                    return nameFilterConfig.getBoolean(KEY_INCLUDE_MISSING) //
                        ? MissingValueHandling.UPPER //
                        : MissingValueHandling.LOWER;
                }
            }
        }
    }

    static final class SettingsUtils {

        private SettingsUtils() {
            // utility class, no instances
        }

        static Optional<String> getFirstCompatibleColumn(final DataTableSpec spec) {
            return getNominalColumnsWithDomain(spec).findFirst().map(DataColumnSpec::getName);
        }

        static Stream<DataColumnSpec> getNominalColumnsWithDomain(final DataTableSpec spec) {
            if (spec == null) {
                return Stream.of();
            } else {
                return spec.stream() //
                    .filter(s -> s.getType().isCompatible(NominalValue.class))//
                    .filter(s -> s.getDomain().getValues() != null);
            }
        }

        /**
         * AP-6231: For backwards compatibility before AP version 3.4
         */
        static final class MigrationFromSelectedAttributes implements NodeParametersMigration<StringFilter> {

            /** Config key for the possible values to be included. */
            private static final String CFG_SELECTED_ATTR = "selected attributes";

            private static StringFilter loadFromSelectedAttr(final NodeSettingsRO settings)
                throws InvalidSettingsException {
                String[] selected = settings.getStringArray(CFG_SELECTED_ATTR);
                return new StringFilter(selected);
            }

            @Override
            public List<ConfigMigration<StringFilter>> getConfigMigrations() {
                return List.of(ConfigMigration.builder(MigrationFromSelectedAttributes::loadFromSelectedAttr)
                    .withDeprecatedConfigPath(CFG_SELECTED_ATTR).build());
            }

        }

        static final class LegacyNameFilterPersistorForNominalValueRowFilter extends LegacyNameFilterPersistor {

            /** Config key for filter configuration. */
            static final String CFG_FILTER_CONFIG = "filter config";

            LegacyNameFilterPersistorForNominalValueRowFilter() {
                super(CFG_FILTER_CONFIG);
            }

        }

        static final class SelectedColumnDomainChoicesStateProviderOnInitAndDepChange implements StringChoicesProvider {

            private Supplier<List<String>> m_domainValues;

            @Override
            public List<String> choices(final NodeParametersInput context) {
                return m_domainValues.get();
            }

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_domainValues = initializer.computeFromProvidedState(SelectedColumnDomainValuesProvider.class);
                initializer.computeAfterOpenDialog();
            }
        }

        static final class NominalValueSelectionDependency implements ParameterReference<StringFilter> {
        }

        /**
         * Adjusts the selected values of the name filter by moving unknown and new values to the left side. On the
         * right side, the ones contained in the new domain remain. Missing values are removed from the right side.
         */
        static final class SelectedDomainValuesStateProvider implements StateProvider<StringFilter> {

            Supplier<List<String>> m_newDomainValuesSupplier;

            Supplier<StringFilter> m_currentNameFilterSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_newDomainValuesSupplier =
                    initializer.computeFromProvidedState(SelectedColumnDomainValuesProvider.class);
                m_currentNameFilterSupplier = initializer.getValueSupplier(NominalValueSelectionDependency.class);
            }

            @Override
            public StringFilter computeState(final NodeParametersInput context) {
                final var newChoices = m_newDomainValuesSupplier.get().stream().collect(Collectors.toSet());
                final var currentSelection = m_currentNameFilterSupplier.get().filter(new String[0]);
                final var filteredSelection =
                    Arrays.stream(currentSelection).filter(newChoices::contains).toArray(String[]::new);
                return new StringFilter(filteredSelection);
            }
        }

        static final class NominalColumnWithDomainChoicesProider implements ColumnChoicesProvider {

            @Override
            public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
                final var spec = context.getInTableSpec(0);
                return getNominalColumnsWithDomain(spec.orElse(null)).toList();
            }

        }

        static final class SelectedColumnDependency implements ParameterReference<String> {
        }

        /**
         * An intermediate state provider used for updating possible values as well as selected values of the name
         * filter on column change.
         */
        static final class SelectedColumnDomainValuesProvider implements DomainValuesProvider {

            Supplier<String> m_selectedColumnSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_selectedColumnSupplier = initializer.computeFromValueSupplier(SelectedColumnDependency.class);
            }

            @Override
            public String getSelectedColumn() {
                return m_selectedColumnSupplier.get();
            }
        }

        static final String TITLE_FILTER_COLUMN = "Filter column";

        static final String DESC_FILTER_COLUMN = "Select the column containing the nominal values to be filtered.";

        static final String TITLE_VALUES = "Values";

        static final String DESC_VALUES_SPLITTER = """
                Select the nominal values to be in the first output table,
                by moving them from left (second table) to right
                (first table).
                """;

        static final String DESC_VALUES_FILTER = """
                Select the nominal values to be included in the output table, by moving them from left \
                (excluded) to right (included).
                """;

        static final String TITLE_MISSING_VALUES = "If value is missing";

        static final String DESC_MISSING_VALUES = "Defines how missing values are handled.";
    }
}
