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
 */
package org.knime.base.node.preproc.filter.rowref;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * Factory for the creation of a Reference Row Filter node.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
@SuppressWarnings("restriction")
public class RowFilterRefNodeFactory extends WebUINodeFactory<RowFilterRefNodeModel> {


    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Reference Row Filter") //
        .icon("./refrowfilter.png") //
        .shortDescription("Allows rows to be filtered from the first table using the second table as reference.")
        .fullDescription("""
                This node allows rows to be filtered from the first table using
                the second table as reference. Depending on the dialog
                setting, the rows from the reference table are either included or
                excluded in the output table.<br />
                During the test for in-/exclusion the values of the selected columns
                 of both tables are compared.
                    """) //
        .modelSettingsClass(RowFilterRefNodeSettings.class) //
        .addInputTable("Table to be filtered", "Table from which rows are to be included or excluded")//
        .addInputTable("Reference table", "Table rows used as reference filter") //
        .addOutputTable("Filtered table", "Table with filtered rows") //
        .nodeType(NodeType.Manipulator) //
        .keywords("Filter table") //
        .sinceVersion(5, 3, 0) //
        .build();

    @SuppressWarnings("javadoc")
    public RowFilterRefNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public RowFilterRefNodeModel createNodeModel() {
        return new RowFilterRefNodeModel(CONFIGURATION);
    }

}
