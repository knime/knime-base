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
 *   Nov 20, 2024 (david): created
 */
package org.knime.time.node.extract.durationperiod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DefaultPersistorWithDeprecations;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.time.node.extract.durationperiod.ExtractDurationPeriodFieldsNodeSettings.SelectedInputColumnHelpers;
import org.knime.time.node.extract.durationperiod.ExtractFieldSettings.OutputColumnNamePlaceholderProvider.ExtractableFieldsReference;

/**
 * A widget group representing a TimeUnit to extract from a {@link DataCell} of type {@link DurationValue} or
 * {@link PeriodValue}, and an associated output column name. Intended to be used as part of an {@link ArrayWidget}.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ExtractFieldSettings implements DefaultNodeSettings {

    public ExtractFieldSettings() {
    }

    public ExtractFieldSettings(final ExtractableField field, final String outputColumnName) {
        m_field = field;
        m_outputcolumnName = outputColumnName;
    }

    @HorizontalLayout
    interface ExtractFieldWidgetLayout {
    }

    @Widget(title = "Field", description = "The type of field to extract.")
    @ChoicesWidget(choicesProvider = FilteredPossibleFieldsChoices.class)
    @Layout(ExtractFieldWidgetLayout.class)
    @ValueReference(ExtractableFieldsReference.class)
    ExtractableField m_field;

    @Widget(title = "Column name", description = """
            The name of the column populated with the values of the selected field. Cannot be empty!
            """)
    @Layout(ExtractFieldWidgetLayout.class)
    @TextInputWidget(placeholderProvider = OutputColumnNamePlaceholderProvider.class)
    String m_outputcolumnName;

    /**
     * Backwards compatible persistor for the {@link ExtractFieldSettings} class. Will load in the old configuration
     * format if it is present, and convert it to the new format.
     */
    static class Persistor extends NodeSettingsPersistorWithConfigKey<ExtractFieldSettings[]>
        implements DefaultPersistorWithDeprecations<ExtractFieldSettings[]> {

        private static final String OLD_KEY_USE_SUBSECONDS = "subsecond";

        private static final String OLD_KEY_SUBSECOND_UNITS = "subsecond_units";

        private static final String KEY_NEW_CONFIG_PATH = "extracted_fields";

        private static ExtractFieldSettings[] loadOldConfig(final NodeSettingsRO settings) {

            Predicate<ExtractableField> isSelectedInOldConfig = field -> field.getOldConfigValue() //
                .map(key -> settings.getBoolean(key, false)) //
                .orElse(false);

            final List<ExtractFieldSettings> fieldsExceptSubSeconds = List.of( //
                ExtractableField.YEARS, //
                ExtractableField.MONTHS, //
                ExtractableField.DAYS, //
                ExtractableField.HOURS, //
                ExtractableField.MINUTES, //
                ExtractableField.SECONDS //
            ).stream() //
                .filter(isSelectedInOldConfig) //
                .map(extractableFieldPresentInOldConfig -> new ExtractFieldSettings( //
                    extractableFieldPresentInOldConfig, //
                    // old config value is same as old output column name, so we can use it here
                    extractableFieldPresentInOldConfig.getOldConfigValue().orElseThrow() //
                )) //
                .collect(Collectors.toCollection(ArrayList<ExtractFieldSettings>::new)); // modifiable list

            // skip the subseconds if the old config is missing the key
            if (settings.getBoolean(OLD_KEY_USE_SUBSECONDS, false)) {
                var subsecondUnits = ExtractableField.getByOldConfigValue( //
                    settings.getString( //
                        OLD_KEY_SUBSECOND_UNITS, //
                        ExtractableField.MILLIS_ALL.getOldConfigValue().orElseThrow() //
                    ) //
                );
                fieldsExceptSubSeconds.add(new ExtractFieldSettings( //
                    subsecondUnits, //
                    // old config value is same as old output column name, so we can use it here
                    subsecondUnits.getOldConfigValue().orElseThrow() //
                ));
            }

            return fieldsExceptSubSeconds.toArray(ExtractFieldSettings[]::new);
        }

        @Override
        public List<ConfigsDeprecation<ExtractFieldSettings[]>> getConfigsDeprecations() {
            var deprecationBuilder = ConfigsDeprecation.<ExtractFieldSettings[]> builder(Persistor::loadOldConfig);

            Arrays.stream(ExtractableField.values()) //
                .map(ExtractableField::getOldConfigValue) //
                .flatMap(Optional::stream) //
                .forEach(deprecationBuilder::withDeprecatedConfigPath);
            deprecationBuilder.withDeprecatedConfigPath(OLD_KEY_USE_SUBSECONDS);

            deprecationBuilder.withMatcher(settings -> !settings.containsKey(getConfigKey())); //
            deprecationBuilder.forNewConfigPath(KEY_NEW_CONFIG_PATH);

            return List.of(deprecationBuilder.build());
        }
    }

    /**
     * A state provider that computes the placeholder text for the output column name text input field in the
     * {@link ExtractFieldSettings}.
     */
    static final class OutputColumnNamePlaceholderProvider implements StateProvider<String> {

        static final class ExtractableFieldsReference implements Reference<ExtractableField> {
        }

        private Supplier<ExtractableField> m_valueSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_valueSupplier = initializer.computeFromValueSupplier(ExtractableFieldsReference.class);
        }

        @Override
        public String computeState(final DefaultNodeSettingsContext context) {
            return Optional.of(m_valueSupplier.get()) //
                .map(ExtractableField::niceName) //
                .orElse("");
        }
    }

    /**
     * A state provider that computes the choices for the dropdown box that selects the field to extract in the
     * {@link ExtractFieldSettings}.
     */
    static final class FilteredPossibleFieldsChoices implements StringChoicesStateProvider {

        private Supplier<String> m_selectedInputColumnNameSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_selectedInputColumnNameSupplier = initializer.computeFromValueSupplier( //
                SelectedInputColumnHelpers.ValueRef.class //
            );
        }

        @Override
        public IdAndText[] computeState(final DefaultNodeSettingsContext context) throws WidgetHandlerException {
            var inputTableSpec = context.getDataTableSpec(0);
            var selectedColumn = m_selectedInputColumnNameSupplier.get();

            var selectedColumnType = inputTableSpec //
                .map(spec -> spec.getColumnSpec(selectedColumn)) //
                .filter(Objects::nonNull) //
                .map(DataColumnSpec::getType);

            return selectedColumnType.isEmpty() //
                ? new IdAndText[0] //
                : Arrays.stream(ExtractableField.values()) //
                    .filter(v -> v.isCompatibleWith(selectedColumnType.get())) //
                    .map(FilteredPossibleFieldsChoices::extractableFieldToIdAndText) //
                    .toArray(IdAndText[]::new); //
        }

        private static IdAndText extractableFieldToIdAndText(final ExtractableField field) {
            return new IdAndText(field.name(), field.niceName());
        }
    }

    /**
     * A state provider that computes the default value for the {@link ExtractableField} enum, which will be used by the
     * {@link DefaultExtractFieldWidgetProvider} to provide the initial {@link ExtractFieldSettings} widget group
     * settings.
     */
    static final class DefaultEnumProvider implements StateProvider<ExtractableField> {

        private Supplier<IdAndText[]> m_possibleDropDownChoicesSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            this.m_possibleDropDownChoicesSupplier =
                initializer.computeFromProvidedState(FilteredPossibleFieldsChoices.class);
        }

        @Override
        public ExtractableField computeState(final DefaultNodeSettingsContext context) {
            return Arrays.stream(m_possibleDropDownChoicesSupplier.get()) //
                .map(IdAndText::id) //
                .map(ExtractableField::valueOf) //
                .findFirst() //
                .orElse(null);
        }
    }

    /**
     * A state provider that provides the initial settings when adding a new {@link ExtractFieldSettings} widget group
     * to the array layout.
     */
    static final class DefaultExtractFieldWidgetProvider implements StateProvider<ExtractFieldSettings> {

        private Supplier<ExtractableField> m_defaultExtractableFieldSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_defaultExtractableFieldSupplier = initializer.computeFromProvidedState(DefaultEnumProvider.class);
        }

        @Override
        public ExtractFieldSettings computeState(final DefaultNodeSettingsContext context) {
            final var choices = m_defaultExtractableFieldSupplier.get();

            return (choices == null) //
                ? new ExtractFieldSettings() //
                : new ExtractFieldSettings(choices, "");
        }
    }
}
