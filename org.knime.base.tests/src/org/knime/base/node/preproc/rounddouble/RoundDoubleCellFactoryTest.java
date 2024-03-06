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
 *   Feb 29, 2024 (kai): created
 */
package org.knime.base.node.preproc.rounddouble;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.RoundingMode;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.NumberMode;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.OutputMode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * Test rounding with various settings.
 *
 * @author Kai Franze, KNIME GmbH, Germany
 */
@SuppressWarnings("static-method") // Cannot make test static because they would be ignored
final class RoundDoubleCellFactoryTest {

    /**
     * Tests decimal rounding
     */
    @Test
    void testDecimalRounding() throws Exception {
        final var row = getDataRow("foo bar", 123000.055555, 1000, 2000L);
        final var settings =
            new Settings(2, NumberMode.DECIMALS, RoundingMode.HALF_UP, OutputMode.AUTO, new int[]{1, 2, 3});
        final var cells = roundCellsInRow(settings, row);

        // Assert the Double cell
        assertThat(cells[0].getType()).as("Check double cell type").isEqualTo(DoubleCell.TYPE);
        assertThat(((DoubleCell)cells[0]).getDoubleValue()).as("Check double cell value").isEqualTo(123000.06);

        // Assert the Integer cell
        assertThat(cells[1].getType()).as("Check integer cell type").isEqualTo(IntCell.TYPE);
        assertThat(((IntCell)cells[1]).getIntValue()).as("Check integer cell value").isEqualTo(1000);

        // Assert the Long cell
        assertThat(cells[2].getType()).as("Check long cell type").isEqualTo(LongCell.TYPE);
        assertThat(((LongCell)cells[2]).getLongValue()).as("Check long cell value").isEqualTo(2000L);
    }

    /**
     * Tests significant digits rounding
     */
    @Test
    void testSignificantDigitRounding() throws Exception {
        final var row = getDataRow("foo bar", 0.00111222333, 1234, 999999L);
        final var settings =
            new Settings(1, NumberMode.SIGNIFICANT_DIGITS, RoundingMode.HALF_UP, OutputMode.AUTO, new int[]{1, 2, 3});
        final var cells = roundCellsInRow(settings, row);

        // Assert the Double cell
        assertThat(cells[0].getType()).as("Check double cell type").isEqualTo(DoubleCell.TYPE);
        assertThat(((DoubleCell)cells[0]).getDoubleValue()).as("Check double cell value").isEqualTo(0.001);

        // Assert the Integer cell
        assertThat(cells[1].getType()).as("Check integer cell type").isEqualTo(IntCell.TYPE);
        assertThat(((IntCell)cells[1]).getIntValue()).as("Check integer cell value").isEqualTo(1000);

        // Assert the Long cell
        assertThat(cells[2].getType()).as("Check long cell type").isEqualTo(LongCell.TYPE);
        assertThat(((LongCell)cells[2]).getLongValue()).as("Check long cell value").isEqualTo(1000000L);
    }

    /**
     * Tests that we get a missing cell for integer / long overflows when rounding
     */
    @Test
    void testIntegerLongOverflow() throws Exception {
        // Integer.MAX_VALUE 2,147,483,647
        // if NumberMode.SIGNIFICANT_DIGITS and precision == 5, we get 2,147,500,000
        // Long.MIN_VALUE -9,223,372,036,854,775,808
        // if NumberMode.SIGNIFICANT_DIGITS and precision == 5, we get -9,223,400,000,000,000,000
        final var row = getDataRow("", 0d, Integer.MAX_VALUE, Long.MIN_VALUE);
        final var settings =
            new Settings(5, NumberMode.SIGNIFICANT_DIGITS, RoundingMode.HALF_UP, OutputMode.AUTO, new int[]{2, 3});
        final var cells = roundCellsInRow(settings, row);

        assertThat(cells[0].isMissing()).as("Check integer overflow leads to missing cell").isTrue();
        assertThat(cells[1].isMissing()).as("Check long overflow leads to missing cell").isTrue();
    }

