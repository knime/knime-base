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

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for {@link ReadUtils}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class ReadUtilsTest {

    @Mock
    private Read<String> m_source = null;

    @Mock
    private RandomAccessible<String> m_randomAccessible = null;

    @Mock
    private TableReadConfig<?> m_config = null;

    /**
     * @throws IOException never thrown
     */
    @Test
    public void testLimit() throws IOException {
        when(m_source.next()).thenReturn(m_randomAccessible);
        Read<String> read = ReadUtils.limit(m_source, 3);
        assertEquals(m_randomAccessible, read.next());
        assertEquals(m_randomAccessible, read.next());
        assertEquals(m_randomAccessible, read.next());
        assertEquals(null, read.next());
    }

    /**
     * @throws IOException never thrown
     */
    @Test
    public void testSkip() throws IOException {
        when(m_source.next()).thenReturn(m_randomAccessible);
        Read<String> read = ReadUtils.skip(m_source, 3);
        assertEquals(m_randomAccessible, read.next());
        verify(m_source, times(4)).next();
        assertEquals(m_randomAccessible, read.next());
        when(m_source.next()).thenReturn(null);
        assertEquals(null, read.next());
    }

    /**
     * @throws IOException never thrown
     */
    @Test
    public void testRange() throws IOException {
        when(m_source.next()).thenReturn(m_randomAccessible);
        Read<String> read = ReadUtils.range(m_source, 1, 3);
        assertEquals(m_randomAccessible, read.next());
        verify(m_source, times(2)).next();
        assertEquals(m_randomAccessible, read.next());
        assertEquals(null, read.next());
    }

    /**
     * @throws IOException never thrown
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSkipEmptyRows() throws IOException {
        when(m_source.next()).thenReturn(m_randomAccessible);
        when(m_randomAccessible.size()).thenReturn(2, 0, 0, 1, 0);
        Read<String> read = ReadUtils.skipEmptyRows(m_source);
        assertEquals(m_randomAccessible, read.next());
        verify(m_source, times(1)).next();
        verify(m_randomAccessible, times(1)).size();
        read.next();
        verify(m_source, times(4)).next();
        verify(m_randomAccessible, times(4)).size();
        when(m_source.next()).thenReturn(m_randomAccessible, (RandomAccessible<String>)null);
        assertEquals(null, read.next());
        verify(m_source, times(6)).next();
        verify(m_randomAccessible, times(5)).size();
    }

    @Test
    public void testDecorateForReadingSkipEmpty() throws IOException {
        when(m_config.skipEmptyRows()).thenReturn(true);
        when(m_config.allowShortRows()).thenReturn(true);
        Read<String> decorated = ReadUtils.decorateForReading(m_source, m_config);
        testSkipEmpty(decorated);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecorateForReadingRejectShortRows() throws IOException {
        when(m_config.allowShortRows()).thenReturn(false);
        Read<String> decorated = ReadUtils.decorateForReading(m_source, m_config);
        testRejectShortRows(decorated);
    }

    @Test
    public void testDecorateForReadingSkipHeader() throws IOException {
        when(m_source.next()).thenReturn(m_randomAccessible);
        when(m_config.useColumnHeaderIdx()).thenReturn(true);
        when(m_config.getColumnHeaderIdx()).thenReturn(1L);
        when(m_config.skipRows()).thenReturn(true);
        when(m_config.allowShortRows()).thenReturn(true);
        when(m_config.getNumRowsToSkip()).thenReturn(2L);
        // Skipping the header row anyway
        assertEquals(m_source, ReadUtils.decorateForReading(m_source, m_config));

        when(m_config.getMaxRows()).thenReturn(1L);
        when(m_config.limitRows()).thenReturn(true);
        when(m_config.getColumnHeaderIdx()).thenReturn(5L);
        // the header row is outside of the range of read rows
        assertEquals(m_source, ReadUtils.decorateForReading(m_source, m_config));

        when(m_config.getColumnHeaderIdx()).thenReturn(2L);
        Read<String> read = ReadUtils.decorateForReading(m_source, m_config);
        // the header row lies in the range of rows -> we need to skip it
        assertNotEquals(m_source, read);
        assertEquals(m_randomAccessible, read.next());
        verify(m_source, times(2)).next();
    }

    @Test
    public void testDecorateForSpecGuessingSkipEmpty() throws IOException {
        when(m_config.skipEmptyRows()).thenReturn(true);
        when(m_config.allowShortRows()).thenReturn(true);
        Read<String> decorated = ReadUtils.decorateForSpecGuessing(m_source, m_config);
        testSkipEmpty(decorated);
    }

    private void testSkipEmpty(Read<String> decorated) throws IOException {
        when(m_source.next()).thenReturn(m_randomAccessible);
        when(m_randomAccessible.size()).thenReturn(2, 0, 1);
        assertEquals(m_randomAccessible, decorated.next());
        assertEquals(m_randomAccessible, decorated.next());
        verify(m_source, times(3)).next();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecorateForSpecGuessingRejectShortRows() throws IOException {
        when(m_config.allowShortRows()).thenReturn(false);
        Read<String> decorated = ReadUtils.decorateForSpecGuessing(m_source, m_config);
        testRejectShortRows(decorated);
    }

    private void testRejectShortRows(Read<String> decorated) throws IOException {
        when(m_source.next()).thenReturn(m_randomAccessible);
        when(m_randomAccessible.size()).thenReturn(1, 2, 1);
        assertEquals(m_randomAccessible, decorated.next());
        assertEquals(m_randomAccessible, decorated.next());
    }

}
