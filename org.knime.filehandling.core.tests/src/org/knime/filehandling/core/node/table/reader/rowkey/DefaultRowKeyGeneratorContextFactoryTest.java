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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.RowKey;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for {@link DefaultRowKeyGeneratorContextFactory}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultRowKeyGeneratorContextFactoryTest {

    @Mock
    private TableReadConfig<?> m_config;

    @Mock
    private RandomAccessible<String> m_randomAccessible;

    @Mock
    private Path m_path;

    private DefaultRowKeyGeneratorContextFactory<Path, String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_testInstance = new DefaultRowKeyGeneratorContextFactory<>(Object::toString, "File");
    }

    /**
     * Tests if the factory correctly creates a context that produces extracting key generators if a rowid index is
     * provided.
     */
    @Test
    public void testCreateExtractingContext() {
        when(m_config.useRowIDIdx()).thenReturn(true);
        when(m_config.getRowIDIdx()).thenReturn(1);
        GenericRowKeyGeneratorContext<Path, String> context = m_testInstance.createContext(m_config);
        when(m_randomAccessible.get(1)).thenReturn("foo");
        when(m_randomAccessible.size()).thenReturn(3);
        RowKeyGenerator<String> keyGen = context.createKeyGenerator(m_path);
        assertEquals(new RowKey("foo"), keyGen.createKey(m_randomAccessible));
        verify(m_randomAccessible, Mockito.times(1)).get(1);
    }

    /**
     * Tests if the factory correctly creates a counting context if no rowid idx is provided.
     */
    @Test
    public void testCreateCountingContext() {
        GenericRowKeyGeneratorContext<Path, String> context = m_testInstance.createContext(m_config);
        assertTrue(context instanceof ContinuousCountingRowKeyGeneratorContext);
    }
}
