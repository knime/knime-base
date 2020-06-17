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
 *   Jun 19, 2019 (Perla Gjoka): created
 */
package org.knime.base.node.preproc.filter.row2.operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.DataRow;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.google.common.collect.Range;

/**
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
public class GroupRowPredicateTest {

    private static final BinaryOperator<BiPredicate<DataRow, Long>> AND =
        (p1, p2) -> ((dataRow, rowIdx) -> p1.test(dataRow, rowIdx) && (p2.test(dataRow, rowIdx)));

    private static final BinaryOperator<BiPredicate<DataRow, Long>> OR =
        (p1, p2) -> ((dataRow, rowIdx) -> p1.test(dataRow, rowIdx) || (p2.test(dataRow, rowIdx)));

    static final BinaryOperator<Range<Long>> AND_RANGE = (p, q) -> p.intersection(q);

    static final BinaryOperator<Range<Long>> OR_RANGE = (p, q) -> p.span(q);

    private Set<Integer> indexesToTest;

    private Set<Integer> indexesToTest2;

    private RowPredicate rowPredicate;

    private RowPredicate rowPredicate2;

    private List<RowPredicate> rowPredicateList;

    private GroupRowPredicate c;

    private GroupRowPredicate c2;

    private Long rowIndex = (long)0;

    /**
     * Before the tests it sets up the needed variables.
     */
    @Before
    public void setup() {

        indexesToTest = new HashSet<>();
        indexesToTest.add(0);
        rowPredicate = Mockito.mock(RowPredicate.class);
        when(rowPredicate.test(ArgumentMatchers.any(), ArgumentMatchers.anyLong())).thenReturn(false);
        when(rowPredicate.getRequiredColumns()).thenReturn(indexesToTest);
        when(rowPredicate.getRowIndexRange()).thenReturn(Range.closed((long)2, (long)4));

        indexesToTest2 = new HashSet<>();
        indexesToTest2.add(2);
        rowPredicate2 = Mockito.mock(RowPredicate.class);
        when(rowPredicate2.test(ArgumentMatchers.any(), ArgumentMatchers.anyLong())).thenReturn(true);
        when(rowPredicate2.getRequiredColumns()).thenReturn(indexesToTest2);
        when(rowPredicate2.getRowIndexRange()).thenReturn(Range.closed((long)3, (long)5));

        rowPredicateList = new ArrayList<>();
        rowPredicateList.add(rowPredicate);
        rowPredicateList.add(rowPredicate2);
        c = new GroupRowPredicate(rowPredicateList.iterator(), OR, OR_RANGE);
        c2 = new GroupRowPredicate(rowPredicateList.iterator(), AND, AND_RANGE);

    }

    /**
     * Test the test method with OR and AND group operations. Combination of testing: false, true. OR is expected to
     * produce true result while AND is expected to produce false result.
     */
    @Test
    public void testTest() {
        assertTrue(c.test(Mockito.mock(DataRow.class), rowIndex));
        assertFalse(c2.test(Mockito.mock(DataRow.class), rowIndex));
    }

    /**
     * Tests if the requiredColumns returned are the one expected.
     */
    @Test
    public void testGetRequiredColumns() {
        Set<Integer> indexToTest = new HashSet<>();
        indexToTest.add(0);
        indexToTest.add(2);
        assertEquals(c.getRequiredColumns(), indexToTest);
    }

    /**
     * Tests if the range of rows required is the one expected.
     */
    @Test
    public void testGetRowIndexRange() {
        assertEquals(c.getRowIndexRange(), Range.closed((long)2, (long)5));
        assertEquals(c2.getRowIndexRange(), Range.closed((long)3, (long)4));
    }

}
