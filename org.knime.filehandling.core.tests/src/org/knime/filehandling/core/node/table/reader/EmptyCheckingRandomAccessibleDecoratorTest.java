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
 *   Jan 28, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.filehandling.core.node.table.reader.EmptyCheckingRandomAccessibleDecorator.NotEmptyException;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for EmptyCheckingRandomAccessibleDecorator.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class EmptyCheckingRandomAccessibleDecoratorTest {

    @Mock
    private RandomAccessible<String> m_decoratee;

    private EmptyCheckingRandomAccessibleDecorator<String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_testInstance = new EmptyCheckingRandomAccessibleDecorator<>(new int[] {0, 2, 3}, new String[]{"A", "C", "D"});
    }

    /**
     * Tests if non-null values in supposedly empty columns are detected.
     */
    @Test (expected = NotEmptyException.class)
    public void testNonEmptyAreDetected() {
        stubDecoratee("foo", "bar", "baz", "bum");
        m_testInstance.set(m_decoratee);
    }

    private void stubDecoratee(final String ...values) {
        for (int i = 0; i < values.length; i++) {
            when(m_decoratee.get(i)).thenReturn(values[i]);
        }
    }

    /**
     * Tests if the get method performs filtering properly.
     */
    @Test
    public void testGet() {
        stubDecoratee(null, "foo", null, null, "baz");
        m_testInstance.set(m_decoratee);
        assertEquals("foo", m_testInstance.get(0));
        assertEquals("baz", m_testInstance.get(1));
    }

    /**
     * Test the get method in case no columns are empty.
     */
    @Test
    public void testGetNoEmpty() {
        EmptyCheckingRandomAccessibleDecorator<String> ra = new EmptyCheckingRandomAccessibleDecorator<>(new int[0], new String[0]);
        stubDecoratee("foo", "bar", "baz");
        ra.set(m_decoratee);
        assertEquals("foo", ra.get(0));
        assertEquals("bar", ra.get(1));
        assertEquals("baz", ra.get(2));
    }

    /**
     * Tests if the size method returns the correct size.
     */
    @Test
    public void testSize() {
        when(m_decoratee.size()).thenReturn(4);
        m_testInstance.set(m_decoratee);
        assertEquals(1, m_testInstance.size());
    }
}
