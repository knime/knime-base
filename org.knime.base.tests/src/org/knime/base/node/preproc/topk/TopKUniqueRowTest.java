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
 *   Apr 27, 2020 (lars.schweikardt): created
 */
package org.knime.base.node.preproc.topk;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.knime.core.data.DataRow;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell.IntCellFactory;

/**
 * Contains unit tests for {@link HeapTopKUniqueRowsSelector}}
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */

public class TopKUniqueRowTest {

    private static int compareRows(final DataRow left, final DataRow right) {

        return Integer.compare(getIntValue(left), getIntValue(right));
    }

    private static int getIntValue(final DataRow row) {
        IntValue intValue = (IntValue)row.getCell(0);

        return intValue.getIntValue();
    }

    private static List<DataRow> createInputRows(final int... values) {
        final List<DataRow> list = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            list.add(new DefaultRow(RowKey.createRowKey((long)i), IntCellFactory.create(values[i])));
        }
        return list;
    }

    private static HeapTopKUniqueRowsSelector createRowSelector(final int topK) {
        return new HeapTopKUniqueRowsSelector(TopKUniqueRowTest::compareRows, topK);
    }

    /**
     * Test for k = 2 with ascending ordered input.
     */
    @Test
    public void testTop2AscendingOrderedInput() {

        final HeapTopKUniqueRowsSelector uniqueRowSelector = createRowSelector(2);

        final List<DataRow> input = createInputRows(1, 1, 2, 2, 3, 3);

        for (DataRow dataRow : input) {
            uniqueRowSelector.consume(dataRow);
        }

        final List<DataRow> expectedOutput = new ArrayList<>();

        expectedOutput.add(new DefaultRow(RowKey.createRowKey(2L), IntCellFactory.create(2)));
        expectedOutput.add(new DefaultRow(RowKey.createRowKey(3L), IntCellFactory.create(2)));
        expectedOutput.add(new DefaultRow(RowKey.createRowKey(4L), IntCellFactory.create(3)));
        expectedOutput.add(new DefaultRow(RowKey.createRowKey(5L), IntCellFactory.create(3)));

        assertEquals(expectedOutput, new ArrayList<DataRow>(uniqueRowSelector.getTopK()));
    }

    /**
     * Test for k = 2 with descending ordered input.
     */
    @Test
    public void testTop2DescendingOrderedInput() {

        final HeapTopKUniqueRowsSelector uniqueRowSelector = createRowSelector(2);
        final List<DataRow> input = createInputRows(4, 3, 3, 2, 2, 1, 1, 0);

        for (DataRow dataRow : input) {
            uniqueRowSelector.consume(dataRow);
        }

        final List<DataRow> expectedOutput = new ArrayList<>();

        expectedOutput.add(new DefaultRow(RowKey.createRowKey(1L), IntCellFactory.create(3)));
        expectedOutput.add(new DefaultRow(RowKey.createRowKey(2L), IntCellFactory.create(3)));
        expectedOutput.add(new DefaultRow(RowKey.createRowKey(0L), IntCellFactory.create(4)));

        assertEquals(expectedOutput, new ArrayList<DataRow>(uniqueRowSelector.getTopK()));
    }

    /**
     * Test for k = 2 with random ordered input.
     */
    @Test
    public void testTop2RandomOrderedInput() {

        final HeapTopKUniqueRowsSelector uniqueRowSelector = createRowSelector(3);
        final List<DataRow> input = createInputRows(0, 1, 2, 2, 1, 2, 1, 4);

        for (DataRow dataRow : input) {
            uniqueRowSelector.consume(dataRow);
        }

        final List<DataRow> expectedOutput = new ArrayList<>();

        expectedOutput.add(new DefaultRow(RowKey.createRowKey(1L), IntCellFactory.create(1)));
        expectedOutput.add(new DefaultRow(RowKey.createRowKey(4L), IntCellFactory.create(1)));
        expectedOutput.add(new DefaultRow(RowKey.createRowKey(6L), IntCellFactory.create(1)));
        expectedOutput.add(new DefaultRow(RowKey.createRowKey(2L), IntCellFactory.create(2)));
        expectedOutput.add(new DefaultRow(RowKey.createRowKey(3L), IntCellFactory.create(2)));
        expectedOutput.add(new DefaultRow(RowKey.createRowKey(5L), IntCellFactory.create(2)));
        expectedOutput.add(new DefaultRow(RowKey.createRowKey(7L), IntCellFactory.create(4)));

        assertEquals(expectedOutput, new ArrayList<DataRow>(uniqueRowSelector.getTopK()));
    }

    /**
     * Test for k = 1.
     */
    @Test
    public void testTop1() {

        final HeapTopKUniqueRowsSelector uniqueRowSelector = createRowSelector(1);
        final List<DataRow> input = createInputRows(0, 1, 2, 2, 1, 2, 1, 4, 4);

        for (DataRow dataRow : input) {
            uniqueRowSelector.consume(dataRow);
        }

        final List<DataRow> expectedOutput = new ArrayList<>();

        expectedOutput.add(new DefaultRow(RowKey.createRowKey(7L), IntCellFactory.create(4)));
        expectedOutput.add(new DefaultRow(RowKey.createRowKey(8L), IntCellFactory.create(4)));

        assertEquals(expectedOutput, new ArrayList<DataRow>(uniqueRowSelector.getTopK()));
    }

}
