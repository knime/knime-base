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
 *   Jan 29, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.randomaccess;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for ChainedRandomAccessibleDecorator.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class ChainedRandomAccessibleDecoratorTest {

    @Mock
    private RandomAccessibleDecorator<String> m_dec1;

    @Mock
    private RandomAccessibleDecorator<String> m_dec2;

    @Mock
    private RandomAccessible<String> m_decoratee;

    private ChainedRandomAccessibleDecorator<String> m_chain;

    /**
     * Initializes the chain.
     */
    @Before
    public void init() {
        m_chain = new ChainedRandomAccessibleDecorator<>(m_dec1, m_dec2);
    }

    /**
     * Verifies that the {@link RandomAccessibleDecorator#set(RandomAccessible)} method is propagated through the chain
     * lazily (i.e. not during executor) and on each invocation of {@code set}.
     */
    @Test
    public void testSet() {
        verify(m_dec1, never()).set(any());
        verify(m_dec2, never()).set(any());
        m_chain.set(m_decoratee);
        verify(m_dec1).set(m_decoratee);
        verify(m_dec2).set(m_dec1);
        m_chain.set(m_decoratee);
        verify(m_dec1, times(2)).set(m_decoratee);
        verify(m_dec2, times(2)).set(m_dec1);
    }

    /**
     * Tests that size is called on the end of the chain.
     */
    @Test
    public void testSize() {
        when(m_dec2.size()).thenReturn(3);
        assertEquals(3, m_chain.size());
        // in case of real (properly implemented) decorators, the call would be propagated through the chain
        verify(m_dec1, never()).size();
    }

    /**
     * Tests that get is only called on the end of the chain.
     */
    @Test
    public void testGet() {
        when(m_dec2.get(2)).thenReturn("foo");
        assertEquals("foo", m_chain.get(2));
        // in case of real (properly implemented) decorators, the call would be propagated through the chain
        verify(m_dec1, never()).get(anyInt());
        verify(m_dec2, times(1)).get(2);
    }

}
