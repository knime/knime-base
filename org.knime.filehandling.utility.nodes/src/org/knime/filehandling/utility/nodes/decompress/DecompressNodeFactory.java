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
 *   24 Aug 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.decompress;

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
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * Node Factory for the "Decompress Files" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class DecompressNodeFactory extends ConfigurableNodeFactory<DecompressNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private static final String OUTPUT_PORT_GRP_NAME = "Output Port";

    static final String CONNECTION_INPUT_FILE_PORT_GRP_NAME = "Source File System Connection";

    static final String CONNECTION_OUTPUT_DIR_PORT_GRP_NAME = "Destination File System Connection";

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder builder = new PortsConfigurationBuilder();
        builder.addOptionalInputPortGroup(CONNECTION_INPUT_FILE_PORT_GRP_NAME, FileSystemPortObject.TYPE);
        builder.addOptionalInputPortGroup(CONNECTION_OUTPUT_DIR_PORT_GRP_NAME, FileSystemPortObject.TYPE);
        builder.addFixedOutputPortGroup(OUTPUT_PORT_GRP_NAME, BufferedDataTable.TYPE);

        return Optional.of(builder);
    }

    @Override
    protected DecompressNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new DecompressNodeModel((creationConfig.getPortConfig().orElseThrow(IllegalStateException::new)));
    }


    @Override
    public NodeView<DecompressNodeModel> createNodeView(final int viewIndex, final DecompressNodeModel nodeModel) {
        return null;
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }
    private static final String NODE_NAME = "Decompress Files";
    private static final String NODE_ICON = "decompress16x16.png";
    private static final String SHORT_DESCRIPTION = """
            Unpacks and decompresses files from an archive.
            """;
    private static final String FULL_DESCRIPTION = """
            <p> This node unpacks and decompresses files from an archive file. The paths to the extracted files are
                provided in the output table using a <a
                href="https://docs.knime.com/2021-06/analytics_platform_file_handling_guide/index.html#path">path
                column.</a> </p> <p> Supported archive formats are: <ul> <li>.zip</li> <li>.jar</li> <li>.tar</li>
                <li>.tar.gz</li> <li>.tar.bz2</li> <li>.cpio</li> <li>.ar</li> <li>.gz / .gzip</li> </ul> </p> <p>
                <b>Note:</b> This node cannot decompress KNIME artifacts such as workflows. In order to decompress
                workflows please use a combination of <a
                href="https://hub.knime.com/knime/extensions/org.knime.features.buildworkflows/latest/
                org.knime.buildworkflows.reader.WorkflowReaderNodeFactory"><i>Workflow
                Reader</i></a> and <a href="https://kni.me/n/ouYgT_6spFNuvnv_"><i>Workflow Writer</i></a> instead. </p>
                <p> <i>This node can access a variety of different</i> <a
                href="https://docs.knime.com/2021-06/analytics_platform_file_handling_guide/
                index.html#analytics-platform-file-systems"><i>file
                systems.</i></a> <i>More information about file handling in KNIME can be found in the official</i> <a
                href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html"><i>File Handling
                Guide.</i></a> </p>
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            dynamicPort(CONNECTION_INPUT_FILE_PORT_GRP_NAME, CONNECTION_INPUT_FILE_PORT_GRP_NAME, """
                The file system connection.
                """),
            dynamicPort(CONNECTION_OUTPUT_DIR_PORT_GRP_NAME, CONNECTION_OUTPUT_DIR_PORT_GRP_NAME, """
                The file system connection.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("Output table", """
                Table containing the list of files and folders that have been extracted.
                """)
    );

    /**
     * {@inheritDoc}
     * @since 5.11
     */
    @Override
    public NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    /**
     * {@inheritDoc}
     * @since 5.11
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, DecompressNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription( //
            NODE_NAME, //
            NODE_ICON, //
            INPUT_PORTS, //
            OUTPUT_PORTS, //
            SHORT_DESCRIPTION, //
            FULL_DESCRIPTION, //
            List.of(), //
            DecompressNodeParameters.class, //
            null, //
            NodeType.Other, //
            List.of(), //
            null //
        );
    }

    /**
     * {@inheritDoc}
     * @since 5.11
     */
    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, DecompressNodeParameters.class));
    }

}
