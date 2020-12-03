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
 *   Mar 30, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.util.IndividualTableReader;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.util.CheckedExceptionFunction;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for DefaultMultiTableRead.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("resource")
@RunWith(MockitoJUnitRunner.class)
public class DefaultMultiTableReadTest {

    private static final String[][] TEST_TABLE = m(//
        a("a", "b", "c"), //
        a("d", "e", "f"), //
        a("g", "h", "i"), //
        a("j", "k", "l")//
    );

    @Mock
    private Path m_path1;

    @Mock
    private Path m_path2;

    @Mock
    private Path m_unknownPath;

    @Mock
    private FileStoreFactory m_fsFactory;

    @Mock
    private TableReadConfig<?> m_config;

    @Mock
    private CheckedExceptionFunction<Path, Read<Path, String>, IOException> m_readFn;

    @Mock
    private BiFunction<Path, FileStoreFactory, IndividualTableReader<Path, String>> m_individualTableReaderFactory;

    @Mock
    private IndividualTableReader<Path, String> m_individualTableReader;

    @Mock
    private RowOutput m_rowOutput;

    @Mock
    private ExecutionMonitor m_exec;

    @Mock
    private TableSpecConfig m_tableSpecConfig;

    private DataTableSpec m_outputSpec;

    private DefaultMultiTableRead<Path, String> m_testInstance;

    private void stubReadFn() throws IOException {
        Read<Path, String> read1 = mockRead(TEST_TABLE[0], TEST_TABLE[1]);
        when(m_readFn.apply(m_path1)).thenReturn(read1);
        Read<Path, String> read2 = mockRead(TEST_TABLE[2], TEST_TABLE[3]);
        when(m_readFn.apply(m_path2)).thenReturn(read2);
    }

    private static Read<Path, String> mockRead(final String[]... rows) throws IOException {
        @SuppressWarnings("unchecked")
        final Read<Path, String> read = mock(Read.class);
        RandomAccessible<String> first = mockRandomAccessible();
        @SuppressWarnings("unchecked")
        RandomAccessible<String>[] restList = Arrays.stream(rows)//
            .skip(1)//
            .map(r -> mockRandomAccessible())//
            .toArray(i -> new RandomAccessible[i + 1]);
        when(read.next()).thenReturn(first, restList);
        return read;
    }

    private static String[] a(final String... values) {
        return values;
    }

    private static String[][] m(final String[]... rows) {
        return rows;
    }

    private static DataRow toRow(final long idx, final String... row) {
        return new DefaultRow(RowKey.createRowKey(idx), row);
    }

    private static RandomAccessible<String> mockRandomAccessible() {
        @SuppressWarnings("unchecked")
        final RandomAccessible<String> randomAccessible = mock(RandomAccessible.class);
        return randomAccessible;
    }

    /**
     * Initializes the test instance before running each test.
     */
    @Before
    public void init() {
        m_outputSpec = new DataTableSpec(new String[]{"hans", "franz", "gunter"},
            new DataType[]{StringCell.TYPE, StringCell.TYPE, StringCell.TYPE});
        m_testInstance = new DefaultMultiTableRead<>(asList(m_path1, m_path2), m_readFn,
            () -> m_individualTableReaderFactory, m_tableSpecConfig, m_outputSpec);
    }

    /**
     * Tests the {@link MultiTableRead#getOutputSpec()} implementation.
     */
    @Test
    public void testGetOuptutSpec() {
        assertEquals(m_outputSpec, m_testInstance.getOutputSpec());
    }

    /**
     * Tests the {@link MultiTableRead#getTableSpecConfig()} implementation.
     */
    @Test
    public void testGetTableSpecConfig() {
        assertEquals(m_tableSpecConfig, m_testInstance.getTableSpecConfig());
    }

    private void stubIndividualTableReaderFactory() {
        when(m_individualTableReaderFactory.apply(any(), any())).thenReturn(m_individualTableReader);
    }

    /**
     * Tests the {@link MultiTableRead#fillRowOutput(RowOutput, org.knime.core.node.ExecutionMonitor, FileStoreFactory)}
     * implementation.
     *
     * @throws Exception
     */
    @Test
    public void testFillRowOutput() throws Exception {
        stubReadFn();
        stubIndividualTableReaderFactory();
        when(m_exec.createSubProgress(anyDouble())).thenReturn(mock(ExecutionMonitor.class));

        m_testInstance.fillRowOutput(m_rowOutput, m_exec, m_fsFactory);

        verify(m_individualTableReaderFactory).apply(m_path1, m_fsFactory);
        verify(m_individualTableReaderFactory).apply(m_path2, m_fsFactory);
        verify(m_individualTableReader, times(2)).fillOutput(any(), eq(m_rowOutput), any());
    }

    /**
     * Tests the {@link MultiTableRead#createPreviewIterator()} implementation.
     * @throws Exception
     */
    @Test
    public void testCreatePreviewIterator() throws Exception {
        stubReadFn();
        stubIndividualTableReaderFactory();
        when(m_individualTableReader.toRow(any())).thenReturn(toRow(0, TEST_TABLE[0]),
            IntStream.range(1, TEST_TABLE.length)
            .mapToObj(i -> toRow(i, TEST_TABLE[i]))
            .toArray(DataRow[]::new));
        try (final PreviewRowIterator iterator = m_testInstance.createPreviewIterator()) {
            for (int i = 0; i < TEST_TABLE.length; i++) {
                assertTrue(iterator.hasNext());
                assertEquals(new DefaultRow(RowKey.createRowKey((long)i), TEST_TABLE[i]), iterator.next());
            }
            assertFalse(iterator.hasNext());
        }
    }

}
