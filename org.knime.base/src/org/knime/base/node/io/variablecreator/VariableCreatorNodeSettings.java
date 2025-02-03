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
 *   Feb 3, 2025 (david): created
 */
package org.knime.base.node.io.variablecreator;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * The settings for the Variable Creator Node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class VariableCreatorNodeSettings implements DefaultNodeSettings {

    @Widget(title = "New flow variables", description = "The new flow variables to create.")
    @ArrayWidget( //
        addButtonText = "Add variable", //
        showSortButtons = false, //
        elementDefaultValueProvider = NewFlowVariableSettings.DefaultNewFlowVariableSettingsProvider.class //
    )
    @ValueReference(NewFlowVariablesRef.class)
    @Persistor(NewFlowVariablesPersistor.class)
    NewFlowVariableSettings[] m_newFlowVariables = new NewFlowVariableSettings[]{ //
        new NewFlowVariableSettings("variable_1", VariableType.STRING, "") //
    };

    static final class NewFlowVariableSettings implements DefaultNodeSettings {

        @HorizontalLayout
        interface NewFlowVariableSettingsLayout {
        }

        @Layout(NewFlowVariableSettingsLayout.class)
        @Widget(title = "Name", description = "The name of the new flow variable.")
        String m_name;

        @Layout(NewFlowVariableSettingsLayout.class)
        @Widget(title = "Type", description = """
                The data type of the new flow variable. See the node description \
                for more information about the available types.
                """)
        @ChoicesWidget(choices = VariableType.VariableTypeChoicesProvider.class)
        @ValueReference(NewFlowVariablesTypeRef.class)
        VariableType m_type;

        @Layout(NewFlowVariableSettingsLayout.class)
        @Widget(title = "Value", description = "The value of the new flow variable.")
        @ValueProvider(NewFlowVariableValueProvider.class)
        String m_value;

        NewFlowVariableSettings(final String name, final VariableType type, final String value) {
            m_name = name;
            m_type = type;
            m_value = value;
        }

        NewFlowVariableSettings() {
            this("variable_1", VariableType.STRING, "");
        }

        static final class DefaultNewFlowVariableSettingsProvider implements StateProvider<NewFlowVariableSettings> {

            private Supplier<NewFlowVariableSettings[]> m_valueSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
                m_valueSupplier = initializer.computeFromValueSupplier(NewFlowVariablesRef.class);
            }

            @Override
            public NewFlowVariableSettings computeState(final DefaultNodeSettingsContext context) {
                return new NewFlowVariableSettings("variable_" + (m_valueSupplier.get().length + 1),
                    VariableType.STRING, VariableType.STRING.m_defaultValue);
            }
        }

        static final class NewFlowVariablesTypeRef implements Reference<VariableType> {
        }

        static final class NewFlowVariableValueProvider implements StateProvider<String> {

            private Supplier<VariableType> m_typeSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_typeSupplier = initializer.computeFromValueSupplier(NewFlowVariablesTypeRef.class);
            }

            @Override
            public String computeState(final DefaultNodeSettingsContext context) {
                return m_typeSupplier.get().m_defaultValue;
            }
        }
    }

    enum VariableType {

            @Label(value = "String")
            STRING("", "str"), //
            @Label(value = "Integer")
            INTEGER("0", "int"), //
            @Label(value = "Long")
            LONG("0", "long"), //
            @Label(value = "Double")
            DOUBLE("0.0", "double"), //
            @Label(value = "Boolean")
            BOOLEAN("false", "bool");

        private final String m_defaultValue;

        private final String m_oldConfigValue;

        VariableType(final String defaultValue, final String oldConfigValue) {

            this.m_defaultValue = defaultValue;
            this.m_oldConfigValue = oldConfigValue;
        }

        String niceName() {
            return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
        }

        static VariableType getByOldConfigValue(final String value) {
            return Arrays.stream(values()) //
                .filter(type -> type.m_oldConfigValue.equals(value)) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException("Unknown type: %s. Allowed types are %s"
                    .formatted(value, Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", ")))));
        }

        static final class VariableTypeChoicesProvider implements ChoicesProvider {

            @Override
            public IdAndText[] choicesWithIdAndText(final DefaultNodeSettingsContext context) {
                return Arrays.stream(values()) //
                    .map(type -> new IdAndText(type.name(), type.niceName())) //
                    .toArray(IdAndText[]::new);
            }
        }
    }

    static final class NewFlowVariablesRef implements Reference<NewFlowVariableSettings[]> {
    }

    static final class NewFlowVariablesPersistor implements NodeSettingsPersistor<NewFlowVariableSettings[]> {

        private static final String CFG_KEY_VARIABLES = "variables";

        private static final String CFG_SUBKEY_TYPES = "types";

        private static final String CFG_SUBKEY_NAMES = "names";

        private static final String CFG_SUBKEY_VALUES = "values";

        @Override
        public NewFlowVariableSettings[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var subConfig = settings.getConfig(CFG_KEY_VARIABLES);

            var types = subConfig.getStringArray(CFG_SUBKEY_TYPES);
            var names = subConfig.getStringArray(CFG_SUBKEY_NAMES);
            var values = subConfig.getStringArray(CFG_SUBKEY_VALUES);

            // let's just check these all have the same length
            if (types.length != names.length || names.length != values.length) {
                throw new InvalidSettingsException("names, types and values must have the same length");
            }

            return IntStream.range(0, types.length) //
                .mapToObj(i -> new NewFlowVariableSettings( //
                    names[i], //
                    VariableType.getByOldConfigValue(types[i]), //
                    values[i]//
                )) //
                .toArray(NewFlowVariableSettings[]::new);
        }

        @Override
        public void save(final NewFlowVariableSettings[] obj, final NodeSettingsWO settings) {
            var types = Arrays.stream(obj).map(v -> v.m_type.m_oldConfigValue).toArray(String[]::new);
            var names = Arrays.stream(obj).map(v -> v.m_name).toArray(String[]::new);
            var values = Arrays.stream(obj).map(v -> v.m_value).toArray(String[]::new);

            var subConfig = settings.addConfig(CFG_KEY_VARIABLES);
            subConfig.addStringArray(CFG_SUBKEY_TYPES, types);
            subConfig.addStringArray(CFG_SUBKEY_NAMES, names);
            subConfig.addStringArray(CFG_SUBKEY_VALUES, values);
        }

        @Override
        public String[][] getConfigPaths() {
            var subKeys = List.of(CFG_SUBKEY_TYPES, CFG_SUBKEY_NAMES, CFG_SUBKEY_VALUES);

            return subKeys.stream() //
                .map(subKey -> new String[]{CFG_KEY_VARIABLES, subKey}) //
                .toArray(String[][]::new);
        }
    }
}
