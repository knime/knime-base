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
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.DeepCopy;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationFactory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.data.location.internal.FSLocationUtils;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.FileSystemChooser;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.status.StatusMessage.MessageType;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.status.StatusReporter;

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
 */
public final class FileSystemConfiguration implements DeepCopy<FileSystemConfiguration>, StatusReporter {

    private static final String CFG_FILE_SYSTEM_CHOOSER_INTERNALS =
        "file_system_chooser_" + SettingsModel.CFGKEY_INTERNAL;

    /**
     * Config key for the file system (saved as {@link FSLocationSpec})
     */
    public static final String CFG_LOCATION_SPEC = "location_spec";

    private final String m_locationCfgKey;

    private final EnumMap<Choice, FileSystemSpecificConfig> m_fsSpecificConfigs = new EnumMap<>(Choice.class);

    private final List<ChangeListener> m_listeners = new LinkedList<>();

    private final ChangeEvent m_changeEvent = new ChangeEvent(this);

    private final int m_portIdx;

    private FSLocationSpec m_locationSpec;

    /**
     * Constructor to be used in the case where there is no file system port.</br>
     * The first file system represented by the first config is used as default.</br>
     * The {@link ConnectedFileSystemSpecificConfig} is not supported in this constructor.</br>
     * The created configuration can be overwritten using the flow variable tab of the corresponding node dialog.
     *
     * @param configs the {@link FileSystemSpecificConfig configs} for the default file systems to choose from (must not
     *            contain {@link ConnectedFileSystemSpecificConfig})
     */
    public FileSystemConfiguration(final FileSystemSpecificConfig... configs) {
        this(-1, configs);
        CheckUtils.checkArgument(Arrays.stream(configs).noneMatch(c -> c instanceof ConnectedFileSystemSpecificConfig),
            "Connected file system config detected even though there is no file system port.");
    }

    /**
     * Constructor to be used in the case where there is a file system input port.</br>
     * Configurations created by this constructor cannot be overwritten using flow variables.
     *
     * @param config an instance of {@link ConnectedFileSystemSpecificConfig}
     */
    public FileSystemConfiguration(final ConnectedFileSystemSpecificConfig config) {
        this(config.getPortIdx(), config);
    }

    private FileSystemConfiguration(final int portIdx, final FileSystemSpecificConfig... configs) {
        m_portIdx = portIdx;
        // by appending the internal key, we can hide settings in the flow variable tab
        m_locationCfgKey = m_portIdx >= 0 ? CFG_LOCATION_SPEC + SettingsModel.CFGKEY_INTERNAL : CFG_LOCATION_SPEC;
        m_locationSpec = configs[0].getLocationSpec();
        for (FileSystemSpecificConfig config : configs) {
            final Choice choice = config.getLocationSpec().getFileSystemChoice();
            CheckUtils.checkArgument(!m_fsSpecificConfigs.containsKey(choice),
                "Duplicate config for choice '%s' detected.", choice);
            m_fsSpecificConfigs.put(choice, config);
            config.addChangeListener(e -> handleSpecificConfigChange());
        }
    }

    private void handleSpecificConfigChange() {
        getCurrentSpecificConfig().ifPresent(c -> setLocationSpec(c.getLocationSpec()));
        // always notify the listeners because the above call does not necessarily fire a change event
        notifyChangeListeners();
    }

    /**
     * Copy constructor. NOTE: Does not copy the listeners.
     *
     * @param toCopy the instance to copy
     */
    private FileSystemConfiguration(final FileSystemConfiguration toCopy) {
        m_portIdx = toCopy.m_portIdx;
        m_locationCfgKey = toCopy.m_locationCfgKey;
        m_locationSpec = toCopy.m_locationSpec;
        toCopy.m_fsSpecificConfigs.entrySet().forEach(e -> m_fsSpecificConfigs.put(e.getKey(), e.getValue().copy()));
    }

