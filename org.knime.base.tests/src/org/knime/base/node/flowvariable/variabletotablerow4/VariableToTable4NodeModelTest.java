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
 *   Jan 30, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.flowvariable.variabletotablerow4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.variable.FlowVariableFilter;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class VariableToTable4NodeModelTest {

    private static final String NODE_NAME = "VariableToTableRow";

    private static final Class<? extends DefaultNodeSettings> SETTINGS_CLASS = VariableToTable4NodeSettings.class;

    @Test
    void testRowIdIsCorrect() throws InvalidSettingsException, IOException {
        final var settings = new VariableToTable4NodeSettings();
        settings.m_rowName = "TestRow";

        final var output = setupAndExecuteWorkflow(settings, new FlowVariable[0]);

        assertNotNull(output, "Output table shouldn't be null");
        assertEquals(1, output.size(), "Should have 1 row");
        try (final var iter = output.iterator()) {
            final var row = iter.next();
            assertEquals("TestRow", row.getKey().toString());
        }
    }

    @Test
    void testDifferentVariableTypes() throws InvalidSettingsException, IOException {
        final var settings = new VariableToTable4NodeSettings();
        final var variables = new FlowVariable[]{ //
            new FlowVariable("StringVar", "TestValue"), //
            new FlowVariable("IntVar", 5), //
            new FlowVariable("LongVar", LongType.INSTANCE, 5L), //
            new FlowVariable("DoubleVar", 5.4), //
            new FlowVariable("BooleanVar", BooleanType.INSTANCE, true) //
        };
        settings.m_filter =
            new FlowVariableFilter(Arrays.stream(variables).map(FlowVariable::getName).toArray(String[]::new));

        final var output = setupAndExecuteWorkflow(settings, variables);

        assertNotNull(output, "Output table shouldn't be null");
        assertEquals(1, output.size(), "Should have 1 row");
        final var spec = output.getDataTableSpec();
        final var cols = spec.getColumnNames();
        try (final var iter = output.iterator()) {
            final var row = iter.next().iterator();
            for (int i = 0; i < cols.length && row.hasNext(); i++) {
                row.next();
                final var colSpec = spec.getColumnSpec(cols[i]);
                final Optional<FlowVariable> flowVar = Arrays.stream(variables)
                    .filter(variable -> variable.getName().equals(colSpec.getName())).findFirst();
                assertFalse(flowVar.isEmpty(), "Flow variable not added to table");
                final var expectedType = switch (flowVar.get().getName()) {
                    case "StringVar" -> StringCellFactory.TYPE;
                    case "IntVar" -> IntCellFactory.TYPE;
                    case "LongVar" -> LongCellFactory.TYPE;
                    case "DoubleVar" -> DoubleCellFactory.TYPE;
                    case "BooleanVar" -> BooleanCellFactory.TYPE;
                    default -> StringCellFactory.TYPE;
                };
                assertEquals(expectedType, colSpec.getType(), "Mismatching type");
            }
        }
    }

    private static BufferedDataTable setupAndExecuteWorkflow(final VariableToTable4NodeSettings settings,
        final FlowVariable[] variables) throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();
        workflowManager.addWorkflowVariables(false, variables);
        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new VariableToTable4NodeFactory());
        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);

        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(SETTINGS_CLASS, settings, modelSettings);
        workflowManager.loadNodeSettings(node.getID(), nodeSettings);
        // execute and wait...
        workflowManager.executeAllAndWaitUntilDone();
        var outputTable = (BufferedDataTable)node.getOutPort(1).getPortObject();
        return outputTable;
    }
}
