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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoice;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.EmptyOrColumnNameValidation;
import org.knime.time.node.extract.durationperiod.ExtractDurationPeriodFieldsNodeSettings.SelectedInputColumnHelpers;
import org.knime.time.node.extract.durationperiod.ExtractFieldSettings.OutputColumnNamePlaceholderProvider.ExtractableFieldsReference;

/**
 * A widget group representing a TimeUnit to extract from a {@link DataCell} of type {@link DurationValue} or
 * {@link PeriodValue}, and an associated output column name. Intended to be used as part of an {@link ArrayWidget}.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ExtractFieldSettings implements NodeParameters {

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
    @ChoicesProvider(FilteredPossibleFieldsChoices.class)
    @Layout(ExtractFieldWidgetLayout.class)
    @ValueReference(ExtractableFieldsReference.class)
    ExtractableField m_field;

    @Widget(title = "Column name",
        description = "The name of the column populated with the values of the selected field.")
    @Layout(ExtractFieldWidgetLayout.class)
    @TextInputWidget(placeholderProvider = OutputColumnNamePlaceholderProvider.class,
        patternValidation = ColumnNameValidationUtils.EmptyOrColumnNameValidation.class)
    String m_outputcolumnName;

    /**
     * A state provider that computes the placeholder text for the output column name text input field in the
     * {@link ExtractFieldSettings}.
     */
    static final class OutputColumnNamePlaceholderProvider implements StateProvider<String> {

        static final class ExtractableFieldsReference implements ParameterReference<ExtractableField> {
        }

        private Supplier<ExtractableField> m_valueSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_valueSupplier = initializer.computeFromValueSupplier(ExtractableFieldsReference.class);
        }

        @Override
        public String computeState(final NodeParametersInput context) {
            return Optional.ofNullable(m_valueSupplier.get()) //
                .map(OutputColumnNamePlaceholderProvider::getPlaceholder) //
                .orElse("");
        }

       static String getPlaceholder(final ExtractableField field) {
            return EnumChoice.fromEnumConst(field).text();
        }
    }

    /**
     * A state provider that computes the choices for the dropdown box that selects the field to extract in the
     * {@link ExtractFieldSettings}.
     */
    static final class FilteredPossibleFieldsChoices implements EnumChoicesProvider<ExtractableField> {

        private Supplier<String> m_selectedInputColumnNameSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_selectedInputColumnNameSupplier = initializer.computeFromValueSupplier( //
                SelectedInputColumnHelpers.ValueRef.class //
            );
        }

        @Override
        public List<ExtractableField> choices(final NodeParametersInput context) throws WidgetHandlerException {
            var inputTableSpec = context.getInTableSpec(0);
            var selectedColumn = m_selectedInputColumnNameSupplier.get();

            var selectedColumnType = inputTableSpec //
                .map(spec -> spec.getColumnSpec(selectedColumn)) //
                .filter(Objects::nonNull) //
                .map(DataColumnSpec::getType);

            return selectedColumnType.isEmpty() //
                ? List.of()//
                : Arrays.stream(ExtractableField.values()) //
                    .filter(v -> v.isCompatibleWith(selectedColumnType.get())) //
                    .toList();
        }

    }

    /**
     * A state provider that computes the default value for the {@link ExtractableField} enum, which will be used by the
     * {@link DefaultExtractFieldWidgetProvider} to provide the initial {@link ExtractFieldSettings} widget group
     * settings.
     */
    static final class DefaultEnumProvider implements StateProvider<ExtractableField> {

        private Supplier<List<EnumChoice<ExtractableField>>> m_possibleDropDownChoicesSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            this.m_possibleDropDownChoicesSupplier =
                initializer.computeFromProvidedState(FilteredPossibleFieldsChoices.class);
        }

        @Override
        public ExtractableField computeState(final NodeParametersInput context) {
            return m_possibleDropDownChoicesSupplier.get().stream() //
                .map(EnumChoice::id) //
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
        public ExtractFieldSettings computeState(final NodeParametersInput context) {
            final var choices = m_defaultExtractableFieldSupplier.get();

            return (choices == null) //
                ? new ExtractFieldSettings() //
                : new ExtractFieldSettings(choices, "");
        }
    }
}