    /**
     * Tests significant digits rounding
     */
    @Test
    void testIntegerRounding() throws Exception {
        final var row = getDataRow("foo bar", 555.6, 1234, 999999L);
        final var settings =
            new Settings(1, NumberMode.INTEGER, RoundingMode.HALF_UP, OutputMode.AUTO, new int[]{1, 3});
        final var cells = roundCellsInRow(settings, row);

        // Assert types
        assertThat(cells[0].getType()).as("Type must be IntCell.TYPE").isEqualTo(IntCell.TYPE);
        assertThat(cells[1].getType()).as("Type must be IntCell.TYPE").isEqualTo(IntCell.TYPE);

        // Assert values
        assertThat(((IntCell)cells[0]).getIntValue()).as("Check double cell value").isEqualTo(556);
        assertThat(((IntCell)cells[1]).getIntValue()).as("Check long cell value").isEqualTo(999999);
    }

    /**
     * Tests the various output modes
     *
     * @throws Exception
     */
    @Test
    void testOutputModes() throws Exception {
        final var row = getDataRow("foo bar", 0.00000035239, 0, 0L);

        // Standard string
        final var settings1 = new Settings(3, NumberMode.SIGNIFICANT_DIGITS, RoundingMode.HALF_UP,
            OutputMode.STANDARD_STRING, new int[]{1});
        final var cell1 = roundCellsInRow(settings1, row)[0];
        assertThat(cell1.getType()).as("Type must be StringCell.TYPE").isEqualTo(StringCell.TYPE);
        assertThat(((StringCell)cell1).getStringValue()).as("Check the standard string").isEqualTo("3.52E-7");

        // Plain string
        final var settings2 =
            new Settings(3, NumberMode.SIGNIFICANT_DIGITS, RoundingMode.HALF_UP, OutputMode.PLAIN_STRING, new int[]{1});
        final var cell2 = roundCellsInRow(settings2, row)[0];
        assertThat(cell2.getType()).as("Type must be StringCell.TYPE").isEqualTo(StringCell.TYPE);
        assertThat(((StringCell)cell2).getStringValue()).as("Check the standard string").isEqualTo("0.000000352");

        // Engineering string
        final var settings3 = new Settings(3, NumberMode.SIGNIFICANT_DIGITS, RoundingMode.HALF_UP,
            OutputMode.ENGINEERING_STRING, new int[]{1});
        final var cell3 = roundCellsInRow(settings3, row)[0];
        assertThat(cell3.getType()).as("Type must be StringCell.TYPE").isEqualTo(StringCell.TYPE);
        assertThat(((StringCell)cell3).getStringValue()).as("Check the standard string").isEqualTo("352E-9");

        // Double
        final var settings4 = new Settings(1, NumberMode.DECIMALS, RoundingMode.HALF_UP,
            OutputMode.DOUBLE, new int[]{1, 2, 3});
        final var cells = roundCellsInRow(settings4, row);
        assertThat(cells[0].getType()).as("Type must be DoubleCell.TYPE").isEqualTo(DoubleCell.TYPE);
        assertThat(cells[1].getType()).as("Type must be DoubleCell.TYPE").isEqualTo(DoubleCell.TYPE);
        assertThat(cells[2].getType()).as("Type must be DoubleCell.TYPE").isEqualTo(DoubleCell.TYPE);
    }

