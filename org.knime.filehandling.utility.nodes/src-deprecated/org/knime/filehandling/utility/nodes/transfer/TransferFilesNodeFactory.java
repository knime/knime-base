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
 *   Mar 5, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer;

import java.util.EnumSet;
import java.util.Optional;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * Node factory of the Transfer Files node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @deprecated since 4.3.3
 */
@Deprecated
public final class TransferFilesNodeFactory extends ConfigurableNodeFactory<TransferFilesNodeModel> {

    /** The name of the optional source connection input port group. */
    private static final String CONNECTION_SOURCE_PORT_GRP_NAME = "Source File System Connection";

    /** The name of the optional destination connection input port group. */
    private static final String CONNECTION_DESTINATION_PORT_GRP_NAME = "Destination File System Connection";

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder b = new PortsConfigurationBuilder();
        b.addOptionalInputPortGroup(CONNECTION_SOURCE_PORT_GRP_NAME, FileSystemPortObject.TYPE);
        b.addOptionalInputPortGroup(CONNECTION_DESTINATION_PORT_GRP_NAME, FileSystemPortObject.TYPE);
        b.addFixedOutputPortGroup("Output", BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected TransferFilesNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        PortsConfiguration portsConfiguration = creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
        return new TransferFilesNodeModel(portsConfiguration, createSettings(portsConfiguration));
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return new TransferFilesNodeDialog(
            createSettings(creationConfig.getPortConfig().orElseThrow(IllegalStateException::new)));
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<TransferFilesNodeModel> createNodeView(final int viewIndex,
        final TransferFilesNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static TransferFilesNodeConfig createSettings(final PortsConfiguration portsConfiguration) {
        return new TransferFilesNodeConfig(
            new SettingsModelReaderFileChooser("source_location", portsConfiguration, CONNECTION_SOURCE_PORT_GRP_NAME,
                EnumConfig.create(FilterMode.FILE, FilterMode.FILES_IN_FOLDERS, FilterMode.FOLDER)),
            new SettingsModelWriterFileChooser("destination_location", portsConfiguration,
                CONNECTION_DESTINATION_PORT_GRP_NAME, EnumConfig.create(FilterMode.FOLDER),
                EnumConfig.create(FileOverwritePolicy.IGNORE, FileOverwritePolicy.FAIL, FileOverwritePolicy.OVERWRITE),
                EnumSet.of(FSCategory.LOCAL, FSCategory.MOUNTPOINT, FSCategory.RELATIVE)));
    }
}
