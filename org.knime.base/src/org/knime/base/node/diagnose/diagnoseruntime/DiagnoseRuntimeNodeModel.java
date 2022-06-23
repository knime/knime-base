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
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultRowIterator;
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
        var m = new SettingsModelString(CFGKEY_LOCATION, "");
        m.setEnabled(false);
        return m;
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
     * @throws InvalidSettingsException
     */
    @Override
    protected PortObject[] execute(final PortObject[] in, final ExecutionContext exec) throws CanceledExecutionException, InvalidSettingsException {
        dumpHeapIfSelected();

        var sysPropsResult = exec.createBufferedDataTable(retrieveSysProperties(), exec);

        var envVarsResult = exec.createBufferedDataTable(retrieveEnvVariables(), exec);

        var threadsResult = exec.createDataContainer(createThreadsSpec());
        threadsResult.close();

        return new PortObject[] {sysPropsResult, envVarsResult, threadsResult.getTable()};
    }

    private void dumpHeapIfSelected() throws InvalidSettingsException {
        if (m_fileSaveLocation.isEnabled()) {
            var location = m_fileSaveLocation.getStringValue();
            if (!m_fileSaveLocation.getStringValue().endsWith(HEAP_DUMP_FORMAT)) {
                location += HEAP_DUMP_FORMAT;
            }
            var locationFile = new File(location);
            if (locationFile.getParentFile() != null && locationFile.getParentFile().exists() && !locationFile.exists()) {
                RuntimeDiagnosticHelper.dumpHeap(locationFile.toString(), true);
            } else {
                throw new InvalidSettingsException("Heap dump file location does not exist.");
            }
        }
    }

    /**
     * Simple spec for System Properties, a StringCell contains the value.
     * @return DataTableSpec
     */
    private static DataTableSpec createSysPropsSpec() {
        return new DataTableSpec(new String[]{"Value"}, new DataType[]{StringCell.TYPE});
    }

    /**
     * Retrieves the System Properties Map and writes it to a DataTable.
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
     * @return DataTableSpec
     */
    private static DataTableSpec createEnvVarsSpec() {
        return new DataTableSpec(new String[]{"Value"}, new DataType[]{StringCell.TYPE});
    }

    /**
     * Retrieves the Environment Variables Map and writes it to a DataTable.
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

    private DataTableSpec createThreadsSpec() {
    var dtsc = new DataTableSpecCreator();
//      dtsc.addColumns(new DataColumnSpecCreator(COLUMN_NAME, XMLCell.TYPE).createSpec());
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