    /**
     * Tests the various expected exceptions being thrown
     */
    @Test
    void testExceptions() throws Exception {
        final var row = getDataRow("foo bar", 0.0, 0, 0L);

        // No rounding mode
        final var settings1 = new Settings(3, NumberMode.DECIMALS, null, OutputMode.AUTO, new int[]{1, 2, 3});
        assertThatThrownBy(() -> roundCellsInRow(settings1, row)).isInstanceOf(IllegalArgumentException.class);

        // No column indices to round
        final var settings2 = new Settings(3, NumberMode.DECIMALS, RoundingMode.HALF_UP, OutputMode.AUTO, null);
        assertThatThrownBy(() -> roundCellsInRow(settings2, row)).isInstanceOf(IllegalArgumentException.class);

        // No number mode set
        final var settings3 = new Settings(3, null, RoundingMode.HALF_UP, OutputMode.AUTO, new int[]{1, 2, 3});
        assertThatThrownBy(() -> roundCellsInRow(settings3, row)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Tests how it handles missing cells
     */
    @Test
    void testMissingCells() throws Exception {
        final var row = getDataRow("foo bar", 123.455, null, null);
        final var settings = new Settings(2, NumberMode.DECIMALS, RoundingMode.HALF_DOWN, OutputMode.AUTO, new int[]{1, 2, 3});
        final var cells = roundCellsInRow(settings, row);

        // Assert the Double cell
        assertThat(cells[0].getType()).as("Check double cell type").isEqualTo(DoubleCell.TYPE);
        assertThat(((DoubleCell)cells[0]).getDoubleValue()).as("Check double cell value").isEqualTo(123.45);

        // Assert missing cells
        assertThat(cells[1].isMissing()).as("Check for missing type").isTrue();
        assertThat(cells[2].isMissing()).as("Check for missing type").isTrue();
    }

    /**
     * Test for NaN and infinite double input values
     */
    @Test
    void testInvalidDoubleInputValues() throws Exception {
        // Test for infinity
        final var row1 = getDataRow("foo bar", Double.valueOf(1.0 / 0.0), null, null);
        final var settings11 =
            new Settings(1, NumberMode.DECIMALS, RoundingMode.HALF_UP, OutputMode.AUTO, new int[]{1});
        final var cell11 = roundCellsInRow(settings11, row1)[0];
        assertThat(cell11.getType()).as("Check double cell type").isEqualTo(DoubleCell.TYPE);
        assertThat(((DoubleCell)cell11).getDoubleValue()) //
            .as("Check for inifinity") //
            .is(new Condition<Double>(x -> x.isInfinite(), "Check for infinity"));

        final var settings12 =
            new Settings(1, NumberMode.DECIMALS, RoundingMode.HALF_UP, OutputMode.STANDARD_STRING, new int[]{1});
        final var cell12 = roundCellsInRow(settings12, row1)[0];
        assertThat(cell12.getType()).as("Check double cell type").isEqualTo(StringCell.TYPE);
        assertThat(((StringCell)cell12).getStringValue()).as("Check for inifinity").isEqualTo("Infinity");

        final var settings13 =
            new Settings(1, NumberMode.DECIMALS, RoundingMode.HALF_UP, OutputMode.DOUBLE, new int[]{1});
        final var cell13 = roundCellsInRow(settings13, row1)[0];
        assertThat(cell13.getType()).as("Check double cell type").isEqualTo(DoubleCell.TYPE);
        assertThat(((DoubleCell)cell13).getDoubleValue()) //
            .as("Check for inifinity") //
            .is(new Condition<Double>(x -> x.isInfinite(), "Check for infinity"));

        // Test for not a number
        final var row2 =
            getDataRow("foo bar", Double.valueOf(Double.POSITIVE_INFINITY + Double.NEGATIVE_INFINITY), null, null);
        final var settings21 =
            new Settings(1, NumberMode.DECIMALS, RoundingMode.HALF_UP, OutputMode.AUTO, new int[]{1});
        final var cell21 = roundCellsInRow(settings21, row2)[0];
        assertThat(cell21.getType()).as("Check double cell type").isEqualTo(DoubleCell.TYPE);
        assertThat(((DoubleCell)cell21).getDoubleValue()) //
            .as("Check for not a number") //
            .is(new Condition<Double>(x -> x.isNaN(), "Check for not a number"));
    }

    private static DataCell[] roundCellsInRow(final Settings settings, final DataRow row) {
        final var columnRearranger = new RoundDoubleCellFactory(settings.precision, settings.numberMode,
            settings.roundingMode, settings.outputMode, settings.colIndexToRound, new DataColumnSpec[]{});
        return columnRearranger.getCells(row);
    }

    private static DataRow getDataRow(final String stringValue, final Double doubleValue, final Integer intValue,
        final Long longValue) {
        return new DefaultRow("Row0", //
            StringCell.StringCellFactory.create(stringValue), //
            doubleValue == null ? DataType.getMissingCell() : DoubleCell.DoubleCellFactory.create(doubleValue), //
            intValue == null ? DataType.getMissingCell() : IntCell.IntCellFactory.create(intValue), //
            longValue == null ? DataType.getMissingCell() : LongCell.LongCellFactory.create(longValue));
    }

    private static record Settings(int precision, NumberMode numberMode, RoundingMode roundingMode,
        OutputMode outputMode, int[] colIndexToRound) {
        //
    }

}
