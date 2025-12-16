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
 * History
 *   Apr 26, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.writer;

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
import org.knime.core.node.context.ports.PortsConfiguration;
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
import org.knime.node.impl.description.ExternalResource;
import org.knime.node.impl.description.PortDescription;

/**
 * <code>NodeFactory</code> for CSV writer node.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class CSVWriter2NodeFactory extends ConfigurableNodeFactory<CSVWriter2NodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {
    /** The name of the optional connection input port group. */
    public static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    /** The name of the data table input port group. */
    static final String DATA_TABLE_INPUT_PORT_GRP_NAME = "Data Table";

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder builder = new PortsConfigurationBuilder();
        builder.addOptionalInputPortGroup(CONNECTION_INPUT_PORT_GRP_NAME, FileSystemPortObject.TYPE);
        builder.addFixedInputPortGroup(DATA_TABLE_INPUT_PORT_GRP_NAME, BufferedDataTable.TYPE);
        return Optional.of(builder);
    }

    @Override
    protected CSVWriter2NodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new CSVWriter2NodeModel(getPortsConfig(creationConfig));
    }

    @Override
    @SuppressWarnings("removal")
    public NodeView<CSVWriter2NodeModel> createNodeView(final int viewIndex, final CSVWriter2NodeModel nodeModel) {
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

    private static PortsConfiguration getPortsConfig(final NodeCreationConfiguration creationConfig) {
        return creationConfig.getPortConfig().orElseThrow(IllegalStateException::new);
    }

    private static final String NODE_NAME = "CSV Writer";

    private static final String NODE_ICON = "csvwriter.png";

    private static final String SHORT_DESCRIPTION = """
            Writes a data table into a CSV file.
            """;

    @SuppressWarnings("java:S103")
    private static final String FULL_DESCRIPTION =
        """
                <p> This node writes out the input data table into a file or to a remote location denoted by an URL. The
                    input data table must contain only string or numerical columns. Other column types are not supported.
                    </p> <p> <i>This node can access a variety of different</i> <a
                    href="https://docs.knime.com/2021-06/analytics_platform_file_handling_guide/index.html#analytics-platform-file-systems"><i>file
                    systems.</i></a> <i>More information about file handling in KNIME can be found in the official</i> <a
                    href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html"><i>File Handling
                    Guide.</i></a> </p>
                """;

    private static final List<PortDescription> INPUT_PORTS = List.of( //
        dynamicPort(CONNECTION_INPUT_PORT_GRP_NAME, "File system connection", "The file system connection."),
        fixedPort("Input table", "The data table to write out."));

    private static final List<PortDescription> OUTPUT_PORTS = List.of();

    private static final List<ExternalResource> LINKS = List.of(new ExternalResource(
        "https://www.knime.com/knime-introductory-course/chapter4/section1/write-data-to-a-csv-file", """
                KNIME E-Learning Course: Write Data to a CSV File
                """));

    @Override
    @SuppressWarnings("removal")
    public NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, CSVWriter2NodeParameters.class);
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
            CSVWriter2NodeParameters.class, //
            null, //
            NodeType.Sink, //
            List.of(), //
            null //
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, CSVWriter2NodeParameters.class));
    }
}
