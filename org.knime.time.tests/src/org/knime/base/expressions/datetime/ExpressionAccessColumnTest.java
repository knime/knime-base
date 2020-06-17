/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.expressions.datetime;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.expressions.core.DefaultScriptRowInput;
import org.knime.expressions.core.Expressions;
import org.knime.expressions.core.FunctionScript;
import org.knime.expressions.core.ScriptRowInput;

/**
 * Class to test Expression column access
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class ExpressionAccessColumnTest {

    private final String[] m_invalidColumnAccess = new String[]{"column(-1)", "column(0.1)", "column(2)",
        "column(\"adf\")", "column(date(1,1,1))", "column(java.lang.Integer.MAX_VALUE+1)"};

    /**
     * Tests access of columns in the script.
     */
    @Test
    public void invalidColumnAccessTest() {
        DataRow row = new DefaultRow("TestRow 1", IntCellFactory.create(3), IntCellFactory.create(2));
        DataColumnSpecCreator specCreator = new DataColumnSpecCreator("col1", DataType.getType(IntCell.class));

        DataColumnSpec[] specs = new DataColumnSpec[2];
        specs[0] = specCreator.createSpec();
        specCreator.setName("col2");
        specs[1] = specCreator.createSpec();

        FunctionScript<ScriptRowInput, DataCell> function = null;

        for (String expr : m_invalidColumnAccess) {
            try {
                function = Expressions.wrap(expr, specs, null, DataType.getType(IntCell.class));
            } catch (Exception e) {
                fail(e.getMessage());
            }

            try {
                assertNotNull(function.apply(new DefaultScriptRowInput(specs, row, 0, 0)));
                fail("Access of column should have failed:\n" + expr);
            } catch (Exception e) {
                // do nothing
            }
        }
    }
}
