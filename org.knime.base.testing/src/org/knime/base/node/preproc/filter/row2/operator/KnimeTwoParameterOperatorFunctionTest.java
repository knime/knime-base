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
 *   Jun 26, 2019 ("Perla Gjoka, KNIME GmbH, Konstanz, Germany"): created
 */
package org.knime.base.node.preproc.filter.row2.operator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.knime.base.data.filter.row.dialog.OperatorParameters;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.core.data.DataCell;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.google.common.collect.Range;

/**
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
public class KnimeTwoParameterOperatorFunctionTest {

    /**
     * Maker interface for the function, which turns the string into a data cell.
     */
    public interface StringToDataCellFunction extends Function<String, DataCell> {
        // marker interface so that Mockito and the compiler are happy
    }

    /**
     * Marker interface for the function which turns the data cell into a predicate data cell.
     */
    public interface DataCellToPredicateDataCellFunction extends BiFunction<DataCell, DataCell, Predicate<DataCell>> {
        // marker interface so that Mockito and the compiler are happy
    }

    /**
     * * Marker interface for the bifunction which based on the user input and the role of the column spec returns the
     * needed range of rows to be tested.
     */
    public interface createRowIndexRange extends BiFunction<ColumnSpec, String[], Range<Long>> {
        //marker interface
    }

    Function<String, DataCell> stringToDataCell;

    BiFunction<DataCell, DataCell, Predicate<DataCell>> dataCellToPredicate;

    OperatorParameters operatorParameters;

    DataCell dataCell1;

    DataCell dataCell2;
    ColumnSpec columnSpec;

    /**
     * Set up the needed variables before the following tests.
     */
    @Before
    public void setup() {
        //function String to DataCell
        stringToDataCell = Mockito.mock(StringToDataCellFunction.class);
        dataCell1 = Mockito.mock(DataCell.class);
        dataCell2 = Mockito.mock(DataCell.class);
        when(stringToDataCell.apply(ArgumentMatchers.any())).thenReturn(dataCell1, dataCell2);

        //Function DataCell to Predicate
        dataCellToPredicate = Mockito.mock(DataCellToPredicateDataCellFunction.class);

        columnSpec = TestUtilityClass.createOrdinaryColumnSpec();
        //create Operator Parameters
        String[] vectorOfString = {"1", "2"};
        operatorParameters = Mockito.mock(OperatorParameters.class);
        when(operatorParameters.getValues()).thenReturn(vectorOfString);
        when(operatorParameters.getColumnSpec()).thenReturn(columnSpec);

    }

    /**
     * Tests the apply method when the user is expected to give two input parameters and the range of the needed rows is
     * set.
     */
    @Test
    public void testApply() {

        //Create the row index range
        BiFunction<ColumnSpec, String[], Range<Long>> rangeFunction = Mockito.mock(createRowIndexRange.class);

        //call constructor
        KnimeTwoParameterOperatorFunction twoParameter =
            new KnimeTwoParameterOperatorFunction(stringToDataCell, dataCellToPredicate, rangeFunction);

        //Apply method to be checked
        twoParameter.apply(operatorParameters);

        ArgumentCaptor<String> stringCapture = ArgumentCaptor.forClass(String.class);
        Mockito.verify(stringToDataCell, times(2)).apply(stringCapture.capture());

        List<String> expectedString = new ArrayList<>();
        expectedString.add(operatorParameters.getValues()[0]);
        expectedString.add(operatorParameters.getValues()[1]);
        assertEquals(expectedString, stringCapture.getAllValues());

        Mockito.verify(dataCellToPredicate).apply(dataCell1, dataCell2);
        Mockito.verify(rangeFunction).apply(operatorParameters.getColumnSpec(), operatorParameters.getValues());
    }

    /**
     * Tests the apply method when the user is expected to give two input parameters, with no range of needed rows set.
     */
    @Test
    public void testApplyNoRange() {
        //call constructor
        KnimeTwoParameterOperatorFunction twoParameter =
            new KnimeTwoParameterOperatorFunction(stringToDataCell, dataCellToPredicate);

        //Apply method to be checked
        twoParameter.apply(operatorParameters);

        ArgumentCaptor<String> stringCapture = ArgumentCaptor.forClass(String.class);
        Mockito.verify(stringToDataCell, times(2)).apply(stringCapture.capture());

        List<String> expectedString = new ArrayList<>();
        expectedString.add(operatorParameters.getValues()[0]);
        expectedString.add(operatorParameters.getValues()[1]);
        assertEquals(expectedString, stringCapture.getAllValues());

        Mockito.verify(dataCellToPredicate).apply(dataCell1, dataCell2);

    }
}
