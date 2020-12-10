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
import static org.knime.filehandling.core.node.table.reader.TRFTestingUtils.createTypedTableSpec;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.node.ExecutionMonitor;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.rowkey.GenericRowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy.TypeResolver;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for {@code DefaultMultiTableReadFactory}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultMultiTableReadFactoryTest {

    private static final TypedReaderTableSpec<String> SPEC1 = createTypedTableSpec(asList("A", "B"), asList("X", "Y"));

    private static final TypedReaderTableSpec<String> SPEC2 = createTypedTableSpec(asList("B", "C"), asList("Y", "Z"));

    private static final TypedReaderTableSpec<String> UNION =
        createTypedTableSpec(asList("A", "B", "C"), asList("X", "Y", "Z"));

    private static final TypedReaderTableSpec<String> INTERSECTION = createTypedTableSpec(asList("B"), asList("Y"));

    private static final RawSpec<String> RAW_SPEC = new RawSpec<>(UNION, INTERSECTION);

    @Mock
    private TypeHierarchy<String, String> m_typeHierarchy;

    @Mock
    private TypeResolver<String, String> m_typeResolver;

    @Mock
    private GenericRowKeyGeneratorContextFactory<String, String> m_rowKeyGenFactory;

    @Mock
    private RowKeyGenerator<String> m_rowKeyGen;

    @Mock
    private MultiTableReadConfig<DummyReaderSpecificConfig, String> m_config;

    @Mock
    private DummyReaderSpecificConfig m_readerSpecificConfig;

    @Mock
    private GenericTableReader<String, DummyReaderSpecificConfig, String, String> m_tableReader;

    @Mock
    private TableReadConfig<DummyReaderSpecificConfig> m_tableReadConfig;

    @Mock
    private ProductionPathProvider<String> m_productionPathProvider;

    @Mock
    private Supplier<ReadAdapter<String, String>> m_readAdapterSupplier;

    private static final String PATH1 = "path1";

    private static final String PATH2 = "path2";

    @Mock
    private SourceGroup<String> m_sourceGroup;

    private DefaultMultiTableReadFactory<String, DummyReaderSpecificConfig, String, String> m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        when(m_config.getTableReadConfig()).thenReturn(m_tableReadConfig);
        m_testInstance = new DefaultMultiTableReadFactory<>(m_typeHierarchy, m_rowKeyGenFactory, m_tableReader,
            m_productionPathProvider, m_readAdapterSupplier);
    }

    /**
     * Tests the implementation of
     * {@link MultiTableReadFactory#create(SourceGroup, MultiTableReadConfig, ExecutionMonitor)}.
     *
     * @throws IOException
     */
    @Test
    public void testCreate() throws IOException {
        when(m_tableReader.readSpec(eq(PATH1), any(), any())).thenReturn(SPEC1);
        when(m_tableReader.readSpec(eq(PATH2), any(), any())).thenReturn(SPEC2);

        when(m_typeHierarchy.createResolver()).thenReturn(m_typeResolver);
        when(m_typeResolver.getMostSpecificType()).thenReturn("X", "Y", "Z");
        when(m_typeResolver.hasType()).thenReturn(true);

        ExecutionMonitor exec = mock(ExecutionMonitor.class);

        when(m_sourceGroup.iterator()).thenReturn(asList(PATH1, PATH2).iterator());
        when(m_sourceGroup.size()).thenReturn(2);

        StagedMultiTableRead<String, String> smtr =
            m_testInstance.create(m_sourceGroup, m_config, exec);

        verify(exec, times(2)).createSubProgress(0.5);

        assertEquals(RAW_SPEC, smtr.getRawSpec());
    }

    /**
     * Tests the implementation of
     * {@link MultiTableReadFactory#createFromConfig(SourceGroup, MultiTableReadConfig)}.
     */
    @SuppressWarnings({"rawtypes", "unchecked"}) // otherwise Mockito won't work
    @Test
    public void testCreateFromConfig() {
        TableTransformation transformationModel = mock(TableTransformation.class);// NOSONAR
        when(transformationModel.getRawSpec()).thenReturn(RAW_SPEC);

        final TableSpecConfig tsc = mock(TableSpecConfig.class);
        when(tsc.getTransformationModel()).thenReturn(transformationModel);
        final RawSpec rawRawSpec = RAW_SPEC;// NOSONAR, necessary to make Mockito work its magic
        when(tsc.getRawSpec()).thenReturn(rawRawSpec);

        when(m_config.getTableSpecConfig()).thenReturn(tsc);

        StagedMultiTableRead<String, String> smtr =
            m_testInstance.createFromConfig(m_sourceGroup, m_config);

        assertEquals(RAW_SPEC, smtr.getRawSpec());
    }

}
