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
 *   Apr 23, 2024 (leonard.woerteler): created
 */
package org.knime.base.node.preproc.filter.row3.operators.rownumber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.base.data.filter.row.v2.FilterPartition;
import org.knime.core.node.InvalidSettingsException;

import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;

@SuppressWarnings("static-method")
final class RowNumberFilterTest {

    private static ImmutableRangeSet<Long> asSet() {
        return ImmutableRangeSet.of();
    }

    private static ImmutableRangeSet<Long> asSet(final Range<Long> range) {
        return ImmutableRangeSet.of(range);
    }

    private static ImmutableRangeSet<Long> asSet(final Range<Long> first, final Range<Long> second) {
        return ImmutableRangeSet.unionOf(List.of(first, second));
    }

    @Test
    void testSliceFromRangesEqNeq() throws InvalidSettingsException {
        // only first
        checkSymmetrical(RowNumberFilterSpec.Operator.EQ, RowNumberFilterSpec.Operator.NEQ, 1, 1_000,
            asSet(Range.closedOpen(0L, 1L)), asSet(Range.closedOpen(1L, 1_000L)));

        // only first (open)
        checkSymmetrical(RowNumberFilterSpec.Operator.EQ, RowNumberFilterSpec.Operator.NEQ, 1, -1,
            asSet(Range.closedOpen(0L, 1L)), asSet(Range.atLeast(1L)));

        // only last
        checkSymmetrical(RowNumberFilterSpec.Operator.EQ, RowNumberFilterSpec.Operator.NEQ, 1_000, 1_000,
            asSet(Range.closedOpen(999L, 1_000L)), asSet(Range.closedOpen(0L, 999L)));

        // somewhere in the middle
        checkSymmetrical(RowNumberFilterSpec.Operator.EQ, RowNumberFilterSpec.Operator.NEQ, 123, 1_000,
            asSet(Range.closedOpen(122L, 123L)), asSet(Range.closedOpen(0L, 122L), Range.closedOpen(123L, 1_000L)));

        // somewhere in the middle (open)
        checkSymmetrical(RowNumberFilterSpec.Operator.EQ, RowNumberFilterSpec.Operator.NEQ, 555, -1,
            asSet(Range.closedOpen(554L, 555L)), asSet(Range.closedOpen(0L, 554L), Range.atLeast(555L)));

        // outside
        checkSymmetrical(RowNumberFilterSpec.Operator.EQ, RowNumberFilterSpec.Operator.NEQ, 7_777, 1_000, asSet(),
            asSet(Range.closedOpen(0L, 1_000L)));

        // special value `Long.MAX_VALUE`
        checkSymmetrical(RowNumberFilterSpec.Operator.EQ, RowNumberFilterSpec.Operator.NEQ, Long.MAX_VALUE, -1,
            asSet(Range.closedOpen(Long.MAX_VALUE - 1, Long.MAX_VALUE)), //
            asSet(Range.closedOpen(0L, Long.MAX_VALUE - 1), Range.atLeast(Long.MAX_VALUE)));
        checkSymmetrical(RowNumberFilterSpec.Operator.EQ, RowNumberFilterSpec.Operator.NEQ, Long.MAX_VALUE,
            Long.MAX_VALUE, asSet(Range.closedOpen(Long.MAX_VALUE - 1, Long.MAX_VALUE)),
            asSet(Range.closedOpen(0L, Long.MAX_VALUE - 1)));
        checkSymmetrical(RowNumberFilterSpec.Operator.EQ, RowNumberFilterSpec.Operator.NEQ, 1_000, Long.MAX_VALUE,
            asSet(Range.closedOpen(999L, 1_000L)),
            asSet(Range.closedOpen(0L, 999L), Range.closedOpen(1_000L, Long.MAX_VALUE)));
    }

