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
 *   Mar 27, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.OptionalInt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.util.IndexMapper;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for IndexMappingRandomAccessibleDecorator.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class IndexMappingRandomAccessibleDecoratorTest {

    @Mock
    private IndexMapper m_idxMapper;

    @Mock
    private RandomAccessible<String> m_decoratee;

    private IndexMappingRandomAccessibleDecorator<String> createTestInstance(final int size) {
        when(m_idxMapper.getIndexRangeEnd()).thenReturn(OptionalInt.of(size - 1));
        return new IndexMappingRandomAccessibleDecorator<>(m_idxMapper);
    }

    /**
     * Tests if the constructor fails on an unboundend index mapper.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFailsOnUnboundedIdxMapper() {
        when(m_idxMapper.getIndexRangeEnd()).thenReturn(OptionalInt.empty());
        new IndexMappingRandomAccessibleDecorator<String>(m_idxMapper);
    }

    /**
     * Tests if the decorator returns the correct size (which depends on the index mapper)
     */
    @Test
    public void testSize() {
        assertEquals(4, createTestInstance(4).size());
    }

    /**
     * Tests the set and get methods.
     */
    @Test
    public void testSetAndGet() {
        IndexMappingRandomAccessibleDecorator<String> testInstance = createTestInstance(4);
        testInstance.set(m_decoratee);
        when(m_idxMapper.hasMapping(1)).thenReturn(true);
        when(m_idxMapper.map(1)).thenReturn(2);
        when(m_decoratee.size()).thenReturn(3);
        when(m_decoratee.get(2)).thenReturn("foo");
        assertEquals("foo", testInstance.get(1));
        verify(m_decoratee).get(2);

        when(m_idxMapper.hasMapping(3)).thenReturn(true);
        when(m_idxMapper.map(3)).thenReturn(3);
        // tests if the decorator hands through null values from the underlying read
        assertEquals(null, testInstance.get(3));

        when(m_idxMapper.hasMapping(2)).thenReturn(false);
        assertEquals(null, testInstance.get(2));
    }

}
