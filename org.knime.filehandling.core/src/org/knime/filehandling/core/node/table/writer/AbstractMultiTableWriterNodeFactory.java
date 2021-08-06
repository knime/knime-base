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
 *   20 Jul 2021 (Moditha Hewasinghage, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.node.table.writer;

import java.util.Map;
import java.util.Optional;

import org.knime.core.data.DataValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * An abstract implementation of a node factory for table writer nodes.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
 *
 * @param <T> the type of {@link DataValue}
 * @param <C> the type of {@link AbstractMultiTableWriterNodeConfig}
 * @param <M> the type of {@link AbstractMultiTableWriterNodeModel}
 * @param <D> the type of {@link AbstractMultiTableWriterNodeDialog}
 *
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class AbstractMultiTableWriterNodeFactory<
        T extends DataValue, C extends AbstractMultiTableWriterNodeConfig<T, C>, //
        M extends AbstractMultiTableWriterNodeModel<C, ? extends AbstractMultiTableWriterCellFactory<T>>, //
        D extends AbstractMultiTableWriterNodeDialog<C>> //
    extends ConfigurableNodeFactory<M> {

    private static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    private static final String DATA_TABLE_INPUT_PORT_GRP_NAME = "Input Data Table";

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var builder = new PortsConfigurationBuilder();
        builder.addOptionalInputPortGroup(CONNECTION_INPUT_PORT_GRP_NAME, FileSystemPortObject.TYPE);
        builder.addFixedInputPortGroup(DATA_TABLE_INPUT_PORT_GRP_NAME, BufferedDataTable.TYPE);
        builder.addFixedOutputPortGroup("Output Table", BufferedDataTable.TYPE);
        return Optional.of(builder);
    }

    @Override
    protected M createNodeModel(final NodeCreationConfiguration creationConfig) {
        return getNodeModel(creationConfig.getPortConfig().orElseThrow(IllegalStateException::new),
            createNodeConfig(creationConfig), getDataTableInputIdx(creationConfig));
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return getDialog(createNodeConfig(creationConfig), getDataTableInputIdx(creationConfig));
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<M> createNodeView(final int viewIndex, final M nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private C createNodeConfig(final NodeCreationConfiguration creationConfig) {
        final PortsConfiguration portConfig = creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
        return getNodeConfig(portConfig, CONNECTION_INPUT_PORT_GRP_NAME);
    }

    private static int getDataTableInputIdx(final NodeCreationConfiguration creationConfig) {
        final PortsConfiguration portConfig = creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
        final Map<String, int[]> inputPortLocation = portConfig.getInputPortLocation();
        return inputPortLocation.get(DATA_TABLE_INPUT_PORT_GRP_NAME)[0];
    }

    /**
     * Creates the node config.
     *
     * @param portConfig storing the ports configurations
     * @param portGroupName data-table-input-port-group-name
     * @return concrete subclass instantiation of {@link AbstractMultiTableWriterNodeConfig}
     */
    protected abstract C getNodeConfig(final PortsConfiguration portConfig, final String portGroupName);

    /**
     * Creates the node model.
     *
     * @param portConfig storing the ports configurations
     * @param nodeConfig storing the user settings (concrete subclass instantiation of
     *            {@link AbstractMultiTableWriterNodeConfig})
     * @param dataTableInputIdx index of data-table-input-port-group-name
     * @return concrete subclass instantiation of {@link AbstractMultiTableWriterNodeModel}
     */
    protected abstract M getNodeModel(final PortsConfiguration portConfig, final C nodeConfig,
        final int dataTableInputIdx);

    /**
     * Creates the node dialog.
     *
     * @param nodeConfig storing the user settings (concrete subclass instantiation of
     *            {@link AbstractMultiTableWriterNodeConfig})
     * @param dataTableInputIdx index of data-table-input-port-group-name
     * @return concrete subclass instantiation of {@link AbstractMultiTableWriterNodeDialog}
     */
    protected abstract D getDialog(C nodeConfig, int dataTableInputIdx);
}
