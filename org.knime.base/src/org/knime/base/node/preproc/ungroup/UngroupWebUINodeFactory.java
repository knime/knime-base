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
 *   17.10.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.ungroup;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.xml.sax.SAXException;

/**
 * WebUI Node Factory for the Ungroup node that provides both modern WebUI dialog and legacy dialog support.
 *
 * @author Tobias Koetter, University of Konstanz
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
public class UngroupWebUINodeFactory extends NodeFactory<UngroupNodeModel> implements NodeDialogFactory {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()
        .name("Ungroup")
        .icon("./ungroup.png") // existing icon from the original factory XML
        .shortDescription("Creates for each list of collection values a list of rows with the values of the collection in one column and all other columns given from the original row.")
        .fullDescription("""
                Creates for each list of collection values a list of rows with the values of the collection in one column and all other columns given from the original row.
                Rows with an empty collection are skipped, as well as rows that contain only missing values in the collection cell with the 'Skip missing values' option enabled.
                
                This node takes a table with collection columns and "ungroupes" them by creating individual rows for each element in the collections.
                For example, if you have a row with a list column containing [A, B, C], this node will create three separate rows, each containing one of these values.
                """)
        .modelSettingsClass(UngroupNodeParameters.class)
        .addInputPort("Data table", BufferedDataTable.TYPE, "The input table to ungroup")
        .addOutputPort("Data table", BufferedDataTable.TYPE, "Ungrouped table")
        .nodeType(NodeType.Manipulator)
        .keywords("ungroup", "expand", "collection", "list", "set")
        .build();

    /**
     * {@inheritDoc}
     */
    @Override
    public UngroupNodeModel createNodeModel() {
        return new UngroupNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<UngroupNodeModel> createNodeView(final int viewIndex, final UngroupNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription(CONFIG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, UngroupNodeParameters.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }
}