    @Test
    void testSliceFromRangesLtGte() throws InvalidSettingsException {
        checkSymmetrical(RowNumberFilterSpec.Operator.LT, RowNumberFilterSpec.Operator.GTE, 1, 1_000, asSet(),
            asSet(Range.closedOpen(0L, 1_000L)));

        checkSymmetrical(RowNumberFilterSpec.Operator.LT, RowNumberFilterSpec.Operator.GTE, 1, -1, asSet(),
            asSet(Range.atLeast(0L)));

        checkSymmetrical(RowNumberFilterSpec.Operator.LT, RowNumberFilterSpec.Operator.GTE, 1_000, 1_000,
            asSet(Range.closedOpen(0L, 999L)), asSet(Range.closedOpen(999L, 1_000L)));

        checkSymmetrical(RowNumberFilterSpec.Operator.LT, RowNumberFilterSpec.Operator.GTE, 123, 1_000,
            asSet(Range.closedOpen(0L, 122L)), asSet(Range.closedOpen(122L, 1_000L)));

        checkSymmetrical(RowNumberFilterSpec.Operator.LT, RowNumberFilterSpec.Operator.GTE, 555, -1,
            asSet(Range.closedOpen(0L, 554L)), asSet(Range.atLeast(554L)));

        checkSymmetrical(RowNumberFilterSpec.Operator.LT, RowNumberFilterSpec.Operator.GTE, 7_777, 1_000,
            asSet(Range.closedOpen(0L, 1000L)), asSet());

        // special value `Long.MAX_VALUE`
        checkSymmetrical(RowNumberFilterSpec.Operator.LT, RowNumberFilterSpec.Operator.GTE, Long.MAX_VALUE, -1,
            asSet(Range.closedOpen(0L, Long.MAX_VALUE - 1)), asSet(Range.atLeast(Long.MAX_VALUE - 1)));
        checkSymmetrical(RowNumberFilterSpec.Operator.LT, RowNumberFilterSpec.Operator.GTE, Long.MAX_VALUE,
            Long.MAX_VALUE, asSet(Range.closedOpen(0L, Long.MAX_VALUE - 1)),
            asSet(Range.closedOpen(Long.MAX_VALUE - 1, Long.MAX_VALUE)));
        checkSymmetrical(RowNumberFilterSpec.Operator.LT, RowNumberFilterSpec.Operator.GTE, 1_000, Long.MAX_VALUE,
            asSet(Range.closedOpen(0L, 999L)), asSet(Range.closedOpen(999L, Long.MAX_VALUE)));
    }

    @Test
    void testSliceFromRangesLteGt() throws InvalidSettingsException {
        checkSymmetrical(RowNumberFilterSpec.Operator.LTE, RowNumberFilterSpec.Operator.GT, 1, 1_000,
            asSet(Range.closedOpen(0L, 1L)), asSet(Range.closedOpen(1L, 1_000L)));

        checkSymmetrical(RowNumberFilterSpec.Operator.LTE, RowNumberFilterSpec.Operator.GT, 1, -1,
            asSet(Range.closedOpen(0L, 1L)), asSet(Range.atLeast(1L)));

        checkSymmetrical(RowNumberFilterSpec.Operator.LTE, RowNumberFilterSpec.Operator.GT, 1_000, 1_000,
            asSet(Range.closedOpen(0L, 1_000L)), asSet());

        checkSymmetrical(RowNumberFilterSpec.Operator.LTE, RowNumberFilterSpec.Operator.GT, 123, 1_000,
            asSet(Range.closedOpen(0L, 123L)), asSet(Range.closedOpen(123L, 1_000L)));

        checkSymmetrical(RowNumberFilterSpec.Operator.LTE, RowNumberFilterSpec.Operator.GT, 555, -1,
            asSet(Range.closedOpen(0L, 555L)), asSet(Range.atLeast(555L)));

        checkSymmetrical(RowNumberFilterSpec.Operator.LTE, RowNumberFilterSpec.Operator.GT, 7_777, 1_000,
            asSet(Range.closedOpen(0L, 1000L)), asSet());

        // special value `Long.MAX_VALUE`
        checkSymmetrical(RowNumberFilterSpec.Operator.LTE, RowNumberFilterSpec.Operator.GT, Long.MAX_VALUE, -1,
            asSet(Range.closedOpen(0L, Long.MAX_VALUE)), asSet(Range.atLeast(Long.MAX_VALUE)));
        checkSymmetrical(RowNumberFilterSpec.Operator.LTE, RowNumberFilterSpec.Operator.GT, Long.MAX_VALUE,
            Long.MAX_VALUE, asSet(Range.closedOpen(0L, Long.MAX_VALUE)), asSet());
        checkSymmetrical(RowNumberFilterSpec.Operator.LTE, RowNumberFilterSpec.Operator.GT, 1_000, Long.MAX_VALUE,
            asSet(Range.closedOpen(0L, 1_000L)), asSet(Range.closedOpen(1_000L, Long.MAX_VALUE)));
    }

