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
 * Tests for the applyStash method of LongParameters, testing stashing from other parameter types to LongParameters.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class LongParametersStashTest {

    private LongParameters m_longParams;

    @BeforeEach
    void setUp() {
        m_longParams = new LongParameters();
    }

    @Test
    void testApplyStashWithEmptyArray() {
        m_longParams.m_value = 42L;
        m_longParams.applyStash(new DataValue[0]);
        assertEquals(42L, m_longParams.m_value, "Value should remain unchanged with empty stash array");
    }

    @Test
    void testApplyStashWithLongCell() {
        final var longCell = new LongCell(123456789L);
        m_longParams.applyStash(new DataValue[]{longCell});
        assertEquals(123456789L, m_longParams.m_value, "Should apply LongCell value");
    }

    @Test
    void testApplyStashWithIntCell() {
        final var intCell = new IntCell(987);
        m_longParams.applyStash(new DataValue[]{intCell});
        assertEquals(987L, m_longParams.m_value, "Should convert IntCell to long");
    }

    @Test
    void testApplyStashWithDoubleCell() {
        final var doubleCell = new DoubleCell(654.32);
        m_longParams.applyStash(new DataValue[]{doubleCell});
        assertEquals(654L, m_longParams.m_value, "Should cast DoubleCell to long");
    }

    @Test
    void testApplyStashWithStringCell() {
        final var stringCell = new StringCell("1234567890");
        m_longParams.applyStash(new DataValue[]{stringCell});
        assertEquals(1234567890L, m_longParams.m_value, "Should parse valid long string");
    }

    @Test
    void testApplyStashWithInvalidString() {
        m_longParams.m_value = 42L;
        final var stringCell = new StringCell("invalid_long");
        m_longParams.applyStash(new DataValue[]{stringCell});
        assertEquals(42L, m_longParams.m_value, "Should ignore invalid string and keep original value");
    }

    @Test
    void testApplyStashWithNegativeValue() {
        final var negativeLongCell = new LongCell(-9876543210L);
        m_longParams.applyStash(new DataValue[]{negativeLongCell});
        assertEquals(-9876543210L, m_longParams.m_value, "Should handle negative long value");
    }

    @Test
    void testApplyStashWithZeroValue() {
        final var zeroLongCell = new LongCell(0L);
        m_longParams.applyStash(new DataValue[]{zeroLongCell});
        assertEquals(0L, m_longParams.m_value, "Should handle zero long value");
    }

    @ParameterizedTest(name = "Stash from Int: {0} -> {1}")
    @CsvSource({
        "98765, 98765",
        "-42, -42",
        "2147483647, 2147483647"
    })
    void testStashFromIntToLong(final int sourceValue, final long expectedValue) {
        final var intParams = new IntParameters();
        intParams.m_value = sourceValue;
        m_longParams.applyStash(intParams.stash());
        assertEquals(expectedValue, m_longParams.m_value, "Should stash int value " + sourceValue + " to long");
    }

    @ParameterizedTest(name = "Stash from Double: {0} -> {1}")
    @CsvSource({
        "12345.678, 12345",
        "-9999.123, -9999",
        "1.23456789e10, 12345678900"})
    void testStashFromDoubleToLong(final double sourceValue, final long expectedValue) {
        final var doubleParams = new DoubleParameters();
        doubleParams.m_value = sourceValue;
        m_longParams.applyStash(doubleParams.stash());
        assertEquals(expectedValue, m_longParams.m_value,
            "Should stash double value " + sourceValue + " to long with truncation");
    }

    @ParameterizedTest(name = "Stash from String: ''{0}'' -> {1}")
    @CsvSource({
        "9876543210, 9876543210",
        "-123456789, -123456789",
        "0, 0"})
    void testStashFromStringToLong(final String sourceValue, final long expectedValue) {
        final var stringParams = new SingleStringParameters(sourceValue);
        m_longParams.applyStash(stringParams.stash());
        assertEquals(expectedValue, m_longParams.m_value, "Should parse string '" + sourceValue + "' to long");
    }

    @Test
    void testStashFromStringToLongWithInvalidString() {
        m_longParams.m_value = 100L;
        final var stringParams = new SingleStringParameters("not_a_number");
        m_longParams.applyStash(stringParams.stash());
        assertEquals(100L, m_longParams.m_value, "Should keep original value when string cannot be parsed");
    }
}
