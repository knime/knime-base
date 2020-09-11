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

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
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
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorContext;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapper;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapping;
import org.knime.filehandling.core.node.table.reader.util.IndexMapper;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.util.CheckedExceptionFunction;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
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
    private TypeMapping<String> m_typeMapping;

    @Mock
    private TypeMapper<String> m_typeMapper;

    @Mock
    private Path m_path1;

    @Mock
    private Path m_path2;

    @Mock
    private Path m_unknownPath;

    @Mock
    private RowKeyGeneratorContext<String> m_keyGenContext;

    @Mock
    private RowKeyGenerator<String> m_keyGen;

    @Mock
    private FileStoreFactory m_fsFactory;

    @Mock
    private TableReadConfig<?> m_config;

    @Mock
    private CheckedExceptionFunction<Path, Read<String>, IOException> m_readFn;

    private Map<Path, IndexMapper> m_indexMappers;

    @Mock
    private RowOutput m_rowOutput;

    @Mock
    private ExecutionMonitor m_exec;

    @Mock
    private TableSpecConfig m_tableSpecConfig;

    private DataTableSpec m_outputSpec;

    private DefaultMultiTableRead<String> m_testInstance;

    private Map<Path, IndexMapper> mockIndexMappers() {
        final Map<Path, IndexMapper> mappers = new LinkedHashMap<>();
        mappers.put(m_path1, mock(IndexMapper.class));
        mappers.put(m_path2, mock(IndexMapper.class));
        return mappers;
    }

    private void stubIndexMappers() {
        for (IndexMapper mock : m_indexMappers.values()) {
            when(mock.hasMapping(anyInt())).thenReturn(true);
            when(mock.map(anyInt())).then(invocation -> invocation.getArgument(0));
            when(mock.getIndexRangeEnd()).thenReturn(OptionalInt.of(2));
        }
    }

    private void stubReadFn() throws IOException {
        Read<String> read1 = mockRead(TEST_TABLE[0], TEST_TABLE[1]);
        when(m_readFn.apply(m_path1)).thenReturn(read1);
        Read<String> read2 = mockRead(TEST_TABLE[2], TEST_TABLE[3]);
        when(m_readFn.apply(m_path2)).thenReturn(read2);
    }

    private static Read<String> mockRead(final String[]... rows) throws IOException {
        final Read<String> read = mock(Read.class);
        RandomAccessible<String> first = mockRandomAccessible(rows[0]);
        RandomAccessible<String>[] restList = Arrays.stream(rows)//
                .skip(1)//
                .map(r -> mockRandomAccessible(r))//
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

    private static List<DataRow> toRows(final String[]... rows) {
        return IntStream.range(0, rows.length)//
            .mapToObj(i -> toRow(i, rows[i]))//
            .collect(toList());
    }

    private static DataRow toRow(final long idx, final String... row) {
        return new DefaultRow(RowKey.createRowKey(idx), row);
    }

    private static RandomAccessible<String> mockRandomAccessible(final String... values) {
        final RandomAccessible<String> randomAccessible = mock(RandomAccessible.class);
        when(randomAccessible.get(ArgumentMatchers.anyInt()))
            .thenAnswer(invocation -> values[(int)invocation.getArgument(0)]);
        when(randomAccessible.size()).thenReturn(values.length);
        return randomAccessible;
    }

    private void stubForReading() throws Exception {
        stubIndexMappers();
        stubReadFn();
        stubRowKeyGeneration();
        stubTypeMapping();
    }

    private void stubRowKeyGeneration() {
        when(m_keyGenContext.createKeyGenerator(any())).thenReturn(m_keyGen);
        RowKey[] keys = IntStream.range(1, TEST_TABLE.length).mapToObj(i -> RowKey.createRowKey((long)i)).toArray(RowKey[]::new);
        when(m_keyGen.createKey(any())).thenReturn(RowKey.createRowKey(0L), keys);
    }

    private void stubTypeMapping() throws Exception {
        when(m_typeMapping.createTypeMapper(any())).thenReturn(m_typeMapper);
        when(m_typeMapper.map(any(), any())).then(invocation -> {
           final RowKey key = invocation.getArgument(0);
           final RandomAccessible<String> row = invocation.getArgument(1);
           String[] values = row.stream().toArray(String[]::new);
           return new DefaultRow(key, values);
        });
    }

    /**
     * Initializes the test instance before running each test.
     */
    @Before
    public void init() {
        m_outputSpec = new DataTableSpec(new String[]{"hans", "franz", "gunter"},
            new DataType[]{StringCell.TYPE, StringCell.TYPE, StringCell.TYPE});
        Mockito.when(m_tableSpecConfig.getDataTableSpec()).thenReturn(m_outputSpec);
        m_indexMappers = mockIndexMappers();
        m_testInstance =
            new DefaultMultiTableRead<>(m_readFn, m_tableSpecConfig, m_typeMapping, m_keyGenContext, m_indexMappers);
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

    /**
     * Tests the {@link MultiTableRead#fillRowOutput(RowOutput, org.knime.core.node.ExecutionMonitor, FileStoreFactory)}
     * implementation.
     *
     * @throws Exception
     */
    @Test
    public void testFillRowOutput() throws Exception {
        stubForReading();
        when(m_exec.createSubProgress(anyDouble())).thenReturn(mock(ExecutionMonitor.class));

        m_testInstance.fillRowOutput(m_rowOutput, m_exec, m_fsFactory);

        ArgumentCaptor<DataRow> captor = ArgumentCaptor.forClass(DataRow.class);
        Mockito.verify(m_rowOutput, times(4)).push(captor.capture());
        List<DataRow> capturedRows = captor.getAllValues();
        final List<DataRow> expected = toRows(TEST_TABLE);
        assertEquals(expected, capturedRows);
    }

    /**
     * Tests the {@link MultiTableRead#createPreviewIterator(FileStoreFactory)} implementation.
     * @throws Exception
     */
    @Test
    public void testCreatePreviewIterator() throws Exception {
        stubForReading();

        try (final PreviewRowIterator iterator = m_testInstance.createPreviewIterator()) {
            for (int i = 0; i < TEST_TABLE.length; i++) {
                assertTrue(iterator.hasNext());
                assertEquals(new DefaultRow(RowKey.createRowKey((long)i), TEST_TABLE[i]), iterator.next());
            }
        }
    }

}
