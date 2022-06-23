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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

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
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowManager.NodeModelFilter;
import org.knime.core.util.workflowsummary.WorkflowSummary.Node;
import org.knime.core.util.workflowsummary.WorkflowSummary.Workflow;
import org.knime.core.util.workflowsummary.WorkflowSummaryCreator;
import org.knime.core.util.workflowsummary.WorkflowSummaryUtil;
import org.xml.sax.SAXException;

/**
 * NodeModel for the Diagnose Workflow node. This node is intended for debug purposes and outputs a brief overview of
 * the nodes in the workflow, as well as a full workflow summary.
 *
 * @author Jasper Krauter, KNIME AG, Schloss
 * @author Leon Wenzler, KNIME AG, Schloss
 */
public class DiagnoseWorkflowNodeModel extends NodeModel {

    /**
     * This map holds all available properties that can be added to the node overview
     */
    static final Map<String, DataType> allProps = new LinkedHashMap<>();
    static {
        allProps.put("Name", StringCell.TYPE);
        allProps.put("Class Name", StringCell.TYPE);
        allProps.put("Type", StringCell.TYPE);
        allProps.put("Source URI", StringCell.TYPE);
        allProps.put("Deprecated", BooleanCell.TYPE);
        allProps.put("Disk Space", LongCell.TYPE);
        allProps.put("Execution Time", LongCell.TYPE);
        allProps.put("Nr of Executions", IntCell.TYPE);
    }

    static final String CFGKEY_SCAN_COMPONENTS = "ScanComponents";

    //    static final String FMT_SELECTION_JSON = "JSON";
    static final String FMT_SELECTION_XML = "XML";

    static final String CFGKEY_FORMAT = "OutputFormat";

    static final String CFGKEY_INCLUDES = "IncludedProperties";

    static final String CFGKEY_EXCLUDES = "ExcludedProperties";

    static final String COLUMN_NAME = "workflow summary";

    static final String ROW_NAME = "summary";

    static final String TABLE_OUTPUT_NAME = "Node Overview";

    static final String SUMMARY_OUTPUT_NAME = "Workflow Summary";

    private SettingsModelBoolean m_scanComponents = createComponentScanSettingsModel();

    private SettingsModelString m_outputFormat = createOutputFormatSelectionModel();

    private SettingsModelStringArray m_includedProperties =
        new SettingsModelStringArray(CFGKEY_INCLUDES, getPropertiesAsStringArray());

    private SettingsModelStringArray m_excludedProperties =
        new SettingsModelStringArray(CFGKEY_EXCLUDES, new String[0]);

    /**
     * Create a new instance
     */
    protected DiagnoseWorkflowNodeModel() {
        // source node with a two output tables
        super(0, 2);
    }

    /**
     * Returns every possible workflow property, as String array.
     *
     * @return String[] allProperties
     */
    static String[] getPropertiesAsStringArray() {
        return allProps.keySet().toArray(new String[0]);
    }

    /**
     * Creates a integer settings model for storing the maximum depth.
     *
     * @return
     */
    static SettingsModelBoolean createComponentScanSettingsModel() {
        return new SettingsModelBoolean(CFGKEY_SCAN_COMPONENTS, false);
    }

