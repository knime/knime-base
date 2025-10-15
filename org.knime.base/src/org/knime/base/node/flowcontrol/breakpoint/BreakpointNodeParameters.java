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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.flowcontrol.breakpoint;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.AutoGuessValueProvider;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Breakpoint.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class BreakpointNodeParameters implements NodeParameters {

    @Persist(configKey = BreakpointNodeDialog.CFG_KEY_ENABLED)
    @Widget(title = "Breakpoint Enabled", description = """
            Determines whether the breakpoint node should halt on a certain condition or just pass on the data.
            """)
    @ValueReference(EnabledRef.class)
    boolean m_enabled;

    @Persistor(value = BreakPointConditionPersistor.class)
    @Widget(title = "Breakpoint active for", description = """
            Specifies the condition that has to be met to halt execution.
            """)
    @ValueReference(BreakpointConditionRef.class)
    @Effect(predicate = IsEnabled.class, type = EffectType.ENABLE)
    BreakpointCondition m_breakpointCondition = BreakpointCondition.EMPTYTABLE;

    @Persist(configKey = BreakpointNodeDialog.CFG_KEY_VARIABLE_NAME)
    @Widget(title = "Select Variable", description = """
            Choose the flow variable that should be matched from a list of available variables.
            """)
    @ChoicesProvider(FlowVariableNameChoicesProvider.class)
    @ValueReference(FlowVariabelNameRef.class)
    @ValueProvider(value = FlowVariableNameProvider.class)
    @Effect(predicate = IsEnabledAndVariableMatch.class, type = EffectType.SHOW)
    String m_variableName;

    @Persist(configKey = BreakpointNodeDialog.CFG_KEY_VARIABLE_VALUE)
    @Widget(title = "Enter Variable Value", description = """
            The value to match the specified flow variable against. Can be e.g. "true", "42", or "test".
            """)
    @TextInputWidget
    @Effect(predicate = IsEnabledAndVariableMatch.class, type = EffectType.SHOW)
    String m_variableValue = "0";

    @Persistor(CustomMessagePersistor.class)
    @Widget(title = "Custom message text", description = """
            Define a custom message that is printed when the breakpoint halts the execution.
            """)
    @OptionalWidget(defaultProvider = CustomMessageDefaultProvider.class)
    @Effect(predicate = IsEnabled.class, type = EffectType.ENABLE)
    @Migration(CustomMessageMigration.class)
    Optional<String> m_customMessage = Optional.of(BreakpointNodeDialog.DEFAULT_CUSTOM_MESSAGE);

    static final class EnabledRef implements ParameterReference<Boolean> {
    }

    static final class BreakpointConditionRef implements ParameterReference<BreakpointCondition> {
    }

    static final class FlowVariabelNameRef implements ParameterReference<String> {
    }

    static final class IsEnabled implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(EnabledRef.class).isTrue();
        }
    }

    static final class IsEnabledAndVariableMatch implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(EnabledRef.class).isTrue()
                .and(i.getEnum(BreakpointConditionRef.class).isOneOf(BreakpointCondition.VARIABLEMATCH));
        }
    }

    static final class FlowVariableNameChoicesProvider implements FlowVariableChoicesProvider {
        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput input) {
            return input.getAvailableInputFlowVariables(StringType.INSTANCE, DoubleType.INSTANCE, IntType.INSTANCE)
                .values().stream().toList();
        }
    }

    static final class FlowVariableNameProvider extends AutoGuessValueProvider<String> {

        protected FlowVariableNameProvider() {
            super(FlowVariabelNameRef.class);
        }

        @Override
        protected boolean isEmpty(final String value) {
            return value == null || value.isEmpty();
        }

        @Override
        protected String autoGuessValue(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return parametersInput
                .getAvailableInputFlowVariables(StringType.INSTANCE, DoubleType.INSTANCE, IntType.INSTANCE).values()
                .stream().map(var -> var.getName()).findFirst().orElse(null);
        }

    }

    static final class CustomMessageDefaultProvider implements DefaultValueProvider<String> {

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return BreakpointNodeDialog.DEFAULT_CUSTOM_MESSAGE;
        }

    }

    static final class BreakPointConditionPersistor implements NodeParametersPersistor<BreakpointCondition> {

        @Override
        public BreakpointCondition load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return BreakpointCondition.getFromValue(
                settings.getString(BreakpointNodeDialog.CFG_KEY_BREAKPOINT_CONDITION, BreakpointNodeDialog.EMTPYTABLE));
        }

        @Override
        public void save(final BreakpointCondition param, final NodeSettingsWO settings) {
            settings.addString(BreakpointNodeDialog.CFG_KEY_BREAKPOINT_CONDITION, param.getValue());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{BreakpointNodeDialog.CFG_KEY_BREAKPOINT_CONDITION}};
        }

    }

    static final class CustomMessagePersistor implements NodeParametersPersistor<Optional<String>> {

        @Override
        public Optional<String> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean(BreakpointNodeDialog.CFG_KEY_USE_CUSTOM_MESSAGE, false)) {
                return Optional.of(settings.getString(BreakpointNodeDialog.CFG_KEY_CUSTOM_MESSAGE,
                    BreakpointNodeDialog.DEFAULT_CUSTOM_MESSAGE));
            }
            return Optional.empty();
        }

        @Override
        public void save(final Optional<String> param, final NodeSettingsWO settings) {
            if (param.isPresent()) {
                settings.addBoolean(BreakpointNodeDialog.CFG_KEY_USE_CUSTOM_MESSAGE, true);
                settings.addString(BreakpointNodeDialog.CFG_KEY_CUSTOM_MESSAGE, param.get());
            } else {
                settings.addBoolean(BreakpointNodeDialog.CFG_KEY_USE_CUSTOM_MESSAGE, false);
                settings.addString(BreakpointNodeDialog.CFG_KEY_CUSTOM_MESSAGE, "");
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{BreakpointNodeDialog.CFG_KEY_USE_CUSTOM_MESSAGE},
                {BreakpointNodeDialog.CFG_KEY_CUSTOM_MESSAGE}};
        }

    }

    static final class UseCustomMessageMigration implements DefaultProvider<Boolean> {

        @Override
        public Boolean getDefault() {
            return false;
        }

    }

    static final class CustomMessageMigration implements DefaultProvider<String> {

        @Override
        public String getDefault() {
            return BreakpointNodeDialog.DEFAULT_CUSTOM_MESSAGE;
        }

    }

    enum BreakpointCondition {

            @Label(value = "empty table", description = "Halts execution if the input table is empty.")
            EMPTYTABLE(BreakpointNodeDialog.EMTPYTABLE),
            @Label(value = "non-empty table", description = "Halts execution if the input table is not empty.")
            NONEMPTYTABLE(BreakpointNodeDialog.NONEMPTYTABLE), @Label(value = "active branch",
                description = "Halts execution if the breakpoint node is executed on an active branch.")
            ACTIVEBRANCH(BreakpointNodeDialog.ACTIVEBRANCH),
            @Label(value = "inactive branch", description = "Halts execution if the breakpoint node is on an inactive "
                + "branch, i.e., the incoming connection originates in a disabled port, such as the output port for an "
                + "<i>IF Switch</i> node or a <i>Joiner</i> node.")
            INACTIVEBRANCH(BreakpointNodeDialog.INACTIVEBRANCH), @Label(value = "variable matches value",
                description = "Halts execution if a specified flow variable matches a provided value.")
            VARIABLEMATCH(BreakpointNodeDialog.VARIABLEMATCH);

        private final String m_value;

        private BreakpointCondition(final String value) {
            m_value = value;
        }

        String getValue() {
            return m_value;
        }

        static BreakpointCondition getFromValue(final String value) throws InvalidSettingsException {
            for (final BreakpointCondition condition : values()) {
                if (condition.getValue().equals(value)) {
                    return condition;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final String name) {
            var values = List.of(BreakpointNodeDialog.EMTPYTABLE, BreakpointNodeDialog.NONEMPTYTABLE,
                BreakpointNodeDialog.VARIABLEMATCH, BreakpointNodeDialog.INACTIVEBRANCH,
                BreakpointNodeDialog.ACTIVEBRANCH).stream().collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", name, values);
        }

    }

}
