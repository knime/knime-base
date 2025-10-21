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

package org.knime.base.node.flowvariable.variabletocredentials;

import java.util.List;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersInputImpl;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.NoneChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.AutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

/**
 * Node parameters for Variable to Credentials.
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class VariableToCredentialsNodeParameters implements NodeParameters {

    /**
     * Flow variable choices provider that provides only string flow variables
     */
    static final class StringFlowVariablesProvider implements FlowVariableChoicesProvider {
        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            return flowVariableStream(context).toList();
        }

        static final Stream<FlowVariable> flowVariableStream(final NodeParametersInput context) {
            return ((NodeParametersInputImpl)context).getAvailableInputFlowVariables(VariableType.StringType.INSTANCE)
                .values().stream().filter(v -> !v.getName().equals("knime.workspace"))
                .sorted((v1, v2) -> v1.getName().compareTo(v2.getName()));
        }
    }

    @Widget(title = "Credentials name", description = "The name of the new credentials variable.")
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @Persist(configKey = "credentialsName")
    String m_credentialsName = "";

    @Widget(title = "Username variable",
        description = "The string flow variable that contains the username of the new credentials variable.")
    @ChoicesProvider(StringFlowVariablesProvider.class)
    @Persist(configKey = "usernameVarName")
    @ValueReference(UsernameVariableReference.class)
    @ValueProvider(GuessUsername.class)
    String m_usernameVariable = "";

    interface UsernameVariableReference extends ParameterReference<String> {
    }

    static final class GuessUsername extends AbstractGuessFirstStringFlowVariableValueProvider {
        GuessUsername() {
            super(UsernameVariableReference.class);
        }
    }

    @Widget(title = "Password variable",
        description = "The string flow variable that contains the password of the new credentials variable.")
    @ChoicesProvider(StringFlowVariablesProvider.class)
    @Persist(configKey = "passwordVarName")
    @ValueReference(PasswordVariableReference.class)
    @ValueProvider(GuessPassword.class)
    String m_passwordVariable = "";

    interface PasswordVariableReference extends ParameterReference<String> {
    }

    static final class GuessPassword extends AbstractGuessFirstStringFlowVariableValueProvider {
        GuessPassword() {
            super(PasswordVariableReference.class);
        }
    }

    @Widget(title = "Second factor variable",
        description = "The string flow variable that contains the second factor of the new credentials variable. "
            + "Select 'None' if not needed.")
    @ChoicesProvider(StringFlowVariablesProvider.class)
    @Persistor(SecondFactorVariablePersistor.class)
    StringOrEnum<NoneChoice> m_secondFactorVariable = new StringOrEnum<>(NoneChoice.NONE);

    static final class SecondFactorVariablePersistor extends NoneAsStringPersistor {
        SecondFactorVariablePersistor() {
            super("secondFactorVarName");
        }

    }

    /**
     * Custom persistor for the second factor variable that handles migration from legacy string format. Converts the
     * legacy "<none>" string to NoneChoice.NONE enum and other values to string choices.
     */
    static abstract class NoneAsStringPersistor implements NodeParametersPersistor<StringOrEnum<NoneChoice>> {

        String m_configKey;

        protected NoneAsStringPersistor(final String configKey) {
            m_configKey = configKey;
        }

        @Override
        public StringOrEnum<NoneChoice> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var legacyValue = settings.getString(m_configKey, VariableToCredentialsNodeModel.NONE_ELEMENT);
            if (VariableToCredentialsNodeModel.NONE_ELEMENT.equals(legacyValue)) {
                return new StringOrEnum<>(NoneChoice.NONE);
            } else {
                return new StringOrEnum<>(legacyValue);
            }
        }

        @Override
        public void save(final StringOrEnum<NoneChoice> obj, final NodeSettingsWO settings) {
            if (obj.getEnumChoice().isPresent()) {
                settings.addString(m_configKey, VariableToCredentialsNodeModel.NONE_ELEMENT);
            } else {
                settings.addString(m_configKey, obj.getStringChoice());
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{m_configKey}};
        }
    }

    /*
     * Set first (with respect to sorting by name) string flow variable as guessed value
     */
    static abstract class AbstractGuessFirstStringFlowVariableValueProvider extends AutoGuessValueProvider<String> {

        protected AbstractGuessFirstStringFlowVariableValueProvider(
            final Class<? extends ParameterReference<String>> selfReference) {
            super(selfReference);
        }

        @Override
        protected boolean isEmpty(final String value) {
            return value == null || value.isEmpty();
        }

        @Override
        protected String autoGuessValue(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return StringFlowVariablesProvider.flowVariableStream(parametersInput).findFirst()
                .orElseThrow(StateComputationFailureException::new).getName();
        }

    }
}
