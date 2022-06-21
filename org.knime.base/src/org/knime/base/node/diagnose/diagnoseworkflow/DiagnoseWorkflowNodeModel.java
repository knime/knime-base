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
package org.knime.base.node.diagnose.diagnoseworkflow;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;

/**
 *
 * @author jasper
 */
public class DiagnoseWorkflowNodeModel extends NodeModel {

    static final Map<String, DataType> allProps = new LinkedHashMap<>();
    static {
        allProps.put("Global ID", StringCell.TYPE);
        allProps.put("Name", StringCell.TYPE);
        allProps.put("Type", StringCell.TYPE);
        allProps.put("Source URI", StringCell.TYPE);
        allProps.put("Deprecated", BooleanCell.TYPE);
        allProps.put("Disk Space", LongCell.TYPE);
        allProps.put("Execution Time", LongCell.TYPE);
        allProps.put("Executions", IntCell.TYPE);
        allProps.put("Class Name", StringCell.TYPE);
    }

    /**
     * Returns every possible workflow property, as String array.
     * @return String[] allProperties
     */
    static String[] getPropertiesAsStringArray() {
        return allProps.keySet().toArray(new String[0]);
    }

    static final String CFGKEY_MAXDEPTH = "MaxDepth";
    static final String CFGKEY_INCLUDES = "IncludedProperties";
    static final String CFGKEY_EXCLUDES = "ExcludedProperties";

    private SettingsModelIntegerBounded m_maxDepth = createMaxDepthSettingsModel();
    private SettingsModelStringArray m_includedProperties = new SettingsModelStringArray(CFGKEY_INCLUDES, getPropertiesAsStringArray());
    private SettingsModelStringArray m_excludedProperties = new SettingsModelStringArray(CFGKEY_EXCLUDES, new String[0]);

    /**
     * Create a new instance
     */
    protected DiagnoseWorkflowNodeModel() {
        // source node with a single output table
        super(0, 1);
    }

    static SettingsModelIntegerBounded createMaxDepthSettingsModel() {
        return new SettingsModelIntegerBounded(CFGKEY_MAXDEPTH, 2, 0, Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] in) {
        return new PortObjectSpec[]{getSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] in, final ExecutionContext ec) {
        var wfm = getMyWFM();
        var result = ec.createDataContainer(getSpec());
        reportNodes(wfm, result, m_maxDepth.getIntValue(), wfm.getID().toString().length() + 1);
        result.close();
        return new PortObject[] {result.getTable()};
    }

    private void reportNodes(final WorkflowManager wfm, final BufferedDataContainer result, final int depth, final int prefixLength) {
        for (var nc : wfm.getNodeContainers()) {
            if (depth > 0 && nc instanceof WorkflowManager) {
                reportNodes((WorkflowManager) nc, result, depth - 1, prefixLength);
            } else {
                var rowID = new RowKey("Node " + nc.getID().toString().substring(prefixLength));
                var cells = new LinkedList<DataCell>();
                getSpec().forEach(col -> cells.add(extractValue(col.getName(), nc)));
                result.addRowToTable(new DefaultRow(rowID, cells.toArray(new DataCell[cells.size()])));
            }
        }
    }

    private DataTableSpec getSpec() {
        var dtsc = new DataTableSpecCreator();
        var dcss = Arrays.stream(m_includedProperties.getStringArrayValue())
            .map(k -> new DataColumnSpecCreator(k, allProps.get(k)).createSpec()).collect(Collectors.toList());
        dtsc.addColumns(dcss.toArray(new DataColumnSpec[0]));
        return dtsc.createSpec();
    }


    private static DataCell extractValue(final String key, final NodeContainer nc) {
        switch (key) {
            case "Global ID":
                return new StringCell(nc.getID().toString());
            case "Name":
                return new StringCell(nc.getName());
            case "Type":
                if (nc instanceof WorkflowManager) {
                    return new StringCell("Metanode");
                } else if (nc instanceof SubNodeContainer) {
                    return new StringCell("Component");
                } else {
                    return new StringCell("Native Node");
                }
            case "Source URI":
                if (nc instanceof WorkflowManager
                    && ((WorkflowManager)nc).getTemplateInformation().getSourceURI() != null) {
                    return new StringCell(((WorkflowManager)nc).getTemplateInformation().getSourceURI().toString());
                } else if (nc instanceof SubNodeContainer
                    && ((SubNodeContainer)nc).getTemplateInformation().getSourceURI() != null) {
                    return new StringCell(((SubNodeContainer)nc).getTemplateInformation().getSourceURI().toString());
                } else {
                    return new MissingCell("No source URI");
                }
            case "Deprecated":
                if (nc instanceof NativeNodeContainer) {
                    return ((NativeNodeContainer)nc).getNode().getFactory().isDeprecated() ? BooleanCell.TRUE
                        : BooleanCell.FALSE;
                } else {
                    return new MissingCell("Metanodes and Components don't have a deprecation state");
                }
            case "Disk Space":
                var dir = nc.getNodeContainerDirectory();
                return (dir == null) ? new MissingCell("Not saved on disk")
                    : new LongCell(FileUtils.sizeOfDirectory(dir.getFile()));
            case "Execution Time":
                var ed = nc.getNodeTimer().getLastExecutionDuration();
                return (ed == -1) ? new MissingCell("Not yet executed") : new LongCell(ed);
            case "Executions":
                return new IntCell(nc.getNodeTimer().getNrExecsSinceStart());
            case "Class Name":
                if (nc instanceof NativeNodeContainer) {
                    return new StringCell(
                        ((NativeNodeContainer)nc).getNode().getNodeModel().getClass().getCanonicalName());
                } else {
                    return new MissingCell("Only Native Nodes have class names");
                }
            default:
                throw new IllegalArgumentException("Cannot extract property " + key);
        }
    }

    private WorkflowManager getMyWFM() {
        var globalWFM = NodeContext.getContext().getWorkflowManager();
        var thisModel = this;
        var thisNodeID =
            globalWFM.findNodes(DiagnoseWorkflowNodeModel.class, new NodeModelFilter<DiagnoseWorkflowNodeModel>() {
                @Override
                public boolean include(final DiagnoseWorkflowNodeModel nodeModel) {
                    return nodeModel == thisModel;
                }
            }, true, true).keySet().iterator().next();
        return globalWFM.findNodeContainer(thisNodeID).getParent();
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
        m_maxDepth.saveSettingsTo(settings);
        m_includedProperties.saveSettingsTo(settings);
        m_excludedProperties.saveSettingsTo(settings);
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
        m_maxDepth.loadSettingsFrom(settings);
        m_includedProperties.loadSettingsFrom(settings);
        m_excludedProperties.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub

    }
}
