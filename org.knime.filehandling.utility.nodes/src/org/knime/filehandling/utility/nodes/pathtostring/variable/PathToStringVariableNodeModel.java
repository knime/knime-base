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
 *   Nov 27, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.pathtostring.variable;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.location.MultiFSPathProviderFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCell;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.utility.nodes.pathtostring.PathToStringUtils;

/**
 * This node allows you to convert a flow variable of type {@link FSLocationVariableType} to a flow variable of type
 * String. Depending on the settings, the created flow variable will contain the value returned by
 * {@link FSLocation#getPath()} stored in the {@link SimpleFSLocationCell} or the string value of the {@link URI} returned by
 * the {@link URIExporter} with id {@link URIExporterIDs#LEGACY_KNIME_URL}.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class PathToStringVariableNodeModel extends NodeModel {

    static final String CFG_VARIABLE_FILTER = "variable_filter";

    private static final String CFG_CREATED_VARIABLE_NAME = "suffix";

    private static final String CFG_CREATE_KNIME_URL = "create_knime_url";

    private final SettingsModelString m_variableSuffix = createSettingsModelVariableSuffix();

    private final SettingsModelBoolean m_createKNIMEUrl = createSettingsModelCreateKNIMEUrl();

    private FlowVariableFilterConfiguration m_filter;

    static SettingsModelString createSettingsModelVariableSuffix() {
        return new SettingsModelString(CFG_CREATED_VARIABLE_NAME, "_location") {
            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                super.validateSettingsForModel(settings);
                final String suffix = settings.getString(getConfigName());
                CheckUtils.checkSetting(!suffix.trim().isEmpty(), "Suffix must not be empty.");
            }
        };
    }

    static SettingsModelBoolean createSettingsModelCreateKNIMEUrl() {
        return new SettingsModelBoolean(CFG_CREATE_KNIME_URL, true);
    }

    PathToStringVariableNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE}, new PortType[]{FlowVariablePortObject.TYPE});
        m_filter = new FlowVariableFilterConfiguration(CFG_VARIABLE_FILTER);
        m_filter.loadDefaults(getAvailableFlowVariables(FSLocationVariableType.INSTANCE), true);
    }

    @Override
    @SuppressWarnings("resource") // the FSPathProviderFactorys are closed by the MultiFSPathProviderCellFactory
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final Map<String, FlowVariable> filteredVariables = getFilteredVariables();
        if (filteredVariables.isEmpty()) {
            setWarningMessage("No path variables selected.");
        }
        final UniqueNameGenerator nameGenerator = new UniqueNameGenerator(
            getAvailableFlowVariables(VariableTypeRegistry.getInstance().getAllTypes()).keySet());
        try (final MultiFSPathProviderFactory multiFSPathProviderCellFactory = new MultiFSPathProviderFactory()) {
            for (final Entry<String, FlowVariable> e : filteredVariables.entrySet()) {
                final FSLocation fsLocation = e.getValue().getValue(FSLocationVariableType.INSTANCE);
                final String result;
                if (createKNIMEUrl(fsLocation)) {
                    result = PathToStringUtils.fsLocationToString(fsLocation,
                        multiFSPathProviderCellFactory.getOrCreateFSPathProviderFactory(fsLocation));
                } else {
                    result = fsLocation.getPath();
                }
                pushFlowVariableString(nameGenerator.newName(e.getKey() + m_variableSuffix.getStringValue()), result);
            }
        }
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    private boolean createKNIMEUrl(final FSLocation fsLocation) {
        return m_createKNIMEUrl.getBooleanValue() && (fsLocation.getFSCategory() == FSCategory.RELATIVE
            || fsLocation.getFSCategory() == FSCategory.MOUNTPOINT);
    }

    private Map<String, FlowVariable> getFilteredVariables() {
        final Map<String, FlowVariable> availableVars = getAvailableFlowVariables(FSLocationVariableType.INSTANCE);
        final Set<String> includeNames = new HashSet<>(Arrays.asList(m_filter.applyTo(availableVars).getIncludes()));

        return availableVars.entrySet().stream() //
            .filter(e -> includeNames.contains(e.getKey())) //
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) {
        // do nothing since configure already pushed the new variables
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_filter.saveConfiguration(settings);
        m_variableSuffix.saveSettingsTo(settings);
        m_createKNIMEUrl.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final FlowVariableFilterConfiguration conf = new FlowVariableFilterConfiguration(CFG_VARIABLE_FILTER);
        conf.loadConfigurationInModel(settings);
        m_variableSuffix.validateSettings(settings);
        m_createKNIMEUrl.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        final FlowVariableFilterConfiguration conf = new FlowVariableFilterConfiguration(CFG_VARIABLE_FILTER);
        conf.loadConfigurationInModel(settings);
        m_filter = conf;
        m_variableSuffix.loadSettingsFrom(settings);
        m_createKNIMEUrl.loadSettingsFrom(settings);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
        // nothing to do
    }

    @Override
    protected void reset() {
        // nothing to do
    }

}
