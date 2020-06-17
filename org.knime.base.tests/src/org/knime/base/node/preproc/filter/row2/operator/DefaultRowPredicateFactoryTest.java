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
import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.mockito.Mockito;

import com.google.common.collect.Range;

/**
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
public class DefaultRowPredicateFactoryTest {

    Predicate<DataCell> predicateDataCell;

    ColumnSpec colSpecOrdinary;

    ColumnSpec colSpecID;
    ColumnSpec colSpecIdx;

    ColumnSpec colSpecFailIdx;

    ColumnSpec colSpecFailType;

    Range<Long> rowIndexRange;

    DataTableSpec tableSpec;

    /**
     * Before starting the tests sets up the needed variables.
     */
    @Before
    public void setup() {
        predicateDataCell = p -> p.equals(Mockito.mock(DataCell.class));

        //create an ordinary column spec
        colSpecOrdinary = TestUtilityClass.createOrdinaryColumnSpec();

        //create a row id column spec
        colSpecID = TestUtilityClass.createIDColumnSpec();

        //create a row index column spec
        colSpecIdx = TestUtilityClass.createIdxColumnSpec();

        //create DataTableSpec
        tableSpec = TestUtilityClass.createTableSpec();

        rowIndexRange = Range.closed((long)2, (long)4);

        //failure column specs
        colSpecFailIdx = TestUtilityClass.createFailIdxColumnSpec();
        colSpecFailType = TestUtilityClass.createFailTypeColumnSpec();
    }

    /**
     * Test the createPredicate method with a column, whose role is Ordinary.
     * @throws InvalidSettingsException
     *
     */
    @Test
    public void testCreatePredicateOrdinary() throws InvalidSettingsException {

        //Calls of Functions
        DefaultRowPredicateFactory rowPredicateFactory =
            new DefaultRowPredicateFactory(predicateDataCell, colSpecOrdinary, rowIndexRange);
        RowPredicate createPredicate = rowPredicateFactory.createPredicate(tableSpec);

        //IntegersToTest for test pass
        Set<Integer> columnToPassTest = new HashSet<>();
        columnToPassTest.add(tableSpec.findColumnIndex(colSpecOrdinary.getName()));

        assertEquals(createPredicate.getRequiredColumns(), columnToPassTest);

        //IntegersToTest for test to Fail
        Set<Integer> columnToFailTest = new HashSet<>();
        columnToFailTest.add(2);
        assertNotEquals(createPredicate.getRequiredColumns(), columnToFailTest);

    }

    /**
     * Tests the createPredicate method with a column whose role is RowID.
     * @throws InvalidSettingsException
     */
    @Test
    public void testCreatePredicateID() throws InvalidSettingsException {
        DefaultRowPredicateFactory rowPredicateFactory =
            new DefaultRowPredicateFactory(predicateDataCell, colSpecID, rowIndexRange);
        rowPredicateFactory.createPredicate(tableSpec);
    }

    /**
     * Tests the createPredicate method with a column, whose role is RowIndex.
     * @throws InvalidSettingsException
     */
    @Test
    public void testCreatePredicateIndex() throws InvalidSettingsException{
        DefaultRowPredicateFactory rowPredicateFactory =
            new DefaultRowPredicateFactory(predicateDataCell, colSpecIdx, rowIndexRange);
        rowPredicateFactory.createPredicate(tableSpec);
    }

    /**
     * Checks if in case of a non existing column, exception is thrown.
     * @throws InvalidSettingsException
     */
    @Test (expected = InvalidSettingsException.class)
    public void testCreatePredicateFailsBecauseColumnIndex() throws InvalidSettingsException{
        //Calls of Functions
        DefaultRowPredicateFactory rowPredicateFactory =
            new DefaultRowPredicateFactory(predicateDataCell, colSpecFailIdx);
        rowPredicateFactory.createPredicate(tableSpec);
    }

    /**
     * Checks if in case of wrong column type, exception is thrown.
     * @throws InvalidSettingsException
     */
    @Test(expected = InvalidSettingsException.class)
    public void testCreatePredicateFailsBecauseColumnType() throws InvalidSettingsException {
        //Calls of Functions
        DefaultRowPredicateFactory rowPredicateFactory =
            new DefaultRowPredicateFactory(predicateDataCell, colSpecFailType);
        rowPredicateFactory.createPredicate(tableSpec);
    }

}
