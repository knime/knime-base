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
 *   28 Nov 2022 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.topk;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;

/**
 * Tests settings for {@link TopKSelectorNodeModel}.
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public class TopKSelectorNodeModelTest {

    private static ExecutionContext execContext;

    private TopKSelectorNodeModel m_tsnm;
    private NodeSettings m_settings;

    private static final NotInWorkflowDataRepository REPO = NotInWorkflowDataRepository.newInstance();

    private static final NodeProgressMonitor PROGRESS = new DefaultNodeProgressMonitor();

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final ExecutionContext exec() {
        return new ExecutionContext(PROGRESS,
            new Node((NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0])),
            SingleNodeContainer.MemoryPolicy.CacheSmallInMemory, REPO);
    }

    /**
     * Configure shared execution context.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        execContext = exec();
    }

    /**
     * Remove the shared execution context.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        execContext = null;
    }

    /**
     * Set up the model and settings.
     */
    @Before
    public void setUp() {
        m_tsnm = new TopKSelectorNodeModel();
        m_settings = new NodeSettings("TopKSelector");
    }

    /**
     * Clear the model and settings.
     */
    @After
    public void tearDown() {
        m_tsnm = null;
        m_settings = null;
    }

    /**
     * Test saving settings in model to settings object.
     * @throws InvalidSettingsException invalid settings for model
     */
    @Test
    public final void testSaveSettingsTo() throws InvalidSettingsException {
        assertFalse(m_settings.containsKey(
            TopKSelectorNodeModel.INCLUDELIST_KEY));
        assertFalse(m_settings.containsKey(
            TopKSelectorNodeModel.SORTORDER_KEY));
        assertFalse(m_settings.containsKey(
            TopKSelectorNodeModel.ALPHANUMCOMP_KEY));
        // save empty
        m_tsnm.saveSettingsTo(m_settings);

        // populate settings
        final boolean[] sortOrder = {true, false, true};
        final String[] inclCols = {"TestCol1", "TestCol2", "-ROWKEY -"};
        final boolean[] alphanum = {true, false, true};
        m_settings.addBooleanArray(TopKSelectorNodeModel.SORTORDER_KEY, sortOrder);
        m_settings.addStringArray(TopKSelectorNodeModel.INCLUDELIST_KEY, inclCols);
        m_settings.addBooleanArray(TopKSelectorNodeModel.ALPHANUMCOMP_KEY, alphanum);

        m_tsnm.validateSettings(m_settings);
        m_tsnm.loadValidatedSettingsFrom(m_settings);
        m_tsnm.configure(new DataTableSpec[] { new DataTableSpec(new String[] {"TestCol1", "TestCol2"},
            new DataType[] {StringCell.TYPE, DoubleCell.TYPE})
            });

        final var newsettings = new NodeSettings("TopKSelector");
        m_tsnm.saveSettingsTo(newsettings);

        final boolean[] sortOrderTest = newsettings.getBooleanArray(TopKSelectorNodeModel.SORTORDER_KEY);
        assertArrayEquals(sortOrder, sortOrderTest);
        final boolean[] alphanumTest = newsettings.getBooleanArray(TopKSelectorNodeModel.ALPHANUMCOMP_KEY);
        assertArrayEquals(alphanum, alphanumTest);
        final String[] inclColsTest = newsettings.getStringArray(TopKSelectorNodeModel.INCLUDELIST_KEY);
        assertArrayEquals(inclCols, inclColsTest);
    }

    /**
     * Tests execution of correctly configured node model.
     * @throws Exception invalid settings, node execution exception, or creating buffered data tables canceled
     */
    @Test
    public final void testExecute() throws Exception {
        m_tsnm.saveSettingsTo(m_settings);
        final boolean[] ascending = {false};
        m_settings.addBooleanArray(TopKSelectorNodeModel.SORTORDER_KEY, ascending);
        m_settings.addString("outputOrder", OutputOrder.SORT.name());
        m_settings.addInt("k", 2);
        final String[] inclCols = {"-ROWKEY -"};
        m_settings.addStringArray(TopKSelectorNodeModel.INCLUDELIST_KEY, inclCols);
        m_settings.addBooleanArray(TopKSelectorNodeModel.ALPHANUMCOMP_KEY, true);

        m_tsnm.validateSettings(m_settings);
        m_tsnm.loadValidatedSettingsFrom(m_settings);
        final var dts = new DataTableSpec(new String[] {"Strings", "Doubles"},
            new DataType[] {StringCell.TYPE, DoubleCell.TYPE});
        m_tsnm.configure(new DataTableSpec[] { dts });

        final var cont = execContext.createDataContainer(dts);
        cont.addRowToTable(new DefaultRow(RowKey.createRowKey(1L), new StringCell("MyRow1"), new DoubleCell(1.0d)));
        cont.addRowToTable(new DefaultRow(RowKey.createRowKey(2L), new StringCell("MyRow2"), new DoubleCell(2.0d)));
        cont.addRowToTable(new DefaultRow(RowKey.createRowKey(10L), new StringCell("MyRow10"), new DoubleCell(10.0d)));
        cont.close();
        final var input = cont.getTable();
        final var top2 = m_tsnm.execute(execContext.createBufferedDataTables(
            new DataTable[] { input }, execContext), execContext)[0];
        try (final var it = top2.iterator()) {
            assertEquals(RowKey.createRowKey(10L), it.next().getKey());
            assertEquals(RowKey.createRowKey(2L), it.next().getKey());
            assertFalse(it.hasNext());
        }
    }
}
