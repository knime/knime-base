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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

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
    @Migration(NewFlowVariablesMigrator.class)
    NewFlowVariableSettings[] m_newFlowVariables = new NewFlowVariableSettings[]{ //
        new NewFlowVariableSettings() //
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
        @ValueReference(NewFlowVariablesTypeRef.class)
        FlowVariableType m_type;

        @Layout(NewFlowVariableSettingsLayout.class)
        @Widget(title = "Value", description = "The value of the new flow variable.")
        String m_value;

        NewFlowVariableSettings(final String name, final FlowVariableType type, final String value) {
            m_name = name;
            m_type = type;
            m_value = value;
        }

        NewFlowVariableSettings() {
            this("variable_1", FlowVariableType.STRING, "");
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
                    FlowVariableType.STRING, "");
            }
        }

        static final class NewFlowVariablesTypeRef implements Reference<FlowVariableType> {
        }
    }

    enum FlowVariableType {

            @Label(value = "String", description = """
                    A string of characters. This is the default when a new variable \
                    is created. The default value is an empty string.
                    """)
            STRING("str", VariableType.StringType.INSTANCE, Function.identity(), x -> true), //
            @Label(value = "Integer", description = """
                    An integer number with possible values from 2&#179;&#185;-1 \
                    to -2&#179;&#185;. The value must be a valid number (consisting only of an \
                    optional sign (&#8220;+&#8221;/&#8220;-&#8221;) or \
                    &#8220;0&#8221;-&#8220;9&#8221;) and be in the range above. If the size of your \
                    value exceeds the limits above, you can try to use a <i>Long</i> or <i>Double</i> \
                    value instead.
                    """)
            INTEGER("int", VariableType.IntType.INSTANCE, Integer::parseInt, s -> Ints.tryParse(s) != null), //
            @Label(value = "Long", description = """
                    An integer number with possible values from 2&#8310;&#170;-1 \
                    to -2&#8310;&#170;. The value must be a valid number (consisting only of an \
                    optional sign (&#8220;+&#8221;/&#8220;-&#8221;) or \
                    &#8220;0&#8221;-&#8220;9&#8221;) and be in the range above.
                    """)
            LONG("long", VariableType.LongType.INSTANCE, Long::parseLong, s -> Longs.tryParse(s) != null), //
            @Label(value = "Double", description = """
                    A floating point decimal number with possible values from around \
                    4.9&#183;10&#8315;&#179;&#178;&#8308; to 1.8&#183;10&#179;&#8304;&#8312; \
                    in both the positive and negative range. The value must be a valid number \
                    (consisting only of an optional sign (&#8220;+&#8221;/&#8220;-&#8221;) or \
                    &#8220;0&#8221;-&#8220;9&#8221;). You can specify an exponent by appending \
                    &#8220;e&#8221; followed by the exponent. Apart from a numeric value you can \
                    also specify one of the following three (case-sensitive) special values: \
                    <ul>
                        <li><i>Infinity</i> for positive infinity</li>
                        <li><i>-Infinity</i> for negative infinity</li>
                        <li><i>NaN</i> for &#8220;Not a Number&#8221;</li>
                    </ul>
                    If the number is too big or too small, it may be converted into one of these \
                    special values. (You will be warned if this happens). You should keep in mind \
                    that you may lose some precision for big values or values that are very close \
                    to zero.
                    """)
            DOUBLE("double", VariableType.DoubleType.INSTANCE, Double::parseDouble, s -> Doubles.tryParse(s) != null), //
            @Label(value = "Boolean", description = """
                    A boolean value, either &#8220;true&#8221; or \
                    &#8220;false&#8221;.  The default value is &#8220;false&#8221;.  Any value that \
                    is not equal (ignoring case) to 'true' will be treated as false.
                    """)
            BOOLEAN("bool", VariableType.BooleanType.INSTANCE, Boolean::parseBoolean, s -> true);

        private final String m_oldConfigValue;

        private final VariableType<?> m_knimeVariableType;

        private final Function<String, ?> m_converter;

        private final Predicate<String> m_canConvert;

        FlowVariableType(final String oldConfigValue, final VariableType<?> knimeType,
            final Function<String, ?> converter, final Predicate<String> canConvert) {
            m_oldConfigValue = oldConfigValue;
            m_knimeVariableType = knimeType;
            m_converter = converter;
            m_canConvert = canConvert;
        }

        VariableType<?> getKnimeVariableType() {
            return m_knimeVariableType;
        }

        @SuppressWarnings("unchecked")
        <T> void convertAndPush(final String value, final BiConsumer<VariableType<T>, T> pushConsumer) {
            pushConsumer.accept((VariableType<T>)m_knimeVariableType, (T)m_converter.apply(value));
        }

        boolean canConvert(final String value) {
            return m_canConvert.test(value);
        }

        String niceName() {
            return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
        }

        static FlowVariableType getByOldConfigValue(final String value) {
            return Arrays.stream(values()) //
                .filter(type -> type.m_oldConfigValue.equals(value)) //
                .findFirst() //
                .orElseThrow(() -> new IllegalArgumentException("Unknown type: %s. Allowed types are %s"
                    .formatted(value, Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", ")))));
        }
    }

    static final class NewFlowVariablesRef implements Reference<NewFlowVariableSettings[]> {
    }

    static final class NewFlowVariablesMigrator implements NodeSettingsMigration<NewFlowVariableSettings[]> {

        private static final String CFG_KEY_VARIABLES = "variables";

        private static final String CFG_SUBKEY_TYPES = "types";

        private static final String CFG_SUBKEY_NAMES = "names";

        private static final String CFG_SUBKEY_VALUES = "values";

        @Override
        public List<ConfigMigration<NewFlowVariableSettings[]>> getConfigMigrations() {
            return List.of( //
                ConfigMigration.builder(NewFlowVariablesMigrator::load) //
                    .withMatcher(NewFlowVariablesMigrator::isOldSettings) //
                    .build());
        }

        static NewFlowVariableSettings[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
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
                    FlowVariableType.getByOldConfigValue(types[i]), //
                    values[i]//
                )) //
                .toArray(NewFlowVariableSettings[]::new);
        }

        static boolean isOldSettings(final NodeSettingsRO settings) {
            return settings.containsKey(CFG_KEY_VARIABLES);
        }
    }
}
