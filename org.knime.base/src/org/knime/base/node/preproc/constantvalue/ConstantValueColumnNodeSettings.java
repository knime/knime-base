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
 *   Feb 5, 2025 (david): created
 */
package org.knime.base.node.preproc.constantvalue;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.data.DataCellFactory.FromString;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.ClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DefaultClassIdStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.DynamicParameters.DynamicParametersProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.createcell.CreateDataCellExtensionsUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.createcell.CreateDataCellParameters;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.DataTypeChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;

/**
 * Settings for the Constant Value Column WebUI node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ConstantValueColumnNodeSettings implements NodeParameters {

    @Widget(title = "", description = "")
    @ArrayWidget( //
        addButtonText = "Add column", //
        elementDefaultValueProvider = NewColumnSettings.InitialNewColumnSettingsValueProvider.class, //
        elementTitle = "Constant column" //
    )
    @ValueReference(NewColumnSettingsArrayRef.class)
    NewColumnSettings[] m_newColumnSettings = new NewColumnSettings[]{new NewColumnSettings()};

    static final class NewColumnSettings implements NodeParameters {

        NewColumnSettings() {
        }

        NewColumnSettings(final NodeParametersInput context) {
            m_columnNameToReplace = context.getInTableSpec(0) //
                .stream() //
                .map(DataTableSpec::stream).flatMap(Function.identity()) //
                .map(DataColumnSpec::getName) //
                .findFirst() //
                .orElse("");
        }

        @Widget(title = "Output column", description = "Whether to replace an existing column or append a new one.")
        @ValueReference(AppendOrReplace.Ref.class)
        @ValueSwitchWidget
        AppendOrReplace m_replaceOrAppend = AppendOrReplace.APPEND;

        @Widget(title = "New column", description = "The name of the new column.")
        @Effect(predicate = AppendOrReplace.IsReplace.class, type = EffectType.HIDE)
        @TextInputWidget(placeholder = "New column name",
            patternValidation = ColumnNameValidationUtils.ColumnNameValidation.class)
        String m_columnNameToAppend = "New column";

        @Widget(title = "Replace column", description = "The name of the column to replace.")
        @ChoicesProvider(AllColumnsProvider.class)
        @Effect(predicate = AppendOrReplace.IsReplace.class, type = EffectType.SHOW)
        String m_columnNameToReplace;

        @Widget(title = "Column type", description = "The type of the new column.")
        @ChoicesProvider(SupportedDataTypeChoicesProvider.class)
        @ValueReference(DataTypeRef.class)
        DataType m_type = StringCellFactory.TYPE;

        @Widget(title = "Fill value", description = """
                Whether to use a custom value for the new column, or fill it with \
                missing cells.
                """)
        @ValueReference(CustomOrMissingValue.Ref.class)
        @ValueSwitchWidget
        CustomOrMissingValue m_customOrMissingValue = CustomOrMissingValue.MISSING;

        static final String DYNAMIC_PARAMETERS_KAI_SCHEMA =
            """
                    {
                      "type": "object",
                      "properties": {
                        "@class": {
                          "const": "org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.createcell.CoreCreateDataCellParameters.FromStringCellParameters"
                        },
                        "value": {
                          "type": "string",
                          "title": "Custom value",
                          "description": "The value to fill the new constant column with. If the column type is not String, an attempt will be made to convert the string to the target type."
                        }
                      },
                      "required": ["@class", "value"]
                    }
                    """;

        @Effect(predicate = CustomOrMissingValue.IsMissing.class, type = EffectType.HIDE)
        @DynamicParameters(value = DataCellParametersProvider.class,
            schemaForDefaultKaiNodeInterface = DYNAMIC_PARAMETERS_KAI_SCHEMA, //
            widgetAppearingInNodeDescription = @Widget(//
                title = CreateDataCellParameters.CUSTOM_VALUE_TITLE,
                description = CreateDataCellParameters.CUSTOM_VALUE_DESCRIPTION//
            ))
        @ValueReference(SelfReference.class)
        CreateDataCellParameters m_customValueParameters;

        static class DataTypeRef implements ParameterReference<DataType> {
        }

        static class SelfReference implements ParameterReference<CreateDataCellParameters> {
        }

        static final class DataCellParametersProvider implements DynamicParametersProvider<CreateDataCellParameters> {

            private Supplier<DataType> m_computeFromValueSupplier;

            private Supplier<CreateDataCellParameters> m_currentValue;

            final Map<DataType, Class<? extends CreateDataCellParameters>> m_parameterClasses;

            private static final Class<? extends CreateDataCellParameters> FROM_STRING_CLASS =
                CreateDataCellExtensionsUtil.getFromStringCreateDataCellParametersClass();

            DataCellParametersProvider() {
                final var parameterClassesFromExtensions =
                    CreateDataCellExtensionsUtil.getCreateDataCellParametersExtensions();
                m_parameterClasses = parameterClassesFromExtensions;
            }

            @Override
            public ClassIdStrategy<CreateDataCellParameters> getClassIdStrategy() {
                final var possibleClasses = Stream.concat(//
                    m_parameterClasses.values().stream(), //
                    Stream.of(FROM_STRING_CLASS)//
                ).toList();
                return new DefaultClassIdStrategy<>(possibleClasses);
            }

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeBeforeOpenDialog();
                m_computeFromValueSupplier = initializer.computeFromValueSupplier(DataTypeRef.class);
                m_currentValue = initializer.getValueSupplier(SelfReference.class);
            }

            @Override
            public CreateDataCellParameters computeParameters(final NodeParametersInput parametersInput)
                throws StateComputationFailureException {
                final var currentType = m_computeFromValueSupplier.get();
                final var currentValue = m_currentValue.get();

                final var targetClass = m_parameterClasses.getOrDefault(currentType, FROM_STRING_CLASS);

                if (currentValue != null && targetClass.isInstance(currentValue)) {
                    return currentValue;
                }
                try {
                    final var constructor = targetClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    return constructor.newInstance();
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    throw new IllegalStateException("Could not create new instance of " + targetClass, e);
                }

            }

        }

        enum AppendOrReplace {

                @Label(value = "Append", description = "Append a new column")
                APPEND, //
                @Label(value = "Replace", description = "Replace an existing column")
                REPLACE;

            static final class Ref implements ParameterReference<AppendOrReplace> {
            }

            static final class IsReplace implements EffectPredicateProvider {
                @Override
                public EffectPredicate init(final PredicateInitializer i) {
                    return i.getEnum(AppendOrReplace.Ref.class).isOneOf(REPLACE);
                }
            }
        }

        enum CustomOrMissingValue {

                @Label(value = "Missing", description = "Fill the new constant column with missing cells")
                MISSING,
                @Label(value = "Custom value", description = "Use a custom value to fill the new constant column")
                CUSTOM; //

            static final class Ref implements ParameterReference<CustomOrMissingValue> {
            }

            static final class IsMissing implements EffectPredicateProvider {
                @Override
                public EffectPredicate init(final PredicateInitializer i) {
                    return i.getEnum(CustomOrMissingValue.Ref.class).isOneOf(MISSING);
                }
            }
        }

        static final class InitialNewColumnSettingsValueProvider implements StateProvider<NewColumnSettings> {

            private Supplier<NewColumnSettings[]> m_currentNewColumnSettings;

            @Override
            public NewColumnSettings computeState(final NodeParametersInput context) {
                var newSettings = new NewColumnSettings(context);

                var existingNewColumns = m_currentNewColumnSettings.get();
                var numberOfAppendedColumns = Arrays.stream(existingNewColumns) //
                    .filter(s -> s.m_replaceOrAppend == AppendOrReplace.APPEND) //
                    .count();

                newSettings.m_columnNameToAppend = "New column " + (numberOfAppendedColumns + 1);
                return newSettings;
            }

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
                m_currentNewColumnSettings = initializer.computeFromValueSupplier(NewColumnSettingsArrayRef.class);
            }
        }
    }

    static final class NewColumnSettingsArrayRef implements ParameterReference<NewColumnSettings[]> {
    }

    static final class SupportedDataTypeChoicesProvider implements DataTypeChoicesProvider {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public List<DataType> choices(final NodeParametersInput context) {
            return DataTypeRegistry.getInstance().availableDataTypes().stream() //
                .filter(d -> {
                    var factory = d.getCellFactory(null);
                    return factory.map(FromString.class::isInstance).orElse(false);
                }).toList();
        }

    }
}
