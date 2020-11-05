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
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.event.ChangeListener;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.filehandling.core.connections.FSCategory;
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
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.defaultnodesettings.status.StatusReporter;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * SettingsModel for the {@link AbstractDialogComponentFileChooser}.</br>
 * It allows to create {@link ReadPathAccessor} and {@link WritePathAccessor} objects for accessing the {@link FSPath
 * paths} specified in the dialog.</br>
 * <b>IMPORTANT NOTE:</b> Nodes that use this settings model must call the
 * {@link #configureInModel(PortObjectSpec[], Consumer)} method in the respective {@code configure} method of the node
 * model before retrieving e.g. the {@link FSLocation} via {@link #getLocation()}.</br>
 * </br>
 *
 * <b>Intended usage:</b> If you only need the path string, it is sufficient to call {@link #getLocation()} and then
 * {@link FSLocation#getPath()}. This call does not cause any I/O as opposed to calls on {@link ReadPathAccessor} or
 * {@link WritePathAccessor}.</b> However, if you need access to the actual {@link FSPath} objects, you will have to use
 * the {@link ReadPathAccessor} or {@link WritePathAccessor}.</br>
 * When used in the {@link NodeModel}, it is paramount to call the {@link #configureInModel(PortObjectSpec[], Consumer)}
 * method in the {@code configure} method of the {@link NodeModel}. This serves two purposes: It updates the model with
 * the incoming file system and validates that the file system is indeed the correct one. The
 * {@link #configureInModel(PortObjectSpec[], Consumer)} accepts a {@link Consumer} of {@link StatusMessage} in order to
 * report warning messages. In most cases you can make use of {@link NodeModelStatusConsumer}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> The actual type of the implementation of the AbstractSettingsModelFileChooser
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class AbstractSettingsModelFileChooser<T extends AbstractSettingsModelFileChooser<T>>
    extends SettingsModel implements StatusReporter {

    private static final DefaultStatusMessage NO_LOCATION_ERROR =
        new DefaultStatusMessage(MessageType.ERROR, "Please specify a path");

    private static final DefaultStatusMessage TRAILING_OR_LEADING_WHITESPACE_ERROR = new DefaultStatusMessage(
        MessageType.ERROR, "The path contains leading and/or trailing whitespaces, please remove them");

    private final FileSystemConfiguration<FSLocation> m_fsConfig;

    private final String m_configName;

    private final SettingsModelFilterMode m_filterModeModel;

    private String[] m_fileExtensions;

    private boolean m_loading = false;

    /**
     * Constructor.
     *
     * @param configName under which to store the settings
     * @param portsConfig {@link PortsConfiguration} of the corresponding KNIME node
     * @param fileSystemPortIdentifier identifier of the file system port group in <b>portsConfig</b>
     * @param defaultFilterMode the default {@link FilterMode}
     * @param convenienceFS the {@link Set} of {@link FSCategory convenience file systems} that should be available if
     *            no file system port is present
     * @param fileExtensions the supported file extensions
     */
    protected AbstractSettingsModelFileChooser(final String configName, final PortsConfiguration portsConfig,
        final String fileSystemPortIdentifier, final FilterMode defaultFilterMode, final Set<FSCategory> convenienceFS,
        final String... fileExtensions) {
        m_fsConfig = FileSystemChooserUtils.createConfig(portsConfig, fileSystemPortIdentifier,
            FSLocationHandler.INSTANCE, convenienceFS);
        m_fsConfig.addChangeListener(e -> notifyChangeListeners());
        m_configName = configName;
        m_fileExtensions =
            CheckUtils.checkArgumentNotNull(fileExtensions, "The fileExtensions may be empty but never null.").clone();
        m_filterModeModel = new SettingsModelFilterMode("filter_mode", defaultFilterMode);
        m_filterModeModel.addChangeListener(e -> notifyChangeListeners());
        addChangeListener(e -> updateFilterMode());
    }

    @Override
    public final void addChangeListener(final ChangeListener l) {
        // overwritten to finalize the method (otherwise Sonar will complain if we use it in the constructor)
        super.addChangeListener(l);
    }

    /**
     * Copy constructor for use in {@link #createClone()}.
     *
     * @param toCopy instance to copy
     */
    protected AbstractSettingsModelFileChooser(final AbstractSettingsModelFileChooser<T> toCopy) {
        m_configName = toCopy.m_configName;
        m_fsConfig = toCopy.m_fsConfig.copy();
        m_fileExtensions = toCopy.m_fileExtensions.clone();
        m_filterModeModel = toCopy.m_filterModeModel.createClone();
    }

    @Override
    protected void notifyChangeListeners() {
        if (!m_loading) {
            super.notifyChangeListeners();
        }
    }

    private void updateFilterMode() {
        final FSCategory fsCategory = m_fsConfig.getFSCategory();
        if (fsCategory == FSCategory.CUSTOM_URL) {
            m_filterModeModel.setFilterMode(FilterMode.FILE);
        }
        m_filterModeModel.setEnabled(fsCategory != FSCategory.CUSTOM_URL && isEnabled());
    }

    /**
     * Configures the settings model in the {@link NodeModel} and validates the input specs and settings.</br>
     * The statusMessageConsumer is used to communicate warning and info messages. In most cases you can use
     * {@link NodeModelStatusConsumer}.
     *
     * @param specs input specs of the node
     * @param statusMessageConsumer for communicating status messages
     * @throws InvalidSettingsException if the settings are invalid or incompatible with <b>specs</b>
     * @see NodeModelStatusConsumer
     */
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer)
        throws InvalidSettingsException {
        checkLocation();
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
     * Returns the keys corresponding to the location configuration within the settings model. If the settings model is
     * not located at the top most level of the settings, you will have to prefix the returned array with the path to
     * the settings model within your settings structure.</br>
     * Use this method when creating the {@link FlowVariableModel} for the location in the node dialog.
     *
     * @return the keys corresponding to the location configuration
     */
    public String[] getKeysForFSLocation() {
        return new String[]{m_configName, FSLocationHandler.CFG_PATH};
    }

    void setLocationFlowVariableModel(final FlowVariableModel locationFvm) {
        m_fsConfig.setLocationFlowVariableModel(locationFvm);
    }

    FileSystemConfiguration<FSLocation> getFileSystemConfiguration() {
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

    /**
     * Returns the {@link SettingsModelFilterMode}.
     *
     * @return the {@link SettingsModelFilterMode}
     */
    public SettingsModelFilterMode getFilterModeModel() {
        return m_filterModeModel;
    }

    /**
     * Convenience getter for the {@link FilterMode}.
     *
     * @return the {@link FilterMode}
     */
    public FilterMode getFilterMode() {
        return m_filterModeModel.getFilterMode();
    }

    /**
     * Returns the {@link FSConnection} or throws an IllegalStateException if the connection can not be retrieved.
     *
     * @return the selected FSConnection or {@link Optional#empty()} if the file system isn't connected
     * @see #canCreateConnection()
     */
    public FSConnection getConnection() {
        final Optional<FSConnection> connection = m_fsConfig.getConnection();
        return FileSystemHelper.retrieveFSConnection(connection, getLocation())
            .orElseThrow(() -> new IllegalStateException("Can't retrieve connection."));
    }

    /**
     * Returns <code>true</code> if the {@link FSConnection} can be created otherwise <code>false</code>.
     * @return <code>true</code> if the FSConnection can be created
     * @see #getConnection()
     */
    public boolean canCreateConnection() {
        return FileSystemHelper.canRetrieveFSConnection(m_fsConfig.getConnection(), getLocation());
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
     * Sets the given <b>path</b> without modifying the file system.
     *
     * @param path to set
     */
    public void setPath(final String path) {
        final FSLocation location = m_fsConfig.getLocationSpec();
        if (!location.getPath().equals(path)) {
            m_fsConfig.setLocationSpec(
                new FSLocation(location.getFSCategory(), location.getFileSystemSpecifier().orElse(null), path));
        }
    }

    /**
     * Returns the currently configured {@link FSLocation}.
     *
     * @return the currently configured {@link FSLocation}
     */
    public FSLocation getLocation() {
        return m_fsConfig.getLocationSpec();
    }

    /**
     * Returns {@code true} if accessing the file at the {@link FSLocation} returned by {@link #getLocation()} has a
     * chance at success.
     *
     * @return {@code true} if accessing the file at the {@link FSLocation} returned by {@link #getLocation()} has a
     *         chance at success
     */
    public boolean isLocationValid() {
        return m_fsConfig.isLocationSpecValid();
    }

    /**
     * Returns {@code true} if the location is controlled / overwritten by a {@link FlowVariable} and {@code false}
     * otherwise.
     *
     * @return {@code true} if the location is controlled / overwritten by a {@link FlowVariable}
     */
    public boolean isOverwrittenByFlowVariable() {
        return m_fsConfig.isLocationOverwrittenByVar();
    }

    @Override
    protected String getConfigName() {
        return m_configName;
    }

    @Override
    protected final void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_loading = true;
        final NodeSettingsRO topLevel = SettingsUtils.getOrEmpty(settings, m_configName);
        m_fsConfig.loadSettingsForDialog(topLevel, specs);
        m_filterModeModel.loadSettingsForDialog(topLevel, specs);
        loadAdditionalSettingsForDialog(topLevel, specs);
        m_loading = false;
        notifyChangeListeners();
    }

    boolean isLoading() {
        return m_loading;
    }

    boolean isLocationOverwrittenByFlowVariable() {
        return m_fsConfig.isLocationOverwrittenByVar();
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

    @Override
    protected final void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        final NodeSettingsWO topLevel = settings.addNodeSettings(m_configName);
        m_fsConfig.saveSettingsForDialog(topLevel);
        m_filterModeModel.saveSettingsTo(topLevel);
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

    void validateConfig() throws InvalidSettingsException {
        m_fsConfig.validate();
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

    /**
     * Loads the {@link FSLocation} from the provided {@link NodeSettingsRO} <b>without</b> changing the state of this
     * instance.
     *
     * @param settings the settings to load the {@link FSLocation} from
     * @return the {@link FSLocation} stored in {@link NodeSettingsRO settings}
     * @throws InvalidSettingsException if the settings don't contain the subsettings corresponding to this
     *             SettingsModel or the location can't be loaded
     */
    public FSLocation extractLocation(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO topLevel = settings.getNodeSettings(m_configName);
        return FSLocationHandler.INSTANCE.load(topLevel);
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
        m_fsConfig.saveSettingsForModel(topLevel);
        m_filterModeModel.saveSettingsTo(topLevel);
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
        final String path = getLocation().getPath();
        if (path.length() == 0) {
            messageConsumer.accept(NO_LOCATION_ERROR);
        }
        if (!path.equals(path.trim())) {
            messageConsumer.accept(TRAILING_OR_LEADING_WHITESPACE_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public abstract T createClone();
}
