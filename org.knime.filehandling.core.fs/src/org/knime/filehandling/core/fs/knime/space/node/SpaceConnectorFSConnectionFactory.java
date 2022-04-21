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
 *   Apr 17, 2022 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.core.fs.knime.space.node;

import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.config.MountpointFSConnectionConfig;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.fs.knime.relativeto.export.RelativeToFileSystemConstants;
import org.knime.filehandling.core.fs.knime.space.node.SpaceConnectorNodeSettings.SpaceMode;

/**
 * Factory class used by the {@link SpaceConnectorNodeModel} node. Creates different types of FS Connections
 * according to provided settings.
 *
 * @author Zkriya Rakhimberdiyev
 */
public abstract class SpaceConnectorFSConnectionFactory {

    /**
     * @param settings The connector settings.
     * @return The factory instance.
     */
    public static SpaceConnectorFSConnectionFactory create(final SpaceConnectorNodeSettings settings) {

        if (settings.getSpaceMode() == SpaceMode.CURRENT) {
            return new RelativeToCurrentSpaceFactory(settings);
        } else {
            return new CustomSpaceFactory(settings);
        }
    }

    /**
     * The settings to use to create the {@link FSConnection}.
     */
    protected final SpaceConnectorNodeSettings m_settings;

    /**
     * @param settings The node settings.
     */
    protected SpaceConnectorFSConnectionFactory(final SpaceConnectorNodeSettings settings) {
        m_settings = settings;
    }

    /**
     * @return The FS type object.
     */
    public abstract FSType getFSType();

    /**
     * @return The FS Location spec.
     */
    public abstract FSLocationSpec getFSLocationSpec();

    /**
     * @return The FS Connection.
     */
    public abstract FSConnection createFSConnection();

    private static class CustomSpaceFactory extends SpaceConnectorFSConnectionFactory {

        protected CustomSpaceFactory(final SpaceConnectorNodeSettings settings) {
            super(settings);
        }

        @Override
        public FSType getFSType() {
            return FSType.SPACE;
        }

        @Override
        public FSLocationSpec getFSLocationSpec() {
            return createMountpointFSConnectionConfig().createFSLocationSpec();
        }

        @Override
        public FSConnection createFSConnection() {
            return DefaultFSConnectionFactory.createMountpointConnection(createMountpointFSConnectionConfig());
        }

        private MountpointFSConnectionConfig createMountpointFSConnectionConfig() {
            return new MountpointFSConnectionConfig( //
                m_settings.getWorkingDirectoryModel().getStringValue(), //
                m_settings.getMountpoint().getMountpoint().getId());
        }
    }

    private static class RelativeToCurrentSpaceFactory extends SpaceConnectorFSConnectionFactory {

        protected RelativeToCurrentSpaceFactory(final SpaceConnectorNodeSettings settings) {
            super(settings);
        }

        @Override
        public FSType getFSType() {
            return FSType.RELATIVE_TO_MOUNTPOINT;
        }

        @Override
        public FSLocationSpec getFSLocationSpec() {
            return RelativeToFileSystemConstants.CONNECTED_MOUNTPOINT_RELATIVE_FS_LOCATION_SPEC;
        }

        @Override
        public FSConnection createFSConnection() {
            return DefaultFSConnectionFactory.createRelativeToConnection(RelativeTo.MOUNTPOINT,
                m_settings.getWorkingDirectoryModel().getStringValue());
        }
    }
}
