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
package org.knime.base.node.preproc.filter.row3.operators.rownumber;

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
 * Tests for the applyStash method of RowNumberParameters, testing stashing from other parameter types.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class RowNumberParametersStashTest {

    private RowNumberParameters m_rowNumberParams;

    @BeforeEach
    void setUp() {
        m_rowNumberParams = new RowNumberParameters();
    }

    @Test
    void testApplyStashWithEmptyArray() {
        m_rowNumberParams.m_value = 42L;
        m_rowNumberParams.applyStash(new DataValue[0]);
        assertEquals(42L, m_rowNumberParams.m_value,
            "Value should remain unchanged with empty stash array");
    }

    @ParameterizedTest(name = "Apply cell: {0} -> {1}")
    @CsvSource({
        "100, 100",
        "50, 50",
        "75.9, 75",
        "123, 123"
    })
    void testApplyStashWithVariousCells(final String cellValue, final long expectedValue) {
        DataValue cell;
        if (cellValue.contains(".")) {
            cell = new DoubleCell(Double.parseDouble(cellValue));
        } else {
            cell = new IntCell(Integer.parseInt(cellValue));
        }
        m_rowNumberParams.applyStash(new DataValue[]{cell});
        assertEquals(expectedValue, m_rowNumberParams.m_value,
            "Should apply cell value " + cellValue);
    }

    @Test
    void testApplyStashWithStringCell() {
        final var stringCell = new StringCell("123");
        m_rowNumberParams.applyStash(new DataValue[]{stringCell});
        assertEquals(123L, m_rowNumberParams.m_value, "Should parse valid long string");
    }

    @ParameterizedTest(name = "Stash from Int cell: {0} -> {1}")
    @CsvSource({
        "1, 1",
        "100, 100",
        "2147483647, 2147483647" // Integer.MAX_VALUE
    })
    void testStashFromIntToRowNumber(final int sourceValue, final long expectedValue) {
        final var intCell = new IntCell(sourceValue);
        m_rowNumberParams.applyStash(new DataValue[]{intCell});
        assertEquals(expectedValue, m_rowNumberParams.m_value,
            "Should stash int value " + sourceValue + " to row number");
    }

    @ParameterizedTest(name = "Stash from Long cell: {0} -> {1}")
    @CsvSource({
        "1, 1",
        "999999999, 999999999",
        "9223372036854775807, 9223372036854775807"
    })
    void testStashFromLongToRowNumber(final long sourceValue, final long expectedValue) {
        final var longCell = new LongCell(sourceValue);
        m_rowNumberParams.applyStash(new DataValue[]{longCell});
        assertEquals(expectedValue, m_rowNumberParams.m_value,
            "Should stash long value " + sourceValue + " to row number");
    }

    @ParameterizedTest(name = "Stash from Double cell: {0} -> {1}")
    @CsvSource({
        "1.0, 1",
        "42.7, 42",
        "100.999, 100"
    })
    void testStashFromDoubleToRowNumber(final double sourceValue, final long expectedValue) {
        final var doubleCell = new DoubleCell(sourceValue);
        m_rowNumberParams.applyStash(new DataValue[]{doubleCell});
        assertEquals(expectedValue, m_rowNumberParams.m_value,
            "Should stash double value " + sourceValue + " to row number with truncation");
    }

    @ParameterizedTest(name = "Stash from String cell: ''{0}'' -> {1}")
    @CsvSource({
        "1, 1",
        "42, 42",
        "1000000, 1000000"
    })
    void testStashFromStringToRowNumber(final String sourceValue, final long expectedValue) {
        final var stringCell = new StringCell(sourceValue);
        m_rowNumberParams.applyStash(new DataValue[]{stringCell});
        assertEquals(expectedValue, m_rowNumberParams.m_value,
            "Should parse string '" + sourceValue + "' to row number");
    }

    @Test
    void testStashFromStringToRowNumberWithInvalidString() {
        m_rowNumberParams.m_value = 100L;
        final var stringCell = new StringCell("not_a_number");
        m_rowNumberParams.applyStash(new DataValue[]{stringCell});
        assertEquals(100L, m_rowNumberParams.m_value,
            "Should keep original value when string cannot be parsed");
    }
}
