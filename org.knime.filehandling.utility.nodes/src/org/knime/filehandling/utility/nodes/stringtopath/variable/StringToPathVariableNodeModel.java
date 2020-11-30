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
 *   Nov 30, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */

package org.knime.filehandling.utility.nodes.stringtopath.variable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationFactory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.SettingsModelFileSystem;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * The node model for the "String to Path (Variable)" node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class StringToPathVariableNodeModel extends NodeModel {

    static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    static final String VARIABLE_INPUT_PORT_GRP_NAME = "Input Variables";

    static final String CFG_VARIABLE_FILTER = "variable_filter";

    private static final String CFG_SUFFIX = "suffix";

    private static final String CFG_FILE_SYSTEM = "file_system";

    private static final String CFG_ABORT_ON_MISSING_FILE = "fail_on_missing_file_folder";

    private final SettingsModelFileSystem m_fileSystemModel;

    private final NodeModelStatusConsumer m_statusConsumer;

    private final SettingsModelString m_variableSuffix = createSettingsModelVariableSuffix();

    private final SettingsModelBoolean m_abortOnMissingFileModel = createSettingsModelAbortOnMissingFile();

    private FlowVariableFilterConfiguration m_filter;

    static SettingsModelFileSystem createSettingsModelFileSystem(final PortsConfiguration portsConfig) {
        return new SettingsModelFileSystem(CFG_FILE_SYSTEM, portsConfig,
            StringToPathVariableNodeModel.CONNECTION_INPUT_PORT_GRP_NAME, EnumSet.allOf(FSCategory.class));
    }

    static SettingsModelString createSettingsModelVariableSuffix() {
        return new SettingsModelString(CFG_SUFFIX, "_location") {
            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                super.validateSettingsForModel(settings);
                final String suffix = settings.getString(getConfigName());
                CheckUtils.checkSetting(!suffix.trim().isEmpty(), "Suffix must not be empty.");
            }
        };
    }

    static SettingsModelBoolean createSettingsModelAbortOnMissingFile() {
        return new SettingsModelBoolean(CFG_ABORT_ON_MISSING_FILE, false);
    }

    StringToPathVariableNodeModel(final PortsConfiguration portsConfig) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_fileSystemModel = createSettingsModelFileSystem(portsConfig);
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.INFO));
        m_filter = new FlowVariableFilterConfiguration(CFG_VARIABLE_FILTER);
        m_filter.loadDefaults(getAvailableFlowVariables(StringType.INSTANCE), true);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final Map<String, FlowVariable> filteredVariables = getFilteredVariables();
        if (filteredVariables.isEmpty()) {
            setWarningMessage("No string variables selected.");
        }
        m_fileSystemModel.configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }

    private Map<String, FlowVariable> getFilteredVariables() {
        final Map<String, FlowVariable> availableVars = getAvailableFlowVariables(StringType.INSTANCE);
        final Set<String> includeNames = new HashSet<>(Arrays.asList(m_filter.applyTo(availableVars).getIncludes()));
        return availableVars.entrySet().stream() //
            .filter(e -> includeNames.contains(e.getKey())) //
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    @Override
    protected PortObject[] execute(final PortObject[] data, final ExecutionContext exec) throws Exception {
        final FSLocationSpec locationSpec = m_fileSystemModel.getLocationSpec();
        try (final FSLocationFactory fsLocationFactory = m_fileSystemModel.createFSLocationFactory();
                final FSPathProviderFactory fsPathProviderFactory =
                    FSPathProviderFactory.newFactory(m_fileSystemModel.getConnection(), locationSpec)) {
            // convert
            final Map<String, FlowVariable> filteredVariables = getFilteredVariables();
            final UniqueNameGenerator nameGenerator = new UniqueNameGenerator(
                getAvailableFlowVariables(VariableTypeRegistry.getInstance().getAllTypes()).keySet());
            for (final Entry<String, FlowVariable> e : filteredVariables.entrySet()) {
                final String stringValue = e.getValue().getValue(StringType.INSTANCE);
                CheckUtils.checkArgument(stringValue != null && !stringValue.trim().isEmpty(),
                    "Some of the selected variables contain empty strings.");
                final FSLocation fsLocation = fsLocationFactory.createLocation(stringValue);
                if (m_abortOnMissingFileModel.getBooleanValue()) {
                    checkIfFileExists(fsLocation, fsPathProviderFactory);
                }
                pushFlowVariable(nameGenerator.newName(e.getKey() + m_variableSuffix.getStringValue()),
                    FSLocationVariableType.INSTANCE, fsLocation);
            }
        }
        // do nothing since configure already pushed the new variables
        return new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    private static void checkIfFileExists(final FSLocation fsLocation,
        final FSPathProviderFactory fsPathProviderFactory) {
        try (final FSPathProvider pathProvider = fsPathProviderFactory.create(fsLocation)) {
            final FSPath fsPath = pathProvider.getPath();
            FSFiles.exists(fsPath);
        } catch (final IOException e) {
            throw new IllegalArgumentException(
                String.format("The file/folder '%s' does not exists or cannot be accessed", fsLocation.getPath()), e);
        }
    }

    @Override
    protected void reset() {
        // Nothing to do here
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_filter.saveConfiguration(settings);
        m_variableSuffix.saveSettingsTo(settings);
        m_fileSystemModel.saveSettingsTo(settings);
        m_abortOnMissingFileModel.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        final FlowVariableFilterConfiguration conf = new FlowVariableFilterConfiguration(CFG_VARIABLE_FILTER);
        conf.loadConfigurationInModel(settings);
        m_filter = conf;
        m_fileSystemModel.loadSettingsFrom(settings);
        m_variableSuffix.loadSettingsFrom(settings);
        m_abortOnMissingFileModel.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final FlowVariableFilterConfiguration conf = new FlowVariableFilterConfiguration(CFG_VARIABLE_FILTER);
        conf.loadConfigurationInModel(settings);
        m_variableSuffix.validateSettings(settings);
        m_fileSystemModel.validateSettings(settings);
        m_abortOnMissingFileModel.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do here
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do here
    }

}
