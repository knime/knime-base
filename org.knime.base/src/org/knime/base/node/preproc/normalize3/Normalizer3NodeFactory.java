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
 *   19.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.normalize3;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;
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
 * Factory class for the Normalize Node.
 *
 * @author Nicolas Cebron, University of Konstanz
 * @since 3.2
 */
@SuppressWarnings("restriction")
public class Normalizer3NodeFactory extends NodeFactory<Normalizer3NodeModel> implements NodeDialogFactory {
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public Normalizer3NodeModel createNodeModel() {
        return new Normalizer3NodeModel();
    }

    @Override
    public NodeView<Normalizer3NodeModel> createNodeView(final int viewIndex, final Normalizer3NodeModel nodeModel) {
        return null;
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * @since 5.3
     */
    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, NormalizerNodeSettings.class);
    }

    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription(
            WebUINodeConfiguration.builder()//
            .name("Normalizer")//
            .icon("./normalize.png")//
            .shortDescription("Normalizes the attributes of a table.")//
            .fullDescription("This node normalizes the values of all (numeric) columns. In the "
                + "dialog, you can choose the columns you want to work on.")//
            .modelSettingsClass(NormalizerNodeSettings.class)//
            .addInputTable("Table to normalize", "Table requiring normalization of some or all columns.")//
            .addOutputTable("Normalized table", "Table with normalized columns.")//
            .addOutputPort("Normalize Model", null, "Model containing normalization parameters, "
                + "which can be used in a \"Normalizer (Apply)\" node to normalize test data the same way "
                + "as the training data has been normalized.")//
            .nodeType(NodeType.Manipulator)//
            .build());
    }
}
