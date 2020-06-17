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
package org.knime.filehandling.core.node.table.reader.type.mapping;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMappingTestUtils.TestReadAdapter;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for DefaultTypeMapper
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultTypeMapperTest {

    @Mock
    private FileStoreFactory m_fsFactory;

    @Mock
    private RandomAccessible<String> m_randomAccessible;

    private RowKey m_key = new RowKey("test");

    private DefaultTypeMapper<String> m_testInstance;

    /**
     * Initializes the test instance before each unit test.
     */
    @Before
    public void init() {
        ReadAdapter<String, String> readAdapter = new TestReadAdapter();
        m_testInstance = new DefaultTypeMapper<>(readAdapter,
            TypeMappingTestUtils.mockProductionPaths("berta", "frieda"), m_fsFactory);
    }

    /**
     * Tests if the {@code map} method fails if the @{@code key} argument is {@code null}.
     *
     * @throws Exception (in this case an {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMapFailsOnNullKey() throws Exception {
        m_testInstance.map(null, m_randomAccessible);
    }

    /**
     * Tests if the {@code map} method fails if the @{@link RandomAccessible randomAccessible} argument is {@code null}.
     *
     * @throws Exception in this case an {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMapFailsOnNullRandomAccessible() throws Exception {
        m_testInstance.map(m_key, null);
    }

    /**
     * Tests if the {@code map} method fails if it is called with a {@link RandomAccessible} of incompatible size.
     *
     * @throws Exception in this case an {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMapFailsOnRandomAccessibleOfDifferentSize() throws Exception {
        when(m_randomAccessible.size()).thenReturn(3);
        m_testInstance.map(m_key, m_randomAccessible);
    }

    /**
     * Tests the {@code map} implementation.
     *
     * @throws Exception never thrown
     */
    @Test
    public void testMap() throws Exception {
        when(m_randomAccessible.size()).thenReturn(2);
        when(m_randomAccessible.get(0)).thenReturn("hans");
        when(m_randomAccessible.get(1)).thenReturn("franz");
        DataRow actual = m_testInstance.map(m_key, m_randomAccessible);
        DataRow expected = new DefaultRow(m_key, new StringCell("hans"), new StringCell("franz"));
        assertEquals(expected, actual);
    }

}
