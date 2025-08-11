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
 * -------------------------------------------------------------------
 */
package org.knime.base.node.preproc.caseconvert;

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

@SuppressWarnings("restriction")
public final class CaseConvertWebUINodeFactory extends NodeFactory implements NodeDialogFactory {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("Case Converter") //
        .icon("case_converter.png") // existing icon in this package
        .shortDescription("Converts letters in selected string columns to upper- or lowercase.") //
        .fullDescription("""
                This node converts the case of alphanumeric characters in the selected columns.
                Choose the columns to convert and whether to convert to upper- or lowercase. Missing values are left as-is.
                """)//
        .modelSettingsClass(CaseConvertNodeSettings.class) //
        .addInputPort("Input Table", BufferedDataTable.TYPE, "Table with string columns to convert.") //
        .addOutputPort("Transformed Table", BufferedDataTable.TYPE, "Table with converted columns.") //
        .nodeType(NodeType.Manipulator) //
        .keywords("Uppercase", "Lowercase", "Case", "String")//
        .build();

    @Override
    public NodeModel createNodeModel() {
        return new CaseConvertWebUINodeModel(CONFIG, CaseConvertNodeSettings.class);
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView createNodeView(final int i, final NodeModel nodeModel) {
        throw new InternalError();
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return WebUINodeFactory.createNodeDescription(CONFIG);
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, CaseConvertNodeSettings.class);
    }

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }
}
