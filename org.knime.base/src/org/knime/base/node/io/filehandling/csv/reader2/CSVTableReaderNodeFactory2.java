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
 *   Dec 11, 2023 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.io.IOException;
import java.util.Optional;

import org.apache.xmlbeans.XmlException;
import org.knime.base.node.io.filehandling.csv.reader.CSVTableReaderNodeFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.impl.PortDescription;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.xml.sax.SAXException;

/**
 * Node factory for the CSV reader node that operates similar to the {@link CSVTableReaderNodeFactory} but features a
 * modern / Web UI {@link NodeDialog}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public class CSVTableReaderNodeFactory2 extends CSVTableReaderNodeFactory // NOSONAR will eventually be merged into a single node
    implements NodeDialogFactory {

    private static final String FULL_DESCRIPTION = """
            <p>
            Reads CSV files. Auto-guesses the structure / format of the selected file.
            </p>

            <p>
            <i>This node can access a variety of different file systems.</i>
            <i>More information about file handling in KNIME can be found in the official</i>
            <a href="https://docs.knime.com/latest/analytics_platform_file_handling_guide/index.html">
            <i>File Handling Guide.</i></a>
            </p>

            <p>
            <b>Parallel reading</b>: Individual files can be read in parallel if
            </p>
            <ul>
                <li>They are located on the machine that is running this node.</li>
                <li>They don't contain any quotes that contain row delimiters.</li>
                <li>They are not gzip compressed.</li>
                <li>No lines or rows are limited or skipped.</li>
                <li>The file index is not prepended to the RowID.</li>
                <li>They are not encoded with UTF-16 (UTF-16LE and UTF-16BE are fine).</li>
            </ul>
            """;

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final var builder = new PortsConfigurationBuilder();
        builder.addOptionalInputPortGroup(FS_CONNECT_GRP_ID, FileSystemPortObject.TYPE);
        builder.addFixedOutputPortGroup("Data Table", BufferedDataTable.TYPE);
        return Optional.of(builder);
    }

    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription("CSV Reader (Labs)", "csvreader.png",
            new PortDescription[]{
                new PortDescription(FS_CONNECT_GRP_ID, FileSystemPortObject.TYPE, "The file system connection.", true)},
            new PortDescription[]{new PortDescription("File Table", BufferedDataTable.TYPE,
                "File being read with number and types of columns guessed automatically.")},
            "Reads CSV files", FULL_DESCRIPTION, CSVTableReaderNodeSettings.class, null, null, NodeType.Source,
            new String[]{"Text", "Comma", "File", "Input", "Read"});
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, CSVTableReaderNodeSettings.class);
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

}
