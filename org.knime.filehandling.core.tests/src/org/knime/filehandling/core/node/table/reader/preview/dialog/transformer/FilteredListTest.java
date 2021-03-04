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
 *   Mar 3, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.a;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for FilteredList
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class FilteredListTest {

    private static class DummyPositionable implements Positionable {

        int m_position;

        DummyPositionable() {
            this(0);
        }

        DummyPositionable(final int position) {
            m_position = position;
        }

        @Override
        public int getPosition() {
            return m_position;
        }

        @Override
        public boolean setPosition(final int position) {
            if (m_position != position) {
                m_position = position;
                return true;
            }
            return false;
        }

    }

    private FilteredList<DummyPositionable> m_testInstance;

    private DummyPositionable m_item1;

    private DummyPositionable m_item2;

    /**
     * Initializes members needed for testing.
     */
    @Before
    public void init() {
        m_testInstance = new FilteredList<>();
        m_item1 = new DummyPositionable();
        m_item2 = new DummyPositionable();
    }

    /**
     * Tests adding without a filter.
     */
    @Test
    public void testAdding() {
        assertEquals(0, m_testInstance.unfilteredSize());
        assertEquals(0, m_testInstance.filteredSize());
        m_testInstance.add(m_item1);
        assertEquals(1, m_testInstance.unfilteredSize());
        assertEquals(1, m_testInstance.filteredSize());
        assertEquals(m_item1, m_testInstance.get(0));
    }

    /**
     * Tests adding with a filter.
     */
    @Test
    public void testAddingWithFilter() {
        m_testInstance.setFilter(i -> i == m_item2);
        m_testInstance.add(m_item1);
        assertEquals(1, m_testInstance.unfilteredSize());
        assertEquals(0, m_testInstance.filteredSize());
        m_testInstance.add(m_item2);
        assertEquals(2, m_testInstance.unfilteredSize());
        assertEquals(1, m_testInstance.filteredSize());
    }

    /**
     * Tests clearing.
     */
    @Test
    public void testClear() {
        testAddingWithFilter();
        m_testInstance.clear();
        assertEquals(0, m_testInstance.unfilteredSize());
        assertEquals(0, m_testInstance.filteredSize());
    }

    /**
     * Tests setting a filter.
     */
    @Test
    public void testSetFilter() {
        fillTestInstance(m_item1, m_item2);
        assertEquals(2, m_testInstance.filteredSize());
        assertEquals(m_item1, m_testInstance.get(0));
        m_testInstance.setFilter(i -> i == m_item2);
        assertEquals(1, m_testInstance.filteredSize());
        assertEquals(2, m_testInstance.unfilteredSize());
        assertEquals(m_item2, m_testInstance.get(0));
    }

    /**
     * Test sorting.
     */
    @Test
    public void testSort() {
        m_item1.m_position = 1;
        fillTestInstance(m_item1, m_item2);
        assertEquals(m_item1, m_testInstance.get(0));
        assertEquals(m_item2, m_testInstance.get(1));
        m_testInstance.sort();
        assertEquals(m_item2, m_testInstance.get(0));
        assertEquals(m_item1, m_testInstance.get(1));
    }

    /**
     * Tests reordering where from &lt to.
     */
    @Test
    public void testReorderFromSmallerTo() {
        testReorder(1, 3, new int[] {0, 2, 3, 1});
    }

    /**
     * Tests reordering where from &gt to.
     */
    @Test
    public void testReorderFromLargerTo() {
        testReorder(3, 1, new int[] {0, 3, 1, 2});
    }

    /**
     * Tests reordering where from == to.
     */
    @Test
    public void testReorderFromEqualTo() {
        testReorder(2, 2, new int[] {0, 1, 2, 3});
    }

    private void testReorder(final int from, final int to, final int[] expectedPositions) {
        m_item1.m_position = 0;
        m_item2.m_position = 1;
        DummyPositionable[] elements = a(m_item1, m_item2, new DummyPositionable(2), new DummyPositionable(3));
        fillTestInstance(elements);
        assertEquals(from != to, m_testInstance.reorder(from, to));
        DummyPositionable[] expected = new DummyPositionable[4];
        for (int i = 0; i < expected.length; i++) {
            DummyPositionable element = elements[expectedPositions[i]];
            expected[i] = element;
            assertEquals(i, element.getPosition());
        }
        assertArrayEquals(expected, m_testInstance.unfilteredStream().toArray(DummyPositionable[]::new));
    }

    /**
     * Tests the unfiltered iterable.
     */
    @Test
    public void testUnfilteredIterable() {
        fillTestInstance(m_item1, m_item2);
        m_testInstance.setFilter(i -> i == m_item2);
        Iterator<DummyPositionable> iterator = m_testInstance.unfilteredIterable().iterator();
        assertEquals(m_item1, iterator.next());
        assertEquals(m_item2, iterator.next());
        assertEquals(false, iterator.hasNext());
    }

    /**
     * Tests the unfiltered stream.
     */
    @Test
    public void testUnfilteredStream() {
        fillTestInstance(m_item1, m_item2);
        m_testInstance.setFilter(i -> i == m_item2);
        final DummyPositionable[] result = m_testInstance.unfilteredStream().toArray(DummyPositionable[]::new);
        assertArrayEquals(new DummyPositionable[]{m_item1, m_item2}, result);
    }

    private void fillTestInstance(final DummyPositionable... values) {
        for (DummyPositionable value : values) {
            m_testInstance.add(value);
        }
    }

}
