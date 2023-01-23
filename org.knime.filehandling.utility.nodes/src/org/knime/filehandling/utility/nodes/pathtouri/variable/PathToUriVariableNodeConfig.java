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
 *   Jan 11, 2023 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.utility.nodes.pathtouri.variable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.utility.nodes.pathtouri.exporter.URIExporterDialogHelper;

/**
 * A centralized class for encapsulating the SettingsModel Objects. The save, validate and load in NodeModel simply
 * invokes this functionality.
 *
 * @author Zkriya Rakhimberdiyev
 */
final class PathToUriVariableNodeConfig {

    static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    static final String FLOW_VARIABLE_INPUT_PORT_GRP_NAME = "Input Variables";

    static final String FLOW_VARIABLE_OUTPUT_PORT_GRP_NAME = "Appended variables (Variables Connection)";

    private static final String CFG_CREATED_VARIABLE_NAME = "suffix";

    static final String CFG_VARIABLE_FILTER = "variable_filter";

    private static final String CFG_FAIL_IF_PATH_NOT_EXISTS = "fail_if_path_not_exists";

    private static final String CFG_URL_FORMAT = "url_format";

    private static final String CFG_URL_FORMAT_SETTINGS = "url_format_settings";

    private final SettingsModelString m_variableSuffixModel;

    private final SettingsModelBoolean m_failIfPathNotExistsModel;

    private final SettingsModelString m_uriExporterModel;

    private final int m_fileSystemConnectionPortIndex;

    private final FlowVariableFilterConfiguration m_filterConfigurationModel;

    private final URIExporterModelHelper m_exporterModelHelper;

    private final URIExporterDialogHelper m_exporterDialogHelper;

    private final Supplier<Map<String, FlowVariable>> m_flowVariablesSupplier;

    /**
     * Constructor for the Configuration class.
     *
     * @param portsConfig {@link PortsConfiguration}
     * @param flowVariablesSupplier supplier of path flow variables
     */
    PathToUriVariableNodeConfig(final PortsConfiguration portsConfig,
        final Supplier<Map<String, FlowVariable>> flowVariablesSupplier) {
        m_fileSystemConnectionPortIndex =
                getFirstPortIndexInGroup(portsConfig, CONNECTION_INPUT_PORT_GRP_NAME);

        m_variableSuffixModel = new SettingsModelString(CFG_CREATED_VARIABLE_NAME, "_location") {
            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                super.validateSettingsForModel(settings);
                final var suffix = settings.getString(getConfigName());
                CheckUtils.checkSetting(!suffix.trim().isEmpty(), "Suffix must not be empty.");
            }
        };
        m_failIfPathNotExistsModel = new SettingsModelBoolean(CFG_FAIL_IF_PATH_NOT_EXISTS, true);
        m_filterConfigurationModel = new FlowVariableFilterConfiguration(CFG_VARIABLE_FILTER);

        m_uriExporterModel = new SettingsModelString(CFG_URL_FORMAT, URIExporterIDs.DEFAULT.toString());

        m_exporterModelHelper = new URIExporterModelHelper(this::getFilteredFlowVariables, //
            m_uriExporterModel, //
            m_fileSystemConnectionPortIndex);

        m_exporterDialogHelper = new URIExporterDialogHelper(m_exporterModelHelper::getURIExporterIDToFactory);
        m_flowVariablesSupplier = flowVariablesSupplier;
    }

    private static int getFirstPortIndexInGroup(final PortsConfiguration portsConfig, final String portGroupName) {
        final int[] portsInGroup = portsConfig.getInputPortLocation().get(portGroupName);
        if (portsInGroup != null && portGroupName.length() > 0) {
            return portsInGroup[0];
        } else {
            return -1;
        }
    }

    int getFileSystemConnectionPortIndex() {
        return m_fileSystemConnectionPortIndex;
    }

    /**
     * @return filter configuration model
     */
    FlowVariableFilterConfiguration getFilterConfigurationModel() {
        return m_filterConfigurationModel;
    }

    SettingsModelString getVariableSuffixModel() {
        return m_variableSuffixModel;
    }

    SettingsModelBoolean getFailIfPathNotExistsModel() {
        return m_failIfPathNotExistsModel;
    }

    SettingsModelString getURIExporterModel() {
        return m_uriExporterModel;
    }

    URIExporterID getURIExporterID() {
        return new URIExporterID(m_uriExporterModel.getStringValue());
    }

    URIExporterModelHelper getExporterModelHelper() {
        return m_exporterModelHelper;
    }

    URIExporterDialogHelper getExporterDialogHelper() {
        return m_exporterDialogHelper;
    }

    /**
     * @return filtered flow variables
     */
    Map<String, FlowVariable> getFilteredFlowVariables() {
        final var flowVariables = m_flowVariablesSupplier.get();

        final Set<String> includeNames = new HashSet<>(Arrays.asList(m_filterConfigurationModel.applyTo(flowVariables).getIncludes()));

        return flowVariables.entrySet().stream() //
            .filter(e -> includeNames.contains(e.getKey())) //
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_variableSuffixModel.validateSettings(settings);
        m_failIfPathNotExistsModel.validateSettings(settings);
        m_uriExporterModel.validateSettings(settings);

        if (!settings.containsKey(CFG_URL_FORMAT_SETTINGS)) {
            throw new InvalidSettingsException(String.format("Settings key %s not found", CFG_URL_FORMAT_SETTINGS));
        }
    }

    void loadValidatedSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_filterConfigurationModel.loadConfigurationInModel(settings);
        m_variableSuffixModel.loadSettingsFrom(settings);
        m_failIfPathNotExistsModel.loadSettingsFrom(settings);
        m_uriExporterModel.loadSettingsFrom(settings);
        m_exporterModelHelper.loadSettingsFrom(settings.getNodeSettings(CFG_URL_FORMAT_SETTINGS));
    }

    void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs,
        final Map<String, FlowVariable> variableMap)
        throws InvalidSettingsException {

        m_filterConfigurationModel.loadConfigurationInDialog(settings, variableMap);
        m_uriExporterModel.loadSettingsFrom(settings);

        m_exporterModelHelper.setPortObjectSpecs(specs);
        // we validate the settings, overwriting invalid values (so we can open the dialog),
        m_exporterModelHelper.validate(m -> {}, true);
        m_exporterDialogHelper.loadSettingsFrom(settings.getNodeSettings(CFG_URL_FORMAT_SETTINGS),
            getURIExporterID(), specs);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_filterConfigurationModel.saveConfiguration(settings);
        m_variableSuffixModel.saveSettingsTo(settings);
        m_failIfPathNotExistsModel.saveSettingsTo(settings);
        m_uriExporterModel.saveSettingsTo(settings);
        m_exporterModelHelper.saveSettingsTo(settings.addNodeSettings(CFG_URL_FORMAT_SETTINGS));
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_filterConfigurationModel.saveConfiguration(settings);
        m_uriExporterModel.saveSettingsTo(settings);
        m_exporterDialogHelper.saveSettingsTo(settings.addNodeSettings(CFG_URL_FORMAT_SETTINGS),
            getURIExporterID(),
            "Chosen URL format is not available for given file system connection or flow variables.");
    }
}
