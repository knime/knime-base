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
package org.knime.base.node.preproc.filter.row3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.filter.row3.RowFilterNodeModel.RowNumberFilter;
import org.knime.base.node.preproc.filter.row3.RowFilterNodeModel.RowRange;

@SuppressWarnings("static-method")
final class RowNumberFilterTest {

    @Test
    void testSliceFromRangesEqNeq() {
        // only first
        checkSymmetrical(FilterOperator.EQ, FilterOperator.NEQ, 1, 1_000,
            List.of(new RowRange(0, 1)),
            List.of(new RowRange(1, 1_000)));

        // only first (open)
        checkSymmetrical(FilterOperator.EQ, FilterOperator.NEQ, 1, -1,
            List.of(new RowRange(0, 1)),
            List.of(new RowRange(1, -1)));

        // only last
        checkSymmetrical(FilterOperator.EQ, FilterOperator.NEQ, 1_000, 1_000,
            List.of(new RowRange(999, 1_000)),
            List.of(new RowRange(0, 999)));

        // somewhere in the middle
        checkSymmetrical(FilterOperator.EQ, FilterOperator.NEQ, 123, 1_000,
            List.of(new RowRange(122, 123)),
            List.of(new RowRange(0, 122), new RowRange(123, 1_000)));

        // somewhere in the middle (open)
        checkSymmetrical(FilterOperator.EQ, FilterOperator.NEQ, 555, -1,
            List.of(new RowRange(554, 555)),
            List.of(new RowRange(0, 554), new RowRange(555, -1)));

        // outside
        checkSymmetrical(FilterOperator.EQ, FilterOperator.NEQ, 7_777, 1_000,
            List.of(),
            List.of(new RowRange(0, 1_000)));

        // special value `Long.MAX_VALUE`
        checkSymmetrical(FilterOperator.EQ, FilterOperator.NEQ, Long.MAX_VALUE, -1,
            List.of(new RowRange(Long.MAX_VALUE - 1, Long.MAX_VALUE)),
            List.of(new RowRange(0, Long.MAX_VALUE - 1), new RowRange(Long.MAX_VALUE, -1)));
        checkSymmetrical(FilterOperator.EQ, FilterOperator.NEQ, Long.MAX_VALUE, Long.MAX_VALUE,
            List.of(new RowRange(Long.MAX_VALUE - 1, Long.MAX_VALUE)),
            List.of(new RowRange(0, Long.MAX_VALUE - 1)));
        checkSymmetrical(FilterOperator.EQ, FilterOperator.NEQ, 1_000, Long.MAX_VALUE,
            List.of(new RowRange(999, 1_000)),
            List.of(new RowRange(0, 999), new RowRange(1_000, Long.MAX_VALUE)));
    }

    @Test
    void testSliceFromRangesLtGte() {
        checkSymmetrical(FilterOperator.LT, FilterOperator.GTE, 1, 1_000,
            List.of(),
            List.of(new RowRange(0, 1_000)));

        checkSymmetrical(FilterOperator.LT, FilterOperator.GTE, 1, -1,
            List.of(),
            List.of(new RowRange(0, -1)));

        checkSymmetrical(FilterOperator.LT, FilterOperator.GTE, 1_000, 1_000,
            List.of(new RowRange(0, 999)),
            List.of(new RowRange(999, 1_000)));

        checkSymmetrical(FilterOperator.LT, FilterOperator.GTE, 123, 1_000,
            List.of(new RowRange(0, 122)),
            List.of(new RowRange(122, 1_000)));

        checkSymmetrical(FilterOperator.LT, FilterOperator.GTE, 555, -1,
            List.of(new RowRange(0, 554)),
            List.of(new RowRange(554, -1)));

        checkSymmetrical(FilterOperator.LT, FilterOperator.GTE, 7_777, 1_000,
            List.of(new RowRange(0, 1000)),
            List.of());

        // special value `Long.MAX_VALUE`
        checkSymmetrical(FilterOperator.LT, FilterOperator.GTE, Long.MAX_VALUE, -1,
            List.of(new RowRange(0, Long.MAX_VALUE - 1)),
            List.of(new RowRange(Long.MAX_VALUE -1, -1)));
        checkSymmetrical(FilterOperator.LT, FilterOperator.GTE, Long.MAX_VALUE, Long.MAX_VALUE,
            List.of(new RowRange(0, Long.MAX_VALUE - 1)),
            List.of(new RowRange(Long.MAX_VALUE - 1, Long.MAX_VALUE)));
        checkSymmetrical(FilterOperator.LT, FilterOperator.GTE, 1_000, Long.MAX_VALUE,
            List.of(new RowRange(0, 999)),
            List.of(new RowRange(999, Long.MAX_VALUE)));
    }

