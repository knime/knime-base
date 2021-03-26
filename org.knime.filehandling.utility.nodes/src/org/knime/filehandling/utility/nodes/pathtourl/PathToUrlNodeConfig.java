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
 *   Dec 22, 2020 (Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.pathtourl;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;

/**
 * A centralized class for encapsulating the SettingsModel Objects. The save, validate and load in NodeModel simply
 * invokes this functionality
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
public class PathToUrlNodeConfig {

    static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    static final String DATA_TABLE_INPUT_PORT_GRP_NAME = "Input Table";

    static final String DATA_TABLE_OUTPUT_PORT_GRP_NAME = "Output Table";

    private static final String CFG_COLUMN = "column";

    private static final String CFG_URL_FORMAT = "url_format";

    private static final String CFG_APPEND_COLUMN = "generate_settings";

    private static final String CFG_APPEND_COLUMN_NAME = "append_column";

    private static final String CFG_REPLACE_COLUMN_NAME = "replace_column";

    private static final String CFG_URL_FORMAT_SETTINGS = "url_format_settings";

    private final int m_dataTablePortIndex;

    private final int m_fileSystemConnectionPortIndex;

    private final SettingsModelString m_pathColumnNameModel;

    private final SettingsModelString m_uriExporterModel;

    private final SettingsModelBoolean m_appendColumnModel;

    private final SettingsModelString m_appendColumnNameModel;

    private final SettingsModelString m_replaceColumnNameModel;

    private final URIExporterDialogHelper m_exporterDialogHelper;

    private final URIExporterModelHelper m_exporterModelHelper;

    /**
     * Constructor for the Configuration class
     *
     * @param portsConfig An object of PortsConfiguration Class
     */
    public PathToUrlNodeConfig(final PortsConfiguration portsConfig) {
        m_dataTablePortIndex =
            getFirstPortIndexInGroup(portsConfig, PathToUrlNodeConfig.DATA_TABLE_INPUT_PORT_GRP_NAME);
        m_fileSystemConnectionPortIndex =
            getFirstPortIndexInGroup(portsConfig, PathToUrlNodeConfig.CONNECTION_INPUT_PORT_GRP_NAME);

        m_pathColumnNameModel = new SettingsModelString(CFG_COLUMN, "");

        m_uriExporterModel =
            new SettingsModelString(CFG_URL_FORMAT, URIExporterIDs.DEFAULT.toString());

        m_appendColumnModel = new SettingsModelBoolean(CFG_APPEND_COLUMN, true);
        m_appendColumnNameModel = new SettingsModelString(CFG_APPEND_COLUMN_NAME, "URL");
        m_replaceColumnNameModel = new SettingsModelString(CFG_REPLACE_COLUMN_NAME, "");

        m_exporterDialogHelper = new URIExporterDialogHelper(m_pathColumnNameModel, //
            m_uriExporterModel, //
            m_fileSystemConnectionPortIndex, //
            m_dataTablePortIndex);

        m_exporterModelHelper = new URIExporterModelHelper(m_pathColumnNameModel, //
            m_uriExporterModel, //
            m_fileSystemConnectionPortIndex, //
            m_dataTablePortIndex);


        m_appendColumnModel.addChangeListener(e -> updateEnabledness());
    }

    private void updateEnabledness() {
        m_appendColumnNameModel.setEnabled(m_appendColumnModel.getBooleanValue());
        m_replaceColumnNameModel.setEnabled(!m_appendColumnModel.getBooleanValue());
    }

    int getDataTablePortIndex() {
        return m_dataTablePortIndex;
    }

    int getFileSystemConnectionPortIndex() {
        return m_fileSystemConnectionPortIndex;
    }
    private static int getFirstPortIndexInGroup(final PortsConfiguration portsConfig, final String portGroupName) {
        final int[] portsInGroup = portsConfig.getInputPortLocation().get(portGroupName);
        if (portsInGroup != null && portGroupName.length() > 0) {
            return portsInGroup[0];
        } else {
            return -1;
        }
    }

    SettingsModelString getPathColumnNameModel() {
        return m_pathColumnNameModel;
    }

    String getPathColumnName() {
        return m_pathColumnNameModel.getStringValue();
    }

    SettingsModelString getURIExporterModel() {
        return m_uriExporterModel;
    }

    URIExporterID getURIExporterID() {
        return new URIExporterID(m_uriExporterModel.getStringValue());
    }

    SettingsModelString getAppendColumnNameModel() {
        return m_appendColumnNameModel;
    }

    SettingsModelString getReplaceColumnNameModel() {
        return m_replaceColumnNameModel;
    }

    SettingsModelBoolean getAppendColumnModel() {
        return m_appendColumnModel;
    }

    String getAppendColumnName() {
        return m_appendColumnNameModel.getStringValue();
    }

    String getReplaceColColumnName() {
        return m_replaceColumnNameModel.getStringValue();
    }

    /**
     * Implements save settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsWO object from NodeModel
     */
    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_pathColumnNameModel.saveSettingsTo(settings);
        m_uriExporterModel.saveSettingsTo(settings);
        m_appendColumnModel.saveSettingsTo(settings);
        m_appendColumnNameModel.saveSettingsTo(settings);
        m_replaceColumnNameModel.saveSettingsTo(settings);
        m_exporterModelHelper.saveSettingsTo(settings.addNodeSettings(CFG_URL_FORMAT_SETTINGS));
    }

    /**
     * Implements validate settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsRO object from NodeModel
     */
    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_pathColumnNameModel.validateSettings(settings);
        m_uriExporterModel.validateSettings(settings);
        m_appendColumnModel.validateSettings(settings);
        m_appendColumnNameModel.validateSettings(settings);
        m_replaceColumnNameModel.validateSettings(settings);

        if (!settings.containsKey(CFG_URL_FORMAT_SETTINGS)) {
            throw new InvalidSettingsException(String.format("Settings key %s not found", CFG_URL_FORMAT_SETTINGS));
        }
    }

    /**
     * Implements load validated settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsRO object from NodeModel
     */
    void loadValidatedSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_pathColumnNameModel.loadSettingsFrom(settings);
        m_uriExporterModel.loadSettingsFrom(settings);
        m_appendColumnModel.loadSettingsFrom(settings);
        m_appendColumnNameModel.loadSettingsFrom(settings);
        m_replaceColumnNameModel.loadSettingsFrom(settings);
        m_exporterModelHelper.loadSettingsFrom(settings.getNodeSettings(CFG_URL_FORMAT_SETTINGS));
    }

    void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws InvalidSettingsException {

        m_uriExporterModel.loadSettingsFrom(settings);
        m_appendColumnModel.loadSettingsFrom(settings);
        m_exporterDialogHelper.setPortObjectSpecs(specs);
        m_exporterDialogHelper.loadSettingsFrom(settings.getNodeSettings(CFG_URL_FORMAT_SETTINGS));
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_uriExporterModel.saveSettingsTo(settings);
        m_appendColumnModel.saveSettingsTo(settings);
        m_exporterDialogHelper.saveSettingsTo(settings.addNodeSettings(CFG_URL_FORMAT_SETTINGS));
    }

    boolean shouldAppendColumn() {
        return m_appendColumnModel.getBooleanValue();
    }

    URIExporterDialogHelper getExporterDialogHelper() {
        return m_exporterDialogHelper;
    }
}
