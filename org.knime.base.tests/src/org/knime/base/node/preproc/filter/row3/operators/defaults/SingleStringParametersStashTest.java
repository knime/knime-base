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
 *   27 Nov 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.defaults;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * Tests for the applyStash method of SingleStringParameters, testing stashing from other parameter types to SingleStringParameters.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class SingleStringParametersStashTest {

    private SingleStringParameters m_stringParams;

    @BeforeEach
    void setUp() {
        m_stringParams = new SingleStringParameters();
    }

    @Test
    void testApplyStashWithEmptyArray() {
        m_stringParams.m_value = "original";
        m_stringParams.applyStash(new DataValue[0]);
        assertEquals("original", m_stringParams.m_value, "Value should remain unchanged with empty stash array");
    }

    @Test
    void testApplyStashWithStringCell() {
        final var stringCell = new StringCell("hello world");
        m_stringParams.applyStash(new DataValue[]{stringCell});
        assertEquals("hello world", m_stringParams.m_value, "Should apply StringCell value");
    }

    @Test
    void testApplyStashWithNullValue() {
        m_stringParams.m_value = "original";
        m_stringParams.applyStash(new DataValue[]{null});
        assertEquals("original", m_stringParams.m_value, "Should ignore null value and keep original");
    }

    @Test
    void testApplyStashWithIntCell() {
        final var intCell = new IntCell(42);
        m_stringParams.applyStash(new DataValue[]{intCell});
        assertEquals("42", m_stringParams.m_value, "Should convert IntCell to string representation");
    }

    @Test
    void testApplyStashWithLongCell() {
        final var longCell = new LongCell(9876543210L);
        m_stringParams.applyStash(new DataValue[]{longCell});
        assertEquals("9876543210", m_stringParams.m_value, "Should convert LongCell to string representation");
    }

    @Test
    void testApplyStashWithDoubleCell() {
        final var doubleCell = new DoubleCell(3.14159);
        m_stringParams.applyStash(new DataValue[]{doubleCell});
        assertEquals("3.14159", m_stringParams.m_value, "Should convert DoubleCell to string representation");
    }

    @Test
    void testApplyStashWithEmptyString() {
        final var emptyStringCell = new StringCell("");
        m_stringParams.applyStash(new DataValue[]{emptyStringCell});
        assertEquals("", m_stringParams.m_value, "Should handle empty string");
    }

    @Test
    void testApplyStashWithWhitespaceString() {
        final var whitespaceStringCell = new StringCell("   \t\n   ");
        m_stringParams.applyStash(new DataValue[]{whitespaceStringCell});
        assertEquals("   \t\n   ", m_stringParams.m_value, "Should handle whitespace string");
    }

    @Test
    void testApplyStashWithNegativeIntCell() {
        final var negativeIntCell = new IntCell(-123);
        m_stringParams.applyStash(new DataValue[]{negativeIntCell});
        assertEquals("-123", m_stringParams.m_value, "Should convert negative IntCell to string");
    }

    // Cross-parameter stashing tests
    @ParameterizedTest(name = "Stash from Int: {0} -> ''{1}''")
    @CsvSource({
        "42, 42",
        "-999, -999",
        "0, 0"
    })
    void testStashFromIntToString(final int sourceValue, final String expectedValue) {
        final var intParams = new IntParameters();
        intParams.m_value = sourceValue;
        m_stringParams.applyStash(intParams.stash());
        assertEquals(expectedValue, m_stringParams.m_value,
            "Should stash int value " + sourceValue + " to string");
    }

    @ParameterizedTest(name = "Stash from Long: {0} -> ''{1}''")
    @CsvSource({
        "123456789012345, 123456789012345",
        "-987654321, -987654321",
        "9223372036854775807, 9223372036854775807" // Long.MAX_VALUE
    })
    void testStashFromLongToString(final long sourceValue, final String expectedValue) {
        final var longParams = new LongParameters();
        longParams.m_value = sourceValue;
        m_stringParams.applyStash(longParams.stash());
        assertEquals(expectedValue, m_stringParams.m_value,
            "Should stash long value " + sourceValue + " to string");
    }

    @ParameterizedTest(name = "Stash from Double: {0} -> ''{1}''")
    @CsvSource(delimiter = '|', textBlock = """
        3.14159     | 3.14159
        -2.718      | -2.718
        0.0         | 0
        1.23e-10    | 1.23E-10
        1.23e100    | 1.23E100
            """)
    void testStashFromDoubleToString(final double sourceValue, final String expectedValue) {
        final var doubleParams = new DoubleParameters();
        doubleParams.m_value = sourceValue;
        m_stringParams.applyStash(doubleParams.stash());
        assertEquals(expectedValue, m_stringParams.m_value, "Should stash double value " + sourceValue + " to string");
    }
}
