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
 *   21 Jun 2022 (jasper): created
 */
package org.knime.base.node.diagnose.diagnoseruntime;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultRowIterator;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;

/**
 * NodeModel for the Diagnose Runtime node.
 *
 * @author Jasper Krauter, KNIME AG, Schloss
 * @author Leon Wenzler, KNIME AG, Schloss
 */
public class DiagnoseRuntimeNodeModel extends NodeModel {

    // setting keys
    static final String CFGKEY_ISSAVED_HEAPDUMP = "HeapDumpIsSaved";
    static final String CFGKEY_LOCATION = "HeapDumpFileLocation";

    private SettingsModelBoolean m_heapDumpIsSaved = createHeapDumpIsSavedSettingsModel();
    private SettingsModelString m_fileSaveLocation = createFileLocationModel();

    /**
     * Create a new instance
     */
    protected DiagnoseRuntimeNodeModel() {
        // source node with a three output tables
        super(0, 3);
    }

    /**
     * Creates a boolean settings model if the heap dump shall be saved.
     * @return
     */
    static SettingsModelBoolean createHeapDumpIsSavedSettingsModel() {
        return new SettingsModelBoolean(CFGKEY_ISSAVED_HEAPDUMP, false);
    }

    /**
     * Creates a String settings model for storing the file location.
     * @return
     */
    static SettingsModelString createFileLocationModel() {
        return new SettingsModelString(CFGKEY_LOCATION, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] in) {
        return new PortObjectSpec[]{createSysPropsSpec(), createEnvVarsSpec(), createThreadsSpec()};
    }

    /**
     * {@inheritDoc}
     * @throws CanceledExecutionException
     */
    @Override
    protected PortObject[] execute(final PortObject[] in, final ExecutionContext exec) throws CanceledExecutionException {
        var sysPropsResult = exec.createDataContainer(createSysPropsSpec());
        sysPropsResult.close();

        var envVarsResult = exec.createDataContainer(createEnvVarsSpec());
        envVarsResult.close();

        var em = new ExecutionMonitor();
        var threadsResult = exec.createBufferedDataTable(dumpThreads(), em);

        return new PortObject[] {sysPropsResult.getTable(), envVarsResult.getTable(), threadsResult};
    }

    /**
     * @param threadsResult
     */
    private DataTable dumpThreads() {
        return new DataTable() {
            @Override
            public RowIterator iterator() {
                var threads = ManagementFactory.getThreadMXBean();
                var deadlockedThreads = threads.findDeadlockedThreads();
                return new DefaultRowIterator(Arrays.stream(threads.dumpAllThreads(true, true)).map(t -> {
                    var id = t.getThreadId();
                    var key = new RowKey("Thread " + id);
                    var name = new StringCell(t.getThreadName());
                    var state = new StringCell(t.getThreadState().toString());

                    var isThreadDeadlocked = deadlockedThreads != null && Arrays.stream(deadlockedThreads).anyMatch(i -> i == id);
                    var deadlockCell = isThreadDeadlocked ? BooleanCell.TRUE : BooleanCell.FALSE;

                    var lockedMonitors = Arrays.stream(t.getLockedSynchronizers()).map(s -> s.toString()).collect(Collectors.joining("\n"));
                    var lockedCell = lockedMonitors.isEmpty() ? new MissingCell("No locks") : new StringCell(lockedMonitors);

                    var isBlockedBy = t.getLockName() != null ? new StringCell(t.getLockName()) : new MissingCell("");

                    var cpuTime = new LongCell(threads.getThreadCpuTime(id));
                    var userTime = new LongCell(threads.getThreadUserTime(id));

                    return new DefaultRow(key, name, state, cpuTime, userTime, deadlockCell, lockedCell, isBlockedBy, formatStacktrace(t.getStackTrace()));
                }).collect(Collectors.toList()));
            }

            @Override
            public DataTableSpec getDataTableSpec() {
                return createThreadsSpec();
            }
        };
    }

    private static DataCell formatStacktrace(final StackTraceElement[] st) {
        if (st.length > 0) {
            var ststring = new StringBuilder("Thread dump");
            for (var i = 0; i < st.length; ++i) {
                ststring.append("\n\tat " + st[i]);
            }
            return new StringCell(ststring.toString());
        }
        return new MissingCell("No stack trace available");

    }

    private DataTableSpec createSysPropsSpec() {
        var dtsc = new DataTableSpecCreator();
//        var dcss = Arrays.stream(m_includedProperties.getStringArrayValue())
//            .map(k -> new DataColumnSpecCreator(k, allProps.get(k)).createSpec()).collect(Collectors.toList());
//        dtsc.addColumns(dcss.toArray(new DataColumnSpec[0]));
        return dtsc.createSpec();
    }

    private DataTableSpec createEnvVarsSpec() {
        var dtsc = new DataTableSpecCreator();
//        dtsc.addColumns(new DataColumnSpecCreator(COLUMN_NAME, XMLCell.TYPE).createSpec());
        return dtsc.createSpec();
    }

    private DataTableSpec createThreadsSpec() {
        var dtsc = new DataTableSpecCreator();
        dtsc.addColumns(new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("State", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("CPU Time", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("User Time", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Deadlock Detected", BooleanCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Holds Locks", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Is Blocked By", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Call Stack", StringCell.TYPE).createSpec());
        return dtsc.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_heapDumpIsSaved.saveSettingsTo(settings);
        m_fileSaveLocation.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_heapDumpIsSaved.loadSettingsFrom(settings);
        m_fileSaveLocation.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub
    }
}
