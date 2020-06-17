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
 *   Aug 26, 2019 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row2.operator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.base.data.filter.row.dialog.component.ColumnRole;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Range;

/**
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class RangeExtractorUtilTest {

    ColumnSpec columnSpec;

    ColumnSpec columnSpec2;

    String[] parameters = {"2"};

    String[] parametersBetween = {"2", "5"};

    /**
     * Sets up all the needed variables before starting the test.
     */
    @Before
    public void setup() {
        columnSpec = Mockito.mock(ColumnSpec.class);
        when(columnSpec.getRole()).thenReturn(ColumnRole.ROW_INDEX);
        columnSpec2 = Mockito.mock(ColumnSpec.class);
        when(columnSpec2.getRole()).thenReturn(ColumnRole.ORDINARY);
    }

    /**
     * Test if the method returns the whole range of indexes.
     */
    @Test
    public void testAll(){
        assertEquals(Range.all(), RangeExtractorUtil.all(columnSpec, parameters));
    }

    /**
     * Test if the range includes only the parameter given by the user.
     */
    @Test
    public void testEqualInclusive(){
        assertEquals(Range.closed(Long.parseLong(parameters[0]), Long.parseLong(parameters[0])),
            RangeExtractorUtil.equalInclusive(columnSpec, parameters));
    }

    /**
     * Test if the range's starting point is the parameter given by the user (inclusive).
     */
    @Test
    public void testStartingInclusive(){
        assertEquals(Range.atLeast(Long.parseLong(parameters[0])),
            RangeExtractorUtil.startingInclusive(columnSpec, parameters));
    }

    /**
     * Test if the range's starting point is the parameter given by the user (exclusive).
     */
    @Test
    public void testStartingExclusive(){
        assertEquals(Range.greaterThan(Long.parseLong(parameters[0])),
            RangeExtractorUtil.startingExclusive(columnSpec, parameters));
    }

    /**
     * Test if the range's ending point is the prameter given by the user (inclusive).
     */
    @Test
    public void testEndingInclusive(){
        assertEquals(Range.atMost(Long.parseLong(parameters[0])),
            RangeExtractorUtil.endingInclusive(columnSpec, parameters));
    }

    /**
     * Test if the range's ending point is the parameter given by the user (exclusive).
     */
    @Test
    public void testEndingExclusive(){
        assertEquals(Range.lessThan(Long.parseLong(parameters[0])),
            RangeExtractorUtil.endingExclusive(columnSpec, parameters));
    }

    /**
     * Test if the range is between the parameters given by the user (both bounds inclusive).
     */
    @Test
    public void testBetween(){
        assertEquals(Range.closed(Long.parseLong(parametersBetween[0]), Long.parseLong(parametersBetween[1])),
            RangeExtractorUtil.between(columnSpec, parametersBetween));
    }

    /**
     * Test if the Column Role check works as expected. In case of a column role, which is not Row
     *             Index, the whole range of rows should be returned.
     */
    @Test
    public void testDecorateWithIdxCheck(){
        assertEquals(Range.all(), RangeExtractorUtil.between(columnSpec2, parametersBetween));
    }

    /**
     * Tests if the right exception is thrown when one parameter is expected and a different number is received.
     *
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCheckOneParameter() {
        RangeExtractorUtil.equalInclusive(columnSpec, parametersBetween);
    }

}
