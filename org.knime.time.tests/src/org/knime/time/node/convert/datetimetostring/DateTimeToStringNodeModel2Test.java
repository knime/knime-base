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
 *   Dec 18, 2024 (david): created
 */
package org.knime.time.node.convert.datetimetostring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.InputTableNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.history.DateTimeFormatStringHistoryManager;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;
import org.knime.time.util.ReplaceOrAppend;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class DateTimeToStringNodeModel2Test {

    private static final String NODE_NAME = "DateTimeToString";

    private static final String INPUT_COLUMN = "input_column";

    private static record TestCase<T extends TemporalAccessor>(String expected, String pattern, T input,
        Locale locale) {

        public TestCase(final String expected, final String pattern, final T input) {
            this(expected, pattern, input, Locale.ENGLISH);
        }
    }

    private static final List<TestCase<?>> TEST_CASES = List.of( //
        new TestCase<>("2024-12-18", "yyyy-MM-dd", LocalDate.of(2024, 12, 18)), //
        new TestCase<>("July", "MMMM", LocalDate.of(2024, 7, 18), Locale.ENGLISH), //
        new TestCase<>("Dezember", "MMMM", LocalDate.of(2024, 12, 18), Locale.GERMANY), //
        new TestCase<>("2024-12-18T12:34:56", "yyyy-MM-dd'T'HH:mm:ss", LocalDateTime.of(2024, 12, 18, 12, 34, 56)), //
        new TestCase<>("12:34:56", "HH:mm:ss", LocalTime.of(12, 34, 56)), //
        new TestCase<>("2024-12-18T12:34:56+01:00", "yyyy-MM-dd'T'HH:mm:ssXXX",
            ZonedDateTime.of(2024, 12, 18, 12, 34, 56, 1, ZoneId.of("Europe/Berlin"))) //
    );

    private static Stream<Arguments> provideTestCases() {
        return TEST_CASES.stream().map(Arguments::of);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTestCases")
    void testConvertWithReplace(final TestCase<?> testCase) throws InvalidSettingsException, IOException {
        var settings = new DateTimeToStringNodeSettings();
        settings.m_format = testCase.pattern;
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = testCase.locale.toLanguageTag();

        var cellToAdd = createDataCellFromTemporal(testCase.input);

        var setup = setupAndExecuteWorkflow(settings, cellToAdd);

        var firstOutputCell = setup.firstOutputCell;
        var firstOutputValue = ((StringValue)firstOutputCell).getStringValue();

        assertEquals(testCase.expected, firstOutputValue);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTestCases")
    void testConvertWithAppend(final TestCase<?> testCase) throws InvalidSettingsException, IOException {
        var settings = new DateTimeToStringNodeSettings();
        settings.m_format = testCase.pattern;
        settings.m_appendOrReplace = ReplaceOrAppend.APPEND;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = testCase.locale.toLanguageTag();

        var cellToAdd = createDataCellFromTemporal(testCase.input);

        var setup = setupAndExecuteWorkflow(settings, cellToAdd);

        var firstOutputCell = setup.firstOutputCell;
        var firstOutputValue = ((StringValue)firstOutputCell).getStringValue();

        assertEquals(testCase.expected, firstOutputValue);
    }

    @Test
    void checkThatFormatsAreAddedToHistory() throws InvalidSettingsException, IOException {
        var stringHistoryBefore = DateTimeFormatStringHistoryManager.getRecentFormats();

        var settings = new DateTimeToStringNodeSettings();
        settings.m_appendOrReplace = ReplaceOrAppend.APPEND;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = Locale.ENGLISH.toLanguageTag();
        settings.m_format = "X";

        var cellToAdd = createDataCellFromTemporal(
            ZonedDateTime.of(LocalDate.EPOCH, LocalTime.MIDNIGHT, ZoneId.of("Africa/Bujumbura")));
        setupAndExecuteWorkflow(settings, cellToAdd);

        var stringHistoryAfter = DateTimeFormatStringHistoryManager.getRecentFormats();

        assertEquals(stringHistoryBefore.size() + 1, stringHistoryAfter.size());
        assertFalse(stringHistoryBefore.contains("X"));
        assertTrue(stringHistoryAfter.contains("X"));
    }

    @Test
    void checkThatZeroLengthTableReturnsZeroLengthTable() throws InvalidSettingsException, IOException {
        var settings = new DateTimeToStringNodeSettings();
        settings.m_format = "yyyy-MM-dd";
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = Locale.ENGLISH.toLanguageTag();

        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new DateTimeToStringNodeFactory2());

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(DateTimeToStringNodeSettings.class, settings, modelSettings);

        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        var inputTableSpec = new TableTestUtil.SpecBuilder() //
            .addColumn(INPUT_COLUMN, LocalDateCellFactory.TYPE) //
            .build();
        var inputTable = new TableTestUtil.TableBuilder(inputTableSpec) //
            .build();
        var tableSupplierNode =
            WorkflowManagerUtil.createAndAddNode(workflowManager, new InputTableNode.InputDataNodeFactory(inputTable));

        // link the nodes
        workflowManager.addConnection(tableSupplierNode.getID(), 1, node.getID(), 1);

        workflowManager.executeAllAndWaitUntilDone();

        var outputTable = (BufferedDataTable)node.getOutPort(1).getPortObject();

        assertEquals(0, outputTable.size(), "Output table should have zero rows");
    }

    @Test
    void checkThatInvalidFormatsFailValidation() {
        var settings = new DateTimeToStringNodeSettings();
        settings.m_format = "invalid format";

        assertThrows(InvalidSettingsException.class, () -> {
            new DateTimeToStringNodeModel2(DateTimeToStringNodeFactory2.CONFIGURATION).validateSettings(settings);
        });
    }

    @Test
    void checkThatIncompatibleFormatsGiveMissingCell() throws InvalidSettingsException, IOException {
        var settings = new DateTimeToStringNodeSettings();
        settings.m_format = "X";
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = Locale.ENGLISH.toLanguageTag();

        var cellToAdd = createDataCellFromTemporal(LocalDate.of(2024, 12, 18));
        var setup = setupAndExecuteWorkflow(settings, cellToAdd);

        var firstOutputCell = setup.firstOutputCell;

        assertTrue(firstOutputCell.isMissing(),
            "Format 'X' should be incompatible with LocalDate and give a missing cell");
    }

    @Test
    void checkThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new DateTimeToStringNodeSettings();
        settings.m_format = "yyyy-MM-dd";
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = Locale.ENGLISH.toLanguageTag();

        var cellToAdd = new MissingCell("test error");
        var outputCell = setupAndExecuteWorkflow(settings, cellToAdd).firstOutputCell;

        assertTrue(outputCell.isMissing());
    }

    private static DataCell createDataCellFromTemporal(final TemporalAccessor a) {
        if (a instanceof LocalDate ld) {
            return LocalDateCellFactory.create(ld);
        } else if (a instanceof LocalDateTime ldt) {
            return LocalDateTimeCellFactory.create(ldt);
        } else if (a instanceof LocalTime lt) {
            return LocalTimeCellFactory.create(lt);
        } else if (a instanceof ZonedDateTime zdt) {
            return ZonedDateTimeCellFactory.create(zdt);
        } else {
            throw new IllegalArgumentException("Unsupported TemporalAccessor type: " + a.getClass());
        }
    }

    private static record TestSetup(BufferedDataTable outputTable, DataCell firstOutputCell) {

    }

    static TestSetup setupAndExecuteWorkflow(final DateTimeToStringNodeSettings settings, final DataCell cellToAdd)
        throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new DateTimeToStringNodeFactory2());

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(DateTimeToStringNodeSettings.class, settings, modelSettings);

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
