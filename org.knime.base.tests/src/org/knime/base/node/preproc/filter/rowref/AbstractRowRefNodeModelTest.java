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
 *   12 Mar 2021 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.filter.rowref;

import static org.junit.Assert.assertEquals;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("javadoc")
public class AbstractRowRefNodeModelTest {

    private static final class TestRowRefNodeSettings extends AbstractRowFilterRefNodeSettings {

    }

    private static final class TestRowRefNodeModel extends AbstractRowRefNodeModel<TestRowRefNodeSettings> {

        static final WebUINodeConfiguration TEST_CONFIGURATION =
            WebUINodeConfiguration.builder().name("TestRowRefNode").icon(null).shortDescription(null)
                .fullDescription(null).modelSettingsClass(TestRowRefNodeSettings.class).build();

        public TestRowRefNodeModel() {
            super(TEST_CONFIGURATION, TestRowRefNodeSettings.class);
        }

        @Override
        DataTableSpec[] getOutputSpecs(final DataTableSpec inputSpec) {
            return new DataTableSpec[]{inputSpec};
        }

        /**
         * {@inheritDoc}
         */
        @Override
        BufferedDataTable[] noopExecute(final BufferedDataTable inputTable) {
            return new BufferedDataTable[]{inputTable};
        }

        /**
         * {@inheritDoc}
         */
        @Override
        OutputCreator createOutputCreator(final DataTableSpec spec, final ExecutionContext exec,
            final TestRowRefNodeSettings settings) {
            return new OutputCreator(spec, exec);
        }

        static final class OutputCreator extends AbstractRowRefNodeModel.OutputCreator {

            private final BufferedDataContainer m_container;

            OutputCreator(final DataTableSpec spec, final ExecutionContext exec) {
                m_container = exec.createDataContainer(spec);
            }

            @Override
            void addRow(final DataRow row, final boolean isIncluded) {
                if (isIncluded) {
                    m_container.addRowToTable(row);
                }
            }

            @Override
            BufferedDataTable[] createTables(final boolean updateDomains,
                final Supplier<ExecutionContext> domainUpdateExecSupplier) {
                m_container.close();
                return new BufferedDataTable[]{m_container.getTable()};
            }
        }
    }

    private static void compare(final BufferedDataTable expected, final BufferedDataTable actual) {
        final long expectedSize = expected.size();
        assertEquals(expectedSize, actual.size());
        try (final CloseableRowIterator expectedRowIt = expected.iterator();
                final CloseableRowIterator actualRowIt = actual.iterator()) {
            for (int i = 0; i < expectedSize; i++) {
                final DataRow expectedRow = expectedRowIt.next();
                final DataRow actualRow = actualRowIt.next();
                assertEquals(expectedRow.getNumCells(), actualRow.getNumCells());
                final Iterator<DataCell> actualCellIt = actualRow.iterator();
                DataCell actualCell;
                for (final DataCell expectedCell : expectedRow) {
                    actualCell = actualCellIt.next();
                    assertEquals(expectedCell, actualCell);
                }
            }
        }
    }

    private static final MemoryAlertSystem MAS = MemoryAlertSystem.getInstance();

    private ExecutionContext m_exec;

    @Before
    public void setUp() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final NodeFactory<NodeModel> dummyFactory =
            (NodeFactory)new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0]);
        m_exec = new ExecutionContext(new DefaultNodeProgressMonitor(), new Node(dummyFactory),
            SingleNodeContainer.MemoryPolicy.CacheOnDisc, NotInWorkflowDataRepository.newInstance());
    }

    private BufferedDataTable generateData(final int from, final int to) {
        final DataTableSpec spec = new DataTableSpec(new DataColumnSpecCreator("int", IntCell.TYPE).createSpec());
        final BufferedDataContainer dc = m_exec.createDataContainer(spec);
        for (int i = from; i <= to; i++) {
            dc.addRowToTable(new DefaultRow(RowKey.createRowKey((long)i), i));
        }
        dc.close();
        return dc.getTable();
    }

    @Test
    public void testSmallInputSmallReference() throws Exception {
        final TestRowRefNodeModel model = new TestRowRefNodeModel();
        final BufferedDataTable[] inData = new BufferedDataTable[]{generateData(1, 64), generateData(33, 96)};
        final BufferedDataTable[] outData = model.execute(inData, m_exec, new TestRowRefNodeSettings());
        compare(generateData(33, 64), outData[0]);
    }

    @Test(timeout = 60000)
    public void testEmptyInputLargeReferenceLowMemory() throws Exception {
        try {
            while (!MAS.isMemoryLow()) {
                forceGC();
                // induce low memory condition by (a) setting low memory threshold to 10 MB below current memory usage
                MAS.setFractionUsageThreshold(Math.min(MemoryAlertSystem.DEFAULT_USAGE_THRESHOLD,
                    (MemoryAlertSystem.getUsedMemory() - (10 << 20)) / (double)MemoryAlertSystem.getMaximumMemory()));
                // ... and (b) forcing a garbage collection to trigger a usage threshold event
                forceGC();
            }

            final TestRowRefNodeModel model = new TestRowRefNodeModel();
            final BufferedDataTable emptyTable = generateData(0, -1);
            final BufferedDataTable[] inData = new BufferedDataTable[]{emptyTable, generateData(0, 128)};
            final BufferedDataTable[] outData = model.execute(inData, m_exec, new TestRowRefNodeSettings());

            compare(emptyTable, outData[0]);

        } finally {
            MAS.setFractionUsageThreshold(MemoryAlertSystem.DEFAULT_USAGE_THRESHOLD);
        }
    }

    // code copied from org.knime.core.data.util.memory.MemoryAlertSystemTest
    private static void forceGC() throws InterruptedException {
        Object obj = new Object();
        final WeakReference<Object> ref = new WeakReference<>(obj);
        obj = null; // NOSONAR
        int max = 10;
        while ((ref.get() != null) && (max-- > 0) && !Thread.currentThread().isInterrupted()) { // NOSONAR
            System.gc(); // NOSONAR
            Thread.sleep(50);
        }
    }

}
