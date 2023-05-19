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
package org.knime.filehandling.core.node.table.reader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.OptionalLong;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessibleDecorator;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapper;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for DefaultIndividualTableReader.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("resource")
public class DefaultIndividualTableReaderTest {

    @Mock
    private TypeMapper<String> m_typeMapper = null;

    @Mock
    private RandomAccessibleDecorator<String> m_idxMapper = null;

    @Mock
    private RowKeyGenerator<String> m_rowKeyGenerator = null;

    @Mock
    private Read<String> m_read = null;

    @Mock
    private RandomAccessible<String> m_randomAccessible = null;

    @Mock
    private DataRow m_first = null;

    @Mock
    private DataRow m_second = null;

    @Mock
    private RowOutput m_output = null;

    @Mock
    private ExecutionMonitor m_monitor = null;

    private DefaultIndividualTableReader<String> m_testInstance = null;

    /**
     * Initializes the test instance.
     *
     * @throws Exception never thrown
     */
    @SuppressWarnings("unchecked")
    @Before
    public void init() throws Exception {
        m_testInstance = new DefaultIndividualTableReader<>(m_typeMapper, m_idxMapper, m_rowKeyGenerator, null);
        RowKey firstKey = RowKey.createRowKey(0L);
        RowKey secondKey = RowKey.createRowKey(1L);
        when(m_rowKeyGenerator.createKey(any())).thenReturn(firstKey, secondKey);
        when(m_read.next()).thenReturn(m_randomAccessible, m_randomAccessible, null);
        when(m_typeMapper.map(any(), any())).thenReturn(m_first, m_second);
    }

    /**
     * Tests the {@code fillOutput} method if the read knows its max progress.
     *
     * @throws Exception never thrown
     */
    @Test
    public void testFillRowOutputWithPogress() throws Exception {
        when(m_read.getMaxProgress()).thenReturn(OptionalLong.of(10));
        m_testInstance.fillOutput(m_read, m_output, m_monitor);
        verify(m_read, times(3)).next();
        // getProgress is only called every 973 rows
        verify(m_read, never()).getProgress();
        verify(m_output).push(m_first);
        verify(m_output).push(m_second);
    }

    /**
     * Tests the {@code fillOutput} method if the read doesn't know its max progress.
     *
     * @throws Exception never thrown
     */
    @Test
    public void testFillRowOutputWithoutPogress() throws Exception {
        when(m_read.getMaxProgress()).thenReturn(OptionalLong.empty());
        m_testInstance.fillOutput(m_read, m_output, m_monitor);
        verify(m_read, times(3)).next();
        verify(m_output).push(m_first);
        verify(m_output).push(m_second);
    }
}
