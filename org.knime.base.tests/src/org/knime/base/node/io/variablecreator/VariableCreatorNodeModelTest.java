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
 *   Feb 20, 2025 (david): created
 */
package org.knime.base.node.io.variablecreator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.base.node.io.variablecreator.VariableCreatorNodeSettings.FlowVariableType;
import org.knime.base.node.io.variablecreator.VariableCreatorNodeSettings.NewFlowVariableSettings;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class VariableCreatorNodeModelTest {

    VariableCreatorNodeModel m_model;

    VariableCreatorNodeSettings m_settings;

    WorkflowManager m_wfm;

    NativeNodeContainer m_nodeContainer;

    private final static String NODE_NAME = "VariableCreator";

    private final static Class<? extends DefaultNodeSettings> SETTINGS_CLASS = VariableCreatorNodeSettings.class;

    @BeforeEach
    void setup() throws IOException {
        m_model = new VariableCreatorNodeModel(VariableCreatorNodeFactory.CONFIGURATION);
        m_settings = new VariableCreatorNodeSettings();
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();

        m_nodeContainer = WorkflowManagerUtil.createAndAddNode(m_wfm, new VariableCreatorNodeFactory());
        NodeContext.pushContext(m_nodeContainer);

        m_model = (VariableCreatorNodeModel)m_nodeContainer.getNodeModel();
    }

    void applySettings() throws InvalidSettingsException {
        final var nodeSettings = new NodeSettings(NODE_NAME);
        m_wfm.saveNodeSettings(m_nodeContainer.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(SETTINGS_CLASS, m_settings, modelSettings);
        m_wfm.loadNodeSettings(m_nodeContainer.getID(), nodeSettings);
    }

    @Test
    void testValidateFailsWithMultipleVariablesOfSameName() {
        m_settings.m_newFlowVariables = new NewFlowVariableSettings[]{ //
            new NewFlowVariableSettings("var1", FlowVariableType.INTEGER, "1"), //
            new NewFlowVariableSettings("var1", FlowVariableType.LONG, "2") //
        };

        var thrown = assertThrows(InvalidSettingsException.class, () -> {
            m_model.validateSettings(m_settings);
        }, "Expected validation error on duplicate flowvar names");

        assertTrue(thrown.getMessage().contains("var1"), "Expected error message to contain duplicate variable name");
    }

    @Test
    void testValidateFailsWithEmptyVariableName() {
        m_settings.m_newFlowVariables = new NewFlowVariableSettings[]{ //
            new NewFlowVariableSettings("", FlowVariableType.INTEGER, "1"), //
        };

        assertThrows(InvalidSettingsException.class, () -> {
            m_model.validateSettings(m_settings);
        }, "Expected validation error on empty flowvar name");
    }

    @Test
    void testValidateFailsWithInvalidValue() {
        m_settings.m_newFlowVariables = new NewFlowVariableSettings[]{ //
            new NewFlowVariableSettings("var1", FlowVariableType.INTEGER, "not a number"), //
        };

        var thrown = assertThrows(InvalidSettingsException.class, () -> {
            m_model.validateSettings(m_settings);
        }, "Expected validation error on invalid value");

        assertTrue(thrown.getMessage().contains("var1"), "Expected error message to contain variable name");
    }

    @Test
    void testWarnsIfNoVariablesCreated() throws Exception {
        m_settings.m_newFlowVariables = new NewFlowVariableSettings[]{};

        var warningReceived = new AtomicBoolean(false);
        m_model.addWarningListener(m -> warningReceived.set(true));

        m_model.execute(new PortObject[0], m_nodeContainer.createExecutionContext(), m_settings);

        assertTrue(warningReceived.get(), "Expected warning message");
    }

    @Test
    void testWarnsIfOverwritesPreviousFlowVariables() throws Exception {
        m_settings.m_newFlowVariables = new NewFlowVariableSettings[]{ //
            new NewFlowVariableSettings("var1", FlowVariableType.INTEGER, "26"), //
        };

        // add a variable with the same name to the input stack
        m_nodeContainer.getFlowObjectStack().push(new FlowVariable("var1", 0));

        var warningReceived = new AtomicBoolean(false);
        m_model.addWarningListener(m -> warningReceived.set(true));

        m_model.execute(new PortObject[0], m_nodeContainer.createExecutionContext(), m_settings);

        assertTrue(warningReceived.get(), "Expected warning message");
    }

    @Test
    void testWarnsIfValueTooLargeForDouble() throws Exception {
        m_settings.m_newFlowVariables = new NewFlowVariableSettings[]{ //
            new NewFlowVariableSettings("var1", FlowVariableType.DOUBLE, "1e1000"), //
        };

        var warningReceived = new AtomicBoolean(false);
        m_model.addWarningListener(m -> warningReceived.set(true));

        m_model.execute(new PortObject[0], m_nodeContainer.createExecutionContext(), m_settings);

        assertTrue(warningReceived.get(), "Expected warning message");
    }

    @Test
    void testOutputFlowVariablesAreCorrect() throws Exception {
        m_settings.m_newFlowVariables = new NewFlowVariableSettings[]{ //
            new NewFlowVariableSettings("var_i", FlowVariableType.INTEGER, "26"), //
            new NewFlowVariableSettings("var_l", FlowVariableType.LONG, "27"), //
            new NewFlowVariableSettings("var_d", FlowVariableType.DOUBLE, "28"), //
            new NewFlowVariableSettings("var_b", FlowVariableType.BOOLEAN, "true"), //
            new NewFlowVariableSettings("var_s", FlowVariableType.STRING, "hello") //
        };

        var expected = List.of( //
            new FlowVariable("var_i", VariableType.IntType.INSTANCE, 26), //
            new FlowVariable("var_l", VariableType.LongType.INSTANCE, 27L), //
            new FlowVariable("var_d", VariableType.DoubleType.INSTANCE, 28.0), //
            new FlowVariable("var_b", VariableType.BooleanType.INSTANCE, true), //
            new FlowVariable("var_s", VariableType.StringType.INSTANCE, "hello") //
        );

        assertEquals(expected.size(), m_settings.m_newFlowVariables.length,
            "Expected all flow variables to be tested. This is a test implementation error.");

        VariableType<?>[] allSupportedTypes = Arrays.stream(m_settings.m_newFlowVariables) //
            .map(s -> s.m_type.m_knimeVariableType) //
            .toArray(VariableType[]::new);

        assertEquals(allSupportedTypes.length, FlowVariableType.values().length,
            "Expected all supported flow variable types to be tested. This is a test implementation error.");

        var warningReceived = new AtomicBoolean(false);
        m_model.addWarningListener(m -> warningReceived.set(true));

        applySettings();

        m_wfm.executeAllAndWaitUntilDone();

        assertTrue(m_nodeContainer.getNodeContainerState().isExecuted(), "Expected node to be executed");

        var actualFlowVarsAfterExecution =
            m_nodeContainer.getOutgoingFlowObjectStack().getAvailableFlowVariables(allSupportedTypes);

        expected.forEach(e -> {
            assertTrue(actualFlowVarsAfterExecution.containsKey(e.getName()),
                "Expected flow variables map to contain an entry " + e.getName());

            var actual = actualFlowVarsAfterExecution.get(e.getName());

            assertEquals(e, actual, "Expected flow variable values to match");
        });
    }
}
