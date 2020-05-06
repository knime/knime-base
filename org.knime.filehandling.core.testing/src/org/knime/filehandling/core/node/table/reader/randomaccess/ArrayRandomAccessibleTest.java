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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for ArrayRandomAccessible.
 * 
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class ArrayRandomAccessibleTest extends AbstractRandomAccessibleTest {

    @Override
    protected <E> RandomAccessible<E> createTestInstance(@SuppressWarnings("unchecked") E... elements) {
        return ArrayRandomAccessible.createUnsafe(elements);
    }

    /**
     * Tests the createUnsafe factory method.
     */
    @Test
    public void testCreateUnsafe() {
        String[] array = {"foo", "bar"};
        ArrayRandomAccessible<String> ra = ArrayRandomAccessible.createUnsafe(array);
        testRa(array, ra);
        array[0] = "bla";
        testRa(array, ra);
    }

    /**
     * Verifies that createUnsafe fails on {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateUnsafeFailsOnNullArray() {
        ArrayRandomAccessible.createUnsafe(null);
    }

    /**
     * Tests the createSafe method.
     */
    @Test
    public void testCreateSafe() {
        String[] array = {"foo", "bar"};
        ArrayRandomAccessible<String> ra = ArrayRandomAccessible.createSafe(array);
        testRa(array, ra);
        array[0] = "bla";
        assertThat(ra.get(0), is(not(array[0])));
    }

    /**
     * Verifies that createSafe fails on {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateSafeFailsOnNullArray() {
        ArrayRandomAccessible.createSafe(null);
    }

    private static void testRa(final String[] array, RandomAccessible<String> ra) {
        assertEquals(array.length, ra.size());
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], ra.get(i));
        }
    }

}
