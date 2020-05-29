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
 *   Apr 24, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser;

import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationFactory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.DefaultFSLocationSpecConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.FileSystemConfiguration;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.status.StatusMessage;

/**
 * {@link SettingsModel} that stores information about a file system.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class SettingsModelFileSystem extends SettingsModel {

    private final String m_configName;

    private final FileSystemConfiguration<DefaultFSLocationSpecConfig> m_fsConfig;

    /**
     * Constructor.
     *
     * @param configName name under which the SettingsModel is stored
     * @param portsConfig the {@link PortsConfiguration} of the node
     * @param fileSystemPortIdentifier identifier of the file system port in {@link PortsConfiguration portsConfig}
     */
    public SettingsModelFileSystem(final String configName, final PortsConfiguration portsConfig,
        final String fileSystemPortIdentifier) {
        if (portsConfig.getInputPortLocation().get(fileSystemPortIdentifier) != null) {
            m_configName = configName + SettingsModel.CFGKEY_INTERNAL;
        } else {
            m_configName = configName;
        }
        m_fsConfig = FileSystemChooserUtils.createConfig(portsConfig, fileSystemPortIdentifier,
            new DefaultFSLocationSpecConfig());
        m_fsConfig.addChangeListener(e -> notifyChangeListeners());
    }

    /**
     * Copy constructor.
     *
     * @param toCopy the instance to copy
     */
    private SettingsModelFileSystem(final SettingsModelFileSystem toCopy) {
        m_configName = toCopy.m_configName;
        m_fsConfig = toCopy.m_fsConfig.copy();
    }

    String[] getKeysForFSLocation() {
        return new String[]{m_configName, DefaultFSLocationSpecConfig.CFG_LOCATION_SPEC};
    }

    /**
     * Retrieves the {@link FSLocationSpec} corresponding to the current settings.
     *
     * @return the {@link FSLocationSpec}
     */
    public FSLocationSpec getLocationSpec() {
        return m_fsConfig.getLocationSpec();
    }

    /**
     * Creates a {@link FSLocationFactory} that can be used to create {@link FSLocation} objects from string paths.
     *
     * @return a {@link FSLocationFactory} corresponding to the current settings
     */
    public FSLocationFactory createFSLocationFactory() {
        return m_fsConfig.createLocationFactory();
    }

    FileSystemConfiguration<DefaultFSLocationSpecConfig> getFileSystemConfiguration() {
        return m_fsConfig;
    }

    /**
     * Updates the settings with the input specs in the node model.
     *
     * @param specs the input {@link PortObjectSpec specs} of the node
     * @param statusMessageConsumer consumer for status messages e.g. warnings
     * @throws InvalidSettingsException if the specs are not compatible with the settings
     */
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer)
        throws InvalidSettingsException {
        m_fsConfig.configureInModel(specs, statusMessageConsumer);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelFileSystem createClone() {
        return new SettingsModelFileSystem(this);
    }

    @Override
    protected String getModelTypeID() {
        return "SMID_file_system";
    }

    @Override
    protected final String getConfigName() {
        return m_configName;
    }

    @Override
    protected final void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        NodeSettingsRO subsettings;
        try {
            subsettings = settings.getNodeSettings(m_configName);
        } catch (InvalidSettingsException ex) {
            subsettings = new NodeSettings(m_configName);
        }
        m_fsConfig.loadSettingsForDialog(subsettings, specs);
    }

    @Override
    protected final void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        final NodeSettingsWO fsSettings = settings.addNodeSettings(m_configName);
        m_fsConfig.saveSettingsForDialog(fsSettings);
    }

    @Override
    protected final void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO fsSettings = settings.getNodeSettings(m_configName);
        m_fsConfig.validateSettingsForModel(fsSettings);
    }

    @Override
    protected final void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO subsettings = settings.getNodeSettings(m_configName);
        m_fsConfig.loadSettingsForModel(subsettings);
    }

    @Override
    protected final void saveSettingsForModel(final NodeSettingsWO settings) {
        final NodeSettingsWO fsSettings = settings.addNodeSettings(m_configName);
        m_fsConfig.saveSettingsForModel(fsSettings);
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "(" + m_configName + ")";
    }

}
