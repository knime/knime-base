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
 *   Mar 24, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.spec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * Unit tests for ExtractColumnHeaderRead.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("resource")
@RunWith(MockitoJUnitRunner.class)
public class DefaultExtractColumnHeaderReadTest {

    @Mock
    private Read<String> m_source;

    @Mock
    private RandomAccessible<String> m_dataRow;

    @Mock
    private RandomAccessible<String> m_headerRow;

    @Mock
    private RandomAccessible<String> m_skipRow;

    /**
     * Tests the next method if no column header idx is provided.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testNextWithoutColumnHeaders() throws IOException {
        stubSource(TestPair.create(10, m_dataRow));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(-1));
        for (int i = 0; i < 10; i++) {
            assertEquals(m_dataRow, testInstance.next());
        }
        assertEquals(null, testInstance.next());
        assertEquals(Optional.empty(), testInstance.getColumnHeaders());
    }

    /**
     * Tests the next method if a column header idx is provided AND we actually reach the column header row during
     * iterating.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testNextWithColumnHeaders() throws IOException {
        stubSource(TestPair.create(3, m_dataRow), TestPair.create(1, m_headerRow), TestPair.create(7, m_dataRow));
        stubHeaderCol("foo", "bar", null);
        ExtractColumnHeaderRead<String> testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(3));
        for (int i = 0; i < 10; i++) {
            assertEquals(m_dataRow, testInstance.next());
        }
        // next should be called 11 times because the third call to
        // testInstance.next()
        // calls m_source.next() twice (once to get the column header and
        // another time
        // to get the next row)
        verify(m_source, times(11)).next();
        final String[] columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(11)).next();
        assertEquals(null, testInstance.next());
        assertArrayEquals(new String[]{"foo", "bar", null}, columnHeaders);
    }

    /**
     * Tests if getColumnHeader iterates through the table up to the row containing the column headers if that row has
     * not been read yet.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeadersIfTheyWereNotReadYet() throws IOException {
        stubSource(TestPair.create(3, m_dataRow), TestPair.create(1, m_headerRow), TestPair.create(5, m_dataRow));
        stubHeaderCol("foo", "bar");
        ExtractColumnHeaderRead<String> testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(3));
        testInstance.next();
        verify(m_source, times(1)).next();
        final String[] columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(4)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests if the getColumnHeaders returns an Optional.emty if the column header row is not contained in the table.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeadersReturnsOptionalEmptyIfTheRowContainingTheColumnHeadersIsNotContained()
        throws IOException {
        stubSource(TestPair.create(2, m_dataRow));
        ExtractColumnHeaderRead<String> testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2));
        assertEquals(m_dataRow, testInstance.next());
        assertTrue(!testInstance.getColumnHeaders().isPresent());
        verify(m_source, times(3)).next();
    }

    /**
     * Tests if the getColumnHeaders returns an Optional.emty if column header is set, but preceding row is null.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeadersReturnsOptionalEmptydHeaderColNotWithinRead() throws IOException {
        // test without read
        stubSource(TestPair.create(0, null));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(5, 0, -1));
        assertTrue(!testInstance.getColumnHeaders().isPresent());
        verify(m_source, times(2)).next();

        // test with read
        stubSource(TestPair.create(0, null));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(5, 0, -1));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        verify(m_source, times(3)).next();
        assertTrue(!testInstance.getColumnHeaders().isPresent());
        verify(m_source, times(5)).next();
    }

    /**
     * Tests if the getColumnHeaders returns an Optional.emty if no column header is set, but a couple of rows are being
     * skipped
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeadersReturnsOptionalEmptyIfNotSetAndSkippingOn() throws IOException {
        // test without read
        stubSource(TestPair.create(2, m_skipRow)//
            , TestPair.create(4, m_dataRow));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(-1, 2, -1));
        assertTrue(!testInstance.getColumnHeaders().isPresent());
        verify(m_source, times(0)).next();

        // test with read
        stubSource(TestPair.create(2, m_skipRow)//
            , TestPair.create(4, m_dataRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(-1, 2, -1));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        verify(m_source, times(7)).next();
        assertTrue(!testInstance.getColumnHeaders().isPresent());
    }

    /**
     * Tests that the column header is returned after skipping a certain number of rows. The column header itself is at
     * a position that is greather than the skipped value.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeaderFoundInReadAfterSkippingZeroRows() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(1, m_dataRow)//
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_dataRow));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(1, 0, -1));
        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(2)).next();
        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(1, m_dataRow)//
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_dataRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(1, 0, -1));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 2 from before + 5 times from the new test
        verify(m_source, times(7)).next();
        columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests that the column header is returned after skipping a certain number of rows. The column header itself is at
     * a position that is greather than the skipped value.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeaderFoundInReadAfterSkippingAndItsPositionIsGtSkip() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_skipRow)//
            , TestPair.create(1, m_dataRow)//
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_dataRow));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(3, 2, -1));
        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(4)).next();
        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_skipRow)//
            , TestPair.create(1, m_dataRow)//
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_dataRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(3, 2, -1));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 4 from before + 7 times from the new test
        verify(m_source, times(11)).next();
        columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests that the column header is returned after skipping a certain number of rows. The column header itself is at
     * a position that is less than the skipped value
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeaderFoundInReadAfterSkippingAndItsPositionIsLtSkip() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_skipRow)//
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(1, m_skipRow)//
            , TestPair.create(2, m_dataRow));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 3, -1));

        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(3)).next();

        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_skipRow)//
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(1, m_skipRow)//
            , TestPair.create(2, m_dataRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 3, -1));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 3 from before + 7 times from the new test
        verify(m_source, times(10)).next();
        columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests that the column header is returned after skipping a certain number of rows. The column header itself is the
     * first row.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeaderFoundInReadAfterSkippingAndItsPositionIsZero() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(1, m_headerRow)//
            , TestPair.create(3, m_skipRow)//
            , TestPair.create(2, m_dataRow));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(0, 3, -1));

        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(1)).next();

        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(1, m_headerRow)//
            , TestPair.create(3, m_skipRow)//
            , TestPair.create(2, m_dataRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(0, 3, -1));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 3 from before + 7 times from the new test
        verify(m_source, times(8)).next();
        columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests that the column header is returned after skipping a certain number of rows. The column header itself is at
     * a position that equals the skipped value.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeaderFoundInReadAfterSkippingAndItsPositionIsEqSkip() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_skipRow)//
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(3, m_dataRow));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 2, -1));

        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(3)).next();

        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_skipRow)//
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(3, m_dataRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 2, -1));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }

        // 3 from before + 7 times from the new test
        verify(m_source, times(10)).next();
        columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests if the getColumnHeaders returns an Optional.emty if no column header is set, but reading is limited.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeadersReturnsOptionalEmptyIfNotSetAndLimittingOn() throws IOException {
        // test without read
        stubSource(TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(-1, 0, 2));
        assertTrue(!testInstance.getColumnHeaders().isPresent());
        verify(m_source, times(0)).next();

        // test with read
        stubSource(TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(-1, 0, 2));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        verify(m_source, times(2)).next();
        assertTrue(!testInstance.getColumnHeaders().isPresent());
    }

    /**
     * Tests if the getColumnHeaders returns an Optional.emty if no column header is set, but reading is limited to 0.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeadersReturnsOptionalEmptydWithLimitedReadEqualToZero() throws IOException {
        // test without read
        stubSource(TestPair.create(3, m_skipRow));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(-1, 0, 0));
        assertTrue(!testInstance.getColumnHeaders().isPresent());
        verify(m_source, times(0)).next();

        // test with read
        stubSource(TestPair.create(3, m_skipRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(-1, 0, 0));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        verify(m_source, times(0)).next();
        assertTrue(!testInstance.getColumnHeaders().isPresent());
        verify(m_source, times(0)).next();

        // test that next does not consume rows from read
        stubSource(TestPair.create(3, m_skipRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(-1, 0, 0));
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 6 from before + 0 times from the new test
        verify(m_source, times(0)).next();
        // now move it without consuming rows from the underlying read
        for (int i = 0; i < 10; i++) {
            testInstance.next();
        }
        // nothing changed
        verify(m_source, times(0)).next();
        assertTrue(!testInstance.getColumnHeaders().isPresent());
        verify(m_source, times(0)).next();
    }

    /**
     * Tests that the column header is returned when setting the number of rows to read to 0.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeaderFoundWithLimitedReadEqualToZero() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_skipRow) //
            , TestPair.create(1, m_headerRow));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 0, 0));
        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(3)).next();

        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_skipRow) //
            , TestPair.create(1, m_headerRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 0, 0));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        verify(m_source, times(3)).next();
        columnHeaders = getColHeaders(testInstance);
        // 3 from before + 3 times from the new test
        verify(m_source, times(6)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);

        // test that next does not consume rows from read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_skipRow) //
            , TestPair.create(1, m_headerRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 0, 0));
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 6 from before + 0 times from the new test
        verify(m_source, times(6)).next();
        // now move it without consuming rows from the underlying read
        for (int i = 0; i < 10; i++) {
            testInstance.next();
        }
        // nothing changed
        verify(m_source, times(6)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(9)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests that the column header is returned when limiting the number of rows to read. The column header itself is at
     * a position that is greater than the limit value.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeaderFoundInLimitedReadAndItsPositionIsGtLimit() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow)//
            , TestPair.create(1, m_headerRow) //
            , TestPair.create(3, m_skipRow));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(4, 0, 2));
        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(5)).next();

        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow)//
            , TestPair.create(1, m_headerRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(4, 0, 2));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 5 from before + 2 times from the new test
        verify(m_source, times(7)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(10)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);

        // test that next does not consume rows from read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow)//
            , TestPair.create(1, m_headerRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(4, 0, 2));
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 10 from before + 2 times from the new test
        verify(m_source, times(12)).next();
        // now move it without consuming rows from the underlying read
        for (int i = 0; i < 10; i++) {
            testInstance.next();
        }
        // nothing changed
        verify(m_source, times(12)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(15)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests that the column header is returned when limiting the number of rows to read. The column header itself is at
     * a position that is less than the limit value.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeaderFoundInLimitedReadAndItsPositionIsLtLimit() throws IOException {

        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(3, m_dataRow) //
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_dataRow)//
            , TestPair.create(5, m_skipRow));
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(3, 0, 5));

        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(4)).next();

        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(3, m_dataRow) //
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_dataRow)//
            , TestPair.create(5, m_skipRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(3, 0, 5));

        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 4 from before + 7 times from the new test
        verify(m_source, times(10)).next();
        columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);

        // test that next does not consume rows from read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(3, m_dataRow) //
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_dataRow)//
            , TestPair.create(5, m_skipRow));
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(3, 0, 5));

        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 10 from before + 6 times from the new test
        verify(m_source, times(16)).next();
        // now move it without consuming rows from the underlying read
        for (int i = 0; i < 10; i++) {
            testInstance.next();
        }
        // nothing changed
        verify(m_source, times(16)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(16)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests that the column header is returned when limiting the number of rows to read. The column header itself is at
     * a position that equals the limit value.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testGetColumnHeaderFoundInLimitedReadAndItsPositionIsEqLimit() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_dataRow) //
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_skipRow));//
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 0, 2));
        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(3)).next();

        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_dataRow) //
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_skipRow));//
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 0, 2));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 3 from before + 2 times from the new test
        verify(m_source, times(5)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(6)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);

        // test that next does not consume rows from read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_dataRow) //
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_skipRow));//
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 0, 2));
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 6 from before + 2 times from the new test
        verify(m_source, times(8)).next();
        // now move it without consuming rows from the underlying read
        for (int i = 0; i < 10; i++) {
            testInstance.next();
        }
        // nothing changed
        verify(m_source, times(8)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(9)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests that the column header is returned when skipping rows and limiting the number of rows to read. The column
     * header itself is at a position that is less than the skip value.
     *
     * @throws IOException
     */
    @Test
    public void testGetColumnHeadersWithSkipLimitAndHeaderPositionLtSkip() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_skipRow) //
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_skipRow)//
            , TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow));//
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 4, 2));
        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(3)).next();

        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_skipRow) //
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_skipRow)//
            , TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow));//
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 4, 2));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 3 from before + 7 times from the new test
        verify(m_source, times(10)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(10)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);

        // test that next does not consume rows from read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(2, m_skipRow) //
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_skipRow)//
            , TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow));//
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(2, 4, 2));
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 10 from before + 7 times from the new test
        verify(m_source, times(17)).next();
        // now move it without consuming rows from the underlying read
        for (int i = 0; i < 10; i++) {
            testInstance.next();
        }
        // nothing changed
        verify(m_source, times(17)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(17)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests that the column header is returned when skipping rows and limiting the number of rows to read. The column
     * header position equals the skip value.
     *
     * @throws IOException
     */
    @Test
    public void testGetColumnHeadersWithSkipLimitAndHeaderPositionEqSkip() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(4, m_skipRow) //
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow));//
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(4, 4, 2));
        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(5)).next();

        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(4, m_skipRow) //
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow));//
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(4, 4, 2));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 5 from before + 7 times from the new test
        verify(m_source, times(12)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(12)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);

        // test that next does not consume rows from read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(4, m_skipRow) //
            , TestPair.create(1, m_headerRow)//
            , TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow));//
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(4, 4, 2));
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 12 from before + 7 times from the new test
        verify(m_source, times(19)).next();
        // now move it without consuming rows from the underlying read
        for (int i = 0; i < 10; i++) {
            testInstance.next();
        }
        // nothing changed
        verify(m_source, times(19)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(19)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests that the column header is returned when skipping rows and limiting the number of rows to read. The column
     * header itself is at a position 0.
     *
     * @throws IOException
     */
    @Test
    public void testGetColumnHeadersWithSkipLimitAndHeaderPositionZero() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(1, m_headerRow)//
            , TestPair.create(4, m_skipRow)//
            , TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow));//
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(0, 4, 2));
        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(1)).next();

        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(1, m_headerRow)//
            , TestPair.create(4, m_skipRow)//
            , TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow));//
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(0, 4, 2));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 1 from before + 7 times from the new test
        verify(m_source, times(8)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(8)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);

        // test that next does not consume rows from read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(1, m_headerRow)//
            , TestPair.create(4, m_skipRow)//
            , TestPair.create(2, m_dataRow) //
            , TestPair.create(2, m_skipRow));//
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(0, 4, 2));
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 8 from before + 7 times from the new test
        verify(m_source, times(15)).next();
        // now move it without consuming rows from the underlying read
        for (int i = 0; i < 10; i++) {
            testInstance.next();
        }
        // nothing changed
        verify(m_source, times(15)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(15)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests that the column header is returned when skipping rows and limiting the number of rows to read. The column
     * header itself is at a position that is after the skipped rows and before the max number of rows to read.
     *
     * @throws IOException
     */
    @Test
    public void testGetColumnHeadersWithSkipLimitAndHeaderPositionBetweenSkipEnd() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(4, m_skipRow) //
            , TestPair.create(1, m_dataRow), TestPair.create(1, m_headerRow)//
            , TestPair.create(1, m_dataRow) //
            , TestPair.create(2, m_skipRow));//
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(5, 4, 2));
        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(6)).next();

        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(4, m_skipRow) //
            , TestPair.create(1, m_dataRow), TestPair.create(1, m_headerRow)//
            , TestPair.create(1, m_dataRow) //
            , TestPair.create(2, m_skipRow));//
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(5, 4, 2));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 6 from before + 7 times from the new test
        verify(m_source, times(13)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(13)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);

        // test that next does not consume rows from read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(4, m_skipRow) //
            , TestPair.create(1, m_dataRow), TestPair.create(1, m_headerRow)//
            , TestPair.create(1, m_dataRow) //
            , TestPair.create(2, m_skipRow));//
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(5, 4, 2));

        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 13 from before + 7 times from the new test
        verify(m_source, times(20)).next();
        // now move it without consuming rows from the underlying read
        for (int i = 0; i < 10; i++) {
            testInstance.next();
        }
        // nothing changed
        verify(m_source, times(20)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(20)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    /**
     * Tests that the column header is returned when skipping rows and limiting the number of rows to read. The column
     * header itself is at a position that is after skipped + limit.
     *
     * @throws IOException
     */
    @Test
    public void testGetColumnHeadersWithSkipLimitAndHeaderPositionAfterLimit() throws IOException {
        // test without read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(4, m_skipRow) //
            , TestPair.create(3, m_dataRow) //
            , TestPair.create(2, m_skipRow)//
            , TestPair.create(1, m_headerRow));//
        ExtractColumnHeaderRead<String> testInstance =
            new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(9, 4, 3));
        String[] columnHeaders = getColHeaders(testInstance);
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
        verify(m_source, times(10)).next();

        // test with read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(4, m_skipRow) //
            , TestPair.create(3, m_dataRow) //
            , TestPair.create(2, m_skipRow)//
            , TestPair.create(1, m_headerRow));//
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(9, 4, 3));
        RandomAccessible<String> dataRow;
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 10 from before + 7 times from the new test
        verify(m_source, times(17)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(20)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);

        // test that next does not consume rows from read
        stubHeaderCol("foo", "bar");
        stubSource(TestPair.create(4, m_skipRow) //
            , TestPair.create(3, m_dataRow) //
            , TestPair.create(2, m_skipRow)//
            , TestPair.create(1, m_headerRow));//
        testInstance = new DefaultExtractColumnHeaderRead<>(m_source, setupConfig(9, 4, 3));
        while ((dataRow = testInstance.next()) != null) {
            assertEquals(m_dataRow, dataRow);
        }
        // 20 from before + 7 times from the new test
        verify(m_source, times(27)).next();
        // now move it without consuming rows from the underlying read
        for (int i = 0; i < 10; i++) {
            testInstance.next();
        }
        // nothing changed
        verify(m_source, times(27)).next();
        columnHeaders = getColHeaders(testInstance);
        verify(m_source, times(30)).next();
        assertArrayEquals(new String[]{"foo", "bar"}, columnHeaders);
    }

    private String[] getColHeaders(final ExtractColumnHeaderRead<String> testInstance) throws IOException {
        Optional<RandomAccessible<String>> columnHeaders = testInstance.getColumnHeaders();
        assertTrue(columnHeaders.isPresent());
        assertEquals(m_headerRow, columnHeaders.get());
        // Object to string ensures that the framework checks and treats nulls
        final String[] colHeaders = convertToStringArray(columnHeaders.get());
        return colHeaders;
    }

    private static String[] convertToStringArray(final RandomAccessible<String> randomAccessible) {
        return randomAccessible.stream()//
            .map(val -> val != null //
                ? val //
                : null)//
            .toArray(String[]::new);
    }

    private void stubSource(final TestPair... rows) throws IOException {
        @SuppressWarnings("unchecked")
        final RandomAccessible<String>[] rowArray = Arrays.stream(rows)//
            .map(p -> Collections.nCopies(p.getFirst(), p.getSecond()))//
            .flatMap(c -> c.stream())//
            .toArray(RandomAccessible[]::new);
        when(m_source.next()).thenAnswer(new Answer<RandomAccessible<String>>() {

            int idx = -1;

            @Override
            public RandomAccessible<String> answer(final InvocationOnMock invocation) throws Throwable {
                if (++idx < rowArray.length) {
                    return rowArray[idx];
                }
                return null;
            }
        });
    }

    private void stubHeaderCol(final String... elements) {
        when(m_headerRow.size()).thenReturn(elements.length);
        for (int i = 0; i < elements.length; i++) {
            when(m_headerRow.get(i)).thenReturn(elements[i]);
        }
        when(m_headerRow.copy()).thenReturn(m_headerRow);
        when(m_headerRow.stream()).thenReturn(Arrays.stream(elements));
    }

    private static TableReadConfig<?> setupConfig(final long colIdx) {
        return setupConfig(colIdx, -1, -1);
    }

    private static TableReadConfig<?> setupConfig(final long colIdx, final long skipRows, final long maxRowsToRead) {
        final TableReadConfig<?> config = mock(TableReadConfig.class);
        if (colIdx >= 0) {
            when(config.useColumnHeaderIdx()).thenReturn(true);
            when(config.getColumnHeaderIdx()).thenReturn(colIdx);
        } else {
            when(config.useColumnHeaderIdx()).thenReturn(false);
        }

        if (skipRows >= 0) {
            when(config.skipRows()).thenReturn(true);
            when(config.getNumRowsToSkip()).thenReturn(skipRows);
        } else {
            when(config.skipRows()).thenReturn(false);
        }

        if (maxRowsToRead >= 0) {
            when(config.limitRowsForSpec()).thenReturn(true);
            when(config.getMaxRowsForSpec()).thenReturn(maxRowsToRead);
        } else {
            when(config.limitRowsForSpec()).thenReturn(false);
        }
        return config;
    }

    private static class TestPair {

        final int m_first;

        final RandomAccessible<String> m_second;

        private TestPair(final int first, final RandomAccessible<String> second) {
            m_first = first;
            m_second = second;
        }

        int getFirst() {
            return m_first;
        }

        RandomAccessible<String> getSecond() {
            return m_second;
        }

        static TestPair create(final int first, final RandomAccessible<String> second) {
            return new TestPair(first, second);
        }

    }
}
