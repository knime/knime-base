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
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 3, 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.deletepaths.table;

import static org.knime.node.impl.description.PortDescription.dynamicPort;
import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.utility.nodes.deletepaths.AbstractDeleteFilesAndFoldersNodeConfig;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * Node Factory of the "Delete Files/Folders (Table based)" node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class DeleteFilesAndFoldersTableNodeFactory extends
    ConfigurableNodeFactory<DeleteFilesAndFoldersTableNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    static final String TABLE_INPUT_PORT_GRP_NAME = "Table Input";

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder b = new PortsConfigurationBuilder();
        b.addFixedInputPortGroup(TABLE_INPUT_PORT_GRP_NAME, BufferedDataTable.TYPE);
        b.addOptionalInputPortGroup(AbstractDeleteFilesAndFoldersNodeConfig.CONNECTION_INPUT_PORT_GRP_NAME,
            FileSystemPortObject.TYPE);
        b.addFixedOutputPortGroup(AbstractDeleteFilesAndFoldersNodeConfig.OUTPUT_PORT_GRP_NAME, BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected DeleteFilesAndFoldersTableNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new DeleteFilesAndFoldersTableNodeModel(
            creationConfig.getPortConfig().orElseThrow(IllegalStateException::new));
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<DeleteFilesAndFoldersTableNodeModel> createNodeView(final int viewIndex,
        final DeleteFilesAndFoldersTableNodeModel nodeModel) {
        return null;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Delete Files/Folders (Table)";

    private static final String NODE_ICON = "../delete16x16.png";

    private static final String SHORT_DESCRIPTION = """
            Deletes files and folders.
            """;

    private static final String FULL_DESCRIPTION =
        """
                <p> This node deletes files and folders listed in a Path column. The node has got an optional input
                    port for a File System connection which can be used to delete files and folders on different file
                    systems. <b>Note that if the node is connected to a file system all paths will be resolved against the
                    connected file system. In case the input column contains a path from a connected file system and no
                    filesystem connection is connected, the node will fail during execution. </b> </p> <p> <i>This node can
                    access a variety of different</i> <a
                    href="https://docs.knime.com/2021-06/analytics_platform_file_handling_guide/index.html#analytics-platform-file-systems"><i>file
                    systems.</i></a> <i>More information about file handling in KNIME can be found in the official</i> <a
                    href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html"><i>File Handling
                    Guide.</i></a> </p>
                """;

    private static final List<PortDescription> INPUT_PORTS = List.of(fixedPort("Input table", """
            The input table containing a Path column.
            """), dynamicPort("File System Connection", "File System Connection", """
            The file system connection.
            """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("Output table", """
            Table containing the list of files/folders that have been deleted. Based on the fail options, there are
            additional boolean columns which indicates whether a deletion was successful or not and if a file
            existed or not.
            """));

    @Override
    public NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, DeleteFilesAndFoldersTableNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
            SHORT_DESCRIPTION, FULL_DESCRIPTION, List.of(), DeleteFilesAndFoldersTableNodeParameters.class, null,
            NodeType.Other, List.of(), null);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, DeleteFilesAndFoldersTableNodeParameters.class));
    }
}
