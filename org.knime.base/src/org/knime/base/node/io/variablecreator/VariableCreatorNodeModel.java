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
 *   06.04.2021 (jl): created
 */
package org.knime.base.node.io.variablecreator;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.base.node.io.variablecreator.VariableCreatorNodeSettings.FlowVariableType;
import org.knime.base.node.io.variablecreator.VariableCreatorNodeSettings.NewFlowVariableSettings;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * The {@link NodeModel} for the “Create Variables” node
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class VariableCreatorNodeModel extends WebUINodeModel<VariableCreatorNodeSettings> {

    // TODO(UIEXT-2611): this node has a lot of potential warnings, which should be displayed in the dialogue

    /** Message set if a variable overrides an external variable. */
    static final String MSG_NAME_EXTERNAL_OVERRIDES = "Name overrides upstream variable";

    /** Message set if a variable has the same name as an external variable. */
    static final String MSG_NAME_EXTERNAL_CONFLICT = "Upstream name conflict; may lead to unexpected behavior";

    static final String SETTINGS_MODEL_CONFIG_NAME = "variableCreationTable";

    /**
     * Create the node model for the "Variable Creator" node
     */
    VariableCreatorNodeModel(final WebUINodeConfiguration config) {
        super(config, VariableCreatorNodeSettings.class);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecsWebUINodeModel,
        final VariableCreatorNodeSettings settings) throws InvalidSettingsException {
        pushVariablesAndSetWarning(settings);
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec,
        final VariableCreatorNodeSettings settings) throws Exception {
        pushVariablesAndSetWarning(settings);
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    private void pushVariablesAndSetWarning(final VariableCreatorNodeSettings settings) {
        if (settings.m_newFlowVariables.length == 0) {
            setWarningMessage("No new variables defined");
        } else {
            getNameWarningMessage(settings).ifPresent(this::setWarningMessage);
            pushVariables(settings);
        }
    }

    /**
     * Generate the warning message for this node. Currently, only name overrides and same names are respected.
     *
     * @return the warning message. Empty optional if there is no warning.
     */
    private Optional<String> getNameWarningMessage(final VariableCreatorNodeSettings settings) {
        var availableInputFlowVars = getAvailableInputFlowVariables(FlowVariableType.knimeTypes());
        // the name cannot be empty in this case
        var foundOverride = false;
        var foundConflict = false;
        for (int i = settings.m_newFlowVariables.length - 1; i >= 0 && (!foundOverride || !foundConflict); i--) {
            var flowVarName = settings.m_newFlowVariables[i].m_name;

            final Pair<Boolean, Optional<String>> result =
                checkVariableNameExternal(availableInputFlowVars, i, flowVarName, settings.m_newFlowVariables);
            final Optional<String> hintMsg = result.getSecond();

            if (!result.getFirst()) { // NOSONAR the boolean isn't null
                throw new IllegalStateException(
                    "Name should be valid!: " + result.getSecond().orElse("(Unknown Error)"));
            }

            if (hintMsg.isPresent()) {
                switch (hintMsg.get()) {
                    case MSG_NAME_EXTERNAL_CONFLICT:
                        foundConflict = true;
                        break;
                    case MSG_NAME_EXTERNAL_OVERRIDES:
                        foundOverride = true;
                        break;
                    default:
                        // we are ignoring other hints
                        break;
                }
            }
        }
        return Optional.ofNullable(buildWarningMessage(foundOverride, foundConflict));
    }

    private static Pair<Boolean, Optional<String>> checkVariableNameExternal(
        final Map<String, FlowVariable> externalVariables, final int row, final String name,
        final NewFlowVariableSettings[] newFlowVariableSettings) {
        if (name.trim().isEmpty()) {
            return new Pair<>(Boolean.FALSE, Optional.of("Name is empty"));
        } else {
            final var externalVariable = externalVariables.get(name);
            if (externalVariable != null) {
                // Will only get overridden if the name and the type are the same
                if (externalVariable.getVariableType()
                    .equals(newFlowVariableSettings[row].m_type.getKnimeVariableType())) {
                    return new Pair<>(Boolean.TRUE, Optional.of(MSG_NAME_EXTERNAL_OVERRIDES));
                } else {
                    return new Pair<>(Boolean.TRUE, Optional.of(MSG_NAME_EXTERNAL_CONFLICT));
                }
            }
        }
        return new Pair<>(Boolean.TRUE, Optional.empty());
    }

    /**
     * Builds a warning message from this node depending on whether errors indicated by the parameters were found.
     *
     * @param foundOverride whether a variable override was found
     * @param foundConflict whether a variable with the same name was found
     * @return the warning message or <code>null</code> if there is no warning.
     */
    private static String buildWarningMessage(final boolean foundOverride, final boolean foundConflict) {
        if (!foundConflict && !foundOverride) {
            return null;
        }

        final StringBuilder result = new StringBuilder("Some defined variables ");

        if (foundOverride) {
            result.append("override");
        }

        if (foundConflict && foundOverride) {
            result.append(" or ");
        }

        if (foundConflict) {
            result.append("have the same name as");
        }

        result.append(" upstream variables.");

        return result.toString();
    }

    /**
     * Push a variable from the <code>m_table</code> table.
     *
     * @param row the row of the variable to push
     */
    private void pushVariables(final VariableCreatorNodeSettings settings) {
        // it is a stack so we have to push  the variables in opposite order
        for (int row = settings.m_newFlowVariables.length - 1; row >= 0; row--) {
            var type = settings.m_newFlowVariables[row].m_type;
            var name = settings.m_newFlowVariables[row].m_name;
            var valueAsString = settings.m_newFlowVariables[row].m_value;

            var parseResult =
                type.convertAndPush(valueAsString, (knimeType, value) -> pushFlowVariable(name, knimeType, value));

            if (parseResult.parsedValue().isEmpty()) {
                throw new IllegalStateException("Variable should be valid since we already checked in validate: "
                    + parseResult.errorOrWarningMessage().orElse("(Unknown Error)"));
            }

            if (parseResult.errorOrWarningMessage().isPresent()) {
                setWarningMessage("Error in variable " + name + ": " + parseResult.errorOrWarningMessage().orElseThrow(
                    () -> new IllegalStateException("Error message should be present if value could not be parsed")));
            }
        }
    }

    @Override
    protected void validateSettings(final VariableCreatorNodeSettings settings) throws InvalidSettingsException {
        Set<String> usedNames = new HashSet<>();
        for (int i = 0; i < settings.m_newFlowVariables.length; i++) {
            var type = settings.m_newFlowVariables[i].m_type;
            var name = settings.m_newFlowVariables[i].m_name;
            var valueString = settings.m_newFlowVariables[i].m_value;

            CheckUtils.checkSetting(!name.isEmpty(), "Please use a (non-empty) name for variable %d!", i + 1);
            CheckUtils.checkSetting(!usedNames.contains(name),
                "Please use a name that is not already used by another variable (%s) for variable %d!", name, i + 1);
            usedNames.add(name);

            final Pair<Optional<Object>, Optional<String>> checked = checkVariableValueString(type, valueString);
            CheckUtils.checkSetting(checked.getFirst().isPresent(), "Please check variable %d (%s) for errors: %s",
                i + 1, name, checked.getSecond().orElse("(Unknown error)"));
        }
    }

    private static Pair<Optional<Object>, Optional<String>> checkVariableValueString(final FlowVariableType type,
        final String valueString) {

        var parseResult = type.tryParse(valueString);
        Optional<Object> parsedValue = parseResult.parsedValue().map(Object.class::cast);
        return new Pair<>(parsedValue, parseResult.errorOrWarningMessage());
    }
}
