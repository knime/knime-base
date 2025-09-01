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
 * 11.12.2019 (Lars Schweikardt): created
 */
package org.knime.base.node.preproc.tablediff;

import java.util.Map;

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

/**
 * Factory of the "Table Difference Finder" node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v0.0
 */
@SuppressWarnings("restriction")
public final class TableDifferNodeFactory extends NodeFactory<TableDifferNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    @Override
    public TableDifferNodeModel createNodeModel() {
        return new TableDifferNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<TableDifferNodeModel> createNodeView(final int viewIndex, final TableDifferNodeModel nodeModel) {
        return null;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, TableDifferNodeParameters.class);
    }

    @Override
    @SuppressWarnings({"deprecation"})
    public NodeDescription createNodeDescription() {
        final var config = WebUINodeConfiguration.builder().name("Table Difference Finder") //
            .icon("equals.png") //
            .shortDescription("""
                    Compares two tables with respect to their values as well as their column specs.
                    """) //
            .fullDescription("""
                    The Table Difference Finder offers the functionality to compare two tables by means
                    of their values and table specs. Firstly, the values in the selected columns are
                    compared in both tables, and the possible differences are shown for each row and
                    column. Secondly, the types, domains, and positions of the selected columns are
                    compared in both tables, and the results are shown for each column. The selected
                    columns are either all columns in both tables, or a subset of columns in the
                    reference table, i.e. the second input.
                    """) //
            .modelSettingsClass(TableDifferNodeParameters.class) //
            .nodeType(NodeType.Manipulator) //
            .addInputTable("Table to compare to", """
                    Table to check for compliance.
                    """) //
            .addInputTable("Reference table", """
                    Reference table.
                    """) //
            .addOutputTable("Value differences", """
                    Table exhibiting all differing entries.
                    """) //
            .addOutputTable("Domain differences", """
                    Table containing a row for each unique column.
                    """) //
            .build();
        return WebUINodeFactory.createNodeDescription(config);
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, TableDifferNodeParameters.class));
    }

}
