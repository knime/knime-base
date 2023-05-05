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
 *   Nov 8, 2022 (ivan.prigarin): created
 */
package org.knime.base.node.preproc.table.cellextractor;

import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 *
 * @author Ivan Prigarin, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class CellExtractorNodeFactory extends WebUINodeFactory<CellExtractorNodeModel> {

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder()//
        .name("Cell Extractor")//
        .icon("./cell_extractor.png")//
        .shortDescription("Extract a single cell from a table.")//
        .fullDescription("Extract a single cell from the input table and output it as a 1x1 table. "
            + " A flow variable containing the value of the cell will also be created.")//
        .modelSettingsClass(CellExtractorSettings.class)//
        .addInputTable("Input table", "The input table containing the target cell.")//
        .addOutputPort("Output flow variable", FlowVariablePortObject.TYPE,
            "Value of the extracted cell as a flow variable.")//
        .addOutputTable("Output table", "Table containing the extracted cell.")//
        .sinceVersion(5, 0, 0)
        .build();

    /**
     * Build the node.
     */
    public CellExtractorNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public CellExtractorNodeModel createNodeModel() {
        return new CellExtractorNodeModel(CONFIGURATION);
    }

}
