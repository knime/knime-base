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
 *   May 22, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.FileSystemHelper;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.FileSystemChooserUtils;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.FileSystemConfiguration;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.defaultnodesettings.status.StatusReporter;

/**
 * SettingsModel for the {@link AbstractDialogComponentFileChooser}.</br>
 * It allows to create {@link ReadPathAccessor} and {@link WritePathAccessor} objects for accessing the {@link FSPath
 * paths} specified in the dialog.</br>
 * <b>IMPORTANT NOTE:</b> Nodes that use this settings model must call the
 * {@link #configureInModel(PortObjectSpec[], Consumer)} method in the respective {@code configure} method of the node
 * model before retrieving e.g. the {@link FSLocation} via {@link #getLocation()}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractSettingsModelFileChooser extends SettingsModel implements StatusReporter {

    private static final DefaultStatusMessage NO_LOCATION_ERROR =
        new DefaultStatusMessage(MessageType.ERROR, "Please specify a location");

    private final FileSystemConfiguration<FSLocationConfig> m_fsConfig;

    private final String m_configName;

    private final SettingsModelFilterMode m_filterModeModel;

    private String[] m_fileExtensions;

    /**
     * Constructor.
     *
     * @param configName under which to store the settings
     * @param portsConfig {@link PortsConfiguration} of the corresponding KNIME node
     * @param fileSystemPortIdentifier identifier of the file system port group in <b>portsConfig</b>
     * @param defaultFilterMode the default {@link FilterMode}
     * @param fileExtensions the supported file extensions
     */
    public AbstractSettingsModelFileChooser(final String configName, final PortsConfiguration portsConfig,
        final String fileSystemPortIdentifier, final FilterMode defaultFilterMode, final String... fileExtensions) {
        m_fsConfig = FileSystemChooserUtils.createConfig(portsConfig, fileSystemPortIdentifier, new FSLocationConfig());
        m_fsConfig.addChangeListener(e -> notifyChangeListeners());
        m_configName = configName;
        m_fileExtensions =
            CheckUtils.checkArgumentNotNull(fileExtensions, "The fileExtensions may be empty but never null.").clone();
        m_filterModeModel = new SettingsModelFilterMode("filter_mode", defaultFilterMode);
        m_filterModeModel.addChangeListener(e -> notifyChangeListeners());
    }

    /**
     * Copy constructor for use in {@link #createClone()}.
     *
     * @param toCopy instance to copy
     */
    protected AbstractSettingsModelFileChooser(final AbstractSettingsModelFileChooser toCopy) {
        m_configName = toCopy.m_configName;
        m_fsConfig = toCopy.m_fsConfig.copy();
        m_fileExtensions = toCopy.m_fileExtensions.clone();
        m_filterModeModel = toCopy.m_filterModeModel.createClone();
    }

    /**
     * Configures the settings model in the node model and validates teh input specs and settings.
     *
     * @param specs input specs of the node
     * @param statusMessageConsumer for communicating status messages
     * @throws InvalidSettingsException if the settings are invalid or incompatible with <b>specs</b>
     */
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer)
        throws InvalidSettingsException {
        checkLocation();
        CheckUtils.checkSetting(getLocation().getPath().length() > 0, "Please specify a location");
        m_fsConfig.configureInModel(specs, statusMessageConsumer);
    }

    private void checkLocation() throws InvalidSettingsException {
        final PriorityStatusConsumer locationStatusConsumer = new PriorityStatusConsumer();
        reportOnLocation(locationStatusConsumer);
        Optional<StatusMessage> locationStatus = locationStatusConsumer.get();
        if (locationStatus.isPresent()) {
            final StatusMessage actualLocationStatus = locationStatus.get();
            CheckUtils.checkSetting(actualLocationStatus.getType() != MessageType.ERROR,
                actualLocationStatus.getMessage());
        }
    }

    /**
     * Returns the keys corresponding to the location configuration.</br>
     * Use this method when creating the {@link FlowVariableModel} for the location in the node dialog.
     *
     * @return the keys corresponding to the location configuration
     */
    public String[] getKeysForFSLocation() {
        return new String[]{m_configName, FSLocationConfig.CFG_LOCATION};
    }

    FileSystemConfiguration<FSLocationConfig> getFileSystemConfiguration() {
        return m_fsConfig;
    }

    String[] getFileExtensions() {
        return m_fileExtensions;
    }

    /**
     * Sets the provided file extensions.
     *
     * @param fileExtensions the new file extensions
     */
    public void setFileExtensions(final String... fileExtensions) {
        CheckUtils.checkArgumentNotNull(fileExtensions, "The fileExtensions may be empty but never null.");
        if (!Arrays.equals(m_fileExtensions, fileExtensions)) {
            m_fileExtensions = fileExtensions.clone();
            notifyChangeListeners();
        }
    }

    /**
     * Creates a {@link FileChooserPathAccessor} that can be used for both reading and writing.
     *
     * @return a {@link FileChooserPathAccessor}
     */
    protected final FileChooserPathAccessor createPathAccessor() {
        return new FileChooserPathAccessor(this, m_fsConfig.getConnection());
    }

    SettingsModelFilterMode getFilterModeModel() {
        return m_filterModeModel;
    }

    /**
     * @return the selected FSConnection or {@link Optional#empty()} if the file system isn't connected
     */
    Optional<FSConnection> getConnection() {
        final Optional<FSConnection> connection = m_fsConfig.getConnection();
        return FileSystemHelper.retrieveFSConnection(connection, getLocation());
    }

    /**
     * Sets the given {@link FSLocation}.
     *
     * @param location the {@link FSLocation} to be set
     */
    public void setLocation(final FSLocation location) {
        m_fsConfig.setLocationSpec(location);
    }

    /**
     * Returns the currently configured {@link FSLocation}.
     *
     * @return the currently configured {@link FSLocation}
     */
    public FSLocation getLocation() {
        return m_fsConfig.getLocationConfig().getLocationSpec();
    }

    @Override
    protected String getModelTypeID() {
        return "SMID_FileChooserGen3";
    }

    @Override
    protected String getConfigName() {
        return m_configName;
    }

    @Override
    protected final void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        final NodeSettingsRO topLevel = getChildOrEmpty(settings, m_configName);
        m_fsConfig.loadSettingsForDialog(topLevel, specs);
        m_filterModeModel.loadSettingsForDialog(topLevel, specs);
        loadAdditionalSettingsForDialog(topLevel, specs);
    }

    /**
     * Hook for extending classes to load further settings in the dialog.
     *
     * @param settings the top level settings of this settings model (already extracted with the config name)
     * @param specs input specs of the node
     * @throws NotConfigurableException if the node is not configurable with the provided settings
     */
    protected void loadAdditionalSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        // no additional settings to load
    }

    private static NodeSettingsRO getChildOrEmpty(final NodeSettingsRO settings, final String childKey) {
        try {
            return settings.getNodeSettings(childKey);
        } catch (InvalidSettingsException ex) {
            return new NodeSettings(childKey);
        }
    }

    @Override
    protected final void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        final NodeSettingsWO topLevel = settings.addNodeSettings(m_configName);
        save(topLevel);
        saveAdditionalSettingsForDialog(topLevel);
    }

    /**
     * Hook for extending classes to save additional settings in the node dialog.
     *
     * @param settings corresponding to this settings model
     * @throws InvalidSettingsException if the current settings are invalid
     */
    protected void saveAdditionalSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        // no additional settings to save
    }

    @Override
    protected final void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO topLevel = settings.getNodeSettings(m_configName);
        m_fsConfig.validateSettingsForModel(topLevel);
        m_filterModeModel.validateSettings(topLevel);
        validateAdditionalSettingsForModel(topLevel);
    }

    /**
     * Hook for extending classes to validate additional settings in the node model.
     *
     * @param settings corresponding to this settings model
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected void validateAdditionalSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no additional settings to validate
    }

    @Override
    protected final void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO topLevel = settings.getNodeSettings(m_configName);
        m_fsConfig.loadSettingsForModel(topLevel);
        m_filterModeModel.loadSettingsFrom(topLevel);
        loadAdditionalSettingsForModel(topLevel);
    }

    /**
     * Hook for extending classes to load additional settings in the node model.
     *
     * @param settings corresponding to this settings model
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected void loadAdditionalSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no additional settings to load
    }

    @Override
    protected final void saveSettingsForModel(final NodeSettingsWO settings) {
        final NodeSettingsWO topLevel = settings.addNodeSettings(m_configName);
        save(topLevel);
        saveAdditionalSettingsForModel(topLevel);
    }

    /**
     * Hook for extending classes to save additional settings in the node model.
     *
     * @param settings corresponding to this settings model
     */
    protected void saveAdditionalSettingsForModel(final NodeSettingsWO settings) {
        // nothing to do
    }

    private void save(final NodeSettingsWO topLevel) {
        m_fsConfig.saveSettingsForModel(topLevel);
        m_filterModeModel.saveSettingsTo(topLevel);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    @Override
    public void report(final Consumer<StatusMessage> messageConsumer) {
        reportOnLocation(messageConsumer);
        m_fsConfig.report(messageConsumer);
    }

    private void reportOnLocation(final Consumer<StatusMessage> messageConsumer) {
        if (getLocation().getPath().length() == 0) {
            messageConsumer.accept(NO_LOCATION_ERROR);
        }
    }

}
