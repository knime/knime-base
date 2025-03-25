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
package org.knime.time.node.manipulate.modifytimezone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.knime.NodeModelTestRunnerUtil;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.time.node.manipulate.modifytimezone.ModifyTimeZoneNodeSettings.BehaviourType;
import org.knime.time.util.ReplaceOrAppend;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ModifyTimeZoneNodeModel2Test {
    private static final String INPUT_COLUMN = "test_input";

    private static final NodeModelTestRunnerUtil RUNNER = new NodeModelTestRunnerUtil(INPUT_COLUMN,
        "ModifyTimeZoneNode", ModifyTimeZoneNodeSettings.class, ModifyTimeZoneNodeFactory2.class);

    @Test
    void testSetOnLocalDateTime() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeZoneNodeSettings();
        settings.m_timeZone = ZoneId.of("Asia/Hong_Kong");
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_behaviourType = ModifyTimeZoneNodeSettings.BehaviourType.SET;

        var testResult = RUNNER.setupAndExecuteWorkflow(settings,
            LocalDateTimeCellFactory.create(LocalDateTime.of(2025, 1, 2, 3, 4, 5)));

        assertTrue(testResult.firstCell() instanceof ZonedDateTimeValue,
            "Expected ZonedDateTimeValue but got " + testResult.firstCell().getClass().getName());

        var value = ((ZonedDateTimeValue)testResult.firstCell()).getZonedDateTime();

        assertEquals(ZonedDateTime.of(2025, 1, 2, 3, 4, 5, 0, ZoneId.of("Asia/Hong_Kong")), value,
            "Expected correct value");
    }

    @Test
    void testSetOnZonedDateTime() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeZoneNodeSettings();
        settings.m_timeZone = ZoneId.of("Asia/Hong_Kong");
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_behaviourType = ModifyTimeZoneNodeSettings.BehaviourType.SET;

        var testResult = RUNNER.setupAndExecuteWorkflow(settings,
            ZonedDateTimeCellFactory.create(ZonedDateTime.of(2025, 1, 2, 3, 4, 5, 0, ZoneId.of("UTC"))));

        assertTrue(testResult.firstCell() instanceof ZonedDateTimeValue,
            "Expected ZonedDateTimeValue but got " + testResult.firstCell().getClass().getName());

        var value = ((ZonedDateTimeValue)testResult.firstCell()).getZonedDateTime();

        assertEquals(ZonedDateTime.of(2025, 1, 2, 3, 4, 5, 0, ZoneId.of("Asia/Hong_Kong")), value,
            "Expected correct value");
    }

    @Test
    void testRemoveOnZonedDateTime() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeZoneNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_behaviourType = ModifyTimeZoneNodeSettings.BehaviourType.REMOVE;

        var testResult = RUNNER.setupAndExecuteWorkflow(settings,
            ZonedDateTimeCellFactory.create(ZonedDateTime.of(2025, 1, 2, 3, 4, 5, 0, ZoneId.of("UTC"))));

        assertTrue(testResult.firstCell() instanceof LocalDateTimeValue,
            "Expected LocalDateTimeValue but got " + testResult.firstCell().getClass().getName());

        var value = ((LocalDateTimeValue)testResult.firstCell()).getLocalDateTime();

        assertEquals(LocalDateTime.of(2025, 1, 2, 3, 4, 5), value, "Expected correct value");
    }

    @Test
    void testShiftOnZonedDateTime() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeZoneNodeSettings();
        settings.m_timeZone = ZoneId.of("GMT+3");
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_behaviourType = ModifyTimeZoneNodeSettings.BehaviourType.SHIFT;

        var testResult = RUNNER.setupAndExecuteWorkflow(settings,
            ZonedDateTimeCellFactory.create(ZonedDateTime.of(2025, 1, 2, 3, 4, 5, 0, ZoneId.of("GMT"))));

        assertTrue(testResult.firstCell() instanceof ZonedDateTimeValue,
            "Expected ZonedDateTimeValue but got " + testResult.firstCell().getClass().getName());

        var value = ((ZonedDateTimeValue)testResult.firstCell()).getZonedDateTime();

        assertEquals(ZonedDateTime.of(2025, 1, 2, 6, 4, 5, 0, ZoneId.of("GMT+3")), value, "Expected correct value");
    }

    @Test
    void testShiftOnZonedDateTimeThatExceedsValidRange() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeZoneNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_behaviourType = ModifyTimeZoneNodeSettings.BehaviourType.SHIFT;

        // first we check with the maximum date
        settings.m_timeZone = ZoneId.of("GMT+3");
        var testResult = RUNNER.setupAndExecuteWorkflow(settings,
            ZonedDateTimeCellFactory.create(ZonedDateTime.of(LocalDateTime.MAX.minusHours(1), ZoneId.of("GMT"))));

        assertTrue(testResult.firstCell().isMissing(), "Expected missing cell when shifting above maximum date");

        // then also check with the minimum date
        settings.m_timeZone = ZoneId.of("GMT-3");
        testResult = RUNNER.setupAndExecuteWorkflow(settings,
            ZonedDateTimeCellFactory.create(ZonedDateTime.of(LocalDateTime.MIN.plusHours(1), ZoneId.of("GMT"))));

        assertTrue(testResult.firstCell().isMissing(), "Expected missing cell when shifting below minimum date");
    }

    @Test
    void testSetThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeZoneNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.SET;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertTrue(testSetup.firstCell().isMissing(), "Output cell should be missing");
    }

    @Test
    void testShiftThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeZoneNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.SHIFT;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertTrue(testSetup.firstCell().isMissing(), "Output cell should be missing");
    }

    @Test
    void testRemoveThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeZoneNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.REMOVE;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertTrue(testSetup.firstCell().isMissing(), "Output cell should be missing");
    }

    @Test
    void testSetThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeZoneNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.SET;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, null);

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertNull(testSetup.firstCell(), "Output cell should not exists");
        assertEquals(0, testSetup.outputTable().size(), "Ouptput table should be empty");
    }

    @Test
    void testShiftThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeZoneNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.SHIFT;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, null);

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertNull(testSetup.firstCell(), "Output cell should not exists");
        assertEquals(0, testSetup.outputTable().size(), "Ouptput table should be empty");
    }

    @Test
    void testRemoveThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        var settings = new ModifyTimeZoneNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_behaviourType = BehaviourType.REMOVE;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, null);

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertNull(testSetup.firstCell(), "Output cell should not exists");
        assertEquals(0, testSetup.outputTable().size(), "Ouptput table should be empty");
    }
}
