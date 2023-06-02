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
 *   Jun 2, 2023 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser.item;

import java.util.List;
import java.util.Set;

import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractSettingsModelFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.ConnectedFileSystemSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.HubSpaceSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.RelativeToSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;

/**
 * Item chooser settings model for Item Version Creator node.
 *
 * @author Zkriya Rakhimberdiyev
 */
public final class SettingsModelItemChooser extends AbstractSettingsModelFileChooser<SettingsModelItemChooser> implements PathSettings {

    /**
     * Constructor.
     *
     * @param configName under which to store the settings
     * @param portsConfig {@link PortsConfiguration} of the corresponding KNIME node
     * @param fileSystemPortIdentifier identifier of the file system port group in <b>portsConfig</b>
     * @param filterModeConfig the {@link EnumConfig} specifying the default and supported {@link FilterMode
     *            FilterModes}
     */
    public SettingsModelItemChooser(final String configName, final PortsConfiguration portsConfig,
        final String fileSystemPortIdentifier, final EnumConfig<FilterMode> filterModeConfig) {

        super(configName, portsConfig,fileSystemPortIdentifier, filterModeConfig, List.of(
            // CONNECTED file system is the only selectable if a file system connection port is present
            hasFSPort -> new ConnectedFileSystemSpecificConfig(hasFSPort, portsConfig, fileSystemPortIdentifier),
            hasFSPort -> new RelativeToSpecificConfig(!hasFSPort, RelativeTo.SPACE,
                Set.of(RelativeTo.MOUNTPOINT, RelativeTo.WORKFLOW, RelativeTo.SPACE)),
            // Hub Space enabled when connection port is not present
            hasFSPort -> new HubSpaceSpecificConfig(!hasFSPort)));
        checkFilterModesSupportedByAllFileSystems();
    }

    private SettingsModelItemChooser(final SettingsModelItemChooser toCopy) {
        super(toCopy);
    }

    /**
     * If you only need the path string, use {@link #getLocation()} instead.
     *
     * @see #getLocation()
     */
    @Override
    public ReadPathAccessor createReadPathAccessor() {
        return super.createPathAccessor();
    }

    @Override
    public SettingsModelItemChooser createClone() {
        return new SettingsModelItemChooser(this);
    }

    @Override
    public String getPath() {
        return getLocation().getPath();
    }

    @Override
    protected String getModelTypeID() {
        return "SMID_ItemChooser";
    }
}
