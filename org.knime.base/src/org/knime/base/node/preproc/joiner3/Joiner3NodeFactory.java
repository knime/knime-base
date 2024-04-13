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
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.preproc.joiner3;

import java.io.IOException;

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
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.xml.sax.SAXException;

/**
 * This factory create all necessary classes for the joiner node.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Thorsten Meinl, University of Konstanz
 * @since 4.2
 *
 */
@SuppressWarnings("restriction")
public class Joiner3NodeFactory extends NodeFactory<Joiner3NodeModel> implements NodeDialogFactory {

    static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("Joiner") //
        .icon("joiner.png") //
        .shortDescription("Combine matching rows from two tables")//
        .fullDescription("""
                This node combines two tables similar to a join in a database.
                It combines each row from the top input port
                with each row from the bottom input port that has identical values in selected columns.
                Rows that remain unmatched can also be output.
                """)//
        .modelSettingsClass(Joiner3NodeSettings.class) //
        .addInputPort("Left table", BufferedDataTable.TYPE, "Left input table") //
        .addInputPort("Right table", BufferedDataTable.TYPE, "Right input table") //
        .addOutputPort("Join result", BufferedDataTable.TYPE,
            "Either all results or the result of the inner join (if the unmatched rows are output "
                + "in separate ports)") //
        .addOutputPort("Left unmatched rows", BufferedDataTable.TYPE,
            "Unmatched rows from the left input table (top input port). "
                + "Inactive if \"Output unmatched rows to separate ports\" is deactivated.") //
        .addOutputPort("Right unmatched rows", BufferedDataTable.TYPE,
            "Unmatched rows from the right input table (bottom input port). "
                + "Inactive if \"Output unmatched rows to separate ports\" is deactivated.") //
        .nodeType(NodeType.Manipulator) //
        .sinceVersion(4, 2, 0) //
        .addExternalResource("https://www.knime.com/knime-introductory-course/chapter3/section3/joins",
            "KNIME E-Learning Course: Join: inner join, right outer join, left outer join, full outer join")
        .keywords("Combine tables").build();

    @Override
    public Joiner3NodeModel createNodeModel() {
        return new Joiner3NodeModel(CONFIG);
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription(CONFIG);
    }

    @Override
    public NodeView<Joiner3NodeModel> createNodeView(final int viewIndex, final Joiner3NodeModel nodeModel) {
        throw new IndexOutOfBoundsException();
    }

    /**
     * @since 5.3
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, Joiner3NodeSettings.class);
    }

}
