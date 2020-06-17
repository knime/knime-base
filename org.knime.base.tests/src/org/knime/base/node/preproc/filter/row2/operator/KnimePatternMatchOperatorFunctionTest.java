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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.knime.base.data.filter.row.dialog.OperatorParameters;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.google.common.collect.Range;

/**
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
public class KnimePatternMatchOperatorFunctionTest {

    /**
     *
     */
    private static final KnimePatternMatchOperatorFunction TEST_INSTANCE = KnimePatternMatchOperatorFunction.INSTANCE;
    OperatorParameters parametersNull = null;
    ColumnSpec colSpec;
    String[] name = {"Perla", "true", "false", "false"};
    DataTableSpec tableSpec;

    ColumnSpec colSpecOrdinary;

    ColumnSpec colSpecID;

    ColumnSpec colSpecFailIdx;

    ColumnSpec colSpecFailType;

    String[] wrongValues = {"Perla", "true", "true", "true"};

    String[] emptyExpression = {null, "true", "false", "false"};

    String[] wildCardTrue = {"p*a", "false", "true", "false"};

    String[] regularExpTrue = {"^P[a-z]*", "true", "false", "true"};

    String[] regularExpTrueCaseFalse = {"^p[a-z]*", "false", "false", "true"};

    String[] nothingSelected = {"Perla", "false", "false", "false"};

    /**
     * Sets up the variables needed in the following tests.
     */
    @Before
    public void setup() {
        tableSpec = TestUtilityClass.createTableSpec();
        colSpec = TestUtilityClass.createOrdinaryColumnSpec();

        //mock an ordinary column spec
        colSpecOrdinary = TestUtilityClass.createOrdinaryColumnSpec();

        //mock a row id column spec
        colSpecID = TestUtilityClass.createIDColumnSpec();

        //mock failing column spec
        colSpecFailIdx = TestUtilityClass.createFailIdxColumnSpec();
        colSpecFailType = TestUtilityClass.createFailTypeColumnSpec();
    }

    /**
     * Tests the apply method.
     * @throws InvalidSettingsException
     */
    @Test
    public void testApply() throws InvalidSettingsException {
        OperatorParameters parameters = TestUtilityClass.patternMatchOperatorParameter(colSpecOrdinary, name);
        RowPredicateFactory r = TEST_INSTANCE.apply(parameters);
        DataRow dataRow = Mockito.mock(DataRow.class);
        when(dataRow.getCell(ArgumentMatchers.anyInt())).thenReturn(new StringCell("Perla"));
        assertTrue(r.createPredicate(tableSpec).test(dataRow, (long)2));
    }
    /**
     * Tests the apply method if the right exception is thrown in case of not intialized parameters.
     */
    @Test(expected = NullPointerException.class)
    public void testApplyNull() {
        TEST_INSTANCE.apply(parametersNull);
    }

    /**
     * Tests the createPredicate method with a column spec, whose role is Ordinary.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testCreatePredicateOrdinary() throws InvalidSettingsException {
        //mock Operator Parameters for ordinary column
        OperatorParameters parameters = TestUtilityClass.patternMatchOperatorParameter(colSpecOrdinary, name);
        RowPredicateFactory p = TEST_INSTANCE.apply(parameters);
        RowPredicate rowPredicate = p.createPredicate(tableSpec);
        Set<Integer> columnToPassTest = new HashSet<>();
        columnToPassTest.add(tableSpec.findColumnIndex(colSpecOrdinary.getName()));
        assertEquals(rowPredicate.getRequiredColumns(), columnToPassTest);

        Set<Integer> columnToFailTest = new HashSet<>();
        columnToFailTest.add(tableSpec.getNumColumns());
        DataRow dataRow = Mockito.mock(DataRow.class);
        when(dataRow.getCell(ArgumentMatchers.anyInt())).thenReturn(new StringCell("Perla"));
        assertTrue(rowPredicate.test(dataRow, (long)2));
        assertNotEquals(rowPredicate.getRequiredColumns(), columnToFailTest);
    }

    /**
     * Tests the createPredicate method with a column spec, whose role is row ID.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testCreatePredicateID() throws InvalidSettingsException {
        OperatorParameters parameters = TestUtilityClass.patternMatchOperatorParameter(colSpecID, name);
        RowPredicate rowPredicate = TEST_INSTANCE.apply(parameters).createPredicate(tableSpec);
        assertEquals(rowPredicate.getRequiredColumns(), Collections.emptySet());
        assertEquals(rowPredicate.getRowIndexRange(), Range.all());
    }

    /**
     * Tests the createPredicate method when the user has given a wildcard as input.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testCreatePredicateWildCard() throws InvalidSettingsException {
        OperatorParameters parameter = TestUtilityClass.patternMatchOperatorParameter(colSpecOrdinary, wildCardTrue);
        RowPredicate rowPredicate = TEST_INSTANCE.apply(parameter).createPredicate(tableSpec);
        DataRow dataRow = Mockito.mock(DataRow.class);
        when(dataRow.getCell(ArgumentMatchers.anyInt())).thenReturn(new StringCell("Perla"));
        assertTrue(rowPredicate.test(dataRow, (long)2));
    }

    /**
     * Test the createPredicate method when the user has given a regular expression and the case sensitive option is
     * selected.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testCreatePredicateRegularExpression() throws InvalidSettingsException {
        OperatorParameters parameter = TestUtilityClass.patternMatchOperatorParameter(colSpecOrdinary, regularExpTrue);
        RowPredicate rowPredicate = TEST_INSTANCE.apply(parameter).createPredicate(tableSpec);
        DataRow dataRow = Mockito.mock(DataRow.class);
        when(dataRow.getCell(ArgumentMatchers.anyInt())).thenReturn(new StringCell("Perla"));
        assertTrue(rowPredicate.test(dataRow, (long)2));
    }

    /**
     * Tests the createPredicate method when the user has given a regular expression and the case sensitive option is
     * not selected.
     *
     * @throws InvalidSettingsException
     */
    @Test
    public void testCreatePredicateRegularExpressionNoCase() throws InvalidSettingsException {
        OperatorParameters parameter =
            TestUtilityClass.patternMatchOperatorParameter(colSpecOrdinary, regularExpTrueCaseFalse);
        RowPredicate rowPredicate = TEST_INSTANCE.apply(parameter).createPredicate(tableSpec);
        DataRow dataRow = Mockito.mock(DataRow.class);
        when(dataRow.getCell(ArgumentMatchers.anyInt())).thenReturn(new StringCell("Perla"));
        assertTrue(rowPredicate.test(dataRow, (long)2));
    }

    /**
     * Tests the createPredicate method when the user has not selected any of the options.
     * @throws InvalidSettingsException
     */
    @Test
    public void testCreatePredicateNoCase() throws InvalidSettingsException {
        OperatorParameters parameter =
            TestUtilityClass.patternMatchOperatorParameter(colSpecOrdinary, nothingSelected);
        RowPredicate rowPredicate = TEST_INSTANCE.apply(parameter).createPredicate(tableSpec);
        DataRow dataRow = Mockito.mock(DataRow.class);
        when(dataRow.getCell(ArgumentMatchers.anyInt())).thenReturn(new StringCell("perla"));
        assertTrue(rowPredicate.test(dataRow, (long)2));
    }

    /**
     * Tests if the right exception is thrown when the expression is null.
     *
     * @throws InvalidSettingsException
     */
    @Test(expected = NullPointerException.class)
    public void testCreatePredicateNullExpression() throws InvalidSettingsException {
        OperatorParameters parameters =
            TestUtilityClass.patternMatchOperatorParameter(colSpecOrdinary, emptyExpression);
        TEST_INSTANCE.apply(parameters).createPredicate(tableSpec);
    }

    /**
     * Tests if the right exception is thrown in case the column is not in the data table spec.
     *
     * @throws InvalidSettingsException
     */
    @Test(expected = InvalidSettingsException.class)
    public void testCreatePredicateFailBecauseColumnIndex() throws InvalidSettingsException {
        OperatorParameters parameter = TestUtilityClass.patternMatchOperatorParameter(colSpecFailIdx, name);
        TEST_INSTANCE.apply(parameter).createPredicate(tableSpec);
    }

    /**
     * Tests if the right exception is thrown in case the column type doesn't match to the type of the column with the
     * same name in the data table spec.
     *
     * @throws InvalidSettingsException
     *
     */
    @Test(expected = InvalidSettingsException.class)
    public void testCreatePredicateFailBecauseColumnType() throws InvalidSettingsException {
        OperatorParameters parameter = TestUtilityClass.patternMatchOperatorParameter(colSpecFailType, name);
        TEST_INSTANCE.apply(parameter).createPredicate(tableSpec);
    }

    /**
     * Tests if in case of null parameters the right exception is thrown.
     *
     * @throws InvalidSettingsException
     *
     */
    @Test(expected = NullPointerException.class)
    public void testCreatePatternPredicateFailValues() throws InvalidSettingsException {
        OperatorParameters parameter = TestUtilityClass.patternMatchOperatorParameter(colSpecOrdinary, null);
        TEST_INSTANCE.apply(parameter).createPredicate(tableSpec);
    }

    /**
     * Tests if in case of wrong parameters the right exception is thrown.
     *
     * @throws InvalidSettingsException
     *
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreatePatternPredicateFailSelections() throws InvalidSettingsException {
        OperatorParameters parameter = TestUtilityClass.patternMatchOperatorParameter(colSpecOrdinary, wrongValues);
        TEST_INSTANCE.apply(parameter).createPredicate(tableSpec);
    }
}
