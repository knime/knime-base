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
 *   Dec 4, 2024 (david): created
 */
package org.knime.time.node.convert.stringtodurationperiod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.InputTableNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.time.duration.DurationCell;
import org.knime.core.data.time.period.PeriodCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;
import org.knime.time.node.convert.stringtodurationperiod.StringToDurationPeriodNodeSettings.ActionIfExtractionFails;
import org.knime.time.node.convert.stringtodurationperiod.StringToDurationPeriodNodeSettings.DurationPeriodType;
import org.knime.time.util.ReplaceOrAppend;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class StringToDurationPeriodNodeModelTest {

    private static final String INPUT_COLUMN = "test_input";

    private static final String NODE_NAME = "StringToDurationPeriodNode";

    private record TestCase<I extends TemporalAmount>(I expectedInterval, String iso, String shortForm,
        String longForm) {
    }

    private static final List<TestCase<Duration>> DURATION_TEST_CASES = List.of( //
        new TestCase<Duration>(Duration.ofHours(1), "PT1H", "1H", "1 hour"), //
        new TestCase<Duration>(Duration.ofMinutes(1), "PT1M", "1m", "1 minute"), //
        new TestCase<Duration>(Duration.ofSeconds(1), "PT1S", "1s", "1 second"), //
        new TestCase<Duration>(Duration.ofMillis(1), "PT0.001S", "0.001s", "0.001 seconds"), //
        new TestCase<Duration>(Duration.ofNanos(1000), "PT0.000001S", "0.000001s", "0.000001 seconds"), //
        new TestCase<Duration>(Duration.ofNanos(1), "PT0.000000001S", "0.000000001s", "0.000000001 seconds"), //
        new TestCase<Duration>(Duration.ofHours(2), "PT2H", "2H", "2 hours"), //
        new TestCase<Duration>(Duration.ofMinutes(2), "PT2M", "2m", "2 minutes"), //
        new TestCase<Duration>(Duration.ofSeconds(2), "PT2S", "2s", "2 seconds"), //
        new TestCase<Duration>(Duration.parse("PT1H2M3.4S"), "PT1H2M3.4S", "1H 2m 3.4s",
            "1 hour 2 minutes 3.4 seconds"), //
        new TestCase<Duration>(Duration.ZERO, "PT0S", "0s", "0 seconds") //
    );

    private static final List<TestCase<Period>> PERIOD_TEST_CASES = List.of( //
        new TestCase<Period>(Period.ofDays(1), "P1D", "1d", "1 day"), //
        new TestCase<Period>(Period.ofMonths(1), "P1M", "1M", "1 month"), //
        new TestCase<Period>(Period.ofYears(1), "P1Y", "1y", "1 year"), //
        new TestCase<Period>(Period.ofDays(2), "P2D", "2d", "2 days"), //
        new TestCase<Period>(Period.ofMonths(2), "P2M", "2M", "2 months"), //
        new TestCase<Period>(Period.ofYears(2), "P2Y", "2y", "2 years"), //
        new TestCase<Period>(Period.ofWeeks(3), "P21D", "21d", "21 days"), //
        new TestCase<Period>(Period.of(1, 2, 3), "P1Y2M3D", "1y 2M 3d", "1 year 2 months 3 days"), //
        new TestCase<Period>(Period.ZERO, "P0D", "0d", "0 days") //
    );

    static Stream<Arguments> provideArgumentsForTestCases() {
        BiFunction<TestCase<? extends TemporalAmount>, List<DurationPeriodType>, Stream<Arguments>> unpackTestCase =
            (tc, types) -> Stream.of( //
                types.stream().map(type -> Stream.of( //
                    Arguments.of(tc.expectedInterval, tc.iso, type), //
                    Arguments.of(tc.expectedInterval, tc.shortForm, type), //
                    Arguments.of(tc.expectedInterval, tc.longForm, type) //
                )).flatMap(Function.identity()) //
            ).flatMap(Function.identity());

        return Stream.concat( //
            DURATION_TEST_CASES.stream().map(
                tc -> unpackTestCase.apply(tc, List.of(DurationPeriodType.DURATION, DurationPeriodType.AUTO_DETECT))), //
            PERIOD_TEST_CASES.stream()
                .map(tc -> unpackTestCase.apply(tc, List.of(DurationPeriodType.PERIOD, DurationPeriodType.AUTO_DETECT))) //
        ).flatMap(Function.identity());
    }

    @ParameterizedTest(name = "Parse {1} as {2}")
    @MethodSource("provideArgumentsForTestCases")
    void testParsing(final TemporalAmount expectedInterval, final String input, final DurationPeriodType type)
        throws InvalidSettingsException, IOException {

        var settings = new StringToDurationPeriodNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_durationType = type;

        var testSetup = setupAndExecuteWorkflow(settings, StringCellFactory.create(input));

        var outputCell = testSetup.firstCell;

        if (outputCell instanceof DurationCell && type != DurationPeriodType.PERIOD) {
            assertEquals(expectedInterval, ((DurationCell)outputCell).getDuration(), "Output cell is not as expected");
        } else if (outputCell instanceof PeriodCell && type != DurationPeriodType.DURATION) {
            assertEquals(expectedInterval, ((PeriodCell)outputCell).getPeriod(), "Output cell is not as expected");
        } else {
            fail("Output cell is not of the expected type");
        }
    }

    @Test
    void testOutputSettingReplace() throws InvalidSettingsException, IOException {
        var settings = new StringToDurationPeriodNodeSettings();
        settings.m_replaceOrAppend = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});

        var testSetup = setupAndExecuteWorkflow(settings, StringCellFactory.create("PT1H2M3.0456789S"));

        var outputTableSpec = testSetup.outputTable.getDataTableSpec();
        assertEquals(1, outputTableSpec.getNumColumns(), "Expected exactly one output column");

        var outputColumnName = outputTableSpec.getColumnNames()[0];
        assertEquals(INPUT_COLUMN, outputColumnName, "Output column name is not as expected");
    }

    @Test
    void testOutputSettingAppendWithSuffix() throws InvalidSettingsException, IOException {
        var settings = new StringToDurationPeriodNodeSettings();
        settings.m_replaceOrAppend = ReplaceOrAppend.APPEND;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_appendedSuffix = "_converted";

        var testSetup = setupAndExecuteWorkflow(settings, StringCellFactory.create("PT1H2M3.0456789S"));

        var outputTableSpec = testSetup.outputTable.getDataTableSpec();
        assertEquals(2, outputTableSpec.getNumColumns(), "Expected exactly two output columns");
        assertEquals(INPUT_COLUMN, outputTableSpec.getColumnNames()[0], "First column name should not have changed");
        assertEquals(INPUT_COLUMN + settings.m_appendedSuffix, outputTableSpec.getColumnNames()[1],
            "Second column name is not as expected");
    }

    @Test
    void testOutputSettingMissingOnFail() throws InvalidSettingsException, IOException {
        var settings = new StringToDurationPeriodNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_replaceOrAppend = ReplaceOrAppend.REPLACE;
        settings.m_actionIfExtractionFails = ActionIfExtractionFails.SET_MISSING;

        var testSetup = setupAndExecuteWorkflow(settings, StringCellFactory.create("not parseable"));

        assertTrue(testSetup.success, "Execution should have been successful");
        assertTrue(testSetup.firstCell.isMissing(), "Output cell should be missing");
    }

    @Test
    void testOutputSettingFailOnFail() throws InvalidSettingsException, IOException {
        var settings = new StringToDurationPeriodNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_replaceOrAppend = ReplaceOrAppend.REPLACE;
        settings.m_actionIfExtractionFails = ActionIfExtractionFails.FAIL;

        var testSetup = setupAndExecuteWorkflow(settings, StringCellFactory.create("not parseable"));

        assertFalse(testSetup.success, "Execution should have failed");
    }

    record TestSetup(BufferedDataTable outputTable, DataCell firstCell, boolean success) {
    }

    static TestSetup setupAndExecuteWorkflow(final StringToDurationPeriodNodeSettings settings,
        final DataCell cellToAdd) throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new StringToDurationPeriodNodeFactory2());

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(StringToDurationPeriodNodeSettings.class, settings, modelSettings);
        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // populate the input table
        var inputTableSpec = new TableTestUtil.SpecBuilder() //
            .addColumn(INPUT_COLUMN, cellToAdd.getType()) //
            .build();
        var inputTable = new TableTestUtil.TableBuilder(inputTableSpec) //
            .addRow(cellToAdd) //
            .build();
        var tableSupplierNode =
            WorkflowManagerUtil.createAndAddNode(workflowManager, new InputTableNode.InputDataNodeFactory(inputTable));

        // link the nodes
        workflowManager.addConnection(tableSupplierNode.getID(), 1, node.getID(), 1);

        // execute and wait...
        var success = workflowManager.executeAllAndWaitUntilDone();

        var outputTable = (BufferedDataTable)node.getOutPort(1).getPortObject();

        if (outputTable == null) {
            return new TestSetup(null, null, success);
        } else {
            try (var it = outputTable.iterator()) {
                var firstCell = it.next().getCell(0);
                return new TestSetup(outputTable, firstCell, success);
            }
        }
    }
}