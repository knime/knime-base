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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorContext;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapper;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapping;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for DefaultMultiTableRead.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultMultiTableReadTest {

    private static final String ROOT_PATH = "path";

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

    private DataTableSpec m_outputSpec;

    private Map<Path, TypedReaderTableSpec<String>> m_individualSpecs;

    private DefaultMultiTableRead<String> m_testInstance;

    /**
     * Initializes the test instance before running each test.
     */
    @Before
    public void init() {
        m_individualSpecs = new HashMap<>();
        m_individualSpecs.put(m_path1, TypedReaderTableSpec.create(asList("hans", "franz"), asList("ilsa", "berta")));
        m_individualSpecs.put(m_path2, TypedReaderTableSpec.create(asList("gunter", "franz"), asList("ilsa", "berta")));
        m_outputSpec = new DataTableSpec(new String[]{"hans", "franz", "gunter"},
            new DataType[]{StringCell.TYPE, StringCell.TYPE, StringCell.TYPE});
        m_testInstance =
            new DefaultMultiTableRead<>(ROOT_PATH, m_individualSpecs, m_outputSpec, m_typeMapping, m_keyGenContext);

    }

    /**
     * Tests the {@code getOutputSpec} implementation.
     */
    @Test
    public void testGetOuptutSpec() {
        assertEquals(m_outputSpec, m_testInstance.getOutputSpec());
    }

    /**
     * Tests the {@code isValidFor} implementation.
     */
    @Test
    public void testIsValidFor() {
        assertTrue(m_testInstance.isValidFor(asList(m_path1, m_path2)));
        assertFalse(m_testInstance.isValidFor(asList(m_path1)));
        assertFalse(m_testInstance.isValidFor(asList(m_path1, m_path2, m_unknownPath)));
        assertFalse(m_testInstance.isValidFor(asList(m_path1, m_unknownPath)));
    }

    /**
     * Tests if {@code createIndividualTableReader} if the provided path is unknown;
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateIndividualTableReaderFailsIfPathIsUnknown() {
        m_testInstance.createIndividualTableReader(m_unknownPath, m_config, m_fsFactory);
    }

    /**
     * Test the {@code createIndividualTableReader} implementation.
     */
    @Test
    public void testCreateIndividualTableReader() {
        when(m_typeMapping.createTypeMapper(m_fsFactory)).thenReturn(m_typeMapper);
        when(m_keyGenContext.createKeyGenerator(any())).thenReturn(m_keyGen);
        m_testInstance.createIndividualTableReader(m_path1, m_config, m_fsFactory);
        verify(m_typeMapping).createTypeMapper(m_fsFactory);
        verify(m_keyGenContext).createKeyGenerator(m_path1);
    }
}
