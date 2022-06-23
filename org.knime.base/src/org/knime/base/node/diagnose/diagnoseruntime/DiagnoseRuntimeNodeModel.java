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
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
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
import org.knime.core.util.workflowsummary.WorkflowSummaryCreator;

/**
 * NodeModel for the Diagnose Runtime node.
 *
 * @author Jasper Krauter, KNIME AG, Schloss
 * @author Leon Wenzler, KNIME AG, Schloss
 */
public class DiagnoseRuntimeNodeModel extends NodeModel {

    static final String HEAP_DUMP_FORMAT = ".hprof";

    // setting keys
    static final String CFGKEY_ISSAVED_HEAPDUMP = "HeapDumpIsSaved";

    static final String CFGKEY_LOCATION = "HeapDumpFileLocation";

    private SettingsModelBoolean m_heapDumpIsSaved = createHeapDumpIsSavedSettingsModel();

    private SettingsModelString m_fileSaveLocation = createFileLocationModel();

    /**
     * Create a new instance of a source node with a four output ports
     */
    protected DiagnoseRuntimeNodeModel() {
        super(0, 4);
    }

    /**
     * Creates a boolean settings model if the heap dump shall be saved.
     *
     * @return
     */
    static SettingsModelBoolean createHeapDumpIsSavedSettingsModel() {
        return new SettingsModelBoolean(CFGKEY_ISSAVED_HEAPDUMP, false);
    }

