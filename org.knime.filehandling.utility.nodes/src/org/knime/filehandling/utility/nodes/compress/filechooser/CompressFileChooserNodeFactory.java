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
 *   27 Aug 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.compress.filechooser;

import static org.knime.node.impl.description.PortDescription.dynamicPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.knime.filehandling.utility.nodes.compress.AbstractCompressNodeConfig;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * Node Factory for the "Compress Files/Folder" no table input node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class CompressFileChooserNodeFactory extends ConfigurableNodeFactory<CompressFileChooserNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder builder = new PortsConfigurationBuilder();
        builder.addOptionalInputPortGroup(AbstractCompressNodeConfig.CONNECTION_INPUT_FILE_PORT_GRP_NAME,
            FileSystemPortObject.TYPE);
        builder.addOptionalInputPortGroup(AbstractCompressNodeConfig.CONNECTION_OUTPUT_DIR_PORT_GRP_NAME,
            FileSystemPortObject.TYPE);

        return Optional.of(builder);
    }

    @Override
    protected CompressFileChooserNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new CompressFileChooserNodeModel(
            (creationConfig.getPortConfig().orElseThrow(IllegalStateException::new)));
    }

    @Override
    @SuppressWarnings("removal")
    public NodeView<CompressFileChooserNodeModel> createNodeView(final int viewIndex,
        final CompressFileChooserNodeModel nodeModel) {
        return null;
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    @SuppressWarnings("removal")
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Compress Files/Folder";

    private static final String NODE_ICON = "../compress16x16.png";

    private static final String SHORT_DESCRIPTION = """
            Compresses a file, a folder or files in folder to an archive.
            """;

    @SuppressWarnings("java:S103")
    private static final String FULL_DESCRIPTION =
        """
                <p> This node compresses files or a folder to an archive. If the compression format is being changed the
                    file extension of the archive to create is adapted automatically. <br /> <br /> Supported archive
                    formats are: <ul> <li>.zip</li> <li>.jar</li> <li>.tar</li> <li>.tar.gz</li> <li>.tar.bz2</li>
                    <li>.cpio</li> </ul> </p> <p> <b>Note:</b>This node cannot compress KNIME artifacts such as workflows.
                    In order to compress workflows please use a combination of <a
                    href="https://hub.knime.com/knime/extensions/org.knime.features.buildworkflows/latest/org.knime.buildworkflows.reader.WorkflowReaderNodeFactory"><i>Workflow
                    Reader</i></a> and <a href="https://kni.me/n/ouYgT_6spFNuvnv_"><i>Workflow Writer</i></a> instead. </p>
                    <p> <i>This node can access a variety of different</i> <a
                    href="https://docs.knime.com/2021-06/analytics_platform_file_handling_guide/index.html#analytics-platform-file-systems"><i>file
                    systems.</i></a> <i>More information about file handling in KNIME can be found in the official</i> <a
                    href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html"><i>File Handling
                    Guide.</i></a> </p>
                """;

    private static final List<PortDescription> INPUT_PORTS =
        List.of(dynamicPort("Source File System Connection", "Source File System Connection", """
                The source file system connection.
                """), dynamicPort("Destination File System Connection", "Destination File System Connection", """
                The destination file system connection.
                """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of();

    @Override
    @SuppressWarnings("removal")
    public NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, CompressFileChooserNodeParameters.class);
    }

    @Override
    public NodeDescription createNodeDescription() {
        return DefaultNodeDescriptionUtil.createNodeDescription(NODE_NAME, NODE_ICON, INPUT_PORTS, OUTPUT_PORTS,
            SHORT_DESCRIPTION, FULL_DESCRIPTION, List.of(), CompressFileChooserNodeParameters.class, null,
            NodeType.Other, List.of(), null);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, CompressFileChooserNodeParameters.class));
    }
}
