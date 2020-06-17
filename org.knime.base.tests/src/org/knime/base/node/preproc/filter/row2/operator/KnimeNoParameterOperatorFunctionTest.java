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
 *   Jun 24, 2019 (Perla Gjoka): created
 */
package org.knime.base.node.preproc.filter.row2.operator;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.base.data.filter.row.dialog.OperatorParameters;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class KnimeNoParameterOperatorFunctionTest {
    OperatorParameters operatorParameters;
    Predicate<DataCell> predicateDataCell;
    DataTableSpec tableSpec;
    DataRow dataRow;
    ColumnSpec columnSpec;

    /**
     * Sets up the needed variables before starting the following tests.
     */
    @Before
    public void setup() {
        columnSpec = TestUtilityClass.createOrdinaryColumnSpec();

        operatorParameters = Mockito.mock(OperatorParameters.class);
        when(operatorParameters.getColumnSpec()).thenReturn(columnSpec);

        dataRow = Mockito.mock(DataRow.class);
        when(dataRow.getCell(ArgumentMatchers.anyInt())).thenReturn(new StringCell("Perla"));

        tableSpec = TestUtilityClass.createTableSpec();

        predicateDataCell = DataCell::isMissing;

    }
    /**
     * Tests the apply method in case there is no needed input parameter and the row Range is set.
     * @throws InvalidSettingsException
     */
    @Test
    public void testApply() throws InvalidSettingsException
    {
       KnimeNoParameterOperatorFunction noParameter = new KnimeNoParameterOperatorFunction(predicateDataCell);
       RowPredicateFactory rowPredicateFactory = noParameter.apply(operatorParameters);
       assertFalse(rowPredicateFactory.createPredicate(tableSpec).test(dataRow, (long)2));
    }

    /**
     * Tests the apply method in case there is no needed input parameter and no range is set.
     * @throws InvalidSettingsException
     */
    @Test
    public void testApplyNoRange() throws InvalidSettingsException {
        KnimeNoParameterOperatorFunction noParameter = new KnimeNoParameterOperatorFunction(predicateDataCell);
        RowPredicateFactory rowPredicateFactory = noParameter.apply(operatorParameters);
        assertFalse(rowPredicateFactory.createPredicate(tableSpec).test(dataRow, (long)2));
    }

}