    @Test
    void testSliceFromRangesLteGt() {
        checkSymmetrical(FilterOperator.LTE, FilterOperator.GT, 1, 1_000,
            List.of(new RowRange(0, 1)),
            List.of(new RowRange(1, 1_000)));

        checkSymmetrical(FilterOperator.LTE, FilterOperator.GT, 1, -1,
            List.of(new RowRange(0, 1)),
            List.of(new RowRange(1, -1)));

        checkSymmetrical(FilterOperator.LTE, FilterOperator.GT, 1_000, 1_000,
            List.of(new RowRange(0, 1_000)),
            List.of());

        checkSymmetrical(FilterOperator.LTE, FilterOperator.GT, 123, 1_000,
            List.of(new RowRange(0, 123)),
            List.of(new RowRange(123, 1_000)));

        checkSymmetrical(FilterOperator.LTE, FilterOperator.GT, 555, -1,
            List.of(new RowRange(0, 555)),
            List.of(new RowRange(555, -1)));

        checkSymmetrical(FilterOperator.LTE, FilterOperator.GT, 7_777, 1_000,
            List.of(new RowRange(0, 1000)),
            List.of());

        // special value `Long.MAX_VALUE`
        checkSymmetrical(FilterOperator.LTE, FilterOperator.GT, Long.MAX_VALUE, -1,
            List.of(new RowRange(0, Long.MAX_VALUE)),
            List.of(new RowRange(Long.MAX_VALUE, -1)));
        checkSymmetrical(FilterOperator.LTE, FilterOperator.GT, Long.MAX_VALUE, Long.MAX_VALUE,
            List.of(new RowRange(0, Long.MAX_VALUE)),
            List.of());
        checkSymmetrical(FilterOperator.LTE, FilterOperator.GT, 1_000, Long.MAX_VALUE,
            List.of(new RowRange(0, 1_000)),
            List.of(new RowRange(1_000, Long.MAX_VALUE)));
    }

    @Test
    void testSliceFromRangesFirstNRows() {
        final var zero = RowNumberFilter.computePartition(FilterOperator.FIRST_N_ROWS, 0, 1_000);
        assertThat(zero.getFirst(), emptyArray());
        assertThat(zero.getSecond(), arrayContaining(new RowRange(0, 1_000)));

        final var zeroOpen = RowNumberFilter.computePartition(FilterOperator.FIRST_N_ROWS, 0, -1);
        assertThat(zeroOpen.getFirst(), emptyArray());
        assertThat(zeroOpen.getSecond(), arrayContaining(new RowRange(0, -1)));

        final var one = RowNumberFilter.computePartition(FilterOperator.FIRST_N_ROWS, 1, 1_000);
        assertThat(one.getFirst(), arrayContaining(new RowRange(0, 1)));
        assertThat(one.getSecond(), arrayContaining(new RowRange(1, 1_000)));

        final var oneOpen = RowNumberFilter.computePartition(FilterOperator.FIRST_N_ROWS, 1, -1);
        assertThat(oneOpen.getFirst(), arrayContaining(new RowRange(0, 1)));
        assertThat(oneOpen.getSecond(), arrayContaining(new RowRange(1, -1)));

        final var some = RowNumberFilter.computePartition(FilterOperator.FIRST_N_ROWS, 123, 1_000);
        assertThat(some.getFirst(), arrayContaining(new RowRange(0, 123)));
        assertThat(some.getSecond(), arrayContaining(new RowRange(123, 1_000)));

        final var someOpen = RowNumberFilter.computePartition(FilterOperator.FIRST_N_ROWS, 555, -1);
        assertThat(someOpen.getFirst(), arrayContaining(new RowRange(0, 555)));
        assertThat(someOpen.getSecond(), arrayContaining(new RowRange(555, -1)));

        final var all = RowNumberFilter.computePartition(FilterOperator.FIRST_N_ROWS, 1_000, 1_000);
        assertThat(all.getFirst(), arrayContaining(new RowRange(0, 1_000)));
        assertThat(all.getSecond(), emptyArray());

        final var overshooting = RowNumberFilter.computePartition(FilterOperator.FIRST_N_ROWS, 7_777, 1_000);
        assertThat(overshooting.getFirst(), arrayContaining(new RowRange(0, 1_000)));
        assertThat(overshooting.getSecond(), emptyArray());

        // special value `Long.MAX_VALUE`

        final var maxOpen = RowNumberFilter.computePartition(FilterOperator.FIRST_N_ROWS, Long.MAX_VALUE, -1);
        assertThat(maxOpen.getFirst(), arrayContaining(new RowRange(0, Long.MAX_VALUE)));
        assertThat(maxOpen.getSecond(), arrayContaining(new RowRange(Long.MAX_VALUE, -1)));

        final var max = RowNumberFilter.computePartition(FilterOperator.FIRST_N_ROWS, Long.MAX_VALUE, Long.MAX_VALUE);
        assertThat(max.getFirst(), arrayContaining(new RowRange(0, Long.MAX_VALUE)));
        assertThat(max.getSecond(), emptyArray());

        final var lenMax = RowNumberFilter.computePartition(FilterOperator.FIRST_N_ROWS, 1_000, Long.MAX_VALUE);
        assertThat(lenMax.getFirst(), arrayContaining(new RowRange(0, 1_000)));
        assertThat(lenMax.getSecond(), arrayContaining(new RowRange(1_000, Long.MAX_VALUE)));
    }

