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
package org.knime.base.node.preproc.append.row;

import java.util.Optional;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;

/**
 * Factory to create nodes that concatenate input tables to one output table.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public class AppendedRowsNodeFactory extends ConfigurableNodeFactory<AppendedRowsNodeModel>
    implements NodeDialogFactory {

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<AppendedRowsNodeModel> createNodeView(final int viewIndex, final AppendedRowsNodeModel nodeModel) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        PortsConfigurationBuilder b = new PortsConfigurationBuilder();
        b.addExtendableInputPortGroup("input",
            new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE}, BufferedDataTable.TYPE);
        b.addFixedOutputPortGroup("Concatenated tables", BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    /**
     * @since 4.1
     */
    @Override
    protected AppendedRowsNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        // cannot be null due to #createPortsConfigBuilder's correctness
        return new AppendedRowsNodeModel(creationConfig.getPortConfig().get());
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return createNodeDialog().createLegacyFlowVariableNodeDialog();
    }

    /**
     * @since 5.0
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, AppendedRowsNodeSettings.class);
    }
}
