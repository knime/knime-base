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
 *   May 7, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * Config for connected file systems.</br>
 * Holds the actual file system type as well as whether a connection is available.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class ConnectedFileSystemSpecificConfig extends AbstractFileSystemSpecificConfig {

    private static final DefaultStatusMessage NO_FS_ERROR = new DefaultStatusMessage(MessageType.ERROR, "No file system connected.");

    private static final String CFG_CONNECTED_FS = "connected_fs";

    private final int m_portIdx;

    private Optional<FSConnection> m_connection = Optional.empty();

    private String m_fileSystemName = null;

    private FSLocationSpec m_locationSpec = new DefaultFSLocationSpec(FSCategory.CONNECTED, null);

    /**
     * Constructor.
     *
     * @param active flag indicating whether this config is active (i.e. selectable for the user)
     * @param portsConfig {@link PortsConfiguration} of the node this settings is used for
     * @param fileSystemPortIdentifier the ID used to identify the file system port in <b>portsConfig</b>
     */
    public ConnectedFileSystemSpecificConfig(final boolean active, final PortsConfiguration portsConfig,
        final String fileSystemPortIdentifier) {
        super(active);
        final int[] portIdx = portsConfig.getInputPortLocation().get(fileSystemPortIdentifier);
        if (portIdx == null) {
            // the portIdx is null if the node has no fs port but then this config must be inactive
            CheckUtils.checkArgument(!active, "No file system port found.");
            m_portIdx = -1;
        } else {
        CheckUtils.checkArgument(portIdx.length == 1,
            "The input port group '%s' should contain at most 1 element but contained %s.", fileSystemPortIdentifier,
            portIdx.length);
        m_portIdx = portIdx[0];
        }
    }

    int getPortIdx() {
        return m_portIdx;
    }

    private ConnectedFileSystemSpecificConfig(final ConnectedFileSystemSpecificConfig toCopy) {
        super(toCopy.isActive());
        m_portIdx = toCopy.m_portIdx;
        m_connection = toCopy.m_connection;
        m_fileSystemName = toCopy.m_fileSystemName;
        m_locationSpec = toCopy.m_locationSpec;
    }

    /**
     * Returns whether or not the file system is connected i.e. the corresponding connector node has been executed.
     *
     * @return {@code true} if the file system is connected
     */
    public boolean hasConnection() {
        return m_connection.isPresent();
    }

    /**
     * Returns the name of the connected file system e.g. "Amazon S3".
     *
     * @return the name of the connected file system
     */
    public String getFileSystemName() {
        return m_fileSystemName;
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        if (m_portIdx == -1) {
            // nothing to load, the config is inactive
            return;
        }
        if (specs[m_portIdx] == null) {
            throw new NotConfigurableException("No file system connected");
        }
        m_fileSystemName = FileSystemPortObjectSpec.getFileSystemType(specs, m_portIdx).orElse(null);
        m_connection = FileSystemPortObjectSpec.getFileSystemConnection(specs, m_portIdx);
        m_locationSpec = extractFSLocationSpec(specs);
        notifyListeners();
    }

    private FSLocationSpec extractFSLocationSpec(final PortObjectSpec[] specs) {
        return ((FileSystemPortObjectSpec)specs[m_portIdx]).getFSLocationSpec();
    }

    @Override
    public void validateInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // check if the settings were written by a compatible config (i.e. one that contains this class)
        settings.getBoolean(CFG_CONNECTED_FS);
    }

    @Override
    public void report(final Consumer<StatusMessage> statusConsumer) {
        if (m_fileSystemName == null) {
            statusConsumer.accept(NO_FS_ERROR);
        }
        if (!m_connection.isPresent()) {
            issueNotConnectedWarning(statusConsumer);
        }
    }

    private static void issueNotConnectedWarning(final Consumer<StatusMessage> statusConsumer) {
        statusConsumer.accept(new DefaultStatusMessage(MessageType.WARNING,
            "No connection available. Execute the connector node first."));
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no settings to load
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        // save placeholder that indicates if the settings were saved with a config that contains this class
        settings.addBoolean(CFG_CONNECTED_FS, true);
    }

    @Override
    public void overwriteWith(final FSLocationSpec locationSpec) {
        // nothing to overwrite
    }

    @Override
    public FileSystemSpecificConfig copy() {
        return new ConnectedFileSystemSpecificConfig(this);
    }

    @Override
    public void validate(final FSLocationSpec location) throws InvalidSettingsException {
        // nothing to validate
    }

    @Override
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer)
        throws InvalidSettingsException {
        if (m_portIdx == -1) {
            // nothing to configure, we are inactive
            return;
        }
        CheckUtils.checkSetting(specs.length > m_portIdx && specs[m_portIdx] != null,
            "No file system connected at port %s.", m_portIdx);
        m_locationSpec = extractFSLocationSpec(specs);
        m_fileSystemName = FileSystemPortObjectSpec.getFileSystemType(specs, m_portIdx).orElse(null);
        m_connection = FileSystemPortObjectSpec.getFileSystemConnection(specs, m_portIdx);
        if (!m_connection.isPresent()) {
            issueNotConnectedWarning(statusMessageConsumer);
        }
    }

    @Override
    public Optional<FSConnection> getConnection() {
        return m_connection;
    }

    @Override
    public FSLocationSpec getLocationSpec() {
        return m_locationSpec;
    }

    @Override
    public Set<FileSelectionMode> getSupportedFileSelectionModes() {
        return EnumSet.allOf(FileSelectionMode.class);
    }

}
