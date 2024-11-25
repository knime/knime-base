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
 *   Mar 7, 2024 (jakob): created
 */
package org.knime.base.node.preproc.filter.nominal;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.EnumFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyNameFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.NameFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.DomainValuesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * WebUI settings class for the "Nominal Value Row Filter" node.
 *
 * @author Jakob Sanowski, KNIME GmbH, Konstanz
 * @since 5.3
 */
@SuppressWarnings("restriction")
public class NominalValueRowFilterSettings implements DefaultNodeSettings {

    static final class NominalColumnWithDomainChoicesProider implements ColumnChoicesProvider {

        /**
         * {@inheritDoc}
         */
        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            final var spec = context.getDataTableSpec(0);
            return getNominalColumnsWithDomain(spec.orElse(null)).toArray(DataColumnSpec[]::new);
        }

    }

    static final class SelectedColumnDependency implements Reference<String> {
    }

    @Widget(title = "Filter column", description = "Select the column containing the nominal values to be filtered.")
    @ValueReference(SelectedColumnDependency.class)
    @ChoicesWidget(choices = NominalColumnWithDomainChoicesProider.class)
    @Persist(configKey = NominalValueRowSplitterNodeDialog.CFG_SELECTED_COL)
    public String m_selectedColumn;

    /**
     * An intermediate state provider used for updating possible values as well as selected values of the name filter on
     * column change.
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

    static final class NominalValueSelectionDependency implements Reference<NameFilter> {
    }

    /**
     * Adjusts the selected values of the name filter by moving unknown and new values to the left side. On the right
     * side, the ones contained in the new domain remain. Missing values are removed from the right side.
     */
    static final class SelectedDomainValuesStateProvider implements StateProvider<NameFilter> {

        Supplier<List<String>> m_newDomainValuesSupplier;

        Supplier<NameFilter> m_currentNameFilterSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_newDomainValuesSupplier = initializer.computeFromProvidedState(SelectedColumnDomainValuesProvider.class);
            m_currentNameFilterSupplier = initializer.getValueSupplier(NominalValueSelectionDependency.class);
        }

        @Override
        public NameFilter computeState(final DefaultNodeSettingsContext context) {
            final var newChoices = m_newDomainValuesSupplier.get().stream().collect(Collectors.toSet());
            final var currentSelection = m_currentNameFilterSupplier.get().getSelected(new String[0]);
            final var filteredSelection =
                Arrays.stream(currentSelection).filter(newChoices::contains).toArray(String[]::new);
            return new NameFilter(filteredSelection);
        }

    }

    /**
     * AP-6231: For backwards compatibility before AP version 3.4
     */
    static final class LegacyNameFilterOrSelectedAttrPersistor extends NodeSettingsPersistorWithConfigKey<NameFilter> {

        /** Config key for the possible values to be included. */
        static final String CFG_SELECTED_ATTR = "selected attributes";

        private LegacyNameFilterPersistor m_legacyNameFilterPersistor;

        /**
         * {@inheritDoc}
         */
        @Override
        public void setConfigKey(final String configKey) {
            super.setConfigKey(configKey);
            m_legacyNameFilterPersistor = new LegacyNameFilterPersistor();
            m_legacyNameFilterPersistor.setConfigKey(configKey);
        }

        @Override
        public NameFilter load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(CFG_SELECTED_ATTR)) {
                String[] selected = settings.getStringArray(CFG_SELECTED_ATTR);
                return new NameFilter(selected);
            }
            return m_legacyNameFilterPersistor.load(settings);
        }

        @Override
        public String[] getConfigKeys() {
            return m_legacyNameFilterPersistor.getConfigKeys();
        }

        @Override
        public void save(final NameFilter nameFilter, final NodeSettingsWO settings) {
            m_legacyNameFilterPersistor.save(nameFilter, settings);
        }

    }

    static final class SelectedColumnDomainChoicesStateProviderOnInitAndDepChange
        implements StringChoicesStateProvider {

        private Supplier<List<String>> m_domainValues;

        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            return m_domainValues.get().toArray(String[]::new);
        }

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_domainValues = initializer.computeFromProvidedState(SelectedColumnDomainValuesProvider.class);
            initializer.computeAfterOpenDialog();
        }

    }

    @Widget(title = "Values",
        description = "Select the nominal values to be in the primary output table, by moving them from left "
            + "(excluded) to right (included).")
    @ValueReference(NominalValueSelectionDependency.class)
    @ChoicesWidget(choicesProvider = SelectedColumnDomainChoicesStateProviderOnInitAndDepChange.class)
    @ValueProvider(SelectedDomainValuesStateProvider.class)
    @Persist(configKey = NominalValueRowSplitterNodeDialog.CFG_CONFIGROOTNAME,
        customPersistor = LegacyNameFilterOrSelectedAttrPersistor.class)
    public NameFilter m_nominalValueSelection = new NameFilter();

    enum MissingValueHandling {
            @Label(value = "Exclude", description = "Missing values are excluded from the primary output table.")
            EXCLUDE, //
            @Label(value = "Include", description = "Missing values are included in the primary output table.")
            INCLUDE, //
    }

    static final class MissingValueHandlingPersistor extends NodeSettingsPersistorWithConfigKey<MissingValueHandling> {

        private static final String KEY_INCLUDE_MISSING = "include_missing";

        private EnumFieldPersistor<MissingValueHandling> m_persistor;

        @Override
        public void setConfigKey(final String configKey) {
            super.setConfigKey(configKey);
            m_persistor = new EnumFieldPersistor<>(configKey, MissingValueHandling.class);
        }

        @Override
        public MissingValueHandling load(final NodeSettingsRO settings) throws InvalidSettingsException {
            // key for setting in current version of the node
            if (settings.containsKey(getConfigKey())) {
                return m_persistor.load(settings);
            }

            // key for setting in previous version of the node
            if (settings.containsKey(NominalValueRowSplitterNodeDialog.CFG_CONFIGROOTNAME)) {
                final var nameFilterConfig =
                    settings.getNodeSettings(NominalValueRowSplitterNodeDialog.CFG_CONFIGROOTNAME);
                return nameFilterConfig.getBoolean(KEY_INCLUDE_MISSING) ? MissingValueHandling.INCLUDE
                    : MissingValueHandling.EXCLUDE;

            }
            /**
             * For backwards compatibility with initial version consisting of selected_column and selected_attr. The
             * default is "EXCLUDE" based on the default in {@link NominalValueFilterConfiguration}.
             */
            return MissingValueHandling.EXCLUDE;
        }

        @Override
        public void save(final MissingValueHandling obj, final NodeSettingsWO settings) {
            m_persistor.save(obj, settings);
        }

        @Override
        public ConfigsDeprecation[] getConfigsDeprecations() {
            return new ConfigsDeprecation[]{new ConfigsDeprecation.Builder().forNewConfigPath(getConfigKey())
                .forDeprecatedConfigPath(NominalValueRowSplitterNodeDialog.CFG_CONFIGROOTNAME, KEY_INCLUDE_MISSING)
                .build()};
        }
    }

    @Widget(title = "Missing value handling", description = "Defines how missing values are handled.")
    @ValueSwitchWidget
    @Persist(customPersistor = MissingValueHandlingPersistor.class)
    MissingValueHandling m_missingValueHandling = MissingValueHandling.EXCLUDE;

    public NominalValueRowFilterSettings() {
        this((DataTableSpec)null);
    }

    public NominalValueRowFilterSettings(final DefaultNodeSettingsContext context) {
        this(context.getDataTableSpecs()[0]);
    }

    public NominalValueRowFilterSettings(final DataTableSpec spec) {
        m_selectedColumn = getFirstCompatibleColumn(spec).orElse(null);
    }

    private static Optional<String> getFirstCompatibleColumn(final DataTableSpec spec) {
        return getNominalColumnsWithDomain(spec).findFirst().map(DataColumnSpec::getName);
    }

    private static Stream<DataColumnSpec> getNominalColumnsWithDomain(final DataTableSpec spec) {
        if (spec == null) {
            return Stream.of();
        } else {
            return spec.stream() //
                .filter(s -> s.getType().isCompatible(NominalValue.class))//
                .filter(s -> s.getDomain().getValues() != null);
        }
    }
}
