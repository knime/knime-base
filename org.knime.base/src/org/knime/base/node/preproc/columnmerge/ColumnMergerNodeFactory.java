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
package org.knime.base.node.preproc.columnmerge;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * Factory to column merger node.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
public final class ColumnMergerNodeFactory extends WebUINodeFactory<ColumnMergerNodeModel> {

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Column Merger") //
        .icon("./column_merger.png") //
        .shortDescription("Merges two columns into one by choosing the cell that is non-missing.") //
        .fullDescription("""
                        <p>
                  Merges two columns into one by choosing the cell that is
                  non-missing. The configuration dialog allows you to choose a
                  primary and a secondary column. The output of the node will
                  be a new column (or a replacement of the selected input columns),
                  whereby the output value for each row will be
                  <ul>
                    <li>the value in the primary column if it is not missing,</li>
                    <li>the value in the secondary column otherwise.</li>
                  </ul>
                </p>
                <p>
                  Note that the output value might be missing if and only if the
                  secondary column contains a missing value. Also note that the type
                  of the output column is a super type of both selected inputs, i.e.
                  if you choose to merge a number and a string column, the output
                  column will have a very general data type.
                </p>
                        """) //
        .modelSettingsClass(ColumnMergerNodeSettings.class) //
        .nodeType(NodeType.Manipulator) //
        .addInputTable("Input", "Input with two columns to merge.") //
        .addOutputTable("Input with amended column", "Input along with the merged column.") //
        .keywords("Coalesce") //
        .build();

    /**
     * Create a new {@link ColumnMergerNodeFactory}
     */
    public ColumnMergerNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public ColumnMergerNodeModel createNodeModel() {
        return new ColumnMergerNodeModel(CONFIGURATION);
    }

}
