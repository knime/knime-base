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
 *   Mar 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for MultiTableReader that also compute the {@link DataTableSpec}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class MultiTableReaderTest {

    private static final String ROOT_PATH = "path";

    @Mock
    private MultiTableReadFactory<DummyReaderSpecificConfig, String> m_multiTableReadFactory = null;

    @Mock
    private StagedMultiTableRead<String> m_stagedMultiTableRead = null;

    @Mock
    private MultiTableRead m_multiTableRead = null;

    @Mock
    private MultiTableReadConfig<DummyReaderSpecificConfig> m_multiReadConfig = null;

    @Mock
    private Path m_path1 = null;

    @Mock
    private Path m_path2 = null;

    @Mock
    private ExecutionContext m_exec = null;

    @Mock
    private RowOutput m_rowOutput = null;

    private List<Path> m_paths;

    private DataTableSpec m_knimeSpec = null;

    private MultiTableReader<DummyReaderSpecificConfig> m_testInstance = null;

    /**
     * Sets up the test instance before each unit test.
     */
    @Before
    public void init() {
        m_testInstance = new MultiTableReader<>(m_multiTableReadFactory);
        m_knimeSpec = TableSpecConfigTestingUtils.createDataTableSpec("Column0", "Column1");
        m_paths = asList(m_path1, m_path2);
    }

    private void stubMultiReads(final boolean isConfigured) throws IOException {
        when(m_multiReadConfig.isConfiguredWith(ROOT_PATH, m_paths)).thenReturn(isConfigured);
        if (isConfigured) {
            when(m_multiTableReadFactory.createFromConfig(ROOT_PATH, m_paths, m_multiReadConfig))
                .thenReturn(m_stagedMultiTableRead);
        } else {
            when(m_multiTableReadFactory.create(eq(ROOT_PATH), eq(m_paths), eq(m_multiReadConfig), any()))
                .thenReturn(m_stagedMultiTableRead);
        }
        when(m_stagedMultiTableRead.withoutTransformation()).thenReturn(m_multiTableRead);
        when(m_multiTableRead.getOutputSpec()).thenReturn(m_knimeSpec);
    }

    /**
     * Tests the {@code createTableSpec} method for the case where the config is not configured for the provided root
     * path and path list.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testCreateTableSpec() throws IOException {
        stubMultiReads(false);
        when(m_multiReadConfig.isConfiguredWith(ROOT_PATH, m_paths)).thenReturn(false);
        assertEquals(m_knimeSpec, m_testInstance.createTableSpec(ROOT_PATH, m_paths, m_multiReadConfig));
        verify(m_multiTableReadFactory, never()).createFromConfig(any(), anyList(), any());
        verify(m_multiTableReadFactory).create(eq(ROOT_PATH), eq(m_paths), eq(m_multiReadConfig), any());
    }

    /**
     * Tests the {@code createTableSpec} method in the case the config is configured for the provided root path and path
     * list.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testCreateTableSpecFromConfig() throws IOException {
        stubMultiReads(true);

        when(m_multiReadConfig.isConfiguredWith(any(), anyList())).thenReturn(true);
        assertEquals(m_knimeSpec,
            m_testInstance.createTableSpec(ROOT_PATH, asList(m_path1, m_path2), m_multiReadConfig));
        verify(m_multiTableReadFactory).createFromConfig(ROOT_PATH, asList(m_path1, m_path2), m_multiReadConfig);
        verify(m_multiTableReadFactory, never()).create(any(), ArgumentMatchers.anyList(), any(), any());
    }

    /**
     * Tests the {@code fillRowOutput} method if createSpec isn't called first i.e. it first has to create the spec.
     *
     * @throws Exception never thrown
     */
    @Test
    public void testFillRowOutputWithoutCallingCreateSpecFirst() throws Exception {
        stubMultiReads(false);
        m_testInstance.fillRowOutput(ROOT_PATH, asList(m_path1, m_path2), m_multiReadConfig, m_rowOutput, m_exec);
        verify(m_multiReadConfig).isConfiguredWith(ROOT_PATH, asList(m_path1, m_path2));
        verify(m_multiTableReadFactory, never()).createFromConfig(any(), anyList(), any());
        verify(m_multiTableReadFactory, times(1)).create(eq(ROOT_PATH), anyList(), eq(m_multiReadConfig), any());
        verify(m_multiTableRead).fillRowOutput(eq(m_rowOutput), eq(m_exec), any());
    }

    /**
     * Tests if {@code fillRowOutput} reuses an existing spec if the paths match.
     *
     * @throws Exception never thrown
     */
    @Test
    public void testFillRowOutputWithCallingCreateSpecFirstAndValidPaths() throws Exception {
        stubMultiReads(false);
        m_testInstance.createTableSpec(ROOT_PATH, asList(m_path1, m_path2), m_multiReadConfig);
        verify(m_multiTableReadFactory).create(eq(ROOT_PATH), anyList(), eq(m_multiReadConfig), any());
        when(m_stagedMultiTableRead.isValidFor(any())).thenReturn(true);
        m_testInstance.fillRowOutput(ROOT_PATH, asList(m_path1, m_path2), m_multiReadConfig, m_rowOutput, m_exec);
        verify(m_multiTableReadFactory, never()).createFromConfig(any(), anyList(), any());
        verify(m_multiTableReadFactory).create(eq(ROOT_PATH), anyList(), eq(m_multiReadConfig), any());
    }

    /**
     * Tests if {@code fillRowOutput} creates a new spec if the existing spec doesn't match the provided paths.
     *
     * @throws Exception never thrown
     */
    @Test
    public void testFillRowOutputWithCallingCreateSpecFirstInvalidPathsUnconfigured() throws Exception {
        stubMultiReads(false);
        m_testInstance.createTableSpec(ROOT_PATH, asList(m_path1, m_path2), m_multiReadConfig);
        when(m_stagedMultiTableRead.isValidFor(anyList())).thenReturn(false);
        m_testInstance.fillRowOutput(ROOT_PATH, asList(m_path1, m_path2), m_multiReadConfig, m_rowOutput, m_exec);
        verify(m_multiTableReadFactory, never()).createFromConfig(any(), ArgumentMatchers.anyList(), any());
        verify(m_multiTableReadFactory, times(2)).create(eq(ROOT_PATH), ArgumentMatchers.anyList(),
            eq(m_multiReadConfig), any());
    }

    /**
     * Tests if {@code fillRowOutput} creates a new spec if the existing spec doesn't match the provided paths.
     *
     * @throws Exception never thrown
     */
    @Test
    public void testFillRowOutputWithCallingCreateSpecFirstInvalidPathsConfigured() throws Exception {
        stubMultiReads(true);
        m_testInstance.createTableSpec(ROOT_PATH, asList(m_path1, m_path2), m_multiReadConfig);
        verify(m_multiTableReadFactory).createFromConfig(ROOT_PATH, m_paths, m_multiReadConfig);
        when(m_stagedMultiTableRead.isValidFor(m_paths)).thenReturn(false);
        when(m_multiReadConfig.isConfiguredWith(ROOT_PATH, m_paths)).thenReturn(true);
        when(m_multiTableReadFactory.createFromConfig(ROOT_PATH, m_paths, m_multiReadConfig))
            .thenReturn(m_stagedMultiTableRead);
        m_testInstance.fillRowOutput(ROOT_PATH, asList(m_path1, m_path2), m_multiReadConfig, m_rowOutput, m_exec);
        verify(m_multiTableReadFactory, times(2)).createFromConfig(ROOT_PATH, m_paths, m_multiReadConfig);
        verify(m_multiTableReadFactory, never()).create(any(), anyList(), any(), any());
    }

    /**
     * Tests the {@code reset} method.
     *
     * @throws Exception never thrown
     */
    @Test
    public void testReset() throws Exception {
        stubMultiReads(false);
        m_testInstance.createTableSpec(ROOT_PATH, asList(m_path1, m_path2), m_multiReadConfig);
        verify(m_multiTableReadFactory).create(eq(ROOT_PATH), eq(asList(m_path1, m_path2)), eq(m_multiReadConfig),
            any());
        m_testInstance.reset();
        m_testInstance.fillRowOutput(ROOT_PATH, asList(m_path1, m_path2), m_multiReadConfig, m_rowOutput, m_exec);
        verify(m_multiTableReadFactory, never()).createFromConfig(any(), anyList(), any());
        verify(m_multiTableReadFactory, times(2)).create(eq(ROOT_PATH), eq(asList(m_path1, m_path2)),
            eq(m_multiReadConfig), any());
    }

    /**
     * Tests readTable in case the config doesn't contain a table spec.
     *
     * @throws Exception never thrown
     */
    @Test
    public void testReadTableUnconfigured() throws Exception {
        stubMultiReads(false);
        ExecutionContext subExec = mock(ExecutionContext.class);
        when(m_exec.createSubExecutionContext(anyDouble())).thenReturn(subExec);
        BufferedDataContainer container = mock(BufferedDataContainer.class);
        when(m_exec.createDataContainer(m_knimeSpec)).thenReturn(container);
        // we can't check for equality because the mock BufferedDataContainer returns null
        // for getTable(). This call can't be mocked because BufferedDataTable is final
        m_testInstance.readTable(ROOT_PATH, m_paths, m_multiReadConfig, m_exec);
        verify(m_multiReadConfig, times(2)).isConfiguredWith(ROOT_PATH, m_paths);
        verify(m_exec, times(2)).createSubExecutionContext(0.5);
        verify(m_multiTableReadFactory).create(ROOT_PATH, m_paths, m_multiReadConfig, subExec);

    }

    /**
     * Tests readTable in case the config contains a table spec.
     *
     * @throws Exception never thrown
     */
    @Test
    public void testReadTableConfigured() throws Exception {
        stubMultiReads(true);
        ExecutionContext subExec = mock(ExecutionContext.class);
        when(m_exec.createSubExecutionContext(anyDouble())).thenReturn(subExec);
        BufferedDataContainer container = mock(BufferedDataContainer.class);
        when(m_exec.createDataContainer(m_knimeSpec)).thenReturn(container);
        // we can't check for equality because the mock BufferedDataContainer returns null
        // for getTable(). This call can't be mocked because BufferedDataTable is final
        m_testInstance.readTable(ROOT_PATH, m_paths, m_multiReadConfig, m_exec);
        verify(m_multiReadConfig, times(2)).isConfiguredWith(ROOT_PATH, m_paths);
        verify(m_exec).createSubExecutionContext(0.0);
        verify(m_exec).createSubExecutionContext(1.0);
        verify(m_multiTableReadFactory).createFromConfig(ROOT_PATH, m_paths, m_multiReadConfig);

    }

}
