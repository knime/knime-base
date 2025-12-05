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
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * Tests for the applyStash method of IntParameters, testing stashing from other parameter types to IntParameters.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class IntParametersStashTest {

    private IntParameters m_intParams;

    @BeforeEach
    void setUp() {
        m_intParams = new IntParameters();
    }

    @Test
    void testApplyStashWithEmptyArray() {
        m_intParams.m_value = 42;
        m_intParams.applyStash(new DataValue[0]);
        assertEquals(42, m_intParams.m_value, "Value should remain unchanged with empty stash array");
    }

    @Test
    void testApplyStashWithIntCell() {
        final var intCell = new IntCell(123);
        m_intParams.applyStash(new DataValue[]{intCell});
        assertEquals(123, m_intParams.m_value, "Should apply IntCell value");
    }

    @Test
    void testApplyStashWithDoubleCell() {
        final var doubleCell = new DoubleCell(456.78);
        m_intParams.applyStash(new DataValue[]{doubleCell});
        assertEquals(456, m_intParams.m_value, "Should cast DoubleCell to int");
    }

    @Test
    void testApplyStashWithLongCell() {
        final var longCell = new LongCell(789L);
        m_intParams.applyStash(new DataValue[]{longCell});
        assertEquals(789, m_intParams.m_value, "Should cast LongCell to int");
    }

    @Test
    void testApplyStashWithStringCell() {
        final var stringCell = new StringCell("321");
        m_intParams.applyStash(new DataValue[]{stringCell});
        assertEquals(321, m_intParams.m_value, "Should parse valid integer string");
    }

    @Test
    void testApplyStashWithInvalidString() {
        m_intParams.m_value = 42;
        final var stringCell = new StringCell("not_a_number");
        m_intParams.applyStash(new DataValue[]{stringCell});
        assertEquals(42, m_intParams.m_value, "Should ignore invalid string and keep original value");
    }

    @Test
    void testApplyStashWithUnsupportedType() {
        m_intParams.m_value = 42;
        final var unsupportedCell = BooleanCell.TRUE;
        m_intParams.applyStash(new DataValue[]{unsupportedCell});
        assertEquals(42, m_intParams.m_value, "Should ignore unsupported types and keep original value");
    }

    @Test
    void testApplyStashWithLargeDoubleValue() {
        final var doubleCell = new DoubleCell(Double.MAX_VALUE);
        m_intParams.applyStash(new DataValue[]{doubleCell});
        // This will overflow, but the test verifies the casting behavior
        assertEquals((int)Double.MAX_VALUE, m_intParams.m_value, "Should handle overflow when casting large double");
    }

    @Test
    void testApplyStashWithNegativeValue() {
        final var negativeIntCell = new IntCell(-123);
        m_intParams.applyStash(new DataValue[]{negativeIntCell});
        assertEquals(-123, m_intParams.m_value, "Should handle negative int value");
    }

    @Test
    void testApplyStashWithZeroValue() {
        final var zeroIntCell = new IntCell(0);
        m_intParams.applyStash(new DataValue[]{zeroIntCell});
        assertEquals(0, m_intParams.m_value, "Should handle zero int value");
    }

    // Cross-parameter stashing tests
    @ParameterizedTest(name = "Stash from Long: {0} -> {1}")
    @CsvSource({
        "12345, 12345",
        "-42, -42"})
    void testStashFromLongToInt(final long sourceValue, final int expectedValue) {
        final var longParams = new LongParameters();
        longParams.m_value = sourceValue;
        m_intParams.applyStash(longParams.stash());
        assertEquals(expectedValue, m_intParams.m_value, "Should stash long value " + sourceValue + " to int");
    }

    @ParameterizedTest(name = "Stash from Double: {0} -> {1}")
    @CsvSource({
        "789.456, 789",
        "-123.789, -123",
        "0.0, 0"})
    void testStashFromDoubleToInt(final double sourceValue, final int expectedValue) {
        final var doubleParams = new DoubleParameters();
        doubleParams.m_value = sourceValue;
        m_intParams.applyStash(doubleParams.stash());
        assertEquals(expectedValue, m_intParams.m_value,
            "Should stash double value " + sourceValue + " to int with truncation");
    }

    @ParameterizedTest(name = "Stash from String: ''{0}'' -> {1}")
    @CsvSource({
        "42, 42",
        "-999, -999",
        "0, 0"})
    void testStashFromStringToInt(final String sourceValue, final int expectedValue) {
        final var stringParams = new SingleStringParameters();
        stringParams.m_value = sourceValue;
        m_intParams.applyStash(stringParams.stash());
        assertEquals(expectedValue, m_intParams.m_value, "Should parse string '" + sourceValue + "' to int");
    }

    @Test
    void testStashFromStringToIntWithInvalidString() {
        m_intParams.m_value = 100;
        final var stringParams = new SingleStringParameters();
        stringParams.m_value = "not_a_number";
        m_intParams.applyStash(stringParams.stash());
        assertEquals(100, m_intParams.m_value, "Should keep original value when string cannot be parsed");
    }
}