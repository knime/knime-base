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
 *   Jan 9, 2025 (david): created
 */
package org.knime.time.node.format.datetimeformatmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.base.node.viz.format.AlignmentSuggestionOption;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("static-method")
final class DateTimeDataValueFormatterTest {

    static final LocalDate TEST_DATE = LocalDate.of(2024, 1, 31);

    @Test
    void testPlainText() throws InvalidSettingsException {
        var toFormat = LocalDateCellFactory.create(TEST_DATE);

        Map<String, String> testCases = Map.of( //
            "yyyy", "2024", //
            "yyy-dd-MM", "2024-31-01", //
            "hh:mm", "Failed to format: 2024-01-31" //
        );

        for (var testCase : testCases.entrySet()) {
            var formatter =
                new DateTimeDataValueFormatter(testCase.getKey(), Locale.ENGLISH, AlignmentSuggestionOption.CENTER);

            var formattedText = formatter.getPlaintext(toFormat);

            assertEquals(testCase.getValue(), formattedText,
                "expected test case for %s to pass".formatted(testCase.getKey()));
        }
    }

    @Test
    void testAlignment() throws InvalidSettingsException {
        var toFormat = PeriodCellFactory.create(Period.of(1, 2, 3));

        Map<AlignmentSuggestionOption, String> testCases = Map.of( //
            AlignmentSuggestionOption.LEFT, "^.*text-align:\\s*left.*$", //
            AlignmentSuggestionOption.CENTER, "^.*text-align:\\s*center.*$", //
            AlignmentSuggestionOption.RIGHT, "^.*text-align:\\s*right.*$" //
        );

        for (var testCase : testCases.entrySet()) {
            var formatter = new DateTimeDataValueFormatter("yyyy", Locale.ENGLISH, testCase.getKey());

            var formattedHtml = formatter.getHTML(toFormat);

            assertTrue(formattedHtml.matches(testCase.getValue()),
                "expected test case for %s to contain something matching '%s' (output string was: %s)"
                    .formatted(testCase.getKey().name(), testCase.getValue(), formattedHtml));
        }
    }

    @Test
    void testEquality() throws InvalidSettingsException {
        // might not be too useful to test the equals method since
        // we only use it in tests right now, but oh well

        assertEquals( //
            new DateTimeDataValueFormatter("yyyy", Locale.ENGLISH, AlignmentSuggestionOption.CENTER), //
            new DateTimeDataValueFormatter("yyyy", Locale.ENGLISH, AlignmentSuggestionOption.CENTER), //
            "expected formatters with equal formats and alignments to be equal" //
        );

        assertNotEquals( //
            new DateTimeDataValueFormatter("yyyy", Locale.ENGLISH, AlignmentSuggestionOption.CENTER), //
            new DateTimeDataValueFormatter("yy", Locale.ENGLISH, AlignmentSuggestionOption.CENTER), //
            "expected different format to prevent equality" //
        );

        assertNotEquals( //
            new DateTimeDataValueFormatter("yyyy", Locale.ENGLISH, AlignmentSuggestionOption.CENTER), //
            new DateTimeDataValueFormatter("yyyy", Locale.GERMAN, AlignmentSuggestionOption.CENTER), //
            "expected different locale to prevent equality" //
        );

        assertNotEquals( //
            new DateTimeDataValueFormatter("yyyy", Locale.ENGLISH, AlignmentSuggestionOption.CENTER), //
            new DateTimeDataValueFormatter("yyyy", Locale.ENGLISH, AlignmentSuggestionOption.LEFT), //
            "expected different alignment to prevent equality" //
        );

        assertNotEquals( //
            new DateTimeDataValueFormatter("yyyy", Locale.ENGLISH, AlignmentSuggestionOption.CENTER), //
            new Object(), //
            "expected different types to prevent equality" //
        );

        assertFalse( // NOSONAR
            new DateTimeDataValueFormatter("yyyy", Locale.ENGLISH, AlignmentSuggestionOption.CENTER).equals(null), //
            "expected null to not be equal to a non-null formatter" //
        );
    }
}
