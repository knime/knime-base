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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.DummyReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.TableSpecConfigTestingUtils;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.selector.TransformationModel;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for {@link DefaultTypeMappingFactory}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultTypeMappingFactoryTest {

    private static final TypedReaderTableSpec<String> SPEC = TableSpecConfigTestingUtils.createTypedTableSpec(asList("A", "B"), asList("X", "Y"));

    private ProductionPath m_p1;

    private ProductionPath m_p2;

    @Mock
    private DummyReaderSpecificConfig m_config;

    @Mock
    private Function<String, ProductionPath> m_defaultProductionPathProvider;

    @Mock
    private ReadAdapterFactory<String, String> m_readAdapterFactory;

    private DefaultTypeMappingFactory<DummyReaderSpecificConfig, String, String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_p1 = TableSpecConfigTestingUtils.mockProductionPath();
        m_p2 = TableSpecConfigTestingUtils.mockProductionPath();
        m_testInstance = new DefaultTypeMappingFactory<>(m_readAdapterFactory, m_defaultProductionPathProvider);
    }

    /**
     * Tests the {@link TypeMappingFactory#create(TypedReaderTableSpec, ReaderSpecificConfig)} implementation.
     */
    @Test
    public void testCreateDefaults() {
        DataTableSpec expected = new DataTableSpec("default", new String[]{"A", "B"},
            new DataType[]{StringCell.TYPE, IntCell.TYPE});
        when(m_p1.getConverterFactory().getDestinationType()).thenReturn(StringCell.TYPE);
        when(m_p2.getConverterFactory().getDestinationType()).thenReturn(IntCell.TYPE);
        when(m_defaultProductionPathProvider.apply("X")).thenReturn(m_p1);
        when(m_defaultProductionPathProvider.apply("Y")).thenReturn(m_p2);
        TypeMapping<String> tm = m_testInstance.create(SPEC, m_config);
        assertEquals(expected, tm.map(SPEC));
        assertArrayEquals(new ProductionPath[]{m_p1, m_p2}, tm.getProductionPaths());
    }

    /**
     * Tests the implementation of {@link TypeMappingFactory#create(TableSpecConfig, ReaderSpecificConfig)}.
     */
    @Test
    public void testCreateFromTableSpecConfig() {
        final TableSpecConfig tableSpecConfig = mock(TableSpecConfig.class);
        DataTableSpec expected = new DataTableSpec("default", new String[]{"A", "B"},
            new DataType[]{DoubleCell.TYPE, LongCell.TYPE});
        when(tableSpecConfig.getProductionPaths()).thenReturn(new ProductionPath[] {m_p1, m_p2});
        when(m_p1.getConverterFactory().getDestinationType()).thenReturn(DoubleCell.TYPE);
        when(m_p2.getConverterFactory().getDestinationType()).thenReturn(LongCell.TYPE);

        TypeMapping<String> tm = m_testInstance.create(tableSpecConfig, m_config);

        assertEquals(expected, tm.map(SPEC));
        assertArrayEquals(new ProductionPath[]{m_p1, m_p2}, tm.getProductionPaths());
    }

    /**
     * Tests the implementation of
     * {@link TypeMappingFactory#create(TypedReaderTableSpec, ReaderSpecificConfig, TransformationModel)}
     */
    @Test
    public void testCreateWithTansformation() {
        TypedReaderTableSpec<String> spec =
            TableSpecConfigTestingUtils.createTypedTableSpec(asList("A", "B"), asList("X", "Y"));

        @SuppressWarnings("unchecked")
        TransformationModel<String> tm = mock(TransformationModel.class);
        when(tm.getProductionPath(spec.getColumnSpec(0))).thenReturn(m_p1);
        when(tm.getProductionPath(spec.getColumnSpec(1))).thenReturn(m_p2);
        when(m_p1.getConverterFactory().getDestinationType()).thenReturn(StringCell.TYPE);
        when(m_p2.getConverterFactory().getDestinationType()).thenReturn(IntCell.TYPE);

        TypeMapping<String> typeMapping = m_testInstance.create(spec, m_config, tm);

        DataTableSpec expected =
            new DataTableSpec("default", new String[]{"A", "B"}, new DataType[]{StringCell.TYPE, IntCell.TYPE});
        assertEquals(expected, typeMapping.map(spec));
        assertArrayEquals(new ProductionPath[]{m_p1, m_p2}, typeMapping.getProductionPaths());
    }

}
