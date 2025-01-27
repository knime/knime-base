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
 *   Jan 27, 2025 (david): created
 */
package org.knime.time.node.manipulate.modifydate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.knime.InputTableNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;
import org.knime.time.util.ReplaceOrAppend;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings({"restriction", "static-method"})
final class ModifyDateNodeModel2Test {

    private final static String INPUT_COLUMN_NAME = "input_col";

    @Test
    void testAppendDateWithoutTimeZone() throws InvalidSettingsException, IOException {
        var settings = new ModifyDateNodeSettings();
        settings.m_behaviourType = ModifyDateNodeSettings.BehaviourType.APPEND;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN_NAME});
        settings.m_localDate = LocalDate.of(2021, 1, 2);
        settings.m_timeZone = null;

        var testResult = setupAndExecuteWorkflow(settings, LocalTimeCellFactory.create(LocalTime.of(12, 34, 56)));

        assertTrue(testResult.firstOutputCell instanceof LocalDateTimeValue,
            "Expected LocalDateTimeValue, got " + testResult.firstOutputCell.getClass());

        var value = ((LocalDateTimeValue)testResult.firstOutputCell).getLocalDateTime();

        assertEquals(LocalDateTime.of(2021, 1, 2, 12, 34, 56), value, "Expected correct output value");
    }

    @Test
    void testAppendDateWithTimeZone() throws InvalidSettingsException, IOException {
        var settings = new ModifyDateNodeSettings();
        settings.m_behaviourType = ModifyDateNodeSettings.BehaviourType.APPEND;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN_NAME});
        settings.m_localDate = LocalDate.of(2021, 1, 2);
        settings.m_timeZone = ZoneId.of("Europe/Berlin");

        var testResult = setupAndExecuteWorkflow(settings, LocalTimeCellFactory.create(LocalTime.of(12, 34, 56)));

        assertTrue(testResult.firstOutputCell instanceof ZonedDateTimeValue,
            "Expected LocalDateTimeValue, got " + testResult.firstOutputCell.getClass());

        var value = ((ZonedDateTimeValue)testResult.firstOutputCell).getZonedDateTime().toLocalDateTime();

        assertEquals(LocalDateTime.of(2021, 1, 2, 12, 34, 56), value, "Expected correct output value");
    }

    @Test
    void testRemoveOnLocalDateTime() throws InvalidSettingsException, IOException {
        var settings = new ModifyDateNodeSettings();
        settings.m_behaviourType = ModifyDateNodeSettings.BehaviourType.REMOVE;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN_NAME});

        var testResult = setupAndExecuteWorkflow(settings,
            LocalDateTimeCellFactory.create(LocalDateTime.of(2001, 3, 4, 12, 34, 56)));

        assertTrue(testResult.firstOutputCell instanceof LocalTimeValue,
            "Expected LocalTimeCell, got " + testResult.firstOutputCell.getClass());

        var value = ((LocalTimeValue)testResult.firstOutputCell).getLocalTime();

        assertEquals(LocalTime.of(12, 34, 56), value, "Expected correct output");
    }

    @Test
    void testRemoveOnZonedDateTime() throws InvalidSettingsException, IOException {
        var settings = new ModifyDateNodeSettings();
        settings.m_behaviourType = ModifyDateNodeSettings.BehaviourType.REMOVE;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN_NAME});

        var testResult = setupAndExecuteWorkflow(settings,
            ZonedDateTimeCellFactory.create(ZonedDateTime.of(2001, 3, 4, 12, 34, 56, 0, ZoneId.of("Europe/Berlin"))));

        assertTrue(testResult.firstOutputCell instanceof LocalTimeValue,
            "Expected LocalTimeCell, got " + testResult.firstOutputCell.getClass());

        var value = ((LocalTimeValue)testResult.firstOutputCell).getLocalTime();

        assertEquals(LocalTime.of(12, 34, 56), value, "Expected correct output");
    }

    @Test
    void testChangeOnZonedDateTime() throws InvalidSettingsException, IOException {
        var settings = new ModifyDateNodeSettings();
        settings.m_behaviourType = ModifyDateNodeSettings.BehaviourType.CHANGE;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN_NAME});
        settings.m_localDate = LocalDate.of(2021, 1, 2);

        var testResult = setupAndExecuteWorkflow(settings,
            ZonedDateTimeCellFactory.create(ZonedDateTime.of(2001, 3, 4, 12, 34, 56, 0, ZoneId.of("Europe/Berlin"))));

        assertTrue(testResult.firstOutputCell instanceof ZonedDateTimeValue,
            "Expected ZonedDateTimeValue, got " + testResult.firstOutputCell.getClass());

        var value = ((ZonedDateTimeValue)testResult.firstOutputCell).getZonedDateTime().toLocalDateTime();

        assertEquals(LocalDateTime.of(2021, 1, 2, 12, 34, 56), value, "Expected correct output");
    }

    @Test
    void testChangeOnLocalDateTime() throws InvalidSettingsException, IOException {
        var settings = new ModifyDateNodeSettings();
        settings.m_behaviourType = ModifyDateNodeSettings.BehaviourType.CHANGE;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN_NAME});
        settings.m_localDate = LocalDate.of(2021, 1, 2);

        var testResult = setupAndExecuteWorkflow(settings,
            LocalDateTimeCellFactory.create(LocalDateTime.of(2001, 3, 4, 12, 34, 56)));

        assertTrue(testResult.firstOutputCell instanceof LocalDateTimeValue,
            "Expected LocalDateTimeValue, got " + testResult.firstOutputCell.getClass());

        var value = ((LocalDateTimeValue)testResult.firstOutputCell).getLocalDateTime();

        assertEquals(LocalDateTime.of(2021, 1, 2, 12, 34, 56), value, "Expected correct output");
    }

    private static record TestSetup(BufferedDataTable outputTable, DataCell firstOutputCell) {
    }

    private static TestSetup setupAndExecuteWorkflow(final ModifyDateNodeSettings settings, final DataCell cellToAdd)
        throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new ModifyDateNodeFactory2());

        // set the settings
        final var nodeSettings = new NodeSettings("ModifyDateNode");
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(ModifyDateNodeSettings.class, settings, modelSettings);

        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // populate the input table
        var inputTableSpec = new TableTestUtil.SpecBuilder() //
            .addColumn(INPUT_COLUMN_NAME, cellToAdd.getType()) //
            .build();
        var inputTable = new TableTestUtil.TableBuilder(inputTableSpec) //
            .addRow(cellToAdd) //
            .build();
        var tableSupplierNode =
            WorkflowManagerUtil.createAndAddNode(workflowManager, new InputTableNode.InputDataNodeFactory(inputTable));

        // link the nodes
        workflowManager.addConnection(tableSupplierNode.getID(), 1, node.getID(), 1);

        workflowManager.executeAllAndWaitUntilDone();

        var outputTable = (BufferedDataTable)node.getOutPort(1).getPortObject();

        DataCell firstCell;
        try (var it = outputTable.iterator()) {
            if (settings.m_appendOrReplace == ReplaceOrAppend.REPLACE) {
                firstCell = it.next().getCell(0);
            } else {
                firstCell = it.next().getCell(1);
            }
        }

        return new TestSetup(outputTable, firstCell);
    }
}