    @Test
    void testSliceFromRangesFirstNRows() throws InvalidSettingsException {
        final var zero = computePartition(RowNumberFilterSpec.Operator.FIRST_N_ROWS, 0, 1_000);
        assertThat(zero.matching()).isEqualTo(asSet());
        assertThat(zero.nonMatching()).isEqualTo(asSet(Range.closedOpen(0L, 1_000L)));

        final var zeroOpen = computePartition(RowNumberFilterSpec.Operator.FIRST_N_ROWS, 0, -1);
        assertThat(zeroOpen.matching()).isEqualTo(asSet());
        assertThat(zeroOpen.nonMatching()).isEqualTo(asSet(Range.atLeast(0L)));

        final var one = computePartition(RowNumberFilterSpec.Operator.FIRST_N_ROWS, 1, 1_000);
        assertThat(one.matching()).isEqualTo(asSet(Range.closedOpen(0L, 1L)));
        assertThat(one.nonMatching()).isEqualTo(asSet(Range.closedOpen(1L, 1_000L)));

        final var oneOpen = computePartition(RowNumberFilterSpec.Operator.FIRST_N_ROWS, 1, -1);
        assertThat(oneOpen.matching()).isEqualTo(asSet(Range.closedOpen(0L, 1L)));
        assertThat(oneOpen.nonMatching()).isEqualTo(asSet(Range.atLeast(1L)));

        final var some = computePartition(RowNumberFilterSpec.Operator.FIRST_N_ROWS, 123, 1_000);
        assertThat(some.matching()).isEqualTo(asSet(Range.closedOpen(0L, 123L)));
        assertThat(some.nonMatching()).isEqualTo(asSet(Range.closedOpen(123L, 1_000L)));

        final var someOpen = computePartition(RowNumberFilterSpec.Operator.FIRST_N_ROWS, 555, -1);
        assertThat(someOpen.matching()).isEqualTo(asSet(Range.closedOpen(0L, 555L)));
        assertThat(someOpen.nonMatching()).isEqualTo(asSet(Range.atLeast(555L)));

        final var all = computePartition(RowNumberFilterSpec.Operator.FIRST_N_ROWS, 1_000, 1_000);
        assertThat(all.matching()).isEqualTo(asSet(Range.closedOpen(0L, 1_000L)));
        assertThat(all.nonMatching()).isEqualTo(asSet());

        final var overshooting = computePartition(RowNumberFilterSpec.Operator.FIRST_N_ROWS, 7_777, 1_000);
        assertThat(overshooting.matching()).isEqualTo(asSet(Range.closedOpen(0L, 1_000L)));
        assertThat(overshooting.nonMatching()).isEqualTo(asSet());

        // special value `Long.MAX_VALUE`

        final var maxOpen = computePartition(RowNumberFilterSpec.Operator.FIRST_N_ROWS, Long.MAX_VALUE, -1);
        assertThat(maxOpen.matching()).isEqualTo(asSet(Range.closedOpen(0L, Long.MAX_VALUE)));
        assertThat(maxOpen.nonMatching()).isEqualTo(asSet(Range.atLeast(Long.MAX_VALUE)));

        final var max = computePartition(RowNumberFilterSpec.Operator.FIRST_N_ROWS, Long.MAX_VALUE, Long.MAX_VALUE);
        assertThat(max.matching()).isEqualTo(asSet(Range.closedOpen(0L, Long.MAX_VALUE)));
        assertThat(max.nonMatching()).isEqualTo(asSet());

        final var lenMax = computePartition(RowNumberFilterSpec.Operator.FIRST_N_ROWS, 1_000, Long.MAX_VALUE);
        assertThat(lenMax.matching()).isEqualTo(asSet(Range.closedOpen(0L, 1_000L)));
        assertThat(lenMax.nonMatching()).isEqualTo(asSet(Range.closedOpen(1_000L, Long.MAX_VALUE)));
    }

