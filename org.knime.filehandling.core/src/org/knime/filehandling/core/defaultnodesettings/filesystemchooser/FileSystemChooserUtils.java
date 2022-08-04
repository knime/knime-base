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
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.ConnectedFileSystemSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.CustomURLSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.FSLocationSpecHandler;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.FileSystemConfiguration;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.FileSystemSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.LocalSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.MountpointSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.RelativeToSpecificConfig;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.ConnectedFileSystemDialog;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.CustomURLFileSystemDialog;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.FileSystemChooser;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.FileSystemSpecificDialog;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.LocalFileSystemDialog;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.MountpointFileSystemDialog;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.RelativeToFileSystemDialog;
import org.knime.filehandling.core.util.MountPointFileSystemAccessService;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * Static uility class that provides factory methods for {@link FileSystemChooser} and {@link FileSystemConfiguration}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class FileSystemChooserUtils {

    private FileSystemChooserUtils() {
        // static utility class
    }

    private static boolean hasFSPort(final PortsConfiguration portsConfig, final String fileSystemPortIdentifier) {
        return portsConfig.getInputPortLocation().get(fileSystemPortIdentifier) != null;
    }

    /**
     * @param <L> the type of {@link FSLocationSpec}
     * @param portsConfig {@link PortsConfiguration} of the corresponding KNIME node
     * @param fileSystemPortIdentifier identifier of the file system port group in <b>portsConfig</b>
     * @param locationHandler the {@link FSLocationSpecHandler} to be used
     * @param convenienceFS the convenience file systems that should be active in the config (provided there is no
     *            connected file system)
     * @return a fresh {@link FileSystemConfiguration}
     */
    public static <L extends FSLocationSpec> FileSystemConfiguration<L> createConfig(
        final PortsConfiguration portsConfig, final String fileSystemPortIdentifier,
        final FSLocationSpecHandler<L> locationHandler, final Set<FSCategory> convenienceFS) {
        final FileSystemSpecificConfig[] specificConfigs =
            createSpecificConfigs(convenienceFS, portsConfig, fileSystemPortIdentifier);
        return new FileSystemConfiguration<>(locationHandler, specificConfigs);
    }

    private static FileSystemSpecificConfig[] createSpecificConfigs(final Set<FSCategory> convenienceFS,
        final PortsConfiguration portsConfig, final String portId) {
        final boolean hasPort = hasFSPort(portsConfig, portId);
        final EnumSet<FSCategory> convenienceWithoutConnected = EnumSet.copyOf(convenienceFS);
        convenienceWithoutConnected.remove(FSCategory.CONNECTED);
        final Stream<FileSystemSpecificConfig> convenienceConfigs =
                convenienceWithoutConnected.stream().map(c -> createConvenienceConfig(c, !hasPort));
        return Stream.concat(convenienceConfigs//
            , Stream.of(new ConnectedFileSystemSpecificConfig(hasPort, portsConfig, portId)))//
            .toArray(FileSystemSpecificConfig[]::new);
    }

    /**
     * Gets the initial default {@link FSLocationSpec}, depending on the current workflow context.
     *
     * @return the initial default {@link FSLocationSpec}.
     */
    public static FSLocationSpec getDefaultFSLocationSpec() {

        final var workflowContext = WorkflowContextUtil.getWorkflowContext();
        final var remoteMountId = workflowContext.getMountpointURI().map(URI::getHost)
                .orElse(null);

        final var hubMountedIds = MountPointFileSystemAccessService.instance().getHubMountedIDs();
        final var serverMountedIds = MountPointFileSystemAccessService.instance().getServerMountedIDs();

        // TODO: clean this up using WorkflowContextV2, in particular don't pick relative to current workflow
        // when we are on server or hub
        if (remoteMountId == null) {
            // we are in Server or Hub Executor (we cannot tell which, unfortunately)
            // hence we pick something that works regardless: Relative To Current workflow
            final var relativeConfig = new RelativeToSpecificConfig(true);
            relativeConfig.setRelativeTo(RelativeTo.WORKFLOW);
            return relativeConfig.getLocationSpec();
        } else if (serverMountedIds.contains(remoteMountId)) {
            // we are in AP (temp workflow copy) and workflow is stored in server repo
            // hence we pick Relative To Current Mountpoint
            final var relativeConfig = new RelativeToSpecificConfig(true);
            relativeConfig.setRelativeTo(RelativeTo.MOUNTPOINT);
           return relativeConfig.getLocationSpec();
        } else if (hubMountedIds.contains(remoteMountId)) {
            // we are in AP (temp workflow copy) and workflow is stored in Hub Space
            // hence we pickRelative To Current Space
            final var relativeConfig = new RelativeToSpecificConfig(true);
            relativeConfig.setRelativeTo(RelativeTo.SPACE);
           return relativeConfig.getLocationSpec();
        } else {
            // we are in AP workflow is stored in a local workspace
            // hence we pick the local file system
            return new LocalSpecificConfig(true).getLocationSpec();
        }
    }

    private static FileSystemSpecificConfig createConvenienceConfig(final FSCategory category, final boolean active) {
        switch (category) {
            case CUSTOM_URL:
                return new CustomURLSpecificConfig(active);
            case LOCAL:
                return new LocalSpecificConfig(active);
            case MOUNTPOINT:
                return new MountpointSpecificConfig(active);
            case RELATIVE:
                return new RelativeToSpecificConfig(active);
            default:
                throw new IllegalArgumentException("Unsupported FSCategory: " + category);

        }
    }

    /**
     * Creates a {@link FileSystemChooser} given the provided {@link FileSystemConfiguration} and the specified
     * {@link FSCategory file system category}.
     *
     * @param config the configuration of the chooser
     * @return a new {@link FileSystemChooser}
     */
    public static FileSystemChooser createFileSystemChooser(final FileSystemConfiguration<?> config) {
        final Set<FSCategory> categories = config.getActiveFSCategories();
        if (config.hasFSPort()) {
            return new FileSystemChooser(config, new ConnectedFileSystemDialog(
                (ConnectedFileSystemSpecificConfig)config.getFileSystemSpecifcConfig(FSCategory.CONNECTED)));
        } else {
            return createConvenienceFileSystemChooser(config, categories);
        }
    }

    private static FileSystemChooser createConvenienceFileSystemChooser(final FileSystemConfiguration<?> config,
        final Set<FSCategory> categories) {
        final List<FileSystemSpecificDialog> dialogs = new ArrayList<>();
        if (categories.contains(FSCategory.LOCAL)) {
            dialogs.add(LocalFileSystemDialog.INSTANCE);
        }
        if (categories.contains(FSCategory.MOUNTPOINT)) {
            dialogs.add(new MountpointFileSystemDialog(
                (MountpointSpecificConfig)config.getFileSystemSpecifcConfig(FSCategory.MOUNTPOINT)));
        }
        if (categories.contains(FSCategory.RELATIVE)) {
            dialogs.add(new RelativeToFileSystemDialog(
                (RelativeToSpecificConfig)config.getFileSystemSpecifcConfig(FSCategory.RELATIVE)));
        }
        if (categories.contains(FSCategory.CUSTOM_URL)) {
            dialogs.add(new CustomURLFileSystemDialog(
                (CustomURLSpecificConfig)config.getFileSystemSpecifcConfig(FSCategory.CUSTOM_URL)));
        }
        return new FileSystemChooser(config, dialogs.toArray(new FileSystemSpecificDialog[0]));
    }
}
