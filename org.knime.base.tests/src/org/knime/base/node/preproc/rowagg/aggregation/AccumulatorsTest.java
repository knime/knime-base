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
 *   23 Jan 2023 (manuelhotz): created
 */
package org.knime.base.node.preproc.rowagg.aggregation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * Tests for the accumulators.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class AccumulatorsTest {


    private static final Double NAN = Double.NaN;
    private static final Double POS_INFINITY = Double.POSITIVE_INFINITY;
    private static final Double NEG_INFINITY = Double.NEGATIVE_INFINITY;

    private static final String ERR_SHORT_CIRCUIT = "Erroneous short-circuit";
    private static final String ERR_SUM = "Incorrect sum";
    private static final String ERR_SKIP = "Erroneously skipping column";


    @SuppressWarnings("static-method")
    @Test
    void testAverage() {
        // test "failure" conditions
        assertThrows(IllegalArgumentException.class, () -> new Average<>(StringCell.TYPE));
        final var avg = new Average<DoubleValue>(DoubleCell.TYPE);
        assertEquals(DoubleCell.TYPE, avg.getResultType(), "Average result type should be Double");
        assertEquals(0, avg.getResult().get().getDoubleValue(), "Initial average should be 0");
        assertNotEquals(Optional.empty(), avg.getResult(), "Average result should never be empty");

        // AVG(+inf, +inf) -> NaN
        avg.reset();
        assertAverage(avg, new boolean[] {false, true}, POS_INFINITY, POS_INFINITY);

        // test reset
        avg.reset();
        assertEquals(0, avg.getResult().get().getDoubleValue(), "Average after reset should be 0");

        // AVG(-inf, -inf) -> NaN
        avg.reset();
        assertAverage(avg, new boolean[] {false, true}, NEG_INFINITY, NEG_INFINITY);

        // AVG(+inf, -inf) -> NaN
        avg.reset();
        assertAverage(avg, new boolean[] {false, true}, POS_INFINITY, NEG_INFINITY);

        // AVG(x, NaN) -> NaN
        avg.reset();
        assertAverage(avg, new boolean[] {false, false, true, true}, 42, POS_INFINITY, NAN, 42);

        // test normal operation
        avg.reset();
        final var rng = new Random(42);
        final var ds = rng.doubles(100, -100, 100).toArray();
        Arrays.stream(ds).mapToObj(DoubleCell::new).forEach(avg::apply);
        final var mean = new Mean();
        mean.incrementAll(ds);
        assertEquals(mean.getResult(), avg.getResult().get().getDoubleValue(), "Incorrect average result");
    }

    private static void assertAverage(final Average<DoubleValue> avg, final boolean[] expectedApplyResults,
        final double... values) {
        // current implementation used in MeanOperator
        final var mean = new Mean();
        mean.incrementAll(values);
        for (int i = 0; i < values.length; i++) {
            final var stop = expectedApplyResults[i];
            final var cell = new DoubleCell(values[i]);
            if (stop) {
                assertTrue(avg.apply(cell), ERR_SHORT_CIRCUIT);
            } else {
                assertFalse(avg.apply(cell), ERR_SHORT_CIRCUIT);
            }
        }
        assertFalse(avg.getResult().isEmpty(), "Double result should never be empty");
        assertEquals(mean.getResult(), avg.getResult().get().getDoubleValue(), "Wrong average result");
    }

    @SuppressWarnings("static-method")
    @Test
    void testSum() {
        // test "failure" conditions and overflow behavior
        assertThrows(IllegalArgumentException.class, () -> new Sum<>(StringCell.TYPE));

        final var iSum = new Sum<IntValue>(IntCell.TYPE);
        assertEquals(IntCell.TYPE, iSum.getResultType(), "Incorrect result type");
        assertEquals(0, iSum.getResult().get().getIntValue(), "Incorrect initial value");
        iSum.reset();
        assertEquals(0, iSum.getResult().get().getIntValue(), "Incorrect value after reset");
        assertFalse(iSum.apply(new IntCell(Integer.MAX_VALUE)), ERR_SKIP);
        assertEquals(Integer.MAX_VALUE, iSum.getResult().get().getIntValue(), ERR_SUM);
        // accumulator type for int is long so this should work...
        assertFalse(iSum.apply(new IntCell(1)), "Should not have indicated overflow");
        // ... but result still does not fit
        assertEquals(Optional.empty(), iSum.getResult(), "Incorrect result after result overflow");


        final var lSum = new Sum<LongValue>(LongCell.TYPE);
        assertEquals(LongCell.TYPE, lSum.getResultType(), "Incorrect result type");
        assertEquals(0, lSum.getResult().get().getLongValue(), "Incorrect initial value");
        lSum.reset();
        assertEquals(0, lSum.getResult().get().getLongValue(), "Incorrect value after reset");
        assertFalse(lSum.apply(new LongCell(Long.MAX_VALUE)), ERR_SKIP);
        assertEquals(Long.MAX_VALUE, lSum.getResult().get().getLongValue(), "Incorrect result");
        assertTrue(lSum.apply(new LongCell(1)), ERR_SKIP);
        assertEquals(Optional.empty(), lSum.getResult(), "Incorrect result after result overflow");


        final var dSum = new Sum<DoubleValue>(DoubleCell.TYPE);
        assertEquals(DoubleCell.TYPE, dSum.getResultType(), "Incorrect result type");
        assertEquals(0, dSum.getResult().get().getDoubleValue(), "Incorrect initial value");
        dSum.reset();
        assertEquals(0, dSum.getResult().get().getDoubleValue(), "Incorrect value after reset");
        assertFalse(dSum.apply(new DoubleCell(POS_INFINITY)), ERR_SKIP);
        assertEquals(POS_INFINITY, dSum.getResult().get().getDoubleValue(), "Incorrect result");
        assertFalse(dSum.apply(new DoubleCell(1)), ERR_SKIP);
        assertEquals(POS_INFINITY, dSum.getResult().get().getDoubleValue(), "Incorrect result after result overflow");


        // test normal operation
        iSum.reset();
        lSum.reset();
        dSum.reset();
        final var rng = new Random(42);
        final var ds = rng.ints(100, -100, 100).toArray();
        final var ref = new org.apache.commons.math3.stat.descriptive.summary.Sum();
        Arrays.stream(ds).forEach(ref::increment);
        final var expected = ref.getResult();

        Arrays.stream(ds).mapToObj(IntCell::new).forEach(iSum::apply);
        final var iRes = iSum.getResult().get().getIntValue();
        assertEquals(expected, iRes, "Incorrect int sum");
        Arrays.stream(ds).mapToObj(LongCell::new).forEach(lSum::apply);
        final var lRes = lSum.getResult().get().getLongValue();
        assertEquals(expected, lRes, "Incorrect long sum");
        Arrays.stream(ds).mapToObj(DoubleCell::new).forEach(dSum::apply);
        final var dRes = dSum.getResult().get().getDoubleValue();
        assertEquals(expected, dRes, "Incorrect double sum");
    }

    @SuppressWarnings("static-method")
    @Test
    void testMultiplyTypes() {

        assertThrows(IllegalArgumentException.class, () -> new Multiply<>(StringCell.TYPE, IntCell.TYPE),
            "Supports only numeric cells");
        assertThrows(IllegalArgumentException.class, () -> new Multiply<>(IntCell.TYPE, StringCell.TYPE),
            "Supports only numeric cells");

        final var types = new DataType[][] {
            new DataType[] { IntCell.TYPE, IntCell.TYPE, IntCell.TYPE },
            new DataType[] { IntCell.TYPE, LongCell.TYPE, LongCell.TYPE },
            new DataType[] { IntCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE },

            new DataType[] { LongCell.TYPE, IntCell.TYPE, LongCell.TYPE },
            new DataType[] { LongCell.TYPE, LongCell.TYPE, LongCell.TYPE },
            new DataType[] { LongCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE },

            new DataType[] { DoubleCell.TYPE, IntCell.TYPE, DoubleCell.TYPE },
            new DataType[] { DoubleCell.TYPE, LongCell.TYPE, DoubleCell.TYPE },
            new DataType[] { DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE },
        };

        for (final var pair : types) {
            final var op = assertDoesNotThrow(() -> new Multiply<>(pair[0], pair[1]));
            assertEquals(pair[2], op.getResultDataType(),
                () -> String.format("Incorrect result type %s for input types %s and %s", pair[2], pair[0], pair[1]));
        }
    }
}
