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
 *   Jan 15, 2025 (Martin Sillye): created
 */
package org.knime.time.node.manipulate.modifytime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.knime.NodeModelTestRunnerUtil;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.time.node.manipulate.modifytime.ModifyTimeNodeSettings.BehaviourType;
import org.knime.time.util.ReplaceOrAppend;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ModifyTimeNodeModel2Test {

    private static final String INPUT_COLUMN = "test_input";

    private static final NodeModelTestRunnerUtil RUNNER = new NodeModelTestRunnerUtil(INPUT_COLUMN, "ModifyTimeNode",
        ModifyTimeNodeSettings.class, ModifyTimeNodeFactory2.class);

    @Test
    void testAppendDateWithoutTimeZone() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeNodeSettings();
        settings.m_behaviourType = ModifyTimeNodeSettings.BehaviourType.APPEND;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_localTime = LocalTime.of(21, 43, 57);
        settings.m_timeZone = null;

        var testResult =
            RUNNER.setupAndExecuteWorkflow(settings, LocalDateCellFactory.create(LocalDate.of(2001, 1, 2)));

        assertTrue(testResult.firstCell() instanceof LocalDateTimeValue,
            "Expected LocalDateTimeValue, got " + testResult.firstCell().getClass());

        var value = ((LocalDateTimeValue)testResult.firstCell()).getLocalDateTime();

        assertEquals(LocalDateTime.of(2001, 1, 2, 21, 43, 57), value, "Expected correct output value");
    }

    @Test
    void testAppendDateWithTimeZone() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeNodeSettings();
        settings.m_behaviourType = ModifyTimeNodeSettings.BehaviourType.APPEND;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_localTime = LocalTime.of(21, 43, 57);
        settings.m_timeZone = ZoneId.of("Europe/Berlin");

        var testResult =
            RUNNER.setupAndExecuteWorkflow(settings, LocalDateCellFactory.create(LocalDate.of(2001, 1, 2)));

        assertTrue(testResult.firstCell() instanceof ZonedDateTimeValue,
            "Expected LocalDateTimeValue, got " + testResult.firstCell().getClass());

        var value = ((ZonedDateTimeValue)testResult.firstCell()).getZonedDateTime().toLocalDateTime();

        assertEquals(LocalDateTime.of(2001, 1, 2, 21, 43, 57), value, "Expected correct output value");
    }

    @Test
    void testRemoveOnLocalDateTime() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeNodeSettings();
        settings.m_behaviourType = ModifyTimeNodeSettings.BehaviourType.REMOVE;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});

        var testResult = RUNNER.setupAndExecuteWorkflow(settings,
            LocalDateTimeCellFactory.create(LocalDateTime.of(2001, 3, 4, 12, 34, 56)));

        assertTrue(testResult.firstCell() instanceof LocalDateValue,
            "Expected LocalTimeCell, got " + testResult.firstCell().getClass());

        var value = ((LocalDateValue)testResult.firstCell()).getLocalDate();

        assertEquals(LocalDate.of(2001, 3, 4), value, "Expected correct output");
    }

    @Test
    void testRemoveOnZonedDateTime() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeNodeSettings();
        settings.m_behaviourType = ModifyTimeNodeSettings.BehaviourType.REMOVE;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});

        var testResult = RUNNER.setupAndExecuteWorkflow(settings,
            ZonedDateTimeCellFactory.create(ZonedDateTime.of(2001, 3, 4, 12, 34, 56, 0, ZoneId.of("Europe/Berlin"))));

        assertTrue(testResult.firstCell() instanceof LocalDateValue,
            "Expected LocalTimeCell, got " + testResult.firstCell().getClass());

        var value = ((LocalDateValue)testResult.firstCell()).getLocalDate();

        assertEquals(LocalDate.of(2001, 3, 4), value, "Expected correct output");
    }

    @Test
    void testChangeOnZonedDateTime() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeNodeSettings();
        settings.m_behaviourType = ModifyTimeNodeSettings.BehaviourType.CHANGE;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_localTime = LocalTime.of(21, 43, 57);

        var testResult = RUNNER.setupAndExecuteWorkflow(settings,
            ZonedDateTimeCellFactory.create(ZonedDateTime.of(2001, 3, 4, 12, 34, 56, 0, ZoneId.of("Europe/Berlin"))));

        assertTrue(testResult.firstCell() instanceof ZonedDateTimeValue,
            "Expected ZonedDateTimeValue, got " + testResult.firstCell().getClass());

        var value = ((ZonedDateTimeValue)testResult.firstCell()).getZonedDateTime().toLocalDateTime();

        assertEquals(LocalDateTime.of(2001, 3, 4, 21, 43, 57), value, "Expected correct output");
    }

    @Test
    void testChangeOnLocalDateTime() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeNodeSettings();
        settings.m_behaviourType = ModifyTimeNodeSettings.BehaviourType.CHANGE;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_localTime = LocalTime.of(21, 43, 57);

        var testResult = RUNNER.setupAndExecuteWorkflow(settings,
            LocalDateTimeCellFactory.create(LocalDateTime.of(2001, 3, 4, 12, 34, 56)));

        assertTrue(testResult.firstCell() instanceof LocalDateTimeValue,
            "Expected LocalDateTimeValue, got " + testResult.firstCell().getClass());

        var value = ((LocalDateTimeValue)testResult.firstCell()).getLocalDateTime();

        assertEquals(LocalDateTime.of(2001, 3, 4, 21, 43, 57), value, "Expected correct output");
    }

    @Test
    void testAppendThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.APPEND;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertTrue(testSetup.firstCell().isMissing(), "Output cell should be missing");
    }

    @Test
    void testChangeThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.CHANGE;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertTrue(testSetup.firstCell().isMissing(), "Output cell should be missing");
    }

    @Test
    void testRemoveThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.REMOVE;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertTrue(testSetup.firstCell().isMissing(), "Output cell should be missing");
    }

    @Test
    void testAppendThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.APPEND;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, null);

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertNull(testSetup.firstCell(), "Output cell should not exists");
        assertEquals(0, testSetup.outputTable().size(), "Ouptput table should be empty");
    }

    @Test
    void testChangeThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.CHANGE;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, null);

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertNull(testSetup.firstCell(), "Output cell should not exists");
        assertEquals(0, testSetup.outputTable().size(), "Ouptput table should be empty");
    }

    @Test
    void testRemoveThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.REMOVE;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, null);

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertNull(testSetup.firstCell(), "Output cell should not exists");
        assertEquals(0, testSetup.outputTable().size(), "Ouptput table should be empty");
    }
}
