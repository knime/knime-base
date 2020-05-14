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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy.TypeResolver;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapping;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMappingFactory;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for {@code DefaultMultiTableReadFactory}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultMultiTableReadFactoryTest {

    private static final String ROOT_PATH = "path";

    @Mock
    private TypeMappingFactory<String, String> m_typeMappingFactory;

    @Mock
    private TypeMapping<String> m_typeMapping;

    @Mock
    private TypeHierarchy<String, String> m_typeHierarchy;

    @Mock
    private TypeResolver<String, String> m_typeResolver;

    @Mock
    private RowKeyGeneratorContextFactory<String> m_rowKeyGenFactory;

    @Mock
    private RowKeyGenerator<String> m_rowKeyGen;

    @Mock
    private Path m_path;

    @Mock
    private MultiTableReadConfig<?> m_config;

    private DefaultMultiTableReadFactory<String, String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_testInstance = new DefaultMultiTableReadFactory<>(m_typeMappingFactory, m_typeHierarchy, m_rowKeyGenFactory);
    }

    /**
     * Tests the {@code create} implementation.
     */
    @Test
    public void testCreate() {
        final TypedReaderTableSpec<String> spec = TypedReaderTableSpec.create(asList("hans"), asList("berta"));
        when(m_typeMappingFactory.create(spec)).thenReturn(m_typeMapping);
        DataTableSpec knimeSpec = new DataTableSpec(new String[]{"hans"}, new DataType[]{StringCell.TYPE});
        when(m_typeMapping.map(spec)).thenReturn(knimeSpec);
        when(m_typeHierarchy.createResolver()).thenReturn(m_typeResolver);
        when(m_typeResolver.getMostSpecificType()).thenReturn("berta");
        when(m_config.getSpecMergeMode()).thenReturn(SpecMergeMode.UNION);

        MultiTableRead<String> multiTableRead =
            m_testInstance.create(ROOT_PATH, Collections.singletonMap(m_path, spec), m_config);
        assertEquals(knimeSpec, multiTableRead.getOutputSpec());
        verify(m_typeMappingFactory).create(spec);
        verify(m_typeMapping).map(spec);

    }

}
