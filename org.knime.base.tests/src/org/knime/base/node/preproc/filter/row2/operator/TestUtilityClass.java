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

import static org.mockito.Mockito.when;

import org.knime.base.data.filter.row.dialog.OperatorParameters;
import org.knime.base.data.filter.row.dialog.component.ColumnRole;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.mockito.Mockito;

/**
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
public final class TestUtilityClass {

    private TestUtilityClass() {
        // static utility class
    }

    /**
     * @return a mock of a column spec with ordinary role.
     */
    public static ColumnSpec createOrdinaryColumnSpec() {
        ColumnSpec colSpecOrdinary = Mockito.mock(ColumnSpec.class);
        when(colSpecOrdinary.getRole()).thenReturn(ColumnRole.ORDINARY);
        when(colSpecOrdinary.getType()).thenReturn(StringCell.TYPE);
        when(colSpecOrdinary.getName()).thenReturn("Name");
        return colSpecOrdinary;
    }

    /**
     * @return a mock of a column spec with a row id role.
     */
    public static ColumnSpec createIDColumnSpec() {
        ColumnSpec colSpecID = Mockito.mock(ColumnSpec.class);
        when(colSpecID.getRole()).thenReturn(ColumnRole.ROW_ID);
        when(colSpecID.getName()).thenReturn(ColumnRole.ROW_ID.getDefaultName());
        return colSpecID;
    }

    /**
     * @return a mock of a column spec with a row index role.
     */
    public static ColumnSpec createIdxColumnSpec() {
        ColumnSpec colSpecIdx = Mockito.mock(ColumnSpec.class);
        when(colSpecIdx.getRole()).thenReturn(ColumnRole.ROW_INDEX);
        when(colSpecIdx.getName()).thenReturn(ColumnRole.ROW_INDEX.getDefaultName());
        return colSpecIdx;
    }

    /**
     * @return a mock of a column spec, whose name is not part of
     * the data table spec.
     */
    public static ColumnSpec createFailIdxColumnSpec() {
        ColumnSpec colSpecFailIdx =  Mockito.mock(ColumnSpec.class);
        when(colSpecFailIdx.getName()).thenReturn("YouKnowNothing");
        when(colSpecFailIdx.getRole()).thenReturn(ColumnRole.ORDINARY);
        return colSpecFailIdx;
    }

    /**
     * @return a mock of a column spec, whose type is not the same
     * as the column with the same name in the data table spec.
     */
    public static ColumnSpec createFailTypeColumnSpec() {
        ColumnSpec colSpecFailType = Mockito.mock(ColumnSpec.class);
        when(colSpecFailType.getName()).thenReturn("Name");
        when(colSpecFailType.getType()).thenReturn(IntCell.TYPE);
        when(colSpecFailType.getRole()).thenReturn(ColumnRole.ORDINARY);
        return colSpecFailType;
    }
    /**
     * @return a table spec
     */
    public static DataTableSpec createTableSpec()
    {
        final String[] names = {"Name", "Data", "Adress"};
        final DataType[] types = {StringCell.TYPE, DoubleCell.TYPE, StringCell.TYPE};

        return new DataTableSpec("tableToTest", names, types);
    }

    /**
     * @param colSpec is a column spec to help create the operator parameters
     * @param name is a string vector which holds finctional user choices for Pattern Match Panel
     * @return the mock of the operator parameters
     */
    public static OperatorParameters patternMatchOperatorParameter(final ColumnSpec colSpec, final String[] name) {
        OperatorParameters parameters = Mockito.mock(OperatorParameters.class);
        when(parameters.getColumnSpec()).thenReturn(colSpec);
        when(parameters.getValues()).thenReturn(name);
        return parameters;
    }
}