    @Test
    void testSliceFromRangesLastNRows() throws InvalidSettingsException {
        final var zero = computePartition(RowNumberFilterSpec.Operator.LAST_N_ROWS, 0, 1_000);
        assertThat(zero.matching()).isEqualTo(asSet());
        assertThat(zero.nonMatching()).isEqualTo(asSet(Range.closedOpen(0L, 1_000L)));

        assertThat(assertThrows(IllegalStateException.class,
            () -> computePartition(RowNumberFilterSpec.Operator.LAST_N_ROWS, 0, -1), "LAST_N_ROWS needs table size")
                .getMessage()).startsWith("Expected table size");

        final var one = computePartition(RowNumberFilterSpec.Operator.LAST_N_ROWS, 1, 1_000);
        assertThat(one.matching()).isEqualTo(asSet(Range.closedOpen(999L, 1_000L)));
        assertThat(one.nonMatching()).isEqualTo(asSet(Range.closedOpen(0L, 999L)));

        assertThat(assertThrows(IllegalStateException.class,
            () -> computePartition(RowNumberFilterSpec.Operator.LAST_N_ROWS, 1, -1), "LAST_N_ROWS needs table size")
                .getMessage()).startsWith("Expected table size");

        final var some = computePartition(RowNumberFilterSpec.Operator.LAST_N_ROWS, 123, 1_000);
        assertThat(some.matching()).isEqualTo(asSet(Range.closedOpen(877L, 1_000L)));
        assertThat(some.nonMatching()).isEqualTo(asSet(Range.closedOpen(0L, 877L)));

        assertThat(assertThrows(IllegalStateException.class,
            () -> computePartition(RowNumberFilterSpec.Operator.LAST_N_ROWS, 555, -1), "LAST_N_ROWS needs table size")
                .getMessage()).startsWith("Expected table size");

        final var all = computePartition(RowNumberFilterSpec.Operator.LAST_N_ROWS, 1_000, 1_000);
        assertThat(all.matching()).isEqualTo(asSet(Range.closedOpen(0L, 1_000L)));
        assertThat(all.nonMatching()).isEqualTo(asSet());

        final var overshooting = computePartition(RowNumberFilterSpec.Operator.LAST_N_ROWS, 7_777, 1_000);
        assertThat(overshooting.matching()).isEqualTo(asSet(Range.closedOpen(0L, 1_000L)));
        assertThat(overshooting.nonMatching()).isEqualTo(asSet());

        // special value `Long.MAX_VALUE`

        assertThat(assertThrows(IllegalStateException.class,
            () -> computePartition(RowNumberFilterSpec.Operator.LAST_N_ROWS, Long.MAX_VALUE, -1),
            "LAST_N_ROWS needs table size").getMessage()).startsWith("Expected table size");

        final var max = computePartition(RowNumberFilterSpec.Operator.LAST_N_ROWS, Long.MAX_VALUE, Long.MAX_VALUE);
        assertThat(max.matching()).isEqualTo(asSet(Range.closedOpen(0L, Long.MAX_VALUE)));
        assertThat(max.nonMatching()).isEqualTo(asSet());

        final var lenMax = computePartition(RowNumberFilterSpec.Operator.LAST_N_ROWS, 1_000, Long.MAX_VALUE);
        assertThat(lenMax.matching()).isEqualTo(asSet(Range.closedOpen(Long.MAX_VALUE - 1_000, Long.MAX_VALUE)));
        assertThat(lenMax.nonMatching()).isEqualTo(asSet(Range.closedOpen(0L, Long.MAX_VALUE - 1_000)));
    }

    private static FilterPartition computePartition(final RowNumberFilterSpec.Operator operator, final long rowNumber,
        final long optSize) throws InvalidSettingsException {
        return FilterPartition.computePartition(new RowNumberFilterSpec(operator, rowNumber).toOffsetFilter(optSize),
            optSize);
    }

    private static void checkSymmetrical(final RowNumberFilterSpec.Operator op1, final RowNumberFilterSpec.Operator op2,
        final long value, final long optSize, final RangeSet<Long> included, final RangeSet<Long> excluded)
        throws InvalidSettingsException {
        final var partition1 = computePartition(op1, value, optSize);
        final var partition2 = computePartition(op2, value, optSize);
        assertThat(partition1.matching()).isEqualTo(included);
        assertThat(partition1.nonMatching()).isEqualTo(excluded);
        assertThat(partition2.matching()).isEqualTo(excluded);
        assertThat(partition2.nonMatching()).isEqualTo(included);
    }
}
