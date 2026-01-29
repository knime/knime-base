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
 *   04.11.2020 (Lars Schweikardt, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.linereader;

import static org.knime.node.impl.description.PortDescription.dynamicPort;
import static org.knime.node.impl.description.PortDescription.fixedPort;

import java.util.List;
import java.util.Map;

import org.knime.base.node.io.filehandling.csv.reader.api.StringReadAdapterFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.node.table.reader.AbstractTableReaderNodeFactory;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AbstractPathTableReaderNodeDialog;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TreeTypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeTester;
import org.knime.node.impl.description.DefaultNodeDescriptionUtil;
import org.knime.node.impl.description.PortDescription;

/**
 * Node factory for the line reader node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Rupert Ettrich, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
public final class LineReaderNodeFactory2 extends AbstractTableReaderNodeFactory<LineReaderConfig2, Class<?>, String>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private static final TypeHierarchy<Class<?>, Class<?>> TYPE_HIERARCHY =
        TreeTypeHierarchy.builder(createTypeTester(String.class)).build();

    private static TypeTester<Class<?>, Class<?>> createTypeTester(final Class<?> type) {
        return TypeTester.createTypeTester(type, s -> true);
    }

    @Override
    protected SettingsModelReaderFileChooser createPathSettings(final NodeCreationConfiguration nodeCreationConfig) {
        return new SettingsModelReaderFileChooser("file_selection",
            nodeCreationConfig.getPortConfig().orElseThrow(IllegalStateException::new), FS_CONNECT_GRP_ID,
            EnumConfig.create(FilterMode.FILE, FilterMode.FILES_IN_FOLDERS));
    }

    @Override
    protected ReadAdapterFactory<Class<?>, String> getReadAdapterFactory() {
        return StringReadAdapterFactory.INSTANCE;
    }

    @Override
    protected TableReader<LineReaderConfig2, Class<?>, String> createReader() {
        return new LineReader2();
    }

    @Override
    protected String extractRowKey(final String value) {
        return value;
    }

    @Override
    protected TypeHierarchy<Class<?>, Class<?>> getTypeHierarchy() {
        return TYPE_HIERARCHY;
    }

    @Override
    protected AbstractPathTableReaderNodeDialog<LineReaderConfig2, Class<?>> createNodeDialogPane(
        final NodeCreationConfiguration creationConfig,
        final MultiTableReadFactory<FSPath, LineReaderConfig2, Class<?>> readFactory,
        final ProductionPathProvider<Class<?>> defaultProductionPathFn) {

        return null;
    }

    @Override
    protected LineMultiTableReadConfig createConfig(final NodeCreationConfiguration nodeCreationConfig) {
        return new LineMultiTableReadConfig();
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static final String NODE_NAME = "Line Reader";

    private static final String NODE_ICON = "linereader.png";

    private static final String SHORT_DESCRIPTION = """
            Reads lines from a file or URL.
            """;

    private static final String FULL_DESCRIPTION =
        """
                <p> Reads lines from a file or URL. Each line will be represented by a single string data
                cell in a single row. The row prefix and column header can be specified in the dialog. </p>
                <p> <i>This node can access a variety of different</i> <a
                href="https://docs.knime.com/2021-06/analytics_platform_file_handling_guide/index.html#analytics-platform-file-systems">
                <i>file systems.</i></a> <i>More information about file handling in KNIME can be found in
                the official</i> <a
                href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html">
                <i>File Handling Guide.</i></a> </p>
                """;

    private static final List<PortDescription> INPUT_PORTS =
        List.of(dynamicPort("File System Connection", "File system connection", """
                The file system connection.
                """));

    private static final List<PortDescription> OUTPUT_PORTS = List.of(fixedPort("Lines from the file(s)", """
            The lines as read from the file(s), each line represented by a single cell in a data row.
            """));

    @SuppressWarnings("deprecation")
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, LineReaderNodeParameters.class);
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
            LineReaderNodeParameters.class, //
            null, //
            NodeType.Source, //
            List.of(), //
            null //
        );
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, LineReaderNodeParameters.class));
    }

}
