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
 */
package org.knime.base.node.preproc.columnheaderinsert;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
public final class ColumnHeaderInsertNodeFactory extends WebUINodeFactory<ColumnHeaderInsertNodeModel> {

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Column Renamer (Dictionary)") //
        .icon("insert_col_header.png") //
        .shortDescription("Updates column names of a table according to the mapping in a second dictionary table.") //
        .fullDescription("""
                <p>
                    Updates column names of a table according to the mapping in
                    second dictionary table. The dictionary table needs to contain two
                    columns, one of which contains the lookup (i.e. the column names of
                    the table to be renamed), the other column containing the new
                    column names. The lookup column may be the RowID column.
                </p>
                <p>
                    If the assigned new value in the value column is missing, the original
                    column name will be retained. If the lookup column contains duplicates
                    of the original column names, the node will fail.
                </p>
                    """) //
        .modelSettingsClass(ColumnHeaderInsertSettings.class) //
        .addInputTable("Data table", "Table whose columns are to be renamed.") //
        .addInputTable("Dictionary table", "Table containing two columns: lookup and new value.") //
        .addOutputTable("Data Table with new column names",
            "Input table, whereby the columns are renamed according to the dictionary.") //
        .keywords("Rename column") //
        .build();

    @SuppressWarnings("javadoc")
    public ColumnHeaderInsertNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public ColumnHeaderInsertNodeModel createNodeModel() {
        return new ColumnHeaderInsertNodeModel(CONFIGURATION);
    }

}
