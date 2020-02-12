/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.filehandling.core.node.portobject;

import java.util.Optional;

import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.filehandling.core.node.portobject.reader.PortObjectReaderNodeFactory;
import org.knime.filehandling.core.node.portobject.writer.PortObjectWriterNodeFactory;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * Abstract node factory for port object reader and writer nodes.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @param <M> the node model of the node
 * @param <D> the node dialog of the node
 * @noextend extend either {@link PortObjectReaderNodeFactory} or {@link PortObjectWriterNodeFactory}
 */
public abstract class PortObjectIONodeFactory<M extends PortObjectIONodeModel<?>, D extends PortObjectIONodeDialog<?>>
    extends ConfigurableNodeFactory<M> {

    @Override
    protected final D createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        final D dia = createDialog(creationConfig);
        dia.finalizeOptionsPanel();
        return dia;
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<M> createNodeView(final int viewIndex, final M nodeModel) {
        return null;
    }

    @Override
    public final boolean hasDialog() {
        return true;
    }

    @Override
    protected final Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder b = new PortsConfigurationBuilder();
        b.addOptionalInputPortGroup(PortObjectIONodeModel.CONNECTION_INPUT_PORT_GRP_NAME, FileSystemPortObject.TYPE);
        addAdditionalPorts(b);
        return Optional.of(b);
    }

    /**
     * Creates and returns a new node dialog pane, if {@link #hasDialog()} returns <code>true</code>.
     *
     * @param creationConfig the node creation configuration
     * @return a new {@link NodeModel}
     */
    protected abstract D createDialog(final NodeCreationConfiguration creationConfig);

    /**
     * Allows to add additional input and output ports. Note that the first input port is occupied by an optional
     * connection port.
     *
     * @param b the ports configuration builder
     */
    protected abstract void addAdditionalPorts(final PortsConfigurationBuilder b);

}