    /**
     * Creates a String settings model for storing the output format selection.
     *
     * @return
     */
    static SettingsModelString createOutputFormatSelectionModel() {
        //        return new SettingsModelString(CFGKEY_FORMAT, FMT_SELECTION_JSON);
        return new SettingsModelString(CFGKEY_FORMAT, FMT_SELECTION_XML);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] in) {
        return new PortObjectSpec[]{createTableSpec(), createSummarySpec()};
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     * @throws XMLStreamException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    @Override
    protected PortObject[] execute(final PortObject[] in, final ExecutionContext ec)
        throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        var summary = WorkflowSummaryCreator.create(getMyWFM(), true);

        var tableResult = ec.createDataContainer(createTableSpec());
        reportNodes(summary.getWorkflow(), tableResult);
        tableResult.close();

        final var summaryResult = ec.createDataContainer(createSummarySpec());
        try (var os = new ByteArrayOutputStream()) {
            //            if (isJsonSelected()) {
            //                WorkflowSummaryUtil.writeJSON(os, summary, false);
            //                summaryResult.addRowToTable(
            //                    new DefaultRow(ROW_NAME, JSONCellFactory.create(os.toString(StandardCharsets.UTF_8.name()), false)));
            //            } else {
            WorkflowSummaryUtil.writeXML(os, summary, false);
            summaryResult.addRowToTable(
                new DefaultRow(ROW_NAME, XMLCellFactory.create(os.toString(StandardCharsets.UTF_8.name()))));
            //            }

            summaryResult.close();
        }

        return new PortObject[]{tableResult.getTable(), summaryResult.getTable()};
    }

    /**
     * create the table spec for the node overview
     *
     * @return
     */
    private DataTableSpec createTableSpec() {
        var dtsc = new DataTableSpecCreator().setName(TABLE_OUTPUT_NAME);
        var dcss = Arrays.stream(m_includedProperties.getStringArrayValue())
            .map(k -> new DataColumnSpecCreator(k, allProps.get(k)).createSpec()).collect(Collectors.toList());
        dtsc.addColumns(dcss.toArray(new DataColumnSpec[0]));
        return dtsc.createSpec();
    }

    /**
     * create the table spec for the workflow summary
     *
     * @return
     */
    private DataTableSpec createSummarySpec() {
        var dtsc = new DataTableSpecCreator().setName(SUMMARY_OUTPUT_NAME);
        //        dtsc.addColumns(new DataColumnSpecCreator(COLUMN_NAME, isJsonSelected() ? JSONCell.TYPE : XMLCell.TYPE).createSpec());
        dtsc.addColumns(new DataColumnSpecCreator(COLUMN_NAME, XMLCell.TYPE).createSpec());
        return dtsc.createSpec();
    }

    /**
     * recursively walk the workflow and report the node properties
     *
     * @param wf
     * @param result
     */
    private void reportNodes(final Workflow wf, final BufferedDataContainer result) {
        for (var node : wf.getNodes()) {
            var rowID = new RowKey("Node " + node.getId());
            var cells = new LinkedList<DataCell>();
            createTableSpec().forEach(col -> cells.add(extractValue(col.getName(), node)));
            result.addRowToTable(new DefaultRow(rowID, cells.toArray(new DataCell[cells.size()])));
            if (node.isMetanode() == Boolean.TRUE
                || (node.isComponent() == Boolean.TRUE && m_scanComponents.getBooleanValue())) {
                reportNodes(node.getSubWorkflow(), result);
            }
        }
    }

    //    private boolean isJsonSelected() {
    //        return m_outputFormat.getStringValue().equals(FMT_SELECTION_JSON);
    //    }

    /**
     * extracts the property with the given key from the workflow summary and wraps it in a datacell
     *
     * @param key
     * @param node
     * @return
     */
    private static DataCell extractValue(final String key, final Node node) {
        switch (key) {
            case "Name":
                return new StringCell(node.getName());
            case "Class Name":
                if (node.isMetanode() != Boolean.TRUE && node.isComponent() != Boolean.TRUE) {
                    return new StringCell(node.getFactoryKey().getClassName());
                } else {
                    return new MissingCell("Only Native Nodes have class names");
                }
            case "Type":
                if (node.isMetanode() == Boolean.TRUE) {
                    return new StringCell("Metanode");
                } else if (node.isComponent() == Boolean.TRUE) {
                    return new StringCell("Component");
                } else {
                    return new StringCell("Native Node");
                }
            case "Source URI":
                if (node.isMetanode() == Boolean.TRUE && node.getLinkInfo() != null) {
                    return new StringCell(node.getLinkInfo().getSourceURI());
                } else if (node.isComponent() == Boolean.TRUE && node.getLinkInfo() != null) {
                    return new StringCell(node.getLinkInfo().getSourceURI());
                } else {
                    return new MissingCell("No source URI");
                }
            case "Deprecated":
                if (node.isMetanode() != Boolean.TRUE && node.isComponent() != Boolean.TRUE) {
                    return node.isDeprecated() == Boolean.TRUE ? BooleanCell.TRUE : BooleanCell.FALSE;
                } else {
                    return new MissingCell("Metanodes and Components don't have a deprecation state");
                }
            case "Disk Space":
                var saved = node.getStorageInformation().isSavedToDisk();
                return saved ? new LongCell(node.getStorageInformation().getSizeOnDisk())
                    : new MissingCell("Not saved on disk");
            case "Execution Time":
                var es = node.getExecutionStatistics();
                if (es != null && es.getLastExecutionDuration() >= 0) {
                    return new LongCell(es.getLastExecutionDuration());
                } else {
                    return new MissingCell("Not yet executed");
                }
            case "Nr of Executions":
                return new IntCell((node.getExecutionStatistics() == null) ? 0
                    : node.getExecutionStatistics().getExecutionCountSinceStart());
            default:
                throw new IllegalArgumentException("Cannot extract property " + key);
        }
    }

    /**
     * Returns the workflow that this node is located in
     *
     * @return
     */
    private WorkflowManager getMyWFM() {
        // This seems to be the easiest way: search the global WFM instance for this node and then return parent
        // Ugly, sorry :(
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
        m_scanComponents.saveSettingsTo(settings);
        m_outputFormat.saveSettingsTo(settings);
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
        m_scanComponents.loadSettingsFrom(settings);
        m_outputFormat.loadSettingsFrom(settings);
        m_includedProperties.loadSettingsFrom(settings);
        m_excludedProperties.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }
}
