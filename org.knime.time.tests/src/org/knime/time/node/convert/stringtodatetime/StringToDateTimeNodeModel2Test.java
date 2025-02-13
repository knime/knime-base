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
package org.knime.time.node.convert.stringtodatetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.NodeModelTestRunnerUtil;
import org.knime.core.data.DataCell;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.history.DateTimeFormatStringHistoryManager;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat.FormatTemporalType;
import org.knime.time.util.ActionIfExtractionFails;
import org.knime.time.util.ReplaceOrAppend;
import org.knime.time.util.TemporalCellUtils;
import org.mockito.Mockito;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings({"static-method", "restriction"})
final class StringToDateTimeNodeModel2Test {

    private static final String INPUT_COLUMN = "input_column";

    private static final NodeModelTestRunnerUtil RUNNER = new NodeModelTestRunnerUtil(INPUT_COLUMN,
        "StringToDateTimeNode", StringToDateTimeNodeSettings.class, StringToDateTimeNodeFactory2.class);

    private static record TestCase<T extends TemporalAccessor>(String input, String pattern, T expected,
        Locale locale) {

        public TestCase(final String input, final String pattern, final T expected) {
            this(input, pattern, expected, Locale.ENGLISH);
        }
    }

    private static final List<TestCase<?>> TEST_CASES = List.of( //
        new TestCase<>("2024-12-18", "yyyy-MM-dd", LocalDate.of(2024, 12, 18)), //
        new TestCase<>("July 11 1996", "MMMM dd yyyy", LocalDate.of(1996, 7, 11), Locale.ENGLISH), //
        new TestCase<>("Dezember 11 1996", "MMMM dd yyyy", LocalDate.of(1996, 12, 11), Locale.GERMANY), //
        new TestCase<>("2024-12-18T12:34:56", "yyyy-MM-dd'T'HH:mm:ss", LocalDateTime.of(2024, 12, 18, 12, 34, 56)), //
        new TestCase<>("12:34:56", "HH:mm:ss", LocalTime.of(12, 34, 56)), //
        new TestCase<>("2024-12-18T12:34:56+01:00", "yyyy-MM-dd'T'HH:mm:ssXXX",
            ZonedDateTime.of(2024, 12, 18, 12, 34, 56, 0, ZoneId.ofOffset("", ZoneOffset.ofHours(1)))) //
    );

