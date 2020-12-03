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
 *   Apr 1, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.read;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.OptionalLong;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for {@link AbstractReadDecorator}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractReadDecoratorTest {

    private static class TestReadDecorator extends AbstractReadDecorator<Object, String> {

        TestReadDecorator(final Read<Object, String> source) {
            super(source);
        }

        @SuppressWarnings("resource")
        @Override
        public RandomAccessible<String> next() throws IOException {
            return getSource().next();
        }

    }

    @Mock
    private Read<Object, String> m_source = null;

    private TestReadDecorator m_testInstance = null;

    /**
     * Sets up the test instance.
     */
    @Before
    public void init() {
        m_testInstance = new TestReadDecorator(m_source);
    }

    /**
     * Tests the {@code getSource} method.
     */
    @SuppressWarnings("resource")
    @Test
    public void testGetSource() {
        assertEquals(m_source, m_testInstance.getSource());
    }

    /**
     * Tests if the {@code getMaxProgress} method properly delegates to the underlying read.
     */
    @Test
    public void testGetEstimatedSizeInBytes() {
        OptionalLong estimatedSize = OptionalLong.of(3);
        when(m_source.getMaxProgress()).thenReturn(estimatedSize);
        assertEquals(estimatedSize, m_testInstance.getMaxProgress());
    }

    /**
     * Tests if the {@code getProgress} method properly delegates to the underlying read.
     */
    @Test
    public void testReadBytes() {
        when(m_source.getProgress()).thenReturn(3L);
        assertEquals(3L, m_testInstance.getProgress());
    }

    /**
     * Tests if the {@code close} method properly delegates to the underlying read.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testClose() throws IOException {
        m_testInstance.close();
        verify(m_source, times(1)).close();
    }

}
