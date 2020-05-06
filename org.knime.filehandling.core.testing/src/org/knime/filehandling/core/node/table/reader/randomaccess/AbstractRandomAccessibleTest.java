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
 *   Mar 13, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.randomaccess;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Contains general tests for implementations of {@link RandomAccessible}. In order to test a specific implementation,
 * simply extend this class and implement {@link #createTestInstance(Object...)}.
 * 
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractRandomAccessibleTest {

    /**
     * Creates a {@link RandomAccessible} with the provided elements for testing.
     * 
     * @param elements the elements contained in the {@link RandomAccessible}
     * @return an instance of {@link RandomAccessible} that contains the provided elements
     */
    @SuppressWarnings("unchecked")
    protected abstract <E> RandomAccessible<E> createTestInstance(E... elements);

    /**
     * Tests if the size method works correctly.
     */
    @Test
    public void testSize() {
        RandomAccessible<String> ra = createTestInstance("foo", "bar");
        assertEquals(2, ra.size());
    }

    /**
     * Tests if the get method works correctly.
     */
    @Test
    public void testGet() {
        RandomAccessible<String> ra = createTestInstance("foo", "bar");
        assertEquals("foo", ra.get(0));
        assertEquals("bar", ra.get(1));
    }

    /**
     * Tests the border case of calling {@link RandomAccessible#get(int)} with the size of the random accessible.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetIndexTooLarge() {
        RandomAccessible<String> ra = createTestInstance("foo", "bar");
        ra.get(ra.size());
    }

    /**
     * Tests the border case of calling {@link RandomAccessible#get(int)} with a negative index.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetNegative() {
        RandomAccessible<String> ra = createTestInstance("foo", "bar");
        ra.get(-1);
    }

    /**
     * Tests the {@link RandomAccessible#copy()} method.
     */
    @Test
    public void testCopy() {
        RandomAccessible<String> ra = createTestInstance("foo", "bar");
        RandomAccessible<String> copy = ra.copy();
        assertFalse(copy == ra);
        assertEquals(ra.size(), copy.size());
        for (int i = 0; i < ra.size(); i++) {
            assertEquals(ra.get(i), copy.get(i));
            assertTrue(ra.get(i) == copy.get(i));
        }
    }

    /**
     * Tests {@link RandomAccessible#iterator()}.
     */
    @Test
    public void testIterator() {
        RandomAccessible<String> ra = createTestInstance("foo", "bar");
        final Iterator<String> iterator = ra.iterator();
        for (int i = 0; i < ra.size(); i++) {
            assertTrue(iterator.hasNext());
            assertEquals(ra.get(i), iterator.next());
        }
        assertFalse(iterator.hasNext());
    }

    /**
     * Tests if the {@link Iterator} returned by {@link RandomAccessible#iterator()} adheres to its contract.
     */
    @Test(expected = NoSuchElementException.class)
    public void testIteratorThrowsNoSuchElementException() {
        final Iterator<String> iterator = createTestInstance("foo", "bar").iterator();
        for (; iterator.hasNext(); iterator.next()) {
            // run to the end
        }
        iterator.next();
    }

}
