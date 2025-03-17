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
 *   28.05.2012 (kilian): created
 */
package org.knime.base.node.preproc.columnresorter;

import java.io.IOException;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.xml.sax.SAXException;

/**
 * The factory of the column resorter node.
 *
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
@SuppressWarnings("restriction")
public class ColumnResorterNodeFactory extends NodeFactory<ColumnResorterNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public ColumnResorterNodeModel createNodeModel() {
        return new ColumnResorterNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<ColumnResorterNodeModel> createNodeView(final int viewIndex,
        final ColumnResorterNodeModel nodeModel) {
        return null;
    }

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("Column Resorter") //
        .icon("column_resorter.png") //
        .shortDescription("Resorts the order of the columns based on user defined settings.") //
        .fullDescription("""
                This node changes the order of the input columns, based on
                user defined settings.
                Columns may be shifted in single steps left or right, or
                completely to the beginning or end of the input table.
                Furthermore columns may also be sorted based on their name.
                The re-sorted table is provided at the out port.
                <br /><br />
                Once the node's dialog has been configured, it is possible to
                connect a new input table with different structure to the
                node and execute it without the need to configure the dialog
                again. New and previously unknown columns will be inserted
                at the position of the column place holder "Any unknown columns".
                This place holder can be positioned anywhere like any column.
                """)//
        .modelSettingsClass(ColumnResorterNodeSettings.class) //
        .addInputPort("Input data", BufferedDataTable.TYPE, "Table containing columns to rearrange.") //
        .addOutputPort("Output data", BufferedDataTable.TYPE,
            "Resorts the order of the columns based on user defined settings") //
        .nodeType(NodeType.Manipulator) //
        .keywords("Column reorder")
        .build();

    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription(CONFIG);
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, ColumnResorterNodeSettings.class);
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, ColumnResorterNodeSettings.class));
    }
}
