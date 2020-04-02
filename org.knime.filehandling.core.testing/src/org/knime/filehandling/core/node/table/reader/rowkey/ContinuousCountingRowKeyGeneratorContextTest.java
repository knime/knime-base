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
 *   Apr 2, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.rowkey;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.RowKey;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for ContinuousCountingRowKeyGeneratorContext.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class ContinuousCountingRowKeyGeneratorContextTest {

    @Mock
    private RandomAccessible<String> m_randomAccessible = null;

    @Mock
    private Path m_path1 = null;

    @Mock
    private Path m_path2 = null;

    private ContinuousCountingRowKeyGeneratorContext<String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_testInstance = new ContinuousCountingRowKeyGeneratorContext<>();
    }

    /**
     * Tests if the key generator works in case of a single path.
     */
    @Test
    public void testSinglePath() {
        RowKeyGenerator<String> keyGen = m_testInstance.createKeyGenerator(m_path1);
        assertEquals(RowKey.createRowKey(0L), keyGen.createKey(m_randomAccessible));
        assertEquals(RowKey.createRowKey(1L), keyGen.createKey(m_randomAccessible));
        assertEquals(RowKey.createRowKey(2L), keyGen.createKey(m_randomAccessible));
        verifyZeroInteractions(m_randomAccessible);
    }

    /**
     * Tests if the key generator works if two paths are read sequentially.
     */
    @Test
    public void testMultiplePathsSequentially() {
        RowKeyGenerator<String> keyGen1 = m_testInstance.createKeyGenerator(m_path1);
        assertEquals(RowKey.createRowKey(0L), keyGen1.createKey(m_randomAccessible));
        assertEquals(RowKey.createRowKey(1L), keyGen1.createKey(m_randomAccessible));
        assertEquals(RowKey.createRowKey(2L), keyGen1.createKey(m_randomAccessible));
        RowKeyGenerator<String> keyGen2 = m_testInstance.createKeyGenerator(m_path2);
        assertEquals(RowKey.createRowKey(3L), keyGen2.createKey(m_randomAccessible));
        assertEquals(RowKey.createRowKey(4L), keyGen2.createKey(m_randomAccessible));
        assertEquals(RowKey.createRowKey(5L), keyGen2.createKey(m_randomAccessible));
        verifyZeroInteractions(m_randomAccessible);
    }

    /**
     * Tests if the key generator works if two paths are read alternatingly.
     */
    @Test
    public void testMultiplePathsAlternatingly() {
        RowKeyGenerator<String> keyGen1 = m_testInstance.createKeyGenerator(m_path1);
        RowKeyGenerator<String> keyGen2 = m_testInstance.createKeyGenerator(m_path2);
        assertEquals(RowKey.createRowKey(0L), keyGen1.createKey(m_randomAccessible));
        assertEquals(RowKey.createRowKey(1L), keyGen2.createKey(m_randomAccessible));
        assertEquals(RowKey.createRowKey(2L), keyGen1.createKey(m_randomAccessible));
        assertEquals(RowKey.createRowKey(3L), keyGen2.createKey(m_randomAccessible));
        assertEquals(RowKey.createRowKey(4L), keyGen2.createKey(m_randomAccessible));
        assertEquals(RowKey.createRowKey(5L), keyGen1.createKey(m_randomAccessible));
        verifyZeroInteractions(m_randomAccessible);
    }

}