    @Test
    void testSliceFromRangesLastNRows() {
        final var zero = RowNumberFilter.computePartition(FilterOperator.LAST_N_ROWS, 0, 1_000);
        assertThat(zero.getFirst(), emptyArray());
        assertThat(zero.getSecond(), arrayContaining(new RowRange(0, 1_000)));

        assertThat(assertThrows(IllegalStateException.class,
                () -> RowNumberFilter.computePartition(FilterOperator.LAST_N_ROWS, 0, -1),
                "LAST_N_ROWS needs table size").getMessage(),
            startsWith("Expected table size"));

        final var one = RowNumberFilter.computePartition(FilterOperator.LAST_N_ROWS, 1, 1_000);
        assertThat(one.getFirst(), arrayContaining(new RowRange(999, 1_000)));
        assertThat(one.getSecond(), arrayContaining(new RowRange(0, 999)));

        assertThat(assertThrows(IllegalStateException.class,
                () -> RowNumberFilter.computePartition(FilterOperator.LAST_N_ROWS, 1, -1),
                "LAST_N_ROWS needs table size").getMessage(),
            startsWith("Expected table size"));

        final var some = RowNumberFilter.computePartition(FilterOperator.LAST_N_ROWS, 123, 1_000);
        assertThat(some.getFirst(), arrayContaining(new RowRange(877, 1_000)));
        assertThat(some.getSecond(), arrayContaining(new RowRange(0, 877)));

        assertThat(assertThrows(IllegalStateException.class,
                () -> RowNumberFilter.computePartition(FilterOperator.LAST_N_ROWS, 555, -1),
                "LAST_N_ROWS needs table size").getMessage(),
            startsWith("Expected table size"));

        final var all = RowNumberFilter.computePartition(FilterOperator.LAST_N_ROWS, 1_000, 1_000);
        assertThat(all.getFirst(), arrayContaining(new RowRange(0, 1_000)));
        assertThat(all.getSecond(), emptyArray());

        final var overshooting = RowNumberFilter.computePartition(FilterOperator.LAST_N_ROWS, 7_777, 1_000);
        assertThat(overshooting.getFirst(), arrayContaining(new RowRange(0, 1_000)));
        assertThat(overshooting.getSecond(), emptyArray());

        // special value `Long.MAX_VALUE`

        assertThat(assertThrows(IllegalStateException.class,
                () -> RowNumberFilter.computePartition(FilterOperator.LAST_N_ROWS, Long.MAX_VALUE, -1),
                "LAST_N_ROWS needs table size").getMessage(),
            startsWith("Expected table size"));

        final var max = RowNumberFilter.computePartition(FilterOperator.LAST_N_ROWS, Long.MAX_VALUE, Long.MAX_VALUE);
        assertThat(max.getFirst(), arrayContaining(new RowRange(0, Long.MAX_VALUE)));
        assertThat(max.getSecond(), emptyArray());

        final var lenMax = RowNumberFilter.computePartition(FilterOperator.LAST_N_ROWS, 1_000, Long.MAX_VALUE);
        assertThat(lenMax.getFirst(), arrayContaining(new RowRange(Long.MAX_VALUE - 1_000, Long.MAX_VALUE)));
        assertThat(lenMax.getSecond(), arrayContaining(new RowRange(0, Long.MAX_VALUE - 1_000)));
    }

    private static void checkSymmetrical(final FilterOperator op1, final FilterOperator op2, final long value,
            final long optSize, final List<RowRange> included, final List<RowRange> excluded) {
        final Matcher<RowRange[]> isIncl = included.isEmpty() ? emptyArray()
            : arrayContaining(included.toArray(RowRange[]::new));
        final Matcher<RowRange[]> isExcl = excluded.isEmpty() ? emptyArray()
            : arrayContaining(excluded.toArray(RowRange[]::new));

        final var partition1 = RowNumberFilter.computePartition(op1, value, optSize);
        final var partition2 = RowNumberFilter.computePartition(op2, value, optSize);
        assertThat(partition1.getFirst(), isIncl);
        assertThat(partition1.getSecond(), isExcl);
        assertThat(partition2.getFirst(), isExcl);
        assertThat(partition2.getSecond(), isIncl);
    }
}
