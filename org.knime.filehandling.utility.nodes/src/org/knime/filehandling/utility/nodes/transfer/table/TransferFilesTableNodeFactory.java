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
 *   Mar 2, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer.table;

import java.util.Map;
import java.util.Optional;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.utility.nodes.transfer.AbstractTransferFilesNodeFactory;

/**
 * Node factory of the Transfer Files/Folder (Table) node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class TransferFilesTableNodeFactory extends AbstractTransferFilesNodeFactory<TransferFilesTableNodeModel> {

    private static final String TABLE_INPUT_FILE_PORT_GRP_NAME = "Input Table";

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder b = new PortsConfigurationBuilder();
        b.addOptionalInputPortGroup(AbstractTransferFilesNodeFactory.CONNECTION_SOURCE_PORT_GRP_NAME,
            FileSystemPortObject.TYPE);
        b.addFixedInputPortGroup(TABLE_INPUT_FILE_PORT_GRP_NAME, BufferedDataTable.TYPE);
        b.addOptionalInputPortGroup(AbstractTransferFilesNodeFactory.CONNECTION_DESTINATION_PORT_GRP_NAME,
            FileSystemPortObject.TYPE);
        b.addFixedOutputPortGroup("Output", BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected TransferFilesTableNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        final PortsConfiguration portsConfig = getPortsConfig(creationConfig);
        final Map<String, int[]> inputPortLocation = portsConfig.getInputPortLocation();
        final int inputTableIdx = inputPortLocation.get(TABLE_INPUT_FILE_PORT_GRP_NAME)[0];
        final int srcConnectionIdx =
            Optional.ofNullable(inputPortLocation.get(AbstractTransferFilesNodeFactory.CONNECTION_SOURCE_PORT_GRP_NAME))//
                .map(a -> a[0])//
                .orElseGet(() -> -1);
        final int destConnectionIdx = Optional
            .ofNullable(inputPortLocation.get(AbstractTransferFilesNodeFactory.CONNECTION_DESTINATION_PORT_GRP_NAME))//
            .map(a -> a[0])//
            .orElseGet(() -> -1);
        return new TransferFilesTableNodeModel(portsConfig, createSettings(portsConfig), inputTableIdx,
            srcConnectionIdx, destConnectionIdx);
    }

    @Override
    protected TransferFilesTableNodeDialog createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        final PortsConfiguration portsConfig = getPortsConfig(creationConfig);
        final Map<String, int[]> inputPortLocation = portsConfig.getInputPortLocation();
        final int inputTableIdx = inputPortLocation.get(TABLE_INPUT_FILE_PORT_GRP_NAME)[0];
        return new TransferFilesTableNodeDialog(createSettings(portsConfig), inputTableIdx);
    }

    private static PortsConfiguration getPortsConfig(final NodeCreationConfiguration creationConfig) {
        return creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<TransferFilesTableNodeModel> createNodeView(final int viewIndex,
        final TransferFilesTableNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static TransferFilesTableNodeConfig createSettings(final PortsConfiguration portsConfiguration) {
        return new TransferFilesTableNodeConfig(getDestinationFileWriter(portsConfiguration));
    }

}
