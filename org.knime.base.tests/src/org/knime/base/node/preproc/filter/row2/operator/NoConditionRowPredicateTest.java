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
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataRow;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Range;

/**
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class NoConditionRowPredicateTest {

    int numberOfColumns;

    private static final NoConditionRowPredicate n = NoConditionRowPredicate.INSTANCE;

    long rowIndex;

    /**
     * Sets up the needed parameters for the following tests.
     */
    @Before
    public void setup() {
        numberOfColumns = 5;
        rowIndex = 0;
    }

    /**
     * Tests the test method in case no condition is given by the user, which means that the whole input table should be
     * returned as an output.
     */
    @Test
    public void testTest() {
        assertTrue(n.test(Mockito.mock(DataRow.class), rowIndex));
    }

    /**
     * Tests that in case of no condition all the columns of the data table are required.
     */
    @Test
    public void testGetRequiredColumns() {
        assertEquals(Collections.emptySet(), n.getRequiredColumns());
    }

    /**
     * Tests that in case of no condition all the rows of the data dable are required.
     */
    @Test
    public void testGetRowIndexRange() {
        assertEquals(Range.all(), n.getRowIndexRange());
    }
}