    /**
     * Creates a {@link FSLocationFactory} that can be used to create {@link FSLocation} objects from string paths.
     *
     * @return a {@link FSLocationFactory} corresponding to the current configuration
     */
    public FSLocationFactory createLocationFactory() {
        return new FSLocationFactory(getLocationSpec(), getConnection());
    }

    private Optional<FSConnection> getConnection() {
        return getCurrentSpecificConfig().flatMap(FileSystemSpecificConfig::getConnection);
    }

    /**
     * Retrieves the {@link FSLocationSpec} corresponding to the current configuration.
     *
     * @return the {@link FSLocationSpec}
     */
    public FSLocationSpec getLocationSpec() {
        return m_locationSpec;
    }

    /**
     * Sets the provided {@link FSLocationSpec} and notifies change listeners if this changed the configuration.
     *
     * @param spec the {@link FSLocationSpec} to set
     */
    public void setLocationSpec(final FSLocationSpec spec) {
        CheckUtils.checkArgumentNotNull(spec, "The spec must not be null.");
        if (!FSLocationSpec.areEqual(getLocationSpec(), spec)) {
            m_locationSpec = spec;
            getCurrentSpecificConfig().ifPresent(c -> c.overwriteWith(spec));
            notifyChangeListeners();
        }
    }

    /**
     * NOTE: Listeners are not copied!
     */
    @Override
    public FileSystemConfiguration copy() {
        return new FileSystemConfiguration(this);
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
        m_listeners.forEach(l -> l.stateChanged(m_changeEvent));
    }

    /**
     * Returns {@code true} if we are dealing with a file system provided via node input.
     *
     * @return {@code true} if the configuration deals with a connected file system (via a node input port)
     */
    public boolean isConnectedFS() {
        return m_portIdx != -1;
    }

    /**
     * Updates the currently selected config with the provided <b>specs</b> in the NodeModel.</br>
     * To be called in the {@code configure} method of the NodeModel.
     *
     * @param specs the input specs of the node
     * @throws InvalidSettingsException if the specs aren't compatible with the configuration
     */
    public void configureInModel(final PortObjectSpec[] specs) throws InvalidSettingsException {
        final Optional<FileSystemSpecificConfig> current = getCurrentSpecificConfig();
        if (current.isPresent()) {
            current.get().configureInModel(specs);
        }
    }

    private Optional<FileSystemSpecificConfig> getCurrentSpecificConfig() {
        return getSpecificConfig(getChoice());
    }

    private Optional<FileSystemSpecificConfig> getSpecificConfig(final Choice choice) {
        return Optional.ofNullable(m_fsSpecificConfigs.get(choice));
    }

    /**
     * Retrieves the current {@link Choice} of file system.
     *
     * @return the current {@link Choice} of file system
     */
    public Choice getChoice() {
        return m_locationSpec.getFileSystemChoice();
    }

