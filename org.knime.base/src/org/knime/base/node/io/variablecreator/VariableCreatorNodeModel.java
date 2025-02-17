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
 *   Feb 17, 2025 (david): created
 */
package org.knime.base.node.io.variablecreator;

import java.util.Arrays;
import java.util.function.Predicate;

import org.knime.base.node.io.variablecreator.VariableCreatorNodeSettings.FlowVariableType;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * Node model for the new webUI version of the variable creator node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class VariableCreatorNodeModel extends WebUINodeModel<VariableCreatorNodeSettings> {

    /**
     * @param config the configuration of the node
     */
    public VariableCreatorNodeModel(final WebUINodeConfiguration config) {
        super(config, VariableCreatorNodeSettings.class);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs,
        final VariableCreatorNodeSettings modelSettings) throws InvalidSettingsException {

        pushFlowVariables(modelSettings);

        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec,
        final VariableCreatorNodeSettings modelSettings) throws Exception {

        pushFlowVariables(modelSettings);

        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    void pushFlowVariables(final VariableCreatorNodeSettings modelSettings) {
        var allSupportedFlowVariableTypes = Arrays.stream(VariableCreatorNodeSettings.FlowVariableType.values()) //
            .map(FlowVariableType::getKnimeVariableType) //
            .toArray(VariableType<?>[]::new);
        var preExistingFlowVariableNames = getAvailableInputFlowVariables(allSupportedFlowVariableTypes).keySet();

        if (modelSettings.m_newFlowVariables.length == 0) {
            setWarningMessage("No flow variables created. The output will be empty.");
        }

        for (var i = modelSettings.m_newFlowVariables.length - 1; i >= 0; --i) {
            var newFlowVariable = modelSettings.m_newFlowVariables[i];

            if (!newFlowVariable.m_type.canConvert(newFlowVariable.m_value)) {
                // this shouldn't happen because we already validated this in validateSettings
                throw new IllegalArgumentException("Implementation error: the value '" + newFlowVariable.m_value
                    + "' cannot be converted to " + newFlowVariable.m_type);
            }

            // should also warn if we are overwriting existing flow variables
            var flowVariableOverwritten = preExistingFlowVariableNames.contains(newFlowVariable.m_name);
            if (flowVariableOverwritten) {
                setWarningMessage("The flow variable '" + newFlowVariable.m_name + "' is being overwritten.");
            }

            // only for doubles: if it's a double, we should warn if it's too big or too small so it parses to
            // ±infinity. But not if they actually typed "Infinity" or "-Infinity".
            if (newFlowVariable.m_type == VariableCreatorNodeSettings.FlowVariableType.DOUBLE) {
                var value = Double.parseDouble(newFlowVariable.m_value);
                if (Double.isInfinite(value) && !newFlowVariable.m_value.endsWith("Infinity")) {
                    setWarningMessage("The value '" + newFlowVariable.m_value
                        + "' is too large or too small to be represented as a double.");
                }
            }

            newFlowVariable.m_type.convertAndPush(newFlowVariable.m_value,
                (type, value) -> pushFlowVariable(newFlowVariable.m_name, type, value));
        }
    }

    @Override
    protected void validateSettings(final VariableCreatorNodeSettings settings) throws InvalidSettingsException {
        var anyVariableNamesAreBlank = Arrays.stream(settings.m_newFlowVariables) //
            .anyMatch(variable -> variable.m_name.isBlank());
        if (anyVariableNamesAreBlank) {
            throw new InvalidSettingsException("Variable names must not be blank.");
        }

        Predicate<String> isInListMultipleTimes = name -> Arrays.stream(settings.m_newFlowVariables) //
            .map(variable -> variable.m_name) //
            .filter(name::equals) //
            .count() > 1;

        var firstDuplicateVariableName = Arrays.stream(settings.m_newFlowVariables) //
            .map(variable -> variable.m_name) //
            .distinct() //
            .filter(isInListMultipleTimes) //
            .findFirst();
        if (firstDuplicateVariableName.isPresent()) {
            throw new InvalidSettingsException(
                "The variable name '" + firstDuplicateVariableName.get() + "' is used more than once.");
        }

        var firstVariableNameThatCouldNotBeParsed = Arrays.stream(settings.m_newFlowVariables) //
            .filter(variable -> !variable.m_type.canConvert(variable.m_value)) //
            .findFirst();
        if (firstVariableNameThatCouldNotBeParsed.isPresent()) {
            var variable = firstVariableNameThatCouldNotBeParsed.get();
            throw new InvalidSettingsException("The value of the variable '" + variable.m_name
                + "' could not be parsed as a " + variable.m_type.niceName() + ".");
        }
    }
}
