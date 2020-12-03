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
 *   Mar 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.read;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for CheckSameSizeRead.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckSameSizeReadTest {

    @Mock
    private Read<Object, String> m_source;

    @Mock
    private RandomAccessible<String> m_randomAccessible;

    private CheckSameSizeRead<Object, String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_testInstance = new CheckSameSizeRead<>(m_source);
    }

    /**
     * Tests if the {@code next} method behaves as expected if the underlying read returns {@link RandomAccessible
     * RandomAccessibles} of the same size.
     *
     * @throws IOException never thrown
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNextWithSameSize() throws IOException {
        when(m_source.next()).thenReturn(m_randomAccessible, m_randomAccessible, null);
        when(m_randomAccessible.size()).thenReturn(3);
        assertEquals(m_randomAccessible, m_testInstance.next());
        assertEquals(m_randomAccessible, m_testInstance.next());
        assertEquals(null, m_testInstance.next());
    }

    /**
     * Tests the behavior of {@code next} if the source is empty.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testNextEmptySource() throws IOException {
        when(m_source.next()).thenReturn(null);
        assertEquals(null, m_testInstance.next());
    }

    /**
     * Verifies that {@code next} if the sizes of the {@link RandomAccessible RandomAccessibles} returned by the source
     * vary.
     *
     * @throws IOException never thrown
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNextWithDifferingSize() throws IOException {
        when(m_source.next()).thenReturn(m_randomAccessible);
        when(m_randomAccessible.size()).thenReturn(3);
        m_testInstance.next();
        when(m_randomAccessible.size()).thenReturn(4);
        m_testInstance.next();
    }

}
