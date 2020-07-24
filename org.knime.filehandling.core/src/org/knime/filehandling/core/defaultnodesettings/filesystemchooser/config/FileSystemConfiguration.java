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
 *   May 6, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.DeepCopy;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.FileSystemBrowser;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationFactory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.FileSystemChooser;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.defaultnodesettings.status.StatusReporter;
import org.knime.filehandling.core.util.CheckNodeContextUtil;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * The configuration of {@link FileSystemChooser}.</br>
 * It handles the top-level implementation of loading/saving/validating and delegates to the individual
 * {@link FileSystemSpecificConfig FileSystemSpecificConfigs} to deal with the specifics of each file system.</br>
 * Note that it stores the state of all {@link FileSystemSpecificConfig FileSystemSpecificConfigs} in an internal
 * settings object that is not displayed in the flow variable tab on a KNIME node dialog.</br>
 * It also stores the currently selected file system as well as its specifier (if any). In case of convenience file
 * systems (e.g. mountpoint) these settings are displayed in the flow variable tab and can thus be overwritten. For
 * connected file systems, it isn't possible to overwrite the settings with flow variables.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <L> the type of {@link FSLocationSpec} used
 */
public final class FileSystemConfiguration<L extends FSLocationSpec>
    implements DeepCopy<FileSystemConfiguration<L>>, StatusReporter {

    private static final String CFG_CONVENIENCE_FS_CATEGORY = "convenience_fs_category";

    private static final DefaultStatusMessage UNCONNECTED_FLOW_VAR_ERROR = new DefaultStatusMessage(MessageType.ERROR,
        "The selected flow variable references a connected file system. Please add the missing file system "
            + "connection port.");

    private static final String CFG_HAS_FS_PORT = "has_fs_port";

    private static final String CFG_OVERWRITTEN_BY_VAR = "overwritten_by_variable";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileSystemConfiguration.class);

    private static final String CFG_FILE_SYSTEM_CHOOSER_INTERNALS =
        "file_system_chooser_" + SettingsModel.CFGKEY_INTERNAL;

    private final EnumMap<FSCategory, FileSystemSpecificConfig> m_fsSpecificConfigs = new EnumMap<>(FSCategory.class);

    private final List<ChangeListener> m_listeners = new LinkedList<>();

    private final ChangeEvent m_changeEvent = new ChangeEvent(this);

    private final int m_portIdx;

    private L m_locationSpec = null;

    private final FSLocationSpecHandler<L> m_locationSpecHandler;

    private FlowVariableModel m_locationFvm;

    private ChangeListener m_fvmListener;

    /**
     * Stores which convenience FS was selected when no fs port is connected so that we can reconstruct the state if we
     * add and then remove the fs port or select and unselect an incompatible flow variable.
     */
    private FSCategory m_convenienceFSCategory = FSCategory.LOCAL;

    private boolean m_settingsStoredWithFSPort;

    private boolean m_overwrittenByVariable = false;

    // flag indicating that we are currently loading the settings i.e. no notification should be issued
    private boolean m_loading = false;

    /**
     * Constructor to be used in the case where there is no file system port.</br>
     * The first file system represented by the first config is used as default.</br>
     * The {@link ConnectedFileSystemSpecificConfig} is not supported in this constructor.</br>
     * The created configuration can be overwritten using the flow variable tab of the corresponding node dialog.
     *
     * @param locationSpecHandler {@link FSLocationSpecHandler} used to handle {@link FSLocationSpec} instances
     * @param configs the {@link FileSystemSpecificConfig configs} for the default file systems to choose from (must not
     *            contain {@link ConnectedFileSystemSpecificConfig})
     */
    public FileSystemConfiguration(final FSLocationSpecHandler<L> locationSpecHandler,
        final FileSystemSpecificConfig... configs) {
        m_portIdx = Arrays.stream(configs)//
            .filter(c -> c instanceof ConnectedFileSystemSpecificConfig)//
            .map(c -> (ConnectedFileSystemSpecificConfig)c)//
            .filter(FileSystemSpecificConfig::isActive)//
            .mapToInt(ConnectedFileSystemSpecificConfig::getPortIdx)//
            .findFirst().orElse(-1);

        m_locationSpecHandler = locationSpecHandler;

        for (FileSystemSpecificConfig config : configs) {
            final FSCategory category = config.getLocationSpec().getFSCategory();
            CheckUtils.checkArgument(!m_fsSpecificConfigs.containsKey(category),
                "Duplicate config for file system category '%s' detected.", category);
            m_fsSpecificConfigs.put(category, config);
            config.addChangeListener(this::handleSpecificConfigChange);
        }
        m_settingsStoredWithFSPort = m_portIdx > -1;
        setLocationSpec(Arrays.stream(configs)//
            .filter(FileSystemSpecificConfig::isActive)//
            .findFirst()//
            .orElseThrow(
                () -> new IllegalArgumentException("At least one of the file system specific configs must be active."))
            .getLocationSpec());
    }

    /**
     * Sets the {@link FlowVariableModel} that controls the locationSpec.
     *
     * @param locationFvm controlling the locationSpec
     */
    public synchronized void setLocationFlowVariableModel(final FlowVariableModel locationFvm) {
        if (m_locationFvm != null) {
            m_locationFvm.removeChangeListener(m_fvmListener);
        }
        m_locationFvm = locationFvm;
        m_fvmListener = e -> handleLocationFvmChange();
        m_locationFvm.addChangeListener(m_fvmListener);
    }

    private void handleLocationFvmChange() {
        if (m_locationFvm.isVariableReplacementEnabled()) {
            final Optional<FlowVariable> var = m_locationFvm.getVariableValue();
            if (var.isPresent()) {
                overwriteWithFlowVariable(var.get().getValue(m_locationSpecHandler.getVariableType()));
            }
        } else {
            disableFlowVariable();
        }
    }

    /**
     * Overwrites the locationSpec with the provided {@link FSLocationSpec} from a flow variable and sets the
     * overwritten flag.
     *
     * @param flowVarLocationSpec {@link FSLocationSpec} from a flow variable
     */
    private void overwriteWithFlowVariable(final L flowVarLocationSpec) {
        final boolean notifyListeners =
            !m_overwrittenByVariable || !Objects.equals(m_locationSpec, flowVarLocationSpec);
        m_overwrittenByVariable = true;
        setLocationSpecInternal(flowVarLocationSpec);
        if (notifyListeners) {
            notifyChangeListeners();
        }
    }

    /**
     * Lets the config know that it is no longer controlled by flow variable. </br>
     * If the config has no fs port and the flow variable was invalid, this method also switches back to the last valid
     * file system.
     */
    private void disableFlowVariable() {
        if (m_overwrittenByVariable) {
            m_overwrittenByVariable = false;
            if (!hasFSPort() && !getCurrentSpecificConfig().isActive()) {
                m_locationSpec = m_locationSpecHandler.adapt(m_locationSpec,
                    m_fsSpecificConfigs.get(m_convenienceFSCategory).getLocationSpec());
            }
            notifyChangeListeners();
        }
    }

    private void handleSpecificConfigChange(final ChangeEvent e) {
        final FileSystemSpecificConfig config = (FileSystemSpecificConfig)e.getSource();
        if (config.isActive() && config == getCurrentSpecificConfig()) {
            // update location config if the currently selected fs config changed
            m_locationSpec = m_locationSpecHandler.adapt(m_locationSpec, config.getLocationSpec());
        }
    }

    /**
     * Copy constructor. NOTE: Does not copy the listeners or the flow variable model.
     *
     * @param toCopy the instance to copy
     */
    private FileSystemConfiguration(final FileSystemConfiguration<L> toCopy) {
        m_portIdx = toCopy.m_portIdx;
        m_locationSpec = toCopy.m_locationSpec;
        m_locationSpecHandler = toCopy.m_locationSpecHandler;
        toCopy.m_fsSpecificConfigs.entrySet().forEach(e -> m_fsSpecificConfigs.put(e.getKey(), e.getValue().copy()));
        m_settingsStoredWithFSPort = toCopy.m_settingsStoredWithFSPort;
        m_overwrittenByVariable = toCopy.m_overwrittenByVariable;
        m_convenienceFSCategory = toCopy.m_convenienceFSCategory;
    }

    /**
     * Creates a {@link FSLocationFactory} that can be used to create {@link FSLocation} objects from string paths.
     *
     * @return a {@link FSLocationFactory} corresponding to the current configuration
     */
    public FSLocationFactory createLocationFactory() {
        return new FSLocationFactory(getLocationSpec(), getConnection());
    }

    /**
     * Returns an {@link Optional} containing the FSConnection if used with a file system provided via input port or
     * {@link Optional#empty()} in case the file system isn't connected or a convenience file system is selected.
     *
     * @return an {@link Optional} containing the {@link FSConnection} or empty if not connected or a convenience file
     *         system is selected
     */
    public Optional<FSConnection> getConnection() {
        if (hasFSPort()) {
            // in the connected case we always have to take the connection from the connected FS config
            // because the input file system always takes precedence but the location might be overwritten
            // with a flow variable pointing to a different fs
            return getConnectedConfig().getConnection();
        } else {
            return getCurrentSpecificConfig().getConnection();
        }
    }

    /**
     * Returns the supported file selection modes of the currently selected file system.
     *
     * @return the supported file selection modes of the currently selected file system
     * @throws IllegalStateException if the current configuration is invalid
     */
    public Set<FileSystemBrowser.FileSelectionMode> getSupportedFileSelectionModes() {
        return getCurrentSpecificConfig().getSupportedFileSelectionModes();
    }

    /**
     * Retrieves the {@link FSLocationSpec} corresponding to the current configuration.
     *
     * @return the {@link FSLocationSpec}
     */
    public L getLocationSpec() {
        if (hasFSPort()) {
            // in the connected case we always use the file system provided via the input port
            return m_locationSpecHandler.adapt(m_locationSpec, getConnectedConfig().getLocationSpec());
        }
        return m_locationSpec;
    }

    /**
     * Evaluates if the spec returned by {@link #getLocationSpec()} is valid. </br>
     * Valid in this case means whether attempting to access the object represented by the spec has a chance at being
     * successful.
     *
     * @return {@code true} if the spec returned by {@link #getLocationSpec()} is valid
     */
    public boolean isLocationSpecValid() {
        return hasFSPort() || getCurrentSpecificConfig().isActive();
    }

    private FileSystemSpecificConfig getConnectedConfig() {
        return m_fsSpecificConfigs.get(FSCategory.CONNECTED);
    }

    /**
     * Sets the provided {@link FSLocationSpec} and notifies change listeners if this changed the configuration.
     *
     * @param spec the {@link FSLocationSpec} to set
     */
    public void setLocationSpec(final FSLocationSpec spec) {
        if (!Objects.equals(m_locationSpec, spec)) {
            setLocationSpecInternal(spec);
            notifyChangeListeners();
        }
    }

    private void setLocationSpecInternal(final FSLocationSpec spec) {
        m_locationSpec = m_locationSpecHandler.adapt(m_locationSpec, spec);
        if (!hasFSPort() && spec.getFSCategory() != FSCategory.CONNECTED) {
            m_convenienceFSCategory = spec.getFSCategory();
        }
    }

    /**
     * NOTE: Listeners are not copied!
     */
    @Override
    public FileSystemConfiguration<L> copy() {
        return new FileSystemConfiguration<>(this);
    }

    /**
     * Adds the provided {@link ChangeListener listener} to the list of listeners.
     *
     * @param listener to add
     */
    public void addChangeListener(final ChangeListener listener) {
        m_listeners.add(listener);
    }

    private void notifyChangeListeners() {
        if (!m_loading) {
            for (ChangeListener listener : m_listeners) {
                listener.stateChanged(m_changeEvent);
            }
        }
    }

    /**
     * Returns {@code true} if we are dealing with a file system provided via node input.
     *
     * @return {@code true} if the configuration deals with a connected file system (via a node input port)
     */
    public boolean hasFSPort() {
        return m_portIdx != -1;
    }

    private void updateConnectedConfigWithSpec(final PortObjectSpec[] specs,
        final Consumer<StatusMessage> statusMessageConsumer) throws InvalidSettingsException {
        FileSystemSpecificConfig connectedConfig = getConnectedConfig();
        connectedConfig.configureInModel(specs, statusMessageConsumer);
    }

    private void failIfWorkflowRelativeInComponentProject() throws InvalidSettingsException {
        failIfWorkflowRelativeInComponentProject(m_locationSpec);

    }

    private static void failIfWorkflowRelativeInComponentProject(final FSLocationSpec locationSpec)
        throws InvalidSettingsException {
        if (locationSpec.getFSCategory() == FSCategory.RELATIVE) {
            final RelativeTo specifier = RelativeTo.fromSettingsValue(locationSpec.getFileSystemSpecifier()
                .orElseThrow(() -> new IllegalStateException("No relative option provided.")));
            CheckUtils.checkSetting(specifier == RelativeTo.MOUNTPOINT || !CheckNodeContextUtil.isInComponentProject(),
                "Nodes in a shared component don't have access to workflow-relative locations");
        }
    }

    private FileSystemSpecificConfig getCurrentSpecificConfig() {
        return getSpecificConfig(getFSCategory());
    }

    private FileSystemSpecificConfig getSpecificConfig(final FSCategory category) {
        return m_fsSpecificConfigs.get(category);
    }

    /**
     * Retrieves the current {@link FSCategory} of file system.
     *
     * @return the current {@link FSCategory} of file system
     */
    public FSCategory getFSCategory() {
        return m_locationSpec.getFSCategory();
    }

    /**
     * Sets the provided {@link FSCategory} and notifies the listeners if the value changed.</br>
     *
     * NOTE: This method is intended for the use in the dialog and will fail if the provided {@link FSCategory category}
     * is not supported by this instance.
     *
     * @param category the {@link FSCategory} to set
     * @throws IllegalArgumentException if the provided {@link FSCategory} is not supported by this instance
     */
    public void setFSCategory(final FSCategory category) {
        if (category != getFSCategory()) {
            setLocationSpec(getSpecificConfig(category).getLocationSpec());
        }
    }

    /**
     * Loads the settings in the node dialog. Defaults are set if some settings are missing. Also notifies all change
     * listeners after the settings are loaded.
     *
     * @param settings to load from
     * @param specs input {@link PortObjectSpec specs} of the node
     * @throws NotConfigurableException currently not thrown
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_loading = true;
        loadLocationSpec(settings);
        final NodeSettingsRO internalSettings = SettingsUtils.getOrEmpty(settings, CFG_FILE_SYSTEM_CHOOSER_INTERNALS);
        m_settingsStoredWithFSPort = internalSettings.getBoolean(CFG_HAS_FS_PORT, false);

        loadFSSpecificConfigsInDialog(specs, internalSettings);
        // This setting was introduced with 4.2.1. In older workflows we fail during validate and thus reset the node settings
        // therefore it is correct to set the overwritten flag to false
        m_overwrittenByVariable = internalSettings.getBoolean(CFG_OVERWRITTEN_BY_VAR, false);

        restoreConvenienceFSIfPortRemoved(internalSettings);
        if (hasFSPort()) {
            // the location spec doesn't know if the connection changed, so we need to tell it
            setLocationSpec(m_fsSpecificConfigs.get(FSCategory.CONNECTED).getLocationSpec());
        }
        if (m_locationFvm != null) {
            handleLocationFvmChange();
        }
        getCurrentSpecificConfig().updateSpecifier(m_locationSpec);
        m_loading = false;
        notifyChangeListeners();
    }

    private void loadFSSpecificConfigsInDialog(final PortObjectSpec[] specs, final NodeSettingsRO internalSettings)
        throws NotConfigurableException {
        for (FileSystemSpecificConfig config : m_fsSpecificConfigs.values()) {
            if (config.isActive()) {
                config.loadInDialog(internalSettings, specs);
            }
        }
    }

    private void loadLocationSpec(final NodeSettingsRO settings) {
        try {
            m_locationSpec = m_locationSpecHandler.load(settings);
        } catch (InvalidSettingsException ise) {
            // keep the old value
            LOGGER.debug("Couldn't load the locationSpec, keeping the old value.", ise);
        }
    }

    /**
     * Loads the configuration in {@link NodeSettingsRO settings} and fails if they are invalid.</br>
     * NOTE: Unlike {@link FileSystemConfiguration#loadSettingsForDialog(NodeSettingsRO, PortObjectSpec[])} this method
     * does not load the settings related to the state of the individual file system dialogs but instead only the
     * {@link FSLocationSpec}.
     *
     * @param settings to load the configuration from
     * @throws InvalidSettingsException if the configuration stored in {@link NodeSettingsRO settings} is invalid
     */
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_loading = true;
        m_locationSpec = m_locationSpecHandler.load(settings);
        final NodeSettingsRO internalSettings = settings.getNodeSettings(CFG_FILE_SYSTEM_CHOOSER_INTERNALS);
        m_settingsStoredWithFSPort = internalSettings.getBoolean(CFG_HAS_FS_PORT);
        // The field was introduced with 4.2.1, so workflows created with 4.2.0 will default to false
        m_overwrittenByVariable = internalSettings.getBoolean(CFG_OVERWRITTEN_BY_VAR, false);

        loadFSSpecificConfigsInModel(internalSettings);
        restoreConvenienceFSIfPortRemoved(internalSettings);
        if (fsPortAdded() && !isLocationOverwrittenByVar()) {
            setLocationSpec(getConnectedConfig().getLocationSpec());
        }
        getCurrentSpecificConfig().updateSpecifier(m_locationSpec);
        m_loading = false;
        notifyChangeListeners();
    }

    private void loadFSSpecificConfigsInModel(final NodeSettingsRO internalSettings) throws InvalidSettingsException {
        for (FileSystemSpecificConfig config : m_fsSpecificConfigs.values()) {
            if (config.isActive()) {
                config.loadInModel(internalSettings);
            }
        }
    }

    /**
     * Updates the currently selected config with the provided <b>specs</b> in the NodeModel.</br>
     * To be called in the {@code configure} method of the NodeModel.
     *
     * @param specs the input specs of the node
     * @param warningConsumer consumer for status messages e.g. for warnings
     * @throws InvalidSettingsException if the specs aren't compatible with the configuration
     */
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> warningConsumer)
        throws InvalidSettingsException {
        if (hasFSPort()) {
            // we need to update the config with the incoming fs spec
            updateConnectedConfigWithSpec(specs, warningConsumer);
        } else {
            final FileSystemSpecificConfig current = getCurrentSpecificConfig();
            if (current.isActive()) {
                current.configureInModel(specs, warningConsumer);
            }
            failIfWorkflowRelativeInComponentProject();
        }

        final PriorityStatusConsumer prioConsumer = new PriorityStatusConsumer();

        report(m -> {
            warningConsumer.accept(m);
            prioConsumer.accept(m);
        });

        Optional<StatusMessage> highestPrioMsg = prioConsumer.get();

        if (highestPrioMsg.isPresent() && highestPrioMsg.get().getType() == MessageType.ERROR) {
            throw new InvalidSettingsException(highestPrioMsg.get().getMessage());
        }

    }

    @Override
    public void report(final Consumer<StatusMessage> statusConsumer) {
        if (isFlowVarIncompatible()) {
            if (hasFSPort()) {
                statusConsumer.accept(m_locationSpecHandler.warnIfConnectedOverwrittenWithFlowVariable(m_locationSpec));
            } else {
                statusConsumer.accept(UNCONNECTED_FLOW_VAR_ERROR);
            }
        } else if (connectedFSDiffers()) {
            statusConsumer.accept(m_locationSpecHandler.warnIfConnectedOverwrittenWithFlowVariable(m_locationSpec));
        } else {
            // nothing to report
        }

        final FileSystemSpecificConfig config = getCurrentSpecificConfig();
        if (config.isActive()) {
            config.report(statusConsumer);
        }
    }

    private boolean connectedFSDiffers() {
        if (isLocationOverwrittenByVar() && hasFSPort()) {
            return !FSLocationSpec.areEqual(m_locationSpec, getConnectedConfig().getLocationSpec());
        }
        return false;
    }

    private boolean isFlowVarIncompatible() {
        return isLocationOverwrittenByVar() && !getCurrentSpecificConfig().isActive();
    }

    /**
     * Indicates whether the location is overwritten by a flow variable.
     *
     * @return {@code true} if the location is overwritten by a flow variable
     */
    public boolean isLocationOverwrittenByVar() {
        return m_overwrittenByVariable;
    }

    private boolean fsPortRemoved() {
        return m_settingsStoredWithFSPort && !hasFSPort();
    }

    private boolean fsPortAdded() {
        return !m_settingsStoredWithFSPort && hasFSPort();
    }

    /**
     * Retrieves the {@link FileSystemSpecificConfig} for the provided {@link FSCategory category}.
     *
     * @param category to retrieve the {@link FileSystemSpecificConfig} for
     * @return the {@link FileSystemSpecificConfig} corresponding to the given {@link FSCategory category}
     * @throws IllegalArgumentException if there is no {@link FileSystemSpecificConfig} associated with the provided
     *             {@link FSCategory category}
     */
    public FileSystemSpecificConfig getFileSystemSpecifcConfig(final FSCategory category) {
        return CheckUtils.checkArgumentNotNull(m_fsSpecificConfigs.get(category),
            "No config for category '%s' available.", category);
    }

    /**
     * Indicates whether there is a {@link FileSystemSpecificConfig} associated with the provided {@link FSCategory
     * category}.
     *
     * @param category to check if an associated FileSystemSpecificConfig is available for
     * @return {@code true} if there is a {@link FileSystemSpecificConfig} is available, {@code false} if not
     */
    public boolean hasFileSystemSpecificConfig(final FSCategory category) {
        return m_fsSpecificConfigs.containsKey(category);
    }

    /**
     * Loads which convenience fs was selected in the unconnected state (LOCAL if no value was stored) and updates the
     * location spec if the file system port was removed since the last load and the fs isn't overwritten with a flow
     * variable. IMPORTANT NOTE: must be called AFTER the fs specific configs have been loaded.
     *
     * @param internalSettings the settings object storing the internal settings
     */
    private void restoreConvenienceFSIfPortRemoved(final NodeSettingsRO internalSettings) {
        m_convenienceFSCategory =
            FSCategory.valueOf(internalSettings.getString(CFG_CONVENIENCE_FS_CATEGORY, FSCategory.LOCAL.name()));
        if (fsPortRemoved() && !isLocationOverwrittenByVar()) {
            m_locationSpec = m_locationSpecHandler.adapt(m_locationSpec,
                m_fsSpecificConfigs.get(m_convenienceFSCategory).getLocationSpec());
        }
    }

    /**
     * Validates the current state and saves it to {@link NodeSettingsWO settings}
     *
     * @param settings to write to
     * @throws InvalidSettingsException if the configuration is invalid
     */
    public final void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        validate();
        save(settings);
    }

    /**
     * Validates the current configuration state.
     *
     * @throws InvalidSettingsException if the configuration is invalid
     */
    public void validate() throws InvalidSettingsException {
        final ValidationConsumer validationConsumer = new ValidationConsumer();
        report(validationConsumer);
        validationConsumer.failIfNecessary();
    }

    private void save(final NodeSettingsWO fsSettings) {
        saveInternalSettings(fsSettings.addNodeSettings(CFG_FILE_SYSTEM_CHOOSER_INTERNALS));
        m_locationSpecHandler.save(fsSettings, m_locationSpec);
    }

    private void saveInternalSettings(final NodeSettingsWO internalSettings) {
        // flag that indicates if the settings were saved from a config with fs port
        // not used in the moment but crucial for AP-14457 because we then won't be able to distinguish
        // settings stored with an fs port from settings stored without one
        internalSettings.addBoolean(CFG_HAS_FS_PORT, hasFSPort());
        // Indicates whether the model is overwritten with a flow variable
        // Introduced in 4.2.1 in order to distinguish between incompatible locations due
        // to flow variables and incompatible locations due to addition/removal of the FS port
        internalSettings.addBoolean(CFG_OVERWRITTEN_BY_VAR, isLocationOverwrittenByVar());

        saveConvenienceFSCategory(internalSettings);

        for (FileSystemSpecificConfig config : m_fsSpecificConfigs.values()) {
            config.save(internalSettings);
        }
    }

    private void saveConvenienceFSCategory(final NodeSettingsWO internalSettings) {
        if (!hasFSPort()) {
            m_convenienceFSCategory = m_locationSpec.getFSCategory();
        }
        internalSettings.addString(CFG_CONVENIENCE_FS_CATEGORY, m_convenienceFSCategory.name());
    }

    /**
     * Validates the configuration stored in {@link NodeSettingsRO settings} without overwriting the values in this
     * configuration.
     *
     * @param settings the {@link NodeSettingsRO} containing the configuration to validate
     * @throws InvalidSettingsException if the configuration in {@link NodeSettingsRO settings} is invalid
     */
    public void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        validateInternalSettings(settings.getNodeSettings(CFG_FILE_SYSTEM_CHOOSER_INTERNALS));
        // validates the location spec e.g. ensures that a path is present if we are in a file chooser
        final FSLocationSpec locationSpec = m_locationSpecHandler.load(settings);
        // will be null if we have a fs port and overwrite with a flow variable that specifies a different fs
        final FileSystemSpecificConfig selected = m_fsSpecificConfigs.get(locationSpec.getFSCategory());
        if (selected != null && selected.isActive()) {
            selected.validate(locationSpec);
        } else {
            // the settings have been overwritten by a flow variable of an incompatible fs type
            // or we added/removed the file system port and load old settings
            // ideally we would warn the user here but the settings model API doesn't allow it
            // therefore the warning will be issued in #configureInModel
        }
    }

    private void validateInternalSettings(final NodeSettingsRO internalSettings) throws InvalidSettingsException {
        internalSettings.getBoolean(CFG_HAS_FS_PORT);

        for (FileSystemSpecificConfig config : m_fsSpecificConfigs.values()) {
            try {
                config.validateInModel(internalSettings);
            } catch (InvalidSettingsException ise) {
                if (config.isActive()) {
                    throw ise;
                } else {
                    LOGGER.debug(String.format("Validating settings for config of type %s failed.",
                        config.getClass().getSimpleName()), ise);
                }
            }
        }
    }

    /**
     * Saves the configuration to {@link NodeSettingsWO settings} without validating them first. To be used in the node
     * model.
     *
     * @param settings to save the configuration to
     */
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        save(settings);
    }

    private static class ValidationConsumer implements Consumer<StatusMessage> {

        private final List<StatusMessage> m_msgs = new LinkedList<>();

        @Override
        public void accept(final StatusMessage t) {
            if (t.getType() == StatusMessage.MessageType.ERROR) {
                m_msgs.add(t);
            }
        }

        public void failIfNecessary() throws InvalidSettingsException {
            if (m_msgs.isEmpty()) {
                return;
            }
            final String message = m_msgs.stream()//
                .map(StatusMessage::getMessage)//
                .collect(joining("\n"));
            throw new InvalidSettingsException(message);
        }

    }

}
