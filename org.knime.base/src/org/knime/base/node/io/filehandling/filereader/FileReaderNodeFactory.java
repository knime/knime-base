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
 *   26.04.2021 (lars.schweikardt): created
 */
package org.knime.base.node.io.filehandling.filereader;

import java.util.Optional;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.core.node.NodeDescription;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import java.util.Map;
import org.knime.node.impl.description.PortDescription;
import java.util.List;
import org.knime.node.impl.description.ExternalResource;
import static org.knime.node.impl.description.PortDescription.fixedPort;
import static org.knime.node.impl.description.PortDescription.dynamicPort;

/**
 *
 * @author lars.schweikardt
 * @author Tim Crundall, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class FileReaderNodeFactory extends ConfigurableNodeFactory<FileReaderNodeModel> implements NodeDialogFactory, KaiNodeInterfaceFactory {

    static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    static final String OUTPUT_PORT_GRP_NAME = "Output Port";

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder b = new PortsConfigurationBuilder();
        b.addOptionalInputPortGroup(CONNECTION_INPUT_PORT_GRP_NAME, FileSystemPortObject.TYPE);
        b.addFixedOutputPortGroup(OUTPUT_PORT_GRP_NAME, BufferedDataTable.TYPE);
        return Optional.of(b);
    }

    @Override
    protected FileReaderNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new FileReaderNodeModel(getPortsConfig(creationConfig), getSettings(creationConfig));
    }


    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FileReaderNodeModel> createNodeView(final int viewIndex, final FileReaderNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static FileReaderNodeSettings getSettings(final NodeCreationConfiguration creationConfig) {
        return new FileReaderNodeSettings(new SettingsModelReaderFileChooser("file_selection",
            getPortsConfig(creationConfig), CONNECTION_INPUT_PORT_GRP_NAME, EnumConfig.create(FilterMode.FILE)));
    }

    private static PortsConfiguration getPortsConfig(final NodeCreationConfiguration creationConfig) {
        return creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
    }
    private static final String NODE_NAME = "File Reader (Complex Format)";
    private static final String NODE_ICON = "./filereader.png";
    private static final String SHORT_DESCRIPTION = """
            Flexible reader for ASCII files.
            """;
    private static final String FULL_DESCRIPTION = """
            This node can be used to read data from a file. It can be configured to read various formats.<br /> When
                you open the node's configuration dialog and provide a filename, it tries to guess the reader's settings
                by analyzing the content of the file. Check the results of these settings in the preview table. If the
                data shown is not correct or an error is reported, you can adjust the settings manually (see below).<br
                /> <p /> The file analysis runs in the background and can be cut short by clicking the "Quick scan",
                which shows if the analysis takes longer. In this case the file is not analyzed completely, but only the
                first fifty lines are taken into account. It could happen then, that the preview appears looking fine,
                but the execution of the File Reader (Complex Format) fails, when it reads the lines it didn't analyze.
                Thus it is recommended you check the settings, when you cut an analysis short. <br /> <p /> <b>Note:</b>
                In case this node is used in a loop, make sure that all files have the same format (e. g. separators,
                column headers, column types). The node saves the configuration only during the first execution. <br />
                Alternatively, the <i>File Reader</i> node can be used. <p> <i>This node can access a variety of
                different</i> <a
                href="https://docs.knime.com/2021-06/analytics_platform_file_handling_guide/index.html#analytics-platform-file-systems"><i>file
                systems.</i></a> <i>More information about file handling in KNIME can be found in the official</i> <a
                href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html"><i>File Handling
                Guide.</i></a> </p>
            """;
    private static final List<PortDescription> INPUT_PORTS = List.of(
            dynamicPort("File System Connection", "File system connection", """
                The file system connection.
                """)
    );
    private static final List<PortDescription> OUTPUT_PORTS = List.of(
            fixedPort("File Table", """
                Datatable just read from the file
                """)
    );
    private static final List<ExternalResource> LINKS = List.of(
         new ExternalResource(
            "https://www.knime.com/knime-introductory-course/chapter2/section1/file-reader-node", """
                KNIME E-Learning Course: File Reader Node
                """)
    );

    @Override
    public NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, FileReaderNodeParameters.class);
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
            LINKS, //
            FileReaderNodeParameters.class, //
            null, //
            NodeType.Source, //
            List.of(), //
            null //
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, FileReaderNodeParameters.class));
    }

}
