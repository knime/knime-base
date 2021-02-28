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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.uriexport.NoSettingsURIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.defaultnodesettings.FileSystemHelper;

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

    private static final String CFG_SELECTED_COLUMN_NAME = "column";

    private static final String CFG_SELECTED_URI_EXPORTER_NAME = "url_format";

    private static final String CFG_GENERATE_COLUMN_MODE_NAME = "generate_settings";

    private static final String CFG_APPEND_COLUMN_NAME = "append_column";

    private static final String CFG_REPLACE_COLUMN_NAME = "replace_column";

    private final SettingsModelString m_selectedColumnNameModel;

    private final SettingsModelString m_selectedUriExporterModel;

    private final SettingsModelBoolean m_generatedColumnModeModel;

    private final SettingsModelString m_appendColumnNameModel;

    private final SettingsModelString m_replaceColumnNameModel;

    private NodeSettingsRO m_uriExporterNodeSettingsRO;

    private int m_fileSystemPortIndex = -1;

    private final int m_dataTablePortIndex;

    /**
     * Constructor for the Configuration class
     *
     * @param portsConfig An object of PortsConfiguration Class
     */
    public PathToUrlNodeConfig(final PortsConfiguration portsConfig) {
        m_selectedColumnNameModel = new SettingsModelString(CFG_SELECTED_COLUMN_NAME, null);
        m_selectedUriExporterModel =
            new SettingsModelString(CFG_SELECTED_URI_EXPORTER_NAME, URIExporterIDs.DEFAULT.toString());
        m_generatedColumnModeModel = new SettingsModelBoolean(CFG_GENERATE_COLUMN_MODE_NAME, true);

        m_appendColumnNameModel = new SettingsModelString(CFG_APPEND_COLUMN_NAME, "URL");
        m_appendColumnNameModel.setEnabled(this.isGenerateColAppendMode());

        m_replaceColumnNameModel = new SettingsModelString(CFG_REPLACE_COLUMN_NAME, null) {

            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                super.validateSettingsForModel(settings);
                //check if user input matches the regex for a valid filename, otherwise throw InvalidSettingsException
                if (StringUtils.isEmpty(settings.getString(CFG_REPLACE_COLUMN_NAME))) {
                    throw new InvalidSettingsException("The selected output column is not valid");
                }
            }
        };
        m_replaceColumnNameModel.setEnabled(!this.isGenerateColAppendMode());

        m_dataTablePortIndex =
            portsConfig.getInputPortLocation().get(PathToUrlNodeConfig.DATA_TABLE_INPUT_PORT_GRP_NAME)[0];

        if (portsConfig.getInputPortLocation().get(CONNECTION_INPUT_PORT_GRP_NAME) != null) {
            m_fileSystemPortIndex = portsConfig.getInputPortLocation().get(CONNECTION_INPUT_PORT_GRP_NAME)[0];
        }

    }

    SettingsModelString getSelectedColumnNameModel() {
        return m_selectedColumnNameModel;
    }

    String getStringValOfSelectedColumnNameModel() {
        return m_selectedColumnNameModel.getStringValue();
    }

    SettingsModelString getSelectedUriExporterModel() {
        return m_selectedUriExporterModel;
    }

    SettingsModelString getAppendColumnNameModel() {
        return m_appendColumnNameModel;
    }

    SettingsModelString getReplaceColumnNameModel() {
        return m_replaceColumnNameModel;
    }

    SettingsModelBoolean getGenerateColumnModeModel() {
        return m_generatedColumnModeModel;
    }

    int getFileSystemPortIndex() {
        return m_fileSystemPortIndex;
    }

    int getDataTablePortIndex() {
        return m_dataTablePortIndex;
    }

    String getSelectedColNameStringVal() {
        return m_selectedColumnNameModel.getStringValue();
    }

    void setSelectedColNameStringVal(final String columnName) {
        m_selectedColumnNameModel.setStringValue(columnName);
    }

    void setReplaceColNameStringVal(final String columnName) {
        m_replaceColumnNameModel.setStringValue(columnName);
    }

    String getAppendColNameStringVal() {
        return m_appendColumnNameModel.getStringValue();
    }

    String getselectedUriExporterStringVal() {
        return m_selectedUriExporterModel.getStringValue();
    }

    String getReplaceColNameStringVal() {
        return m_replaceColumnNameModel.getStringValue();
    }

    /**
     * Implements save settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsWO object from NodeModel
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        m_selectedColumnNameModel.saveSettingsTo(settings);
        m_selectedUriExporterModel.saveSettingsTo(settings);
        m_generatedColumnModeModel.saveSettingsTo(settings);
        m_appendColumnNameModel.saveSettingsTo(settings);
        m_replaceColumnNameModel.saveSettingsTo(settings);

        if (m_uriExporterNodeSettingsRO != null) {
            final NodeSettingsWO uriExporterSetting = settings.addNodeSettings(URIExporter.CFG_URI_EXPORTER_SETTINGS);
            m_uriExporterNodeSettingsRO.copyTo(uriExporterSetting);
        }
    }

    /**
     * Implements validate settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsRO object from NodeModel
     */
    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selectedColumnNameModel.validateSettings(settings);
        m_selectedUriExporterModel.validateSettings(settings);
        m_generatedColumnModeModel.validateSettings(settings);
        m_appendColumnNameModel.validateSettings(settings);
        m_replaceColumnNameModel.validateSettings(settings);
    }

    /**
     * Implements load validated settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsRO object from NodeModel
     */
    void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selectedColumnNameModel.loadSettingsFrom(settings);
        m_selectedUriExporterModel.loadSettingsFrom(settings);
        m_generatedColumnModeModel.loadSettingsFrom(settings);
        m_appendColumnNameModel.loadSettingsFrom(settings);
        m_replaceColumnNameModel.loadSettingsFrom(settings);

        m_uriExporterNodeSettingsRO = settings.getNodeSettings(URIExporter.CFG_URI_EXPORTER_SETTINGS);
    }


    /**
     * @return the uriExporterNodeSettingsRO
     */
    NodeSettingsRO getUriExporterNodeSettingsRO() {
        return m_uriExporterNodeSettingsRO;
    }

    /**
     * Load the value of column generation model from settings. This method is invoked from the NodeDialog
     *
     * @param settings NodeSettingsRO object
     * @throws NotConfigurableException Exception
     */
    void loadGenerateColModeForDialog(final NodeSettingsRO settings) throws NotConfigurableException {
        try {
            m_generatedColumnModeModel.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage(), ex);
        }
    }

    /**
     * Update value for the settings model of generate column mode
     *
     * @param settings NodeSettingsWO object
     * @param modelVal new boolean value
     */
    void saveGenerateColModeForDialog(final NodeSettingsWO settings, final boolean modelVal) {
        m_generatedColumnModeModel.setBooleanValue(modelVal);
        m_generatedColumnModeModel.saveSettingsTo(settings);
    }

    /**
     * Check if the generate column is in true (APPEND) mode
     *
     * @return true or false value
     */
    final boolean isGenerateColAppendMode() {
        return m_generatedColumnModeModel.getBooleanValue();
    }

    /**
     * Convert URI Exporter IDs to a list of string
     *
     * @param mapOfUriExporters A map of URI Exporters
     * @return A list of URI exporter Ids expressed as Strings
     */
    static List<String> getListOfUriExporterIds(final Map<URIExporterID, URIExporter> mapOfUriExporters) {
        return mapOfUriExporters.keySet().stream().map(URIExporterID::toString).collect(Collectors.toList());
    }

    /**
     * Generate FSConnection objects using fake paths by properties from FSLocationSpec parameter. In case of CUSTOM_URL
     * use a placeholder URL, since only the URI Exporters are used the provided URL is inconsequential
     *
     * @param locationSpec Instance of FSLocationSpec
     * @return Optional<FSConnection> An object of FSConnection
     */
    static Optional<FSConnection> getFSConnectionWithFakePath(final FSLocationSpec locationSpec) {

        final Optional<String> fileSysSpecifier = locationSpec.getFileSystemSpecifier();
        final String fakePathStringVal =
            locationSpec.getFSCategory() == FSCategory.CUSTOM_URL ? "https://www.knime.com/" : ".";
        final FSLocation fakeFSLocation =
            new FSLocation(locationSpec.getFSCategory(), fileSysSpecifier.orElse(null), fakePathStringVal);

        return FileSystemHelper.retrieveFSConnection(Optional.empty(), fakeFSLocation);

    }

    /**
     * An empty implementation of URI Exporter to be used as placeholder if no other URIExporters are available
     */
    static final class DefaultURIExporter extends NoSettingsURIExporter {

        @Override
        public String getLabel() {
            return "Default";
        }

        @Override
        public String getDescription() {
            return "Default URL Format";
        }

        @Override
        public URI toUri(final FSPath path) throws URISyntaxException {
            return null;
        }

    }

}