    /**
     * Sets the provided {@link Choice} and notifies the listeners if the value changed.</br>
     *
     * NOTE: This method is intended for the use in the dialog and will fail if the provided {@link Choice choice} is
     * not supported by this instance.
     *
     * @param choice the {@link Choice} to set
     * @throws IllegalArgumentException if the provided {@link Choice} is not supported by this instance
     */
    public void setChoice(final Choice choice) {
        if (choice != m_locationSpec.getFileSystemChoice()) {
            setLocationSpec(getSpecificConfig(choice)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file system choice: " + choice))
                .getLocationSpec());
            notifyChangeListeners();
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
        FSLocationSpec locationSpec = null;
        try {
            locationSpec = loadLocation(settings);
        } catch (InvalidSettingsException ise) {
            throw new IllegalStateException("Loading the FSLocationSpec failed.", ise);
        }
        loadInternalSettingsInDialog(getChildSettingsOrEmpty(settings, CFG_FILE_SYSTEM_CHOOSER_INTERNALS), specs);
        // sets the location spec AND updates the selected specific config IF the location spec corresponds to a config
        setLocationSpec(locationSpec);
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
        // the internal settings aren't loaded because they are irrelevant during execution in the model
        setLocationSpec(loadLocation(settings));
    }

    private FSLocationSpec loadLocation(final NodeSettingsRO settings) throws InvalidSettingsException {
        return FSLocationUtils.loadFSLocationSpec(settings.getNodeSettings(m_locationCfgKey));
    }

    /**
     * Retrieves the {@link FileSystemSpecificConfig} for the provided {@link Choice choice}.
     *
     * @param choice to retrieve the {@link FileSystemSpecificConfig} for
     * @return the {@link FileSystemSpecificConfig} corresponding to the given {@link Choice choice}
     * @throws IllegalArgumentException if there is no {@link FileSystemSpecificConfig} associated with the provided
     *             {@link Choice choice}
     */
    public FileSystemSpecificConfig getFileSystemSpecifcConfig(final Choice choice) {
        return CheckUtils.checkArgumentNotNull(m_fsSpecificConfigs.get(choice), "No config for choice '%s' available.",
            choice);
    }

    /**
     * Indicates whether there is a {@link FileSystemSpecificConfig} associated with the provided {@link Choice choice}.
     *
     * @param choice to check if an associated FileSystemSpecificConfig is available for
     * @return {@code true} if there is a {@link FileSystemSpecificConfig} is available, {@code false} if not
     */
    public boolean hasFileSystemSpecificConfig(final Choice choice) {
        return m_fsSpecificConfigs.containsKey(choice);
    }

    @Override
    public void report(final Consumer<StatusMessage> statusConsumer) {
        final Optional<FileSystemSpecificConfig> config = getCurrentSpecificConfig();
        if (config.isPresent()) {
            config.get().report(statusConsumer);
        } else {
            statusConsumer.accept(
                new DefaultStatusMessage(MessageType.ERROR, "The choice specified via flow variable is invalid."));
        }
    }

    private void loadInternalSettingsInDialog(final NodeSettingsRO internalSettings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        for (FileSystemSpecificConfig config : m_fsSpecificConfigs.values()) {
            config.loadInDialog(internalSettings, specs);
        }
    }

    private static NodeSettingsRO getChildSettingsOrEmpty(final NodeSettingsRO settings, final String childKey) {
        try {
            return settings.getNodeSettings(childKey);
        } catch (InvalidSettingsException ise) {
            // settings aren't present -> create an empty settings object with the provided key
            return new NodeSettings(childKey);
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
        for (FileSystemSpecificConfig config : m_fsSpecificConfigs.values()) {
            config.report(validationConsumer);
        }
        validationConsumer.failIfNecessary();
    }

    private void save(final NodeSettingsWO fsSettings) {
        saveInternalSettings(fsSettings.addNodeSettings(CFG_FILE_SYSTEM_CHOOSER_INTERNALS));
        FSLocationUtils.saveFSLocationSpec(getLocationSpec(), fsSettings.addNodeSettings(m_locationCfgKey));
    }

    private void saveInternalSettings(final NodeSettingsWO internalSettings) {
        for (FileSystemSpecificConfig config : m_fsSpecificConfigs.values()) {
            config.save(internalSettings);
        }
    }

    /**
     * Validates the configuration stored in {@link NodeSettingsRO settings} without overwriting the values in this
     * configuration.
     *
     * @param settings the {@link NodeSettingsRO} containing the configuration to validate
     * @throws InvalidSettingsException if the configuration in {@link NodeSettingsRO settings} is invalid
     */
    public void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (FileSystemSpecificConfig config : m_fsSpecificConfigs.values()) {
            config.validateInModel(settings.getNodeSettings(CFG_FILE_SYSTEM_CHOOSER_INTERNALS));
        }
        FSLocationSpec locationSpec = FSLocationUtils.loadFSLocationSpec(settings.getNodeSettings(CFG_LOCATION_SPEC));
        final FileSystemSpecificConfig selected = m_fsSpecificConfigs.get(locationSpec.getFileSystemChoice());
        CheckUtils.checkSettingNotNull(selected, "The specified file system '%s' is not supported by this config.",
            locationSpec);
        selected.validate(locationSpec);
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
