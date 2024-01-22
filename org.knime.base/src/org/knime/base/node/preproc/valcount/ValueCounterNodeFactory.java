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
package org.knime.base.node.preproc.valcount;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * This factory creates all necessary object for the value counter node.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
public class ValueCounterNodeFactory extends WebUINodeFactory<ValueCounterNodeModel> {
    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("Value Counter") //
        .icon("valueCounter.png") //
        .shortDescription("Counts the occurrences of values in a column") //
        .fullDescription("""
                This node counts the number of occurrences of all values in a selected column.
                """) //
        .modelSettingsClass(ValueCounterNodeSettings.class) //
        .addInputTable("Any datatable", "Any datatable")
        .addOutputTable("Occurrences",
            "One-column table with the different values as row keys and their occurrences as the only column.")
        .nodeType(NodeType.Other) //
        .build();

    /**
     *
     */
    public ValueCounterNodeFactory() {
        super(CONFIG);
    }

    /**
     * @since 5.3
     */
    @Override
    public ValueCounterNodeModel createNodeModel() {
        return new ValueCounterNodeModel(CONFIG);
    }
}
