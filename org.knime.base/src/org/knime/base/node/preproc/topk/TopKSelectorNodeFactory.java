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
 *   Feb 3, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.topk;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * WebUI node factory for the "Top k Row Filter" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class TopKSelectorNodeFactory extends WebUINodeFactory<TopKSelectorNodeModel> {

    /**
     * Default constructor
     */
    public TopKSelectorNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public TopKSelectorNodeModel createNodeModel() {
        return new TopKSelectorNodeModel(CONFIGURATION);
    }

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Top k Row Filter") //
        .icon("elementselector.png") //
        .shortDescription("Selects the top k rows according to user-defined criteria.") //
        .fullDescription("""
                The node behaves the same as a combination of the <b>Sorter</b> node followed by a <b>Row Filter</b> \
                that only keeps the first k rows of the table except for the order of the rows which depends on the \
                <i>Output order</i> settings.
                Note, however, that the implementation of this node is more efficient then the node combination \
                above. In the dialog, select the columns according to which the data should be selected. For each \
                column you can also specify whether a larger or smaller value is considered as superior.
                    """) //
        .modelSettingsClass(TopKSelectorNodeSettings.class) //
        .nodeType(NodeType.Manipulator) //
        .addInputTable("Input Table", "Table to select rows from.") //
        .addOutputTable("Top k Table", "A table containing the top k rows.") //
        .keywords("top", "table", "k", "row", "ascending", "descending", "sort", "filter") //
        .build();
}
