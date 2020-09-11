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
 *   Sep 10, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataRow;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.filehandling.core.util.CheckedExceptionBiFunction;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for MultiTablePreviewRowIterator.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("resource")
public class MultiTablePreviewRowIteratorTest {

    @Mock
    private Iterator<Path> m_pathIterator;

    @Mock
    private Path m_path;

    @Mock
    private CheckedExceptionBiFunction<Path, FileStoreFactory, PreviewRowIterator, IOException> m_iteratorFn;

    @Mock
    private DataRow m_row;

    @Mock
    private PreviewRowIterator m_currentIterator;

    private MultiTablePreviewRowIterator m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_testInstance = new MultiTablePreviewRowIterator(m_pathIterator, m_iteratorFn);
    }

    private void stubPathIterator(final Boolean value, final Boolean ... values) {
        when(m_pathIterator.next()).thenReturn(m_path);
        when(m_pathIterator.hasNext()).thenReturn(value, values);
    }

    /**
     * Tests the implementation of {@link PreviewRowIterator#hasNext()}.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testHasNext() throws IOException {
        stubPathIterator(true, true, false);
        when(m_currentIterator.next()).thenReturn(m_row);
        when(m_currentIterator.hasNext()).thenReturn(true);
        when(m_iteratorFn.apply(eq(m_path), any())).thenReturn(m_currentIterator);

        assertTrue(m_testInstance.hasNext());
        assertTrue(m_testInstance.hasNext());
        m_testInstance.next();
        when(m_currentIterator.hasNext()).thenReturn(false, true);
        assertTrue(m_testInstance.hasNext());
        m_testInstance.next();
        assertTrue(m_testInstance.hasNext());
        when(m_currentIterator.hasNext()).thenReturn(false);
        assertFalse(m_testInstance.hasNext());
    }

    /**
     * Tests if {@link IOException} are wrapped into {@link PreviewIteratorException}.
     *
     * @throws IOException thrown but then wrapped into {@link PreviewIteratorException}.
     */
    @Test (expected = PreviewIteratorException.class)
    public void testHasNextWrapsIOExceptions() throws IOException {
        stubPathIterator(true, false);
        when(m_iteratorFn.apply(eq(m_path), any())).thenThrow(IOException.class);
        m_testInstance.hasNext();
    }

    /**
     * Tests the implementation of {@link PreviewRowIterator#next()}.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testNext() throws IOException {
        DataRow second = mock(DataRow.class);
        stubPathIterator(true, true, false);
        when(m_currentIterator.next()).thenReturn(m_row, second);
        when(m_currentIterator.hasNext()).thenReturn(true);
        when(m_iteratorFn.apply(eq(m_path), any())).thenReturn(m_currentIterator);

        assertEquals(m_row, m_testInstance.next());
        when(m_currentIterator.hasNext()).thenReturn(false, true);
        assertEquals(second, m_testInstance.next());
        when(m_currentIterator.hasNext()).thenReturn(false);
        assertFalse(m_testInstance.hasNext());
    }

    /**
     * Tests that {@link NoSuchElementException} is thrown if there are no more paths.
     */
    @Test (expected = NoSuchElementException.class)
    public void testNextThrowsNoSuchElementExceptionOnEmptyPathIterator() {
        m_testInstance.next();
    }

    /**
     * Tests that {@link NoSuchElementException} is thrown if there are more paths but they are all empty.
     *
     * @throws IOException never thrown
     */
    @Test (expected = NoSuchElementException.class)
    public void testNextThrowsNoSuchElementExceptionIfAllRemainingIteratorsAreEmpty() throws IOException {
        stubPathIterator(true, false);
        when(m_iteratorFn.apply(eq(m_path), any())).thenReturn(m_currentIterator);
        m_testInstance.next();
    }

    /**
     * Test {@link PreviewRowIterator#close()} if hasNext has never been called.
     */
    @Test
    public void testCloseWithoutIteration() {
        m_testInstance.close();
        verify(m_currentIterator, never()).close();
    }


    /**
     * Tests {@link PreviewRowIterator#close()} if hasNext has been called at least once.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testCloseAfterIterating() throws IOException {
        stubPathIterator(true);
        when(m_iteratorFn.apply(eq(m_path), any())).thenReturn(m_currentIterator);
        when(m_currentIterator.hasNext()).thenReturn(true);
        m_testInstance.hasNext();
        m_testInstance.close();
        verify(m_currentIterator).close();
    }

}