    private static Stream<Arguments> provideTestCases() {
        return TEST_CASES.stream().map(Arguments::of);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTestCases")
    void testConvertWithReplace(final TestCase<?> testCase) throws InvalidSettingsException, IOException {
        var settings = new StringToDateTimeNodeSettings();
        settings.m_format = new TemporalFormat(testCase.pattern, inferFormatTypeFromAccessor(testCase.expected));
        settings.m_appendOrReplace = ReplaceOrAppend.REPLACE;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = testCase.locale.toLanguageTag();

        var cellToAdd = StringCellFactory.create(testCase.input);

        var setup = RUNNER.setupAndExecuteWorkflow(settings, cellToAdd);

        var firstOutputCell = setup.firstCell();
        var firstOutputValue = TemporalCellUtils.getTemporalFromCell(firstOutputCell);

        assertEquals(testCase.expected, firstOutputValue);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTestCases")
    void testConvertWithAppend(final TestCase<?> testCase) throws InvalidSettingsException, IOException {
        var settings = new StringToDateTimeNodeSettings();
        settings.m_format = new TemporalFormat(testCase.pattern, inferFormatTypeFromAccessor(testCase.expected));
        settings.m_appendOrReplace = ReplaceOrAppend.APPEND;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = testCase.locale.toLanguageTag();

        var cellToAdd = StringCellFactory.create(testCase.input);

        var setup = RUNNER.setupAndExecuteWorkflow(settings, cellToAdd);

        DataCell firstCell;
        try (var it = setup.outputTable().iterator()) {
            if (settings.m_appendOrReplace == ReplaceOrAppend.REPLACE) {
                firstCell = it.next().getCell(0);
            } else {
                firstCell = it.next().getCell(1);
            }
        }

        var firstOutputValue = TemporalCellUtils.getTemporalFromCell(firstCell);

        assertEquals(testCase.expected, firstOutputValue);
    }

    @Test
    void checkThatZeroLengthTableReturnsZeroLengthTable() throws InvalidSettingsException, IOException {
        var settings = new StringToDateTimeNodeSettings();
        settings.m_format = new TemporalFormat("yyyy-MM-dd", FormatTemporalType.DATE);
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = Locale.ENGLISH.toLanguageTag();

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, null, StringCellFactory.TYPE);

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertNull(testSetup.firstCell(), "Output cell should not exists");
        assertEquals(0, testSetup.outputTable().size(), "Ouptput table should be empty");
    }

    @Test
    void checkThatFormatsAreAddedToHistory() throws InvalidSettingsException, IOException {
        var settings = new StringToDateTimeNodeSettings();
        settings.m_appendOrReplace = ReplaceOrAppend.APPEND;
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = Locale.ENGLISH.toLanguageTag();
        settings.m_format = new TemporalFormat("X", FormatTemporalType.ZONED_DATE_TIME);

        var cellToAdd = StringCellFactory.create("-083015");

        try (final var staticStringHistoryManagerMock =
            Mockito.mockStatic(DateTimeFormatStringHistoryManager.class, Mockito.CALLS_REAL_METHODS)) {
            RUNNER.setupAndExecuteWorkflow(settings, cellToAdd);

            staticStringHistoryManagerMock
                .verify(() -> DateTimeFormatStringHistoryManager.addFormatToStringHistoryIfNotPresent("X"));
        }
    }

    @Test
    void checkThatInvalidFormatsFailValidation() {
        var settings = new StringToDateTimeNodeSettings();
        settings.m_format = new TemporalFormat("invalid format", FormatTemporalType.DATE);

        assertThrows(InvalidSettingsException.class, () -> {
            new StringToDateTimeNodeModel2(StringToDateTimeNodeFactory2.CONFIGURATION).validateSettings(settings);
        });
    }

    @Test
    void checkThatIncompatibleFormatsGiveMissingCellWhenFailSettingIsNotSet()
        throws InvalidSettingsException, IOException {
        var settings = new StringToDateTimeNodeSettings();
        settings.m_format = new TemporalFormat("HH:mm:ss", FormatTemporalType.TIME);
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = Locale.ENGLISH.toLanguageTag();
        settings.m_onError = ActionIfExtractionFails.SET_MISSING;

        var cellToAdd = StringCellFactory.create("2024-12-18");
        var setup = RUNNER.setupAndExecuteWorkflow(settings, cellToAdd);

        var firstOutputCell = setup.firstCell();

        assertTrue(firstOutputCell.isMissing(),
            "Format 'yyyy' should be incompatible with provided string and give a missing cell");
    }

    @Test
    void checkThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {
        var settings = new StringToDateTimeNodeSettings();
        settings.m_format = new TemporalFormat("yyyy-MM-dd", FormatTemporalType.DATE);
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = Locale.ENGLISH.toLanguageTag();

        var cellToAdd = new MissingCell("test error");
        var outputCell = RUNNER.setupAndExecuteWorkflow(settings, cellToAdd).firstCell();

        assertTrue(outputCell.isMissing());
    }

    @Test
    void checkThatIncompatibleFormatsGiveErrorWhenFailSettingIsSet() throws InvalidSettingsException, IOException {
        var settings = new StringToDateTimeNodeSettings();
        settings.m_format = new TemporalFormat("yyyy", FormatTemporalType.DATE);
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = Locale.ENGLISH.toLanguageTag();
        settings.m_onError = ActionIfExtractionFails.FAIL;

        var cellToAdd = StringCellFactory.create("2024-12-18");
        var setup = RUNNER.setupAndExecuteWorkflow(settings, cellToAdd);

        assertFalse(setup.nodeState().isExecuted(), "The node should fail");
        assertNull(setup.outputTable(),
            "Format 'yyyy' should be incompatible with provided string and give no output at all");
    }

    private static FormatTemporalType inferFormatTypeFromAccessor(final TemporalAccessor a) {
        if (a instanceof LocalDate) {
            return FormatTemporalType.DATE;
        } else if (a instanceof LocalTime) {
            return FormatTemporalType.TIME;
        } else if (a instanceof LocalDateTime) {
            return FormatTemporalType.DATE_TIME;
        } else if (a instanceof ZonedDateTime) {
            return FormatTemporalType.ZONED_DATE_TIME;
        } else {
            throw new IllegalArgumentException("TemporalAccessor of class '%s' does not have a matching TemporalType"
                .formatted(a.getClass().getSimpleName()));
        }
    }
}