    /**
     * Creates a String settings model for storing the file location.
     *
     * @return
     */
    static SettingsModelString createFileLocationModel() {
        var m = new SettingsModelString(CFGKEY_LOCATION, "");
        // disabled per default
        m.setEnabled(false);
        return m;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] in) {
        return new PortObjectSpec[]{createSysPropsSpec(), createEnvVarsSpec(), createThreadsSpec(), createVMOptSpec()};
    }

    /**
     * {@inheritDoc}
     *
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    @Override
    protected PortObject[] execute(final PortObject[] in, final ExecutionContext exec)
        throws CanceledExecutionException, InvalidSettingsException {
        dumpHeapIfSelected();

        var sysPropsResult = exec.createBufferedDataTable(retrieveSysProperties(), exec);
        var envVarsResult = exec.createBufferedDataTable(retrieveEnvVariables(), exec);
        var em = new ExecutionMonitor();
        var threadsResult = exec.createBufferedDataTable(dumpThreads(), em);
        var vmOptionsResult = exec.createBufferedDataTable(retrieveVMOptions(), exec);

        return new PortObject[]{sysPropsResult, envVarsResult, threadsResult, vmOptionsResult};
    }

    /**
     * Dumps the heap to an HPROF file, location can be specified via the node dialog. Uses the
     * {@link RuntimeDiagnosticHelper} for accessing the heapDump method.
     *
     * @throws InvalidSettingsException
     */
    private void dumpHeapIfSelected() throws InvalidSettingsException {
        if (m_fileSaveLocation.isEnabled()) {
            var location = m_fileSaveLocation.getStringValue();
            if (!m_fileSaveLocation.getStringValue().endsWith(HEAP_DUMP_FORMAT)) {
                location += HEAP_DUMP_FORMAT;
            }
            var locationFile = new File(location);
            if (locationFile.getParentFile() != null && locationFile.getParentFile().canWrite()) {
                RuntimeDiagnosticHelper.dumpHeap(locationFile.toString(), true);
            } else {
                throw new InvalidSettingsException("Heap dump file could not be written to specified location.");
            }
        }
    }

    /**
     * Simple spec for System Properties, a StringCell contains the value.
     *
     * @return DataTableSpec
     */
    private static DataTableSpec createSysPropsSpec() {
        return new DataTableSpec(new String[]{"Value"}, new DataType[]{StringCell.TYPE});
    }

    /**
     * Retrieves the System Properties Map and writes it to a DataTable.
     *
     * @return DataTable
     */
    private static DataTable retrieveSysProperties() {
        return new DataTable() {

            @Override
            public RowIterator iterator() {
                List<DataRow> rows = new ArrayList<>();
                for (Entry<Object, Object> e : System.getProperties().entrySet()) {
                    var key = String.valueOf(e.getKey());
                    var value = String.valueOf(e.getValue());
                    rows.add(new DefaultRow(key, WorkflowSummaryCreator.getValueHidePasswords(key, value)));
                }
                return new DefaultRowIterator(rows);
            }

            @Override
            public DataTableSpec getDataTableSpec() {
                return createSysPropsSpec();
            }
        };
    }

    /**
     * Simple spec for Environment Variables, a StringCell contains the value.
     *
     * @return DataTableSpec
     */
    private static DataTableSpec createEnvVarsSpec() {
        return new DataTableSpec(new String[]{"Value"}, new DataType[]{StringCell.TYPE});
    }

    /**
     * Retrieves the Environment Variables Map and writes it to a DataTable.
     *
     * @return DataTable
     */
    private static DataTable retrieveEnvVariables() {
        return new DataTable() {

            @Override
            public RowIterator iterator() {
                List<DataRow> rows = new ArrayList<>();
                for (Entry<String, String> e : System.getenv().entrySet()) {
                    var key = e.getKey();
                    var value = e.getValue();
                    rows.add(new DefaultRow(key, WorkflowSummaryCreator.getValueHidePasswords(key, value)));
                }
                return new DefaultRowIterator(rows);
            }

            @Override
            public DataTableSpec getDataTableSpec() {
                return createEnvVarsSpec();
            }
        };
    }

    /**
     * Creates the spec for the thread information table.
     *
     * @return
     */
    private static DataTableSpec createThreadsSpec() {
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
     * Retrieves the information about the treads, they include:
     * A name, running state, the CPU and user time, call stacks, as well as interaction with other threads:
     * - whether the thread is block by another one
     * - wheteher a deadlock was detected
     * - whether the thread currently holds any locks.
     *
     * @param threadsResult
     */
    private static DataTable dumpThreads() {
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

                    var isThreadDeadlocked =
                        deadlockedThreads != null && Arrays.stream(deadlockedThreads).anyMatch(i -> i == id);
                    var deadlockCell = isThreadDeadlocked ? BooleanCell.TRUE : BooleanCell.FALSE;

                    var lockedMonitors = Arrays.stream(t.getLockedSynchronizers()).map(LockInfo::toString)
                        .collect(Collectors.joining("\n"));
                    var lockedCell =
                        lockedMonitors.isEmpty() ? new MissingCell("No locks") : new StringCell(lockedMonitors);

                    var isBlockedBy = t.getLockName() != null ? new StringCell(t.getLockName()) : new MissingCell("");

                    var cpuTime = new LongCell(threads.getThreadCpuTime(id));
                    var userTime = new LongCell(threads.getThreadUserTime(id));

                    return new DefaultRow(key, name, state, cpuTime, userTime, deadlockCell, lockedCell, isBlockedBy,
                        formatStacktrace(t.getStackTrace()));
                }).collect(Collectors.toList()));
            }

            @Override
            public DataTableSpec getDataTableSpec() {
                return createThreadsSpec();
            }
        };
    }

    /**
     * Formats the call stack traces cell. Each StackTraceElement will be appended
     * with the common "at"-notation to be displayed nicely in the StringCell.
     *
     * @param st StackTraceElement array
     * @return formatted StringCell containing all stack traces
     */
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

    /**
     * Simple spec for the VM options, a StringCell contains the value.
     *
     * @return DataTableSpec
     */
    private static DataTableSpec createVMOptSpec() {
        return new DataTableSpec(new String[]{"Option"}, new DataType[]{StringCell.TYPE});
    }

    /**
     * Retrieves all arguments/options set to the JVM.
     *
     * @return
     */
    private static DataTable retrieveVMOptions() {
        return new DataTable() {

            @Override
            public RowIterator iterator() {
                var rowId = 0;
                List<DataRow> rows = new ArrayList<>();
                for (String option : RuntimeDiagnosticHelper.getVMOptions()) {
                    rows.add(new DefaultRow("Row " + rowId, option));
                    rowId++;
                }
                return new DefaultRowIterator(rows);
            }

            @Override
            public DataTableSpec getDataTableSpec() {
                return createVMOptSpec();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
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
        // nothing to do
    }
}
