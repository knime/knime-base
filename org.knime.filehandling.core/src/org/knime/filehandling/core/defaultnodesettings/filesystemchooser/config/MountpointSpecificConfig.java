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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.util.MountPointFileSystemAccessService;

/**
 * Config for the mountpoint file system.</br>
 * Contains the selected mountpoint.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class MountpointSpecificConfig extends AbstractConvenienceFileSystemSpecificConfig
    implements ComboBoxModel<KNIMEConnection> {

    private static final DefaultStatusMessage NO_MOUNTPOINT_SELECTED_MSG =
        new DefaultStatusMessage(MessageType.ERROR, "No mountpoint selected.");

    private static final String CFG_MOUNTPOINT = "mountpoint";

    private final Supplier<List<String>> m_mountedIdsSupplier;

    private KNIMEConnection m_mountpoint;

    private final List<KNIMEConnection> m_availableMountpoints = new ArrayList<>();

    private final List<ListDataListener> m_listListeners = new LinkedList<>();

    private ListDataEvent m_selectionChangedEvent = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, -1, -1);

    /**
     * Constructor.
     *
     * @param active flag indicating whether this config is active (i.e. selectable for the user)
     */
    public MountpointSpecificConfig(final boolean active) {
        this(active, () -> MountPointFileSystemAccessService.instance().getAllMountedIDs());
    }

    /**
     * Constructor.
     *
     * @param active flag indicating whether this config is active (i.e. selectable for the user)
     * @param mountedIdsSupplier mounted ids supplier
     */
    public MountpointSpecificConfig(final boolean active, final Supplier<List<String>> mountedIdsSupplier) {
        super(active);
        m_mountedIdsSupplier = mountedIdsSupplier;
        m_mountpoint = getDefaultMountpoint();
    }

    /**
     * Copy constructor.
     *
     * @param toCopy instance to copy
     */
    private MountpointSpecificConfig(final MountpointSpecificConfig toCopy) {
        super(toCopy.isActive());
        m_mountedIdsSupplier = toCopy.m_mountedIdsSupplier;
        m_mountpoint = toCopy.m_mountpoint;
    }

    /**
     * Returns the selected mountpoint.
     *
     * @return the mountpoint
     */
    public KNIMEConnection getMountpoint() {
        return m_mountpoint;
    }

    /**
     * Returns an unmodifiable list of the available mountpoints.
     *
     * @return an unmodifiable list of the available mountpoints
     */
    public List<KNIMEConnection> getAvailableMountpoints() {
        return Collections.unmodifiableList(m_availableMountpoints);
    }

    /**
     * Sets the provided mountpoint and notifies listeners if the value changed.
     *
     * @param mountpoint to set
     */
    public void setMountpoint(final KNIMEConnection mountpoint) {
        if (!Objects.equals(m_mountpoint, mountpoint)) {
            m_mountpoint = mountpoint;
            notifyListeners();
            notifySelectionChanged();
        }
    }

    private void notifySelectionChanged() {
        m_listListeners.forEach(l -> l.contentsChanged(m_selectionChangedEvent));
    }

    @Override
    public FSLocationSpec getLocationSpec() {
        return new DefaultFSLocationSpec(FSCategory.MOUNTPOINT, m_mountpoint.getId());
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        String mountpoint = settings.getString(CFG_MOUNTPOINT, null);
        m_mountpoint = mountpoint == null ? getDefaultMountpoint()
            : KNIMEConnection.getOrCreateMountpointAbsoluteConnection(mountpoint);
        m_availableMountpoints.clear();
        m_mountedIdsSupplier.get().stream()
            .map(KNIMEConnection::getOrCreateMountpointAbsoluteConnection).forEach(m_availableMountpoints::add);
        if (m_mountpoint != null && !m_mountpoint.isValid()) {
            // mountpoint is no longer available and therefore not among the available mountpoints
            assert !m_availableMountpoints.contains(m_mountpoint);
            m_availableMountpoints.add(m_mountpoint);
        }
        notifyListUpdated();
        notifyListeners();
    }

    private void notifyListUpdated() {
        final ListDataEvent listEvent =
            new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, m_availableMountpoints.size());
        m_listListeners.forEach(l -> l.contentsChanged(listEvent));
    }

    private KNIMEConnection getDefaultMountpoint() {
        final List<KNIMEConnection> connections =
                m_mountedIdsSupplier.get().stream()//
                .map(KNIMEConnection::getOrCreateMountpointAbsoluteConnection)//
                .collect(toList());
        final Optional<KNIMEConnection> connected =
            connections.stream().filter(KNIMEConnection::isConnected).findFirst();
        if (connected.isPresent()) {
            return connected.get();
        } else {
            return connections.isEmpty() ? null : connections.get(0);
        }
    }

    @Override
    public void updateSpecifier(final FSLocationSpec locationSpec) {
        String mountpoint = locationSpec.getFileSystemSpecifier()
            .orElseThrow(() -> new IllegalArgumentException("No mountpoint specified."));
        setMountpoint(KNIMEConnection.getOrCreateMountpointAbsoluteConnection(mountpoint));
    }

    @Override
    public void validateInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFG_MOUNTPOINT);
    }

    @Override
    public void report(final Consumer<StatusMessage> statusConsumer) {
        if (m_mountpoint == null) {
            statusConsumer.accept(NO_MOUNTPOINT_SELECTED_MSG);
        }
        if (m_mountpoint != null && !m_mountpoint.isValid()) {
            statusConsumer.accept(
                new DefaultStatusMessage(MessageType.ERROR, "The selected mountpoint '%s' is invalid.", m_mountpoint));
        }
        if (m_mountpoint != null && m_mountpoint.isValid() && !m_mountpoint.isConnected()) {
            issueNotConnectedWarning(statusConsumer);
        }
    }

    private void issueNotConnectedWarning(final Consumer<StatusMessage> statusConsumer) {
        statusConsumer.accept(new DefaultStatusMessage(MessageType.WARNING,
            "The selected mountpoint '%s' is not connected.", m_mountpoint));
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_mountpoint = KNIMEConnection.getOrCreateMountpointAbsoluteConnection(settings.getString(CFG_MOUNTPOINT));
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFG_MOUNTPOINT, m_mountpoint.getId());
    }

    @Override
    public FileSystemSpecificConfig copy() {
        return new MountpointSpecificConfig(this);
    }

    @Override
    public int getSize() {
        return m_availableMountpoints.size();
    }

    @Override
    public KNIMEConnection getElementAt(final int index) {
        return m_availableMountpoints.get(index);
    }

    @Override
    public void addListDataListener(final ListDataListener l) {
        m_listListeners.add(l);
    }

    @Override
    public void removeListDataListener(final ListDataListener l) {
        m_listListeners.remove(l);
    }

    @Override
    public void setSelectedItem(final Object anItem) {
        setMountpoint((KNIMEConnection)anItem);
    }

    @Override
    public Object getSelectedItem() {
        return getMountpoint();
    }

    @Override
    public void validate(final FSLocationSpec location) throws InvalidSettingsException {
        final Optional<String> specifier = location.getFileSystemSpecifier();
        CheckUtils.checkSetting(specifier.isPresent(), "No mountpoint specified for the mountpoint file system.");
    }

    @Override
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer)
        throws InvalidSettingsException {
        // nothing to configure
        CheckUtils.checkSetting(m_mountpoint.isValid(), "The selected mountpoint '%s' is no longer valid.",
            m_mountpoint);
        if (!m_mountpoint.isConnected()) {
            issueNotConnectedWarning(statusMessageConsumer);
        }
    }

    @Override
    public Set<FileSelectionMode> getSupportedFileSelectionModes() {
        return EnumSet.allOf(FileSelectionMode.class);
    }

    @Override
    public boolean canConnect() {
        return m_mountpoint != null && m_mountpoint.isValid() && m_mountpoint.isConnected();
    }

    @Override
    public String getFileSystemName() {
        return "Mountpoint";
    }

}
