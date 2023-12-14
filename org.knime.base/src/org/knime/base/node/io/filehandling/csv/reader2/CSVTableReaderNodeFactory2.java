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
import org.knime.base.node.preproc.append.row.AppendedRowsNodeFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialog.OnApplyNodeModifier;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LocalFileChooserWidget;
import org.knime.core.webui.node.impl.PortDescription;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.xml.sax.SAXException;

/**
 * See
 * <ul>
 * <li>{@link AppendedRowsNodeFactory}</li>
 * <li>{@link LocalFileChooserWidget}</li>
 * <li>{@link FileChooser}</li>
 * <li>{@link WebUINodeFactory}</li>
 * <li>{@link OnApplyNodeModifier}</li>
 * <li>{@link NodeContainerEditPart}</li>
 * </ul>
 * CEFNodeView NodeContainerEditPart::openNodeDialog maybe add hasPreview and createNodePreview to NodeDialogFactory
 *
 * Settings are probably view settings, but on apply, if settings have changed, the node will have to be reset using a
 * {@link OnApplyNodeModifier}
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class CSVTableReaderNodeFactory2 extends ConfigurableNodeFactory<CSVTableReaderNodeModel2>
    implements NodeDialogFactory {

    static final String FS_CONNECT_GRP_ID = "File System Connection";

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        var builder = new PortsConfigurationBuilder();
        builder.addOptionalInputPortGroup(FS_CONNECT_GRP_ID, FileSystemPortObject.TYPE);
        builder.addFixedOutputPortGroup("Data Table", BufferedDataTable.TYPE);
        return Optional.of(builder);
    }

    @Override
    protected CSVTableReaderNodeModel2 createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new CSVTableReaderNodeModel2(creationConfig);
    }

    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription("CSV Reader (Labs)", "csvreader.png", new PortDescription[0],
            new PortDescription[]{new PortDescription("name", BufferedDataTable.TYPE, "description")}, "short", "full",
            CSVTableReaderNodeSettings.class, null, null, NodeType.Source, new String[]{"keywords"});

    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<CSVTableReaderNodeModel2> createNodeView(final int viewIndex,
        final CSVTableReaderNodeModel2 nodeModel) {
        return null;
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
