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
 *   Mar 23, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.spec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy.TypeResolver;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for TypeGuesser.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class TypeGuesserTest {

    @Mock
    private TypeHierarchy<String, String> m_typeHierarchy = null;

    @Mock
    private TypeResolver<String, String> m_typeResolver = null;

    @Mock
    private RandomAccessible<String> m_randomAccessible = null;

    private void stubRandomAccessible(final String... values) {
        when(m_randomAccessible.size()).thenReturn(values.length);
        for (int i = 0; i < values.length; i++) {
            when(m_randomAccessible.get(i)).thenReturn(values[i]);
        }
    }

    /**
     * Sets up method stubbings that are used by all tests.
     */
    @Before
    public void init() {
        when(m_typeHierarchy.createResolver()).thenReturn(m_typeResolver);
        when(m_typeResolver.reachedTop()).thenReturn(false);
    }

    /**
     * Tests if the TypeGuesser behaves correctly if all rows have the same number of columns.
     */
    @Test
    public void testAllRowsSameSize() {
        final TypeGuesser<String, String> tg = new TypeGuesser<>(m_typeHierarchy, true);
        stubRandomAccessible("foo", "bar", null);
        tg.update(m_randomAccessible);
        verify(m_typeResolver).accept("foo");
        verify(m_typeResolver).accept("bar");
        verify(m_typeResolver).accept(null);
        stubRandomAccessible("bar", "foo", "foobar");
        tg.update(m_randomAccessible);
        verify(m_typeResolver, times(2)).accept("bar");
        verify(m_typeResolver, times(2)).accept("foo");
        verify(m_typeResolver, times(1)).accept("foobar");

        when(m_typeResolver.getMostSpecificType()).thenReturn("foo", "bar", "foobar");
        assertEquals(Arrays.asList("foo", "bar", "foobar"), tg.getMostSpecificTypes(0));
    }

    /**
     * Tests if the TypeGuesser behaves correctly if the initial rows contain fewer values.
     */
    @Test
    public void testFirstRowShorter() {
        final TypeGuesser<String, String> tg = new TypeGuesser<>(m_typeHierarchy, true);
        stubRandomAccessible("foo");
        tg.update(m_randomAccessible);
        verify(m_typeResolver, times(1)).accept("foo");
        stubRandomAccessible("bar", "foo");
        tg.update(m_randomAccessible);
        verify(m_typeResolver, times(1)).accept("bar");
        verify(m_typeResolver, times(2)).accept("foo");

        when(m_typeResolver.getMostSpecificType()).thenReturn("bar", "foo");
        assertEquals(Arrays.asList("bar", "foo"), tg.getMostSpecificTypes(0));
    }

    /**
     * Tests if the canStop method behaves correctly for both early stopping enabled and disabled.
     */
    @Test
    public void testCanStop() {
        testCanStop(false);
        testCanStop(true);
    }

    private void testCanStop(final boolean earlyStoppingEnabled) {
        final TypeGuesser<String, String> tg = new TypeGuesser<>(m_typeHierarchy, earlyStoppingEnabled);
        assertFalse(tg.canStop());
        stubRandomAccessible("foo");
        when(m_typeResolver.reachedTop()).thenReturn(false);
        tg.update(m_randomAccessible);
        assertFalse(tg.canStop());
        when(m_typeResolver.reachedTop()).thenReturn(true);
        tg.update(m_randomAccessible);
        assertEquals(earlyStoppingEnabled, tg.canStop());
    }

    /**
     * Tests if getMostSpecificTypes behaves correctly if the provided minimum is larger than the number of actually
     * observed columns.
     */
    @Test
    public void testGetMostSpecificTypesWithLargerMinimum() {
        final TypeGuesser<String, String> tg = new TypeGuesser<>(m_typeHierarchy, true);
        stubRandomAccessible("foo");
        tg.update(m_randomAccessible);
        when(m_typeResolver.getMostSpecificType()).thenReturn("foo", "bar");
        assertEquals(Arrays.asList("foo", "bar"), tg.getMostSpecificTypes(2));
    }

}
