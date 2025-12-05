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
 * Tests for the applyStash method of DoubleParameters, testing stashing from other parameter types to DoubleParameters.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class DoubleParametersStashTest {

    private DoubleParameters m_doubleParams;

    @BeforeEach
    void setUp() {
        m_doubleParams = new DoubleParameters();
    }

    @Test
    void testApplyStashWithEmptyArray() {
        m_doubleParams.m_value = 3.14;
        m_doubleParams.applyStash(new DataValue[0]);
        assertEquals(3.14, m_doubleParams.m_value, 1e-9, "Value should remain unchanged with empty stash array");
    }

    @Test
    void testApplyStashWithDoubleCell() {
        final var doubleCell = new DoubleCell(2.718281828);
        m_doubleParams.applyStash(new DataValue[]{doubleCell});
        assertEquals(2.718281828, m_doubleParams.m_value, 1e-9, "Should apply DoubleCell value");
    }

    @Test
    void testApplyStashWithIntCell() {
        final var intCell = new IntCell(42);
        m_doubleParams.applyStash(new DataValue[]{intCell});
        assertEquals(42.0, m_doubleParams.m_value, 1e-9, "Should convert IntCell to double");
    }

    @Test
    void testApplyStashWithLongCell() {
        final var longCell = new LongCell(987654321L);
        m_doubleParams.applyStash(new DataValue[]{longCell});
        assertEquals(987654321.0, m_doubleParams.m_value, 1e-9, "Should convert LongCell to double");
    }

    @ParameterizedTest(name = "Parse string ''{0}'' -> {1}")
    @CsvSource({
        "123.456, 123.456",
        "1.23e-4, 1.23e-4"})
    void testApplyStashWithStringCell(final String stringValue, final double expectedValue) {
        final var stringCell = new StringCell(stringValue);
        m_doubleParams.applyStash(new DataValue[]{stringCell});
        assertEquals(expectedValue, m_doubleParams.m_value, 1e-9, "Should parse double string '" + stringValue + "'");
    }

    @Test
    void testApplyStashWithInvalidString() {
        m_doubleParams.m_value = 3.14;
        final var stringCell = new StringCell("not_a_double");
        m_doubleParams.applyStash(new DataValue[]{stringCell});
        assertEquals(3.14, m_doubleParams.m_value, 1e-9, "Should ignore invalid string and keep original value");
    }

    @ParameterizedTest(name = "Apply DoubleCell: {0}")
    @CsvSource({
        "-99.99, 1e-9",
        "0.0, 1e-9",
        "1e-100, 1e-110"})
    void testApplyStashWithVariousDoubleValues(final double doubleValue, final double delta) {
        final var doubleCell = new DoubleCell(doubleValue);
        m_doubleParams.applyStash(new DataValue[]{doubleCell});
        assertEquals(doubleValue, m_doubleParams.m_value, delta, "Should handle double value " + doubleValue);
    }

    // Cross-parameter stashing tests
    @ParameterizedTest(name = "Stash from Int: {0} -> {1}")
    @CsvSource({
        "42, 42.0",
        "-123, -123.0",
        "2147483647, 2.147483647E9"
    })
    void testStashFromIntToDouble(final int sourceValue, final double expectedValue) {
        final var intParams = new IntParameters();
        intParams.m_value = sourceValue;
        m_doubleParams.applyStash(intParams.stash());
        assertEquals(expectedValue, m_doubleParams.m_value, 1e-9,
            "Should stash int value " + sourceValue + " to double");
    }

    @ParameterizedTest(name = "Stash from Long: {0} -> {1}")
    @CsvSource({
        "9876543210, 9.87654321E9",
        "-999999999, -9.99999999E8"})
    void testStashFromLongToDouble(final long sourceValue, final double expectedValue) {
        final var longParams = new LongParameters();
        longParams.m_value = sourceValue;
        m_doubleParams.applyStash(longParams.stash());
        assertEquals(expectedValue, m_doubleParams.m_value, 1e-9,
            "Should stash long value " + sourceValue + " to double");
    }

    @Test
    void testStashFromLongToDoubleWithMaxLong() {
        final var longParams = new LongParameters();
        longParams.m_value = Long.MAX_VALUE;
        m_doubleParams.applyStash(longParams.stash());
        assertEquals(Long.MAX_VALUE, m_doubleParams.m_value, 1e9,
            "Should stash Long.MAX_VALUE to double (precision loss expected)");
    }

    @ParameterizedTest(name = "Stash from String: ''{0}'' -> {1}")
    @CsvSource({
        "3.14159, 3.14159",
        "-123.456, -123.456",
        "6.022e23, 6.022E23"})
    void testStashFromStringToDouble(final String sourceValue, final double expectedValue) {
        final var stringParams = new SingleStringParameters(sourceValue);
        m_doubleParams.applyStash(stringParams.stash());
        assertEquals(expectedValue, m_doubleParams.m_value, 1e14,
            "Should parse string '" + sourceValue + "' to double");
    }

    @Test
    void testStashFromStringToDoubleWithInvalidString() {
        final var stringParams = new SingleStringParameters("not_a_number");
        m_doubleParams.m_value = 2.718;
        m_doubleParams.applyStash(stringParams.stash());
        assertEquals(2.718, m_doubleParams.m_value, 1e-9, "Should keep original value when string cannot be parsed");
    }
}
