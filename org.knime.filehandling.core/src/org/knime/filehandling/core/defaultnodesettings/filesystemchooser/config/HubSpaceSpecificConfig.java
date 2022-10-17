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
 *   Sep 12, 2022 (Alexander Bondaletov): created
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
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * Config for Hub Space file system.
 *
 * @author Alexander Bondaletov
 */
public class HubSpaceSpecificConfig extends AbstractConvenienceFileSystemSpecificConfig {

    private final HubSpaceSettings m_settings;

    /**
     * @param active the flag indicating whether this config is active (i.e. selectable for the user).
     */
    public HubSpaceSpecificConfig(final boolean active) {
        this(active, "", "");
    }

    private HubSpaceSpecificConfig(final HubSpaceSpecificConfig copy) {
        this(copy.isActive(), copy.m_settings.getSpaceId(), copy.m_settings.getSpaceName());
    }

    private HubSpaceSpecificConfig(final boolean active, final String spaceId, final String spaceName) {
        super(active);
        m_settings = new HubSpaceSettings(false);
        m_settings.set(spaceId, spaceName);
        m_settings.addChangeListener(e -> notifyListeners());
    }

    /**
     * @return the underlying space settings.
     */
    public HubSpaceSettings getSpaceSettings() {
        return m_settings;
    }

    @Override
    public FSLocationSpec getLocationSpec() {
        return new DefaultFSLocationSpec(FSCategory.HUB_SPACE, m_settings.getSpaceId());
    }

    @Override
    public String getFileSystemName() {
        return FSCategory.HUB_SPACE.getLabel();
    }

    @Override
    public void updateSpecifier(final FSLocationSpec locationSpec) {
        String id = locationSpec.getFileSystemSpecifier()
            .orElseThrow(() -> new IllegalArgumentException("Hub Space is not specified"));
        m_settings.set(id, "");
    }

    @Override
    public void validate(final FSLocationSpec location) throws InvalidSettingsException {
        final Optional<String> specifier = location.getFileSystemSpecifier();
        CheckUtils.checkSetting(specifier.isPresent(), "No space specified for the Hub Space file system.");
    }

    @Override
    public boolean canConnect() {
        return WorkflowContextUtil.isCurrentWorkflowOnHub() && !m_settings.getSpaceId().isEmpty();
    }

    @Override
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer)
        throws InvalidSettingsException {

        m_settings.validate();
    }

    @Override
    public Set<FileSelectionMode> getSupportedFileSelectionModes() {
        return EnumSet.allOf(FileSelectionMode.class);
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            loadInModel(settings);
        } catch (InvalidSettingsException ex) { //NOSONAR
            m_settings.set("", "");
        }
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    @Override
    public void validateInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    public FileSystemSpecificConfig copy() {
        return new HubSpaceSpecificConfig(this);
    }

    @Override
    public void report(final Consumer<StatusMessage> messageConsumer) {
        // avoid doing any checks here, it will prevent the dialog from being closed
        // instead we do validation in configureInModel()
    }
}
