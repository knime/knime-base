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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
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

    private static final String FAKE_PATH = "fake_path";

    private static final String ROOT_PATH = "path";

    @Mock
    private TypeMappingFactory<DummyReaderSpecificConfig, String, String> m_typeMappingFactory;

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
    private MultiTableReadConfig<DummyReaderSpecificConfig> m_config;

    @Mock
    private DummyReaderSpecificConfig m_readerSpecificConfig;

    @Mock
    private TableReadConfig<DummyReaderSpecificConfig> m_tableReadConfig;

    private DefaultMultiTableReadFactory<DummyReaderSpecificConfig, String, String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_testInstance = new DefaultMultiTableReadFactory<>(m_typeMappingFactory, m_typeHierarchy, m_rowKeyGenFactory);
        when(m_config.getTableReadConfig()).thenReturn(m_tableReadConfig);
        when (m_tableReadConfig.getReaderSpecificConfig()).thenReturn(m_readerSpecificConfig);
    }

    /**
     * Tests the {@link DefaultMultiTableReadFactory#create(String, java.util.Map, MultiTableReadConfig)}
     * implementation.
     */
    @Test
    public void testCreate() {
        final String colName = "hans";
        final TypedReaderTableSpec<String> spec =
            TypedReaderTableSpec.create(asList(colName), asList("berta"), asList(Boolean.TRUE));
        when(m_typeMappingFactory.create(spec, m_readerSpecificConfig)).thenReturn(m_typeMapping);
        DataTableSpec knimeSpec = new DataTableSpec(new String[]{colName}, new DataType[]{StringCell.TYPE});
        when(m_typeMapping.map(spec)).thenReturn(knimeSpec);
        when(m_typeHierarchy.createResolver()).thenReturn(m_typeResolver);
        when(m_typeResolver.getMostSpecificType()).thenReturn("berta");
        when(m_config.getSpecMergeMode()).thenReturn(SpecMergeMode.UNION);
        when(m_typeMapping.getProductionPaths()).thenReturn(
            TableSpecConfigUtils.getProducerRegistry().getAvailableProductionPaths().toArray(new ProductionPath[0]));
        final Path p = TableSpecConfigUtils.mockPath(FAKE_PATH);

        MultiTableRead<String> multiTableRead =
            m_testInstance.create(ROOT_PATH, Collections.singletonMap(p, spec), m_config);
        assertEquals(knimeSpec, multiTableRead.getOutputSpec());
        verify(m_typeMappingFactory).create(spec, m_readerSpecificConfig);
        verify(m_typeMapping).map(spec);

        // check that the proper TableSpecConfig is created
        final TableSpecConfig tableSpecCfg = multiTableRead.createTableSpec();
        assertEquals(knimeSpec, tableSpecCfg.getDataTableSpec());
        assertTrue(tableSpecCfg.getPaths().size() == 1);
        assertEquals(p.toString(), tableSpecCfg.getPaths().get(0));
        assertTrue(tableSpecCfg.getSpec(p.toString()) != null);

        final ReaderTableSpec<?> readerTableSpec = tableSpecCfg.getSpec(p.toString());
        assertTrue(readerTableSpec.size() == 1);
        assertTrue(readerTableSpec.getColumnSpec(0).getName().isPresent());
        assertEquals(colName, readerTableSpec.getColumnSpec(0).getName().get());
    }

    /**
     * Tests the {@link DefaultMultiTableReadFactory#create(String, java.util.List, MultiTableReadConfig)}
     * implementation.
     */
    @Test
    public void testCreateFromTableSpecConfig() {
        final DataTableSpec knimeSpec = TableSpecConfigUtils.createDataTableSpec("A", "B");
        final ProducerRegistry<String, TableSpecConfigUtils> registry = TableSpecConfigUtils.getProducerRegistry();
        final ProductionPath[] prodPaths = registry.getAvailableProductionPaths().toArray(new ProductionPath[0]);
        when(m_typeMapping.getProductionPaths()).thenReturn(prodPaths);

        // create individual specs
        final Map<Path, ReaderTableSpec<?>> individualSpecs = new HashMap<>();
        final Path p1 = TableSpecConfigUtils.mockPath(FAKE_PATH);
        final Path p2 = TableSpecConfigUtils.mockPath("another_path");
        individualSpecs.put(p1, ReaderTableSpec.createReaderTableSpec(Arrays.asList("C", "D")));
        individualSpecs.put(p2, ReaderTableSpec.createReaderTableSpec(Arrays.asList("X", "Y")));

        final TableSpecConfig cfg = new TableSpecConfig(ROOT_PATH, knimeSpec, individualSpecs, prodPaths);
        when(m_config.getTableSpecConfig()).thenReturn(cfg);
        when(m_typeMappingFactory.create(cfg, m_readerSpecificConfig)).thenReturn(m_typeMapping);

        MultiTableRead<String> multiTableRead = m_testInstance.create(ROOT_PATH, Arrays.asList(p1, p2), m_config);
        assertEquals(knimeSpec, multiTableRead.getOutputSpec());

        final TableSpecConfig tableSpecCfg = multiTableRead.createTableSpec();
        assertEquals(cfg, tableSpecCfg);

    }


}
