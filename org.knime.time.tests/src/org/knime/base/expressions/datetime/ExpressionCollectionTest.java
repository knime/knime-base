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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.expressions.core.DefaultScriptRowInput;
import org.knime.expressions.core.Expressions;
import org.knime.expressions.core.FunctionScript;
import org.knime.expressions.core.ScriptRowInput;

/**
 * TestCase to test execution of expressions.
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
public class ExpressionCollectionTest {

    /**
     * Tests if simple conversion.
     */
    @Test
    public void collectionSimpleConversionTest() {
        String expr = "arrayCreate(2, new java.lang.Long(1), new java.lang.Double(3))";

        FunctionScript<ScriptRowInput, DataCell> function = null;

        try {
            /* Test integer conversion */
            function = Expressions.wrap(expr, null, null, DataType.getType(ListCell.class, IntCell.TYPE));

            Object result = function.apply(new DefaultScriptRowInput(null, null, 0, 0));

            assertEquals(
                CollectionCellFactory.createListCell(Arrays.asList(new IntCell(2), new IntCell(1), new IntCell(3))),
                result);

            /* Test long conversion */
            function = Expressions.wrap(expr, null, null, DataType.getType(ListCell.class, LongCell.TYPE));

            result = function.apply(new DefaultScriptRowInput(null, null, 0, 0));

            assertEquals(
                CollectionCellFactory.createListCell(Arrays.asList(new LongCell(2), new LongCell(1), new LongCell(3))),
                result);

            /* Test double conversion */
            function = Expressions.wrap(expr, null, null, DataType.getType(ListCell.class, DoubleCell.TYPE));

            result = function.apply(new DefaultScriptRowInput(null, null, 0, 0));

            assertEquals(CollectionCellFactory
                .createListCell(Arrays.asList(new DoubleCell(2), new DoubleCell(1), new DoubleCell(3))), result);

            /* Test string conversion */
            expr = "arrayCreate(date(1,1,1))";

            function = Expressions.wrap(expr, null, null, DataType.getType(ListCell.class, StringCell.TYPE));

            result = function.apply(new DefaultScriptRowInput(null, null, 0, 0));

            assertEquals(CollectionCellFactory.createListCell(Arrays.asList(new StringCell("0001-01-01"))), result);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
